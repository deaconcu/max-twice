<script setup>
import { ref, onMounted, inject } from 'vue';
import UserStatsComponent from '@/components/user/UserStatsComponent.vue';

const showSnackbar = inject('showSnackbar');

// 统计数据相关状态
const selectedStatsPeriod = ref('7');
const statsData = ref(null);
const statsLoading = ref(false);
const statsError = ref(null);

// 加载统计数据
const loadStats = async () => {
  try {
    statsLoading.value = true;
    statsError.value = null;
    
    // TODO: 实现统计数据加载 API
    // const response = await statsServiceV1.getUserStats(selectedStatsPeriod.value);
    // if (response.code === 200) {
    //   statsData.value = response.data;
    // }
    
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 1000));
    statsData.value = {
      period: selectedStatsPeriod.value,
      totalViews: 12580,
      totalLikes: 456,
      totalComments: 89,
      totalShares: 23
    };
    
  } catch (error) {
    console.error('Error loading stats:', error);
    statsError.value = '加载统计数据失败';
    showSnackbar('加载统计数据失败');
  } finally {
    statsLoading.value = false;
  }
};

// 监听时间段变化
const onPeriodChange = (period) => {
  selectedStatsPeriod.value = period;
  loadStats();
};

onMounted(() => {
  loadStats();
});
</script>

<template>
  <div>
    <!-- 提示信息 -->
    <div class="mb-5 px-3 text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">查看您的学习和内容创作统计数据</span>
    </div>

    <!-- 时间段选择 -->
    <div class="mb-6">
      <v-chip-group 
        v-model="selectedStatsPeriod" 
        mandatory 
        color="primary"
        @update:model-value="onPeriodChange"
      >
        <v-chip value="7" size="small">最近7天</v-chip>
        <v-chip value="30" size="small">最近30天</v-chip>
        <v-chip value="90" size="small">最近3个月</v-chip>
        <v-chip value="365" size="small">最近一年</v-chip>
      </v-chip-group>
    </div>

    <!-- 加载状态 -->
    <div v-if="statsLoading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
      <p class="text-body-2 text-grey-darken-1 mt-3">加载统计数据中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="statsError" class="text-center py-8">
      <v-icon icon="mdi-alert-circle" color="error" size="48" class="mb-3"></v-icon>
      <p class="text-body-2 text-error mb-3">{{ statsError }}</p>
      <v-btn variant="outlined" color="primary" @click="loadStats">重新加载</v-btn>
    </div>

    <!-- 统计数据 -->
    <div v-else-if="statsData">
      <!-- 使用现有的 UserStatsComponent -->
      <UserStatsComponent />
      
      <!-- 详细统计卡片 -->
      <v-row class="mt-6">
        <v-col cols="12" sm="6" md="3">
          <v-card class="text-center pa-4" elevation="0" border>
            <v-icon icon="mdi-eye" size="32" color="blue" class="mb-2"></v-icon>
            <div class="text-h6 font-weight-bold">{{ statsData.totalViews.toLocaleString() }}</div>
            <div class="text-body-2 text-grey">总浏览量</div>
          </v-card>
        </v-col>
        
        <v-col cols="12" sm="6" md="3">
          <v-card class="text-center pa-4" elevation="0" border>
            <v-icon icon="mdi-heart" size="32" color="red" class="mb-2"></v-icon>
            <div class="text-h6 font-weight-bold">{{ statsData.totalLikes.toLocaleString() }}</div>
            <div class="text-body-2 text-grey">获得点赞</div>
          </v-card>
        </v-col>
        
        <v-col cols="12" sm="6" md="3">
          <v-card class="text-center pa-4" elevation="0" border>
            <v-icon icon="mdi-comment" size="32" color="green" class="mb-2"></v-icon>
            <div class="text-h6 font-weight-bold">{{ statsData.totalComments.toLocaleString() }}</div>
            <div class="text-body-2 text-grey">收到评论</div>
          </v-card>
        </v-col>
        
        <v-col cols="12" sm="6" md="3">
          <v-card class="text-center pa-4" elevation="0" border>
            <v-icon icon="mdi-share" size="32" color="orange" class="mb-2"></v-icon>
            <div class="text-h6 font-weight-bold">{{ statsData.totalShares.toLocaleString() }}</div>
            <div class="text-body-2 text-grey">被分享次数</div>
          </v-card>
        </v-col>
      </v-row>

      <!-- 趋势图表区域 -->
      <v-card class="mt-6 pa-6" elevation="0" border>
        <h3 class="text-h6 font-weight-bold mb-4">
          <v-icon icon="mdi-chart-line" class="mr-2"></v-icon>
          数据趋势
        </h3>
        
        <!-- 这里可以集成图表库，如 Chart.js 或 ECharts -->
        <div class="text-center py-8 text-grey">
          <v-icon icon="mdi-chart-areaspline" size="64" class="mb-4"></v-icon>
          <p class="text-body-1">图表功能开发中...</p>
          <p class="text-body-2">将显示{{ selectedStatsPeriod }}天的数据趋势</p>
        </div>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
.v-card {
  box-shadow: none !important;
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
  transform: translateY(-2px);
}
</style>