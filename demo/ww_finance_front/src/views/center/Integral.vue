<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface IntegralInfo {
  totalIntegral?: number
  integral?: number
  levelName?: string
  borrowLimit?: number
  integralList?: Array<{ id: number; integral: number; content: string; createTime: string }>
}

const info = ref<IntegralInfo | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    info.value = (await request.get('/api/user/center/integral/info')) as unknown as IntegralInfo
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h3 class="page-title">我的积分</h3>
    <div v-loading="loading">
      <div class="integral-card">
        <div class="num">{{ info?.totalIntegral ?? info?.integral ?? 0 }}</div>
        <div class="label">当前积分</div>
        <div v-if="info?.levelName" class="level">
          等级：<el-tag type="warning">{{ info.levelName }}</el-tag>
          可借额度：<b style="color: #409eff">¥{{ Number(info.borrowLimit ?? 0).toLocaleString() }}</b>
        </div>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>积分明细</template>
        <el-table :data="info?.integralList ?? []" size="small">
          <el-table-column label="分值" width="100">
            <template #default="{ row }">
              <span style="color: #52c41a">+{{ row.integral }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" min-width="160" />
        </el-table>
      </el-card>

      <el-button style="margin-top: 12px" @click="load">刷新</el-button>
      <el-button text type="primary" @click="ElMessage.info('积分用于匹配借款等级额度，详见讲义')">积分规则？</el-button>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
.integral-card {
  background: linear-gradient(135deg, #faad14, #ffd666);
  border-radius: 12px;
  padding: 28px;
  text-align: center;
  color: #fff;
}
.num {
  font-size: 40px;
  font-weight: 700;
}
.label {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}
.level {
  margin-top: 12px;
}
</style>
