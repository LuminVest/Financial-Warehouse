// 智能客服对话模型配置 API
import { mockChatModelBindings, mockModelConfigs, mockPromptConfigs, mockKnowledgeBases, type ChatModelBinding } from './mock'

// 获取对话模型绑定列表
// TODO: 替换为 → request({ url: '/chat/model/list', method: 'get' })
//       完整路径：GET /admin/core/chat/model/list
export function getChatModelBindings(): Promise<ChatModelBinding[]> {
  return Promise.resolve([...mockChatModelBindings])
}

// 更新绑定配置（模型/Prompt/知识库/启用状态）
// TODO: 替换为 → request({ url: '/chat/model', method: 'put', data })
//       完整路径：PUT /admin/core/chat/model
export function updateChatModelBinding(data: ChatModelBinding): Promise<null> {
  const idx = mockChatModelBindings.findIndex((b) => b.id === data.id)
  if (idx !== -1) {
    // 同步冗余字段
    const model = mockModelConfigs.find((m) => m.id === data.modelId)
    if (model) data.modelName = model.name
    const prompt = mockPromptConfigs.find((p) => p.id === data.promptId)
    if (prompt) data.promptKey = prompt.promptKey
    const kb = data.kbId ? mockKnowledgeBases.find((k) => k.id === data.kbId) : null
    data.kbName = kb ? kb.name : '—'
    mockChatModelBindings[idx] = { ...mockChatModelBindings[idx], ...data }
    mockChatModelBindings[idx].updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return Promise.resolve(null)
}

// 获取可选项（模型列表 + Prompt列表 + 知识库列表）
export function getSelectOptions() {
  return {
    models: mockModelConfigs
      .filter((m) => m.modelType !== 'rerank' && m.status === 1)
      .map((m) => ({ id: m.id, name: m.name, modelName: m.modelName })),
    prompts: mockPromptConfigs
      .filter((p) => p.status === 1)
      .map((p) => ({ id: p.id, promptKey: p.promptKey, name: p.name })),
    knowledgeBases: [{ id: 0, name: '不检索' }, ...mockKnowledgeBases.map((k) => ({ id: k.id, name: k.name }))],
  }
}
