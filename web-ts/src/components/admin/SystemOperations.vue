<template>
  <div class="system-operations">
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-purple-lighten-5 mr-3">
          <v-icon icon="mdi-cog-sync" color="purple-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('admin.systemOperations.title') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">
            {{ t('admin.systemOperations.subtitle') }}
          </p>
        </div>
      </div>
      <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
        <v-icon icon="mdi-tools" color="purple-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-purple-darken-2 text-caption">{{
          t('systemOperations.adminTools')
        }}</span>
      </v-chip>
    </div>

    <!-- Redis 统计数据同步 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-database-sync" color="red-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">
          {{ t('systemOperations.redisSync.title') }}
        </h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          {{ t('systemOperations.redisSync.description') }}
        </p>

        <v-row class="mb-4">
          <!-- 手动同步 -->
          <v-col cols="12" md="6">
            <v-card flat class="pa-4 bg-orange-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon icon="mdi-sync" color="orange-darken-2" size="20" class="mr-2"></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-orange-darken-2">
                  {{ t('systemOperations.redisSync.manualSync') }}
                </h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                {{ t('systemOperations.redisSync.manualSyncDesc') }}
              </p>
              <v-btn
                variant="flat"
                color="orange-darken-1"
                rounded="lg"
                size="small"
                :loading="syncingManual"
                :disabled="syncingManual || syncingSpecific"
                @click="syncStatsManual"
              >
                <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
                {{ t('systemOperations.redisSync.syncNow') }}
              </v-btn>
            </v-card>
          </v-col>

          <!-- 指定日期同步 -->
          <v-col cols="12" md="6">
            <v-card flat class="pa-4 bg-blue-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon
                  icon="mdi-calendar-sync"
                  color="blue-darken-2"
                  size="20"
                  class="mr-2"
                ></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-blue-darken-2">指定日期同步</h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                同步指定日期的统计数据，留空则同步昨日数据
              </p>
              <div class="d-flex align-center gap-3">
                <v-text-field
                  v-model="syncDate"
                  type="date"
                  variant="outlined"
                  density="compact"
                  rounded="lg"
                  bg-color="white"
                  hide-details
                  class="date-input"
                  placeholder="选择日期"
                ></v-text-field>
                <v-btn
                  variant="flat"
                  color="blue-darken-1"
                  rounded="lg"
                  density="compact"
                  :loading="syncingSpecific"
                  :disabled="syncingManual || syncingSpecific"
                  class="sync-button ml-3"
                  @click="syncStatsSpecificDate"
                >
                  <v-icon icon="mdi-play" size="16" class="mr-2"></v-icon>
                  同步
                </v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- 同步状态显示 -->
        <v-alert
          v-if="syncResult"
          :type="syncResult.type"
          variant="tonal"
          class="mt-3"
          rounded="lg"
          closable
          @click:close="syncResult = null"
        >
          <div class="font-weight-bold">{{ syncResult.title }}</div>
          <div class="text-body-2 mt-1">{{ syncResult.message }}</div>
          <div v-if="syncResult.details" class="text-caption mt-2 text-grey-darken-1">
            {{ syncResult.details }}
          </div>
        </v-alert>
      </div>
    </v-card>

    <!-- 系统健康检查 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-heart-pulse" color="green-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">系统健康检查</h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          检查系统各个组件的运行状态，包括数据库连接、Redis连接、统计服务等。
        </p>

        <div class="d-flex align-center justify-space-between">
          <v-btn
            variant="flat"
            color="green-darken-1"
            rounded="lg"
            size="small"
            :loading="checkingHealth"
            @click="checkSystemHealth"
          >
            <v-icon icon="mdi-stethoscope" size="16" class="mr-2"></v-icon>
            检查系统健康状态
          </v-btn>

          <div v-if="lastHealthCheck" class="text-caption text-grey-darken-1">
            上次检查：{{ lastHealthCheck }}
          </div>
        </div>

        <!-- 健康检查结果 -->
        <v-alert
          v-if="healthResult"
          :type="healthResult.type"
          variant="tonal"
          class="mt-4"
          rounded="lg"
          closable
          @click:close="healthResult = null"
        >
          <div class="font-weight-bold">{{ healthResult.title }}</div>
          <div class="text-body-2 mt-1">{{ healthResult.message }}</div>
          <div v-if="healthResult.details" class="text-caption mt-2 text-grey-darken-1">
            <pre class="text-caption">{{ healthResult.details }}</pre>
          </div>
        </v-alert>
      </div>
    </v-card>

    <!-- 缓存管理 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-cached" color="indigo-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">缓存管理</h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          管理系统缓存，包括清理过期缓存、重置缓存等操作。
        </p>

        <v-row>
          <v-col cols="12" md="4">
            <v-card flat class="pa-3 bg-indigo-lighten-5" rounded="lg" elevation="0">
              <div class="text-center">
                <v-icon
                  icon="mdi-delete-sweep"
                  color="indigo-darken-2"
                  size="24"
                  class="mb-2"
                ></v-icon>
                <h6 class="text-subtitle-2 font-weight-bold text-indigo-darken-2 mb-2">
                  清理过期缓存
                </h6>
                <v-btn variant="flat" color="indigo-darken-1" rounded="lg" size="x-small" disabled>
                  <v-icon icon="mdi-broom" size="14" class="mr-1"></v-icon>
                  清理
                </v-btn>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card flat class="pa-3 bg-indigo-lighten-5" rounded="lg" elevation="0">
              <div class="text-center">
                <v-icon icon="mdi-refresh" color="indigo-darken-2" size="24" class="mb-2"></v-icon>
                <h6 class="text-subtitle-2 font-weight-bold text-indigo-darken-2 mb-2">重置缓存</h6>
                <v-btn variant="flat" color="indigo-darken-1" rounded="lg" size="x-small" disabled>
                  <v-icon icon="mdi-restart" size="14" class="mr-1"></v-icon>
                  重置
                </v-btn>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="4">
            <v-card flat class="pa-3 bg-indigo-lighten-5" rounded="lg" elevation="0">
              <div class="text-center">
                <v-icon
                  icon="mdi-information"
                  color="indigo-darken-2"
                  size="24"
                  class="mb-2"
                ></v-icon>
                <h6 class="text-subtitle-2 font-weight-bold text-indigo-darken-2 mb-2">缓存信息</h6>
                <v-btn variant="flat" color="indigo-darken-1" rounded="lg" size="x-small" disabled>
                  <v-icon icon="mdi-eye" size="14" class="mr-1"></v-icon>
                  查看
                </v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <v-alert type="info" variant="tonal" class="mt-3" rounded="lg">
          <div class="font-weight-bold">功能开发中</div>
          <div class="text-body-2 mt-1">缓存管理功能正在开发中，敬请期待...</div>
        </v-alert>
      </div>
    </v-card>

    <!-- AutoAuthor 队列管理 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-robot" color="purple-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">AutoAuthor 队列管理</h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          将指定的节点（Node）加入到 AutoAuthor 自动创作队列中，系统将自动为该节点生成内容。
        </p>

        <v-row class="mb-4">
          <!-- 手动入队 -->
          <v-col cols="12" md="3">
            <v-card flat class="pa-4 bg-purple-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon icon="mdi-playlist-plus" color="purple-darken-2" size="20" class="mr-2"></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-purple-darken-2">
                  加入创作队列
                </h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                将节点ID加入到AutoAuthor队列中
              </p>
              <div class="d-flex align-center gap-3">
                <v-text-field
                  v-model="nodeId"
                  label="节点ID"
                  type="number"
                  variant="outlined"
                  density="compact"
                  rounded="lg"
                  bg-color="white"
                  hide-details
                  class="node-id-input"
                  placeholder="请输入节点ID"
                ></v-text-field>
                <v-btn
                  variant="flat"
                  color="purple-darken-1"
                  rounded="lg"
                  density="compact"
                  :loading="enqueuingNode"
                  :disabled="!nodeId || enqueuingNode"
                  class="enqueue-button ml-3"
                  @click="enqueueNode"
                >
                  <v-icon icon="mdi-plus" size="16" class="mr-2"></v-icon>
                  加入
                </v-btn>
              </div>
            </v-card>
          </v-col>

          <!-- 扫描节点 -->
          <v-col cols="12" md="3">
            <v-card flat class="pa-4 bg-green-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon icon="mdi-magnify-scan" color="green-darken-2" size="20" class="mr-2"></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-green-darken-2">
                  扫描节点
                </h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                扫描缺少AI内容的节点并批量加入队列
              </p>
              <v-btn
                variant="flat"
                color="green-darken-1"
                rounded="lg"
                density="compact"
                :loading="scanningNodes"
                :disabled="scanningNodes"
                class="scan-button"
                @click="scanNodes"
              >
                <v-icon icon="mdi-radar" size="16" class="mr-2"></v-icon>
                开始扫描
              </v-btn>
            </v-card>
          </v-col>

          <!-- 重置会话 -->
          <v-col cols="12" md="3">
            <v-card flat class="pa-4 bg-orange-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon icon="mdi-refresh-auto" color="orange-darken-2" size="20" class="mr-2"></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-orange-darken-2">
                  重置会话
                </h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                重置与opencode的连接会话
              </p>
              <v-btn
                variant="flat"
                color="orange-darken-1"
                rounded="lg"
                density="compact"
                :loading="resettingSession"
                :disabled="resettingSession"
                class="reset-button"
                @click="resetSession"
              >
                <v-icon icon="mdi-connection" size="16" class="mr-2"></v-icon>
                重置会话
              </v-btn>
            </v-card>
          </v-col>

          <!-- 清空队列 -->
          <v-col cols="12" md="3">
            <v-card flat class="pa-4 bg-red-lighten-5" rounded="lg" elevation="0">
              <div class="d-flex align-center mb-3">
                <v-icon icon="mdi-delete-sweep" color="red-darken-2" size="20" class="mr-2"></v-icon>
                <h5 class="text-subtitle-1 font-weight-bold text-red-darken-2">
                  清空队列
                </h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                清空所有待处理的AutoAuthor创作队列
              </p>
              <v-btn
                variant="flat"
                color="red-darken-1"
                rounded="lg"
                density="compact"
                :loading="clearingQueue"
                :disabled="clearingQueue"
                class="clear-button"
                @click="confirmClearQueue"
              >
                <v-icon icon="mdi-trash-can" size="16" class="mr-2"></v-icon>
                清空队列
              </v-btn>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </v-card>

    <!-- 操作历史 -->
    <v-card flat class="pa-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-history" color="grey-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">操作历史</h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          最近的系统操作记录，包括同步操作、健康检查等。
        </p>

        <div v-if="operationHistory.length === 0" class="text-center py-8">
          <v-icon icon="mdi-history" size="48" color="grey-lighten-1" class="mb-3"></v-icon>
          <p class="text-body-2 text-grey-darken-1">暂无操作记录</p>
        </div>

        <v-timeline v-else density="compact" side="end">
          <v-timeline-item
            v-for="(operation, index) in operationHistory"
            :key="index"
            :dot-color="operation.success ? 'success' : 'error'"
            size="small"
          >
            <template #opposite>
              <div class="text-caption text-grey-darken-1">
                {{ operation.time }}
              </div>
            </template>

            <v-card
              flat
              class="pa-3"
              rounded="lg"
              :color="operation.success ? 'success-lighten-5' : 'error-lighten-5'"
            >
              <div class="d-flex align-center">
                <v-icon
                  :icon="operation.success ? 'mdi-check-circle' : 'mdi-alert-circle'"
                  :color="operation.success ? 'success-darken-1' : 'error-darken-1'"
                  size="20"
                  class="mr-2"
                ></v-icon>
                <div>
                  <div class="text-subtitle-2 font-weight-bold">{{ operation.title }}</div>
                  <div class="text-caption text-grey-darken-1">{{ operation.description }}</div>
                </div>
              </div>
            </v-card>
          </v-timeline-item>
        </v-timeline>
      </div>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import { statsServiceV1, adminAutoAuthorServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 操作历史记录类型
interface OperationRecord {
  title: string
  description: string
  success: boolean
  time: string
}

// 同步结果类型
interface SyncResult {
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  details?: string
}

// 健康检查结果类型
interface HealthResult {
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  details?: string
}

// 响应式数据
const syncDate = ref<string>('')
const syncingManual = ref<boolean>(false)
const syncingSpecific = ref<boolean>(false)
const syncResult = ref<SyncResult | null>(null)

const checkingHealth = ref<boolean>(false)
const healthResult = ref<HealthResult | null>(null)
const lastHealthCheck = ref<string>('')

const nodeId = ref<string>('')
const enqueuingNode = ref<boolean>(false)
const scanningNodes = ref<boolean>(false)
const resettingSession = ref<boolean>(false)
const clearingQueue = ref<boolean>(false)

const operationHistory = ref<OperationRecord[]>([])

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

/**
 * 添加操作记录到历史
 */
const addOperationToHistory = (title: string, description: string, success: boolean = true): void => {
  const operation: OperationRecord = {
    title,
    description,
    success,
    time: new Date().toLocaleString(),
  }

  operationHistory.value.unshift(operation)

  // 只保留最近20条记录
  if (operationHistory.value.length > 20) {
    operationHistory.value = operationHistory.value.slice(0, 20)
  }
}

/**
 * 手动同步Redis统计数据到数据库
 * 触发后端的手动同步接口，同步所有Redis中的统计数据
 */
const syncStatsManual = async (): Promise<void> => {
  syncingManual.value = true
  syncResult.value = null

  try {
    console.log('[SystemOperations] 开始手动同步Redis统计数据...')

    const response = await statsServiceV1.syncManual()

    if (response.code === 200) {
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: 'Redis统计数据已成功同步到数据库',
        details: `同步时间: ${new Date().toLocaleString()}`,
      }

      showSnackbar?.('统计数据同步成功', 'success')
      addOperationToHistory('Redis数据同步', '手动同步Redis统计数据到数据库', true)
      console.log('[SystemOperations] Redis统计数据同步成功:', response.data)
    } else {
      throw new Error(response.message || '同步失败')
    }
  } catch (error: any) {
    console.error('[SystemOperations] Redis统计数据同步失败:', error)

    syncResult.value = {
      type: 'error',
      title: '同步失败',
      message: error.message || '同步过程中发生错误',
      details: `错误时间: ${new Date().toLocaleString()}`,
    }

    showSnackbar?.(`统计数据同步失败: ${error.message}`, 'error')
    addOperationToHistory('Redis数据同步', `同步失败: ${error.message}`, false)
  } finally {
    syncingManual.value = false
  }
}

/**
 * 同步指定日期的Redis统计数据到数据库
 * 可以指定具体日期，如果不指定则同步昨日数据
 */
const syncStatsSpecificDate = async (): Promise<void> => {
  syncingSpecific.value = true
  syncResult.value = null

  try {
    const targetDate = syncDate.value || null
    console.log('[SystemOperations] 开始同步指定日期的统计数据:', targetDate || '昨日')

    const response = await statsServiceV1.syncDate(targetDate)

    if (response.code === 200) {
      const displayDate = targetDate || '昨日'
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: `${displayDate}的统计数据已成功同步到数据库`,
        details: response.data || `同步时间: ${new Date().toLocaleString()}`,
      }

      showSnackbar?.(`${displayDate}统计数据同步成功`, 'success')
      addOperationToHistory('指定日期数据同步', `同步${displayDate}的统计数据`, true)
      console.log('[SystemOperations] 指定日期统计数据同步成功:', response.data)
    } else {
      throw new Error(response.message || '同步失败')
    }
  } catch (error: any) {
    console.error('[SystemOperations] 指定日期统计数据同步失败:', error)

    const displayDate = syncDate.value || '昨日'
    syncResult.value = {
      type: 'error',
      title: '同步失败',
      message: `${displayDate}数据同步失败: ${error.message || '同步过程中发生错误'}`,
      details: `错误时间: ${new Date().toLocaleString()}`,
    }

    showSnackbar?.(`${displayDate}统计数据同步失败: ${error.message}`, 'error')
    addOperationToHistory('指定日期数据同步', `${displayDate}同步失败: ${error.message}`, false)
  } finally {
    syncingSpecific.value = false
  }
}

/**
 * 检查系统健康状态
 */
const checkSystemHealth = async (): Promise<void> => {
  checkingHealth.value = true
  healthResult.value = null

  try {
    console.log('[SystemOperations] 开始系统健康检查...')

    const response = await statsServiceV1.getHealth()

    if (response.code === 200) {
      healthResult.value = {
        type: 'success',
        title: '系统健康',
        message: '所有系统组件运行正常',
        details:
          typeof response.data === 'string'
            ? response.data
            : JSON.stringify(response.data, null, 2),
      }

      lastHealthCheck.value = new Date().toLocaleString()
      addOperationToHistory('系统健康检查', '系统运行状态正常', true)
      console.log('[SystemOperations] 系统健康检查成功:', response.data)
    } else {
      throw new Error(response.message || '健康检查失败')
    }
  } catch (error: any) {
    console.error('[SystemOperations] 系统健康检查失败:', error)

    healthResult.value = {
      type: 'warning',
      title: '健康检查异常',
      message: error.message || '健康检查过程中发生错误',
      details: `检查时间: ${new Date().toLocaleString()}`,
    }

    lastHealthCheck.value = new Date().toLocaleString()
    addOperationToHistory('系统健康检查', `检查异常: ${error.message}`, false)
  } finally {
    checkingHealth.value = false
  }
}

/**
 * 将节点加入到 AutoAuthor 队列
 */
const enqueueNode = async (): Promise<void> => {
  if (!nodeId.value) {
    showSnackbar?.('请输入节点ID', 'error')
    return
  }

  enqueuingNode.value = true

  try {
    const nodeIdNumber = parseInt(nodeId.value, 10)
    const response = await adminAutoAuthorServiceV1.enqueue(nodeIdNumber)

    if (response.code === 200) {
      showSnackbar?.(response.message || '操作成功', 'success')
      addOperationToHistory('AutoAuthor队列', `节点 ${nodeIdNumber} 加入创作队列`, true)
      // 清空输入框
      nodeId.value = ''
    } else {
      throw new Error(response.message || '加入队列失败')
    }
  } catch (error: any) {
    showSnackbar?.(`节点加入队列失败: ${error.message}`, 'error')
    addOperationToHistory('AutoAuthor队列', `节点 ${nodeId.value} 加入队列失败: ${error.message}`, false)
  } finally {
    enqueuingNode.value = false
  }
}

/**
 * 扫描节点并批量加入队列
 */
const scanNodes = async (): Promise<void> => {
  scanningNodes.value = true

  try {
    const response = await adminAutoAuthorServiceV1.scan()

    if (response.code === 200) {
      showSnackbar?.('扫描已开始', 'success')
      addOperationToHistory('AutoAuthor扫描', '开始扫描缺少AI内容的节点', true)
    } else {
      throw new Error(response.message || '启动扫描失败')
    }
  } catch (error: any) {
    showSnackbar?.(`启动扫描失败: ${error.message}`, 'error')
    addOperationToHistory('AutoAuthor扫描', `扫描失败: ${error.message}`, false)
  } finally {
    scanningNodes.value = false
  }
}

/**
 * 重置 opencode 会话
 */
const resetSession = async (): Promise<void> => {
  resettingSession.value = true

  try {
    const response = await adminAutoAuthorServiceV1.resetSession()

    if (response.code === 200) {
      showSnackbar?.('会话已重置', 'success')
      addOperationToHistory('AutoAuthor会话', '重置opencode会话成功', true)
    } else {
      throw new Error(response.message || '重置会话失败')
    }
  } catch (error: any) {
    showSnackbar?.(`重置会话失败: ${error.message}`, 'error')
    addOperationToHistory('AutoAuthor会话', `重置失败: ${error.message}`, false)
  } finally {
    resettingSession.value = false
  }
}

/**
 * 确认清空队列
 */
const confirmClearQueue = (): void => {
  if (confirm('确定要清空所有AutoAuthor队列吗？此操作不可撤销。')) {
    clearQueue()
  }
}

/**
 * 清空 AutoAuthor 队列
 */
const clearQueue = async (): Promise<void> => {
  clearingQueue.value = true

  try {
    const response = await adminAutoAuthorServiceV1.clearQueue()

    if (response.code === 200) {
      showSnackbar?.(response.message || '队列已清空', 'success')
      addOperationToHistory('AutoAuthor队列', '清空所有待处理队列', true)
    } else {
      throw new Error(response.message || '清空队列失败')
    }
  } catch (error: any) {
    showSnackbar?.(`清空队列失败: ${error.message}`, 'error')
    addOperationToHistory('AutoAuthor队列', `清空队列失败: ${error.message}`, false)
  } finally {
    clearingQueue.value = false
  }
}
</script>

<style scoped>
  .system-operations {
    padding: 0;
  }

  /* 日期输入框样式 */
  .date-input {
    min-width: 40px;
    max-width: 180px;
  }

  /* 节点ID输入框样式 */
  .node-id-input {
    min-width: 80px;
    max-width: 280px;
    flex-shrink: 1;
  }

  /* 同步按钮样式 */
  .sync-button {
    height: 40px;
  }

  /* 队列按钮样式 */
  .enqueue-button,
  .scan-button,
  .reset-button,
  .clear-button {
    height: 40px;
  }

  /* 卡片边框样式 */
  :deep(.v-card[outlined]) {
    border: 1px solid rgba(0, 0, 0, 0.08) !important;
  }

  /* 时间线样式优化 */
  :deep(.v-timeline-item__body) {
    padding-left: 16px;
  }

  /* 预格式化文本样式 */
  pre {
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.4;
    white-space: pre-wrap;
    word-wrap: break-word;
    margin: 0;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    :deep(.d-flex.gap-3) {
      flex-direction: column;
      gap: 12px !important;
    }

    :deep(.d-flex.gap-3 .v-text-field) {
      max-width: 100% !important;
      min-width: 100% !important;
    }

    :deep(.d-flex.gap-3 .v-btn) {
      width: 100%;
    }
  }
</style>