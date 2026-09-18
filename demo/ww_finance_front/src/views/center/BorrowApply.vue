<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBorrowAmount, getBorrowInfoStatus, saveBorrowInfo } from '@/api/borrowInfo'

const router = useRouter()
const amount = ref(0)
const status = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)

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
async function load() {
  loading.value = true
  try {
    amount.value = (await getBorrowAmount()) as unknown as number
    status.value = (await getBorrowInfoStatus()) as unknown as number
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入借款金额')
    return
  }
  if (form.amount > amount.value) {
    ElMessage.warning(`借款金额超过可借额度 ${amount.value} 元`)
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

    <el-form v-loading="loading" label-position="top" class="borrow-form">
      <div class="section-title">借款信息</div>
      <div class="grid-2">
        <el-form-item label="借款金额">
          <div class="amount-row">
            <el-input-number
              v-model="form.amount"
              :min="100"
              :max="Math.max(amount, 100)"
              :step="100"
              :precision="2"
              :disabled="amount <= 0"
              :controls="false"
              style="width: 160px"
            />
            <span class="unit">元</span>
            <span class="limit-tip">您最多可借款{{ Number(amount).toLocaleString() }}元</span>
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
          <el-select v-model="form.returnMethod" style="width: 200px">
            <el-option label="等额本息" :value="1" />
            <el-option label="等额本金" :value="2" />
            <el-option label="按月付息到期还本" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="资金用途">
          <el-select v-model="form.moneyUse" style="width: 200px">
            <el-option label="旅游" :value="1" />
            <el-option label="买房" :value="2" />
            <el-option label="装修" :value="3" />
            <el-option label="医疗" :value="4" />
            <el-option label="美容" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="年利率">
          <div class="amount-row">
            <el-input-number
              v-model="form.borrowYearRate"
              :min="0.04"
              :max="0.24"
              :step="0.01"
              :precision="2"
              :controls="false"
              style="width: 120px"
            />
            <span class="unit">%</span>
            <span class="limit-tip">年利率越高，借款越容易成功</span>
          </div>
        </el-form-item>
      </div>

      <el-button
        type="primary"
        size="large"
        class="submit-btn"
        :loading="submitting"
        :disabled="amount <= 0"
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
.submit-btn {
  width: 220px;
}
</style>
