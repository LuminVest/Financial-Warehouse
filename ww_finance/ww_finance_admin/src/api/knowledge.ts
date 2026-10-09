// RAG 知识库管理 API
import { type KnowledgeBase, type KnowledgeDoc } from './mock'
import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
}

// ===== 知识库 CRUD（真实后端） =====

// 获取知识库列表
// GET /admin/core/knowledge/base/page
export function getKnowledgeBaseList(): Promise<KnowledgeBase[]> {
  return request<KnowledgeBase[]>({ url: '/knowledge/base/page', method: 'get' })
}

// 新增知识库
// POST /admin/core/knowledge/base
export function addKnowledgeBase(data: { name: string; description: string; embeddingModel: string }): Promise<null> {
  return request<null>({ url: '/knowledge/base', method: 'post', data })
}

// 修改知识库
// PUT /admin/core/knowledge/base
export function updateKnowledgeBase(data: KnowledgeBase): Promise<null> {
  return request<null>({ url: '/knowledge/base', method: 'put', data })
}

// 删除知识库（同时删除其下所有文档）
// DELETE /admin/core/knowledge/base/{id}
export function deleteKnowledgeBase(id: number): Promise<null> {
  return request<null>({ url: `/knowledge/base/${id}`, method: 'delete' })
}

// ===== 知识文档 CRUD（真实后端） =====

// 获取某个知识库下的文档列表
// GET /admin/core/knowledge/doc/page?kbId={kbId}
export function getKnowledgeDocList(kbId: number): Promise<KnowledgeDoc[]> {
  return request<KnowledgeDoc[]>({ url: '/knowledge/doc/page', method: 'get', params: { kbId } })
}

// 添加文档（文本类型）
// POST /admin/core/knowledge/doc/text
export function addKnowledgeDocText(data: { kbId: number; title: string; content: string }): Promise<null> {
  return request<null>({ url: '/knowledge/doc/text', method: 'post', data })
}

// 添加文档（PDF 类型，先存元数据；文件解析/向量化由 AI 服务后续完成）
// POST /admin/core/knowledge/doc/pdf
export function addKnowledgeDocPdf(data: { kbId: number; title: string; fileName: string; fileSize: number }): Promise<number> {
  return request<number>({ url: '/knowledge/doc/pdf', method: 'post', data })
}

// 删除文档
// DELETE /admin/core/knowledge/doc/{id}
export function deleteKnowledgeDoc(id: number): Promise<null> {
  return request<null>({ url: `/knowledge/doc/${id}`, method: 'delete' })
}
