<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getChatModelBindings, updateChatModelBinding, getSelectOptions } from '@/api/chatModel'
import type { ChatModelBinding } from '@/api/mock'

const tableData = ref<ChatModelBinding[]>([])
const loading = ref(false)

// 下拉选项
const modelOptions = ref<{ id: number; name: string; modelName: string }[]>([])
const promptOptions = ref<{ id: number; promptKey: string; name: string }[]>([])
const kbOptions = ref<{ id: number; name: string }[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [bindings, options] = await Promise.all([getChatModelBindings(), Promise.resolve(getSelectOptions())])
    tableData.value = bindings
    modelOptions.value = options.models
    promptOptions.value = options.prompts
    kbOptions.value = options.knowledgeBases
  } finally {
    loading.value = false
  }
}

// 编辑弹窗
const dialogVisible = ref(false)
const formData = reactive<ChatModelBinding>({
  id: 0, scene: '', sceneName: '', modelId: 0, modelName: '',
  promptId: 0, promptKey: '', kbId: null, kbName: '', enabled: 1, remark: '', updateTime: '',
})

function openDialog(row: ChatModelBinding) {
  Object.assign(formData, row)
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
}

async function submitForm() {
  try {
    await updateChatModelBinding(formData)
    ElMessage.success('配置已更新')
    closeDialog()
    fetchData()
  } catch {
    // error 已由拦截器处理
  }
}

// 快速切换启用状态
async function handleToggleEnabled(row: ChatModelBinding) {
  const newVal = row.enabled === 1 ? 0 : 1
  await updateChatModelBinding({ ...row, enabled: newVal })
  ElMessage.success(newVal === 1 ? '已启用' : '已禁用')
  fetchData()
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>对话模型选择</h2>
    </div>

    <el-alert type="info" :closable="false" class="info-alert">
      为智能客服的每个处理环节选择对应的模型、Prompt 指令和知识库。模型列表在「模型配置」中管理，Prompt 在「Prompt设置」中管理。
    </el-alert>

    <el-card shadow="never" class="page-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="sceneName" label="处理环节" width="120">
          <template #default="{ row }">
            <span class="scene-name">{{ row.sceneName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用模型" min-width="150">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.modelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Prompt 指令" min-width="170">
          <template #default="{ row }">
            <div class="prompt-cell">
              <el-tag effect="plain" size="small" type="success">{{ row.promptKey }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="知识库" min-width="120">
          <template #default="{ row }">
            <span v-if="row.kbName === '—'" class="no-kb">不检索</span>
            <el-tag v-else effect="plain" size="small" type="warning">{{ row.kbName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="warning" link @click="openDialog(row)">配置</el-button>
            <el-button :type="row.enabled === 1 ? 'danger' : 'success'" link @click="handleToggleEnabled(row)">
              {{ row.enabled === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 配置弹窗 -->
    <el-dialog v-model="dialogVisible" :title="`配置 - ${formData.sceneName}`" width="560px" :close-on-click-modal="false" @close="closeDialog">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="处理环节">
          <el-tag>{{ formData.sceneName }}</el-tag>
        </el-form-item>
        <el-form-item label="使用模型">
          <el-select v-model="formData.modelId" placeholder="请选择模型" style="width: 100%">
            <el-option
              v-for="m in modelOptions"
              :key="m.id"
              :label="`${m.name} (${m.modelName})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Prompt 指令">
          <el-select v-model="formData.promptId" placeholder="请选择指令" style="width: 100%">
            <el-option
              v-for="p in promptOptions"
              :key="p.id"
              :label="`${p.name} (${p.promptKey})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="formData.kbId" placeholder="选择知识库" style="width: 100%">
            <el-option
              v-for="k in kbOptions"
              :key="k.id"
              :label="k.id === 0 ? '不检索' : k.name"
              :value="k.id === 0 ? null : k.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="formData.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
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
.info-alert {
  margin-bottom: 16px;
  border-radius: 6px;
}
.page-card {
  border-radius: 6px;
}
.scene-name {
  font-weight: 500;
  color: #303133;
}
.no-kb {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
