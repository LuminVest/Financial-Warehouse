<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyBorrowInfo, type BorrowInfo } from '@/api/borrowInfo'

const router = useRouter()
const list = ref<Array<{ borrowInfo: BorrowInfo; lend: Record<string, any> | null }>>([])
const loading = ref(false)

const statusMap: Record<number, { text: string; type: 'info' | 'warning' | 'success' | 'danger' }> = {
  0: { text: '未提交', type: 'info' },
  1: { text: '审核中', type: 'warning' },
  2: { text: '审核通过', type: 'success' },
  [-1]: { text: '不通过', type: 'danger' },
}

const returnMethodText: Record<number, string> = { 1: '等额本息', 2: '等额本金', 3: '按月付息到期还本' }

const lendStatusMap: Record<number, string> = {
  0: '待发布',
  1: '募集中',
  2: '满标',
  3: '还款中',
  4: '已下架',
}

function fmt(v: number | null | undefined) {
  return Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })
}

function fmtRate(v: number | null | undefined) {
  return ((v ?? 0) * 100).toFixed(2) + '%'
}

function viewReturn(lendId: number | null | undefined) {
  if (lendId == null) {
    return
  }
  router.push({ path: '/center/my-lend-return', query: { lendId: String(lendId) } })
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = (await getMyBorrowInfo()) as unknown as Array<{
      borrowInfo: BorrowInfo
      lend: Record<string, any> | null
    }>
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <h3 class="page-title">借款记录</h3>
    <el-table v-loading="loading" :data="list" size="small">
      <el-table-column label="借款金额（元）" width="120" align="right">
        <template #default="{ row }">{{ fmt(row.borrowInfo.amount) }}</template>
      </el-table-column>
      <el-table-column label="期限" width="90">
        <template #default="{ row }">{{ row.borrowInfo.period }} 个月</template>
      </el-table-column>
      <el-table-column label="年化利率" width="100">
        <template #default="{ row }">{{ fmtRate(row.borrowInfo.borrowYearRate) }}</template>
      </el-table-column>
      <el-table-column label="还款方式" width="140">
        <template #default="{ row }">{{ returnMethodText[row.borrowInfo.returnMethod] ?? row.borrowInfo.returnMethod }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.borrowInfo.status]?.type ?? 'info'" size="small">
            {{ statusMap[row.borrowInfo.status]?.text ?? row.borrowInfo.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联标的" min-width="130">
        <template #default="{ row }">
          <template v-if="row.lend">
            <div>{{ row.lend.title }}</div>
            <div style="font-size: 12px; color: #999">状态：{{ lendStatusMap[row.lend.status] ?? row.lend.status }}</div>
          </template>
          <span v-else style="color: #bbb">未生成标的</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ row.borrowInfo.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button
            v-if="row.lend"
            text
            type="primary"
            size="small"
            @click="viewReturn(row.lend.id)"
          >
            查看还款计划
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && list.length === 0" description="暂无借款记录" />
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
</style>
