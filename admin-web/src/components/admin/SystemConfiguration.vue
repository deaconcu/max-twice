<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { useFetch, useMutation } from '@/composables'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 响应式数据
const courseCategories = ref<string>('')
const professionCategories = ref<string>('')
const courseUpdatedAt = ref<string>('')
const professionUpdatedAt = ref<string>('')

// 对话框状态
const dialog = ref(false)
const dialogKey = ref<'courseCategories' | 'professionCategories'>('courseCategories')
const dialogTitle = ref('')
const dialogValue = ref('')

// 使用 useFetch 加载配置
const { loading: loadingConfig } = useFetch({
  fetchFn: adminApi.getSystemConfig,
  immediate: true,
  onSuccess: (data: Array<{ key: string; value: string; updatedAt: string }>) => {
    if (data) {
      for (const item of data) {
        const formatted = (() => {
          try {
            const parsed = typeof item.value === 'object' ? item.value : JSON.parse(item.value)
            return JSON.stringify(parsed, null, 2)
          } catch {
            return item.value
          }
        })()

        if (item.key === 'courseCategories') {
          courseCategories.value = formatted
          courseUpdatedAt.value = item.updatedAt
        } else if (item.key === 'professionCategories') {
          professionCategories.value = formatted
          professionUpdatedAt.value = item.updatedAt
        }
      }
    }
  },
  onError: (error: any) => {
    console.error('加载配置时发生错误:', error)
  },
})

// 打开对话框
const openDialog = (key: 'courseCategories' | 'professionCategories'): void => {
  dialogKey.value = key
  dialogTitle.value = key === 'courseCategories' ? '课程分类' : '职业分类'
  dialogValue.value = key === 'courseCategories' ? courseCategories.value : professionCategories.value
  dialog.value = true
}

// 格式化 JSON
const formatDialogValue = (): void => {
  try {
    const parsed = JSON.parse(dialogValue.value)
    dialogValue.value = JSON.stringify(parsed, null, 2)
  } catch {
    showSnackbar?.('JSON 格式无效，无法格式化', 'error')
  }
}

// 检查对话框内容是否有效
const isDialogValueValid = (): boolean => {
  if (!dialogValue.value.trim()) return false
  try {
    JSON.parse(dialogValue.value)
    return true
  } catch {
    return false
  }
}

// 使用 useMutation 保存单项配置
const { execute: saveConfig, loading: saving } = useMutation(
  (data: { key: string; value: string }) => adminApi.updateConfigByKey(data.key, data.value),
  {
    successMessage: '配置已保存',
    onSuccess: (_, data) => {
      if (data.key === 'courseCategories') {
        courseCategories.value = dialogValue.value
        courseUpdatedAt.value = new Date().toISOString()
      } else {
        professionCategories.value = dialogValue.value
        professionUpdatedAt.value = new Date().toISOString()
      }
      dialog.value = false
    },
  }
)

const saveDialogConfig = async (): Promise<void> => {
  if (!isDialogValueValid()) {
    showSnackbar?.('JSON 格式无效', 'error')
    return
  }
  await saveConfig({ key: dialogKey.value, value: dialogValue.value })
}
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">系统配置</h2>

    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-cog-outline" size="18" class="mr-2"></v-icon>
        配置项
      </v-card-title>
      <v-card-text class="pa-0">
        <!-- 加载状态 -->
        <div v-if="loadingConfig" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <v-list v-else>
          <!-- 课程分类 -->
          <v-list-item
            prepend-icon="mdi-book-outline"
            title="课程分类"
            :subtitle="courseUpdatedAt ? `上次更新: ${new Date(courseUpdatedAt).toLocaleString('zh-CN')}` : '课程主分类与子分类 JSON 配置'"
            @click="openDialog('courseCategories')"
          >
            <template #append>
              <div class="d-flex align-center ga-2">
                <v-chip
                  v-if="courseCategories"
                  size="x-small"
                  variant="tonal"
                  color="green"
                >
                  已配置
                </v-chip>
                <v-chip v-else size="x-small" variant="tonal" color="orange">未配置</v-chip>
                <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
              </div>
            </template>
          </v-list-item>

          <v-divider></v-divider>

          <!-- 职业分类 -->
          <v-list-item
            prepend-icon="mdi-briefcase-outline"
            title="职业分类"
            :subtitle="professionUpdatedAt ? `上次更新: ${new Date(professionUpdatedAt).toLocaleString('zh-CN')}` : '职业主分类与子分类 JSON 配置'"
            @click="openDialog('professionCategories')"
          >
            <template #append>
              <div class="d-flex align-center ga-2">
                <v-chip
                  v-if="professionCategories"
                  size="x-small"
                  variant="tonal"
                  color="green"
                >
                  已配置
                </v-chip>
                <v-chip v-else size="x-small" variant="tonal" color="orange">未配置</v-chip>
                <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
              </div>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>

    <!-- 编辑对话框 -->
    <v-dialog v-model="dialog" max-width="900px" persistent>
      <v-card variant="flat" rounded="lg">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <v-icon icon="mdi-code-json" color="blue-darken-1" class="mr-3"></v-icon>
              <span class="text-h6 font-weight-bold">编辑 {{ dialogTitle }}</span>
            </div>
            <v-btn icon variant="text" @click="dialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </div>
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-textarea
            v-model="dialogValue"
            :label="`${dialogTitle} JSON 配置`"
            variant="outlined"
            rounded="lg"
            bg-color="grey-lighten-5"
            :error="dialogValue.trim().length > 0 && !isDialogValueValid()"
            :error-messages="dialogValue.trim().length > 0 && !isDialogValueValid() ? 'JSON 格式无效' : ''"
            class="config-textarea"
          ></v-textarea>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-btn variant="tonal" color="grey" @click="formatDialogValue">
            <v-icon icon="mdi-code-json" class="mr-1"></v-icon>
            格式化
          </v-btn>
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" @click="dialog = false">取消</v-btn>
          <v-btn
            variant="flat"
            color="primary"
            :loading="saving"
            :disabled="!isDialogValueValid()"
            @click="saveDialogConfig"
          >
            <v-icon icon="mdi-content-save" class="mr-1"></v-icon>
            保存
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.config-textarea :deep(.v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
  line-height: 1.4 !important;
  min-height: calc(100vh - 300px) !important;
}
</style>
