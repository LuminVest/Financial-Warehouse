<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getModelConfigList, addModelConfig, updateModelConfig, deleteModelConfig, setDefaultModel } from '@/api/modelConfig'

interface ModelConfig {
  id: number
  name: string
  modelName: string
  modelType: string
  apiUrl: string
  apiKey: string
  temperature: number
  topP: number
  maxTokens: number
  isDefault: number
  status: number
  remark: string
  createTime?: string
  updateTime?: string
}

const tableData = ref<ModelConfig[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getModelConfigList()
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

// 编辑弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增模型')
const formRef = ref<FormInstance>()
const formData = reactive({
  id: 0,
  name: '',
  modelName: '',
  modelType: 'chat',
  apiUrl: '',
  apiKey: '',
  temperature: 0.7,
  topP: 0.9,
  maxTokens: 2048,
  isDefault: 0,
  status: 1,
  remark: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  apiUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
}

function openDialog(row?: ModelConfig) {
  if (row) {
    dialogTitle.value = '编辑模型'
    Object.assign(formData, row)
  } else {
    dialogTitle.value = '新增模型'
    Object.assign(formData, {
      id: 0, name: '', modelName: '', modelType: 'chat', apiUrl: '', apiKey: '',
      temperature: 0.7, topP: 0.9, maxTokens: 2048, isDefault: 0, status: 1, remark: '',
    })
  }
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  formRef.value?.resetFields()
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (formData.id) {
        await updateModelConfig(formData)
        ElMessage.success('修改成功')
      } else {
        await addModelConfig(formData)
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

// 删除
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

// 设默认
async function handleSetDefault(row: ModelConfig) {
  await setDefaultModel(row.id)
  ElMessage.success(`已将「${row.name}」设为默认模型`)
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>模型配置</h2>
      <el-button type="primary" @click="openDialog()">
        新增模型
      </el-button>
    </div>

    <el-card shadow="never" class="page-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="模型名称" min-width="160">
          <template #default="{ row }">
            <div class="name-cell">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 6px">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型标识" min-width="120">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.modelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.modelType === 'chat' ? '对话' : row.modelType === 'embedding' ? '向量' : row.modelType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiUrl" label="API 地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
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
      width="680px"
      :close-on-click-modal="false"
      @close="closeDialog"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="name">
              <el-input v-model="formData.name" placeholder="如 通义千问" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型标识" prop="modelName">
              <el-input v-model="formData.modelName" placeholder="如 qwen-max" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="formData.modelType" style="width: 100%">
                <el-option label="对话模型" value="chat" />
                <el-option label="向量模型" value="embedding" />
                <el-option label="重排模型" value="rerank" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为默认">
              <el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API 地址" prop="apiUrl">
          <el-input v-model="formData.apiUrl" placeholder="如 https://dashscope.aliyuncs.com/api/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="formData.apiKey" type="password" show-password placeholder="请输入 API Key" />
        </el-form-item>
        <el-row :gutter="20">
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
.page-card {
  border-radius: 6px;
}
.name-cell {
  display: flex;
  align-items: center;
}
</style>
