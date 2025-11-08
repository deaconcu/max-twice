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
      <!-- 用户信息和统计 -->
      <v-row class="mb-6">
        <!-- 左列：用户信息 -->
        <v-col cols="12" md="6">
          <v-card rounded="lg" flat class="h-100 no-border">
            <v-card-text class="px-0 py-4 d-flex align-center">
              <v-avatar size="56" color="primary" class="mr-4" rounded="md">
                <v-icon icon="mdi-account" size="28" color="white"></v-icon>
              </v-avatar>
              <div class="flex-grow-1">
                <div class="d-flex align-center mb-1">
                  <h2 class="text-h6 font-weight-bold mr-3">{{ userInfo.name }}</h2>
                  <v-btn color="grey" variant="text" rounded="lg" size="small" @click="activeTab = 'info'">
                    <v-icon icon="mdi-pencil" size="16" class="mr-1"></v-icon>
                    编辑资料
                  </v-btn>
                </div>
                <p class="text-body-2 text-grey-darken-2 mb-1">{{ userInfo.email }}</p>
                <p class="text-caption text-grey mb-0">加入于 {{ userInfo.joinDate }}</p>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 右列：统计信息 -->
        <v-col cols="12" md="6">
          <v-card rounded="lg" flat class="h-100 no-border">
            <v-card-text class="px-0 py-4 d-flex align-center">
              <div class="d-flex justify-space-between align-center flex-grow-1">
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-primary">{{ stats.totalCourses }}</div>
                  <div class="text-caption text-grey">学习课程</div>
                </div>
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-success">{{ stats.completedCourses }}</div>
                  <div class="text-caption text-grey">完成课程</div>
                </div>
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-info">{{ stats.totalCareers }}</div>
                  <div class="text-caption text-grey">关注职业</div>
                </div>
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-warning">{{ stats.studyDays }}</div>
                  <div class="text-caption text-grey">学习天数</div>
                </div>
                <div class="text-center">
                  <div class="text-h6 font-weight-bold text-purple">{{ stats.studyHours }}</div>
                  <div class="text-caption text-grey">学习时长(h)</div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

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
