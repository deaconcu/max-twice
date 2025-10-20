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
  <div class="mb-5">
    <v-card border rounded="xl" class="category-card">
      <v-card-text class="pa-5">
        <!-- 分类标题行 -->
        <div class="d-flex align-center">
          <v-btn
            variant="text"
            :ripple="false"
            class="category-title-btn pa-2"
            :color="isExpanded ? 'primary' : 'grey-darken-2'"
            @click="handleToggle"
          >
            <v-icon
              :icon="isExpanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
              size="24"
              class="mr-2"
            ></v-icon>
            <v-icon :icon="category.icon" size="24" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">{{ category.title }}</span>
          </v-btn>

          <!-- 统计信息 -->
          <div class="d-flex align-center ml-4 text-body-2 text-grey-darken-1 stats-info">
            <v-icon icon="mdi-book-multiple" size="16" color="primary" class="mr-1"></v-icon>
            <span class="mr-4">{{ Math.floor(Math.random() * 500) + 100 }}门课程</span>
            <v-icon icon="mdi-account-multiple" size="16" color="success" class="mr-1"></v-icon>
            <span>{{ Math.floor(Math.random() * 50000) + 10000 }}人学习</span>
          </div>
        </div>

        <!-- 展开内容 -->
        <v-expand-transition>
          <div v-if="isExpanded && subcategories.length > 0" class="mt-4">
            <!-- 二级分类按钮组 -->
            <div class="subcategory-group mb-4">
              <v-btn
                v-for="(subcat, index) in subcategories"
                :key="subcat.id"
                :color="activeSecondLvl === index ? 'primary' : 'grey-lighten-3'"
                :variant="activeSecondLvl === index ? 'flat' : 'flat'"
                rounded="lg"
                class="ma-1 subcategory-btn"
                @click="handleSelectSubcategory(index)"
              >
                {{ subcat.name }}
              </v-btn>
            </div>

            <!-- 课程列表 -->
            <v-expand-transition>
              <div v-if="activeSecondLvl !== -1" class="courses-section">
                <v-divider class="mb-4"></v-divider>

                <!-- 加载状态 -->
                <div v-if="loading" class="text-center py-8">
                  <v-progress-circular
                    indeterminate
                    color="primary"
                    size="48"
                  ></v-progress-circular>
                  <p class="text-body-2 text-grey mt-4">加载中...</p>
                </div>

                <!-- 课程列表 -->
                <div v-else-if="courses.length > 0" class="courses-list">
                  <v-btn
                    v-for="course in courses"
                    :key="course.id"
                    variant="outlined"
                    rounded="lg"
                    class="ma-1 course-btn"
                    @click="handleCourseClick(course)"
                  >
                    <v-icon icon="mdi-play-circle-outline" size="18" class="mr-2"></v-icon>
                    {{ course.name }}
                    <v-chip
                      v-if="course.learnerCount"
                      size="x-small"
                      color="purple-lighten-2"
                      class="ml-2"
                    >
                      {{ course.learnerCount }}人学习
                    </v-chip>
                  </v-btn>
                </div>

                <!-- 空状态 -->
                <div v-else class="text-center py-8">
                  <v-icon icon="mdi-book-outline" size="64" color="grey-lighten-1" class="mb-3"></v-icon>
                  <p class="text-body-1 text-grey-darken-2">暂无课程</p>
                  <p class="text-body-2 text-grey">请选择其他子分类查看</p>
                </div>
              </div>
            </v-expand-transition>
          </div>
        </v-expand-transition>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.category-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  transition: all 0.2s ease;
}

.category-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.category-title-btn {
  text-transform: none;
  font-size: 1.125rem;
  height: auto !important;
}

.stats-info {
  align-self: center;
}

.subcategory-group {
  display: flex;
  flex-wrap: wrap;
}

.subcategory-btn {
  text-transform: none;
  font-weight: 500;
  border: 1px solid #EDEFF1;
}

.courses-section {
  background-color: #FAFBFC;
  border-radius: 12px;
  padding: 16px;
}

.courses-list {
  display: flex;
  flex-wrap: wrap;
}

.course-btn {
  text-transform: none;
  font-weight: 500;
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
  transition: all 0.2s ease;
}

.course-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}
</style>
