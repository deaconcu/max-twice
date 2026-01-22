<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { useRoute } from 'vue-router'
import draggable from 'vuedraggable'
import { postApi } from '@/api'
import { useMutation } from '@/composables/useMutation'
import { PostType } from '@/enums'

interface Props {
  nodeId?: number
  pathText?: string
}

const props = withDefaults(defineProps<Props>(), {
  nodeId: 0,
  pathText: '',
})

const emit = defineEmits<Emits>()

type Emits = (e: 'load-data', data: any[]) => void

const route = useRoute()
const dialog = defineModel<boolean>({ default: false })
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

interface CatalogItem {
  id?: number // 可选的节点ID，用于选择已有节点
  name: string
  description: string
}

const createTab = ref<string>('one')
const catalogName = ref('')
const catalogDescription = ref('')
const catalogItems = ref<CatalogItem[]>([])
const aiGenerating = ref(false)

// 监听路由变化，清空数据
watch(
  () => route.query.path,
  () => {
    catalogItems.value = []
  }
)

// 添加目录项
const addCatalogItem = () => {
  if (catalogName.value.trim() !== '' && catalogDescription.value.trim() !== '') {
    catalogItems.value.push({
      name: catalogName.value.trim(),
      description: catalogDescription.value.trim(),
    })
    catalogName.value = ''
    catalogDescription.value = ''
  }
}

// 删除目录项
const removeCatalogItem = (index: number) => {
  catalogItems.value.splice(index, 1)
}

// AI 生成目录（Mock）
const generateWithAI = () => {
  aiGenerating.value = true

  setTimeout(() => {
    catalogItems.value = [
      { name: '基础概念', description: '了解核心概念和基本原理' },
      { name: '实践应用', description: '通过实例学习具体应用' },
      { name: '进阶技巧', description: '掌握高级使用技巧' },
      { name: '常见问题', description: '解决开发中的常见问题' },
    ]
    aiGenerating.value = false
  }, 1000)
}

// 使用 useMutation 提交目录
const { execute: executeSubmit, loading: submitting } = useMutation(
  async () => {
    // 构建 JSON 格式
    // 如果有 id，格式：{"id": 123, "name": "节点名", "description": "描述"}
    // 如果没有 id，格式：{"name": "节点名", "description": "描述"}
    const jsonContent = catalogItems.value.map((item) => {
      const obj: any = {
        name: item.name,
        description: item.description,
      }
      if (item.id) {
        obj.id = item.id
      }
      return obj
    })

    // 调用创建 contents 类型帖子的接口
    const response = await postApi.createPost({
      nodeId: props.nodeId!,
      type: PostType.INDEX,
      content: JSON.stringify(jsonContent),
    })

    return response
  },
  {
    onSuccess: () => {
      showSnackbar?.('目录添加成功', 'success')
      dialog.value = false
      emit('load-data', [])

      // 清空数据
      catalogItems.value = []
      catalogName.value = ''
      catalogDescription.value = ''
    },
    onError: (error) => {
      console.error('添加目录失败:', error)
      showSnackbar?.('添加目录失败', 'error')
    },
  }
)

// 提交目录
const submitCatalog = async () => {
  if (catalogItems.value.length < 2) {
    showSnackbar?.('目录至少需要2个子目录', 'warning')
    return
  }

  if (!props.nodeId) {
    showSnackbar?.('缺少节点ID', 'error')
    return
  }

  await executeSubmit()
}
</script>

<template>
  <v-dialog v-model="dialog" width="1100" persistent>
    <v-card rounded="xl">
      <!-- 头部 -->
      <v-card-title class="pa-4 d-flex align-center justify-space-between border-b">
        <div class="d-flex align-center">
          <v-icon icon="mdi-format-list-group-plus" color="primary" class="mr-2"></v-icon>
          <span class="text-h6 font-weight-bold">添加目录</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="dialog = false"></v-btn>
      </v-card-title>

      <!-- 内容区 -->
      <v-row class="ma-0" style="height: 600px">
        <!-- 左侧：输入区域 -->
        <v-col cols="6" class="pa-3 border-e">
          <v-card-text class="pa-4">
            <v-tabs v-model="createTab" density="compact" color="primary">
              <v-tab value="one" class="text-body-2">创建节点</v-tab>
              <v-tab value="two" class="text-body-2">选择现有</v-tab>
              <v-tab value="three" class="text-body-2">我的节点</v-tab>
            </v-tabs>

            <v-tabs-window v-model="createTab" class="mt-6">
              <!-- Tab 1: 创建节点 -->
              <v-tabs-window-item value="one">
                <v-text-field
                  v-model="catalogName"
                  label="节点名称"
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  class="my-3"
                ></v-text-field>

                <v-textarea
                  v-model="catalogDescription"
                  label="节点描述（必填）"
                  variant="outlined"
                  density="comfortable"
                  rows="3"
                  hide-details
                  class="mb-4"
                ></v-textarea>

                <div class="d-flex gap-2">
                  <v-btn
                    variant="tonal"
                    color="primary"
                    density="comfortable"
                    :disabled="!catalogName.trim() || !catalogDescription.trim()"
                    @click="addCatalogItem"
                  >
                    <v-icon icon="mdi-plus" size="18" class="mr-1"></v-icon>
                    添加
                  </v-btn>

                  <v-btn
                    variant="text"
                    color="grey-darken-2"
                    density="comfortable"
                    :loading="aiGenerating"
                    class="ms-4"
                    @click="generateWithAI"
                  >
                    AI 生成
                  </v-btn>
                </div>
              </v-tabs-window-item>

              <!-- Tab 2: 选择现有 -->
              <v-tabs-window-item value="two">
                <div class="text-body-2 text-grey pa-4 text-center">功能开发中...</div>
              </v-tabs-window-item>

              <!-- Tab 3: 我的节点 -->
              <v-tabs-window-item value="three">
                <div class="text-body-2 text-grey pa-4 text-center">功能开发中...</div>
              </v-tabs-window-item>
            </v-tabs-window>
          </v-card-text>
        </v-col>

        <!-- 右侧：目录预览 -->
        <v-col cols="6" class="pa-3">
          <div class="preview-container pa-4">
            <div class="text-body-2 font-weight-medium text-grey-darken-3 mb-3">
              目录预览 ({{ catalogItems.length }})
            </div>

            <!-- 可拖拽列表 -->
            <div class="catalog-list">
              <draggable
                v-model="catalogItems"
                item-key="name"
                handle=".drag-handle"
                animation="200"
                class="draggable-container"
              >
                <template #item="{ element, index }">
                  <div class="catalog-item">
                    <div class="d-flex align-start">
                      <!-- 拖拽手柄 -->
                      <div class="drag-handle">
                        <v-icon icon="mdi-drag" size="18" color="grey"></v-icon>
                      </div>

                      <!-- 内容 -->
                      <div class="flex-grow-1">
                        <div class="text-body-2 font-weight-medium text-grey-darken-3">
                          {{ index + 1 }}. {{ element.name }}
                        </div>
                        <div class="text-caption text-grey-darken-1 mt-1">
                          {{ element.description }}
                        </div>
                      </div>

                      <!-- 删除按钮 -->
                      <v-btn
                        icon="mdi-close"
                        variant="text"
                        size="x-small"
                        color="grey"
                        @click="removeCatalogItem(index)"
                      ></v-btn>
                    </div>
                  </div>
                </template>
              </draggable>

              <!-- 空状态 -->
              <div v-if="catalogItems.length === 0" class="text-body-2 text-grey text-center py-8">
                暂无目录项，请在左侧添加
              </div>
            </div>
          </div>
        </v-col>
      </v-row>

      <!-- 底部操作 -->
      <v-card-actions class="pa-4 border-t d-flex justify-center">
        <v-btn
          variant="text"
          color="grey-darken-2"
          density="default"
          class="px-4"
          @click="dialog = false"
        >
          取消
        </v-btn>
        <v-btn
          variant="flat"
          color="primary"
          class="px-6"
          :loading="submitting"
          :disabled="catalogItems.length < 2"
          @click="submitCatalog"
        >
          确认添加
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.border-b {
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.border-e {
  border-right: 1px solid rgb(var(--v-theme-border));
}

.border-t {
  border-top: 1px solid rgb(var(--v-theme-border));
}

.preview-container {
  height: 100%;
  overflow-y: auto;
}

.catalog-list {
  display: flex;
  flex-direction: column;
}

.draggable-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.catalog-item {
  padding: 12px;
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 8px;
  background-color: white;
  transition: all 0.2s ease;
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
</style>
