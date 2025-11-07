<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// 核心菜单项（精简版）
const menuItems = [
  { icon: 'mdi-home-variant-outline', text: '首页', path: '/home' },
  { icon: 'mdi-briefcase-search-outline', text: '职业中心', path: '/career' },
  { icon: 'mdi-book-open-page-variant-outline', text: '课程中心', path: '/learning' },
  { icon: 'mdi-brain', text: '复习中心', path: '/memory-review' },
  { icon: 'mdi-account-circle-outline', text: '我的', path: '/profile' }
]

// 判断是否是当前路由
const isActive = (path: string) => {
  return route.path === path
}
</script>

<template>
  <div class="left-sidebar">
    <!-- 核心菜单 -->
    <div class="nav-section">
      <div
        v-for="item in menuItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <router-link :to="item.path" class="nav-link">
          <v-icon size="22" class="nav-icon">{{ item.icon }}</v-icon>
          <span class="nav-text">{{ item.text }}</span>
        </router-link>
      </div>
    </div>

    <!-- 底部图标 -->
    <div class="sidebar-bottom">
      <div class="bottom-icons">
        <v-btn icon variant="text" size="x-small" title="设置">
          <v-icon size="18">mdi-cog</v-icon>
        </v-btn>
        <v-btn icon variant="text" size="x-small" title="隐私">
          <v-icon size="18">mdi-shield-lock</v-icon>
        </v-btn>
        <v-btn icon variant="text" size="x-small" title="常用链接">
          <v-icon size="18">mdi-star</v-icon>
        </v-btn>
        <v-btn icon variant="text" size="x-small" title="帮助">
          <v-icon size="18">mdi-help-circle</v-icon>
        </v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.left-sidebar {
  width: 160px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background-color: #FFFFFF;
  padding: 80px 16px 20px 16px;
  display: flex;
  flex-direction: column;
}

.nav-section {
  margin-bottom: 16px;
}

.nav-item {
  margin-bottom: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  text-decoration: none;
  color: #656D76;
  border-radius: 8px;
  transition: all 0.15s ease;
  gap: 12px;
}

.nav-item:hover .nav-link {
  background-color: #F6F8FA;
  color: #1F2328;
}

.nav-item.active .nav-link {
  background-color: #EFF6FF;
  color: #2563EB;
  font-weight: 600;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
}

.divider {
  height: 1px;
  background-color: #D0D7DE;
  margin: 16px 0;
}

.sidebar-bottom {
  margin-top: auto;
  padding-top: 16px;
}

.bottom-icons {
  display: flex;
  justify-content: space-around;
  align-items: center;
}

/* 移动端 - 底部导航栏 */
@media (max-width: 960px) {
  .left-sidebar {
    width: 100%;
    height: auto;
    position: fixed;
    left: 0;
    top: auto;
    bottom: 0;
    padding: 0;
    flex-direction: row;
    border-top: 1px solid #E5E5E5;
    z-index: 1000;
    display: flex;
  }

  .nav-section {
    display: flex;
    flex: 1;
    margin-bottom: 0;
  }

  .nav-section .divider {
    display: none;
  }

  .nav-item {
    flex: 1;
    margin-bottom: 0;
  }

  .nav-link {
    flex-direction: column;
    padding: 8px 4px;
    gap: 4px;
    text-align: center;
    border-radius: 0;
  }

  .nav-icon {
    font-size: 20px !important;
  }

  .nav-text {
    font-size: 11px;
  }

  .sidebar-bottom {
    display: none;
  }
}
</style>
