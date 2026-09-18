<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isSafety = computed(() => route.path === '/safety')

const safetyItems = [
  { title: '资金存管', desc: '平台资金由旺旺银行存管系统独立托管，平台不接触用户资金，实现资金与交易分离。' },
  { title: '实名认证', desc: '用户需完成实名认证与存管开户后方可投资、借款，确保交易主体真实可溯。' },
  { title: '信用审核', desc: '借款人需通过平台信息认证与材料核验，借款申请经管理端审核后方可发标。' },
  { title: '数据加密', desc: '用户敏感信息全程加密存储与传输，接口签名校验防止数据篡改与重放。' },
  { title: '还款保障', desc: '借款标的自投标后自动生成还款计划，借款人按期还款，投资人按期回款。' },
  { title: '全程留痕', desc: '充值、提现、投标、还款等资金操作全部生成资金流水，账目清晰可查。' },
]

const aboutItems = [
  { title: '项目名称', desc: '旺旺金融信贷系统（全栈实训项目）' },
  { title: '技术栈', desc: '前端 Vue3 + TypeScript + Vite + Element Plus；后端 Spring Boot + MyBatis-Plus + MySQL + Redis + Nacos' },
  { title: '资金托管', desc: '对接旺旺银行存管系统（模拟），充值、提现、投标、还款均通过存管表单交互' },
  { title: '项目定位', desc: '商丘师范学院金融科技方向教学实训项目，模拟真实 P2P 信贷业务全流程，不涉及真实资金' },
]
</script>

<template>
  <div class="static-page">
    <h2>{{ isSafety ? '安全保障' : '关于我们' }}</h2>
    <p class="sub">{{ isSafety ? '旺旺金融平台安全体系' : '了解旺旺金融' }}</p>

    <div class="grid">
      <el-card v-for="item in (isSafety ? safetyItems : aboutItems)" :key="item.title" shadow="hover" class="item">
        <div class="item-title">{{ item.title }}</div>
        <div class="item-desc">{{ item.desc }}</div>
      </el-card>
    </div>

    <el-alert
      title="本系统为教学演示项目，所有资金均为虚拟金额，不代表任何真实金融业务"
      type="warning"
      :closable="false"
      style="margin-top: 20px"
    />
  </div>
</template>

<style scoped>
.static-page {
  max-width: 900px;
  margin: 0 auto;
}
h2 {
  margin: 0 0 4px;
}
.sub {
  color: #999;
  margin: 0 0 20px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.item-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #409eff;
}
.item-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.7;
}
</style>
