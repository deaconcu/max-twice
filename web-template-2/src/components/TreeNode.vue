<template>
  <div class="tree-node">
    <div
      class="node-item"
      :class="{
        'active': isActive,
        'has-children': node.children && node.children.length > 0
      }"
      :style="{ paddingLeft: `${depth * 16}px` }"
    >
      <div class="node-content" @click="handleClick">
        <!-- 完成状态图标 -->
        <v-icon
          v-if="node.completed"
          icon="mdi-check-circle"
          size="small"
          color="success"
          class="status-icon"
        />
        <v-icon
          v-else
          icon="mdi-circle-outline"
          size="small"
          color="grey-lighten-1"
          class="status-icon"
        />

        <!-- 节点名称 -->
        <span class="node-name">{{ node.name }}</span>

        <!-- 展开/收起图标 - 放在右侧 -->
        <v-icon
          v-if="node.children && node.children.length > 0"
          :icon="node.expanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
          size="small"
          class="expand-icon"
        />
      </div>
    </div>

    <!-- 子节点 -->
    <v-expand-transition>
      <div v-if="node.expanded && node.children" class="children">
        <TreeNode
          v-for="(child, index) in node.children"
          :key="index"
          :node="child"
          :depth="depth + 1"
          :active-node="activeNode"
          @node-click="$emit('node-click', $event)"
        />
      </div>
    </v-expand-transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  node: any
  depth?: number
  activeNode?: any
}

const props = withDefaults(defineProps<Props>(), {
  depth: 0,
  activeNode: null
})

const emit = defineEmits<{
  (e: 'node-click', node: any): void
}>()

const isActive = computed(() => {
  return props.activeNode === props.node
})

const handleClick = () => {
  if (props.node.children && props.node.children.length > 0) {
    props.node.expanded = !props.node.expanded
  }
  emit('node-click', props.node)
}
</script>

<style scoped>
.tree-node {
  user-select: none;
}

.node-item {
  border-radius: 4px;
  transition: background-color 0.2s;
}

.node-item.active {
  background-color: rgba(var(--v-theme-primary), 0.1);
}

.node-item.active .node-name {
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  gap: 8px;
  transition: background-color 0.2s;
}

.node-content:hover {
  background-color: #F6F7F8;
  border-radius: 4px;
}

.expand-icon {
  flex-shrink: 0;
  margin-left: auto;
}

.status-icon {
  flex-shrink: 0;
}

.node-name {
  font-size: 14px;
  color: #1A1A1B;
  flex: 1;
  margin-left: 4px;
}

.children {
  overflow: hidden;
}
</style>
