<script setup lang="ts">
import { ref, inject } from 'vue'
import { adminApi } from '@/api'
import type { ErrorLogDTO } from '@/api/modules/admin'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 筛选条件
const sourceFilter = ref<string>('')
const statusFilter = ref<string>('')

// 数据
const logs = ref<ErrorLogDTO[]>([])
const loading = ref(false)
const hasMore = ref(true)
const lastId = ref<number | null>(null)

// 详情弹窗
const detailDialog = ref(false)
const selectedLog = ref<ErrorLogDTO | null>(null)

// 来源选项
const sourceOptions = [
  { title: '全部', value: '' },
  { title: '后端', value: 'backend' },
  { title: '前端', value: 'frontend' },
]

// 状态选项
const statusOptions = [
  { title: '全部', value: '' },
  { title: '新错误', value: 'new' },
  { title: '已忽略', value: 'ignored' },
  { title: '已解决', value: 'resolved' },
]

/**
 * 获取状态颜色
 */
const getStatusColor = (status: string): string => {
  switch (status) {
    case 'new':
      return 'error'
    case 'ignored':
      return 'grey'
    case 'resolved':
      return 'success'
    default:
      return 'grey'
  }
}

/**
 * 获取状态文本
 */
const getStatusText = (status: string): string => {
  switch (status) {
    case 'new':
      return '新错误'
    case 'ignored':
      return '已忽略'
    case 'resolved':
      return '已解决'
    default:
      return status
  }
}

/**
 * 获取来源颜色
 */
const getSourceColor = (source: string): string => {
  return source === 'backend' ? 'blue' : 'orange'
}

/**
 * 获取来源文本
 */
const getSourceText = (source: string): string => {
  return source === 'backend' ? '后端' : '前端'
}

/**
 * 查询日志
 */
const fetchLogs = async (reset = false) => {
  if (reset) {
    logs.value = []
    lastId.value = null
    hasMore.value = true
  }

  if (!hasMore.value || loading.value) return

  loading.value = true
  try {
    const response = await adminApi.getErrorLogs({
      source: sourceFilter.value || undefined,
      status: statusFilter.value || undefined,
      lastId: lastId.value ?? undefined,
      limit: 20,
    })

    if (response.code === 200 && response.data) {
      const newLogs = response.data
      logs.value.push(...newLogs)
      hasMore.value = newLogs.length >= 20
      if (newLogs.length > 0) {
        lastId.value = newLogs[newLogs.length - 1].id
      }
    }
  } catch (error: any) {
    showSnackbar?.(`获取错误日志失败: ${error.message}`, 'error')
  } finally {
    loading.value = false
  }
}

/**
 * 加载更多
 */
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchLogs()
  }
}

/**
 * 查看详情
 */
const showDetail = (log: ErrorLogDTO) => {
  selectedLog.value = log
  detailDialog.value = true
}

/**
 * 更新状态
 */
const updateStatus = async (id: number, status: 'new' | 'ignored' | 'resolved') => {
  try {
    const response = await adminApi.updateErrorLogStatus(id, status)
    if (response.code === 200) {
      // 更新本地数据
      const log = logs.value.find((l) => l.id === id)
      if (log) {
        log.status = status
      }
      if (selectedLog.value?.id === id) {
        selectedLog.value.status = status
      }
      showSnackbar?.('状态更新成功', 'success')
    }
  } catch (error: any) {
    showSnackbar?.(`更新状态失败: ${error.message}`, 'error')
  }
}

/**
 * 删除过期日志
 */
const deleteExpired = async () => {
  try {
    const response = await adminApi.deleteExpiredErrorLogs(30)
    if (response.code === 200) {
      showSnackbar?.(`已删除 ${response.data} 条过期日志`, 'success')
      fetchLogs(true)
    }
  } catch (error: any) {
    showSnackbar?.(`删除失败: ${error.message}`, 'error')
  }
}

// 初始加载
fetchLogs(true)
</script>

<template>
  <div>
    <div class="d-flex justify-space-between align-center mb-4">
      <h2 class="text-h5 font-weight-bold">错误日志</h2>
      <v-btn variant="tonal" color="warning" size="small" @click="deleteExpired">
        <v-icon icon="mdi-delete-clock" size="16" class="mr-1"></v-icon>
        清理30天前日志
      </v-btn>
    </div>

    <!-- 筛选区域 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row dense>
          <v-col cols="auto">
            <v-select
              v-model="sourceFilter"
              label="来源"
              :items="sourceOptions"
              variant="outlined"
              density="compact"
              hide-details
              style="width: 120px"
            ></v-select>
          </v-col>
          <v-col cols="auto">
            <v-select
              v-model="statusFilter"
              label="状态"
              :items="statusOptions"
              variant="outlined"
              density="compact"
              hide-details
              style="width: 120px"
            ></v-select>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" color="primary" :loading="loading" @click="fetchLogs(true)">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 日志列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 空状态 -->
        <div v-if="logs.length === 0 && !loading" class="text-center py-12">
          <v-icon icon="mdi-bug-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">暂无错误日志</p>
        </div>

        <!-- 列表 -->
        <div v-if="logs.length > 0">
          <div
            v-for="log in logs"
            :key="log.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (isIntersecting && log === logs[logs.length - 1] && hasMore && !loading) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
            @click="showDetail(log)"
          >
            <!-- 标题行 -->
            <div class="d-flex align-center justify-space-between mb-2">
              <div class="d-flex align-center">
                <v-chip
                  :color="getSourceColor(log.source)"
                  size="x-small"
                  variant="tonal"
                  class="mr-2"
                >
                  {{ getSourceText(log.source) }}
                </v-chip>
                <v-chip
                  :color="getStatusColor(log.status)"
                  size="x-small"
                  variant="tonal"
                  class="mr-2"
                >
                  {{ getStatusText(log.status) }}
                </v-chip>
                <span class="text-body-1 font-weight-medium text-grey-darken-3">
                  {{ log.errorType }}
                </span>
              </div>
              <div class="d-flex align-center text-caption text-grey">
                <v-chip size="x-small" color="error" variant="tonal" class="mr-2">
                  × {{ log.count }}
                </v-chip>
                <span>{{ log.lastSeenAt }}</span>
                <span class="mx-1">·</span>
                <span>#{{ log.id }}</span>
              </div>
            </div>

            <!-- 错误消息 -->
            <div class="content-wrapper">
              <div class="text-body-2 text-grey-darken-2 message-text">
                {{ log.message }}
              </div>
              <div class="text-caption text-grey mt-1">
                {{ log.url }}
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

    <!-- 详情弹窗 -->
    <v-dialog v-model="detailDialog" max-width="800">
      <v-card v-if="selectedLog" variant="flat" rounded="lg">
        <v-card-title class="d-flex justify-space-between align-center pa-6 pb-4">
          <span>错误详情</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="detailDialog = false"></v-btn>
        </v-card-title>

        <v-card-text>
          <!-- 基本信息 -->
          <v-row dense class="mb-4">
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">来源</span>
              <v-chip :color="getSourceColor(selectedLog.source)" size="small" variant="tonal">
                {{ getSourceText(selectedLog.source) }}
              </v-chip>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">状态</span>
              <v-chip :color="getStatusColor(selectedLog.status)" size="small" variant="tonal">
                {{ getStatusText(selectedLog.status) }}
              </v-chip>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">错误类型</span>
              <span class="text-body-2">{{ selectedLog.errorType }}</span>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">发生次数</span>
              <span class="text-body-2">{{ selectedLog.count }}</span>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">首次发生</span>
              <span class="text-body-2">{{ selectedLog.firstSeenAt }}</span>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">最近发生</span>
              <span class="text-body-2">{{ selectedLog.lastSeenAt }}</span>
            </v-col>
            <v-col cols="12">
              <span class="text-caption text-grey mr-2">URL</span>
              <span class="text-body-2">{{ selectedLog.url }}</span>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">IP</span>
              <span class="text-body-2">{{ selectedLog.ip || '-' }}</span>
            </v-col>
            <v-col cols="6">
              <span class="text-caption text-grey mr-2">用户ID</span>
              <span class="text-body-2">{{ selectedLog.userId || '-' }}</span>
            </v-col>
          </v-row>

          <!-- 错误消息 -->
          <div class="text-caption text-grey mb-1">错误消息</div>
          <div class="code-block mb-4">{{ selectedLog.message }}</div>

          <!-- 堆栈信息 -->
          <div class="text-caption text-grey mb-1">堆栈信息</div>
          <div class="code-block stack-trace">{{ selectedLog.stackTrace || '无堆栈信息' }}</div>
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            v-if="selectedLog.status !== 'ignored'"
            variant="tonal"
            color="grey"
            @click="updateStatus(selectedLog.id, 'ignored')"
          >
            忽略
          </v-btn>
          <v-btn
            v-if="selectedLog.status !== 'resolved'"
            variant="tonal"
            color="success"
            @click="updateStatus(selectedLog.id, 'resolved')"
          >
            标记已解决
          </v-btn>
          <v-btn
            v-if="selectedLog.status !== 'new'"
            variant="tonal"
            color="error"
            @click="updateStatus(selectedLog.id, 'new')"
          >
            标记为新
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

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
  cursor: pointer;
  transition: background-color 0.2s;
}

.list-item:hover {
  background-color: #f0f0f0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}

.message-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-block {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  background-color: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.stack-trace {
  max-height: 300px;
  overflow-y: auto;
}
</style>
