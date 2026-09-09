// 模型配置管理 API
import { mockModelConfigs, type ModelConfig } from './mock'

export interface ModelQuery {
  keyword?: string
  modelType?: string
  provider?: string
}

// 获取模型列表
// TODO: 替换为 → request({ url: '/model/config/list', method: 'get', params })
//       完整路径：GET /admin/core/model/config/list
export function getModelConfigList(params: ModelQuery = {}): Promise<ModelConfig[]> {
  let list = [...mockModelConfigs]
  const keyword = params.keyword?.trim()
  const modelType = params.modelType
  const provider = params.provider

  if (keyword) {
    list = list.filter(
      (m) => m.name.includes(keyword) || m.modelName.includes(keyword),
    )
  }
  if (modelType) {
    list = list.filter((m) => m.modelType === modelType)
  }
  if (provider) {
    list = list.filter((m) => m.provider === provider)
  }
  return Promise.resolve(list)
}

// 新增模型
// TODO: 替换为 → request({ url: '/model/config', method: 'post', data })
//       完整路径：POST /admin/core/model/config
export function addModelConfig(data: Omit<ModelConfig, 'id' | 'createTime' | 'updateTime'>): Promise<null> {
  const newId = Math.max(...mockModelConfigs.map((m) => m.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  // 如果设为默认，取消同类型其他默认
  if (data.isDefault === 1) {
    mockModelConfigs.forEach((m) => {
      if (m.modelType === data.modelType) m.isDefault = 0
    })
  }
  mockModelConfigs.push({
    ...data,
    id: newId,
    createTime: now,
    updateTime: now,
  })
  return Promise.resolve(null)
}

// 修改模型
// TODO: 替换为 → request({ url: '/model/config', method: 'put', data })
//       完整路径：PUT /admin/core/model/config
export function updateModelConfig(data: ModelConfig): Promise<null> {
  const idx = mockModelConfigs.findIndex((m) => m.id === data.id)
  if (idx !== -1) {
    // 如果设为默认，取消同类型其他默认
    if (data.isDefault === 1) {
      mockModelConfigs.forEach((m) => {
        if (m.modelType === data.modelType && m.id !== data.id) m.isDefault = 0
      })
    }
    mockModelConfigs[idx] = { ...mockModelConfigs[idx], ...data }
    mockModelConfigs[idx].updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return Promise.resolve(null)
}

// 删除模型
// TODO: 替换为 → request({ url: `/model/config/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/model/config/{id}
export function deleteModelConfig(id: number): Promise<null> {
  const idx = mockModelConfigs.findIndex((m) => m.id === id)
  if (idx !== -1) mockModelConfigs.splice(idx, 1)
  return Promise.resolve(null)
}

// 测试模型连通性
// TODO: 替换为 → request({ url: '/model/config/test', method: 'post', data: { id } })
//       完整路径：POST /admin/core/model/config/test
export function testModelConfig(id: number): Promise<{ success: boolean; message: string; latency: number }> {
  const model = mockModelConfigs.find((m) => m.id === id)
  if (!model) return Promise.resolve({ success: false, message: '模型不存在', latency: 0 })
  // 模拟测试
  const latency = Math.floor(Math.random() * 500) + 100
  if (model.status === 0) {
    return Promise.resolve({ success: false, message: '模型未启用', latency })
  }
  return Promise.resolve({ success: true, message: '连接成功', latency })
}
