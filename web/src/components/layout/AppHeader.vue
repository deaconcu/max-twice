<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import NotificationMenu from '@/components/common/NotificationMenu.vue'
import UserMenu from '@/components/common/UserMenu.vue'
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue'
import IcosahedronLogoStatic from '@/components/common/IcosahedronLogoStatic.vue'
import IcosahedronLogo from '@/components/common/IcosahedronLogo.vue'
import { HEADER_HEIGHT } from '@/constants/layout'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const searchQuery = ref('')

// 进入搜索页时回填关键词
watch(
  () => route.query.q,
  (q) => {
    searchQuery.value = (q as string) || ''
  },
  { immediate: true }
)

const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  router.push({
    path: '/search',
    query: { q: searchQuery.value },
  })
}
</script>

<template>
  <v-app-bar :elevation="0" class="app-header" :height="HEADER_HEIGHT">
    <v-container fluid class="px-2 px-sm-4">
      <v-row align="center" no-gutters>
        <!-- Logo -->
        <v-col cols="auto">
          <div class="d-flex align-center" style="cursor: pointer" @click="$router.push('/home')">
            <IcosahedronLogo :size="32" color="#3aa876" :stroke-width="1.2" :speed="60" />
            <!-- Logo 文字在小屏幕隐藏 -->
            <h2 class="logo-text ml-2 d-none d-sm-block">TwiceMax</h2>
          </div>
        </v-col>

        <v-spacer />

        <!-- 中间搜索框 - 只在中大屏显示 -->
        <v-col cols="auto" class="d-none d-md-block">
          <div class="search-container">
            <v-text-field
              v-model="searchQuery"
              density="compact"
              variant="solo"
              :placeholder="t('common.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              hide-details
              single-line
              class="search-field"
              style="width: 480px"
              @keyup.enter="handleSearch"
            ></v-text-field>
          </div>
        </v-col>

        <v-spacer />

        <!-- Right side actions -->
        <v-col cols="auto">
          <div class="d-flex align-center" :class="$vuetify.display.xs ? 'ga-2' : 'ga-4'">
            <!-- 语言切换 -->
            <LanguageSwitcher />

            <!-- 通知菜单 -->
            <NotificationMenu />

            <!-- 用户菜单 -->
            <UserMenu />
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app-bar>
</template>

<style scoped>
.app-header {
  background-color: rgb(var(--v-theme-surface)) !important;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
}

/* 搜索框样式 */
.search-container {
  display: flex;
  align-items: center;
}

.search-field :deep(.v-field) {
  background-color: rgb(var(--v-theme-surface-variant)) !important;
  border-radius: 24px !important;
  box-shadow: none !important;
  transition: all 0.2s ease;
}

.search-field :deep(.v-field):hover {
  background-color: rgba(var(--v-theme-on-surface), 0.08) !important;
}

.search-field :deep(.v-field--focused) {
  background-color: rgb(var(--v-theme-surface)) !important;
  box-shadow: 0 0 0 2px rgb(var(--v-theme-primary)) !important;
}

.search-field :deep(.v-field__input) {
  font-size: 16px;
  padding: 8px 0;
  min-height: 36px;
}

.search-field :deep(.v-field__prepend-inner) {
  padding-left: 8px;
  padding-right: 4px;
}

/* 移动端优化 */
@media (max-width: 599px) {
  .logo-text {
    font-size: 1.1rem;
  }
}
</style>
