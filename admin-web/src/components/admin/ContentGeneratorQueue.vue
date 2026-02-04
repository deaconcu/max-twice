<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'
import { useMutation } from '@/composables'

// AI 用户 ID 配置
const aiUserId = ref<string>('')
const originalAiUserId = ref<string>('')

// 加载 AI 用户 ID
onMounted(async () => {
  try {
    const response = await adminApi.getConfigByKey('autoAuthor.aiUserId')
    if (response.code === 200 && response.data) {
      aiUserId.value = response.data
      originalAiUserId.value = response.data
    }
  } catch (error) {
    console.error('加载 AI 用户 ID 失败:', error)
  }
})

// 保存 AI 用户 ID
const { execute: saveAiUserId, loading: savingAiUserId } = useMutation(
  () => adminApi.updateConfigByKey('autoAuthor.aiUserId', aiUserId.value),
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

// 扫描节点
const { execute: scanNodes, loading: scanningNodes } = useMutation(adminApi.scanAutoAuthorNodes, {
  successMessage: '扫描已开始',
})

// 重置会话
const { execute: resetSession, loading: resettingSession } = useMutation(
  adminApi.resetAutoAuthorSession,
  {
    successMessage: 'OpenCode 会话已重置',
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
  }
)
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

    <!-- 队列操作 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-cog-outline" class="mr-2"></v-icon>
        队列操作
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">批量管理生成队列</p>
        <div class="d-flex ga-2 flex-wrap">
          <v-btn variant="tonal" :loading="scanningNodes" @click="scanNodes">
            <v-icon icon="mdi-radar" class="mr-2"></v-icon>
            扫描空节点
          </v-btn>
          <v-btn variant="tonal" :loading="resettingSession" @click="resetSession">
            <v-icon icon="mdi-refresh-auto" class="mr-2"></v-icon>
            重置会话
          </v-btn>
          <v-btn variant="tonal" color="error" :loading="clearingQueue" @click="confirmClearQueue">
            <v-icon icon="mdi-delete-sweep" class="mr-2"></v-icon>
            清空队列
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 队列状态 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-format-list-checks" class="mr-2"></v-icon>
        队列状态
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">查看当前队列中的任务</p>
        <div class="pa-4 bg-grey-lighten-4 rounded">
          <div class="text-body-2 text-grey text-center">
            队列查看功能开发中，需要后端提供队列查询接口
          </div>
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

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>

