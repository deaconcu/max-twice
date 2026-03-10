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
  courseProgress?: number // 课程进度（万分位：0-10000）
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

// 将万分位转换为百分比（0-100）
const progressPercent = computed(() => {
  return props.courseProgress ? props.courseProgress / 100 : 0
})

// 获取课程图标
const courseIcon = computed(() => {
  return props.parentCourseInfo?.icon || props.currentCourse?.icon || 'mdi-book-open-variant'
})

// 获取课程颜色
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
    <div class="header-content">
      <!-- 课程图标 + 课程名称 -->
      <div class="course-info" @click="goToCourse">
        <v-icon :icon="courseIcon" size="20" :color="courseColor" class="course-icon"></v-icon>
        <span class="course-name">{{ parentCourseInfo?.name || currentCourse?.name }}</span>
      </div>

      <!-- 收藏按钮 -->
      <v-btn
        :icon="parentCourseInfo?.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
        :color="parentCourseInfo?.bookmarked ? 'amber-darken-2' : 'grey'"
        variant="text"
        size="default"
        density="comfortable"
        @click="toggleSubscribe"
      ></v-btn>

      <!-- 学习信息 -->
      <div class="learning-info">
        <v-btn
          :color="isLearning ? 'teal' : 'primary'"
          :variant="isLearning ? 'tonal' : 'flat'"
          size="small"
          rounded="pill"
          class="text-none"
          elevation="0"
          @click="toggleLearning"
        >
          <v-icon size="12" class="mr-1">{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon>
          {{ isLearning ? '学习中' : '开始学习' }}
        </v-btn>

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
      </div>

      <!-- 课程统计 - 放到最右侧 -->
      <div class="course-stats">
        <v-icon icon="mdi-account-group" size="14" color="grey"></v-icon>
        <span>{{ currentCourse?.learnerCount?.toLocaleString() || 0 }} 人学习</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.course-header {
  padding: 12px 0;
  margin-bottom: 8px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.course-info:hover .course-name {
  color: rgb(var(--v-theme-primary));
}

.course-icon {
  flex-shrink: 0;
}

.course-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  transition: color 0.2s;
}

.course-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #888;
  margin-left: auto;
}

.learning-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
