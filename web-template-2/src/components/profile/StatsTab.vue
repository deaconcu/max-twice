<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10">
          <div class="d-flex align-center mb-3">
            <v-icon icon="mdi-chart-line" color="primary" size="20" class="mr-2"></v-icon>
            <h4 class="text-body-1 font-weight-bold">数据统计</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            查看您的学习数据统计和趋势分析。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-chart-box" size="14" class="mr-1"></v-icon>
              可视化数据展示
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-calendar-range" size="14" class="mr-1"></v-icon>
              多时间段对比
            </div>
            <div>
              <v-icon icon="mdi-trending-up" size="14" class="mr-1"></v-icon>
              学习趋势分析
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <h3 class="text-h6 font-weight-bold">数据统计</h3>

          <!-- 时间段选择 -->
          <v-btn-toggle
            v-model="selectedPeriod"
            color="primary"
            variant="outlined"
            divided
            rounded="md"
            density="compact"
          >
            <v-btn value="today" size="small" rounded="md">今天</v-btn>
            <v-btn value="7days" size="small" rounded="md">7天</v-btn>
            <v-btn value="30days" size="small" rounded="md">30天</v-btn>
            <v-btn value="all" size="small" rounded="md">全部</v-btn>
          </v-btn-toggle>
        </div>

        <!-- 统计卡片 -->
        <v-row class="mb-6">
          <v-col cols="12" md="6" lg="3">
            <v-card border rounded="md" class="pa-4 stat-card">
              <div class="d-flex align-center mb-2">
                <v-avatar color="blue-lighten-5" size="48" class="mr-3">
                  <v-icon icon="mdi-book-open-outline" color="primary" size="24"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <div class="text-h5 font-weight-bold text-primary">{{ currentStats.totalCourses }}</div>
                  <div class="text-caption text-grey">学习课程</div>
                </div>
              </div>
              <div v-if="currentStats.coursesGrowth" class="d-flex align-center text-caption">
                <v-icon
                  :icon="currentStats.coursesGrowth > 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
                  :color="currentStats.coursesGrowth > 0 ? 'success' : 'error'"
                  size="14"
                  class="mr-1"
                ></v-icon>
                <span :class="currentStats.coursesGrowth > 0 ? 'text-success' : 'text-error'">
                  {{ Math.abs(currentStats.coursesGrowth) }}%
                </span>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="6" lg="3">
            <v-card border rounded="md" class="pa-4 stat-card">
              <div class="d-flex align-center mb-2">
                <v-avatar color="green-lighten-5" size="48" class="mr-3">
                  <v-icon icon="mdi-check-circle" color="success" size="24"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <div class="text-h5 font-weight-bold text-success">{{ currentStats.completedCourses }}</div>
                  <div class="text-caption text-grey">完成课程</div>
                </div>
              </div>
              <div v-if="currentStats.completedGrowth" class="d-flex align-center text-caption">
                <v-icon
                  :icon="currentStats.completedGrowth > 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
                  :color="currentStats.completedGrowth > 0 ? 'success' : 'error'"
                  size="14"
                  class="mr-1"
                ></v-icon>
                <span :class="currentStats.completedGrowth > 0 ? 'text-success' : 'text-error'">
                  {{ Math.abs(currentStats.completedGrowth) }}%
                </span>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="6" lg="3">
            <v-card border rounded="md" class="pa-4 stat-card">
              <div class="d-flex align-center mb-2">
                <v-avatar color="orange-lighten-5" size="48" class="mr-3">
                  <v-icon icon="mdi-calendar-check" color="warning" size="24"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <div class="text-h5 font-weight-bold text-warning">{{ currentStats.studyDays }}</div>
                  <div class="text-caption text-grey">学习天数</div>
                </div>
              </div>
              <div v-if="currentStats.daysGrowth" class="d-flex align-center text-caption">
                <v-icon
                  :icon="currentStats.daysGrowth > 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
                  :color="currentStats.daysGrowth > 0 ? 'success' : 'error'"
                  size="14"
                  class="mr-1"
                ></v-icon>
                <span :class="currentStats.daysGrowth > 0 ? 'text-success' : 'text-error'">
                  {{ Math.abs(currentStats.daysGrowth) }}%
                </span>
              </div>
            </v-card>
          </v-col>

          <v-col cols="12" md="6" lg="3">
            <v-card border rounded="md" class="pa-4 stat-card">
              <div class="d-flex align-center mb-2">
                <v-avatar color="purple-lighten-5" size="48" class="mr-3">
                  <v-icon icon="mdi-clock-outline" color="purple" size="24"></v-icon>
                </v-avatar>
                <div class="flex-grow-1">
                  <div class="text-h5 font-weight-bold text-purple">{{ currentStats.studyHours }}</div>
                  <div class="text-caption text-grey">学习时长(h)</div>
                </div>
              </div>
              <div v-if="currentStats.hoursGrowth" class="d-flex align-center text-caption">
                <v-icon
                  :icon="currentStats.hoursGrowth > 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
                  :color="currentStats.hoursGrowth > 0 ? 'success' : 'error'"
                  size="14"
                  class="mr-1"
                ></v-icon>
                <span :class="currentStats.hoursGrowth > 0 ? 'text-success' : 'text-error'">
                  {{ Math.abs(currentStats.hoursGrowth) }}%
                </span>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <v-divider class="my-6"></v-divider>

        <!-- 学习趋势图 -->
        <h4 class="text-body-1 font-weight-bold mb-4">学习趋势</h4>
        <v-card border rounded="md" class="pa-5">
          <div class="chart-container">
            <!-- 简单的趋势图展示 -->
            <div class="d-flex align-center justify-space-between mb-4">
              <span class="text-caption text-grey">学习时长 (小时)</span>
              <v-chip size="small" color="primary" variant="flat">{{ selectedPeriodText }}</v-chip>
            </div>

            <!-- 简单柱状图展示 -->
            <div class="simple-chart">
              <div
                v-for="(day, index) in chartData"
                :key="index"
                class="chart-bar-wrapper"
              >
                <div class="chart-bar-container">
                  <v-tooltip location="top">
                    <template #activator="{ props }">
                      <div
                        v-bind="props"
                        class="chart-bar"
                        :style="{
                          height: `${(day.value / maxValue) * 100}%`,
                          backgroundColor: getBarColor(day.value)
                        }"
                      ></div>
                    </template>
                    <span>{{ day.value }} 小时</span>
                  </v-tooltip>
                </div>
                <div class="chart-label">{{ day.label }}</div>
              </div>
            </div>
          </div>
        </v-card>
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

// 时间段选择
const selectedPeriod = ref('7days')

// 统计数据 Mock
const statsData = {
  today: {
    totalCourses: 3,
    completedCourses: 1,
    studyDays: 1,
    studyHours: 2,
    coursesGrowth: 0,
    completedGrowth: 100,
    daysGrowth: 0,
    hoursGrowth: 50
  },
  '7days': {
    totalCourses: 5,
    completedCourses: 2,
    studyDays: 5,
    studyHours: 18,
    coursesGrowth: 25,
    completedGrowth: 100,
    daysGrowth: 150,
    hoursGrowth: 80
  },
  '30days': {
    totalCourses: 8,
    completedCourses: 3,
    studyDays: 18,
    studyHours: 72,
    coursesGrowth: 60,
    completedGrowth: 50,
    daysGrowth: 125,
    hoursGrowth: 140
  },
  all: {
    totalCourses: 12,
    completedCourses: 5,
    studyDays: 45,
    studyHours: 128,
    coursesGrowth: 20,
    completedGrowth: 25,
    daysGrowth: 80,
    hoursGrowth: 65
  }
}

// 当前统计数据
const currentStats = computed(() => {
  return statsData[selectedPeriod.value as keyof typeof statsData]
})

// 时间段文本
const selectedPeriodText = computed(() => {
  const map: Record<string, string> = {
    today: '今天',
    '7days': '最近7天',
    '30days': '最近30天',
    all: '全部时间'
  }
  return map[selectedPeriod.value] || '最近7天'
})

// 图表数据
const chartData = computed(() => {
  if (selectedPeriod.value === 'today') {
    return [
      { label: '0-6h', value: 0 },
      { label: '6-12h', value: 1 },
      { label: '12-18h', value: 0.5 },
      { label: '18-24h', value: 0.5 }
    ]
  } else if (selectedPeriod.value === '7days') {
    return [
      { label: '周一', value: 2 },
      { label: '周二', value: 3 },
      { label: '周三', value: 1.5 },
      { label: '周四', value: 4 },
      { label: '周五', value: 2.5 },
      { label: '周六', value: 3 },
      { label: '周日', value: 2 }
    ]
  } else if (selectedPeriod.value === '30days') {
    return [
      { label: '第1周', value: 12 },
      { label: '第2周', value: 18 },
      { label: '第3周', value: 22 },
      { label: '第4周', value: 20 }
    ]
  } else {
    return [
      { label: '1月', value: 24 },
      { label: '2月', value: 28 },
      { label: '3月', value: 32 },
      { label: '4月', value: 26 },
      { label: '5月', value: 18 }
    ]
  }
})

// 最大值
const maxValue = computed(() => {
  return Math.max(...chartData.value.map(d => d.value))
})

// 获取柱状图颜色
const getBarColor = (value: number) => {
  const percentage = (value / maxValue.value) * 100
  if (percentage >= 80) return '#4CAF50' // success
  if (percentage >= 60) return '#2196F3' // primary
  if (percentage >= 40) return '#FF9800' // warning
  return '#9E9E9E' // grey
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.stat-card {
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.chart-container {
  min-height: 300px;
}

.simple-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 240px;
  padding: 20px 0;
  border-bottom: 2px solid #E0E0E0;
}

.chart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 80px;
}

.chart-bar-container {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0 8px;
}

.chart-bar {
  width: 100%;
  min-height: 4px;
  border-radius: 4px 4px 0 0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.chart-bar:hover {
  opacity: 0.8;
}

.chart-label {
  font-size: 0.75rem;
  color: #757575;
  margin-top: 8px;
  text-align: center;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
