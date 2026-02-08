<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { adminApi } from '@/api'
import { useMutation, useFetch } from '@/composables'

// AI 用户 ID 配置
const aiUserId = ref<string>('')
const originalAiUserId = ref<string>('')

// AI 服务配置
const aiService = ref<string>('openrouter')
const model = ref<string>('deepseek/deepseek-chat')
const originalAiService = ref<string>('')
const originalModel = ref<string>('')

// AI 服务选项
const aiServiceOptions = [
  { label: 'OpenRouter', value: 'openrouter' },
  { label: 'Gemini', value: 'gemini' },
  { label: 'OpenCode (本地)', value: 'opencode' },
]

// 模型选项（根据不同的 AI 服务）
const modelOptions = computed(() => {
  switch (aiService.value) {
    case 'openrouter':
      return [
        { label: 'Google Gemini 3.0 Flash', value: 'google/gemini-3-flash-preview' },
        { label: 'Google Gemini 3.0 Pro', value: 'google/gemini-3-pro-preview' },
        { label: 'GPT 5.2', value: 'openai/gpt-5.2' },
        { label: 'GPT5 mini', value: 'openai/gpt-5-mini' },
        { label: 'Claude Sonnet 4.5', value: 'anthropic/claude-sonnet-4.5' },
        { label: 'Claude Opus 4.6', value: 'anthropic/claude-opus-4.6' },
        { label: 'DeepSeek V3.2', value: 'deepseek/deepseek-v3.2' },
      ]
    case 'gemini':
      return [
        { label: 'Gemini 3.0 Flash Preview', value: 'gemini-3-flash-preview' },
        { label: 'Gemini 3.0 Pro Preview', value: 'gemini-3-pro-preview' },
        { label: 'Gemini 2.5 Pro ', value: 'gemini-2.5-pro' },
      ]
    case 'opencode':
      return [
        { label: 'Gemini 3.0 Pro Preview', value: 'gemini-3-pro-preview' },
        { label: 'Gemini 3.0 Flash Preview', value: 'gemini-3-flash-preview' },
        { label: 'Gemini 2.5 Pro', value: 'gemini-2.5-pro' },
        { label: 'GPT 5.2', value: 'gpt-5.2' },
        { label: 'GPT 5 mini', value: 'gpt-5-mini' },
        { label: 'Claude Sonnet 4.5', value: 'claude-sonnet-4.5' },
        { label: 'Claude Sonnet 4.5', value: 'claude-sonnet-4.5' },
      ]
    default:
      return []
  }
})

// 保存 AI 用户 ID
const { execute: saveAiUserId, loading: savingAiUserId } = useMutation(
  () => adminApi.updateConfigByKey('robot.aiUserId', aiUserId.value),
  {
    successMessage: 'AI 用户 ID 已保存',
    onSuccess: () => {
      originalAiUserId.value = aiUserId.value
    },
  }
)

const hasAiUserIdChanged = () => {
  return aiUserId.value !== originalAiUserId.value
}

// 保存 AI 服务配置
const { execute: saveAiConfig, loading: savingAiConfig } = useMutation(
  () => adminApi.updateRobotConfig({ aiService: aiService.value, model: model.value }),
  {
    successMessage: 'AI 配置已保存',
    onSuccess: () => {
      originalAiService.value = aiService.value
      originalModel.value = model.value
    },
  }
)

const hasAiConfigChanged = () => {
  return aiService.value !== originalAiService.value || model.value !== originalModel.value
}

// 监听 AI 服务切换，自动更新模型选项
watch(aiService, (newService) => {
  const options = modelOptions.value
  if (options.length > 0) {
    // 检查当前model是否在新的选项列表中
    const modelExists = options.some((opt) => opt.value === model.value)
    if (!modelExists) {
      // 如果当前model不在新列表中，选择第一个选项
      model.value = options[0].value
    }
  }
})

// 加载 AI 服务配置
const { data: robotConfig, loading: loadingRobotConfig } = useFetch({
  fetchFn: adminApi.getRobotConfig,
  immediate: true,
  onSuccess: (data) => {
    if (data) {
      aiService.value = data.aiService || 'openrouter'
      model.value = data.model || 'deepseek/deepseek-chat'
      originalAiService.value = data.aiService || 'openrouter'
      originalModel.value = data.model || 'deepseek/deepseek-chat'
    }
  },
})

// 队列状态
const { data: queueStats, loading: loadingStats, execute: fetchQueueStats } = useFetch({
  fetchFn: adminApi.getRobotQueueStats,
  immediate: false,
  defaultValue: {
    pendingCount: 0,
    todayCompletedCount: 0,
    lastExecuteTime: null,
    status: 'IDLE',
  },
})

// 暂停/恢复队列
const { execute: pauseQueue, loading: pausing } = useMutation(adminApi.pauseRobotQueue, {
  successMessage: '队列已暂停',
  onSuccess: () => {
    fetchQueueStats()
  },
})

const { execute: resumeQueue, loading: resuming } = useMutation(adminApi.resumeRobotQueue, {
  successMessage: '队列已恢复',
  onSuccess: () => {
    fetchQueueStats()
  },
})

const togglingPause = ref(false)
const toggleQueuePause = async () => {
  togglingPause.value = true
  try {
    if (queueStats.value.status === 'PAUSED') {
      await resumeQueue()
    } else {
      await pauseQueue()
    }
  } finally {
    togglingPause.value = false
  }
}

// 重置会话
const { execute: resetSession, loading: resettingSession } = useMutation(
  adminApi.resetAutoAuthorSession,
  {
    successMessage: 'OpenCode 会话已重置',
  }
)

// 压缩会话
const { execute: summarizeSession, loading: summarizingSession } = useMutation(
  adminApi.summarizeAutoAuthorSession,
  {
    successMessage: 'OpenCode 会话已压缩',
  }
)

// 清空队列
const confirmClearQueue = (): void => {
  if (confirm('确定要清空所有队列吗？此操作不可恢复。')) {
    clearQueueExecute()
  }
}

const { execute: clearQueueExecute, loading: clearingQueue } = useMutation(
  adminApi.clearAutoAuthorQueue,
  {
    successMessage: '队列已清空',
    onSuccess: () => {
      fetchQueueStats()
    },
  }
)

// 组件加载时
onMounted(async () => {
  // 加载 AI 用户 ID
  try {
    const response = await adminApi.getConfigByKey('robot.aiUserId')
    if (response.code === 200 && response.data) {
      aiUserId.value = response.data
      originalAiUserId.value = response.data
    }
  } catch (error) {
    console.error('加载 AI 用户 ID 失败:', error)
  }

  // 获取队列状态
  fetchQueueStats()
})
</script>

<template>
  <div class="content-generator-queue">
    <h2 class="text-h5 font-weight-bold mb-6">队列与配置管理</h2>

    <!-- AI 用户配置 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-account-cog" class="mr-2"></v-icon>
        AI 用户配置
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">设置发布 AI 生成内容的用户 ID</p>
        <v-row>
          <v-col cols="12" md="6">
            <v-text-field
              v-model="aiUserId"
              label="AI 用户 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              placeholder="输入用户 ID"
            ></v-text-field>
          </v-col>
        </v-row>
        <div class="mt-4">
          <v-btn
            variant="tonal"
            :loading="savingAiUserId"
            :disabled="!hasAiUserIdChanged()"
            @click="saveAiUserId"
          >
            <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
            保存配置
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- AI 服务配置 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-robot" class="mr-2"></v-icon>
        AI 服务配置
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">选择 AI 服务提供商和模型</p>
        <v-row>
          <v-col cols="12" md="2">
            <v-select
              v-model="aiService"
              :items="aiServiceOptions"
              item-title="label"
              item-value="value"
              label="AI 服务"
              variant="outlined"
              density="compact"
              hide-details
            ></v-select>
          </v-col>
          <v-col cols="12" md="4">
            <v-select
              v-model="model"
              :items="modelOptions"
              item-title="label"
              item-value="value"
              label="模型名称"
              variant="outlined"
              density="compact"
              hide-details
            ></v-select>
          </v-col>
        </v-row>
        <div class="mt-4">
          <v-btn
            variant="tonal"
            :loading="savingAiConfig"
            :disabled="!hasAiConfigChanged()"
            @click="saveAiConfig"
          >
            <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
            保存配置
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 队列操作 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-cog-outline" class="mr-2"></v-icon>
          队列操作
        </div>
        <v-btn
          variant="text"
          size="small"
          :loading="loadingStats"
          @click="fetchQueueStats"
          icon="mdi-refresh"
        ></v-btn>
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">批量管理生成队列</p>

        <!-- 队列统计 -->
        <div class="d-flex ga-4 mb-4">
          <div class="stat-item-compact">
            <div class="text-caption text-grey mb-1">队列状态</div>
            <div class="d-flex align-center">
              <v-chip
                :color="
                  queueStats.status === 'RUNNING'
                    ? 'success'
                    : queueStats.status === 'PAUSED'
                      ? 'warning'
                      : 'grey'
                "
                size="small"
                variant="flat"
              >
                {{
                  queueStats.status === 'RUNNING'
                    ? '执行中'
                    : queueStats.status === 'PAUSED'
                      ? '已暂停'
                      : '空闲'
                }}
              </v-chip>
            </div>
          </div>
          <div class="stat-item-compact">
            <div class="text-caption text-grey mb-1">当前任务数</div>
            <div class="text-h6 font-weight-bold">{{ queueStats.pendingCount }}</div>
          </div>
          <div class="stat-item-compact">
            <div class="text-caption text-grey mb-1">今天完成数</div>
            <div class="text-h6 font-weight-bold">{{ queueStats.todayCompletedCount }}</div>
          </div>
          <div class="stat-item-compact">
            <div class="text-caption text-grey mb-1">最后执行时间</div>
            <div class="text-body-2 font-weight-medium">
              {{ queueStats.lastExecuteTime || '暂无' }}
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="d-flex ga-2 flex-wrap">
          <v-btn
            variant="tonal"
            :loading="togglingPause"
            :disabled="queueStats.status === 'IDLE'"
            @click="toggleQueuePause"
          >
            <v-icon
              :icon="queueStats.status === 'PAUSED' ? 'mdi-play' : 'mdi-pause'"
              class="mr-2"
            ></v-icon>
            {{ queueStats.status === 'PAUSED' ? '恢复执行' : '暂停执行' }}
          </v-btn>
          <v-btn variant="tonal" color="error" :loading="clearingQueue" @click="confirmClearQueue">
            <v-icon icon="mdi-delete-sweep" class="mr-2"></v-icon>
            清空队列
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 会话管理 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-chat-processing" class="mr-2"></v-icon>
        会话管理
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">管理 OpenCode AI 会话状态</p>
        <div class="d-flex ga-2">
          <v-btn variant="tonal" :loading="summarizingSession" @click="summarizeSession">
            <v-icon icon="mdi-arrow-collapse-all" class="mr-2"></v-icon>
            压缩上下文
          </v-btn>
          <v-btn variant="tonal" :loading="resettingSession" @click="resetSession">
            <v-icon icon="mdi-refresh-auto" class="mr-2"></v-icon>
            重置会话
          </v-btn>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.content-generator-queue {
  max-width: 100%;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.stat-item-compact {
  padding: 8px 12px;
  background-color: #fafafa;
  border-radius: 6px;
  min-width: 120px;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>

