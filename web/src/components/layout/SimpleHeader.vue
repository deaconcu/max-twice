<script setup lang="ts">
import { HEADER_HEIGHT } from '@/constants/layout'
import { useI18n } from '@/composables/useI18n'

const { locale, switchLocale } = useI18n()

const toggleLocale = () => {
  switchLocale(locale.value === 'zh' ? 'en' : 'zh')
}
</script>

<template>
  <v-app-bar :elevation="0" class="simple-header" :height="HEADER_HEIGHT" flat>
    <v-container fluid class="px-4 px-sm-6">
      <v-row align="center" no-gutters>
        <!-- Logo 和名称 -->
        <v-col cols="auto">
          <div class="d-flex align-center">
            <h2 class="logo-text">TwiceMax</h2>
          </div>
        </v-col>

        <v-spacer />

        <!-- 语言切换 -->
        <v-col cols="auto">
          <v-btn
            variant="text"
            size="small"
            class="lang-btn"
            @click="toggleLocale"
          >
            {{ locale === 'zh' ? '中' : 'EN' }}
          </v-btn>
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

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  letter-spacing: -0.02em;
}

.lang-btn {
  font-size: 0.85rem;
  color: rgb(var(--v-theme-on-surface));
  opacity: 0.7;
}

.lang-btn:hover {
  opacity: 1;
}
</style>
