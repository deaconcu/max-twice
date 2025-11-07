<template>
  <div class="course-detail-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 返回按钮 -->
      <v-btn
        variant="text"
        color="grey-darken-2"
        class="mb-4"
        @click="$router.back()"
      >
        <v-icon icon="mdi-arrow-left" class="mr-1"></v-icon>
        返回
      </v-btn>

      <!-- 主内容区 -->
      <div class="content-wrapper">
        <!-- 左侧内容 -->
        <div class="left-content">
          <!-- 课程信息卡片 -->
          <v-card rounded="lg" class="course-info-card mb-6" hover @click="goToRead(0)">
            <v-card-text class="pa-6">
              <!-- 课程头部信息 -->
              <div class="d-flex align-start mb-4">
                <v-avatar color="grey-lighten-3" size="80" rounded="lg" class="mr-4">
                  <v-icon icon="mdi-book-open-variant" color="#666666" size="40"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <!-- 标题和按钮在同一行 -->
                  <div class="d-flex align-center justify-space-between mb-2">
                    <h1 class="text-h4 font-weight-bold text-grey-darken-4">
                      {{ courseData.title }}
                    </h1>
                    <!-- 操作按钮 -->
                    <div class="d-flex align-center flex-shrink-0 ml-4" style="gap: 8px;">
                      <v-btn
                        color="primary"
                        variant="flat"
                        size="default"
                        rounded="lg"
                        class="text-none"
                        @click.stop="goToRead(0)"
                      >
                        <v-icon size="18" class="mr-2">mdi-book-open-page-variant</v-icon>
                        开始阅读
                      </v-btn>
                      <v-btn
                        :color="courseData.subscribed ? 'error' : 'grey-darken-2'"
                        :variant="courseData.subscribed ? 'flat' : 'outlined'"
                        size="default"
                        rounded="lg"
                        class="text-none"
                        @click.stop="toggleSubscribe"
                      >
                        <v-icon size="18" class="mr-2">{{ courseData.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
                        {{ courseData.subscribed ? '已订阅' : '订阅' }}
                      </v-btn>
                    </div>
                  </div>
                  <p class="text-body-1 text-grey-darken-2 mb-3">
                    {{ courseData.description }}
                  </p>
                  <!-- 统计信息 -->
                  <div class="d-flex align-center flex-wrap" style="gap: 24px;">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-book-multiple" size="20" color="primary" class="mr-2"></v-icon>
                      <span class="text-body-2 text-grey-darken-2">
                        <span class="font-weight-bold text-grey-darken-4">{{ subCourses.length }}</span> 个子课程
                      </span>
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-group" size="20" color="primary" class="mr-2"></v-icon>
                      <span class="text-body-2 text-grey-darken-2">
                        <span class="font-weight-bold text-grey-darken-4">{{ courseData.learnerCount.toLocaleString() }}</span> 人学习
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>

          <!-- 子课程列表 -->
          <div class="sub-courses-section">
            <div class="d-flex align-center justify-space-between mb-4">
              <div class="d-flex align-center">
                <v-icon icon="mdi-book-multiple" size="20" color="grey-darken-2" class="mr-2"></v-icon>
                <h2 class="text-h6 font-weight-bold text-grey-darken-4">子课程列表</h2>
              </div>
              <v-btn
                color="grey-lighten-4"
                variant="flat"
                size="small"
                rounded="lg"
                class="text-none"
                @click="showSubCourseApplicationDialog = true"
              >
                <v-icon icon="mdi-plus" size="16" class="mr-2"></v-icon>
                <span class="text-grey-darken-3">申请子课程</span>
              </v-btn>
            </div>

            <!-- 子课程网格 -->
            <div class="sub-course-grid">
              <v-card
                v-for="(subCourse, index) in subCourses"
                :key="index"
                rounded="lg"
                class="sub-course-card"
                hover
                @click="goToRead(index)"
              >
                <v-card-text class="pa-5">
                  <!-- 序号和按钮 -->
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="sub-course-number">
                      {{ index + 1 }}
                    </div>
                    <div class="d-flex align-center" style="gap: 4px;">
                      <v-btn
                        icon
                        size="x-small"
                        variant="text"
                        color="grey"
                        @click.stop="goToRead(index)"
                      >
                        <v-icon size="18">mdi-book-open-page-variant</v-icon>
                      </v-btn>
                      <v-btn
                        icon
                        size="x-small"
                        variant="text"
                        :color="subCourse.subscribed ? 'error' : 'grey'"
                        @click.stop="toggleSubCourseSubscribe(index)"
                      >
                        <v-icon size="18">{{ subCourse.subscribed ? 'mdi-heart' : 'mdi-heart-outline' }}</v-icon>
                      </v-btn>
                    </div>
                  </div>

                  <!-- 子课程信息 -->
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">
                    {{ subCourse.name }}
                  </h3>
                  <p class="text-body-2 text-grey-darken-2 mb-3" style="min-height: 60px;">
                    {{ subCourse.description }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1"></v-icon>
                    <span class="text-caption text-grey-darken-2">{{ subCourse.learnerCount.toLocaleString() }} 人学习</span>
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </div>

        <!-- 右侧帮助信息 -->
        <div class="right-sidebar">
          <v-card rounded="lg" class="help-card sticky-card" flat border>
            <v-card-title class="pa-4 pb-3">
              <div class="d-flex align-center">
                <v-icon icon="mdi-help-circle" color="primary" class="mr-2"></v-icon>
                <span class="text-h6 font-weight-bold">帮助信息</span>
              </div>
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">如何学习</h4>
                <p class="text-body-2 text-grey-darken-2 mb-0">
                  点击子课程卡片进入学习内容，按顺序完成每个章节的学习。
                </p>
              </div>

              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">学习进度</h4>
                <p class="text-body-2 text-grey-darken-2 mb-0">
                  系统会自动记录你的学习进度，完成的章节会显示为已完成状态。
                </p>
              </div>

              <div class="help-section mb-4">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">订阅课程</h4>
                <p class="text-body-2 text-grey-darken-2 mb-0">
                  点击订阅按钮可以收藏课程，方便随时回来继续学习。
                </p>
              </div>

              <div class="help-section">
                <h4 class="text-subtitle-2 font-weight-bold mb-2">记忆卡片</h4>
                <p class="text-body-2 text-grey-darken-2 mb-0">
                  在学习过程中可以创建记忆卡片，帮助巩固知识点。
                </p>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'

const router = useRouter()
const route = useRoute()

// 课程数据
const courseData = ref({
  title: 'Vue 3 完整教程',
  description: '从入门到精通，掌握 Vue 3 的核心概念和最佳实践。本课程涵盖响应式系统、组合式 API、组件开发、路由管理、状态管理等核心内容，通过大量实战案例帮助你深入理解 Vue 3 的设计理念和使用方法，最终能够独立开发完整的 Vue 3 应用程序。',
  subscribed: false,
  learnerCount: 18765,
  totalNodes: 47,
  completedNodes: 25,
  progress: 53
})

// 子课程列表
const subCourses = ref([
  {
    name: 'Vue 3 基础',
    description: '学习 Vue 3 的核心概念、响应式系统、组件基础和模板语法',
    progress: 80,
    totalNodes: 12,
    completedNodes: 10,
    subscribed: false,
    learnerCount: 15234
  },
  {
    name: 'Vue 3 进阶',
    description: '深入学习组合式 API、自定义指令、插件开发和高级组件模式',
    progress: 60,
    totalNodes: 15,
    completedNodes: 9,
    subscribed: false,
    learnerCount: 12890
  },
  {
    name: 'Vue 3 实战',
    description: '通过完整项目实战掌握 Vue 3 生态系统，包括路由、状态管理和构建部署',
    progress: 30,
    totalNodes: 20,
    completedNodes: 6,
    subscribed: false,
    learnerCount: 9876
  }
])

const toggleSubscribe = () => {
  courseData.value.subscribed = !courseData.value.subscribed
}

const toggleSubCourseSubscribe = (index: number) => {
  subCourses.value[index].subscribed = !subCourses.value[index].subscribed
}

const goToRead = (subCourseIndex: number) => {
  const courseId = route.params.courseId || '1'
  router.push({
    name: 'content-read',
    params: {
      courseId: courseId,
      subCourseId: subCourseIndex
    }
  })
}
</script>

<style scoped>
.course-detail-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

/* 使用flex布局实现左右结构 */
.content-wrapper {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.left-content {
  flex: 1;
  min-width: 0;
}

.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.course-info-card {
  background-color: white;
  border: 1px solid #E5E5E5;
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-info-card:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.progress-section {
  padding-top: 20px;
  border-top: 1px solid #E5E5E5;
  margin-top: 20px;
}

.sub-courses-section {
  margin-top: 0;
}

.sub-course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.sub-course-card {
  border: 1px solid #E5E5E5;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
}

.sub-course-card:hover {
  border-color: rgb(var(--v-theme-primary));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.sub-course-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: rgb(var(--v-theme-primary));
  color: white;
  border-radius: 8px;
  font-weight: bold;
  font-size: 0.875rem;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow-y: auto;
}

.help-card {
  background-color: white;
  border: 1px solid #E5E5E5;
}

.help-section {
  padding-bottom: 12px;
  border-bottom: 1px solid #F5F5F5;
}

.help-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 移动端响应式 */
@media (max-width: 1264px) {
  .content-wrapper {
    flex-direction: column;
  }

  .right-sidebar {
    width: 100%;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }
}

@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 40px 20px;
  }

  .sub-course-grid {
    grid-template-columns: 1fr;
  }
}
</style>
