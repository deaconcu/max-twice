<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Post } from '@/types/post'
import type { CreateDeckRequest, CreateCardRequest, MemoryCardDeck } from '@/types/memoryCard'

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

// 表单验证规则
const titleRules = [
  (v: string) => !!v || '标题不能为空',
  (v: string) => v.length <= 100 || '标题不能超过100个字符'
]

const frontRules = [
  (v: string) => !!v || '问题不能为空',
  (v: string) => v.length <= 500 || '问题不能超过500个字符'
]

const backRules = [
  (v: string) => !!v || '答案不能为空',
  (v: string) => v.length <= 1000 || '答案不能超过1000个字符'
]

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
    title: '',
    description: ''
  }
  cardForm.value = {
    front: '',
    back: ''
  }
  cards.value = []
  currentDeck.value = null
}

const createDeck = async () => {
  if (!deckForm.value.title.trim()) return

  loading.value = true
  
  try {
    // TODO: 调用真实API
    // const response = await memoryCardService.createDeck({
    //   sourcePostId: props.post.id,
    //   title: deckForm.value.title,
    //   description: deckForm.value.description
    // })

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟创建的卡片组
    currentDeck.value = {
      id: Date.now(),
      sourcePostId: props.post.id,
      creatorId: 1,
      creator: { id: 1, name: '当前用户', email: 'user@example.com' },
      title: deckForm.value.title,
      description: deckForm.value.description,
      version: 1, // 新创建的卡片组版本为1
      state: 0, // 审核中
      upvoteCount: 0,
      cardCount: 0,
      score: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    step.value = 2
  } catch (error) {
    console.error('Failed to create deck:', error)
  } finally {
    loading.value = false
  }
}

const addCard = () => {
  if (!cardForm.value.front.trim() || !cardForm.value.back.trim()) return

  cards.value.push({
    deckId: currentDeck.value!.id,
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
    return
  }

  loading.value = true

  try {
    // TODO: 批量创建卡片
    // for (const card of cards.value) {
    //   await memoryCardService.createCard(card)
    // }

    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    if (currentDeck.value) {
      currentDeck.value.cardCount = cards.value.length
      emit('created', currentDeck.value)
    }

    dialog.value = false
  } catch (error) {
    console.error('Failed to create cards:', error)
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
        <v-form @submit.prevent="createDeck">
          <v-text-field
            v-model="deckForm.title"
            label="卡片组标题"
            placeholder="为你的卡片组起一个有意义的名字"
            :rules="titleRules"
            variant="outlined"
            rounded="lg"
            class="mb-4"
          ></v-text-field>

          <v-textarea
            v-model="deckForm.description"
            label="描述（可选）"
            placeholder="简要描述这个卡片组的内容和用途"
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
        <v-form @submit.prevent="addCard" class="mb-6">
          <v-textarea
            v-model="cardForm.front"
            label="问题（卡片正面）"
            placeholder="输入问题..."
            :rules="frontRules"
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
            :rules="backRules"
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
            :disabled="!cardForm.front.trim() || !cardForm.back.trim()"
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
          
          <v-card
            v-for="(card, index) in cards"
            :key="index"
            class="mb-3"
            elevation="1"
            rounded="lg"
          >
            <v-card-text class="pa-4">
              <div class="d-flex justify-space-between align-start mb-2">
                <h5 class="text-subtitle-2 font-weight-bold text-grey-darken-3">
                  卡片 {{ index + 1 }}
                </h5>
                <v-btn
                  icon="mdi-delete-outline"
                  variant="text"
                  size="small"
                  color="error"
                  @click="removeCard(index)"
                ></v-btn>
              </div>
              
              <div class="mb-2">
                <span class="text-body-2 font-weight-medium text-grey-darken-2">问题：</span>
                <p class="text-body-2 text-grey-darken-1 mb-0">{{ card.front }}</p>
              </div>
              
              <div>
                <span class="text-body-2 font-weight-medium text-grey-darken-2">答案：</span>
                <p class="text-body-2 text-grey-darken-1 mb-0">{{ card.back }}</p>
              </div>
            </v-card-text>
          </v-card>
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
          :disabled="!deckForm.title.trim()"
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