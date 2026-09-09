// RAG 知识库管理 API
import { mockKnowledgeBases, mockKnowledgeDocs, type KnowledgeBase, type KnowledgeDoc } from './mock'

export interface PageResult<T> {
  list: T[]
  total: number
}

// ===== 知识库 CRUD =====

// 获取知识库列表
// TODO: 替换为 → request({ url: '/knowledge/base/page', method: 'get', params })
//       完整路径：GET /admin/core/knowledge/base/page
export function getKnowledgeBaseList(): Promise<KnowledgeBase[]> {
  return Promise.resolve([...mockKnowledgeBases])
}

// 新增知识库
// TODO: 替换为 → request({ url: '/knowledge/base', method: 'post', data })
//       完整路径：POST /admin/core/knowledge/base
export function addKnowledgeBase(data: { name: string; description: string; embeddingModel: string }): Promise<null> {
  const newId = Math.max(...mockKnowledgeBases.map((k) => k.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  mockKnowledgeBases.push({
    ...data,
    id: newId,
    docCount: 0,
    chunkCount: 0,
    status: 1,
    createTime: now,
    updateTime: now,
  })
  return Promise.resolve(null)
}

// 修改知识库
// TODO: 替换为 → request({ url: '/knowledge/base', method: 'put', data })
//       完整路径：PUT /admin/core/knowledge/base
export function updateKnowledgeBase(data: KnowledgeBase): Promise<null> {
  const idx = mockKnowledgeBases.findIndex((k) => k.id === data.id)
  if (idx !== -1) {
    mockKnowledgeBases[idx] = { ...mockKnowledgeBases[idx], ...data }
    mockKnowledgeBases[idx].updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
  return Promise.resolve(null)
}

// 删除知识库（同时删除其下所有文档）
// TODO: 替换为 → request({ url: `/knowledge/base/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/knowledge/base/{id}
export function deleteKnowledgeBase(id: number): Promise<null> {
  const idx = mockKnowledgeBases.findIndex((k) => k.id === id)
  if (idx !== -1) mockKnowledgeBases.splice(idx, 1)
  // 删除关联文档
  for (let i = mockKnowledgeDocs.length - 1; i >= 0; i--) {
    if (mockKnowledgeDocs[i].kbId === id) mockKnowledgeDocs.splice(i, 1)
  }
  return Promise.resolve(null)
}

// ===== 知识文档 CRUD =====

// 获取某个知识库下的文档列表
// TODO: 替换为 → request({ url: `/knowledge/doc/page`, method: 'get', params: { kbId } })
//       完整路径：GET /admin/core/knowledge/doc/page?kbId={kbId}
export function getKnowledgeDocList(kbId: number): Promise<KnowledgeDoc[]> {
  return Promise.resolve(mockKnowledgeDocs.filter((d) => d.kbId === kbId))
}

// 添加文档（文本类型）
// TODO: 替换为 → request({ url: '/knowledge/doc/text', method: 'post', data })
//       完整路径：POST /admin/core/knowledge/doc/text
export function addKnowledgeDocText(data: { kbId: number; title: string; content: string }): Promise<null> {
  const newId = Math.max(...mockKnowledgeDocs.map((d) => d.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  mockKnowledgeDocs.push({
    id: newId,
    kbId: data.kbId,
    title: data.title,
    source: 'text',
    content: data.content,
    fileName: '',
    fileSize: 0,
    chunkCount: Math.ceil(data.content.length / 500),
    status: 1,
    createTime: now,
  })
  // 更新知识库文档数
  const kb = mockKnowledgeBases.find((k) => k.id === data.kbId)
  if (kb) {
    kb.docCount++
    kb.chunkCount += Math.ceil(data.content.length / 500)
    kb.updateTime = now
  }
  return Promise.resolve(null)
}

// 添加文档（PDF 类型，模拟上传）
// TODO: 替换为 → const formData = new FormData(); formData.append('file', file); formData.append('kbId', kbId)
//       request({ url: '/knowledge/doc/upload', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
//       完整路径：POST /admin/core/knowledge/doc/upload
export function addKnowledgeDocPdf(data: { kbId: number; title: string; fileName: string; fileSize: number }): Promise<null> {
  const newId = Math.max(...mockKnowledgeDocs.map((d) => d.id), 0) + 1
  const now = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  mockKnowledgeDocs.push({
    id: newId,
    kbId: data.kbId,
    title: data.title,
    source: 'pdf',
    content: '',
    fileName: data.fileName,
    fileSize: data.fileSize,
    chunkCount: Math.ceil(data.fileSize / 50),
    status: 0, // 处理中
    createTime: now,
  })
  // 模拟处理完成
  setTimeout(() => {
    const doc = mockKnowledgeDocs.find((d) => d.id === newId)
    if (doc) doc.status = 1
    const kb = mockKnowledgeBases.find((k) => k.id === data.kbId)
    if (kb) {
      kb.docCount++
      kb.chunkCount += Math.ceil(data.fileSize / 50)
      kb.updateTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    }
  }, 2000)
  return Promise.resolve(null)
}

// 删除文档
// TODO: 替换为 → request({ url: `/knowledge/doc/${id}`, method: 'delete' })
//       完整路径：DELETE /admin/core/knowledge/doc/{id}
export function deleteKnowledgeDoc(id: number): Promise<null> {
  const idx = mockKnowledgeDocs.findIndex((d) => d.id === id)
  if (idx !== -1) {
    const doc = mockKnowledgeDocs[idx]
    const kb = mockKnowledgeBases.find((k) => k.id === doc.kbId)
    if (kb) {
      kb.docCount = Math.max(0, kb.docCount - 1)
      kb.chunkCount = Math.max(0, kb.chunkCount - doc.chunkCount)
    }
    mockKnowledgeDocs.splice(idx, 1)
  }
  return Promise.resolve(null)
}
