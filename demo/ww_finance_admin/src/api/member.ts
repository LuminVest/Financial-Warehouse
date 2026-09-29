// 会员管理 API（对接后端 /admin/core/member/**）
import { request } from '@/utils/request'
import type { Member } from './mock'

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
// 完整路径：GET /admin/core/member/page
export function getMemberList(params: MemberQuery = {}): Promise<PageResult<Member>> {
  return request({
    url: '/member/page',
    method: 'get',
    params: {
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
      keyword: params.keyword?.trim() || undefined,
      status: params.status ?? undefined,
    },
  })
}

// 新增会员
// 完整路径：POST /admin/core/member
export function addMember(data: Omit<Member, 'id' | 'registerTime' | 'lastLoginTime'>): Promise<null> {
  return request({
    url: '/member',
    method: 'post',
    data,
  })
}

// 修改会员状态（启用/禁用）
// 完整路径：PUT /admin/core/member/{id}/status
export function updateMemberStatus(id: number, status: number): Promise<null> {
  return request({
    url: `/member/${id}/status`,
    method: 'put',
    data: { status },
  })
}

// 登录日志
export interface LoginRecord {
  id: number
  userId: number
  mobile: string
  nickName: string
  ip: string
  createTime: string
}

export interface LoginRecordPage {
  records: LoginRecord[]
  total: number
}

// 获取登录日志（分页 + 手机号搜索）
// 完整路径：POST /admin/core/user/loginRecord/{page}/{limit}
export function getLoginRecord(
  pageNum: number,
  pageSize: number,
  mobile?: string,
): Promise<LoginRecordPage> {
  return request({
    url: `/user/loginRecord/${pageNum}/${pageSize}`,
    method: 'post',
    data: mobile && mobile.trim() ? { mobile: mobile.trim() } : {},
  })
}
