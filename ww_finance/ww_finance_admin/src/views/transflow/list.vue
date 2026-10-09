<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getTransFlowList, type TransFlow, type TransFlowQuery } from '@/api/transFlow'

// ------ 搜索条件 ------
const searchForm = reactive<TransFlowQuery>({
  userName: '',
  transType: undefined,
})

const transTypeOptions = [
  { value: 1, label: '充值' },
  { value: 2, label: '提现' },
  { value: 3, label: '投标' },
  { value: 4, label: '投资回款' },
  { value: 5, label: '放款' },
  { value: 6, label: '还款' },
]

// ------ 列表数据 ------
const tableData = ref<TransFlow[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const data = await getTransFlowList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      userName: searchForm.userName,
      transType: searchForm.transType,
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
  searchForm.userName = ''
  searchForm.transType = undefined
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

// 金额方向颜色：充值/回款/放款为收入(+)，提现/投标/还款为支出(-)
function amountClass(row: TransFlow) {
  const income = [1, 4, 5]
  return income.includes(row.transType) ? 'amount-income' : 'amount-out'
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.userName"
            placeholder="输入用户名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select v-model="searchForm.transType" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="opt in transTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 流水表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userName" label="用户名" min-width="100" />
        <el-table-column prop="transTypeName" label="交易类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.transType === 2 || row.transType === 6 ? 'danger' : 'success'" size="small">
              {{ row.transTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="transNo" label="交易单号" min-width="210" show-overflow-tooltip />
        <el-table-column prop="transAmount" label="金额(元)" width="130" align="right">
          <template #default="{ row }">
            <span :class="amountClass(row)">{{ row.transAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding: 16px;
}
.search-card {
  margin-bottom: 16px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.amount-income {
  color: #52c41a;
  font-weight: 600;
}
.amount-out {
  color: #ea6668;
  font-weight: 600;
}
</style>
