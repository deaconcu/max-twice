<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores'
import UserAvatar from '@/components/common/UserAvatar.vue'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => ({
  name: userStore.userName || '用户',
  role: userStore.userRole,
  avatar: userStore.currentUser?.avatar,
}))

// 用户角色显示文本
const userRoleText = computed(() => {
  // TODO: 这里应该根据 userStore.userRole 返回对应的 i18n key
  // 目前暂时使用硬编码，后续需要在 zh.json 中添加角色相关的翻译
  return t('userMenu.learner')
})

// 跳转到设置页面
const goToSettings = () => {
  router.push({ path: '/users/me', query: { tab: 'info' } })
}

// TODO: 实现真实的退出登录逻辑
// 需要：1. 清除本地存储的 token 2. 清除 Pinia store 中的用户状态 3. 调用后端登出 API
// 退出登录
const handleLogout = () => {
  // TODO: 这里添加实际的登出逻辑
  // 1. 调用 API 通知后端退出
  // 2. 清除本地 token (localStorage/sessionStorage)
  // 3. 清除 Pinia store 中的用户信息
  userStore.logout()
  // 4. 跳转到登录页
  router.push('/login')
}
</script>

<template>
  <v-menu
    :close-on-content-click="false"
    location="bottom end"
    offset="8"
    open-on-hover
    :open-delay="100"
  >
    <template #activator="{ props }">
      <button class="icon-btn" :title="t('userMenu.profileCenter')" v-bind="props">
        <UserAvatar
          :name="userInfo.name"
          :avatar-url="userInfo.avatar"
          size="28"
          rounded="lg"
        />
      </button>
    </template>

    <v-card rounded="lg" class="user-menu" width="220">
      <!-- 用户信息 -->
      <v-card-text class="pa-4">
        <div class="d-flex flex-column align-center">
          <UserAvatar
            :name="userInfo.name"
            :avatar-url="userInfo.avatar"
            size="56"
            rounded="xl"
            class="mb-3 user-avatar"
          />
          <div class="user-name text-center mb-1">{{ userInfo.name }}</div>
          <div class="user-role">{{ userRoleText }}</div>
        </div>
      </v-card-text>

      <v-divider></v-divider>

      <!-- 菜单项 -->
      <div class="menu-items py-2">
        <div class="menu-item" @click="goToSettings">
          <v-icon icon="mdi-cog-outline" size="18" color="grey-darken-1" class="menu-icon"></v-icon>
          <span class="menu-title">{{ t('userMenu.personalSettings') }}</span>
        </div>
        <div class="menu-item" @click="handleLogout">
          <v-icon icon="mdi-logout" size="18" color="error" class="menu-icon"></v-icon>
          <span class="menu-title text-error">{{ t('userMenu.logout') }}</span>
        </div>
      </div>
    </v-card>
  </v-menu>
</template>

<style scoped>
.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: rgb(var(--v-theme-on-surface-variant));
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.icon-btn:hover {
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface));
}

/* 用户菜单 */
.user-menu {
  border: 1px solid rgb(var(--v-theme-border));
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

/* 用户头像 */
.user-avatar {
  /* no shadow */
}

/* 用户信息 */
.user-name {
  font-size: 15px;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
  line-height: 1.3;
}

.user-role {
  font-size: 12px;
  color: rgb(var(--v-theme-on-surface-variant));
  opacity: 0.7;
  padding: 2px 10px;
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 12px;
}

/* 菜单项列表 */
.menu-items {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.menu-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
}

.menu-item:active {
  background-color: rgba(var(--v-theme-surface-variant), 0.8);
  transform: scale(0.98);
}

.menu-icon {
  flex-shrink: 0;
  margin-right: 12px;
}

.menu-title {
  flex: 1;
  font-size: 14px;
  color: rgb(var(--v-theme-on-surface));
  font-weight: 500;
}
</style>
