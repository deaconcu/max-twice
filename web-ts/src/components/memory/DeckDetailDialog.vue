<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import type { MemoryCardDeck, DeckDetail, MemoryCardView, DeckUpdateDiff } from '@/types/memoryCard'
import { MemoryService } from '@/services/memoryService'
import DeckUpdateDiffDialog from '@/components/memory/DeckUpdateDiffDialog.vue'
import { cardFrontRules, cardBackRules } from '@/utils/validationRules'
import { CARD_VALIDATION } from '@/types/validation'

interface Props {
  modelValue: boolean
  deck: MemoryCardDeck | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'addToStudy', deck: MemoryCardDeck): void
  (e: 'showDiffDialog', deckDiff: DeckUpdateDiff): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()
const userStore = useUserStore()

// 获取当前用户ID
const currentUserId = computed(() => userStore.currentUser?.id)

const dialog = ref(false)
const loading = ref(false)
const deckDetail = ref<DeckDetail | null>(null)
const selectedCard = ref<MemoryCardView | null>(null)
const showCardDetail = ref(false)
const isFlipped = ref(false) // 翻转状态

// Tab相关状态
const currentTab = ref('all') // 'all' | 'study' | 'diff'
const studyCards = ref<MemoryCardView[]>([]) // 用户学习的卡片
const diffTab = ref('all') // diff页面内的子tab: 'all' | 'modified' | 'added' | 'nodeOnly'

// 检查是否为当前用户创建的卡片组
const isOwnDeck = computed(() => {
  return props.deck?.creator?.id === currentUserId.value
})

// 前端计算差异 - 新增的卡片（deck中有但用户没学习的）
const addedDiffs = computed(() => {
  if (!deckDetail.value?.cards || !studyCards.value) return []

  const studiedCardIds = new Set(studyCards.value.map(card => card.id))
  return deckDetail.value.cards
    .filter(card => !studiedCardIds.has(card.id))
    .map(card => ({
      cardId: card.id,
      type: 'added',
      newVersion: {
        front: card.front,
        back: card.back
      }
    }))
})

// 前端计算差异 - 修改的卡片（版本不同的）
const modifiedDiffs = computed(() => {
  if (!deckDetail.value?.cards || !studyCards.value) return []

  const studiedCardsMap = new Map(studyCards.value.map(card => [card.id, card]))

  return deckDetail.value.cards
    .filter(deckCard => {
      const studiedCard = studiedCardsMap.get(deckCard.id)
      // 存在学习记录且内容不同
      return studiedCard && (
        studiedCard.front !== deckCard.front ||
        studiedCard.back !== deckCard.back
      )
    })
    .map(deckCard => {
      const studiedCard = studiedCardsMap.get(deckCard.id)!
      return {
        cardId: deckCard.id,
        type: 'modified',
        newVersion: {
          front: deckCard.front,
          back: deckCard.back
        },
        oldVersion: {
          front: studiedCard.front,
          back: studiedCard.back
        }
      }
    })
})

// 计算学习记录独有的卡片数量
const nodeOnlyCardsCount = computed(() => {
  // 从studyCards中过滤出不在当前deck中的卡片
  if (!studyCards.value || !deckDetail.value?.cards) return 0
  
  const deckCardIds = new Set(deckDetail.value.cards.map(card => card.id))
  return studyCards.value.filter(card => !deckCardIds.has(card.id)).length
})

// 计算学习记录独有的卡片
const nodeOnlyCards = computed(() => {
  if (!studyCards.value || !deckDetail.value?.cards) return []

  const deckCardIds = new Set(deckDetail.value.cards.map(card => card.id))
  return studyCards.value.filter(card => !deckCardIds.has(card.id))
})

// 自动选择第一个有数据的tab
const getFirstAvailableTab = (): string => {
  if (modifiedDiffs.value.length > 0) return 'modified'
  if (addedDiffs.value.length > 0) return 'added'
  if (nodeOnlyCardsCount.value > 0) return 'nodeOnly'
  return 'modified' // 默认返回第一个tab
}

// 监听数据变化，自动切换到第一个有数据的tab
watch([modifiedDiffs, addedDiffs, nodeOnlyCards], () => {
  // 只在当前tab没有数据时才自动切换
  const currentTabHasData =
    (diffTab.value === 'modified' && modifiedDiffs.value.length > 0) ||
    (diffTab.value === 'added' && addedDiffs.value.length > 0) ||
    (diffTab.value === 'nodeOnly' && nodeOnlyCardsCount.value > 0)

  if (!currentTabHasData) {
    diffTab.value = getFirstAvailableTab()
  }
}, { immediate: true })

watch(() => props.modelValue, (newVal) => {
  dialog.value = newVal
  if (newVal && props.deck) {
    loadDeckDetail()
  }
})

watch(dialog, (newVal) => {
  emit('update:modelValue', newVal)
})

const loadDeckDetail = async () => {
  if (!props.deck) return
  
  loading.value = true
  try {
    const response = await MemoryService.getDeckDetail(props.deck.id)
    if (response.code === 200) {
      deckDetail.value = response.data
      
      // 获取deck所属的nodeId
      const nodeId = deckDetail.value.nodeId 
      if (nodeId && currentUserId.value) {
        // 获取用户在这个node下学习的所有卡片
        const nodeCardsResponse = await MemoryService.getUserCardsByNode(nodeId)
        if (nodeCardsResponse.code === 200) {
          studyCards.value = nodeCardsResponse.data
        }
      }
      
      console.log('Deck detail loaded:', {
        totalCards: response.data.cards.length,
        studyCards: studyCards.value.length,
        cardsWithUpdates: response.data.cards.filter(card => card.hasUpdate).length
      })
    }
  } catch (error) {
    console.error('Failed to load deck detail:', error)
  } finally {
    loading.value = false
  }
}

// 当切换到diff tab时，不需要额外加载数据
const onTabChange = (tab: string) => {
  currentTab.value = tab
  // diff数据完全基于前端计算，无需额外加载
}

const viewCard = (card: MemoryCardView) => {
  selectedCard.value = card
  isFlipped.value = false // 重置翻转状态，总是从正面开始
  showCardDetail.value = true
}

const flipCard = () => {
  isFlipped.value = !isFlipped.value
}

const addToStudy = async () => {
  if (!props.deck) return
  
  // TODO: 调用API添加整个卡片组到学习计划
  emit('addToStudy', props.deck)
  dialog.value = false
}

const addCardToStudy = async (card: MemoryCardView) => {
  try {
    const response = await MemoryService.addCardToStudy(card.id)
    if (response.code === 200) {
      // 更新卡片的用户状态
      card.srsState = response.data
      // TODO: 显示成功消息
    }
  } catch (error) {
    console.error('Failed to add card to study:', error)
  }
}

// 卡片管理功能
const editingCard = ref<MemoryCardView | null>(null)
const showEditDialog = ref(false)
const editCardFront = ref('')
const editCardBack = ref('')
const editCardFormValid = ref(true)

// 新建卡片
const createNewCard = () => {
  if (!props.deck) return
  editingCard.value = null
  editCardFront.value = ''
  editCardBack.value = ''
  showEditDialog.value = true
}

// 编辑卡片
const editCard = (card: MemoryCardView) => {
  editingCard.value = card
  editCardFront.value = card.front
  editCardBack.value = card.back
  showEditDialog.value = true
}

// 删除卡片
const deleteCard = async (card: MemoryCardView) => {
  if (!confirm('确定要删除这张卡片吗？')) return
  
  try {
    const response = await MemoryService.deleteCard(card.id)
    if (response.code === 200) {
      // 从本地数据中移除
      if (deckDetail.value) {
        const index = deckDetail.value.cards.findIndex(c => c.id === card.id)
        if (index > -1) {
          deckDetail.value.cards.splice(index, 1)
          deckDetail.value.cardCount--
        }
      }
    }
  } catch (error) {
    console.error('Failed to delete card:', error)
  }
}

// 保存卡片
const saveCard = async () => {
  if (!props.deck || !editCardFront.value.trim() || !editCardBack.value.trim()) return
  
  try {
    if (editingCard.value) {
      // 更新现有卡片
      const response = await MemoryService.updateCard(editingCard.value.id, {
        id: editingCard.value.id,
        front: editCardFront.value,
        back: editCardBack.value
      })
      
      if (response.code === 200) {
        editingCard.value.front = editCardFront.value
        editingCard.value.back = editCardBack.value
      }
    } else {
      // 创建新卡片
      const response = await MemoryService.createCard({
        deckId: props.deck.id,
        front: editCardFront.value,
        back: editCardBack.value
      })
      
      if (response.code === 200 && deckDetail.value) {
        deckDetail.value.cards.push(response.data)
        deckDetail.value.cardCount++
      }
    }
    
    showEditDialog.value = false
  } catch (error) {
    console.error('Failed to save card:', error)
  }
}

// 滚动到指定区域
const scrollToSection = (sectionType: 'modified' | 'added' | 'nodeOnly') => {
  const elementId = `${sectionType}-section`
  const element = document.getElementById(elementId)
  if (element) {
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }
}

const closeDialog = () => {
  dialog.value = false
}

// 跳转到更新差异标签页
const goToDiffTab = () => {
  currentTab.value = 'diff'
  // 如果还没有加载diff数据，则加载
  if (!deckDiff.value && props.deck) {
    loadDeckDiff()
  }
}

// Diff相关方法
const getDiffTypeText = (type: string): string => {
  switch (type) {
    case 'added': return '新增'
    case 'modified': return '修改'
    case 'deleted': return '删除'
    default: return '未知'
  }
}

const getDiffTypeColor = (type: string): string => {
  switch (type) {
    case 'added': return 'success'
    case 'modified': return 'warning'
    case 'deleted': return 'error'
    default: return 'grey'
  }
}

// 显示详细差异对话框
const showDiffDialog = () => {
  // 触发显示DeckUpdateDiffDialog
  emit('showDiffDialog', deckDiff.value)
}

// 接受单个卡片更新
const acceptUpdate = async (cardId: number) => {
  try {
    await MemoryService.acceptDeckChanges(props.deck!.id, [cardId])
    // 重新加载数据
    loadDeckDetail()
    loadDeckDiff()
  } catch (error) {
    console.error('Failed to accept update:', error)
  }
}

// 添加单个卡片到学习（diff界面用）
const addCardToStudyFromDiff = async (cardId: number) => {
  try {
    await MemoryService.acceptDeckChanges(props.deck!.id, [cardId])
    // 重新加载数据
    loadDeckDetail()
    loadDeckDiff()
  } catch (error) {
    console.error('Failed to add card to study:', error)
  }
}

// 移除学习记录
const removeFromStudy = async (cardId: number) => {
  try {
    // TODO: 实现移除学习记录的API
    console.log('Remove card from study:', cardId)
    // 重新加载数据
    loadDeckDetail()
  } catch (error) {
    console.error('Failed to remove from study:', error)
  }
}

// 接受所有更新
const acceptAllChanges = async () => {
  try {
    await MemoryService.acceptDeckChanges(props.deck!.id, [])
    // 重新加载数据
    loadDeckDetail()
    loadDeckDiff()
  } catch (error) {
    console.error('Failed to accept all changes:', error)
  }
}

// 添加所有新卡片
const addAllNewCards = async () => {
  try {
    const addedCardIds = addedDiffs.value.map(diff => diff.cardId)
    await MemoryService.acceptDeckChanges(props.deck!.id, addedCardIds)
    // 重新加载数据
    loadDeckDetail()
    loadDeckDiff()
  } catch (error) {
    console.error('Failed to add all new cards:', error)
  }
}

// 处理点赞
const handleUpvote = async () => {
  if (!props.deck) return

  try {
    const response = await MemoryService.upvoteDeck(props.deck.id)
    if (response.code === 200) {
      // 更新本地状态
      if (props.deck) {
        props.deck.hasUpvoted = response.data.upvoted
        props.deck.upvoteCount = response.data.upvotes
      }
    }
  } catch (error) {
    console.error('Failed to upvote deck:', error)
  }
}
</script>

<template>
  <v-dialog v-model="dialog" width="900" persistent>
    <v-card rounded="xl" elevation="0" class="overflow-hidden" style="max-height: 80vh; display: flex; flex-direction: column;">
      <!-- 头部背景 -->
      <div class="header-bg pa-4 pb-6 position-relative">
        <v-btn 
          icon="mdi-close" 
          size="small"
          variant="text" 
          color="white"
          class="position-absolute close-btn"
          @click="closeDialog"
        ></v-btn>
        
        <div class="text-white">
          <h2 class="text-h5 font-weight-bold mb-2">
            {{ deck?.title || '卡片组详情' }}
          </h2>
          
          <p v-if="deck?.description" class="text-body-1 opacity-90 mb-3 font-weight-light">
            {{ deck.description }}
          </p>
          
          <!-- 作者和统计信息 -->
          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <v-avatar size="36" class="mr-3">
                <v-img v-if="deck?.creator?.avatar" :src="deck.creator.avatar" />
                <v-icon v-else icon="mdi-account-circle" color="white"></v-icon>
              </v-avatar>
              <div>
                <div class="text-subtitle-2 font-weight-medium">{{ deck?.creator?.name || '匿名用户' }}</div>
                <div class="text-caption opacity-80">创建者</div>
              </div>
            </div>
            
            <div class="d-flex align-center text-white">
              <div class="text-center mr-4">
                <div class="text-subtitle-1 font-weight-bold">{{ deckDetail?.stats?.totalCards || deckDetail?.cardCount || 0 }}</div>
                <div class="text-caption opacity-80">卡片</div>
              </div>
              <div
                class="text-center upvote-area"
                :class="{ 'upvoted': deck?.hasUpvoted }"
                @click="handleUpvote"
              >
                <div class="text-subtitle-1 font-weight-bold">{{ deck?.upvoteCount || 0 }}</div>
                <div class="text-caption opacity-80">点赞</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 卡片列表 - 可滚动区域 -->
      <div class="flex-grow-1" style="overflow-y: auto;">
        <div class="px-6 pt-6 pb-6">
          <!-- Tab 导航 -->
          <div class="mb-6">
            <v-tabs
              v-model="currentTab"
              color="primary"
              align-tabs="center"
              @update:model-value="onTabChange"
            >
              <v-tab value="all">
                <v-icon icon="mdi-cards-variant" class="mr-2"></v-icon>
                所有卡片 ({{ deckDetail?.cards.length || 0 }})
              </v-tab>
              <v-tab v-if="studyCards.length > 0" value="study">
                <v-icon icon="mdi-school" class="mr-2"></v-icon>
                我的学习卡片 ({{ studyCards.length }})
              </v-tab>
              <v-tab v-if="studyCards.length > 0" value="diff">
                <v-icon icon="mdi-compare" class="mr-2"></v-icon>
                更新差异
              </v-tab>
            </v-tabs>
          </div>

          <!-- Tab 内容 -->
          <v-window v-model="currentTab">
            <!-- 所有卡片 Tab -->
            <v-window-item value="all">
              <div class="d-flex align-center justify-space-between mb-4">
                <h3 class="text-h6 font-weight-bold text-grey-darken-3">
                  <v-icon icon="mdi-cards-variant" class="mr-2" color="primary"></v-icon>
                  卡片内容
                </h3>
                
                <!-- 如果是当前用户的卡片组，显示新建卡片按钮 -->
                <v-btn
                  v-if="isOwnDeck"
                  color="primary"
                  variant="tonal"
                  size="small"
                  rounded="lg"
                  prepend-icon="mdi-plus"
                  @click="createNewCard"
                >
                  新建卡片
                </v-btn>
              </div>
              
              <div v-if="loading" class="text-center pa-8">
                <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
                <p class="text-body-1 text-grey-darken-1 mt-4">加载中...</p>
              </div>

              <div v-else-if="deckDetail" class="cards-container">
                <v-card
                  v-for="(card, index) in deckDetail.cards"
                  :key="card.id"
                  class="mb-4 card-item"
                  rounded="12"
                  elevation="0"
                  variant="outlined"
                >
                  <v-card-text class="pa-5">
                    <div class="d-flex align-start justify-space-between">
                      <div class="flex-grow-1 mr-4">
                        <!-- 卡片标题和状态 -->
                        <div class="d-flex align-center justify-space-between mb-3">
                          <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                            第 {{ index + 1 }} 张卡片
                          </h4>
                          <div class="d-flex align-center" style="gap: 8px;">
                            <v-chip
                              v-if="card.srsState"
                              size="small"
                              :color="card.srsState.repetitions >= 3 ? 'success' : 'warning'"
                              variant="flat"
                              prepend-icon="mdi-trophy"
                            >
                              {{ card.srsState.repetitions >= 3 ? '已掌握' : `学习${card.srsState.repetitions}次` }}
                            </v-chip>
                            <v-chip v-else size="small" color="grey-lighten-1" variant="flat" prepend-icon="mdi-circle-outline">
                              未开始
                            </v-chip>
                            <v-chip
                              v-if="card.hasUpdate"
                              size="small"
                              color="warning"
                              variant="flat"
                              prepend-icon="mdi-update"
                            >
                              有更新
                            </v-chip>
                          </div>
                        </div>
                        
                        <!-- 问题 -->
                        <div class="question-section mb-4">
                          <div class="d-flex align-center mb-2">
                            <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                          </div>
                          <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.front }}</p>
                          </div>
                        </div>
                        
                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center mb-2">
                            <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                          </div>
                          <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.back }}</p>
                          </div>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                        <v-btn
                          color="primary"
                          variant="tonal"
                          size="small"
                          rounded="xl"
                          prepend-icon="mdi-eye"
                          class="action-btn"
                          @click="viewCard(card)"
                        >
                          预览
                        </v-btn>
                        
                        <!-- 如果是当前用户的卡片组，显示编辑和删除按钮 -->
                        <template v-if="isOwnDeck">
                          <v-btn
                            color="warning"
                            variant="tonal"
                            size="small"
                            rounded="xl"
                            prepend-icon="mdi-pencil"
                            class="action-btn"
                            @click="editCard(card)"
                          >
                            编辑
                          </v-btn>
                          
                          <v-btn
                            color="error"
                            variant="tonal"
                            size="small"
                            rounded="xl"
                            prepend-icon="mdi-delete"
                            class="action-btn"
                            @click="deleteCard(card)"
                          >
                            删除
                          </v-btn>
                        </template>
                        
                        <!-- 如果不是当前用户的卡片组，显示学习按钮 -->
                        <template v-else>
                          <v-btn
                            v-if="!card.srsState"
                            color="success"
                            variant="tonal"
                            size="small"
                            rounded="xl"
                            prepend-icon="mdi-plus"
                            class="action-btn"
                            @click="addCardToStudy(card)"
                          >
                            学习
                          </v-btn>
                          
                          <v-btn
                            v-else
                            color="success"
                            variant="tonal"
                            size="small"
                            rounded="xl"
                            prepend-icon="mdi-check-circle"
                            class="action-btn"
                            disabled
                          >
                            已添加
                          </v-btn>
                        </template>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>

              <div v-else class="text-center pa-8">
                <v-icon icon="mdi-alert-circle-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">加载失败</h4>
                <p class="text-body-2 text-grey-darken-1">请检查网络连接后重试</p>
              </div>
            </v-window-item>

            <!-- 我的学习卡片 Tab -->
            <v-window-item value="study">
              <div v-if="loading" class="text-center pa-8">
                <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
                <p class="text-body-1 text-grey-darken-1 mt-4">加载中...</p>
              </div>

              <div v-else-if="studyCards.length === 0" class="text-center pa-8">
                <v-icon icon="mdi-school-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                <h4 class="text-h6 text-grey-darken-1 mb-2">暂无学习卡片</h4>
                <p class="text-body-2 text-grey-darken-1">您还没有开始学习此卡片组中的任何卡片</p>
              </div>

              <div v-else class="cards-container">
                <v-card
                  v-for="(card, index) in studyCards"
                  :key="card.id"
                  class="mb-4 card-item"
                  rounded="12"
                  elevation="0"
                  variant="outlined"
                >
                  <v-card-text class="pa-5">
                    <div class="d-flex align-start justify-space-between">
                      <div class="flex-grow-1 mr-4">
                        <!-- 卡片标题和状态 -->
                        <div class="d-flex align-center justify-space-between mb-3">
                          <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                            学习卡片 {{ index + 1 }}
                          </h4>
                          <div class="d-flex align-center" style="gap: 8px;">
                            <v-chip
                              v-if="card.srsState"
                              size="small"
                              :color="card.srsState.repetitions >= 3 ? 'success' : 'warning'"
                              variant="flat"
                              prepend-icon="mdi-trophy"
                            >
                              {{ card.srsState.repetitions >= 3 ? '已掌握' : `学习${card.srsState.repetitions}次` }}
                            </v-chip>
                            <v-chip
                              v-if="card.hasUpdate"
                              size="small"
                              color="warning"
                              variant="flat"
                              prepend-icon="mdi-update"
                            >
                              有更新
                            </v-chip>
                          </div>
                        </div>
                        
                        <!-- 问题 -->
                        <div class="question-section mb-4">
                          <div class="d-flex align-center mb-2">
                            <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                          </div>
                          <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.front }}</p>
                          </div>
                        </div>
                        
                        <!-- 答案 -->
                        <div class="answer-section">
                          <div class="d-flex align-center mb-2">
                            <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                            <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                          </div>
                          <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                            <p class="text-body-1 mb-0">{{ card.back }}</p>
                          </div>
                        </div>
                        
                        <!-- 学习进度信息 -->
                        <div v-if="card.srsState" class="mt-4">
                          <div class="d-flex align-center justify-space-between text-body-2 text-grey-darken-2">
                            <span>复习次数：{{ card.srsState.repetitions }}次</span>
                            <span>难度系数：{{ card.srsState.easeFactor }}</span>
                            <span>下次复习：{{ new Date(card.srsState.reviewDueAt).toLocaleDateString() }}</span>
                          </div>
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                        <v-btn
                          color="primary"
                          variant="tonal"
                          size="small"
                          rounded="xl"
                          prepend-icon="mdi-eye"
                          class="action-btn"
                          @click="viewCard(card)"
                        >
                          预览
                        </v-btn>
                        
                        <v-btn
                          color="success"
                          variant="tonal"
                          size="small"
                          rounded="xl"
                          prepend-icon="mdi-play"
                          class="action-btn"
                        >
                          复习
                        </v-btn>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
              </div>
            </v-window-item>

            <!-- 更新差异 Tab -->
            <v-window-item value="diff">
              <div v-if="addedDiffs.length === 0 && modifiedDiffs.length === 0 && nodeOnlyCardsCount === 0" class="text-center pa-8">
                <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
                <h4 class="text-h6 text-success mb-2">没有差异</h4>
                <p class="text-body-2 text-grey-darken-1">卡片组与您的学习记录完全同步</p>
              </div>

              <div v-else>
                <!-- 更新摘要 - 作为Tab导航 -->
                <div class="mb-4">
                  <v-card flat color="blue-lighten-5" rounded="lg" class="py-0 px-4">
                    <div class="d-flex align-center justify-space-between">
                      <div class="d-flex align-center">
                        <v-icon color="blue-darken-2" size="20" class="mr-2">mdi-compare</v-icon>
                        <h4 class="text-body-1 text-blue-darken-2 mb-0">
                          差异对比 - 检测到 {{ addedDiffs.length + modifiedDiffs.length + nodeOnlyCardsCount }} 项差异
                        </h4>
                      </div>
                      <div class="d-flex align-center justify-end">
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'modified' }"
                          @click="diffTab = 'modified'"
                        >
                          <div class="diff-tab-number text-warning">{{ modifiedDiffs.length }}</div>
                          <div class="diff-tab-label">内容有更新</div>
                        </div>
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'added' }"
                          @click="diffTab = 'added'"
                        >
                          <div class="diff-tab-number text-success">{{ addedDiffs.length }}</div>
                          <div class="diff-tab-label">可添加</div>
                        </div>
                        <div
                          class="diff-tab-item"
                          :class="{ 'diff-tab-active': diffTab === 'nodeOnly' }"
                          @click="diffTab = 'nodeOnly'"
                        >
                          <div class="diff-tab-number text-info">{{ nodeOnlyCardsCount }}</div>
                          <div class="diff-tab-label">其他来源</div>
                        </div>
                      </div>
                    </div>
                  </v-card>
                </div>

                <!-- Tab内容 -->
                <v-window v-model="diffTab">
                  <!-- 内容有更新Tab -->
                  <v-window-item value="modified">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-information" size="16" color="grey-lighten-1" class="mr-2"></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些卡片的内容在卡片组中已更新，与您学习记录中的版本不同。您可以选择接受更新以获取最新内容。
                          </span>
                        </div>
                      </div>

                      <div v-if="modifiedDiffs.length === 0" class="text-center pa-8">
                        <v-icon icon="mdi-check-circle" size="64" color="success" class="mb-4"></v-icon>
                        <h4 class="text-h6 text-success mb-2">没有修改</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有修改的卡片</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(diff, index) in modifiedDiffs"
                          :key="'modified-' + diff.cardId"
                          class="mb-4 card-item"
                          rounded="12"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题和状态 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="warning"
                                    variant="flat"
                                    prepend-icon="mdi-pencil"
                                  >
                                    内容有更新
                                  </v-chip>
                                </div>

                                <!-- 新版本 -->
                                <div class="version-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-chip size="small" color="success" variant="flat" class="mr-2">新版本</v-chip>
                                    <span class="text-caption text-grey-darken-1">卡片组最新内容</span>
                                  </div>

                                  <div class="question-section mb-3">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                                    </div>
                                    <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-3">{{ diff.newVersion.front }}</p>
                                    </div>
                                  </div>

                                  <div class="answer-section">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                                    </div>
                                    <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-3">{{ diff.newVersion.back }}</p>
                                    </div>
                                  </div>
                                </div>

                                <!-- 旧版本 -->
                                <div class="version-section">
                                  <div class="d-flex align-center mb-2">
                                    <v-chip size="small" color="grey" variant="flat" class="mr-2">学习版本</v-chip>
                                    <span class="text-caption text-grey-darken-1">您当前学习的内容</span>
                                  </div>

                                  <div class="question-section mb-3">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                                    </div>
                                    <div class="question-content pa-3 bg-grey-lighten-4 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-2">{{ diff.oldVersion.front }}</p>
                                    </div>
                                  </div>

                                  <div class="answer-section">
                                    <div class="d-flex align-center mb-2">
                                      <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                                      <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                                    </div>
                                    <div class="answer-content pa-3 bg-grey-lighten-4 rounded-lg">
                                      <p class="text-body-1 mb-0 text-grey-darken-2">{{ diff.oldVersion.back }}</p>
                                    </div>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-eye"
                                  class="action-btn"
                                  @click="viewCard({...diff.newVersion, id: diff.cardId})"
                                >
                                  预览
                                </v-btn>

                                <v-btn
                                  color="warning"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-check"
                                  class="action-btn"
                                  @click="acceptUpdate(diff.cardId)"
                                >
                                  接受修改
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>

                  <!-- 可添加Tab -->
                  <v-window-item value="added">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-information" size="16" color="grey-lighten-1" class="mr-2"></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些是卡片组中新增的卡片，您还未开始学习。可以选择添加到您的学习计划中。
                          </span>
                        </div>
                      </div>

                      <div v-if="addedDiffs.length === 0" class="text-center pa-8">
                        <v-icon icon="mdi-plus-circle-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有可添加的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有新的卡片可以添加</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(diff, index) in addedDiffs"
                          :key="'added-' + diff.cardId"
                          class="mb-4 card-item"
                          rounded="12"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题和状态 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="success"
                                    variant="flat"
                                    prepend-icon="mdi-plus"
                                  >
                                    未学习
                                  </v-chip>
                                </div>

                                <!-- 问题 -->
                                <div class="question-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                                  </div>
                                  <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0 text-grey-darken-3">{{ diff.newVersion.front }}</p>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                                  </div>
                                  <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0 text-grey-darken-3">{{ diff.newVersion.back }}</p>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-eye"
                                  class="action-btn"
                                  @click="viewCard({...diff.newVersion, id: diff.cardId})"
                                >
                                  预览
                                </v-btn>

                                <v-btn
                                  color="success"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-plus"
                                  class="action-btn"
                                  @click="addCardToStudyFromDiff(diff.cardId)"
                                >
                                  添加学习
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>

                  <!-- 其他来源Tab -->
                  <v-window-item value="nodeOnly">
                    <div class="cards-container">
                      <!-- 解释说明 -->
                      <div class="mb-4 pa-3">
                        <div class="d-flex align-center">
                          <v-icon icon="mdi-information" size="16" color="grey-lighten-1" class="mr-2"></v-icon>
                          <span class="text-body-2 text-grey-lighten-1">
                            这些卡片来自其他卡片组，或者是已经被删除的卡片。如果不需要，可以选择移除学习。
                          </span>
                        </div>
                      </div>

                      <div v-if="nodeOnlyCards.length === 0" class="text-center pa-8">
                        <v-icon icon="mdi-bookmark-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
                        <h4 class="text-h6 text-grey-darken-1 mb-2">没有其他来源的卡片</h4>
                        <p class="text-body-2 text-grey-darken-1">当前没有来自其他来源的卡片</p>
                      </div>

                      <template v-else>
                        <v-card
                          v-for="(card, index) in nodeOnlyCards"
                          :key="'node-only-' + card.id"
                          class="mb-4 card-item"
                          rounded="12"
                          elevation="0"
                          variant="outlined"
                        >
                          <v-card-text class="pa-5">
                            <div class="d-flex align-start justify-space-between">
                              <div class="flex-grow-1 mr-4">
                                <!-- 卡片标题和状态 -->
                                <div class="d-flex align-center justify-space-between mb-3">
                                  <h4 class="text-h6 font-weight-bold text-grey-darken-3">
                                    卡片 {{ index + 1 }}
                                  </h4>
                                  <v-chip
                                    size="small"
                                    color="info"
                                    variant="flat"
                                    prepend-icon="mdi-bookmark"
                                  >
                                    其他来源
                                  </v-chip>
                                </div>

                                <!-- 问题 -->
                                <div class="question-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                                  </div>
                                  <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0 text-grey-darken-3">{{ card.front }}</p>
                                  </div>
                                </div>

                                <!-- 答案 -->
                                <div class="answer-section mb-4">
                                  <div class="d-flex align-center mb-2">
                                    <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                                    <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                                  </div>
                                  <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                                    <p class="text-body-1 mb-0 text-grey-darken-3">{{ card.back }}</p>
                                  </div>
                                </div>

                                <!-- 学习进度信息 -->
                                <div v-if="card.srsState" class="mt-4">
                                  <div class="d-flex align-center justify-space-between text-body-2 text-grey-darken-2">
                                    <span>复习次数：{{ card.srsState.repetitions }}次</span>
                                    <span>难度系数：{{ card.srsState.easeFactor }}</span>
                                    <span>下次复习：{{ new Date(card.srsState.reviewDueAt).toLocaleDateString() }}</span>
                                  </div>
                                </div>
                              </div>

                              <!-- 操作按钮 -->
                              <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                                <v-btn
                                  color="primary"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-eye"
                                  class="action-btn"
                                  @click="viewCard(card)"
                                >
                                  预览
                                </v-btn>

                                <v-btn
                                  color="error"
                                  variant="tonal"
                                  size="small"
                                  rounded="xl"
                                  prepend-icon="mdi-close"
                                  class="action-btn"
                                  @click="removeFromStudy(card.id)"
                                >
                                  移除学习
                                </v-btn>
                              </div>
                            </div>
                          </v-card-text>
                        </v-card>
                      </template>
                    </div>
                  </v-window-item>
                </v-window>
              </div>
            </v-window-item>
          </v-window>
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div class="bottom-actions pa-6 bg-grey-lighten-5 d-flex align-center justify-space-between" style="flex-shrink: 0;">
        <!-- Diff标签页的操作按钮 -->
        <div v-if="currentTab === 'diff' && (addedDiffs.length > 0 || modifiedDiffs.length > 0 || nodeOnlyCardsCount > 0)" class="d-flex align-center" style="gap: 12px;">
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            prepend-icon="mdi-sync"
            @click="acceptAllChanges"
          >
            同步所有更新
          </v-btn>
          <v-btn
            color="success"
            variant="outlined"
            rounded="lg"
            prepend-icon="mdi-plus"
            @click="addAllNewCards"
          >
            添加所有新卡片
          </v-btn>
        </div>

        <!-- 其他标签页的提示信息 -->
        <div v-else class="text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          <span v-if="studyCards.length === 0">点击"学习卡片组"将所有卡片加入您的学习计划</span>
          <span v-else>点击"学习卡片组"查看更新差异并选择要学习的卡片</span>
        </div>

        <div class="d-flex" style="gap: 12px;">
          <v-btn
            variant="text"
            rounded="xl"
            size="large"
            @click="closeDialog"
          >
            关闭
          </v-btn>

          <v-btn
            v-if="deckDetail && currentTab === 'study'"
            color="primary"
            variant="flat"
            rounded="xl"
            size="large"
            prepend-icon="mdi-compare"
            @click="goToDiffTab"
          >
            查看对比更新
          </v-btn>

          <v-btn
            v-else-if="deckDetail && currentTab === 'all' && studyCards.length === 0"
            color="primary"
            variant="flat"
            rounded="xl"
            size="large"
            prepend-icon="mdi-playlist-plus"
            @click="addToStudy"
          >
            学习卡片组
          </v-btn>

          <v-btn
            v-else-if="deckDetail && currentTab === 'all' && studyCards.length > 0"
            color="primary"
            variant="flat"
            rounded="xl"
            size="large"
            prepend-icon="mdi-compare"
            @click="goToDiffTab"
          >
            学习卡片组
          </v-btn>
        </div>
      </div>
    </v-card>

    <!-- 编辑/新建卡片对话框 -->
    <v-dialog v-model="showEditDialog" width="600" persistent>
      <v-card rounded="xl" elevation="0">
        <v-card-title class="pa-6 bg-primary text-white">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-h5 font-weight-bold">
              {{ editingCard ? '编辑卡片' : '新建卡片' }}
            </h3>
            <v-btn 
              icon="mdi-close" 
              variant="text" 
              color="white"
              @click="showEditDialog = false"
            ></v-btn>
          </div>
        </v-card-title>
        
        <v-card-text class="pa-6">
          <v-form v-model="editCardFormValid">
            <div class="mb-6">
              <label class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-2 d-block">
                <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                问题 (卡片正面)
              </label>
              <v-textarea
                v-model="editCardFront"
                variant="outlined"
                rounded="lg"
                placeholder="请输入问题内容..."
                rows="3"
                :rules="cardFrontRules"
                :counter="CARD_VALIDATION.FRONT_MAX_LENGTH"
              ></v-textarea>
            </div>
            
            <div class="mb-4">
              <label class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-2 d-block">
                <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                答案 (卡片背面)
              </label>
              <v-textarea
                v-model="editCardBack"
                variant="outlined"
                rounded="lg"
                placeholder="请输入答案内容..."
                rows="4"
                :rules="cardBackRules"
                :counter="CARD_VALIDATION.BACK_MAX_LENGTH"
              ></v-textarea>
            </div>
          </v-form>
        </v-card-text>
        
        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn 
            variant="tonal"
            rounded="xl"
            @click="showEditDialog = false"
          >
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="tonal"
            rounded="xl"
            @click="saveCard"
            :disabled="!editCardFormValid || !editCardFront.trim() || !editCardBack.trim()"
          >
            {{ editingCard ? '保存修改' : '创建卡片' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 卡片预览对话框 - 正反面翻转效果 -->
    <v-dialog v-model="showCardDetail" width="600">
      <v-card v-if="selectedCard" rounded="xl" elevation="0">
        <!-- 预览头部 -->
        <div class="preview-header pa-6 bg-primary text-white">
          <div class="d-flex align-center justify-space-between">
            <h3 class="text-h5 font-weight-bold">卡片预览</h3>
            <v-btn icon="mdi-close" variant="text" color="white" @click="showCardDetail = false"></v-btn>
          </div>
        </div>
        
        <!-- 预览内容 -->
        <v-card-text class="pa-8">
          <!-- 卡片容器 -->
          <div class="card-container" @click="flipCard">
            <div class="card" :class="{ flipped: isFlipped }">
              <!-- 正面（问题） -->
              <div class="card-face card-front">
                <div class="d-flex align-center justify-center mb-4">
                  <v-icon icon="mdi-help-circle" color="primary" size="32"></v-icon>
                </div>
                <div class="text-center">
                  <h4 class="text-h6 font-weight-bold text-primary mb-4">问题</h4>
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.front }}</p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="primary" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    点击翻转查看答案
                  </v-chip>
                </div>
              </div>
              
              <!-- 反面（答案） -->
              <div class="card-face card-back">
                <div class="d-flex align-center justify-center mb-4">
                  <v-icon icon="mdi-lightbulb" color="success" size="32"></v-icon>
                </div>
                <div class="text-center">
                  <h4 class="text-h6 font-weight-bold text-success mb-4">答案</h4>
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.back }}</p>
                </div>
                <div class="text-center mt-6">
                  <v-chip size="small" color="success" variant="outlined">
                    <v-icon icon="mdi-gesture-tap" size="16" class="mr-1"></v-icon>
                    点击翻转查看问题
                  </v-chip>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
        
        <!-- 预览底部 -->
        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn color="primary" variant="flat" rounded="xl" @click="showCardDetail = false">
            关闭预览
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<style scoped>
/* 点赞区域样式 */
.upvote-area {
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  padding: 4px 8px;
}

.upvote-area:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.05);
}

.upvote-area.upvoted {
  background: rgba(255, 255, 255, 0.25);
}

.upvote-area.upvoted:hover {
  background: rgba(255, 255, 255, 0.35);
}

/* 头部背景 */
.header-bg {
  background: rgb(var(--v-theme-primary));
}

/* 关闭按钮定位 */
.close-btn {
  top: 12px;
  right: 12px;
}

/* 卡片项样式 - 移除阴影效果 */
.card-item {
  transition: all 0.3s ease;
  border: 1px solid #e0e0e0;
}

.card-item:hover {
  border-color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.02);
}

/* 问题和答案内容区域 */
.question-content, .answer-content {
  border-left: 4px solid transparent;
  transition: all 0.2s ease;
}

.question-content {
  border-left-color: rgb(var(--v-theme-primary));
}

.answer-content {
  border-left-color: rgb(var(--v-theme-success));
}

/* 操作按钮样式 */
.action-btn {
  min-width: 90px !important;
  transition: all 0.2s ease;
}

.action-btn:hover {
  transform: scale(1.05);
}

/* 底部操作栏样式 */
.bottom-actions {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

/* 预览对话框样式 */
.preview-header {
  background: rgb(var(--v-theme-primary));
}

/* 3D翻转卡片样式 */
.card-container {
  perspective: 1000px;
  height: 300px;
  cursor: pointer;
}

.card {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.6s ease-in-out;
}

.card.flipped {
  transform: rotateY(180deg);
}

.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 2rem;
  border-radius: 16px;
  border: 2px solid #e0e0e0;
  background: white;
}

.card-front {
  /* 正面默认显示 */
}

.card-back {
  transform: rotateY(180deg);
}

/* 差异对比tab样式 - Flat设计 */
.diff-tab-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 0px;
  padding: 8px 12px;
  min-width: 80px;
  background: transparent;
  text-align: center;
}

.diff-tab-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.diff-tab-active {
  background: rgba(var(--v-theme-primary), 0.08) !important;
}

.diff-tab-number {
  font-size: 1.125rem;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
}

.diff-tab-label {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.7);
  font-weight: 400;
  line-height: 1;
}

/* Tab项固定宽度 */
.tab-item {
  min-width: 80px;
  flex: 0 0 80px;
}

/* 可点击的统计数字样式 */
.clickable-stat {
  transition: all 0.2s ease;
}

.clickable-stat:hover {
  transform: scale(1.05);
}

/* 选中状态的背景 */
.selected-tab {
  background-color: rgba(0, 0, 0, 0.08) !important;
}

/* 滚动条样式 */
.flex-grow-1::-webkit-scrollbar {
  width: 8px;
}

.flex-grow-1::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.flex-grow-1::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.flex-grow-1::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .action-btn {
    min-width: 70px !important;
    font-size: 0.75rem;
  }
  
  .close-btn {
    top: 8px;
    right: 8px;
  }
}

/* Diff内容样式 */
.diff-content {
  border-left: 4px solid transparent;
  transition: all 0.2s ease;
}

.diff-content.added {
  background-color: rgba(76, 175, 80, 0.1);
  border-left-color: #4caf50;
}

.diff-content.deleted {
  background-color: rgba(244, 67, 54, 0.1);
  border-left-color: #f44336;
}

/* 双列Diff界面样式 */
.diff-container {
  height: auto;
  min-height: 400px;
}

/* 修改的卡片行对齐 */
.modified-cards-section {
  margin-bottom: 24px;
  margin-top: 32px;
}

.section-header {
  padding: 8px 0;
  border-bottom: 1px solid #e8eaed;
}

.modified-card-row {
  align-items: flex-start;
}

.modified-row {
  align-items: flex-start;
}

.panel-header {
  padding: 12px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #e8eaed;
}

.card-list-content {
  padding: 0;
}

.diff-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e8eaed;
  transition: all 0.2s ease;
  position: relative;
  height: fit-content;
  width: 100%;
  box-sizing: border-box;
}

.diff-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.diff-item.modified {
  border-left: 4px solid #ff9800;
  background: linear-gradient(135deg, #fff8e1 0%, #ffffff 100%);
  margin-bottom: 0;
}

.diff-item.modified-old {
  border-left: 4px solid #9e9e9e;
  background: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
  margin-bottom: 0;
}

.diff-item.added {
  border-left: 4px solid #4caf50;
  background: linear-gradient(135deg, #f1f8e9 0%, #ffffff 100%);
}

.diff-item.node-only {
  border-left: 4px solid #2196f3;
  background: linear-gradient(135deg, #e8f4fd 0%, #ffffff 100%);
}

.diff-content-card {
  margin-top: 8px;
}

.diff-content-card.old-version {
  opacity: 0.85;
}

.content-section {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.content-label {
  font-size: 12px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}

.content-text {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-break: break-word;
}

.card-id {
  color: #666;
  font-size: 14px;
  font-family: 'Monaco', 'Menlo', monospace;
}

.diff-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.diff-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 左右面板间距 */
.left-panel {
  padding-right: 8px;
  flex: 0 0 50%;
  max-width: 50%;
}

.right-panel {
  padding-left: 8px;
  flex: 0 0 50%;
  max-width: 50%;
}
</style>