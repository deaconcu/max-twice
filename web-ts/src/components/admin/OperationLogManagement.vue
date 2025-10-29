<template>
  <div class="operation-log-management">
    <!-- 筛选区域 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" elevation="0" color="grey-lighten-5">
      <v-row dense>
        <!-- 时间范围选择 -->
        <v-col cols="12" md="3">
          <v-text-field
            v-model="filters.startTime"
            label="开始时间"
            type="datetime-local"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <v-text-field
            v-model="filters.endTime"
            label="结束时间"
            type="datetime-local"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>

        <!-- 操作人ID -->
        <v-col cols="12" md="2">
          <v-text-field
            v-model="filters.operatorId"
            label="操作人ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>

        <!-- 操作级别 -->
        <v-col cols="12" md="2">
          <v-select
            v-model="filters.operationLevel"
            label="操作级别"
            :items="operationLevels"
            item-title="text"
            item-value="value"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-select>
        </v-col>

        <!-- 查询按钮 -->
        <v-col cols="12" md="2">
          <div class="d-flex gap-2">
            <v-btn
              variant="flat"
              color="primary"
              rounded="lg"
              :loading="loading"
              @click="fetchLogs(true)"
              block
            >
              <v-icon icon="mdi-magnify" size="16" class="mr-2"></v-icon>
              查询
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <!-- 模块和操作类型筛选 -->
      <v-row dense class="mt-2">
        <v-col cols="12" md="3">
          <v-text-field
            v-model="filters.module"
            label="模块"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <v-text-field
            v-model="filters.operationType"
            label="操作类型"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" md="3">
          <v-text-field
            v-model="filters.targetType"
            label="目标类型"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
      </v-row>
    </v-card>

    <!-- 操作日志列表 -->
    <div v-if="logs.length === 0 && !loading" class="text-center py-12">
      <v-icon icon="mdi-clipboard-text-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-h6 text-grey-darken-1 mb-2">暂无操作日志</p>
      <p class="text-body-2 text-grey">请调整筛选条件后重新查询</p>
    </div>

    <!-- 滚动容器 -->
    <div v-else class="logs-container" @scroll="handleScroll">
      <v-card
        v-for="log in logs"
        :key="log.id"
        flat
        class="pa-4 mb-3"
        rounded="lg"
        outlined
      >
        <div class="d-flex align-start justify-space-between">
          <!-- 左侧：操作信息 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-2">
              <!-- 操作级别标签 -->
              <v-chip
                :color="getLevelColor(log.operationLevel)"
                size="small"
                variant="flat"
                class="mr-2"
              >
                {{ getLevelText(log.operationLevel) }}
              </v-chip>

              <!-- 模块 -->
              <v-chip size="small" variant="outlined" class="mr-2">
                {{ log.module }}
              </v-chip>

              <!-- 操作类型 -->
              <span class="text-subtitle-1 font-weight-bold text-grey-darken-3">
                {{ log.operationType }}
              </span>
            </div>

            <!-- 操作详情 -->
            <div class="text-body-2 text-grey-darken-1 mb-2">
              <v-icon icon="mdi-account" size="16" class="mr-1"></v-icon>
              <strong>{{ log.operatorName }}</strong> (ID: {{ log.operatorId }})
              <v-chip size="x-small" variant="flat" color="grey-lighten-2" class="ml-2">
                {{ getRoleText(log.operatorRole) }}
              </v-chip>
            </div>

            <div class="text-body-2 text-grey-darken-1 mb-2">
              <v-icon icon="mdi-target" size="16" class="mr-1"></v-icon>
              目标: <strong>{{ log.targetType }}</strong> (ID: {{ log.targetId }})
              <span v-if="log.targetName" class="ml-1">- {{ log.targetName }}</span>
            </div>

            <div v-if="log.reason" class="text-body-2 text-grey-darken-1 mb-2">
              <v-icon icon="mdi-comment-text" size="16" class="mr-1"></v-icon>
              原因: {{ log.reason }}
            </div>

            <div class="text-body-2 text-grey">
              <v-icon icon="mdi-clock-outline" size="16" class="mr-1"></v-icon>
              {{ formatDateTime(log.createdAt) }}
              <span v-if="log.ipAddress" class="ml-3">
                <v-icon icon="mdi-ip" size="16" class="mr-1"></v-icon>
                {{ log.ipAddress }}
              </span>
            </div>
          </div>

          <!-- 右侧：操作按钮 -->
          <div>
            <v-btn
              icon
              variant="text"
              size="small"
              @click="showDetail(log)"
            >
              <v-icon icon="mdi-eye" size="20"></v-icon>
            </v-btn>
          </div>
        </div>
      </v-card>

      <!-- 加载中提示 -->
      <div v-if="loading" class="text-center py-4">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
        <p class="text-body-2 text-grey mt-2">加载中...</p>
      </div>

      <!-- 没有更多数据提示 -->
      <div v-if="!hasMore && logs.length > 0" class="text-center py-4">
        <v-divider class="mb-4"></v-divider>
        <p class="text-body-2 text-grey">没有更多数据了</p>
      </div>
    </div>

    <!-- 详情对话框 -->
    <v-dialog v-model="detailDialog" max-width="800">
      <v-card rounded="lg">
        <v-card-title class="d-flex align-center justify-space-between bg-grey-lighten-4">
          <span class="text-h6">操作日志详情</span>
          <v-btn icon variant="text" @click="detailDialog = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>

        <v-card-text v-if="selectedLog" class="pt-4">
          <v-list lines="two">
            <v-list-item>
              <v-list-item-title class="font-weight-bold">日志ID</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.id }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">操作人</v-list-item-title>
              <v-list-item-subtitle>
                {{ selectedLog.operatorName }} (ID: {{ selectedLog.operatorId }})
                - {{ getRoleText(selectedLog.operatorRole) }}
              </v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">模块</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.module }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">操作类型</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.operationType }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">操作级别</v-list-item-title>
              <v-list-item-subtitle>
                <v-chip :color="getLevelColor(selectedLog.operationLevel)" size="small">
                  {{ getLevelText(selectedLog.operationLevel) }}
                </v-chip>
              </v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">目标类型</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.targetType }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">目标ID</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.targetId }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item v-if="selectedLog.targetName">
              <v-list-item-title class="font-weight-bold">目标名称</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.targetName }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item v-if="selectedLog.reason">
              <v-list-item-title class="font-weight-bold">原因</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.reason }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item v-if="selectedLog.ipAddress">
              <v-list-item-title class="font-weight-bold">IP地址</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.ipAddress }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">操作时间</v-list-item-title>
              <v-list-item-subtitle>{{ formatDateTime(selectedLog.createdAt) }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item v-if="selectedLog.extraData">
              <v-list-item-title class="font-weight-bold">额外数据</v-list-item-title>
              <v-list-item-subtitle>
                <pre class="text-caption mt-2">{{ JSON.stringify(selectedLog.extraData, null, 2) }}</pre>
              </v-list-item-subtitle>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, inject } from 'vue'
import { adminOperationLogServiceV1 } from '@/services/api/v1/adminApiServiceV1'
import type { OperationLogDTO, OperationLogQueryRequest } from '@/types/operationLog'
import { OperationLevel } from '@/types/operationLog'
import { UserRole } from '@/types/user'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 响应式数据
const logs = ref<OperationLogDTO[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number | undefined>(undefined)

const detailDialog = ref(false)
const selectedLog = ref<OperationLogDTO | null>(null)

// 筛选条件
const filters = reactive<Omit<OperationLogQueryRequest, 'lastId' | 'limit'>>({
  operatorId: undefined,
  module: '',
  operationType: '',
  targetType: '',
  operationLevel: undefined,
  startTime: '',
  endTime: '',
})

// 操作级别选项
const operationLevels = [
  { text: '低', value: OperationLevel.LOW },
  { text: '中', value: OperationLevel.MEDIUM },
  { text: '高', value: OperationLevel.HIGH },
]

/**
 * 获取操作级别颜色
 */
const getLevelColor = (level: OperationLevel): string => {
  switch (level) {
    case OperationLevel.LOW:
      return 'green'
    case OperationLevel.MEDIUM:
      return 'orange'
    case OperationLevel.HIGH:
      return 'red'
    default:
      return 'grey'
  }
}

/**
 * 获取操作级别文本
 */
const getLevelText = (level: OperationLevel): string => {
  switch (level) {
    case OperationLevel.LOW:
      return '低'
    case OperationLevel.MEDIUM:
      return '中'
    case OperationLevel.HIGH:
      return '高'
    default:
      return '未知'
  }
}

/**
 * 获取角色文本
 */
const getRoleText = (role: UserRole): string => {
  switch (role) {
    case UserRole.ADMIN:
      return '管理员'
    case UserRole.MODERATOR:
      return '版主'
    case UserRole.USER:
      return '用户'
    default:
      return '未知'
  }
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dateTime: string): string => {
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

/**
 * 获取操作日志
 */
const fetchLogs = async (reset: boolean = false): Promise<void> => {
  if (loading.value) return

  if (reset) {
    logs.value = []
    lastId.value = undefined
    hasMore.value = true
  }

  if (!hasMore.value) return

  loading.value = true

  try {
    const query: OperationLogQueryRequest = {
      ...filters,
      lastId: lastId.value,
      limit: 20,
    }

    // 清空空字符串值
    if (query.module === '') delete query.module
    if (query.operationType === '') delete query.operationType
    if (query.targetType === '') delete query.targetType
    if (query.startTime === '') delete query.startTime
    if (query.endTime === '') delete query.endTime

    const response = await adminOperationLogServiceV1.getOperationLogs(query)

    if (response.code === 200 && response.data) {
      const newLogs = response.data.items || []
      logs.value.push(...newLogs)
      hasMore.value = response.data.hasMore
      lastId.value = response.data.nextLastId
    } else {
      throw new Error(response.message || '获取操作日志失败')
    }
  } catch (error: any) {
    console.error('[OperationLogManagement] 获取操作日志失败:', error)
    showSnackbar?.(`获取操作日志失败: ${error.message}`, 'error')
  } finally {
    loading.value = false
  }
}

/**
 * 处理滚动事件（无限滚动）
 */
const handleScroll = (event: Event): void => {
  const target = event.target as HTMLElement
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight

  // 当滚动到底部附近时加载更多
  if (scrollTop + clientHeight >= scrollHeight - 100 && !loading.value && hasMore.value) {
    fetchLogs(false)
  }
}

/**
 * 显示详情对话框
 */
const showDetail = (log: OperationLogDTO): void => {
  selectedLog.value = log
  detailDialog.value = true
}

// 初始加载
fetchLogs(true)
</script>

<style scoped>
.operation-log-management {
  width: 100%;
}

.logs-container {
  max-height: 600px;
  overflow-y: auto;
}

pre {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
}

/* 滚动条样式 */
.logs-container::-webkit-scrollbar {
  width: 8px;
}

.logs-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.logs-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.logs-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
