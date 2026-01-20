<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { debounce } from 'lodash-es'
import draggable from 'vuedraggable'
import { useRouter } from 'vue-router'
import { postApi } from '@/api/modules/post'
import { useMutation } from '@/composables/useMutation'
import { PostType } from '@/enums'
import type { Node } from '@/types/node'

interface Props {
  courseId?: number
  nodeId?: number // 当前节点ID，用于创建 contents post
}

const props = withDefaults(defineProps<Props>(), {
  courseId: 0,
  nodeId: 0,
})

const emit = defineEmits<{
  confirm: [nodes: Node[]]
  'load-data': []
}>()

const dialog = defineModel<boolean>({ default: false })
const router = useRouter()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 搜索相关
const searchQuery = ref('')
const searchResults = ref<Node[]>([])
const loading = ref(false)
const hasSearched = ref(false)  // 是否已执行搜索

// 创建表单
const showCreateForm = ref(false)
const newNode = ref({ name: '', description: '' })

// 相似节点提示
const similarNodes = ref<Node[]>([])
const showSimilarWarning = ref(false)

// 已选择的节点列表（右侧）
const selectedNodes = ref<Node[]>([])

// 使用 useMutation 提交目录
const { execute: executeSubmit, loading: submitting } = useMutation(
  async () => {
    // 构建 JSON 格式
    const jsonContent = selectedNodes.value.map((node) => {
      const obj: any = {
        name: node.name,
        description: node.description,
      }
      // 如果节点有真实 ID（不是临时ID），则使用现有节点
      if (node.id && node.id < Date.now() - 1000000) {
        obj.id = node.id
      }
      return obj
    })

    // 调用创建 contents 类型帖子的接口
    return await postApi.createPost({
      nodeId: props.nodeId!,
      type: PostType.CONTENTS,
      content: JSON.stringify(jsonContent),
    })
  },
  {
    onSuccess: () => {
      showSnackbar?.('目录添加成功', 'success')
      emit('load-data')
      close()
    },
    onError: (error) => {
      console.error('添加目录失败:', error)
      showSnackbar?.('添加目录失败', 'error')
    },
  }
)

const canConfirm = computed(() => selectedNodes.value.length >= 2)

// 检测相似节点（防抖）
const checkSimilarNodes = debounce(async (name: string) => {
  if (!name || name.length < 2) {
    similarNodes.value = []
    showSimilarWarning.value = false
    return
  }

  try {
    const response = await postApi.searchSimilarNodes(name, 5)
    similarNodes.value = response.data || []
    showSimilarWarning.value = similarNodes.value.length > 0
  } catch (error) {
    console.error('检测相似节点失败', error)
    similarNodes.value = []
    showSimilarWarning.value = false
  }
}, 500)

// 搜索节点
const handleSearch = async () => {
  const query = searchQuery.value.trim()
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

// 添加新创建的节点
const addNewNode = () => {
  if (!newNode.value.name || !newNode.value.description || newNode.value.description.length < 20) {
    return
  }

  const node: Node = {
    id: Date.now(),
    name: newNode.value.name,
    description: newNode.value.description,
  }
  selectedNodes.value.push(node)
  newNode.value = { name: '', description: '' }
  similarNodes.value = []
  showSimilarWarning.value = false
}

// 直接使用相似节点
const useSimilarNode = (node: Node) => {
  addNode(node)
  newNode.value = { name: '', description: '' }
  similarNodes.value = []
  showSimilarWarning.value = false
}

// 删除节点
const removeNode = (index: number) => {
  selectedNodes.value.splice(index, 1)
}

// 确认
const confirm = async () => {
  if (!canConfirm.value) return

  if (!props.nodeId) {
    showSnackbar?.('缺少节点ID', 'error')
    return
  }

  await executeSubmit()
}

const close = () => {
  dialog.value = false
  setTimeout(() => {
    searchQuery.value = ''
    searchResults.value = []
    hasSearched.value = false
    selectedNodes.value = []
    showCreateForm.value = false
    newNode.value = { name: '', description: '' }
  }, 300)
}

const open = () => {
  dialog.value = true
}

defineExpose({ open })
</script>

<template>
  <v-dialog v-model="dialog" width="1100" persistent>
    <v-card rounded="xl">
      <v-card-title class="pa-4 d-flex align-center justify-space-between border-b">
        <div class="d-flex align-center">
          <v-icon icon="mdi-format-list-group-plus" color="primary" class="mr-2" />
          <span class="text-h6 font-weight-bold">添加目录</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="close" />
      </v-card-title>

      <div class="dialog-body">
        <div class="body-row">
          <!-- 左侧：搜索/创建 -->
          <div :class="['left-panel', { 'full-width': selectedNodes.length === 0 }]">
            <!-- 创建模式标题 -->
            <div v-if="showCreateForm" class="panel-header">
              <span class="text-h6 font-weight-medium">创建新节点</span>
              <v-btn
                variant="text"
                size="small"
                prepend-icon="mdi-arrow-left"
                class="ml-4"
                @click="showCreateForm = false"
              >
                返回搜索
              </v-btn>
            </div>

            <!-- 搜索模式 -->
            <div v-if="!showCreateForm" class="search-panel">
              <div class="search-header">
                <div class="text-body-2 text-grey mb-3">
                  请输入想要创建的节点描述，系统会查找最相似的已存在节点
                </div>
                <v-text-field
                  v-model="searchQuery"
                  placeholder="搜索节点..."
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  @keyup.enter="handleSearch"
                >
                  <template #append-inner>
                    <v-icon
                      icon="mdi-magnify"
                      class="cursor-pointer"
                      @click="handleSearch"
                    />
                  </template>
                </v-text-field>
              </div>

              <div class="search-results-container">
                <div v-if="loading" class="text-center py-8">
                  <v-progress-circular indeterminate />
                </div>

                <div v-else-if="hasSearched && searchResults.length > 0">
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
                              已验证
                            </v-chip>
                            <v-chip
                              v-if="node.similarityScore !== undefined"
                              size="x-small"
                              color="primary"
                              variant="tonal"
                              class="ml-2"
                            >
                              相似度：{{ Math.round(node.similarityScore * 100) }}%
                            </v-chip>
                            <v-chip
                              v-if="node.nodeReferenceCount !== undefined && node.nodeReferenceCount > 0"
                              size="x-small"
                              color="grey"
                              variant="tonal"
                              class="ml-2"
                            >
                              <v-icon icon="mdi-link-variant" size="12" class="mr-1"></v-icon>
                              {{ node.nodeReferenceCount }}
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
                          添加
                        </v-btn>
                        <v-btn
                          v-else
                          size="small"
                          color="grey"
                          variant="flat"
                          class="ml-3"
                          disabled
                        >
                          已添加
                        </v-btn>
                      </div>
                    </v-card-text>
                  </v-card>
                </div>

                <div
                  v-else-if="hasSearched && searchResults.length === 0"
                  class="text-center text-grey py-8"
                >
                  没有找到匹配的节点
                </div>
              </div>

              <div v-if="hasSearched" class="search-footer">
                <span class="text-grey text-body-2">没有合适的节点？</span>
                <a class="text-primary cursor-pointer ml-1 text-body-2" @click="showCreateForm = true">
                  点击创建新节点
                </a>
              </div>
            </div>

            <!-- 创建模式 -->
            <div v-else class="create-panel">
              <v-text-field
                v-model="newNode.name"
                label="节点名称"
                variant="outlined"
                density="comfortable"
                hide-details
                class="mb-3 mt-2"
                @update:model-value="checkSimilarNodes"
              />

              <v-expand-transition>
                <v-alert
                  v-if="showSimilarWarning && similarNodes.length > 0"
                  type="warning"
                  variant="tonal"
                  density="compact"
                  class="mb-3"
                >
                  <div class="text-body-2">
                    <strong>💡 发现相似的节点</strong>
                    <p class="mb-2 mt-1">建议优先使用现有节点，避免知识碎片化</p>

                    <div>
                      <v-card
                        v-for="node in similarNodes.slice(0, 3)"
                        :key="node.id"
                        class="mb-2"
                        variant="outlined"
                        hover
                        @click="useSimilarNode(node)"
                      >
                        <v-card-text class="pa-2">
                          <div class="d-flex align-center justify-space-between">
                            <div class="flex-grow-1">
                              <div class="text-body-2 font-weight-medium">{{ node.name }}</div>
                              <div class="text-caption text-grey">{{ node.description?.slice(0, 50) }}...</div>
                            </div>
                            <v-btn
                              size="x-small"
                              color="success"
                              variant="tonal"
                              @click.stop="useSimilarNode(node)"
                            >
                              使用
                            </v-btn>
                          </div>
                        </v-card-text>
                      </v-card>
                    </div>

                    <div class="text-caption text-grey mt-2">
                      如果这些节点都不合适，你仍然可以创建新节点
                    </div>
                  </div>
                </v-alert>
              </v-expand-transition>

              <v-textarea
                v-model="newNode.description"
                label="节点描述（必填，至少20字）"
                placeholder="节点描述很重要，清晰的描述能帮助其他人找到并复用这个节点"
                variant="outlined"
                density="comfortable"
                rows="3"
                hide-details
                class="mb-3 mt-6"
              />

              <v-btn
                color="primary"
                variant="tonal"
                class="mt-3"
                :disabled="
                  !newNode.name.trim() ||
                  !newNode.description.trim() ||
                  newNode.description.length < 20
                "
                @click="addNewNode"
              >
                <v-icon start>mdi-plus</v-icon>
                {{ showSimilarWarning ? '仍要创建新节点' : '添加到列表' }}
              </v-btn>
            </div>
          </div>

          <!-- 右侧：目录预览 -->
          <div v-if="selectedNodes.length > 0" class="right-panel">
            <div class="panel-header-simple">
              目录预览 ({{ selectedNodes.length }})
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

      <v-card-actions class="pa-4 border-t">
        <v-spacer />
        <v-btn variant="text" @click="close">取消</v-btn>
        <v-btn
          color="primary"
          :disabled="!canConfirm"
          :loading="submitting"
          @click="confirm"
        >
          提交目录{{ selectedNodes.length < 2 ? ' (至少需要2个)' : '' }}
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
