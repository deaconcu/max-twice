<template>
  <v-col cols="12">
    <v-card
      variant="flat"
      class="flat-card roadmap-card position-relative"
      @click="$emit('open-detail', roadmap)"
    >
      <!-- 学习按钮 - 置于卡片顶部醒目位置 -->
      <div class="learn-button-container">
        <v-btn
          color="success"
          variant="flat"
          class="learn-button"
          :class="roadmap.learning ? 'learning' : ''"
          @click.stop="$emit('start-learning', roadmap, $event)"
        >
          <v-icon icon="mdi-play-circle" class="mr-1" size="16"></v-icon>
          {{ roadmap.learning ? t('roadmapCard.learning') : t('roadmapCard.startLearning') }}
        </v-btn>
      </div>

      <div class="d-flex align-stretch roadmap-content-container">
        <!-- 左侧信息区域 -->
        <div class="d-flex flex-column flex-grow-1 pt-2 roadmap-left-section">
          <v-card-item class="pb-1">
            <div class="d-flex align-center mb-2">
              <v-avatar :color="getAvatarColor(roadmap.creator?.name)" class="mr-3 flat-avatar">
                <span class="text-white">{{ roadmap.creator?.name?.charAt(0) || 'U' }}</span>
              </v-avatar>
              <div>
                <v-card-title class="pa-0 text-subtitle-1">{{
                  roadmap.creator?.name || t('roadmapCard.unknownUser')
                }}</v-card-title>
                <v-card-subtitle class="pa-0 text-caption text-primary">{{
                  formatDate(roadmap.updatedAt)
                }}</v-card-subtitle>
              </div>
            </div>
          </v-card-item>

          <v-card-text v-if="roadmap.description" class="text-body-2 flex-grow-1 pt-1">
            <h3 class="text-subtitle-1 font-weight-normal mb-2">{{ roadmap.description }}</h3>
            <div class="d-flex flex-wrap align-center mb-3">
              <v-chip size="small" color="grey-lighten-3" variant="flat" class="mr-2 mb-1">
                <v-icon
                  icon="mdi-account-group"
                  size="14"
                  class="mr-1"
                  color="grey-darken-2"
                ></v-icon>
                <span class="text-grey-darken-3"
                  >{{ roadmap.learners || Math.floor(Math.random() * 1000) + 100 }}
                  {{ t('roadmapCard.learners') }}</span
                >
              </v-chip>
              <v-chip size="small" color="grey-lighten-3" variant="flat" class="mr-2 mb-1">
                <v-icon icon="mdi-star" size="14" class="mr-1" color="grey-darken-2"></v-icon>
                <span class="text-grey-darken-3"
                  >{{ (Math.random() * 2 + 3).toFixed(1) }} {{ t('roadmapCard.rating') }}</span
                >
              </v-chip>
              <v-chip size="small" color="grey-lighten-3" variant="flat" class="mr-2 mb-1">
                <v-icon icon="mdi-chart-line" size="14" class="mr-1" color="grey-darken-2"></v-icon>
                <span class="text-grey-darken-3"
                  >{{ roadmap.nodes?.length || 0 }} {{ t('roadmapCard.nodes') }}</span
                >
              </v-chip>
            </div>
          </v-card-text>

          <!-- 操作按钮区域 -->
          <div class="px-4 py-2 d-flex justify-space-between border-t">
            <v-btn
              variant="text"
              size="small"
              class="flat-action-icon"
              :color="roadmap.upvoted ? 'red-darken-2' : 'primary'"
              @click="$emit('vote', roadmap, $event)"
            >
              <v-icon size="20" :class="{ 'vote-animation': roadmap.upvoted }">
                {{ roadmap.upvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline' }}
              </v-icon>
              <span class="ml-1 text-body-2">{{ roadmap.vote || 0 }}</span>
              <v-tooltip activator="parent" location="top">
                {{ roadmap.upvoted ? t('roadmapCard.upvoted') : t('roadmapCard.vote') }}
              </v-tooltip>
            </v-btn>

            <v-btn
              variant="text"
              size="small"
              class="flat-action-icon"
              color="grey-darken-2"
              @click="$emit('open-detail', roadmap)"
            >
              <v-icon size="20">mdi-comment-outline</v-icon>
              <span class="ml-1 text-body-2">{{ roadmap.comment || 0 }}</span>
              <v-tooltip activator="parent" location="top">{{
                t('roadmapCard.viewComments')
              }}</v-tooltip>
            </v-btn>

            <v-spacer></v-spacer>

            <v-btn
              variant="text"
              size="small"
              class="flat-action-icon"
              color="grey-darken-2"
              @click="$emit('copy', roadmap, $event)"
            >
              <v-icon size="20">mdi-content-copy</v-icon>
              <v-tooltip activator="parent" location="top">{{
                t('roadmapCard.copyToEditor')
              }}</v-tooltip>
            </v-btn>

            <v-btn
              variant="text"
              size="small"
              class="flat-action-icon"
              :color="roadmap.pinned ? 'primary' : 'grey-darken-2'"
              @click="$emit('toggle-pin', roadmap, $event)"
            >
              <v-icon size="20">{{
                roadmap.pined ? 'mdi-arrow-up-thick' : 'mdi-arrow-up-thick'
              }}</v-icon>
              <v-tooltip activator="parent" location="top">
                {{ roadmap.pinned ? t('roadmapCard.unpin') : t('roadmapCard.pin') }}
              </v-tooltip>
            </v-btn>
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
              @node-click="handleNodeClick"
            />
          </div>
        </div>
      </div>
    </v-card>
  </v-col>
</template>

<script setup lang="ts">
  import { useI18n } from 'vue-i18n'
  import RoadmapVueFlow from '@/components/common/RoadmapVueFlow.vue'
  import type { Node, Edge } from '@vue-flow/core'
  import type { Roadmap } from '@/types/roadmap'

  const { t } = useI18n()

  interface Props {
    roadmap: Roadmap
  }

  defineProps<Props>()

  interface Emits {
    (e: 'open-detail', roadmap: Roadmap): void
    (e: 'vote', roadmap: Roadmap, event: Event): void
    (e: 'copy', roadmap: Roadmap, event: Event): void
    (e: 'toggle-pin', roadmap: Roadmap, event: Event): void
    (e: 'start-learning', roadmap: Roadmap, event: Event): void
  }

  defineEmits<Emits>()

  // 日期格式化
  const formatDate = (dateString?: string): string => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const handleNodeClick = ({ node }: { node: Node }): void => {
    // 根节点不跳转
    if (node.id === '0') {
      return
    }

    if (node.data?.link) {
      window.open(node.data.link as string, '_blank')
    }
  }

  // 根据用户名获取头像颜色
  const getAvatarColor = (name?: string): string => {
    const colors = ['grey-darken-2', 'primary', 'grey-darken-1', 'grey-darken-3']
    if (!name) return 'grey-darken-2'

    // 使用名字的第一个字符来确定颜色
    const charCode = name.charCodeAt(0)
    return colors[charCode % colors.length]
  }
</script>

<style scoped>
  /* 学习按钮样式 */
  .learn-button-container {
    position: absolute;
    top: 0px;
    right: 0px;
    z-index: 10;
  }

  .learn-button {
    border-radius: 0px !important;
    border-top-right-radius: 11px !important;
    border-bottom-left-radius: 11px !important;
    border: 0px !important;
    padding: 0px 8px !important;
    text-transform: none !important;
    font-weight: 700 !important;
    font-size: 14px !important;
    height: 32px !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
    min-width: 80px !important;
    letter-spacing: 0.3px !important;
  }

  .learn-button:hover {
    font-size: 16px !important;
    height: 36px !important;
  }

  .learning {
    background-color: #f33b3b !important;
    color: white !important;
    opacity: 0.9 !important;
  }

  .learn-button :deep(.v-btn__content) {
    font-weight: 700 !important;
  }

  /* 扁平化设计样式 */
  .flat-action-icon {
    border-radius: 6px !important;
    transition: all 0.2s ease !important;
    min-width: auto !important;
    padding: 6px 8px !important;
    height: 32px !important;
  }

  .flat-action-icon :deep(.v-btn__content) {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    gap: 4px !important;
    height: 100% !important;
  }

  .flat-action-icon :deep(.v-icon) {
    margin: 0 !important;
    vertical-align: middle !important;
  }

  .flat-action-icon:hover {
    background: rgba(178, 223, 219, 0.15) !important;
    transform: scale(1.05) !important;
  }

  /* 点赞动画效果 */
  .vote-animation {
    animation: vote-bounce 0.6s ease-in-out;
  }

  @keyframes vote-bounce {
    0% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.3);
    }
    100% {
      transform: scale(1);
    }
  }

  .flat-card {
    border-radius: 12px !important;
    border: 1px solid #b2dfdb !important;
    background: white !important;
    transition: all 0.3s ease !important;
    box-shadow: none !important;
  }

  .flat-card:hover {
    border-color: #4db6ac !important;
    transform: translateY(-4px) !important;
    box-shadow: none !important;
  }

  .flat-chip {
    border-radius: 6px !important;
    border: 1px solid rgba(25, 118, 210, 0.3) !important;
  }

  .roadmap-card {
    cursor: pointer;
  }

  /* Vue Flow 预览样式 */
  .vue-flow-preview {
    border-radius: 12px;
    overflow: hidden;
    background: #fafafa;
    border: 1px solid #e0e0e0;
  }

  /* 卡片操作区域样式 */
  .v-card-actions {
    background: linear-gradient(
      135deg,
      rgba(248, 250, 252, 0.9) 0%,
      rgba(240, 245, 255, 0.9) 100%
    ) !important;
    border-top: 1px solid #e3f2fd !important;
    min-height: 48px !important;
    border-radius: 0 0 12px 12px !important;
  }

  /* 响应式设计 */
  @media (max-width: 1024px) {
    .roadmap-card .d-flex {
      flex-direction: column !important;
    }

    .roadmap-card .vue-flow-preview {
      width: 100% !important;
      min-width: unset !important;
      height: 200px !important;
      margin-top: 12px;
    }

    .roadmap-card .d-flex > div:first-child {
      min-width: unset !important;
    }
  }

  @media (max-width: 768px) {
    .roadmap-card .vue-flow-preview {
      height: 180px !important;
    }
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

  .vue-flow-chart-container {
    width: 100%;
    height: 100%;
  }
</style>