<script lang="ts">
export default {
  name: 'ProfilePage',
}
</script>

<script setup lang="ts">
import { ref, watch, computed, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useUserStore } from '@/stores/modules/user'
import { useFetch } from '@/composables/useFetch'
import { userApi, statsApi } from '@/api'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ActivityHeatmap from '@/components/profile/ActivityHeatmap.vue'
import UserInfoTab from '@/components/profile/UserInfoTab.vue'
import OverviewTab from '@/components/profile/OverviewTab.vue'
import LearningRolesTab from '@/components/profile/LearningRolesTab.vue'
import LearningCoursesTab from '@/components/profile/LearningCoursesTab.vue'
import CreatorStatsTab from '@/components/profile/CreatorStatsTab.vue'
import FollowingTab from '@/components/profile/FollowingTab.vue'
import CatalogsTab from '@/components/profile/CatalogsTab.vue'
import ArticlesTab from '@/components/profile/ArticlesTab.vue'
import MemoryDecksTab from '@/components/profile/MemoryDecksTab.vue'
import RoadmapsTab from '@/components/profile/RoadmapsTab.vue'
import BookmarksTab from '@/components/profile/BookmarksTab.vue'

interface Props {
  id: string
}

const props = defineProps<Props>()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

// 模式选择：learner 或 creator
const currentMode = ref((route.query.mode as string) || 'learner')

// Tab 选择 - 默认为 overview
const activeTab = ref((route.query.tab as string) || 'overview')

// Tab 数据刷新时间记录（用于智能刷新）
const tabRefreshKey = ref(0)

// 判断是否查看自己的资料
const isOwnProfile = computed(() => {
  return props.id === 'me' || (userStore.currentUser && props.id === userStore.currentUser.name)
})

// 根据ID获取用户信息（如果是自己且 store 有值，则不调用接口）
const {
  data: profileUser,
  loading: userLoading,
  execute: fetchUser,
} = useFetch({
  fetchFn: () => {
    if (props.id === 'me') {
      return userApi.getCurrentUser()
    } else {
      return userApi.getUser(props.id)
    }
  },
  immediate: false,
  defaultValue: isOwnProfile.value && userStore.currentUser ? userStore.currentUser : null,
})

// 只有在需要时才调用接口
if (!isOwnProfile.value || !userStore.currentUser) {
  fetchUser()
}

// KeepAlive: 从其他页面返回时触发
onActivated(() => {
  console.log('ProfilePage activated')
  // 可选：检查数据是否需要刷新
  // 这里我们不做自动刷新，让各个Tab组件自己决定
  // 如果需要自动刷新，可以在这里触发
})

// 计算用户信息 - 优先使用 profileUser
const userInfo = computed(() => {
  const user = profileUser.value || (isOwnProfile.value ? userStore.currentUser : null)

  if (!user) {
    return {
      name: '',
      email: '',
      avatar: '',
      joinDate: '',
      bio: '',
      timezone: '',
    }
  }

  return {
    name: user.name || '',
    email: user.email || '',
    avatar: user.avatar || '',
    joinDate: user.createdAt
      ? new Date(user.createdAt).toLocaleDateString('zh-CN')
      : '',
    bio: user.biography || '',
    timezone: user.timezone || '',
  }
})

// 更新头像
const handleUpdateAvatar = (avatarUrl: string) => {
  if (profileUser.value) {
    profileUser.value = {
      ...profileUser.value,
      avatar: avatarUrl,
    }
  }
  // 同时更新 userStore，确保 header 头像同步
  userStore.updateUser({ avatar: avatarUrl })
}

// 获取当前页面对应的用户ID（优先从 userStore 取，避免等待接口）
const currentUserId = computed(() => {
  if (isOwnProfile.value && userStore.currentUser?.id) {
    return userStore.currentUser.id
  }
  return profileUser.value?.id ?? null
})

// 获取用户统计数据
const { data: userStats, execute: fetchUserStats } = useFetch({
  fetchFn: () => {
    const userId = currentUserId.value
    if (!userId) return Promise.resolve(null)
    return statsApi.getUserAllTimeStats(userId)
  },
  immediate: false,
  defaultValue: null,
})

// 监听用户ID变化，加载统计数据
watch(
  currentUserId,
  (userId) => {
    if (userId) {
      fetchUserStats()
    }
  },
  { immediate: true }
)

// 学习统计数据（从 userStats 计算）
const stats = computed(() => ({
  totalCourses: (userStats.value?.learningCourseCount || 0) + (userStats.value?.completedCourseCount || 0),
  totalRoles: (userStats.value?.inProgressProfessionCount || 0) + (userStats.value?.completedProfessionCount || 0),
  studyDays: userStats.value?.learningStreakDays || 0,
  reviewDays: userStats.value?.reviewStreakDays || 0,
  following: userStats.value?.followingUserCount || 0,
}))

// 创作者统计数据（从 userStats 计算）
const creatorStats = computed(() => ({
  articles: userStats.value?.createdArticleCount || 0,
  catalogs: userStats.value?.createdIndexCount || 0,
  roadmaps: userStats.value?.createdRoadmapCount || 0,
  decks: userStats.value?.createdCardDeckCount || 0,
}))

// 监听路由变化
watch(
  () => [route.query.tab, route.query.mode],
  ([newTab, newMode]) => {
    if (newMode && typeof newMode === 'string') {
      currentMode.value = newMode as 'learner' | 'creator' | 'settings'
    }
    if (newTab && typeof newTab === 'string') {
      activeTab.value = newTab
    }
  }
)

// Tab 切换时更新 URL
watch(activeTab, (newTab) => {
  if (route.query.tab !== newTab || route.query.mode !== currentMode.value) {
    router.push({ query: { mode: currentMode.value, tab: newTab } })
  }
})

// 处理从 OverviewTab 导航
const handleNavigate = (tab: string, mode: string) => {
  activeTab.value = tab
  currentMode.value = mode
}
</script>

<template>
  <DefaultLayout>
    <!-- 加载状态 -->
    <LoadingSpinner v-if="userLoading" />

    <!-- 内容区 -->
    <div v-else-if="profileUser" class="profile-container">
      <!-- 用户信息卡片 -->
      <v-card rounded="xl" class="profile-header-card mb-2 no-border" elevation="0">
        <v-card-text class="pt-1 pb-6 px-2">
          <!-- 用户信息 -->
          <div class="profile-header-content">
            <!-- 左侧：头像和基本信息 -->
            <div class="profile-user-info">
              <!-- 头像 -->
              <UserAvatar
                :name="userInfo.name"
                :avatar-url="userInfo.avatar"
                :size="90"
                rounded="xl"
                class="flex-shrink-0"
              />

              <!-- 信息区 -->
              <div class="user-details">
                <!-- 用户名和编辑按钮 -->
                <div class="d-flex align-center justify-center justify-sm-start flex-wrap ga-3 mb-2">
                  <h1 class="text-h5 font-weight-bold text-grey-darken-4">
                    {{ userInfo.name }}
                  </h1>
                  <v-btn
                    v-if="isOwnProfile"
                    color="grey-darken-1"
                    variant="tonal"
                    rounded="lg"
                    size="small"
                    @click="currentMode = 'settings'"
                  >
                    <v-icon icon="mdi-pencil" size="14" class="mr-1" />
                    编辑资料
                  </v-btn>
                </div>

                <!-- 个人简介 -->
                <p v-if="userInfo.bio" class="text-body-2 text-grey-darken-2 mb-3">
                  {{ userInfo.bio }}
                </p>
                <p v-else-if="isOwnProfile" class="text-body-2 text-grey mb-3">
                  点击编辑资料添加个人简介
                </p>

                <!-- 加入时间 -->
                <div class="text-caption text-grey-darken-1">
                  <v-icon icon="mdi-calendar" size="14" class="mr-1" />
                  加入于 {{ userInfo.joinDate }}
                </div>
              </div>
            </div>

            <!-- 右侧：热力图 -->
            <div class="profile-heatmap d-none d-lg-block">
              <ActivityHeatmap :months="12" />
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- 主内容区：左侧导航 + 右侧内容 -->
      <v-row>
        <!-- 左侧垂直导航 - 大屏显示 -->
        <v-col cols="12" md="2" lg="2" class="d-none d-md-block pr-16">
          <div class="side-nav">
            <!-- 概览 -->
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'overview' }"
              @click="activeTab = 'overview'; currentMode = 'learner'"
            >
              <v-icon icon="mdi-view-dashboard-outline" size="18" class="mr-2" />
              概览
            </div>

            <!-- 学习分组 -->
            <div class="nav-group-title mt-4">学习</div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'roles' }"
              @click="activeTab = 'roles'; currentMode = 'learner'"
            >
              学习的职业路线
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'courses-learning' }"
              @click="activeTab = 'courses-learning'; currentMode = 'learner'"
            >
              学习的课程
            </div>
            <div
              v-if="isOwnProfile"
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'bookmarks' }"
              @click="activeTab = 'bookmarks'; currentMode = 'learner'"
            >
              我的收藏
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'people' }"
              @click="activeTab = 'people'; currentMode = 'learner'"
            >
              关注的人
            </div>

            <!-- 创作分组 -->
            <div class="nav-group-title mt-4">创作</div>
            <div
              v-if="isOwnProfile"
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'stats' }"
              @click="activeTab = 'stats'; currentMode = 'creator'"
            >
              创作统计
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'articles' }"
              @click="activeTab = 'articles'; currentMode = 'creator'"
            >
              创建的文章
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'catalogs' }"
              @click="activeTab = 'catalogs'; currentMode = 'creator'"
            >
              创建的目录
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'roadmaps' }"
              @click="activeTab = 'roadmaps'; currentMode = 'creator'"
            >
              创建的路线图
            </div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'decks' }"
              @click="activeTab = 'decks'; currentMode = 'creator'"
            >
              创建的卡片组
            </div>

            <!-- 设置 -->
            <div v-if="isOwnProfile" class="nav-group-title mt-4">设置</div>
            <div
              v-if="isOwnProfile"
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'info' }"
              @click="activeTab = 'info'; currentMode = 'settings'"
            >
              个人信息
            </div>
          </div>
        </v-col>

        <!-- 移动端：一级分类 + 二级 Tab -->
        <v-col cols="12" class="d-md-none pb-4">
          <!-- 一级分类按钮 -->
          <div class="d-flex align-center mb-3 ga-2">
            <v-btn
              :color="activeTab === 'overview' ? 'primary' : undefined"
              :variant="activeTab === 'overview' ? 'flat' : 'text'"
              rounded="lg"
              size="small"
              class="primary-mode-btn"
              @click="activeTab = 'overview'; currentMode = 'learner'"
            >
              概览
            </v-btn>
            <v-btn
              :color="currentMode === 'learner' && activeTab !== 'overview' ? 'primary' : undefined"
              :variant="currentMode === 'learner' && activeTab !== 'overview' ? 'flat' : 'text'"
              rounded="lg"
              size="small"
              class="primary-mode-btn"
              @click="activeTab = 'roles'; currentMode = 'learner'"
            >
              学习
            </v-btn>
            <v-btn
              :color="currentMode === 'creator' ? 'primary' : undefined"
              :variant="currentMode === 'creator' ? 'flat' : 'text'"
              rounded="lg"
              size="small"
              class="primary-mode-btn"
              @click="activeTab = isOwnProfile ? 'stats' : 'articles'; currentMode = 'creator'"
            >
              创作
            </v-btn>
            <v-btn
              v-if="isOwnProfile"
              :color="currentMode === 'settings' ? 'primary' : undefined"
              :variant="currentMode === 'settings' ? 'flat' : 'text'"
              rounded="lg"
              size="small"
              class="primary-mode-btn"
              @click="activeTab = 'info'; currentMode = 'settings'"
            >
              设置
            </v-btn>
          </div>

          <!-- 二级 Tab -->
          <v-tabs
            v-if="currentMode === 'learner' && activeTab !== 'overview'"
            v-model="activeTab"
            color="primary"
            class="profile-tabs"
            height="36"
            density="compact"
            show-arrows
          >
            <v-tab value="roles">职业</v-tab>
            <v-tab value="courses-learning">课程</v-tab>
            <v-tab v-if="isOwnProfile" value="bookmarks">收藏</v-tab>
            <v-tab value="people">好友</v-tab>
          </v-tabs>
          <v-tabs
            v-else-if="currentMode === 'creator'"
            v-model="activeTab"
            color="primary"
            class="profile-tabs"
            height="36"
            density="compact"
            show-arrows
          >
            <v-tab v-if="isOwnProfile" value="stats">统计</v-tab>
            <v-tab value="articles">文章</v-tab>
            <v-tab value="catalogs">目录</v-tab>
            <v-tab value="roadmaps">路线图</v-tab>
            <v-tab value="decks">卡片组</v-tab>
          </v-tabs>
        </v-col>

        <!-- 右侧内容区 -->
        <v-col cols="12" md="10" lg="10">
          <!-- 概览页 -->
          <OverviewTab
            v-if="activeTab === 'overview'"
            :user-stats="userStats"
            :is-own-profile="isOwnProfile"
            @navigate="handleNavigate"
          />

          <!-- 学习模式的内容 -->
          <template v-else-if="currentMode === 'learner'">
            <LearningRolesTab v-if="activeTab === 'roles'" />
            <LearningCoursesTab v-else-if="activeTab === 'courses-learning'" />
            <BookmarksTab v-else-if="activeTab === 'bookmarks' && isOwnProfile" />
            <FollowingTab v-else-if="activeTab === 'people'" />
          </template>

          <!-- 创作模式的内容 -->
          <template v-else-if="currentMode === 'creator'">
            <CreatorStatsTab v-if="activeTab === 'stats' && isOwnProfile" />
            <ArticlesTab v-else-if="activeTab === 'articles'" :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
            <CatalogsTab v-else-if="activeTab === 'catalogs'" />
            <RoadmapsTab v-else-if="activeTab === 'roadmaps'" :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
            <MemoryDecksTab v-else-if="activeTab === 'decks'" :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </template>

          <!-- 设置模式的内容 -->
          <template v-else-if="currentMode === 'settings'">
            <UserInfoTab :user-info="userInfo" @update-avatar="handleUpdateAvatar" />
          </template>
        </v-col>
      </v-row>
    </div>
  </DefaultLayout>
</template>

<style scoped>
.profile-container {
  max-width: 1550px;
  margin: 0 auto;
  padding-top: 24px;
}

@media (max-width: 960px) {
  .profile-container {
    padding-top: 16px;
  }
}

/* 用户信息卡片 */
.profile-header-card {
  background: rgb(var(--v-theme-surface));
}

/* Header 布局 */
.profile-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.profile-user-info {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  flex-shrink: 0;
}

.user-details {
  text-align: left;
}

/* 统计区域 */
.profile-heatmap {
  flex-shrink: 0;
}

/* 小屏：头像和信息上下排列 */
@media (max-width: 600px) {
  .profile-user-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-details {
    text-align: left;
  }
}

/* 一级按钮样式 */
.primary-mode-btn {
  font-weight: 500 !important;
  letter-spacing: 0 !important;
}

.primary-mode-btn:not(.v-btn--variant-flat) {
  color: rgb(var(--v-theme-on-surface-variant)) !important;
}

/* 左侧导航 */
.side-nav {
  position: sticky;
  top: 80px;
}

.nav-group-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface-variant));
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 8px 12px 4px;
}

.nav-item {
  padding: 8px 12px;
  margin-bottom: 2px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  color: rgb(var(--v-theme-on-surface-variant));
  transition: all 0.15s ease;
}

.nav-item:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.05);
  color: rgb(var(--v-theme-on-surface));
}

.nav-item-active {
  background-color: rgba(var(--v-theme-primary), 0.1);
  color: rgb(var(--v-theme-primary));
  font-weight: 500;
}

/* 移动端 Tab 样式 */
.profile-tabs {
  background-color: transparent;
}

.profile-tabs :deep(.v-tab) {
  color: rgb(var(--v-theme-on-surface-variant)) !important;
  font-size: 0.85rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  min-width: auto !important;
  padding: 0 12px !important;
  letter-spacing: 0 !important;
}

.profile-tabs :deep(.v-tab--selected) {
  color: rgb(var(--v-theme-primary)) !important;
  font-weight: 600 !important;
}

.profile-tabs :deep(.v-tab:hover) {
  color: rgb(var(--v-theme-on-surface)) !important;
}
</style>