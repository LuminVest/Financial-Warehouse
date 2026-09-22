// 模型配置管理 API
import request from '@/utils/request'

export interface ModelConfig {
  id?: number
  name: string
  modelName: string
  baseUrl: string
  apiKey: string
  embeddingModel: string
  maxTokens: number
  isDefault: number
  status: number
  remark: string
}

export interface ModelQuery {
  keyword?: string
}

// 获取模型列表
export function getModelConfigList(params: ModelQuery = {}) {
  return request({
    url: '/chat/model/list',
    method: 'get',
    params,
  })
}

// 新增模型
export function addModelConfig(data: ModelConfig) {
  return request({
    url: '/chat/model',
    method: 'post',
    data,
  })
}

// 修改模型
export function updateModelConfig(data: ModelConfig) {
  return request({
    url: `/chat/model/${data.id}`,
    method: 'put',
    data,
  })
}

// 删除模型
export function deleteModelConfig(id: number) {
  return request({
    url: `/chat/model/${id}`,
    method: 'delete',
  })
}

// 设置默认模型
export function setDefaultModel(id: number) {
  return request({
    url: `/chat/model/${id}/default`,
    method: 'put',
  })
}
