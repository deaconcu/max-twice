<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// Props
const props = defineProps({
  selectedNavTab: {
    type: String,
    default: 'courses'
  }
});

// Emits
const emit = defineEmits([
  'update:selectedNavTab',
  'search',
  'openCreateDialog'
]);

// Computed properties for v-model
const selectedNavTabModel = computed({
  get: () => props.selectedNavTab,
  set: (value) => emit('update:selectedNavTab', value)
});

// 处理搜索
const handleSearch = () => {
  emit('search');
};

// 处理打开创建对话框
const handleOpenCreateDialog = () => {
  emit('openCreateDialog');
};

// 处理导航
const handleNavigation = (route) => {
  // 这里可以使用 router.push 或者通过 emit 让父组件处理
  window.location.href = route;
};
</script>

<template>
  <div class="mb-8">
    <!-- 页面标题和导航 -->
    <v-row justify="start" class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="teal-lighten-4" size="40" class="mr-3">
              <v-icon icon="mdi-book-multiple" color="teal-darken-2" size="20"></v-icon>
            </v-avatar>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">{{ t('course.center') }}</h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">探索知识，成就未来</p>
            </div>
          </div>
          
          <!-- 导航栏 -->
          <div class="d-flex align-center">
            <v-btn-toggle :model-value="selectedNavTab" @update:model-value="selectedNavTabModel = $event" variant="text" color="primary" class="nav-toggle">
              <v-btn value="learning" class="nav-btn" @click="handleNavigation('/learning')">
                正在学习
              </v-btn>
              <v-btn value="career" class="nav-btn" @click="handleNavigation('/career')">
                职业中心
              </v-btn>
              <v-btn value="courses" class="nav-btn" :class="{ 'nav-btn-active': selectedNavTab === 'courses' }">
                {{ t('course.center') }}
              </v-btn>
            </v-btn-toggle>
          </div>
        </div>
      </v-col>
    </v-row>
    
    <!-- 搜索和创建区域 -->
    <v-row justify="start" align="center">
      <v-col cols="6">
        <v-text-field 
          hide-details="auto" 
          density="compact" 
          class="search-input" 
          rounded="lg"
          :placeholder="t('course.search')" 
          variant="outlined" 
          @click:append-inner="handleSearch">
          <template v-slot:prepend-inner>
            <v-icon icon="mdi-magnify" color="grey-lighten-1" size="18"></v-icon>
          </template>
        </v-text-field>
      </v-col>
      <v-col class="d-flex justify-end">
        <v-btn 
          @click="handleOpenCreateDialog" 
          variant="flat" 
          color="grey-darken-2" 
          class="px-4 text-white" 
          rounded="lg"
          density="default">
          <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
          {{ t('course.createNew') }}
        </v-btn>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.search-input {
  width: 100%;
}

/* 导航栏样式 */
.nav-toggle {
  background: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px !important;
  border: 1px solid #e0e0e0 !important;
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
</style>