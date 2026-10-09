import request from '@/utils/request'

// 账户 API（充值/提现/余额）

export interface UserAccount {
  id: number
  userId: number
  amount: number // 可用余额
  freezeAmount: number // 冻结金额
  version?: number
}

// 查询账户余额
export function getAccount() {
  return request.get<number | UserAccount>('/api/core/userAccount/auth/getAccount')
}

// 充值（返回 HTML 表单，需浏览器打开自动提交）
export function commitCharge(chargeAmt: number) {
  return request.get<string>(`/api/core/userAccount/auth/commitCharge/${chargeAmt}`)
}

// 提现（返回 HTML 表单）
export function commitWithdraw(withdrawAmt: number) {
  return request.get<string>(`/api/core/userAccount/auth/commitWithdraw/${withdrawAmt}`)
}

// 流水分页（MyBatis-Plus 结构：records/total/current/pages）
export function getTransFlowPage(pageNum: number, pageSize: number) {
  return request.get<{ records: TransFlow[]; total: number; pages?: number }>(
    '/api/user/center/transFlow/page',
    { params: { pageNum, pageSize } },
  )
}

export interface TransFlow {
  id: number
  userId: number
  transType: number // 1充值 2提现 3投标 4投资回款 5放款 6还款
  transNo: string
  transAmount: number
  memo: string
  createTime: string
}
