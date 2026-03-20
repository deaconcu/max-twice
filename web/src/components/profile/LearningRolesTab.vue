<template>
  <div>
      <div class="pa-0 pa-sm-2">
        <!-- 顶部操作栏 -->
        <div
          class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mb-md-6 ga-3"
        >
          <!-- Tab 切换 -->
          <v-btn-toggle
            v-model="statusTab"
            mandatory
            color="grey-darken-3"
            variant="text"
            rounded="lg"
            density="compact"
          >
            <v-btn
              value="learning"
              rounded="lg"
              :size="$vuetify.display.mobile ? 'small' : 'default'"
            >
              <v-icon
                icon="mdi-school"
                :size="$vuetify.display.mobile ? 16 : 18"
                :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
              />
              <span class="d-none d-sm-inline">正在学习</span>
              <span class="d-sm-none">学习中</span>
            </v-btn>
            <v-btn
              value="completed"
              rounded="lg"
              :size="$vuetify.display.mobile ? 'small' : 'default'"
            >
              <v-icon
                icon="mdi-check-circle"
                :size="$vuetify.display.mobile ? 16 : 18"
                :class="$vuetify.display.mobile ? 'mr-1' : 'mr-2'"
              />
              <span class="d-none d-sm-inline">已经完成</span>
              <span class="d-sm-none">已完成</span>
            </v-btn>
          </v-btn-toggle>

          <v-btn
            color="primary"
            variant="text"
            rounded="lg"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            to="/role"
          >
            <span class="d-none d-sm-inline">浏览全部职业</span>
            <span class="d-sm-none">浏览</span>
            <v-icon
              icon="mdi-chevron-right"
              :size="$vuetify.display.mobile ? 16 : 18"
              class="ml-1"
            />
          </v-btn>
        </div>

        <!-- 加载状态 -->
        <LoadingSpinner v-if="loading" />

        <!-- 职业列表 -->
        <div v-else-if="filteredRoles.length > 0">
          <v-row>
            <v-col v-for="role in filteredRoles" :key="role.id" cols="12" md="6">
              <v-card rounded="xl" hover border elevation="0" class="role-card hoverable">
                <v-card-text class="pa-4 pa-sm-6" @click="goToRole(role.roleId)">
                  <div class="d-flex align-start justify-space-between mb-3 mb-md-4">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar
                        :color="role.iconColor"
                        :size="$vuetify.display.mobile ? 48 : 56"
                        rounded="lg"
                        class="mr-3 mr-sm-4 flex-shrink-0"
                      >
                        <v-icon
                          :icon="role.icon"
                          color="white"
                          :size="$vuetify.display.mobile ? 24 : 28"
                        />
                      </v-avatar>
                      <div class="min-w-0">
                        <h4 class="text-body-1 text-md-h6 font-weight-bold mb-1 text-truncate">
                          {{ role.title }}
                        </h4>
                        <p class="text-caption text-grey mb-0">{{ role.lastActivity }}</p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="text"
                      :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                      icon="mdi-close"
                      @click.stop="cancelLearning(role.id)"
                    />
                  </div>

                  <v-progress-linear
                    :model-value="role.progress"
                    color="primary"
                    bg-color="grey-lighten-3"
                    :height="$vuetify.display.mobile ? 6 : 8"
                    rounded
                    class="mb-2 mb-md-3"
                  />

                  <div class="d-flex align-center justify-space-between">
                    <span class="text-caption text-md-body-2 text-grey-darken-2">
                      <v-icon
                        icon="mdi-check-circle"
                        :size="$vuetify.display.mobile ? 14 : 16"
                        color="success"
                        class="mr-1"
                      />
                      {{ role.completedCourses }} / {{ role.totalCourses }} 门课程
                    </span>
                    <span class="text-body-2 text-md-body-1 font-weight-bold text-primary"
                      >{{ role.progress }}%</span
                    >
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>
        <div v-else class="text-center py-8 py-md-12">
          <v-icon
            icon="mdi-briefcase"
            :size="$vuetify.display.mobile ? 48 : 64"
            color="grey-lighten-2"
            class="mb-3 mb-md-4"
          />
          <p class="text-body-2 text-md-body-1 text-grey-darken-2">
            {{ statusTab === 'learning' ? '暂无正在学习的职业' : '暂无已完成的职业' }}
          </p>
          <p class="text-caption text-md-body-2 text-grey">开始学习职业路径，系统化提升技能</p>
        </div>
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
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { progressApi } from '@/api'
import { UserProgressState } from '@/enums'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
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

// 取消学习路线图
const { execute: deleteProgress } = useMutation(
  (roadmapId: number) => progressApi.cancelRoadmap(roadmapId),
  {
    successMessage: '已取消学习该职业',
    onSuccess: () => {
      fetchRoadmaps()
    },
  }
)

// 转换路线图数据为组件所需格式
const roles = computed(() => {
  if (!roadmaps.value) return []

  return roadmaps.value.map((userRoadmap) => {
    const roadmap = userRoadmap.roadmap
    // 后端返回的是万分位（0-10000），转换为百分比（0-100）
    const progress = userRoadmap.progressPercent ? userRoadmap.progressPercent / 100 : 0
    // 后端返回的 state：1=进行中, 2=已完成
    const state = userRoadmap.state || UserProgressState.IN_PROGRESS
    const isCompleted = state === UserProgressState.COMPLETED

    // 计算最后活动时间
    const lastActivity = userRoadmap.updatedAt
      ? formatLastActivity(new Date(userRoadmap.updatedAt))
      : '暂无活动'

    return {
      id: userRoadmap.id,
      roleId: roadmap?.id || 0,
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
const filteredRoles = computed(() => {
  return roles.value.filter((role) => role.status === statusTab.value)
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
const goToRole = (roleId: number) => {
  router.push(`/role/${roleId}`)
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
.role-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

.min-w-0 {
  min-width: 0;
}
</style>
