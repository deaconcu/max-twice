<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { userServiceV1 } from '@/services/api/v1/apiServiceV1'
import type { Roadmap } from '@/types/roadmap'
import { ContentState } from '@/types/enums'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

interface LoadEventData {
  done: (status: 'ok' | 'empty') => void
}

interface Props {
  userId?: number // 如果提供了 userId，则查看指定用户的路线图；否则查看当前用户的路线图
}

const props = defineProps<Props>()
const router = useRouter()

// 判断是否查看自己的路线图
const isViewingSelf = !props.userId

// 使用 useInfiniteScroll 加载路线图列表
const {
  items: roadmapList,
  loadMore: loadMoreRoadmaps,
  hasMore,
} = useInfiniteScroll<Roadmap>({
  fetchFn: (params) => {
    return isViewingSelf
      ? userServiceV1.getCurrentUserRoadmaps(params.lastId)
      : userServiceV1.getUserRoadmaps(props.userId!, params.lastId)
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: { lastId: 0 },
})

// 适配 v-infinite-scroll 的 load 事件
const loadRoadmaps = async ({ done }: LoadEventData): Promise<void> => {
  await loadMoreRoadmaps()
  done(hasMore.value ? 'ok' : 'empty')
}

// 获取状态颜色
const getStateColor = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return 'orange'
    case ContentState.PUBLISHED:
      return 'success'
    case ContentState.REJECTED:
      return 'error'
    case ContentState.BANNED:
      return 'grey'
    default:
      return 'grey'
  }
}

// 获取状态文本
const getStateText = (state: number): string => {
  switch (state) {
    case ContentState.SUBMITTED:
      return '待审核'
    case ContentState.PUBLISHED:
      return '已发布'
    case ContentState.REJECTED:
      return '已拒绝'
    case ContentState.BANNED:
      return '已封禁'
    default:
      return '未知'
  }
}

// 查看路线图详情
const viewRoadmap = (roadmap: Roadmap): void => {
  if (roadmap.professionId) {
    router.push(`/profession/${roadmap.professionId}`)
  }
}

// 删除相关
const deleteDialog = ref(false)
const roadmapToDelete: Ref<Roadmap | null> = ref(null)

const deleteMessage = computed(() =>
  roadmapToDelete.value
    ? `确定要删除路线图 #${roadmapToDelete.value.id} 吗？此操作无法撤销。`
    : ''
)

// 打开删除确认对话框
const confirmDelete = (roadmap: Roadmap, event: Event): void => {
  event.stopPropagation() // 阻止事件冒泡，避免触发卡片的点击事件
  roadmapToDelete.value = roadmap
  deleteDialog.value = true
}

// 使用 useMutation 删除路线图
const { execute: deleteRoadmapApi, loading: deleting } = useMutation(
  (id: number) => userServiceV1.deleteRoadmap(id),
  {
    successMessage: '删除成功',
    onSuccess: () => {
      // 从列表中移除已删除的路线图
      const index = roadmapList.value.findIndex(r => r.id === roadmapToDelete.value!.id)
      if (index !== -1) {
        roadmapList.value.splice(index, 1)
      }
      deleteDialog.value = false
      roadmapToDelete.value = null
    },
  },
)

// 执行删除
const handleDelete = async (): Promise<void> => {
  if (!roadmapToDelete.value) return
  await deleteRoadmapApi(roadmapToDelete.value.id)
}
</script>

<template>
  <div>
    <v-card flat class="pa-4 rounded-lg mb-4 border">
      <div class="d-flex align-center mb-2">
        <v-icon icon="mdi-map-marker-path" color="primary" size="20" class="mr-2"></v-icon>
        <h3 class="text-h6 font-weight-bold text-grey-darken-3">
          {{ isViewingSelf ? '我创建的路线图' : '创建的路线图' }}
        </h3>
      </div>
      <p class="text-body-2 text-grey-darken-1 mb-0">
        {{ isViewingSelf ? '管理您创建的所有职业路线图' : '查看该用户创建的职业路线图' }}
      </p>
    </v-card>

    <!-- 空状态 -->
    <div v-if="roadmapList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-map-marker-path" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-h6 text-grey-darken-1 mb-2">
        {{ isViewingSelf ? '还没有创建路线图' : '该用户还没有创建路线图' }}
      </p>
      <p class="text-body-2 text-grey-darken-1">
        {{ isViewingSelf ? '创建路线图帮助他人规划学习路径' : '' }}
      </p>
    </div>

    <!-- 路线图列表 -->
    <v-infinite-scroll v-else :items="roadmapList" @load="loadRoadmaps">
      <div
        v-for="roadmap in roadmapList"
        :key="roadmap.id"
        class="mb-4"
      >
        <v-card
          flat
          class="pa-4 rounded-lg border roadmap-card"
          hover
          @click="viewRoadmap(roadmap)"
        >
          <div class="d-flex justify-space-between align-start mb-3">
            <div class="flex-grow-1">
              <div class="d-flex align-center mb-2">
                <!-- 只在查看自己的路线图时显示状态 -->
                <v-chip
                  v-if="isViewingSelf"
                  :color="getStateColor(roadmap.state)"
                  variant="tonal"
                  size="small"
                  class="mr-2"
                >
                  {{ getStateText(roadmap.state) }}
                </v-chip>
                <span class="text-caption text-grey-darken-1">
                  创建于 {{ roadmap.createdAt }}
                </span>
              </div>
              <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-2">
                路线图 #{{ roadmap.id }}
              </h4>
              <p v-if="roadmap.description" class="text-body-2 text-grey-darken-2 mb-2">
                {{ roadmap.description }}
              </p>
            </div>
          </div>

          <v-divider class="my-3"></v-divider>

          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <div class="d-flex align-center mr-4">
                <v-icon icon="mdi-thumb-up-outline" size="16" class="mr-1 text-grey-darken-1"></v-icon>
                <span class="text-caption text-grey-darken-2">{{ roadmap.vote || 0 }}</span>
              </div>
              <div class="d-flex align-center">
                <v-icon icon="mdi-comment-outline" size="16" class="mr-1 text-grey-darken-1"></v-icon>
                <span class="text-caption text-grey-darken-2">{{ roadmap.comment || 0 }}</span>
              </div>
            </div>
            <div class="d-flex align-center gap-2">
              <v-btn
                variant="text"
                color="primary"
                size="small"
                append-icon="mdi-arrow-right"
              >
                查看详情
              </v-btn>
              <!-- 只在查看自己的路线图时显示删除按钮 -->
              <v-btn
                v-if="isViewingSelf"
                variant="text"
                color="error"
                size="small"
                icon="mdi-delete"
                @click="confirmDelete(roadmap, $event)"
              ></v-btn>
            </div>
          </div>

          <!-- 拒绝/封禁原因（只在查看自己的路线图时显示） -->
          <v-alert
            v-if="isViewingSelf && roadmap.reason && (roadmap.state === ContentState.REJECTED || roadmap.state === ContentState.BANNED)"
            type="warning"
            variant="tonal"
            density="compact"
            class="mt-3"
          >
            <template #text>
              <span class="text-body-2">
                <strong>原因：</strong>{{ roadmap.reason }}
              </span>
            </template>
          </v-alert>
        </v-card>
      </div>

      <template #empty>
        <div class="text-center py-4">
          <span class="text-body-2 text-grey-darken-1">没有更多路线图了</span>
        </div>
      </template>
    </v-infinite-scroll>

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model="deleteDialog"
      title="确认删除"
      :message="deleteMessage"
      confirm-text="删除"
      cancel-text="取消"
      :loading="deleting"
      @confirm="handleDelete"
    />
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.roadmap-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.roadmap-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: rgba(25, 118, 210, 0.3);
}
</style>
