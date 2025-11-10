<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// TODO: 替换为真实用户数据，从用户状态管理（Pinia store）或 API 获取
// 用户信息（当前为模拟数据）
const userInfo = ref({
  name: '张三',
})

// TODO: 实现真实的设置页面跳转逻辑
// 跳转到设置页面
const goToSettings = () => {
  router.push('/settings')
}

// TODO: 实现真实的退出登录逻辑
// 需要：1. 清除本地存储的 token 2. 清除 Pinia store 中的用户状态 3. 调用后端登出 API
// 退出登录
const handleLogout = () => {
  // TODO: 这里添加实际的登出逻辑
  // 1. 调用 API 通知后端退出
  // 2. 清除本地 token (localStorage/sessionStorage)
  // 3. 清除 Pinia store 中的用户信息
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
      <button class="icon-btn" title="个人中心" v-bind="props">
        <v-icon size="22">mdi-account-circle-outline</v-icon>
      </button>
    </template>

    <v-card rounded="lg" class="user-menu" width="220">
      <!-- 用户信息 -->
      <v-card-text class="pa-4">
        <div class="d-flex flex-column align-center">
          <v-avatar size="56" color="primary" class="mb-3 user-avatar">
            <v-icon size="28" color="white">mdi-account</v-icon>
          </v-avatar>
          <div class="user-name text-center mb-1">{{ userInfo.name }}</div>
          <div class="user-role">学习者</div>
        </div>
      </v-card-text>

      <v-divider></v-divider>

      <!-- 菜单项 -->
      <div class="menu-items py-2">
        <div class="menu-item" @click="goToSettings">
          <v-icon icon="mdi-cog-outline" size="18" color="grey-darken-1" class="menu-icon"></v-icon>
          <span class="menu-title">个人设置</span>
        </div>
        <div class="menu-item" @click="handleLogout">
          <v-icon icon="mdi-logout" size="18" color="error" class="menu-icon"></v-icon>
          <span class="menu-title text-error">退出登录</span>
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
  color: #656d76;
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
  border: 1px solid #e0e0e0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

/* 用户头像 */
.user-avatar {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 用户信息 */
.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1b;
  line-height: 1.3;
}

.user-role {
  font-size: 12px;
  color: #999;
  padding: 2px 10px;
  background-color: #f5f5f5;
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
  background-color: #f8f9fa;
}

.menu-item:active {
  background-color: #f0f0f0;
  transform: scale(0.98);
}

.menu-icon {
  flex-shrink: 0;
  margin-right: 12px;
}

.menu-title {
  flex: 1;
  font-size: 14px;
  color: #1a1a1b;
  font-weight: 500;
}
</style>
