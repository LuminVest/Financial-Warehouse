<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getModelConfigList, setDefaultModel } from '@/api/modelConfig'

interface ModelConfig {
  id: number
  name: string
  modelName: string
  modelType: string
  status: number
  remark: string
  isDefault: number
}

const list = ref<ModelConfig[]>([])
const loading = ref(false)
const currentDefault = ref(0)

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getModelConfigList()
    list.value = (res || []).filter((m: ModelConfig) => m.status === 1)
    const def = list.value.find((m: ModelConfig) => m.isDefault === 1)
    currentDefault.value = def ? def.id : 0
  } finally {
    loading.value = false
  }
}

async function chooseModel(row: ModelConfig) {
  if (row.id === currentDefault.value) return
  await setDefaultModel(row.id)
  currentDefault.value = row.id
  ElMessage.success(`智能客服已切换为「${row.name}」`)
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>对话模型选择</h2>
      <p class="page-desc">选择智能客服当前使用的对话模型，切换后实时生效</p>
    </div>

    <el-card shadow="never" class="page-card">
      <el-table :data="list" v-loading="loading" stripe border @row-click="chooseModel">
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-radio v-model="currentDefault" :value="row.id" @click.stop>
              <span></span>
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column label="模型名称" min-width="180">
          <template #default="{ row }">
            <span style="font-weight: 600">{{ row.name }}</span>
            <el-tag v-if="row.id === currentDefault" type="success" size="small" style="margin-left: 8px">使用中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型标识" min-width="140">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.modelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button v-if="row.id !== currentDefault" type="primary" link @click.stop="chooseModel(row)">
              设为当前模型
            </el-button>
            <span v-else style="color: #67c23a; font-size: 13px">正在使用</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无可选对话模型，请先在模型配置中添加" />
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  width: 100%;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 6px;
}
.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
}
.page-card {
  border-radius: 6px;
}
</style>
