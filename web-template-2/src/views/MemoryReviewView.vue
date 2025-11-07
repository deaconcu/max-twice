<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type {
  MemoryCardView,
  CourseMemoryBank,
  ReviewSession,
  ReviewStats
} from '@/types/memoryCard'
import { ReviewResult, FrequencySetting, CourseStudyStatus } from '@/types/memoryCard'
import { MemoryService } from '@/services/memoryService'

// 当前选中的标签
const activeTab = ref<string>('all')
const viewMode = ref<'review' | 'list' | 'manage'>('review')

// 课程记忆库数据
const courseMemoryBanks = ref<CourseMemoryBank[]>([])

const loading = ref(false)
const reviewCards = ref<MemoryCardView[]>([])
const currentCardIndex = ref(0)
const isReviewing = ref(false)
const showAnswer = ref(false)
const reviewSession = ref<ReviewSession | null>(null)
const selectedCards = ref<number[]>([])

// 列表模式数据
const listCards = ref<MemoryCardView[]>([])
const listLoading = ref(false)
const listLastId = ref<number | undefined>(undefined)
const listPageSize = ref(20)
const listHasMore = ref(true)

// 统计信息
const stats = ref<ReviewStats>({
  totalReviews: 0,
  streakDays: 0,
  averageScore: 0,
  timeSpent: 0
})

// 计算所有课程的到期卡片总数
const totalDueCards = computed(() => {
  return courseMemoryBanks.value.reduce((sum, bank) => sum + bank.dueCardCount, 0)
})

// 计算当前选中标签的课程
const selectedCourse = computed(() => {
  if (activeTab.value === 'all') return null
  const courseId = parseInt(activeTab.value)
  return courseMemoryBanks.value.find((bank) => bank.course.id === courseId)
})

// 计算当前要显示的卡片
const currentCards = computed(() => {
  if (viewMode.value === 'list') {
    return listCards.value
  } else {
    return reviewCards.value
  }
})

// 当前卡片
const currentCard = computed(() => {
  if (currentCards.value.length === 0 || currentCardIndex.value >= currentCards.value.length) {
    return null
  }
  return currentCards.value[currentCardIndex.value]
})

// 复习进度百分比
const reviewProgress = computed(() => {
  if (currentCards.value.length === 0) return 0
  return Math.round((currentCardIndex.value / currentCards.value.length) * 100)
})

onMounted(() => {
  loadMemoryBankCourses()
  loadReviewQueue()
  loadReviewStats()
})

// 加载记忆库课程
const loadMemoryBankCourses = async () => {
  try {
    const response = await MemoryService.getMemoryBankCourses()
    if (response.code === 200) {
      courseMemoryBanks.value = response.data
    }
  } catch (error) {
    console.error('Failed to load memory bank courses:', error)
  }
}

// 加载复习统计
const loadReviewStats = async () => {
  try {
    const response = await MemoryService.getReviewStats()
    if (response.code === 200) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('Failed to load review stats:', error)
  }
}

// 加载复习队列
const loadReviewQueue = async () => {
  loading.value = true
  try {
    const response = await MemoryService.getReviewQueue({
      courseId: selectedCourse.value?.course.id
    })

    if (response.code === 200) {
      reviewCards.value = response.data
    }
  } catch (error) {
    console.error('Failed to load review queue:', error)
  } finally {
    loading.value = false
  }
}

// 加载列表卡片
const loadListCards = async (reset = false) => {
  if (listLoading.value) return

  listLoading.value = true
  try {
    const response = await MemoryService.getCardList({
      courseId: selectedCourse.value?.course.id,
      limit: listPageSize.value,
      lastId: reset ? undefined : listLastId.value
    })

    if (response.code === 200) {
      if (reset) {
        listCards.value = response.data
        listLastId.value = undefined
      } else {
        listCards.value = [...listCards.value, ...response.data]
      }

      if (response.data.length > 0) {
        const lastCard = response.data[response.data.length - 1]
        listLastId.value = lastCard.id
      }

      listHasMore.value = response.data.length === listPageSize.value
    }
  } catch (error) {
    console.error('Failed to load list cards:', error)
  } finally {
    listLoading.value = false
  }
}

// 切换标签
const switchTab = (tabValue: string) => {
  activeTab.value = tabValue

  if (viewMode.value === 'review') {
    resetReview()
    loadReviewQueue()
  } else if (viewMode.value === 'list') {
    listLastId.value = undefined
    listHasMore.value = true
    loadListCards(true)
  }
}

// 切换视图模式
const switchViewMode = (mode: 'review' | 'list' | 'manage') => {
  viewMode.value = mode
  selectedCards.value = []
  if (mode === 'review') {
    resetReview()
  } else if (mode === 'list') {
    loadListCards(true)
  }
}

// 开始复习
const startReview = () => {
  if (currentCards.value.length === 0) return

  isReviewing.value = true
  currentCardIndex.value = 0
  showAnswer.value = false

  reviewSession.value = {
    startTime: new Date().toISOString(),
    totalCards: currentCards.value.length,
    reviewedCards: 0,
    correctAnswers: 0,
    results: []
  }
}

// 重置复习状态
const resetReview = () => {
  isReviewing.value = false
  currentCardIndex.value = 0
  showAnswer.value = false
  reviewSession.value = null
}

// 显示答案
const revealAnswer = () => {
  showAnswer.value = true
}

// 提交复习结果
const submitReview = async (result: ReviewResult) => {
  if (!currentCard.value) return

  try {
    const response = await MemoryService.reviewCard({
      cardId: currentCard.value.id,
      result: result,
      timeSpent: 5
    })

    if (response.code === 200) {
      if (reviewSession.value) {
        reviewSession.value.reviewedCards++
        if (result >= ReviewResult.GOOD) {
          reviewSession.value.correctAnswers++
        }
        reviewSession.value.results.push({
          cardId: currentCard.value.id,
          result: result,
          timeSpent: 5
        })
      }

      // 从当前复习队列中移除已复习的卡片
      reviewCards.value = reviewCards.value.filter((card) => card.id !== currentCard.value?.id)

      if (reviewCards.value.length === 0) {
        await completeReview()
      } else {
        if (currentCardIndex.value >= reviewCards.value.length) {
          currentCardIndex.value = reviewCards.value.length - 1
        }
        showAnswer.value = false
      }
    }
  } catch (error) {
    console.error('Failed to submit review:', error)
  }
}

// 完成复习
const completeReview = async () => {
  isReviewing.value = false

  if (reviewSession.value) {
    reviewSession.value.endTime = new Date().toISOString()
  }

  await loadReviewQueue()

  if (currentCards.value.length > 0) {
    setTimeout(() => {
      console.log(`发现还有 ${currentCards.value.length} 张到期卡片，继续复习...`)
      startReview()
    }, 1000)
  } else {
    console.log('恭喜！所有到期卡片复习完成')
    await loadMemoryBankCourses()
    await loadReviewStats()
  }
}

// 跳过当前卡片
const skipCard = () => {
  showAnswer.value = false
  if (currentCardIndex.value < currentCards.value.length - 1) {
    currentCardIndex.value++
  }
}

// 返回
const goBack = (): void => {
  if (activeTab.value === 'all') {
    if (viewMode.value === 'manage') {
      viewMode.value = 'review'
    } else {
      window.history.back()
    }
  } else {
    switchTab('all')
  }
}

// 获取课程状态颜色
const getCourseStatusColor = (status: CourseStudyStatus): string => {
  switch (status) {
    case CourseStudyStatus.STUDYING:
      return 'success'
    case CourseStudyStatus.PAUSED:
      return 'warning'
    case CourseStudyStatus.ARCHIVED:
      return 'grey'
    default:
      return 'grey'
  }
}

// 获取频率设置文本
const getFrequencyText = (frequency: FrequencySetting): string => {
  switch (frequency) {
    case FrequencySetting.HIGH:
      return '高频'
    case FrequencySetting.NORMAL:
      return '普通'
    case FrequencySetting.LOW:
      return '低频'
    default:
      return '普通'
  }
}

// 全选/取消全选
const toggleSelectAll = (): void => {
  if (selectedCards.value.length === currentCards.value.length) {
    selectedCards.value = []
  } else {
    selectedCards.value = currentCards.value.map((card) => card.id)
  }
}

// 删除选中的卡片
const deleteSelectedCards = async (): Promise<void> => {
  if (selectedCards.value.length === 0) return

  try {
    await MemoryService.deleteCards(selectedCards.value)
    reviewCards.value = reviewCards.value.filter((card) => !selectedCards.value.includes(card.id))
    selectedCards.value = []
    await loadReviewQueue()
  } catch (error) {
    console.error('Failed to delete cards:', error)
  }
}

// 重置选中卡片的学习进度
const resetSelectedCards = async (): Promise<void> => {
  if (selectedCards.value.length === 0) return

  try {
    await MemoryService.resetCards(selectedCards.value)
    selectedCards.value = []
    await loadReviewQueue()
  } catch (error) {
    console.error('Failed to reset cards:', error)
  }
}

// 立即复习选中的卡片
const reviewSelectedCards = (): void => {
  if (selectedCards.value.length === 0) return

  const cardsToReview = reviewCards.value.filter((card) => selectedCards.value.includes(card.id))
  switchViewMode('review')
  reviewCards.value = cardsToReview
  currentCardIndex.value = 0
  startReview()
}

// 是否到期
const isCardDue = (card: MemoryCardView): boolean => {
  if (!card.srsState) {
    return true
  }
  const dueTime = new Date(card.srsState.reviewDueAt).getTime()
  const now = new Date().getTime()
  return now >= dueTime
}

// 获取卡片状态标签
const getCardStatusChips = (card: MemoryCardView): Array<{ text: string; color: string }> => {
  const chips: Array<{ text: string; color: string }> = []

  if (!card.srsState) {
    chips.push({ text: '新卡片', color: 'grey' })
    return chips
  }

  const isDue = isCardDue(card)

  if (isDue) {
    chips.push({ text: '待复习', color: 'primary' })
  }

  if (card.srsState.repetitions === 0) {
    chips.push({ text: '新卡片', color: 'grey' })
  }

  if (card.srsState.repetitions >= 3 && !isDue) {
    chips.push({ text: '已掌握', color: 'success' })
  }

  if (card.srsState.repetitions > 0 && card.srsState.repetitions < 3 && !isDue) {
    chips.push({ text: `复习${card.srsState.repetitions}次`, color: 'warning' })
  }

  return chips.length > 0 ? chips : [{ text: '未知状态', color: 'grey' }]
}

// 更新课程设置
const updateCourseSetting = async () => {
  if (!selectedCourse.value) return

  try {
    await MemoryService.updateCourseMemorySetting(
      selectedCourse.value.course.id,
      selectedCourse.value.setting
    )
    console.log('Course setting updated successfully')
  } catch (error) {
    console.error('Failed to update course setting:', error)
  }
}

// 移除课程
const removeCourse = async () => {
  if (!selectedCourse.value) return

  try {
    await MemoryService.removeCourse(selectedCourse.value.course.id)
    await loadMemoryBankCourses()
    switchTab('all')
    console.log('Course removed successfully')
  } catch (error) {
    console.error('Failed to remove course:', error)
  }
}
</script>

<template>
  <div class="memory-review-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
          <!-- 页面标题 -->
          <div class="mb-6">
            <div class="d-flex align-center justify-space-between mb-4">
              <div class="d-flex align-center">
                <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
                  <v-icon size="32" color="#666666">mdi-brain</v-icon>
                </v-avatar>
                <div>
                  <h1 class="text-h4 font-weight-bold text-grey-darken-4">记忆复习中心</h1>
                  <p class="text-body-2 text-grey-darken-2 mt-1">
                    {{
                      activeTab === 'all'
                        ? '基于间隔重复算法的智能复习计划'
                        : `${selectedCourse?.course.name || ''}课程的记忆卡片复习`
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
                  复习
                </v-btn>

                <v-btn
                  :color="viewMode === 'list' ? 'primary' : 'grey'"
                  :variant="viewMode === 'list' ? 'flat' : 'outlined'"
                  rounded="lg"
                  size="small"
                  @click="switchViewMode('list')"
                >
                  <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-2"></v-icon>
                  列表
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
                  管理
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
                  <p class="text-body-1 text-grey-darken-2 mt-4">正在加载复习队列...</p>
                </div>

                <!-- 空队列状态 -->
                <div v-else-if="!isReviewing && currentCards.length === 0" class="text-center">
                  <v-card border rounded="lg" class="pa-8">
                    <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">太棒了！</h3>
                    <p class="text-body-1 text-grey-darken-1 mb-4">
                      {{ selectedCourse ? `${selectedCourse.course.name}课程` : '全部课程' }}暂时没有需要复习的卡片
                    </p>
                  </v-card>
                </div>

                <!-- 开始复习状态 -->
                <div v-else-if="!isReviewing" class="text-center">
                  <v-card class="no-border review-start-card" rounded="xl" style="padding: 300px 32px;">
                    <v-icon icon="mdi-cards" size="64" color="primary" class="mb-4"></v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">准备开始复习</h3>
                    <p class="text-body-1 text-grey-darken-1 mb-4">
                      {{ selectedCourse ? selectedCourse.course.name : '全部课程' }}有
                      <span class="font-weight-bold text-primary">{{
                        selectedCourse ? selectedCourse.dueCardCount : totalDueCards
                      }}</span>
                      张卡片等待复习
                    </p>
                    <v-btn color="primary" variant="flat" rounded="lg" size="large" @click="startReview">
                      <v-icon icon="mdi-play" class="mr-2"></v-icon>
                      开始复习
                    </v-btn>
                  </v-card>
                </div>

                <!-- 复习中状态 -->
                <div v-else-if="currentCard">
                  <!-- 进度条 -->
                  <v-card class="no-border mb-0 pt-0 pb-2 px-0">
                    <div class="d-flex align-center justify-space-between">
                      <span class="text-caption text-grey-darken-1"> 第 {{ currentCardIndex + 1 }} / {{ currentCards.length }} 张 </span>
                      <span class="text-caption text-grey-darken-1">{{ reviewProgress }}%</span>
                    </div>
                  </v-card>

                  <!-- 卡片区域 -->
                  <v-card border rounded="lg" class="mb-4">
                    <div class="card-container pa-8 d-flex align-center justify-center" style="min-height: 700px">
                      <!-- 问题面 -->
                      <div v-if="!showAnswer" class="text-center">
                        <div class="d-flex align-center justify-center mb-4">
                          <v-icon icon="mdi-help-circle" color="primary" size="48"></v-icon>
                        </div>
                        <h3 class="text-h5 font-weight-bold text-primary mb-4">问题</h3>

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
                          <v-btn color="primary" variant="flat" rounded="lg" size="large" @click="revealAnswer">
                            <v-icon icon="mdi-eye" class="mr-2"></v-icon>
                            显示答案
                          </v-btn>
                        </div>
                      </div>

                      <!-- 答案面 -->
                      <div v-else class="text-center">
                        <div class="d-flex align-center justify-center mb-4">
                          <v-icon icon="mdi-lightbulb" color="success" size="48"></v-icon>
                        </div>
                        <h3 class="text-h5 font-weight-bold text-success mb-4">答案</h3>

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
                          <p class="text-body-1 text-grey-darken-1 mb-4">请根据你的掌握程度选择：</p>
                          <div class="d-flex justify-center flex-wrap" style="gap: 12px">
                            <v-btn color="error" variant="outlined" rounded="lg" @click="submitReview(ReviewResult.FAILED)">
                              <v-icon icon="mdi-close" class="mr-2"></v-icon>
                              忘记了
                            </v-btn>
                            <v-btn color="warning" variant="outlined" rounded="lg" @click="submitReview(ReviewResult.HARD)">
                              <v-icon icon="mdi-help" class="mr-2"></v-icon>
                              困难
                            </v-btn>
                            <v-btn color="success" variant="outlined" rounded="lg" @click="submitReview(ReviewResult.GOOD)">
                              <v-icon icon="mdi-check" class="mr-2"></v-icon>
                              良好
                            </v-btn>
                            <v-btn color="primary" variant="flat" rounded="lg" @click="submitReview(ReviewResult.EASY)">
                              <v-icon icon="mdi-thumb-up" class="mr-2"></v-icon>
                              简单
                            </v-btn>
                          </div>
                        </div>
                      </div>
                    </div>
                  </v-card>

                  <!-- 操作按钮 -->
                  <div class="d-flex justify-space-between">
                    <v-btn variant="outlined" rounded="lg" @click="resetReview">
                      <v-icon icon="mdi-stop" class="mr-2"></v-icon>
                      结束复习
                    </v-btn>

                    <v-btn variant="text" rounded="lg" @click="skipCard" :disabled="!showAnswer">
                      跳过
                      <v-icon icon="mdi-skip-next" class="ml-2"></v-icon>
                    </v-btn>
                  </div>
                </div>
              </div>

              <!-- 列表模式 -->
              <div v-else-if="viewMode === 'list'">
                <div>
                    <div class="d-flex align-center justify-space-between mb-4">
                      <h3 class="text-h6 font-weight-bold text-grey-darken-4">卡片列表 ({{ currentCards.length }} 张)</h3>

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
                          复习选中 ({{ selectedCards.length }})
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
                          重新学习
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
                          删除
                        </v-btn>

                        <v-btn
                          :color="selectedCards.length === currentCards.length && currentCards.length > 0 ? 'primary' : 'grey'"
                          variant="tonal"
                          rounded="lg"
                          size="small"
                          @click="toggleSelectAll"
                          :disabled="currentCards.length === 0"
                        >
                          <v-icon
                            :icon="selectedCards.length === currentCards.length && currentCards.length > 0 ? 'mdi-checkbox-marked' : 'mdi-checkbox-blank-outline'"
                            size="16"
                            class="mr-2"
                          ></v-icon>
                          {{ selectedCards.length === currentCards.length && currentCards.length > 0 ? '取消全选' : '全选' }}
                        </v-btn>
                      </div>
                    </div>

                    <div v-if="currentCards.length === 0" class="text-center pa-8">
                      <v-icon icon="mdi-format-list-bulleted" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                      <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">暂无卡片</h3>
                      <p class="text-body-1 text-grey-darken-1">该课程暂时没有记忆卡片</p>
                    </div>

                    <div v-else>
                      <div
                        v-for="(card, index) in currentCards"
                        :key="card.id"
                        class="card-item pa-4 rounded-lg mb-2"
                        :class="[selectedCards.includes(card.id) ? 'card-selected' : '']"
                        @click="
                          selectedCards.includes(card.id)
                            ? (selectedCards = selectedCards.filter((id) => id !== card.id))
                            : selectedCards.push(card.id)
                        "
                      >
                        <div class="d-flex align-center">
                          <!-- 选择框 -->
                          <div class="mr-3">
                            <v-checkbox
                              :model-value="selectedCards.includes(card.id)"
                              @update:model-value="
                                (value) => {
                                  if (value) {
                                    if (!selectedCards.includes(card.id)) {
                                      selectedCards.push(card.id)
                                    }
                                  } else {
                                    selectedCards = selectedCards.filter((id) => id !== card.id)
                                  }
                                }
                              "
                              density="compact"
                              hide-details
                              @click.stop
                            ></v-checkbox>
                          </div>

                          <!-- 序号 -->
                          <div class="rank-number mr-4 text-center" style="min-width: 40px">
                            <div class="text-body-1 font-weight-bold text-grey-darken-2">{{ index + 1 }}</div>
                          </div>

                          <!-- 卡片内容 -->
                          <div class="flex-grow-1">
                            <div class="d-flex align-center mb-1">
                              <div class="text-body-1 font-weight-medium">{{ card.front }}</div>
                              <span v-if="card.deck" class="text-body-2 text-grey ml-2"> - {{ card.deck.title }} </span>
                            </div>
                            <div class="text-body-2 text-grey-darken-2">{{ card.back }}</div>
                          </div>

                          <!-- 状态标签 -->
                          <div class="d-flex align-center" style="gap: 8px">
                            <v-chip
                              v-for="(chip, idx) in getCardStatusChips(card)"
                              :key="idx"
                              size="small"
                              :color="chip.color"
                              variant="flat"
                              :class="chip.text === '待复习' ? 'text-white' : ''"
                            >
                              {{ chip.text }}
                            </v-chip>
                          </div>
                        </div>
                      </div>
                    </div>
                </div>
              </div>

              <!-- 管理模式 -->
              <div v-else-if="viewMode === 'manage'">
                <div v-if="!selectedCourse" class="text-center pa-8">
                  <v-card border rounded="lg" class="pa-8">
                    <v-icon icon="mdi-cog" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                    <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">选择课程</h3>
                    <p class="text-body-1 text-grey-darken-1">请先选择一个课程进行管理设置</p>
                  </v-card>
                </div>

                <v-card v-else border rounded="lg" class="pa-6">
                  <h3 class="text-h6 font-weight-bold text-grey-darken-3 mb-4">{{ selectedCourse.course.name }} - 复习设置</h3>

                  <v-row>
                    <v-col cols="12" md="6">
                      <v-select
                        v-model="selectedCourse.setting.frequencySetting"
                        :items="[
                          { title: '高频复习', value: FrequencySetting.HIGH },
                          { title: '普通复习', value: FrequencySetting.NORMAL },
                          { title: '低频复习', value: FrequencySetting.LOW }
                        ]"
                        label="复习频率"
                        variant="outlined"
                        rounded="lg"
                      ></v-select>
                    </v-col>

                    <v-col cols="12" md="6">
                      <v-select
                        v-model="selectedCourse.setting.status"
                        :items="[
                          { title: '学习中', value: CourseStudyStatus.STUDYING },
                          { title: '已暂停', value: CourseStudyStatus.PAUSED },
                          { title: '已归档', value: CourseStudyStatus.ARCHIVED }
                        ]"
                        label="学习状态"
                        variant="outlined"
                        rounded="lg"
                      ></v-select>
                    </v-col>

                    <v-col cols="12" md="6">
                      <v-text-field
                        v-model.number="selectedCourse.setting.dailyNewCards"
                        label="每日新卡片数"
                        type="number"
                        variant="outlined"
                        rounded="lg"
                      ></v-text-field>
                    </v-col>

                    <v-col cols="12" md="6">
                      <v-text-field
                        v-model.number="selectedCourse.setting.dailyReviewCards"
                        label="每日复习卡片数"
                        type="number"
                        variant="outlined"
                        rounded="lg"
                      ></v-text-field>
                    </v-col>
                  </v-row>

                  <div class="mt-4">
                    <v-btn color="primary" variant="flat" rounded="lg" class="mr-3" @click="updateCourseSetting"> 保存设置 </v-btn>
                    <v-btn color="error" variant="outlined" rounded="lg" @click="removeCourse"> 移除课程 </v-btn>
                  </div>
                </v-card>
              </div>
            </v-col>

            <!-- 右侧课程分类 -->
            <v-col cols="12" lg="3">
              <v-card border rounded="lg" class="sticky-nav pa-4">
                <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
                  <v-icon icon="mdi-chart-line" color="primary" size="18" class="mr-2"></v-icon>
                  课程分类
                </h3>

                <!-- 全部标签 -->
                <div
                  class="nav-item pa-3 rounded-lg mb-2"
                  :class="[activeTab === 'all' ? 'nav-item-active' : 'nav-item-inactive']"
                  @click="switchTab('all')"
                >
                  <div class="d-flex align-center">
                    <v-avatar :color="activeTab === 'all' ? 'primary' : 'grey-lighten-2'" size="32" class="mr-3">
                      <v-icon icon="mdi-view-dashboard" :color="activeTab === 'all' ? 'white' : 'grey'" size="16"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-bold" :class="activeTab === 'all' ? 'text-primary' : 'text-grey-darken-3'">
                        全部课程
                      </div>
                      <div class="text-caption text-grey">{{ totalDueCards }} 张待复习</div>
                    </div>
                    <v-chip size="small" color="primary" variant="flat">{{ totalDueCards }}</v-chip>
                  </div>
                </div>

                <!-- 分课程标签 -->
                <div
                  v-for="bank in courseMemoryBanks"
                  :key="bank.course.id"
                  class="nav-item pa-3 rounded-lg mb-2"
                  :class="[activeTab === bank.course.id.toString() ? 'nav-item-active' : 'nav-item-inactive']"
                  @click="switchTab(bank.course.id.toString())"
                >
                  <div class="d-flex align-center">
                    <v-avatar
                      :color="activeTab === bank.course.id.toString() ? 'primary' : getCourseStatusColor(bank.setting.status)"
                      size="32"
                      class="mr-3"
                    >
                      <v-icon
                        icon="mdi-book-open-page-variant"
                        :color="activeTab === bank.course.id.toString() ? 'white' : 'white'"
                        size="16"
                      ></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <div
                        class="text-body-2 font-weight-bold"
                        :class="activeTab === bank.course.id.toString() ? 'text-primary' : 'text-grey-darken-3'"
                      >
                        {{ bank.course.name }}
                      </div>
                      <div class="text-caption text-grey">共{{ bank.cardCount }}张 · {{ getFrequencyText(bank.setting.frequencySetting) }}</div>
                    </div>
                    <v-chip size="small" :color="bank.dueCardCount > 0 ? 'error' : 'success'" variant="flat">
                      {{ bank.dueCardCount }}
                    </v-chip>
                  </div>
                </div>

                <!-- 统计信息 -->
                <div class="mt-4 pa-3 rounded-lg" style="background-color: #fafbfc">
                  <h4 class="text-body-1 font-weight-bold text-grey-darken-4 mb-3">学习统计</h4>
                  <div class="d-flex justify-space-between text-body-2 mb-2">
                    <span class="text-grey-darken-2">总复习次数</span>
                    <span class="font-weight-bold text-primary">{{ stats.totalReviews }}</span>
                  </div>
                  <div class="d-flex justify-space-between text-body-2 mb-2">
                    <span class="text-grey-darken-2">连续天数</span>
                    <span class="font-weight-bold text-success">{{ stats.streakDays }}天</span>
                  </div>
                  <div class="d-flex justify-space-between text-body-2 mb-2">
                    <span class="text-grey-darken-2">平均正确率</span>
                    <span class="font-weight-bold text-warning">{{ stats.averageScore }}%</span>
                  </div>
                  <div class="d-flex justify-space-between text-body-2">
                    <span class="text-grey-darken-2">总学习时长</span>
                    <span class="font-weight-bold text-info">{{ stats.timeSpent }}分钟</span>
                  </div>
                </div>
              </v-card>
            </v-col>
          </v-row>
        </div>

  </div>
</template>

<style scoped>
.memory-review-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

.sticky-nav {
  position: sticky;
  top: 20px;
  background-color: #ffffff;
  border: 1px solid #edeff1;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.2);
  background-color: #fafbfc;
}

.nav-item-active {
  background: rgba(25, 118, 210, 0.08);
  border-color: rgba(25, 118, 210, 0.2);
}

.nav-item-inactive {
  background: white;
}

.question-content,
.answer-content {
  background: #f8f9fa;
  border-radius: 12px;
}

.answer-content {
}

.card-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #edeff1;
  background-color: #ffffff;
}

.card-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.3);
  background-color: #fafbfc;
}

.card-selected {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.05);
}

.review-start-card {
  background-color: #FAFAFA !important;
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }
}
</style>
