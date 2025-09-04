<script setup lang="ts">
  import { inject } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { progressServiceV1, roadmapServiceV1 } from '@/services/api/v1/apiServiceV1'
  import RoadmapCard from './RoadmapCard.vue'

  interface Roadmap {
    id: number
    upvoted?: boolean
    learning?: boolean
    [key: string]: any
  }

  interface RoadmapUpdate {
    vote?: number
    upvoted?: boolean
    learning?: boolean
    [key: string]: any
  }

  interface Props {
    roadmaps?: Roadmap[]
    loading?: boolean
    error?: string | null
    pinnedRoadmaps?: (string | number)[]
    professionId?: number 
  }

  interface Emits {
    (e: 'open-detail', roadmap: Roadmap): void
    (e: 'copy-roadmap', roadmap: Roadmap): void
    (e: 'create-roadmap'): void
    (e: 'roadmaps-updated', roadmapId: number, status: string): void
    (e: 'update-roadmap', roadmapId: number, updates: RoadmapUpdate): void
  }

  const props = withDefaults(defineProps<Props>(), {
    roadmaps: () => [],
    loading: false,
    error: null,
    pinnedRoadmaps: () => [],
    professionId: 1,
  })

  const emit = defineEmits<Emits>()

  const { t } = useI18n()
  const showSnackbar = inject<(message: string) => void>('showSnackbar')

  // 检查是否置顶
  const isPinned = (roadmapId: number): boolean => {
    return props.pinnedRoadmaps.includes(roadmapId)
  }

  // 投票功能
  const handleVote = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation() // 阻止卡片点击事件

    try {
      const response = await roadmapServiceV1.upvoteRoadmap(roadmap.id)
      if (response.code === 200) {
        // 通过事件通知父组件更新数据
        emit('update-roadmap', roadmap.id, {
          vote: response.data.vote,
          upvoted: response.data.upvoted,
        })
        if (roadmap.upvoted) {
          showSnackbar?.(t('roadmap.voteSuccess'))
        } else {
          showSnackbar?.(t('roadmap.voteCancel'))
        }
      } else {
        showSnackbar?.(t('roadmap.voteFailed'))
      }
    } catch {
      showSnackbar?.('投票失败，请稍后重试')
    }
  }

  // 置顶功能
  const handleTogglePin = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation() // 阻止卡片点击事件

    try {
      const response = await roadmapServiceV1.pinRoadmap(props.professionId, roadmap.id)

      if (response.code === 200) {
        const status = response.data

        if (status === 'pinned') {
          showSnackbar?.(t('roadmap.pinSuccess'))
        } else if (status === 'unpinned') {
          showSnackbar?.(t('roadmap.unpinSuccess'))
        }

        // 通知父组件更新数据和重新排序
        emit('roadmaps-updated', roadmap.id, status)
      } else {
        showSnackbar?.(t('roadmap.pinFailed'))
      }
    } catch {
      showSnackbar?.('置顶操作失败，请稍后重试')
    }
  }

  // 开始学习功能
  const handleStartLearning = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation() // 阻止卡片点击事件

    try {
      const response = await progressServiceV1.startRoadmap(roadmap.id)

      if (response.code === 200) {
        showSnackbar?.(t('roadmap.startLearningSuccess'))
        // 通过事件通知父组件更新数据
        emit('update-roadmap', roadmap.id, { learning: response.data })
      } else {
        showSnackbar?.(t('roadmap.startLearningFailed'))
      }
    } catch {
      showSnackbar?.(t('roadmap.startLearningFailed'))
    }
  }

  // 事件处理
  const handleOpenDetail = (roadmap: Roadmap): void => {
    emit('open-detail', roadmap)
  }

  const handleCopy = (roadmap: Roadmap, event: Event): void => {
    event.stopPropagation() // 阻止卡片点击事件
    emit('copy-roadmap', roadmap)
  }

  const handleCreateRoadmap = (): void => {
    emit('create-roadmap')
  }
</script>

<template>
  <div class="roadmap-list">
    <!-- 加载和错误提示 -->
    <div v-if="loading || error" class="text-center py-8">
      <v-progress-circular
        v-if="loading"
        indeterminate
        color="primary"
        size="64"
      ></v-progress-circular>
      <p v-if="loading" class="text-grey-darken-2 mt-4">正在加载课程表...</p>
      <v-alert v-if="error" type="error" variant="tonal" class="mt-4">{{ error }}</v-alert>
    </div>

    <!-- 课程表列表 -->
    <div v-else>
      <v-row v-if="roadmaps.length > 0">
        <RoadmapCard
          v-for="roadmap in roadmaps"
          :key="roadmap.id"
          :roadmap="roadmap"
          :is-pinned="isPinned(roadmap.id)"
          @open-detail="handleOpenDetail"
          @vote="handleVote"
          @copy="handleCopy"
          @toggle-pin="handleTogglePin"
          @start-learning="handleStartLearning"
        />
      </v-row>

      <!-- 空状态 -->
      <v-row v-else>
        <v-col cols="12">
          <v-card flat class="text-center py-12" color="grey-lighten-5" rounded="lg">
            <v-icon
              icon="mdi-book-open-page-variant-outline"
              size="64"
              color="grey-lighten-1"
              class="mb-4"
            ></v-icon>
            <h3 class="text-h6 text-grey-darken-1 mb-2">暂无课程表</h3>
            <p class="text-body-2 text-grey mb-4">{{ t('roadmap.firstToCreate') }}</p>
            <v-btn variant="flat" color="primary" @click="handleCreateRoadmap">
              <v-icon icon="mdi-plus" class="mr-2"></v-icon>
              {{ t('roadmap.createFirst') }}
            </v-btn>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </div>
</template>

<style scoped>
</style>