<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({
  userType: 1,
  mobile: '',
  password: '',
})
const loading = ref(false)

async function handleLogin() {
  if (!form.mobile || !form.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    const res = (await login({
      userType: form.userType,
      mobile: form.mobile,
      password: form.password,
    })) as unknown as {
      token: string
      userInfo: never
    }
    userStore.setAuth(res.token)
    userStore.setInfo(res.userInfo)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/home')
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">旺</div>
        <h1>旺旺金融</h1>
        <p>用户端 · 智慧金融服务</p>
      </div>

      <el-segmented
        v-model="form.userType"
        :options="[{ label: '借款人', value: 1 }, { label: '投资人', value: 2 }]"
        class="type-tabs"
      />

      <el-form size="large" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.mobile"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          class="login-btn"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <div class="login-footer">
        <el-button text type="primary" @click="router.push('/register')">
          注册账号
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 50%, #0050b3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-container::before,
.login-container::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.login-container::before {
  width: 500px;
  height: 500px;
  top: -200px;
  left: -150px;
}
.login-container::after {
  width: 400px;
  height: 400px;
  bottom: -180px;
  right: -120px;
}

.login-box {
  width: 400px;
  padding: 40px 36px 28px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.login-header {
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
.login-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
}
.login-header p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.type-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.login-footer {
  margin-top: 16px;
  text-align: center;
}
</style>
