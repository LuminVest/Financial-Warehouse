<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getBindInfo, bind, type UserBind } from '@/api/bind'
import { getUserInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'

const bindInfo = ref<UserBind | null>(null)
const loading = ref(false)

const form = reactive({
  name: '',
  idCard: '',
  bankNo: '',
  bankType: 'CCB',
  mobile: '',
})
const agreed = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const agreementVisible = ref(false)

// 存管账户服务协议（模拟旺旺银行存管服务条款）
const agreementText = `旺旺银行托管账户服务协议

协议编号：WWB-CUST-2026

甲方（用户）：本人
乙方：旺旺银行股份有限公司

鉴于甲方在旺旺金融信贷平台（以下简称"平台"）注册并使用网络借贷信息中介服务，为保障交易资金安全，双方就甲方在乙方开立资金存管电子账户（以下简称"存管账户"）事宜，达成如下协议：

第一条 定义
1.1 存管账户：甲方在乙方开立的，用于存管其在平台交易资金的电子银行账户。
1.2 交易指令：甲方向平台提交，并由平台经授权传输至乙方的充值、投标、还款、提现等资金划转指令。
1.3 资金对账：乙方与平台按日核对存管账户资金变动，确保账实相符。

第二条 服务内容
2.1 甲方委托乙方对其在平台的交易资金进行存管，具体包括：
（1）为甲方开立并管理存管账户；
（2）依据甲方授权与交易指令办理资金收付；
（3）记录并展示甲方账户资金变动明细；
（4）按日与平台完成资金对账。
2.2 乙方存管服务不构成对甲方投资行为的任何担保，不对平台项目的收益作出任何承诺。

第三条 甲方权利与义务
3.1 甲方应保证所提交的姓名、身份证号、银行卡号、手机号等身份与账户信息真实、准确、完整，并对信息的真实性承担相应法律责任。
3.2 甲方应妥善保管账户登录密码、交易密码、短信验证码等敏感信息，因甲方泄露或保管不当造成的损失由甲方自行承担。
3.3 甲方有权随时查询其存管账户的资金余额与交易明细。
3.4 甲方不得利用存管账户从事洗钱、恐怖融资、套现等法律法规禁止的活动。

第四条 乙方权利与义务
4.1 乙方应按照甲方授权指令及时办理资金划转，无正当理由不得延迟或拒绝执行。
4.2 乙方对甲方的账户信息、交易信息负有保密义务，除法律法规另有规定或经甲方书面同意外，不得向任何第三方披露。
4.3 乙方发现可疑交易或甲方账户异常时，有权依法采取限制交易等措施并及时通知甲方。
4.4 乙方按本协议提供存管服务，服务费用按平台公示标准执行（当前阶段免费）。

第五条 授权与委托
5.1 甲方授权平台基于其交易行为向乙方发送资金划转指令，乙方依据该指令办理资金收付。
5.2 甲方可随时终止对平台的授权，但已生效或正在执行中的交易指令不受影响。

第六条 风险提示
6.1 甲方应充分了解网络借贷投资可能存在的信用风险、市场风险与流动性风险，并自行承担相应的投资损失。
6.2 存管账户仅保障资金流转过程的安全，不保障投资本金与收益，乙方不承担任何投资损失赔偿责任。

第七条 协议变更与终止
7.1 乙方可依据法律法规或监管要求修订本协议，修订后的协议在平台公示后对双方生效。
7.2 甲方注销存管账户或经双方协商一致，本协议终止；协议终止不影响已发生交易的资金结算。

第八条 法律适用与争议解决
8.1 本协议适用中华人民共和国法律。
8.2 因本协议产生的争议，双方应友好协商解决；协商不成的，任何一方均可向乙方所在地有管辖权的人民法院提起诉讼。

第九条 其他
9.1 本协议自甲方勾选同意并成功开立存管账户之日起生效。
9.2 本协议以电子形式签署，与纸质协议具有同等法律效力。`

// 表单校验规则：失焦即校验，错误红字显示在字段下方
const rules: FormRules = {
  name: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请填写身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确（需为 18 位）', trigger: 'blur' },
  ],
  bankNo: [
    { required: true, message: '请填写银行卡号', trigger: 'blur' },
    { pattern: /^\d{16,19}$/, message: '银行卡号格式不正确（需为 16-19 位数字）', trigger: 'blur' },
  ],
  bankType: [{ required: true, message: '请选择绑定银行', trigger: 'change' }],
  mobile: [
    { required: true, message: '请填写预留手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '预留手机号格式不正确（需为 11 位手机号）', trigger: 'blur' },
  ],
}

// 银行列表（值=UserBindDTO.bankType）
const banks = [
  { value: 'CCB', label: '建设银行' },
  { value: 'ICBC', label: '工商银行' },
  { value: 'ABC', label: '农业银行' },
  { value: 'BOC', label: '中国银行' },
  { value: 'CMB', label: '招商银行' },
  { value: 'COMM', label: '交通银行' },
  { value: 'PSBC', label: '邮储银行' },
]

function bankLabel(v: string | undefined) {
  return banks.find((b) => b.value === v)?.label ?? v ?? ''
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

async function load() {
  loading.value = true
  try {
    bindInfo.value = (await getBindInfo()) as unknown as UserBind
  } catch {
    // 未绑定可能返回空
  } finally {
    loading.value = false
  }
}

async function handleBind() {
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意《旺旺银行托管账户协议》')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const html = (await bind({ ...form })) as unknown as string
    submitHtmlForm(html)
    // 绑定成功后刷新用户信息（name/idCard/bindStatus 会同步更新）
    try {
      const userStore = useUserStore()
      const info = await getUserInfo()
      userStore.setInfo(info as never)
    } catch {
      // 刷新失败不影响绑定结果
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="page-title">开通第三方账户</h2>
    <div class="sub-title">请开通旺旺银行存管账户以便于您正常理财</div>

    <div v-if="bindInfo && bindInfo.bindCode" class="bound-card">
      <div class="success-icon">✓</div>
      <div class="success-title">账户已绑定</div>
      <div class="success-text">您已完成旺旺银行存管账户的绑定</div>
      <el-descriptions :column="2" border style="margin-top: 20px">
        <el-descriptions-item label="姓名">{{ bindInfo.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ bindInfo.mobile }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ bindInfo.idCard }}</el-descriptions-item>
        <el-descriptions-item label="银行卡号">{{ bindInfo.bankNo }}</el-descriptions-item>
        <el-descriptions-item label="绑定银行">{{ bankLabel(bindInfo.bankType) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" v-else v-loading="loading" label-width="90px" style="max-width: 460px">
      <el-form-item label="真实姓名" prop="name" required>
        <el-input v-model="form.name" placeholder="张三" maxlength="16" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard" required>
        <el-input v-model="form.idCard" placeholder="18 位身份证号" maxlength="18" />
      </el-form-item>
      <div class="idcard-tip">身份证信息认证后将不可修改，请您仔细填写</div>
      <el-form-item label="绑定银行" prop="bankType" required>
        <el-select v-model="form.bankType" style="width: 100%">
          <el-option v-for="b in banks" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="银行卡号" prop="bankNo" required>
        <el-input v-model="form.bankNo" placeholder="本人储蓄卡号" maxlength="19" />
      </el-form-item>
      <el-form-item label="预留手机" prop="mobile" required>
        <el-input v-model="form.mobile" placeholder="银行预留手机号" maxlength="11" />
      </el-form-item>
      <div class="agree-row">
        <el-checkbox v-model="agreed">我已阅读并同意</el-checkbox>
        <el-link type="primary" :underline="false" @click.stop="agreementVisible = true">《旺旺银行托管账户协议》</el-link>
      </div>
      <div style="margin-top: 16px">
        <el-button type="primary" :loading="submitting" @click="handleBind">立即开户</el-button>
      </div>
    </el-form>

    <el-dialog v-model="agreementVisible" title="旺旺银行托管账户服务协议" width="720px" top="6vh">
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
.idcard-tip {
  color: #f56c6c;
  font-size: 12px;
  margin: -6px 0 14px 90px;
}
.agree-row {
  display: flex;
  align-items: center;
}
.agreement-body {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 13px;
  color: #333;
  max-height: 60vh;
  overflow-y: auto;
}
.bound-card {
  text-align: center;
  padding: 32px 0 8px;
}
.success-icon {
  width: 56px;
  height: 56px;
  line-height: 56px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  font-size: 28px;
  margin: 0 auto 16px;
}
.success-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.success-text {
  font-size: 13px;
  color: #666;
  margin-top: 6px;
}
</style>
