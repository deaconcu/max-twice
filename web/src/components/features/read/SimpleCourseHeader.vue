<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  courseId: number
  courseName: string
  nodeId?: number
  nodeName?: string
}

const props = defineProps<Props>()
const router = useRouter()

// 后退
const goBack = () => {
  router.back()
}

// 跳转到课程详情页
const goToCourse = () => {
  router.push(`/courses/${props.courseId}`)
}

// 跳转到节点帖子列表
const gotoNode = () => {
  if (props.nodeId) {
    router.push({
      path: '/read',
      query: { nodeId: String(props.nodeId) },
    })
  }
}
</script>

<template>
  <div class="simple-course-header">
    <div class="header-wrapper">
      <!-- 后退按钮 - 在左侧边缘 -->
      <v-btn
        icon="mdi-arrow-left"
        variant="tonal"
        size="small"
        class="back-button"
        @click="goBack"
      ></v-btn>

      <!-- 面包屑导航 -->
      <div class="breadcrumb d-flex align-center mb-0">
        <!-- 课程名 -->
        <a class="breadcrumb-link" @click="goToCourse">
          <v-icon size="18" class="mr-1">mdi-book-open-outline</v-icon>
          {{ courseName }}
        </a>

        <!-- 分隔符 -->
        <v-icon v-if="nodeName" size="16" class="mx-2" color="grey-lighten-1"
          >mdi-chevron-right</v-icon
        >

        <!-- 节点名 -->
        <a v-if="nodeName" class="breadcrumb-link" @click="gotoNode">
          <v-icon size="18" class="mr-1">mdi-file-tree-outline</v-icon>
          {{ nodeName }}
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.simple-course-header {
  background-color: rgb(var(--v-theme-surface));
  display: flex;
  align-items: center;
  padding-top: 8px;
}

.header-wrapper {
  width: 100%;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  flex-shrink: 0;
}

.breadcrumb {
  font-size: 14px;
  line-height: 1.5;
  padding: 0;
  margin-bottom: 8px;
  flex: 1;
}

.breadcrumb-link {
  display: inline-flex;
  align-items: center;
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 4px 8px;
  border-radius: 6px;
}

.breadcrumb-link:hover {
  background-color: rgba(var(--v-theme-primary), 0.08);
  color: rgb(var(--v-theme-primary-darken-1));
}

.breadcrumb-link:active {
  transform: scale(0.98);
}

/* 响应式：宽屏时整个header向左偏移按钮宽度 */
@media (min-width: 1201px) {
  .simple-course-header {
    margin-left: -28px;
  }

  .header-wrapper {
    gap: 8px;
  }
}

/* 中等屏幕：按钮在内容区域内 */
@media (max-width: 1200px) {
  .back-button {
    margin-left: 16px;
  }
}

@media (max-width: 960px) {
  .header-wrapper {
    padding: 0 16px;
  }

  .back-button {
    margin-left: 0;
  }
}
</style>
