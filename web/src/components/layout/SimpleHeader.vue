<script setup lang="ts">
import { HEADER_HEIGHT } from '@/constants/layout'
</script>

<template>
  <v-app-bar :elevation="0" class="simple-header" :height="HEADER_HEIGHT" flat>
    <v-container fluid class="px-4 px-sm-6">
      <v-row align="center" no-gutters>
        <!-- Logo 和名称 -->
        <v-col cols="auto">
          <div class="d-flex align-center" style="cursor: pointer" @click="$router.push('/home')">
            <div class="logo-icon">
              <v-icon size="28" color="primary">mdi-infinity</v-icon>
            </div>
            <h2 class="logo-text ml-2">TwiceMax</h2>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app-bar>
</template>

<style scoped>
.simple-header {
  background-color: transparent !important;
  border-bottom: none;
}

/* 移除 Vuetify 默认的背景 */
.simple-header :deep(.v-toolbar__content) {
  background-color: transparent;
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(var(--v-theme-surface-variant), 0.8);
  border: 2px solid rgba(var(--v-theme-border), 0.3);
  border-radius: 50%;
  backdrop-filter: blur(10px);
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  letter-spacing: -0.02em;
}
</style>
