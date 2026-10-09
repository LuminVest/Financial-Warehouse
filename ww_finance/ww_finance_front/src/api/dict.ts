import request from '@/utils/request'

// 数据字典 API

export interface DictItem {
  id: number
  parentId: number
  name: string
  value: number
}

// 按分类编码查询字典项列表（如 industry / education / income）
export function listByDictCode(dictCode: string) {
  return request.get<DictItem[]>(`/api/core/dict/listByDictCode/${dictCode}`)
}
