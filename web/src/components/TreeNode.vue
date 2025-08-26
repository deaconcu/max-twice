<script setup>
import { ref, onMounted, nextTick } from 'vue';

const props = defineProps({
  nodeData: { type: Object, required: true },
  nodeNames: { type: Object, required: true },
  courseId: { type: Number},
  path: { type: String, required: true },
  currPath: { type: String },
  depth: { type: Number }
});

const emit = defineEmits(['getNextNode']);

const expandedNodes = ref([]);
const expanded = ref(false);

// 处理子节点
const extractSubNodes = (ids) => {
  let subNodes = {};
  if (Array.isArray(ids)) {
    ids.forEach(id => {
      subNodes[id] = props.nodeData[id] || {};
    });
  }
  return subNodes;
};

function calculatePath(currPath, key) {
  return currPath + "-" + key;
}

// 切换节点展开/收起
function toggleNode(key) {
  if (expandedNodes.value.includes(key)) {
    expandedNodes.value = expandedNodes.value.filter(node => node !== key);
    expanded.value = false;
  } else {
    expandedNodes.value.push(key);
    expanded.value = true;
  }
}

Object.keys(props.nodeData).forEach((key) => {
  console.log("props.path: " + props.path);
  if (props.path.startsWith(props.currPath + "-" + key)) {
    toggleNode(key)
  }
});

// 获取下一个节点信息
const getNextNode = (currentPath) => {
  try {
    // 解析当前路径，例如 "1-2-3"
    const pathParts = currentPath.split('-').map(Number);
    
    // 递归函数检查节点是否存在
    const checkNodeExists = (nodeData, pathSegments) => {
      if (pathSegments.length === 0) {
        return true;
      }
      
      const [currentSegment, ...remainingSegments] = pathSegments;
      const nodeKey = currentSegment.toString();
      
      if (nodeData[nodeKey] && typeof nodeData[nodeKey] === 'object') {
        return checkNodeExists(nodeData[nodeKey], remainingSegments);
      }
      
      return false;
    };
    
    // 尝试找到下一个同级节点
    const findNextSibling = (pathParts, depth = pathParts.length - 1) => {
      if (depth <= 0) return null;
      
      const testPathParts = [...pathParts];
      testPathParts[depth]++;
      
      // 构建测试路径的节点检查路径
      const checkPath = testPathParts.slice(1); // 移除第一个元素（通常是根路径）
      
      if (checkNodeExists(props.nodeData, checkPath)) {
        const nextPath = testPathParts.join('-');
        const nodeName = props.nodeNames[testPathParts[depth]];
        return {
          path: nextPath,
          name: nodeName || `节点 ${testPathParts[depth]}`
        };
      }
      
      // 如果没有同级节点，尝试上一级的下一个节点
      return findNextSibling(pathParts, depth - 1);
    };
    
    return findNextSibling(pathParts);
  } catch (error) {
    console.error('Error getting next node:', error);
    return null;
  }
};

// 暴露方法给父组件
defineExpose({
  getNextNode
});
</script>

<template>
  <div>
    <div v-for="(node, key) in nodeData" :key="key">
      <template v-if="key != '+' && key != '^'">
        <div class="d-flex align-center"
          :class="{ 'pb-1': depth === 1, 'border-e-lg border-success': calculatePath(currPath, key) == path }"
          style="font-size: 0.95em; margin-bottom: 0.38em;">
          <router-link :to="{ name: 'read', query: { courseId: courseId, path: calculatePath(currPath, key) } }"
            class="custom-link">
            <span v-if="depth === 1 && calculatePath(currPath, key) == path" class="font-weight-black text-primary"
              style="font-size: 1.1em;">
              {{ nodeNames[key] }}
            </span>
            <span v-else-if="depth === 1" class="font-weight-black" style="font-size: 1.1em;">
              {{ nodeNames[key] }}
            </span>
            <span v-else-if="calculatePath(currPath, key) == path" class="font-weight-bold text-teal">
              {{ nodeNames[key] }}
            </span>
            <span v-else style="font-weight: 400;">
              {{ nodeNames[key] }}
            </span>
          </router-link>
          <template v-if="Object.keys(node).filter(key => key !== '^').length > 0">
            <v-btn icon="mdi-chevron-down" @click="toggleNode(key)" :class="{ flipped: expanded }" class="slow"
              variant="text" size="small" density="compact"></v-btn>
          </template>
        </div>
        <template v-if="Object.keys(node).filter(key => key !== '^').length > 0">
          <v-scroll-x-transition>
            <div v-if="expandedNodes.includes(key)" :class="{ 'pl-4': depth > 1 }">
              <TreeNode :nodeData="node" :nodeNames="nodeNames" :course-id="courseId" :path="path"
                :currPath="calculatePath(currPath, key)" :depth="depth + 1" />
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