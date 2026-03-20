<template>
  <div class="pa-0 pa-sm-1">
    <!-- 总览卡片 -->
    <v-card rounded="lg" border class="mb-4" elevation="0">
      <v-card-text class="pa-4">
        <div class="d-flex align-center justify-space-between mb-4">
          <div class="text-body-1 font-weight-bold">创作数据总览</div>
          <v-btn
            icon
            variant="text"
            size="x-small"
            :loading="totalStatsLoading"
            @click="refreshAllData"
          >
            <v-icon size="18">mdi-refresh</v-icon>
          </v-btn>
        </div>

        <LoadingSpinner v-if="totalStatsLoading" />

        <div v-else-if="totalStatsData" class="stats-grid">
          <div class="stat-item">
            <div class="text-h5 font-weight-bold">{{ formatNumber(totalStatsData.totalViews) }}</div>
            <div class="text-caption text-medium-emphasis">阅读量</div>
          </div>
          <div class="stat-item">
            <div class="text-h5 font-weight-bold">{{ formatNumber(totalStatsData.totalTwice) }}</div>
            <div class="text-caption text-medium-emphasis">两遍秒懂</div>
          </div>
          <div class="stat-item">
            <div class="text-h5 font-weight-bold">{{ formatNumber(totalStatsData.totalHelpful) }}</div>
            <div class="text-caption text-medium-emphasis">有用</div>
          </div>
          <div class="stat-item">
            <div class="text-h5 font-weight-bold">{{ formatNumber(totalStatsData.totalComments) }}</div>
            <div class="text-caption text-medium-emphasis">评论</div>
          </div>
        </div>
      </v-card-text>
    </v-card>

    <!-- 时间段统计 -->
    <v-card rounded="lg" border elevation="0">
      <v-card-text class="pa-4">
        <div class="d-flex align-center justify-space-between mb-4 flex-wrap ga-3">
          <div class="text-body-1 font-weight-bold">{{ getPeriodText() }}</div>
          <div class="d-flex align-center">
            <v-btn
              variant="text"
              size="small"
              rounded="lg"
              :color="selectedPeriod === '7' ? 'primary' : 'default'"
              @click="selectedPeriod = '7'; onPeriodChange()"
            >
              7天
            </v-btn>
            <v-btn
              variant="text"
              size="small"
              rounded="lg"
              :color="selectedPeriod === '30' ? 'primary' : 'default'"
              @click="selectedPeriod = '30'; onPeriodChange()"
            >
              30天
            </v-btn>
            <v-btn
              variant="text"
              size="small"
              rounded="lg"
              :color="selectedPeriod === '180' ? 'primary' : 'default'"
              @click="selectedPeriod = '180'; onPeriodChange()"
            >
              半年
            </v-btn>
            <v-btn
              variant="text"
              size="small"
              rounded="lg"
              :color="selectedPeriod === '365' ? 'primary' : 'default'"
              @click="selectedPeriod = '365'; onPeriodChange()"
            >
              一年
            </v-btn>
          </div>
        </div>

        <LoadingSpinner v-if="loading" />

        <div v-else-if="statsData">
          <div class="stats-grid mb-4">
            <div class="stat-item">
              <div class="text-h5 font-weight-bold">{{ formatNumber(statsData.totalViews) }}</div>
              <div class="text-caption text-medium-emphasis">阅读量</div>
            </div>
            <div class="stat-item">
              <div class="text-h5 font-weight-bold">{{ formatNumber(statsData.totalTwice) }}</div>
              <div class="text-caption text-medium-emphasis">两遍秒懂</div>
            </div>
            <div class="stat-item">
              <div class="text-h5 font-weight-bold">{{ formatNumber(statsData.totalHelpful) }}</div>
              <div class="text-caption text-medium-emphasis">有用</div>
            </div>
            <div class="stat-item">
              <div class="text-h5 font-weight-bold">{{ formatNumber(statsData.totalComments) }}</div>
              <div class="text-caption text-medium-emphasis">评论</div>
            </div>
          </div>

          <!-- 图表 -->
          <div v-if="showDailyDetails" class="chart-container">
            <canvas ref="chartCanvas"></canvas>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { statsApi } from '@/api'
import { useFetch } from '@/composables'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
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
  viewCount: 0,
  twiceCount: 0,
  likeCount: 0,
  commentCount: 0,
})

// 曲线可见性控制
const lineVisibility = ref({
  viewCount: true,
  twiceCount: true,
  likeCount: true,
  commentCount: true,
})

// 切换曲线显示
const toggleLine = (key: 'viewCount' | 'twiceCount' | 'likeCount' | 'commentCount') => {
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
    viewCount: day.viewCount || 0,
    twiceCount: day.twiceCount || 0,
    likeCount: day.likeCount || 0,
    commentCount: day.commentCount || 0,
  }))
})

// 表格头部配置
const tableHeaders = [
  { title: '日期', key: 'date', align: 'start' as const },
  { title: '阅读量', key: 'viewCount', align: 'center' as const },
  { title: '两遍秒懂', key: 'twiceCount', align: 'center' as const },
  { title: '有用', key: 'likeCount', align: 'center' as const },
  { title: '评论', key: 'commentCount', align: 'center' as const },
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
  if (lineVisibility.value.viewCount) visibleValues.push(...data.map((d) => d.viewCount))
  if (lineVisibility.value.twiceCount) visibleValues.push(...data.map((d) => d.twiceCount))
  if (lineVisibility.value.likeCount) visibleValues.push(...data.map((d) => d.likeCount))
  if (lineVisibility.value.commentCount) visibleValues.push(...data.map((d) => d.commentCount))

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
  if (lineVisibility.value.viewCount) {
    drawLine(
      data.map((d) => d.viewCount),
      '#1976D2'
    )
  }
  if (lineVisibility.value.twiceCount) {
    drawLine(
      data.map((d) => d.twiceCount),
      '#F57C00'
    )
  }
  if (lineVisibility.value.likeCount) {
    drawLine(
      data.map((d) => d.likeCount),
      '#388E3C'
    )
  }
  if (lineVisibility.value.commentCount) {
    drawLine(
      data.map((d) => d.commentCount),
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
      viewCount: item.viewCount,
      twiceCount: item.twiceCount,
      likeCount: item.likeCount,
      commentCount: item.commentCount,
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
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-item {
  text-align: center;
}

.chart-container {
  width: 100%;
  height: 250px;
}

.chart-container canvas {
  width: 100% !important;
  height: 100% !important;
}
</style>
