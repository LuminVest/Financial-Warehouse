<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBindInfo, bind, type UserBind } from '@/api/bind'

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
  if (!form.name || !form.idCard || !form.bankNo || !form.mobile) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意《旺旺银行托管账户协议》')
    return
  }
  submitting.value = true
  try {
    const html = (await bind({ ...form })) as unknown as string
    submitHtmlForm(html)
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

    <el-form v-else v-loading="loading" label-width="90px" style="max-width: 460px">
      <el-form-item label="真实姓名">
        <el-input v-model="form.name" placeholder="张三" maxlength="16" />
        <span class="word-count">{{ form.name.length }}/16</span>
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input v-model="form.idCard" placeholder="18 位身份证号" maxlength="18" />
        <span class="word-count">{{ form.idCard.length }}/18</span>
      </el-form-item>
      <div class="idcard-tip">身份证信息认证后将不可修改，请您仔细填写</div>
      <el-form-item label="绑定银行">
        <el-select v-model="form.bankType" style="width: 100%">
          <el-option v-for="b in banks" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="银行卡号">
        <el-input v-model="form.bankNo" placeholder="本人储蓄卡号" maxlength="19" />
      </el-form-item>
      <el-form-item label="预留手机">
        <el-input v-model="form.mobile" placeholder="银行预留手机号" />
      </el-form-item>
      <el-checkbox v-model="agreed">
        我已阅读并同意《旺旺银行托管账户协议》
      </el-checkbox>
      <div style="margin-top: 16px">
        <el-button type="primary" :loading="submitting" @click="handleBind">立即开户</el-button>
      </div>
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
  margin-bottom: 20px;
}
.word-count {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}
.idcard-tip {
  color: #f56c6c;
  font-size: 12px;
  margin: -6px 0 14px 90px;
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
