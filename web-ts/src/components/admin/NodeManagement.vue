<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { nodeServiceV1 } from '@/services/api/v1/apiServiceV1'
import type { Node } from '@/types/node'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 筛选条件
const filterNodeId = ref<number | undefined>(undefined)
const filterCourseId = ref<number | undefined>(undefined)
const filterCreatorId = ref<number | undefined>(undefined)

// 节点列表
const nodeList = ref<Node[]>([])
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)

// 搜索节点
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

// 加载更多
const loadMore = async () => {
  if (hasMore.value && !loading.value) {
    await searchNodes(true)
  }
}

// 清除搜索
const clearSearch = () => {
  filterNodeId.value = undefined
  filterCourseId.value = undefined
  filterCreatorId.value = undefined
  nodeList.value = []
  hasMore.value = true
}

const getStateColor = (state: number) => {
  switch (state) {
    case 0:
      return 'orange-lighten-4'
    case 1:
      return 'teal-lighten-4'
    case 2:
      return 'red-lighten-4'
    default:
      return 'grey-lighten-4'
  }
}

const getStateText = (state: number) => {
  switch (state) {
    case 0:
      return '待审核'
    case 1:
      return '正常'
    case 2:
      return '已屏蔽'
    default:
      return '未知'
  }
}

const updateNodeState = async (node: Node, newState: number) => {
  try {
    const response = await nodeServiceV1.updateNodeState(node.id, newState)
    if (response.code === 200) {
      const updatedNode = response.data as Node
      const index = nodeList.value.findIndex(n => n.id === node.id)
      if (index !== -1) {
        nodeList.value[index].state = updatedNode.state
      }
      showSnackbar?.(`节点状态已更新为${getStateText(newState)}`, 'success')
    }
  } catch (error) {
    console.error('Error updating node state:', error)
    showSnackbar?.('更新节点状态失败', 'error')
  }
}
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

    <!-- 搜索区域 -->
    <v-card flat class="border rounded-lg pa-4 mb-6">
      <div class="text-subtitle-2 font-weight-bold text-grey-darken-3 mb-3">
        <v-icon icon="mdi-magnify" size="18" class="mr-2"></v-icon>
        搜索节点
      </div>
      <v-row dense align="center">
        <v-col cols="12" sm="3">
          <v-text-field
            v-model.number="filterNodeId"
            type="number"
            label="节点 ID"
            variant="outlined"
            density="compact"
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
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="3">
          <div class="d-flex ga-2">
            <v-btn
              variant="flat"
              color="primary"
              @click="searchNodes()"
              :loading="loading"
            >
              <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
              搜索
            </v-btn>
            <v-btn
              variant="outlined"
              color="grey-darken-1"
              @click="clearSearch"
            >
              <v-icon icon="mdi-close" class="mr-1"></v-icon>
              清除
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 搜索结果 -->
    <div v-if="nodeList.length === 0 && !loading" class="text-center py-12">
      <v-icon
        icon="mdi-file-tree-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">请输入搜索条件查询节点</p>
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
        <div class="d-flex justify-space-between align-start">
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-2">
              <v-chip size="small" variant="flat" color="blue-lighten-4" class="mr-2">
                <span class="text-blue-darken-2">ID: {{ node.id }}</span>
              </v-chip>
              <v-chip size="small" variant="flat" color="green-lighten-4" class="mr-2">
                <span class="text-green-darken-2">课程: {{ node.courseId }}</span>
              </v-chip>
              <v-chip
                size="small"
                variant="flat"
                :color="getStateColor(node.state)"
              >
                <span :class="`text-${getStateColor(node.state)}-darken-2`">{{ getStateText(node.state) }}</span>
              </v-chip>
            </div>
            <div class="text-h6 font-weight-bold text-grey-darken-3 mb-2">
              {{ node.name }}
            </div>
            <div v-if="node.description" class="text-body-2 text-grey-darken-1 mb-2">
              {{ node.description }}
            </div>
            <div class="text-caption text-grey-darken-1">
              创建时间: {{ node.createdAt }}
            </div>
          </div>
          <div class="d-flex ga-2">
            <v-btn
              v-if="node.state === 1"
              variant="outlined"
              color="red-darken-1"
              size="small"
              rounded="lg"
              @click="updateNodeState(node, 2)"
            >
              <v-icon icon="mdi-cancel" size="16" class="mr-1"></v-icon>
              屏蔽
            </v-btn>
            <v-btn
              v-else-if="node.state === 2"
              variant="outlined"
              color="green-darken-1"
              size="small"
              rounded="lg"
              @click="updateNodeState(node, 1)"
            >
              <v-icon icon="mdi-check" size="16" class="mr-1"></v-icon>
              恢复
            </v-btn>
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
</style>
