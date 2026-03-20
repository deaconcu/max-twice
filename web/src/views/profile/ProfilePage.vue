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
import { userApi } from '@/api'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import UserInfoTab from '@/components/profile/UserInfoTab.vue'
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

// Tab 选择 - 支持路由参数，没有则让 v-tabs 使用默认值（第一个）
const activeTab = ref((route.query.tab as string) || '')

// Tab 数据刷新时间记录（用于智能刷新）
const tabRefreshKey = ref(0)

// 判断是否查看自己的资料
const isOwnProfile = computed(() => {
  return props.id === 'me' || (userStore.currentUser && props.id === userStore.currentUser.name)
})

// 根据ID获取用户信息
const {
  data: profileUser,
  loading: userLoading,
  execute: fetchUser,
} = useFetch({
  fetchFn: () => {
    if (props.id === 'me') {
      return userApi.getCurrentUser()
    } else {
      // 假设props.id是用户名，使用现有的getUser方法
      return userApi.getUser(props.id)
    }
  },
  immediate: true,
})

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

// 统计数据（暂时用默认值，各个Tab自己加载数据）
const stats = ref({
  totalCourses: 0,
  completedCourses: 0,
  totalRoles: 0,
  studyDays: 0,
  studyHours: 0,
  followers: 0,
  following: 0,
  articles: 0,
  roadmaps: 0,
})

// 创作者统计数据
const creatorStats = ref({
  articles: 0,
  catalogs: 0,
  roadmaps: 0,
  decks: 0,
})

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
</script>

<template>
  <DefaultLayout>
    <!-- 加载状态 -->
    <LoadingSpinner v-if="userLoading" />

    <!-- 内容区 -->
    <div v-else-if="profileUser" class="profile-container">
      <!-- 用户信息卡片 -->
      <v-card rounded="xl" class="profile-header-card mb-2 no-border" elevation="0">
        <v-card-text class="pt-1 pb-6 px-0">
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

            <!-- 右侧：统计信息 -->
            <div class="profile-stats">
              <!-- 学习统计 -->
              <div class="stats-group">
                <div class="stats-label">学习</div>
                <div class="stats-items">
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ stats.totalCourses }}</div>
                    <div class="text-caption text-grey">学习课程</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ stats.completedCourses }}</div>
                    <div class="text-caption text-grey">完成课程</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ stats.totalRoles }}</div>
                    <div class="text-caption text-grey">关注职业</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ stats.studyDays }}</div>
                    <div class="text-caption text-grey">学习天数</div>
                  </div>
                </div>
              </div>

              <!-- 分隔符 - 学习和创作之间 -->
              <v-divider vertical class="stats-divider" />

              <!-- 创作统计 -->
              <div class="stats-group">
                <div class="stats-label">创作</div>
                <div class="stats-items">
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ creatorStats.articles }}</div>
                    <div class="text-caption text-grey">文章</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ creatorStats.catalogs }}</div>
                    <div class="text-caption text-grey">目录</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ creatorStats.roadmaps }}</div>
                    <div class="text-caption text-grey">路线图</div>
                  </div>
                  <div class="text-center">
                    <div class="text-h6 font-weight-bold text-grey-darken-1">{{ creatorStats.decks }}</div>
                    <div class="text-caption text-grey">卡片组</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- 主内容区：左侧导航 + 右侧内容 -->
      <v-row>
        <!-- 左侧垂直导航 - 大屏显示 -->
        <v-col cols="12" md="3" lg="2" class="d-none d-md-block">
          <div class="side-nav">
            <!-- 学习分组 -->
            <div class="nav-group-title">学习</div>
            <div
              class="nav-item"
              :class="{ 'nav-item-active': activeTab === 'roles' }"
              @click="activeTab = 'roles'; currentMode = 'learner'"
            >
              学习的职业
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
              我的卡片组
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
              :color="currentMode === 'learner' ? 'primary' : undefined"
              :variant="currentMode === 'learner' ? 'flat' : 'text'"
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
            v-if="currentMode === 'learner'"
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
        <v-col cols="12" md="9" lg="10">
          <!-- 学习模式的内容 -->
          <template v-if="currentMode === 'learner'">
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
.profile-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-shrink: 0;
}

.stats-group {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stats-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface-variant));
}

.stats-items {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stats-divider {
  height: 40px;
  align-self: center;
}

/* 中屏：上下布局，但统计区域保持一行 */
@media (max-width: 1264px) {
  .profile-header-content {
    flex-direction: column;
    align-items: center;
  }

  .profile-stats {
    justify-content: center;
  }

  .stats-group {
    gap: 16px;
  }

  .stats-items {
    gap: 16px;
  }
}

/* 更小屏：统计区域改为纯文字紧凑布局 */
@media (max-width: 900px) {
  .profile-stats {
    flex-direction: column;
    align-items: center;
    gap: 6px;
    width: 100%;
  }

  .stats-divider {
    display: none;
  }

  .stats-group {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .stats-label {
    font-size: 0.75rem;
  }

  .stats-items {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .stats-items .text-center {
    display: flex;
    flex-direction: row-reverse;
    align-items: center;
    gap: 4px;
  }

  .stats-items .text-h6 {
    font-size: 0.875rem !important;
    font-weight: 600 !important;
  }

  .stats-items .text-caption {
    font-size: 0.75rem !important;
  }
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

  .stats-group {
    gap: 12px;
  }

  .stats-items {
    gap: 12px;
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