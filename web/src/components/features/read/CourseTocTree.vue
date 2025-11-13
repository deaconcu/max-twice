<template>
  <div class="course-toc-tree pa-4">
    <h3 class="text-h6 mb-4">课程目录</h3>
    <div v-for="item in tocData" :key="item.id">
      <div
        class="toc-item"
        :class="{ active: activeNodeId === item.id }"
        @click="$emit('node-click', item.id)"
      >
        {{ item.name }}
      </div>
      <div v-if="item.children" class="toc-children">
        <div
          v-for="child in item.children"
          :key="child.id"
          class="toc-item child"
          :class="{ active: activeNodeId === child.id }"
          @click="$emit('node-click', child.id)"
        >
          {{ child.name }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface TocNode {
  id: number
  name: string
  completed: boolean
  expanded?: boolean
  children?: TocNode[]
}

defineProps<{
  tocData: TocNode[]
  activeNodeId: number | null
}>()

defineEmits<{
  'node-click': [nodeId: number]
}>()
</script>

<style scoped>
.toc-item {
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
}

.toc-item:hover {
  background: #f5f5f5;
}

.toc-item.active {
  background: #e3f2fd;
  color: #1976d2;
  font-weight: 600;
}

.toc-children {
  margin-left: 16px;
}

.toc-item.child {
  font-size: 0.9em;
}
</style>
