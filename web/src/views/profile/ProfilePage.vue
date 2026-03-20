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

// 切换一级模式时，自动选择对应的第一个二级 Tab
watch(currentMode, (newMode) => {
  if (newMode === 'learner') {
    activeTab.value = 'roles'
  } else if (newMode === 'creator') {
    activeTab.value = isOwnProfile.value ? 'stats' : 'articles'
  } else if (newMode === 'settings') {
    activeTab.value = 'info'
  }
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
      <v-card rounded="xl" class="profile-header-card mb-6 mb-md-8 no-border" elevation="0">
        <v-card-text class="pt-1 pb-5 pb-sm-6 pb-md-8 px-0">
          <!-- 用户信息 - 新布局 -->
          <div class="d-flex flex-column flex-lg-row align-center justify-space-between ga-4 ga-md-6">
            <!-- 左侧：头像和信息 -->
            <div class="d-flex flex-column flex-sm-row align-center ga-4 ga-sm-6">
              <!-- 大头像 -->
              <UserAvatar
                :name="userInfo.name"
                :avatar-url="userInfo.avatar"
                :size="90"
                rounded="xl"
                class="flex-shrink-0"
              />

              <!-- 信息区 -->
              <div class="text-center text-sm-start">
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
                    @click="activeTab = 'info'; currentMode = 'learner'"
                  >
                    <v-icon icon="mdi-pencil" size="14" class="mr-1" />
                    编辑资料
                  </v-btn>
                </div>

                <!-- 个人简介 -->
                <p v-if="userInfo.bio" class="text-body-2 text-md-body-1 text-grey-darken-2 mb-3">
                  {{ userInfo.bio }}
                </p>
                <p v-else-if="isOwnProfile" class="text-body-2 text-grey mb-3">
                  点击编辑资料添加个人简介
                </p>

                <!-- 加入时间和邮箱 -->
                <div class="d-flex align-center justify-center justify-sm-start flex-wrap ga-4 text-caption text-md-body-2 text-grey-darken-1">
                  <span class="d-flex align-center">
                    <v-icon icon="mdi-calendar" size="16" class="mr-1" />
                    加入于 {{ userInfo.joinDate }}
                  </span>
                  <span v-if="isOwnProfile" class="d-flex align-center">
                    <v-icon icon="mdi-email-outline" size="16" class="mr-1" />
                    {{ userInfo.email }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 右侧：统计数据卡片 -->
            <div class="d-flex flex-nowrap justify-center justify-lg-end ga-3">
              <template v-if="currentMode === 'learner'">
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ stats.totalCourses }}</div>
                  <div class="text-caption text-grey-darken-1">学习课程</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ stats.completedCourses }}</div>
                  <div class="text-caption text-grey-darken-1">完成课程</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ stats.totalRoles }}</div>
                  <div class="text-caption text-grey-darken-1">关注职业</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ stats.studyDays }}</div>
                  <div class="text-caption text-grey-darken-1">学习天数</div>
                </div>
              </template>
              <template v-else>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ creatorStats.articles }}</div>
                  <div class="text-caption text-grey-darken-1">创建文章</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ creatorStats.catalogs }}</div>
                  <div class="text-caption text-grey-darken-1">创建目录</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ creatorStats.roadmaps }}</div>
                  <div class="text-caption text-grey-darken-1">创建路线</div>
                </div>
                <div class="stat-item text-center px-4">
                  <div class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ creatorStats.decks }}</div>
                  <div class="text-caption text-grey-darken-1">卡片组</div>
                </div>
              </template>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- 一级分类：按钮组样式 -->
      <div class="tabs-sticky">
        <div class="d-flex align-center mb-5 ga-2">
          <v-btn
            :color="currentMode === 'learner' ? 'primary' : undefined"
            :variant="currentMode === 'learner' ? 'flat' : 'text'"
            rounded="lg"
            size="default"
            class="primary-mode-btn"
            @click="currentMode = 'learner'"
          >
            <v-icon icon="mdi-school" size="18" class="mr-1" />
            学习
          </v-btn>
          <v-btn
            :color="currentMode === 'creator' ? 'primary' : undefined"
            :variant="currentMode === 'creator' ? 'flat' : 'text'"
            rounded="lg"
            size="default"
            class="primary-mode-btn"
            @click="currentMode = 'creator'"
          >
            <v-icon icon="mdi-pen" size="18" class="mr-1" />
            创作
          </v-btn>
          <v-btn
            v-if="isOwnProfile"
            :color="currentMode === 'settings' ? 'primary' : undefined"
            :variant="currentMode === 'settings' ? 'flat' : 'text'"
            rounded="lg"
            size="default"
            class="primary-mode-btn"
            @click="currentMode = 'settings'"
          >
            <v-icon icon="mdi-cog" size="18" class="mr-1" />
            设置
          </v-btn>
        </div>

        <!-- 二级 Tab：学习模式 -->
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

        <!-- 二级 Tab：创作模式 -->
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
      </div>

      <!-- Tab 内容 -->
      <v-window v-model="activeTab">
        <!-- 学习模式的内容 -->
        <template v-if="currentMode === 'learner'">
          <v-window-item value="roles">
            <LearningRolesTab />
          </v-window-item>
          <v-window-item value="courses-learning">
            <LearningCoursesTab />
          </v-window-item>
          <v-window-item value="people">
            <FollowingTab />
          </v-window-item>
          <v-window-item v-if="isOwnProfile" value="bookmarks">
            <BookmarksTab />
          </v-window-item>
        </template>

        <!-- 创作模式的内容 -->
        <template v-else-if="currentMode === 'creator'">
          <v-window-item v-if="isOwnProfile" value="stats">
            <CreatorStatsTab />
          </v-window-item>
          <v-window-item value="articles">
            <ArticlesTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </v-window-item>
          <v-window-item value="catalogs">
            <CatalogsTab />
          </v-window-item>
          <v-window-item value="roadmaps">
            <RoadmapsTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </v-window-item>
          <v-window-item value="decks">
            <MemoryDecksTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </v-window-item>
        </template>

        <!-- 设置模式的内容 -->
        <template v-else-if="currentMode === 'settings'">
          <v-window-item value="info">
            <UserInfoTab :user-info="userInfo" @update-avatar="handleUpdateAvatar" />
          </v-window-item>
        </template>
      </v-window>
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

/* 统计项 */
.stat-item {
  min-width: 80px;
}

/* Tab 固定在顶部 */
.tabs-sticky {
  position: sticky;
  top: 56px;
  background-color: rgb(var(--v-theme-background));
  z-index: 100;
  padding-bottom: 16px;
}

/* 一级按钮样式 */
.primary-mode-btn {
  font-weight: 500 !important;
  letter-spacing: 0 !important;
}

.primary-mode-btn:not(.v-btn--variant-flat) {
  color: rgb(var(--v-theme-on-surface-variant)) !important;
}

/* 二级 Tab 样式 */
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

/* 移动端 */
@media (max-width: 960px) {
  .tabs-sticky {
    top: 56px;
  }
}
</style>