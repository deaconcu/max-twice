<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import ProfessionManagement from '@/components/admin/ProfessionManagement.vue'
import SystemConfiguration from '@/components/admin/SystemConfiguration.vue'
import SystemOperations from '@/components/admin/SystemOperations.vue'
import CourseManagement from '@/components/admin/CourseManagement.vue'
import RoadmapManagement from '@/components/admin/RoadmapManagement.vue'
import NodeManagement from '@/components/admin/NodeManagement.vue'
import PostReview from '@/components/admin/PostReview.vue'
import CommentReview from '@/components/admin/CommentReview.vue'
import MemoryCardReview from '@/components/admin/MemoryCardReview.vue'
import UserManagement from '@/components/admin/UserManagement.vue'
import OperationLogManagement from '@/components/admin/OperationLogManagement.vue'
import ContentGenerator from '@/components/admin/ContentGenerator.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// 有效的 tab 值列表
const validTabs = [
  'system-config',
  'system-operations',
  'operation-logs',
  'user-management',
  'content-generator',
  'profession-management',
  'course-management',
  'roadmap-management',
  'node-management',
  'post-review',
  'comment-review',
  'memory-card-review',
] as const

type TabValue = (typeof validTabs)[number]

// 从 URL 参数读取 tab,如果无效则使用默认值
const getInitialTab = (): TabValue => {
  const tabFromUrl = route.query.tab as string
  return validTabs.includes(tabFromUrl as TabValue) ? (tabFromUrl as TabValue) : 'system-config'
}

const tab = ref<string>(getInitialTab())

// 菜单项配置
const menuItems = [
  { icon: 'mdi-cog-outline', text: '系统配置', value: 'system-config' },
  { icon: 'mdi-cog-sync', text: '系统操作', value: 'system-operations' },
  { icon: 'mdi-clipboard-text-clock', text: '操作日志', value: 'operation-logs' },
  { divider: true },
  { icon: 'mdi-account-multiple', text: '用户管理', value: 'user-management' },
  { icon: 'mdi-robot', text: '内容生成', value: 'content-generator' },
  { icon: 'mdi-briefcase-check-outline', text: '职业管理', value: 'profession-management' },
  { icon: 'mdi-book-check-outline', text: '课程管理', value: 'course-management' },
  { divider: true },
  { icon: 'mdi-map-marker-path', text: '路线图管理', value: 'roadmap-management' },
  { icon: 'mdi-file-tree-outline', text: '节点管理', value: 'node-management' },
  { icon: 'mdi-note-check-outline', text: '文章审核', value: 'post-review' },
  { icon: 'mdi-comment-check-outline', text: '评论审核', value: 'comment-review' },
  { icon: 'mdi-cards-variant', text: '记忆卡片管理', value: 'memory-card-review' },
]

// 监听 tab 变化,同步更新 URL 参数
watch(tab, (newTab) => {
  if (route.query.tab !== newTab) {
    router.replace({ query: { tab: newTab } })
  }
})

// 监听 URL 参数变化,同步更新 tab
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && validTabs.includes(newTab as TabValue) && tab.value !== newTab) {
      tab.value = newTab as string
    }
  }
)

onMounted(async () => {
  // 组件挂载时的初始化逻辑
  console.log('Admin view mounted, current tab:', tab.value)
})
</script>

<template>
  <v-container class="ma-0 pa-0 bg-white" fluid>
    <div class="d-flex flex-row" style="min-height: 100vh">
      <!-- 左侧边栏 -->
      <div class="sidebar-container">
        <v-list density="comfortable" nav class="pa-4">
          <template v-for="item in menuItems" :key="item.value">
            <v-divider v-if="item.divider" class="my-4"></v-divider>
            <v-list-item
              v-else
              :value="item.value"
              :active="tab === item.value"
              :prepend-icon="item.icon"
              :title="item.text"
              rounded="lg"
              class="mb-2"
              @click="tab = item.value"
            ></v-list-item>
          </template>
        </v-list>
      </div>

      <!-- 右侧主内容区域 -->
      <div class="flex-grow-1 px-4 py-4">
        <!-- 系统配置 -->
        <v-card v-if="tab == 'system-config'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <SystemConfiguration />
        </v-card>

        <!-- 系统操作 -->
        <v-card v-if="tab == 'system-operations'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <SystemOperations />
        </v-card>

        <!-- 操作日志 -->
        <v-card v-if="tab == 'operation-logs'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <OperationLogManagement />
        </v-card>

        <!-- 职业申请管理 -->
        <v-card v-if="tab == 'profession-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <ProfessionManagement />
        </v-card>

        <!-- 课程管理 -->
        <v-card v-if="tab == 'course-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <CourseManagement />
        </v-card>

        <!-- 路线图管理 -->
        <v-card v-if="tab == 'roadmap-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <RoadmapManagement />
        </v-card>

        <!-- 节点管理 -->
        <v-card v-if="tab == 'node-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <NodeManagement />
        </v-card>

        <!-- 文章审核 -->
        <v-card v-if="tab == 'post-review'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <PostReview />
        </v-card>

        <!-- 评论审核 -->
        <v-card v-if="tab == 'comment-review'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <CommentReview />
        </v-card>

        <!-- 记忆卡片审核 -->
        <v-card v-if="tab == 'memory-card-review'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <MemoryCardReview />
        </v-card>

        <!-- 用户管理 -->
        <v-card v-if="tab == 'user-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <UserManagement />
        </v-card>

        <!-- 内容生成 -->
        <v-card v-if="tab == 'content-generator'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <ContentGenerator />
        </v-card>
      </div>
    </div>
  </v-container>
</template>

<style scoped>
/* 侧边栏样式 */
.sidebar-container {
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  min-width: 240px;
  background-color: #fafafa;
}

/* 列表项激活状态 */
:deep(.v-list-item--active) {
  background-color: rgb(var(--v-theme-teal-lighten-5)) !important;
  color: rgb(var(--v-theme-teal-darken-2)) !important;
}

:deep(.v-list-item--active .v-list-item__prepend .v-icon) {
  color: rgb(var(--v-theme-teal-darken-2)) !important;
}

:deep(.v-list-item .v-list-item-title) {
  font-size: 16px !important;
  font-weight: 500 !important;
}

:deep(.v-list-item--active .v-list-item-title) {
  font-weight: 600 !important;
}

/* 卡片边框样式 */
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 卡片悬停效果 */
:deep(.v-card[hover]:hover) {
  transform: translateY(-2px);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

/* 文章内容样式 */
.post-content {
  max-height: 700px;
  overflow-y: auto;
  line-height: 1.6;
  padding: 2px 12px;
}

.post-content :deep(p) {
  margin-bottom: 8px;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  margin-bottom: 8px;
  margin-top: 16px;
}

/* 行高优化 */
.line-height-relaxed {
  line-height: 1.7;
}

/* 文本区域样式 */
.config-textarea :deep(.v-field__input) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
  font-size: 13px !important;
  line-height: 1.5 !important;
}

:deep(.v-field__input) {
  font-size: 14px !important;
}

:deep(.tiptap) {
  line-height: 1.6;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.border-dashed {
  border-style: dashed;
}

.w-85 {
  max-width: 85%;
}
</style>
