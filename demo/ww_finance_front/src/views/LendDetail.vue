<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLendDetail, getInterestCount, commitInvest, type Lend } from '@/api/lend'
import { getAccount } from '@/api/account'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const lend = ref<Lend | null>(null)
const loading = ref(false)

const investAmount = ref(100)
const investing = ref(false)
const investAgreed = ref(false)
const expectInterest = ref<number | null>(null)
const calculating = ref(false)
const balance = ref<number | null>(null)

// 输入投资金额 → 实时算预期收益（总利息）
watch(
  [investAmount, lend],
  async () => {
    const amt = investAmount.value
    if (!lend.value || !amt || amt <= 0) {
      expectInterest.value = null
      return
    }
    calculating.value = true
    try {
      expectInterest.value = (await getInterestCount(
        amt,
        lend.value.lendYearRate ?? 0,
        lend.value.period ?? 0,
        lend.value.returnMethod ?? 1,
      )) as unknown as number
    } catch {
      expectInterest.value = null
    } finally {
      calculating.value = false
    }
  },
  { immediate: true },
)

const returnMethodText: Record<number, string> = { 1: '等额本息', 2: '等额本金', 3: '按月付息到期还本' }
const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待发布', type: 'info' },
  1: { text: '募集中', type: 'primary' },
  2: { text: '满标', type: 'success' },
  3: { text: '还款中', type: 'warning' },
  4: { text: '已下架', type: 'info' },
}

const progress = computed(() => {
  if (!lend.value?.amount) return 0
  return Math.min(100, Math.round(((lend.value.investAmount ?? 0) / lend.value.amount) * 100))
})

const remain = computed(() => {
  const l = lend.value
  if (!l) return 0
  return Math.max(0, l.amount - (l.investAmount ?? 0))
})

// 提交投标：后端返回存管表单 HTML，新窗口自动提交到银行
async function handleInvest() {
  if (!lend.value) return
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!investAmount.value || investAmount.value <= 0 || investAmount.value > remain.value) {
    ElMessage.warning(`请输入正确的投标金额（1~${remain.value}）`)
    return
  }
  if (!investAgreed.value) {
    ElMessage.warning('请先阅读并同意《出借协议》')
    return
  }
  investing.value = true
  try {
    const html = (await commitInvest(lend.value.id, investAmount.value)) as unknown as string
    submitHtmlForm(html)
  } catch {
    // 拦截器已提示
  } finally {
    investing.value = false
  }
}

// 把后端返回的 HTML 表单在新窗口打开并自动提交
function submitHtmlForm(html: string) {
  const win = window.open('', '_blank')
  if (!win) {
    ElMessage.warning('浏览器拦截了弹窗，请允许后重试')
    return
  }
  win.document.write(html)
  win.document.close()
}

onMounted(async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    // 后端 show/{id} 返回 { lend: {...}, borrowerName, investProgress }，取 lend 字段
    const data = (await getLendDetail(id)) as unknown as { lend: Lend }
    lend.value = (data && data.lend) || (data as unknown as Lend)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
  if (userStore.isLogin) {
    try {
      const data = (await getAccount()) as unknown as number | { availableAmount: number }
      balance.value = typeof data === 'number' ? data : data?.availableAmount ?? null
    } catch {
      balance.value = null
    }
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="breadcrumb">
      <el-link type="primary" :underline="false" @click="router.push('/home')">首页</el-link>
      <span class="sep">&gt;</span>
      <el-link type="primary" :underline="false" @click="router.push('/invest')">散标投资列表</el-link>
      <span class="sep">&gt;</span>
      <span>项目详情</span>
    </div>

    <template v-if="lend">
      <div class="detail-card">
        <div class="head">
          <h2 class="title">{{ lend.title }}</h2>
          <el-tag :type="(statusMap[lend.status]?.type as any) ?? 'info'">
            {{ statusMap[lend.status]?.text ?? lend.status }}
          </el-tag>
        </div>

        <div class="stat-row">
          <div class="stat">
            <div class="k">借款金额</div>
            <div class="v">{{ Number(lend.amount).toLocaleString() }}元</div>
          </div>
          <div class="stat">
            <div class="k">年利率</div>
            <div class="v">{{ ((lend.lendYearRate ?? 0) * 100).toFixed(2) }}%</div>
          </div>
          <div class="stat">
            <div class="k">借款期限</div>
            <div class="v">{{ lend.period }}个月</div>
          </div>
          <div class="stat">
            <div class="k">还款方式</div>
            <div class="v">{{ returnMethodText[lend.returnMethod] ?? lend.returnMethod }}</div>
          </div>
        </div>

        <div class="progress-area">
          <el-progress :percentage="progress" :stroke-width="12" :color="'#409eff'" style="flex: 1" />
          <span class="progress-pct">{{ progress.toFixed(2) }}%</span>
        </div>
        <div class="invest-count">
          已有 {{ lend.investNum ?? 0 }}人投资
          <span class="invested">已投金额: {{ Number(lend.investAmount ?? 0).toLocaleString() }}元</span>
        </div>

        <!-- 投资表单行（PDF P10 行内布局） -->
        <div class="invest-form">
          <span class="label">投资金额</span>
          <el-input-number v-model="investAmount" :min="1" :max="remain" :precision="2" :controls="false" style="width: 120px" />
          <span class="unit">元</span>
          <span v-loading="calculating" class="expect">
            预期收益 {{ expectInterest != null ? Number(expectInterest).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '—' }}元
          </span>
          <el-checkbox v-model="investAgreed">我已阅读并同意《出借协议》</el-checkbox>
          <el-button
            v-if="lend.status === 1"
            type="primary"
            :loading="investing"
            class="invest-btn"
            @click="handleInvest"
          >
            立即投资
          </el-button>
          <el-tag v-else :type="(statusMap[lend.status]?.type as any) ?? 'info'">
            {{ statusMap[lend.status]?.text ?? lend.status }}
          </el-tag>
        </div>

        <div v-if="userStore.isLogin" class="balance-tip">
          您的账户余额 {{ balance != null ? Number(balance).toLocaleString() : '—' }}元，
          <el-link type="primary" :underline="false" @click="router.push('/center/charge')">马上充值</el-link>
        </div>
      </div>

      <div class="purpose-card">
        <div class="purpose-label">借款用途</div>
        <div class="purpose-text">{{ lend.lendInfo || '—' }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.breadcrumb {
  color: #999;
  font-size: 13px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.breadcrumb .sep {
  color: #ccc;
}
.detail-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.head .title {
  margin: 0;
  font-size: 20px;
  color: #333;
}
.stat-row {
  display: flex;
  gap: 48px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}
.stat .k {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}
.stat .v {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.progress-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}
.progress-pct {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.invest-count {
  font-size: 13px;
  color: #666;
  margin-top: 8px;
}
.invest-count .invested {
  margin-left: 24px;
}
.invest-form {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}
.invest-form .label {
  font-size: 14px;
  color: #333;
}
.invest-form .unit {
  font-size: 13px;
  color: #666;
}
.invest-form .expect {
  font-size: 14px;
  color: #409eff;
  font-weight: 600;
}
.invest-btn {
  margin-left: 4px;
}
.balance-tip {
  margin-top: 16px;
  font-size: 13px;
  color: #666;
}
.purpose-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 24px;
}
.purpose-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.purpose-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
</style>
