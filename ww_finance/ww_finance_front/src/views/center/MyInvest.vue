<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getTransFlowPage, type TransFlow } from '@/api/account'

// 我的投资：用资金流水中 投标(3)/投资回款(4) 筛选展示（用户端无独立投资接口时用流水替代）
const list = ref<TransFlow[]>([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const all = (await getTransFlowPage(1, 999)) as unknown as { records: TransFlow[]; total: number }
    const invest = (all.records ?? []).filter((t) => [3, 4].includes(t.transType))
    list.value = invest
    total.value = invest.length
  } finally {
    loading.value = false
  }
}

function fmt(v: number) {
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2 })
}

onMounted(load)
</script>

<template>
  <div>
    <h3 class="page-title">我的投资</h3>
    <el-alert
      title="投资与回款记录（来源：资金流水，投标/投资回款）"
      type="info"
      :closable="false"
      style="margin-bottom: 12px"
    />
    <el-table v-loading="loading" :data="list" size="small">
      <el-table-column prop="transNo" label="流水号" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.transType === 3 ? 'warning' : 'success'" size="small">
            {{ row.transType === 3 ? '投标' : '投资回款' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额（元）" width="130" align="right">
        <template #default="{ row }">
          <span :style="{ color: row.transType === 4 ? '#52c41a' : '#e8421f' }">
            {{ row.transType === 4 ? '+' : '-' }}{{ fmt(row.amount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="memo" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" min-width="160" />
    </el-table>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
</style>
