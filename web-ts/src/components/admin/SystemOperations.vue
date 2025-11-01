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

    <!-- 只读模式控制 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-shield-lock-outline" color="deep-orange-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">只读模式</h4>
      </div>

      <div class="mb-4">
        <p class="text-body-2 text-grey-darken-1 mb-4">
          开启只读模式后，系统将禁止所有写操作（创建、修改、删除），适用于系统维护、数据迁移等场景。登录、注册等基础功能不受影响。
        </p>

        <v-card
          flat
          class="pa-4"
          rounded="lg"
          elevation="0"
          :color="readonlyModeEnabled ? 'red-lighten-5' : 'green-lighten-5'"
        >
          <div class="d-flex align-center justify-space-between">
            <div>
              <div class="d-flex align-center mb-2">
                <v-icon
                  :icon="readonlyModeEnabled ? 'mdi-lock' : 'mdi-lock-open'"
                  :color="readonlyModeEnabled ? 'red-darken-2' : 'green-darken-2'"
                  size="24"
                  class="mr-2"
                ></v-icon>
                <h5
                  class="text-subtitle-1 font-weight-bold"
                  :class="readonlyModeEnabled ? 'text-red-darken-2' : 'text-green-darken-2'"
                >
                  当前状态
                </h5>
              </div>
              <p class="text-h6 font-weight-bold" :class="readonlyModeEnabled ? 'text-red-darken-2' : 'text-green-darken-2'">
                {{ readonlyModeEnabled ? '只读模式已开启' : '正常运行中' }}
              </p>
              <p class="text-caption text-grey-darken-1">
                {{ readonlyModeEnabled ? '所有写操作已被禁止' : '系统可正常读写' }}
              </p>
            </div>
            <v-switch
              v-model="readonlyModeEnabled"
              color="red-darken-1"
              :loading="togglingReadonlyMode"
              :disabled="togglingReadonlyMode"
              hide-details
              @update:model-value="toggleReadonlyMode"
            ></v-switch>
          </div>
        </v-card>
      </div>
    </v-card>

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

    <!-- 最近的系统操作 -->
    <v-card flat class="pa-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-history" color="grey-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">最近的系统操作</h4>
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
import { statsServiceV1 } from '@/services/api/v1/apiServiceV1'
import { adminAutoAuthorServiceV1, adminSystemServiceV1 } from '@/services/api/v1/adminApiServiceV1'
import { useI18n } from 'vue-i18n'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

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

// 操作历史记录
const operationHistory = ref<OperationRecord[]>([])

// 同步相关
const syncDate = ref<string>('')
const syncResult = ref<SyncResult | null>(null)

// 健康检查相关
const healthResult = ref<HealthResult | null>(null)
const lastHealthCheck = ref<string>('')

// AutoAuthor 相关
const nodeId = ref<string>('')

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

// 使用 useFetch 加载只读模式状态
const {
  data: readonlyModeData,
  refresh: loadReadonlyMode
} = useFetch({
  fetchFn: async () => {
    const response = await adminSystemServiceV1.getReadonlyMode()
    if (response.code === 200 && response.data) {
      return response.data
    }
    return null
  },
  immediate: true,
  onError: (error) => {
    console.error('[SystemOperations] 加载只读模式状态失败:', error)
  }
})

// 响应式只读模式开关
const readonlyModeEnabled = ref<boolean>(false)

// 监听数据变化更新 switch 状态
const updateReadonlyMode = () => {
  if (readonlyModeData.value) {
    readonlyModeEnabled.value = readonlyModeData.value.enabled
  }
}

// 切换只读模式
const { execute: toggleReadonlyModeExecute, loading: togglingReadonlyMode } = useMutation(
  (enabled: boolean) => adminSystemServiceV1.setReadonlyMode(enabled),
  {
    successMessage: '',
    showToast: false,
    onSuccess: (result, enabled) => {
      showSnackbar?.(enabled ? '只读模式已开启' : '只读模式已关闭', 'success')
      addOperationToHistory(
        '只读模式',
        enabled ? '开启系统只读模式' : '关闭系统只读模式',
        true
      )
      readonlyModeEnabled.value = enabled
      console.log('[SystemOperations] 只读模式切换成功')
    },
    onError: (error) => {
      console.error('[SystemOperations] 切换只读模式失败:', error)
      // 恢复原状态
      readonlyModeEnabled.value = !readonlyModeEnabled.value
      addOperationToHistory('只读模式', `切换失败: ${error.message}`, false)
    }
  }
)

const toggleReadonlyMode = (enabled: boolean) => {
  toggleReadonlyModeExecute(enabled)
}

// 手动同步Redis统计数据
const { execute: syncStatsManual, loading: syncingManual } = useMutation(
  statsServiceV1.syncManual,
  {
    showToast: false,
    onSuccess: (result) => {
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: 'Redis统计数据已成功同步到数据库',
        details: `同步时间: ${new Date().toLocaleString()}`,
      }
      showSnackbar?.('统计数据同步成功', 'success')
      addOperationToHistory('Redis数据同步', '手动同步Redis统计数据到数据库', true)
      console.log('[SystemOperations] Redis统计数据同步成功:', result)
    },
    onError: (error) => {
      console.error('[SystemOperations] Redis统计数据同步失败:', error)
      syncResult.value = {
        type: 'error',
        title: '同步失败',
        message: error.message || '同步过程中发生错误',
        details: `错误时间: ${new Date().toLocaleString()}`,
      }
      addOperationToHistory('Redis数据同步', `同步失败: ${error.message}`, false)
    }
  }
)

// 同步指定日期的Redis统计数据
const { execute: syncStatsSpecificDateExecute, loading: syncingSpecific } = useMutation(
  (targetDate: string | null) => statsServiceV1.syncDate(targetDate),
  {
    showToast: false,
    onSuccess: (result, targetDate) => {
      const displayDate = targetDate || '昨日'
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: `${displayDate}的统计数据已成功同步到数据库`,
        details: result || `同步时间: ${new Date().toLocaleString()}`,
      }
      showSnackbar?.(`${displayDate}统计数据同步成功`, 'success')
      addOperationToHistory('指定日期数据同步', `同步${displayDate}的统计数据`, true)
      console.log('[SystemOperations] 指定日期统计数据同步成功:', result)
    },
    onError: (error) => {
      console.error('[SystemOperations] 指定日期统计数据同步失败:', error)
      const displayDate = syncDate.value || '昨日'
      syncResult.value = {
        type: 'error',
        title: '同步失败',
        message: `${displayDate}数据同步失败: ${error.message || '同步过程中发生错误'}`,
        details: `错误时间: ${new Date().toLocaleString()}`,
      }
      addOperationToHistory('指定日期数据同步', `${displayDate}同步失败: ${error.message}`, false)
    }
  }
)

const syncStatsSpecificDate = () => {
  const targetDate = syncDate.value || null
  console.log('[SystemOperations] 开始同步指定日期的统计数据:', targetDate || '昨日')
  syncStatsSpecificDateExecute(targetDate)
}

// 检查系统健康状态
const { execute: checkSystemHealth, loading: checkingHealth } = useMutation(
  statsServiceV1.getHealth,
  {
    showToast: false,
    onSuccess: (result) => {
      healthResult.value = {
        type: 'success',
        title: '系统健康',
        message: '所有系统组件运行正常',
        details:
          typeof result === 'string'
            ? result
            : JSON.stringify(result, null, 2),
      }
      lastHealthCheck.value = new Date().toLocaleString()
      addOperationToHistory('系统健康检查', '系统运行状态正常', true)
      console.log('[SystemOperations] 系统健康检查成功:', result)
    },
    onError: (error) => {
      console.error('[SystemOperations] 系统健康检查失败:', error)
      healthResult.value = {
        type: 'warning',
        title: '健康检查异常',
        message: error.message || '健康检查过程中发生错误',
        details: `检查时间: ${new Date().toLocaleString()}`,
      }
      lastHealthCheck.value = new Date().toLocaleString()
      addOperationToHistory('系统健康检查', `检查异常: ${error.message}`, false)
    }
  }
)

// 将节点加入到 AutoAuthor 队列
const { execute: enqueueNodeExecute, loading: enqueuingNode } = useMutation(
  (nodeIdNumber: number) => adminAutoAuthorServiceV1.enqueue(nodeIdNumber),
  {
    showToast: false,
    onSuccess: (result, nodeIdNumber) => {
      showSnackbar?.(result?.message || '操作成功', 'success')
      addOperationToHistory('AutoAuthor队列', `节点 ${nodeIdNumber} 加入创作队列`, true)
      nodeId.value = ''
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor队列', `节点 ${nodeId.value} 加入队列失败: ${error.message}`, false)
    }
  }
)

const enqueueNode = () => {
  if (!nodeId.value) {
    showSnackbar?.('请输入节点ID', 'error')
    return
  }
  const nodeIdNumber = parseInt(nodeId.value, 10)
  enqueueNodeExecute(nodeIdNumber)
}

// 扫描节点并批量加入队列
const { execute: scanNodes, loading: scanningNodes } = useMutation(
  adminAutoAuthorServiceV1.scan,
  {
    successMessage: '扫描已开始',
    onSuccess: () => {
      addOperationToHistory('AutoAuthor扫描', '开始扫描缺少AI内容的节点', true)
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor扫描', `扫描失败: ${error.message}`, false)
    }
  }
)

// 重置 opencode 会话
const { execute: resetSession, loading: resettingSession } = useMutation(
  adminAutoAuthorServiceV1.resetSession,
  {
    successMessage: '会话已重置',
    onSuccess: () => {
      addOperationToHistory('AutoAuthor会话', '重置opencode会话成功', true)
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor会话', `重置失败: ${error.message}`, false)
    }
  }
)

// 确认清空队列
const confirmClearQueue = (): void => {
  if (confirm('确定要清空所有AutoAuthor队列吗？此操作不可撤销。')) {
    clearQueueExecute()
  }
}

// 清空 AutoAuthor 队列
const { execute: clearQueueExecute, loading: clearingQueue } = useMutation(
  adminAutoAuthorServiceV1.clearQueue,
  {
    showToast: false,
    onSuccess: (result) => {
      showSnackbar?.(result?.message || '队列已清空', 'success')
      addOperationToHistory('AutoAuthor队列', '清空所有待处理队列', true)
    },
    onError: (error) => {
      addOperationToHistory('AutoAuthor队列', `清空队列失败: ${error.message}`, false)
    }
  }
)

// 监听只读模式数据变化
const watchReadonlyMode = () => {
  updateReadonlyMode()
}

// 初始化时更新只读模式状态
setTimeout(watchReadonlyMode, 100)
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