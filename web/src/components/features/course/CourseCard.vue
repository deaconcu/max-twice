<template>
  <v-card rounded="xl" class="course-card hoverable" border hover @click="handleClick">
    <v-card-text class="pa-6">
      <div class="d-flex align-center mb-4">
        <div class="icon-container flex-shrink-0 mr-4">
          <DynamicIcon
            :icon="course.icon"
            default-icon="mdi-book-open-variant"
            :size="28"
            :color="getColorByString(course.name)"
          />
        </div>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
            {{ course.name }}
          </h3>
          <div class="d-flex align-center ga-3">
            <div class="d-flex align-center">
              <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1" />
              <span class="text-caption text-grey-darken-2">
                {{ formatNumber(course.learnerCount) }} 人学习
              </span>
            </div>
            <div v-if="course.subCourseCount && course.subCourseCount > 0" class="d-flex align-center">
              <v-icon icon="mdi-book-multiple" size="14" color="grey" class="mr-1" />
              <span class="text-caption text-grey-darken-2">
                {{ course.subCourseCount }} 个子课程
              </span>
            </div>
          </div>
        </div>
      </div>

      <p v-if="course.description" class="text-body-2 text-grey-darken-2 course-description">
        {{ course.description }}
      </p>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Course } from '@/types/course'
import { getColorByString } from '@/utils/color'
import DynamicIcon from '@/components/common/DynamicIcon.vue'

interface Props {
  course: Course
}

type Emits = (e: 'subscribe' | 'unsubscribe', courseId: number) => void

const props = defineProps<Props>()
defineEmits<Emits>()

const router = useRouter()

/**
 * 格式化数字（千位分隔）
 */
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

/**
 * 处理卡片点击 - 跳转到课程详情页
 */
const handleClick = () => {
  void router.push(`/courses/${String(props.course.id)}`)
}
</script>

<style scoped>
.course-card {
  background-color: #ffffff;
}

.icon-container {
  width: 56px;
  height: 56px;
  border: 1px solid #e5e5e5;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.course-description {
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
