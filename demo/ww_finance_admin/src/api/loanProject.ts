// 标的管理 API（对接后端 /admin/core/loan/project/**）
import { request } from '@/utils/request'
import type { LoanProject, Investment } from './mock'

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
// 完整路径：GET /admin/core/loan/project/page
export function getLoanProjectList(params: LoanProjectQuery = {}): Promise<PageResult<LoanProject>> {
  return request({
    url: '/loan/project/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      keyword: params.keyword?.trim() || undefined,
      status: params.status ?? undefined,
    },
  })
}

// 发布标的（测试功能，正常由借款人发布）
// 完整路径：POST /admin/core/loan/project
export function publishLoanProject(data: Omit<LoanProject, 'id' | 'raisedAmount' | 'progress' | 'status' | 'publishTime' | 'endTime' | 'createTime' | 'remark'>): Promise<null> {
  return request({
    url: '/loan/project',
    method: 'post',
    data,
  })
}

// 下架标的
// 完整路径：PUT /admin/core/loan/project/{id}/offline
export function offlineLoanProject(id: number): Promise<null> {
  return request({
    url: `/loan/project/${id}/offline`,
    method: 'put',
  })
}

// 获取标的的投资列表
// 完整路径：GET /admin/core/loan/project/{projectId}/investments
export function getInvestmentList(projectId: number): Promise<Investment[]> {
  return request({
    url: `/loan/project/${projectId}/investments`,
    method: 'get',
  })
}
