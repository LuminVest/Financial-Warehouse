<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAccount, commitWithdraw } from '@/api/account'

const amount = ref(100)
const balance = ref(0)
const submitting = ref(false)

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
  try {
    const res = (await getAccount()) as unknown as number | { amount?: number }
    balance.value = typeof res === 'number' ? res : Number(res?.amount ?? 0)
  } catch {
    balance.value = 0
  }
})

async function handleWithdraw() {
  if (!amount.value || amount.value <= 0) {
    ElMessage.warning('请输入提现金额')
    return
  }
  if (amount.value > balance.value) {
    ElMessage.warning('提现金额超过余额')
    return
  }
  submitting.value = true
  try {
    const html = (await commitWithdraw(amount.value)) as unknown as string
    submitHtmlForm(html)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h2 class="page-title">提现</h2>
    <div class="sub-title">旺旺银行提现</div>
    <div class="form-card">
      <div class="balance-row">
        <span class="label">当前余额</span>
        <span class="balance">¥{{ Number(balance).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
      </div>
      <div class="field-row">
        <span class="label">提现金额</span>
        <el-input-number
          v-model="amount"
          :min="1"
          :max="Math.max(balance, 1)"
          :disabled="balance <= 0"
          :precision="2"
          :controls="false"
          style="width: 200px"
        />
        <span class="unit">元</span>
      </div>
      <div class="fee-row">
        <span class="label">提现费用</span>
        <span class="fee">¥0.00</span>
      </div>
      <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleWithdraw">
        立即提现
      </el-button>
      <el-alert
        title="温馨提示：提现为 T+1 到账，实际到账金额以银行存管处理结果为准。请确认提现银行卡为本人已绑定银行卡。"
        type="info"
        :closable="false"
        class="tip"
      />
    </div>
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
  margin-bottom: 20px;
}
.form-card {
  max-width: 480px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 24px 28px;
}
.field-row,
.fee-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.balance-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.balance-row .label {
  font-size: 14px;
  color: #333;
}
.balance {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}
.field-row .label,
.fee-row .label {
  font-size: 14px;
  color: #333;
}
.field-row .unit {
  font-size: 13px;
  color: #666;
}
.fee {
  color: #52c41a;
  font-weight: 600;
}
.submit-btn {
  width: 100%;
  height: 42px;
}
.tip {
  margin-top: 16px;
}
</style>
