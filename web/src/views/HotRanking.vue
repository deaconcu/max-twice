<script setup>
import { ref, onMounted, inject } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { courseServiceV1, professionServiceV1 } from '@/services/api/v1/apiServiceV1';

const { t } = useI18n();

const showSnackbar = inject('showSnackbar');
const router = useRouter();
const route = useRoute();

// 选项卡相关
const activeTab = ref('courses');
const tabItems = ref([
  { key: 'courses', title: t('hotRanking.tabs.courses'), icon: 'mdi-book-multiple' },
  { key: 'professions', title: t('hotRanking.tabs.professions'), icon: 'mdi-briefcase-variant' }
]);

// 课程相关数据
const courses = ref([]);
const coursesLoading = ref(false);
const coursesSortBy = ref('total');

const courseSortOptions = [
  { value: 'total', title: t('hotRanking.sortOptions.total') },
  { value: 'learning', title: t('hotRanking.sortOptions.learning') },
  { value: 'subscription', title: t('hotRanking.sortOptions.subscription') }
];

// 职业相关数据
const professions = ref([]);
const professionsLoading = ref(false);
const professionsSortBy = ref('learning');

const professionSortOptions = [
  { value: 'learning', title: '学习人数' }
];

onMounted(() => {
  // 根据查询参数设置默认选项卡
  if (route.query.tab === 'professions') {
    activeTab.value = 'professions';
  }
  
  loadHotCoursesRanking();
  loadHotProfessionsRanking();
});

// 课程相关方法
const loadHotCoursesRanking = async () => {
  try {
    coursesLoading.value = true;
    console.log("加载热门课程排行榜");
    const response = await courseServiceV1.getCoursesRanking();
    
    if (response.code === 401) {
      console.log('未登录');
      courses.value = [];
    } else if (response.code === 200) {
      console.log('获取热门课程排行榜数据:', response.data);
      courses.value = response.data || [];
    } else {
      console.error('获取课程排行榜失败:', response);
      showSnackbar("获取课程排行榜失败，请重试！", "error");
      courses.value = [];
    }
  } catch (error) {
    console.error('Error loading hot courses ranking:', error);
    showSnackbar("网络错误，请重试！", "error");
    courses.value = [];
  } finally {
    coursesLoading.value = false;
  }
};

const sortCourses = () => {
  const sortedCourses = [...courses.value];
  
  switch (coursesSortBy.value) {
    case 'learning':
      sortedCourses.sort((a, b) => (b.learnerCount || 0) - (a.learnerCount || 0));
      break;
    case 'subscription':
      sortedCourses.sort((a, b) => (b.subscriptionCount || 0) - (a.subscriptionCount || 0));
      break;
    case 'total':
    default:
      sortedCourses.sort((a, b) => 
        ((b.learnerCount || 0) + (b.subscriptionCount || 0)) - 
        ((a.learnerCount || 0) + (a.subscriptionCount || 0))
      );
      break;
  }
  
  courses.value = sortedCourses;
};

const openCourse = (courseId) => {
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href;
  window.open(url, '_blank');
};

// 职业相关方法
const loadHotProfessionsRanking = async () => {
  try {
    professionsLoading.value = true;
    console.log("加载热门职业排行榜");
    const response = await professionServiceV1.getHotProfessions(100);
    
    if (response.code === 401) {
      console.log('未登录');
      professions.value = [];
    } else if (response.code === 200) {
      console.log('获取热门职业排行榜数据:', response.data);
      professions.value = response.data || [];
    } else {
      console.error('获取职业排行榜失败:', response);
      showSnackbar("获取职业排行榜失败，请重试！", "error");
      professions.value = [];
    }
  } catch (error) {
    console.error('Error loading hot professions ranking:', error);
    showSnackbar("网络错误，请重试！", "error");
    professions.value = [];
  } finally {
    professionsLoading.value = false;
  }
};

const sortProfessions = () => {
  const sortedProfessions = [...professions.value];
  
  switch (professionsSortBy.value) {
    case 'learning':
    default:
      sortedProfessions.sort((a, b) => (b.learnerCount || 0) - (a.learnerCount || 0));
      break;
  }
  
  professions.value = sortedProfessions;
};

const openProfession = (professionId) => {
  const url = router.resolve({ path: '/roadmap', query: { professionId: professionId } }).href;
  window.open(url, '_blank');
};

// 通用方法
const goBack = () => {
  if (activeTab.value === 'courses') {
    router.push('/course/list');
  } else {
    router.push('/career');
  }
};

const getRankIcon = (index) => {
  if (index === 0) return 'mdi-trophy';
  if (index === 1) return 'mdi-medal';
  if (index === 2) return 'mdi-medal-outline';
  return null;
};

const getRankColor = (index) => {
  if (index === 0) return 'amber';
  if (index === 1) return 'amber';
  if (index === 2) return 'amber';
  return 'grey-lighten-3';
};

// 根据当前选项卡获取数据
const getCurrentData = () => {
  return activeTab.value === 'courses' ? courses.value : professions.value;
};

const getCurrentLoading = () => {
  return activeTab.value === 'courses' ? coursesLoading.value : professionsLoading.value;
};

const getCurrentSortBy = () => {
  return activeTab.value === 'courses' ? coursesSortBy.value : professionsSortBy.value;
};

const getCurrentSortOptions = () => {
  return activeTab.value === 'courses' ? courseSortOptions : professionSortOptions;
};

const handleSort = () => {
  if (activeTab.value === 'courses') {
    sortCourses();
  } else {
    sortProfessions();
  }
};

const handleItemClick = (item) => {
  if (activeTab.value === 'courses') {
    openCourse(item.id);
  } else {
    openProfession(item.id);
  }
};
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <!-- 页面头部 -->
      <v-col cols="12" class="mb-4">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center">
            <v-btn
              @click="goBack"
              icon="mdi-arrow-left"
              variant="text"
              color="grey-darken-2"
              class="mr-3"
            ></v-btn>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">
                热门排行榜
              </h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                <v-icon icon="mdi-fire" color="primary" size="16" class="mr-1"></v-icon>
                {{ activeTab === 'courses' ? '基于学习人数和收藏人数的综合排名' : '基于学习人数的职业热度排名' }}
              </p>
            </div>
          </div>
          
          <!-- 排序选择器 -->
          <div class="d-flex align-center">
            <span class="text-body-2 text-grey-darken-2 mr-3">排序方式：</span>
            <v-select
              :model-value="getCurrentSortBy()"
              :items="getCurrentSortOptions()"
              item-title="title"
              item-value="value"
              variant="outlined"
              density="compact"
              hide-details
              style="width: 150px;"
              @update:modelValue="(value) => {
                if (activeTab === 'courses') {
                  coursesSortBy = value;
                } else {
                  professionsSortBy = value;
                }
                handleSort();
              }"
            ></v-select>
          </div>
        </div>
      </v-col>

      <!-- 左侧导航栏 -->
      <v-col cols="3" class="pr-6">
        <v-card flat color="grey-lighten-5" rounded="xl" class="sticky-nav">
          <v-card-text class="pa-4">
            <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
              <v-icon icon="mdi-chart-line" color="primary" size="18" class="mr-2"></v-icon>
              排行榜分类
            </h3>
            
            <v-list bg-color="transparent" class="pa-0">
              <v-list-item
                v-for="item in tabItems"
                :key="item.key"
                :value="item.key"
                :class="[
                  'nav-item ma-1 rounded-lg',
                  activeTab === item.key ? 'nav-item-active' : 'nav-item-inactive'
                ]"
                @click="activeTab = item.key"
              >
                <template v-slot:prepend>
                  <v-avatar 
                    :color="activeTab === item.key ? 'primary' : 'grey-lighten-2'" 
                    size="32" 
                    class="mr-3"
                  >
                    <v-icon 
                      :icon="item.icon" 
                      :color="activeTab === item.key ? 'white' : 'grey-darken-2'" 
                      size="16"
                    ></v-icon>
                  </v-avatar>
                </template>

                <v-list-item-title 
                  class="font-weight-medium"
                  :class="activeTab === item.key ? 'text-primary' : 'text-grey-darken-3'"
                >
                  {{ item.title }}
                </v-list-item-title>
                
                <v-list-item-subtitle class="text-caption">
                  {{ item.key === 'courses' ? `${courses.length} 个课程` : `${professions.length} 个职业` }}
                </v-list-item-subtitle>

                <template v-slot:append>
                  <v-icon 
                    icon="mdi-chevron-right" 
                    :color="activeTab === item.key ? 'primary' : 'grey-lighten-1'" 
                    size="16"
                  ></v-icon>
                </template>
              </v-list-item>
            </v-list>
            
            <!-- 统计信息 -->
            <div class="mt-6 pa-3 rounded-lg bg-white">
              <h4 class="text-body-1 font-weight-bold text-grey-darken-4 mb-2">
                📊 统计概览
              </h4>
              <div class="d-flex justify-space-between text-body-2 mb-1">
                <span class="text-grey-darken-2">热门课程</span>
                <span class="font-weight-bold text-primary">{{ courses.length }}</span>
              </div>
              <div class="d-flex justify-space-between text-body-2">
                <span class="text-grey-darken-2">热门职业</span>
                <span class="font-weight-bold text-success">{{ professions.length }}</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- 右侧内容区域 -->
      <v-col cols="9">
        <!-- 加载状态 -->
        <div v-if="getCurrentLoading()" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
          <p class="text-body-1 text-grey-darken-2 mt-4">正在加载排行榜...</p>
        </div>

        <!-- 排行榜内容 -->
        <div v-else-if="getCurrentData().length > 0">
          
          <!-- 前三名特殊显示 -->
          <v-card flat color="grey-lighten-5" rounded="xl" class="mb-6">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4 text-center">
                🏆 {{ activeTab === 'courses' ? '热门课程' : '热门职业' }}三甲 🏆
              </h3>
              <v-row class="mb-2">
                <v-col 
                  v-for="(item, index) in getCurrentData().slice(0, 3)" 
                  :key="item.id"
                  cols="4"
                >
                  <div 
                    class="top-card text-center pa-4 rounded-lg"
                    :class="{
                      'winner-card': index === 0,
                      'second-place-card': index === 1,
                      'third-place-card': index === 2
                    }"
                    @click="handleItemClick(item)"
                  >
                    <v-avatar :color="getRankColor(index)" size="40" class="mb-3">
                      <v-icon 
                        v-if="getRankIcon(index)"
                        :icon="getRankIcon(index)" 
                        color="white" 
                        size="20"
                      ></v-icon>
                      <span v-else class="text-white font-weight-bold">{{ index + 1 }}</span>
                    </v-avatar>
                    
                    <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">
                      {{ item.name }}
                    </h4>
                    
                    <div class="text-body-2 text-grey-darken-2 mb-3">
                      {{ item.description || '暂无描述' }}
                    </div>
                    
                    <!-- 课程统计 -->
                    <div v-if="activeTab === 'courses'" class="d-flex justify-space-around">
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-primary">
                          {{ (item.learnerCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">学习</div>
                      </div>
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-success">
                          {{ (item.subscriptionCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">收藏</div>
                      </div>
                    </div>
                    
                    <!-- 职业统计 -->
                    <div v-else class="text-center">
                      <div class="text-h6 font-weight-bold text-primary">
                        {{ (item.learnerCount || 0).toLocaleString() }}
                      </div>
                      <div class="text-caption text-grey-darken-1">人正在学习</div>
                    </div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- 完整排行榜列表 -->
          <v-card flat color="grey-lighten-5" rounded="xl">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
                完整排行榜 (Top {{ getCurrentData().length }})
              </h3>
              
              <v-list bg-color="transparent" class="pa-0">
                <v-list-item
                  v-for="(item, index) in getCurrentData()"
                  :key="item.id"
                  class="ranking-item ma-1 pa-4 rounded-lg"
                  :class="index < 3 ? 'top-three-item' : 'regular-item'"
                  @click="handleItemClick(item)"
                >
                  <template v-slot:prepend>
                    <div class="rank-number mr-4 text-center" style="min-width: 40px;">
                      <v-avatar 
                        v-if="index < 3"
                        :color="getRankColor(index)" 
                        size="32"
                      >
                        <v-icon 
                          v-if="getRankIcon(index)"
                          :icon="getRankIcon(index)" 
                          color="white" 
                          size="16"
                        ></v-icon>
                      </v-avatar>
                      <div 
                        v-else
                        class="text-h6 font-weight-bold"
                        :class="index < 10 ? 'text-grey-darken-2' : 'text-grey-lighten-1'"
                      >
                        {{ index + 1 }}
                      </div>
                    </div>
                  </template>

                  <v-list-item-title class="text-h6 font-weight-medium">
                    {{ item.name }}
                  </v-list-item-title>
                  
                  <v-list-item-subtitle class="text-body-2 mt-1">
                    {{ item.description || '暂无描述' }}
                  </v-list-item-subtitle>

                  <template v-slot:append>
                    <!-- 课程数据显示 -->
                    <div v-if="activeTab === 'courses'" class="d-flex align-center">
                      <div class="text-center mr-6">
                        <div class="text-h6 font-weight-bold text-primary">
                          {{ (item.learnerCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-school" size="12" class="mr-1"></v-icon>
                          学习
                        </div>
                      </div>
                      
                      <div class="text-center mr-6">
                        <div class="text-h6 font-weight-bold text-success">
                          {{ (item.subscriptionCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-heart" size="12" class="mr-1"></v-icon>
                          收藏
                        </div>
                      </div>
                      
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-grey-darken-2">
                          {{ ((item.learnerCount || 0) + (item.subscriptionCount || 0)).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-trending-up" size="12" class="mr-1"></v-icon>
                          总计
                        </div>
                      </div>
                    </div>
                    
                    <!-- 职业数据显示 -->
                    <div v-else class="d-flex align-center">
                      <div class="text-center mr-6">
                        <div class="text-h6 font-weight-bold text-primary">
                          {{ (item.learnerCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-school" size="12" class="mr-1"></v-icon>
                          学习人数
                        </div>
                      </div>
                      
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-grey-darken-2">
                          {{ item.price || '面议' }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-currency-usd" size="12" class="mr-1"></v-icon>
                          薪资水平
                        </div>
                      </div>
                    </div>
                    
                    <v-icon icon="mdi-chevron-right" color="grey-lighten-1" class="ml-4"></v-icon>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon 
            :icon="activeTab === 'courses' ? 'mdi-chart-line-stacked' : 'mdi-briefcase-search'" 
            size="64" 
            color="grey-lighten-1" 
            class="mb-4"
          ></v-icon>
          <h3 class="text-h5 font-weight-medium text-grey-darken-2 mb-2">暂无排行榜数据</h3>
          <p class="text-body-1 text-grey-darken-1">
            等待更多{{ activeTab === 'courses' ? '课程' : '职业' }}加入热门排行榜
          </p>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.sticky-nav {
  position: sticky;
  top: 20px;
}

.nav-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.nav-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.2) !important;
}

.nav-item-active {
  background: rgba(25, 118, 210, 0.08) !important;
  border-color: rgba(25, 118, 210, 0.2) !important;
}

.nav-item-inactive {
  background: white !important;
}

.top-card {
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.top-card:hover {
  transform: translateY(-2px);
  border-color: rgba(25, 118, 210, 0.2);
}

.winner-card {
  border: 2px solid #ffc107;
  box-shadow: 0 0 8px rgba(255, 193, 7, 0.2);
}

.second-place-card {
  border: 2px solid #c0c0c0;
  box-shadow: 0 0 6px rgba(192, 192, 192, 0.15);
}

.third-place-card {
  border: 2px solid #cd7f32;
  box-shadow: 0 0 6px rgba(205, 127, 50, 0.15);
}

.ranking-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.ranking-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.3) !important;
}

.top-three-item {
  background: rgba(25, 118, 210, 0.02) !important;
  border-color: rgba(25, 118, 210, 0.08) !important;
}

.regular-item {
  background: white !important;
}

/* 改善字体渲染 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 文字对比度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

h1, h2, h3, h4, h5, h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 为v-card添加细节 */
.v-card {
  border: 1px solid rgba(0, 0, 0, 0.04);
}
</style>