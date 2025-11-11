<script setup lang="ts">
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import {
  HEADER_HEIGHT,
  SIDEBAR_WIDTH,
  BOTTOM_NAV_HEIGHT,
  MOBILE_BREAKPOINT,
} from '@/constants/layout'
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
  margin-left: v-bind('`${SIDEBAR_WIDTH}px`');
  margin-top: v-bind('`${HEADER_HEIGHT}px`');
  min-height: v-bind('`calc(100vh - ${HEADER_HEIGHT}px)`');
  padding: 24px;
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
