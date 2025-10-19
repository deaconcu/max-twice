<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { UserProgressState } from '@/types/enums'
import type { Course } from '@/types/course'

interface Props {
  course: Course
}

interface Emits {
  (e: 'open-course', course: Course): void
  (e: 'close-course', course: Course, event: Event): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { t } = useI18n()

// 工具函数
const getDifficultyColor = (difficulty: string): string => {
  switch (difficulty) {
    case 'beginner':
      return 'success'
    case 'intermediate':
      return 'warning'
    case 'advanced':
      return 'error'
    default:
      return 'grey'
  }
}

const getCategoryIcon = (category: string): string => {
  switch (category) {
    case 'frontend':
      return 'mdi-web'
    case 'backend':
      return 'mdi-server'
    case 'database':
      return 'mdi-database'
    case 'mobile':
      return 'mdi-cellphone'
    default:
      return 'mdi-book'
  }
}

const getStatusText = (state: number): string => {
  switch (state) {
    case UserProgressState.NOT_STARTED:
      return t('learning.status.notStarted')
    case UserProgressState.IN_PROGRESS:
      return t('learning.status.inProgress')
    case UserProgressState.COMPLETED:
      return t('learning.status.completed')
    default:
      return t('learning.status.unknown')
  }
}

// 事件处理
const handleOpenCourse = (): void => {
  emit('open-course', props.course)
}

const handleCloseCourse = (event: Event): void => {
  event.stopPropagation()
  emit('close-course', props.course, event)
}
</script>

<template>
  <v-col cols="12" md="6">
    <v-card
      flat
      color="grey-lighten-5"
      rounded="lg"
      class="course-card mb-3"
      @click="handleOpenCourse"
    >
      <v-card-text class="pa-4">
        <div class="d-flex align-start justify-space-between mb-3">
          <div class="d-flex align-start flex-grow-1 mr-3">
            <v-avatar color="teal-lighten-4" size="32" class="mr-3 flex-shrink-0 avatar-top-margin">
              <v-icon
                icon="mdi-book"
                color="teal-darken-2"
                size="16"
              ></v-icon>
            </v-avatar>
            <div class="flex-grow-1 min-width-0">
              <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">
                {{ course.name}}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0 course-description-text">
                {{ course.description }}
              </p>
            </div>
          </div>
          <div class="d-flex align-start flex-shrink-0">
            <v-btn
              variant="text"
              size="x-small"
              class="course-close-btn"
              color="grey-darken-2"
              @click="handleCloseCourse"
            >
              <v-icon size="16">mdi-close</v-icon>
              <v-tooltip activator="parent" location="bottom">
                {{ t('learning.exitLearning') }}
              </v-tooltip>
            </v-btn>
          </div>
        </div>

        <div class="mb-3">
          <div class="d-flex justify-space-between text-body-2 mb-2">
            <span class="text-grey-darken-3">{{ t('learning.courseProgress') }}</span>
            <span class="text-primary font-weight-bold"
              >{{ parseFloat(course.progress.toFixed(2)) }}%</span
            >
          </div>
          <v-progress-linear
            :model-value="course.progress"
            color="primary"
            background-color="grey-lighten-3"
            height="8"
            rounded="lg"
          >
          </v-progress-linear>
        </div>

        <div class="d-flex justify-space-between align-center text-body-2 text-grey-darken-2">
          <span>{{ getStatusText(course.state) }}</span>
          <span>2025-12-31</span>
        </div>
      </v-card-text>
    </v-card>
  </v-col>
</template>

<style scoped>
.course-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.course-card:hover {
  transform: translateY(-2px);
  border-color: rgba(25, 118, 210, 0.2);
}

.course-description-text {
  line-height: 1.4;
  max-height: 2.8em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.course-difficulty-chip {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.course-close-btn {
  min-width: 24px !important;
  width: 24px;
  height: 24px;
  transition: all 0.2s ease;
}

.course-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.04) !important;
  transform: scale(1.1);
}

.avatar-top-margin {
  margin-top: 5px;
}
</style>