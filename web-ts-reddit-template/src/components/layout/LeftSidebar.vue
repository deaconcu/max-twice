<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 导航项配置
const navigationItems = [
  {
    title: '核心功能',
    items: [
      { icon: 'mdi-home', text: '首页', path: '/home', badge: null },
      { icon: 'mdi-book-open-variant', text: '课程中心', path: '/learning', badge: null },
      { icon: 'mdi-briefcase-variant', text: '职业中心', path: '/career', badge: null },
      { icon: 'mdi-map-marker-path', text: '学习路线', path: '/roadmap', badge: null }
    ]
  },
  {
    title: '个人空间',
    items: [
      { icon: 'mdi-account', text: '个人主页', path: '/profile', badge: null },
      { icon: 'mdi-book-multiple', text: '我的课程', path: '/my-courses', badge: null },
      { icon: 'mdi-briefcase-variant', text: '我的职业', path: '/my-careers', badge: null },
      { icon: 'mdi-star', text: '收藏', path: '/favorites', badge: '12' },
      { icon: 'mdi-note-text', text: '笔记', path: '/notes', badge: null }
    ]
  },
  {
    title: '社区',
    items: [
      { icon: 'mdi-fire', text: '热门', path: '/trending', badge: null },
      { icon: 'mdi-forum', text: '讨论', path: '/discussions', badge: '5' },
      { icon: 'mdi-help-circle', text: '问答', path: '/qa', badge: null }
    ]
  },
  {
    title: '工具',
    items: [
      { icon: 'mdi-cards-variant', text: '记忆卡片', path: '/memory-cards', badge: null },
      { icon: 'mdi-chart-line', text: '学习统计', path: '/stats', badge: null }
    ]
  }
]

// 底部固定项
const bottomItems = [
  { icon: 'mdi-cog', text: '设置', path: '/settings' },
  { icon: 'mdi-help-circle-outline', text: '帮助', path: '/help' }
]

// 判断是否是当前路由
const isActive = (path: string) => {
  return route.path === path
}
</script>

<template>
  <div class="left-sidebar">
    <div class="sidebar-sticky">
      <!-- 导航分组 -->
      <div
        v-for="(group, groupIndex) in navigationItems"
        :key="groupIndex"
        class="nav-group mb-4"
      >
        <v-card border rounded="xl" class="nav-card">
          <!-- 分组标题 -->
          <div class="group-header pa-3 pb-2">
            <span class="text-caption text-grey-darken-1 font-weight-bold">
              {{ group.title }}
            </span>
          </div>

          <!-- 导航项 -->
          <v-list density="compact" class="pa-2">
            <v-list-item
              v-for="(item, index) in group.items"
              :key="index"
              :to="item.path"
              :class="{ 'nav-item-active': isActive(item.path) }"
              class="nav-item rounded-lg mb-1"
              @click="$router.push(item.path)"
            >
              <template #prepend>
                <v-icon
                  :icon="item.icon"
                  :color="isActive(item.path) ? 'primary' : 'grey-darken-1'"
                  size="20"
                ></v-icon>
              </template>

              <v-list-item-title
                :class="isActive(item.path) ? 'text-primary font-weight-bold' : 'text-grey-darken-3'"
              >
                {{ item.text }}
              </v-list-item-title>

              <template v-if="item.badge" #append>
                <v-chip
                  size="x-small"
                  color="primary"
                  variant="flat"
                  class="badge-chip"
                >
                  {{ item.badge }}
                </v-chip>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </div>

      <!-- 底部固定项 -->
      <v-card border rounded="xl" class="nav-card">
        <v-list density="compact" class="pa-2">
          <v-list-item
            v-for="(item, index) in bottomItems"
            :key="index"
            :to="item.path"
            :class="{ 'nav-item-active': isActive(item.path) }"
            class="nav-item rounded-lg mb-1"
          >
            <template #prepend>
              <v-icon
                :icon="item.icon"
                :color="isActive(item.path) ? 'primary' : 'grey-darken-1'"
                size="20"
              ></v-icon>
            </template>

            <v-list-item-title
              :class="isActive(item.path) ? 'text-primary font-weight-bold' : 'text-grey-darken-3'"
            >
              {{ item.text }}
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.left-sidebar {
  width: 260px;
  flex-shrink: 0;
  padding: 10px 0 20px 20px;
}

.sidebar-sticky {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

/* 自定义滚动条 */
.sidebar-sticky::-webkit-scrollbar {
  width: 4px;
}

.sidebar-sticky::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-sticky::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.sidebar-sticky::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.nav-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.group-header {
  border-bottom: 1px solid #EDEFF1;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 2px;
}

.nav-item:hover {
  background-color: rgba(var(--v-theme-primary), 0.08);
}

.nav-item-active {
  background-color: rgba(var(--v-theme-primary), 0.12);
}

.badge-chip {
  height: 18px;
  font-size: 0.7rem;
  font-weight: 700;
}

/* 响应式 */
@media (max-width: 1280px) {
  .left-sidebar {
    display: none;
  }
}
</style>
