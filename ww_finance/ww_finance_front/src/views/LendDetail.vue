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
const investAmountText = ref('100')

// 投资金额实时清洗：只留数字/小数点、整数最多8位、小数2位、超剩余可投金额立即截断
function onInvestInput(v: string) {
  let s = v.replace(/[^\d.]/g, '')
  const firstDot = s.indexOf('.')
  if (firstDot !== -1) {
    s = s.slice(0, firstDot + 1) + s.slice(firstDot + 1).replace(/\./g, '')
  }
  const [i, d] = s.split('.')
  const intPart = (i || '').slice(0, 8)
  const decPart = (d || '').slice(0, 2)
  let out = intPart + (firstDot !== -1 ? '.' + decPart : '')
  out = out.replace(/^0+(?=\d)/, '')
  const num = parseFloat(out || '0')
  const cap = remain.value > 0 ? remain.value : 100000000
  if (!Number.isNaN(num) && num > cap) {
    out = String(cap)
    investAmountText.value = out
    investAmount.value = parseFloat(out)
    return
  }
  investAmountText.value = out
  investAmount.value = num
}
const investing = ref(false)
const investAgreed = ref(false)
const expectInterest = ref<number | null>(null)
const calculating = ref(false)
const balance = ref<number | null>(null)
const agreementVisible = ref(false)

// 出借协议（模拟网络借贷出借人服务条款）
const agreementText = `旺旺信贷出借协议
协议编号：WWL-LEND-2026

甲方（出借人）：本协议项下通过旺旺金融信贷平台（以下简称"平台"）向借款项目出借资金的用户
乙方（平台）：旺旺金融信贷平台（平台运营方）

鉴于甲方拟通过平台将自有合法资金出借给经平台审核的借款项目，双方在平等自愿的基础上达成如下协议：

第一条 定义
1.1 借款项目（标的）：指借款人通过平台发布并经平台审核的融资项目，包含借款金额、期限、利率、还款方式等信息。
1.2 出借：指甲方向平台指定借款项目投入资金，并按期获得本金及利息的行为。
1.3 电子协议：以数据电文形式订立并存储于平台的协议文件，与纸质协议具有同等法律效力。

第二条 出借行为
2.1 甲方在平台选定借款项目后，输入出借金额并确认，即视为发出不可撤销的出借指令。
2.2 出借金额、预期年化利率、借款期限、还款方式以平台展示的项目信息为准。
2.3 甲方出借资金自项目满标放款之日起开始计息，按约定的还款方式向甲方分期偿付本金及利息。

第三条 出借人权利与义务
3.1 甲方应保证出借资金来源合法，不得以非法所得进行出借。
3.2 甲方应自行审慎评估借款项目的风险，独立作出出借决策，并自行承担出借风险。
3.3 项目满标放款前，甲方可撤销出借指令；满标放款后，出借资金不可提前赎回。
3.4 甲方应妥善保管账户登录密码、支付密码、短信验证码等敏感信息，因保管不当造成的损失由甲方自行承担。

第四条 平台责任
4.1 平台为网络借贷信息中介，负责对借款项目进行审核与信息披露，为甲方的出借行为提供信息撮合服务。
4.2 平台应如实披露借款项目的金额、期限、利率、还款方式、风险评估等信息。
4.3 平台不承诺、不保证出借本息不受损失，不对借款项目的偿付能力提供任何形式的担保。

第五条 资金存管与划转
5.1 甲方出借资金由资金存管银行独立存管，平台不得挪用、占用。
5.2 甲方出借、回款均通过存管银行划转，平台只负责交易指令的传递与交易信息的记录。

第六条 费用
6.1 平台可按照公示的收费标准向甲方收取服务费用，具体以平台公示为准。
6.2 出借过程中因银行、第三方支付机构产生的费用由甲方承担，平台应提前明示。

第七条 风险提示
7.1 信用风险：借款人不按约偿还本息时，甲方可能面临逾期或本金损失。
7.2 流动性风险：出借资金在项目存续期内不可提前赎回。
7.3 收益不确定性：预期收益仅为测算参考，不构成对实际收益的承诺。
7.4 政策风险：因法律法规、监管政策变化可能导致出借行为受到限制或调整。

第八条 违约与追偿
8.1 借款项目逾期后，平台将通过短信、电话等方式向借款人催收，甲方授权平台协助开展催收工作。
8.2 甲方自行决定是否接受平台的债权转让、诉讼等追偿方案，相关费用由甲方与平台另行约定。

第九条 其他
9.1 本协议适用中华人民共和国法律；因本协议产生的争议，双方应友好协商解决，协商不成的，向乙方所在地有管辖权的人民法院提起诉讼。
9.2 本协议自甲方勾选同意并确认出借之日起生效，项目本息全部结清后自动终止。
9.3 本协议以电子形式签署，与纸质协议具有同等法律效力。`

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
// 风险等级：1低 2中 3高
const riskMap: Record<number, { text: string; type: string }> = {
  1: { text: '低风险', type: 'success' },
  2: { text: '中风险', type: 'warning' },
  3: { text: '高风险', type: 'danger' },
}
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
          <div class="stat">
            <div class="k">风险等级</div>
            <div class="v">
              <el-tag :type="riskMap[lend.riskLevel]?.type ?? 'info'" size="small">
                {{ riskMap[lend.riskLevel]?.text ?? '未知' }}
              </el-tag>
            </div>
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
          <el-input
            v-model="investAmountText"
            :maxlength="11"
            placeholder="请输入"
            style="width: 130px"
            @input="onInvestInput"
          />
          <span class="unit">元</span>
          <span v-loading="calculating" class="expect">
            预期收益 {{ expectInterest != null ? Number(expectInterest).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '—' }}元
          </span>
          <el-checkbox v-model="investAgreed">
            我已阅读并同意
            <el-link type="primary" :underline="false" @click.stop="agreementVisible = true">《出借协议》</el-link>
          </el-checkbox>
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

  <el-dialog v-model="agreementVisible" title="旺旺信贷出借协议" width="720px" top="6vh">
    <div class="agreement-body">{{ agreementText }}</div>
  </el-dialog>
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
.agreement-body {
  max-height: 55vh;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.8;
  color: #333;
}
.invest-form .el-checkbox {
  white-space: nowrap;
}
.invest-form .el-link {
  display: inline;
  vertical-align: baseline;
  padding: 0;
}
</style>
