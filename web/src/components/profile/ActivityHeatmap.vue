<script lang="ts">
export default {
  name: 'ActivityHeatmap',
}
</script>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  // 未来接入真实数据时使用
  // data?: Record<string, number>
  months?: number // 显示几个月，默认4个月
}

const props = withDefaults(defineProps<Props>(), {
  months: 4,
})

// 生成假数据：最近 N 个月的每日活跃值
const generateMockData = () => {
  const data: { date: string; value: number }[] = []
  const today = new Date()
  const totalDays = props.months * 30

  for (let i = totalDays - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]

    // 随机生成活跃值（0-15），有30%的天数为0
    const random = Math.random()
    let value = 0
    if (random > 0.3) {
      value = Math.floor(Math.random() * 15) + 1
    }

    data.push({ date: dateStr, value })
  }

  return data
}

const activityData = computed(() => generateMockData())

// 按周分组数据
const weeklyData = computed(() => {
  const weeks: { date: string; value: number }[][] = []
  let currentWeek: { date: string; value: number }[] = []

  // 找到第一天是周几，补齐前面的空白
  const firstDate = new Date(activityData.value[0].date)
  const firstDayOfWeek = firstDate.getDay() // 0=周日, 1=周一...

  // 补齐第一周前面的空白
  for (let i = 0; i < firstDayOfWeek; i++) {
    currentWeek.push({ date: '', value: -1 }) // -1 表示空白
  }

  activityData.value.forEach((item) => {
    currentWeek.push(item)
    if (currentWeek.length === 7) {
      weeks.push(currentWeek)
      currentWeek = []
    }
  })

  // 处理最后一周
  if (currentWeek.length > 0) {
    weeks.push(currentWeek)
  }

  return weeks
})

// 获取月份标签
const monthLabels = computed(() => {
  const labels: { month: string; index: number }[] = []
  let lastMonth = ''

  weeklyData.value.forEach((week, weekIndex) => {
    const firstValidDay = week.find((d) => d.date)
    if (firstValidDay && firstValidDay.date) {
      const date = new Date(firstValidDay.date)
      const month = date.toLocaleDateString('zh-CN', { month: 'short' })
      if (month !== lastMonth) {
        labels.push({ month, index: weekIndex })
        lastMonth = month
      }
    }
  })

  return labels
})

// 根据活跃值获取颜色等级
const getColorLevel = (value: number): string => {
  if (value < 0) return 'empty'
  if (value === 0) return 'level-0'
  if (value <= 3) return 'level-1'
  if (value <= 6) return 'level-2'
  if (value <= 10) return 'level-3'
  return 'level-4'
}

// 格式化日期显示
const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })
}

// 格式化活跃值显示
const formatValue = (value: number): string => {
  if (value <= 0) return '无活动'
  return `${value} 次活动`
}
</script>

<template>
  <div class="heatmap-container">
    <!-- 月份标签 -->
    <div class="month-labels">
      <span
        v-for="label in monthLabels"
        :key="label.index"
        class="month-label"
        :style="{ left: `${label.index * 14 + 20}px` }"
      >
        {{ label.month }}
      </span>
    </div>

    <!-- 热力图主体 -->
    <div class="heatmap-body">
      <!-- 星期标签 -->
      <div class="weekday-labels">
        <span class="weekday-label"></span>
        <span class="weekday-label">一</span>
        <span class="weekday-label"></span>
        <span class="weekday-label">三</span>
        <span class="weekday-label"></span>
        <span class="weekday-label">五</span>
        <span class="weekday-label"></span>
      </div>

      <!-- 格子区域 -->
      <div class="heatmap-grid">
        <div v-for="(week, weekIndex) in weeklyData" :key="weekIndex" class="heatmap-week">
          <v-tooltip
            v-for="(day, dayIndex) in week"
            :key="dayIndex"
            location="top"
            :disabled="day.value < 0"
          >
            <template #activator="{ props: tooltipProps }">
              <div
                v-bind="tooltipProps"
                class="heatmap-cell"
                :class="getColorLevel(day.value)"
              />
            </template>
            <div class="tooltip-content">
              <div class="tooltip-date">{{ formatDate(day.date) }}</div>
              <div class="tooltip-value">{{ formatValue(day.value) }}</div>
            </div>
          </v-tooltip>
        </div>
      </div>
    </div>

    <!-- 图例 -->
    <div class="heatmap-footer">
      <div class="heatmap-summary">
        <span class="text-caption text-grey">过去一年：完成 <strong class="text-grey-darken-2">128</strong> 个节点，复习 <strong class="text-grey-darken-2">1,024</strong> 张卡片</span>
      </div>
      <div class="heatmap-legend">
        <span class="legend-text">少</span>
        <div class="legend-cell level-0" />
        <div class="legend-cell level-1" />
        <div class="legend-cell level-2" />
        <div class="legend-cell level-3" />
        <div class="legend-cell level-4" />
        <span class="legend-text">多</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.heatmap-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.month-labels {
  position: relative;
  height: 16px;
  margin-left: 28px;
}

.month-label {
  position: absolute;
  font-size: 11px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.heatmap-body {
  display: flex;
  gap: 4px;
}

.weekday-labels {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.weekday-label {
  width: 20px;
  height: 10px;
  font-size: 10px;
  color: rgb(var(--v-theme-on-surface-variant));
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 4px;
}

.heatmap-grid {
  display: flex;
  gap: 3px;
}

.heatmap-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heatmap-cell {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
  cursor: pointer;
}

.heatmap-cell.empty {
  background-color: transparent;
  cursor: default;
}

.heatmap-cell.level-0 {
  background-color: rgba(var(--v-theme-on-surface), 0.08);
}

.heatmap-cell.level-1 {
  background-color: rgba(76, 175, 80, 0.3);
}

.heatmap-cell.level-2 {
  background-color: rgba(76, 175, 80, 0.5);
}

.heatmap-cell.level-3 {
  background-color: rgba(76, 175, 80, 0.7);
}

.heatmap-cell.level-4 {
  background-color: rgb(76, 175, 80);
}

.heatmap-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0;
  margin-left: 24px;
}

.heatmap-summary {
  flex-shrink: 0;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 3px;
}

.legend-text {
  font-size: 10px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.legend-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-cell.level-0 {
  background-color: rgba(var(--v-theme-on-surface), 0.08);
}

.legend-cell.level-1 {
  background-color: rgba(76, 175, 80, 0.3);
}

.legend-cell.level-2 {
  background-color: rgba(76, 175, 80, 0.5);
}

.legend-cell.level-3 {
  background-color: rgba(76, 175, 80, 0.7);
}

.legend-cell.level-4 {
  background-color: rgb(76, 175, 80);
}

.tooltip-content {
  text-align: center;
}

.tooltip-date {
  font-size: 12px;
  margin-bottom: 2px;
}

.tooltip-value {
  font-size: 11px;
  font-weight: 500;
}
</style>
