<script setup lang="ts">
import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Title,
  Tooltip,
} from 'chart.js'
import type { TooltipItem, ChartOptions, ChartData } from 'chart.js'
import { Line as LineChart } from 'vue-chartjs'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

// 注册Chart.js组件
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

// 类型定义
interface DailyStat {
  date: string
  views?: number
  twice?: number
  helpful?: number
  comments?: number
}

interface Props {
  dailyStats?: DailyStat[]
}

const props = withDefaults(defineProps<Props>(), {
  dailyStats: () => [],
})

const { t } = useI18n()

const chartData = computed((): ChartData<'line'> | null => {
  if (!props.dailyStats || props.dailyStats.length === 0) {
    return null
  }

  // 按日期排序（升序）
  const sortedStats = [...props.dailyStats].sort(
    (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
  )

  const labels = sortedStats.map((stat) => {
    const date = new Date(stat.date)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)

    if (date.toDateString() === today.toDateString()) {
      return t('trendChart.today')
    } else if (date.toDateString() === yesterday.toDateString()) {
      return t('trendChart.yesterday')
    } else {
      return date.toLocaleDateString('zh-CN', {
        month: 'numeric',
        day: 'numeric',
      })
    }
  })

  const viewsData = sortedStats.map((stat) => stat.views || 0)
  const twiceData = sortedStats.map((stat) => stat.twice || 0)
  const helpfulData = sortedStats.map((stat) => stat.helpful || 0)
  const commentsData = sortedStats.map((stat) => stat.comments || 0)

  return {
    labels,
    datasets: [
      {
        label: t('trendChart.views'),
        data: viewsData,
        borderColor: '#2196F3',
        backgroundColor: 'rgba(33, 150, 243, 0.1)',
        tension: 0.4,
        fill: true,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#2196F3',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
      },
      {
        label: 'Max Twice',
        data: twiceData,
        borderColor: '#4CAF50',
        backgroundColor: 'rgba(76, 175, 80, 0.1)',
        tension: 0.4,
        fill: false,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#4CAF50',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
      },
      {
        label: t('trendChart.helpful'),
        data: helpfulData,
        borderColor: '#F44336',
        backgroundColor: 'rgba(244, 67, 54, 0.1)',
        tension: 0.4,
        fill: false,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#F44336',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
      },
      {
        label: t('trendChart.comments'),
        data: commentsData,
        borderColor: '#FF9800',
        backgroundColor: 'rgba(255, 152, 0, 0.1)',
        tension: 0.4,
        fill: false,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#FF9800',
        pointBorderColor: '#fff',
        pointBorderWidth: 2,
      },
    ],
  }
})

const chartOptions = computed((): ChartOptions<'line'> => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    intersect: false,
    mode: 'index',
  },
  plugins: {
    legend: {
      display: true,
      position: 'top',
      labels: {
        usePointStyle: true,
        padding: 20,
        font: {
          size: 12,
        },
      },
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      titleColor: '#fff',
      bodyColor: '#fff',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      borderWidth: 1,
      cornerRadius: 8,
      displayColors: true,
      callbacks: {
        title(context: TooltipItem<'line'>[]): string {
          return context[0].label
        },
        label(context: TooltipItem<'line'>): string {
          const label = context.dataset.label || ''
          const value = context.parsed.y
          return `${label}: ${value}`
        },
      },
    },
  },
  scales: {
    x: {
      display: true,
      border: {
        display: false,
      },
      grid: {
        display: true,
        color: 'rgba(0, 0, 0, 0.05)',
      },
      ticks: {
        font: {
          size: 11,
        },
        color: '#666',
      },
    },
    y: {
      display: true,
      beginAtZero: true,
      border: {
        display: false,
      },
      grid: {
        display: true,
        color: 'rgba(0, 0, 0, 0.05)',
      },
      ticks: {
        font: {
          size: 11,
        },
        color: '#666',
        callback(value: string | number): string | number {
          const numValue = typeof value === 'string' ? parseFloat(value) : value
          if (numValue >= 1000) {
            return `${(numValue / 1000).toFixed(1)}k`
          }
          return numValue
        },
      },
    },
  },
  elements: {
    point: {
      hoverRadius: 8,
    },
  },
}))
</script>

<template>
  <div class="trend-chart-container">
    <LineChart v-if="chartData" :data="chartData" :options="chartOptions" class="chart-canvas" />
    <div v-else class="chart-loading">
      <v-progress-circular
        indeterminate
        color="primary"
        size="32"
        class="mb-2"
      ></v-progress-circular>
      <p class="loading-text">{{ t('trendChart.loadingData') }}</p>
    </div>
  </div>
</template>

<style scoped>
.trend-chart-container {
  width: 100%;
  height: 300px;
  position: relative;
}

.chart-canvas {
  width: 100% !important;
  height: 100% !important;
}

.chart-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.loading-text {
  font-size: 0.875rem;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .trend-chart-container {
    height: 250px;
  }
}
</style>