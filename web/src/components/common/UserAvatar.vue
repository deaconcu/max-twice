<template>
  <v-avatar :size="size" :rounded="rounded" :class="avatarClass">
    <!-- 自定义头像 -->
    <img v-if="avatarUrl" :src="avatarUrl" :alt="name" class="avatar-img" />

    <!-- 首字母头像 -->
    <div v-else class="initial-avatar" :style="{ backgroundColor: bgColor }">
      <span class="text-white font-weight-bold" :style="{ fontSize: fontSize }">
        {{ initial }}
      </span>
    </div>
  </v-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  avatarUrl?: string
  size?: number | string
  rounded?: string
  avatarClass?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 40,
  rounded: 'circle',
  avatarUrl: '',
  avatarClass: '',
})

// 获取首字母
const initial = computed(() => {
  if (!props.name) return '?'
  return props.name.charAt(0).toUpperCase()
})

// 根据名称生成颜色
const bgColor = computed(() => {
  const colors = [
    '#FF6B6B', // 红
    '#4ECDC4', // 青
    '#45B7D1', // 蓝
    '#FFA07A', // 橙
    '#98D8C8', // 绿松石
    '#F7DC6F', // 黄
    '#BB8FCE', // 紫
    '#85C1E2', // 天蓝
    '#F06292', // 粉红
    '#BA68C8', // 淡紫
    '#9575CD', // 深紫
    '#7986CB', // 靛蓝
  ]

  if (!props.name) return colors[0]

  // 简单的hash算法
  let hash = 0
  for (let i = 0; i < props.name.length; i++) {
    hash = props.name.charCodeAt(i) + ((hash << 5) - hash)
  }

  return colors[Math.abs(hash) % colors.length]
})

// 字体大小根据 avatar 大小调整
const fontSize = computed(() => {
  const size = typeof props.size === 'number' ? props.size : parseInt(props.size)
  return `${size * 0.45}px`
})
</script>

<style scoped>
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.initial-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
