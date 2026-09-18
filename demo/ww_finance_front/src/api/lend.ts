import request from '@/utils/request'

// 标的 API

export interface Lend {
  id: number
  lendNo: string
  title: string
  amount: number // 标的金额（元）
  period: number // 期限（月）
  lendYearRate: number // 年化利率（小数，如 0.08）
  returnMethod: number // 还款方式 1等额本息 2等额本金 3按月付息到期还本
  investAmount: number // 已投金额
  investNum: number // 投标人数
  status: number // 0待发布 1募集中 2满标 3还款中 4已下架
  lendInfo: string // 借款用途
  publishDate: string
}

export interface LendItem {
  id: number
  lendId: number
  investUserId: number
  investName?: string
  investAmount: number
  investTime: string
}

// 标的列表（募集中/满标/还款中）
export function getLendList() {
  return request.get<Lend[]>('/api/core/lend/list')
}

// 标的详情
export function getLendDetail(id: number) {
  return request.get<Lend>(`/api/core/lend/show/${id}`)
}

// 推荐标的（登录）
export function getRecommend() {
  return request.get<Lend[]>('/api/core/lend/auth/recommend')
}

// 收益计算（返回总利息）——后端为路径参数：/getInterestCount/{invest}/{yearRate}/{totalmonth}/{returnMethod}
export function getInterestCount(invest: number, yearRate: number, totalmonth: number, returnMethod: number) {
  return request.get<number>(`/api/core/lend/getInterestCount/${invest}/${yearRate}/${totalmonth}/${returnMethod}`)
}

// 某标的的投资记录
export function getLendItemList(lendId: number) {
  return request.get<LendItem[]>(`/api/core/lendItem/list/${lendId}`)
}

// 投标（返回 HTML 表单自动提交到银行）
export function commitInvest(lendId: number, investAmount: number) {
  return request.post<string>('/api/core/lendItem/auth/commitInvest', {
    lendId,
    investAmount,
  })
}

// 还款计划（按标的查）
export function getLendReturnList(lendId: number) {
  return request.get<LendReturn[]>(`/api/core/lendReturn/list/${lendId}`)
}

export interface LendReturn {
  id: number
  lendId: number
  borrowInfoId: number
  returnNo: string
  userId: number
  amount: number
  currentPeriod: number
  principal: number
  interest: number
  total: number
  returnDate: string
  realReturnTime: string | null
  isOverdue: boolean
  overdueTotal: number
  isLast: boolean
  status: number // 0未还款 1已还款
}

// 我的还款计划：我的借款标的 → 还款计划
export function getMyLendReturn() {
  return request.get<Array<{ lend: Lend; returns: LendReturn[] }>>('/api/user/center/myLendReturn')
}

// 发起还款（返回 HTML 表单自动提交到银行）
export function commitRepayment(lendId: number, currentPeriod: number) {
  return request.post<string>('/api/core/lendReturn/auth/commitRepayment', {
    lendId,
    currentPeriod,
  })
}
