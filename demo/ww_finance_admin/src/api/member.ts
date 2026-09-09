// 会员管理 API
// 当前使用本地 Mock 数据运行，后端接口就绪后打开 TODO 注释的 request 调用即可
import { mockMembers, type Member } from './mock'

export interface MemberQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string // 手机号/昵称/真实姓名 模糊搜索
  status?: number // 1-正常 0-禁用
}

export interface PageResult<T> {
  list: T[]
  total: number
}

// 获取会员列表（分页 + 搜索）
// TODO: 替换为 → request({ url: '/member/page', method: 'get', params })
//       完整路径：GET /admin/core/member/page
export function getMemberList(params: MemberQuery = {}): Promise<PageResult<Member>> {
  const pageNum = params.pageNum ?? 1
  const pageSize = params.pageSize ?? 10
  const keyword = params.keyword?.trim()
  const status = params.status

  let list = [...mockMembers]
  if (keyword) {
    const kw = keyword.toLowerCase()
    list = list.filter(
      (m) =>
        m.phone.includes(keyword) ||
        m.nickname.toLowerCase().includes(kw) ||
        m.realName.toLowerCase().includes(kw),
    )
  }
  if (status !== undefined && status !== null) {
    list = list.filter((m) => m.status === status)
  }

  const total = list.length
  const start = (pageNum - 1) * pageSize
  list = list.slice(start, start + pageSize)
  return Promise.resolve({ list, total })
}

// 新增会员
// TODO: 替换为 → request({ url: '/member', method: 'post', data })
//       完整路径：POST /admin/core/member
export function addMember(data: Omit<Member, 'id' | 'registerTime' | 'lastLoginTime'>): Promise<null> {
  const newId = Math.max(...mockMembers.map((m) => m.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  mockMembers.push({
    ...data,
    id: newId,
    registerTime: now,
    lastLoginTime: '',
  })
  return Promise.resolve(null)
}

// 修改会员状态（启用/禁用）
// TODO: 替换为 → request({ url: `/member/${id}/status`, method: 'put', data: { status } })
//       完整路径：PUT /admin/core/member/{id}/status
export function updateMemberStatus(id: number, status: number): Promise<null> {
  const idx = mockMembers.findIndex((m) => m.id === id)
  if (idx !== -1) {
    mockMembers[idx].status = status
  }
  return Promise.resolve(null)
}
