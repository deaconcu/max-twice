<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">学习的课程</h4>
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
          <!-- Tab 切换 -->
          <div class="d-flex align-center ga-3">
            <v-icon icon="mdi-menu" size="18" color="grey-lighten-1"></v-icon>
            <v-btn-toggle
              v-model="statusTab"
              mandatory
              color="primary"
              variant="plain"
              rounded="md"
              density="compact"
            >
              <v-btn value="learning" size="small" rounded="md">正在学习</v-btn>
              <v-btn value="completed" size="small" rounded="md">已经完成</v-btn>
            </v-btn-toggle>
          </div>

          <v-btn color="primary" variant="text" rounded="md" density="compact" to="/learning">
            查看全部课程
            <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 课程列表 -->
        <div v-if="filteredCourses.length > 0">
          <v-row>
            <v-col
              v-for="course in filteredCourses"
              :key="course.id"
              cols="12"
              md="6"
            >
              <v-card
                border
                rounded="lg"
                hover
                class="hoverable"
              >
                <v-card-text class="pa-4" @click="goToCourse(course.courseId)">
                  <div class="d-flex align-start justify-space-between mb-3">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar :color="course.iconColor" size="48" rounded="md" class="mr-3">
                        <v-icon :icon="course.icon" color="white" size="24"></v-icon>
                      </v-avatar>
                      <div>
                        <h4 class="text-body-1 font-weight-bold mb-1">{{ course.title }}</h4>
                        <p class="text-caption text-grey mb-0">{{ course.lastActivity }}</p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-close"
                      @click.stop="cancelLearning(course.id)"
                    ></v-btn>
                  </div>

                  <v-progress-linear
                    :model-value="course.progress"
                    color="grey-lighten-1"
                    height="6"
                    rounded
                    class="mb-2"
                  ></v-progress-linear>

                  <div class="d-flex align-center justify-space-between text-caption text-grey">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      {{ course.completedLessons }} / {{ course.totalLessons }} 节已完成
                    </div>
                    <div class="text-grey">
                      {{ course.progress }}%
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-school" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-2">{{ statusTab === 'learning' ? '暂无正在学习的课程' : '暂无已完成的课程' }}</p>
          <p class="text-body-2 text-grey">开始学习新课程，掌握新技能</p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认取消学习"
          message="确定要取消学习该课程吗？此操作无法撤销。"
          confirm-text="确认取消"
          @confirm="confirmDelete"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// Tab 状态
const statusTab = ref('learning')

// 删除确认对话框
const showDeleteDialog = ref(false)
const courseToDelete = ref<number | null>(null)

// Mock 课程列表
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
    iconColor: '#42b883',
    status: 'learning'
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
    iconColor: '#3178c6',
    status: 'learning'
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
    iconColor: '#3776ab',
    status: 'learning'
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
    iconColor: '#61dafb',
    status: 'learning'
  },
  {
    id: 5,
    courseId: 105,
    title: 'JavaScript 基础',
    progress: 100,
    totalLessons: 30,
    completedLessons: 30,
    lastActivity: '2周前完成',
    icon: 'mdi-language-javascript',
    iconColor: '#f7df1e',
    status: 'completed'
  }
])

// 根据状态过滤课程列表
const filteredCourses = computed(() => {
  return courses.value.filter(course => course.status === statusTab.value)
})

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 取消学习课程
const cancelLearning = (courseId: number) => {
  courseToDelete.value = courseId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (courseToDelete.value !== null) {
    const courseIndex = courses.value.findIndex(c => c.id === courseToDelete.value)
    if (courseIndex !== -1) {
      // TODO: 调用 API 取消学习
      courses.value.splice(courseIndex, 1)
    }
  }
  courseToDelete.value = null
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
