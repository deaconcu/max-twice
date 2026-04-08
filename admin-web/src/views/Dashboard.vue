<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useUserStore } from '@/stores/modules/user'
import { UserRole } from '@/enums'
import { userApi } from '@/api'
import RoleManagement from '@/components/admin/RoleManagement.vue'
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
import ErrorLogManagement from '@/components/admin/ErrorLogManagement.vue'
import ContentGenerator from '@/components/admin/ContentGenerator.vue'
import ContentGeneratorQueue from '@/components/admin/ContentGeneratorQueue.vue'
import ContentGeneratorRoadmap from '@/components/admin/ContentGeneratorRoadmap.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

// 登出
const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

// 角色显示名称
const roleName = computed(() => {
  const role = userStore.userRole
  switch (role) {
    case UserRole.SUPER_ADMIN:
      return '超级管理员'
    case UserRole.ADMIN:
      return '管理员'
    case UserRole.MODERATOR:
      return '审核员'
    default:
      return '普通用户'
  }
})

// 有效的 tab 值列表
const validTabs = [
  'system-config',
  'system-operations',
  'operation-logs',
  'error-logs',
  'user-management',
  'content-generator-node',
  'content-generator-path',
  'content-generator-management',
  'role-management',
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
  { header: '系统' },
  { icon: 'mdi-cog-outline', text: '系统配置', value: 'system-config' },
  { icon: 'mdi-cog-sync', text: '系统操作', value: 'system-operations' },
  { icon: 'mdi-clipboard-text-clock', text: '操作日志', value: 'operation-logs' },
  { icon: 'mdi-bug-outline', text: '错误日志', value: 'error-logs' },
  { header: '全局内容管理' },
  { icon: 'mdi-account-multiple', text: '用户', value: 'user-management' },
  { icon: 'mdi-briefcase-check-outline', text: '角色', value: 'role-management' },
  { icon: 'mdi-book-check-outline', text: '课程', value: 'course-management' },
  { icon: 'mdi-file-tree-outline', text: '节点', value: 'node-management' },
  { header: '用户内容管理' },
  { icon: 'mdi-map-marker-path', text: '路线图', value: 'roadmap-management' },
  { icon: 'mdi-note-check-outline', text: '文章', value: 'post-review' },
  { icon: 'mdi-comment-check-outline', text: '评论', value: 'comment-review' },
  { icon: 'mdi-cards-variant', text: '记忆卡片', value: 'memory-card-review' },
  { header: 'AI 内容生成' },
  { icon: 'mdi-file-document-edit', text: '节点', value: 'content-generator-node' },
  { icon: 'mdi-graph-outline', text: '路径', value: 'content-generator-path' },
  { icon: 'mdi-cog', text: '队列与配置', value: 'content-generator-management' },
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
  // 获取当前用户信息
  if (!userStore.currentUser) {
    try {
      const response = await userApi.getCurrentUser()
      if (response.code === 200 && response.data) {
        userStore.setUser(response.data)
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  console.log('Admin view mounted, current tab:', tab.value)
})
</script>

<template>
  <v-container class="ma-0 pa-0" fluid>
    <div class="d-flex flex-row" style="min-height: 100vh">
      <!-- 左侧导航栏 -->
      <div class="sidebar-container">
        <!-- Logo区域 -->
        <div class="logo-section">
          <div>
            <div class="text-body-1 font-weight-bold">MaxTwice</div>
            <div class="text-caption text-grey">管理后台</div>
          </div>
        </div>

        <!-- 菜单列表 -->
        <v-list density="comfortable" nav class="pa-0">
          <template v-for="(item, index) in menuItems" :key="index">
            <v-list-subheader v-if="item.header" class="text-caption font-weight-bold px-4 mt-2">
              {{ item.header }}
            </v-list-subheader>
            <v-divider v-else-if="item.divider" class="my-2"></v-divider>
            <v-list-item
              v-else
              :value="item.value"
              :active="tab === item.value"
              :prepend-icon="item.icon"
              :title="item.text"
              :ripple="false"
              rounded="lg"
              class="mb-1 menu-item"
              @click="tab = item.value"
            ></v-list-item>
          </template>
        </v-list>

        <!-- 用户信息区域 -->
        <div class="user-section">
          <v-divider class="mb-3"></v-divider>
          <div class="d-flex align-center justify-space-between pt-2">
            <div class="d-flex align-center">
              <v-avatar size="32" color="grey-lighten-2" class="mr-2">
                <v-icon icon="mdi-account" size="18"></v-icon>
              </v-avatar>
              <div class="user-info">
                <div class="text-body-2 font-weight-medium">{{ userStore.userName }}</div>
                <div class="text-caption text-grey">{{ roleName }}</div>
              </div>
            </div>
            <v-btn
              icon="mdi-logout"
              variant="text"
              size="small"
              @click="handleLogout"
              title="退出登录"
            ></v-btn>
          </div>
        </div>
      </div>

      <!-- 右侧主内容区域 -->
      <div class="flex-grow-1 content-area">
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

        <!-- 错误日志 -->
        <v-card v-if="tab == 'error-logs'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <ErrorLogManagement />
        </v-card>

        <!-- 职业申请管理 -->
        <v-card v-if="tab == 'role-management'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <RoleManagement />
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

        <!-- AI 内容生成 - 节点内容 -->
        <v-card v-if="tab == 'content-generator-node'" class="px-6 pt-2 pb-6 no-border" rounded="lg">
          <ContentGenerator />
        </v-card>

        <!-- AI 内容生成 - 学习路径 -->
        <v-card
          v-if="tab == 'content-generator-path'"
          class="px-6 pt-2 pb-6 no-border"
          rounded="lg"
        >
          <ContentGeneratorRoadmap />
        </v-card>

        <!-- AI 内容生成 - 队列与配置 -->
        <v-card
          v-if="tab == 'content-generator-management'"
          class="px-6 pt-2 pb-6 no-border"
          rounded="lg"
        >
          <ContentGeneratorQueue />
        </v-card>
      </div>
    </div>
  </v-container>
</template>

<style scoped>
/* 侧边栏样式 */
.sidebar-container {
  min-width: 200px;
  max-width: 200px;
  background-color: #f5f5f5;
  padding: 24px 16px;
  height: 100vh;
  overflow-y: hidden;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
}

/* Logo区域 */
.logo-section {
  display: flex;
  align-items: center;
  padding: 0px 8px 8px 8px;
}

/* 用户信息区域 */
.user-section {
  margin-top: auto;
  padding-top: 16px;
}

.user-info {
  max-width: 120px;
  overflow: hidden;
}

.user-info .text-body-2 {
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* v-list去掉背景色 */
:deep(.v-list) {
  background-color: transparent !important;
}

/* 菜单分组标题 */
:deep(.v-list-subheader) {
  color: rgba(0, 0, 0, 0.6);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 11px;
  height: 32px;
  line-height: 32px;
}

/* 主内容区域 */
.content-area {
  background-color: #fdfdfd;
  padding: 24px;
  overflow-y: auto;
}

/* 菜单项样式 */
.menu-item {
  margin: 0 2px 4px 2px;
  transition: all 0.2s ease;
}

/* 列表项激活状态 - 去掉背景色 */
:deep(.v-list-item--active) {
  background-color: #fefefe;
  font-weight: 600 !important;
}

:deep(.v-list-item .v-list-item-title) {
  font-size: 14px !important;
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
