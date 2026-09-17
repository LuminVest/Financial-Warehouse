<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getBorrowerList, auditBorrower, type BorrowerQuery } from '@/api/borrower'
import type { Borrower } from '@/api/mock'

// ------ 搜索 ------
const searchForm = reactive<BorrowerQuery>({
  keyword: '',
  auditStatus: undefined,
})

// ------ 列表 ------
const tableData = ref<Borrower[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const data = await getBorrowerList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
      auditStatus: searchForm.auditStatus,
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
  searchForm.auditStatus = undefined
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

// ------ 审核弹窗 ------
const auditDialogVisible = ref(false)
const auditRow = ref<Borrower | null>(null)
const auditForm = reactive({
  auditStatus: 1,
  idCardOk: 1,
  carOk: 1,
  houseOk: 1,
  remark: '',
})

// 预计可获积分：基本信息 30 + 身份证 30 + 车辆 60 + 房产 100
const expectScore = ref(0)
function calcExpectScore() {
  expectScore.value = 30 + (auditForm.idCardOk === 1 ? 30 : 0) + (auditForm.carOk === 1 ? 60 : 0) + (auditForm.houseOk === 1 ? 100 : 0)
}

function openAuditDialog(row: Borrower) {
  auditRow.value = row
  auditForm.auditStatus = 1
  auditForm.idCardOk = 1
  auditForm.carOk = 1
  auditForm.houseOk = 1
  auditForm.remark = ''
  calcExpectScore()
  auditDialogVisible.value = true
}

async function submitAudit() {
  if (!auditRow.value) return
  try {
    const res = await auditBorrower(auditRow.value.id, {
      auditStatus: auditForm.auditStatus,
      idCardOk: auditForm.idCardOk,
      carOk: auditForm.carOk,
      houseOk: auditForm.houseOk,
      remark: auditForm.remark,
    })
    const score = res?.data?.score ?? 0
    ElMessage.success(
      auditForm.auditStatus === 1
        ? score > 0 ? `审核通过，本次回写积分 ${score}` : '审核通过'
        : '已拒绝',
    )
    auditDialogVisible.value = false
    fetchList()
  } catch {
    // error 已由拦截器处理
  }
}

// ------ 详情弹窗 ------
const detailVisible = ref(false)
const detailData = ref<Borrower | null>(null)

function openDetail(row: Borrower) {
  detailData.value = row
  detailVisible.value = true
}

// ------ 工具 ------
const auditMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '审核通过', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
}
function genderText(g: number) {
  return g === 1 ? '男' : g === 2 ? '女' : '未知'
}

// ------ 生命周期 ------
onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>借款人列表</h2>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="姓名/手机号/身份证"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" :value="0" />
            <el-option label="审核通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
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
        <el-table-column prop="realName" label="真实姓名" min-width="100" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄" width="70" align="center">
          <template #default="{ row }">{{ row.age ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="是否结婚" width="90" align="center">
          <template #default="{ row }">{{ row.isMarry === 1 ? '是' : row.isMarry === 0 ? '否' : '—' }}</template>
        </el-table-column>
        <el-table-column label="月收入" min-width="110" align="center">
          <template #default="{ row }">¥{{ row.monthlyIncome.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="授信额度" min-width="120" align="center">
          <template #default="{ row }">¥{{ row.creditLimit.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditMap[row.auditStatus]?.type as string">
              {{ auditMap[row.auditStatus]?.text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.auditStatus === 0" type="warning" link @click="openAuditDialog(row)">审核</el-button>
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="审批信息" width="460px" :close-on-click-modal="false">
      <el-form :model="auditForm" label-width="150px">
        <el-form-item label="借款人">
          <span>{{ auditRow?.realName }}（{{ auditRow?.phone }}）</span>
        </el-form-item>
        <el-form-item label="是否通过">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="基本信息积分">
          <span>30</span>
          <span style="color: #999; font-size: 12px; margin-left: 8px">可获取数30至100积分</span>
        </el-form-item>
        <el-form-item label="身份证信息是否正确">
          <el-radio-group v-model="auditForm.idCardOk">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
          <span style="color: #999; font-size: 12px; margin-left: 8px">可获得积分 30 积分</span>
        </el-form-item>
        <el-form-item label="车辆信息是否正确">
          <el-radio-group v-model="auditForm.carOk">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
          <span style="color: #999; font-size: 12px; margin-left: 8px">可获得积分 60 积分</span>
        </el-form-item>
        <el-form-item label="房产信息是否正确">
          <el-radio-group v-model="auditForm.houseOk">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
          <span style="color: #999; font-size: 12px; margin-left: 8px">可获得积分 100 积分</span>
        </el-form-item>
        <el-form-item v-if="auditForm.auditStatus === 1" label="预计可获积分">
          <span style="color: #e8421f; font-weight: 600">{{ expectScore }} 积分</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">返回列表</el-button>
        <el-button type="primary" @click="submitAudit">提交审批</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="借款人详情" width="580px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detailData.memberId }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detailData.realName }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderText(detailData.gender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ detailData.age ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="是否结婚">{{ detailData.isMarry === 1 ? '是' : detailData.isMarry === 0 ? '否' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detailData.idCard }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailData.phone }}</el-descriptions-item>
        <el-descriptions-item label="月收入">¥{{ detailData.monthlyIncome.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="授信额度">¥{{ detailData.creditLimit.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="已用额度">¥{{ detailData.usedLimit.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditMap[detailData.auditStatus]?.type as string">
            {{ auditMap[detailData.auditStatus]?.text }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ detailData.auditTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
