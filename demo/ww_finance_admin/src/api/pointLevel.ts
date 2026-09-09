// 积分等级管理 API
// 当前使用本地 Mock 数据运行，后端接口就绪后打开 TODO 注释的 request 调用即可
import { mockPointLevels, type PointLevel } from './mock'

export interface PointLevelQuery {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取积分等级列表
// TODO: 替换为 → request({ url: '/point/level/page', method: 'get', params })
//       完整路径：GET /admin/core/point/level/page
export function getPointLevelList(params: PointLevelQuery = {}): Promise<PageResult<PointLevel>> {
  const pageNum = params.pageNum ?? 1
  const pageSize = params.pageSize ?? 10
  const start = (pageNum - 1) * pageSize
  const list = mockPointLevels.slice(start, start + pageSize)
  return Promise.resolve({ list, total: mockPointLevels.length })
}

// 新增积分等级
// TODO: 替换为 → request({ url: '/point/level', method: 'post', data })
//       完整路径：POST /admin/core/point/level
export function addPointLevel(data: Omit<PointLevel, 'id' | 'createTime'>): Promise<null> {
  const newId = Math.max(...mockPointLevels.map((l) => l.id), 0) + 1
  mockPointLevels.push({
    ...data,
    id: newId,
    createTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
  })
  return Promise.resolve(null)
}

// 修改积分等级
// TODO: 替换为 → request({ url: '/point/level', method: 'put', data })
//       完整路径：PUT /admin/core/point/level
export function updatePointLevel(data: PointLevel): Promise<null> {
  const idx = mockPointLevels.findIndex((l) => l.id === data.id)
  if (idx !== -1) {
    mockPointLevels[idx] = { ...mockPointLevels[idx], ...data }
  }
  return Promise.resolve(null)
}

// 删除积分等级
// TODO: 替换为 → request({ url: `/point/level/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/point/level/{id}
export function deletePointLevel(id: number): Promise<null> {
  const idx = mockPointLevels.findIndex((l) => l.id === id)
  if (idx !== -1) {
    mockPointLevels.splice(idx, 1)
  }
  return Promise.resolve(null)
}
