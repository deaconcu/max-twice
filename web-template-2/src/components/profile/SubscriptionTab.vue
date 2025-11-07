<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10">
          <div class="d-flex align-center mb-3">
            <v-icon icon="mdi-book-multiple" color="primary" size="20" class="mr-2"></v-icon>
            <h4 class="text-body-1 font-weight-bold">关注的课程</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            管理您关注的所有课程，获取最新动态。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-bell-outline" size="14" class="mr-1"></v-icon>
              接收课程更新通知
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-heart" size="14" class="mr-1"></v-icon>
              快速取消关注
            </div>
            <div>
              <v-icon icon="mdi-view-grid" size="14" class="mr-1"></v-icon>
              网格化浏览
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <h3 class="text-h6 font-weight-bold">我关注的课程</h3>
          <v-btn color="primary" variant="text" rounded="md" density="compact" to="/learning">
            浏览更多课程
            <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 课程网格 -->
        <div v-if="subscriptions.length > 0">
          <v-row>
            <v-col
              v-for="course in subscriptions"
              :key="course.id"
              cols="12"
              sm="6"
              md="4"
              lg="3"
            >
              <v-card
                border
                rounded="md"
                hover
                class="course-card"
                @click="goToCourse(course.courseId)"
              >
                <v-card-text class="pa-4">
                  <!-- 课程图标和取消关注按钮 -->
                  <div class="d-flex align-center justify-space-between mb-3">
                    <v-avatar :color="course.course.iconColor" size="48" rounded="md">
                      <v-icon :icon="course.course.icon" color="white" size="24"></v-icon>
                    </v-avatar>
                    <v-btn
                      icon
                      size="small"
                      variant="text"
                      color="error"
                      @click.stop="unsubscribe(course.id)"
                    >
                      <v-icon icon="mdi-heart" size="20"></v-icon>
                    </v-btn>
                  </div>

                  <!-- 课程信息 -->
                  <h4 class="text-body-1 font-weight-bold mb-2 text-truncate">
                    {{ course.course.name }}
                  </h4>
                  <p class="text-caption text-grey mb-3 course-description">
                    {{ course.course.description }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center justify-space-between">
                    <div class="d-flex align-center text-caption text-grey">
                      <v-icon icon="mdi-account-multiple" size="14" class="mr-1"></v-icon>
                      {{ course.course.learnerCount || 0 }} 人学习
                    </div>
                    <v-chip
                      v-if="course.course.category"
                      size="x-small"
                      variant="outlined"
                      color="grey"
                    >
                      {{ course.course.category }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-book-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-2">暂无关注的课程</p>
          <p class="text-body-2 text-grey">关注感兴趣的课程，及时获取更新</p>
          <v-btn color="primary" variant="flat" rounded="md" density="compact" class="mt-4" to="/learning">
            <v-icon icon="mdi-plus" size="18" class="mr-2"></v-icon>
            浏览课程
          </v-btn>
        </div>

        <!-- 取消关注确认对话框 -->
        <v-dialog v-model="showUnsubscribeDialog" max-width="400">
          <v-card rounded="md">
            <v-card-title class="text-h6 font-weight-bold">确认取消关注</v-card-title>
            <v-card-text>
              确定要取消关注《{{ unsubscribeCourse?.course.name }}》吗？
            </v-card-text>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn
                variant="text"
                rounded="md"
                density="compact"
                @click="showUnsubscribeDialog = false"
              >
                取消
              </v-btn>
              <v-btn
                color="error"
                variant="flat"
                rounded="md"
                density="compact"
                @click="confirmUnsubscribe"
              >
                确认取消
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Mock 订阅课程数据
const subscriptions = ref([
  {
    id: 1,
    courseId: 101,
    course: {
      id: 101,
      name: 'Vue 3 完整教程',
      description: '从入门到精通，掌握 Vue 3 的核心概念和最佳实践',
      icon: 'mdi-vuejs',
      iconColor: '#42b883',
      learnerCount: 15234,
      category: '前端开发'
    },
    order: 1
  },
  {
    id: 2,
    courseId: 102,
    course: {
      id: 102,
      name: 'TypeScript 进阶',
      description: '深入学习 TypeScript 高级特性和类型系统',
      icon: 'mdi-language-typescript',
      iconColor: '#3178c6',
      learnerCount: 8932,
      category: '编程语言'
    },
    order: 2
  },
  {
    id: 3,
    courseId: 103,
    course: {
      id: 103,
      name: 'Python 数据分析',
      description: '使用 Python 进行数据处理和可视化分析',
      icon: 'mdi-language-python',
      iconColor: '#3776ab',
      learnerCount: 12456,
      category: '数据科学'
    },
    order: 3
  },
  {
    id: 4,
    courseId: 104,
    course: {
      id: 104,
      name: 'React 实战开发',
      description: '通过实际项目学习 React 开发技巧',
      icon: 'mdi-react',
      iconColor: '#61dafb',
      learnerCount: 10234,
      category: '前端开发'
    },
    order: 4
  },
  {
    id: 5,
    courseId: 105,
    course: {
      id: 105,
      name: 'Node.js 后端开发',
      description: '构建高性能的 Node.js 后端应用',
      icon: 'mdi-nodejs',
      iconColor: '#68a063',
      learnerCount: 7845,
      category: '后端开发'
    },
    order: 5
  },
  {
    id: 6,
    courseId: 106,
    course: {
      id: 106,
      name: 'Docker 容器化',
      description: '学习容器化部署和微服务架构',
      icon: 'mdi-docker',
      iconColor: '#2496ed',
      learnerCount: 6234,
      category: 'DevOps'
    },
    order: 6
  }
])

// 取消关注对话框
const showUnsubscribeDialog = ref(false)
const unsubscribeCourse = ref<typeof subscriptions.value[0] | null>(null)

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 取消关注
const unsubscribe = (id: number) => {
  unsubscribeCourse.value = subscriptions.value.find(c => c.id === id) || null
  showUnsubscribeDialog.value = true
}

// 确认取消关注
const confirmUnsubscribe = () => {
  if (unsubscribeCourse.value) {
    subscriptions.value = subscriptions.value.filter(c => c.id !== unsubscribeCourse.value!.id)
  }
  showUnsubscribeDialog.value = false
  unsubscribeCourse.value = null
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

.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
}

.course-card:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
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
