<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MemoryCardDeck, DeckDetail, MemoryCardWithVersion } from '@/types/memoryCard'
import { MemoryCardMockService } from '@/services/memoryCardMockService'

interface Props {
  modelValue: boolean
  deck: MemoryCardDeck | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'addToStudy', deck: MemoryCardDeck): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

const dialog = ref(false)
const loading = ref(false)
const deckDetail = ref<DeckDetail | null>(null)
const selectedCard = ref<MemoryCardWithVersion | null>(null)
const showCardDetail = ref(false)
const isFlipped = ref(false) // 翻转状态

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
    const response = await MemoryCardMockService.getDeckDetail(props.deck.id)
    if (response.code === 200) {
      deckDetail.value = response.data
    }
  } catch (error) {
    console.error('Failed to load deck detail:', error)
  } finally {
    loading.value = false
  }
}

const viewCard = (card: MemoryCardWithVersion) => {
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

const addCardToStudy = async (card: MemoryCardWithVersion) => {
  try {
    const response = await MemoryCardMockService.addCardToStudy(card.id)
    if (response.code === 200) {
      // 更新卡片的用户状态
      card.userCard = response.data
      // TODO: 显示成功消息
    }
  } catch (error) {
    console.error('Failed to add card to study:', error)
  }
}

const closeDialog = () => {
  dialog.value = false
}
</script>

<template>
  <v-dialog v-model="dialog" width="900" persistent>
    <v-card rounded="xl" elevation="0" class="overflow-hidden" style="max-height: 80vh; display: flex; flex-direction: column;">
      <!-- 头部背景 -->
      <div class="header-bg pa-4 pb-6 position-relative">
        <v-btn 
          icon="mdi-close" 
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
                <div class="text-subtitle-1 font-weight-bold">{{ deckDetail?.stats.totalCards || 0 }}</div>
                <div class="text-caption opacity-80">卡片</div>
              </div>
              <div class="text-center">
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
          <h3 class="text-h6 font-weight-bold text-grey-darken-3 mb-4 d-flex align-center">
            <v-icon icon="mdi-cards-variant" class="mr-2" color="primary"></v-icon>
            卡片内容
          </h3>
          
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
                      <v-chip
                        v-if="card.userCard"
                        size="small"
                        :color="card.userCard.repetitions >= 3 ? 'success' : 'warning'"
                        variant="flat"
                        prepend-icon="mdi-trophy"
                      >
                        {{ card.userCard.repetitions >= 3 ? '已掌握' : `学习${card.userCard.repetitions}次` }}
                      </v-chip>
                      <v-chip v-else size="small" color="grey-lighten-1" variant="flat" prepend-icon="mdi-circle-outline">
                        未开始
                      </v-chip>
                    </div>
                    
                    <!-- 问题 -->
                    <div class="question-section mb-4">
                      <div class="d-flex align-center mb-2">
                        <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                      </div>
                      <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                        <p class="text-body-1 mb-0">{{ card.currentVersion.front }}</p>
                      </div>
                    </div>
                    
                    <!-- 答案 -->
                    <div class="answer-section">
                      <div class="d-flex align-center mb-2">
                        <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                      </div>
                      <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                        <p class="text-body-1 mb-0">{{ card.currentVersion.back }}</p>
                      </div>
                    </div>
                  </div>

                  <!-- 操作按钮 -->
                  <div class="d-flex flex-column align-center" style="gap: 12px; min-width: 100px;">
                    <v-btn
                      color="primary"
                      variant="outlined"
                      size="small"
                      rounded="xl"
                      prepend-icon="mdi-eye"
                      class="action-btn"
                      @click="viewCard(card)"
                    >
                      预览
                    </v-btn>
                    
                    <v-btn
                      v-if="!card.userCard"
                      color="success"
                      variant="flat"
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
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div class="bottom-actions pa-6 bg-grey-lighten-5 d-flex align-center justify-space-between" style="flex-shrink: 0;">
        <div class="text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          点击"学习整个卡片组"将所有卡片加入您的学习计划
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
            v-if="deckDetail"
            color="primary"
            variant="flat"
            rounded="xl"
            size="large"
            prepend-icon="mdi-playlist-plus"
            @click="addToStudy"
          >
            学习整个卡片组
          </v-btn>
        </div>
      </div>
    </v-card>

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
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.currentVersion.front }}</p>
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
                  <p class="text-h6 text-grey-darken-3">{{ selectedCard.currentVersion.back }}</p>
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
</style>