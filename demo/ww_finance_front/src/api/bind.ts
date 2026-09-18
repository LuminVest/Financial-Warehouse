import request from '@/utils/request'

// 存管绑卡 API

export interface UserBind {
  id: number
  userId: number
  name: string
  mobile: string
  idCard: string
  bankNo: string
  bankType?: string
  bindCode: string
  status: number // 0-未绑定 1-已绑定
  returnUrl?: string
  notifyUrl?: string
}

export interface BindDto {
  name: string
  idCard: string
  bankNo: string
  bankType: string
  mobile: string
}

// 查询绑定信息
export function getBindInfo() {
  return request.get<UserBind>('/api/core/userBind/getBindInfo')
}

// 存管开户绑定（返回 HTML 表单）
export function bind(data: BindDto) {
  return request.post<string>('/api/core/userBind/auth/bind', data)
}
