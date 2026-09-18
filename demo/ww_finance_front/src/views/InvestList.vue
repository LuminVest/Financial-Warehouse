<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLendList, type Lend } from '@/api/lend'

const router = useRouter()
const lends = ref<Lend[]>([])
const loading = ref(false)

// 筛选条件
const keyword = ref('')
const methodFilter = ref(0) // 0 全部
const periodFilter = ref(0) // 0 全部

const statusMap: Record<number, { text: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = {
  1: { text: '募集中', type: 'primary' },
  2: { text: '满标', type: 'success' },
  3: { text: '还款中', type: 'warning' },
  4: { text: '已下架', type: 'info' },
  0: { text: '待发布', type: 'info' },
}

const methodText: Record<number, string> = { 1: '等额本息', 2: '等额本金', 3: '按月付息到期还本' }

const filteredLends = computed(() => {
  return lends.value.filter((l) => {
    if (methodFilter.value !== 0 && l.returnMethod !== methodFilter.value) return false
    if (periodFilter.value !== 0 && l.period !== periodFilter.value) return false
    if (keyword.value && !l.title.includes(keyword.value.trim())) return false
    return true
  })
})

onMounted(async () => {
  loading.value = true
  try {
    lends.value = (await getLendList()) as unknown as Lend[]
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="page-head">
      <h2 class="page-title">投资列表</h2>
      <div class="filters">
        <el-select v-model="methodFilter" placeholder="还款方式" style="width: 140px">
          <el-option label="还款方式" :value="0" />
          <el-option v-for="(t, v) in methodText" :key="v" :label="t" :value="Number(v)" />
        </el-select>
        <el-select v-model="periodFilter" placeholder="借款期限" style="width: 130px">
          <el-option label="借款期限" :value="0" />
          <el-option label="3 个月" :value="3" />
          <el-option label="6 个月" :value="6" />
          <el-option label="12 个月" :value="12" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索借款标题" clearable style="width: 180px" />
      </div>
    </div>

    <div v-loading="loading">
      <el-table :data="filteredLends" stripe>
        <el-table-column label="借款标题" min-width="200">
          <template #default="{ row }">
            <span class="lend-title">{{ row.title }}</span>
            <el-tag :type="statusMap[row.status]?.type ?? 'info'" size="small" style="margin-left: 8px">
              {{ statusMap[row.status]?.text ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="借款金额" width="130">
          <template #default="{ row }">{{ Number(row.amount).toLocaleString() }} 元</template>
        </el-table-column>
        <el-table-column label="年利率" width="100">
          <template #default="{ row }">{{ ((row.lendYearRate ?? 0) * 100).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="借款期限" width="100">
          <template #default="{ row }">{{ row.period }}个月</template>
        </el-table-column>
        <el-table-column label="还款方式" width="140">
          <template #default="{ row }">
            {{ methodText[row.returnMethod as number] ?? row.returnMethod }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="primary"
              size="small"
              @click="router.push(`/lend/${row.id}`)"
            >
              投标
            </el-button>
            <span v-else class="status-text">{{ statusMap[row.status]?.text ?? row.status }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的标的" :image-size="80" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.filters {
  display: flex;
  gap: 10px;
}
.lend-title {
  font-weight: 500;
  color: #333;
}
.status-text {
  color: #999;
  font-size: 13px;
}
</style>
