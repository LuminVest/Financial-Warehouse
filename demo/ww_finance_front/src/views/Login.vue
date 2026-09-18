<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">欢迎登录</h2>
      <p class="subtitle">登录您的账户，开启金融服务之旅</p>
      <el-segmented v-model="form.userType" :options="[{ label: '借款人', value: 1 }, { label: '投资人', value: 2 }]" class="type-tabs" />
      <el-form label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="请输入手机号" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
      <div class="links">
        <el-button text type="primary" @click="router.push('/register')">注册账号</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}
.login-card {
  width: 380px;
}
.title {
  text-align: center;
  color: #409eff;
  margin: 0 0 4px;
}
.subtitle {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin: 0 0 14px;
}
.type-tabs {
  margin: 0 auto 16px;
  display: flex;
  justify-content: center;
}
.links {
  text-align: center;
  margin-top: 8px;
}
</style>
