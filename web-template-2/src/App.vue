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
</style>
