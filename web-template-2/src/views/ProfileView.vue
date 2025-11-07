<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
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

// 用户信息
const userInfo = ref({
  name: '张三',
  email: 'zhangsan@example.com',
  avatar: '',
  joinDate: '2024-01-01',
  bio: '热爱学习的程序员，专注于前端开发和用户体验设计。'
})

// 统计数据
const stats = ref({
  totalCourses: 12,
  completedCourses: 5,
  totalCareers: 3,
  studyDays: 45,
  studyHours: 128,
  followers: 24,
  following: 36,
  articles: 8,
  roadmaps: 3
})

// Tab 选择 - 支持路由参数
const activeTab = ref((route.query.tab as string) || 'careers')

// 监听路由变化
watch(() => route.query.tab, (newTab) => {
  if (newTab && typeof newTab === 'string') {
    activeTab.value = newTab
  }
})

// Tab 切换时更新 URL
watch(activeTab, (newTab) => {
  if (route.query.tab !== newTab) {
    router.push({ query: { tab: newTab } })
  }
})

// 更新用户信息
const handleUpdateUserInfo = (updatedInfo: typeof userInfo.value) => {
  userInfo.value = { ...updatedInfo }
  // 在真实项目中，这里会调用 API 保存数据
  console.log('用户信息已更新:', updatedInfo)
}
</script>

<template>
  <div class="profile-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="#666666">mdi-account</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">我的</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">管理您的个人信息和学习数据</p>
          </div>
        </div>
      </div>

      <!-- 用户卡片 -->
      <v-card border rounded="lg" class="mb-6">
        <v-card-text class="pa-6">
          <div class="d-flex align-center mb-6">
            <v-avatar size="80" color="primary" class="mr-4">
              <v-icon icon="mdi-account" size="40" color="white"></v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <h2 class="text-h5 font-weight-bold mb-1">{{ userInfo.name }}</h2>
              <p class="text-body-2 text-grey-darken-2 mb-1">{{ userInfo.email }}</p>
              <p class="text-caption text-grey">加入于 {{ userInfo.joinDate }}</p>
            </div>
            <v-btn color="primary" variant="outlined" rounded="lg">
              <v-icon icon="mdi-pencil" size="18" class="mr-2"></v-icon>
              编辑资料
            </v-btn>
          </div>

          <!-- 统计信息 -->
          <v-divider class="mb-4"></v-divider>
          <div class="d-flex justify-space-around flex-wrap" style="gap: 16px;">
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-primary mb-1">{{ stats.totalCourses }}</div>
              <div class="text-caption text-grey">学习课程</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-success mb-1">{{ stats.completedCourses }}</div>
              <div class="text-caption text-grey">完成课程</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-info mb-1">{{ stats.totalCareers }}</div>
              <div class="text-caption text-grey">关注职业</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-warning mb-1">{{ stats.studyDays }}</div>
              <div class="text-caption text-grey">学习天数</div>
            </div>
            <div class="text-center">
              <div class="text-h5 font-weight-bold text-purple mb-1">{{ stats.studyHours }}</div>
              <div class="text-caption text-grey">学习时长(h)</div>
            </div>
          </div>
        </v-card-text>
      </v-card>

      <!-- Tab 导航 -->
      <v-tabs v-model="activeTab" color="primary" class="mb-6 tabs-with-border">
        <v-tab value="careers">
          <v-icon icon="mdi-briefcase" size="18" class="mr-2"></v-icon>
          学习的职业
        </v-tab>
        <v-tab value="courses-learning">
          <v-icon icon="mdi-school" size="18" class="mr-2"></v-icon>
          学习的课程
        </v-tab>
        <v-tab value="stats">
          <v-icon icon="mdi-chart-line" size="18" class="mr-2"></v-icon>
          数据统计
        </v-tab>
        <v-tab value="courses">
          <v-icon icon="mdi-book-multiple" size="18" class="mr-2"></v-icon>
          关注的课程
        </v-tab>
        <v-tab value="people">
          <v-icon icon="mdi-account-multiple" size="18" class="mr-2"></v-icon>
          关注的人
        </v-tab>
        <v-tab value="catalogs">
          <v-icon icon="mdi-folder-multiple" size="18" class="mr-2"></v-icon>
          创建的目录
        </v-tab>
        <v-tab value="articles">
          <v-icon icon="mdi-file-document-multiple" size="18" class="mr-2"></v-icon>
          创建的文章
        </v-tab>
        <v-tab value="decks">
          <v-icon icon="mdi-cards" size="18" class="mr-2"></v-icon>
          我的卡片组
        </v-tab>
        <v-tab value="roadmaps">
          <v-icon icon="mdi-map-marker-path" size="18" class="mr-2"></v-icon>
          创建的路线图
        </v-tab>
        <v-tab value="info">
          <v-icon icon="mdi-account-circle" size="18" class="mr-2"></v-icon>
          个人信息
        </v-tab>
      </v-tabs>

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

        <!-- 我关注的课程 -->
        <v-window-item value="courses">
          <SubscriptionTab />
        </v-window-item>

        <!-- 我关注的人 -->
        <v-window-item value="people">
          <FollowingTab />
        </v-window-item>

        <!-- 我创建的目录 -->
        <v-window-item value="catalogs">
          <CatalogsTab />
        </v-window-item>

        <!-- 我创建的文章 -->
        <v-window-item value="articles">
          <ArticlesTab />
        </v-window-item>

        <!-- 我的卡片组 -->
        <v-window-item value="decks">
          <MemoryDecksTab />
        </v-window-item>

        <!-- 我创建的路线图 -->
        <v-window-item value="roadmaps">
          <RoadmapsTab />
        </v-window-item>
      </v-window>
    </div>

  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

/* Tab 标签字体颜色调整 */
:deep(.v-tab) {
  color: rgba(0, 0, 0, 0.5) !important;
}

:deep(.v-tab--selected) {
  color: rgb(var(--v-theme-primary)) !important;
}

:deep(.v-tab:hover) {
  color: rgba(0, 0, 0, 0.7) !important;
}

/* Tab 下边框 */
.tabs-with-border {
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }
}
</style>
