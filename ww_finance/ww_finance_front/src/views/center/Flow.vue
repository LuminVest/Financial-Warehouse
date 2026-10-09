<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getTransFlowPage, type TransFlow } from '@/api/account'

const list = ref<TransFlow[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const typeMap: Record<number, { text: string; type: 'success' | 'danger' | 'warning' | 'info' }> = {
  1: { text: '充值', type: 'success' },
  2: { text: '提现', type: 'danger' },
  3: { text: '投标', type: 'warning' },
  4: { text: '投资回款', type: 'success' },
  5: { text: '放款', type: 'success' },
  6: { text: '还款', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const res = (await getTransFlowPage(page.value, size)) as unknown as {
      records: TransFlow[]
      total: number
    }
    list.value = res.records ?? []
    total.value = Number(res.total ?? 0)
  } finally {
    loading.value = false
  }
}

function fmt(v: number | null | undefined) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2 })
}

onMounted(load)
</script>

<template>
  <div>
    <h3 class="page-title">资金记录</h3>
    <el-table v-loading="loading" :data="list" size="small">
      <el-table-column prop="transNo" label="交易流水号" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeMap[row.transType]?.type ?? 'info'" size="small">
            {{ typeMap[row.transType]?.text ?? row.transType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额（元）" width="130" align="right">
        <template #default="{ row }">
          <span :style="{ color: [1, 4, 5].includes(row.transType) ? '#52c41a' : '#e8421f' }">
            {{ [1, 4, 5].includes(row.transType) ? '+' : '-' }}{{ fmt(row.transAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="memo" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" min-width="160" />
    </el-table>
    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="prev, pager, next, total"
      style="margin-top: 12px; justify-content: flex-end"
      @current-change="load"
    />
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
</style>
