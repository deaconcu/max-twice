<script setup lang="ts">
import { defineEmits, defineProps } from 'vue'
import { ContentState } from '@/enums'
import type { Course } from '@/types/course'
import type { MainCategory, SubCategory, CategoryMapping, StateConfig } from '@/types/common'

interface Props {
  course: Course
  mainCategories?: MainCategory[]
  categoryMapping?: CategoryMapping[]
}

interface Emits {
  (e: 'edit', course: Course): void
  (e: 'approve', course: Course): void
  (e: 'reject', course: Course): void
  (e: 'delete', course: Course): void
  (e: 'restore', course: Course): void
  (e: 'ban', course: Course): void
  (e: 'unban', course: Course): void
}

const props = withDefaults(defineProps<Props>(), {
  mainCategories: () => [],
  categoryMapping: () => [],
})

defineEmits<Emits>()

const getStateConfig = (state: number): StateConfig => {
  const configs: Record<number, StateConfig> = {
    [ContentState.SUBMITTED]: { text: '待审核', color: 'orange-lighten-4', icon: 'mdi-clock-outline' },
    [ContentState.PUBLISHED]: { text: '已通过', color: 'green-lighten-4', icon: 'mdi-check-circle' },
    [ContentState.REJECTED]: { text: '已拒绝', color: 'red-lighten-4', icon: 'mdi-close-circle' },
    [ContentState.BANNED]: { text: '已封禁', color: 'grey-lighten-2', icon: 'mdi-cancel' },
  }
  return configs[state] || { text: '未知', color: 'grey-lighten-3', icon: 'mdi-help-circle' }
}

// 根据主分类ID获取主分类名称
const getMainCategoryName = (mainCategoryId?: number): string | null => {
  if (!mainCategoryId || !props.mainCategories) return null
  const category = props.mainCategories.find((cat) => cat.id === mainCategoryId)
  return category ? category.name : null
}

// 根据主分类ID和子分类ID获取子分类名称
const getSubCategoryName = (mainCategoryId?: number, subCategoryId?: number): string | null => {
  if (!mainCategoryId || !subCategoryId || !props.categoryMapping) return null
  const mapping = props.categoryMapping.find((m) => m.mainCategoryId === mainCategoryId)
  if (!mapping || !mapping.subcategories) return null
  const subCategory = mapping.subcategories.find((sub) => sub.id === subCategoryId)
  return subCategory ? subCategory.name : null
}
</script>

<template>
  <div class="list-item mb-3">
    <div class="d-flex align-start">
      <!-- 操作区 -->
      <div class="action-area mr-4">
        <v-chip variant="flat" :color="getStateConfig(course.state || 0).color" size="small" class="mb-4 d-flex justify-center">
          {{ getStateConfig(course.state || 0).text }}
        </v-chip>

        <!-- 待审核 -->
        <div v-if="course.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
          <v-btn variant="tonal" color="success" size="small" block @click="$emit('approve', course)">
            批准
          </v-btn>
          <v-btn variant="tonal" color="error" size="small" block @click="$emit('reject', course)">
            拒绝
          </v-btn>
          <v-btn variant="tonal" color="grey" size="small" block @click="$emit('ban', course)">
            屏蔽
          </v-btn>
        </div>

        <!-- 已通过 -->
        <div v-if="course.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
          <v-btn variant="tonal" color="warning" size="small" block @click="$emit('reject', course)">
            撤回
          </v-btn>
          <v-btn variant="tonal" color="grey" size="small" block @click="$emit('ban', course)">
            屏蔽
          </v-btn>
        </div>

        <!-- 已拒绝 -->
        <div v-if="course.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
          <v-btn variant="tonal" color="success" size="small" block @click="$emit('approve', course)">
            通过
          </v-btn>
          <v-btn variant="tonal" color="grey" size="small" block @click="$emit('ban', course)">
            屏蔽
          </v-btn>
        </div>

        <!-- 已屏蔽 -->
        <div v-if="course.state === ContentState.BANNED" class="d-flex flex-column ga-2">
          <v-btn variant="tonal" color="info" size="small" block @click="$emit('unban', course)">
            解封
          </v-btn>
          <v-btn variant="tonal" color="warning" size="small" block @click="$emit('reject', course)">
            降级
          </v-btn>
        </div>

        <!-- 编辑按钮 -->
        <div class="mt-2">
          <v-btn variant="tonal" color="info" size="small" block @click="$emit('edit', course)">
            编辑
          </v-btn>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="flex-grow-1">
        <!-- 标题行 -->
        <div class="d-flex align-center justify-space-between mb-2">
          <div class="d-flex align-center">
            <div class="text-body-1 font-weight-medium text-grey-darken-3">
              {{ course.name }}
            </div>
            <v-chip v-if="course.parentCourse" variant="flat" color="teal-lighten-4" size="x-small" class="ml-2">
              子课程
            </v-chip>
          </div>
          <div class="text-caption text-grey-darken-1">ID: {{ course.id }}</div>
        </div>

        <!-- 元信息 -->
        <div class="d-flex align-center mb-2 text-caption text-grey-darken-1">
          <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
          <span>{{ course.creator?.name || '系统' }}</span>
          <span class="ml-2">{{ course.createdAt }}</span>
        </div>

        <!-- 内容 -->
        <div class="content-wrapper">
          <!-- 分类信息 -->
          <div v-if="course.mainCategory || course.subCategory" class="text-caption text-grey-darken-1 mb-2">
            <span>分类：</span>
            <span v-if="course.mainCategory">{{ getMainCategoryName(course.mainCategory) }}</span>
            <span v-if="course.mainCategory && course.subCategory"> | </span>
            <span v-if="course.subCategory">{{ getSubCategoryName(course.mainCategory, course.subCategory) }}</span>
          </div>

          <!-- 描述 -->
          <div class="text-body-2 text-grey-darken-1 mb-2">
            {{ course.description || '暂无描述' }}
          </div>

          <!-- 父课程 -->
          <div v-if="course.parentCourse" class="text-caption text-grey-darken-1">
            父课程：<a :href="`/read?courseId=${course.parentCourse.id}`" target="_blank">{{ course.parentCourse.name }}</a>
          </div>

          <!-- 拒绝原因 -->
          <div v-if="course.reason" class="mt-2">
            <span class="text-caption text-red-darken-2">拒绝原因：{{ course.reason }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}
</style>
