<script setup lang="ts">
import { ref, watch } from 'vue'

interface NodeInfo {
  name?: string
  isCompleted?: boolean
}

type NodeData = Record<string, any>

interface Props {
  nodeData: NodeData
  nodeInfos: Record<string, NodeInfo>
  courseId?: number | null
  path: string
  currPath?: string
  depth?: number
  isLearning?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  courseId: null,
  currPath: '',
  depth: 0,
  isLearning: false,
})

const expandedNodes = ref<string[]>([])

const calculatePath = (currPath: string, key: string): string => {
  return `${currPath}-${key}`
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
    Object.keys(props.nodeData).forEach((key) => {
      if (props.path.startsWith(`${props.currPath}-${key}`)) {
        expandNode(key)
      }
    })
  },
  { immediate: true }
)

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
          }"
        >
          <router-link
            :to="{
              path: '/read',
              query: { courseId: String(courseId), path: calculatePath(currPath, key as string) },
            }"
            class="custom-link"
            @click="scrollToTop"
          >
            <div class="d-flex align-center flex-grow-1">
              <!-- 完成状态图标 - 只在学习模式下显示 -->
              <template v-if="isLearning">
                <!-- 有子节点的显示横线，但如果是根目录层级(depth=1)则不显示 -->
                <template v-if="Object.keys(node).filter((key) => key !== '^').length > 0">
                  <v-icon
                    v-if="depth !== 1"
                    icon="mdi-minus"
                    color="grey-darken-1"
                    size="16"
                  ></v-icon>
                </template>
                <!-- 叶子节点显示完成状态，但如果是根目录层级(depth=1)则不显示 -->
                <template v-else>
                  <template v-if="depth !== 1">
                    <v-icon
                      v-if="nodeInfos[key]?.isCompleted"
                      icon="mdi-check-circle"
                      color="success"
                      size="16"
                    ></v-icon>
                    <v-icon
                      v-else
                      icon="mdi-circle-outline"
                      color="grey-lighten-2"
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
            </div>
          </router-link>
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
                :course-id="courseId"
                :path="path"
                :curr-path="calculatePath(currPath, key as string)"
                :depth="depth + 1"
                :is-learning="isLearning"
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
  font-size: 15px;
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
</style>
