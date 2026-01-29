<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

interface Props {
  parentCourseInfo?: any
  currentCourse?: any
  subCourseList?: any[]
  isMainCourse?: boolean
  isLearning?: boolean
  showBackButton?: boolean
  courseProgress?: number // 课程进度（万分位：0-10000）
}

type Emits = (e: 'start-learning', data: any) => void

const props = withDefaults(defineProps<Props>(), {
  parentCourseInfo: null,
  currentCourse: () => ({}),
  subCourseList: () => [],
  isMainCourse: true,
  isLearning: false,
  showBackButton: true,
  courseProgress: 0,
})

const emit = defineEmits<Emits>()
const router = useRouter()

// 将万分位转换为百分比（0-100）
const progressPercent = computed(() => {
  return props.courseProgress ? props.courseProgress / 100 : 0
})

const goBackToCourse = () => {
  router.back()
}

const toggleLearning = () => {
  emit('start-learning', !props.isLearning)
}

const toggleSubscribe = () => {
  // TODO: 实现订阅逻辑
}
</script>

<template>
  <div class="subcourse-info-section">
    <div class="d-flex align-center justify-space-between">
      <!-- 左侧：返回按钮 + 课程路径 -->
      <div class="d-flex align-center course-breadcrumb">
        <v-btn
          v-if="showBackButton"
          icon="mdi-arrow-left"
          variant="flat"
          color="grey-lighten-5"
          :size="$vuetify.display.mobile ? 'small' : 'default'"
          class="flex-shrink-0 mr-3"
          @click="goBackToCourse"
        ></v-btn>
        <v-chip size="small" density="comfortable" color="grey-darken-1" variant="tonal"
          >课程</v-chip
        >
        <v-btn variant="text" class="course-link-btn px-1" @click="goBackToCourse">
          {{ parentCourseInfo?.name || currentCourse?.name }}
        </v-btn>
        <template v-if="!isMainCourse && currentCourse">
          <v-icon icon="mdi-chevron-right" size="18" color="grey-darken-1" class="mx-1"></v-icon>
          <v-chip size="small" density="comfortable" color="grey-darken-1" variant="tonal"
            >子课程</v-chip
          >
          <v-btn variant="text" class="course-link-btn px-1" @click="goBackToCourse">
            {{ currentCourse.name }}
          </v-btn>
        </template>
        <span class="text-caption text-grey mx-2">·</span>
        <span class="text-caption text-grey">{{ currentCourse?.totalNodes || 0 }} 个节点</span>
        <span class="text-caption text-grey mx-2">·</span>
        <span class="text-caption text-grey">1,234 人学习</span>
      </div>

      <!-- 右侧按钮 -->
      <div class="d-flex align-center flex-shrink-0" style="gap: 8px">
        <!-- 学习进度显示（只在学习中时显示） -->
        <div v-if="isLearning" class="d-flex align-center mr-2">
          <v-progress-circular
            :model-value="progressPercent"
            :size="$vuetify.display.smAndDown ? 32 : 36"
            :width="3"
            color="primary"
            class="mr-2"
          >
            <span class="text-caption font-weight-bold">{{ Math.round(progressPercent) }}%</span>
          </v-progress-circular>
        </div>

        <v-btn
          :color="isLearning ? 'success' : 'primary'"
          :variant="isLearning ? 'tonal' : 'flat'"
          :icon="$vuetify.display.smAndDown"
          density="comfortable"
          rounded="pill"
          class="text-none px-4"
          elevation="0"
          @click="toggleLearning"
        >
          <v-icon
            :size="$vuetify.display.smAndDown ? 20 : 16"
            :class="$vuetify.display.smAndDown ? '' : 'mr-1'"
            >{{ isLearning ? 'mdi-check-circle' : 'mdi-play-circle' }}</v-icon
          >
          <span v-if="!$vuetify.display.smAndDown">{{ isLearning ? '学习中' : '开始学习' }}</span>
          <v-tooltip v-if="$vuetify.display.smAndDown" activator="parent" location="bottom">
            {{ isLearning ? '学习中' : '开始学习' }}
          </v-tooltip>
        </v-btn>
        <v-btn
          :icon="parentCourseInfo?.bookmarked ? 'mdi-heart' : 'mdi-heart-outline'"
          :color="parentCourseInfo?.bookmarked ? 'error' : 'grey-lighten-1'"
          :variant="parentCourseInfo?.bookmarked ? 'flat' : 'text'"
          density="comfortable"
          rounded="circle"
          @click="toggleSubscribe"
        ></v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.course-breadcrumb {
  display: flex;
  align-items: center;
}

/* 宽屏时向左延伸，让后退按钮露出到页面外 */
@media (min-width: 1500px) {
  .subcourse-info-section {
    margin-left: -56px;
  }
}

.course-link-btn {
  font-size: 16px;
  font-weight: 600;
  color: #666;
  text-transform: none;
  letter-spacing: normal;
  height: auto;
  min-width: auto;
}

.course-link-btn:hover {
  color: rgb(var(--v-theme-primary));
}
</style>
