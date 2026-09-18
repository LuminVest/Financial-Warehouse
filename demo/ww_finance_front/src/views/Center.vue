<script setup lang="ts">
// 个人中心内容容器：左侧全局菜单已在 App.vue（PDF P3 布局）
</script>

<template>
  <div class="center-container">
    <router-view />
  </div>
</template>

<style scoped>
.center-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  min-height: 520px;
  box-sizing: border-box;
}
</style>
