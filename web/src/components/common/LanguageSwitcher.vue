<script setup lang="ts">
import { useMutation } from '@tanstack/vue-query'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores/modules/user'
import { accountApi } from '@/api'
import { logger } from '@/utils/logger'

const { locale, switchLocale } = useI18n()
const userStore = useUserStore()

// 登录态：先写后端持久化（跨设备跟随账号），成功后再切本地 + reload
// 未登录：跳过网络请求，只切本地。两种路径都要 reload —— 内容按语言分库，需要重新拉数据。
const updateLocaleMutation = useMutation({
  mutationFn: (next: 'zh' | 'en') => accountApi.updateLocale(next),
})

const toggleLocale = async () => {
  const next = locale.value === 'zh' ? 'en' : 'zh'

  if (userStore.isLoggedIn) {
    try {
      await updateLocaleMutation.mutateAsync(next)
    } catch (e) {
      logger.error('更新语言异常', e)
      return
    }
  }

  switchLocale(next)
  window.location.reload()
}
</script>

<template>
  <v-btn
    variant="text"
    size="small"
    class="lang-btn"
    :loading="updateLocaleMutation.isPending.value"
    :disabled="updateLocaleMutation.isPending.value"
    @click="toggleLocale"
  >
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
