<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface Message {
  role: 'user' | 'ai'
  content: string
  time: string
}

const messages = ref<Message[]>([
  {
    role: 'ai',
    content: '您好！我是旺旺金融智能客服，请问有什么可以帮您？\n\n您可以问我：\n- 支持哪些还款方式？\n- 企业法人需要满足什么条件？\n- 审批需要多长时间？',
    time: new Date().toLocaleTimeString(),
  },
])
const inputText = ref('')
const loading = ref(false)
const chatBox = ref<HTMLElement>()

// 会话ID（第一次对话后从后端获取）
let sessionId = 0

// 用户身份：首次打开时生成并持久化到 localStorage，
// 同一浏览器刷新后身份不变（对话记忆不丢），不同浏览器/设备互不串号。
// 后续接登录模块后，这里改成从 token 里解析真实用户 ID 即可。
let userId = localStorage.getItem('ww_user_id')
if (!userId) {
  userId = 'u' + Math.random().toString(36).slice(2, 10)
  localStorage.setItem('ww_user_id', userId)
}
const userName = localStorage.getItem('ww_user_name') || '客户'
const userPhone = localStorage.getItem('ww_user_phone') || ''

// 固定的对话ID，用持久化 userId，不用 Date.now()，
// 否则每次刷新页面都会生成新会话，Redis 里的对话记忆就断了
const chatId = `user_${userId}`

// 自动滚动到底部
async function scrollToBottom() {
  await nextTick()
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

// 保存聊天记录到后端
async function saveChatRecord(userMsg: string, aiMsg: string) {
  try {
    const firstQuestion = sessionId === 0 ? userMsg : messages.value[0]?.content || ''
    await fetch('http://localhost:8990/api/core/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: sessionId,
        userName: userName,
        userPhone: userPhone,
        title: firstQuestion,
        kbName: '默认知识库',
        model: 'qwen-plus',
        message: {
          role: 'user',
          content: userMsg,
        }
      })
    }).then(res => res.json()).then((res: any) => {
      if (res.code === 200 && res.data) {
        sessionId = res.data
      }
    })

    // 保存 AI 回复
    await fetch('http://localhost:8990/api/core/chat/save', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: sessionId,
        userName: userName,
        userPhone: userPhone,
        message: {
          role: 'assistant',
          content: aiMsg,
        }
      })
    })
  } catch (e) {
    console.error('保存聊天记录失败:', e)
  }
}

// 发送消息
async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    time: new Date().toLocaleTimeString(),
  })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  // 添加 AI 占位消息
  const aiIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    time: new Date().toLocaleTimeString(),
  })

  try {
    // 调用 finance-ai 的 RAG 问答接口，传 chatId 启用对话记忆
    const response = await fetch(
      `http://localhost:8089/ai/knowledge-chat?prompt=${encodeURIComponent(text)}&chatId=${encodeURIComponent(chatId)}`
    )

    // 流式读取
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

    if (!aiContent) {
      messages.value[aiIndex].content = '抱歉，我暂时无法回答这个问题，请稍后再试。'
    }

    // 保存聊天记录
    await saveChatRecord(text, aiContent)
  } catch (e) {
    console.error('调用智能客服失败:', e)
    messages.value[aiIndex].content = '网络异常，请稍后再试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 快捷问题
const quickQuestions = [
  '支持哪些还款方式？',
  '企业法人需要满足什么条件？',
  '审批需要多长时间？',
  '如何申请贷款？',
]

function askQuick(q: string) {
  inputText.value = q
  sendMessage()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <div class="chat-page">
    <!-- 顶部导航 -->
    <div class="chat-header">
      <el-button :icon="'ArrowLeft'" text @click="router.back()">返回</el-button>
      <h3>智能客服</h3>
      <el-icon size="20" color="#409eff"><ChatDotRound /></el-icon>
    </div>

    <!-- 聊天内容区 -->
    <div ref="chatBox" class="chat-box">
      <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role">
        <!-- AI 头像（在左边） -->
        <div v-if="msg.role === 'ai'" class="avatar ai-avatar">
          <el-icon size="20"><Service /></el-icon>
        </div>

        <!-- 消息气泡 -->
        <div class="message-bubble">
          <div class="bubble-content">{{ msg.content || '正在输入...' }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>

        <!-- 用户头像（在右边） -->
        <div v-if="msg.role === 'user'" class="avatar user-avatar">
          <el-icon size="20"><User /></el-icon>
        </div>
      </div>

      <!-- 加载中提示 -->
      <div v-if="loading" class="loading-row">
        <span class="typing-indicator">正在输入...</span>
      </div>
    </div>

    <!-- 快捷问题 -->
    <div class="quick-questions">
      <el-button
        v-for="q in quickQuestions"
        :key="q"
        size="small"
        round
        plain
        class="quick-btn"
        @click="askQuick(q)"
      >
        {{ q }}
      </el-button>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        placeholder="请输入您的问题..."
        size="large"
        @keyup.enter="sendMessage"
        :disabled="loading"
      >
        <template #append>
          <el-button
            type="primary"
            :loading="loading"
            @click="sendMessage"
          >
            发送
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  gap: 10px;
}

/* AI 消息：左对齐，头像在左 */
.message-row.ai {
  justify-content: flex-start;
}

/* 用户消息：右对齐，头像在右 */
.message-row.user {
  justify-content: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar {
  background: #409eff;
  color: #fff;
}

.user-avatar {
  background: #67c23a;
  color: #fff;
}

.message-bubble {
  max-width: 70%;
}

.bubble-content {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.ai .bubble-content {
  background: #fff;
  color: #303133;
  border-top-left-radius: 0;
}

.message-row.user .bubble-content {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 0;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.message-row.user .message-time {
  text-align: right;
}

.loading-row {
  margin-bottom: 16px;
  padding-left: 46px;
}

.typing-indicator {
  font-size: 13px;
  color: #909399;
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.quick-questions {
  padding: 8px 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-btn {
  margin: 0;
}

.input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}
</style>
