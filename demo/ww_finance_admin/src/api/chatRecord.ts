// 智能客服咨询记录 API
import request from '@/utils/request'
import type { ChatSession } from './mock'

export interface ChatSessionQuery {
  keyword?: string
  status?: number
}

// 获取咨询记录列表
export function getChatSessionList(params: ChatSessionQuery & { page?: number; size?: number }) {
  return request({
    url: '/chat/session/list',
    method: 'get',
    params,
  })
}

// 获取会话详情（含完整对话消息）
export function getChatSessionDetail(id: number) {
  return request({
    url: `/chat/session/${id}`,
    method: 'get',
  })
}

// 结束会话
export function closeChatSession(id: number) {
  return request({
    url: `/chat/session/${id}/close`,
    method: 'put',
  })
}

// 删除会话
export function deleteChatSession(id: number) {
  return request({
    url: `/chat/session/${id}`,
    method: 'delete',
  })
}
