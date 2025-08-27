<template>
  <div class="system-operations">
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-purple-lighten-5 mr-3">
          <v-icon icon="mdi-cog-sync" color="purple-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">{{ t('admin.systemOperations.title') }}</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.systemOperations.subtitle') }}</p>
        </div>
      </div>
      <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
        <v-icon icon="mdi-tools" color="purple-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-purple-darken-2 text-caption">{{ t('systemOperations.adminTools') }}</span>
      </v-chip>
    </div>

    <!-- Redis 统计数据同步 -->
    <v-card flat class="pa-4 mb-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-database-sync" color="red-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">{{ t('systemOperations.redisSync.title') }}</h4>
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
                <h5 class="text-subtitle-1 font-weight-bold text-orange-darken-2">{{ t('systemOperations.redisSync.manualSync') }}</h5>
              </div>
              <p class="text-body-2 text-grey-darken-1 mb-3">
                {{ t('systemOperations.redisSync.manualSyncDesc') }}
              </p>
              <v-btn 
                variant="flat" 
                color="orange-darken-1" 
                rounded="lg"
                size="small"
                @click="syncStatsManual"
                :loading="syncingManual"
                :disabled="syncingManual || syncingSpecific"
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
                <v-icon icon="mdi-calendar-sync" color="blue-darken-2" size="20" class="mr-2"></v-icon>
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
                  style="min-width: 40px; max-width: 180px;"
                  placeholder="选择日期"
                ></v-text-field>
                <v-btn 
                  variant="flat" 
                  color="blue-darken-1" 
                  rounded="lg"
                  density="compact"
                  @click="syncStatsSpecificDate"
                  :loading="syncingSpecific"
                  :disabled="syncingManual || syncingSpecific"
                  style="height: 40px;"
                  class="ml-3"
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
            @click="checkSystemHealth"
            :loading="checkingHealth"
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
                <v-icon icon="mdi-delete-sweep" color="indigo-darken-2" size="24" class="mb-2"></v-icon>
                <h6 class="text-subtitle-2 font-weight-bold text-indigo-darken-2 mb-2">清理过期缓存</h6>
                <v-btn 
                  variant="flat" 
                  color="indigo-darken-1" 
                  rounded="lg"
                  size="x-small"
                  disabled
                >
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
                <v-btn 
                  variant="flat" 
                  color="indigo-darken-1" 
                  rounded="lg"
                  size="x-small"
                  disabled
                >
                  <v-icon icon="mdi-restart" size="14" class="mr-1"></v-icon>
                  重置
                </v-btn>
              </div>
            </v-card>
          </v-col>
          
          <v-col cols="12" md="4">
            <v-card flat class="pa-3 bg-indigo-lighten-5" rounded="lg" elevation="0">
              <div class="text-center">
                <v-icon icon="mdi-information" color="indigo-darken-2" size="24" class="mb-2"></v-icon>
                <h6 class="text-subtitle-2 font-weight-bold text-indigo-darken-2 mb-2">缓存信息</h6>
                <v-btn 
                  variant="flat" 
                  color="indigo-darken-1" 
                  rounded="lg"
                  size="x-small"
                  disabled
                >
                  <v-icon icon="mdi-eye" size="14" class="mr-1"></v-icon>
                  查看
                </v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>
        
        <v-alert
          type="info"
          variant="tonal"
          class="mt-3"
          rounded="lg"
        >
          <div class="font-weight-bold">功能开发中</div>
          <div class="text-body-2 mt-1">缓存管理功能正在开发中，敬请期待...</div>
        </v-alert>
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
            
            <v-card flat class="pa-3" rounded="lg" :color="operation.success ? 'success-lighten-5' : 'error-lighten-5'">
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

<script setup>
import { ref, inject } from 'vue';
import { learnService } from '@/services/learnService';
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 响应式数据
const syncDate = ref('');
const syncingManual = ref(false);
const syncingSpecific = ref(false);
const syncResult = ref(null);

const checkingHealth = ref(false);
const healthResult = ref(null);
const lastHealthCheck = ref('');

const operationHistory = ref([]);

const showSnackbar = inject('showSnackbar');

/**
 * 添加操作记录到历史
 */
const addOperationToHistory = (title, description, success = true) => {
  const operation = {
    title,
    description,
    success,
    time: new Date().toLocaleString()
  };
  
  operationHistory.value.unshift(operation);
  
  // 只保留最近20条记录
  if (operationHistory.value.length > 20) {
    operationHistory.value = operationHistory.value.slice(0, 20);
  }
};

/**
 * 手动同步Redis统计数据到数据库
 * 触发后端的手动同步接口，同步所有Redis中的统计数据
 */
const syncStatsManual = async () => {
  syncingManual.value = true;
  syncResult.value = null;
  
  try {
    console.log('[SystemOperations] 开始手动同步Redis统计数据...');
    
    const response = await learnService.syncStatsManual();
    
    if (response.code === 200) {
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: 'Redis统计数据已成功同步到数据库',
        details: `同步时间: ${new Date().toLocaleString()}`
      };
      
      showSnackbar('统计数据同步成功', 'success');
      addOperationToHistory('Redis数据同步', '手动同步Redis统计数据到数据库', true);
      console.log('[SystemOperations] Redis统计数据同步成功:', response.data);
    } else {
      throw new Error(response.msg || '同步失败');
    }
  } catch (error) {
    console.error('[SystemOperations] Redis统计数据同步失败:', error);
    
    syncResult.value = {
      type: 'error',
      title: '同步失败',
      message: error.message || '同步过程中发生错误',
      details: `错误时间: ${new Date().toLocaleString()}`
    };
    
    showSnackbar('统计数据同步失败: ' + error.message, 'error');
    addOperationToHistory('Redis数据同步', `同步失败: ${error.message}`, false);
  } finally {
    syncingManual.value = false;
  }
};

/**
 * 同步指定日期的Redis统计数据到数据库
 * 可以指定具体日期，如果不指定则同步昨日数据
 */
const syncStatsSpecificDate = async () => {
  syncingSpecific.value = true;
  syncResult.value = null;
  
  try {
    const targetDate = syncDate.value || null;
    console.log('[SystemOperations] 开始同步指定日期的统计数据:', targetDate || '昨日');
    
    const response = await learnService.syncStatsSpecificDate(targetDate);
    
    if (response.code === 200) {
      const displayDate = targetDate || '昨日';
      syncResult.value = {
        type: 'success',
        title: '同步成功',
        message: `${displayDate}的统计数据已成功同步到数据库`,
        details: response.data || `同步时间: ${new Date().toLocaleString()}`
      };
      
      showSnackbar(`${displayDate}统计数据同步成功`, 'success');
      addOperationToHistory('指定日期数据同步', `同步${displayDate}的统计数据`, true);
      console.log('[SystemOperations] 指定日期统计数据同步成功:', response.data);
    } else {
      throw new Error(response.msg || '同步失败');
    }
  } catch (error) {
    console.error('[SystemOperations] 指定日期统计数据同步失败:', error);
    
    const displayDate = syncDate.value || '昨日';
    syncResult.value = {
      type: 'error',
      title: '同步失败',
      message: `${displayDate}数据同步失败: ${error.message || '同步过程中发生错误'}`,
      details: `错误时间: ${new Date().toLocaleString()}`
    };
    
    showSnackbar(`${displayDate}统计数据同步失败: ` + error.message, 'error');
    addOperationToHistory('指定日期数据同步', `${displayDate}同步失败: ${error.message}`, false);
  } finally {
    syncingSpecific.value = false;
  }
};

/**
 * 检查系统健康状态
 */
const checkSystemHealth = async () => {
  checkingHealth.value = true;
  healthResult.value = null;
  
  try {
    console.log('[SystemOperations] 开始系统健康检查...');
    
    const response = await learnService.getStatsHealth();
    
    if (response.code === 200) {
      healthResult.value = {
        type: 'success',
        title: '系统健康',
        message: '所有系统组件运行正常',
        details: typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2)
      };
      
      lastHealthCheck.value = new Date().toLocaleString();
      addOperationToHistory('系统健康检查', '系统运行状态正常', true);
      console.log('[SystemOperations] 系统健康检查成功:', response.data);
    } else {
      throw new Error(response.msg || '健康检查失败');
    }
  } catch (error) {
    console.error('[SystemOperations] 系统健康检查失败:', error);
    
    healthResult.value = {
      type: 'warning',
      title: '健康检查异常',
      message: error.message || '健康检查过程中发生错误',
      details: `检查时间: ${new Date().toLocaleString()}`
    };
    
    lastHealthCheck.value = new Date().toLocaleString();
    addOperationToHistory('系统健康检查', `检查异常: ${error.message}`, false);
  } finally {
    checkingHealth.value = false;
  }
};
</script>

<style scoped>
.system-operations {
  padding: 0;
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