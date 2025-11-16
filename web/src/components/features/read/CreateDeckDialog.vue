<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { memoryApi } from '@/api'
import type { MemoryCardDeck } from '@/types/memory'
import { useMutation } from '@/composables'

interface Card {
  front: string
  back: string
}

interface Props {
  postId: number
}

type Emits = (e: 'created', deck: MemoryCardDeck) => void

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const dialog = defineModel<boolean>({ default: false })
const step = ref(1) // 1: 创建卡片组, 2: 添加卡片

// 卡片组表单
const deckForm = ref({
  title: '',
  description: '',
})

// 卡片表单
const cardForm = ref({
  front: '',
  back: '',
})

const cards = ref<Card[]>([])
const showEmptyError = ref(false)

// 表单验证
const deckFormValid = computed(() => {
  return deckForm.value.title.trim().length > 0 && deckForm.value.title.length <= 100
})

const cardFormValid = computed(() => {
  return (
    cardForm.value.front.trim().length > 0 &&
    cardForm.value.front.length <= 500 &&
    cardForm.value.back.trim().length > 0 &&
    cardForm.value.back.length <= 2000
  )
})

// 使用 useMutation 处理创建卡片组
const { execute: createDeckMutation, loading } = useMutation(
  () =>
    memoryApi.createDeck({
      sourcePostId: props.postId,
      title: deckForm.value.title,
      description: deckForm.value.description || undefined,
      cards: cards.value.map((card) => ({
        front: card.front,
        back: card.back,
      })),
    }),
  {
    successMessage: '创建成功',
    onSuccess: (result) => {
      if (result) {
        emit('created', result)
      }
      dialog.value = false
    },
  }
)

watch(dialog, (newVal) => {
  if (newVal) {
    resetForm()
  }
})

const resetForm = () => {
  step.value = 1
  deckForm.value = {
    title: '',
    description: '',
  }
  cardForm.value = {
    front: '',
    back: '',
  }
  cards.value = []
  showEmptyError.value = false
}

const createDeck = () => {
  if (!deckFormValid.value) return
  step.value = 2
}

const addCard = () => {
  if (!cardFormValid.value) return

  cards.value.push({
    front: cardForm.value.front,
    back: cardForm.value.back,
  })

  cardForm.value = {
    front: '',
    back: '',
  }

  // 聚焦到前面输入框
  nextTick(() => {
    const frontInput = document.querySelector('textarea[placeholder="输入问题..."]')!
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
    showEmptyError.value = true
    return
  }

  showEmptyError.value = false
  await createDeckMutation()
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
              {{ step === 1 ? '为文章创建新的记忆卡片组' : `为"${deckForm.title}"添加卡片` }}
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
            :counter="100"
            variant="outlined"
            rounded="lg"
            class="mb-4"
          ></v-text-field>

          <v-textarea
            v-model="deckForm.description"
            label="描述（可选）"
            placeholder="简要描述这个卡片组的内容和用途"
            :counter="500"
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
        <v-form class="mb-6" @submit.prevent="addCard">
          <v-textarea
            v-model="cardForm.front"
            label="问题（卡片正面）"
            placeholder="输入问题..."
            :counter="500"
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
            :counter="2000"
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
            :disabled="!cardFormValid"
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
            class="mb-3 pa-4 border rounded-lg position-relative card-item"
          >
            <div class="d-flex justify-space-between align-start mb-3">
              <div class="d-flex align-center">
                <v-chip size="small" variant="outlined" color="primary" class="mr-2">
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
              <div class="text-body-2 text-grey-darken-3 pa-2 rounded card-content">
                {{ card.front }}
              </div>
            </div>

            <div>
              <div class="text-caption font-weight-medium text-primary mb-1">答案</div>
              <div class="text-body-2 text-grey-darken-3 pa-2 rounded card-content">
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
          <p class="text-caption text-grey-darken-1 mb-0">至少需要添加一张卡片才能完成创建</p>
        </div>

        <v-spacer></v-spacer>

        <v-btn variant="outlined" rounded="lg" class="mr-3" @click="closeDialog"> 取消 </v-btn>

        <v-btn
          v-if="step === 1"
          color="primary"
          variant="flat"
          rounded="lg"
          :loading="loading"
          :disabled="!deckFormValid"
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

.card-item {
  border-color: #e0e0e0 !important;
  background-color: #fafafa;
}

.card-content {
  background-color: white;
  border: 1px solid #e0e0e0;
}
</style>
