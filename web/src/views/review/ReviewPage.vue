<template>
  <DefaultLayout>
    <!-- 页面标题 -->
    <div class="mb-6">
      <div class="d-flex align-center justify-space-between mb-4">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="grey-darken-1">mdi-brain</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">{{ t('review.title') }}</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">
              {{
                activeTab === 'all'
                  ? t('review.subtitle')
                  : `${selectedCourse?.course.name || ''}${t('review.courseSubtitle')}`
              }}
            </p>
          </div>
        </div>

        <!-- 视图模式选择器 -->
        <div class="d-flex align-center" style="gap: 8px">
          <v-btn
            :color="viewMode === 'review' ? 'primary' : 'grey'"
            :variant="viewMode === 'review' ? 'flat' : 'outlined'"
            rounded="lg"
            size="small"
            @click="switchViewMode('review')"
          >
            <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
            {{ t('review.modeReview') }}
          </v-btn>

          <v-btn
            :color="viewMode === 'list' ? 'primary' : 'grey'"
            :variant="viewMode === 'list' ? 'flat' : 'outlined'"
            rounded="lg"
            size="small"
            @click="switchViewMode('list')"
          >
            <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-2"></v-icon>
            {{ t('review.modeList') }}
          </v-btn>

          <v-btn
            v-if="selectedCourse"
            :color="viewMode === 'manage' ? 'primary' : 'grey'"
            :variant="viewMode === 'manage' ? 'flat' : 'outlined'"
            rounded="lg"
            size="small"
            @click="switchViewMode('manage')"
          >
            <v-icon icon="mdi-cog" size="16" class="mr-2"></v-icon>
            {{ t('review.modeManage') }}
          </v-btn>
        </div>
      </div>
    </div>

    <v-row>
      <!-- 主内容区域 -->
      <v-col cols="12" lg="9">
        <!-- 复习模式 -->
        <div v-if="viewMode === 'review'">
          <!-- 加载状态 -->
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
            <p class="text-body-1 text-grey-darken-2 mt-4">{{ t('common.loading') }}</p>
          </div>

          <!-- 空队列状态 -->
          <div v-else-if="!isReviewing && reviewCards.length === 0" class="text-center">
            <v-card border rounded="lg" class="pa-8">
              <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.excellent') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{
                  selectedCourse
                    ? `${selectedCourse.course.name}${t('review.courseNoDue')}`
                    : t('review.allNoDue')
                }}
              </p>
            </v-card>
          </div>

          <!-- 开始复习状态 -->
          <div v-else-if="!isReviewing" class="text-center">
            <v-card border rounded="lg" style="padding: 200px 32px">
              <v-icon icon="mdi-cards" size="64" color="primary" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.readyToReview') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ selectedCourse ? selectedCourse.course.name : t('review.allCourses')
                }}{{ t('review.has') }}
                <span class="font-weight-bold text-primary">{{
                  selectedCourse ? selectedCourse.dueCardCount : totalDueCards
                }}</span>
                {{ t('review.cardsWaiting') }}
              </p>
              <v-btn color="primary" variant="flat" rounded="lg" size="large" @click="startReview">
                <v-icon icon="mdi-play" class="mr-2"></v-icon>
                {{ t('review.startReview') }}
              </v-btn>
            </v-card>
          </div>

          <!-- 复习中状态 -->
          <div v-else-if="currentCard">
            <!-- 进度条 -->
            <v-card rounded="lg" class="mb-0 py-1 pb-2 no-border">
              <div class="d-flex align-center justify-space-between">
                <span class="text-caption text-grey-darken-1">
                  {{
                    t('review.cardProgress', {
                      current: currentCardIndex + 1,
                      total: reviewCards.length,
                    })
                  }}
                </span>
                <span class="text-caption text-grey-darken-1">{{ reviewProgress }}%</span>
              </div>
            </v-card>

            <!-- 卡片区域 -->
            <v-card rounded="lg" class="mb-4">
              <div
                class="card-container pa-8 d-flex align-center justify-center"
                style="min-height: 500px"
              >
                <!-- 问题面 -->
                <div v-if="!showAnswer" class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-help-circle" color="primary" size="48"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-primary mb-4">
                    {{ t('review.question') }}
                  </h3>

                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="primary" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.title }}
                    </v-chip>
                  </div>

                  <div class="question-content pa-6 mx-auto" style="max-width: 600px">
                    <p class="text-h6 text-grey-darken-3">{{ currentCard.front }}</p>
                  </div>
                  <div class="mt-8">
                    <v-btn
                      color="primary"
                      variant="flat"
                      rounded="lg"
                      size="large"
                      @click="revealAnswer"
                    >
                      <v-icon icon="mdi-eye" class="mr-2"></v-icon>
                      {{ t('review.showAnswer') }}
                    </v-btn>
                  </div>
                </div>

                <!-- 答案面 -->
                <div v-else class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-lightbulb" color="success" size="48"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-success mb-4">
                    {{ t('review.answer') }}
                  </h3>

                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="success" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.title }}
                    </v-chip>
                  </div>

                  <div class="answer-content pa-6 mx-auto" style="max-width: 600px">
                    <p class="text-h6 text-grey-darken-3">{{ currentCard.back }}</p>
                  </div>

                  <!-- 评价按钮 -->
                  <div class="mt-8">
                    <p class="text-body-1 text-grey-darken-1 mb-4">
                      {{ t('review.ratePrompt') }}
                    </p>
                    <div class="d-flex justify-center flex-wrap" style="gap: 12px">
                      <v-btn
                        color="error"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.AGAIN)"
                      >
                        <v-icon icon="mdi-close" class="mr-2"></v-icon>
                        {{ t('review.forgot') }}
                      </v-btn>
                      <v-btn
                        color="warning"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.HARD)"
                      >
                        <v-icon icon="mdi-help" class="mr-2"></v-icon>
                        {{ t('review.hard') }}
                      </v-btn>
                      <v-btn
                        color="success"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.GOOD)"
                      >
                        <v-icon icon="mdi-check" class="mr-2"></v-icon>
                        {{ t('review.good') }}
                      </v-btn>
                      <v-btn
                        color="primary"
                        variant="flat"
                        rounded="lg"
                        @click="submitReview(ReviewResult.EASY)"
                      >
                        <v-icon icon="mdi-thumb-up" class="mr-2"></v-icon>
                        {{ t('review.easy') }}
                      </v-btn>
                    </div>
                  </div>
                </div>
              </div>
            </v-card>

            <!-- 操作按钮 -->
            <div class="d-flex justify-space-between">
              <v-btn variant="tonal" rounded="lg" @click="resetReview">
                <v-icon icon="mdi-stop" class="mr-2"></v-icon>
                {{ t('review.stopReview') }}
              </v-btn>

              <v-btn variant="text" rounded="lg" :disabled="!showAnswer" @click="skipCard">
                {{ t('review.skip') }}
                <v-icon icon="mdi-skip-next" class="ml-2"></v-icon>
              </v-btn>
            </div>
          </div>
        </div>

        <!-- 列表模式 -->
        <div v-else-if="viewMode === 'list'">
          <div>
            <div class="d-flex align-center justify-space-between mb-4">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4">
                {{ t('review.cardList') }} ({{ listCards.length }} {{ t('review.cards') }})
              </h3>

              <!-- 批量操作按钮 -->
              <div class="d-flex align-center flex-wrap" style="gap: 8px">
                <v-btn
                  v-if="selectedCards.length > 0"
                  color="primary"
                  variant="tonal"
                  rounded="lg"
                  size="small"
                  @click="reviewSelectedCards"
                >
                  <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
                  {{ t('review.reviewSelected') }} ({{ selectedCards.length }})
                </v-btn>

                <v-btn
                  v-if="selectedCards.length > 0"
                  color="warning"
                  variant="tonal"
                  rounded="lg"
                  size="small"
                  @click="resetSelectedCards"
                >
                  <v-icon icon="mdi-restart" size="16" class="mr-2"></v-icon>
                  {{ t('review.resetLearning') }}
                </v-btn>

                <v-btn
                  v-if="selectedCards.length > 0"
                  color="error"
                  variant="tonal"
                  rounded="lg"
                  size="small"
                  @click="deleteSelectedCards"
                >
                  <v-icon icon="mdi-delete" size="16" class="mr-2"></v-icon>
                  {{ t('common.delete') }}
                </v-btn>

                <v-btn
                  :color="
                    selectedCards.length === listCards.length && listCards.length > 0
                      ? 'primary'
                      : 'grey'
                  "
                  variant="tonal"
                  rounded="lg"
                  size="small"
                  :disabled="listCards.length === 0"
                  @click="toggleSelectAll"
                >
                  <v-icon
                    :icon="
                      selectedCards.length === listCards.length && listCards.length > 0
                        ? 'mdi-checkbox-marked'
                        : 'mdi-checkbox-blank-outline'
                    "
                    size="16"
                    class="mr-2"
                  ></v-icon>
                  {{
                    selectedCards.length === listCards.length && listCards.length > 0
                      ? t('common.deselectAll')
                      : t('common.selectAll')
                  }}
                </v-btn>
              </div>
            </div>

            <div v-if="listCards.length === 0" class="text-center pa-8">
              <v-icon
                icon="mdi-format-list-bulleted"
                size="64"
                color="grey-lighten-2"
                class="mb-4"
              ></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.noCards') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1">{{ t('review.noCardsInCourse') }}</p>
            </div>

            <div v-else>
              <div
                v-for="(card, index) in listCards"
                :key="card.id"
                class="card-item pa-4 rounded-lg mb-2"
                :class="[selectedCards.includes(card.id) ? 'card-selected' : '']"
                @click="toggleCardSelection(card.id)"
              >
                <div class="d-flex align-center">
                  <!-- 选择框 -->
                  <div class="mr-3">
                    <v-checkbox
                      :model-value="selectedCards.includes(card.id)"
                      density="compact"
                      hide-details
                      @click.stop="toggleCardSelection(card.id)"
                    ></v-checkbox>
                  </div>

                  <!-- 序号 -->
                  <div class="rank-number mr-4 text-center" style="min-width: 40px">
                    <div class="text-body-1 font-weight-bold text-grey-darken-2">
                      {{ index + 1 }}
                    </div>
                  </div>

                  <!-- 卡片内容 -->
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-1">
                      <div class="text-body-1 font-weight-medium">{{ card.front }}</div>
                      <span v-if="card.deck" class="text-body-2 text-grey ml-2">
                        - {{ card.deck.title }}
                      </span>
                    </div>
                    <div class="text-body-2 text-grey-darken-2 mb-1">{{ card.back }}</div>
                    <!-- 到期时间 -->
                    <div v-if="card.srsState" class="text-caption text-grey-darken-1">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                      {{ t('review.nextReview') }}: {{ formatDueDate(card.srsState.reviewDueAt) }}
                    </div>
                  </div>

                  <!-- 状态标签 -->
                  <div class="d-flex align-center" style="gap: 8px">
                    <v-chip
                      v-for="(chip, idx) in getCardStatusChips(card)"
                      :key="idx"
                      size="small"
                      :color="chip.color"
                      variant="flat"
                    >
                      {{ chip.text }}
                    </v-chip>
                  </div>
                </div>
              </div>

              <!-- 加载更多 -->
              <div v-if="listHasMore" class="text-center mt-4">
                <v-btn
                  variant="outlined"
                  rounded="lg"
                  :loading="listLoading"
                  @click="loadMoreListCards"
                >
                  {{ t('common.loadMore') }}
                </v-btn>
              </div>
            </div>
          </div>
        </div>

        <!-- 管理模式 -->
        <div v-else-if="viewMode === 'manage'">
          <div v-if="!selectedCourse" class="text-center pa-8">
            <v-card border rounded="lg" class="pa-8">
              <v-icon icon="mdi-cog" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('review.selectCourse') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1">{{ t('review.selectCourseHint') }}</p>
            </v-card>
          </div>

          <v-card v-else border rounded="lg" class="pa-6">
            <h3 class="text-h6 font-weight-bold text-grey-darken-3 mb-4">
              {{ selectedCourse.course.name }} - {{ t('review.reviewSettings') }}
            </h3>

            <v-row>
              <v-col cols="12" md="6">
                <v-select
                  v-model="selectedCourse.setting.frequencySetting"
                  :items="frequencyOptions"
                  :label="t('review.reviewFrequency')"
                  variant="outlined"
                  rounded="lg"
                ></v-select>
              </v-col>

              <v-col cols="12" md="6">
                <v-select
                  v-model="selectedCourse.setting.status"
                  :items="statusOptions"
                  :label="t('review.learningStatus')"
                  variant="outlined"
                  rounded="lg"
                ></v-select>
              </v-col>
            </v-row>

            <div class="mt-4">
              <v-btn
                color="primary"
                variant="flat"
                rounded="lg"
                class="mr-3"
                @click="updateCourseSetting"
              >
                {{ t('common.saveSettings') }}
              </v-btn>
              <v-btn color="error" variant="outlined" rounded="lg" @click="removeCourse">
                {{ t('review.removeCourse') }}
              </v-btn>
            </div>
          </v-card>
        </div>
      </v-col>

      <!-- 右侧课程分类 -->
      <v-col cols="12" lg="3">
        <v-card rounded="lg" class="sticky-nav px-4 no-border">
          <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
            <v-icon icon="mdi-chart-line" color="primary" size="18" class="mr-2"></v-icon>
            {{ t('review.courseCategory') }}
          </h3>

          <!-- 全部标签 -->
          <div
            class="nav-item pa-3 rounded-lg mb-2"
            :class="[activeTab === 'all' ? 'nav-item-active' : 'nav-item-inactive']"
            @click="switchTab('all')"
          >
            <div class="d-flex align-center">
              <v-avatar
                :color="activeTab === 'all' ? 'primary' : 'grey-lighten-2'"
                size="32"
                class="mr-3"
              >
                <v-icon
                  icon="mdi-view-dashboard"
                  :color="activeTab === 'all' ? 'white' : 'grey'"
                  size="16"
                ></v-icon>
              </v-avatar>
              <div class="flex-grow-1">
                <div
                  class="text-body-2 font-weight-bold"
                  :class="activeTab === 'all' ? 'text-primary' : 'text-grey-darken-3'"
                >
                  {{ t('review.allCourses') }}
                </div>
                <div class="text-caption text-grey">
                  {{ totalDueCards }} {{ t('review.dueForReview') }}
                </div>
              </div>
              <v-chip size="small" color="primary" variant="flat">{{ totalDueCards }}</v-chip>
            </div>
          </div>

          <!-- 分课程标签 -->
          <div
            v-for="bank in courseMemoryBanks"
            :key="bank.course.id"
            class="nav-item pa-3 rounded-lg mb-2"
            :class="[
              activeTab === bank.course.id.toString() ? 'nav-item-active' : 'nav-item-inactive',
            ]"
            @click="switchTab(bank.course.id.toString())"
          >
            <div class="d-flex align-center">
              <v-avatar
                :color="
                  activeTab === bank.course.id.toString()
                    ? 'primary'
                    : getCourseStatusColor(bank.setting.status)
                "
                size="32"
                class="mr-3"
              >
                <v-icon icon="mdi-book-open-page-variant" color="white" size="16"></v-icon>
              </v-avatar>
              <div class="flex-grow-1">
                <div
                  class="text-body-2 font-weight-bold"
                  :class="
                    activeTab === bank.course.id.toString() ? 'text-primary' : 'text-grey-darken-3'
                  "
                >
                  {{ bank.course.name }}
                </div>
                <div class="text-caption text-grey">
                  {{ t('review.total') }}{{ bank.cardCount }}{{ t('review.cards') }} ·
                  {{ getFrequencyText(bank.setting.frequencySetting) }}
                </div>
              </div>
              <v-chip
                size="small"
                :color="bank.dueCardCount > 0 ? 'error' : 'success'"
                variant="flat"
              >
                {{ bank.dueCardCount }}
              </v-chip>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="mt-4 pa-3 rounded-lg" style="background-color: rgb(var(--v-theme-surface))">
            <h4 class="text-body-1 font-weight-bold text-grey-darken-4 mb-3">
              {{ t('review.learningStats') }}
            </h4>
            <div class="d-flex justify-space-between text-body-2 mb-2">
              <span class="text-grey-darken-2">{{ t('review.totalReviews') }}</span>
              <span class="font-weight-bold text-primary">{{ stats.totalReviews }}</span>
            </div>
            <div class="d-flex justify-space-between text-body-2 mb-2">
              <span class="text-grey-darken-2">{{ t('review.streakDays') }}</span>
              <span class="font-weight-bold text-success"
                >{{ stats.streakDays }}{{ t('review.days') }}</span
              >
            </div>
            <div class="d-flex justify-space-between text-body-2 mb-2">
              <span class="text-grey-darken-2">{{ t('review.avgAccuracy') }}</span>
              <span class="font-weight-bold text-warning">{{ stats.averageScore }}%</span>
            </div>
            <div class="d-flex justify-space-between text-body-2">
              <span class="text-grey-darken-2">{{ t('review.totalTime') }}</span>
              <span class="font-weight-bold text-info"
                >{{ stats.timeSpent }}{{ t('review.minutes') }}</span
              >
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { memoryApi } from '@/api'
import type {
  CourseMemoryBank,
  MemoryCardView,
  ReviewStats,
  CourseStudyStatus,
} from '@/types/memory'
import { ReviewResult, FrequencySetting, CourseStudyStatus as Status } from '@/types/memory'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'

const { t } = useI18n()

// 状态
const activeTab = ref<string>('all')
const viewMode = ref<'review' | 'list' | 'manage'>('review')
const isReviewing = ref(false)
const showAnswer = ref(false)
const currentCardIndex = ref(0)
const selectedCards = ref<number[]>([])

// 列表分页
const listCards = ref<MemoryCardView[]>([])
const listLoading = ref(false)
const listLastId = ref<number | undefined>(undefined)
const listHasMore = ref(true)

// 统计数据
const stats = ref<ReviewStats>({
  totalReviews: 0,
  streakDays: 0,
  averageScore: 0,
  timeSpent: 0,
})

// 使用 useFetch 加载记忆库课程
const {
  data: courseMemoryBanks,
  loading: _loadingCourses,
  refresh: refreshCourses,
} = useFetch<CourseMemoryBank[]>({
  fetchFn: memoryApi.getMemoryBankCourses,
  immediate: true,
  defaultValue: [],
})

// 使用 useFetch 加载复习队列
const {
  data: reviewCards,
  loading,
  refresh: refreshReviewQueue,
} = useFetch<MemoryCardView[]>({
  fetchFn: async () => {
    return memoryApi.getReviewQueue({
      courseId: selectedCourse.value?.course.id,
    })
  },
  immediate: true,
  defaultValue: [],
})

// 计算属性
const totalDueCards = computed(() => {
  return courseMemoryBanks.value.reduce((sum, bank) => sum + bank.dueCardCount, 0)
})

const selectedCourse = computed(() => {
  if (activeTab.value === 'all') return null
  const courseId = parseInt(activeTab.value)
  return courseMemoryBanks.value.find((bank) => bank.course.id === courseId)
})

const currentCard = computed(() => {
  if (reviewCards.value.length === 0 || currentCardIndex.value >= reviewCards.value.length) {
    return null
  }
  return reviewCards.value[currentCardIndex.value]
})

const reviewProgress = computed(() => {
  if (reviewCards.value.length === 0) return 0
  return Math.round((currentCardIndex.value / reviewCards.value.length) * 100)
})

const frequencyOptions = computed(() => [
  { title: t('review.frequencyHigh'), value: FrequencySetting.HIGH },
  { title: t('review.frequencyNormal'), value: FrequencySetting.NORMAL },
  { title: t('review.frequencyLow'), value: FrequencySetting.LOW },
])

const statusOptions = computed(() => [
  { title: t('review.statusStudying'), value: Status.STUDYING },
  { title: t('review.statusPaused'), value: Status.PAUSED },
  { title: t('review.statusArchived'), value: Status.ARCHIVED },
])

// 方法
const switchTab = (tabValue: string) => {
  activeTab.value = tabValue

  if (viewMode.value === 'review') {
    resetReview()
    void refreshReviewQueue()
  } else if (viewMode.value === 'list') {
    listLastId.value = undefined
    listHasMore.value = true
    void loadListCards(true)
  }
}

const switchViewMode = (mode: 'review' | 'list' | 'manage') => {
  viewMode.value = mode
  selectedCards.value = []
  if (mode === 'review') {
    resetReview()
  } else if (mode === 'list') {
    void loadListCards(true)
  }
}

const startReview = () => {
  if (reviewCards.value.length === 0) return
  isReviewing.value = true
  currentCardIndex.value = 0
  showAnswer.value = false
}

const resetReview = () => {
  isReviewing.value = false
  currentCardIndex.value = 0
  showAnswer.value = false
}

const revealAnswer = () => {
  showAnswer.value = true
}

const skipCard = () => {
  if (currentCardIndex.value < reviewCards.value.length - 1) {
    currentCardIndex.value++
    showAnswer.value = false
  }
}

// 提交复习
const { execute: executeReview } = useMutation(
  (params: { cardId: number; result: ReviewResult; timeSpent: number }) => {
    return memoryApi.reviewCard(params)
  },
  {
    showToast: false,
    onSuccess: () => {
      const currentCardId = currentCard.value?.id
      reviewCards.value = reviewCards.value.filter((card) => card.id !== currentCardId)

      if (reviewCards.value.length === 0) {
        void completeReview()
      } else {
        if (currentCardIndex.value >= reviewCards.value.length) {
          currentCardIndex.value = reviewCards.value.length - 1
        }
        showAnswer.value = false
      }
    },
  }
)

const submitReview = (result: ReviewResult) => {
  if (!currentCard.value) return
  void executeReview({
    cardId: currentCard.value.id,
    result,
    timeSpent: 5,
  })
}

const completeReview = async () => {
  isReviewing.value = false
  await refreshReviewQueue()
  await refreshCourses()

  if (reviewCards.value.length > 0) {
    setTimeout(() => {
      startReview()
    }, 1000)
  }
}

// 列表卡片
const loadListCards = async (reset = false) => {
  if (listLoading.value) return

  listLoading.value = true
  try {
    const response = await memoryApi.getCardList({
      courseId: selectedCourse.value?.course.id,
      limit: 20,
      lastId: reset ? undefined : listLastId.value,
    })

    if (response.code === 200 && response.data) {
      if (reset) {
        listCards.value = response.data
        listLastId.value = undefined
      } else {
        listCards.value = [...listCards.value, ...response.data]
      }

      if (response.data.length > 0) {
        const lastCard = response.data[response.data.length - 1]
        if (lastCard) {
          listLastId.value = lastCard.id
        }
      }

      listHasMore.value = response.data.length === 20
    }
  } finally {
    listLoading.value = false
  }
}

const loadMoreListCards = () => {
  void loadListCards(false)
}

// 卡片选择
const toggleCardSelection = (cardId: number) => {
  const index = selectedCards.value.indexOf(cardId)
  if (index > -1) {
    selectedCards.value.splice(index, 1)
  } else {
    selectedCards.value.push(cardId)
  }
}

const toggleSelectAll = () => {
  if (selectedCards.value.length === listCards.value.length) {
    selectedCards.value = []
  } else {
    selectedCards.value = listCards.value.map((card) => card.id)
  }
}

// 批量操作
const { execute: executeDelete } = useMutation(memoryApi.deleteCards, {
  successMessage: t('review.deleteSuccess'),
  onSuccess: () => {
    listCards.value = listCards.value.filter((card) => !selectedCards.value.includes(card.id))
    selectedCards.value = []
    void refreshReviewQueue()
  },
})

const deleteSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  void executeDelete(selectedCards.value)
}

const { execute: executeReset } = useMutation(memoryApi.resetCardProgress, {
  successMessage: t('review.resetSuccess'),
  onSuccess: () => {
    selectedCards.value = []
    void refreshReviewQueue()
    if (viewMode.value === 'list') {
      void loadListCards(true)
    }
  },
})

const resetSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  void executeReset(selectedCards.value)
}

const reviewSelectedCards = () => {
  if (selectedCards.value.length === 0) return
  reviewCards.value = listCards.value.filter((card) => selectedCards.value.includes(card.id))
  viewMode.value = 'review'
  startReview()
}

// 更新课程设置
const { execute: executeUpdateSetting } = useMutation(
  () => {
    if (!selectedCourse.value) throw new Error('No course selected')
    return memoryApi.updateCourseMemorySetting({
      courseId: selectedCourse.value.course.id,
      status: selectedCourse.value.setting.status,
      frequencySetting: selectedCourse.value.setting.frequencySetting,
    })
  },
  {
    successMessage: t('review.updateSuccess'),
  }
)

const updateCourseSetting = () => {
  void executeUpdateSetting()
}

// 移除课程
const { execute: executeRemove } = useMutation(
  () => {
    if (!selectedCourse.value) throw new Error('No course selected')
    return memoryApi.removeCourseMemoryBank(selectedCourse.value.course.id)
  },
  {
    successMessage: t('review.removeSuccess'),
    onSuccess: () => {
      void refreshCourses()
      activeTab.value = 'all'
    },
  }
)

const removeCourse = () => {
  void executeRemove()
}

// 工具函数
const getCourseStatusColor = (status: CourseStudyStatus): string => {
  switch (status) {
    case Status.STUDYING:
      return 'success'
    case Status.PAUSED:
      return 'warning'
    case Status.ARCHIVED:
      return 'grey'
    default:
      return 'grey'
  }
}

const getFrequencyText = (frequency: FrequencySetting): string => {
  switch (frequency) {
    case FrequencySetting.HIGH:
      return t('review.frequencyHigh')
    case FrequencySetting.NORMAL:
      return t('review.frequencyNormal')
    case FrequencySetting.LOW:
      return t('review.frequencyLow')
    default:
      return t('review.frequencyNormal')
  }
}

const getCardStatusChips = (card: MemoryCardView): { text: string; color: string }[] => {
  const chips: { text: string; color: string }[] = []

  if (!card.srsState) {
    chips.push({ text: t('review.newCard'), color: 'grey' })
    return chips
  }

  const isDue = new Date(card.srsState.reviewDueAt).getTime() <= Date.now()

  if (isDue) {
    chips.push({ text: t('review.dueCard'), color: 'primary' })
  }

  if (card.srsState.repetitions === 0) {
    chips.push({ text: t('review.newCard'), color: 'grey' })
  } else if (card.srsState.repetitions >= 3 && !isDue) {
    chips.push({ text: t('review.mastered'), color: 'success' })
  } else if (card.srsState.repetitions > 0 && card.srsState.repetitions < 3 && !isDue) {
    chips.push({
      text: `${t('review.reviewed')}${card.srsState.repetitions}${t('review.times')}`,
      color: 'warning',
    })
  }

  return chips.length > 0 ? chips : [{ text: t('review.unknownStatus'), color: 'grey' }]
}

// 格式化到期日期
const formatDueDate = (dateString: string): string => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

// 组件挂载
onMounted(() => {
  // 可以加载统计数据
  void memoryApi.getReviewStats().then((res) => {
    if (res.code === 200 && res.data) {
      stats.value = res.data
    }
  })
})
</script>

<style scoped>
.review-page {
  max-width: 1550px;
  margin: 0 auto;
}

.sticky-nav {
  position: sticky;
  top: 80px;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  transform: translateX(4px);
  border-color: rgba(var(--v-theme-primary), 0.2);
  background-color: rgb(var(--v-theme-surface));
}

.nav-item-active {
  background: rgba(var(--v-theme-primary), 0.08);
  border-color: rgba(var(--v-theme-primary), 0.2);
}

.nav-item-inactive {
  background: rgb(var(--v-theme-surface));
}

.question-content,
.answer-content {
  background: rgb(var(--v-theme-surface));
  border-radius: 12px;
}

.card-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background-color: rgb(var(--v-theme-surface));
}

.card-item:hover {
  transform: translateX(4px);
  border-color: rgba(var(--v-theme-primary), 0.3);
}

.card-selected {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.05);
}

@media (max-width: 1264px) {
  .sticky-nav {
    position: static;
  }
}
</style>
