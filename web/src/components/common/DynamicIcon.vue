<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  icon?: string | null
  defaultIcon?: string
  size?: number | string
  color?: string
  start?: boolean // 用于按钮内部左侧图标
}

const props = withDefaults(defineProps<Props>(), {
  icon: null,
  defaultIcon: 'mdi-help-circle',
  size: 24,
  color: 'grey',
  start: false,
})

// 判断图标类型
const iconType = computed(() => {
  if (!props.icon) return 'default'
  if (props.icon.startsWith('mdi-')) return 'mdi'
  if (props.icon.startsWith('http')) return 'image'
  return 'default'
})

// 实际使用的图标
const displayIcon = computed(() => {
  if (iconType.value === 'mdi') return props.icon
  return props.defaultIcon
})
</script>

<template>
  <v-icon
    v-if="iconType !== 'image'"
    :icon="displayIcon"
    :size="size"
    :color="color"
    :start="start"
  />
  <v-img
    v-else
    :src="icon!"
    :width="size"
    :height="size"
    cover
    class="rounded"
    :class="{ 'mr-2': start }"
  />
</template>
