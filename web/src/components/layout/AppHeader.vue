<script setup lang="ts">
import NotificationMenu from '@/components/common/NotificationMenu.vue'
import UserMenu from '@/components/common/UserMenu.vue'
import { HEADER_HEIGHT } from '@/constants/layout'
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
            <h2 class="logo-text ml-2 d-none d-sm-block">MaxTwice</h2>
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

/* 移动端优化 */
@media (max-width: 599px) {
  .logo-text {
    font-size: 1.1rem;
  }
}
</style>
