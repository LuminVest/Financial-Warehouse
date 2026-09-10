// 积分等级管理 API（对接后端 /admin/core/point/level/**）
import { request } from '@/utils/request'
import type { PointLevel } from './mock'

export interface PointLevelQuery {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取积分等级列表（分页）
// 完整路径：GET /admin/core/point/level/page
export function getPointLevelList(params: PointLevelQuery = {}): Promise<PageResult<PointLevel>> {
  return request({
    url: '/point/level/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
    },
  })
}

// 新增积分等级
// 完整路径：POST /admin/core/point/level
export function addPointLevel(data: Omit<PointLevel, 'id' | 'createTime'>): Promise<null> {
  return request({
    url: '/point/level',
    method: 'post',
    data,
  })
}

// 修改积分等级
// 完整路径：PUT /admin/core/point/level
export function updatePointLevel(data: PointLevel): Promise<null> {
  return request({
    url: '/point/level',
    method: 'put',
    data,
  })
}

// 删除积分等级
// 完整路径：DELETE /admin/core/point/level/{id}
export function deletePointLevel(id: number): Promise<null> {
  return request({
    url: `/point/level/${id}`,
    method: 'delete',
  })
}
