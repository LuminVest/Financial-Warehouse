import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getToken, setToken, clearToken } from '@/utils/request'

export interface UserInfo {
  id: number
  name: string
  mobile: string
  idCard?: string
  borrowAuthStatus?: number
  integral?: number
  userType?: number // 1借款人 2投资人
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserInfo | null>(loadUserInfo())

  const isLogin = computed(() => !!token.value)

  function setAuth(tk: string) {
    token.value = tk
    setToken(tk)
  }

  function setInfo(info: UserInfo | null) {
    userInfo.value = info
    if (info) {
      localStorage.setItem('user_info', JSON.stringify(info))
    } else {
      localStorage.removeItem('user_info')
    }
  }

  function logout() {
    token.value = null
    userInfo.value = null
    clearToken()
    localStorage.removeItem('user_info')
  }

  return { token, userInfo, isLogin, setAuth, setInfo, logout }
})

function loadUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('user_info')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}
