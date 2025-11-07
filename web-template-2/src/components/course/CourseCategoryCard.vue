<script setup lang="ts">
import { computed } from 'vue'
import type { CourseCategory, Subcategory, CourseWithDisplay } from '@/types/profession'

interface Props {
  category: CourseCategory
  categoryIndex: number
  categoryMapping: Array<{ mainCategoryId: number; subcategories: Subcategory[] }>
  activeFirstLvl: number
  activeSecondLvl: number
  courses: CourseWithDisplay[]
  loading: boolean
}

interface Emits {
  (e: 'toggleFirstLevel', categoryIndex: number): void
  (e: 'selectSecondLevel', subcategoryIndex: number): void
  (e: 'openCourse', course: CourseWithDisplay): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isExpanded = computed(() => props.activeFirstLvl === props.categoryIndex)

const subcategories = computed(() => {
  const mapping = props.categoryMapping.find(
    (m) => m.mainCategoryId === props.category.id
  )
  return mapping?.subcategories || []
})

const handleToggle = (): void => {
  emit('toggleFirstLevel', props.categoryIndex)
}

const handleSelectSubcategory = (index: number): void => {
  emit('selectSecondLevel', index)
}

const handleCourseClick = (course: CourseWithDisplay): void => {
  emit('openCourse', course)
}
</script>

<template>
  <div class="mb-6">
    <!-- 分类标题行 -->
    <div
      class="category-header pa-4 cursor-pointer"
      :class="{ 'category-header-active': isExpanded }"
      @click="handleToggle"
    >
      <div class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon
            :icon="isExpanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
            size="20"
            class="mr-3"
            :color="isExpanded ? 'primary' : 'grey-darken-2'"
          ></v-icon>
          <v-avatar :color="isExpanded ? 'primary-lighten-4' : 'grey-lighten-3'" size="40" rounded="lg" class="mr-3">
            <v-icon :icon="category.icon" :color="isExpanded ? 'primary' : '#666666'" size="20"></v-icon>
          </v-avatar>
          <span class="text-h6" :class="isExpanded ? 'text-primary' : 'text-grey-darken-4'">
            {{ category.title }}
          </span>
        </div>

        <!-- 统计信息 -->
        <div class="d-flex align-center text-body-2 text-grey-darken-1">
          <v-icon icon="mdi-book-multiple" size="14" class="mr-1"></v-icon>
          <span class="mr-3">{{ Math.floor(Math.random() * 500) + 100 }}</span>
          <v-icon icon="mdi-account-multiple" size="14" class="mr-1"></v-icon>
          <span>{{ (Math.floor(Math.random() * 50000) + 10000).toLocaleString() }}</span>
        </div>
      </div>
    </div>

    <!-- 展开内容 -->
    <v-expand-transition>
      <div v-if="isExpanded && subcategories.length > 0">
        <!-- 二级分类按钮组 -->
        <div class="px-4 pt-7 pb-4">
          <div class="d-flex flex-wrap" style="gap: 8px">
            <v-btn
              v-for="(subcat, index) in subcategories"
              :key="subcat.id"
              :color="activeSecondLvl === index ? 'primary' : 'grey-lighten-3'"
              :variant="activeSecondLvl === index ? 'flat' : 'flat'"
              rounded="lg"
              size="default"
              class="subcategory-btn"
              @click="handleSelectSubcategory(index)"
            >
              {{ subcat.name }}
            </v-btn>
          </div>
        </div>

        <!-- 课程列表 -->
        <v-expand-transition>
          <div v-if="activeSecondLvl !== -1" class="px-4 pb-4 pt-3">

            <!-- 加载状态 -->
            <div v-if="loading" class="text-center py-8">
              <v-progress-circular indeterminate color="primary" size="40"></v-progress-circular>
            </div>

            <!-- 空状态 -->
            <div v-else-if="courses.length === 0" class="text-center py-8">
              <v-icon icon="mdi-book-open-blank-variant" size="48" color="grey-lighten-2" class="mb-2"></v-icon>
              <p class="text-body-2 text-grey">暂无课程</p>
            </div>

            <!-- 课程网格 -->
            <div v-else class="courses-grid">
              <v-card
                v-for="course in courses"
                :key="course.id"
                class="course-card"
                border
                rounded="lg"
                hover
                @click="handleCourseClick(course)"
              >
                <v-card-text class="pa-4">
                  <div class="d-flex align-center mb-3">
                    <v-avatar :color="course.iconColor" size="36" rounded="lg" class="mr-3">
                      <v-icon :icon="course.icon" color="white" size="18"></v-icon>
                    </v-avatar>
                    <div class="flex-grow-1">
                      <h3 class="text-body-1 font-weight-bold text-grey-darken-4 line-clamp-1">
                        {{ course.name }}
                      </h3>
                    </div>
                  </div>
                  <p class="text-body-2 text-grey-darken-2 line-clamp-2 mb-3">
                    {{ course.description }}
                  </p>
                  <div class="d-flex align-center text-caption text-grey">
                    <v-icon icon="mdi-account-group" size="14" class="mr-1"></v-icon>
                    {{ course.learnerCount?.toLocaleString() || 0 }} 人学习
                  </div>
                </v-card-text>
              </v-card>
            </div>
          </div>
        </v-expand-transition>
      </div>
    </v-expand-transition>
  </div>
</template>

<style scoped>
.category-header {
  border-bottom: 1px solid #E5E5E5;
  transition: all 0.2s ease;
}

.category-header:hover {
  background-color: #FAFAFA;
}

.cursor-pointer {
  cursor: pointer;
}

.subcategory-btn {
  font-weight: normal !important;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.course-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.8em;
}

@media (max-width: 960px) {
  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>
