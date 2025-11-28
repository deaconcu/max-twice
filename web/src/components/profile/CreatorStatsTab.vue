<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" lg="2" class="d-none d-lg-block">
      <div class="sticky-sidebar">
        <div class="sidebar-content">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">创作统计</h4>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              查看您的内容创作数据表现
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="sidebar-metrics">
            <div class="d-flex align-center mb-3">
              <div class="metric-dot bg-blue-lighten-4"></div>
              <span class="text-body-2 text-grey-darken-2">阅读量统计</span>
            </div>
            <div class="d-flex align-center mb-3">
              <div class="metric-dot bg-amber-lighten-4"></div>
              <span class="text-body-2 text-grey-darken-2">两遍秒懂</span>
            </div>
            <div class="d-flex align-center mb-3">
              <div class="metric-dot bg-green-lighten-4"></div>
              <span class="text-body-2 text-grey-darken-2">有用统计</span>
            </div>
            <div class="d-flex align-center">
              <div class="metric-dot bg-purple-lighten-4"></div>
              <span class="text-body-2 text-grey-darken-2">评论统计</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" lg="10">
      <div class="creator-stats-container">
        <!-- 总览卡片 -->
        <v-card rounded="xl" class="overview-card mb-6" elevation="0">
          <v-card-text class="pa-6">
            <div class="d-flex align-center justify-space-between mb-6">
              <div>
                <h2 class="text-h5 font-weight-bold text-grey-darken-4 mb-1">创作数据总览</h2>
                <p class="text-body-2 text-grey-darken-1">全部时间统计</p>
              </div>
              <v-btn
                icon
                variant="text"
                size="small"
                :loading="totalStatsLoading"
                @click="refreshAllData"
              >
                <v-icon>mdi-refresh</v-icon>
              </v-btn>
            </div>

            <!-- 加载状态 -->
            <div v-if="totalStatsLoading" class="text-center py-12">
              <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
            </div>

            <!-- 错误状态 -->
            <v-alert v-else-if="totalStatsError" type="error" variant="tonal" class="mb-0">
              {{ totalStatsError }}
            </v-alert>

            <!-- 数据展示 -->
            <v-row v-else-if="totalStatsData" class="stats-grid">
              <v-col cols="6" md="3">
                <div class="stat-item">
                  <div class="stat-icon-wrapper bg-blue-lighten-5">
                    <v-icon icon="mdi-eye" color="blue-darken-2" size="28"></v-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-value text-grey-darken-4">
                      {{ formatNumber(totalStatsData.totalViews) }}
                    </div>
                    <div class="stat-label text-grey-darken-1">阅读量</div>
                  </div>
                </div>
              </v-col>

              <v-col cols="6" md="3">
                <div class="stat-item">
                  <div class="stat-icon-wrapper bg-amber-lighten-5">
                    <v-icon icon="mdi-lightbulb" color="amber-darken-2" size="28"></v-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-value text-grey-darken-4">
                      {{ formatNumber(totalStatsData.totalTwice) }}
                    </div>
                    <div class="stat-label text-grey-darken-1">两遍秒懂</div>
                  </div>
                </div>
              </v-col>

              <v-col cols="6" md="3">
                <div class="stat-item">
                  <div class="stat-icon-wrapper bg-green-lighten-5">
                    <v-icon icon="mdi-thumb-up" color="green-darken-2" size="28"></v-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-value text-grey-darken-4">
                      {{ formatNumber(totalStatsData.totalHelpful) }}
                    </div>
                    <div class="stat-label text-grey-darken-1">有用</div>
                  </div>
                </div>
              </v-col>

              <v-col cols="6" md="3">
                <div class="stat-item">
                  <div class="stat-icon-wrapper bg-purple-lighten-5">
                    <v-icon icon="mdi-comment" color="purple-darken-2" size="28"></v-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-value text-grey-darken-4">
                      {{ formatNumber(totalStatsData.totalComments) }}
                    </div>
                    <div class="stat-label text-grey-darken-1">评论</div>
                  </div>
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- 时间段统计 -->
        <v-card rounded="xl" class="period-card" elevation="0">
          <v-card-text class="pa-6">
            <div class="d-flex align-center justify-space-between mb-6 flex-wrap ga-3">
              <div>
                <h2 class="text-h5 font-weight-bold text-grey-darken-4 mb-1">时间段统计</h2>
                <p class="text-body-2 text-grey-darken-1">{{ getPeriodText() }}</p>
              </div>
              <v-btn-toggle
                v-model="selectedPeriod"
                color="primary"
                variant="outlined"
                rounded="lg"
                density="comfortable"
                mandatory
                @update:model-value="onPeriodChange"
              >
                <v-btn value="7" size="small">7天</v-btn>
                <v-btn value="30" size="small">30天</v-btn>
                <v-btn value="180" size="small">半年</v-btn>
                <v-btn value="365" size="small">一年</v-btn>
              </v-btn-toggle>
            </div>

            <!-- 加载状态 -->
            <div v-if="loading" class="text-center py-12">
              <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
              <p class="text-body-2 text-grey mt-4 mb-0">加载中...</p>
            </div>

            <!-- 错误状态 -->
            <v-alert v-else-if="error" type="warning" variant="tonal" class="mb-0">
              {{ error }}
              <template #append>
                <v-btn size="small" variant="text" color="warning" @click="onPeriodChange">
                  重试
                </v-btn>
              </template>
            </v-alert>

            <!-- 数据展示 -->
            <div v-else-if="statsData">
              <v-row class="mb-6">
                <v-col cols="6" md="3">
                  <div class="period-stat-card">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-eye" color="blue-darken-2" size="20" class="mr-2"></v-icon>
                      <span class="text-caption text-grey-darken-1">阅读量</span>
                    </div>
                    <div class="text-h4 font-weight-bold text-blue-darken-2">
                      {{ formatNumber(statsData.totalViews) }}
                    </div>
                  </div>
                </v-col>

                <v-col cols="6" md="3">
                  <div class="period-stat-card">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-lightbulb" color="amber-darken-2" size="20" class="mr-2"></v-icon>
                      <span class="text-caption text-grey-darken-1">两遍秒懂</span>
                    </div>
                    <div class="text-h4 font-weight-bold text-amber-darken-2">
                      {{ formatNumber(statsData.totalTwice) }}
                    </div>
                  </div>
                </v-col>

                <v-col cols="6" md="3">
                  <div class="period-stat-card">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-thumb-up" color="green-darken-2" size="20" class="mr-2"></v-icon>
                      <span class="text-caption text-grey-darken-1">有用</span>
                    </div>
                    <div class="text-h4 font-weight-bold text-green-darken-2">
                      {{ formatNumber(statsData.totalHelpful) }}
                    </div>
                  </div>
                </v-col>

                <v-col cols="6" md="3">
                  <div class="period-stat-card">
                    <div class="d-flex align-center mb-2">
                      <v-icon icon="mdi-comment" color="purple-darken-2" size="20" class="mr-2"></v-icon>
                      <span class="text-caption text-grey-darken-1">评论</span>
                    </div>
                    <div class="text-h4 font-weight-bold text-purple-darken-2">
                      {{ formatNumber(statsData.totalComments) }}
                    </div>
                  </div>
                </v-col>
              </v-row>

              <!-- 每日明细 -->
              <div v-if="showDailyDetails" class="daily-details">
                <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">每日数据趋势</h3>

                <!-- 合并图表 -->
                <div class="chart-card-wrapper">
                  <div class="py-4 px-0">
                    <!-- 图例 -->
                    <div class="d-flex flex-wrap justify-center mb-4 ga-4">
                      <div
                        class="legend-item d-flex align-center"
                        :class="{ 'legend-item-inactive': !lineVisibility.views }"
                        @click="toggleLine('views')"
                      >
                        <div class="legend-dot bg-blue-darken-2 mr-2"></div>
                        <span class="text-body-2 text-grey-darken-2">阅读量</span>
                      </div>
                      <div
                        class="legend-item d-flex align-center"
                        :class="{ 'legend-item-inactive': !lineVisibility.twice }"
                        @click="toggleLine('twice')"
                      >
                        <div class="legend-dot bg-amber-darken-2 mr-2"></div>
                        <span class="text-body-2 text-grey-darken-2">两遍秒懂</span>
                      </div>
                      <div
                        class="legend-item d-flex align-center"
                        :class="{ 'legend-item-inactive': !lineVisibility.helpful }"
                        @click="toggleLine('helpful')"
                      >
                        <div class="legend-dot bg-green-darken-2 mr-2"></div>
                        <span class="text-body-2 text-grey-darken-2">有用</span>
                      </div>
                      <div
                        class="legend-item d-flex align-center"
                        :class="{ 'legend-item-inactive': !lineVisibility.comments }"
                        @click="toggleLine('comments')"
                      >
                        <div class="legend-dot bg-purple-darken-2 mr-2"></div>
                        <span class="text-body-2 text-grey-darken-2">评论</span>
                      </div>
                    </div>

                    <!-- 图表容器 -->
                    <div class="chart-wrapper">
                      <div class="chart-container">
                        <canvas ref="chartCanvas" @mousemove="handleChartHover" @mouseleave="handleChartLeave"></canvas>

                        <!-- Tooltip -->
                        <div
                          v-if="tooltip.visible"
                          class="chart-tooltip"
                          :style="{
                            left: tooltip.x + 'px',
                            top: tooltip.y + 'px'
                          }"
                        >
                          <div class="tooltip-title">{{ tooltip.date }}</div>
                          <div class="tooltip-item">
                            <span class="tooltip-dot bg-blue-darken-2"></span>
                            阅读量: <strong>{{ tooltip.views }}</strong>
                          </div>
                          <div class="tooltip-item">
                            <span class="tooltip-dot bg-amber-darken-2"></span>
                            两遍秒懂: <strong>{{ tooltip.twice }}</strong>
                          </div>
                          <div class="tooltip-item">
                            <span class="tooltip-dot bg-green-darken-2"></span>
                            有用: <strong>{{ tooltip.helpful }}</strong>
                          </div>
                          <div class="tooltip-item">
                            <span class="tooltip-dot bg-purple-darken-2"></span>
                            评论: <strong>{{ tooltip.comments }}</strong>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { statsApi } from '@/api'
import { useFetch } from '@/composables'
import type { UserStatsDTO, DailyStatsDTO } from '@/types/user'

const userStore = useUserStore()

// 响应式数据
const selectedPeriod = ref('7') // 默认7天
const error = ref('')
const totalStatsError = ref('')

// Canvas 引用
const chartCanvas = ref<HTMLCanvasElement | null>(null)

// Tooltip 状态
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  date: '',
  views: 0,
  twice: 0,
  helpful: 0,
  comments: 0,
})

// 曲线可见性控制
const lineVisibility = ref({
  views: true,
  twice: true,
  helpful: true,
  comments: true,
})

// 切换曲线显示
const toggleLine = (key: 'views' | 'twice' | 'helpful' | 'comments') => {
  lineVisibility.value[key] = !lineVisibility.value[key]
  nextTick(() => {
    drawChart()
  })
}

// 使用 useFetch 加载全部时间统计数据
const {
  data: totalStatsData,
  loading: totalStatsLoading,
  execute: refreshTotalStats,
} = useFetch<UserStatsDTO>({
  fetchFn: () => {
    if (!userStore.userId) {
      throw new Error('请先登录')
    }
    return statsApi.getUserAllTimeStats(userStore.userId)
  },
  immediate: true,
  onError: (err) => {
    console.error('加载全部时间统计失败:', err)
    totalStatsError.value = err.message
  },
})

// 使用 useFetch 加载时间段统计数据
const {
  data: statsData,
  loading,
  execute: loadStatsData,
} = useFetch<UserStatsDTO>({
  fetchFn: () => {
    if (!userStore.userId) {
      throw new Error('请先登录')
    }
    const days = parseInt(selectedPeriod.value)
    return statsApi.getUserPeriodStats(userStore.userId, days)
  },
  immediate: true,
  onError: (err) => {
    console.error('加载时间段统计失败:', err)
    error.value = err.message
  },
})

// 计算属性
const showDailyDetails = computed(() => {
  return statsData.value?.dailyStats && statsData.value.dailyStats.length > 0
})

const dailyStatsItems = computed(() => {
  if (!statsData.value?.dailyStats) return []

  return statsData.value.dailyStats.map((day: DailyStatsDTO) => ({
    date: day.date,
    views: day.views || 0,
    twice: day.twice || 0,
    helpful: day.helpful || 0,
    comments: day.comments || 0,
  }))
})

// 表格头部配置
const tableHeaders = [
  { title: '日期', key: 'date', align: 'start' as const },
  { title: '阅读量', key: 'views', align: 'center' as const },
  { title: '两遍秒懂', key: 'twice', align: 'center' as const },
  { title: '有用', key: 'helpful', align: 'center' as const },
  { title: '评论', key: 'comments', align: 'center' as const },
]

// 工具方法
const formatNumber = (num: number | undefined | null): string => {
  if (num === null || num === undefined || isNaN(Number(num))) return '0'
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
      return '今天'
    } else if (date.toDateString() === yesterday.toDateString()) {
      return '昨天'
    } else {
      return date.toLocaleDateString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
      })
    }
  } catch (e) {
    console.error('格式化日期错误:', e)
    return dateString
  }
}

const getPeriodText = (): string => {
  switch (selectedPeriod.value) {
    case '7':
      return '最近7天'
    case '30':
      return '最近30天'
    case '180':
      return '最近半年'
    case '365':
      return '最近一年'
    default:
      return '自定义'
  }
}

// 数据加载方法
const onPeriodChange = (): void => {
  error.value = ''
  loadStatsData()
}

const refreshAllData = (): void => {
  refreshTotalStats()
  loadStatsData()
}

// 绘制图表
const drawChart = () => {
  const canvas = chartCanvas.value
  if (!canvas || !statsData.value?.dailyStats) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 获取父容器的宽度
  const container = canvas.parentElement
  if (!container) return

  const containerWidth = container.clientWidth
  const containerHeight = 300

  // 设置 canvas 尺寸
  const dpr = window.devicePixelRatio || 1
  canvas.width = containerWidth * dpr
  canvas.height = containerHeight * dpr
  canvas.style.width = containerWidth + 'px'
  canvas.style.height = containerHeight + 'px'
  ctx.scale(dpr, dpr)

  const width = containerWidth
  const height = containerHeight
  const padding = { top: 20, right: 20, bottom: 40, left: 50 }
  const chartWidth = width - padding.left - padding.right
  const chartHeight = height - padding.top - padding.bottom

  // 清空画布
  ctx.clearRect(0, 0, width, height)

  const data = dailyStatsItems.value
  if (data.length === 0) return

  // 找到最大值用于缩放（只计算可见的曲线）
  const visibleValues: number[] = []
  if (lineVisibility.value.views) visibleValues.push(...data.map((d) => d.views))
  if (lineVisibility.value.twice) visibleValues.push(...data.map((d) => d.twice))
  if (lineVisibility.value.helpful) visibleValues.push(...data.map((d) => d.helpful))
  if (lineVisibility.value.comments) visibleValues.push(...data.map((d) => d.comments))

  const maxValue = visibleValues.length > 0 ? Math.max(...visibleValues, 1) : 1
  const yScale = chartHeight / maxValue

  // 绘制网格线和Y轴标签
  ctx.strokeStyle = '#e0e0e0'
  ctx.lineWidth = 1
  ctx.fillStyle = '#666'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'right'

  const gridLines = 5
  for (let i = 0; i <= gridLines; i++) {
    const y = padding.top + (chartHeight / gridLines) * i
    const value = Math.round(maxValue * (1 - i / gridLines))

    ctx.beginPath()
    ctx.moveTo(padding.left, y)
    ctx.lineTo(width - padding.right, y)
    ctx.stroke()

    ctx.fillText(String(value), padding.left - 10, y + 4)
  }

  // 绘制曲线的函数
  const drawLine = (values: number[], color: string) => {
    if (values.length === 0) return

    ctx.strokeStyle = color
    ctx.lineWidth = 2.5
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'

    ctx.beginPath()
    values.forEach((value, index) => {
      const x = padding.left + (chartWidth / Math.max(values.length - 1, 1)) * index
      const y = padding.top + chartHeight - value * yScale

      if (index === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    })
    ctx.stroke()

    // 绘制数据点
    ctx.fillStyle = color
    values.forEach((value, index) => {
      const x = padding.left + (chartWidth / Math.max(values.length - 1, 1)) * index
      const y = padding.top + chartHeight - value * yScale
      ctx.beginPath()
      ctx.arc(x, y, 4, 0, Math.PI * 2)
      ctx.fill()
    })
  }

  // 绘制四条线（根据可见性）
  if (lineVisibility.value.views) {
    drawLine(
      data.map((d) => d.views),
      '#1976D2'
    )
  }
  if (lineVisibility.value.twice) {
    drawLine(
      data.map((d) => d.twice),
      '#F57C00'
    )
  }
  if (lineVisibility.value.helpful) {
    drawLine(
      data.map((d) => d.helpful),
      '#388E3C'
    )
  }
  if (lineVisibility.value.comments) {
    drawLine(
      data.map((d) => d.comments),
      '#7B1FA2'
    )
  }

  // 绘制X轴日期标签
  ctx.fillStyle = '#666'
  ctx.font = '11px sans-serif'
  ctx.textAlign = 'center'

  const labelStep = Math.max(Math.ceil(data.length / 7), 1)
  data.forEach((item, index) => {
    if (index % labelStep === 0 || index === data.length - 1) {
      const x = padding.left + (chartWidth / Math.max(data.length - 1, 1)) * index
      const y = height - 15
      ctx.fillText(formatDate(item.date), x, y)
    }
  })
}

// 处理鼠标悬停
const handleChartHover = (event: MouseEvent) => {
  const canvas = chartCanvas.value
  if (!canvas || !statsData.value?.dailyStats) return

  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const padding = { left: 50, right: 20 }
  const chartWidth = rect.width - padding.left - padding.right
  const data = dailyStatsItems.value

  // 找到最近的数据点
  const index = Math.round(((x - padding.left) / chartWidth) * (data.length - 1))

  if (index >= 0 && index < data.length) {
    const item = data[index]
    tooltip.value = {
      visible: true,
      x: event.clientX - rect.left + rect.left,
      y: event.clientY - rect.top + rect.top - 20,
      date: formatDate(item.date),
      views: item.views,
      twice: item.twice,
      helpful: item.helpful,
      comments: item.comments,
    }
  }
}

// 处理鼠标离开
const handleChartLeave = () => {
  tooltip.value.visible = false
}

// 监听数据变化重绘图表
watch(
  () => statsData.value,
  () => {
    nextTick(() => {
      drawChart()
    })
  }
)

// 组件挂载后绘制图表
onMounted(() => {
  nextTick(() => {
    drawChart()
  })

  // 窗口大小变化时重绘
  window.addEventListener('resize', drawChart)
})

// 监听用户状态变化
watch(
  () => userStore.userId,
  (newUserId) => {
    if (newUserId) {
      refreshAllData()
    }
  }
)
</script>

<style scoped>
/* 左侧边栏 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.sidebar-content {
  padding: 20px 16px;
}

.sidebar-metrics {
  display: flex;
  flex-direction: column;
}

.metric-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

/* 主内容区 */
.creator-stats-container {
  max-width: 100%;
}

/* 总览卡片 */
.overview-card {
  background: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
}

/* 统计项布局 */
.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.stat-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 0.875rem;
  font-weight: 500;
}

/* 时间段卡片 */
.period-card {
  background: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
}

.period-stat-card {
  padding: 20px;
  border-radius: 12px;
  background: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline));
  transition: all 0.2s ease;
}

.period-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 每日明细 */
.daily-details {
  margin-top: 32px;
}

/* 图表卡片包装器 */
.chart-card-wrapper {
  width: 100%;
}

/* 图例 */
.legend-item {
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  user-select: none;
}

.legend-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.legend-item-inactive {
  opacity: 0.3;
}

.legend-item-inactive:hover {
  opacity: 0.5;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.legend-item-inactive .legend-dot {
  opacity: 0.5;
}

/* 图表包装器 */
.chart-wrapper {
  width: 100%;
  overflow: hidden;
}

/* 图表容器 */
.chart-container {
  position: relative;
  width: 100%;
  min-height: 300px;
}

.chart-container canvas {
  display: block;
  width: 100% !important;
  height: 300px !important;
  cursor: crosshair;
}

/* Tooltip */
.chart-tooltip {
  position: fixed;
  background: white;
  border: 1.5px solid rgb(var(--v-theme-outline));
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  pointer-events: none;
  z-index: 1000;
  min-width: 180px;
  transform: translate(-50%, -100%);
  margin-top: -10px;
}

.tooltip-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: rgb(var(--v-theme-on-surface));
  border-bottom: 1px solid rgb(var(--v-theme-outline));
  padding-bottom: 6px;
}

.tooltip-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  margin-bottom: 4px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.tooltip-item:last-child {
  margin-bottom: 0;
}

.tooltip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
}

.tooltip-item strong {
  margin-left: auto;
  color: rgb(var(--v-theme-on-surface));
}

/* 响应式调整 */
@media (max-width: 1280px) {
  .stat-item {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }

  .stat-icon-wrapper {
    width: 48px;
    height: 48px;
  }

  .stat-value {
    font-size: 1.5rem;
  }
}

@media (max-width: 960px) {
  .period-stat-card {
    padding: 16px;
  }
}

@media (max-width: 600px) {
  .overview-card,
  .period-card {
    border-radius: 0 !important;
    border-left: none;
    border-right: none;
  }
}
</style>
