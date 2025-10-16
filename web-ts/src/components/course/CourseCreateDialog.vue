<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import type { MainCategory, SubCategory } from '@/types/common'
  import type { CreateCourseRequest } from '@/types/course'
  import { courseNameRules, courseDescriptionRules, categoryRules } from '@/utils/validationRules'
  import { COURSE_VALIDATION } from '@/types/validation'

  const { t } = useI18n()

  interface Props {
    modelValue?: boolean
    categories?: MainCategory[]
    resetForm?: boolean
  }

  // Props
  const props = withDefaults(defineProps<Props>(), {
    modelValue: false,
    categories: () => [],
    resetForm: false,
  })

  interface Emits {
    (e: 'update:modelValue', value: boolean): void
    (e: 'submit', courseData: CreateCourseRequest): void
  }

  // Emits
  const emit = defineEmits<Emits>()

  // 响应式数据
  const applyCourseData = ref({
    name: '',
    description: '',
    mainCategoryId: null as number | null,
    subCategoryId: null as number | null,
  })

  // 表单引用
  const formRef = ref()

  // 表单是否有效
  const isFormValid = ref(false)

  // Computed properties for v-model
  const dialogModel = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  })

  // 获取当前选中主分类的子分类列表
  const getSubCategories = (): SubCategory[] => {
    if (!applyCourseData.value.mainCategoryId || !props.categories) {
      return []
    }

    const mainCategory = props.categories.find(
      (cat) => cat.id === applyCourseData.value.mainCategoryId
    )
    return mainCategory ? mainCategory.list || [] : []
  }

  // 监听主分类变化，清空子分类选择
  watch(
    () => applyCourseData.value.mainCategoryId,
    () => {
      applyCourseData.value.subCategoryId = null
    }
  )

  // 监听重置表单信号
  watch(
    () => props.resetForm,
    (newVal) => {
      if (newVal) {
        applyCourseData.value = {
          name: '',
          description: '',
          mainCategoryId: null,
          subCategoryId: null,
        }
      }
    }
  )

  // 处理表单提交
  const handleSubmit = async (): Promise<void> => {
    const { valid } = await formRef.value.validate()
    if (!valid) return

    const courseData: CreateCourseRequest = {
      name: applyCourseData.value.name,
      description: applyCourseData.value.description,
      mainCategory: applyCourseData.value.mainCategoryId,
      subCategory: applyCourseData.value.subCategoryId,
    }

    emit('submit', courseData)
  }

  // 关闭对话框
  const closeDialog = (): void => {
    dialogModel.value = false
  }
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="600px" persistent>
    <v-card rounded="lg">
      <v-card-title class="pa-6">
        <div class="d-flex align-center">
          <v-avatar color="primary" size="32" class="mr-3">
            <v-icon icon="mdi-plus-circle" color="white" size="16"></v-icon>
          </v-avatar>
          <span class="text-h6 font-weight-bold">{{ t('course.createNew') }}</span>
        </div>
      </v-card-title>

      <v-card-text class="px-6 pb-0">
        <v-form ref="formRef" v-model="isFormValid">
          <v-text-field
            v-model="applyCourseData.name"
            :label="t('course.name')"
            :rules="courseNameRules"
            :counter="COURSE_VALIDATION.NAME_MAX_LENGTH"
            variant="outlined"
            clearable
            required
            class="mb-4"
          >
          </v-text-field>

          <v-textarea
            v-model="applyCourseData.description"
            :label="t('course.description')"
            :rules="courseDescriptionRules"
            :counter="COURSE_VALIDATION.DESCRIPTION_MAX_LENGTH"
            variant="outlined"
            clearable
            required
            rows="3"
            class="mb-4"
          >
          </v-textarea>

          <v-select
            v-model="applyCourseData.mainCategoryId"
            :items="categories || []"
            item-title="name"
            item-value="id"
            :label="t('course.mainCategory')"
            :rules="categoryRules"
            variant="outlined"
            density="compact"
            class="mb-4"
            clearable
            required
          >
          </v-select>

          <v-select
            v-model="applyCourseData.subCategoryId"
            :items="getSubCategories()"
            item-title="name"
            item-value="id"
            :label="t('course.subCategory')"
            :rules="categoryRules"
            variant="outlined"
            density="compact"
            class="mb-4"
            :disabled="!applyCourseData.mainCategoryId"
            clearable
            required
          >
          </v-select>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="closeDialog">
          {{ t('common.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          :disabled="!isFormValid"
          @click="handleSubmit"
        >
          {{ t('course.create') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* Flat 风格 - 无阴影设计 */
</style>