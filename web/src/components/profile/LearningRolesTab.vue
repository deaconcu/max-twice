<template>
  <div>
    <div class="pa-0 pa-sm-1">
      <!-- 顶部操作栏 -->
      <div
        class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mb-md-6 ga-3"
      >
        <!-- Tab 切换 -->
        <div class="d-flex align-center">
          <v-btn
            variant="text"
            size="small"
            rounded="lg"
            :color="statusTab === 'learning' ? 'primary' : 'default'"
            @click="statusTab = 'learning'"
          >
            正在学习
          </v-btn>
          <v-btn
            variant="text"
            size="small"
            rounded="lg"
            :color="statusTab === 'completed' ? 'primary' : 'default'"
            @click="statusTab = 'completed'"
          >
            已完成
          </v-btn>
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <!-- 职业列表 -->
      <div v-else-if="filteredRoles.length > 0">
        <div class="role-grid">
          <v-card
            v-for="role in filteredRoles"
            :key="role.id"
            rounded="lg"
            border
            hover
            class="role-card"
            @click="goToRole(role.roleId)"
          >
            <v-card-text class="pa-4 position-relative">
              <v-btn
                color="grey"
                variant="text"
                size="x-small"
                icon="mdi-close"
                class="close-btn"
                @click.stop="cancelLearning(role.id)"
              />
              <div class="d-flex align-center ga-3 mb-3">
                <div class="icon-container flex-shrink-0">
                  <DynamicIcon
                    :icon="role.icon"
                    default-icon="mdi-briefcase-variant"
                    :size="24"
                    :color="role.iconColor"
                  />
                </div>
                <div class="flex-grow-1" style="min-width: 0">
                  <div
                    class="text-body-1 font-weight-bold text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ role.title }}
                  </div>
                  <div class="text-caption text-medium-emphasis">
                    {{ role.totalCourses }} 个知识节点
                  </div>
                </div>
              </div>
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-caption text-medium-emphasis">学习进度</span>
                <span class="text-caption font-weight-bold text-grey">
                  {{ role.progress }}%
                </span>
              </div>
              <v-progress-linear
                :model-value="role.progress"
                color="grey-lighten-3"
                height="6"
                rounded
              />
            </v-card-text>
          </v-card>
        </div>
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
import { getColorByString } from '@/utils/color'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

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

    const title = roadmap?.profession?.name || '未知职业'

    return {
      id: userRoadmap.id,
      roleId: roadmap?.id || 0,
      title,
      progress,
      totalCourses: roadmap?.nodeCount || 0,
      icon: roadmap?.profession?.icon || 'mdi-briefcase-variant',
      iconColor: getColorByString(title),
      status: isCompleted ? 'completed' : 'learning',
    }
  })
})

// 根据状态过滤职业列表
const filteredRoles = computed(() => {
  return roles.value.filter((role) => role.status === statusTab.value)
})

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
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
}

/* 基于容器宽度的响应式网格 */
.role-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@container (max-width: 1200px) {
  .role-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@container (max-width: 750px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}

/* 启用 container query */
.pa-0 {
  container-type: inline-size;
}

.icon-container {
  width: 48px;
  height: 48px;
  border: 1px solid rgb(var(--v-theme-outline));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cursor-pointer {
  cursor: pointer;
  transition: opacity 0.15s;
}

.cursor-pointer:hover {
  opacity: 0.7;
}
</style>
