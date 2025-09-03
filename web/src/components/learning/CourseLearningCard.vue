<script setup>
  import { useI18n } from 'vue-i18n'
  import { USER_COURSE_STATE } from '@/constants/statusConstants'

  const props = defineProps({
    course: {
      type: Object,
      required: true,
    },
  })

  const emit = defineEmits(['open-course', 'close-course'])

  const { t } = useI18n()

  // 工具函数
  const getDifficultyColor = (difficulty) => {
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

  const getCategoryIcon = (category) => {
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

  const getStatusText = (state) => {
    switch (state) {
      case USER_COURSE_STATE.NOT_STARTED:
        return t('learning.status.notStarted')
      case USER_COURSE_STATE.IN_PROGRESS:
        return t('learning.status.inProgress')
      case USER_COURSE_STATE.COMPLETED:
        return t('learning.status.completed')
      default:
        return t('learning.status.unknown')
    }
  }

  // 事件处理
  const handleOpenCourse = () => {
    emit('open-course', props.course)
  }

  const handleCloseCourse = (event) => {
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
                :icon="getCategoryIcon(course.category)"
                color="teal-darken-2"
                size="16"
              ></v-icon>
            </v-avatar>
            <div class="flex-grow-1 min-width-0">
              <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-4 mb-1">
                {{ course.title }}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0 course-description-text">
                {{ course.description }}
              </p>
            </div>
          </div>
          <div class="d-flex align-start flex-shrink-0">
            <v-chip
              variant="flat"
              :color="getDifficultyColor(course.difficulty)"
              size="small"
              class="course-difficulty-chip mr-2"
            >
              {{ t(`learning.difficulty.${course.difficulty}`) }}
            </v-chip>
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
          <span>{{ course.lastActivity }}</span>
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
    -webkit-line-clamp: 2;
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
