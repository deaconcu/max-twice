<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import type { MainCategory, SubCategory } from '@/types/common'
  import type { CreateCourseRequest } from '@/types/course'

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
  const handleSubmit = (): void => {
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
  <v-dialog
    :model-value="modelValue"
    width="800"
    height="620"
    content-class="fix-dialog"
    @update:model-value="dialogModel = $event"
  >
    <v-card class="px-1 py-2" rounded="lg">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-file-cog-outline" size="small" class=""></v-icon>
        <span class="ps-2">{{ t('course.createNew') }}</span>
      </v-card-title>
      <v-card-subtitle>
        {{ t('course.fillInfo') }}
      </v-card-subtitle>
      <v-card-text class="px-4 py-8">
        <v-text-field
          v-model="applyCourseData.name"
          :label="t('course.name')"
          variant="outlined"
          density="compact"
          class="mb-4"
          required
        >
        </v-text-field>

        <v-textarea
          v-model="applyCourseData.description"
          :label="t('course.description')"
          variant="outlined"
          density="compact"
          rows="4"
          class="mb-4"
          required
        >
        </v-textarea>

        <v-select
          v-model="applyCourseData.mainCategoryId"
          :items="categories || []"
          item-title="name"
          item-value="id"
          :label="t('course.mainCategory')"
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
          variant="outlined"
          density="compact"
          class="mb-4"
          :disabled="!applyCourseData.mainCategoryId"
          clearable
          required
        >
        </v-select>
      </v-card-text>
      <v-card-actions class="justify-center">
        <v-btn text="取消" class="px-4" variant="outlined" @click="closeDialog"></v-btn>
        <v-btn
          :text="t('course.create')"
          class="px-4"
          color="primary"
          @click="handleSubmit"
        ></v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
  :deep(.fix-dialog) {
    top: 150px !important;
    position: absolute !important;
  }
</style>