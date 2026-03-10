<template>
  <DefaultLayout>
    <div class="read-page">
      <!-- 课程头部 -->
      <div v-if="postDetailRef?.data" class="course-header-sticky">
        <div class="course-header-wrapper">
          <CourseHeader
            :parent-course-info="postDetailRef.data.parentCourse"
            :current-course="postDetailRef.data.course"
            :sub-course-list="postDetailRef.data.subCourseList"
            :is-main-course="isMainCourse"
            :is-learning="false"
          />
        </div>
      </div>

      <div class="read-content">
        <div class="center-right-container">
          <div class="center-right-wrapper">
            <PostDetail ref="postDetailRef" />
          </div>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import CourseHeader from '@/components/features/read/CourseHeader.vue'
import PostDetail from '@/components/features/read/PostDetail.vue'
import { useLearningTracker } from '@/composables/useLearningTracker'

// 学习追踪（停留5分钟+3次交互后上报）
useLearningTracker()

const postDetailRef = ref<InstanceType<typeof PostDetail> | null>(null)

// 是否为主课程
const isMainCourse = computed(() => {
  const data = postDetailRef.value?.data
  if (data?.course && data.parentCourse) {
    return data.course.id === data.parentCourse.id
  }
  return true
})
</script>

<style scoped>
.read-page {
  min-height: 100vh;
  background-color: #ffffff;
}

/* 固定课程头部 */
.course-header-sticky {
  position: sticky;
  top: 56px;
  background-color: white;
  z-index: 999;
  padding-bottom: 8px;
  max-width: 1470px;
  margin: 0 auto;
}

.course-header-wrapper {
  max-width: 1110px;
  margin: 0 auto;
  padding: 0 26px;
}

/* 布局 - 无左侧目录 */
.read-content {
  display: flex;
  position: relative;
  z-index: 1;
  max-width: 1470px;
  width: 100%;
  margin: 0 auto;
}

/* 中间+右侧容器包装 */
.center-right-container {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.center-right-wrapper {
  display: flex;
  flex: 1;
  justify-content: center;
  max-width: 100%;
}

/* 中等屏幕 */
@media (max-width: 1700px) {
  .course-header-wrapper {
    max-width: 750px;
  }

  .center-right-wrapper {
    justify-content: center;
  }
}

/* 小屏幕 */
@media (max-width: 1280px) and (min-width: 751px) {
  .course-header-sticky {
    max-width: 750px;
  }

  .read-content {
    max-width: 750px;
  }

  .center-right-wrapper {
    justify-content: center;
  }

  .course-header-wrapper {
    max-width: 750px;
    margin: 0 auto;
    padding: 0 !important;
  }
}

/* 超小屏幕 */
@media (max-width: 750px) {
  .course-header-sticky {
    max-width: none;
  }

  .read-content {
    max-width: none;
    width: 100% !important;
  }

  .course-header-wrapper {
    padding: 0 !important;
    max-width: none !important;
    margin: 0 !important;
  }

  .center-right-container {
    width: 100% !important;
  }

  .center-right-wrapper {
    width: 100% !important;
    max-width: none !important;
  }
}
</style>
