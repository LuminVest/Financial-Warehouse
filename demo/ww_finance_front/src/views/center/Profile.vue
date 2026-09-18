<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getAccount } from '@/api/account'
import { getBorrowerStatus } from '@/api/borrower'

const router = useRouter()
const userStore = useUserStore()

const balance = ref(0)
const borrowStatus = ref<number | null>(null)

const borrowStatusText: Record<number, { text: string; type: string }> = {
  0: { text: '未认证', type: 'info' },
  1: { text: '认证中', type: 'warning' },
  2: { text: '已认证', type: 'success' },
  [-1]: { text: '已拒绝', type: 'danger' },
}

const entries = [
  { path: '/center/bind', title: '账户绑定', desc: '开通旺旺银行存管账户', icon: 'CreditCard' },
  { path: '/center/borrow-apply', title: '立即借款', desc: '提交借款申请', icon: 'Money' },
  { path: '/center/flow', title: '资金记录', desc: '充值/提现/投标/回款流水', icon: 'List' },
  { path: '/center/borrow-record', title: '借款记录', desc: '查看我的借款', icon: 'Document' },
  { path: '/center/my-lend-return', title: '还款计划', desc: '按期还款', icon: 'Calendar' },
  { path: '/center/charge', title: '充值', desc: '旺旺银行充值', icon: 'Wallet' },
  { path: '/center/withdraw', title: '提现', desc: '旺旺银行提现', icon: 'WalletFilled' },
]

onMounted(async () => {
  try {
    const res = (await getAccount()) as unknown as number | { availableAmount?: number; totalAmount?: number }
    balance.value = typeof res === 'number' ? res : Number(res?.availableAmount ?? res?.totalAmount ?? 0)
  } catch {
    // 未绑卡等场景忽略
  }
  try {
    borrowStatus.value = (await getBorrowerStatus()) as unknown as number
  } catch {
    // 忽略
  }
})
</script>

<template>
  <div>
    <h3 class="page-title">个人中心</h3>

    <div class="profile-card">
      <div class="left">
        <el-avatar :size="64" style="background: #409eff; font-size: 26px">
          {{ (userStore.userInfo?.name || '旺').slice(0, 1) }}
        </el-avatar>
        <div class="name">{{ userStore.userInfo?.name || '未登录用户' }}</div>
        <div class="mobile">{{ userStore.userInfo?.mobile || '' }}</div>
      </div>
      <div class="right">
        <div class="row">
          <span class="k">可用余额</span>
          <span class="v">¥{{ Number(balance).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
        </div>
        <div class="row">
          <span class="k">实名绑卡</span>
          <span class="v">{{ userStore.userInfo?.idCard ? '已绑定' : '未绑定' }}</span>
        </div>
        <div class="row">
          <span class="k">借款人认证</span>
          <span class="v">
            <el-tag :type="(borrowStatusText[borrowStatus ?? 0]?.type as any) ?? 'info'" size="small">
              {{ borrowStatusText[borrowStatus ?? 0]?.text ?? '未认证' }}
            </el-tag>
          </span>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="entry-card">
      <template #header>功能入口</template>
      <div class="entry-grid">
        <div v-for="e in entries" :key="e.path" class="entry" @click="router.push(e.path)">
          <el-icon :size="20" style="color: #409eff"><component :is="e.icon" /></el-icon>
          <div class="t">{{ e.title }}</div>
          <div class="d">{{ e.desc }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 16px;
}
.profile-card {
  display: flex;
  gap: 32px;
  align-items: center;
  background: linear-gradient(135deg, #409eff, #409eff);
  color: #fff;
  border-radius: 12px;
  padding: 28px;
  flex-wrap: wrap;
}
.left {
  text-align: center;
}
.name {
  font-size: 18px;
  font-weight: 600;
  margin-top: 8px;
}
.mobile {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 2px;
}
.right {
  flex: 1;
  min-width: 220px;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
.row .k {
  opacity: 0.9;
}
.row .v {
  font-weight: 600;
}
.entry-card {
  margin-top: 16px;
}
.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}
.entry {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.entry:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.entry .t {
  font-weight: 600;
  margin-top: 8px;
}
.entry .d {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
