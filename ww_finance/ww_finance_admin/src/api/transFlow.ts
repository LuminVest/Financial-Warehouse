// 资金流水监控 API（对接后端 /admin/core/transFlow/**）
import { request } from '@/utils/request'

export interface TransFlow {
  id: number
  userId: number
  userName: string
  transNo: string
  transType: number
  transTypeName: string
  transAmount: number
  memo: string
  createTime: string
}

export interface TransFlowQuery {
  pageNum?: number
  pageSize?: number
  userName?: string // 用户名模糊搜索
  transType?: number // 1充值 2提现 3投标 4投资回款 5放款 6还款
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取资金流水分页列表
// 完整路径：GET /admin/core/transFlow/page
export function getTransFlowList(params: TransFlowQuery = {}): Promise<PageResult<TransFlow>> {
  return request({
    url: '/transFlow/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      userName: params.userName?.trim() || undefined,
      transType: params.transType ?? undefined,
    },
  })
}
