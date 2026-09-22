// 智能客服对话模型配置 API
import request from '@/utils/request'

// 获取对话模型绑定列表
export function getChatModelBindings() {
  return request({
    url: '/chat/model/list',
    method: 'get',
  })
}

// 更新绑定配置（模型/Prompt/知识库/启用状态）
export function updateChatModelBinding(data: any) {
  return request({
    url: `/chat/model/${data.id}`,
    method: 'put',
    data,
  })
}

// 获取可选项（模型列表 + Prompt列表 + 知识库列表）
export async function getSelectOptions() {
  const [models, prompts] = await Promise.all([
    request({ url: '/chat/model/list', method: 'get' }),
    request({ url: '/chat/prompt/list', method: 'get' }),
  ])
  return {
    models: (models || []).map((m: any) => ({ id: m.id, name: m.name, modelName: m.modelName })),
    prompts: (prompts || []).map((p: any) => ({ id: p.id, promptKey: p.name, name: p.name })),
    knowledgeBases: [{ id: 0, name: '不检索' }],
  }
}
