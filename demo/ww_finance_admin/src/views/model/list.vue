<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getModelConfigList,
  addModelConfig,
  updateModelConfig,
  deleteModelConfig,
  testModelConfig,
  type ModelQuery,
} from '@/api/modelConfig'
import type { ModelConfig } from '@/api/mock'

// ------ 搜索 ------
const searchForm = reactive<ModelQuery>({
  keyword: '',
  modelType: '',
  provider: '',
})

// ------ 列表 ------
const tableData = ref<ModelConfig[]>([])
const loading = ref(false)
const testingId = ref<number | null>(null)

async function fetchList() {
  loading.value = true
  try {
    tableData.value = await getModelConfigList({
      keyword: searchForm.keyword,
      modelType: searchForm.modelType,
      provider: searchForm.provider,
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
  searchForm.modelType = ''
  searchForm.provider = ''
  fetchList()
}

// ------ 新增/编辑弹窗 ------
const dialogVisible = ref(false)
const dialogTitle = ref('新增模型')
const formRef = ref<FormInstance>()
const formData = reactive({
  id: 0,
  name: '',
  provider: 'openai',
  modelType: 'chat',
  modelName: '',
  apiUrl: '',
  apiKey: '',
  maxTokens: 4096,
  temperature: 0.7,
  isDefault: 0,
  status: 1,
  remark: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }],
}

const providerOptions = [
  { label: 'OpenAI', value: 'openai' },
  { label: 'Anthropic (Claude)', value: 'anthropic' },
  { label: '智谱 (GLM)', value: 'zhipu' },
  { label: '通义千问 (Qwen)', value: 'qwen' },
  { label: '百川 (Baichuan)', value: 'baichuan' },
  { label: '本地部署', value: 'local' },
]
const typeOptions = [
  { label: '对话模型 (Chat)', value: 'chat' },
  { label: '嵌入模型 (Embedding)', value: 'embedding' },
  { label: '重排模型 (Rerank)', value: 'rerank' },
]

function openDialog(row?: ModelConfig) {
  if (row) {
    dialogTitle.value = '编辑模型'
    Object.assign(formData, row)
  } else {
    dialogTitle.value = '新增模型'
    Object.assign(formData, {
      id: 0, name: '', provider: 'openai', modelType: 'chat', modelName: '',
      apiUrl: '', apiKey: '', maxTokens: 4096, temperature: 0.7, isDefault: 0, status: 1, remark: '',
    })
  }
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

// 提供商变化时自动填充默认 API 地址
function onProviderChange() {
  const defaults: Record<string, string> = {
    openai: 'https://api.openai.com/v1/chat/completions',
    anthropic: 'https://api.anthropic.com/v1/messages',
    zhipu: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    qwen: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
    baichuan: 'https://api.baichuan-ai.com/v1/chat/completions',
    local: 'http://localhost:8081/',
  }
  if (!formData.apiUrl || Object.values(defaults).includes(formData.apiUrl)) {
    formData.apiUrl = defaults[formData.provider] || ''
  }
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (formData.id) {
        await updateModelConfig(formData as ModelConfig)
        ElMessage.success('修改成功')
      } else {
        await addModelConfig({
          name: formData.name,
          provider: formData.provider,
          modelType: formData.modelType,
          modelName: formData.modelName,
          apiUrl: formData.apiUrl,
          apiKey: formData.apiKey,
          maxTokens: formData.maxTokens,
          temperature: formData.temperature,
          isDefault: formData.isDefault,
          status: formData.status,
          remark: formData.remark,
        })
        ElMessage.success('新增成功')
      }
      closeDialog()
      fetchList()
    } catch {
      // error 已由拦截器处理
    }
  })
}

// ------ 删除 ------
function handleDelete(row: ModelConfig) {
  if (row.isDefault === 1) {
    ElMessage.warning('默认模型不可删除，请先取消默认')
    return
  }
  ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteModelConfig(row.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

// ------ 测试连通性 ------
async function handleTest(row: ModelConfig) {
  testingId.value = row.id
  try {
    const res = await testModelConfig(row.id)
    if (res.success) {
      ElMessage.success(`${row.name} 连接成功（${res.latency}ms）`)
    } else {
      ElMessage.error(`${row.name} 连接失败：${res.message}`)
    }
  } finally {
    testingId.value = null
  }
}

// ------ 设为默认 ------
async function handleSetDefault(row: ModelConfig) {
  await updateModelConfig({ ...row, isDefault: 1 })
  ElMessage.success(`已将「${row.name}」设为默认${row.modelType === 'chat' ? '对话' : row.modelType === 'embedding' ? '嵌入' : '重排'}模型`)
  fetchList()
}

// ------ 工具 ------
const providerMap: Record<string, string> = {
  openai: 'OpenAI',
  anthropic: 'Anthropic',
  zhipu: '智谱',
  qwen: '通义千问',
  baichuan: '百川',
  local: '本地部署',
}
const typeMap: Record<string, { text: string; type: string }> = {
  chat: { text: '对话', type: 'primary' },
  embedding: { text: '嵌入', type: 'success' },
  rerank: { text: '重排', type: 'warning' },
}

// ------ 生命周期 ------
onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>模型列表</h2>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon>
        新增模型
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="名称/模型标识" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.modelType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="提供商">
          <el-select v-model="searchForm.provider" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in providerOptions" :key="o.value" :label="o.label" :value="o.value" />
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
        <el-table-column prop="name" label="模型名称" min-width="150">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 8px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提供商" width="110" align="center">
          <template #default="{ row }">{{ providerMap[row.provider] || row.provider }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.modelType]?.type as string" size="small">
              {{ typeMap[row.modelType]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型标识" min-width="160" />
        <el-table-column prop="apiUrl" label="API地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="最大Token" width="100" align="center">
          <template #default="{ row }">{{ row.maxTokens.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="温度" width="70" align="center">
          <template #default="{ row }">{{ row.temperature }}</template>
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
            <el-button type="primary" link :loading="testingId === row.id" @click="handleTest(row)">测试</el-button>
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
      width="580px"
      :close-on-click-modal="false"
      @close="closeDialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：GPT-4 对话模型" />
        </el-form-item>
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="formData.provider" placeholder="请选择" style="width: 100%" @change="onProviderChange">
            <el-option v-for="o in providerOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-radio-group v-model="formData.modelType">
            <el-radio v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模型标识" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="如：gpt-4, text-embedding-ada-002" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="formData.apiUrl" placeholder="https://api.openai.com/v1/..." />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="formData.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="formData.maxTokens" :min="1" :step="512" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-slider v-model="formData.temperature" :min="0" :max="2" :step="0.1" show-input style="width: 100%" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" />
          <span class="form-hint">同类型模型仅一个默认</span>
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
.form-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
