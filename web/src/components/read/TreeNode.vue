<script setup>
  import { ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  const props = defineProps({
    nodeData: { type: Object, required: true },
    nodeInfos: { type: Object, required: true }, // 替换 nodeNames
    courseId: { type: Number, default: null },
    path: { type: String, required: true },
    currPath: { type: String, default: '' },
    depth: { type: Number, default: 0 },
    isLearning: { type: Boolean, default: false },
  })

  defineEmits(['getNextNode'])

  const expandedNodes = ref([])

  const calculatePath = (currPath, key) => {
    return `${currPath}-${key}`
  }

  // 切换节点展开/收起
  const toggleNode = (key) => {
    if (expandedNodes.value.includes(key)) {
      expandedNodes.value = expandedNodes.value.filter((node) => node !== key)
    } else {
      expandedNodes.value.push(key)
    }
  }

  // 确保节点展开
  const expandNode = (key) => {
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

  // 获取下一个节点信息
  const getNextNode = (currentPath) => {
    try {
      // 解析当前路径，例如 "1-2-3"
      const pathParts = currentPath.split('-').map(Number)

      // 递归函数检查节点是否存在
      const checkNodeExists = (nodeData, pathSegments) => {
        if (pathSegments.length === 0) {
          return true
        }

        const [currentSegment, ...remainingSegments] = pathSegments
        const nodeKey = currentSegment.toString()

        if (nodeData[nodeKey] && typeof nodeData[nodeKey] === 'object') {
          return checkNodeExists(nodeData[nodeKey], remainingSegments)
        }

        return false
      }

      // 尝试找到下一个同级节点
      const findNextSibling = (pathParts, depth = pathParts.length - 1) => {
        if (depth <= 0) return null

        const testPathParts = [...pathParts]
        testPathParts[depth]++

        // 构建测试路径的节点检查路径
        const checkPath = testPathParts.slice(1) // 移除第一个元素（通常是根路径）

        if (checkNodeExists(props.nodeData, checkPath)) {
          const nextPath = testPathParts.join('-')
          const nodeInfo = props.nodeInfos[testPathParts[depth]]
          return {
            path: nextPath,
            name: nodeInfo?.name || `${t('treeNode.node')} ${testPathParts[depth]}`,
          }
        }

        // 如果没有同级节点，尝试上一级的下一个节点
        return findNextSibling(pathParts, depth - 1)
      }

      return findNextSibling(pathParts)
    } catch (error) {
      console.error('Error getting next node:', error)
      return null
    }
  }

  // 暴露方法给父组件
  defineExpose({
    getNextNode,
  })
</script>

<template>
  <div>
    <div v-for="(node, key) in nodeData" :key="key">
      <template v-if="key != '+' && key != '^'">
        <div
          class="d-flex align-center tree-node-item"
          :class="{
            'pb-1': depth === 1,
            'border-e-lg border-success': calculatePath(currPath, key) == path,
          }"
        >
          <router-link
            :to="{
              name: 'read',
              query: { courseId: courseId, path: calculatePath(currPath, key) },
            }"
            class="custom-link"
          >
            <div class="d-flex align-center">
              <!-- 完成状态图标 - 只在学习模式下显示 -->
              <template v-if="isLearning">
                <!-- 有子节点的显示横线 -->
                <template v-if="Object.keys(node).filter((key) => key !== '^').length > 0">
                  <v-icon icon="mdi-minus" color="grey-darken-1" size="16" class="mr-2"></v-icon>
                </template>
                <!-- 叶子节点显示完成状态 -->
                <template v-else>
                  <v-icon
                    v-if="nodeInfos[key]?.isCompleted"
                    icon="mdi-check-circle"
                    color="success"
                    size="16"
                    class="mr-2"
                  ></v-icon>
                  <v-icon
                    v-else
                    icon="mdi-circle-outline"
                    color="grey-lighten-2"
                    size="16"
                    class="mr-2"
                  ></v-icon>
                </template>
              </template>

              <span
                v-if="depth === 1 && calculatePath(currPath, key) == path"
                class="tree-node-text text-primary text-body-1"
              >
                {{ nodeInfos[key]?.name || key }}
              </span>
              <span v-else-if="depth === 1" class="tree-node-text">
                {{ nodeInfos[key]?.name || key }}
              </span>
              <span
                v-else-if="calculatePath(currPath, key) == path"
                class="tree-node-text text-teal"
              >
                {{ nodeInfos[key]?.name || key }}
              </span>
              <span v-else class="tree-node-text">
                {{ nodeInfos[key]?.name || key }}
              </span>
            </div>
          </router-link>
          <template v-if="Object.keys(node).filter((key) => key !== '^').length > 0">
            <v-btn
              icon="mdi-chevron-down"
              :class="{ flipped: expandedNodes.includes(key) }"
              class="slow"
              variant="text"
              size="small"
              density="compact"
              @click="toggleNode(key)"
            ></v-btn>
          </template>
        </div>
        <template v-if="Object.keys(node).filter((key) => key !== '^').length > 0">
          <v-scroll-x-transition>
            <div v-if="expandedNodes.includes(key)" :class="{ 'pl-4': depth > 1 }">
              <TreeNode
                :node-data="node"
                :node-infos="nodeInfos"
                :course-id="courseId"
                :path="path"
                :curr-path="calculatePath(currPath, key)"
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
  .node {
    cursor: pointer;
    margin: 5px 0;
  }

  .tree-node-item {
    font-size: 0.95em;
    margin-bottom: 0.38em;
  }

  .tree-node-text {
    font-weight: 500;
  }

  a {
    text-decoration: none;
    color: #333;
    transition: 0.4s;
    padding: 3px;
  }

  .custom-link {
    display: inline-block;
    text-decoration: none;
    transition: background-color 0.3s;
  }

  .custom-link:hover {
    background-color: rgba(0, 0, 0, 0.03);
    /* 变灰色 */
    border-radius: 4px;
  }
</style>
