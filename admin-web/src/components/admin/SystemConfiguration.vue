<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import { useFetch, useMutation } from '@/composables'
import { useSystemConfigStore } from '@/stores'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
const systemConfigStore = useSystemConfigStore()

// 配置项类型
type ConfigKey =
  | 'courseCategories'
  | 'roleCategories'
  | 'rejectReasons'
  | 'banReasons'
  | 'homepageRecommendations'
  | 'postSystemPrompt'
  | 'postAutoPrompt'
  | 'postArticlePrompt'
  | 'postIndexPrompt'
  | 'roadmapSystemPrompt'
  | 'roadmapPrompt'
  | 'memoryCardSystemPrompt'
  | 'memoryCardPrompt'

// 纯文本配置项（不需要 JSON 校验）
const plainTextKeys = new Set<ConfigKey>([
  'postSystemPrompt',
  'postAutoPrompt',
  'postArticlePrompt',
  'postIndexPrompt',
  'roadmapSystemPrompt',
  'roadmapPrompt',
  'memoryCardSystemPrompt',
  'memoryCardPrompt',
])

// 响应式数据
const courseCategories = ref<string>('')
const roleCategories = ref<string>('')
const rejectReasons = ref<string>('')
const banReasons = ref<string>('')
const homepageRecommendations = ref<string>('')
const frontendUrl = ref<string>('')
const frontendUrlOriginal = ref<string>('')  // 原始值，用于对比是否修改
const courseUpdatedAt = ref<string>('')
const roleUpdatedAt = ref<string>('')
const rejectReasonsUpdatedAt = ref<string>('')
const banReasonsUpdatedAt = ref<string>('')
const homepageRecommendationsUpdatedAt = ref<string>('')
const frontendUrlUpdatedAt = ref<string>('')

// AI Prompt 响应式数据
const postSystemPrompt = ref<string>('')
const postAutoPrompt = ref<string>('')
const postArticlePrompt = ref<string>('')
const postIndexPrompt = ref<string>('')
const roadmapSystemPrompt = ref<string>('')
const roadmapPrompt = ref<string>('')
const memoryCardSystemPrompt = ref<string>('')
const memoryCardPrompt = ref<string>('')
const postSystemPromptUpdatedAt = ref<string>('')
const postAutoPromptUpdatedAt = ref<string>('')
const postArticlePromptUpdatedAt = ref<string>('')
const postIndexPromptUpdatedAt = ref<string>('')
const roadmapSystemPromptUpdatedAt = ref<string>('')
const roadmapPromptUpdatedAt = ref<string>('')
const memoryCardSystemPromptUpdatedAt = ref<string>('')
const memoryCardPromptUpdatedAt = ref<string>('')

// 对话框状态
const dialog = ref(false)
const dialogKey = ref<ConfigKey>('courseCategories')
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
        } else if (item.key === 'roleCategories') {
          roleCategories.value = formatted
          roleUpdatedAt.value = item.updatedAt
        } else if (item.key === 'rejectReasons') {
          rejectReasons.value = formatted
          rejectReasonsUpdatedAt.value = item.updatedAt
        } else if (item.key === 'banReasons') {
          banReasons.value = formatted
          banReasonsUpdatedAt.value = item.updatedAt
        } else if (item.key === 'homepage_recommendations') {
          homepageRecommendations.value = formatted
          homepageRecommendationsUpdatedAt.value = item.updatedAt
        } else if (item.key === 'frontendUrl') {
          frontendUrl.value = item.value
          frontendUrlOriginal.value = item.value
          frontendUrlUpdatedAt.value = item.updatedAt
        } else if (item.key === 'postSystemPrompt') {
          postSystemPrompt.value = item.value
          postSystemPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'postAutoPrompt') {
          postAutoPrompt.value = item.value
          postAutoPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'postArticlePrompt') {
          postArticlePrompt.value = item.value
          postArticlePromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'postIndexPrompt') {
          postIndexPrompt.value = item.value
          postIndexPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'roadmapSystemPrompt') {
          roadmapSystemPrompt.value = item.value
          roadmapSystemPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'roadmapPrompt') {
          roadmapPrompt.value = item.value
          roadmapPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'memoryCardSystemPrompt') {
          memoryCardSystemPrompt.value = item.value
          memoryCardSystemPromptUpdatedAt.value = item.updatedAt
        } else if (item.key === 'memoryCardPrompt') {
          memoryCardPrompt.value = item.value
          memoryCardPromptUpdatedAt.value = item.updatedAt
        }
      }
    }
  },
  onError: (error: any) => {
    console.error('加载配置时发生错误:', error)
  },
})

// 打开对话框
const openDialog = (key: ConfigKey): void => {
  dialogKey.value = key
  const titleMap: Record<ConfigKey, string> = {
    courseCategories: '课程分类',
    roleCategories: '职业分类',
    rejectReasons: '拒绝理由',
    banReasons: '屏蔽理由',
    homepageRecommendations: '首页新手推荐',
    postSystemPrompt: '文章/目录 System Prompt',
    postAutoPrompt: '自动判断 User Prompt',
    postArticlePrompt: '生成文章 User Prompt',
    postIndexPrompt: '生成目录 User Prompt',
    roadmapSystemPrompt: '路线图 System Prompt',
    roadmapPrompt: '路线图 User Prompt',
    memoryCardSystemPrompt: '记忆卡片 System Prompt',
    memoryCardPrompt: '记忆卡片 User Prompt',
  }
  const valueMap: Record<ConfigKey, string> = {
    courseCategories: courseCategories.value,
    roleCategories: roleCategories.value,
    rejectReasons: rejectReasons.value,
    banReasons: banReasons.value,
    homepageRecommendations: homepageRecommendations.value,
    postSystemPrompt: postSystemPrompt.value,
    postAutoPrompt: postAutoPrompt.value,
    postArticlePrompt: postArticlePrompt.value,
    postIndexPrompt: postIndexPrompt.value,
    roadmapSystemPrompt: roadmapSystemPrompt.value,
    roadmapPrompt: roadmapPrompt.value,
    memoryCardSystemPrompt: memoryCardSystemPrompt.value,
    memoryCardPrompt: memoryCardPrompt.value,
  }
  dialogTitle.value = titleMap[key]
  dialogValue.value = valueMap[key]
  dialog.value = true
}

// 格式化 JSON（纯文本配置项不可格式化）
const formatDialogValue = (): void => {
  if (plainTextKeys.has(dialogKey.value)) {
    showSnackbar?.('该配置项为纯文本，无需格式化', 'info')
    return
  }
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
  // 纯文本配置项不需要校验 JSON
  if (plainTextKeys.has(dialogKey.value)) return true
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
      const now = new Date().toISOString()
      if (data.key === 'courseCategories') {
        courseCategories.value = dialogValue.value
        courseUpdatedAt.value = now
      } else if (data.key === 'roleCategories') {
        roleCategories.value = dialogValue.value
        roleUpdatedAt.value = now
      } else if (data.key === 'rejectReasons') {
        rejectReasons.value = dialogValue.value
        rejectReasonsUpdatedAt.value = now
        // 同步更新 store
        try {
          const parsed = JSON.parse(dialogValue.value)
          if (Array.isArray(parsed)) {
            systemConfigStore.setRejectReasons(parsed)
          }
        } catch { /* ignore */ }
      } else if (data.key === 'banReasons') {
        banReasons.value = dialogValue.value
        banReasonsUpdatedAt.value = now
        // 同步更新 store
        try {
          const parsed = JSON.parse(dialogValue.value)
          if (Array.isArray(parsed)) {
            systemConfigStore.setBanReasons(parsed)
          }
        } catch { /* ignore */ }
      } else if (data.key === 'homepage_recommendations') {
        homepageRecommendations.value = dialogValue.value
        homepageRecommendationsUpdatedAt.value = now
      } else if (data.key === 'postSystemPrompt') {
        postSystemPrompt.value = dialogValue.value
        postSystemPromptUpdatedAt.value = now
      } else if (data.key === 'postAutoPrompt') {
        postAutoPrompt.value = dialogValue.value
        postAutoPromptUpdatedAt.value = now
      } else if (data.key === 'postArticlePrompt') {
        postArticlePrompt.value = dialogValue.value
        postArticlePromptUpdatedAt.value = now
      } else if (data.key === 'postIndexPrompt') {
        postIndexPrompt.value = dialogValue.value
        postIndexPromptUpdatedAt.value = now
      } else if (data.key === 'roadmapSystemPrompt') {
        roadmapSystemPrompt.value = dialogValue.value
        roadmapSystemPromptUpdatedAt.value = now
      } else if (data.key === 'roadmapPrompt') {
        roadmapPrompt.value = dialogValue.value
        roadmapPromptUpdatedAt.value = now
      } else if (data.key === 'memoryCardSystemPrompt') {
        memoryCardSystemPrompt.value = dialogValue.value
        memoryCardSystemPromptUpdatedAt.value = now
      } else if (data.key === 'memoryCardPrompt') {
        memoryCardPrompt.value = dialogValue.value
        memoryCardPromptUpdatedAt.value = now
      }
      dialog.value = false
    },
  }
)

const saveDialogConfig = async (): Promise<void> => {
  if (!isDialogValueValid()) {
    showSnackbar?.('内容不能为空', 'error')
    return
  }
  // 映射前端 key 到后端 key
  const keyMap: Record<ConfigKey, string> = {
    courseCategories: 'courseCategories',
    roleCategories: 'roleCategories',
    rejectReasons: 'rejectReasons',
    banReasons: 'banReasons',
    homepageRecommendations: 'homepage_recommendations',
    postSystemPrompt: 'postSystemPrompt',
    postAutoPrompt: 'postAutoPrompt',
    postArticlePrompt: 'postArticlePrompt',
    postIndexPrompt: 'postIndexPrompt',
    roadmapSystemPrompt: 'roadmapSystemPrompt',
    roadmapPrompt: 'roadmapPrompt',
    memoryCardSystemPrompt: 'memoryCardSystemPrompt',
    memoryCardPrompt: 'memoryCardPrompt',
  }
  await saveConfig({ key: keyMap[dialogKey.value], value: dialogValue.value })
}

// 保存前端 URL
const savingFrontendUrl = ref(false)
const frontendUrlChanged = (): boolean => {
  return frontendUrl.value.trim() !== frontendUrlOriginal.value
}
const saveFrontendUrl = async (): Promise<void> => {
  if (!frontendUrl.value.trim()) {
    showSnackbar?.('请输入前端 URL', 'warning')
    return
  }
  savingFrontendUrl.value = true
  try {
    await adminApi.updateConfigByKey('frontendUrl', frontendUrl.value.trim())
    frontendUrlOriginal.value = frontendUrl.value.trim()
    frontendUrlUpdatedAt.value = new Date().toISOString()
    // 同步更新 store
    systemConfigStore.setFrontendUrl(frontendUrl.value.trim())
    showSnackbar?.('配置已保存', 'success')
  } catch {
    showSnackbar?.('保存失败', 'error')
  } finally {
    savingFrontendUrl.value = false
  }
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

        <v-list v-else class="pa-2">
          <!-- 课程分类 -->
          <v-list-item
            prepend-icon="mdi-book-outline"
            title="课程分类"
            :subtitle="courseUpdatedAt ? `上次更新: ${new Date(courseUpdatedAt).toLocaleString('zh-CN')}` : '课程主分类与子分类 JSON 配置'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('courseCategories')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 职业分类 -->
          <v-list-item
            prepend-icon="mdi-briefcase-outline"
            title="职业分类"
            :subtitle="roleUpdatedAt ? `上次更新: ${new Date(roleUpdatedAt).toLocaleString('zh-CN')}` : '职业主分类与子分类 JSON 配置'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('roleCategories')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 拒绝理由 -->
          <v-list-item
            prepend-icon="mdi-close-circle-outline"
            title="拒绝理由"
            :subtitle="rejectReasonsUpdatedAt ? `上次更新: ${new Date(rejectReasonsUpdatedAt).toLocaleString('zh-CN')}` : '审核拒绝时可选的预设理由'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('rejectReasons')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 屏蔽理由 -->
          <v-list-item
            prepend-icon="mdi-cancel"
            title="屏蔽理由"
            :subtitle="banReasonsUpdatedAt ? `上次更新: ${new Date(banReasonsUpdatedAt).toLocaleString('zh-CN')}` : '内容屏蔽时可选的预设理由'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('banReasons')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 首页新手推荐 -->
          <v-list-item
            prepend-icon="mdi-star-outline"
            title="首页新手推荐"
            :subtitle="homepageRecommendationsUpdatedAt ? `上次更新: ${new Date(homepageRecommendationsUpdatedAt).toLocaleString('zh-CN')}` : '首页空状态时显示的推荐职业、路线图、课程'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('homepageRecommendations')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 前端 URL -->
          <v-list-item
            prepend-icon="mdi-web"
            title="前端 URL"
            :subtitle="frontendUrlUpdatedAt ? `上次更新: ${new Date(frontendUrlUpdatedAt).toLocaleString('zh-CN')}` : '前端网站的 URL 地址'"
            rounded="lg"
            class="config-item px-4"
          >
            <template #append>
              <div class="d-flex align-center">
                <v-text-field
                  v-model="frontendUrl"
                  variant="outlined"
                  density="compact"
                  hide-details
                  placeholder="https://example.com"
                  style="width: 300px"
                  @keyup.enter="saveFrontendUrl"
                ></v-text-field>
                <v-btn
                  v-if="frontendUrlChanged()"
                  variant="tonal"
                  color="primary"
                  size="small"
                  class="ml-2"
                  :loading="savingFrontendUrl"
                  @click="saveFrontendUrl"
                >
                  保存
                </v-btn>
              </div>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>

    <!-- AI Prompt 配置 -->
    <v-card flat class="border mt-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-robot-outline" size="18" class="mr-2"></v-icon>
        AI Prompt 配置
      </v-card-title>
      <v-card-text class="pa-0">
        <v-list v-if="!loadingConfig" class="pa-2">
          <!-- 文章/目录 System Prompt -->
          <v-list-item
            prepend-icon="mdi-script-text-outline"
            title="文章/目录 System Prompt"
            :subtitle="postSystemPromptUpdatedAt ? `上次更新: ${new Date(postSystemPromptUpdatedAt).toLocaleString('zh-CN')}` : '生成文章和目录时的系统提示词'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('postSystemPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 自动判断 User Prompt -->
          <v-list-item
            prepend-icon="mdi-auto-fix"
            title="自动判断 User Prompt"
            :subtitle="postAutoPromptUpdatedAt ? `上次更新: ${new Date(postAutoPromptUpdatedAt).toLocaleString('zh-CN')}` : '自动判断生成文章或目录的提示词，变量: {courseName} {nodeName} {nodeDescription}'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('postAutoPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 生成文章 User Prompt -->
          <v-list-item
            prepend-icon="mdi-file-document-outline"
            title="生成文章 User Prompt"
            :subtitle="postArticlePromptUpdatedAt ? `上次更新: ${new Date(postArticlePromptUpdatedAt).toLocaleString('zh-CN')}` : '强制生成文章的提示词，变量: {courseName} {nodeName} {nodeDescription}'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('postArticlePrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 生成目录 User Prompt -->
          <v-list-item
            prepend-icon="mdi-format-list-bulleted"
            title="生成目录 User Prompt"
            :subtitle="postIndexPromptUpdatedAt ? `上次更新: ${new Date(postIndexPromptUpdatedAt).toLocaleString('zh-CN')}` : '强制生成目录的提示词，变量: {courseName} {nodeName} {nodeDescription}'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('postIndexPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 路线图 System Prompt -->
          <v-list-item
            prepend-icon="mdi-map-outline"
            title="路线图 System Prompt"
            :subtitle="roadmapSystemPromptUpdatedAt ? `上次更新: ${new Date(roadmapSystemPromptUpdatedAt).toLocaleString('zh-CN')}` : '生成学习路线图时的系统提示词'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('roadmapSystemPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 路线图 User Prompt -->
          <v-list-item
            prepend-icon="mdi-map-marker-path"
            title="路线图 User Prompt"
            :subtitle="roadmapPromptUpdatedAt ? `上次更新: ${new Date(roadmapPromptUpdatedAt).toLocaleString('zh-CN')}` : '生成学习路线图的提示词，变量: {roleName} {roleDescription}'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('roadmapPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 记忆卡片 System Prompt -->
          <v-list-item
            prepend-icon="mdi-card-multiple-outline"
            title="记忆卡片 System Prompt"
            :subtitle="memoryCardSystemPromptUpdatedAt ? `上次更新: ${new Date(memoryCardSystemPromptUpdatedAt).toLocaleString('zh-CN')}` : '生成记忆卡片时的系统提示词'"
            rounded="lg"
            class="config-item mb-2 px-4"
            @click="openDialog('memoryCardSystemPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
            </template>
          </v-list-item>

          <!-- 记忆卡片 User Prompt -->
          <v-list-item
            prepend-icon="mdi-cards-outline"
            title="记忆卡片 User Prompt"
            :subtitle="memoryCardPromptUpdatedAt ? `上次更新: ${new Date(memoryCardPromptUpdatedAt).toLocaleString('zh-CN')}` : '生成记忆卡片的提示词，变量: {articleContent}'"
            rounded="lg"
            class="config-item px-4"
            @click="openDialog('memoryCardPrompt')"
          >
            <template #append>
              <v-icon icon="mdi-chevron-right" size="18" color="grey"></v-icon>
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
              <v-icon
                :icon="plainTextKeys.has(dialogKey) ? 'mdi-text-box-outline' : 'mdi-code-json'"
                color="blue-darken-1"
                class="mr-3"
              ></v-icon>
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
            :label="plainTextKeys.has(dialogKey) ? `${dialogTitle}` : `${dialogTitle} JSON 配置`"
            variant="outlined"
            rounded="lg"
            bg-color="grey-lighten-5"
            :error="!plainTextKeys.has(dialogKey) && dialogValue.trim().length > 0 && !isDialogValueValid()"
            :error-messages="!plainTextKeys.has(dialogKey) && dialogValue.trim().length > 0 && !isDialogValueValid() ? 'JSON 格式无效' : ''"
            class="config-textarea"
          ></v-textarea>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-btn variant="tonal" color="grey" :disabled="plainTextKeys.has(dialogKey)" @click="formatDialogValue">
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

.config-item {
  min-height: 64px;
  cursor: pointer;
  background-color: #fafafa;
}

.config-item:hover {
  background-color: #f0f0f0;
}

.config-textarea :deep(.v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
  line-height: 1.4 !important;
  min-height: calc(100vh - 300px) !important;
}
</style>
