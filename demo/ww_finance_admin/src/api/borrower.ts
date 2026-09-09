// 借款人管理 API
import { mockBorrowers, type Borrower } from './mock'

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
// TODO: 替换为 → request({ url: '/borrower/page', method: 'get', params })
//       完整路径：GET /admin/core/borrower/page

// // 获取借款人列表（分页 + 搜索）
// export function getBorrowerList(params: BorrowerQuery = {}): Promise<PageResult<Borrower>> {
//   return request({
//     url: '/borrower/page',
//     method: 'get',
//     params: {
//       pageNum: params.pageNum ?? 1,
//       pageSize: params.pageSize ?? 10,
//       keyword: params.keyword || undefined,
//       auditStatus: params.auditStatus ?? undefined,
//     },
//   })
// }

export function getBorrowerList(params: BorrowerQuery = {}): Promise<PageResult<Borrower>> {
  const pageNum = params.pageNum ?? 1
  const pageSize = params.pageSize ?? 10
  const keyword = params.keyword?.trim()
  const auditStatus = params.auditStatus

  let list = [...mockBorrowers]
  if (keyword) {
    list = list.filter(
      (b) =>
        b.realName.includes(keyword) ||
        b.phone.includes(keyword) ||
        b.idCard.includes(keyword),
    )
  }
  if (auditStatus !== undefined && auditStatus !== null) {
    list = list.filter((b) => b.auditStatus === auditStatus)
  }

  const total = list.length
  const start = (pageNum - 1) * pageSize
  list = list.slice(start, start + pageSize)
  return Promise.resolve({ list, total })
}

// 审核借款人（通过/拒绝）
// TODO: 替换为 → request({ url: `/borrower/${id}/audit`, method: 'put', data: { auditStatus } })
//       完整路径：PUT /admin/core/borrower/{id}/audit
export function auditBorrower(id: number, auditStatus: number, remark?: string): Promise<null> {
  const idx = mockBorrowers.findIndex((b) => b.id === id)
  if (idx !== -1) {
    mockBorrowers[idx].auditStatus = auditStatus
    mockBorrowers[idx].auditTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    if (remark !== undefined) {
      mockBorrowers[idx].remark = remark
    }
    // 审核通过时设置授信额度
    if (auditStatus === 1 && mockBorrowers[idx].creditLimit === 0) {
      mockBorrowers[idx].creditLimit = Math.floor(mockBorrowers[idx].monthlyIncome * 5)
    }
  }
  return Promise.resolve(null)
}
