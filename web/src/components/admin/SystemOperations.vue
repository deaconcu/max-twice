<script setup lang="ts">
import { inject, ref } from 'vue'
import { statsApi, systemApi, adminApi } from '@/api'
import { useFetch, useMutation } from '@/composables'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 操作历史
interface OperationRecord {
  title: string
  description: string
  success: boolean
  time: string
}

const operationHistory = ref<OperationRecord[]>([])

const addOperationToHistory = (title: string, description: string, success = true): void => {
  operationHistory.value.unshift({
    title,
    description,
    success,
    time: new Date().toLocaleString(),
  })
  if (operationHistory.value.length > 20) {
    operationHistory.value = operationHistory.value.slice(0, 20)
  }
}

// 只读模式
const { data: readonlyModeData } = useFetch({
  fetchFn: async () => {
    const response = await systemApi.getReadonlyMode()
    return response.code === 200 && response.data ? response.data : null
  },
  immediate: true,
})

const readonlyModeEnabled = ref<boolean>(false)

const updateReadonlyMode = () => {
  if (readonlyModeData.value) {
    readonlyModeEnabled.value = readonlyModeData.value.enabled
  }
}

const { execute: toggleReadonlyModeExecute, loading: togglingReadonlyMode } = useMutation(
  (enabled: boolean) => systemApi.setReadonlyMode(enabled),
  {
    successMessage: '',
    showToast: false,
    onSuccess: (result, enabled) => {
      showSnackbar?.(enabled ? '只读模式已开启' : '只读模式已关闭', 'success')
      addOperationToHistory('只读模式', enabled ? '开启' : '关闭', true)
      readonlyModeEnabled.value = enabled
    },
    onError: (error) => {
      readonlyModeEnabled.value = !readonlyModeEnabled.value
      addOperationToHistory('只读模式', `切换失败: ${error.message}`, false)
    },
  }
)

const toggleReadonlyMode = (enabled: boolean) => {
  toggleReadonlyModeExecute(enabled)
}

// Redis 同步
const syncDate = ref<string>('')

const { execute: syncStatsManual, loading: syncingManual } = useMutation(statsApi.syncManual, {
  successMessage: '统计数据同步成功',
  onSuccess: () => {
    addOperationToHistory('Redis同步', '手动同步成功', true)
  },
  onError: (error) => {
    addOperationToHistory('Redis同步', `失败: ${error.message}`, false)
  },
})

const { execute: syncStatsSpecificDateExecute, loading: syncingSpecific } = useMutation(
  (targetDate: string | null) => statsApi.syncDate(targetDate),
  {
    successMessage: '统计数据同步成功',
    onSuccess: (result, targetDate) => {
      addOperationToHistory('Redis同步', `同步${targetDate || '昨日'}数据`, true)
    },
    onError: (error) => {
      addOperationToHistory('Redis同步', `失败: ${error.message}`, false)
    },
  }
)

const syncStatsSpecificDate = () => {
  syncStatsSpecificDateExecute(syncDate.value || null)
}

// 健康检查
const lastHealthCheck = ref<string>('')

const { execute: checkSystemHealth, loading: checkingHealth } = useMutation(statsApi.getHealth, {
  successMessage: '系统健康',
  onSuccess: () => {
    lastHealthCheck.value = new Date().toLocaleString()
    addOperationToHistory('健康检查', '系统正常', true)
  },
  onError: (error) => {
    lastHealthCheck.value = new Date().toLocaleString()
    addOperationToHistory('健康检查', `异常: ${error.message}`, false)
  },
})

// AutoAuthor
const nodeId = ref<string>('')

const { execute: enqueueNodeExecute, loading: enqueuingNode } = useMutation(
  (nodeIdNumber: number) => adminApi.enqueueAutoAuthorNode(nodeIdNumber),
  {
    successMessage: '节点已加入队列',
    onSuccess: () => {
      addOperationToHistory('AutoAuthor', `节点${nodeId.value}加入队列`, true)
      nodeId.value = ''
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor', `失败: ${error.message}`, false)
    },
  }
)

const enqueueNode = () => {
  if (!nodeId.value) {
    showSnackbar?.('请输入节点ID', 'error')
    return
  }
  enqueueNodeExecute(parseInt(nodeId.value, 10))
}

const { execute: scanNodes, loading: scanningNodes } = useMutation(adminApi.scanAutoAuthorNodes, {
  successMessage: '扫描已开始',
  onSuccess: () => {
    addOperationToHistory('AutoAuthor', '开始扫描节点', true)
  },
  onError: (error) => {
    addOperationToHistory('AutoAuthor', `扫描失败: ${error.message}`, false)
  },
})

const { execute: resetSession, loading: resettingSession } = useMutation(
  adminApi.resetAutoAuthorSession,
  {
    successMessage: '会话已重置',
    onSuccess: () => {
      addOperationToHistory('AutoAuthor', '重置会话', true)
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor', `重置失败: ${error.message}`, false)
    },
  }
)

const confirmClearQueue = (): void => {
  if (confirm('确定要清空所有AutoAuthor队列吗？')) {
    clearQueueExecute()
  }
}

const { execute: clearQueueExecute, loading: clearingQueue } = useMutation(
  adminApi.clearAutoAuthorQueue,
  {
    successMessage: '队列已清空',
    onSuccess: () => {
      addOperationToHistory('AutoAuthor', '清空队列', true)
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor', `清空失败: ${error.message}`, false)
    },
  }
)

// 节点引用数统计
const recalculateResult = ref<{ processedPosts: number; updatedNodes: number } | null>(null)

const { execute: recalculateReferences, loading: recalculating } = useMutation(
  adminApi.recalculateNodeReferences,
  {
    showToast: false,
    onSuccess: (result) => {
      recalculateResult.value = result
      showSnackbar?.('节点引用数统计已更新', 'success')
      addOperationToHistory(
        '节点引用数统计',
        `处理${result.processedPosts}个目录，更新${result.updatedNodes}个节点`,
        true
      )
    },
    onError: (error) => {
      addOperationToHistory('节点引用数统计', `失败: ${error.message}`, false)
    },
  }
)

// 搜索索引同步
const { execute: syncAllIndexes, loading: syncingAll } = useMutation(
  adminApi.syncAllSearchIndexes,
  {
    successMessage: '全量同步已开始',
    onSuccess: () => {
      addOperationToHistory('搜索索引', '全量同步', true)
    },
    onError: (error) => {
      addOperationToHistory('搜索索引', `全量同步失败: ${error.message}`, false)
    },
  }
)

const { execute: syncCourses, loading: syncingCourses } = useMutation(
  adminApi.syncCourseIndexes,
  {
    showToast: false,
    onSuccess: (result) => {
      showSnackbar?.(`同步了${result}个课程`, 'success')
      addOperationToHistory('搜索索引', `同步${result}个课程`, true)
    },
    onError: (error) => {
      addOperationToHistory('搜索索引', `课程同步失败: ${error.message}`, false)
    },
  }
)

const { execute: syncNodes, loading: syncingNodes } = useMutation(
  adminApi.syncNodeIndexes,
  {
    showToast: false,
    onSuccess: (result) => {
      showSnackbar?.(`同步了${result}个节点`, 'success')
      addOperationToHistory('搜索索引', `同步${result}个节点`, true)
    },
    onError: (error) => {
      addOperationToHistory('搜索索引', `节点同步失败: ${error.message}`, false)
    },
  }
)

const { execute: syncUsers, loading: syncingUsers } = useMutation(
  adminApi.syncUserIndexes,
  {
    showToast: false,
    onSuccess: (result) => {
      showSnackbar?.(`同步了${result}个用户`, 'success')
      addOperationToHistory('搜索索引', `同步${result}个用户`, true)
    },
    onError: (error) => {
      addOperationToHistory('搜索索引', `用户同步失败: ${error.message}`, false)
    },
  }
)

const { execute: syncProfessions, loading: syncingProfessions } = useMutation(
  adminApi.syncProfessionIndexes,
  {
    showToast: false,
    onSuccess: (result) => {
      showSnackbar?.(`同步了${result}个职业`, 'success')
      addOperationToHistory('搜索索引', `同步${result}个职业`, true)
    },
    onError: (error) => {
      addOperationToHistory('搜索索引', `职业同步失败: ${error.message}`, false)
    },
  }
)

setTimeout(() => {
  updateReadonlyMode()
}, 100)
</script>

<template>
  <div class="system-operations">
    <h2 class="text-h5 font-weight-bold mb-6">系统操作</h2>

    <!-- 只读模式 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-shield-lock-outline" class="mr-2"></v-icon>
        只读模式
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">
          开启后禁止所有写操作，适用于系统维护、数据迁移等场景
        </p>
        <div class="d-flex align-center justify-space-between pa-3 border rounded">
          <div>
            <div class="font-weight-medium">
              {{ readonlyModeEnabled ? '只读模式已开启' : '正常运行中' }}
            </div>
            <div class="text-caption text-grey">
              {{ readonlyModeEnabled ? '所有写操作已禁止' : '系统可正常读写' }}
            </div>
          </div>
          <v-switch
            v-model="readonlyModeEnabled"
            color="primary"
            :loading="togglingReadonlyMode"
            hide-details
            @update:model-value="toggleReadonlyMode"
          ></v-switch>
        </div>
      </v-card-text>
    </v-card>

    <!-- Redis 统计数据同步 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-database-sync" class="mr-2"></v-icon>
        Redis 统计数据同步
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">将 Redis 中的统计数据同步到数据库</p>
        <div class="d-flex ga-3 flex-wrap">
          <v-btn
            variant="tonal"
            :loading="syncingManual"
            :disabled="syncingSpecific"
            @click="syncStatsManual"
          >
            <v-icon icon="mdi-sync" class="mr-2"></v-icon>
            立即同步
          </v-btn>
          <v-text-field
            v-model="syncDate"
            type="date"
            variant="outlined"
            density="compact"
            hide-details
            style="max-width: 200px"
          ></v-text-field>
          <v-btn
            variant="tonal"
            :loading="syncingSpecific"
            :disabled="syncingManual"
            @click="syncStatsSpecificDate"
          >
            同步指定日期
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 系统健康检查 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-heart-pulse" class="mr-2"></v-icon>
        系统健康检查
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">检查系统各组件运行状态</p>
        <div class="d-flex align-center justify-space-between">
          <v-btn variant="tonal" :loading="checkingHealth" @click="checkSystemHealth">
            <v-icon icon="mdi-stethoscope" class="mr-2"></v-icon>
            检查系统健康
          </v-btn>
          <div v-if="lastHealthCheck" class="text-caption text-grey">
            上次检查：{{ lastHealthCheck }}
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 节点引用数统计 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-chart-line" class="mr-2"></v-icon>
        节点引用数统计
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">重新计算所有节点的引用次数</p>
        <v-btn variant="tonal" :loading="recalculating" @click="recalculateReferences">
          <v-icon icon="mdi-refresh" class="mr-2"></v-icon>
          重新计算
        </v-btn>
        <div v-if="recalculateResult" class="mt-3 pa-3 bg-grey-lighten-4 rounded">
          <div class="text-body-2">
            处理了 {{ recalculateResult.processedPosts }} 个目录，更新了
            {{ recalculateResult.updatedNodes }} 个节点
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 搜索索引同步 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-magnify-scan" class="mr-2"></v-icon>
        搜索索引同步
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">同步数据到 Meilisearch（仅同步已发布内容）</p>
        <div class="d-flex ga-2 flex-wrap mb-3">
          <v-btn variant="flat" color="primary" :loading="syncingAll" @click="syncAllIndexes">
            <v-icon icon="mdi-sync" class="mr-2"></v-icon>
            全量同步
          </v-btn>
          <v-btn variant="tonal" :loading="syncingCourses" @click="syncCourses">
            课程
          </v-btn>
          <v-btn variant="tonal" :loading="syncingNodes" @click="syncNodes"> 节点 </v-btn>
          <v-btn variant="tonal" :loading="syncingUsers" @click="syncUsers"> 用户 </v-btn>
          <v-btn variant="tonal" :loading="syncingProfessions" @click="syncProfessions">
            职业
          </v-btn>
        </div>
        <div class="text-caption text-grey">
          提示：全量同步会删除并重建索引，请查看服务器日志了解进度
        </div>
      </v-card-text>
    </v-card>

    <!-- AutoAuthor 队列管理 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-robot" class="mr-2"></v-icon>
        AutoAuthor 队列管理
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">AI 自动创作队列管理</p>
        <div class="d-flex ga-3 flex-wrap mb-3">
          <v-text-field
            v-model="nodeId"
            label="节点ID"
            type="number"
            variant="outlined"
            density="compact"
            hide-details
            style="max-width: 150px"
          ></v-text-field>
          <v-btn variant="tonal" :loading="enqueuingNode" :disabled="!nodeId" @click="enqueueNode">
            <v-icon icon="mdi-plus" class="mr-2"></v-icon>
            加入队列
          </v-btn>
        </div>
        <div class="d-flex ga-2 flex-wrap">
          <v-btn variant="tonal" :loading="scanningNodes" @click="scanNodes">
            <v-icon icon="mdi-radar" class="mr-2"></v-icon>
            扫描节点
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

    <!-- 操作历史 -->
    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-history" class="mr-2"></v-icon>
        操作历史
      </v-card-title>
      <v-card-text>
        <div v-if="operationHistory.length === 0" class="text-center py-8 text-grey">
          暂无操作记录
        </div>
        <div v-else>
          <div
            v-for="(op, index) in operationHistory"
            :key="index"
            class="d-flex align-center justify-space-between py-2 border-b"
          >
            <div class="d-flex align-center">
              <v-icon
                :icon="op.success ? 'mdi-check-circle' : 'mdi-alert-circle'"
                :color="op.success ? 'success' : 'error'"
                size="18"
                class="mr-2"
              ></v-icon>
              <div>
                <div class="text-body-2 font-weight-medium">{{ op.title }}</div>
                <div class="text-caption text-grey">{{ op.description }}</div>
              </div>
            </div>
            <div class="text-caption text-grey">{{ op.time }}</div>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.system-operations {
  max-width: 100%;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>
