<script setup lang="ts">
import { inject, ref, computed, onUnmounted } from 'vue'
import { adminApi } from '@/api'
import { useMutation } from '@/composables'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 任务状态
const taskId = ref<string | null>(null)
const taskResult = ref<{ checked: number; updated: number; timeout: number } | null>(null)
const taskProgress = ref<{ checked: number; updated: number } | null>(null)
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

// 是否正在执行（启动中或轮询中）
const running = computed(() => starting.value || polling.value)

// 启动任务
const { execute: startTask, loading: starting } = useMutation(
  adminApi.startRecalculateSubCourseCounts,
  {
    showToast: false,
    onSuccess: (result) => {
      taskId.value = result.taskId
      taskResult.value = null
      taskProgress.value = null
      showSnackbar?.('任务已启动，请等待完成', 'info')
      // 立即查询一次状态
      pollTaskStatus()
      startPolling()
    },
    onError: (error) => {
      showSnackbar?.(`启动失败: ${error.message}`, 'error')
    },
  }
)

// 单次查询任务状态
const pollTaskStatus = async () => {
  if (!taskId.value) return

  try {
    const response = await adminApi.getTaskResult(taskId.value)
    if (response.code === 200 && response.data) {
      const task = response.data

      if (task.status === 'RUNNING') {
        // 更新进度
        if (task.result) {
          taskProgress.value = task.result
        }
      } else if (task.status === 'COMPLETED') {
        taskResult.value = task.result ?? null
        taskProgress.value = null
        const timeoutText = task.result?.timeout ? ' (超时中断)' : ''
        showSnackbar?.(
          `重算完成${timeoutText}：检查 ${task.result?.checked} 个，更新 ${task.result?.updated} 个`,
          'success'
        )
        stopPolling()
      } else if (task.status === 'FAILED') {
        taskProgress.value = null
        showSnackbar?.(`任务失败: ${task.error}`, 'error')
        stopPolling()
      }
    }
  } catch (error) {
    console.error('查询任务状态失败', error)
  }
}

// 轮询任务状态
const startPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
  polling.value = true

  pollTimer = setInterval(pollTaskStatus, 1000)
}

const stopPolling = () => {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <v-card flat class="border">
    <v-card-title class="d-flex align-center">
      <v-icon icon="mdi-book-multiple" class="mr-2"></v-icon>
      子课程数量重算
    </v-card-title>
    <v-card-text>
      <p class="text-body-2 text-grey mb-4">
        重新计算所有课程的子课程数量，修正数据不一致问题
      </p>

      <div class="d-flex align-center ga-3">
        <v-btn
          variant="tonal"
          :loading="running"
          :disabled="running"
          @click="startTask"
        >
          <v-icon icon="mdi-refresh" class="mr-2"></v-icon>
          {{ running ? '执行中...' : '开始重算' }}
        </v-btn>

        <span v-if="taskProgress && running" class="text-body-2 d-flex align-center">
          <v-progress-circular
            indeterminate
            size="16"
            width="2"
            color="primary"
            class="mr-1"
          ></v-progress-circular>
          已检查 {{ taskProgress.checked }} 个，更新 {{ taskProgress.updated }} 个
        </span>

        <span v-else-if="taskResult" class="text-body-2 d-flex align-center">
          <v-icon
            :icon="taskResult.timeout ? 'mdi-alert' : 'mdi-check-circle'"
            :color="taskResult.timeout ? 'warning' : 'success'"
            size="18"
            class="mr-1"
          ></v-icon>
          {{ taskResult.timeout ? '超时中断：' : '' }}检查 {{ taskResult.checked }} 个，更新 {{ taskResult.updated }} 个
        </span>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
