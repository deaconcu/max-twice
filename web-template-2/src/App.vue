<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <v-app>
    <RouterView />
  </v-app>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #FFFFFF;
  color: #000000;
}

/* 简约风格全局样式 */
.v-card {
  border: 1px solid #E5E5E5 !important;
  background-color: #FFFFFF !important;
}

.v-card.no-border {
  border: none !important;
}

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
  background-color: rgba(97, 97, 97, 0.9) !important;
  color: #FFFFFF !important;
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
  background-color: #FAFAFA !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
