<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import RoadmapVueFlow from '@/components/common/RoadmapVueFlow.vue'
import { UserRoadmapState } from '@/types/enums'
import type { ProcessedUserRoadmap } from '@/types/userRoadmap'

interface Props {
  roadmap: ProcessedUserRoadmap
}

interface Emits {
  (e: 'open-detail', roadmap: ProcessedUserRoadmap): void
  (e: 'vote', roadmap: ProcessedUserRoadmap, event: Event): void
  (e: 'move-up', roadmap: ProcessedUserRoadmap, event: Event): void
  (e: 'move-down', roadmap: ProcessedUserRoadmap, event: Event): void
  (e: 'close', roadmap: ProcessedUserRoadmap, event: Event): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { t } = useI18n()

// 工具函数
const getAvatarColor = (name: string): string => {
  if (!name) return 'grey'
  const colors = [
    'red',
    'pink',
    'purple',
    'indigo',
    'blue',
    'cyan',
    'teal',
    'green',
    'amber',
    'orange',
  ]
  const charCode = name.charCodeAt(0)
  return colors[charCode % colors.length]
}

const formatDate = (dateString: string): string => {
  if (!dateString) return t('learning.timeAgo.unknownTime')
  return new Date(dateString).toLocaleDateString()
}

const formatDateTime = (dateString: string): string => {
  if (!dateString) return t('learning.timeAgo.unknownTime')
  return new Date(dateString).toLocaleString()
}

const getStatusColor = (state: number): string => {
  switch (state) {
    case UserRoadmapState.NOT_STARTED:
      return 'grey'
    case UserRoadmapState.IN_PROGRESS:
      return 'primary'
    case UserRoadmapState.COMPLETED:
      return 'success'
    default:
      return 'grey'
  }
}

const getStatusIcon = (state: number): string => {
  switch (state) {
    case UserRoadmapState.NOT_STARTED:
      return 'mdi-circle-outline'
    case UserRoadmapState.IN_PROGRESS:
      return 'mdi-play-circle'
    case UserRoadmapState.COMPLETED:
      return 'mdi-check-circle'
    default:
      return 'mdi-circle-outline'
  }
}

const getStatusText = (state: number): string => {
  switch (state) {
    case UserRoadmapState.NOT_STARTED:
      return t('learning.status.notStarted')
    case UserRoadmapState.IN_PROGRESS:
      return t('learning.status.inProgress')
    case UserRoadmapState.COMPLETED:
      return t('learning.status.completed')
    default:
      return t('learning.status.unknown')
  }
}

// 事件处理
const handleOpenDetail = (): void => {
  emit('open-detail', props.roadmap)
}

const handleVote = (event: Event): void => {
  event.stopPropagation()
  emit('vote', props.roadmap, event)
}

const handleMoveUp = (event: Event): void => {
  event.stopPropagation()
  emit('move-up', props.roadmap, event)
}

const handleMoveDown = (event: Event): void => {
  event.stopPropagation()
  emit('move-down', props.roadmap, event)
}

const handleClose = (event: Event): void => {
  event.stopPropagation()
  emit('close', props.roadmap, event)
}
</script>

<template>
  <div class="mb-4">
    <v-card
      variant="flat"
      class="flat-card roadmap-card position-relative"
      @click="handleOpenDetail"
    >
      <!-- 学习状态标签 -->
      <div class="status-badge-container">
        <div class="d-flex align-center">
          <v-chip
            :color="getStatusColor(roadmap.state)"
            variant="flat"
            size="small"
            class="status-badge"
          >
            <v-icon :icon="getStatusIcon(roadmap.state)" class="mr-1" size="14"></v-icon>
            {{ getStatusText(roadmap.state) }}
          </v-chip>

          <!-- 进行中状态的关闭按钮 -->
          <v-btn
            v-if="roadmap.state === UserRoadmapState.IN_PROGRESS"
            variant="text"
            size="x-small"
            class="ml-2 close-btn"
            color="grey-darken-2"
            @click="handleClose"
          >
            <v-icon size="16">mdi-close</v-icon>
            <v-tooltip activator="parent" location="bottom"> 退出学习 </v-tooltip>
          </v-btn>
        </div>
      </div>

      <div class="d-flex align-stretch roadmap-content-container">
        <!-- 左侧信息区域 -->
        <div class="d-flex flex-column flex-grow-1 pt-2 roadmap-left-section">
          <!-- 标题 -->
          <div class="px-4 pt-2 pb-1">
            <h3 class="text-h5 font-weight-normal mb-3 text-grey-darken-2">
              {{ roadmap.profession?.name }}
            </h3>
          </div>

          <!-- 用户信息和描述并排 -->
          <div class="px-4 pb-2">
            <div class="d-flex align-start">
              <v-avatar
                :color="getAvatarColor(roadmap.creator?.name)"
                size="32"
                class="mr-3 flat-avatar flex-shrink-0"
              >
                <span class="text-white text-caption">{{ roadmap.creator?.name?.charAt(0) || 'U' }}</span>
              </v-avatar>
              <div class="flex-grow-1 min-width-0">
                <div class="text-body-2 text-grey-darken-2 mb-1">
                  {{ roadmap.creator?.name || '未知用户' }} · {{ formatDate(roadmap.createdAt) }}
                </div>
                <div class="text-body-2 text-grey-darken-3 description-text">
                  {{ roadmap.description || '暂无描述' }}
                </div>
              </div>
            </div>
          </div>

          <v-card-text class="text-body-2 flex-grow-1 pt-1 pb-2">
            <!-- 学习进度信息 -->
            <div class="mb-3">
              <div class="d-flex justify-space-between text-body-2 mb-2">
                <span class="text-grey-darken-3">{{ t('learning.completionProgress') }}</span>
                <span class="text-primary font-weight-bold"
                  >{{ parseFloat(roadmap.progress.toFixed(2)) }}%</span
                >
              </div>
              <v-progress-linear
                :model-value="roadmap.progress"
                color="primary"
                background-color="grey-lighten-3"
                height="8"
                rounded="lg"
              >
              </v-progress-linear>
            </div>

            <div class="d-flex flex-wrap align-center mb-3">
              <v-chip size="small" color="success" variant="tonal" class="mr-2 mb-1">
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ roadmap.completedNodes }}/{{ roadmap.totalNodes }} {{ t('learning.nodes') }}
              </v-chip>
              <v-chip size="small" color="info" variant="tonal" class="mr-2 mb-1">
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ roadmap.lastActivity }}
              </v-chip>
              <v-chip
                v-for="tag in roadmap.tags"
                :key="tag"
                size="small"
                color="grey-lighten-1"
                variant="tonal"
                class="mr-2 mb-1"
              >
                {{ tag }}
              </v-chip>
            </div>

            <!-- 时间信息 -->
            <div class="time-info text-caption text-grey-darken-2">
              <div v-if="roadmap.startedAt" class="mb-1">
                <v-icon icon="mdi-calendar-start" size="12" class="mr-1"></v-icon>
                {{ t('learning.startTime') }}: {{ formatDateTime(roadmap.startedAt) }}
              </div>
              <div v-if="roadmap.completedAt">
                <v-icon icon="mdi-calendar-check" size="12" class="mr-1"></v-icon>
                {{ t('learning.completionTime') }}: {{ formatDateTime(roadmap.completedAt) }}
              </div>
            </div>
          </v-card-text>

          <!-- 操作按钮区域 -->
          <div class="px-4 py-2 d-flex justify-space-between border-t">
            <div class="d-flex align-center">
              <v-btn
                variant="text"
                size="small"
                class="flat-action-icon"
                :color="roadmap.upvoted ? 'red-darken-2' : 'primary'"
                @click="handleVote"
              >
                <v-icon size="20" :class="{ 'vote-animation': roadmap.upvoted }">
                  {{ roadmap.upvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline' }}
                </v-icon>
                <span class="ml-1 text-body-2">{{ roadmap.vote || 0 }}</span>
                <v-tooltip activator="parent" location="top">
                  {{ roadmap.upvoted ? t('learning.voted') : t('learning.voteSupport') }}
                </v-tooltip>
              </v-btn>

              <v-btn variant="text" size="small" class="flat-action-icon" color="info">
                <v-icon size="20">mdi-comment-outline</v-icon>
                <span class="ml-1 text-body-2">{{ roadmap.comment || 0 }}</span>
                <v-tooltip activator="parent" location="top">{{
                  t('learning.viewComments')
                }}</v-tooltip>
              </v-btn>
            </div>

            <div class="d-flex align-center">
              <v-btn variant="text" size="small" class="flat-action-icon" color="success">
                <v-icon size="20">mdi-school</v-icon>
                <v-tooltip activator="parent" location="top">{{
                  t('learning.continueLearning')
                }}</v-tooltip>
              </v-btn>

              <!-- 上下移动按钮 -->
              <v-btn
                variant="text"
                size="small"
                class="flat-action-icon ml-1"
                color="grey-darken-2"
                @click="handleMoveUp"
              >
                <v-icon size="18">mdi-arrow-up</v-icon>
                <v-tooltip activator="parent" location="top">{{ t('learning.up') }}</v-tooltip>
              </v-btn>

              <v-btn
                variant="text"
                size="small"
                class="flat-action-icon ml-1"
                color="grey-darken-2"
                @click="handleMoveDown"
              >
                <v-icon size="18">mdi-arrow-down</v-icon>
                <v-tooltip activator="parent" location="top">{{ t('learning.down') }}</v-tooltip>
              </v-btn>
            </div>
          </div>
        </div>

        <!-- 右侧VueFlow图表区域 -->
        <div class="d-flex align-center roadmap-chart-section">
          <div class="vue-flow-preview vue-flow-chart-container">
            <RoadmapVueFlow
              :nodes="roadmap.nodes"
              :edges="roadmap.edges"
              :readonly="true"
              :show-background="true"
              background-pattern="#aaa"
            />
          </div>
        </div>
      </div>
    </v-card>
  </div>
</template>

<style scoped>
.roadmap-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  overflow: hidden;
  border: 1px solid #e0e0e0 !important;
  border-radius: 12px !important;
}

.roadmap-card:hover {
  transform: translateY(-4px);
  border-color: rgba(25, 118, 210, 0.4) !important;
}

.status-badge-container {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
}

.status-badge {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.close-btn {
  min-width: 24px !important;
  width: 24px;
  height: 24px;
}

.flat-avatar {
  border: 2px solid rgba(255, 255, 255, 0.8);
}

.description-text {
  line-height: 1.4;
  max-height: 2.8em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.time-info {
  background: rgba(0, 0, 0, 0.02);
  padding: 8px;
  border-radius: 8px;
  border-left: 3px solid #1976d2;
}

.flat-action-icon {
  min-width: auto !important;
  border-radius: 12px !important;
  transition: all 0.2s ease !important;
}

.flat-action-icon:hover {
  background-color: rgba(0, 0, 0, 0.04) !important;
  transform: scale(1.05);
}

.vote-animation {
  animation: vote-pulse 0.3s ease;
}

@keyframes vote-pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.border-t {
  border-top: 1px solid rgba(0, 0, 0, 0.06) !important;
}

.roadmap-content-container {
  min-height: 240px;
}

.roadmap-left-section {
  min-width: 0;
  flex: 1;
}

.roadmap-chart-section {
  width: 400px;
  min-width: 400px;
}

.vue-flow-preview {
  border-radius: 12px;
  overflow: hidden;
  background: #fafafa;
  border: 1px solid #e0e0e0;
}

.vue-flow-chart-container {
  width: 100%;
  height: 100%;
}
</style>