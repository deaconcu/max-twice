<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import UserInfoTab from '@/components/user/UserInfoTab.vue'
import LearningTab from '@/components/user/LearningTab.vue'
import UserPostsTab from '@/components/user/UserPostsTab.vue'
import UserContentsTab from '@/components/user/UserContentsTab.vue'
import UserFollowingTab from '@/components/user/UserFollowingTab.vue'
import SubscriptionTab from '@/components/user/SubscriptionTab.vue'
import UserSidebar from '@/components/user/UserSidebar.vue'
import RightSidebar from '@/components/common/RightSidebar.vue'
import type { TabItem, ComponentProps } from '@/types/common'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// 标签页配置（User页面不包含stats）
const items: Ref<TabItem[]> = ref([
  { text: t('user.profile.personalInfo'), icon: 'mdi-information-outline', value: 'info' },
  { text: t('user.profile.learning'), icon: 'mdi-school-outline', value: 'learning' },
  {
    text: t('user.profile.subscriptions'),
    icon: 'mdi-book-multiple-outline',
    value: 'subscription',
  },
  { text: t('user.profile.following'), icon: 'mdi-account-heart', value: 'follow' },
  { text: t('user.profile.myContents'), icon: 'mdi-format-list-group', value: 'contents' },
  { text: t('user.profile.myArticles'), icon: 'mdi-file-document-outline', value: 'article' },
])

// 组件映射
const tabComponents: Record<string, any> = {
  info: UserInfoTab,
  learning: LearningTab,
  subscription: SubscriptionTab,
  follow: UserFollowingTab,
  contents: UserContentsTab,
  article: UserPostsTab,
}

// 组件属性映射 - User页面所有组件都传入userId，都不可编辑
const getComponentProps = (tabValue: string): ComponentProps => {
  const baseProps: ComponentProps = {
    userId: route.query.id as string,
  }

  if (tabValue === 'article') {
    return { ...baseProps, postType: 'article' }
  }

  return baseProps
}

// 当前选中的标签页
if (!route.query || !route.query.tab)
  router.replace({ query: { tab: 'info', id: route.query.id } })
const selected = computed(() => (route.query.tab as string) || 'info')

// 当前渲染的组件
const currentComponent = computed(() => tabComponents[selected.value])
const currentProps = computed(() => getComponentProps(selected.value))

// 处理标签页切换
const handleTabChange = (newTab: string): void => {
  router.push({ query: { tab: newTab, id: route.query.id } })
}

// 获取当前选中标签的描述
const getSelectedTabDescription = (): string => {
  const descriptions: Record<string, string> = {
    info: '查看用户的个人信息',
    learning: '查看用户的学习进度',
    subscription: '查看用户的订阅内容',
    follow: '查看用户的关注列表',
    contents: '查看用户创建的内容',
    article: '查看用户发布的文章',
  }
  return descriptions[selected.value] || '查看用户详细信息'
}

// 生命周期
onMounted(() => {
  // UserProfileCard 组件会自动加载用户信息
})
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <!-- 页面头部 -->
      <v-col cols="12" class="mb-4">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-btn
              icon="mdi-arrow-left"
              variant="text"
              color="grey-darken-2"
              class="mr-3"
              @click="$router.go(-1)"
            ></v-btn>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">用户详情</h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                <v-icon icon="mdi-account" color="primary" size="16" class="mr-1"></v-icon>
                {{ getSelectedTabDescription() }}
              </p>
            </div>
          </div>

          <!-- 用户ID显示 -->
          <div class="d-flex align-center">
            <v-chip color="grey-lighten-1" variant="flat" size="small">
              <v-icon icon="mdi-identifier" size="14" class="mr-1"></v-icon>
              用户ID: {{ route.query.id || '未知' }}
            </v-chip>
          </div>
        </div>
      </v-col>

      <!-- 左侧导航栏 -->
      <UserSidebar
        v-model:selected-tab="selected"
        :items="items"
        :user-id="Number(route.query.id as string)"
        @tab-change="handleTabChange"
      />

      <!-- 主内容区域 -->
      <v-col cols="auto" class="flex-grow-1 d-flex justify-center pt-0">
        <div class="main-content-container py-0">
          <v-slide-y-reverse-transition hide-on-leave>
            <component
              :is="currentComponent"
              v-bind="currentProps"
              :key="`${selected}-${route.query.id}`"
            />
          </v-slide-y-reverse-transition>
        </div>
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="3" class="pt-0">
        <RightSidebar />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
/* 主内容区域样式 */
.main-content-container {
  width: 720px;
  max-width: 800px;
}

/* 新版导航样式 */
.user-profile-card {
  background: #fafafa;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.profile-avatar {
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.stat-item {
  padding: 8px 0;
}

.navigation-card {
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-item-modern {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s ease;
  padding: 0;
  border: 1px solid transparent;
}

.nav-item-modern:hover {
  background-color: rgba(25, 118, 210, 0.04);
  border-color: rgba(25, 118, 210, 0.1);
  transform: translateY(-1px);
}

.nav-item-active-modern {
  background-color: rgba(25, 118, 210, 0.08);
  border-color: rgba(25, 118, 210, 0.2);
}

.nav-item-content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  width: 100%;
}

.nav-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  transition: all 0.2s ease;
}

.nav-item-modern:hover .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.1);
}

.nav-item-active-modern .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.15);
}

.nav-icon-modern {
  color: rgba(0, 0, 0, 0.7);
  transition: color 0.2s ease;
}

.nav-item-active-modern .nav-icon-modern {
  color: #1976d2;
}

.nav-title-modern {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
  flex: 1;
  font-size: 14px;
}

.nav-item-active-modern .nav-title-modern {
  color: #1976d2;
  font-weight: 600;
}

.nav-arrow {
  margin-left: auto;
  opacity: 0.8;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

/* 课程项悬停效果 */
.course-item {
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.course-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

/* 确保卡片无阴影 - 参考消息页面的flat设计 (不包括导航卡片和学习卡片) */
.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card) {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card):hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
}

/* 快速操作按钮间距 */
.gap-2 > * + * {
  margin-top: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sticky-left,
  .sticky-right {
    position: relative !important;
    top: unset !important;
    margin-bottom: 20px;
  }

  .pr-8,
  .ps-8 {
    padding-left: 16px !important;
    padding-right: 16px !important;
  }
}
</style>