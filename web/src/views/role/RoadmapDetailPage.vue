<template>
  <DefaultLayout>
    <div class="roadmap-detail-page">
      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <div v-else-if="roadmap">
        <div class="content-layout pt-4 pt-md-5">
          <!-- 左侧：Header + 流程图 -->
          <div class="main-content">
            <!-- 职业信息 -->
            <div class="back-button-wrapper mb-2 mb-md-2 sticky-header">
              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <!-- 职业信息 -->
                  <div v-if="roadmap?.role" class="d-flex align-center">
                    <v-avatar
                      color="primary"
                      :size="$vuetify.display.mobile ? 36 : 40"
                      class="mr-3 flex-shrink-0"
                    >
                      <v-icon
                        icon="mdi-briefcase-outline"
                        color="white"
                        :size="$vuetify.display.mobile ? 18 : 20"
                      />
                    </v-avatar>
                    <span class="text-subtitle-1 text-md-h6 font-weight-bold text-grey-darken-4">
                      {{ roadmap.role.name }}
                    </span>
                  </div>
                </div>

                <!-- 点赞和收藏按钮 -->
                <div class="d-flex align-center gap-2">
                  <v-btn
                    color="grey-darken-2"
                    variant="outlined"
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    rounded="lg"
                    @click="handleCopy"
                  >
                    <v-icon icon="mdi-content-copy" size="18" class="mr-1" />
                    <span class="d-none d-sm-inline">{{ t('roadmapDetail.copyPath') }}</span>
                    <span class="d-sm-none">{{ t('common.copy') }}</span>
                  </v-btn>
                  <v-divider vertical class="mx-1 align-self-center" style="height: 28px" />
                  <v-btn
                    :color="roadmap.learning ? 'success' : 'primary'"
                    :variant="roadmap.learning ? 'outlined' : 'flat'"
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    rounded="lg"
                    @click="handleStartLearning"
                  >
                    <v-icon
                      :icon="roadmap.learning ? 'mdi-check' : 'mdi-play'"
                      size="18"
                      class="mr-1"
                    />
                    {{
                      roadmap.learning ? t('roadmapCard.learning') : t('roadmapCard.startLearning')
                    }}
                  </v-btn>

                  <!-- 分隔线 -->
                  <v-divider vertical class="mx-1 align-self-center" style="height: 28px" />

                  <v-btn
                    :color="roadmap.liked ? 'primary' : 'grey-darken-1'"
                    variant="tonal"
                    :size="$vuetify.display.mobile ? 'small' : 'default'"
                    rounded="lg"
                    @click="handleVote"
                  >
                    <v-icon
                      :icon="roadmap.liked ? 'mdi-heart' : 'mdi-heart-outline'"
                      size="18"
                      class="mr-1"
                    />
                    {{ roadmap.likeCount }}
                  </v-btn>
                  <v-tooltip location="bottom">
                    <template #activator="{ props }">
                      <v-btn
                        v-bind="props"
                        :icon="roadmap.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                        :color="roadmap.bookmarked ? 'primary' : 'grey-darken-1'"
                        variant="tonal"
                        density="comfortable"
                        :size="$vuetify.display.mobile ? 'small' : 'default'"
                        rounded="lg"
                        @click="handleToggleBookmark"
                      />
                    </template>
                    {{
                      roadmap.bookmarked
                        ? t('roadmapDetail.unbookmark')
                        : t('roadmapDetail.bookmark')
                    }}
                  </v-tooltip>
                </div>
              </div>
            </div>

            <!-- 流程图 -->
            <v-card rounded="xl" class="flow-card no-border mt-4" flat>
              <v-card-text class="pa-0">
                <div class="vue-flow-container">
                  <RoadmapViewer
                    :content="roadmap.content"
                    :role-name="roadmap?.role?.name || ''"
                    fit-parent
                  />
                </div>
              </v-card-text>
            </v-card>
          </div>

          <!-- 右侧：路径信息和评论区 -->
          <div class="right-sidebar pt-1">
            <!-- 路径信息卡片 -->
            <v-card rounded="0" class="roadmap-info-card mb-0 no-border">
              <v-card-text class="px-0 pt-0 pb-2 pb-sm-2">
                <!-- 标题和状态 -->
                <div class="d-flex align-center justify-space-between mb-4">
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-2">
                      <v-chip
                        v-if="roadmap.pinned"
                        color="warning"
                        size="small"
                        variant="flat"
                        class="mr-2"
                      >
                        <v-icon icon="mdi-pin" size="14" class="mr-1" />
                        {{ t('roadmapCard.pin') }}
                      </v-chip>
                    </div>

                    <!-- 创建者信息 -->
                    <div class="d-flex align-center mb-3">
                      <UserAvatar
                        :name="roadmap.creator?.name || t('common.anonymous')"
                        :avatar-url="roadmap.creator?.avatar"
                        size="24"
                        rounded="lg"
                        class="mr-2"
                      />
                      <span class="text-body-2 text-grey-darken-3">
                        {{ roadmap.creator?.name }}
                      </span>
                      <span class="text-caption text-grey mx-2">·</span>
                      <span class="text-caption text-grey">
                        {{ getTimeDisplay(roadmap.createdAt) }}
                      </span>
                    </div>

                    <!-- 描述 -->
                    <div class="text-body-1 text-grey-darken-4 mb-3">
                      {{ roadmap.description }}
                    </div>

                    <!-- 统计信息 -->
                    <div class="d-flex flex-wrap align-center gap-4 gap-md-5">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-account-group" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.learnerCount ?? 0 }}
                          <span class="d-none d-sm-inline">{{ t('roadmapCard.learners') }}</span>
                        </span>
                      </div>
                      <div class="d-flex align-center d-none d-sm-flex">
                        <v-icon icon="mdi-comment-outline" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.commentCount ?? 0 }} {{ t('roleDetail.comments') }}
                        </span>
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-graph-outline" size="18" color="grey" class="mr-2" />
                        <span class="text-body-2 text-grey-darken-2">
                          {{ roadmap.nodeCount }}
                          <span class="d-none d-sm-inline">{{ t('roleDetail.nodeCountUnit') }}</span
                          >{{ t('roadmap.nodes') }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>

            <!-- 评论区 -->
            <CommentSection
              :post-id="roadmapId"
              :object-type="ObjectType.ROADMAP"
              :comment-count="roadmap.commentCount"
              :target-comment-id="targetCommentId"
              :target-sub-comment-id="targetSubCommentId"
            />
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useRoadmapDetailQuery } from '@/queries/roadmap'
import { useUpvoteMutation, useBookmarkToggleMutation } from '@/queries/interaction'
import { useStartRoadmapMutation, useCancelRoadmapMutation } from '@/queries/progress'
import { ObjectType, VoteType } from '@/enums'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import CommentSection from '@/components/common/CommentSection.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useI18n } from '@/composables/useI18n'
import RoadmapViewer from '@/components/features/role/RoadmapViewer.vue'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()

// 从路由获取参数
const roleId = computed(() => roadmap.value?.roleId || 0)
const roadmapId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})

// 目标评论ID（从 URL 获取）
const targetCommentId = computed(() => {
  if (route.query.commentId) {
    return Number(route.query.commentId)
  }
  return null
})

// 目标子评论ID（从 URL 获取）
const targetSubCommentId = computed(() => {
  if (route.query.subCommentId) {
    return Number(route.query.subCommentId)
  }
  return null
})

// 加载路径详情
const {
  data: roadmapData,
  isLoading: loading,
  error: fetchError,
} = useRoadmapDetailQuery(roadmapId)

// 加载失败时跳转到 404
watch(
  fetchError,
  (err) => {
    if (err) {
      router.replace({ path: '/error/404', state: { message: t('roadmapDetail.notFound') } })
    }
  },
  { immediate: true }
)

const roadmap = computed(() => roadmapData.value)

// 投票
const { mutate: upvoteMutate } = useUpvoteMutation()

const handleVote = (): void => {
  if (!roadmap.value) return
  upvoteMutate(
    { objectId: roadmapId.value, objectType: ObjectType.ROADMAP, type: VoteType.LIKE },
    {
      onSuccess: (result) => {
        if (result && roadmap.value) {
          roadmap.value.liked = result.liked || false
          roadmap.value.likeCount = result.likeCount || 0
        }
      },
    }
  )
}

// 开始/取消学习
const { mutate: startRoadmap } = useStartRoadmapMutation()
const { mutate: cancelRoadmap } = useCancelRoadmapMutation()

const handleStartLearning = (): void => {
  if (!roadmap.value) return

  if (roadmap.value.learning) {
    cancelRoadmap(roadmapId.value, {
      onSuccess: (result) => {
        if (result && roadmap.value) roadmap.value.learning = result.learning
      },
    })
  } else {
    startRoadmap(roadmapId.value, {
      onSuccess: (result) => {
        if (result && roadmap.value) roadmap.value.learning = result.learning
      },
    })
  }
}

// 复制路径
const handleCopy = (): void => {
  void router.push(`/role/${roleId.value}/roadmap/create?copy=${roadmapId.value}`)
}

// 切换收藏状态
const { mutate: bookmarkMutate } = useBookmarkToggleMutation()

const handleToggleBookmark = () => {
  if (!roadmap.value) return
  bookmarkMutate(
    { contentType: 'roadmap', contentId: roadmapId.value },
    {
      onSuccess: (result) => {
        if (result !== null && roadmap.value) {
          roadmap.value.bookmarked = result
        }
      },
    }
  )
}

// 获取时间显示
const getTimeDisplay = (date: string | undefined): string => {
  if (!date) return ''
  const now = new Date()
  const created = new Date(date)
  const days = Math.floor((now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24))

  if (days === 0) return t('roleDetail.today')
  if (days === 1) return t('roleDetail.yesterday')
  if (days < 7) return t('roleDetail.daysAgo', { days })
  if (days < 30) return t('roleDetail.weeksAgo', { weeks: Math.floor(days / 7) })
  return date
}
</script>

<style scoped>
.roadmap-detail-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 48px;
    align-items: flex-start;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 左侧 header 和流程图 - 整体 sticky */
@media (min-width: 1280px) {
  .main-content {
    position: sticky;
    top: 76px;
    align-self: flex-start;
    display: flex;
    flex-direction: column;
  }

  .sticky-header {
    flex-shrink: 0;
    background: rgb(var(--v-theme-background));
    padding-bottom: 16px;
  }
}

/* 右侧评论区 */
.right-sidebar {
  width: 100%;
}

@media (min-width: 1280px) {
  .right-sidebar {
    width: 360px;
    flex-shrink: 0;
  }
}

.roadmap-info-card {
  background-color: rgb(var(--v-theme-surface));
}

.flow-card {
  background-color: rgb(var(--v-theme-surface));
}

.vue-flow-container {
  background: rgb(var(--v-theme-surface));
  position: relative;
  height: 600px;
}

@media (min-width: 1280px) {
  .vue-flow-container {
    height: calc(100vh - 200px);
  }
}

.gap-3 {
  gap: 12px;
}

.gap-4 {
  gap: 16px;
}

.gap-2 {
  gap: 8px;
}
</style>
