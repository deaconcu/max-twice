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
    <LoadingSpinner v-if="loading && courses.length === 0" />

    <!-- 课程列表 -->
    <div v-else-if="courses.length > 0">
      <div class="course-grid">
        <v-card
          v-for="course in courses"
          :key="course.id"
          rounded="lg"
          border
          hover
          class="course-card"
          @click="goToCourseDetail(course.id)"
        >
          <v-card-text class="pa-4 position-relative">
            <!-- 图标和标题区域 -->
            <div class="d-flex align-center ga-3 mb-3">
              <div class="icon-container flex-shrink-0">
                <v-icon icon="mdi-book-open-variant" :size="24" color="primary" />
              </div>
              <div class="flex-grow-1" style="min-width: 0">
                <div class="d-flex align-center ga-2">
                  <span
                    class="text-body-1 font-weight-bold text-truncate"
                    :style="{ color: 'rgb(var(--v-theme-on-surface))' }"
                  >
                    {{ course.name }}
                  </span>
                  <v-chip
                    v-if="isOwnProfile"
                    :color="getStatusColor(course.uiStatus)"
                    size="x-small"
                    variant="tonal"
                    class="flex-shrink-0"
                  >
                    {{ getStatusText(course.uiStatus) }}
                  </v-chip>
                </div>
              </div>
            </div>

            <!-- 描述 -->
            <p class="text-body-2 text-grey-darken-2 mb-3 course-description">
              {{ course.description || t('hotRanking.noDescription') }}
            </p>

            <!-- 底部：操作按钮 -->
            <div v-if="isOwnProfile" class="d-flex align-center justify-end ga-2">
              <v-btn
                v-if="course.uiStatus === 'pending'"
                color="grey"
                variant="text"
                size="small"
                @click.stop="onWithdraw(course.id)"
              >
                {{ t('common.withdraw') }}
              </v-btn>
              <v-btn
                v-if="course.uiStatus === 'rejected' || course.uiStatus === 'unpublished'"
                color="primary"
                variant="text"
                size="small"
                @click.stop="onResubmit(course.id)"
              >
                {{ t('common.edit') }}
              </v-btn>
              <v-btn
                color="primary"
                variant="text"
                size="small"
                @click.stop="goToCourseDetail(course.id)"
              >
                {{ t('home.viewAll') }}
              </v-btn>
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
          @click="loadMore({ done: () => {} })"
        >
          {{ t('common.loadMore') }}
        </v-btn>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-book-open-variant"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">
        {{
          statusFilter !== 'all' ? t('user.profile.noArticlesFound') : t('user.profile.noCourses')
        }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMyCoursesQuery, useWithdrawCourseMutation } from '@/queries/course'
import { useI18n } from '@/composables/useI18n'
import { getGlobalSnackbar } from '@/composables/config'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import type { Course } from '@/types/course'

const props = withDefaults(defineProps<Props>(), {
  isOwnProfile: false,
})

interface Props {
  isOwnProfile?: boolean
}

const { t } = useI18n()
const router = useRouter()

// 状态筛选：course 主体只有 NEVER_PUBLISHED / PUBLISHED；BANNED 由后端拦截
const statusFilter = ref<'all' | 'unpublished' | 'published'>('all')

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

// 仅自己可看；他人 profile 不展示
const enabled = computed(() => props.isOwnProfile)

const { data, isLoading, hasNextPage, fetchNextPage } = useMyCoursesQuery(stateValue, enabled)

const rawCourses = computed<Course[]>(() => data.value?.pages.flatMap((p) => p.items) ?? [])
const loading = computed(() => isLoading.value)
const hasMore = computed(() => Boolean(hasNextPage.value))

/**
 * 把后端 state + pendingRevisionId 折算到 UI 4 态：pending / rejected / unpublished / published
 * - pendingRevisionId != null  → pending（审核中）
 * - state == NEVER_PUBLISHED 且 pendingRevisionId == null → unpublished（草稿/已撤回，可重提）
 * - state == PUBLISHED → published
 */
type UiStatus = 'pending' | 'rejected' | 'unpublished' | 'published'
const courses = computed(() =>
  rawCourses.value.map((c) => {
    let uiStatus: UiStatus
    if (c.pendingRevisionId != null) uiStatus = 'pending'
    else if (c.state === 'PUBLISHED') uiStatus = 'published'
    else uiStatus = 'unpublished'
    return { ...c, uiStatus }
  })
)

const getStatusColor = (status: UiStatus) => {
  const colors: Record<UiStatus, string> = {
    pending: 'warning',
    rejected: 'error',
    unpublished: 'grey',
    published: 'success',
  }
  return colors[status]
}

const getStatusText = (status: UiStatus) => {
  const texts: Record<UiStatus, string> = {
    pending: t('common.pending'),
    rejected: t('user.profile.rejected'),
    unpublished: t('user.profile.draft'),
    published: t('user.profile.published'),
  }
  return texts[status]
}

const goToCourseDetail = (courseId: number) => {
  void router.push(`/courses/${String(courseId)}`)
}

// 重新提交：跳到 CourseListPage 并通过 query 触发对话框回填
const onResubmit = (courseId: number) => {
  void router.push({ path: '/courses', query: { resubmitId: String(courseId) } })
}

// 撤回
const { mutate: withdrawMutate } = useWithdrawCourseMutation()
const onWithdraw = (courseId: number) => {
  withdrawMutate(courseId, {
    onSuccess: () => {
      getGlobalSnackbar()?.(t('common.withdrawSuccess'), 'success')
    },
  })
}

const loadMore = async ({ done }: { done: () => void } = { done: () => undefined }) => {
  await fetchNextPage()
  done()
}
</script>

<style scoped>
.course-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@container (max-width: 900px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}

.pa-0 {
  container-type: inline-size;
}
</style>
