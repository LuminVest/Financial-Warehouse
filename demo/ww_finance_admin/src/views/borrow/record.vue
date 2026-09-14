<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getBorrowRecordList, auditBorrowRecord, type BorrowRecordQuery } from '@/api/borrowRecord'
import type { BorrowRecord } from '@/api/mock'

// ------ 搜索 ------
const searchForm = reactive<BorrowRecordQuery>({
  keyword: '',
  status: undefined,
})

// ------ 列表 ------
const tableData = ref<BorrowRecord[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const data = await getBorrowRecordList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
      status: searchForm.status,
    })
    tableData.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}
function handlePageChange(p: number) {
  pageNum.value = p
  fetchList()
}
function handleSizeChange(s: number) {
  pageSize.value = s
  pageNum.value = 1
  fetchList()
}

// ------ 审核：通过 ------
function handleApprove(row: BorrowRecord) {
  ElMessageBox.confirm(`确定通过「${row.borrowerName}」的借款申请吗？`, '审核通过', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    type: 'success',
  })
    .then(async () => {
      await auditBorrowRecord(row.id, 1)
      ElMessage.success('已通过')
      fetchList()
    })
    .catch(() => {})
}

// ------ 审核：拒绝（表单弹窗） ------
const rejectVisible = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({
  id: 0,
  borrowerName: '',
  reason: '',
})
const rejectRules: FormRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { min: 5, message: '原因至少5个字符', trigger: 'blur' },
  ],
}
// 快捷拒绝原因
const rejectReasonOptions = [
  '信用评分不足，未达到借款门槛',
  '负债比过高，还款能力不足',
  '借款用途不明确或不合规',
  '收入不稳定，偿还风险较高',
  '提供资料不完整或存疑',
]

function openReject(row: BorrowRecord) {
  rejectForm.id = row.id
  rejectForm.borrowerName = row.borrowerName
  rejectForm.reason = ''
  rejectVisible.value = true
}

function closeReject() {
  rejectVisible.value = false
  rejectFormRef.value?.resetFields()
}

async function submitReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await auditBorrowRecord(rejectForm.id, 4, rejectForm.reason)
      ElMessage.success('已拒绝')
      closeReject()
      fetchList()
    } catch {
      // error 已由拦截器处理
    }
  })
}

// ------ 详情弹窗 ------
const detailVisible = ref(false)
const detailData = ref<BorrowRecord | null>(null)

function openDetail(row: BorrowRecord) {
  detailData.value = row
  detailVisible.value = true
}

// ------ 工具 ------
const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '审核通过', type: 'success' },
  2: { text: '还款中', type: 'primary' },
  3: { text: '已结清', type: 'info' },
  4: { text: '已拒绝', type: 'danger' },
}

// ------ 生命周期 ------
onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>借款列表</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="借款人">
          <el-input
            v-model="searchForm.keyword"
            placeholder="借款人姓名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" :value="0" />
            <el-option label="还款中" :value="2" />
            <el-option label="已结清" :value="3" />
            <el-option label="已拒绝" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="page-card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column prop="borrowerName" label="借款人" min-width="100" />
        <el-table-column label="借款金额" min-width="130" align="center">
          <template #default="{ row }">
            <span class="money">¥{{ row.amount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期限" width="80" align="center">
          <template #default="{ row }">{{ row.term }} 个月</template>
        </el-table-column>
        <el-table-column label="年化利率" width="100" align="center">
          <template #default="{ row }">{{ row.rate }}%</template>
        </el-table-column>
        <el-table-column prop="purpose" label="借款用途" min-width="110" />
        <el-table-column label="已还金额" min-width="110" align="center">
          <template #default="{ row }">¥{{ row.repayAmount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type as string">
              {{ statusMap[row.status]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 0" type="danger" link @click="openReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="借款详情" width="580px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="借款编号">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="借款人">{{ detailData.borrowerName }}</el-descriptions-item>
        <el-descriptions-item label="借款人ID">{{ detailData.borrowerId }}</el-descriptions-item>
        <el-descriptions-item label="借款金额">
          <span class="money">¥{{ detailData.amount.toLocaleString() }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="借款期限">{{ detailData.term }} 个月</el-descriptions-item>
        <el-descriptions-item label="年化利率">{{ detailData.rate }}%</el-descriptions-item>
        <el-descriptions-item label="借款用途">{{ detailData.purpose }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detailData.status]?.type as string">
            {{ statusMap[detailData.status]?.text }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="已还金额">¥{{ detailData.repayAmount.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="待还金额">
          <span class="money">¥{{ (detailData.amount - detailData.repayAmount).toLocaleString() }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailData.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailData.auditTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="应还清时间" :span="2">{{ detailData.repayEndTime || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.status === 4 && detailData.rejectReason" label="拒绝原因" :span="2">
          <span class="reject-reason">{{ detailData.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 还款进度 -->
      <div v-if="detailData && detailData.status === 2" class="repay-progress">
        <span class="progress-label">还款进度</span>
        <el-progress
          :percentage="Math.round((detailData.repayAmount / detailData.amount) * 100)"
          :color="['#67c23a', '#409eff', '#e6a23c']"
        />
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝原因表单弹窗 -->
    <el-dialog
      v-model="rejectVisible"
      title="拒绝借款申请"
      width="480px"
      :close-on-click-modal="false"
      @close="closeReject"
    >
      <el-alert
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      >
        确定拒绝「{{ rejectForm.borrowerName }}」的借款申请吗？请填写拒绝原因。
      </el-alert>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因（至少5个字符）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="快捷选择">
          <div class="quick-reasons">
            <el-tag
              v-for="r in rejectReasonOptions"
              :key="r"
              class="quick-tag"
              effect="plain"
              @click="rejectForm.reason = r"
            >
              {{ r }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeReject">取消</el-button>
        <el-button type="danger" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  width: 100%;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
}
.search-card {
  margin-bottom: 16px;
  border-radius: 6px;
}
.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}
.page-card {
  border-radius: 6px;
}
.money {
  color: #f56c6c;
  font-weight: 600;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.repay-progress {
  margin-top: 16px;
}
.progress-label {
  font-size: 14px;
  color: #606266;
  margin-right: 12px;
}
.reject-reason {
  color: #f56c6c;
  font-size: 14px;
}
.quick-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.quick-tag {
  cursor: pointer;
}
.quick-tag:hover {
  color: #f56c6c;
  border-color: #f56c6c;
}
</style>
