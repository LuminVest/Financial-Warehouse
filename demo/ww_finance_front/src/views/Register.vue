<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendCode } from '@/api/user'

const router = useRouter()

const form = reactive({
  userType: 1,
  mobile: '',
  password: '',
  passwordto: '',
  code: '',
})
const loading = ref(false)
const sendLoading = ref(false)
const countdown = ref(0)

async function handleSendCode() {
  if (!/^1\d{10}$/.test(form.mobile)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  sendLoading.value = true
  try {
    await sendCode(form.mobile)
    ElMessage.success('验证码已发送（测试环境任意填）')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sendLoading.value = false
  }
}

async function handleRegister() {
  if (!/^1\d{10}$/.test(form.mobile)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (form.password !== form.passwordto) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    await register({
      userType: form.userType,
      mobile: form.mobile,
      password: form.password,
      passwordto: form.passwordto,
      code: form.code || '8888',
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-wrap">
    <el-card class="register-card">
      <h2 class="title">创建您的账户，开启金融服务之旅</h2>
      <el-segmented v-model="form.userType" :options="[{ label: '借款人', value: 1 }, { label: '投资人', value: 2 }]" class="type-tabs" />
      <el-form label-position="top">
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="请输入手机号" size="large" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="验证码" size="large" />
            <el-button size="large" :disabled="countdown > 0" :loading="sendLoading" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" size="large" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.passwordto" type="password" show-password placeholder="再次输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>
      <div class="links">
        <el-button text type="primary" @click="router.push('/login')">返回登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.register-wrap {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}
.register-card {
  width: 400px;
}
.title {
  text-align: center;
  color: #409eff;
  margin: 0 0 20px;
}
.type-tabs {
  margin: 0 auto 16px;
  display: flex;
  justify-content: center;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}
.links {
  text-align: center;
  margin-top: 8px;
}
</style>
