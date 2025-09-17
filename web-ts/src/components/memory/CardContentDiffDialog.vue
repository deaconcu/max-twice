<script setup lang="ts">
import { computed } from 'vue'
import type { CardContentDiff } from '@/types/memoryCard'

interface Props {
  modelValue: boolean
  cardDiff: CardContentDiff | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'accept-update', cardId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const show = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 接受更新
const acceptUpdate = (): void => {
  if (props.cardDiff) {
    emit('accept-update', props.cardDiff.cardId)
  }
  show.value = false
}

// 取消更新
const cancelUpdate = (): void => {
  show.value = false
}
</script>

<template>
  <v-dialog v-model="show" width="900" persistent>
    <v-card v-if="cardDiff" rounded="xl" elevation="0" class="overflow-hidden" style="max-height: 80vh; display: flex; flex-direction: column;">
      <!-- 头部背景 -->
      <div class="header-bg pa-4 pb-6 position-relative">
        <v-btn
          icon="mdi-close"
          variant="text"
          color="white"
          class="position-absolute close-btn"
          @click="cancelUpdate"
        ></v-btn>

        <div class="text-white">
          <h2 class="text-h5 font-weight-bold mb-2">
            卡片内容更新
          </h2>

          <p class="text-body-1 opacity-90 mb-0 font-weight-light">
            检测到卡片内容有新版本，请确认是否接受更新
          </p>
        </div>
      </div>

      <!-- 卡片内容 - 可滚动区域 -->
      <div class="flex-grow-1" style="overflow-y: auto;">
        <div class="px-6 pt-6 pb-6">
          <!-- 解释说明 -->
          <div class="mb-4 pa-3">
            <div class="d-flex align-center">
              <v-icon icon="mdi-information" size="16" color="grey-lighten-1" class="mr-2"></v-icon>
              <span class="text-body-2 text-grey-lighten-1">
                卡片内容已更新，与您学习记录中的版本不同。您可以选择接受更新以获取最新内容。
              </span>
            </div>
          </div>

          <!-- 差异对比卡片 -->
          <v-card
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
                      卡片内容对比
                    </h4>
                    <v-chip
                      size="small"
                      color="primary"
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
                      <span class="text-caption text-grey-darken-1">最新内容</span>
                    </div>

                    <div class="question-section mb-3">
                      <div class="d-flex align-center mb-2">
                        <v-icon icon="mdi-help-circle" color="primary" size="20" class="mr-2"></v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-primary">问题</span>
                      </div>
                      <div class="question-content pa-3 bg-blue-lighten-5 rounded-lg">
                        <p class="text-body-1 mb-0 text-grey-darken-3">{{ cardDiff.newVersion.front }}</p>
                      </div>
                    </div>

                    <div class="answer-section">
                      <div class="d-flex align-center mb-2">
                        <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                      </div>
                      <div class="answer-content pa-3 bg-green-lighten-5 rounded-lg">
                        <p class="text-body-1 mb-0 text-grey-darken-3">{{ cardDiff.newVersion.back }}</p>
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
                        <p class="text-body-1 mb-0 text-grey-darken-2">{{ cardDiff.oldVersion.front }}</p>
                      </div>
                    </div>

                    <div class="answer-section">
                      <div class="d-flex align-center mb-2">
                        <v-icon icon="mdi-lightbulb" color="success" size="20" class="mr-2"></v-icon>
                        <span class="text-subtitle-2 font-weight-bold text-success">答案</span>
                      </div>
                      <div class="answer-content pa-3 bg-grey-lighten-4 rounded-lg">
                        <p class="text-body-1 mb-0 text-grey-darken-2">{{ cardDiff.oldVersion.back }}</p>
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
                    prepend-icon="mdi-check"
                    class="action-btn"
                    @click="acceptUpdate"
                  >
                    接受更新
                  </v-btn>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>

      <!-- 底部固定操作栏 -->
      <div class="bottom-actions pa-6 bg-grey-lighten-5 d-flex align-center justify-space-between" style="flex-shrink: 0;">
        <div class="text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-information" size="16" class="mr-1"></v-icon>
          接受更新将使用最新版本的内容覆盖您当前学习的版本
        </div>

        <div class="d-flex" style="gap: 12px;">
          <v-btn
            variant="text"
            rounded="xl"
            size="large"
            @click="cancelUpdate"
          >
            关闭
          </v-btn>

          <v-btn
            color="primary"
            variant="flat"
            rounded="xl"
            size="large"
            prepend-icon="mdi-check"
            @click="acceptUpdate"
          >
            接受更新
          </v-btn>
        </div>
      </div>
    </v-card>
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

/* 外层对话框移除边框，内容卡片保留边框 */
.v-dialog > .v-card {
  border: none !important;
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