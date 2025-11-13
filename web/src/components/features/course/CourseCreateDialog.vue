<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from '@/composables/useI18n'

interface Category {
  id: number
  name: string
  icon?: string
}

interface SubCategory {
  id: number
  name: string
  mainCategoryId: number
}

interface CreateCourseRequest {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
}

interface Props {
  modelValue?: boolean
  categories?: Category[]
  subCategories?: SubCategory[]
  resetForm?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', courseData: CreateCourseRequest): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  categories: () => [],
  subCategories: () => [],
  resetForm: false,
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

// 表单数据
const courseData = ref({
  name: '',
  description: '',
  mainCategoryId: null as number | null,
  subCategoryId: null as number | null,
})

// 表单引用
interface VForm {
  validate: () => Promise<{ valid: boolean }>
  resetValidation: () => void
}

const formRef = ref<VForm>()

// 表单是否有效
const isFormValid = ref(false)

// Dialog v-model
const dialogModel = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    emit('update:modelValue', value)
  },
})

// 获取当前选中主分类的子分类列表
const filteredSubCategories = computed(() => {
  if (!courseData.value.mainCategoryId) {
    return []
  }
  return props.subCategories.filter((sub) => sub.mainCategoryId === courseData.value.mainCategoryId)
})

// 验证规则
const nameRules = [
  (v: string) => !!v || '请输入课程名称',
  (v: string) => (v && v.length <= 100) || '课程名称不能超过100个字符',
]

const descriptionRules = [
  (v: string) => !!v || '请输入课程描述',
  (v: string) => (v && v.length <= 500) || '课程描述不能超过500个字符',
]

const categoryRules = [(v: number | null) => v !== null || '请选择分类']

// 监听主分类变化，清空子分类选择
watch(
  () => courseData.value.mainCategoryId,
  () => {
    courseData.value.subCategoryId = null
  }
)

// 监听重置表单信号
watch(
  () => props.resetForm,
  (newVal) => {
    if (newVal) {
      handleResetForm()
    }
  }
)

// 重置表单
const handleResetForm = () => {
  courseData.value = {
    name: '',
    description: '',
    mainCategoryId: null,
    subCategoryId: null,
  }
  if (formRef.value) {
    formRef.value.resetValidation()
  }
}

// 处理表单提交
const handleSubmit = async () => {
  if (!formRef.value) return

  const result = await formRef.value.validate()
  if (!result.valid) return

  const request: CreateCourseRequest = {
    name: courseData.value.name,
    description: courseData.value.description,
    mainCategory: courseData.value.mainCategoryId,
    subCategory: courseData.value.subCategoryId,
  }

  emit('submit', request)
}

// 关闭对话框
const closeDialog = () => {
  dialogModel.value = false
}
</script>

<template>
  <v-dialog v-model="dialogModel" max-width="560px" persistent>
    <v-card rounded="xl" class="create-dialog no-border" flat>
      <v-card-title class="pa-6 pb-4">
        <div class="d-flex align-center">
          <v-icon icon="mdi-book-plus" color="primary" size="24" class="mr-3" />
          <span class="text-h6 font-weight-bold text-grey-darken-4">{{
            t('course.createNew')
          }}</span>
        </div>
      </v-card-title>

      <v-card-text class="px-6 pb-6">
        <VForm ref="formRef" v-model="isFormValid">
          <!-- 课程名称 -->
          <div class="mb-4">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.name') }}
              <span class="text-error">*</span>
            </label>
            <v-text-field
              v-model="courseData.name"
              :rules="nameRules"
              placeholder="请输入课程名称"
              variant="outlined"
              density="compact"
              hide-details="auto"
              clearable
            />
          </div>

          <!-- 课程描述 -->
          <div class="mb-4">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.description') }}
              <span class="text-error">*</span>
            </label>
            <v-textarea
              v-model="courseData.description"
              :rules="descriptionRules"
              placeholder="请输入课程描述"
              variant="outlined"
              density="compact"
              hide-details="auto"
              rows="10"
              clearable
            />
          </div>

          <!-- 主分类 -->
          <div class="mb-4">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.mainCategory') }}
              <span class="text-error">*</span>
            </label>
            <v-select
              v-model="courseData.mainCategoryId"
              :items="categories"
              item-title="name"
              item-value="id"
              :rules="categoryRules"
              placeholder="请选择主分类"
              variant="outlined"
              density="compact"
              hide-details="auto"
              clearable
            >
              <template #prepend-inner>
                <v-icon icon="mdi-folder-outline" size="20" class="mr-1" />
              </template>
            </v-select>
          </div>

          <!-- 子分类 -->
          <div class="mb-2">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.subCategory') }}
              <span class="text-error">*</span>
            </label>
            <v-select
              v-model="courseData.subCategoryId"
              :items="filteredSubCategories"
              item-title="name"
              item-value="id"
              :rules="categoryRules"
              placeholder="请先选择主分类"
              variant="outlined"
              density="compact"
              hide-details="auto"
              :disabled="!courseData.mainCategoryId"
              clearable
            >
              <template #prepend-inner>
                <v-icon icon="mdi-folder-open-outline" size="20" class="mr-1" />
              </template>
            </v-select>
          </div>
        </VForm>
      </v-card-text>

      <v-card-actions class="px-6 pb-6 pt-2">
        <v-spacer />
        <v-btn variant="outlined" size="default" rounded="lg" class="px-6" @click="closeDialog">
          {{ t('common.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          size="default"
          rounded="lg"
          class="px-6"
          :disabled="!isFormValid"
          @click="handleSubmit"
        >
          {{ t('common.confirm') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.create-dialog {
  background-color: #ffffff;
}

/* 优化表单标签样式 */
label {
  font-size: 14px;
}

/* 移除输入框的额外边距 */
:deep(.v-field) {
  font-size: 14px;
}

:deep(.v-field__input) {
  min-height: 40px;
}
</style>
