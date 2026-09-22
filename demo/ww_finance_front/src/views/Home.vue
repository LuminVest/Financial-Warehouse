<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLendList, type Lend } from '@/api/lend'

const router = useRouter()
const recommend = ref<Lend[]>([])
const loading = ref(false)

const statusMap: Record<number, string> = {
  0: '待发布',
  1: '募集中',
  2: '满标',
  3: '还款中',
  4: '已下架',
}

onMounted(async () => {
  loading.value = true
  try {
    const list = (await getLendList()) as unknown as Lend[]
    // 首页展示推荐标的：优先募集中，最多 4 个
    recommend.value = [...list].sort((a, b) => (b.status === 1 ? 1 : 0) - (a.status === 1 ? 1 : 0)).slice(0, 4)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="banner">
      <h1>旺旺信贷 · 让金融服务更简单</h1>
      <p>资金由旺旺银行存管系统托管，平台不碰资金，安全透明</p>
      <div class="banner-actions">
        <el-button type="primary" size="large" @click="router.push('/invest')">我要投资</el-button>
        <el-button size="large" plain @click="router.push('/center/borrow-apply')">我要借款</el-button>
      </div>
    </div>

    <div class="section">
      <div class="section-head">
        <h2 class="section-title">推荐标的</h2>
        <el-link type="primary" :underline="false" @click="router.push('/invest')">查看全部 &gt;</el-link>
      </div>
      <div v-loading="loading">
        <el-empty v-if="!loading && recommend.length === 0" description="暂无标的，请稍后再来" :image-size="80" />
        <div class="lend-grid">
          <el-card v-for="l in recommend" :key="l.id" class="lend-card" shadow="hover">
            <div class="card-head">
              <span class="title">{{ l.title }}</span>
              <el-tag size="small" :type="l.status === 1 ? 'primary' : l.status === 3 ? 'warning' : 'info'">
                {{ statusMap[l.status] ?? l.status }}
              </el-tag>
            </div>
            <div class="amount-row">
              <div class="amount">{{ Number(l.amount).toLocaleString() }}<span class="unit"> 元</span></div>
              <div class="rate">{{ ((l.lendYearRate ?? 0) * 100).toFixed(2) }}<span class="unit">%</span></div>
            </div>
            <div class="meta-row">
              <span>期限 {{ l.period }} 个月</span>
              <span>{{ { 1: '等额本息', 2: '等额本金', 3: '按月付息到期还本' }[l.returnMethod] ?? l.returnMethod }}</span>
            </div>
            <el-button
              type="primary"
              style="width: 100%; margin-top: 10px"
              :disabled="l.status !== 1"
              @click="router.push(`/lend/${l.id}`)"
            >
              {{ l.status === 1 ? '立即投资' : statusMap[l.status] ?? '查看' }}
            </el-button>
          </el-card>
        </div>
      </div>
    </div>

    <div class="section">
      <h2 class="section-title">为什么选择旺旺信贷</h2>
      <div class="feature-grid">
        <div class="feature">
          <div class="feature-icon">🏦</div>
          <div class="feature-title">银行存管</div>
          <div class="feature-desc">资金由旺旺银行存管账户托管，平台不碰资金，交易更透明</div>
        </div>
        <div class="feature">
          <div class="feature-icon">🛡️</div>
          <div class="feature-title">安全保障</div>
          <div class="feature-desc">借款人实名认证 + 材料审核 + 积分授信，多维度把控风险</div>
        </div>
        <div class="feature">
          <div class="feature-icon">📈</div>
          <div class="feature-title">收益可观</div>
          <div class="feature-desc">年化利率 4%~24%，多种还款方式，投资选择更灵活</div>
        </div>
      </div>
    </div>

    <!-- 悬浮智能客服按钮 -->
    <div class="chat-float-btn" @click="router.push('/chat')">
      <el-icon size="24"><Service /></el-icon>
      <span>智能客服</span>
    </div>
  </div>
</template>

<style scoped>
.banner {
  background: linear-gradient(135deg, #409eff, #79bbff);
  color: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  margin-bottom: 24px;
}
.banner h1 {
  margin: 0 0 10px;
  font-size: 26px;
}
.banner p {
  margin: 0 0 20px;
  opacity: 0.92;
  font-size: 14px;
}
.banner-actions {
  display: flex;
  gap: 12px;
}
.section {
  margin-bottom: 28px;
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}
.lend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-head .title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.amount {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
}
.rate {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
}
.unit {
  font-size: 12px;
  color: #999;
  font-weight: 400;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  color: #666;
  font-size: 12px;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}
.feature {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 22px;
  text-align: center;
}
.feature-icon {
  font-size: 32px;
}
.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 10px 0 6px;
}
.feature-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.chat-float-btn {
  position: fixed;
  right: 24px;
  bottom: 80px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transition: all 0.3s;
  z-index: 999;
}

.chat-float-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.5);
}

.chat-float-btn span {
  font-size: 10px;
  margin-top: 2px;
}
</style>
