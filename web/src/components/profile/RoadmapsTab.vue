<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的路线图</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">规划您的学习路径，系统化掌握技能。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-map" size="14" class="mr-1" />
              绘制学习路线
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-share-variant" size="14" class="mr-1" />
              分享给他人
            </div>
            <div>
              <v-icon icon="mdi-progress-check" size="14" class="mr-1" />
              跟踪学习进度
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1" />
        </div>

        <!-- 路线图列表 -->
        <div v-if="roadmaps.length > 0">
          <v-row>
            <v-col v-for="roadmap in roadmaps" :key="roadmap.id" cols="12">
              <v-card border rounded="lg" hover class="hoverable">
                <v-card-text class="pa-4">
                  <div class="d-flex" style="gap: 16px">
                    <!-- 左侧内容区 -->
                    <div class="flex-grow-1">
                      <!-- 路线图头部 -->
                      <div class="d-flex align-center justify-space-between mb-3">
                        <div class="d-flex align-center">
                          <v-avatar color="purple-lighten-5" size="40" rounded="md" class="mr-3">
                            <v-icon icon="mdi-map-marker-path" color="purple" size="20" />
                          </v-avatar>
                          <div>
                            <h4 class="text-body-1 font-weight-bold mb-1">{{ roadmap.name }}</h4>
                            <p class="text-caption text-grey mb-0">{{ roadmap.profession }}</p>
                          </div>
                        </div>
                        <!-- 状态标签 -->
                        <v-chip :color="getStatusColor(roadmap.status)" size="small" variant="flat">
                          {{ getStatusText(roadmap.status) }}
                        </v-chip>
                      </div>

                      <!-- 路线图描述 -->
                      <p class="text-body-2 text-grey-darken-2 mb-3">{{ roadmap.description }}</p>

                      <!-- 统计信息 -->
                      <div
                        class="d-flex align-center mb-3 text-caption text-grey"
                        style="gap: 16px"
                      >
                        <div>
                          <v-icon icon="mdi-account-multiple" size="14" class="mr-1" />
                          {{ roadmap.usageCount }} 人使用
                        </div>
                        <div>
                          <v-icon icon="mdi-star" size="14" class="mr-1" />
                          {{ roadmap.starCount }} 收藏
                        </div>
                        <div>
                          <v-icon icon="mdi-timeline" size="14" class="mr-1" />
                          {{ roadmap.nodeCount }} 节点
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex" style="gap: 8px">
                        <v-btn
                          color="primary"
                          variant="outlined"
                          rounded="md"
                          density="compact"
                          @click.stop="goToRoadmap(roadmap.id)"
                        >
                          <v-icon icon="mdi-eye" size="16" class="mr-1" />
                          查看
                        </v-btn>
                        <v-btn
                          variant="text"
                          rounded="md"
                          density="compact"
                          color="grey"
                          @click.stop="goToRoadmap(roadmap.id)"
                        >
                          <v-icon icon="mdi-pencil" size="16" class="mr-1" />
                          编辑
                        </v-btn>
                        <v-btn
                          variant="text"
                          rounded="md"
                          density="compact"
                          color="error"
                          @click.stop="deleteRoadmap(roadmap.id)"
                        >
                          <v-icon icon="mdi-delete" size="16" class="mr-1" />
                          删除
                        </v-btn>
                      </div>
                    </div>

                    <!-- 右侧缩略图 -->
                    <div class="roadmap-thumbnail">
                      <div
                        class="d-flex align-center justify-center"
                        style="width: 200px; height: 150px; background: #f5f5f5; border-radius: 8px"
                      >
                        <v-icon icon="mdi-sitemap" size="48" color="grey-lighten-1" />
                      </div>
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { userApi } from '@/api'

const router = useRouter()

// 获取用户创建的路线图
const {
  data: roadmapsData,
  loading,
  execute: fetchRoadmaps,
} = useFetch({
  fetchFn: userApi.getCurrentUserRoadmaps,
  immediate: true,
  defaultValue: [],
})

// 删除路线图
const { execute: deleteRoadmapAction } = useMutation(
  (roadmapId: number) => userApi.deleteRoadmap(roadmapId),
  {
    successMessage: '已删除该路线图',
    onSuccess: () => {
      fetchRoadmaps()
    },
  }
)

// 转换路线图数据
const roadmaps = computed(() => {
  if (!roadmapsData.value) return []

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
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.roadmap-thumbnail {
  cursor: pointer;
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
