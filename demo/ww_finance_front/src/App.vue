<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore, type UserInfo } from '@/stores/user'
import { getUserInfo, logout as apiLogout } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

onMounted(async () => {
  if (userStore.isLogin && !userStore.userInfo) {
    try {
      const info = await getUserInfo()
      userStore.setInfo(info as unknown as UserInfo)
    } catch {
      // 未登录态忽略
    }
  }
})

// 左侧全局菜单（PDF P3/P9）：借款人 8 项 / 投资人 5 项
const menus = computed(() => {
  const common = [
    { path: '/center/bind', text: '账户绑定' },
    { path: '/center/flow', text: '资金记录' },
    { path: '/center/charge', text: '充值' },
    { path: '/center/withdraw', text: '提现' },
    { path: '/center/profile', text: '个人中心' },
  ]
  if (userStore.userInfo?.userType === 1) {
    return [
      { path: '/center/bind', text: '账户绑定' },
      { path: '/center/borrow-apply', text: '立即借款' },
      { path: '/center/flow', text: '资金记录' },
      { path: '/center/borrow-record', text: '借款记录' },
      { path: '/center/my-lend-return', text: '还款计划' },
      { path: '/center/charge', text: '充值' },
      { path: '/center/withdraw', text: '提现' },
      { path: '/center/profile', text: '个人中心' },
    ]
  }
  return common
})

function isActive(path: string) {
  return route.path === path || (path === '/center/profile' && route.path === '/center')
}

function goLogin() {
  router.push('/login')
}

async function handleLogout() {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  try {
    await apiLogout()
  } catch {
    // 忽略
  }
  userStore.logout()
  router.push('/home')
}
</script>

<template>
  <div class="app-shell">
    <header class="top-bar">
      <nav class="top-nav">
        <el-button text :class="{ active: $route.path === '/home' }" @click="router.push('/home')">
          首页
        </el-button>
        <el-button text :class="{ active: $route.path === '/invest' }" @click="router.push('/invest')">
          我要投资
        </el-button>
        <el-button text :class="{ active: $route.path === '/safety' }" @click="router.push('/safety')">
          安全保障
        </el-button>
        <el-button text :class="{ active: $route.path === '/about' }" @click="router.push('/about')">
          关于我们
        </el-button>
      </nav>
      <div class="top-right">
        <template v-if="userStore.isLogin">
          <span class="phone">{{ userStore.userInfo?.name || userStore.userInfo?.mobile }}</span>
          <el-button text class="logout" @click="handleLogout">退出登录</el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="goLogin">登录</el-button>
          <el-button size="small" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>
    <div class="body">
      <aside v-if="userStore.isLogin" class="side">
        <div class="side-logo">
          <div class="logo-text">旺旺信贷</div>
          <div class="logo-slogan">让金融服务更简单</div>
        </div>
        <nav class="side-menu">
          <div
            v-for="m in menus"
            :key="m.path"
            class="menu-item"
            :class="{ active: isActive(m.path) }"
            @click="router.push(m.path)"
          >
            {{ m.text }}
          </div>
        </nav>
      </aside>
      <main class="main">
        <router-view />
      </main>
    </div>
    <footer class="footer">旺旺信贷 · 商丘师范学院项目实训 · 模拟教学系统，不涉及真实资金</footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}
.top-bar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.top-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-right .phone {
  font-size: 14px;
  color: #333;
}
.top-right .logout {
  color: #409eff;
}
.nav .active,
.top-nav .active {
  color: #409eff;
  font-weight: 600;
}
.body {
  flex: 1;
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}
.side {
  width: 150px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #eee;
  padding: 20px 0;
}
.side-logo {
  padding: 0 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.side-logo .logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}
.side-logo .logo-slogan {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.side-menu {
  padding: 8px 0;
}
.menu-item {
  padding: 12px 20px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.15s;
}
.menu-item:hover {
  background: #f0f7ff;
  color: #409eff;
}
.menu-item.active {
  background: #f0f7ff;
  color: #409eff;
  border-left-color: #409eff;
  font-weight: 600;
}
.main {
  flex: 1;
  min-width: 0;
  padding: 20px 24px 40px;
  box-sizing: border-box;
}
.footer {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 16px;
}
</style>
