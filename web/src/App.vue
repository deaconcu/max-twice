<template>
  <v-app>
    <router-view />
  </v-app>
</template>

<script setup lang="ts">
// Vue 3 Composition API with Vuetify
</script>

<style>
/* 全局重置 - 只重置 body，避免影响 Vuetify 组件 */
body {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: rgb(var(--v-theme-background));
  color: rgb(var(--v-theme-on-background));
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

/* 简约风格全局样式 */
.v-card {
  border: 1px solid rgb(var(--v-theme-border)) !important;
  background-color: rgb(var(--v-theme-surface)) !important;
}

.v-card.no-border {
  border: none !important;
}

/* Vuetify 按钮文本不转换大写 */
.v-btn {
  text-transform: none !important;
  letter-spacing: normal !important;
}

/* 自定义 Vuetify 圆角大小 */
.rounded-lg {
  border-radius: 14px !important;
}

.rounded-md {
  border-radius: 6px !important;
}

/* Tooltip 样式修复 */
.v-tooltip .v-overlay__content {
  background-color: rgba(var(--v-theme-on-surface), 0.9) !important;
  color: rgb(var(--v-theme-surface)) !important;
  padding: 4px 8px !important;
  font-size: 12px !important;
}

/* 统一的卡片 hover 效果 */
.v-card.hoverable {
  transition: all 0.3s ease;
  cursor: pointer;
}

.v-card.hoverable:hover {
  transform: translateY(-4px);
  background-color: rgb(var(--v-theme-surface-variant)) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
