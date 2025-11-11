<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { MAIN_MENU_ITEMS, BOTTOM_TOOLS } from '@/constants/menu'
import {
  HEADER_HEIGHT,
  SIDEBAR_WIDTH,
  BOTTOM_NAV_HEIGHT,
  MOBILE_BREAKPOINT,
} from '@/constants/layout'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

// 判断是否是当前路由
const isActive = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}

// TODO: 根据用户权限过滤菜单
const visibleMenuItems = computed(() => {
  // 这里可以添加权限过滤逻辑
  return MAIN_MENU_ITEMS
})

// 工具按钮点击处理
const handleToolClick = (tool: (typeof BOTTOM_TOOLS)[number]) => {
  if (tool.path) {
    router.push(tool.path)
  } else if (tool.action) {
    tool.action()
  }
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
          <span class="nav-text">{{ t(item.textKey) }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 底部工具栏 -->
    <div class="sidebar-bottom">
      <div class="bottom-tools">
        <v-btn
          v-for="tool in BOTTOM_TOOLS"
          :key="tool.icon"
          icon
          variant="text"
          size="x-small"
          :title="t(tool.titleKey)"
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
  width: v-bind('`${SIDEBAR_WIDTH}px`');
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  background-color: rgb(var(--v-theme-surface));
  padding: v-bind('`${HEADER_HEIGHT + 24}px`') 16px 20px 16px;
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
  color: rgb(var(--v-theme-on-surface-variant));
  border-radius: 8px;
  transition: all 0.15s ease;
  gap: 12px;
}

.nav-item:hover .nav-link {
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface));
}

.nav-item.active .nav-link {
  background-color: rgba(var(--v-theme-primary), 0.1);
  color: rgb(var(--v-theme-primary));
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
    height: v-bind('`${BOTTOM_NAV_HEIGHT}px`');
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
