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
    [ContentState.SUBMITTED]: { text: '待审核', color: 'warning', icon: 'mdi-clock-outline' },
    [ContentState.PUBLISHED]: { text: '已通过', color: 'success', icon: 'mdi-check-circle' },
    [ContentState.REJECTED]: { text: '已拒绝', color: 'error', icon: 'mdi-close-circle' },
    [ContentState.BANNED]: { text: '已封禁', color: 'grey', icon: 'mdi-cancel' },
  }
  return configs[state] || { text: '未知', color: 'grey', icon: 'mdi-help-circle' }
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
  if (!mapping || !mapping.subCategories) return null
  const subCategory = mapping.subCategories.find((sub) => sub.id === subCategoryId)
  return subCategory ? subCategory.name : null
}
</script>

<template>
  <v-card flat class="course-card border rounded-lg pa-4 mb-3" hover elevation="0">
    <v-row no-gutters>
      <!-- 左侧：状态和操作按钮 -->
      <v-col cols="12" md="2" class="pr-md-4 mb-3 mb-md-0">
        <div class="d-flex flex-column h-100">
          <!-- 状态标签 -->
          <div class="mb-3">
            <v-chip
              :color="getStateConfig(course.state || 0).color"
              variant="flat"
              rounded="lg"
              size="small"
              class="font-weight-medium"
            >
              <v-icon
                :icon="getStateConfig(course.state || 0).icon"
                size="14"
                class="mr-1"
              ></v-icon>
              {{ getStateConfig(course.state || 0).text }}
            </v-chip>
          </div>

          <!-- 操作按钮组 -->
          <div class="d-flex flex-column ga-2">
            <!-- 编辑按钮 -->
            <v-btn
              variant="flat"
              color="blue-lighten-4"
              rounded="lg"
              size="small"
              class="text-none"
              @click="$emit('edit', course)"
            >
              <v-icon icon="mdi-pencil" color="blue-darken-2" size="16" class="mr-1"></v-icon>
              编辑
            </v-btn>

            <!-- 待审核状态：批准、拒绝、屏蔽 -->
            <template v-if="course.state === ContentState.SUBMITTED">
              <!-- 批准按钮 -->
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('approve', course)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                批准
              </v-btn>

              <!-- 拒绝按钮 -->
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('reject', course)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                拒绝
              </v-btn>

              <!-- 屏蔽按钮 -->
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('ban', course)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </template>

            <!-- 已批准状态：撤销通过、屏蔽 -->
            <template v-if="course.state === ContentState.PUBLISHED">
              <!-- 撤销通过按钮 -->
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('reject', course)"
              >
                <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                撤销通过
              </v-btn>

              <!-- 屏蔽按钮 -->
              <v-btn
                variant="flat"
                color="grey-lighten-2"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('ban', course)"
              >
                <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                屏蔽
              </v-btn>
            </template>

            <!-- 已拒绝状态：通过 -->
            <template v-if="course.state === ContentState.REJECTED">
              <!-- 通过按钮 -->
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('approve', course)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
            </template>

            <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
            <template v-if="course.state === ContentState.BANNED">
              <!-- 取消屏蔽按钮 -->
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('unban', course)"
              >
                <v-icon icon="mdi-lock-open" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                取消屏蔽
              </v-btn>

              <!-- 降级为拒绝按钮 -->
              <v-btn
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                class="text-none"
                @click="$emit('reject', course)"
              >
                <v-icon
                  icon="mdi-arrow-down"
                  color="orange-darken-2"
                  size="16"
                  class="mr-1"
                ></v-icon>
                降级为拒绝
              </v-btn>
            </template>

            <!-- 拒绝原因显示 -->
            <div v-if="course.reason" class="mt-3">
              <div class="rejection-reason">
                <div class="text-body-2 text-red-darken-1">
                  <div class="d-flex align-center">
                    <v-icon
                      icon="mdi-alert-circle"
                      size="14"
                      color="red-darken-1"
                      class="mr-1"
                    ></v-icon>
                    <strong>拒绝原因：</strong>
                  </div>
                  <div class="rejection-text">{{ course.reason }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </v-col>

      <!-- 右侧：课程详细信息 -->
      <v-col cols="12" md="10">
        <div class="course-info">
          <!-- 课程标题行 -->
          <div class="d-flex align-center mb-3">
            <h4 class="font-weight-bold text-grey-darken-3 mr-3 course-title">
              {{ course.name }}
            </h4>
            <div v-if="course.parentCourse" class="d-flex align-center">
              <v-chip
                v-if="course.parentCourse"
                variant="flat"
                size="x-small"
                rounded="lg"
                color="teal"
              >
                子课程
              </v-chip>
              <span class="text-caption text-grey-darken-1 ps-4">
                父课程：<a :href="`/read?courseId=${course.parentCourse.id}`" target="_blank">
                  {{ course.parentCourse.name }}
                </a>
              </span>
            </div>
          </div>

          <!-- 分类信息行 -->
          <div class="d-flex align-center mb-3">
            <v-chip
              v-if="course.mainCategory"
              variant="flat"
              color="grey-lighten-4"
              rounded="lg"
              size="small"
              class="mr-2"
            >
              <v-icon icon="mdi-folder" color="blue-darken-2" size="14" class="mr-2"></v-icon>
              <span class="text-blue-darken-2">{{ getMainCategoryName(course.mainCategory) }}</span>
            </v-chip>
            <v-chip
              v-if="course.subCategory"
              variant="flat"
              color="grey-lighten-4"
              rounded="lg"
              size="small"
            >
              <v-icon
                icon="mdi-folder-multiple"
                color="green-darken-2"
                size="14"
                class="mr-2"
              ></v-icon>
              <span class="text-green-darken-2">{{
                getSubCategoryName(course.mainCategory, course.subCategory)
              }}</span>
            </v-chip>
            <v-chip
              v-if="!course.mainCategory && !course.subCategory"
              variant="outlined"
              color="grey"
              rounded="lg"
              size="small"
            >
              <v-icon icon="mdi-folder-off" color="grey-darken-1" size="14" class="mr-1"></v-icon>
              <span class="text-grey-darken-1">未分类</span>
            </v-chip>
          </div>

          <!-- 课程信息行 -->
          <div class="course-details">
            <v-row no-gutters class="mb-3">
              <v-col cols="12">
                <div class="d-flex align-start">
                  <v-icon
                    icon="mdi-text-long"
                    size="16"
                    color="grey-darken-1"
                    class="mr-2 mt-1"
                  ></v-icon>
                  <span class="text-body-2 text-grey-darken-1">
                    <strong>描述：</strong>{{ course.description || '暂无描述' }}
                  </span>
                </div>
              </v-col>
            </v-row>

            <v-row v-if="course.parentCourse" no-gutters class="mb-2">
              <v-col cols="12">
                <div class="d-flex align-center"></div>
              </v-col>
            </v-row>

            <v-row no-gutters class="mb-0">
              <v-col cols="12">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-account" size="16" color="grey-darken-1" class="mr-2"></v-icon>
                  <span class="text-body-2 text-grey-darken-1">
                    <strong>创建者：</strong>{{ course.creator?.name || '系统' }}
                  </span>
                </div>
              </v-col>
            </v-row>
          </div>

          <!-- 时间信息 -->
          <div class="course-timestamp d-flex align-center">
            <v-icon icon="mdi-clock-outline" size="14" color="grey-lighten-1" class="mr-2"></v-icon>
            <span class="text-caption text-grey-darken-1"> 申请时间：{{ course.createdAt }} </span>
            <span
              v-if="course.updatedAt && course.updatedAt !== course.createdAt"
              class="mx-2 text-grey-lighten-1"
              >|</span
            >
            <span
              v-if="course.updatedAt && course.updatedAt !== course.createdAt"
              class="text-caption text-grey-darken-1"
            >
              更新时间：{{ course.updatedAt }}
            </span>
          </div>
        </div>
      </v-col>
    </v-row>
  </v-card>
</template>

<style scoped>
.course-card {
  transition: all 0.2s ease-in-out;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  background: #fafafa;
  min-height: 100px;
}

.course-card:hover {
  border-color: rgba(25, 118, 210, 0.2) !important;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.1) !important;
  background: #ffffff;
}

.course-title {
  line-height: 1;
  word-break: break-word;
  max-width: 100%;
}

.course-info {
  width: 100%;
}

.course-details {
  background: rgba(245, 245, 245, 0.8);
  border-radius: 8px;
  padding: 12px;
  border-left: 3px solid #e3f2fd;
}

.course-timestamp {
  margin-top: 12px;
}

/* 响应式调整 */
@media (max-width: 960px) {
  .course-card {
    margin-bottom: 16px !important;
  }

  .course-details {
    margin-top: 12px;
  }
}

/* 按钮样式优化 */
.v-btn.text-none {
  text-transform: none !important;
  letter-spacing: normal !important;
}

/* 拒绝原因样式 */
.rejection-reason {
  display: flex;
  align-items: flex-start;
  background: rgba(255, 245, 245, 0.8);
  border-radius: 6px;
  padding: 8px;
  border-left: 3px solid #ffcdd2;
  max-width: 100%;
}

.rejection-text {
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  line-height: 1.4;
  margin-top: 2px;
  overflow-wrap: break-word;
  hyphens: auto;
}
</style>
