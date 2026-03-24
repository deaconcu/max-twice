<script lang="ts">
export default {
  name: 'ActivityHeatmap',
}
</script>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { statsApi } from '@/api'
import { useFetch } from '@/composables'
import { useUserStore } from '@/stores/modules/user'
import type { HeatmapData } from '@/types/stats'

interface Props {
  userId?: number // 用户ID，不传则使用当前登录用户
  months?: number // 显示几个月，默认12个月
}

const props = withDefaults(defineProps<Props>(), {
  months: 12,
})

const userStore = useUserStore()

// 实际使用的用户ID
const targetUserId = computed(() => props.userId ?? userStore.currentUser?.id)

// 加载热力图数据
const { data: heatmapData, refresh } = useFetch<HeatmapData>({
  fetchFn: () => statsApi.getHeatmap(targetUserId.value!, props.months),
  immediate: !!targetUserId.value,
  defaultValue: null,
})

// 监听用户ID变化，重新加载数据
watch(targetUserId, (newVal) => {
  if (newVal) {
    refresh()
  }
})

// 日数据类型
interface DayData {
  date: string
  value: number
  completedNodes: number
  cancelCompletedNodes: number
  reviewedCards: number
  hasData: boolean // 是否有数据（用户注册后的日期才有数据）
}

// 将API数据转换为组件需要的格式
const activityData = computed((): DayData[] => {
  if (!heatmapData.value?.dailyData) {
    return generateEmptyData()
  }

  // 用户注册日期
  const joinedDate = heatmapData.value.joinedDate

  // 使用后端返回的日期范围（用户时区）
  const startDate = heatmapData.value.startDate
  const endDate = heatmapData.value.endDate

  // 将 API 返回的数据转换为 date -> dayInfo 的 Map
  const dataMap = new Map<
    string,
    { value: number; completedNodes: number; cancelCompletedNodes: number; reviewedCards: number }
  >()
  heatmapData.value.dailyData.forEach((day) => {
    dataMap.set(day.date, {
      value: day.activityValue,
      completedNodes: day.completedNodes,
      cancelCompletedNodes: day.cancelCompletedNodes,
      reviewedCards: day.reviewedCards,
    })
  })

  // 生成完整的日期范围（基于后端返回的 startDate 和 endDate）
  const result: DayData[] = []
  const start = new Date(startDate)
  const end = new Date(endDate)

  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dateStr = d.toISOString().split('T')[0]
    const dayInfo = dataMap.get(dateStr)
    // 判断是否有数据：日期 >= 用户注册日期
    const hasData = !joinedDate || dateStr >= joinedDate
    result.push({
      date: dateStr,
      value: dayInfo?.value ?? 0,
      completedNodes: dayInfo?.completedNodes ?? 0,
      cancelCompletedNodes: dayInfo?.cancelCompletedNodes ?? 0,
      reviewedCards: dayInfo?.reviewedCards ?? 0,
      hasData,
    })
  }

  return result
})

// 生成空数据（未登录或加载失败时）
const generateEmptyData = (): DayData[] => {
  const data: DayData[] = []
  const today = new Date()
  const totalDays = props.months * 30

  for (let i = totalDays - 1; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    data.push({
      date: dateStr,
      value: 0,
      completedNodes: 0,
      cancelCompletedNodes: 0,
      reviewedCards: 0,
      hasData: false,
    })
  }

  return data
}

// 按周分组数据
const weeklyData = computed(() => {
  const weeks: DayData[][] = []
  let currentWeek: DayData[] = []

  // 找到第一天是周几，补齐前面的空白
  const firstDate = new Date(activityData.value[0].date)
  const firstDayOfWeek = firstDate.getDay() // 0=周日, 1=周一...

  // 补齐第一周前面的空白
  for (let i = 0; i < firstDayOfWeek; i++) {
    currentWeek.push({
      date: '',
      value: -1,
      completedNodes: 0,
      cancelCompletedNodes: 0,
      reviewedCards: 0,
      hasData: false,
    }) // -1 表示空白
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

// 统计数据
const totalCompletedNodes = computed(() => heatmapData.value?.totalCompletedNodes ?? 0)
const totalReviewedCards = computed(() => heatmapData.value?.totalReviewedCards ?? 0)

// 根据活跃值获取颜色等级
const getColorLevel = (day: DayData): string => {
  if (day.value < 0) return 'empty' // 补齐周的空白格
  if (!day.hasData) return 'empty' // 尚未加入
  if (day.value === 0) return 'level-0'
  if (day.value <= 5) return 'level-1'
  if (day.value <= 15) return 'level-2'
  if (day.value <= 30) return 'level-3'
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
const formatValue = (day: DayData): string => {
  if (!day.hasData) return '尚未加入'
  const parts: string[] = []
  if (day.completedNodes > 0 || day.cancelCompletedNodes > 0) {
    const net = day.completedNodes - day.cancelCompletedNodes
    parts.push(`共完成 ${net} 个节点（完成 ${day.completedNodes}，取消 ${day.cancelCompletedNodes}）`)
  }
  if (day.reviewedCards > 0) {
    parts.push(`复习 ${day.reviewedCards} 张卡片`)
  }
  return parts.length > 0 ? parts.join('，') : '无活动'
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
            :disabled="day.value < 0 || !day.hasData"
          >
            <template #activator="{ props: tooltipProps }">
              <div
                v-bind="tooltipProps"
                class="heatmap-cell"
                :class="getColorLevel(day)"
              />
            </template>
            <div class="tooltip-content">
              <div class="tooltip-date">{{ formatDate(day.date) }}</div>
              <div class="tooltip-value">{{ formatValue(day) }}</div>
            </div>
          </v-tooltip>
        </div>
      </div>
    </div>

    <!-- 图例 -->
    <div class="heatmap-footer">
      <div class="heatmap-summary">
        <span class="text-caption text-grey"
          >过去一年：完成
          <strong class="text-grey-darken-2">{{ totalCompletedNodes }}</strong> 个节点，复习
          <strong class="text-grey-darken-2">{{ totalReviewedCards.toLocaleString() }}</strong>
          张卡片</span
        >
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
