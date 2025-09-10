<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { DeckUpdateDiff, CardDiff, CardDiffType } from '@/types/memoryCard'
import { CardDiffType as DiffType } from '@/types/memoryCard'

interface Props {
  modelValue: boolean
  deckDiff: DeckUpdateDiff | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'accept-update', acceptedChanges: {
    updateMeta: boolean
    cardIds: number[]
  }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const show = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 选择状态
const selectedCardIds = ref<number[]>([]) // 选中的卡片ID

// 当对话框打开时初始化选择状态
const initializeSelection = () => {
  if (!props.deckDiff) return
  
  // 默认选择所有有ID的卡片（包括新增、修改和删除的卡片）
  selectedCardIds.value = props.deckDiff.cardDiffs
    .filter(diff => diff.cardId)
    .map(diff => diff.cardId!)
}

// 获取diff类型的显示文本
const getDiffTypeText = (type: CardDiffType): string => {
  switch (type) {
    case DiffType.ADDED: return '新增'
    case DiffType.MODIFIED: return '修改'
    case DiffType.DELETED: return '删除'
    default: return '未知'
  }
}

// 获取diff类型的颜色
const getDiffTypeColor = (type: CardDiffType): string => {
  switch (type) {
    case DiffType.ADDED: return 'success'
    case DiffType.MODIFIED: return 'warning'
    case DiffType.DELETED: return 'error'
    default: return 'grey'
  }
}

// 全选/取消全选
const toggleSelectAll = (): void => {
  if (!props.deckDiff) return
  
  const availableCardIds = props.deckDiff.cardDiffs
    .filter(diff => diff.cardId)
    .map(diff => diff.cardId!)
  
  if (selectedCardIds.value.length === availableCardIds.length) {
    selectedCardIds.value = []
  } else {
    selectedCardIds.value = [...availableCardIds]
  }
}

// 接受更新
const acceptUpdate = (): void => {
  emit('accept-update', {
    updateMeta: false, // 不更新基本信息
    cardIds: selectedCardIds.value
  })
  show.value = false
}

// 取消更新
const cancelUpdate = (): void => {
  show.value = false
}

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  if (newValue && props.deckDiff) {
    initializeSelection()
  }
})
</script>

<template>
  <v-dialog 
    v-model="show" 
    width="900" 
    height="800"
    persistent
  >
    <v-card v-if="deckDiff" rounded="xl" elevation="0">
      <!-- 头部 -->
      <div class="dialog-header pa-6 bg-primary text-white">
        <div class="d-flex align-center justify-space-between">
          <div>
            <h3 class="text-h5 font-weight-bold mb-1">卡片组更新</h3>
            <p class="text-body-2 mb-0 opacity-90">
              检测到卡片组有新版本可用，请选择要应用的更新
            </p>
          </div>
          <v-btn 
            icon="mdi-close" 
            variant="text" 
            color="white" 
            @click="cancelUpdate"
          ></v-btn>
        </div>
      </div>

      <!-- 内容 -->
      <v-card-text class="pa-0">
        <div class="pa-6">


          <!-- 卡片更新列表 -->
          <div class="mb-6">
            <div class="d-flex align-center justify-space-between mb-3">
              <h4 class="text-h6 font-weight-bold text-grey-darken-4">
                <v-icon icon="mdi-cards" class="mr-2"></v-icon>
                卡片更新 ({{ deckDiff.cardDiffs.length }} 项变更)
              </h4>
              <v-btn 
                color="primary" 
                variant="tonal" 
                size="small"
                rounded="lg"
                @click="toggleSelectAll"
              >
                {{ selectedCardIds.length === deckDiff.cardDiffs.filter(d => d.cardId).length ? '取消全选' : '全选' }}
              </v-btn>
            </div>

            <div class="max-height-400 overflow-y-auto no-scrollbar">
              <v-card 
                v-for="(diff, index) in deckDiff.cardDiffs" 
                :key="index"
                flat 
                variant="outlined" 
                rounded="lg" 
                class="mb-3"
              >
                <v-card-text class="pa-4">
                  <div class="d-flex align-start">
                    <!-- 选择框 -->
                    <div class="mr-3">
                      <v-checkbox
                        v-model="selectedCardIds"
                        :value="diff.cardId"
                        :hide-details="true"
                        class="custom-checkbox"
                      ></v-checkbox>
                    </div>

                    <!-- 变更类型标识 -->
                    <div class="mr-4">
                      <v-chip
                        :color="getDiffTypeColor(diff.type)"
                        variant="flat"
                        size="small"
                        class="font-weight-bold"
                      >
                        {{ getDiffTypeText(diff.type) }}
                      </v-chip>
                    </div>

                    <!-- 内容对比 -->
                    <div class="flex-grow-1 w-75">
                      <!-- 新增卡片 -->
                      <div v-if="diff.type === 'added' && diff.newVersion">
                        <div class="text-body-2 text-grey-darken-1 mb-1">新增卡片：</div>
                        <div class="diff-content added">
                          <div class="font-weight-medium text-success mb-1">{{ diff.newVersion.front }}</div>
                          <div class="text-body-2 text-grey-darken-2">{{ diff.newVersion.back }}</div>
                        </div>
                      </div>

                      <!-- 删除卡片 -->
                      <div v-else-if="diff.type === 'deleted' && diff.oldVersion">
                        <div class="text-body-2 text-grey-darken-1 mb-1">删除卡片：</div>
                        <div class="diff-content deleted">
                          <div class="font-weight-medium text-error mb-1">{{ diff.oldVersion.front }}</div>
                          <div class="text-body-2 text-grey-darken-2">{{ diff.oldVersion.back }}</div>
                        </div>
                      </div>

                      <!-- 修改卡片 -->
                      <div v-else-if="diff.type === 'modified' && diff.oldVersion && diff.newVersion">
                        <div class="text-body-2 text-grey-darken-1 mb-2">卡片修改：</div>
                        
                        <!-- 问题对比 -->
                        <div class="mb-3">
                          <div class="text-caption text-grey-darken-1 mb-1">问题：</div>
                          <div class="diff-container">
                            <div class="diff-content deleted mb-2">
                              <span class="text-error">- </span>{{ diff.oldVersion.front }}
                            </div>
                            <div class="diff-content added">
                              <span class="text-success">+ </span>{{ diff.newVersion.front }}
                            </div>
                          </div>
                        </div>

                        <!-- 答案对比 -->
                        <div>
                          <div class="text-caption text-grey-darken-1 mb-1">答案：</div>
                          <div class="diff-container">
                            <div class="diff-content deleted mb-2">
                              <span class="text-error">- </span>{{ diff.oldVersion.back }}
                            </div>
                            <div class="diff-content added">
                              <span class="text-success">+ </span>{{ diff.newVersion.back }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </div>
      </v-card-text>

      <!-- 底部操作 -->
      <v-card-actions class="pa-6 pt-0">
        <div class="d-flex align-center justify-space-between w-100">
          <div class="text-body-2 text-grey-darken-1">
            已选择 {{ selectedCardIds.length }} 项卡片更新
          </div>
          <div class="d-flex" style="gap: 12px;">
            <v-btn 
              color="grey-darken-2" 
              variant="tonal" 
              rounded="lg"
              @click="cancelUpdate"
            >
              取消
            </v-btn>
            <v-btn 
              color="primary" 
              variant="flat" 
              rounded="lg"
              @click="acceptUpdate"
              :disabled="selectedCardIds.length === 0"
            >
              <v-icon icon="mdi-download" class="mr-2"></v-icon>
              应用更新
            </v-btn>
          </div>
        </div>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.dialog-header {
  background: rgb(var(--v-theme-primary));
}

.max-height-400 {
  max-height: 519px;
}

.custom-checkbox {
  min-height: 28px;
}

/* 隐藏滚动条 */
.no-scrollbar {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.no-scrollbar::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.checkbox-placeholder {
  width: 40px;
  height: 40px;
}

.diff-container {
  border-radius: 8px;
  overflow: hidden;
}

.diff-content {
  padding: 8px 12px;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
  line-height: 1.4;
}

.diff-content.added {
  background-color: rgba(76, 175, 80, 0.1);
  border-left: 3px solid #4caf50;
}

.diff-content.deleted {
  background-color: rgba(244, 67, 54, 0.1);
  border-left: 3px solid #f44336;
}

/* 改善字体渲染 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}
</style>