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
        :color="statusFilter === 'unpublished' ? 'primary' : 'default'"
        @click="statusFilter = 'unpublished'"
      >
        {{ t('user.profile.draft') }}
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMyRoadmapsQuery, useUserRoadmapsQuery, useDeleteRoadmapMutation } from '@/queries/user'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
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

// 搜索和筛选（revision 模型主体只有 NEVER_PUBLISHED / PUBLISHED / BANNED；BANNED 由后端过滤掉）
const statusFilter = ref<'all' | 'unpublished' | 'published'>('all')

// 将 statusFilter 转换为后端 RoadmapState 字符串
const stateValue = computed((): string | undefined => {
  switch (statusFilter.value) {
    case 'unpublished':
      return 'NEVER_PUBLISHED'
    case 'published':
      return 'PUBLISHED'
    default:
      return undefined
  }
})

const isOwn = computed(() => props.isOwnProfile || props.userId === null)

// 自己的路线图
const {
  data: myRoadmapsData,
  isLoading: myLoading,
  hasNextPage: myHasMore,
  fetchNextPage: myFetchNext,
} = useMyRoadmapsQuery(stateValue, isOwn)

// 他人的路线图
const {
  data: userRoadmapsData,
  isLoading: userLoading,
  hasNextPage: userHasMore,
  fetchNextPage: userFetchNext,
} = useUserRoadmapsQuery(
  computed(() => props.userId ?? 0),
  computed(() => !isOwn.value && !!props.userId)
)

const roadmapsData = computed(() => {
  if (isOwn.value) return myRoadmapsData.value?.pages.flatMap((p) => p as unknown[]) ?? []
  return userRoadmapsData.value?.pages.flatMap((p) => p as unknown[]) ?? []
})

const loading = computed(() => (isOwn.value ? myLoading.value : userLoading.value))
const hasMore = computed(() => (isOwn.value ? !!myHasMore.value : !!userHasMore.value))

// 加载更多
const loadMoreRoadmaps = async ({ done }: { done: () => void } = { done: () => {} }) => {
  if (isOwn.value) await myFetchNext()
  else await userFetchNext()
  done()
}

// 删除确认对话框
const showDeleteDialog = ref(false)
const roadmapToDelete = ref<number | null>(null)

// 删除路线图
const { mutate: deleteRoadmapMutate } = useDeleteRoadmapMutation()

const deleteRoadmapAction = (roadmapId: number) => {
  deleteRoadmapMutate(roadmapId, {
    onSuccess: () => {
      getGlobalSnackbar()?.(t('user.profile.roadmapDeleted'), 'success')
      // useDeleteRoadmapMutation 内部已 invalidateQueries
    },
  })
}

// 转换路线图数据
const roadmaps = computed(() => {
  if (!roadmapsData.value.length) return []

  return roadmapsData.value.map((item) => {
    const roadmap = item as Record<string, any>
    return {
      id: roadmap.id,
      roleId: roadmap.role?.id || roadmap.roleId,
      name: roadmap.role?.name || t('user.profile.unknownRole'),
      role: roadmap.role?.name || t('user.profile.unknownRole'),
      description: roadmap.description || t('hotRanking.noDescription'),
      usageCount: roadmap.learnerCount || 0,
      starCount: roadmap.likeCount || 0,
      nodeCount: roadmap.nodeCount || 0,
      status:
        roadmap.state === 'PUBLISHED'
          ? 'published'
          : roadmap.state === 'BANNED'
            ? 'banned'
            : 'unpublished',
      createdAt: roadmap.createdAt || '',
    }
  })
})

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    unpublished: 'grey',
    published: 'success',
    banned: 'error',
  }
  return colors[status] || 'grey'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    unpublished: t('user.profile.draft'),
    published: t('user.profile.published'),
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
const confirmDelete = () => {
  if (roadmapToDelete.value !== null) {
    deleteRoadmapAction(roadmapToDelete.value)
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
