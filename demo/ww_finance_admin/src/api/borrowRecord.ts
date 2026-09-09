// 借款记录管理 API
import { mockBorrowRecords, type BorrowRecord } from './mock'

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
// TODO: 替换为 → request({ url: '/borrow/record/page', method: 'get', params })
//       完整路径：GET /admin/core/borrow/record/page
export function getBorrowRecordList(params: BorrowRecordQuery = {}): Promise<PageResult<BorrowRecord>> {
  const pageNum = params.pageNum ?? 1
  const pageSize = params.pageSize ?? 10
  const keyword = params.keyword?.trim()
  const status = params.status

  let list = [...mockBorrowRecords]
  if (keyword) {
    list = list.filter((r) => r.borrowerName.includes(keyword))
  }
  if (status !== undefined && status !== null) {
    list = list.filter((r) => r.status === status)
  }

  const total = list.length
  const start = (pageNum - 1) * pageSize
  list = list.slice(start, start + pageSize)
  return Promise.resolve({ list, total })
}

// 审核借款申请（通过/拒绝）
// TODO: 替换为 → request({ url: `/borrow/record/${id}/audit`, method: 'put', data: { status, rejectReason } })
//       完整路径：PUT /admin/core/borrow/record/{id}/audit
export function auditBorrowRecord(id: number, status: number, rejectReason?: string): Promise<null> {
  const idx = mockBorrowRecords.findIndex((r) => r.id === id)
  if (idx !== -1) {
    mockBorrowRecords[idx].status = status
    mockBorrowRecords[idx].auditTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    // 审核通过 → 还款中，设定应还清时间
    if (status === 1) {
      const d = new Date()
      d.setDate(d.getDate() + mockBorrowRecords[idx].term)
      mockBorrowRecords[idx].repayEndTime = d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
      mockBorrowRecords[idx].status = 2 // 直接进入还款中
    }
    // 拒绝时记录原因
    if (status === 4 && rejectReason) {
      mockBorrowRecords[idx].rejectReason = rejectReason
    }
  }
  return Promise.resolve(null)
}
