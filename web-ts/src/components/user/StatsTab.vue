<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { statsServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useFetch } from '@/composables/useFetch'
import TrendChart from '@/components/ranking/TrendChart.vue'
import type { DailyStats } from '@/types/stats'

interface StatsData {
  totalViews: number
  totalTwice: number
  totalHelpful: number
  totalComments: number
  dailyStats?: DailyStats[]
}

interface TableHeader {
  title: string
  key: string
  align?: 'start' | 'center' | 'end'
  width?: string
}

const userStore = useUserStore()
const { t } = useI18n()

// 响应式数据
const selectedPeriod: Ref<string> = ref('7') // 默认选择7天

// 使用 useFetch 加载全部时间统计数据
const {
  data: totalStatsData,
  loading: totalStatsLoading,
  error: totalStatsError,
  refresh: refreshTotalStats
} = useFetch<StatsData>({
  fetchFn: () => {
    if (!userStore.currentUser?.id) {
      throw new Error(t('userStats.userInfoFailed'))
    }
    return statsServiceV1.getUserAllTimeStats(userStore.currentUser.id)
  },
  immediate: true,
  onError: (error) => {
    console.error('Error loading total stats:', error)
  }
})

// 获取时间段统计数据的函数
const getPeriodStatsApi = () => {
  if (!userStore.currentUser?.id) {
    throw new Error(t('userStats.userInfoFailed'))
  }

  const userId = userStore.currentUser.id

  switch (selectedPeriod.value) {
    case 'today':
      return statsServiceV1.getUserTodayStats(userId)
    case 'yesterday':
      return statsServiceV1.getUserYesterdayStats(userId)
    default: {
      const days = parseInt(selectedPeriod.value)
      return statsServiceV1.getUserPeriodStats(userId, days)
    }
  }
}

// 使用 useFetch 加载时间段统计数据
const {
  data: statsData,
  loading,
  error,
  execute: loadStatsData,
  refresh: refreshStatsData
} = useFetch<StatsData>({
  fetchFn: getPeriodStatsApi,
  immediate: true,
  onError: (err) => {
    console.error('Error loading stats:', err)
  }
})

// 计算属性
const showTrendChart = computed(() => {
  return ['7', '30', '180', '365'].includes(selectedPeriod.value)
})

const showDailyDetails = computed(() => {
  return (
    statsData.value && statsData.value.dailyStats && statsData.value.dailyStats.length > 0
  )
})

const dailyStatsItems = computed(() => {
  if (!statsData.value || !statsData.value.dailyStats) return []

  return statsData.value.dailyStats.map((day) => ({
    date: day.date,
    views: day.views || 0,
    twice: day.twice || 0,
    helpful: day.helpful || 0,
    comments: day.comments || 0,
  }))
})

// 表格头部配置
const tableHeaders = computed((): TableHeader[] => [
  { title: t('userStats.date'), key: 'date', align: 'start', width: '120px' },
  { title: t('userStats.views'), key: 'views', align: 'center', width: '100px' },
  { title: t('userStats.maxTwice'), key: 'twice', align: 'center', width: '100px' },
  { title: t('userStats.helpful'), key: 'helpful', align: 'center', width: '100px' },
  { title: t('userStats.commentsCount'), key: 'comments', align: 'center', width: '100px' },
])

// 工具方法
const formatNumber = (num: number | null | undefined): string => {
  if (num === null || isNaN(Number(num))) return '--'
  const number = Number(num)
  if (number >= 10000) {
    return `${(number / 1000).toFixed(1)}k`
  }
  return number.toLocaleString()
}

const formatDate = (dateString: string): string => {
  try {
    const date = new Date(dateString)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)

    if (date.toDateString() === today.toDateString()) {
      return t('userStats.today')
    } else if (date.toDateString() === yesterday.toDateString()) {
      return t('userStats.yesterday')
    } else {
      return date.toLocaleDateString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
      })
    }
  } catch (e) {
    console.error('Error formatting date:', e)
    return dateString
  }
}

const getPeriodText = (): string => {
  switch (selectedPeriod.value) {
    case 'today':
      return t('userStats.today_')
    case 'yesterday':
      return t('userStats.yesterday_')
    case '7':
      return t('userStats.recent7Days')
    case '15':
      return t('userStats.recent15Days')
    case '30':
      return t('userStats.recent30Days')
    case '365':
      return t('userStats.recentYear')
    default:
      return t('userStats.custom')
  }
}

// 数据加载方法
const onPeriodChange = (): void => {
  loadStatsData()
}

const refreshData = (): void => {
  loadStatsData()
  refreshTotalStats()
}

// 监听用户状态变化
watch(
  () => userStore.currentUser?.id,
  (newUserId) => {
    if (newUserId) {
      loadStatsData()
      refreshTotalStats()
    }
  }
)
</script>

<template>
  <div class="user-stats-container">
    <!-- 提示信息 -->
    <div class="mb-5 text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">查看您的学习和内容创作统计数据</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header mb-5">
      <div class="d-flex justify-space-between align-center">
        <div>
          <h2 class="page-title mb-2">{{ t('userStats.title') }}</h2>
          <p class="page-subtitle">{{ t('userStats.subtitle') }}</p>
        </div>
        <v-btn
          color="primary"
          variant="tonal"
          :loading="loading || totalStatsLoading"
          icon
          @click="refreshData"
        >
          <v-icon>mdi-refresh</v-icon>
          <v-tooltip activator="parent" location="bottom">
            {{ t('userStats.refreshData') }}
          </v-tooltip>
        </v-btn>
      </div>
    </div>

    <!-- 全部时间统计概览 -->
    <div class="total-stats-overview mb-6">
      <div class="flat-card pa-4">
        <div class="d-flex align-center mb-3">
          <v-icon color="primary" size="20" class="mr-2">mdi-chart-timeline-variant</v-icon>
          <h3 class="text-subtitle-1 text-primary font-weight-bold">
            {{ t('userStats.allTimeStats') }}
          </h3>
          <v-spacer></v-spacer>
          <v-chip color="primary" variant="tonal" size="small">
            {{ t('userStats.realtimeData') }}
          </v-chip>
        </div>

        <div v-if="totalStatsLoading" class="text-center py-6">
          <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
          <p class="text-body-2 text-grey mt-3 mb-0">{{ t('userStats.loadingTotalData') }}</p>
        </div>

        <v-row v-else-if="totalStatsData" class="mt-1">
          <v-col cols="6" sm="3">
            <div class="text-center py-3">
              <v-icon icon="mdi-eye" color="grey-darken-1" size="20" class="mb-1"></v-icon>
              <h4 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                {{ formatNumber(totalStatsData.totalViews) }}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.totalViews') }}</p>
            </div>
          </v-col>
          <v-col cols="6" sm="3">
            <div class="text-center py-3">
              <v-icon icon="mdi-thumb-up" color="grey-darken-1" size="20" class="mb-1"></v-icon>
              <h4 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                {{ formatNumber(totalStatsData.totalTwice) }}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.totalTwice') }}</p>
            </div>
          </v-col>
          <v-col cols="6" sm="3">
            <div class="text-center py-3">
              <v-icon icon="mdi-heart" color="grey-darken-1" size="20" class="mb-1"></v-icon>
              <h4 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                {{ formatNumber(totalStatsData.totalHelpful) }}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.totalHelpful') }}</p>
            </div>
          </v-col>
          <v-col cols="6" sm="3">
            <div class="text-center py-3">
              <v-icon icon="mdi-comment" color="grey-darken-1" size="20" class="mb-1"></v-icon>
              <h4 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                {{ formatNumber(totalStatsData.totalComments) }}
              </h4>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.totalComments') }}</p>
            </div>
          </v-col>
        </v-row>

        <div v-else-if="totalStatsError" class="text-center py-3">
          <v-icon color="warning" size="20" class="mr-1">mdi-alert-circle</v-icon>
          <span class="text-body-2 text-warning">{{ totalStatsError.message }}</span>
        </div>
      </div>
    </div>

    <!-- 分隔线和提示 -->
    <div class="section-divider mb-6">
      <div class="text-center">
        <v-chip color="blue-grey-lighten-2" variant="tonal" size="default" class="px-4 py-2 mt-4">
          <v-icon icon="mdi-calendar-range" size="16" class="mr-2"></v-icon>
          <span class="font-weight-bold">{{ t('userStats.periodStats') }}</span>
        </v-chip>
      </div>
    </div>

    <!-- 时间选择器和数据概览并排布局 -->
    <div class="period-stats-section">
      <!-- 时间选择器和概览标题 -->
      <div class="d-flex justify-space-between align-center mb-4">
        <!-- 左侧：时间选择器 -->
        <div class="time-selector-container">
          <div class="text-body-2 text-grey-darken-2 mb-2">
            {{ t('userStats.selectTimeRange') }}
          </div>
          <v-btn-toggle
            v-model="selectedPeriod"
            color="primary"
            rounded="lg"
            mandatory
            class="flat-btn-toggle"
            density="comfortable"
            @update:model-value="onPeriodChange"
          >
            <v-btn value="7" size="small" variant="outlined">
              <v-icon class="mr-1" size="16">mdi-calendar-week</v-icon>
              {{ t('userStats.days7') }}
            </v-btn>
            <v-btn value="30" size="small" variant="outlined">
              <v-icon class="mr-1" size="16">mdi-calendar-month</v-icon>
              {{ t('userStats.days30') }}
            </v-btn>
            <v-btn value="180" size="small" variant="outlined">
              <v-icon class="mr-1" size="16">mdi-calendar-month-outline</v-icon>
              {{ t('userStats.halfYear') }}
            </v-btn>
            <v-btn value="365" size="small" variant="outlined">
              <v-icon class="mr-1" size="16">mdi-calendar</v-icon>
              {{ t('userStats.oneYear') }}
            </v-btn>
          </v-btn-toggle>
        </div>

        <!-- 右侧：数据概览标题 -->
        <div class="overview-title-container text-right">
          <h3 class="section-title mb-0">{{ getPeriodText() }}{{ t('userStats.dataOverview') }}</h3>
          <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.viewDetailedData') }}</p>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <v-progress-circular
          indeterminate
          color="primary"
          size="48"
          class="mb-3"
        ></v-progress-circular>
        <p class="loading-text">{{ t('userStats.loadingData', { period: getPeriodText() }) }}</p>
      </div>

      <!-- 错误状态 -->
      <v-alert
        v-if="error && !loading"
        type="warning"
        variant="tonal"
        class="mb-6"
        closable
        @click:close="error = null"
      >
        <template #prepend>
          <v-icon>mdi-alert-circle</v-icon>
        </template>
        <div class="alert-content">
          <div class="alert-title">{{ t('userStats.dataLoadFailed') }}</div>
          <div class="alert-text">{{ error.message }}</div>
          <v-btn size="small" variant="outlined" color="warning" class="mt-2" @click="refreshData">
            {{ t('userStats.retry') }}
          </v-btn>
        </div>
      </v-alert>

      <!-- 统计数据展示 -->
      <div v-if="!loading && !error && statsData" class="stats-content">
        <!-- 概览卡片 -->
        <div class="overview-cards mb-5">
          <v-row>
            <v-col cols="12" sm="6" md="3">
              <div class="flat-stats-card">
                <div class="text-center pa-3">
                  <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                    {{ formatNumber(statsData.totalViews) }}
                  </h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">
                    {{ t('userStats.articleViews') }}
                  </p>
                </div>
              </div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="flat-stats-card">
                <div class="text-center pa-3">
                  <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                    {{ formatNumber(statsData.totalTwice) }}
                  </h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">
                    {{ t('userStats.maxTwiceLikes') }}
                  </p>
                </div>
              </div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="flat-stats-card">
                <div class="text-center pa-3">
                  <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                    {{ formatNumber(statsData.totalHelpful) }}
                  </h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">
                    {{ t('userStats.helpfulLikes') }}
                  </p>
                </div>
              </div>
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <div class="flat-stats-card">
                <div class="text-center pa-3">
                  <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-1">
                    {{ formatNumber(statsData.totalComments) }}
                  </h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('userStats.comments') }}</p>
                </div>
              </div>
            </v-col>
          </v-row>
        </div>

        <!-- 趋势图表 -->
        <div v-if="showTrendChart" class="trend-chart-container mb-5">
          <div class="flat-card pa-4">
            <div class="text-subtitle-1 d-flex align-center mb-3">
              <v-icon class="mr-2" color="primary" size="20">mdi-chart-line-variant</v-icon>
              {{ getPeriodText() }}{{ t('userStats.dataTrend') }}
            </div>
            <div class="pa-1">
              <TrendChart :daily-stats="statsData?.dailyStats || []" :period="selectedPeriod" />
            </div>
          </div>
        </div>

        <!-- 每日明细表格 -->
        <div v-if="showDailyDetails" class="daily-details-container">
          <div class="flat-card pa-4">
            <div class="text-subtitle-1 d-flex align-center mb-3">
              <v-icon class="mr-2" color="primary" size="20">mdi-calendar-multiple</v-icon>
              {{ t('userStats.dailyDetails') }}
            </div>
            <div class="pa-1">
              <v-data-table
                :headers="tableHeaders"
                :items="dailyStatsItems"
                :loading="loading"
                item-value="date"
                class="flat-table"
                :items-per-page="15"
                :sort-by="[{ key: 'date', order: 'desc' }]"
              >
                <template #[`item.date`]="{ item }">
                  <span class="date-cell">
                    {{ formatDate(item.date) }}
                  </span>
                </template>

                <template #[`item.views`]="{ item }">
                  <v-chip
                    size="small"
                    :color="item.views > 0 ? 'grey-darken-1' : 'grey-lighten-3'"
                    variant="tonal"
                  >
                    {{ item.views }}
                  </v-chip>
                </template>

                <template #[`item.twice`]="{ item }">
                  <v-chip
                    size="small"
                    :color="item.twice > 0 ? 'grey-darken-1' : 'grey-lighten-3'"
                    variant="tonal"
                  >
                    {{ item.twice }}
                  </v-chip>
                </template>

                <template #[`item.helpful`]="{ item }">
                  <v-chip
                    size="small"
                    :color="item.helpful > 0 ? 'grey-darken-1' : 'grey-lighten-3'"
                    variant="tonal"
                  >
                    {{ item.helpful }}
                  </v-chip>
                </template>

                <template #[`item.comments`]="{ item }">
                  <v-chip
                    size="small"
                    :color="item.comments > 0 ? 'grey-darken-1' : 'grey-lighten-3'"
                    variant="tonal"
                  >
                    {{ item.comments }}
                  </v-chip>
                </template>
                <template #no-data>
                  <div class="no-data-placeholder">
                    <v-icon size="48" color="grey-lighten-1">mdi-calendar-blank</v-icon>
                    <p>{{ t('userStats.noData') }}</p>
                  </div>
                </template>
              </v-data-table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && !error && !statsData" class="empty-state">
      <v-icon size="80" color="grey-lighten-1">mdi-chart-pie</v-icon>
      <h3 class="empty-title">{{ t('userStats.noStatsData') }}</h3>
      <p class="empty-subtitle">{{ t('userStats.noStatsDesc') }}</p>
      <v-btn color="primary" variant="outlined" rounded="lg" @click="refreshData">
        {{ t('userStats.refreshData') }}
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.user-stats-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: left;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1976d2;
}

.page-subtitle {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1976d2;
  text-align: left;
}

.time-range-selector {
  display: flex;
  justify-content: center;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.loading-text {
  color: #666;
  font-size: 1rem;
  margin: 0;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-title {
  font-weight: 600;
  font-size: 1rem;
}

.alert-text {
  color: #666;
}

.stats-content {
  animation: fadeIn 0.5s ease-in;
}

.stats-card {
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.stats-card:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 0, 0, 0.12);
}

/* Flat风格统计卡片 */
.flat-stats-card {
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.flat-stats-card:hover {
  border-color: #bdbdbd;
  transform: translateY(-1px);
}

/* Flat风格卡片 */
.flat-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.flat-card:hover {
  border-color: #bdbdbd;
}

/* Flat风格按钮组 */
.flat-btn-toggle {
  border: 1px solid #e0e0e0 !important;
  box-shadow: none !important;
}

/* Flat风格表格 */
.flat-table {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

/* 分隔线样式 */
.section-divider {
  position: relative;
}

/* 时间选择器容器 */
.time-selector-container {
  min-width: 350px;
}

/* 概览标题容器 */
.overview-title-container {
  max-width: 280px;
}

/* 时间段统计区域 */
.period-stats-section {
  background: rgba(245, 245, 245, 0.6);
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  padding: 20px;
  width: 100%;
  margin: 0;
}

.date-cell {
  font-weight: 500;
  color: #1976d2;
}

.no-data-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: #666;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  text-align: center;
}

.empty-title {
  font-size: 1.25rem;
  color: #666;
  margin: 16px 0 8px 0;
}

.empty-subtitle {
  color: #999;
  margin: 0 0 24px 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-stats-container {
    padding: 16px;
  }

  .page-title {
    font-size: 1.5rem;
  }

  .time-range-selector .v-btn-toggle {
    flex-wrap: wrap;
    gap: 8px;
  }
}
</style>