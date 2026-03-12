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
import SubscriptionTab from '@/components/profile/SubscriptionTab.vue'
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

// 模式切换函数
const switchMode = (mode: 'learner' | 'creator') => {
  currentMode.value = mode
}

// 监听路由变化
watch(
  () => [route.query.tab, route.query.mode],
  ([newTab, newMode]) => {
    if (newMode && typeof newMode === 'string') {
      currentMode.value = newMode as 'learner' | 'creator'
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
        <v-card-text class="pa-4 pa-sm-6 pa-md-8">
          <!-- 用户信息 -->
          <div>
            <!-- 头像和用户名一行 -->
            <div class="d-flex align-center mb-3">
              <UserAvatar
                :name="userInfo.name"
                :avatar-url="userInfo.avatar"
                :size="$vuetify.display.mobile ? 40 : 48"
                rounded="lg"
                class="mr-4"
              />
              <div class="d-flex align-center flex-wrap ga-3">
                <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4">
                  {{ userInfo.name }}
                </h1>
                <v-btn
                  v-if="currentMode === 'learner' && isOwnProfile"
                  color="grey-darken-2"
                  variant="outlined"
                  rounded="lg"
                  size="small"
                  @click="activeTab = 'info'"
                >
                  <v-icon icon="mdi-pencil" size="16" class="mr-1" />
                  编辑资料
                </v-btn>
              </div>
            </div>

            <!-- 其他信息靠左 -->
            <p class="text-body-2 text-md-body-1 text-grey-darken-2 mb-1 mb-md-2">
              {{ userInfo.email }}
            </p>
            <p class="text-caption text-md-body-2 text-grey mb-3 mb-md-4">
              加入于 {{ userInfo.joinDate }}
            </p>

            <!-- 统计信息 -->
            <div class="d-flex align-center flex-wrap ga-4 ga-md-8">
                <!-- 学习者模式统计 -->
                <template v-if="currentMode === 'learner'">
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-school"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="primary"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        stats.totalCourses
                      }}</span>
                      学习课程
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-check-circle"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="success"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        stats.completedCourses
                      }}</span>
                      完成课程
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-briefcase"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="info"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        stats.totalRoles
                      }}</span>
                      关注职业
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-calendar-check"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="warning"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        stats.studyDays
                      }}</span>
                      学习天数
                    </span>
                  </div>
                </template>

                <!-- 创作者模式统计 -->
                <template v-else>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-file-document-multiple"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="primary"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        creatorStats.articles
                      }}</span>
                      创建文章
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-folder-multiple"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="success"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        creatorStats.catalogs
                      }}</span>
                      创建目录
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-map-marker-path"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="info"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                        creatorStats.roadmaps
                      }}</span>
                      创建路线
                    </span>
                  </div>
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-cards"
                      :size="$vuetify.display.mobile ? 18 : 20"
                      color="warning"
                      class="mr-2"
                    />
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <span class="font-weight-bold text-grey-darken-4 mr-1">{{ creatorStats.decks }}</span>
                      卡片组
                    </span>
                  </div>
                </template>
              </div>
            </div>
        </v-card-text>
      </v-card>

      <!-- 模式切换栏 -->
      <div class="mode-switcher-bar d-flex align-center mb-4 mb-md-6">
        <h2 class="text-h6 text-md-h5 font-weight-bold text-grey-darken-3 d-flex align-center">
          <v-icon
            :icon="currentMode === 'learner' ? 'mdi-school' : 'mdi-pen'"
            class="mr-2"
            color="primary"
          />
          {{ currentMode === 'learner' ? '学习者' : '创作者' }}
        </h2>
        <v-btn
          variant="text"
          size="default"
          class="text-body-2 font-weight-medium ml-3"
          @click="switchMode(currentMode === 'learner' ? 'creator' : 'learner')"
        >
          切换到{{ currentMode === 'learner' ? '创作者' : '学习者' }}
          <v-icon icon="mdi-chevron-right" size="16" class="ml-1" />
        </v-btn>
      </div>

      <!-- Tab 导航 -->
      <div class="tabs-sticky">
        <!-- 学习者模式的 tabs -->
        <v-tabs
          v-if="currentMode === 'learner'"
          v-model="activeTab"
          color="primary"
          class="profile-tabs"
          :height="$vuetify.display.mobile ? 48 : 56"
          density="comfortable"
          show-arrows
        >
          <v-tab value="roles" rounded="lg">
            <v-icon
              icon="mdi-briefcase"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">学习的职业</span>
            <span class="d-sm-none">职业</span>
          </v-tab>
          <v-tab value="courses-learning" rounded="lg">
            <v-icon
              icon="mdi-school"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">学习的课程</span>
            <span class="d-sm-none">课程</span>
          </v-tab>
          <v-tab value="courses" rounded="lg">
            <v-icon
              icon="mdi-book-multiple"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">关注的课程</span>
            <span class="d-sm-none">关注</span>
          </v-tab>
          <v-tab v-if="isOwnProfile" value="bookmarks" rounded="lg">
            <v-icon
              icon="mdi-bookmark"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">我的收藏</span>
            <span class="d-sm-none">收藏</span>
          </v-tab>
          <v-tab value="people" rounded="lg">
            <v-icon
              icon="mdi-account-multiple"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">关注的人</span>
            <span class="d-sm-none">好友</span>
          </v-tab>
          <v-tab v-if="isOwnProfile" value="info" rounded="lg">
            <v-icon
              icon="mdi-account-circle"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">个人信息</span>
            <span class="d-sm-none">资料</span>
          </v-tab>
        </v-tabs>

        <!-- 创作者模式的 tabs -->
        <v-tabs
          v-else
          v-model="activeTab"
          color="primary"
          class="profile-tabs"
          :height="$vuetify.display.mobile ? 48 : 56"
          density="comfortable"
          show-arrows
        >
          <v-tab v-if="isOwnProfile" value="stats" rounded="lg">
            <v-icon
              icon="mdi-chart-line"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">创作统计</span>
            <span class="d-sm-none">统计</span>
          </v-tab>
          <v-tab value="articles" rounded="lg">
            <v-icon
              icon="mdi-file-document-multiple"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">创建的文章</span>
            <span class="d-sm-none">文章</span>
          </v-tab>
          <v-tab value="catalogs" rounded="lg">
            <v-icon
              icon="mdi-folder-multiple"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">创建的目录</span>
            <span class="d-sm-none">目录</span>
          </v-tab>
          <v-tab value="roadmaps" rounded="lg">
            <v-icon
              icon="mdi-map-marker-path"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">创建的路线图</span>
            <span class="d-sm-none">路线</span>
          </v-tab>
          <v-tab value="decks" rounded="lg">
            <v-icon
              icon="mdi-cards"
              :size="$vuetify.display.mobile ? 16 : 18"
              :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
            />
            <span class="d-none d-sm-inline">我的卡片组</span>
            <span class="d-sm-none">卡片</span>
          </v-tab>
        </v-tabs>
      </div>

      <!-- Tab 内容 -->
      <v-window v-model="activeTab">
        <!-- 学习者模式的内容 -->
        <template v-if="currentMode === 'learner'">
          <!-- 学习的职业 -->
          <v-window-item value="roles">
            <LearningRolesTab />
          </v-window-item>

          <!-- 学习的课程 -->
          <v-window-item value="courses-learning">
            <LearningCoursesTab />
          </v-window-item>

          <!-- 关注的课程 -->
          <v-window-item value="courses">
            <SubscriptionTab />
          </v-window-item>

          <!-- 关注的人 -->
          <v-window-item value="people">
            <FollowingTab />
          </v-window-item>

          <!-- 我的收藏 -->
          <v-window-item v-if="isOwnProfile" value="bookmarks">
            <BookmarksTab />
          </v-window-item>

          <!-- 个人信息 -->
          <v-window-item v-if="isOwnProfile" value="info">
            <UserInfoTab :user-info="userInfo" @update-avatar="handleUpdateAvatar" />
          </v-window-item>
        </template>

        <!-- 创作者模式的内容 -->
        <template v-else>
          <!-- 创作统计 -->
          <v-window-item v-if="isOwnProfile" value="stats">
            <CreatorStatsTab />
          </v-window-item>

          <!-- 创建的文章 -->
          <v-window-item value="articles">
            <ArticlesTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </v-window-item>

          <!-- 创建的目录 -->
          <v-window-item value="catalogs">
            <CatalogsTab />
          </v-window-item>

          <!-- 创建的路线图 -->
          <v-window-item value="roadmaps">
            <RoadmapsTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
          </v-window-item>

          <!-- 我的卡片组 -->
          <v-window-item value="decks">
            <MemoryDecksTab :user-id="profileUser?.id" :is-own-profile="isOwnProfile" />
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
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

/* Tab 固定在顶部 */
.tabs-sticky {
  position: sticky;
  top: 56px;
  background-color: rgb(var(--v-theme-background));
  z-index: 100;
  padding-bottom: 16px;
}

@media (min-width: 600px) {
  .tabs-sticky {
    padding-bottom: 24px;
  }
}

/* Tab 样式 */
.profile-tabs {
  background-color: transparent;
}

:deep(.v-tab) {
  color: rgb(var(--v-theme-on-surface-variant)) !important;
  font-size: 0.875rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  min-width: auto !important;
}

@media (min-width: 600px) {
  :deep(.v-tab) {
    font-size: 0.9rem !important;
  }
}

:deep(.v-tab--selected) {
  color: rgb(var(--v-theme-primary)) !important;
  font-weight: 600 !important;
}

:deep(.v-tab:hover) {
  color: rgb(var(--v-theme-on-surface)) !important;
  background-color: rgba(var(--v-theme-primary), 0.05);
}

/* 移动端 */
@media (max-width: 960px) {
  .tabs-sticky {
    top: 56px;
  }
}
</style>
