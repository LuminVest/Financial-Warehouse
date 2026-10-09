<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { saveBorrower, getBorrowerStatus, uploadFile, type BorrowerAttach } from '@/api/borrower'
import { listByDictCode, type DictItem } from '@/api/dict'

const router = useRouter()

const status = ref<number | null>(null)
const auditRemark = ref('')
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 下拉选项（从字典表动态加载）
const educationOptions = ref<DictItem[]>([])
const industryOptions = ref<DictItem[]>([])
const incomeOptions = ref<DictItem[]>([])
const returnSourceOptions = ref<DictItem[]>([])
const relationOptions = ref<DictItem[]>([])

// 表单校验规则：失焦即校验，错误红字显示在字段下方
const rules: FormRules = {
  age: [
    { required: true, message: '请填写年龄', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: unknown, callback: (e?: Error) => void) => {
        const n = Number(value)
        if (!Number.isInteger(n) || n < 18 || n > 70) {
          callback(new Error('年龄需在 18-70 之间'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  employer: [{ required: true, message: '请填写工作单位', trigger: 'blur' }],
  contactsName: [{ required: true, message: '请填写联系人姓名', trigger: 'blur' }],
  contactsMobile: [
    { required: true, message: '请填写联系人手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '联系人手机号格式不正确（需为 11 位手机号）', trigger: 'blur' },
  ],
}

const form = reactive({
  sex: 1,
  age: null as number | null,
  education: 1,
  isMarry: 1,
  industry: 1,
  employer: '',
  income: 1,
  returnSource: 1,
  contactsName: '',
  contactsMobile: '',
  contactsRelation: 1,
})

// 材料：四类，每类传一张图
const attachItems = ref<
  Array<{ key: string; label: string; required?: boolean; url: string; fileName: string }>
>([
  { key: 'idCard1', label: '身份证正面', required: true, url: '', fileName: '' },
  { key: 'idCard2', label: '身份证反面', required: true, url: '', fileName: '' },
  { key: 'car', label: '车辆信息', url: '', fileName: '' },
  { key: 'house', label: '房产信息', url: '', fileName: '' },
])

const uploadingKey = ref('')
const fileInputs = ref<Record<string, HTMLInputElement | null>>({})

// OSS 返回的是本地路径（file:///D:/uploads/x.png 或 D:\uploads\x.png），
// 转成 /uploads/x.png（经 vite 代理到 8130 静态映射）供 <img> 展示
function toDisplayUrl(url: string): string {
  const name = url.split(/[\\/]/).pop() ?? ''
  return `/uploads/${name}`
}

function pickFile(key: string) {
  fileInputs.value[key]?.click()
}

// 材料上传：先调 OSS 拿 URL，再随认证提交
async function handleUploadFile(key: string, file: File | undefined) {
  if (!file) return
  uploadingKey.value = key
  try {
    const url = (await uploadFile(file)) as unknown as string
    const item = attachItems.value.find((i) => i.key === key)
    if (item) {
      item.url = toDisplayUrl(url)
      item.fileName = file.name
    }
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败，请确认 OSS 服务(8130)已启动')
  } finally {
    uploadingKey.value = ''
    if (fileInputs.value[key]) fileInputs.value[key]!.value = ''
  }
}

async function load() {
  loading.value = true
  try {
    const info = (await getBorrowerStatus()) as unknown as { status: number; auditRemark: string | null }
    status.value = info.status
    auditRemark.value = info.auditRemark ?? ''
    const [edu, ind, inc, rs, rel] = await Promise.all([
      listByDictCode('education'),
      listByDictCode('industry'),
      listByDictCode('income'),
      listByDictCode('returnSource'),
      listByDictCode('relation'),
    ])
    educationOptions.value = edu as unknown as DictItem[]
    industryOptions.value = ind as unknown as DictItem[]
    incomeOptions.value = inc as unknown as DictItem[]
    returnSourceOptions.value = rs as unknown as DictItem[]
    relationOptions.value = rel as unknown as DictItem[]
  } finally {
    loading.value = false
  }
}

// 材料上传：先调 OSS 拿 URL，再随认证提交
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const missing = attachItems.value.filter((i) => i.required && !i.url)
  if (missing.length > 0) {
    ElMessage.warning(`请上传必传材料：${missing.map((i) => i.label).join('、')}`)
    return
  }
  submitting.value = true
  try {
    const attachList: BorrowerAttach[] = attachItems.value
      .filter((i) => i.url)
      .map((i) => ({ imageType: i.key, imageUrl: i.url, imageName: i.fileName }))
    await saveBorrower({ ...form, age: Number(form.age), borrowerAttachList: attachList })
    ElMessage.success('认证资料已提交，等待管理端审核')
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h2 class="page-title">借款人信息认证</h2>
    <div class="sub-title">请填写您的真实信息，以便我们更好地为您服务</div>

    <el-steps :active="status === 2 ? 3 : status === 1 ? 2 : 1" align-center style="margin: 0 auto 28px; max-width: 720px">
      <el-step title="填写借款人信息" description="完善个人资料" />
      <el-step title="提交平台审核" description="等待审核结果" />
      <el-step title="等待认证结果" description="查看认证状态" />
    </el-steps>

    <!-- 认证中：显示审核中状态，不展示可编辑表单 -->
    <el-result v-if="status === 1" icon="info" title="认证审核中" sub-title="您的认证资料已提交，等待管理端审核，请耐心等待">
      <template #extra>
        <el-button type="primary" @click="router.push('/home')">返回首页</el-button>
      </template>
    </el-result>

    <el-form v-else-if="status !== 2" ref="formRef" :model="form" :rules="rules" v-loading="loading" label-position="top" class="auth-form">
      <el-alert
        v-if="status === -1"
        type="warning"
        :closable="false"
        show-icon
        title="上次认证未通过"
        :description="auditRemark ? `审核意见：${auditRemark}。请按意见修改认证信息后重新提交` : '请修改认证信息后重新提交'"
        style="margin-bottom: 20px"
      />
      <div class="section">
        <div class="section-title">个人基本信息</div>
        <div class="grid-3">
          <el-form-item label="年龄" prop="age" required>
            <el-input-number v-model="form.age" style="width: 100%" :controls="false" />
          </el-form-item>
          <el-form-item label="性别" required>
            <el-select v-model="form.sex" style="width: 100%">
              <el-option label="男" :value="1" />
              <el-option label="女" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="婚否" required>
            <el-select v-model="form.isMarry" style="width: 100%">
              <el-option label="已婚" :value="1" />
              <el-option label="未婚" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="学历" required>
            <el-select v-model="form.education" style="width: 100%" placeholder="请选择学历">
              <el-option v-for="d in educationOptions" :key="d.value" :label="d.name" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="行业" required>
            <el-select v-model="form.industry" style="width: 100%" placeholder="请选择行业">
              <el-option v-for="d in industryOptions" :key="d.value" :label="d.name" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="月收入" required>
            <el-select v-model="form.income" style="width: 100%" placeholder="请选择月收入">
              <el-option v-for="d in incomeOptions" :key="d.value" :label="d.name" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="还款来源" required>
            <el-select v-model="form.returnSource" style="width: 100%" placeholder="请选择还款来源">
              <el-option v-for="d in returnSourceOptions" :key="d.value" :label="d.name" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="工作单位" prop="employer" required>
            <el-input v-model="form.employer" placeholder="当前工作单位" maxlength="50" />
          </el-form-item>
        </div>
      </div>

      <div class="section">
        <div class="section-title">联系人信息</div>
        <div class="grid-3">
          <el-form-item label="联系人姓名" prop="contactsName" required>
            <el-input v-model="form.contactsName" placeholder="紧急联系人" />
          </el-form-item>
          <el-form-item label="联系人手机" prop="contactsMobile" required>
            <el-input v-model="form.contactsMobile" placeholder="11 位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="联系人关系" required>
            <el-select v-model="form.contactsRelation" style="width: 100%" placeholder="请选择联系人关系">
              <el-option v-for="d in relationOptions" :key="d.value" :label="d.name" :value="d.value" />
            </el-select>
          </el-form-item>
        </div>
      </div>

      <div class="section">
        <div class="section-title">身份认证信息</div>
        <div class="attach-grid">
          <el-form-item v-for="item in attachItems" :key="item.key" :label="item.label">
            <div class="attach-row">
              <el-button :loading="uploadingKey === item.key" @click="pickFile(item.key)">
                {{ item.url ? '已上传，重新上传' : '选择图片' }}
              </el-button>
              <input
                :ref="(el: unknown) => (fileInputs[item.key] = el as HTMLInputElement)"
                type="file"
                accept="image/*"
                style="display: none"
                @change="(e: Event) => handleUploadFile(item.key, (e.target as HTMLInputElement).files?.[0])"
              />
              <img v-if="item.url" :src="item.url" class="attach-preview" alt="预览" />
              <span v-if="item.url" class="attach-name">{{ item.fileName }}</span>
              <span v-if="item.required" class="attach-required">*</span>
            </div>
          </el-form-item>
        </div>
      </div>

      <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleSubmit">
        提交认证
      </el-button>
    </el-form>

    <el-result v-else icon="success" title="您的认证审核已通过" sub-title="恭喜您已通过认证，现在可以申请借款了。">
      <template #extra>
        <el-button type="primary" size="large" @click="router.push('/center/borrow-apply')">我要借款</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 4px;
  font-size: 20px;
  color: #333;
}
.sub-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.auth-form {
  max-width: 860px;
}
.section {
  margin-bottom: 8px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.grid-3 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0 24px;
}
.attach-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 0 24px;
}
.attach-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.attach-preview {
  width: 72px;
  height: 48px;
  object-fit: cover;
  border: 1px solid #eee;
  border-radius: 4px;
}
.attach-name {
  color: #52c41a;
  font-size: 12px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attach-required {
  color: #f56c6c;
  margin-left: 2px;
}
.submit-btn {
  width: 220px;
  margin-top: 8px;
}
</style>
