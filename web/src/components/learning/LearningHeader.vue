<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { USER_COURSE_STATE, USER_ROADMAP_STATE } from '@/constants/statusConstants';

const props = defineProps({
  selectedTab: {
    type: String,
    default: 'roadmaps'
  },
  selectedNavTab: {
    type: String,
    default: 'learning'
  },
  selectedStatus: {
    type: String,
    default: 'all'
  },
  searchQuery: {
    type: String,
    default: ''
  }
});

const emit = defineEmits([
  'update:selectedTab',
  'update:selectedNavTab', 
  'update:selectedStatus',
  'update:searchQuery'
]);

// 根据当前选中的tab返回对应的状态常量
const currentStateConstants = computed(() => {
  return props.selectedTab === 'roadmaps' ? USER_ROADMAP_STATE : USER_COURSE_STATE;
});

// 状态文本映射
const getStateText = (state) => {
  const stateTexts = {
    [0]: '未开始',  // NOT_STARTED
    [1]: '进行中',  // IN_PROGRESS  
    [2]: '已完成'   // COMPLETED
  };
  return stateTexts[state] || '未知状态';
};

const { t } = useI18n();
const router = useRouter();

// 本地计算属性
const localSelectedTab = computed({
  get: () => props.selectedTab,
  set: (value) => emit('update:selectedTab', value)
});

const localSelectedNavTab = computed({
  get: () => props.selectedNavTab,
  set: (value) => emit('update:selectedNavTab', value)
});

const localSelectedStatus = computed({
  get: () => props.selectedStatus,
  set: (value) => emit('update:selectedStatus', value)
});

const localSearchQuery = computed({
  get: () => props.searchQuery,
  set: (value) => emit('update:searchQuery', value)
});

// 导航处理
const handleNavigation = (route) => {
  router.push(route);
};
</script>

<template>
  <div class="learning-header mb-8">
    <!-- 页面标题区域 -->
    <v-row justify="start" class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="teal-lighten-4" size="40" class="mr-3">
              <v-icon icon="mdi-school" color="teal-darken-2" size="20"></v-icon>
            </v-avatar>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">{{ t('learning.title') }}</h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('learning.subtitle') }}</p>
            </div>
          </div>
          
          <!-- 导航栏 -->
          <div class="d-flex align-center">
            <v-btn-toggle v-model="localSelectedNavTab" variant="text" color="primary" class="nav-toggle">
              <v-btn value="learning" class="nav-btn" :class="{ 'nav-btn-active': localSelectedNavTab === 'learning' }">
                {{ t('learning.title') }}
              </v-btn>
              <v-btn value="career" class="nav-btn" @click="handleNavigation('/career')">
                {{ t('learning.careerCenter') }}
              </v-btn>
              <v-btn value="courses" class="nav-btn" @click="handleNavigation('/course/list')">
                {{ t('learning.courseCenter') }}
              </v-btn>
            </v-btn-toggle>
          </div>
        </div>
      </v-col>
    </v-row>

    <!-- 搜索和筛选区域 -->
    <v-row justify="start" align="center" class="mb-4">
      <v-col cols="4" class="d-flex justify-start">
        <v-btn-toggle v-model="localSelectedTab" variant="outlined" color="primary" rounded="lg" density="comfortable">
          <v-btn value="roadmaps" size="default">
            <v-icon icon="mdi-map" class="mr-2" size="16"></v-icon>
            {{ t('learning.roadmaps') }}
          </v-btn>
          <v-btn value="courses" size="default">
            <v-icon icon="mdi-book-multiple" class="mr-2" size="16"></v-icon>
            {{ t('learning.courses') }}
          </v-btn>
        </v-btn-toggle>
      </v-col>
      
      <v-col cols="5" class="d-flex justify-start">
        <!-- 状态筛选按钮 -->
        <v-btn-toggle v-model="localSelectedStatus" rounded="lg" color="grey-darken-2" variant="text"
          density="compact" mandatory class="status-filter">
          <v-btn value="all" size="small" class="me-1 rounded-lg text-body-2">
            <v-icon icon="mdi-format-list-bulleted" class="mr-1" size="14"></v-icon>
            {{ t('learning.all') }}
          </v-btn>
          <v-btn :value="currentStateConstants.NOT_STARTED" size="small" class="me-1 rounded-lg text-body-2">
            <v-icon icon="mdi-circle-outline" class="mr-1" size="14"></v-icon>
            {{ getStateText(currentStateConstants.NOT_STARTED) }}
          </v-btn>
          <v-btn :value="currentStateConstants.IN_PROGRESS" size="small" class="me-1 rounded-lg text-body-2">
            <v-icon icon="mdi-play-circle" class="mr-1" size="14"></v-icon>
            {{ getStateText(currentStateConstants.IN_PROGRESS) }}
          </v-btn>
          <v-btn :value="currentStateConstants.COMPLETED" size="small" class="me-1 rounded-lg text-body-2">
            <v-icon icon="mdi-check-circle" class="mr-1" size="14"></v-icon>
            {{ getStateText(currentStateConstants.COMPLETED) }}
          </v-btn>
        </v-btn-toggle>
      </v-col>
      <v-col cols="3" class="d-flex justify-end">
        <v-text-field 
          v-model="localSearchQuery"
          hide-details="auto" 
          density="compact" 
          class="search-input" 
          rounded="lg"
          :placeholder="t('learning.searchPlaceholder')" 
          variant="outlined">
          <template v-slot:prepend-inner>
            <v-icon icon="mdi-magnify" color="grey-lighten-1" size="18"></v-icon>
          </template>
        </v-text-field>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>

.nav-toggle {
  background: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px !important;
  border: 1px solid #e0e0e0 !important;
  overflow: hidden;
}

.nav-btn {
  padding: 8px 16px !important;
  font-size: 0.875rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  border-radius: 6px !important;
  margin: 2px !important;
  transition: all 0.2s ease !important;
  color: #666 !important;
}

.nav-btn:hover {
  background: rgba(25, 118, 210, 0.08) !important;
  color: #1976d2 !important;
}

.nav-btn-active {
  background: #1976d2 !important;
  color: white !important;
}

.nav-btn-active:hover {
  background: #1565c0 !important;
  color: white !important;
}

.status-filter {
  border-radius: 12px !important;
}

.search-input {
  max-width: 280px;
}

.search-input :deep(.v-field) {
  border-radius: 12px !important;
}
</style>