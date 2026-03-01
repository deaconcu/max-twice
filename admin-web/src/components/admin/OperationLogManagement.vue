<script setup lang="ts">
import { ref, reactive, inject } from 'vue'
import { adminApi } from '@/api'
import { useFetchForScroll } from '@/composables/useFetchForScroll'
import type { OperationLogDTO, OperationLogQueryRequest } from '@/types/operationLog'
import { OperationLevel } from '@/types/operationLog'
import { UserRole } from '@/enums'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 详情对话框
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

// 使用 useFetchForScroll 管理分页数据
const {
  items: logs,
  loading,
  hasMore,
  loadMore: loadMoreLogs,
  reset: resetLogs,
} = useFetchForScroll<OperationLogDTO>({
  fetchFn: (params) => {
    const query: OperationLogQueryRequest = {
      ...filters,
      lastId: params.lastId ?? undefined,
      limit: 20,
    }

    // 清空空字符串值
    if (query.module === '') delete query.module
    if (query.operationType === '') delete query.operationType
    if (query.targetType === '') delete query.targetType
    if (query.startTime === '') delete query.startTime
    if (query.endTime === '') delete query.endTime

    return adminApi.getOperationLogs(query)
  },
  initialParams: {
    lastId: null,
  },
  onError: (error) => {
    console.error('获取操作日志失败:', error)
    showSnackbar?.(`获取操作日志失败: ${error.message}`, 'error')
  },
})

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
 * 获取操作日志（用于查询按钮）
 */
const fetchLogs = async (reset = false): Promise<void> => {
  if (reset) {
    resetLogs()
  }
  loadMoreLogs({ done: () => {} } as any)
}

/**
 * 加载更多数据（v-infinite-scroll 回调）
 */
const loadMore = async ({
  done,
}: {
  done: (status: 'ok' | 'empty' | 'error') => void
}): Promise<void> => {
  try {
    await loadMoreLogs({ done: () => {} } as any)
    done(hasMore.value ? 'ok' : 'empty')
  } catch (error) {
    done('error')
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

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">操作日志</h2>

    <!-- 筛选区域 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-filter-variant" size="18" class="mr-2"></v-icon>
        筛选条件
      </v-card-title>
      <v-card-text>
        <v-row dense>
          <v-col cols="12" md="3">
            <v-text-field
              v-model="filters.startTime"
              label="开始时间"
              type="datetime-local"
              variant="outlined"
              density="compact"
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
              hide-details
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" md="2">
            <v-text-field
              v-model="filters.operatorId"
              label="操作人ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              clearable
            ></v-text-field>
          </v-col>
          <v-col cols="12" md="2">
            <v-select
              v-model="filters.operationLevel"
              label="操作级别"
              :items="operationLevels"
              item-title="text"
              item-value="value"
              variant="outlined"
              density="compact"
              hide-details
              clearable
            ></v-select>
          </v-col>
          <v-col cols="12" md="2">
            <v-btn
              variant="tonal"
              color="primary"
              :loading="loading"
              block
              @click="fetchLogs(true)"
            >
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
          </v-col>
        </v-row>

        <v-row dense class="mt-2">
          <v-col cols="12" md="3">
            <v-text-field
              v-model="filters.module"
              label="模块"
              variant="outlined"
              density="compact"
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
              hide-details
              clearable
            ></v-text-field>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 日志列表 -->
    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-clipboard-text" size="18" class="mr-2"></v-icon>
        操作日志列表
      </v-card-title>
      <v-card-text>
        <!-- 空状态 -->
        <div v-if="logs.length === 0 && !loading" class="text-center py-12">
          <v-icon icon="mdi-clipboard-text-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">暂无操作日志</p>
          <p class="text-body-2 text-grey">请调整筛选条件后重新查询</p>
        </div>

        <!-- 列表 -->
        <div v-if="logs.length > 0">
          <div
            v-for="log in logs"
            :key="log.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (isIntersecting && log === logs[logs.length - 1] && hasMore && !loading) {
                  loadMoreLogs({ done: () => {} } as any)
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start justify-space-between">
              <!-- 左侧：操作信息 -->
              <div class="flex-grow-1">
                <div class="d-flex align-center mb-2">
                  <v-chip
                    :color="getLevelColor(log.operationLevel)"
                    size="small"
                    variant="flat"
                    class="mr-2"
                  >
                    {{ getLevelText(log.operationLevel) }}
                  </v-chip>
                  <v-chip size="small" variant="tonal" color="grey" class="mr-2">
                    {{ log.module }}
                  </v-chip>
                  <span class="text-subtitle-1 font-weight-bold text-grey-darken-3">
                    {{ log.operationType }}
                  </span>
                </div>

                <div class="content-wrapper">
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
              </div>

              <!-- 右侧：查看详情 -->
              <div class="ml-3">
                <v-btn variant="tonal" color="info" size="small" @click="showDetail(log)">
                  <v-icon icon="mdi-eye" size="16" class="mr-1"></v-icon>
                  详情
                </v-btn>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="loading" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMore && logs.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

    <!-- 详情对话框 -->
    <v-dialog v-model="detailDialog" max-width="800">
      <v-card rounded="lg" variant="flat">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <v-icon icon="mdi-clipboard-text-outline" color="blue-darken-1" class="mr-3"></v-icon>
              <span class="text-h6 font-weight-bold">操作日志详情</span>
            </div>
            <v-btn icon variant="text" @click="detailDialog = false">
              <v-icon>mdi-close</v-icon>
            </v-btn>
          </div>
        </v-card-title>

        <v-card-text v-if="selectedLog" class="pa-6 pt-2">
          <v-list lines="two">
            <v-list-item>
              <v-list-item-title class="font-weight-bold">日志ID</v-list-item-title>
              <v-list-item-subtitle>{{ selectedLog.id }}</v-list-item-subtitle>
            </v-list-item>

            <v-list-item>
              <v-list-item-title class="font-weight-bold">操作人</v-list-item-title>
              <v-list-item-subtitle>
                {{ selectedLog.operatorName }} (ID: {{ selectedLog.operatorId }}) -
                {{ getRoleText(selectedLog.operatorRole) }}
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
                <v-chip :color="getLevelColor(selectedLog.operationLevel)" size="small" variant="flat">
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

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
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
</style>
