<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MemoryCardView, UserCardSRSState, ReviewSession, ReviewCardResult, CourseMemoryBank, DeckUpdateDiff, MemoryCardDeck, CardContentDiff } from '@/types/memoryCard'
import { ReviewResult, FrequencySetting, CourseStudyStatus, DeckState } from '@/types/memoryCard'
import { MemoryService } from '@/services/memoryService'
import DeckUpdateDiffDialog from '@/components/memory/DeckUpdateDiffDialog.vue'
import DeckDetailDialog from '@/components/memory/DeckDetailDialog.vue'
import CardContentDiffDialog from '@/components/memory/CardContentDiffDialog.vue'

const { t } = useI18n()

// 当前选中的标签
const activeTab = ref<string>('all')
const viewMode = ref<'review' | 'list' | 'manage'>('review') // review: 复习模式, list: 列表模式, manage: 管理模式


// 课程记忆库数据
const courseMemoryBanks = ref<CourseMemoryBank[]>([])

const loading = ref(false)
const reviewCards = ref<MemoryCardView[]>([])
const currentCardIndex = ref(0)
const isReviewing = ref(false)
const showAnswer = ref(false)
const reviewSession = ref<ReviewSession | null>(null)
const selectedCards = ref<number[]>([]) // 选中的卡片ID列表

// 列表模式专用数据
const listCards = ref<MemoryCardView[]>([])
const listLoading = ref(false)
const listLastId = ref<number | undefined>(undefined)
const listPageSize = ref(20)
const listHasMore = ref(true)
const stats = ref({
  totalReviews: 0,
  streakDays: 0,
  averageScore: 0,
  timeSpent: 0
})

// 更新检测相关状态
const showUpdateDiffDialog = ref(false)
const currentDeckDiff = ref<DeckUpdateDiff | null>(null)
const deckUpdateMap = ref<Map<number, boolean>>(new Map()) // 卡片组更新状态

// 卡片内容差异相关状态
const showCardDiffDialog = ref(false)
const currentCardDiff = ref<CardContentDiff | null>(null)

// 卡片组详情对话框相关状态
const showDeckDetailDialog = ref(false)
const selectedDeck = ref<MemoryCardDeck | null>(null)

// 计算所有课程的到期卡片总数
const totalDueCards = computed(() => {
  return courseMemoryBanks.value.reduce((sum, bank) => sum + bank.dueCardCount, 0)
})

// 计算当前选中标签的课程
const selectedCourse = computed(() => {
  if (activeTab.value === 'all') return null
  const courseId = parseInt(activeTab.value)
  return courseMemoryBanks.value.find(bank => bank.course.id === courseId)
})

// 计算当前要显示的卡片（根据视图模式和选中的标签）
const currentCards = computed(() => {
  if (viewMode.value === 'list') {
    // 列表模式显示全部卡片
    return listCards.value
  } else {
    // 复习模式显示到期卡片
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
  // loadReviewStats() // 已注释：学习统计功能
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

      // 检测卡片组更新
      await checkDeckUpdates()
    }
  } catch (error) {
    console.error('Failed to load review queue:', error)
  } finally {
    loading.value = false
  }
}

// 加载列表卡片（全部卡片，支持keyset分页）
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
      
      // 更新lastId为当前页最后一张卡片的ID
      if (response.data.length > 0) {
        const lastCard = response.data[response.data.length - 1]
        listLastId.value = lastCard.id
      }
      
      // 判断是否还有更多数据
      listHasMore.value = response.data.length === listPageSize.value
      
      // 检测卡片组更新
      await checkDeckUpdates()
    }
  } catch (error) {
    console.error('Failed to load list cards:', error)
  } finally {
    listLoading.value = false
  }
}

// 检测卡片组更新
const checkDeckUpdates = async () => {
  try {
    // 直接从当前卡片中检测是否有更新
    const cardsWithDeckUpdates = currentCards.value.filter(card => card.hasDeckUpdate === true)

    // 按deck分组统计有更新的卡片
    const deckUpdateStatus = new Map<number, boolean>()
    cardsWithDeckUpdates.forEach(card => {
      if (card.deck?.id) {
        deckUpdateStatus.set(card.deck.id, true)
      }
    })

    deckUpdateMap.value = deckUpdateStatus

    // 如果有更新，可以显示全局提示
    if (cardsWithDeckUpdates.length > 0) {
      console.log(`发现 ${cardsWithDeckUpdates.length} 张卡片的deck有更新`)
    }

    // 检测卡片内容更新
    const cardsWithContentUpdates = currentCards.value.filter(card => card.hasCardUpdate === true)
    if (cardsWithContentUpdates.length > 0) {
      console.log(`发现 ${cardsWithContentUpdates.length} 张卡片内容有更新`)
    }
  } catch (error) {
    console.error('Failed to check deck updates:', error)
  }
}

// 显示卡片组详情
const viewDeckDetails = (deckId: number | undefined) => {
  if (!deckId) return
  
  // 从当前卡片中找到对应的deck信息
  const card = currentCards.value.find(c => c.deck?.id === deckId)
  if (card?.deck) {
    selectedDeck.value = card.deck
    showDeckDetailDialog.value = true
  }
}

// 处理从DeckDetailDialog触发的显示diff对话框事件
const handleShowDiffDialog = (deckDiff: DeckUpdateDiff) => {
  currentDeckDiff.value = deckDiff
  showUpdateDiffDialog.value = true
}

// 显示卡片内容差异
const showCardContentDiff = async (cardId: number) => {
  try {
    const response = await MemoryService.getCardDiff(cardId)
    if (response.code === 200) {
      currentCardDiff.value = response.data
      showCardDiffDialog.value = true
    }
  } catch (error) {
    console.error('Failed to get card diff:', error)
  }
}

// 应用卡片内容更新
const applyCardUpdate = async (cardId: number) => {
  try {
    // 使用现有的接受deck更新API，只传入单个卡片ID
    const response = await MemoryService.acceptDeckChanges(
      currentCardDiff.value?.cardId || cardId,
      [cardId]
    )

    if (response.code === 200) {
      // 重新加载数据
      await loadReviewQueue()
      if (viewMode.value === 'list') {
        await loadListCards(true)
      }

      // 关闭对话框
      showCardDiffDialog.value = false
      currentCardDiff.value = null

      console.log('Card content update accepted successfully')
    }
  } catch (error) {
    console.error('Failed to accept card update:', error)
  }
}

// 应用卡片组更新
const applyDeckUpdate = async (acceptedChanges: { updateMeta: boolean; cardIds: number[] }) => {
  if (!currentDeckDiff.value) return
  
  try {
    const response = await MemoryService.acceptDeckChanges(
      currentDeckDiff.value.deckId, 
      acceptedChanges.cardIds
    )
    
    if (response.code === 200) {
      // 更新成功，移除更新标记
      deckUpdateMap.value.set(currentDeckDiff.value.deckId, false)
      
      // 重新加载数据
      await loadReviewQueue()
      if (viewMode.value === 'list') {
        await loadListCards(true)
      }
      
      // 关闭对话框
      showUpdateDiffDialog.value = false
      currentDeckDiff.value = null
      
      console.log('Deck changes accepted successfully')
    }
  } catch (error) {
    console.error('Failed to accept deck changes:', error)
  }
}

// 检查卡片组是否有更新
const hasDeckUpdate = (deckId: number): boolean => {
  return deckUpdateMap.value.get(deckId) === true
}

// 切换标签
const switchTab = (tabValue: string) => {
  activeTab.value = tabValue
  
  // 根据当前视图模式加载对应数据
  if (viewMode.value === 'review') {
    resetReview()
    loadReviewQueue()
  } else if (viewMode.value === 'list') {
    // 切换课程时重置列表分页状态
    listLastId.value = undefined
    listHasMore.value = true
    loadListCards(true)
  }
}

// 切换视图模式
const switchViewMode = (mode: 'review' | 'list' | 'manage') => {
  viewMode.value = mode
  selectedCards.value = [] // 切换模式时清空选中状态
  if (mode === 'review') {
    resetReview()
  } else if (mode === 'list') {
    // 切换到列表模式时加载列表数据
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
      reviewCards.value = reviewCards.value.filter(card => card.id !== currentCard.value?.id)
      
      // 检查是否还有卡片
      if (reviewCards.value.length === 0) {
        // 当前批次完成，尝试加载下一批
        await completeReview()
      } else {
        // 继续复习当前批次的下一张卡片
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

// 下一张卡片
const nextCard = () => {
  showAnswer.value = false
  
  if (currentCardIndex.value < currentCards.value.length - 1) {
    currentCardIndex.value++
  } else {
    completeReview()
  }
}

// 完成复习
const completeReview = async () => {
  isReviewing.value = false
  
  if (reviewSession.value) {
    reviewSession.value.endTime = new Date().toISOString()
  }
  
  // 重新加载复习队列，检查是否还有更多到期卡片
  await loadReviewQueue()
  
  // 如果还有到期卡片，自动开始下一轮复习
  if (currentCards.value.length > 0) {
    // 短暂延迟后自动开始，给用户一个喘息时间
    setTimeout(() => {
      console.log(`发现还有 ${currentCards.value.length} 张到期卡片，继续复习...`)
      startReview()
    }, 1000)
  } else {
    // 没有更多到期卡片，复习完成
    console.log('恭喜！所有到期卡片复习完成')
    // 重新加载课程数据以更新统计信息
    await loadMemoryBankCourses()
    // await loadReviewStats() // 已注释：学习统计功能
  }
}

// 跳过当前卡片
const skipCard = () => {
  nextCard()
}

// 通用方法
const goBack = (): void => {
  if (activeTab.value === 'all') {
    // 根据当前视图模式返回不同页面
    if (viewMode.value === 'manage') {
      // 从管理页面返回到记忆中心
      viewMode.value = 'review'
    } else {
      // 从记忆中心返回到课程列表
      window.history.back()
    }
  } else {
    // 从具体课程返回到全部课程
    switchTab('all')
  }
}

// 获取课程状态颜色
const getCourseStatusColor = (status: CourseStudyStatus): string => {
  switch (status) {
    case CourseStudyStatus.STUDYING: return 'success'
    case CourseStudyStatus.PAUSED: return 'warning'
    case CourseStudyStatus.ARCHIVED: return 'grey'
    default: return 'grey'
  }
}

// 获取频率设置文本
const getFrequencyText = (frequency: FrequencySetting): string => {
  switch (frequency) {
    case FrequencySetting.HIGH: return '高频'
    case FrequencySetting.NORMAL: return '普通'
    case FrequencySetting.LOW: return '低频'
    default: return '普通'
  }
}

// 获取排名图标
const getRankIcon = (index: number): string | null => {
  if (index === 0) return 'mdi-trophy'
  if (index === 1) return 'mdi-medal'
  if (index === 2) return 'mdi-medal-outline'
  return null
}

// 获取排名颜色
const getRankColor = (index: number): string => {
  if (index === 0) return 'amber'
  if (index === 1) return 'amber'
  if (index === 2) return 'amber'
  return 'grey-lighten-3'
}

// 卡片操作相关方法
// 全选/取消全选
const toggleSelectAll = (): void => {
  if (selectedCards.value.length === currentCards.value.length) {
    selectedCards.value = []
  } else {
    selectedCards.value = currentCards.value.map(card => card.id)
  }
}

// 删除选中的卡片
const deleteSelectedCards = async (): Promise<void> => {
  if (selectedCards.value.length === 0) return
  
  try {
    // TODO: 调用API删除卡片
    console.log('删除卡片:', selectedCards.value)
    
    // 模拟删除操作
    reviewCards.value = reviewCards.value.filter(card => !selectedCards.value.includes(card.id))
    selectedCards.value = []
    
    // 刷新数据
    await loadReviewQueue()
  } catch (error) {
    console.error('Failed to delete cards:', error)
  }
}

// 重置选中卡片的学习进度
const resetSelectedCards = async (): Promise<void> => {
  if (selectedCards.value.length === 0) return
  
  try {
    // TODO: 调用API重置卡片进度
    console.log('重置卡片进度:', selectedCards.value)
    
    // 模拟重置操作
    selectedCards.value.forEach(cardId => {
      const card = reviewCards.value.find(c => c.id === cardId)
      if (card && card.srsState) {
        card.srsState.repetitions = 0
        card.srsState.intervalDays = 0
        card.srsState.reviewDueAt = new Date().toISOString()
      }
    })
    
    selectedCards.value = []
    
    // 刷新数据
    await loadReviewQueue()
  } catch (error) {
    console.error('Failed to reset cards:', error)
  }
}

// 立即复习选中的卡片
const reviewSelectedCards = (): void => {
  if (selectedCards.value.length === 0) return
  
  // 过滤出选中的卡片
  const cardsToReview = reviewCards.value.filter(card => selectedCards.value.includes(card.id))
  
  // 切换到复习模式
  switchViewMode('review')
  
  // 设置复习队列为选中的卡片
  reviewCards.value = cardsToReview
  currentCardIndex.value = 0
  
  // 开始复习
  startReview()
}

// 是否到期
const isCardDue = (card: MemoryCardView): boolean => {
  if (!card.srsState) {
    return true
  }
  const dueTime = new Date(card.srsState.reviewDueAt).getTime()
  const now = new Date().getTime()
  const result = now >= dueTime
  return result
}

// 获取卡片状态标签（返回标签对象数组）
const getCardStatusChips = (card: MemoryCardView): Array<{ text: string; color: string }> => {
  const chips: Array<{ text: string; color: string }> = []

  if (!card.srsState) {
    chips.push({ text: '新卡片', color: 'grey' })
    return chips
  }

  const isDue = isCardDue(card)

  // 待复习：到期需要复习
  if (isDue) {
    chips.push({ text: '待复习', color: 'primary' })
  }

  // 新卡片：从未复习过
  if (card.srsState.repetitions === 0) {
    chips.push({ text: '新卡片', color: 'grey' })
  }

  // 已掌握：复习3次以上
  if (card.srsState.repetitions >= 3 && !isDue) {
    chips.push({ text: '已掌握', color: 'success' })
  }

  // 复习中：已复习但未到期且未掌握
  if (card.srsState.repetitions > 0 && card.srsState.repetitions < 3 && !isDue) {
    chips.push({ text: `复习${card.srsState.repetitions}次`, color: 'warning' })
  }

  return chips.length > 0 ? chips : [{ text: '未知状态', color: 'grey' }]
}
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <!-- 页面头部 -->
      <v-col cols="12" class="mb-4">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-btn
              icon="mdi-arrow-left"
              variant="text"
              color="grey-darken-2"
              class="mr-3"
              @click="goBack"
            ></v-btn>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">记忆复习中心</h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                <v-icon icon="mdi-brain" color="primary" size="16" class="mr-1"></v-icon>
                {{
                  activeTab === 'all'
                    ? '基于间隔重复算法的智能复习计划'
                    : `${selectedCourse?.course.name || ''}课程的记忆卡片复习`
                }}
              </p>
            </div>
          </div>

          <!-- 视图模式选择器 -->
          <div class="d-flex align-center" style="gap: 8px;">
            <span class="text-body-2 text-grey-darken-2 mr-3">视图模式：</span>
            <v-btn
              :color="viewMode === 'review' ? 'primary' : 'grey-darken-2'"
              :variant="viewMode === 'review' ? 'flat' : 'tonal'"
              rounded="lg"
              size="small"
              @click="switchViewMode('review')"
            >
              <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
              复习
            </v-btn>
            
            <v-btn
              :color="viewMode === 'list' ? 'primary' : 'grey-darken-2'"
              :variant="viewMode === 'list' ? 'flat' : 'tonal'"
              rounded="lg"
              size="small"
              @click="switchViewMode('list')"
            >
              <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-2"></v-icon>
              列表
            </v-btn>
            
            <v-btn
              v-if="selectedCourse"
              :color="viewMode === 'manage' ? 'primary' : 'grey-darken-2'"
              :variant="viewMode === 'manage' ? 'flat' : 'tonal'"
              rounded="lg"
              size="small"
              @click="switchViewMode('manage')"
            >
              <v-icon icon="mdi-cog" size="16" class="mr-2"></v-icon>
              管理
            </v-btn>
          </div>
        </div>
      </v-col>

      <!-- 左侧导航栏 -->
      <v-col cols="3" class="pr-6">
        <v-card flat color="grey-lighten-5" rounded="xl" class="sticky-nav">
          <v-card-text class="pa-4">
            <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
              <v-icon icon="mdi-chart-line" color="primary" size="18" class="mr-2"></v-icon>
              课程分类
            </h3>

            <v-list bg-color="transparent" class="pa-0">
              <!-- 全部标签 -->
              <v-list-item
                :value="'all'"
                class="nav-item ma-1 rounded-lg"
                :class="[activeTab === 'all' ? 'nav-item-active' : 'nav-item-inactive']"
                @click="switchTab('all')"
              >
                <template #prepend>
                  <v-avatar
                    :color="activeTab === 'all' ? 'primary' : 'grey-lighten-2'"
                    size="32"
                    class="mr-3"
                  >
                    <v-icon
                      icon="mdi-view-dashboard"
                      :color="activeTab === 'all' ? 'white' : 'grey-darken-2'"
                      size="16"
                    ></v-icon>
                  </v-avatar>
                </template>

                <v-list-item-title
                  class="font-weight-medium"
                  :class="activeTab === 'all' ? 'text-primary' : 'text-grey-darken-3'"
                >
                  全部课程
                </v-list-item-title>

                <v-list-item-subtitle class="text-caption">
                  {{ totalDueCards }} 张待复习
                </v-list-item-subtitle>

                <template #append>
                  <v-chip size="small" color="primary" variant="flat">
                    {{ totalDueCards }}
                  </v-chip>
                </template>
              </v-list-item>

              <!-- 分课程标签 -->
              <v-list-item
                v-for="bank in courseMemoryBanks"
                :key="bank.course.id"
                :value="bank.course.id.toString()"
                class="nav-item ma-1 rounded-lg"
                :class="[activeTab === bank.course.id.toString() ? 'nav-item-active' : 'nav-item-inactive']"
                @click="switchTab(bank.course.id.toString())"
              >
                <template #prepend>
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
                </template>

                <v-list-item-title
                  class="font-weight-medium"
                  :class="activeTab === bank.course.id.toString() ? 'text-primary' : 'text-grey-darken-3'"
                >
                  {{ bank.course.name }}
                </v-list-item-title>

                <v-list-item-subtitle class="text-caption">
                  共{{ bank.cardCount }}张 · {{ getFrequencyText(bank.setting.frequencySetting) }}
                </v-list-item-subtitle>

                <template #append>
                  <v-chip
                    size="small"
                    :color="bank.dueCardCount > 0 ? 'error' : 'success'"
                    variant="flat"
                  >
                    {{ bank.dueCardCount }}
                  </v-chip>
                </template>
              </v-list-item>
            </v-list>

            <!-- 统计信息 -->
            <!-- 已注释：学习统计功能
            <div class="mt-6 pa-3 rounded-lg bg-white">
              <h4 class="text-body-1 font-weight-bold text-grey-darken-4 mb-2">📊 学习统计</h4>
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="text-grey-darken-2">总复习次数</span>
                <span class="font-weight-bold text-primary">{{ stats.totalReviews }}</span>
              </div>
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="text-grey-darken-2">连续天数</span>
                <span class="font-weight-bold text-success">{{ stats.streakDays }}天</span>
              </div>
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="text-grey-darken-2">平均正确率</span>
                <span class="font-weight-bold text-warning">{{ stats.averageScore }}%</span>
              </div>
              <div class="d-flex justify-space-between text-body-2">
                <span class="text-grey-darken-2">总学习时长</span>
                <span class="font-weight-bold text-info">{{ stats.timeSpent }}分钟</span>
              </div>
            </div>
            -->
          </v-card-text>
        </v-card>
      </v-col>

      <!-- 右侧内容区域 -->
      <v-col cols="9">
        <!-- 复习模式 -->
        <div v-if="viewMode === 'review'">
          <!-- 加载状态 -->
          <div v-if="loading" class="text-center py-8">
            <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
            <p class="text-body-1 text-grey-darken-2 mt-4">正在加载复习队列...</p>
          </div>

          <!-- 空队列状态 -->
          <div v-else-if="!isReviewing && currentCards.length === 0" class="text-center py-8">
            <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
            <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">太棒了！</h3>
            <p class="text-body-1 text-grey-darken-1 mb-4">
              {{ selectedCourse ? `${selectedCourse.course.name}课程` : '全部课程' }}暂时没有需要复习的卡片
            </p>
          </div>

          <!-- 开始复习状态 -->
          <div v-else-if="!isReviewing" class="text-center py-8">
            <v-card flat color="grey-lighten-5" rounded="xl" class="pa-8 mx-auto" style="max-width: 500px;">
              <v-icon icon="mdi-cards" size="64" color="primary" class="mb-4"></v-icon>
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">准备开始复习</h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{ selectedCourse ? selectedCourse.course.name : '全部课程' }}有
                <span class="font-weight-bold text-primary">{{ selectedCourse ? selectedCourse.dueCardCount : totalDueCards }}</span> 张卡片等待复习
              </p>
              <v-btn color="primary" variant="flat" rounded="lg" size="large" @click="startReview">
                <v-icon icon="mdi-play" class="mr-2"></v-icon>
                开始复习
              </v-btn>
            </v-card>
          </div>

          <!-- 复习中状态 -->
          <div v-else-if="currentCard" class="review-area">
            <!-- 进度条 -->
            <v-card flat color="grey-lighten-5" rounded="xl" class="mb-4 pa-4">
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-body-2 text-grey-darken-1">
                  第 {{ currentCardIndex + 1 }} / {{ currentCards.length }} 张
                </span>
                <span class="text-body-2 text-grey-darken-1">{{ reviewProgress }}%</span>
              </div>
              <v-progress-linear :model-value="reviewProgress" color="primary" height="8" rounded></v-progress-linear>
            </v-card>

            <!-- 卡片区域 -->
            <v-card flat color="grey-lighten-5" rounded="xl" class="mb-4">
              <div class="card-container pa-8" style="min-height: 400px;">
                <!-- 问题面 -->
                <div v-if="!showAnswer" class="text-center">
                  <div class="d-flex align-center justify-center mb-4">
                    <v-icon icon="mdi-help-circle" color="primary" size="48"></v-icon>
                  </div>
                  <h3 class="text-h5 font-weight-bold text-primary mb-4">问题</h3>
                  
                  <!-- 上下文信息 -->
                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="primary" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.title }}
                    </v-chip>
                  </div>
                  
                  <div class="question-content pa-6 mx-auto" style="max-width: 600px;">
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
                  
                  <!-- 上下文信息 -->
                  <div v-if="currentCard.deck" class="mb-4">
                    <v-chip size="small" color="success" variant="outlined">
                      <v-icon icon="mdi-book-open-page-variant" size="16" class="mr-1"></v-icon>
                      {{ currentCard.deck.title }}
                    </v-chip>
                  </div>
                  
                  <div class="answer-content pa-6 mx-auto" style="max-width: 600px;">
                    <p class="text-h6 text-grey-darken-3">{{ currentCard.back }}</p>
                  </div>
                  
                  <!-- 评价按钮 -->
                  <div class="mt-8">
                    <p class="text-body-1 text-grey-darken-1 mb-4">请根据你的掌握程度选择：</p>
                    <div class="d-flex justify-center" style="gap: 12px;">
                      <v-btn
                        color="error"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.AGAIN)"
                      >
                        <v-icon icon="mdi-close" class="mr-2"></v-icon>
                        重来
                      </v-btn>
                      <v-btn
                        color="warning"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.HARD)"
                      >
                        <v-icon icon="mdi-help" class="mr-2"></v-icon>
                        困难
                      </v-btn>
                      <v-btn
                        color="success"
                        variant="outlined"
                        rounded="lg"
                        @click="submitReview(ReviewResult.GOOD)"
                      >
                        <v-icon icon="mdi-check" class="mr-2"></v-icon>
                        良好
                      </v-btn>
                      <v-btn
                        color="primary"
                        variant="flat"
                        rounded="lg"
                        @click="submitReview(ReviewResult.EASY)"
                      >
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
          <v-card flat color="grey-lighten-5" rounded="xl">
            <v-card-text class="pa-6">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold text-grey-darken-4">
                  卡片列表 ({{ currentCards.length }} 张)
                </h3>
                
                <!-- 批量操作按钮 -->
                <div class="d-flex align-center" style="gap: 8px;">
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
                    v-if="selectedCards.length > 0 && selectedCards.length < currentCards.length"
                    color="grey-darken-2"
                    variant="tonal"
                    rounded="lg"
                    size="small"
                    @click="selectedCards = []"
                  >
                    <v-icon icon="mdi-close" size="16" class="mr-2"></v-icon>
                    取消选中
                  </v-btn>

                  <v-btn
                    :color="selectedCards.length === currentCards.length && currentCards.length > 0 ? 'primary' : 'grey-darken-2'"
                    :variant="selectedCards.length === currentCards.length && currentCards.length > 0 ? 'flat' : 'tonal'"
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
                <p class="text-body-1 text-grey-darken-1">该课程暂时没有需要复习的卡片</p>
              </div>

              <div v-else>
                <div
                  v-for="(card, index) in currentCards"
                  :key="card.id"
                  class="ranking-item ma-1 pa-4 rounded-lg mb-2"
                  :class="[
                    'regular-item',
                    selectedCards.includes(card.id) ? 'card-selected' : ''
                  ]"
                  @click="selectedCards.includes(card.id) ? selectedCards = selectedCards.filter(id => id !== card.id) : selectedCards.push(card.id)"
                >
                  <div class="d-flex align-center">
                    <!-- 选择框 -->
                    <div class="mr-3">
                      <v-checkbox
                        :model-value="selectedCards.includes(card.id)"
                        @update:model-value="(value) => {
                          if (value) {
                            if (!selectedCards.includes(card.id)) {
                              selectedCards.push(card.id)
                            }
                          } else {
                            selectedCards = selectedCards.filter(id => id !== card.id)
                          }
                        }"
                        density="compact"
                        hide-details
                        @click.stop
                      ></v-checkbox>
                    </div>
                    
                    <!-- 排名号码 -->
                    <div class="rank-number mr-4 text-center rank-number-container">
                      <v-avatar v-if="index < 3" :color="getRankColor(index)" size="32">
                        <v-icon
                          v-if="getRankIcon(index)"
                          :icon="getRankIcon(index)"
                          color="white"
                          size="16"
                        ></v-icon>
                      </v-avatar>
                      <div
                        v-else
                        class="text-h6 font-weight-bold"
                        :class="index < 10 ? 'text-grey-darken-2' : 'text-grey-lighten-1'"
                      >
                        {{ index + 1 }}
                      </div>
                    </div>

                    <!-- 卡片内容 -->
                    <div class="flex-grow-1">
                      <div class="d-flex align-center mb-1">
                        <div class="text-h6 font-weight-medium">
                          {{ card.front }}
                        </div>
                        <span v-if="card.deck" class="text-body-2 text-grey ml-2">
                          - {{ card.deck.title }}
                        </span>
                      </div>
                      <div class="text-body-2 text-grey-darken-2">
                        {{ card.back }}
                      </div>
                    </div>

                    <!-- 状态和操作 -->
                    <div class="d-flex flex-column align-end" style="gap: 8px; min-width: 180px;">
                      <!-- 第一行：状态标签和deck详情按钮 -->
                      <div class="d-flex align-center" style="gap: 8px;">
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

                        <!-- deck详情按钮 -->
                        <v-btn
                          v-if="card.deck?.id"
                          icon
                          size="small"
                          variant="text"
                          color="grey-darken-2"
                          class="deck-detail-btn"
                          @click.stop="viewDeckDetails(card.deck?.id)"
                        >
                          <v-tooltip activator="parent" location="top">
                            {{ card.hasDeckUpdate ? '所在记忆卡片组有更新，点击查看' : '查看卡片组详情' }}
                          </v-tooltip>
                          <div class="position-relative">
                            <v-icon icon="mdi-folder-open" size="18"></v-icon>
                            <!-- 更新小红点 -->
                            <div
                              v-if="card.hasDeckUpdate"
                              class="update-dot"
                            ></div>
                          </div>
                        </v-btn>
                      </div>

                      <!-- 第二行：更多操作菜单 -->
                      <div class="d-flex align-center justify-end" style="gap: 8px;">
                        <v-menu>
                          <template v-slot:activator="{ props }">
                            <v-btn
                              v-bind="props"
                              icon="mdi-dots-vertical"
                              variant="text"
                              size="small"
                              @click.stop
                            ></v-btn>
                          </template>
                          <v-list density="compact">
                            <v-list-item @click="reviewSelectedCards(); selectedCards = [card.id]">
                              <v-list-item-title>
                                <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
                                立即复习
                              </v-list-item-title>
                            </v-list-item>
                            <v-list-item @click="resetSelectedCards(); selectedCards = [card.id]">
                              <v-list-item-title>
                                <v-icon icon="mdi-restart" size="16" class="mr-2"></v-icon>
                                重新学习
                              </v-list-item-title>
                            </v-list-item>
                            <v-list-item @click="deleteSelectedCards(); selectedCards = [card.id]">
                              <v-list-item-title class="text-error">
                                <v-icon icon="mdi-delete" size="16" class="mr-2"></v-icon>
                                删除卡片
                              </v-list-item-title>
                            </v-list-item>
                          </v-list>
                        </v-menu>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 管理模式 -->
        <div v-else-if="viewMode === 'manage'">
          <div v-if="!selectedCourse" class="text-center pa-8">
            <v-icon icon="mdi-cog" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
            <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">选择课程</h3>
            <p class="text-body-1 text-grey-darken-1">请先选择一个课程进行管理设置</p>
          </div>

          <v-card v-else flat color="grey-lighten-5" rounded="xl" class="pa-6">
            <h3 class="text-h6 font-weight-bold text-grey-darken-3 mb-4">
              {{ selectedCourse.course.name }} - 复习设置
            </h3>

            <v-row>
              <v-col cols="6">
                <v-select
                  :model-value="selectedCourse.setting.frequencySetting"
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

              <v-col cols="6">
                <v-select
                  :model-value="selectedCourse.setting.status"
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
            </v-row>

            <div class="mt-4">
              <v-btn color="primary" variant="flat" rounded="lg" class="mr-3">
                保存设置
              </v-btn>
              <v-btn color="error" variant="outlined" rounded="lg">
                移除课程
              </v-btn>
            </div>
          </v-card>
        </div>
      </v-col>
    </v-row>
    
    <!-- 卡片组更新对比对话框 -->
    <DeckUpdateDiffDialog
      v-model="showUpdateDiffDialog"
      :deck-diff="currentDeckDiff"
      @accept-update="applyDeckUpdate"
    />

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetailDialog"
      :deck="selectedDeck"
      @show-diff-dialog="handleShowDiffDialog"
    />

    <!-- 卡片内容差异对话框 -->
    <CardContentDiffDialog
      v-model="showCardDiffDialog"
      :card-diff="currentCardDiff"
      @accept-update="applyCardUpdate"
    />
  </v-container>
</template>

<style scoped>
.sticky-nav {
  position: sticky;
  top: 20px;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.2) !important;
}

.nav-item-active {
  background: rgba(25, 118, 210, 0.08) !important;
  border-color: rgba(25, 118, 210, 0.2) !important;
}

.nav-item-inactive {
  background: white !important;
}

.review-area {
  max-width: 800px;
  margin: 0 auto;
}

.question-content, .answer-content {
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #1976d2;
}

.answer-content {
  border-left-color: #4caf50;
}

.ranking-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.ranking-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.3) !important;
}

.top-three-item {
  background: rgba(25, 118, 210, 0.02) !important;
  border-color: rgba(25, 118, 210, 0.08) !important;
}

.regular-item {
  background: white !important;
}

.rank-number-container {
  min-width: 40px;
}

/* 改善字体渲染 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 文字对比度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 为v-card添加细节 */
.v-card {
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* 卡片选中状态 */
.card-selected {
  border-color: rgb(var(--v-theme-primary)) !important;
  background: rgba(var(--v-theme-primary), 0.05) !important;
}

/* 鼠标指针样式 */
.cursor-pointer {
  cursor: pointer;
}

/* deck详情按钮样式 */
.deck-detail-btn {
  transition: all 0.2s ease;
}

.deck-detail-btn:hover {
  background: rgba(var(--v-theme-primary), 0.1) !important;
}

/* 更新小红点 */
.update-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #f44336;
  border-radius: 50%;
  border: 1px solid white;
  z-index: 1;
}
</style>