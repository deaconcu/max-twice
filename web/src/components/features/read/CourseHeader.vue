<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getColorByString } from '@/utils/color'

interface Props {
  parentCourseInfo?: any
  currentCourse?: any
  subCourseList?: any[]
  isMainCourse?: boolean
  isLearning?: boolean
  courseProgress?: number
}

type Emits = (e: 'start-learning', data: any) => void

const props = withDefaults(defineProps<Props>(), {
  parentCourseInfo: null,
  currentCourse: () => ({}),
  subCourseList: () => [],
  isMainCourse: true,
  isLearning: false,
  courseProgress: 0,
})

const emit = defineEmits<Emits>()
const router = useRouter()

const progressPercent = computed(() => {
  return props.courseProgress ? props.courseProgress / 100 : 0
})

const courseIcon = computed(() => {
  return props.parentCourseInfo?.icon || props.currentCourse?.icon || 'mdi-book-open-variant'
})

const courseColor = computed(() => {
  const name = props.parentCourseInfo?.name || props.currentCourse?.name || ''
  return getColorByString(name)
})

const goToCourse = () => {
  if (props.parentCourseInfo?.id) {
    router.push(`/courses/${props.parentCourseInfo.id}`)
  }
}

const toggleLearning = () => {
  emit('start-learning', !props.isLearning)
}

const toggleSubscribe = () => {
  // TODO: 实现订阅逻辑
}
</script>

<template>
  <div class="course-header">
    <!-- 左侧：课程信息 -->
    <div class="course-left">
      <!-- 课程图标 -->
      <div class="course-icon-wrapper">
        <v-icon :icon="courseIcon" size="18" :color="courseColor"></v-icon>
      </div>

      <!-- 课程名称 -->
      <span class="course-name" @click="goToCourse">{{ parentCourseInfo?.name || currentCourse?.name }}</span>

      <!-- 学习人数 -->
      <span class="course-meta">{{ currentCourse?.learnerCount?.toLocaleString() || 0 }} 人学习</span>

      <!-- 收藏 -->
      <v-btn
        :icon="parentCourseInfo?.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
        :color="parentCourseInfo?.bookmarked ? 'amber-darken-2' : 'grey-lighten-1'"
        variant="text"
        size="small"
        @click.stop="toggleSubscribe"
      ></v-btn>
    </div>

    <!-- 右侧：学习信息 -->
    <div class="course-right">
      <!-- 进度（学习中显示） -->
      <div v-if="isLearning" class="progress-info">
        <div class="progress-header">
          <span class="progress-label">进度</span>
          <span class="progress-text">{{ Math.round(progressPercent) }}%</span>
        </div>
        <v-progress-linear
          :model-value="progressPercent"
          height="4"
          rounded
          color="teal"
          bg-color="grey-lighten-3"
          class="progress-bar"
        ></v-progress-linear>
      </div>

      <!-- 学习按钮 -->
      <v-btn
        color="success"
        :variant="isLearning ? 'tonal' : 'flat'"
        size="small"
        rounded="pill"
        class="text-none"
        elevation="0"
        @click.stop="toggleLearning"
      >
        <v-icon size="14" class="mr-1">{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon>
        {{ isLearning ? '学习中' : '开始学习' }}
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.course-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 12px;
}

.course-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.course-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.course-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: #f5f5f5;
  border-radius: 8px;
  flex-shrink: 0;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  transition: color 0.2s;
}

.course-name:hover {
  color: rgb(var(--v-theme-primary));
}

.course-meta {
  font-size: 13px;
  color: #999;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: 12px;
  color: #888;
}

.progress-text {
  font-size: 13px;
  font-weight: 600;
  color: #009688;
}

.progress-bar {
  width: 100%;
}
</style>
