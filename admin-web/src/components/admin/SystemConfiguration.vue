<script setup lang="ts">
import { inject, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminApi } from '@/api'
import { useFetch, useMutation } from '@/composables'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 响应式数据
const courseCategories = ref<string>('')
const professionCategories = ref<string>('')
const lastUpdateTime = ref<string>('')

// 使用 useFetch 加载配置
const { loading: loadingConfig } = useFetch({
  fetchFn: adminApi.getSystemConfig,
  immediate: true,
  onSuccess: (data) => {
    if (data) {
      // 处理课程分类配置
      if (data.courseCategories) {
        try {
          if (typeof data.courseCategories === 'object') {
            courseCategories.value = JSON.stringify(data.courseCategories, null, 2)
          } else {
            const parsedCourse = JSON.parse(data.courseCategories)
            courseCategories.value = JSON.stringify(parsedCourse, null, 2)
          }
        } catch {
          courseCategories.value = data.courseCategories
        }
      } else {
        courseCategories.value = ''
      }

      // 处理职业分类配置
      if (data.professionCategories) {
        try {
          if (typeof data.professionCategories === 'object') {
            professionCategories.value = JSON.stringify(data.professionCategories, null, 2)
          } else {
            const parsedProfession = JSON.parse(data.professionCategories)
            professionCategories.value = JSON.stringify(parsedProfession, null, 2)
          }
        } catch {
          professionCategories.value = data.professionCategories
        }
      } else {
        professionCategories.value = ''
      }

      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    }
  },
  onError: (error: any) => {
    console.error('加载配置时发生错误:', error)
  },
})

// 使用 useMutation 保存配置
const { execute: saveConfig, loading: saving } = useMutation(
  async () => {
    const promises: Promise<any>[] = []

    if (courseCategories.value.trim()) {
      promises.push(adminApi.updateConfigByKey('courseCategories', courseCategories.value))
    }
    if (professionCategories.value.trim()) {
      promises.push(adminApi.updateConfigByKey('professionCategories', professionCategories.value))
    }

    const responses = await Promise.all(promises)
    const allSuccess = responses.every((response) => response.code === 200)

    if (!allSuccess) {
      const failedResponses = responses.filter((response) => response.code !== 200)
      throw new Error(failedResponses.map((r) => r.message).join(', '))
    }

    // 返回符合 ApiResponse 格式的对象
    return {
      code: 200,
      message: '配置保存成功',
      data: { success: true },
    }
  },
  {
    successMessage: '系统配置已保存',
    onSuccess: () => {
      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    },
  }
)

// 保存配置
const saveConfiguration = async (): Promise<void> => {
  if (!isValidConfig.value) {
    return
  }
  await saveConfig()
}
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">系统配置</h2>

    <v-row>
      <!-- 课程类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="border">
          <v-card-title class="d-flex align-center">
            <v-icon icon="mdi-book-outline" color="blue-darken-1" size="18" class="mr-2"></v-icon>
            课程分类
          </v-card-title>
          <v-card-text>
            <v-textarea
              v-model="courseCategories"
              label="课程分类 JSON 配置"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              placeholder='请输入课程分类 JSON 配置，例如：&#10;{&#10;  "backend": "后端开发",&#10;  "frontend": "前端开发"&#10;}'
              hint="请输入有效的 JSON 格式配置"
              persistent-hint
              class="config-textarea"
            ></v-textarea>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- 职业类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="border">
          <v-card-title class="d-flex align-center">
            <v-icon icon="mdi-briefcase-outline" color="orange-darken-1" size="18" class="mr-2"></v-icon>
            职业分类
          </v-card-title>
          <v-card-text>
            <v-textarea
              v-model="professionCategories"
              label="职业分类 JSON 配置"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              placeholder='请输入职业分类 JSON 配置，例如：&#10;{&#10;  "engineer": "软件工程师",&#10;  "designer": "设计师"&#10;}'
              hint="请输入有效的 JSON 格式配置"
              persistent-hint
              class="config-textarea"
            ></v-textarea>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- 操作按钮 -->
    <div class="d-flex align-center justify-space-between mt-4">
      <div>
        <v-btn
          variant="tonal"
          color="teal"
          class="mr-3"
          :loading="saving"
          :disabled="!isValidConfig"
          @click="saveConfiguration"
        >
          <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
          保存配置
        </v-btn>
        <v-btn variant="tonal" color="grey" @click="formatConfig">
          <v-icon icon="mdi-code-json" class="mr-2"></v-icon>
          格式化
        </v-btn>
      </div>
      <div v-if="lastUpdateTime" class="text-caption text-grey-darken-1">
        最后更新: {{ lastUpdateTime }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 文本域样式优化 */
.config-textarea :deep(.v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
  line-height: 1.4 !important;
  min-height: calc(100vh - 300px) !important;
}
</style>
