<script setup lang="ts">
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import type { Node, Edge } from '@vue-flow/core'

interface Props {
  nodes?: Node[]
  edges?: Edge[]
  readonly?: boolean
  showBackground?: boolean
  showControls?: boolean
  backgroundPattern?: string
  minZoom?: number
  maxZoom?: number
}

withDefaults(defineProps<Props>(), {
  nodes: () => [],
  edges: () => [],
  readonly: true,
  showBackground: true,
  showControls: true,
  backgroundPattern: '#aaa',
  minZoom: 0.1,
  maxZoom: 1.2,
})
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
      :zoom-on-scroll="true"
      :pan-on-scroll="true"
      :pan-on-drag="true"
      :nodes-draggable="false"
      :nodes-connectable="false"
      :elements-selectable="false"
      :class="{ 'vue-flow-readonly': readonly }"
    >
      <Background v-if="showBackground" :pattern-color="backgroundPattern" :gap="20" />
      <Controls v-if="showControls" :show-interactive="false" />
    </VueFlow>
    <div v-else class="d-flex align-center justify-center h-100 text-grey-lighten-1">
      <v-icon icon="mdi-map-outline" size="48"></v-icon>
    </div>
  </div>
</template>

<!-- Vue Flow 核心样式必须全局导入 -->
<style>
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
@import '@vue-flow/controls/dist/style.css';
</style>

<!-- 组件特定样式使用 scoped -->
<style scoped>
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

/* 普通节点样式 */
.vue-flow-readonly :deep(.vue-flow__node) {
  border-radius: 8px !important;
  background: #fafafa !important;
  border: 2px solid #757575 !important;
  color: #424242 !important;
  font-weight: 500 !important;
  font-size: 0.75rem !important;
  padding: 4px 6px !important;
  align-items: center !important;
  justify-content: center !important;
  display: flex !important;
}

/* 根节点特殊样式 */
.vue-flow-readonly :deep(.vue-flow__node[data-id='0']) {
  background: #616161 !important;
  border: 2px solid #616161 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
}

/* 课程节点样式 - 绿色边框和浅绿背景 */
.vue-flow-readonly :deep(.vue-flow__node.course-node) {
  border: 2px solid #1b5e20 !important;
  background: #e8f5e9 !important;
}

/* 边样式 */
:deep(.vue-flow__edge-path) {
  stroke: #9e9e9e !important;
  stroke-width: 2px !important;
}
</style>
