<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">学习的职业</h4>
            <p class="text-body-2 text-grey mb-0">
              查看您正在学习的职业路径和完成进度。
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-target" size="18" color="grey" class="mr-2 mt-1" />
              <span>职业目标规划</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-chart-line" size="18" color="grey" class="mr-2 mt-1" />
              <span>学习进度跟踪</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-certificate" size="18" color="grey" class="mr-2 mt-1" />
              <span>能力认证</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <!-- 顶部操作栏 -->
        <div class="d-flex align-center justify-space-between mb-6">
          <!-- Tab 切换 -->
          <v-btn-toggle
            v-model="statusTab"
            mandatory
            color="grey-darken-3"
            variant="text"
            rounded="lg"
            density="compact"
          >
            <v-btn value="learning" rounded="lg">
              <v-icon icon="mdi-school" size="18" class="mr-2" />
              正在学习
            </v-btn>
            <v-btn value="completed" rounded="lg">
              <v-icon icon="mdi-check-circle" size="18" class="mr-2" />
              已经完成
            </v-btn>
          </v-btn-toggle>

          <v-btn color="primary" variant="text" rounded="lg" to="/career">
            浏览全部职业
            <v-icon icon="mdi-chevron-right" size="18" class="ml-1" />
          </v-btn>
        </div>

        <!-- 职业列表 -->
        <div v-if="filteredCareers.length > 0">
          <v-row>
            <v-col v-for="career in filteredCareers" :key="career.id" cols="12" md="6">
              <v-card rounded="xl" hover border elevation="0" class="career-card hoverable">
                <v-card-text class="pa-6" @click="goToCareer(career.careerId)">
                  <div class="d-flex align-start justify-space-between mb-4">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar :color="career.iconColor" size="56" rounded="lg" class="mr-4">
                        <v-icon :icon="career.icon" color="white" size="28" />
                      </v-avatar>
                      <div>
                        <h4 class="text-h6 font-weight-bold mb-1">{{ career.title }}</h4>
                        <p class="text-caption text-grey mb-0">{{ career.lastActivity }}</p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="text"
                      size="small"
                      icon="mdi-close"
                      @click.stop="cancelLearning(career.id)"
                    />
                  </div>

                  <v-progress-linear
                    :model-value="career.progress"
                    color="primary"
                    bg-color="grey-lighten-3"
                    height="8"
                    rounded
                    class="mb-3"
                  />

                  <div class="d-flex align-center justify-space-between">
                    <span class="text-body-2 text-grey-darken-2">
                      <v-icon icon="mdi-check-circle" size="16" color="success" class="mr-1" />
                      {{ career.completedCourses }} / {{ career.totalCourses }} 门课程
                    </span>
                    <span class="text-body-1 font-weight-bold text-primary">{{ career.progress }}%</span>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-briefcase" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">
            {{ statusTab === 'learning' ? '暂无正在学习的职业' : '暂无已完成的职业' }}
          </p>
          <p class="text-body-2 text-grey">开始学习职业路径，系统化提升技能</p>
        </div>
      </div>
    </v-col>

    <!-- 空状态 -->
    

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="showDeleteDialog"
      title="确认取消学习"
      message="确定要取消学习该职业吗？此操作无法撤销。"
      confirm-text="确认取消"
      @confirm="confirmDelete"
    />
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { progressApi } from '@/api'
import { UserProgressState } from '@/enums'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// Tab 状态
const statusTab = ref('learning')

// 删除确认对话框
const showDeleteDialog = ref(false)
const roadmapToDelete = ref<number | null>(null)

// 获取用户路线图数据
const {
  data: roadmaps,
  loading,
  execute: fetchRoadmaps,
} = useFetch({
  fetchFn: progressApi.getUserRoadmaps,
  immediate: true,
  defaultValue: [],
})

// 删除路线图进度
const { execute: deleteProgress } = useMutation(
  (roadmapId: number) => progressApi.deleteRoadmapProgress(roadmapId),
  {
    successMessage: '已取消学习该职业',
    onSuccess: () => {
      fetchRoadmaps()
    },
  }
)

// 转换路线图数据为组件所需格式
const careers = computed(() => {
  if (!roadmaps.value) return []

  return roadmaps.value.map((userRoadmap) => {
    const roadmap = userRoadmap.roadmap
    const progress = userRoadmap.progressPercent || 0
    const state = userRoadmap.state || UserProgressState.NOT_STARTED
    const isCompleted = state === UserProgressState.COMPLETED

    // 计算最后活动时间
    const lastActivity = userRoadmap.updatedAt
      ? formatLastActivity(new Date(userRoadmap.updatedAt))
      : '暂无活动'

    return {
      id: userRoadmap.id,
      careerId: roadmap?.id || 0,
      title: roadmap?.profession?.name || '未知职业',
      progress,
      totalCourses: roadmap?.nodeCount || 0,
      completedCourses: Math.round(((roadmap?.nodeCount || 0) * progress) / 100),
      lastActivity,
      icon: 'mdi-briefcase',
      iconColor: '#42b883',
      status: isCompleted ? 'completed' : 'learning',
    }
  })
})

// 根据状态过滤职业列表
const filteredCareers = computed(() => {
  return careers.value.filter((career) => career.status === statusTab.value)
})

// 格式化最后活动时间
const formatLastActivity = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const months = Math.floor(days / 30)

  if (months > 0) return `${months}个月前学习`
  if (days > 0) return `${days}天前学习`
  if (hours > 0) return `${hours}小时前学习`
  if (minutes > 0) return `${minutes}分钟前学习`
  return '刚刚学习'
}

// 跳转到职业详情
const goToCareer = (careerId: number) => {
  router.push(`/career/${careerId}`)
}

// 取消学习职业
const cancelLearning = (roadmapId: number) => {
  roadmapToDelete.value = roadmapId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (roadmapToDelete.value !== null) {
    await deleteProgress(roadmapToDelete.value)
  }
  roadmapToDelete.value = null
}
</script>

<style scoped>
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.career-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
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
