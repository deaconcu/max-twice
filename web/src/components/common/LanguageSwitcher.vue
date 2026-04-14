<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/composables/useI18n'
import type { Locale } from '@/constants/locale'

const { locale, switchLocale } = useI18n()

// 语言选项
const languages = [
  { value: 'zh', label: '中文', flag: '🇨🇳' },
  { value: 'en', label: 'EN', flag: '🇺🇸' },
]

// 当前语言的显示
const currentLang = computed(() => {
  return languages.find((l) => l.value === locale.value) || languages[0]
})

// 切换语言
const handleSwitch = (lang: Locale) => {
  if (lang !== locale.value) {
    switchLocale(lang)
    // 刷新页面以重新加载数据（因为数据来自不同的数据库）
    window.location.reload()
  }
}
</script>

<template>
  <v-menu offset-y>
    <template #activator="{ props }">
      <v-btn variant="text" size="small" v-bind="props" class="lang-btn">
        <span class="text-body-2">{{ currentLang.flag }} {{ currentLang.label }}</span>
        <v-icon size="small" class="ml-1">mdi-chevron-down</v-icon>
      </v-btn>
    </template>
    <v-list density="compact" class="lang-list">
      <v-list-item
        v-for="lang in languages"
        :key="lang.value"
        :active="lang.value === locale"
        @click="handleSwitch(lang.value as Locale)"
      >
        <template #prepend>
          <span class="mr-2">{{ lang.flag }}</span>
        </template>
        <v-list-item-title>{{ lang.label }}</v-list-item-title>
        <template #append>
          <v-icon v-if="lang.value === locale" size="small" color="primary">mdi-check</v-icon>
        </template>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<style scoped>
.lang-btn {
  min-width: auto;
  padding: 0 8px;
}

.lang-list {
  min-width: 120px;
}
</style>
