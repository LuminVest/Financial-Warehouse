<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Phone, Lock, Key } from '@element-plus/icons-vue'
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
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <div class="logo-icon">旺</div>
        <h1>创建账户</h1>
        <p>开启您的智慧金融之旅</p>
      </div>

      <el-segmented
        v-model="form.userType"
        :options="[{ label: '借款人', value: 1 }, { label: '投资人', value: 2 }]"
        class="type-tabs"
      />

      <el-form size="large" @keyup.enter="handleRegister">
        <el-form-item>
          <el-input
            v-model="form.mobile"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="验证码"
              :prefix-icon="Key"
            />
            <el-button
              :disabled="countdown > 0"
              :loading="sendLoading"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少6位）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.passwordto"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          class="register-btn"
          @click="handleRegister"
        >
          注 册
        </el-button>
      </el-form>

      <div class="register-footer">
        <el-button text type="primary" @click="router.push('/login')">
          返回登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 50%, #0050b3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.register-container::before,
.register-container::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.register-container::before {
  width: 500px;
  height: 500px;
  top: -200px;
  left: -150px;
}
.register-container::after {
  width: 400px;
  height: 400px;
  bottom: -180px;
  right: -120px;
}

.register-box {
  width: 400px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}
.logo-icon {
  width: 56px;
  height: 56px;
  line-height: 56px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  color: #fff;
  font-size: 26px;
  font-weight: bold;
  border-radius: 12px;
}
.register-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
}
.register-header p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.type-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.register-footer {
  margin-top: 16px;
  text-align: center;
}
</style>
