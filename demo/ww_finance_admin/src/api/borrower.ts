// 借款人管理 API（对接后端 /admin/core/borrower/**）
import { request } from '@/utils/request'
import type { Borrower } from './mock'

export interface BorrowerQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string // 姓名/手机号/身份证
  auditStatus?: number // 0-待审核 1-通过 2-拒绝
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取借款人列表（分页 + 搜索）
// 完整路径：GET /admin/core/borrower/page
export function getBorrowerList(params: BorrowerQuery = {}): Promise<PageResult<Borrower>> {
  return request({
    url: '/borrower/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      keyword: params.keyword || undefined,
      auditStatus: params.auditStatus ?? undefined,
    },
  })
}

// 审核借款人（通过/拒绝，逐项积分判定）
// 完整路径：PUT /admin/core/borrower/{id}/audit
// 返回 data.score：审批通过时本次回写的积分
export interface AuditBorrowerBody {
  auditStatus: number // 1-通过 2-拒绝
  idCardOk?: number // 身份证是否正确 1/0
  carOk?: number // 车辆是否正确 1/0
  houseOk?: number // 房产是否正确 1/0
  remark?: string
}

export function auditBorrower(
  id: number,
  body: AuditBorrowerBody,
): Promise<{ data?: { score?: number } }> {
  return request({
    url: `/borrower/${id}/audit`,
    method: 'put',
    data: body,
  })
}