<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { memoryApi } from '@/api'
import type { MemoryCardDeck } from '@/types/memory'
import { useMutation } from '@/composables'
import { useValidationConfigStore } from '@/stores/validationConfig'
import { useI18n } from '@/composables/useI18n'

const props = defineProps<Props>()

const emit = defineEmits<Emits>()

const { t } = useI18n()

interface Card {
  front: string
  back: string
}

interface Props {
  postId?: number
  nodeId?: number
}

type Emits = (e: 'created', deck: MemoryCardDeck) => void

const dialog = defineModel<boolean>({ default: false })
const step = ref(1) // 1: 添加卡片, 2: 填写说明

// 获取验证配置
const validationStore = useValidationConfigStore()

// 获取长度配置
const frontMinLength = computed(() => validationStore.getRule('card-front')?.minLength || 1)
const frontMaxLength = computed(() => validationStore.getRule('card-front')?.maxLength || 500)
const backMinLength = computed(() => validationStore.getRule('card-back')?.minLength || 1)
const backMaxLength = computed(() => validationStore.getRule('card-back')?.maxLength || 500)
const descriptionRules = computed(() => validationStore.createRules('deck-description'))
const descriptionMaxLength = computed(
  () => validationStore.getRule('deck-description')?.maxLength || 200
)

// 错误提示：长度不够时显示红色
const frontError = computed(() => {
  const val = cardForm.value.front.trim()
  if (!val) return ''
  if (val.length < frontMinLength.value)
    return t('createDeck.needMoreChars', { count: frontMinLength.value - val.length })
  return ''
})
const backError = computed(() => {
  const val = cardForm.value.back.trim()
  if (!val) return ''
  if (val.length < backMinLength.value)
    return t('createDeck.needMoreChars', { count: backMinLength.value - val.length })
  return ''
})

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
  const frontMinLen = validationStore.getRule('card-front')?.minLength || 1
  const frontMaxLen = validationStore.getRule('card-front')?.maxLength || 500
  const backMinLen = validationStore.getRule('card-back')?.minLength || 1
  const backMaxLen = validationStore.getRule('card-back')?.maxLength || 500

  if (front.length < frontMinLen || front.length > frontMaxLen) return false
  if (back.length < backMinLen || back.length > backMaxLen) return false

  return true
})

// 使用 useMutation 处理创建卡片组
const { execute: createDeckMutation, loading } = useMutation(
  () => {
    const requestData: {
      sourcePostId?: number
      nodeId?: number
      description?: string
      cards: { front: string; back: string }[]
    } = {
      description: deckForm.value.description || undefined,
      cards: cards.value.map((card) => ({
        front: card.front,
        back: card.back,
      })),
    }

    if (props.postId) {
      requestData.sourcePostId = props.postId
    } else if (props.nodeId) {
      requestData.sourcePostId = 0
      requestData.nodeId = props.nodeId
    }

    return memoryApi.createDeck(requestData)
  },
  {
    successMessage: t('createDeck.createSuccess'),
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
    <v-card rounded="xl" elevation="8" class="position-relative">
      <!-- 关闭按钮 -->
      <v-btn
        icon="mdi-close"
        variant="text"
        size="small"
        class="close-btn"
        @click="closeDialog"
      ></v-btn>

      <!-- 头部 -->
      <div class="pa-6 pb-2">
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
              {{ step === 1 ? t('createDeck.addCards') : t('createDeck.fillDescription') }}
            </h3>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              {{ step === 1 ? t('createDeck.addCardsHint') : t('createDeck.fillDescriptionHint') }}
            </p>
          </div>
        </div>
      </div>

      <!-- 步骤1: 添加卡片 -->
      <v-card-text v-if="step === 1" class="px-6 pt-2 pb-6">
        <!-- 卡片添加表单 -->
        <v-form class="mb-6" @submit.prevent="addCard">
          <v-textarea
            v-model="cardForm.front"
            :label="t('createDeck.questionLabel')"
            :placeholder="t('createDeck.questionPlaceholder')"
            :maxlength="frontMaxLength"
            :counter="frontMaxLength"
            :error-messages="frontError"
            variant="outlined"
            rounded="lg"
            rows="2"
            no-resize
            class="mb-4"
          ></v-textarea>

          <v-textarea
            v-model="cardForm.back"
            :label="t('createDeck.answerLabel')"
            :placeholder="t('createDeck.answerPlaceholder')"
            :maxlength="backMaxLength"
            :counter="backMaxLength"
            :error-messages="backError"
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
            {{ t('createDeck.addCard') }}
          </v-btn>
        </v-form>

        <!-- 已添加的卡片列表 -->
        <div v-if="cards.length > 0">
          <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
            {{ t('createDeck.addedCards', { count: cards.length }) }}
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
                <span class="text-body-2 font-weight-medium text-grey-darken-2">{{
                  t('createDeck.card')
                }}</span>
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
              <div class="text-caption font-weight-medium text-primary mb-1">
                {{ t('createDeck.question') }}
              </div>
              <div class="text-body-2 text-grey-darken-3 pa-2 rounded card-content">
                {{ card.front }}
              </div>
            </div>

            <div>
              <div class="text-caption font-weight-medium text-primary mb-1">
                {{ t('createDeck.answer') }}
              </div>
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
            {{ t('createDeck.emptyHint') }}
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
              {{ t('createDeck.cardsAdded', { count: cards.length }) }}
            </span>
          </div>

          <v-textarea
            v-model="deckForm.description"
            :label="t('createDeck.descriptionLabel')"
            :placeholder="t('createDeck.descriptionPlaceholder')"
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
          <p class="text-caption text-error mb-0">{{ t('createDeck.needAtLeastOneCard') }}</p>
        </div>

        <v-spacer></v-spacer>

        <v-btn variant="outlined" rounded="lg" class="mr-3" @click="closeDialog">
          {{ t('common.cancel') }}
        </v-btn>

        <v-btn
          v-if="step === 1"
          color="primary"
          variant="flat"
          rounded="lg"
          :disabled="cards.length === 0"
          @click="goToDescription"
        >
          {{ t('common.next') }}
        </v-btn>

        <v-btn
          v-if="step === 2"
          color="success"
          variant="flat"
          rounded="lg"
          :loading="loading"
          @click="finishCreation"
        >
          {{ t('createDeck.finishCreate') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1;
}

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
