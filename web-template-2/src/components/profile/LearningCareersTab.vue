<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">学习的职业</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            查看您正在学习的职业路径和完成进度。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-target" size="14" class="mr-1"></v-icon>
              职业目标规划
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-chart-line" size="14" class="mr-1"></v-icon>
              学习进度跟踪
            </div>
            <div>
              <v-icon icon="mdi-certificate" size="14" class="mr-1"></v-icon>
              能力认证
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

          <v-btn color="primary" variant="text" rounded="md" density="compact" to="/career">
            浏览全部职业
            <v-icon icon="mdi-chevron-right" class="ml-1"></v-icon>
          </v-btn>
        </div>

        <!-- 职业列表 -->
        <div v-if="filteredCareers.length > 0">
          <v-row>
            <v-col
              v-for="career in filteredCareers"
              :key="career.id"
              cols="12"
              md="6"
            >
              <v-card
                rounded="lg"
                hover
                class="hoverable"
              >
                <v-card-text class="pa-4" @click="goToCareer(career.careerId)">
                  <div class="d-flex align-start justify-space-between mb-3">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar :color="career.iconColor" size="48" rounded="md" class="mr-3">
                        <v-icon :icon="career.icon" color="white" size="24"></v-icon>
                      </v-avatar>
                      <div>
                        <h4 class="text-body-1 font-weight-bold mb-1">{{ career.title }}</h4>
                        <p class="text-caption text-grey mb-0">{{ career.lastActivity }}</p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-close"
                      @click.stop="cancelLearning(career.id)"
                    ></v-btn>
                  </div>

                  <v-progress-linear
                    :model-value="career.progress"
                    color="grey-lighten-1"
                    height="6"
                    rounded
                    class="mb-2"
                  ></v-progress-linear>

                  <div class="d-flex align-center justify-space-between text-caption text-grey">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      {{ career.completedCourses }} / {{ career.totalCourses }} 门课程已完成
                    </div>
                    <div class="text-grey">
                      {{ career.progress }}%
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-briefcase" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-2">{{ statusTab === 'learning' ? '暂无正在学习的职业' : '暂无已完成的职业' }}</p>
          <p class="text-body-2 text-grey">开始学习职业路径，系统化提升技能</p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认取消学习"
          message="确定要取消学习该职业吗？此操作无法撤销。"
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
const careerToDelete = ref<number | null>(null)

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
    iconColor: '#42b883',
    status: 'learning'
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
    iconColor: '#3178c6',
    status: 'learning'
  },
  {
    id: 3,
    careerId: 203,
    title: 'Java 工程师',
    progress: 100,
    totalCourses: 15,
    completedCourses: 15,
    lastActivity: '1个月前完成',
    icon: 'mdi-language-java',
    iconColor: '#f89820',
    status: 'completed'
  }
])

// 根据状态过滤职业列表
const filteredCareers = computed(() => {
  return careers.value.filter(career => career.status === statusTab.value)
})

// 跳转到职业详情
const goToCareer = (careerId: number) => {
  router.push(`/career/${careerId}`)
}

// 取消学习职业
const cancelLearning = (careerId: number) => {
  careerToDelete.value = careerId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (careerToDelete.value !== null) {
    const careerIndex = careers.value.findIndex(c => c.id === careerToDelete.value)
    if (careerIndex !== -1) {
      // TODO: 调用 API 取消学习
      careers.value.splice(careerIndex, 1)
    }
  }
  careerToDelete.value = null
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
