<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { nodeServiceV1 } from '@/services/api/v1/apiServiceV1'
import { ContentState } from '@/types/enums'
import type { Node } from '@/types/node'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 当前选中的tab
const currentTab = ref<string>('pending')

// 标签配置
interface TabConfig {
  key: string
  label: string
  state: ContentState
  icon: string
  color: string
}

const tabs: TabConfig[] = [
  {
    key: 'pending',
    label: '待审核',
    state: ContentState.SUBMITTED,
    icon: 'mdi-clock-outline',
    color: 'orange'
  },
  {
    key: 'approved',
    label: '已通过',
    state: ContentState.PUBLISHED,
    icon: 'mdi-check-circle',
    color: 'green'
  },
  {
    key: 'rejected',
    label: '已拒绝',
    state: ContentState.REJECTED,
    icon: 'mdi-close-circle',
    color: 'red'
  },
  {
    key: 'banned',
    label: '已封禁',
    state: ContentState.BANNED,
    icon: 'mdi-cancel',
    color: 'grey'
  }
]

// 筛选条件
const filterNodeId = ref<number | undefined>(undefined)
const filterCourseId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)
const isFilterMode = ref<boolean>(false)

// 节点列表
const nodeList = ref<Node[]>([])
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)

// 根据当前tab获取节点列表
const getNodesByTab = async (tabKey: string, isLoadMore: boolean = false): Promise<void> => {
  if (loading.value) return

  loading.value = true

  try {
    const lastId = isLoadMore && nodeList.value.length > 0
      ? nodeList.value[nodeList.value.length - 1].id
      : undefined

    const currentTabConfig = tabs.find(tab => tab.key === tabKey)
    if (!currentTabConfig) return

    const response = await nodeServiceV1.getNodesByState(
      currentTabConfig.state,
      lastId
    )

    if (response.code === 200) {
      const newNodes = response.data as Node[]

      if (isLoadMore) {
        nodeList.value.push(...newNodes)
      } else {
        nodeList.value = newNodes
      }

      hasMore.value = newNodes.length === 20
    } else {
      showSnackbar && showSnackbar(response.message || 'error.loadFailed', 'error')
    }
  } catch (error) {
    console.error('Error loading nodes:', error)
    showSnackbar?.('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索节点（筛选模式）
const searchNodes = async (isLoadMore: boolean = false) => {
  if (!isLoadMore && !filterNodeId.value && !filterCourseId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }

  loading.value = true
  try {
    const lastId = isLoadMore && nodeList.value.length > 0
      ? nodeList.value[nodeList.value.length - 1].id
      : undefined

    const response = await nodeServiceV1.getNodesByFilter(
      filterNodeId.value,
      filterCourseId.value,
      filterCreatorId.value,
      lastId
    )

    if (response.code === 200) {
      const newNodes = response.data as Node[]

      if (isLoadMore) {
        nodeList.value.push(...newNodes)
      } else {
        nodeList.value = newNodes
      }

      hasMore.value = newNodes.length === 20
    }
  } catch (error) {
    console.error('Error searching nodes:', error)
    showSnackbar?.('搜索失败', 'error')
  } finally {
    loading.value = false
  }
}

// 应用筛选
const applyFilter = async (): Promise<void> => {
  if (!filterNodeId.value && !filterCourseId.value && !filterCreatorId.value) {
    showSnackbar?.('请至少输入一个筛选条件', 'warning')
    return
  }
  isFilterMode.value = true
  hasMore.value = true
  await searchNodes()
}

// 清除筛选
const clearFilter = async (): Promise<void> => {
  filterNodeId.value = undefined
  filterCourseId.value = undefined
  filterCreatorId.value = undefined
  isFilterMode.value = false
  hasMore.value = true
  await getNodesByTab(currentTab.value)
}

// 加载更多
const loadMore = async () => {
  if (hasMore.value && !loading.value) {
    if (isFilterMode.value) {
      await searchNodes(true)
    } else {
      await getNodesByTab(currentTab.value, true)
    }
  }
}

// 监听tab切换
const handleTabChange = async (newTab: string) => {
  if (isFilterMode.value) {
    // 如果在筛选模式下，切换tab时清除筛选
    await clearFilter()
  } else {
    hasMore.value = true
    await getNodesByTab(newTab)
  }
}

const getStateColor = (state: ContentState): string => {
  switch (state) {
    case ContentState.SUBMITTED: return 'orange-lighten-4'
    case ContentState.PUBLISHED: return 'green-lighten-4'
    case ContentState.REJECTED: return 'red-lighten-4'
    case ContentState.BANNED: return 'grey-lighten-2'
    default: return 'grey-lighten-4'
  }
}

const getStateText = (state: ContentState): string => {
  switch (state) {
    case ContentState.SUBMITTED: return '待审核'
    case ContentState.PUBLISHED: return '已通过'
    case ContentState.REJECTED: return '已拒绝'
    case ContentState.BANNED: return '已封禁'
    default: return '未知'
  }
}

const updateNodeState = async (node: Node, newState: number) => {
  try {
    const response = await nodeServiceV1.updateNodeState(node.id, newState)
    if (response.code === 200) {
      const updatedNode = response.data as Node
      // 从当前列表中移除（如果状态改变且不在筛选模式）
      if (!isFilterMode.value) {
        const index = nodeList.value.findIndex(n => n.id === node.id)
        if (index !== -1) {
          nodeList.value.splice(index, 1)
        }
      } else {
        // 筛选模式下更新状态
        const index = nodeList.value.findIndex(n => n.id === node.id)
        if (index !== -1) {
          nodeList.value[index].state = updatedNode.state
        }
      }
      showSnackbar?.(`节点状态已更新为${getStateText(newState)}`, 'success')
    } else {
      showSnackbar && showSnackbar(response.message || 'error.operationFailed', 'error')
    }
  } catch (error) {
    console.error('Error updating node state:', error)
    showSnackbar?.('更新节点状态失败', 'error')
  }
}

// 组件挂载时加载默认tab的数据
import { onMounted } from 'vue'

onMounted(() => {
  getNodesByTab(currentTab.value)
})
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-file-tree-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">节点管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">查询和管理课程节点</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon
          icon="mdi-file-tree"
          color="blue-darken-2"
          size="16"
          class="mr-1"
        ></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ nodeList.length }}</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card v-if="!isFilterMode" flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense align="center">
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterNodeId"
            type="number"
            label="节点 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterCourseId"
            type="number"
            label="课程 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterCreatorId"
            type="number"
            label="创建者 ID"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            block
            @click="applyFilter"
          >
            <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
            筛选
          </v-btn>
        </v-col>
      </v-row>
    </v-card>

    <!-- 筛选结果提示 -->
    <v-alert
      v-if="isFilterMode"
      type="info"
      color="primary"
      variant="tonal"
      class="mb-6"
      rounded="lg"
      closable
      @click:close="clearFilter"
    >
      <div class="d-flex align-center justify-space-between">
        <div>
          <span class="font-weight-medium">筛选条件：</span>
          <v-chip v-if="filterNodeId" size="small" class="mx-1">节点 ID: {{ filterNodeId }}</v-chip>
          <v-chip v-if="filterCourseId" size="small" class="mx-1">课程 ID: {{ filterCourseId }}</v-chip>
          <v-chip v-if="filterCreatorId" size="small" class="mx-1">创建者 ID: {{ filterCreatorId }}</v-chip>
        </div>
      </div>
    </v-alert>

    <!-- 状态标签 -->
    <v-tabs
      v-if="!isFilterMode"
      v-model="currentTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="handleTabChange"
    >
      <v-tab
        v-for="tab in tabs"
        :key="tab.key"
        :value="tab.key"
        class="text-none"
      >
        <v-icon
          :icon="tab.icon"
          :color="`${tab.color}-darken-1`"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <!-- 空状态 -->
    <div v-if="nodeList.length === 0 && !loading" class="text-center py-12">
      <v-icon
        icon="mdi-file-tree-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{ isFilterMode ? '未找到符合条件的节点' : `暂无${tabs.find(tab => tab.key === currentTab)?.label}的节点` }}
      </p>
    </div>

    <!-- 节点列表 -->
    <div
      v-for="node in nodeList"
      :key="node.id"
      class="mb-4"
      v-intersect="{
        handler: (isIntersecting) => {
          if (isIntersecting && node === nodeList[nodeList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        }
      }"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 status-actions-area">
            <div class="mb-3">
              <v-chip
                v-if="node.state === ContentState.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.PUBLISHED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.REJECTED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
              <v-chip
                v-if="node.state === ContentState.BANNED"
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                {{ t('admin.banned') }}
              </v-chip>
            </div>

            <!-- 待审核状态 -->
            <div v-if="node.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.REJECTED)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.reject') }}
              </v-btn>
            </div>

            <!-- 已通过状态下显示屏蔽按钮 -->
            <div v-if="node.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.REJECTED)"
              >
                <v-icon icon="mdi-block-helper" color="red-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </div>

            <!-- 已拒绝状态下显示通过按钮 -->
            <div v-if="node.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
            </div>

            <!-- 已封禁状态下显示恢复按钮 -->
            <div v-if="node.state === ContentState.BANNED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="updateNodeState(node, ContentState.PUBLISHED)"
              >
                <v-icon icon="mdi-restore" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                恢复
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-file-tree" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  节点ID: {{ node.id }} | 课程ID: {{ node.courseId }}
                </div>
                <div class="text-caption text-grey-darken-1">{{ node.createdAt }}</div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="text-h6 font-weight-bold text-grey-darken-3 mb-2">
                {{ node.name }}
              </div>
              <div v-if="node.description" class="text-body-2 text-grey-darken-1">
                {{ node.description }}
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">搜索中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && nodeList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.status-actions-area {
  min-width: 200px;
}
</style>
