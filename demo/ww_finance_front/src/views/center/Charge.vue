<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commitCharge, getAccount } from '@/api/account'

const amount = ref(100)
const balance = ref(0)
const chargeAgreed = ref(false)
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

async function handleCharge() {
  if (!amount.value || amount.value <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  if (!chargeAgreed.value) {
    ElMessage.warning('请先阅读并同意《旺旺信贷投资咨询与管理服务电子协议》')
    return
  }
  submitting.value = true
  try {
    const html = (await commitCharge(amount.value)) as unknown as string
    submitHtmlForm(html)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h2 class="page-title">充值</h2>
    <div class="sub-title">旺旺银行充值</div>
    <div class="form-card">
      <div class="balance-row">
        <span class="label">当前余额</span>
        <span class="balance">¥{{ Number(balance).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
      </div>
      <div class="field-row">
        <span class="label">充值金额</span>
        <el-input-number v-model="amount" :min="1" :precision="2" :controls="false" style="width: 200px" />
        <span class="unit">元</span>
      </div>
      <div class="agree-row">
        <el-checkbox v-model="chargeAgreed">
          我已阅读并同意《旺旺信贷投资咨询与管理服务电子协议》
        </el-checkbox>
      </div>
      <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleCharge">
        立即充值
      </el-button>
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
.field-row {
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
.field-row .label {
  font-size: 14px;
  color: #333;
}
.field-row .unit {
  font-size: 13px;
  color: #666;
}
.agree-row {
  margin-bottom: 20px;
}
.submit-btn {
  width: 100%;
  height: 42px;
}
</style>
