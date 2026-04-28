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
            {{ t('user.profile.learningTab') }}
          </v-btn>
          <v-btn
            variant="text"
            size="small"
            rounded="lg"
            :color="statusTab === 'completed' ? 'primary' : 'default'"
            @click="statusTab = 'completed'"
          >
            {{ t('user.profile.completedTab') }}
          </v-btn>
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <!-- 职业列表 -->
      <div v-else-if="roles.length > 0">
        <div class="role-grid">
          <v-card
            v-for="role in roles"
            :key="role.id"
            rounded="lg"
            border
            hover
            class="role-card"
            @click="goToRoadmap(role.roadmapId)"
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
                    {{ role.totalCourses }} {{ t('rightSidebar.knowledgeNodes') }}
                  </div>
                </div>
              </div>
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-caption text-medium-emphasis">{{
                  t('user.profile.progress')
                }}</span>
                <span class="text-caption font-weight-bold text-grey"> {{ role.progress }}% </span>
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
      <!-- 加载更多 -->
      <div v-if="hasNextPage" class="text-center py-4">
        <v-btn variant="text" color="primary" :loading="loading" @click="fetchNextPage()">
          {{ t('common.loadMore') }}
        </v-btn>
      </div>
      <div v-else class="text-center py-8 py-md-12">
        <v-icon
          icon="mdi-briefcase"
          :size="$vuetify.display.mobile ? 48 : 64"
          color="grey-lighten-2"
          class="mb-3 mb-md-4"
        />
        <p class="text-body-2 text-md-body-1 text-grey-darken-2">
          {{
            statusTab === 'learning'
              ? t('user.profile.noLearningRoles')
              : t('user.profile.noCompletedRoles')
          }}
        </p>
        <p class="text-caption text-md-body-2 text-grey">{{ t('learning.browseRoadmapsDesc') }}</p>
      </div>
    </div>
    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="showDeleteDialog"
      :title="t('common.confirm')"
      :message="t('learning.confirmExit')"
      :confirm-text="t('common.confirm')"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAllRoadmapsProgressQuery, useCancelRoadmapMutation } from '@/queries/progress'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
import { getColorByString } from '@/utils/color'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

const { t } = useI18n()
const router = useRouter()

// Tab 状态
const statusTab = ref<'learning' | 'completed'>('learning')

// 删除确认对话框
const showDeleteDialog = ref(false)
const roadmapToDelete = ref<number | null>(null)

// 获取用户路线图数据（queryKey 随 statusTab 变化自动重新请求）
const {
  data: roadmapsData,
  isLoading: loading,
  fetchNextPage,
  hasNextPage,
} = useAllRoadmapsProgressQuery(statusTab)

// 展开分页数据
const roadmaps = computed(() => roadmapsData.value?.pages.flat() ?? [])

// 取消学习路线图
const { mutate: cancelRoadmapMutate } = useCancelRoadmapMutation()

const deleteProgress = (roadmapId: number) => {
  cancelRoadmapMutate(roadmapId, {
    onSuccess: () => {
      getGlobalSnackbar()?.(t('user.profile.roleUnlearned'), 'success')
    },
  })
}

// 转换路线图数据为组件所需格式
const roles = computed(() => {
  if (!roadmaps.value) return []

  return roadmaps.value.map((userLearning: any) => {
    // 后端返回的是 UserLearningDTO，关联对象在 object 字段
    const roadmap = userLearning.object
    // 后端返回的是万分位（0-10000），转换为百分比（0-100）
    const progress = userLearning.progressPercent ? userLearning.progressPercent / 100 : 0

    const title = roadmap?.roleName || t('user.profile.unknownRole')

    return {
      id: userLearning.id,
      roadmapId: userLearning.objectId,
      title,
      progress,
      totalCourses: roadmap?.nodeCount || 0,
      icon: roadmap?.roleIcon || 'mdi-briefcase-variant',
      iconColor: getColorByString(title),
    }
  })
})

// 跳转到职业路线详情
const goToRoadmap = (roadmapId: number) => {
  router.push(`/roadmap/${roadmapId}`)
}

// 取消学习职业
const cancelLearning = (roadmapId: number) => {
  roadmapToDelete.value = roadmapId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (roadmapToDelete.value !== null) {
    deleteProgress(roadmapToDelete.value)
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
