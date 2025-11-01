<template>
  <div class="system-configuration">
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-cog-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('systemConfiguration.title') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('systemConfiguration.subtitle') }}</p>
        </div>
      </div>
      <v-chip variant="flat" color="green-lighten-4" rounded="lg">
        <v-icon icon="mdi-check-circle" color="green-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-green-darken-2 text-caption">{{
          t('systemConfiguration.configValid')
        }}</span>
      </v-chip>
    </div>

    <v-row>
      <!-- 课程类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="pa-4" rounded="lg" outlined>
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-book-outline" color="blue-darken-1" class="mr-2"></v-icon>
            <h4 class="text-h6 font-weight-bold text-grey-darken-3">
              {{ t('systemConfiguration.courseCategories') }}
            </h4>
          </div>
          <v-textarea
            v-model="courseCategories"
            :label="t('systemConfiguration.courseCategoriesJSON')"
            variant="outlined"
            rows="30"
            rounded="lg"
            bg-color="grey-lighten-5"
            :placeholder="t('systemConfiguration.courseCategoriesPlaceholder')"
            :hint="t('systemConfiguration.courseCategoriesHint')"
            persistent-hint
          ></v-textarea>
        </v-card>
      </v-col>

      <!-- 职业类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="pa-4" rounded="lg" outlined>
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-briefcase-outline" color="orange-darken-1" class="mr-2"></v-icon>
            <h4 class="text-h6 font-weight-bold text-grey-darken-3">
              {{ t('systemConfiguration.professionCategories') }}
            </h4>
          </div>
          <v-textarea
            v-model="professionCategories"
            :label="t('systemConfiguration.professionCategoriesJSON')"
            variant="outlined"
            rows="30"
            rounded="lg"
            bg-color="grey-lighten-5"
            :placeholder="t('systemConfiguration.professionCategoriesPlaceholder')"
            :hint="t('systemConfiguration.professionCategoriesHint')"
            persistent-hint
          ></v-textarea>
        </v-card>
      </v-col>
    </v-row>

    <!-- 操作按钮 -->
    <div class="d-flex align-center justify-space-between mt-6">
      <div>
        <v-btn
          variant="flat"
          color="teal"
          class="mr-3"
          rounded="lg"
          :loading="saving"
          :disabled="!isValidConfig"
          @click="saveConfiguration"
        >
          <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
          {{ t('systemConfiguration.saveConfig') }}
        </v-btn>
        <v-btn variant="flat" color="grey-lighten-3" rounded="lg" @click="formatConfig">
          <v-icon icon="mdi-code-json" class="mr-2"></v-icon>
          {{ t('systemConfiguration.format') }}
        </v-btn>
      </div>
      <div class="text-caption text-grey-darken-1">
        {{ t('systemConfiguration.lastUpdate') }}{{ lastUpdateTime }}
      </div>
    </div>

    <!-- 错误提示 -->
    <v-alert v-if="configError" type="error" variant="tonal" class="mt-4" rounded="lg">
      <div class="font-weight-bold">{{ t('systemConfiguration.configFormatError') }}</div>
      <div class="text-body-2 mt-1">{{ configError }}</div>
    </v-alert>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { adminSystemServiceV1 } from '@/services/api/v1/adminApiServiceV1'
import { useI18n } from 'vue-i18n'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 响应式数据
const courseCategories = ref<string>('')
const professionCategories = ref<string>('')
const configError = ref<string>('')
const lastUpdateTime = ref<string>('')

// 使用 useFetch 加载配置
const {
  data: configData,
  loading: loadingConfig,
  refresh: loadConfiguration
} = useFetch({
  fetchFn: adminSystemServiceV1.getSystemConfig,
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
      configError.value = ''
    }
  },
  onError: (error) => {
    console.error('加载配置时发生错误:', error)
    configError.value = `${t('systemConfiguration.loadFailed')}: ${error.message}`
  }
})

// 使用 useMutation 保存配置
const { execute: saveConfig, loading: saving } = useMutation(
  async () => {
    const promises: Promise<any>[] = []

    if (courseCategories.value.trim()) {
      promises.push(adminSystemServiceV1.updateConfigByKey('courseCategories', courseCategories.value))
    }

    if (professionCategories.value.trim()) {
      promises.push(
        adminSystemServiceV1.updateConfigByKey('professionCategories', professionCategories.value)
      )
    }

    const responses = await Promise.all(promises)
    const allSuccess = responses.every((response) => response.code === 200)

    if (!allSuccess) {
      const failedResponses = responses.filter((response) => response.code !== 200)
      throw new Error(failedResponses.map((r) => r.message).join(', '))
    }

    return { success: true }
  },
  {
    successMessage: t('systemConfiguration.saveSuccess'),
    onSuccess: () => {
      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
      configError.value = ''
    },
    onError: (error) => {
      configError.value = `${t('systemConfiguration.saveFailed')}: ${error.message}`
    }
  }
)

// 计算属性：配置是否有效
const isValidConfig = computed<boolean>(() => {
  try {
    if (courseCategories.value.trim()) {
      JSON.parse(courseCategories.value)
    }
    if (professionCategories.value.trim()) {
      JSON.parse(professionCategories.value)
    }
    return true
  } catch (error) {
    console.error('配置格式错误:', error)
    return false
  }
})

// 格式化配置
const formatConfig = (): void => {
  try {
    if (courseCategories.value.trim()) {
      const parsedCourse = JSON.parse(courseCategories.value)
      courseCategories.value = JSON.stringify(parsedCourse, null, 2)
    }

    if (professionCategories.value.trim()) {
      const parsedProfession = JSON.parse(professionCategories.value)
      professionCategories.value = JSON.stringify(parsedProfession, null, 2)
    }
  } catch (error: any) {
    configError.value = `${t('systemConfiguration.formatFailed')}: ${error.message}`
  }
}

// 保存配置
const saveConfiguration = async (): Promise<void> => {
  if (!isValidConfig.value) {
    return
  }
  await saveConfig()
}
</script>

<style scoped>
  .system-configuration {
    padding: 0;
  }

  /* 卡片边框样式 */
  .v-card[outlined] {
    border: 1px solid rgba(0, 0, 0, 0.08) !important;
  }

  /* 文本域样式优化 */
  :deep(.v-textarea .v-field__input) {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.4;
  }

  /* 配置预览区域样式 */
  :deep(.v-textarea[readonly] .v-field__input) {
    color: rgba(0, 0, 0, 0.6) !important;
  }
</style>