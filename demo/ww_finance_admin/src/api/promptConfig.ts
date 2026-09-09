// Prompt 指令配置 API
import { mockPromptConfigs, type PromptConfig } from './mock'

export interface PromptQuery {
  keyword?: string // 名称或 promptKey
  scene?: string
  contentType?: string // prompt / keywords
}

// 获取 Prompt 列表
// TODO: 替换为 → request({ url: '/chat/prompt/list', method: 'get', params })
//       完整路径：GET /admin/core/chat/prompt/list
export function getPromptList(params: PromptQuery = {}): Promise<PromptConfig[]> {
  let list = [...mockPromptConfigs]
  const keyword = params.keyword?.trim()
  const scene = params.scene
  const contentType = params.contentType

  if (keyword) {
    list = list.filter(
      (p) => p.name.includes(keyword) || p.promptKey.includes(keyword.toUpperCase()),
    )
  }
  if (scene) {
    list = list.filter((p) => p.scene === scene)
  }
  if (contentType) {
    list = list.filter((p) => p.contentType === contentType)
  }
  return Promise.resolve(list)
}

// 新增 Prompt
// TODO: 替换为 → request({ url: '/chat/prompt', method: 'post', data })
//       完整路径：POST /admin/core/chat/prompt
export function addPrompt(data: Omit<PromptConfig, 'id' | 'updateTime'>): Promise<null> {
  // promptKey 唯一性校验
  if (mockPromptConfigs.some((p) => p.promptKey === data.promptKey)) {
    return Promise.reject(new Error(`promptKey "${data.promptKey}" 已存在`))
  }
  const newId = Math.max(...mockPromptConfigs.map((p) => p.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  if (data.isDefault === 1) {
    mockPromptConfigs.forEach((p) => {
      if (p.scene === data.scene) p.isDefault = 0
    })
  }
  mockPromptConfigs.push({ ...data, id: newId, updateTime: now })
  return Promise.resolve(null)
}

// 修改 Prompt
// TODO: 替换为 → request({ url: '/chat/prompt', method: 'put', data })
//       完整路径：PUT /admin/core/chat/prompt
export function updatePrompt(data: PromptConfig): Promise<null> {
  const idx = mockPromptConfigs.findIndex((p) => p.id === data.id)
  if (idx !== -1) {
    if (data.isDefault === 1) {
      mockPromptConfigs.forEach((p) => {
        if (p.scene === data.scene && p.id !== data.id) p.isDefault = 0
      })
    }
    mockPromptConfigs[idx] = { ...mockPromptConfigs[idx], ...data }
    mockPromptConfigs[idx].updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return Promise.resolve(null)
}

// 删除 Prompt
// TODO: 替换为 → request({ url: `/chat/prompt/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/chat/prompt/{id}
export function deletePrompt(id: number): Promise<null> {
  const idx = mockPromptConfigs.findIndex((p) => p.id === id)
  if (idx !== -1) mockPromptConfigs.splice(idx, 1)
  return Promise.resolve(null)
}
