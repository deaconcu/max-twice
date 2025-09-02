<script setup>
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// Props
const props = defineProps({
  courses: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['openCourse']);

// 处理课程点击
const handleCourseClick = (courseId) => {
  emit('openCourse', courseId);
};
</script>

<template>
  <div class="border-t-sm pt-3">
    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
      <p class="text-body-2 text-grey-darken-2 mt-2">{{ t('course.loading') }}</p>
    </div>
    
    <!-- 课程列表 -->
    <div v-else-if="courses.length > 0" class="mt-0">
      <v-row class="px-4 pb-4">
        <div v-for="course in courses" :key="course.id">
          <v-btn
            @click="handleCourseClick(course.id)" 
            class="hover-card mt-4 me-2 text-body-1"
            variant="flat"
            size="default"
            rounded="lg"
            :ripple="false"
            prepend-icon="mdi-play-circle-outline"
          >
            {{ course.name }}
            <v-chip size="x-small" class="ms-2" color="purple-lighten-2">
              {{ Math.floor(Math.random() * 9000) + 1000 }}人学习
            </v-chip>
          </v-btn>
        </div>
      </v-row>
    </div>
    
    <!-- 无课程提示 -->
    <div v-else class="text-center py-6">
      <v-icon icon="mdi-book-outline" size="48" color="grey-lighten-1" class="mb-3"></v-icon>
      <p class="text-body-1 text-grey-darken-2 mb-2">{{ t('course.noCourses') }}</p>
      <p class="text-body-2 text-grey-darken-1">{{ t('course.selectOther') }}</p>
    </div>
  </div>
</template>

<style scoped>
.hover-card {
  border: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.hover-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 课程按钮样式 */
.v-btn {
  text-transform: none !important;
  font-weight: 500 !important;
}

.v-chip:hover {
  transform: scale(1.02);
  transition: transform 0.2s ease;
}
</style>