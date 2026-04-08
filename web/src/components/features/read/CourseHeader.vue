<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getColorByString } from '@/utils/color'
import { useI18n } from '@/composables/useI18n'

const props = withDefaults(defineProps<Props>(), {
  parentCourseInfo: null,
  currentCourse: () => ({}),
  subCourseList: () => [],
  isMainCourse: true,
  isLearning: false,
  courseProgress: 0,
})

const emit = defineEmits<Emits>()

const { t } = useI18n()

interface Props {
  parentCourseInfo?: any
  currentCourse?: any
  subCourseList?: any[]
  isMainCourse?: boolean
  isLearning?: boolean
  courseProgress?: number
}

type Emits = (e: 'start-learning', data: any) => void

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
    <div class="course-row">
      <v-icon :icon="courseIcon" size="35" :color="courseColor"></v-icon>
      <div class="course-info">
        <span class="course-name" @click="goToCourse">{{
          parentCourseInfo?.name || currentCourse?.name
        }}</span>
        <span class="course-meta">{{
          t('course.learnersCount', { count: currentCourse?.learnerCount?.toLocaleString() || 0 })
        }}</span>
      </div>
      <v-btn variant="text" size="small" icon @click.stop="toggleSubscribe">
        <v-icon
          :icon="parentCourseInfo?.bookmarked ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
          :color="parentCourseInfo?.bookmarked ? 'amber-darken-2' : 'grey-lighten-1'"
        ></v-icon>
        <v-tooltip activator="parent" location="bottom">{{
          parentCourseInfo?.bookmarked ? t('common.unbookmark') : t('common.bookmark')
        }}</v-tooltip>
      </v-btn>
      <v-btn
        v-if="!isLearning"
        color="success"
        variant="tonal"
        size="small"
        rounded="lg"
        class="text-none"
        @click.stop="toggleLearning"
      >
        {{ t('course.learn') }}
      </v-btn>
      <div v-else class="progress-area" @click.stop="toggleLearning">
        <v-tooltip location="bottom" :text="t('course.clickToExit')">
          <template #activator="{ props }">
            <div class="progress-ring" v-bind="props">
              <svg viewBox="0 0 36 36">
                <path
                  class="ring-bg"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
                <path
                  class="ring-progress"
                  :stroke-dasharray="`${progressPercent}, 100`"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
              </svg>
              <span class="progress-num">{{ Math.round(progressPercent) }}</span>
            </div>
          </template>
        </v-tooltip>
      </div>
    </div>
  </div>
</template>

<style scoped>
.course-header {
  padding-right: 42px;
  margin-bottom: 12px;
}

.course-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-info {
  flex: 1;
  min-width: 0;
}

.course-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-name:hover {
  color: rgb(var(--v-theme-primary));
}

.course-meta {
  display: block;
  font-size: 11px;
  color: #aaa;
  margin-top: 2px;
}

.progress-area {
  flex-shrink: 0;
}

.progress-ring {
  position: relative;
  width: 36px;
  height: 36px;
  cursor: pointer;
}

.progress-ring svg {
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: #eee;
  stroke-width: 3;
}

.ring-progress {
  fill: none;
  stroke: rgb(var(--v-theme-success));
  stroke-width: 3;
  stroke-linecap: round;
}

.progress-num {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  font-weight: 700;
  color: rgb(var(--v-theme-success));
}
</style>
