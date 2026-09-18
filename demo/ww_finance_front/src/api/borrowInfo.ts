import request from '@/utils/request'

// 借款申请 API

export interface BorrowInfoDto {
  amount: number // 借款金额
  period: number // 期限（月）
  borrowYearRate: number // 年化利率（小数，如 0.08）
  returnMethod: number // 还款方式 1等额本息 2等额本金 3按月付息到期还本
  moneyUse: number // 借款用途字典值
}

export interface BorrowInfo {
  id: number
  userId: number
  amount: number
  period: number
  borrowYearRate: number
  returnMethod: number
  moneyUse: number
  status: number // 0未提交 1审核中 2通过 -1不通过
  createTime: string
}

// 可借额度
export function getBorrowAmount() {
  return request.get<number>('/api/core/borrowInfo/auth/getBorrowAmount')
}

// 借款申请状态 0未提交 1审核中 2通过 -1失败
export function getBorrowInfoStatus() {
  return request.get<number>('/api/core/borrowInfo/auth/getBorrowInfoStatus')
}

// 提交借款申请
export function saveBorrowInfo(data: BorrowInfoDto) {
  return request.post('/api/core/borrowInfo/auth/save', data)
}

// 我的借款记录：借款申请 + 关联标的
export function getMyBorrowInfo() {
  return request.get<Array<{ borrowInfo: BorrowInfo; lend: Record<string, unknown> | null }>>(
    '/api/user/center/myBorrowInfo',
  )
}
