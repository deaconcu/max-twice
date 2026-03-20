<template>
  <div class="pa-0 pa-sm-2">
    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading && roadmaps.length === 0" />

    <!-- 路线图列表 -->
    <v-infinite-scroll v-else-if="roadmaps.length > 0" :items="roadmaps" @load="onLoadMore">
      <template v-for="(roadmap, index) in roadmaps" :key="roadmap.id">
        <v-card
          rounded="xl"
          hover
          border
          elevation="0"
          class="roadmap-card mb-4 mb-md-6 hoverable"
          @click="goToRoadmapDetail(roadmap.id)"
        >
          <v-card-text class="pa-4 pa-sm-6">
            <div class="d-flex align-start justify-space-between mb-3 mb-md-4">
              <!-- 左侧：图标和标题 -->
              <div class="d-flex align-center flex-grow-1">
                <v-avatar
                  color="purple-lighten-5"
                  :size="$vuetify.display.mobile ? 48 : 56"
                  rounded="lg"
                  class="mr-3 mr-sm-4 flex-shrink-0"
                >
                  <v-icon
                    icon="mdi-map-marker-path"
                    color="purple"
                    :size="$vuetify.display.mobile ? 24 : 28"
                  />
                </v-avatar>
                <div class="flex-grow-1 min-w-0">
                  <div
                    class="d-flex flex-column flex-sm-row align-start align-sm-center mb-1 ga-2"
                  >
                    <h4
                      class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 text-truncate"
                    >
                      {{ roadmap.name }}
                    </h4>
                    <v-chip
                      :color="getStatusColor(roadmap.status)"
                      :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                      variant="flat"
                    >
                      {{ getStatusText(roadmap.status) }}
                    </v-chip>
                  </div>
                  <p class="text-caption text-grey mb-0 text-truncate">
                    {{ roadmap.profession }}
                  </p>
                </div>
              </div>

              <!-- 右侧：删除按钮 -->
              <v-btn
                v-if="isOwnProfile"
                color="grey"
                variant="text"
                :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                icon="mdi-delete"
                @click.stop="deleteRoadmap(roadmap.id)"
              >
                <v-icon>mdi-delete</v-icon>
                <v-tooltip activator="parent" location="top">删除路线图</v-tooltip>
              </v-btn>
            </div>

            <!-- 路线图描述 -->
            <p
              class="text-caption text-md-body-2 text-grey-darken-2 mb-3 mb-md-4 roadmap-description"
            >
              {{ roadmap.description }}
            </p>

            <!-- 统计信息 -->
            <div
              class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between ga-3"
            >
              <div
                class="d-flex align-center flex-wrap text-caption text-md-body-2 text-grey"
                style="gap: 12px"
              >
                <div class="d-flex align-center">
                  <v-icon
                    icon="mdi-account-multiple"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    color="grey"
                    class="mr-1"
                  />
                  {{ roadmap.usageCount }} <span class="d-none d-sm-inline">人使用</span>
                </div>
                <div class="d-flex align-center">
                  <v-icon
                    icon="mdi-star"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    color="grey"
                    class="mr-1"
                  />
                  {{ roadmap.starCount }} <span class="d-none d-sm-inline">收藏</span>
                </div>
                <div class="d-flex align-center">
                  <v-icon
                    icon="mdi-timeline"
                    :size="$vuetify.display.mobile ? 14 : 16"
                    color="grey"
                    class="mr-1"
                  />
                  {{ roadmap.nodeCount }} <span class="d-none d-sm-inline">节点</span>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="d-flex align-center" style="gap: 8px">
                <v-btn
                  color="primary"
                  variant="flat"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                  @click.stop="goToRoadmapDetail(roadmap.id)"
                >
                  <v-icon
                    icon="mdi-eye"
                    :size="$vuetify.display.mobile ? 14 : 18"
                    :class="$vuetify.display.mobile ? '' : 'mr-1'"
                  />
                  <span class="d-none d-sm-inline">查看</span>
                </v-btn>
                <v-btn
                  variant="tonal"
                  rounded="lg"
                  :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                  color="grey-darken-2"
                  @click.stop="editRoadmap(roadmap.id, roadmap.professionId)"
                >
                  <v-icon
                    icon="mdi-pencil"
                    :size="$vuetify.display.mobile ? 14 : 18"
                    :class="$vuetify.display.mobile ? '' : 'mr-1'"
                  />
                  <span class="d-none d-sm-inline">编辑</span>
                </v-btn>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </template>
    </v-infinite-scroll>

    <!-- 空状态 -->
    <div v-else class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-map-marker-path"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">暂无创建的路线图</p>
      <p class="text-caption text-md-body-2 text-grey">创建学习路线图，规划职业发展路径</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { userApi } from '@/api'

interface Props {
  userId?: number | null
  isOwnProfile?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  userId: null,
  isOwnProfile: false,
})
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()

// 使用无限滚动加载路线图
const {
  items: roadmapsData,
  loading,
  hasMore,
  loadMore: loadMoreRoadmaps,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    if (props.isOwnProfile || props.userId === null) {
      // 获取当前用户的路线图
      const response = await userApi.getCurrentUserRoadmaps(params.lastId)
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

// 首次加载数据
onMounted(() => {
  if (roadmapsData.value.length === 0) {
    loadMoreRoadmaps({ done: () => {} })
  }
})

// 加载更多
const onLoadMore = async ({ done }: { done: (status: string) => void }) => {
  await loadMoreRoadmaps({
    done: () => {
      done(hasMore.value ? 'ok' : 'empty')
    },
  })
}

// 删除路线图
const { execute: deleteRoadmapAction } = useMutation(
  (roadmapId: number) => userApi.deleteRoadmap(roadmapId),
  {
    successMessage: '已删除该路线图',
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
    professionId: roadmap.profession?.id || roadmap.professionId,
    name: roadmap.profession?.name || '未知职业',
    profession: roadmap.profession?.name || '未知职业',
    description: roadmap.description || '暂无描述',
    usageCount: roadmap.learnerCount || 0,
    starCount: roadmap.likeCount || 0,
    nodeCount: roadmap.nodeCount || 0,
    status: roadmap.state === 0 ? 'draft'
          : roadmap.state === 1 ? 'submitted'
          : roadmap.state === 2 ? 'published'
          : roadmap.state === 3 ? 'rejected'
          : roadmap.state === 4 ? 'banned'
          : 'unknown',
    createdAt: roadmap.createdAt || '',
  }))
})

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'warning',
    submitted: 'info',
    published: 'success',
    rejected: 'error',
    banned: 'error',
  }
  return colors[status] || 'grey'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: '草稿',
    submitted: '审核中',
    published: '已发布',
    rejected: '已拒绝',
    banned: '已封禁',
  }
  return texts[status] || '未知'
}

// 跳转到路线图详情
const goToRoadmapDetail = (roadmapId: number) => {
  router.push(`/roadmap/${roadmapId}`)
}

// 编辑路线图
const editRoadmap = (roadmapId: number, professionId?: number) => {
  if (professionId) {
    const editPath = `/role/${professionId}/roadmap/${roadmapId}/edit`
    console.log('跳转到编辑页面:', editPath)
    router.push(editPath)
  } else {
    console.warn('缺少 professionId，无法跳转到编辑页面')
  }
}

// 删除路线图
const deleteRoadmap = async (roadmapId: number) => {
  if (confirm('确定要删除该路线图吗？此操作不可恢复。')) {
    await deleteRoadmapAction(roadmapId)
  }
}
</script>

<style scoped>
.roadmap-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

.roadmap-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}

@media (min-width: 600px) {
  .roadmap-description {
    min-height: 48px;
  }
}

.min-w-0 {
  min-width: 0;
}
</style>
