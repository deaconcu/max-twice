<script setup>
  import { VueFlow } from '@vue-flow/core'
  import { Background } from '@vue-flow/background'
  import { onMounted, watch } from 'vue'

  const props = defineProps({
    nodes: {
      type: Array,
      default: () => [],
    },
    edges: {
      type: Array,
      default: () => [],
    },
    readonly: {
      type: Boolean,
      default: true,
    },
    showBackground: {
      type: Boolean,
      default: true,
    },
    backgroundPattern: {
      type: String,
      default: '#aaa',
    },
    minZoom: {
      type: Number,
      default: 0.3,
    },
    maxZoom: {
      type: Number,
      default: 0.8,
    },
    snapToGrid: {
      type: Boolean,
      default: true,
    },
    snapGrid: {
      type: Array,
      default: () => [20, 20],
    },
  })

  const emit = defineEmits(['node-click'])

  const handleNodeClick = (event) => {
    emit('node-click', event)
  }

  // 添加调试日志
  onMounted(() => {
    console.log('RoadmapVueFlow mounted - nodes:', props.nodes)
    console.log('RoadmapVueFlow mounted - edges:', props.edges)
    console.log('RoadmapVueFlow mounted - nodes length:', props.nodes?.length)
    console.log('RoadmapVueFlow mounted - edges length:', props.edges?.length)
  })

  watch(
    () => props.nodes,
    (newNodes) => {
      console.log('RoadmapVueFlow nodes updated:', newNodes)
    },
    { deep: true }
  )

  watch(
    () => props.edges,
    (newEdges) => {
      console.log('RoadmapVueFlow edges updated:', newEdges)
    },
    { deep: true }
  )
</script>

<template>
  <div class="roadmap-vue-flow">
    <VueFlow
      v-if="nodes && nodes.length > 0"
      :nodes="nodes"
      :edges="edges"
      :fit-view-on-init="true"
      :min-zoom="minZoom"
      :max-zoom="maxZoom"
      :snap-to-grid="snapToGrid"
      :snap-grid="snapGrid"
      :zoom-on-scroll="false"
      :pan-on-scroll="false"
      :pan-on-drag="false"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="true"
      :class="{ 'vue-flow-readonly': readonly }"
      @node-click="handleNodeClick"
    >
      <Background v-if="showBackground" :pattern-color="backgroundPattern" :gap="20" />
    </VueFlow>
    <div v-else class="d-flex align-center justify-center h-100 text-grey-lighten-1">
      <v-icon icon="mdi-map-outline" size="48"></v-icon>
    </div>
  </div>
</template>

<style scoped>
  @import '@vue-flow/core/dist/style.css';
  @import '@vue-flow/core/dist/theme-default.css';

  .roadmap-vue-flow {
    width: 100%;
    height: 100%;
  }

  /* 只读模式下隐藏连接点 */
  .vue-flow-readonly :deep(.vue-flow__handle) {
    width: 0 !important;
    height: 0 !important;
    border: none !important;
    background: transparent !important;
  }

  /* 只读模式下美化默认节点 */
  .vue-flow-readonly :deep(.vue-flow__node) {
    border-radius: 12px !important;
    background: #fafafa !important;
    border: 3px solid #1976d2 !important;
    color: #1976d2 !important;
    font-weight: 500 !important;
    font-size: 0.85rem !important;
    transition: all 0.2s ease !important;
    cursor: pointer !important;
    padding: 6px 8px !important;
    align-items: center !important;
    justify-content: center !important;
    display: flex !important;
  }

  /* 根节点特殊样式 */
  .vue-flow-readonly :deep(.vue-flow__node[data-id='0']) {
    background: #1976d2 !important;
    border: 3px solid #1976d2 !important;
    color: #ffffff !important;
    font-weight: 600 !important;
  }

  .vue-flow-readonly :deep(.vue-flow__node[data-id='0']:hover) {
    background: #1976d2 !important;
    border: 3px solid #1976d2 !important;
    color: #ffffff !important;
    font-weight: 600 !important;
  }

  .vue-flow-readonly :deep(.vue-flow__node:hover) {
    background: #e3f2fd !important;
    border-color: #1976d2 !important;
    transform: translateY(-5px) !important;
    color: #0d47a1 !important;
  }

  /* 已完成课程样式 */
  .vue-flow-readonly :deep(.vue-flow__node.completed-course) {
    background: #e8f5e9 !important;
    border-color: #4caf50 !important;
    color: #2e7d32 !important;
  }

  /* 已完成课程标识 */
  .vue-flow-readonly :deep(.vue-flow__node.completed-course::after) {
    content: '✓';
    position: absolute;
    top: -6px;
    right: -6px;
    width: 16px;
    height: 16px;
    background: #4caf50;
    color: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: bold;
    border: 2px solid white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  }

  /* 有进度课程的背景进度填充 */
  .vue-flow-readonly :deep(.vue-flow__node.progress-course) {
    background: linear-gradient(
      to right,
      #88ee76 var(--progress, 0%),
      #fafafa var(--progress, 0%)
    ) !important;
    border: 3px solid #2fae19 !important;
    color: #333 !important;
  }

  /* 边样式 */
  :deep(.vue-flow__edge-path) {
    stroke: #9e9e9e !important;
    stroke-width: 2px !important;
    visibility: visible !important;
    opacity: 1 !important;
  }

  :deep(.vue-flow__edge.selected .vue-flow__edge-path) {
    stroke: #1976d2 !important;
    stroke-width: 3px !important;
  }

  /* 确保边的其他元素也可见 */
  :deep(.vue-flow__edge) {
    pointer-events: all !important;
    visibility: visible !important;
    opacity: 1 !important;
  }

  :deep(.vue-flow__edges) {
    pointer-events: all !important;
    visibility: visible !important;
    opacity: 1 !important;
  }
</style>
