<script setup lang="ts">
  import { inject } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { progressServiceV1, roadmapServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { ObjectType, VoteType } from '@/types/enums'
  import type { Roadmap } from '@/types/roadmap'
  import RoadmapCard from './RoadmapCard.vue'
  import { useMutation } from '@/composables/useMutation'

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
    (e: 'roadmaps-updated', roadmapId: number, pinned: boolean): void
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

  // 使用 useMutation 处理投票
  const { execute: executeVote } = useMutation(
    (roadmapId: number) => upvoteServiceV1.upvote(roadmapId, ObjectType.ROADMAP, VoteType.NORMAL),
    {
      onSuccess: (response, roadmapId) => {
        emit('update-roadmap', roadmapId, {
          vote: response.upvotes,
          upvoted: response.upvoted,
        })
        showSnackbar?.(response.upvoted ? t('roadmap.voteSuccess') : t('roadmap.voteCancel'))
      },
      onError: () => {
        showSnackbar?.(t('roadmap.voteFailed'))
      },
    },
  )

  // 投票功能
  const handleVote = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation()
    await executeVote(roadmap.id)
  }

  // 使用 useMutation 处理置顶
  const { execute: executeTogglePin } = useMutation(
    (data: { professionId: number; roadmapId: number }) =>
      roadmapServiceV1.pinRoadmap(data.professionId, data.roadmapId),
    {
      onSuccess: (pinned, data) => {
        showSnackbar?.(pinned ? t('roadmap.pinSuccess') : t('roadmap.unpinSuccess'))
        emit('roadmaps-updated', data.roadmapId, pinned)
      },
      onError: () => {
        showSnackbar?.(t('roadmap.pinFailed'))
      },
    },
  )

  // 置顶功能
  const handleTogglePin = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation()
    await executeTogglePin({ professionId: props.professionId, roadmapId: roadmap.id })
  }

  // 使用 useMutation 处理开始学习
  const { execute: executeStartLearning } = useMutation(
    (roadmapId: number) => progressServiceV1.startRoadmap(roadmapId),
    {
      successMessage: t('roadmap.startLearningSuccess'),
      onSuccess: (response, roadmapId) => {
        emit('update-roadmap', roadmapId, { learning: response })
      },
      onError: () => {
        showSnackbar?.(t('roadmap.startLearningFailed'))
      },
    },
  )

  // 开始学习功能
  const handleStartLearning = async (roadmap: Roadmap, event: Event): Promise<void> => {
    event.stopPropagation()
    await executeStartLearning(roadmap.id)
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