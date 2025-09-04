<template>
  <v-menu offset-y>
    <template #activator="{ props }">
      <v-btn v-bind="props" variant="text" size="small" class="language-switcher">
        <v-icon start>mdi-translate</v-icon>
        {{ currentLanguageDisplay }}
        <v-icon end>mdi-chevron-down</v-icon>
      </v-btn>
    </template>

    <v-list density="compact" min-width="120">
      <v-list-item
        v-for="lang in languages"
        :key="lang.code"
        :active="currentLocale === lang.code"
        class="language-item"
        @click="changeLanguage(lang.code)"
      >
        <template #prepend>
          <span class="language-flag">{{ lang.flag }}</span>
        </template>
        <v-list-item-title>{{ lang.name }}</v-list-item-title>
        <template v-if="currentLocale === lang.code" #append>
          <v-icon color="primary" size="small">mdi-check</v-icon>
        </template>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { useI18n } from 'vue-i18n'

  const { locale } = useI18n()

  interface Language {
    code: string
    name: string
    flag: string
  }

  // 支持的语言列表
  const languages: Language[] = [
    { code: 'zh', name: '中文', flag: '🇨🇳' },
    { code: 'en', name: 'English', flag: '🇺🇸' },
  ]

  // 当前语言
  const currentLocale = computed(() => locale.value)

  // 当前语言显示名称
  const currentLanguageDisplay = computed(() => {
    const currentLang = languages.find((lang) => lang.code === currentLocale.value)
    return currentLang ? currentLang.name : 'Language'
  })

  // 切换语言
  const changeLanguage = (langCode: string): void => {
    if (langCode !== currentLocale.value) {
      locale.value = langCode

      // 保存到localStorage
      localStorage.setItem('locale', langCode)

      // 在生产环境中，可能需要刷新页面以加载对应的静态资源
      if (import.meta.env.PROD) {
        // 构建时不同语言有不同的域名，所以在生产环境切换需要跳转
        const currentHost = window.location.host
        if (langCode === 'zh' && !currentHost.includes('zh.')) {
          window.location.href = window.location.href.replace(
            currentHost,
            `zh.${currentHost.replace('en.', '')}`
          )
        } else if (langCode === 'en' && !currentHost.includes('en.')) {
          window.location.href = window.location.href.replace(
            currentHost,
            `en.${currentHost.replace('zh.', '')}`
          )
        }
      }
    }
  }
</script>

<style scoped>
  .language-switcher {
    text-transform: none;
  }

  .language-item {
    min-height: 40px;
  }

  .language-flag {
    font-size: 16px;
    margin-right: 8px;
  }

  :deep(.v-list-item-title) {
    font-size: 14px;
  }
</style>