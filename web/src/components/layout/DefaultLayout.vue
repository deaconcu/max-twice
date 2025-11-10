<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
</script>

<template>
  <div class="default-layout">
    <!-- 顶部导航 -->
    <AppHeader />

    <!-- 侧边栏 -->
    <AppSidebar />

    <!-- 主内容区域 -->
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.default-layout {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
}

.main-content {
  margin-left: 160px;
  margin-top: 56px;
  min-height: calc(100vh - 56px);
  padding: 24px;
}

/* 移动端布局 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    margin-bottom: 60px; /* 为底部导航栏留空间 */
    padding: 16px;
  }
}
</style>
