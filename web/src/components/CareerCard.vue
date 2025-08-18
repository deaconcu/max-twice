<template>
  <v-card 
    flat 
    hover 
    class="career-card h-100" 
    rounded="lg"
    @click="$emit('click')"
    :ripple="false">
    <div class="d-flex flex-column h-100">
      <!-- 卡片头部 -->
      <v-card-item class="pb-2">
        <div class="d-flex align-center mb-3">
          <v-avatar color="grey-lighten-3" size="48" class="mr-3">
            <v-icon :icon="career.icon" :color="career.iconColor || 'primary'" size="24"></v-icon>
          </v-avatar>
          <div class="flex-grow-1">
            <v-card-title class="pa-0 text-subtitle-1 font-weight-bold text-grey-darken-4">
              {{ career.name}}
            </v-card-title>
            <div class="d-flex align-center mt-1">
              <v-chip 
                color="blue-lighten-4" 
                variant="flat" 
                size="x-small" 
                class="mr-2">
                <span class="text-blue-darken-2">{{ getCategoryName(career.mainCategory) }}</span>
              </v-chip>
              <v-chip 
                v-if="career.subCategory && career.subCategory !== '0'"
                color="green-lighten-4" 
                variant="flat" 
                size="x-small" 
                class="mr-2">
                <span class="text-green-darken-2">{{ getSubCategoryNameById(career.subCategory) }}</span>
              </v-chip>
            </div>
          </div>
        </div>
      </v-card-item>

      <!-- 描述 -->
      <v-card-text class="pt-0 pb-2 flex-grow-1">
        <p class="text-body-2 text-grey-darken-2 mb-3">
          {{ career.description }}
        </p>

        <!-- 薪资范围 -->
        <div class="d-flex align-center mb-3">
          <v-icon icon="mdi-currency-usd" color="success" size="16" class="mr-2"></v-icon>
          <span class="text-body-2 font-weight-medium text-success">{{ career.price }}</span>
        </div>

        <!-- 核心技能 -->
        <div class="mb-3">
          <div class="text-caption text-grey-darken-1 mb-2">核心技能</div>
          <div class="d-flex flex-wrap gap-1">
            <v-chip 
              v-for="skill in career.skills.split(',').slice(0, 4)" 
              :key="skill"
              variant="outlined" 
              size="x-small"
              color="grey-darken-1">
              {{ skill }}
            </v-chip>
            <v-chip 
              v-if="career.skills.split(',').length > 4"
              variant="outlined" 
              size="x-small"
              color="grey-darken-1">
              +{{ career.skills.split(',').length - 4 }}
            </v-chip>
          </div>
        </div>
      </v-card-text>

      <!-- 卡片底部统计 -->
      <v-card-actions class="px-4 py-3 mt-auto border-t-sm">
        <div class="d-flex align-center justify-space-between w-100">
          <div class="d-flex align-center">
            <v-icon icon="mdi-book-multiple-outline" color="primary" size="16" class="mr-1"></v-icon>
            <span class="text-caption text-grey-darken-2">11 门课程</span>
          </div>
          <div class="d-flex align-center">
            <v-icon icon="mdi-account-group-outline" color="grey-darken-2" size="16" class="mr-1"></v-icon>
            <span class="text-caption text-grey-darken-2">{{ formatLearnerCount(career.learnerCount) }} 人学习</span>
          </div>
        </div>
      </v-card-actions>

      <!-- 悬浮操作按钮 -->
      <div class="card-overlay">
        <v-btn 
          color="primary" 
          variant="flat" 
          rounded="lg"
          size="small"
          class="explore-btn">
          <v-icon icon="mdi-arrow-right" class="mr-1" size="16"></v-icon>
          探索职业
        </v-btn>
      </div>
    </div>
  </v-card>
</template>

<script setup>
const props = defineProps({
  career: {
    type: Object,
    required: true
  },
  getCategoryName: {
    type: Function,
    default: () => () => ''
  },
  getSubCategoryNameById: {
    type: Function, 
    default: () => () => ''
  }
});

const emit = defineEmits(['click']);

// 获取类别颜色
const getCategoryColor = (category) => {
  // 统一使用灰色，不再按类别区分颜色
  return 'grey-lighten-3';
};

// 获取难度名称
const getDifficultyName = (difficulty) => {
  const difficultyMap = {
    'beginner': '初级',
    'intermediate': '中级',
    'advanced': '高级',
  };
  return difficultyMap[difficulty] || '未知';
};

// 获取难度颜色 - 保留语义化颜色
const getDifficultyColor = (difficulty) => {
  const colorMap = {
    'beginner': 'success',
    'intermediate': 'warning', 
    'advanced': 'error',
  };
  return colorMap[difficulty] || 'grey';
};

// 格式化学习人数
const formatLearnerCount = (count) => {
  if (count === undefined || count === null) return '0';
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万';
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'K';
  }
  return count.toString();
};
</script>

<style scoped>
.career-card {
  border: 1px solid #f0f0f0 !important;
  transition: all 0.3s ease !important;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.career-card:hover {
  transform: translateY(-4px) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  border-color: #e0e0e0 !important;
}

.career-card:hover .card-overlay {
  opacity: 1;
  visibility: visible;
}

.card-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
}

.explore-btn {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  text-transform: none !important;
  font-weight: 500 !important;
}

.gap-1 {
  gap: 4px;
}

.border-t-sm {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
</style>
