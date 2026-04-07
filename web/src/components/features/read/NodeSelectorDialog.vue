<script setup lang="ts">
import { ref, computed, inject, watch } from 'vue'
import draggable from 'vuedraggable'
import { useRouter } from 'vue-router'
import { postApi } from '@/api/modules/post'
import { useMutation } from '@/composables/useMutation'
import { PostType } from '@/enums'
import type { Node } from '@/types/node'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

interface Props {
  courseId?: number
  nodeId?: number // 当前节点ID，用于创建 index post
  draftPost?: any // 已有的草稿post，用于编辑模式
}

const props = withDefaults(defineProps<Props>(), {
  courseId: 0,
  nodeId: 0,
  draftPost: null,
})

const emit = defineEmits<{
  confirm: [nodes: Node[]]
  'load-data': []
}>()

const dialog = defineModel<boolean>({ default: false })
const router = useRouter()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 搜索相关
const searchResults = ref<Node[]>([])
const loading = ref(false)
const hasSearched = ref(false)  // 是否已执行搜索

// 创建表单
const newNode = ref({ name: '', description: '' })

// 已选择的节点列表（右侧）
const selectedNodes = ref<Node[]>([])

// 当前编辑的草稿（用于创建模式转编辑模式）
const currentDraft = ref<any>(null)

// 编辑模式标志
const isEditMode = computed(() => props.draftPost != null || currentDraft.value != null)

// 获取当前草稿对象
const getCurrentDraft = computed(() => props.draftPost || currentDraft.value)

// 构建 JSON 内容
const buildJsonContent = () => {
  return selectedNodes.value.map((node) => {
    // 如果是已有节点（真实ID），只传id
    if (node.id && node.id < Date.now() - 1000000) {
      return { id: node.id }
    }
    // 如果是新节点（临时ID），传name和description
    return {
      name: node.name,
      description: node.description,
    }
  })
}

// 保存草稿
const { execute: executeSaveDraft, loading: savingDraft } = useMutation(
  async () => {
    const jsonContent = buildJsonContent()

    if (isEditMode.value) {
      // 编辑模式：更新现有草稿
      const draft = getCurrentDraft.value
      return await postApi.updatePost(draft.id, {
        content: JSON.stringify(jsonContent),
      })
    } else {
      // 创建模式：创建新草稿
      return await postApi.createPost({
        nodeId: props.nodeId!,
        type: PostType.INDEX,
        content: JSON.stringify(jsonContent),
        state: 0, // DRAFT
      })
    }
  },
  {
    onSuccess: (response) => {
      showSnackbar?.(t('nodeSelector.draftSaved'), 'success')

      // 如果是创建模式，保存返回的草稿信息，转为编辑模式
      if (!isEditMode.value && response.data) {
        currentDraft.value = response.data
      }

      // 不关闭对话框，不刷新列表
    },
    onError: (error) => {
      console.error('保存草稿失败:', error)
      showSnackbar?.(t('nodeSelector.draftSaveFailed'), 'error')
    },
  }
)

// 提交审核
const { execute: executeSubmit, loading: submitting } = useMutation(
  async () => {
    const jsonContent = buildJsonContent()

    if (isEditMode.value) {
      // 编辑模式：更新内容并提交审核
      const draft = getCurrentDraft.value
      return await postApi.updatePost(draft.id, {
        content: JSON.stringify(jsonContent),
        state: 1, // SUBMITTED
      })
    } else {
      // 创建模式：直接创建并提交审核
      return await postApi.createPost({
        nodeId: props.nodeId!,
        type: PostType.INDEX,
        content: JSON.stringify(jsonContent),
        state: 1, // SUBMITTED
      })
    }
  },
  {
    onSuccess: () => {
      showSnackbar?.(isEditMode.value ? t('nodeSelector.submitUpdated') : t('nodeSelector.submitSuccess'), 'success')

      // 只有编辑模式（从个人页进入）才需要刷新列表
      // 创建模式（从 read 页进入）不需要刷新
      if (props.draftPost) {
        emit('load-data')
      }

      close()
    },
    onError: (error) => {
      console.error('提交目录失败:', error)
      showSnackbar?.(t('nodeSelector.submitFailed'), 'error')
    },
  }
)

const canConfirm = computed(() => selectedNodes.value.length >= 2)
const isSubmitting = computed(() => savingDraft.value || submitting.value)

// 监听输入框变化，清空搜索结果
watch(() => newNode.value.name, () => {
  searchResults.value = []
  hasSearched.value = false
})

watch(() => newNode.value.description, () => {
  searchResults.value = []
  hasSearched.value = false
})

// 搜索节点
const handleSearch = async () => {
  // 使用节点名称和描述进行搜索
  const query = `${newNode.value.name} ${newNode.value.description}`.trim()
  if (!query || query.length < 2) {
    searchResults.value = []
    hasSearched.value = false
    return
  }

  loading.value = true
  hasSearched.value = true
  try {
    const response = await postApi.searchSimilarNodes(query, 20)
    searchResults.value = response.data || []
  } catch (error) {
    console.error('搜索节点失败', error)
    searchResults.value = []
  } finally {
    loading.value = false
  }
}

// 检查节点是否已添加
const isNodeAdded = (nodeId: number) => {
  return selectedNodes.value.some((n) => n.id === nodeId)
}

// 跳转到节点页面（新窗口）
const goToNode = (nodeId: number) => {
  const url = router.resolve({ path: '/read', query: { nodeId: nodeId.toString() } }).href
  window.open(url, '_blank')
}

// 添加节点到列表
const addNode = (node: Node, event?: Event) => {
  if (event) {
    event.stopPropagation()
  }
  if (selectedNodes.value.find((n) => n.id === node.id)) {
    return
  }
  selectedNodes.value.push(node)
}

// 添加新创建的节点到列表
const addNewNode = async () => {
  if (!newNode.value.name || !newNode.value.description || newNode.value.description.length < 20) {
    return
  }

  // 检查课程内是否有同名已发布节点
  if (props.courseId) {
    try {
      const response = await postApi.checkDuplicateNode(props.courseId, newNode.value.name)
      if (response.data === true) {
        showSnackbar?.(
          `${t('nodeSelector.nodeExists', { name: newNode.value.name })}`,
          'error'
        )
        return
      }
    } catch (error) {
      console.error('检查节点重名失败', error)
      showSnackbar?.(t('nodeSelector.checkDuplicateFailed'), 'error')
      return
    }
  }

  const node: Node = {
    id: Date.now(),
    name: newNode.value.name,
    description: newNode.value.description,
  }
  selectedNodes.value.push(node)
  newNode.value = { name: '', description: '' }
  searchResults.value = []
  hasSearched.value = false
}

// 删除节点
const removeNode = (index: number) => {
  selectedNodes.value.splice(index, 1)
}

const close = () => {
  dialog.value = false
  setTimeout(() => {
    searchResults.value = []
    hasSearched.value = false
    selectedNodes.value = []
    newNode.value = { name: '', description: '' }
    currentDraft.value = null // 清空当前草稿
  }, 300)
}

const open = () => {
  dialog.value = true

  // 加载草稿的节点列表（优先使用 currentDraft，其次是 props.draftPost）
  const draft = getCurrentDraft.value
  if (draft && draft.content) {
    try {
      const nodes = JSON.parse(draft.content)
      selectedNodes.value = nodes.map((node: any) => ({
        id: node.id || Date.now() + Math.random(), // 临时ID
        name: node.name,
        description: node.description,
      }))
    } catch (e) {
      console.error('解析草稿内容失败:', e)
    }
  }
}

defineExpose({ open })
</script>

<template>
  <v-dialog v-model="dialog" width="1100" persistent>
    <v-card rounded="xl">
      <v-card-title class="pa-4 d-flex align-center justify-space-between border-b">
        <div class="d-flex align-center">
          <v-icon icon="mdi-format-list-group-plus" color="primary" class="mr-2" />
          <span class="text-h6 font-weight-bold">{{ isEditMode ? t('nodeSelector.editDraft') : t('nodeSelector.addCatalog') }}</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="close" />
      </v-card-title>

      <div class="dialog-body">
        <div class="body-row">
          <!-- 左侧：搜索/创建合并 -->
          <div :class="['left-panel', { 'full-width': selectedNodes.length === 0 }]">
            <div class="search-panel">
              <div class="search-header">
                <div class="text-body-2 text-grey mb-3">
                  {{ t('nodeSelector.hint') }}
                </div>

                <!-- 节点名称 -->
                <v-text-field
                  v-model="newNode.name"
                  :label="t('nodeSelector.nodeName')"
                  :placeholder="t('nodeSelector.nodeNamePlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  counter="50"
                  :hint="t('nodeSelector.inputCount', { count: newNode.name.length })"
                  class="mb-3"
                />

                <!-- 节点描述 -->
                <v-textarea
                  v-model="newNode.description"
                  :label="t('nodeSelector.nodeDesc')"
                  :placeholder="t('nodeSelector.nodeDescPlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  rows="3"
                  counter="500"
                  :hint="t('nodeSelector.descCount', { count: newNode.description.length })"
                  class="mb-3"
                />

                <!-- 搜索按钮 -->
                <div class="d-flex ga-2">
                  <v-btn
                    color="primary"
                    variant="tonal"
                    :disabled="!newNode.name.trim() || !newNode.description.trim() || newNode.description.length < 20"
                    @click="handleSearch"
                  >
                    <v-icon start>mdi-magnify</v-icon>
                    {{ t('nodeSelector.searchExisting') }}
                  </v-btn>

                  <!-- 创建新节点按钮 - 只在有搜索结果时显示 -->
                  <v-btn
                    v-if="hasSearched && searchResults.length > 0"
                    color="grey-darken-1"
                    variant="outlined"
                    @click="addNewNode"
                  >
                    <v-icon start>mdi-plus</v-icon>
                    {{ t('nodeSelector.createNew') }}
                  </v-btn>
                </div>
              </div>

              <div class="search-results-container">
                <div v-if="loading" class="text-center py-8">
                  <v-progress-circular indeterminate />
                </div>

                <div v-else-if="hasSearched && searchResults.length > 0">
                  <div class="text-body-2 text-grey-darken-1 font-weight-medium mb-2 mt-4">
                    {{ t('nodeSelector.foundNodes', { count: searchResults.length }) }}
                  </div>
                  <v-card
                    v-for="node in searchResults"
                    :key="node.id"
                    class="result-card mb-2"
                    variant="outlined"
                    hover
                    @click="goToNode(node.id)"
                  >
                    <v-card-text class="pa-3">
                      <div class="d-flex align-center justify-space-between">
                        <div class="flex-grow-1">
                          <div class="d-flex align-center mb-1">
                            <span class="text-body-1 font-weight-medium">{{ node.name }}</span>
                            <v-chip size="x-small" color="success" variant="tonal" class="ml-2">
                              {{ t('nodeSelector.verified') }}
                            </v-chip>
                            <v-chip
                              v-if="node.similarityScore !== undefined"
                              size="x-small"
                              color="primary"
                              variant="tonal"
                              class="ml-2"
                            >
                              {{ t('nodeSelector.similarity', { score: Math.round(node.similarityScore * 100) }) }}
                            </v-chip>
                            <v-chip
                              v-if="node.nodeReferenceCount !== undefined && node.nodeReferenceCount > 0"
                              size="x-small"
                              color="grey"
                              variant="tonal"
                              class="ml-2"
                            >
                              {{ t('nodeSelector.refCount', { count: node.nodeReferenceCount }) }}
                            </v-chip>
                          </div>
                          <div class="text-caption text-grey">{{ node.description }}</div>
                        </div>
                        <v-btn
                          v-if="!isNodeAdded(node.id)"
                          size="small"
                          color="primary"
                          variant="tonal"
                          class="ml-3"
                          @click.stop="addNode(node, $event)"
                        >
                          {{ t('common.add') }}
                        </v-btn>
                        <v-btn
                          v-else
                          size="small"
                          color="grey"
                          variant="flat"
                          class="ml-3"
                          disabled
                        >
                          {{ t('nodeSelector.added') }}
                        </v-btn>
                      </div>
                    </v-card-text>
                  </v-card>
                </div>

                <div
                  v-else-if="hasSearched && searchResults.length === 0"
                  class="text-center py-6"
                >
                  <v-icon icon="mdi-file-search-outline" size="48" color="grey-lighten-1" class="mb-2" />
                  <p class="text-body-2 text-grey">{{ t('nodeSelector.noResults') }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：目录预览 -->
          <div v-if="selectedNodes.length > 0" class="right-panel">
            <div class="panel-header-simple">
              {{ t('nodeSelector.preview', { count: selectedNodes.length }) }}
            </div>

            <div class="catalog-list">
              <draggable
                v-model="selectedNodes"
                item-key="id"
                handle=".drag-handle"
                animation="200"
              >
                <template #item="{ element, index }">
                  <div class="catalog-item">
                    <div class="drag-handle">
                      <v-icon icon="mdi-drag" size="18" color="grey" />
                    </div>

                    <div class="catalog-content">
                      <div class="catalog-title">
                        {{ index + 1 }}. {{ element.name }}
                      </div>
                      <div class="catalog-desc">{{ element.description }}</div>
                    </div>

                    <v-btn
                      icon="mdi-close"
                      variant="text"
                      size="x-small"
                      color="grey"
                      @click="removeNode(index)"
                    />
                  </div>
                </template>
              </draggable>
            </div>
          </div>
        </div>
      </div>

      <v-card-actions class="pa-4 border-t d-flex ga-3">
        <v-spacer />
        <v-btn variant="tonal" color="grey" class="px-6" @click="close">{{ t('common.cancel') }}</v-btn>
        <v-btn
          variant="tonal"
          color="grey-darken-1"
          class="px-6"
          :disabled="!canConfirm"
          :loading="savingDraft"
          @click="executeSaveDraft"
        >
          <v-icon start>mdi-content-save-outline</v-icon>
          {{ t('nodeSelector.saveDraft') }}
        </v-btn>
        <v-btn
          variant="tonal"
          color="primary"
          class="px-6"
          :disabled="!canConfirm"
          :loading="submitting"
          @click="executeSubmit"
        >
          <v-icon start>mdi-send</v-icon>
          {{ selectedNodes.length < 2 ? t('nodeSelector.submitWithMin') : t('nodeSelector.submit') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.border-b {
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.border-t {
  border-top: 1px solid rgb(var(--v-theme-border));
}

.cursor-pointer {
  cursor: pointer;
}

/* Dialog body */
.dialog-body {
  height: 600px;
  overflow: hidden;
}

.body-row {
  display: flex;
  height: 100%;
}

/* 左侧面板 */
.left-panel {
  width: 50%;
  padding: 16px;
  border-right: 1px solid rgb(var(--v-theme-border));
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-panel.full-width {
  width: 100%;
  border-right: none;
}

/* 右侧面板 */
.right-panel {
  width: 50%;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 面板头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-space-between: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.panel-header-simple {
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 12px;
  flex-shrink: 0;
}

/* 搜索面板 */
.search-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.search-header {
  flex-shrink: 0;
}

.search-results-container {
  flex: 1;
  overflow-y: auto;
  margin-top: 16px;
  margin-bottom: 12px;
  min-height: 0;
  padding-right: 6px;
}

.search-footer {
  flex-shrink: 0;
  text-align: center;
}

/* 搜索结果卡片 */
.result-card {
  cursor: pointer;
}

.result-card:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

/* 创建面板 */
.create-panel {
  overflow-y: auto;
  padding-right: 6px;
}

/* 目录列表 */
.catalog-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 6px;
}

.catalog-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 8px;
  background-color: white;
  transition: all 0.2s;
}

.catalog-item:hover {
  background-color: #f6f7f8;
}

.drag-handle {
  cursor: move;
  padding: 4px;
  margin-right: 8px;
  flex-shrink: 0;
}

.drag-handle:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.catalog-content {
  flex: 1;
  min-width: 0;
}

.catalog-title {
  font-size: 0.875rem;
  font-weight: 500;
}

.catalog-desc {
  font-size: 0.75rem;
  color: rgb(var(--v-theme-on-surface-variant));
  margin-top: 4px;
}

/* 美化滚动条 */
.search-results-container::-webkit-scrollbar,
.catalog-list::-webkit-scrollbar,
.create-panel::-webkit-scrollbar {
  width: 4px;
}

.search-results-container::-webkit-scrollbar-track,
.catalog-list::-webkit-scrollbar-track,
.create-panel::-webkit-scrollbar-track {
  background: transparent;
}

.search-results-container::-webkit-scrollbar-thumb,
.catalog-list::-webkit-scrollbar-thumb,
.create-panel::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.search-results-container::-webkit-scrollbar-thumb:hover,
.catalog-list::-webkit-scrollbar-thumb:hover,
.create-panel::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.15);
}
</style>
