<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { commitCharge, getAccount } from '@/api/account'

const amount = ref('100')
const balance = ref(0)
const chargeAgreed = ref(false)
const submitting = ref(false)
const agreementVisible = ref(false)

// 投资咨询与管理服务电子协议（模拟平台服务条款）
const agreementText = `旺旺信贷投资咨询与管理服务电子协议
协议编号：WWX-AGREE-2026

甲方：本协议项下在旺旺金融信贷平台注册并接受服务的用户
乙方：旺旺金融信贷平台（平台运营方）

鉴于甲方拟通过平台获得网络借贷信息中介服务及相关的资金存管、支付结算辅助服务，双方在平等自愿的基础上，就甲方向乙方申请投资咨询与管理服务相关事宜达成如下协议：

第一条 定义与解释
1.1 平台：指旺旺金融信贷平台，为用户提供借款、投资等网络借贷信息中介服务的互联网平台。
1.2 用户：指在平台注册并通过实名认证的自然人或法人（甲方）。
1.3 电子协议：指以数据电文形式订立并存储于平台的协议文件，与纸质协议具有同等法律效力。

第二条 服务内容与范围
2.1 乙方为甲方提供以下服务：
（1）借款信息、投资信息的展示与撮合；
（2）充值、提现、投标、还款等交易指令的接收与传递；
（3）协助甲方与资金存管银行完成交易资金的划转；
（4）向甲方提供账户交易记录与资金流水查询服务。
2.2 乙方为网络借贷信息中介，不直接吸收公众存款，不从事非法集资，不对投资本息作出任何形式的承诺或担保。

第三条 甲方权利与义务
3.1 甲方应保证提供的身份信息、联系方式、资产信息真实、准确、完整，并对其真实性负责。
3.2 甲方应确保充值资金来源合法，不得以任何非法所得进行充值或投资。
3.3 甲方应妥善保管账户登录密码、支付密码、短信验证码等敏感信息；因甲方泄露或保管不当造成的损失由甲方自行承担。
3.4 甲方应自行审慎判断投资项目的风险，独立作出投资决策并自行承担相应风险。

第四条 乙方权利与义务
4.1 乙方应向甲方如实披露借款项目的基本信息、风险评估、利率及费用情况。
4.2 乙方应妥善保管甲方交易数据，未经甲方授权不得向任何第三方披露，法律法规另有规定的除外。
4.3 乙方不得挪用、占用甲方交易资金；甲方资金由存管银行独立存管。
4.4 乙方有权按照平台公示的收费标准向甲方收取相关服务费用。

第五条 服务费用
5.1 平台服务费按平台公示的收费标准执行，包括但不限于借款撮合服务费、资金划转手续费等。
5.2 服务费标准的调整以平台公示为准，调整后的标准对双方均具有约束力。

第六条 免责条款
6.1 因不可抗力（包括但不限于自然灾害、战争、政策调整、电力中断、网络故障、黑客攻击等）导致本协议不能履行或延迟履行的，双方互不承担违约责任。
6.2 因银行、第三方支付机构系统原因导致的资金划转延迟，乙方不承担违约责任，但应及时通知甲方并协助处理。

第七条 协议变更与终止
7.1 乙方可依据法律法规或监管要求修订本协议，修订后的协议在平台公示后对甲方生效。
7.2 甲方注销平台账户或经双方协商一致，本协议终止；协议终止不影响已发生交易的资金结算。

第八条 法律适用与争议解决
8.1 本协议适用中华人民共和国法律。
8.2 因本协议产生的争议，双方应友好协商解决；协商不成的，任何一方均可向乙方所在地有管辖权的人民法院提起诉讼。

第九条 协议生效
9.1 本协议自甲方勾选同意并注册成功之日起生效。
9.2 本协议以电子形式签署，与纸质协议具有同等法律效力。`

// 金额输入实时清洗：只留数字和小数点，整数最多 8 位、小数 2 位，且单次不超过 1000 万
function onAmountInput() {
  let v = amount.value.replace(/[^\d.]/g, '')
  const idx = v.indexOf('.')
  if (idx >= 0) {
    v = v.slice(0, idx + 1) + v.slice(idx + 1).replace(/\./g, '')
  }
  const hasDot = v.includes('.')
  let intPart = v.split('.')[0].slice(0, 8)
  const decPart = hasDot ? v.split('.')[1].slice(0, 2) : ''
  if (intPart && parseInt(intPart, 10) > 10000000) {
    intPart = '10000000'
  }
  amount.value = hasDot ? intPart + '.' + decPart : intPart
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

onMounted(async () => {
  try {
    const res = (await getAccount()) as unknown as number | { amount?: number }
    balance.value = typeof res === 'number' ? res : Number(res?.amount ?? 0)
  } catch {
    balance.value = 0
  }
})

async function handleCharge() {
  const amt = Number(amount.value)
  if (!amount.value || isNaN(amt) || amt <= 0) {
    ElMessage.warning('请输入充值金额')
    return
  }
  if (amt > 10000000) {
    ElMessage.warning('单次充值金额不能超过 1000 万元')
    return
  }
  if (!chargeAgreed.value) {
    ElMessage.warning('请先阅读并同意《旺旺信贷投资咨询与管理服务电子协议》')
    return
  }
  submitting.value = true
  try {
    const html = (await commitCharge(amt)) as unknown as string
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
        <el-input
          v-model="amount"
          :maxlength="11"
          inputmode="decimal"
          placeholder="请输入充值金额"
          style="width: 200px"
          @input="onAmountInput"
        />
        <span class="unit">元</span>
      </div>
      <div class="limit-tip">单次最高充值 1000 万元</div>
      <div class="agree-row">
        <el-checkbox v-model="chargeAgreed">
          我已阅读并同意
          <el-link type="primary" :underline="false" @click.stop="agreementVisible = true">《旺旺信贷投资咨询与管理服务电子协议》</el-link>
        </el-checkbox>
      </div>
      <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleCharge">
        立即充值
      </el-button>
    </div>

    <el-dialog v-model="agreementVisible" title="旺旺信贷投资咨询与管理服务电子协议" width="720px" top="6vh">
      <div class="agreement-body">{{ agreementText }}</div>
    </el-dialog>
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
.limit-tip {
  font-size: 12px;
  color: #909399;
  margin: -10px 0 14px;
  padding-left: 84px;
}
.agreement-body {
  max-height: 55vh;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.8;
  color: #333;
}
.agree-row {
  margin-bottom: 20px;
}
.agree-row .el-checkbox {
  white-space: nowrap;
}
.agree-row .el-link {
  display: inline;
  vertical-align: baseline;
  padding: 0;
}
.submit-btn {
  width: 100%;
  height: 42px;
}
</style>
