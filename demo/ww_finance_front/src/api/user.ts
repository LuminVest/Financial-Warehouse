import request from '@/utils/request'

// 用户端 API（对接后端 /api/core/**）

export interface UserDto {
  userType?: number
  mobile: string
  password: string
  passwordto?: string
  code?: string
}

// 发送短信验证码
export function sendCode(mobile: string) {
  return request.post('/api/core/user/sendCode', null, { params: { mobile } })
}

// 注册
export function register(data: UserDto) {
  return request.post('/api/core/user/register', data)
}

// 登录，返回 { token, userInfo }
export function login(data: UserDto) {
  return request.post<{ token: string; userInfo: unknown }>('/api/core/user/login', data)
}

// 当前用户信息
export function getUserInfo() {
  return request.get('/api/core/user/userInfo')
}

// 退出登录
export function logout() {
  return request.get('/api/core/user/logout')
}
