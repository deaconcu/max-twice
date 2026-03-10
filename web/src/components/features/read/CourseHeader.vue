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
    <!-- 课程名称行 -->
    <div class="course-title-row" @click="goToCourse">
      <div class="course-icon-wrapper">
        <v-icon :icon="courseIcon" size="16" :color="courseColor"></v-icon>
      </div>
      <span class="course-name">{{ parentCourseInfo?.name || currentCourse?.name }}</span>
    </div>

    <!-- 统计/收藏/学习 一行 -->
    <div class="action-row">
      <span class="course-meta">{{ currentCourse?.learnerCount?.toLocaleString() || 0 }} 人学习</span>
      <v-btn
        :icon="parentCourseInfo?.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
        :color="parentCourseInfo?.bookmarked ? 'amber-darken-2' : 'grey-lighten-1'"
        variant="text"
        size="x-small"
        @click.stop="toggleSubscribe"
      ></v-btn>
      <div class="learning-area">
        <v-btn
          color="success"
          :variant="isLearning ? 'tonal' : 'flat'"
          size="x-small"
          rounded="lg"
          class="text-none"
          elevation="0"
          @click.stop="toggleLearning"
        >
          {{ isLearning ? '学习中' : '开始学习' }}
        </v-btn>
        <template v-if="isLearning">
          <v-progress-linear
            :model-value="progressPercent"
            height="4"
            rounded
            color="success"
            bg-color="grey-lighten-3"
            class="progress-bar"
          ></v-progress-linear>
          <span class="progress-text">{{ Math.round(progressPercent) }}%</span>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.course-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-bottom: 8px;
  margin-right: 42px;
  margin-bottom: 8px;
}

.course-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.course-title-row:hover .course-name {
  color: rgb(var(--v-theme-primary));
}

.course-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.course-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  transition: color 0.2s;
  line-height: 1.3;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.learning-area {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.progress-bar {
  flex: 1;
  max-width: 60px;
}

.course-meta {
  font-size: 12px;
  color: #888;
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: rgb(var(--v-theme-success));
}
</style>
