<script setup lang="ts">
import { useI18n } from '@/composables/useI18n'

const { locale, switchLocale } = useI18n()

// 只有 zh / en 两种语言，直接 toggle；与登录页 SimpleHeader 的样式保持一致。
// 切换后 reload 是因为内容按语言分库，必须重新拉数据。
const toggleLocale = () => {
  switchLocale(locale.value === 'zh' ? 'en' : 'zh')
  window.location.reload()
}
</script>

<template>
  <v-btn variant="text" size="small" class="lang-btn" @click="toggleLocale">
    {{ locale === 'zh' ? '中' : 'EN' }}
  </v-btn>
</template>

<style scoped>
.lang-btn {
  font-size: 0.85rem;
  color: rgb(var(--v-theme-on-surface));
  opacity: 0.7;
}

.lang-btn:hover {
  opacity: 1;
}
</style>
