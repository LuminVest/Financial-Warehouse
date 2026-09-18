import axios from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

// 从 cookie 读取 token（用户端独立 cookie 名，避免与管理端 localhost 串台）
export function getToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)user_token=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

export function setToken(token: string) {
  document.cookie = `user_token=${encodeURIComponent(token)}; path=/`
}

export function clearToken() {
  document.cookie = 'user_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT'
}

const request = axios.create({
  baseURL: '',
  timeout: 30000,
})

// 请求拦截器：自动带 token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.set('token', token)
  }
  return config
})

// 响应拦截器：统一处理 { code, msg, data }
request.interceptors.response.use(
  (response) => {
    // 二进制响应（文件下载等）直接返回
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }
    const res = response.data as ApiResult
    if (res.code === 200) {
      return res.data as never
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const msg = error?.response?.data?.msg || error.message || '网络错误'
    if (msg.includes('未登录') || error?.response?.status === 401) {
      clearToken()
      window.location.hash = '#/login'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default request
