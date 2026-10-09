// 借款记录管理 API（对接后端 /admin/core/borrow/record/**）
import { request } from '@/utils/request'
import type { BorrowRecord } from './mock'

export interface BorrowRecordQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string // 借款人姓名
  status?: number // 0-待审核 1-通过 2-还款中 3-已结清 4-已拒绝
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取借款记录列表（分页 + 搜索）
// 完整路径：GET /admin/core/borrow/record/page
export function getBorrowRecordList(params: BorrowRecordQuery = {}): Promise<PageResult<BorrowRecord>> {
  return request({
    url: '/borrow/record/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      keyword: params.keyword || undefined,
      status: params.status ?? undefined,
    },
  })
}

// 审核借款申请（通过/拒绝）
// 完整路径：PUT /admin/core/borrow/record/{id}/audit
export function auditBorrowRecord(id: number, status: number, rejectReason?: string): Promise<null> {
  return request({
    url: `/borrow/record/${id}/audit`,
    method: 'put',
    data: { status, rejectReason },
  })
}