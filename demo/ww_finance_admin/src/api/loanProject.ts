// 标的管理 API
import { mockLoanProjects, mockInvestments, type LoanProject, type Investment } from './mock'

export interface LoanProjectQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string // 标的名称/借款人
  status?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取标的列表（分页 + 搜索）
// TODO: 替换为 → request({ url: '/loan/project/page', method: 'get', params })
//       完整路径：GET /admin/core/loan/project/page
export function getLoanProjectList(params: LoanProjectQuery = {}): Promise<PageResult<LoanProject>> {
  const pageNum = params.pageNum ?? 1
  const pageSize = params.pageSize ?? 10
  const keyword = params.keyword?.trim()
  const status = params.status

  let list = [...mockLoanProjects]
  if (keyword) {
    list = list.filter(
      (p) => p.title.includes(keyword) || p.borrowerName.includes(keyword),
    )
  }
  if (status !== undefined && status !== null) {
    list = list.filter((p) => p.status === status)
  }

  const total = list.length
  const start = (pageNum - 1) * pageSize
  list = list.slice(start, start + pageSize)
  return Promise.resolve({ list, total })
}

// 发布标的（测试功能，正常由借款人发布）
// TODO: 替换为 → request({ url: '/loan/project', method: 'post', data })
//       完整路径：POST /admin/core/loan/project
export function publishLoanProject(data: Omit<LoanProject, 'id' | 'raisedAmount' | 'progress' | 'status' | 'publishTime' | 'endTime' | 'createTime' | 'remark'>): Promise<null> {
  const newId = Math.max(...mockLoanProjects.map((p) => p.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  const end = new Date()
  end.setDate(end.getDate() + 10) // 默认募集期10天
  mockLoanProjects.push({
    ...data,
    id: newId,
    raisedAmount: 0,
    progress: 0,
    status: 1, // 发布后直接进入募资中
    publishTime: now,
    endTime: end.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
    createTime: now,
    remark: '',
  })
  return Promise.resolve(null)
}

// 下架标的
// TODO: 替换为 → request({ url: `/loan/project/${id}/offline`, method: 'put' })
//       完整路径：PUT /admin/core/loan/project/{id}/offline
export function offlineLoanProject(id: number): Promise<null> {
  const idx = mockLoanProjects.findIndex((p) => p.id === id)
  if (idx !== -1) {
    mockLoanProjects[idx].status = 4
  }
  return Promise.resolve(null)
}

// 获取标的的投资列表
// TODO: 替换为 → request({ url: `/loan/project/${projectId}/investments`, method: 'get' })
//       完整路径：GET /admin/core/loan/project/{projectId}/investments
export function getInvestmentList(projectId: number): Promise<Investment[]> {
  return Promise.resolve(mockInvestments.filter((i) => i.projectId === projectId))
}
