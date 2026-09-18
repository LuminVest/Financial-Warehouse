import request from '@/utils/request'

// 借款人认证 API

export interface BorrowerAttach {
  id?: number
  borrowerId?: number
  imageType: string // idCard1身份证正面 idCard2身份证反面 car车辆 house房产
  imageUrl: string
  imageName: string
}

export interface BorrowerDto {
  sex: number
  age: number
  education: number
  isMarry: number
  industry: number
  employer?: string
  income: number
  returnSource: number
  contactsName: string
  contactsMobile: string
  contactsRelation: number
  borrowerAttachList: BorrowerAttach[]
}

// 提交借款人认证
export function saveBorrower(data: BorrowerDto) {
  return request.post('/api/core/borrower/auth/save', data)
}

// 查询认证状态 0未认证 1认证中 2通过 -1失败
export function getBorrowerStatus() {
  return request.get<number>('/api/core/borrower/auth/getBorrowerStatus')
}

// OSS 文件上传（本地模拟，form-data 字段 file）
export function uploadFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<string>('/oss/file/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
