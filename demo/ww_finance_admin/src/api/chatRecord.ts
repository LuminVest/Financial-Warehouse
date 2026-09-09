// 智能客服咨询记录 API
import { mockChatSessions, type ChatSession } from './mock'

export interface ChatSessionQuery {
  keyword?: string // 用户名/手机号/标题
  status?: number // 0-进行中 1-已结束
}

// 获取咨询记录列表
// TODO: 替换为 → request({ url: '/chat/session/list', method: 'get', params })
//       完整路径：GET /admin/core/chat/session/list
export function getChatSessionList(params: ChatSessionQuery = {}): Promise<ChatSession[]> {
  let list = [...mockChatSessions]
  const keyword = params.keyword?.trim()
  const status = params.status

  if (keyword) {
    list = list.filter(
      (s) =>
        s.userName.includes(keyword) ||
        s.userPhone.includes(keyword) ||
        s.title.includes(keyword),
    )
  }
  if (status !== undefined && status !== null) {
    list = list.filter((s) => s.status === status)
  }
  // 按更新时间倒序
  list.sort((a, b) => b.updateTime.localeCompare(a.updateTime))
  return Promise.resolve(list)
}

// 获取会话详情（含完整对话消息）
// TODO: 替换为 → request({ url: `/chat/session/${id}`, method: 'get' })
//       完整路径：GET /admin/core/chat/session/{id}
export function getChatSessionDetail(id: number): Promise<ChatSession | null> {
  const session = mockChatSessions.find((s) => s.id === id)
  return Promise.resolve(session || null)
}

// 结束会话
// TODO: 替换为 → request({ url: `/chat/session/${id}/close`, method: 'put' })
//       完整路径：PUT /admin/core/chat/session/{id}/close
export function closeChatSession(id: number): Promise<null> {
  const session = mockChatSessions.find((s) => s.id === id)
  if (session) {
    session.status = 1
    session.updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return Promise.resolve(null)
}

// 删除会话
// TODO: 替换为 → request({ url: `/chat/session/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/chat/session/{id}
export function deleteChatSession(id: number): Promise<null> {
  const idx = mockChatSessions.findIndex((s) => s.id === id)
  if (idx !== -1) mockChatSessions.splice(idx, 1)
  return Promise.resolve(null)
}
