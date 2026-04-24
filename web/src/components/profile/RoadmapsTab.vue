<template>
  <div class="pa-0 pa-sm-1">
    <!-- 顶部筛选栏（仅自己的 profile 显示）-->
    <div v-if="isOwnProfile" class="d-flex align-center mb-4">
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'all' ? 'primary' : 'default'"
        @click="statusFilter = 'all'"
      >
        {{ t('user.profile.all') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'draft' ? 'primary' : 'default'"
        @click="statusFilter = 'draft'"
      >
        {{ t('user.profile.draft') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'pending' ? 'primary' : 'default'"
        @click="statusFilter = 'pending'"
      >
        {{ t('user.profile.pending') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'published' ? 'primary' : 'default'"
        @click="statusFilter = 'published'"
      >
        {{ t('user.profile.published') }}
      </v-btn>
      <v-btn
        variant="text"
        size="small"
        rounded="lg"
        :color="statusFilter === 'rejected' ? 'primary' : 'default'"
        @click="statusFilter = 'rejected'"
      >
        {{ t('user.profile.rejected') }}
      </v-btn>
    </div>

    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading && roadmaps.length === 0" />

    <!-- 路线图列表 -->
    <div v-else-if="roadmaps.length > 0">
      <div class="roadmap-grid">
        <v-card
          v-for="roadmap in roadmaps"
          :key="roadmap.id"
          rounded="lg"
          border
          hover
          class="roadmap-card"
          @click="goToRoadmapDetail(roadmap.id)"
        >
          <v-card-text class="pa-4 position-relative">
            <!-- 删除按钮 -->
            <v-btn
              v-if="isOwnProfile"
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="close-btn"
              @click.stop="deleteRoadmap(roadmap.id)"
            />

            <!-- 图标和标题区域 -->
            <div class="d-flex align-center ga-3 mb-3">
              <div class="icon-container flex-shrink-0">
                <v-icon icon="mdi-map-marker-path" :size="24" color="purple" />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div class="d-flex align-center ga-2">
                  <span
                    class="text-body-1 font-weight-bold text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ roadmap.name }}
                  </span>
                  <v-chip
                    v-if="isOwnProfile"
                    :color="getStatusColor(roadmap.status)"
                    size="x-small"
                    variant="tonal"
                    class="flex-shrink-0"
                  >
                    {{ getStatusText(roadmap.status) }}
                  </v-chip>
                </div>
                <div class="text-body-2 text-medium-emphasis text-truncate">
                  {{ roadmap.nodeCount }} {{ t('roadmap.nodes') }}
                </div>
              </div>
            </div>

            <!-- 描述 -->
            <p class="text-body-2 text-grey-darken-2 mb-3 roadmap-description">
              {{ roadmap.description }}
            </p>

            <!-- 底部：统计信息 + 操作按钮 -->
            <div class="d-flex align-center justify-space-between">
              <div class="d-flex align-center ga-3 text-body-2 text-medium-emphasis">
                <span class="d-flex align-center">
                  <v-icon icon="mdi-account-multiple" size="16" class="mr-1" />
                  {{ roadmap.usageCount }}
                </span>
                <span class="d-flex align-center">
                  <v-icon icon="mdi-star" size="16" class="mr-1" />
                  {{ roadmap.starCount }}
                </span>
              </div>
              <div class="d-flex align-center ga-2">
                <v-btn
                  color="primary"
                  variant="text"
                  size="small"
                  @click.stop="goToRoadmapDetail(roadmap.id)"
                >
                  {{ t('home.viewAll') }}
                </v-btn>
                <v-btn
                  v-if="isOwnProfile"
                  color="grey"
                  variant="text"
                  size="small"
                  @click.stop="editRoadmap(roadmap.id, roadmap.roleId)"
                >
                  {{ t('common.edit') }}
                </v-btn>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="text-center py-4">
        <v-btn
          variant="text"
          color="primary"
          :loading="loading"
          @click="loadMoreRoadmaps({ done: () => {} })"
        >
          {{ t('common.loadMore') }}
        </v-btn>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-map-marker-path"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">
        {{ statusFilter !== 'all' ? t('user.profile.noArticlesFound') : t('learning.noRoadmaps') }}
      </p>
      <p class="text-caption text-md-body-2 text-grey">
        {{
          statusFilter !== 'all' ? t('user.profile.adjustFilters') : t('roadmap.systematicLearning')
        }}
      </p>
    </div>

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="showDeleteDialog"
      :title="t('common.confirm')"
      :message="t('common.delete') + '?'"
      :confirm-text="t('common.confirm')"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { useI18n } from '@/composables/useI18n'
import { userApi } from '@/api'
import { ContentState } from '@/enums'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const props = withDefaults(defineProps<Props>(), {
  userId: null,
  isOwnProfile: false,
})

const { t } = useI18n()

interface Props {
  userId?: number | null
  isOwnProfile?: boolean
}

const router = useRouter()

// 搜索和筛选
const statusFilter = ref<'all' | 'draft' | 'pending' | 'published' | 'rejected'>('all')

// 将 statusFilter 转换为后端 state 值
const getStateValue = (): number | undefined => {
  switch (statusFilter.value) {
    case 'draft':
      return ContentState.DRAFT
    case 'pending':
      return ContentState.SUBMITTED
    case 'published':
      return ContentState.PUBLISHED
    case 'rejected':
      return ContentState.REJECTED
    default:
      return undefined // all - 后端返回除 BANNED 外的所有状态
  }
}

// 使用无限滚动加载路线图
const {
  items: roadmapsData,
  loading,
  hasMore,
  loadMore: loadMoreRoadmaps,
  reset: resetRoadmaps,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    if (props.isOwnProfile || props.userId === null) {
      // 获取当前用户的路线图
      const response = await userApi.getCurrentUserRoadmaps(params.lastId, getStateValue())
      return {
        code: response.code,
        data: response.data || [],
        message: response.message || '',
        hasMore: response.data?.length === 20, // 假设每页20条
      }
    } else {
      // 获取指定用户的路线图
      const response = await userApi.getUserRoadmaps(props.userId, params.lastId)
      return {
        code: response.code,
        data: response.data || [],
        message: response.message || '',
        hasMore: response.data?.length === 20, // 假设每页20条
      }
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: { lastId: undefined },
})

// 监听 statusFilter 变化，重新加载列表
watch(statusFilter, () => {
  resetRoadmaps()
  loadMoreRoadmaps()
})

// 首次加载数据
onMounted(() => {
  if (roadmapsData.value.length === 0) {
    loadMoreRoadmaps({ done: () => {} })
  }
})

// 删除确认对话框
const showDeleteDialog = ref(false)
const roadmapToDelete = ref<number | null>(null)

// 删除路线图
const { execute: deleteRoadmapAction } = useMutation(
  (roadmapId: number) => userApi.deleteRoadmap(roadmapId),
  {
    successMessage: t('user.profile.roadmapDeleted'),
    onSuccess: () => {
      // 刷新列表
      roadmapsData.value = []
      loadMoreRoadmaps()
    },
  }
)

// 转换路线图数据
const roadmaps = computed(() => {
  if (!roadmapsData?.value || !Array.isArray(roadmapsData.value)) return []

  return roadmapsData.value.map((roadmap) => ({
    id: roadmap.id,
    roleId: roadmap.role?.id || roadmap.roleId,
    name: roadmap.role?.name || t('user.profile.unknownRole'),
    role: roadmap.role?.name || t('user.profile.unknownRole'),
    description: roadmap.description || t('hotRanking.noDescription'),
    usageCount: roadmap.learnerCount || 0,
    starCount: roadmap.likeCount || 0,
    nodeCount: roadmap.nodeCount || 0,
    status:
      roadmap.state === 0
        ? 'draft'
        : roadmap.state === 1
          ? 'submitted'
          : roadmap.state === 2
            ? 'published'
            : roadmap.state === 3
              ? 'rejected'
              : roadmap.state === 4
                ? 'banned'
                : 'unknown',
    createdAt: roadmap.createdAt || '',
  }))
})

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'grey',
    submitted: 'warning',
    published: 'success',
    rejected: 'error',
    banned: 'error',
  }
  return colors[status] || 'grey'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: t('user.profile.draft'),
    submitted: t('admin.pending'),
    published: t('user.profile.published'),
    rejected: t('admin.rejected'),
    banned: t('admin.banned'),
  }
  return texts[status] || t('user.profile.unknown')
}

// 跳转到路线图详情
const goToRoadmapDetail = (roadmapId: number) => {
  router.push(`/roadmap/${roadmapId}`)
}

// 编辑路线图
const editRoadmap = (roadmapId: number, roleId?: number) => {
  if (roleId) {
    const editPath = `/role/${roleId}/roadmap/${roadmapId}/edit`
    console.log('跳转到编辑页面:', editPath)
    router.push(editPath)
  } else {
    console.warn('缺少 roleId，无法跳转到编辑页面')
  }
}

// 删除路线图
const deleteRoadmap = (roadmapId: number) => {
  roadmapToDelete.value = roadmapId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (roadmapToDelete.value !== null) {
    await deleteRoadmapAction(roadmapToDelete.value)
  }
  roadmapToDelete.value = null
}
</script>

<style scoped>
.roadmap-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.roadmap-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
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

.roadmap-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}

/* 基于容器宽度的响应式网格 */
.roadmap-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@container (max-width: 900px) {
  .roadmap-grid {
    grid-template-columns: 1fr;
  }
}

/* 启用 container query */
.pa-0 {
  container-type: inline-size;
}
</style>
