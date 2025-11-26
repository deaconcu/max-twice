<template>
  <v-dialog v-model="dialogModel" max-width="560px" persistent>
    <v-card rounded="xl" class="application-dialog no-border" flat>
      <v-card-title class="pa-6 pb-4">
        <div class="d-flex align-center">
          <v-icon icon="mdi-book-plus" color="primary" size="24" class="mr-3" />
          <span class="text-h6 font-weight-bold text-grey-darken-4">
            {{ t('course.applySubCourse') }}
          </span>
        </div>
      </v-card-title>

      <v-card-text class="px-6 pb-6">
        <VForm ref="formRef" v-model="isFormValid">
          <!-- 子课程名称 -->
          <div class="mb-4">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.name') }}
              <span class="text-error">*</span>
            </label>
            <v-text-field
              v-model="formData.name"
              :rules="courseNameRules"
              :counter="courseNameMaxLength"
              placeholder="请输入子课程名称"
              variant="outlined"
              density="compact"
              clearable
            />
          </div>

          <!-- 子课程描述 -->
          <div class="mb-2">
            <label class="text-body-2 font-weight-medium text-grey-darken-3 mb-2 d-block">
              {{ t('course.description') }}
              <span class="text-error">*</span>
            </label>
            <v-textarea
              v-model="formData.description"
              :rules="courseDescriptionRules"
              :counter="courseDescriptionMaxLength"
              placeholder="请输入子课程描述"
              variant="outlined"
              density="compact"
              rows="10"
              clearable
            />
          </div>
        </VForm>
      </v-card-text>

      <v-card-actions class="px-6 pb-6 pt-2">
        <v-spacer />
        <v-btn variant="outlined" size="default" rounded="lg" class="px-6" @click="handleClose">
          {{ t('common.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          size="default"
          rounded="lg"
          class="px-6"
          :disabled="!isFormValid"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ t('common.confirm') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from '@/composables/useI18n'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'

interface Props {
  modelValue?: boolean
  parentCourseId?: number
  submitting?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', data: { name: string; description: string }): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  parentCourseId: 0,
  submitting: false,
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

// 验证规则
const courseNameRules = useValidationRules('course-name')
const courseDescriptionRules = useValidationRules('course-description')
const courseNameMaxLength = useMaxLength('course-name')
const courseDescriptionMaxLength = useMaxLength('course-description')

// 表单数据
const formData = ref({
  name: '',
  description: '',
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

// 重置表单
const handleResetForm = () => {
  formData.value = {
    name: '',
    description: '',
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

  emit('submit', {
    name: formData.value.name,
    description: formData.value.description,
  })
}

// 关闭对话框
const handleClose = () => {
  dialogModel.value = false
  handleResetForm()
}

// 监听对话框关闭，重置表单
watch(
  () => props.modelValue,
  (newVal) => {
    if (!newVal) {
      setTimeout(handleResetForm, 200)
    }
  }
)
</script>

<style scoped>
.application-dialog {
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
