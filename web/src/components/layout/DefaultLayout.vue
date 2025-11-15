<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import { HEADER_HEIGHT, BOTTOM_NAV_HEIGHT, SIDEBAR_WIDTH } from '@/constants/layout'
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
  margin-left: v-bind('`max(${SIDEBAR_WIDTH}px, calc(${SIDEBAR_WIDTH}px + (100vw - ${SIDEBAR_WIDTH}px - 1550px) / 2))`');
  margin-top: v-bind('`${HEADER_HEIGHT}px`');
  min-height: v-bind('`calc(100vh - ${HEADER_HEIGHT}px)`');
  padding: 20px 32px;
  max-width: 1550px;
  width: v-bind('`calc(100vw - ${SIDEBAR_WIDTH}px - max(0px, (100vw - ${SIDEBAR_WIDTH}px - 1550px) / 2))`');
}

/* 移动端布局 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    margin-bottom: v-bind('`${BOTTOM_NAV_HEIGHT}px`');
    padding: 16px;
  }
}
</style>
