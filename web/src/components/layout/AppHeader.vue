<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import NotificationMenu from '@/components/common/NotificationMenu.vue'
import UserMenu from '@/components/common/UserMenu.vue'
import { HEADER_HEIGHT } from '@/constants/layout'

const { t } = useI18n()
const router = useRouter()
const searchQuery = ref('')

const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  router.push({
    path: '/search',
    query: { q: searchQuery.value },
  })
  searchQuery.value = ''
}
</script>

<template>
  <v-app-bar :elevation="0" class="app-header" :height="HEADER_HEIGHT">
    <v-container fluid class="px-2 px-sm-4">
      <v-row align="center" no-gutters>
        <!-- Logo -->
        <v-col cols="auto">
          <div class="d-flex align-center" style="cursor: pointer" @click="$router.push('/home')">
            <div class="logo-icon">
              <v-icon size="28" color="primary">mdi-reddit</v-icon>
            </div>
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

.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
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
  font-size: 14px;
  padding: 8px 0;
  min-height: 36px;
}

.search-field :deep(.v-field__prepend-inner) {
  padding-left: 12px;
}

/* 移动端优化 */
@media (max-width: 599px) {
  .logo-text {
    font-size: 1.1rem;
  }
}
</style>
