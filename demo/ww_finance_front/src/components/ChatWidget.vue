<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Message {
  role: 'user' | 'ai'
  content: string
  time: string
}

// 是否展开聊天窗
const open = ref(false)
const messages = ref<Message[]>([
  {
    role: 'ai',
    content: '您好！我是旺旺金融智能客服，请问有什么可以帮您？',
    time: new Date().toLocaleTimeString(),
  },
])
const inputText = ref('')
const loading = ref(false)
const chatBox = ref<HTMLElement>()
// 当前使用的对话模型名，从后端获取
const currentModelName = ref('qwen-plus')

let sessionId = 0
let userId = localStorage.getItem('ww_user_id')
if (!userId) {
  userId = 'u' + Math.random().toString(36).slice(2, 10)
  localStorage.setItem('ww_user_id', userId)
}
// 从登录态读取真实用户名和手机号，游客时显示"客户"
const userName = computed(() => userStore.userInfo?.name || '客户')
const userPhone = computed(() => userStore.userInfo?.mobile || '')
const chatId = `user_${userId}`

async function scrollToBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

async function saveChatRecord(userMsg: string, aiMsg: string) {
  try {
    const firstQuestion = sessionId === 0 ? userMsg : messages.value[1]?.content || ''
    await fetch('http://localhost:8990/api/core/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId, userName: userName.value, userPhone: userPhone.value,
        title: firstQuestion, kbName: '默认知识库', model: currentModelName.value,
        message: { role: 'user', content: userMsg },
      }),
    }).then(res => res.json()).then((res: any) => {
      if (res.code === 200 && res.data) sessionId = res.data
    })
    await fetch('http://localhost:8990/api/core/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId, userName: userName.value, userPhone: userPhone.value,
        message: { role: 'assistant', content: aiMsg },
      }),
    })
  } catch (e) {
    console.error('保存聊天记录失败:', e)
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text, time: new Date().toLocaleTimeString() })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  const aiIndex = messages.value.length
  messages.value.push({ role: 'ai', content: '', time: new Date().toLocaleTimeString() })

  try {
    const response = await fetch(
      `http://localhost:8089/ai/knowledge-chat?prompt=${encodeURIComponent(text)}&chatId=${encodeURIComponent(chatId)}`
    )
    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应')
    const decoder = new TextDecoder()
    let aiContent = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      aiContent += decoder.decode(value, { stream: true })
      messages.value[aiIndex].content = aiContent
      scrollToBottom()
    }
    if (!aiContent) messages.value[aiIndex].content = '抱歉，我暂时无法回答这个问题。'
    await saveChatRecord(text, aiContent)
  } catch (e) {
    console.error('调用智能客服失败:', e)
    messages.value[aiIndex].content = '网络异常，请稍后再试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    scrollToBottom()
    // 每次打开时刷新当前模型
    fetch('http://localhost:8990/api/core/chat/default-model')
      .then(res => res.json())
      .then((res: any) => { if (res.code === 200 && res.data) currentModelName.value = res.data })
      .catch(() => {})
  }
}

// 简单 Markdown 渲染：处理粗体、斜体、换行、列表
function renderMarkdown(text: string): string {
  if (!text) return '正在输入...'
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
  html = html.replace(/^[-•]\s+(.+)$/gm, '<div style="padding-left:12px">• $1</div>')
  html = html.replace(/\n/g, '<br/>')
  return html
}

onMounted(() => {
  scrollToBottom()
  // 获取当前默认对话模型名
  fetch('http://localhost:8990/api/core/chat/default-model')
    .then(res => res.json())
    .then((res: any) => { if (res.code === 200 && res.data) currentModelName.value = res.data })
    .catch(() => {})
})
</script>

<template>
  <div class="chat-widget">
    <!-- 展开的聊天窗 -->
    <transition name="slide-up">
      <div v-if="open" class="chat-panel">
        <div class="chat-panel-header">
          <div class="header-left">
            <el-icon :size="22" color="#fff" class="header-icon"><ChatDotRound /></el-icon>
            <div>
              <div class="header-title">智能客服</div>
              <div class="header-status"><span class="dot"></span>在线</div>
            </div>
          </div>
          <div class="header-actions">
            <el-icon :size="18" color="#fff" class="action-icon" @click="open = false"><Close /></el-icon>
          </div>
        </div>

        <div ref="chatBox" class="chat-messages">
          <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
            <div v-if="msg.role === 'ai'" class="msg-avatar ai">
              <el-icon :size="16" color="#fff"><ChatDotRound /></el-icon>
            </div>
            <div class="msg-bubble" v-html="msg.role === 'ai' ? renderMarkdown(msg.content) : msg.content"></div>
            <div v-if="msg.role === 'user'" class="msg-avatar user">
              <el-icon :size="16" color="#fff">客</el-icon>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="请输入您的问题，Enter 发送，Shift+Enter 换行"
            :disabled="loading"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" :loading="loading" class="send-btn" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>
    </transition>

    <!-- 悬浮按钮（收起时显示） -->
    <div v-if="!open" class="chat-fab" @click="toggle">
      <el-icon :size="24" color="#fff"><ChatDotRound /></el-icon>
    </div>
  </div>
</template>

<style scoped>
.chat-widget {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 9999;
}

/* 悬浮按钮 */
.chat-fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.4);
  transition: transform 0.2s;
}
.chat-fab:hover {
  transform: scale(1.08);
}

/* 聊天窗 */
.chat-panel {
  width: 360px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-panel-header {
  height: 52px;
  background: linear-gradient(135deg, #36cfc9, #1890ff);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  padding: 5px;
}
.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
}
.header-status {
  font-size: 11px;
  color: rgba(255,255,255,0.85);
  display: flex;
  align-items: center;
  gap: 4px;
}
.header-status .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #52c41a;
  display: inline-block;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.action-icon {
  cursor: pointer;
  opacity: 0.9;
}
.action-icon:hover {
  opacity: 1;
}

/* 消息区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f7f8fa;
}
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 13px;
}
.msg-avatar.ai {
  background: linear-gradient(135deg, #36cfc9, #1890ff);
}
.msg-avatar.user {
  background: #409eff;
}
.msg-bubble {
  max-width: 75%;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}
.msg-row.ai .msg-bubble {
  background: #fff;
  color: #333;
  border-top-left-radius: 2px;
}
.msg-row.user .msg-bubble {
  background: #1890ff;
  color: #fff;
  border-top-right-radius: 2px;
}
.msg-bubble.typing {
  color: #999;
}

/* 输入区 */
.chat-input-area {
  padding: 10px 12px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;
}
.chat-input-area .el-textarea {
  flex: 1;
}
.send-btn {
  flex-shrink: 0;
}

/* 展开动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.25s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
</style>
