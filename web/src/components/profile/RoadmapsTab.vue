<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">创建的路线图</h4>
            <p class="text-body-2 text-grey mb-0">规划您的学习路径，系统化掌握技能。</p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-map" size="18" color="grey" class="mr-2 mt-1" />
              <span>绘制学习路线</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-share-variant" size="18" color="grey" class="mr-2 mt-1" />
              <span>分享给他人</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-progress-check" size="18" color="grey" class="mr-2 mt-1" />
              <span>跟踪学习进度</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-6">
          <div></div>
        </div>

        <!-- 路线图列表 -->
        <v-infinite-scroll v-if="roadmaps.length > 0" :items="roadmaps" @load="onLoadMore">
          <template v-for="(roadmap, index) in roadmaps" :key="roadmap.id">
            <v-card
              rounded="xl"
              hover
              border
              elevation="0"
              class="roadmap-card mb-6 hoverable"
              @click="goToRoadmap(roadmap.id)"
            >
              <v-card-text class="pa-6">
                <div class="d-flex align-start justify-space-between mb-4">
                  <!-- 左侧：图标和标题 -->
                  <div class="d-flex align-center flex-grow-1">
                    <v-avatar color="purple-lighten-5" size="56" rounded="lg" class="mr-4">
                      <v-icon icon="mdi-map-marker-path" color="purple" size="28" />
                    </v-avatar>
                    <div class="flex-grow-1">
                      <div class="d-flex align-center mb-1">
                        <h4 class="text-h6 font-weight-bold text-grey-darken-4 mr-2">
                          {{ roadmap.name }}
                        </h4>
                        <v-chip :color="getStatusColor(roadmap.status)" size="small" variant="flat">
                          {{ getStatusText(roadmap.status) }}
                        </v-chip>
                      </div>
                      <p class="text-caption text-grey mb-0">{{ roadmap.profession }}</p>
                    </div>
                  </div>

                  <!-- 右侧：删除按钮 -->
                  <v-btn
                    color="grey"
                    variant="text"
                    size="small"
                    icon="mdi-delete"
                    @click.stop="deleteRoadmap(roadmap.id)"
                  >
                    <v-icon>mdi-delete</v-icon>
                    <v-tooltip activator="parent" location="top">删除路线图</v-tooltip>
                  </v-btn>
                </div>

                <!-- 路线图描述 -->
                <p class="text-body-2 text-grey-darken-2 mb-4 roadmap-description">
                  {{ roadmap.description }}
                </p>

                <!-- 统计信息 -->
                <div class="d-flex align-center justify-space-between">
                  <div class="d-flex align-center text-body-2 text-grey" style="gap: 16px">
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-account-multiple" size="16" color="grey" class="mr-1" />
                      {{ roadmap.usageCount }} 人使用
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-star" size="16" color="grey" class="mr-1" />
                      {{ roadmap.starCount }} 收藏
                    </div>
                    <div class="d-flex align-center">
                      <v-icon icon="mdi-timeline" size="16" color="grey" class="mr-1" />
                      {{ roadmap.nodeCount }} 节点
                    </div>
                  </div>

                  <!-- 操作按钮 -->
                  <div class="d-flex align-center" style="gap: 8px">
                    <v-btn
                      color="primary"
                      variant="flat"
                      rounded="lg"
                      size="small"
                      @click.stop="goToRoadmap(roadmap.id)"
                    >
                      <v-icon icon="mdi-eye" size="18" class="mr-1" />
                      查看
                    </v-btn>
                    <v-btn
                      variant="tonal"
                      rounded="lg"
                      size="small"
                      color="grey-darken-2"
                      @click.stop="goToRoadmap(roadmap.id)"
                    >
                      <v-icon icon="mdi-pencil" size="18" class="mr-1" />
                      编辑
                    </v-btn>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </template>
        </v-infinite-scroll>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-map-marker-path" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">暂无创建的路线图</p>
          <p class="text-body-2 text-grey">创建学习路线图，规划职业发展路径</p>
        </div>
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import { userApi } from '@/api'

const router = useRouter()

// 使用无限滚动加载路线图
const {
  items: roadmapsData,
  loading,
  hasMore,
  loadMore: loadMoreRoadmaps,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await userApi.getCurrentUserRoadmaps(params.lastId)
    return {
      code: response.code,
      data: response.data || [],
      message: response.message || '',
      hasMore: response.data && response.data.length > 0,
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
    name: roadmap.profession?.name || '未知职业',
    profession: roadmap.profession?.name || '未知职业',
    description: roadmap.description || '暂无描述',
    usageCount: roadmap.learnerCount || 0,
    starCount: roadmap.vote || 0,
    nodeCount: roadmap.nodeCount || 0,
    status: roadmap.state === 1 ? 'public' : roadmap.state === 0 ? 'draft' : 'private',
    createdAt: roadmap.createdAt || '',
  }))
})

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    public: 'success',
    private: 'grey',
    draft: 'warning',
  }
  return colors[status] || 'grey'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    public: '公开',
    private: '私密',
    draft: '草稿',
  }
  return texts[status] || '未知'
}

// 跳转到路线图详情
const goToRoadmap = (roadmapId: number) => {
  router.push(`/roadmap/${roadmapId}`)
}

// 删除路线图
const deleteRoadmap = async (roadmapId: number) => {
  if (confirm('确定要删除该路线图吗？此操作不可恢复。')) {
    await deleteRoadmapAction(roadmapId)
  }
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.roadmap-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

.roadmap-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 48px;
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
