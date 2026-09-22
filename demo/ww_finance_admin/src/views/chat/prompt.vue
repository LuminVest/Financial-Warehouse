<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getPromptList, addPrompt, updatePrompt, deletePrompt, setDefaultPrompt } from '@/api/promptConfig'

interface PromptConfig {
  id: number
  name: string
  systemPrompt: string
  temperature: number
  topP: number
  maxTokens: number
  isDefault: number
  status: number
  remark: string
  createTime?: string
  updateTime?: string
}

// ------ 搜索 ------
const searchForm = reactive({
  keyword: '',
})

// ------ 列表 ------
const tableData = ref<PromptConfig[]>([])
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getPromptList({ keyword: searchForm.keyword })
    tableData.value = res || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchList()
}
function handleReset() {
  searchForm.keyword = ''
  fetchList()
}

// ------ 新增/编辑弹窗 ------
const dialogVisible = ref(false)
const dialogTitle = ref('新增指令')
const formRef = ref<FormInstance>()
const formData = reactive({
  id: 0,
  name: '',
  systemPrompt: '',
  temperature: 0.7,
  topP: 0.9,
  maxTokens: 2000,
  isDefault: 0,
  status: 1,
  remark: '',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入指令名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入指令内容', trigger: 'blur' }],
}

function openDialog(row?: PromptConfig) {
  if (row) {
    dialogTitle.value = '编辑指令'
    Object.assign(formData, row)
  } else {
    dialogTitle.value = '新增指令'
    Object.assign(formData, {
      id: 0, name: '', systemPrompt: '',
      temperature: 0.7, topP: 0.9, maxTokens: 2000,
      isDefault: 0, status: 1, remark: '',
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
        await updatePrompt(formData)
        ElMessage.success('修改成功')
      } else {
        await addPrompt(formData)
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
  ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deletePrompt(row.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

// ------ 设默认 ------
async function handleSetDefault(row: PromptConfig) {
  await setDefaultPrompt(row.id)
  ElMessage.success(`已将「${row.name}」设为默认指令`)
  fetchList()
}

// ------ 查看指令内容 ------
const contentVisible = ref(false)
const contentData = ref<PromptConfig | null>(null)

function openContent(row: PromptConfig) {
  contentData.value = row
  contentVisible.value = true
}

// ------ 生命周期 ------
onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Prompt 设置</h2>
      <el-button type="primary" @click="openDialog()">
        新增指令
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="指令名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
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
            <span>{{ row.name }}</span>
            <el-tag v-if="row.isDefault === 1" type="success" size="small" style="margin-left: 6px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容预览" min-width="300">
          <template #default="{ row }">
            <span class="content-preview">{{ row.systemPrompt.slice(0, 80) }}...</span>
          </template>
        </el-table-column>
        <el-table-column label="温度" width="80" align="center">
          <template #default="{ row }">{{ row.temperature }}</template>
        </el-table-column>
        <el-table-column label="MaxTokens" width="100" align="center">
          <template #default="{ row }">{{ row.maxTokens }}</template>
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
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="指令名称" prop="name">
          <el-input v-model="formData.name" placeholder="如 客服系统指令" />
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="10"
            placeholder="输入系统 Prompt 指令内容..."
            maxlength="5000"
            show-word-limit
          />
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
        <el-form-item label="设为默认">
          <el-switch v-model="formData.isDefault" :active-value="1" :inactive-value="0" />
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
          <el-descriptions-item label="指令名称">{{ contentData.name }}</el-descriptions-item>
          <el-descriptions-item label="温度">{{ contentData.temperature }}</el-descriptions-item>
          <el-descriptions-item label="TopP">{{ contentData.topP }}</el-descriptions-item>
          <el-descriptions-item label="MaxTokens">{{ contentData.maxTokens }}</el-descriptions-item>
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
        <h4>系统指令</h4>
        <pre class="prompt-content">{{ contentData.systemPrompt }}</pre>
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
.content-preview {
  color: #909399;
  font-size: 13px;
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
.remark-text {
  font-size: 14px;
  color: #606266;
}
</style>
