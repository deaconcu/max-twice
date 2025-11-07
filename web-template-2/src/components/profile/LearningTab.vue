<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10">
          <div class="d-flex align-center mb-3">
            <v-icon icon="mdi-school" color="primary" size="20" class="mr-2"></v-icon>
            <h4 class="text-body-1 font-weight-bold">正在学习</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            跟踪您当前学习的所有课程进度和完成情况。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-chart-line" size="14" class="mr-1"></v-icon>
              实时同步学习进度
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
              记录最近学习时间
            </div>
            <div>
              <v-icon icon="mdi-target" size="14" class="mr-1"></v-icon>
              设定学习目标
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <h3 class="text-h6 font-weight-bold">正在学习</h3>
          <v-btn color="primary" variant="text" rounded="md" density="compact" to="/learning">
            查看全部
            <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 正在学习的职业 -->
        <div class="mb-8">
          <h4 class="text-body-1 font-weight-bold mb-3">正在学习的职业</h4>
          <div v-if="careers.length > 0">
            <v-row>
              <v-col
                v-for="career in careers"
                :key="career.id"
                cols="12"
                md="6"
              >
                <v-card
                  border
                  rounded="md"
                  hover
                  class="career-card"
                  @click="goToCareer(career.careerId)"
                >
                  <v-card-text class="pa-4">
                    <div class="d-flex align-center mb-3">
                      <v-avatar :color="career.iconColor" size="48" rounded="md" class="mr-3">
                        <v-icon :icon="career.icon" color="white" size="24"></v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <h4 class="text-body-1 font-weight-bold mb-1">{{ career.title }}</h4>
                        <p class="text-caption text-grey mb-0">{{ career.lastActivity }}</p>
                      </div>
                      <v-chip
                        :color="getProgressColor(career.progress)"
                        size="small"
                        variant="flat"
                      >
                        {{ career.progress }}%
                      </v-chip>
                    </div>

                    <v-progress-linear
                      :model-value="career.progress"
                      :color="getProgressColor(career.progress)"
                      height="6"
                      rounded
                      class="mb-2"
                    ></v-progress-linear>

                    <div class="d-flex align-center text-caption text-grey">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      {{ career.completedCourses }} / {{ career.totalCourses }} 门课程已完成
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </div>
          <div v-else class="text-center py-8">
            <v-icon icon="mdi-briefcase" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
            <p class="text-body-2 text-grey">暂无正在学习的职业</p>
          </div>
        </div>

        <!-- 正在学习的课程 -->
        <div>
          <h4 class="text-body-1 font-weight-bold mb-3">正在学习的课程</h4>
          <div v-if="courses.length > 0">
            <v-row>
            <v-col
              v-for="course in courses"
              :key="course.id"
              cols="12"
              md="6"
            >
              <v-card
                border
                rounded="md"
                hover
                class="course-card"
                @click="goToCourse(course.courseId)"
              >
                <v-card-text class="pa-4">
                  <div class="d-flex align-center mb-3">
                    <v-avatar :color="course.iconColor" size="48" rounded="md" class="mr-3">
                      <v-icon :icon="course.icon" color="white" size="24"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <h4 class="text-body-1 font-weight-bold mb-1">{{ course.title }}</h4>
                      <p class="text-caption text-grey mb-0">{{ course.lastActivity }}</p>
                    </div>
                    <v-chip
                      :color="getProgressColor(course.progress)"
                      size="small"
                      variant="flat"
                    >
                      {{ course.progress }}%
                    </v-chip>
                  </div>

                  <v-progress-linear
                    :model-value="course.progress"
                    :color="getProgressColor(course.progress)"
                    height="6"
                    rounded
                    class="mb-2"
                  ></v-progress-linear>

                  <div class="d-flex align-center text-caption text-grey">
                    <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                    {{ course.completedLessons }} / {{ course.totalLessons }} 节已完成
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>
        <div v-else class="text-center py-8">
          <v-icon icon="mdi-school" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
          <p class="text-body-2 text-grey">暂无正在学习的课程</p>
        </div>
      </div>
    </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock 正在学习的职业数据
const careers = ref([
  {
    id: 1,
    careerId: 201,
    title: '前端工程师',
    progress: 35,
    totalCourses: 12,
    completedCourses: 4,
    lastActivity: '2小时前学习',
    icon: 'mdi-web',
    iconColor: '#42b883'
  },
  {
    id: 2,
    careerId: 202,
    title: '全栈工程师',
    progress: 18,
    totalCourses: 20,
    completedCourses: 3,
    lastActivity: '昨天学习',
    icon: 'mdi-layers',
    iconColor: '#3178c6'
  }
])

// Mock 学习数据
const courses = ref([
  {
    id: 1,
    courseId: 101,
    title: 'Vue 3 完整教程',
    progress: 68,
    totalLessons: 47,
    completedLessons: 32,
    lastActivity: '2小时前学习',
    icon: 'mdi-vuejs',
    iconColor: '#42b883'
  },
  {
    id: 2,
    courseId: 102,
    title: 'TypeScript 进阶',
    progress: 42,
    totalLessons: 38,
    completedLessons: 16,
    lastActivity: '昨天学习',
    icon: 'mdi-language-typescript',
    iconColor: '#3178c6'
  },
  {
    id: 3,
    courseId: 103,
    title: 'Python 数据分析',
    progress: 25,
    totalLessons: 56,
    completedLessons: 14,
    lastActivity: '3天前学习',
    icon: 'mdi-language-python',
    iconColor: '#3776ab'
  },
  {
    id: 4,
    courseId: 104,
    title: 'React 实战开发',
    progress: 15,
    totalLessons: 42,
    completedLessons: 6,
    lastActivity: '1周前学习',
    icon: 'mdi-react',
    iconColor: '#61dafb'
  }
])

// 跳转到职业详情
const goToCareer = (careerId: number) => {
  router.push(`/career/${careerId}`)
}

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 根据进度获取颜色
const getProgressColor = (progress: number) => {
  if (progress >= 80) return 'success'
  if (progress >= 50) return 'primary'
  if (progress >= 30) return 'warning'
  return 'grey'
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.progress-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-item {
  text-align: center;
  padding: 8px 0;
}

.career-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.career-card:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-card:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
