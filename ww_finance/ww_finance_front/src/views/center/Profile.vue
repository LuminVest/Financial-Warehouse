<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getAccount } from '@/api/account'
import { getBorrowerStatus } from '@/api/borrower'
import { getIntegralInfo, getUserInfo, updateProfile } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const balance = ref(0)
const borrowStatus = ref<number | null>(null)

// 个人资料：昵称/性别编辑
const profileForm = ref({ nickName: '', gender: 0 })
const savingProfile = ref(false)
const integralInfo = ref<{ integral?: number; gradeName?: string }>({})

function initProfileForm() {
  profileForm.value.nickName = userStore.userInfo?.nickName || userStore.userInfo?.name || ''
  profileForm.value.gender = userStore.userInfo?.gender ?? 0
}
initProfileForm()

async function loadIntegral() {
  try {
    integralInfo.value = (await getIntegralInfo()) as unknown as { integral?: number; gradeName?: string }
  } catch {
    // 忽略
  }
}

async function saveProfile() {
  if (!profileForm.value.nickName.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    await updateProfile({ nickName: profileForm.value.nickName.trim(), gender: profileForm.value.gender })
    const info = await getUserInfo()
    userStore.setInfo(info as never)
    ElMessage.success('保存成功')
  } catch {
    // 拦截器已提示
  } finally {
    savingProfile.value = false
  }
}

const borrowStatusText: Record<number, { text: string; type: string }> = {
  0: { text: '未认证', type: 'info' },
  1: { text: '认证中', type: 'warning' },
  2: { text: '已认证', type: 'success' },
  [-1]: { text: '已拒绝', type: 'danger' },
}

// 功能入口：借款人 7 项 / 投资人 4 项（与左侧菜单一致）
const entries = computed(() => {
  const common = [
    { path: '/center/bind', title: '账户绑定', desc: '开通旺旺银行存管账户', icon: 'CreditCard' },
    { path: '/center/flow', title: '资金记录', desc: '充值/提现/投标/回款流水', icon: 'List' },
    { path: '/center/charge', title: '充值', desc: '旺旺银行充值', icon: 'Wallet' },
    { path: '/center/withdraw', title: '提现', desc: '旺旺银行提现', icon: 'WalletFilled' },
  ]
  if (userStore.userInfo?.userType === 2) {
    return [
      { path: '/center/bind', title: '账户绑定', desc: '开通旺旺银行存管账户', icon: 'CreditCard' },
      { path: '/center/borrow-apply', title: '立即借款', desc: '提交借款申请', icon: 'Money' },
      { path: '/center/flow', title: '资金记录', desc: '充值/提现/投标/回款流水', icon: 'List' },
      { path: '/center/borrow-record', title: '借款记录', desc: '查看我的借款', icon: 'Document' },
      { path: '/center/my-lend-return', title: '还款计划', desc: '按期还款', icon: 'Calendar' },
      { path: '/center/charge', title: '充值', desc: '旺旺银行充值', icon: 'Wallet' },
      { path: '/center/withdraw', title: '提现', desc: '旺旺银行提现', icon: 'WalletFilled' },
    ]
  }
  return common
})

onMounted(async () => {
  try {
    const res = (await getAccount()) as unknown as number | { availableAmount?: number; totalAmount?: number }
    balance.value = typeof res === 'number' ? res : Number(res?.availableAmount ?? res?.totalAmount ?? 0)
  } catch {
    // 未绑卡等场景忽略
  }
  try {
    const info = (await getBorrowerStatus()) as unknown as { status: number; auditRemark?: string | null }
    borrowStatus.value = info.status
  } catch {
    // 忽略
  }
  loadIntegral()
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
          <span class="k">账户类型</span>
          <span class="v">{{ userStore.userInfo?.userType === 2 ? '借款人' : '投资人' }}</span>
        </div>
        <div class="row">
          <span class="k">实名绑卡</span>
          <span class="v">{{ userStore.userInfo?.bindStatus === 1 ? '已绑定' : '未绑定' }}</span>
        </div>
        <div v-if="userStore.userInfo?.userType === 2" class="row">
          <span class="k">借款人认证</span>
          <span class="v">
            <el-tag :type="(borrowStatusText[borrowStatus ?? 0]?.type as any) ?? 'info'" size="small">
              {{ borrowStatusText[borrowStatus ?? 0]?.text ?? '未认证' }}
            </el-tag>
          </span>
        </div>
      </div>
    </div>

    <!-- 个人资料：昵称/性别编辑 + 积分等级展示 -->
    <el-card shadow="never" class="entry-card">
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <span class="integral-bar">
            积分 {{ integralInfo.integral ?? 0 }}
            <el-tag size="small" type="warning" effect="plain">{{ integralInfo.gradeName || '普通用户' }}</el-tag>
          </span>
        </div>
      </template>
      <el-form :model="profileForm" label-width="60px" style="max-width: 420px">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickName" :maxlength="20" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

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
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.integral-bar {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
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
