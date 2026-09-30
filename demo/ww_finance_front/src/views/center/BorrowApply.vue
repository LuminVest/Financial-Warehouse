<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getBorrowAmount, getBorrowInfoStatus, getMyBorrowInfo, saveBorrowInfo, type BorrowInfo } from '@/api/borrowInfo'
import { listByDictCode, type DictItem } from '@/api/dict'

const router = useRouter()
const amount = ref(0)
const status = ref<number | null>(null)
// 最近一次借款申请的审核意见（拒绝原因）
const auditRemark = ref('')
const loading = ref(false)
const submitting = ref(false)
// 当前积分等级信息（额度系数/最低利率由管理端配置）
const gradeInfo = ref<{ levelName?: string; borrowRate?: number; borrowCoefficient?: number } | null>(null)

// 下拉选项（从字典表动态加载）
const returnMethodOptions = ref<DictItem[]>([])
const moneyUseOptions = ref<DictItem[]>([])

const form = reactive({
  amount: 1000,
  period: 6,
  borrowYearRate: 0.08,
  returnMethod: 1,
  moneyUse: 1,
})

const statusText = computed(() => {
  const map: Record<number, string> = { 0: '未提交', 1: '审核中', 2: '已通过', [-1]: '已拒绝' }
  return map[status.value ?? 0] ?? '未知'
})
// 年利率可填下限 = 当前等级最低利率（后端也会强制）
const minRate = computed(() => {
  const r = gradeInfo.value?.borrowRate
  return typeof r === 'number' && r > 0 ? r : 0.04
})
// 输入框按百分数展示（10 = 10%），内部仍存小数（0.1 = 10%）与后端一致
const minRatePercent = computed(() => Math.round(minRate.value * 10000) / 100)
// 金额/年利率文本输入（el-input + maxlength 限制位数：金额整数8位+小数2位，利率整数3位+小数1位）
const amountText = ref('1000')
const rateText = ref('8.0')

function syncTexts() {
  amountText.value = String(form.amount)
  rateText.value = String(Math.round(form.borrowYearRate * 10000) / 100)
}

function onAmountInput() {
  let v = amountText.value.replace(/[^\d.]/g, '')
  const idx = v.indexOf('.')
  if (idx >= 0) {
    v = v.slice(0, idx + 1) + v.slice(idx + 1).replace(/\./g, '')
  }
  const hasDot = v.includes('.')
  const intPart = v.split('.')[0].slice(0, 8)
  const decPart = hasDot ? v.split('.')[1].slice(0, 2) : ''
  v = hasDot ? intPart + '.' + decPart : intPart
  amountText.value = v
  form.amount = v ? parseFloat(v) : 0
}

function onRateInput() {
  let v = rateText.value.replace(/[^\d.]/g, '')
  const idx = v.indexOf('.')
  if (idx >= 0) {
    v = v.slice(0, idx + 1) + v.slice(idx + 1).replace(/\./g, '')
  }
  const hasDot = v.includes('.')
  const intPart = v.split('.')[0].slice(0, 3)
  const decPart = hasDot ? v.split('.')[1].slice(0, 1) : ''
  v = hasDot ? intPart + '.' + decPart : intPart
  rateText.value = v
  form.borrowYearRate = v ? parseFloat(v) / 100 : 0
}
// 审核中：展示当前审核中的申请摘要（只读）
const reviewInfo = ref<{
  amount: number
  period: number
  rate: number
  returnMethodName: string
  moneyUseName: string
  createTime: string
} | null>(null)

async function load() {
  loading.value = true
  try {
    amount.value = (await getBorrowAmount()) as unknown as number
    const st = (await getBorrowInfoStatus()) as unknown as { status: number; auditRemark?: string }
    status.value = st.status
    auditRemark.value = st.auditRemark ?? ''
    gradeInfo.value = (await request.get('/api/user/center/integral/info')) as unknown as {
      levelName?: string
      borrowRate?: number
      borrowCoefficient?: number
    }
    const [rm, mu] = await Promise.all([
      listByDictCode('returnMethod'),
      listByDictCode('moneyUse'),
    ])
    returnMethodOptions.value = rm as unknown as DictItem[]
    moneyUseOptions.value = mu as unknown as DictItem[]
    // 表单默认利率不低于等级最低利率
    if (gradeInfo.value && typeof gradeInfo.value.borrowRate === 'number') {
      form.borrowYearRate = Math.max(form.borrowYearRate, gradeInfo.value.borrowRate)
    }
    syncTexts()
    // 审核中：拉取当前审核中的申请摘要（字典名翻译）
    reviewInfo.value = null
    if (status.value === 1) {
      const list = (await getMyBorrowInfo()) as unknown as Array<{ borrowInfo: BorrowInfo }>
      const cur = list.find((i) => i.borrowInfo.status === 1)
      if (cur) {
        const bi = cur.borrowInfo
        reviewInfo.value = {
          amount: bi.amount,
          period: bi.period,
          rate: bi.borrowYearRate,
          returnMethodName: returnMethodOptions.value.find((o) => o.value === bi.returnMethod)?.name ?? String(bi.returnMethod),
          moneyUseName: moneyUseOptions.value.find((o) => o.value === bi.moneyUse)?.name ?? String(bi.moneyUse),
          createTime: bi.createTime,
        }
      }
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入借款金额')
    return
  }
  if (form.amount < 100) {
    ElMessage.warning('借款金额最低 100 元')
    return
  }
  if (form.amount > amount.value) {
    ElMessage.warning(`借款金额超过可借额度 ${amount.value} 元`)
    return
  }
  if (form.borrowYearRate < minRate.value) {
    ElMessage.warning(`年利率不能低于当前等级最低利率 ${minRatePercent.value}%`)
    return
  }
  if (form.borrowYearRate > 0.24) {
    ElMessage.warning('年利率不能超过 24%')
    return
  }
  submitting.value = true
  try {
    await saveBorrowInfo({
      amount: form.amount,
      period: form.period,
      borrowYearRate: form.borrowYearRate,
      returnMethod: form.returnMethod,
      moneyUse: form.moneyUse,
    })
    ElMessage.success('借款申请已提交，等待管理端审核（通过后自动生成标的）')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="page-title">提交借款信息</h2>
    <div class="sub-title">
      填写借款详情
      <el-tag v-if="status != null" size="small" style="margin-left: 8px">{{ statusText }}</el-tag>
    </div>

    <el-steps :active="status === 2 ? 3 : status === 1 ? 2 : status === -1 ? 2 : 1" align-center style="margin: 0 auto 28px; max-width: 720px">
      <el-step title="提交借款信息" description="填写借款详情" />
      <el-step title="审核" description="等待平台审核" />
      <el-step title="等待审核结果" description="查看审核状态" />
    </el-steps>

    <div v-if="amount <= 0" class="limit-card zero">
      可借额度：<b>¥0</b> —— 借款人认证未通过，暂无法借款
      <el-button text type="primary" size="small" @click="router.push('/center/borrower-auth')">
        去认证
      </el-button>
    </div>

    <!-- 审核中：只显示状态与已提交申请摘要，不显示借款信息表单 -->
    <div v-if="status === 1" v-loading="loading">
      <div class="limit-card">
        借款申请审核中：等待管理端审核，通过后自动生成标的
      </div>
      <div class="review-card">
        <div class="section-title">当前审核中的申请</div>
        <el-descriptions v-if="reviewInfo" :column="2" border>
          <el-descriptions-item label="借款金额">¥{{ Number(reviewInfo.amount).toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="期数">{{ reviewInfo.period }}个月</el-descriptions-item>
          <el-descriptions-item label="年利率">{{ (reviewInfo.rate * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="还款方式">{{ reviewInfo.returnMethodName }}</el-descriptions-item>
          <el-descriptions-item label="资金用途">{{ reviewInfo.moneyUseName }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ reviewInfo.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div v-else class="empty-tip">暂无申请信息</div>
      </div>
    </div>

    <!-- 非审核中：可填写借款信息（未提交/审核不通过可填，已通过可继续申请） -->
    <el-form v-else v-loading="loading" label-position="top" class="borrow-form">
      <div v-if="status === 2" class="limit-card ok">
        上笔借款申请已通过，可继续提交新的借款申请
      </div>
      <div v-else-if="status === -1" class="limit-card reject">
        借款申请未通过：{{ auditRemark || '审核未通过，请修改后重新提交' }}
      </div>
      <div class="section-title">借款信息</div>
      <div class="grid-2">
        <el-form-item label="借款金额">
          <div class="amount-row">
            <el-input
              v-model="amountText"
              :maxlength="11"
              inputmode="decimal"
              placeholder="请输入金额"
              :disabled="amount <= 0"
              style="width: 160px"
              @input="onAmountInput"
            />
            <span class="unit">元</span>
            <span class="limit-tip">您最多可借款{{ Number(amount).toLocaleString() }}元</span>
          </div>
          <div v-if="form.amount > amount" class="over-tip">
            已超出可借额度{{ Number(amount).toLocaleString() }}元，无法提交
          </div>
          <div v-if="gradeInfo?.levelName" class="grade-tip">
            当前等级：<el-tag size="small" type="warning">{{ gradeInfo.levelName }}</el-tag>
            额度系数 x{{ gradeInfo.borrowCoefficient ?? 1 }}（等级越高，额度越高）
          </div>
        </el-form-item>
        <el-form-item label="期数">
          <el-select v-model="form.period" style="width: 200px">
            <el-option label="3个月" :value="3" />
            <el-option label="6个月" :value="6" />
            <el-option label="12个月" :value="12" />
          </el-select>
        </el-form-item>
        <el-form-item label="还款方式">
          <el-select v-model="form.returnMethod" style="width: 200px" placeholder="请选择还款方式">
            <el-option v-for="d in returnMethodOptions" :key="d.value" :label="d.name" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="资金用途">
          <el-select v-model="form.moneyUse" style="width: 200px" placeholder="请选择资金用途">
            <el-option v-for="d in moneyUseOptions" :key="d.value" :label="d.name" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="年利率">
          <div class="amount-row">
            <el-input
              v-model="rateText"
              :maxlength="5"
              inputmode="decimal"
              placeholder="请输入年利率"
              style="width: 120px"
              @input="onRateInput"
            />
            <span class="unit">%</span>
            <span class="limit-tip">
              年利率越高，借款越容易成功；当前等级最低利率 {{ minRatePercent }}%
            </span>
          </div>
        </el-form-item>
      </div>

      <el-button
        v-if="status !== 1"
        type="primary"
        size="large"
        class="submit-btn"
        :loading="submitting"
        :disabled="amount <= 0 || form.amount > amount"
        @click="handleSubmit"
      >
        提交申请
      </el-button>
    </el-form>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  color: #333;
}
.sub-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.borrow-form {
  max-width: 720px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}
.grid-2 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 0 32px;
}
.amount-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.amount-row .unit {
  font-size: 13px;
  color: #666;
}
.amount-row .limit-tip {
  font-size: 12px;
  color: #999;
}
.over-tip {
  font-size: 12px;
  color: #cf1322;
  margin-top: 2px;
}
.grade-tip {
  font-size: 12px;
  color: #666;
  margin-top: 6px;
}
.grade-tip .el-tag {
  margin: 0 4px;
}
.limit-card {
  background: #fff7e6;
  border: 1px solid #ffd591;
  color: #ad6800;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  margin-bottom: 16px;
}
.limit-card.zero {
  background: #fef0f0;
  border-color: #fbc4c4;
  color: #cf1322;
}
.limit-card.ok {
  background: #f0f9eb;
  border-color: #e1f3d8;
  color: #529b2e;
}
.limit-card.reject {
  background: #fef0f0;
  border-color: #fde2e2;
  color: #f56c6c;
}
.review-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  max-width: 720px;
}
.review-card .section-title {
  margin-bottom: 12px;
}
.empty-tip {
  color: #999;
  font-size: 13px;
  padding: 12px 0;
}
.submit-btn {
  width: 220px;
}
</style>
