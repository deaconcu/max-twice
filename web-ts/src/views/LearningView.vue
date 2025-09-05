<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RightSidebar from '@/components/common/RightSidebar.vue'
import LearningHeader from '@/components/learning/LearningHeader.vue'
import RoadmapLearningContainer from '@/components/learning/RoadmapLearningContainer.vue'
import CourseLearningContainer from '@/components/learning/CourseLearningContainer.vue'

const { t } = useI18n()

// 响应式数据
const selectedTab = ref<string>('roadmaps') // 默认显示学习路线图
const searchQuery = ref<string>('')
const selectedNavTab = ref<string>('learning') // 导航栏当前选中项
const selectedRoadmapStatus = ref<string>('all') // 路线图状态筛选
const selectedCourseStatus = ref<string>('all') // 课程状态筛选

// 计算属性：根据当前选中的tab返回对应的状态值
const selectedStatus = computed({
  get: (): string => {
    return selectedTab.value === 'roadmaps'
      ? selectedRoadmapStatus.value
      : selectedCourseStatus.value
  },
  set: (value: string): void => {
    if (selectedTab.value === 'roadmaps') {
      selectedRoadmapStatus.value = value
    } else {
      selectedCourseStatus.value = value
    }
  },
})
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="9" class="pr-8">
        <!-- 页面头部 -->
        <LearningHeader
          v-model:selected-tab="selectedTab"
          v-model:selected-nav-tab="selectedNavTab"
          v-model:selected-status="selectedStatus"
          v-model:search-query="searchQuery"
        />

        <!-- 学习路线图标签页 -->
        <div v-if="selectedTab === 'roadmaps'">
          <RoadmapLearningContainer
            :search-query="searchQuery"
            :selected-status="selectedRoadmapStatus"
          />
        </div>

        <!-- 课程标签页 -->
        <div v-if="selectedTab === 'courses'">
          <CourseLearningContainer
            :search-query="searchQuery"
            :selected-status="selectedCourseStatus"
          />
        </div>
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="3">
        <RightSidebar :exclude-modules="['learning']" />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
/* 基本样式 */
.text-h7 {
  font-size: 1.15rem;
}

/* 改善字体渲染和清晰度 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 增加hover效果但保持flat风格 */
.v-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}

.v-chip:hover {
  transform: scale(1.02);
  transition: transform 0.2s ease;
}
</style>