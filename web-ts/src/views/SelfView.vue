<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { userServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import { PostType } from '@/types/enums'
import UserInfoTab from '@/components/user/UserInfoTab.vue'
import LearningTab from '@/components/user/LearningTab.vue'
import StatsTab from '@/components/user/StatsTab.vue'
import UserPostsTab from '@/components/user/UserPostsTab.vue'
import UserContentsTab from '@/components/user/UserContentsTab.vue'
import UserFollowingTab from '@/components/user/UserFollowingTab.vue'
import SubscriptionTab from '@/components/user/SubscriptionTab.vue'
import UserMemoryDecksTab from '@/components/user/UserMemoryDecksTab.vue'
import UserRoadmapsTab from '@/components/user/UserRoadmapsTab.vue'
import UserSidebar from '@/components/user/UserSidebar.vue'
import RightSidebar from '@/components/common/RightSidebar.vue'
import type { TabItem, ComponentProps } from '@/types/common'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const user = useUserStore()

// 学习进度相关
const selectedLearningTab: Ref<string> = ref('roadmaps')

// 标签页配置
const items: Ref<TabItem[]> = ref([
  { text: t('user.profile.personalInfo'), icon: 'mdi-information-outline', value: 'info' },
  { text: t('user.profile.learning'), icon: 'mdi-school-outline', value: 'learning' },
  { text: t('user.profile.statistics'), icon: 'mdi-chart-line', value: 'stats' },
  {
    text: t('user.profile.subscriptions'),
    icon: 'mdi-book-multiple-outline',
    value: 'subscription',
  },
  { text: t('user.profile.following'), icon: 'mdi-account-heart', value: 'follow' },
  { text: t('user.profile.myContents'), icon: 'mdi-format-list-group', value: 'contents' },
  { text: t('user.profile.myArticles'), icon: 'mdi-file-document-outline', value: 'article' },
  { text: t('user.profile.myMemoryDecks'), icon: 'mdi-cards-outline', value: 'memoryDecks' },
  { text: '我创建的路线图', icon: 'mdi-map-marker-path', value: 'roadmaps' },
])

// 组件映射
const tabComponents: Record<string, any> = {
  info: UserInfoTab,
  learning: LearningTab,
  stats: StatsTab,
  subscription: SubscriptionTab,
  follow: UserFollowingTab,
  contents: UserContentsTab,
  article: UserPostsTab,
  memoryDecks: UserMemoryDecksTab,
  roadmaps: UserRoadmapsTab,
}

// 组件属性映射
const getComponentProps = (tabValue: string): ComponentProps => {
  if (tabValue === 'article') {
    return { postType: PostType.ARTICLE }
  } else if (tabValue === 'info') {
    return { editable: true }
  } else if (tabValue === 'follow') {
    return { editable: true }
  } else if (tabValue === 'subscription') {
    return { editable: true }
  }
  return {}
}

// 当前选中的标签页
if (!route.query || !route.query.tab) router.replace({ query: { tab: 'info' } })
const selected = computed(() => (route.query.tab as string) || 'info')

// 当前渲染的组件
const currentComponent = computed(() => tabComponents[selected.value])
const currentProps = computed(() => getComponentProps(selected.value))
const info: Ref<any> = ref({})

// 加载用户信息
const loadUser = async (): Promise<void> => {
  console.log('load user')
  try {
    const response = await userServiceV1.getCurrentUser()

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      console.log('get data:', response.data)
      info.value = response.data
    }
  } catch (error) {
    console.error('Error get message:', error)
  }
}

// 标签页切换处理
const onTabChange = (value: string): void => {
  if (value === 'info') {
    loadUser()
  }
}

// 处理标签页切换
const handleTabChange = (newTab: string): void => {
  router.push({ query: { tab: newTab } })
}

// 生命周期
onMounted(() => {
  onTabChange((route.query.tab as string) || 'info')
  // 检查是否有learningTab参数来设置学习子标签页
  if (route.query.learningTab) {
    selectedLearningTab.value = route.query.learningTab as string
  }
})

// 监听路由变化
watch(
  () => route.query.tab,
  (newValue) => {
    onTabChange(newValue as string)
  }
)

// 监听 learningTab 参数变化
watch(
  () => route.query.learningTab,
  (newValue) => {
    if (newValue && (newValue === 'roadmaps' || newValue === 'courses')) {
      selectedLearningTab.value = newValue as string
    }
  }
)

// 获取当前选中标签的描述
const getSelectedTabDescription = (): string => {
  const descriptions: Record<string, string> = {
    info: '管理个人信息和账户设置',
    learning: '查看学习进度和路线图',
    stats: '统计数据和学习分析',
    subscription: '管理订阅的课程和内容',
    follow: '管理关注的用户和动态',
    contents: '查看和管理我的内容',
    article: '查看和管理我的文章',
    memoryDecks: '查看和管理我的记忆卡片组',
    roadmaps: '查看和管理我创建的路线图',
  }
  return descriptions[selected.value] || '个人中心管理'
}
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
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">个人中心</h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                <v-icon icon="mdi-account-circle" color="primary" size="16" class="mr-1"></v-icon>
                {{ getSelectedTabDescription() }}
              </p>
            </div>
          </div>

          <!-- 用户信息简要显示 -->
          <div class="d-flex align-center">
            <v-avatar color="primary" size="32" class="mr-2">
              <span class="text-white font-weight-bold">{{
                user.name ? user.name.charAt(0).toUpperCase() : 'U'
              }}</span>
            </v-avatar>
            <div>
              <div class="text-body-2 font-weight-bold text-grey-darken-4">
                {{ user.name || '未登录' }}
              </div>
              <div class="text-caption text-grey-darken-2">{{ '' }}</div>
            </div>
          </div>
        </div>
      </v-col>

      <!-- 左侧导航栏 -->
      <UserSidebar v-model:selected-tab="selected" :items="items" @tab-change="handleTabChange" />

      <!-- 主内容区域 -->
      <v-col cols="auto" class="flex-grow-1 d-flex justify-center pt-0">
        <div class="main-content-container py-0">
          <v-slide-y-reverse-transition hide-on-leave>
            <component :is="currentComponent" v-bind="currentProps" :key="selected" />
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