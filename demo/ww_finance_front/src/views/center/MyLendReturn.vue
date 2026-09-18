<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyLendReturn, commitRepayment, type Lend, type LendReturn } from '@/api/lend'

const route = useRoute()
const groups = ref<Array<{ lend: Lend; returns: LendReturn[] }>>([])
const loading = ref(false)

const activeLendId = computed(() => (route.query.lendId ? Number(route.query.lendId) : null))

const statusMap: Record<number, { text: string; type: 'info' | 'success' | 'danger' }> = {
  0: { text: '未还款', type: 'info' },
  1: { text: '已还款', type: 'success' },
}

function fmt(v: number | null | undefined) {
  return '¥' + Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })
}

function fmtDate(d: string | null) {
  return d ? String(d).slice(0, 10) : '—'
}

function submitHtmlForm(html: string) {
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('浏览器拦截了弹窗，请允许后重试')
    return
  }
  win.document.write(html)
  win.document.close()
}

// 发起还款：调后端拿存管表单，新窗口自动提交
async function handleRepay(lendId: number, currentPeriod: number) {
  try {
    const html = (await commitRepayment(lendId, currentPeriod)) as unknown as string
    submitHtmlForm(html)
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  loading.value = true
  try {
    groups.value = (await getMyLendReturn()) as unknown as Array<{
      lend: Lend
      returns: LendReturn[]
    }>
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <h3 class="page-title">还款计划</h3>

    <el-empty v-if="!loading && groups.length === 0" description="暂无借款标的" />

    <div v-for="g in groups" :key="g.lend.id" class="group" :class="{ active: activeLendId === g.lend.id }">
      <div class="group-head">
        <div>
          <b>{{ g.lend.title }}</b>
          <el-tag size="small" style="margin-left: 8px" type="warning">
            待还 {{ (g.returns ?? []).filter((r) => r.status === 0).length }} 期
          </el-tag>
        </div>
        <div class="meta">
          借款 {{ fmt(g.lend.amount) }} · {{ g.lend.period }} 个月 · 年化
          {{ ((g.lend.lendYearRate ?? 0) * 100).toFixed(2) }}%
        </div>
      </div>

      <el-table :data="g.returns ?? []" size="small">
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="期数" width="90">
          <template #default="{ row }">第 {{ row.currentPeriod }} 期</template>
        </el-table-column>
        <el-table-column label="本金" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.principal) }}</template>
        </el-table-column>
        <el-table-column label="利息" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.interest) }}</template>
        </el-table-column>
        <el-table-column label="本息合计" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.total) }}</template>
        </el-table-column>
        <el-table-column label="应还日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.returnDate) }}</template>
        </el-table-column>
        <el-table-column label="是否逾期" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.isOverdue ? '#e8421f' : '#52c41a' }">
              {{ row.isOverdue ? '是' : '否' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type ?? 'info'" size="small">
              {{ statusMap[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              text
              type="primary"
              size="small"
              @click="handleRepay(g.lend.id, row.currentPeriod)"
            >
              还款
            </el-button>
            <span v-else style="color: #bbb; font-size: 12px">已还</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
.group {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}
.group.active {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff;
}
.group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
}
.meta {
  font-size: 12px;
  color: #999;
}
</style>
