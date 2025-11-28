<script lang="ts">
export default {
  name: 'CreatorPage',
}
</script>

<script setup lang="ts">
import { ref, watch, computed, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useFetch } from '@/composables/useFetch'
import { userApi } from '@/api'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import UserInfoTab from '@/components/profile/UserInfoTab.vue'
import StatsTab from '@/components/profile/StatsTab.vue'
import CatalogsTab from '@/components/profile/CatalogsTab.vue'
import ArticlesTab from '@/components/profile/ArticlesTab.vue'
import MemoryDecksTab from '@/components/profile/MemoryDecksTab.vue'
import RoadmapsTab from '@/components/profile/RoadmapsTab.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Tab 选择 - 支持路由参数，默认显示统计
const activeTab = ref((route.query.tab as string) || 'stats')

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
  console.log('CreatorPage activated')
})

// 计算用户信息 - 优先使用 authStore 的数据
const userInfo = computed(() => {
  const user = currentUser.value || authStore.user

  if (!user) {
    return {
      name: '',
      email: '',
      avatar: '',
      joinDate: '',
      bio: '',
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
  }
})

// 统计数据（暂时用默认值，各个Tab自己加载数据）
const stats = ref({
  articles: 0,
  catalogs: 0,
  roadmaps: 0,
  decks: 0,
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
    <!-- 加载状态 -->
    <LoadingSpinner v-if="userLoading" />

    <!-- 内容区 -->
    <div v-else-if="currentUser" class="profile-container">
      <!-- 用户信息卡片 -->
      <v-card rounded="xl" class="profile-header-card mb-6 mb-md-8 no-border" elevation="0">
        <v-card-text class="pa-4 pa-sm-6 pa-md-8">
          <div class="d-flex flex-column flex-sm-row align-center align-sm-start">
            <!-- 用户头像 -->
            <v-avatar
              :size="$vuetify.display.mobile ? 72 : 96"
              color="primary"
              class="mb-4 mb-sm-0 mr-sm-4 mr-md-6"
              rounded="xl"
            >
              <v-icon icon="mdi-account" :size="$vuetify.display.mobile ? 36 : 48" color="white" />
            </v-avatar>

            <!-- 用户信息 -->
            <div class="flex-grow-1 text-center text-sm-start w-100">
              <div
                class="d-flex flex-column flex-sm-row align-center align-sm-start justify-center justify-sm-start mb-3 ga-3"
              >
                <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4">
                  {{ userInfo.name }}
                </h1>
                <v-btn
                  color="grey-darken-2"
                  variant="outlined"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  @click="activeTab = 'info'"
                >
                  <v-icon
                    icon="mdi-pencil"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1 mr-sm-2"
                  />
                  编辑资料
                </v-btn>
              </div>

              <p class="text-body-2 text-md-body-1 text-grey-darken-2 mb-1 mb-md-2">
                {{ userInfo.email }}
              </p>
              <p class="text-caption text-md-body-2 text-grey mb-3 mb-md-4">
                加入于 {{ userInfo.joinDate }}
              </p>

              <!-- 统计信息 -->
              <div
                class="d-flex align-center justify-center justify-sm-start flex-wrap ga-4 ga-md-8"
              >
                <div class="d-flex align-center">
                  <v-icon
                    icon="mdi-file-document-multiple"
                    :size="$vuetify.display.mobile ? 18 : 20"
                    color="primary"
                    class="mr-2"
                  />
                  <span class="text-caption text-md-body-2 text-grey-darken-2">
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{
                      stats.articles
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
                      stats.catalogs
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
                      stats.roadmaps
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
                    <span class="font-weight-bold text-grey-darken-4 mr-1">{{ stats.decks }}</span>
                    卡片组
                  </span>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- 模式切换栏 -->
      <div class="mode-switcher-bar d-flex align-center mb-4 mb-md-6">
        <h2 class="text-h6 text-md-h5 font-weight-bold text-grey-darken-3 d-flex align-center">
          <v-icon icon="mdi-pen" class="mr-2" color="primary" />
          创作者
        </h2>
        <v-btn
          variant="text"
          size="default"
          class="text-body-2 font-weight-medium ml-3"
          @click="$router.push('/profile')"
        >
          切换到学习者
          <v-icon icon="mdi-chevron-right" size="16" class="ml-1" />
        </v-btn>
      </div>

      <!-- Tab 导航 -->
      <div class="tabs-sticky">
        <v-tabs
          v-model="activeTab"
          color="primary"
          class="profile-tabs"
          :height="$vuetify.display.mobile ? 48 : 56"
          density="comfortable"
          show-arrows
        >
          <v-tab value="stats" rounded="lg">
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
        <!-- 创作统计 -->
        <v-window-item value="stats">
          <StatsTab />
        </v-window-item>

        <!-- 创建的文章 -->
        <v-window-item value="articles">
          <ArticlesTab />
        </v-window-item>

        <!-- 创建的目录 -->
        <v-window-item value="catalogs">
          <CatalogsTab />
        </v-window-item>

        <!-- 创建的路线图 -->
        <v-window-item value="roadmaps">
          <RoadmapsTab />
        </v-window-item>

        <!-- 我的卡片组 -->
        <v-window-item value="decks">
          <MemoryDecksTab />
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
