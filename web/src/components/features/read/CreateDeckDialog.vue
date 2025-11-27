<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { memoryApi } from '@/api'
import type { MemoryCardDeck } from '@/types/memory'
import { useMutation } from '@/composables'
import { useValidationConfigStore } from '@/stores/validationConfig'

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
const step = ref(1) // 1: 添加卡片, 2: 填写说明

// 获取验证配置
const validationStore = useValidationConfigStore()

// 生成验证规则
const frontRules = computed(() => validationStore.createRules('card-front'))
const backRules = computed(() => validationStore.createRules('card-back'))
const descriptionRules = computed(() => validationStore.createRules('deck-description'))

// 获取最大长度（用于 counter）
const frontMaxLength = computed(() => validationStore.getRule('card-front')?.maxLength || 500)
const backMaxLength = computed(() => validationStore.getRule('card-back')?.maxLength || 500)
const descriptionMaxLength = computed(
  () => validationStore.getRule('deck-description')?.maxLength || 200
)

// 卡片组表单
const deckForm = ref({
  description: '',
})

// 卡片表单
const cardForm = ref({
  front: '',
  back: '',
})

const cards = ref<Card[]>([])
const showEmptyError = ref(false)

// 卡片表单验证状态
const cardFormValid = computed(() => {
  const front = cardForm.value.front.trim()
  const back = cardForm.value.back.trim()

  // 检查是否为空
  if (!front || !back) return false

  // 检查长度限制
  const frontMaxLen = validationStore.getRule('card-front')?.maxLength || 500
  const backMaxLen = validationStore.getRule('card-back')?.maxLength || 500

  if (front.length > frontMaxLen || back.length > backMaxLen) return false

  return true
})

// 使用 useMutation 处理创建卡片组
const { execute: createDeckMutation, loading } = useMutation(
  () =>
    memoryApi.createDeck({
      sourcePostId: props.postId,
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
    description: '',
  }
  cardForm.value = {
    front: '',
    back: '',
  }
  cards.value = []
  showEmptyError.value = false
}

const goToDescription = () => {
  if (cards.value.length === 0) {
    showEmptyError.value = true
    return
  }
  showEmptyError.value = false
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
      <div class="d-flex align-center justify-space-between pa-6 pb-2">
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
              {{ step === 1 ? '添加记忆卡片' : '填写说明' }}
            </h3>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              {{ step === 1 ? '先添加卡片，然后填写说明' : '为卡片组添加描述说明' }}
            </p>
          </div>
        </div>
        <v-btn icon="mdi-close" variant="text" @click="closeDialog"></v-btn>
      </div>

      <!-- 步骤1: 添加卡片 -->
      <v-card-text v-if="step === 1" class="px-6 pt-2 pb-6">
        <!-- 卡片添加表单 -->
        <v-form class="mb-6" @submit.prevent="addCard">
          <v-textarea
            v-model="cardForm.front"
            label="问题（卡片正面）"
            placeholder="输入问题..."
            :rules="frontRules"
            :counter="frontMaxLength"
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
            :counter="backMaxLength"
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

      <!-- 步骤2: 填写说明 -->
      <v-card-text v-if="step === 2" class="px-6 pt-2 pb-6">
        <v-form @submit.prevent="finishCreation">
          <!-- 已添加的卡片数量提示 -->
          <div class="d-flex align-center pa-3 rounded-lg mb-4" style="background-color: #f5f5f5">
            <v-icon icon="mdi-information-outline" size="20" color="primary" class="mr-2"></v-icon>
            <span class="text-body-2 text-grey-darken-2">
              已添加 <strong>{{ cards.length }}</strong> 张卡片
            </span>
          </div>

          <v-textarea
            v-model="deckForm.description"
            label="描述（可选）"
            placeholder="说说你为什么要创建这个卡片组，它和别的卡片组相比有什么不一样的地方"
            :rules="descriptionRules"
            :counter="descriptionMaxLength"
            variant="outlined"
            rounded="lg"
            rows="4"
            no-resize
          ></v-textarea>
        </v-form>
      </v-card-text>

      <!-- 底部操作按钮 -->
      <v-card-actions class="pa-6 pt-0">
        <!-- 空卡片错误提示 -->
        <div v-if="step === 1 && showEmptyError" class="d-flex align-center">
          <v-icon icon="mdi-alert-circle" size="14" color="error" class="mr-1"></v-icon>
          <p class="text-caption text-error mb-0">至少需要添加一张卡片才能进入下一步</p>
        </div>

        <v-spacer></v-spacer>

        <v-btn variant="outlined" rounded="lg" class="mr-3" @click="closeDialog"> 取消 </v-btn>

        <v-btn
          v-if="step === 1"
          color="primary"
          variant="flat"
          rounded="lg"
          :disabled="cards.length === 0"
          @click="goToDescription"
        >
          下一步
        </v-btn>

        <v-btn
          v-if="step === 2"
          color="success"
          variant="flat"
          rounded="lg"
          :loading="loading"
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
