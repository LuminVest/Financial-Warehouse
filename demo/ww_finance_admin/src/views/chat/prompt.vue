<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getPromptList, addPrompt, updatePrompt, deletePrompt, type PromptQuery } from '@/api/promptConfig'
import type { PromptConfig } from '@/api/mock'

// ------ 搜索 ------
const searchForm = reactive<PromptQuery>({
  keyword: '',
  scene: '',
  contentType: '',
})

// ------ 列表 ------
const tableData = ref<PromptConfig[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    tableData.value = await getPromptList({
      keyword: searchForm.keyword,
      scene: searchForm.scene,
      contentType: searchForm.contentType,
    })
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchList()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.scene = ''
  searchForm.contentType = ''
  fetchList()
}

// ------ 新增/编辑弹窗 ------
const dialogVisible = ref(false)
const dialogTitle = ref('新增指令')
const formRef = ref<FormInstance>()
const formData = reactive({
  id: 0,
  promptKey: '',
  name: '',
  scene: 'chat',
  contentType: 'prompt',
  systemPrompt: '',
  temperature: 0.7,
  maxTokens: 2048,
  topP: 0.9,
  model: 'gpt-4',
  isDefault: 0,
  status: 1,
  remark: '',
})

const formRules: FormRules = {
  promptKey: [
    { required: true, message: '请输入指令标识', trigger: 'blur' },
    { pattern: /^[A-Z_][A-Z0-9_]*$/i, message: '仅支持字母、数字、下划线', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入指令名称', trigger: 'blur' }],
  scene: [{ required: true, message: '请选择使用场景', trigger: 'change' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  systemPrompt: [{ required: true, message: '请输入指令内容', trigger: 'blur' }],
}

const sceneOptions = [
  { label: '客服对话 (chat)', value: 'chat' },
  { label: '风控审核 (risk)', value: 'risk' },
  { label: '借款审核 (audit)', value: 'audit' },
  { label: '对话摘要 (summary)', value: 'summary' },
]
const contentTypeOptions = [
  { label: '系统指令 (prompt)', value: 'prompt' },
  { label: '关键词列表 (keywords)', value: 'keywords' },
]
const modelOptions = ['gpt-4', 'glm-4', 'qwen-max', 'claude-3-sonnet']
const sceneText: Record<string, string> = { chat: '客服对话', risk: '风控审核', audit: '借款审核', summary: '对话摘要' }

// 关键词类型不需要模型参数
const isKeywords = computed(() => formData.contentType === 'keywords')

function openDialog(row?: PromptConfig) {
  if (row) {
    dialogTitle.value = '编辑指令'
    Object.assign(formData, row)
  } else {
    dialogTitle.value = '新增指令'
    Object.assign(formData, {
      id: 0, promptKey: '', name: '', scene: 'chat', contentType: 'prompt',
      systemPrompt: '', temperature: 0.7, maxTokens: 2048, topP: 0.9, model: 'gpt-4',
      isDefault: 0, status: 1, remark: '',
    })
  }
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

function onContentTypeChange() {
  if (isKeywords.value) {
    formData.temperature = 0
    formData.maxTokens = 0
    formData.topP = 0
    formData.model = '-'
  } else {
    formData.temperature = 0.7
    formData.maxTokens = 2048
    formData.topP = 0.9
    formData.model = 'gpt-4'
  }
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (formData.id) {
        await updatePrompt(formData as PromptConfig)
        ElMessage.success('修改成功')
      } else {
        await addPrompt({
          promptKey: formData.promptKey,
          name: formData.name,
          scene: formData.scene,
          contentType: formData.contentType,
          systemPrompt: formData.systemPrompt,
          temperature: formData.temperature,
          maxTokens: formData.maxTokens,
          topP: formData.topP,
          model: formData.model,
          isDefault: formData.isDefault,
          status: formData.status,
          remark: formData.remark,
        })
        ElMessage.success('新增成功')
      }
      closeDialog()
      fetchList()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : ''
      if (msg) ElMessage.error(msg)
    }
  })
}

// ------ 删除 ------
function handleDelete(row: PromptConfig) {
  if (row.isDefault === 1) {
    ElMessage.warning('默认指令不可删除，请先取消默认')
    return
  }
  ElMessageBox.confirm(`确定删除「${row.name}」(${row.promptKey}) 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deletePrompt(row.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

// ------ 设默认 ------
async function handleSetDefault(row: PromptConfig) {
  await updatePrompt({ ...row, isDefault: 1 })
  ElMessage.success(`已将「${row.name}」设为${sceneText[row.scene]}场景默认指令`)
  fetchList()
}

// ------ 查看指令内容 ------
const contentVisible = ref(false)
const contentData = ref<PromptConfig | null>(null)

function openContent(row: PromptConfig) {
  contentData.value = row
  contentVisible.value = true
}

// ------ 关键词数量统计 ------
function keywordCount(text: string): number {
  return text.split('\n').filter((l) => l.trim()).length
}

// ------ 生命周期 ------
onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Prompt 设置</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增指令
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="名称/标识" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="场景">
          <el-select v-model="searchForm.scene" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in sceneOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.contentType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in contentTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="page-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="指令名称" min-width="180">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-main">
                <span>{{ row.name }}</span>
                <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 6px">默认</el-tag>
              </div>
              <el-tag effect="plain" size="small" class="name-key">{{ row.promptKey }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="场景" width="100" align="center">
          <template #default="{ row }">{{ sceneText[row.scene] || row.scene }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.contentType === 'keywords' ? 'warning' : 'primary'" size="small">
              {{ row.contentType === 'keywords' ? '关键词' : '系统指令' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容预览" min-width="240">
          <template #default="{ row }">
            <span v-if="row.contentType === 'keywords'" class="content-preview">
              共 {{ keywordCount(row.systemPrompt) }} 个关键词
            </span>
            <span v-else class="content-preview">{{ row.systemPrompt.slice(0, 50) }}...</span>
          </template>
        </el-table-column>
        <el-table-column label="模型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.model !== '-'" effect="plain" size="small">{{ row.model }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openContent(row)">查看</el-button>
            <el-button v-if="row.isDefault === 0" type="success" link @click="handleSetDefault(row)">设默认</el-button>
            <el-button type="warning" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      :close-on-click-modal="false"
      @close="closeDialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="指令标识" prop="promptKey">
              <el-input v-model="formData.promptKey" placeholder="如 CS_SYSTEM_PROMPT" :disabled="formData.id !== 0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指令名称" prop="name">
              <el-input v-model="formData.name" placeholder="如 客服系统指令" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="使用场景" prop="scene">
              <el-select v-model="formData.scene" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in sceneOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内容类型" prop="contentType">
              <el-radio-group v-model="formData.contentType" @change="onContentTypeChange">
                <el-radio v-for="o in contentTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="isKeywords ? '关键词' : '系统指令'" prop="systemPrompt">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="isKeywords ? 8 : 10"
            :placeholder="isKeywords ? '每行一个关键词' : '输入系统 Prompt 指令内容...'"
            maxlength="5000"
            show-word-limit
          />
          <div v-if="isKeywords" class="form-hint">每行一个关键词，命中后触发对应逻辑</div>
        </el-form-item>
        <!-- 关键词类型不显示模型参数 -->
        <el-row v-if="!isKeywords" :gutter="20">
          <el-col :span="12">
            <el-form-item label="绑定模型" prop="model">
              <el-select v-model="formData.model" placeholder="请选择" style="width: 100%">
                <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为默认">
              <el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" />
              <span class="form-hint">同场景仅一个默认</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="!isKeywords" :gutter="20">
          <el-col :span="8">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" :precision="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="TopP" prop="topP">
              <el-input-number v-model="formData.topP" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="MaxTokens" prop="maxTokens">
              <el-input-number v-model="formData.maxTokens" :min="1" :step="256" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="isKeywords" label="设为默认">
          <el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" />
          <span class="form-hint">同场景仅一个默认</span>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看指令内容弹窗 -->
    <el-dialog v-model="contentVisible" title="指令详情" width="640px">
      <template v-if="contentData">
        <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="指令标识">
            <el-tag effect="plain" size="small">{{ contentData.promptKey }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="指令名称">{{ contentData.name }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ sceneText[contentData.scene] }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="contentData.contentType === 'keywords' ? 'warning' : 'primary'" size="small">
              {{ contentData.contentType === 'keywords' ? '关键词列表' : '系统指令' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="contentData.model !== '-'" label="模型">{{ contentData.model }}</el-descriptions-item>
          <el-descriptions-item v-if="contentData.model !== '-'" label="温度">{{ contentData.temperature }}</el-descriptions-item>
          <el-descriptions-item v-if="contentData.model !== '-'" label="TopP">{{ contentData.topP }}</el-descriptions-item>
          <el-descriptions-item v-if="contentData.model !== '-'" label="MaxTokens">{{ contentData.maxTokens }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="contentData.status === 1 ? 'success' : 'info'" size="small">
              {{ contentData.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="默认">
            <el-tag v-if="contentData.isDefault === 1" type="success" size="small">默认</el-tag>
            <span v-else>—</span>
          </el-descriptions-item>
        </el-descriptions>
        <h4>{{ contentData.contentType === 'keywords' ? '关键词列表' : '系统指令' }}</h4>
        <pre v-if="contentData.contentType === 'keywords'" class="keyword-content">{{ contentData.systemPrompt }}</pre>
        <pre v-else class="prompt-content">{{ contentData.systemPrompt }}</pre>
        <div v-if="contentData.remark" style="margin-top: 12px">
          <h4>备注</h4>
          <p class="remark-text">{{ contentData.remark }}</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="contentVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  width: 100%;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
}
.search-card {
  margin-bottom: 16px;
  border-radius: 6px;
}
.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}
.page-card {
  border-radius: 6px;
}
.name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.name-main {
  font-weight: 500;
  color: #303133;
}
.name-key {
  width: fit-content;
}
.content-preview {
  color: #909399;
  font-size: 13px;
}
.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.content-viewer h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
}
.prompt-content {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 16px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  max-height: 350px;
  overflow-y: auto;
}
.keyword-content {
  background: #fdf6ec;
  border: 1px solid #f0d784;
  border-radius: 6px;
  padding: 12px 16px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  max-height: 350px;
  overflow-y: auto;
}
.remark-text {
  font-size: 14px;
  color: #606266;
}
</style>
