<script lang="ts">
export default {
  name: 'ProfilePage',
}
</script>

<script setup lang="ts">
import { ref, watch, computed, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useFetch } from '@/composables/useFetch'
import { userApi } from '@/api'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import UserInfoTab from '@/components/profile/UserInfoTab.vue'
import LearningCareersTab from '@/components/profile/LearningCareersTab.vue'
import LearningCoursesTab from '@/components/profile/LearningCoursesTab.vue'
import StatsTab from '@/components/profile/StatsTab.vue'
import SubscriptionTab from '@/components/profile/SubscriptionTab.vue'
import FollowingTab from '@/components/profile/FollowingTab.vue'
import CatalogsTab from '@/components/profile/CatalogsTab.vue'
import ArticlesTab from '@/components/profile/ArticlesTab.vue'
import MemoryDecksTab from '@/components/profile/MemoryDecksTab.vue'
import RoadmapsTab from '@/components/profile/RoadmapsTab.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Tab 选择 - 支持路由参数
const activeTab = ref((route.query.tab as string) || 'careers')

// Tab 数据刷新时间记录（用于智能刷新）
const tabRefreshKey = ref(0)

// 使用 useFetch 获取用户信息
const {
  data: currentUser,
  loading: userLoading,
  execute: fetchUser,
} = useFetch({
  fetchFn: userApi.getCurrentUser,
  immediate: true,
})

// KeepAlive: 从其他页面返回时触发
onActivated(() => {
  console.log('ProfilePage activated')
  // 可选：检查数据是否需要刷新
  // 这里我们不做自动刷新，让各个Tab组件自己决定
  // 如果需要自动刷新，可以在这里触发
})

// 计算用户信息
const userInfo = computed(() => {
  if (!currentUser.value) {
    return {
      name: authStore.user?.name || '',
      email: authStore.user?.email || '',
      avatar: '',
      joinDate: '',
      bio: '',
    }
  }

  return {
    name: currentUser.value.name || '',
    email: currentUser.value.email || '',
    avatar: currentUser.value.avatar || '',
    joinDate: currentUser.value.createdAt
      ? new Date(currentUser.value.createdAt).toLocaleDateString('zh-CN')
      : '',
    bio: currentUser.value.biography || '',
  }
})

// 统计数据（暂时用默认值，各个Tab自己加载数据）
const stats = ref({
  totalCourses: 0,
  completedCourses: 0,
  totalCareers: 0,
  studyDays: 0,
  studyHours: 0,
  followers: 0,
  following: 0,
  articles: 0,
  roadmaps: 0,
})

// 监听路由变化
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && typeof newTab === 'string') {
      activeTab.value = newTab
    }
  }
)

// Tab 切换时更新 URL
watch(activeTab, (newTab) => {
  if (route.query.tab !== newTab) {
    router.push({ query: { tab: newTab } })
  }
})

// 更新用户信息后刷新
const handleUpdateUserInfo = async (updatedInfo: typeof userInfo.value) => {
  await fetchUser()
}
</script>

<template>
  <DefaultLayout>
    <div class="profile-container">
      <!-- 用户信息卡片 -->
      <v-card rounded="xl" class="profile-header-card mb-8 no-border" elevation="0">
        <v-card-text class="pa-8">
          <div class="d-flex align-start">
            <!-- 用户头像 -->
            <v-avatar size="96" color="primary" class="mr-6" rounded="xl">
              <v-icon icon="mdi-account" size="48" color="white" />
            </v-avatar>

            <!-- 用户信息 -->
            <div class="flex-grow-1">
              <div class="d-flex align-center mb-3">
                <h1 class="text-h4 font-weight-bold text-grey-darken-4 mr-4">
                  {{ userInfo.name }}
                </h1>
                <v-btn
                  color="grey-darken-2"
                  variant="outlined"
                  rounded="lg"
                  size="default"
                  @click="activeTab = 'info'"
                >
                  <v-icon icon="mdi-pencil" size="18" class="mr-2" />
                  编辑资料
                </v-btn>
              </div>

              <p class="text-body-1 text-grey-darken-2 mb-2">{{ userInfo.email }}</p>
              <p class="text-body-2 text-grey mb-4">加入于 {{ userInfo.joinDate }}</p>

              <!-- 统计信息 -->
              <div class="d-flex align-center flex-wrap ga-8">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-school" size="20" color="primary" class="mr-2" />
                  <span class="text-body-2 text-grey-darken-2">
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                      stats.totalCourses
                    }}</span>
                    学习课程
                  </span>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-check-circle" size="20" color="success" class="mr-2" />
                  <span class="text-body-2 text-grey-darken-2">
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                      stats.completedCourses
                    }}</span>
                    完成课程
                  </span>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-briefcase" size="20" color="info" class="mr-2" />
                  <span class="text-body-2 text-grey-darken-2">
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                      stats.totalCareers
                    }}</span>
                    关注职业
                  </span>
                </div>
                <div class="d-flex align-center">
                  <v-icon icon="mdi-calendar-check" size="20" color="warning" class="mr-2" />
                  <span class="text-body-2 text-grey-darken-2">
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                      stats.studyDays
                    }}</span>
                    学习天数
                  </span>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- Tab 导航 -->
      <div class="tabs-sticky">
        <v-tabs
          v-model="activeTab"
          color="primary"
          class="profile-tabs"
          height="56"
          density="comfortable"
        >
          <v-tab value="careers" rounded="lg">
            <v-icon icon="mdi-briefcase" size="18" class="mr-2" />
            学习的职业
          </v-tab>
          <v-tab value="courses-learning" rounded="lg">
            <v-icon icon="mdi-school" size="18" class="mr-2" />
            学习的课程
          </v-tab>
          <v-tab value="stats" rounded="lg">
            <v-icon icon="mdi-chart-line" size="18" class="mr-2" />
            数据统计
          </v-tab>
          <v-tab value="courses" rounded="lg">
            <v-icon icon="mdi-book-multiple" size="18" class="mr-2" />
            关注的课程
          </v-tab>
          <v-tab value="people" rounded="lg">
            <v-icon icon="mdi-account-multiple" size="18" class="mr-2" />
            关注的人
          </v-tab>
          <v-tab value="catalogs" rounded="lg">
            <v-icon icon="mdi-folder-multiple" size="18" class="mr-2" />
            创建的目录
          </v-tab>
          <v-tab value="articles" rounded="lg">
            <v-icon icon="mdi-file-document-multiple" size="18" class="mr-2" />
            创建的文章
          </v-tab>
          <v-tab value="decks" rounded="lg">
            <v-icon icon="mdi-cards" size="18" class="mr-2" />
            我的卡片组
          </v-tab>
          <v-tab value="roadmaps" rounded="lg">
            <v-icon icon="mdi-map-marker-path" size="18" class="mr-2" />
            创建的路线图
          </v-tab>
          <v-tab value="info" rounded="lg">
            <v-icon icon="mdi-account-circle" size="18" class="mr-2" />
            个人信息
          </v-tab>
        </v-tabs>
      </div>

      <!-- Tab 内容 -->
      <v-window v-model="activeTab">
        <!-- 个人信息 -->
        <v-window-item value="info">
          <UserInfoTab :user-info="userInfo" @update="handleUpdateUserInfo" />
        </v-window-item>

        <!-- 学习的职业 -->
        <v-window-item value="careers">
          <LearningCareersTab />
        </v-window-item>

        <!-- 学习的课程 -->
        <v-window-item value="courses-learning">
          <LearningCoursesTab />
        </v-window-item>

        <!-- 数据统计 -->
        <v-window-item value="stats">
          <StatsTab />
        </v-window-item>

        <!-- 关注的课程 -->
        <v-window-item value="courses">
          <SubscriptionTab />
        </v-window-item>

        <!-- 关注的人 -->
        <v-window-item value="people">
          <FollowingTab />
        </v-window-item>

        <!-- 创建的目录 -->
        <v-window-item value="catalogs">
          <CatalogsTab />
        </v-window-item>

        <!-- 创建的文章 -->
        <v-window-item value="articles">
          <ArticlesTab />
        </v-window-item>

        <!-- 我的卡片组 -->
        <v-window-item value="decks">
          <MemoryDecksTab />
        </v-window-item>

        <!-- 创建的路线图 -->
        <v-window-item value="roadmaps">
          <RoadmapsTab />
        </v-window-item>
      </v-window>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.profile-container {
  max-width: 1550px;
  margin: 0 auto;
}

/* 用户信息卡片 */
.profile-header-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 1px solid #e9ecef !important;
}

/* Tab 固定在顶部 */
.tabs-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 100;
  padding-bottom: 24px;
}

/* Tab 样式 */
.profile-tabs {
  background-color: transparent;
}

:deep(.v-tab) {
  color: rgba(0, 0, 0, 0.6) !important;
  font-size: 0.9rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  min-width: auto !important;
}

:deep(.v-tab--selected) {
  color: rgb(var(--v-theme-primary)) !important;
  font-weight: 600 !important;
}

:deep(.v-tab:hover) {
  color: rgba(0, 0, 0, 0.8) !important;
  background-color: rgba(var(--v-theme-primary), 0.05);
}

/* 移动端 */
@media (max-width: 960px) {
  .tabs-sticky {
    top: 56px;
  }
}
</style>
