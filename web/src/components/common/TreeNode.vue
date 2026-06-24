<script setup lang="ts">
import { ref, watch } from 'vue'
import { ContentState } from '@/enums'
import type { Node } from '@/types/node'

type NodeData = Record<string, any>

interface Props {
  nodeData: NodeData
  nodeInfos?: Record<string, Node>
  nodeId?: number | null
  courseId?: number | null
  path?: string
  currPath?: string
  depth?: number
  isLearning?: boolean
  parentPath?: string // 父节点路径，用于判断是否被覆盖
  tocIndex?: number // 当前目录组索引（0=第一个目录）
}

const props = withDefaults(defineProps<Props>(), {
  nodeInfos: () => ({}),
  nodeId: null,
  courseId: null,
  path: '',
  currPath: '',
  depth: 0,
  isLearning: false,
  parentPath: '',
  tocIndex: 0,
})

const expandedNodes = ref<string[]>([])

const calculatePath = (currPath: string, key: string): string => {
  return `${currPath}-${key}`
}

// 判断节点是否被父节点覆盖
const isNodeCovered = (nodeKey: string): boolean => {
  if (!props.parentPath) return false

  // 获取从根到当前节点的路径上所有父节点
  const pathParts = props.parentPath.split('-').filter((p) => p)

  // 检查路径上的每个父节点是否已完成
  for (const part of pathParts) {
    if (props.nodeInfos[part]?.isCompleted) {
      return true // 被父节点覆盖
    }
  }

  return false
}

// 切换节点展开/收起
const toggleNode = (key: string): void => {
  if (expandedNodes.value.includes(key)) {
    expandedNodes.value = expandedNodes.value.filter((node) => node !== key)
  } else {
    expandedNodes.value.push(key)
  }
}

// 确保节点展开
const expandNode = (key: string): void => {
  if (!expandedNodes.value.includes(key)) {
    expandedNodes.value.push(key)
  }
}

// Watch for changes in props to update expanded nodes
watch(
  () => [props.nodeData, props.path, props.currPath],
  () => {
    if (!props.path) return // path 为空时不处理
    Object.keys(props.nodeData).forEach((key) => {
      if (props.path.startsWith(`${props.currPath}-${key}`)) {
        expandNode(key)
      }
    })
  },
  { immediate: true }
)

// 判断节点是否被封禁
const isNodeBanned = (nodeKey: string): boolean => {
  return props.nodeInfos[nodeKey]?.state === ContentState.BANNED
}

// 滚动到页面顶部
const scrollToTop = (): void => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  })
}
</script>

<template>
  <div>
    <div v-for="(node, key) in nodeData" :key="key">
      <template v-if="key != '+' && key != '^'">
        <div
          class="d-flex align-center tree-node-item"
          :class="{
            'active-node': calculatePath(currPath, key as string) == path,
            'banned-node': isNodeBanned(key as string),
          }"
        >
          <!-- 被封禁的节点：显示为不可点击的文本 -->
          <template v-if="isNodeBanned(key as string)">
            <div class="banned-link">
              <div class="d-flex align-center flex-grow-1">
                <v-icon icon="mdi-cancel" color="grey-lighten-1" size="16" class="mr-2"></v-icon>
                <span class="tree-node-text text-grey">
                  {{ nodeInfos[key]?.name || key }}
                </span>
              </div>
            </div>
          </template>
          <!-- 正常节点：可点击跳转 -->
          <template v-else>
            <router-link
              :to="{
                path: '/read',
                query: courseId
                  ? { courseId: String(courseId), path: calculatePath(currPath, key as string) }
                  : { nodeId: String(nodeId), path: calculatePath(currPath, key as string) },
              }"
              class="custom-link"
              @click="scrollToTop"
            >
              <div class="d-flex align-center flex-grow-1">
                <!-- 完成状态图标 - 只在学习模式下且是第一个目录时显示 -->
                <template v-if="isLearning && depth !== 1 && tocIndex === 0">
                  <!-- 判断是否被父节点覆盖 -->
                  <template v-if="isNodeCovered(key as string)">
                    <!-- 被覆盖：全部灰色 -->
                    <!-- 已完成：灰色勾 -->
                    <v-icon
                      v-if="nodeInfos[key]?.isCompleted"
                      icon="mdi-check-circle"
                      color="grey-lighten-1"
                      size="16"
                    ></v-icon>
                    <!-- 未完成：灰色横线（无论目录还是叶子） -->
                    <v-icon v-else icon="mdi-minus" color="grey-lighten-1" size="16"></v-icon>
                  </template>
                  <!-- 未被覆盖：正常显示 -->
                  <template v-else>
                    <!-- 目录节点（有子节点） -->
                    <template v-if="Object.keys(node).filter((k) => k !== '^').length > 0">
                      <v-icon
                        v-if="nodeInfos[key]?.isCompleted"
                        icon="mdi-check-circle"
                        color="success"
                        size="16"
                      ></v-icon>
                      <v-icon v-else icon="mdi-minus" color="grey-lighten-1" size="16"></v-icon>
                    </template>
                    <!-- 叶子节点 -->
                    <template v-else>
                      <v-icon
                        v-if="nodeInfos[key]?.isCompleted"
                        icon="mdi-check-circle"
                        color="success"
                        size="16"
                      ></v-icon>
                      <v-icon
                        v-else
                        icon="mdi-circle-outline"
                        color="grey-lighten-1"
                        size="16"
                      ></v-icon>
                    </template>
                  </template>
                </template>

                <span
                  v-if="calculatePath(currPath, key as string) == path"
                  class="tree-node-text text-grey-darken-5 font-weight-black"
                >
                  {{ nodeInfos[key]?.name || key }}
                </span>
                <span v-else class="tree-node-text">
                  {{ nodeInfos[key]?.name || key }}
                </span>

                <!-- 可完成标识 - 只在学习模式下且是第一个目录时显示 -->
                <div
                  v-if="isLearning && nodeInfos[key]?.canComplete && tocIndex === 0"
                  class="d-inline-block"
                >
                  <v-icon icon="mdi-playlist-check" color="green" size="18" class="ml-2"></v-icon>
                  <v-tooltip activator="parent" location="top">
                    该目录下的所有节点都已完成，可以标记当前目录为已完成了！
                  </v-tooltip>
                </div>
              </div>
            </router-link>
          </template>
          <v-btn
            v-if="Object.keys(node).filter((key) => key !== '^').length > 0"
            icon="mdi-chevron-down"
            :class="{ flipped: expandedNodes.includes(key as string) }"
            class="slow"
            variant="text"
            size="small"
            density="comfortable"
            @click="toggleNode(key as string)"
          ></v-btn>
        </div>
        <template v-if="Object.keys(node).filter((key) => key !== '^').length > 0">
          <v-scroll-x-transition>
            <div v-if="expandedNodes.includes(key as string)" :class="{ 'pl-4': depth > 1 }">
              <TreeNode
                :node-data="node"
                :node-infos="nodeInfos"
                :node-id="nodeId"
                :course-id="courseId"
                :path="path"
                :curr-path="calculatePath(currPath, key as string)"
                :parent-path="currPath ? `${currPath}-${key}` : String(key)"
                :depth="depth + 1"
                :is-learning="isLearning"
                :toc-index="tocIndex"
              />
            </div>
          </v-scroll-x-transition>
        </template>
      </template>
    </div>
  </div>
</template>

<style scoped>
.tree-node-item {
  user-select: none;
  border-radius: 8px;
  transition: background-color 0.2s;
  margin-bottom: 2px;
}

.tree-node-item:hover {
  background-color: #f6f7f8;
}

.tree-node-item.active-node {
  background-color: #f5f5f5;
}

.custom-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  padding: 6px 4px;
  gap: 8px;
  flex: 1;
  transition: background-color 0.2s;
  border-radius: 4px;
}

.tree-node-text {
  font-size: 14px;
  color: #1a1a1b;
  flex: 1;
  margin-left: 4px;
  font-weight: 400;
}

.tree-node-text.text-primary,
.tree-node-text.text-teal {
  font-weight: 600;
}

.flipped {
  transform: rotate(180deg);
}

.slow {
  transition: transform 0.3s;
}

:deep(.v-btn) {
  flex-shrink: 0;
  margin-left: auto;
  margin-right: 8px;
}

/* 被封禁节点样式 */
.banned-node {
  opacity: 0.6;
}

.banned-link {
  display: flex;
  align-items: center;
  padding: 6px 4px;
  gap: 8px;
  flex: 1;
  cursor: not-allowed;
}
</style>
