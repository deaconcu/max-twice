<script setup lang="ts">
import { useRouter } from 'vue-router'

interface Props {
  parentCourseInfo?: any
  currentCourse?: any
  subCourseList?: any[]
  isMainCourse?: boolean
  isLearning?: boolean
}

type Emits = (e: 'start-learning', data: any) => void

const props = withDefaults(defineProps<Props>(), {
  parentCourseInfo: null,
  currentCourse: () => ({}),
  subCourseList: () => [],
  isMainCourse: true,
  isLearning: false,
})

const emit = defineEmits<Emits>()
const router = useRouter()

const goBackToCourse = () => {
  if (props.currentCourse?.id) {
    router.push({
      name: 'course-detail',
      params: { id: props.currentCourse.id },
    })
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
  <div class="subcourse-info-section">
    <div class="d-flex align-center justify-space-between">
      <!-- 左侧：返回按钮 + 课程路径 -->
      <div class="d-flex align-center course-breadcrumb">
        <v-btn
          icon="mdi-arrow-left"
          variant="flat"
          color="grey-lighten-4"
          size="small"
          class="mr-2"
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
        <v-btn
          :color="isLearning ? 'success' : 'primary'"
          :variant="isLearning ? 'tonal' : 'flat'"
          density="comfortable"
          rounded="pill"
          class="text-none px-4"
          elevation="0"
          @click="toggleLearning"
        >
          <v-icon size="16" class="mr-1">{{
            isLearning ? 'mdi-check-circle' : 'mdi-play-circle'
          }}</v-icon>
          {{ isLearning ? '学习中' : '开始学习' }}
        </v-btn>
        <v-btn
          :icon="parentCourseInfo?.subscribed ? 'mdi-heart' : 'mdi-heart-outline'"
          :color="parentCourseInfo?.subscribed ? 'error' : 'grey-lighten-1'"
          :variant="parentCourseInfo?.subscribed ? 'flat' : 'text'"
          density="comfortable"
          rounded="circle"
          @click="toggleSubscribe"
        ></v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
.subcourse-info-section {
  padding: 0 0 0px 0;
}

.course-breadcrumb {
  display: flex;
  align-items: center;
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
