<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// TODO: 后续根据用户权限动态过滤菜单项
interface MenuItem {
  icon: string
  text: string
  path: string
  // TODO: 添加权限字段，用于权限控制
  // permission?: string
}

// 核心菜单项
const menuItems: MenuItem[] = [
  { icon: 'mdi-home-variant-outline', text: '首页', path: '/home' },
  { icon: 'mdi-briefcase-search-outline', text: '职业中心', path: '/career' },
  { icon: 'mdi-book-open-page-variant-outline', text: '课程中心', path: '/learning' },
  { icon: 'mdi-brain', text: '复习中心', path: '/memory-review' },
  { icon: 'mdi-account-circle-outline', text: '我的', path: '/profile' },
]

// TODO: 实现底部工具按钮的实际功能
// 底部工具按钮
const bottomTools = [
  { icon: 'mdi-cog', title: '设置' },
  { icon: 'mdi-shield-lock', title: '隐私' },
  { icon: 'mdi-star', title: '常用链接' },
  { icon: 'mdi-help-circle', title: '帮助' },
]

// 判断是否是当前路由
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// TODO: 根据用户权限过滤菜单
const visibleMenuItems = computed(() => {
  // 这里可以添加权限过滤逻辑
  return menuItems
})

// TODO: 实现工具按钮点击处理
const handleToolClick = (_tool: (typeof bottomTools)[number]) => {
  // 后续实现实际功能
}
</script>

<template>
  <aside class="app-sidebar">
    <!-- 核心菜单 -->
    <nav class="nav-section">
      <div
        v-for="item in visibleMenuItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <router-link :to="item.path" class="nav-link">
          <v-icon size="22" class="nav-icon">{{ item.icon }}</v-icon>
          <span class="nav-text">{{ item.text }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 底部工具栏 -->
    <div class="sidebar-bottom">
      <div class="bottom-tools">
        <v-btn
          v-for="tool in bottomTools"
          :key="tool.icon"
          icon
          variant="text"
          size="x-small"
          :title="tool.title"
          @click="handleToolClick(tool)"
        >
          <v-icon size="18">{{ tool.icon }}</v-icon>
        </v-btn>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.app-sidebar {
  width: 160px;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background-color: rgb(var(--v-theme-surface));
  padding: 80px 16px 20px 16px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgb(var(--v-theme-border));
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
  color: #656d76;
  border-radius: 8px;
  transition: all 0.15s ease;
  gap: 12px;
}

.nav-item:hover .nav-link {
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface));
}

.nav-item.active .nav-link {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
}

.sidebar-bottom {
  margin-top: auto;
  padding-top: 16px;
}

.bottom-tools {
  display: flex;
  justify-content: space-around;
  align-items: center;
}

/* 移动端 - 转为底部导航栏 */
@media (max-width: 960px) {
  .app-sidebar {
    width: 100%;
    height: auto;
    position: fixed;
    left: 0;
    top: auto;
    bottom: 0;
    padding: 0;
    flex-direction: row;
    border-right: none;
    border-top: 1px solid rgb(var(--v-theme-border));
    z-index: 1000;
  }

  .nav-section {
    display: flex;
    flex: 1;
    margin-bottom: 0;
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
