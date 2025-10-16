<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import type { Post } from '@/types/post'
import type { CreateDeckRequest, CreateCardRequest, MemoryCardDeck } from '@/types/memoryCard'
import { MemoryService } from '@/services/memoryService'
import { deckTitleRules, deckDescriptionRules, cardFrontRules, cardBackRules } from '@/utils/validationRules'
import { DECK_VALIDATION, CARD_VALIDATION } from '@/types/validation'

interface Props {
  modelValue: boolean
  post: Post
  deck?: MemoryCardDeck
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'created', deck: MemoryCardDeck): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()
const userStore = useUserStore()

const dialog = ref(false)
const loading = ref(false)
const step = ref(1) // 1: 创建卡片组, 2: 添加卡片

// 卡片组表单
const deckForm = ref({
  title: '',
  description: ''
})

// 卡片表单
const cardForm = ref({
  front: '',
  back: ''
})

const cards = ref<CreateCardRequest[]>([])
const currentDeck = ref<MemoryCardDeck | null>(null)
const showEmptyError = ref(false)

// 表单验证状态
const deckFormValid = ref(true)
const cardFormValid = ref(true)

watch(() => props.modelValue, (newVal) => {
  dialog.value = newVal
  if (newVal) {
    resetForm()
  }
})

watch(dialog, (newVal) => {
  emit('update:modelValue', newVal)
})

const resetForm = () => {
  step.value = 1
  deckForm.value = {
    title: `${userStore.name}的记忆卡片组`,
    description: ''
  }
  cardForm.value = {
    front: '',
    back: ''
  }
  cards.value = []
  currentDeck.value = null
  showEmptyError.value = false
}

const createDeck = async () => {
  if (!deckForm.value.title.trim()) return

  // 第一步只是进入下一步，不调用接口
  // 创建临时的卡片组对象用于显示
  currentDeck.value = {
    id: 0, // 临时ID，实际创建时由后端分配
    sourcePostId: props.post.id,
    creator: userStore.userId ? { 
      id: userStore.userId, 
      name: userStore.name || '当前用户', 
      email: 'user@example.com' 
    } : undefined,
    title: deckForm.value.title,
    description: deckForm.value.description,
    state: 0,
    upvoteCount: 0,
    cardCount: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }

  step.value = 2
}

const addCard = () => {
  if (!cardForm.value.front.trim() || !cardForm.value.back.trim()) return

  cards.value.push({
    deckId: 0, // 临时值，实际创建时会被忽略
    front: cardForm.value.front,
    back: cardForm.value.back
  })

  cardForm.value = {
    front: '',
    back: ''
  }

  // 聚焦到前面输入框
  nextTick(() => {
    const frontInput = document.querySelector('textarea[placeholder="输入问题..."]') as HTMLTextAreaElement
    if (frontInput) {
      frontInput.focus()
    }
  })
}

const removeCard = (index: number) => {
  cards.value.splice(index, 1)
}

const finishCreation = async () => {
  if (cards.value.length === 0) {
    // 至少需要一张卡片
    showEmptyError.value = true
    return
  }

  showEmptyError.value = false
  loading.value = true

  try {
    // 第二步：一次性创建卡片组和所有卡片
    const response = await MemoryService.createDeck({
      sourcePostId: props.post.id,
      title: deckForm.value.title,
      description: deckForm.value.description,
      cards: cards.value.map(card => ({
        front: card.front,
        back: card.back
      }))
    })

    if (response.code === 200) {
      // 使用后端返回的真实卡片组数据
      currentDeck.value = response.data
      currentDeck.value.cardCount = cards.value.length
      emit('created', currentDeck.value)
      dialog.value = false
    }
  } catch (error) {
    console.error('Failed to create deck and cards:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (step.value === 2) {
    step.value = 1
  }
}

const closeDialog = () => {
  dialog.value = false
}
</script>

<template>
  <v-dialog v-model="dialog" width="600" persistent>
    <v-card rounded="xl" elevation="8">
      <!-- 头部 -->
      <div class="d-flex align-center justify-space-between pa-6 pb-4">
        <div class="d-flex align-center">
          <v-btn
            v-if="step === 2"
            icon="mdi-arrow-left"
            variant="text"
            size="small"
            class="mr-3"
            @click="goBack"
          ></v-btn>
          <div>
            <h3 class="text-h5 font-weight-bold text-grey-darken-3">
              {{ step === 1 ? '创建记忆卡片组' : '添加记忆卡片' }}
            </h3>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              {{ step === 1 ? '为文章创建新的记忆卡片组' : `为"${currentDeck?.title}"添加卡片` }}
            </p>
          </div>
        </div>
        <v-btn icon="mdi-close" variant="text" @click="closeDialog"></v-btn>
      </div>

      <v-divider></v-divider>

      <!-- 步骤1: 创建卡片组 -->
      <v-card-text v-if="step === 1" class="pa-6">
        <v-form v-model="deckFormValid" @submit.prevent="createDeck">
          <v-text-field
            v-model="deckForm.title"
            label="卡片组标题"
            placeholder="为你的卡片组起一个有意义的名字"
            :rules="deckTitleRules"
            :counter="DECK_VALIDATION.TITLE_MAX_LENGTH"
            variant="outlined"
            rounded="lg"
            class="mb-4"
          ></v-text-field>

          <v-textarea
            v-model="deckForm.description"
            label="描述（可选）"
            placeholder="简要描述这个卡片组的内容和用途"
            :rules="deckDescriptionRules"
            :counter="DECK_VALIDATION.DESCRIPTION_MAX_LENGTH"
            variant="outlined"
            rounded="lg"
            rows="3"
            no-resize
          ></v-textarea>
        </v-form>
      </v-card-text>

      <!-- 步骤2: 添加卡片 -->
      <v-card-text v-if="step === 2" class="pa-6">
        <!-- 卡片添加表单 -->
        <v-form v-model="cardFormValid" @submit.prevent="addCard" class="mb-6">
          <v-textarea
            v-model="cardForm.front"
            label="问题（卡片正面）"
            placeholder="输入问题..."
            :rules="cardFrontRules"
            :counter="CARD_VALIDATION.FRONT_MAX_LENGTH"
            variant="outlined"
            rounded="lg"
            rows="2"
            no-resize
            class="mb-4"
          ></v-textarea>

          <v-textarea
            v-model="cardForm.back"
            label="答案（卡片背面）"
            placeholder="输入答案..."
            :rules="cardBackRules"
            :counter="CARD_VALIDATION.BACK_MAX_LENGTH"
            variant="outlined"
            rounded="lg"
            rows="3"
            no-resize
            class="mb-4"
          ></v-textarea>

          <v-btn
            type="submit"
            color="primary"
            variant="outlined"
            rounded="lg"
            prepend-icon="mdi-plus"
            :disabled="!cardFormValid || !cardForm.front.trim() || !cardForm.back.trim()"
            block
          >
            添加卡片
          </v-btn>
        </v-form>

        <!-- 已添加的卡片列表 -->
        <div v-if="cards.length > 0">
          <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
            已添加的卡片 ({{ cards.length }})
          </h4>
          
          <div
            v-for="(card, index) in cards"
            :key="index"
            class="mb-3 pa-4 border rounded-lg position-relative"
            style="border-color: #e0e0e0; background-color: #fafafa;"
          >
            <div class="d-flex justify-space-between align-start mb-3">
              <div class="d-flex align-center">
                <v-chip 
                  size="small" 
                  variant="outlined" 
                  color="primary"
                  class="mr-2"
                >
                  {{ index + 1 }}
                </v-chip>
                <span class="text-body-2 font-weight-medium text-grey-darken-2">卡片</span>
              </div>
              <v-btn
                icon="mdi-close"
                variant="text"
                size="x-small"
                color="grey"
                class="ml-2"
                @click="removeCard(index)"
              ></v-btn>
            </div>
            
            <div class="mb-3">
              <div class="text-caption font-weight-medium text-primary mb-1">问题</div>
              <div class="text-body-2 text-grey-darken-3 pa-2 rounded" style="background-color: white; border: 1px solid #e0e0e0;">
                {{ card.front }}
              </div>
            </div>
            
            <div>
              <div class="text-caption font-weight-medium text-primary mb-1">答案</div>
              <div class="text-body-2 text-grey-darken-3 pa-2 rounded" style="background-color: white; border: 1px solid #e0e0e0;">
                {{ card.back }}
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态提示 -->
        <div v-else class="text-center pa-6">
          <v-icon icon="mdi-cards-outline" size="48" color="grey-lighten-2" class="mb-3"></v-icon>
          <p class="text-body-2 text-grey-darken-1">
            还没有添加任何卡片，请在上方表单中添加第一张卡片
          </p>
        </div>
      </v-card-text>

      <!-- 底部操作按钮 -->
      <v-card-actions class="pa-6 pt-0">
        <!-- 空卡片错误提示 -->
        <div v-if="step === 2 && showEmptyError" class="d-flex align-center">
          <v-icon icon="mdi-alert-circle" size="14" color="grey-darken-1" class="mr-1"></v-icon>
          <p class="text-caption text-grey-darken-1 mb-0">
            至少需要添加一张卡片才能完成创建
          </p>
        </div>
        
        <v-spacer></v-spacer>
        
        <v-btn
          variant="outlined"
          rounded="lg"
          class="mr-3"
          @click="closeDialog"
        >
          取消
        </v-btn>

        <v-btn
          v-if="step === 1"
          color="primary"
          variant="flat"
          rounded="lg"
          :loading="loading"
          :disabled="!deckFormValid || !deckForm.title.trim()"
          @click="createDeck"
        >
          下一步
        </v-btn>

        <v-btn
          v-if="step === 2"
          color="success"
          variant="flat"
          rounded="lg"
          :loading="loading"
          :disabled="cards.length === 0"
          @click="finishCreation"
        >
          完成创建
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.v-form {
  width: 100%;
}
</style>