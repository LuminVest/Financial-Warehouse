// Prompt 指令配置 API
import request from '@/utils/request'

export interface PromptConfig {
  id?: number
  name: string
  systemPrompt: string
  temperature: number
  topP: number
  maxTokens: number
  isDefault: number
  status: number
  remark: string
}

export interface PromptQuery {
  keyword?: string
}

// 获取 Prompt 列表
export function getPromptList(params: PromptQuery = {}) {
  return request({
    url: '/chat/prompt/list',
    method: 'get',
    params,
  })
}

// 新增 Prompt
export function addPrompt(data: PromptConfig) {
  return request({
    url: '/chat/prompt',
    method: 'post',
    data,
  })
}

// 修改 Prompt
export function updatePrompt(data: PromptConfig) {
  return request({
    url: `/chat/prompt/${data.id}`,
    method: 'put',
    data,
  })
}

// 删除 Prompt
export function deletePrompt(id: number) {
  return request({
    url: `/chat/prompt/${id}`,
    method: 'delete',
  })
}

// 设置默认 Prompt
export function setDefaultPrompt(id: number) {
  return request({
    url: `/chat/prompt/${id}/default`,
    method: 'put',
  })
}
