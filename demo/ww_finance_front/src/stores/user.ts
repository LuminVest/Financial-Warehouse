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
  const userInfo = ref<UserInfo | null>(null)

  const isLogin = computed(() => !!token.value)

  function setAuth(tk: string) {
    token.value = tk
    setToken(tk)
  }

  function setInfo(info: UserInfo | null) {
    userInfo.value = info
  }

  function logout() {
    token.value = null
    userInfo.value = null
    clearToken()
  }

  return { token, userInfo, isLogin, setAuth, setInfo, logout }
})
