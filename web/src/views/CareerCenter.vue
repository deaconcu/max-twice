<script setup>
import { ref, onMounted, inject } from 'vue';
import { useRouter } from 'vue-router';
import { learnService } from '@/services/learnService';
import CareerCard from '@/components/CareerCard.vue';
import CategorySelector from '@/components/CategorySelector.vue';

const showSnackbar = inject('showSnackbar');
const router = useRouter();

// 状态管理
const careers = ref([]);
const filteredCareers = ref([]);
const loading = ref(true);
const searchText = ref('');
const selectedCategory = ref('all');
const selectedDifficulty = ref('all');
const selectedNavTab = ref('career'); // 导航栏当前选中项

// 热门职业数据
const hotProfessions = ref([]);

// 动态加载的分类数据
const categories = ref([]);
const categoryMapping = ref([]);

// 三级目录相关状态
const activeFirstLvl = ref(-1); // 当前选中的一级分类 (基于categories)
const activeSecondLvl = ref(-1); // 当前选中的二级分类
const currentCareers = ref([]); // 当前显示的职业列表

// 分页相关状态
const displayedCareers = ref([]); // 实际显示的职业列表
const careerPage = ref(1); // 当前页码
const careerPageSize = ref(20); // 每页显示数量
const hasMoreCareers = ref(false); // 是否还有更多职业
const loadingMore = ref(false); // 是否正在加载更多
const lastId = ref(0); // 最后一个职业的ID，用于分页

// 动态加载职业类别数据
const loadProfessionCategories = async () => {
  try {
    const response = await learnService.getProfessionCategories();
    console.log('Loaded profession categories:', response.data);
    
    if (response.data) {
      categories.value = response.data.mainCategories || [];
      categoryMapping.value = response.data.categoryMapping || [];
    }
  } catch (error) {
    console.error('Failed to load profession categories:', error);
    // 如果API调用失败，可以提供默认值
  }
};

// 工具函数：根据主分类ID获取子分类列表
const getSubcategoriesByMainCategory = (mainCategoryId) => {
  const mapping = categoryMapping.value.find(
    item => item.mainCategoryId === mainCategoryId
  );
  return mapping?.subcategories || [];
};

// 工具函数：获取子分类ID
const getSubCategoryId = (mainCategoryId, subcategoryIndex) => {
  const subcategories = getSubcategoriesByMainCategory(mainCategoryId);
  return subcategories[subcategoryIndex]?.value || null;
};

// 工具函数：根据ID获取分类名称
const getCategoryName = (categoryId) => {
  const category = categories.value.find(cat => cat.value === categoryId);
  return category?.title || '';
};

// 工具函数：根据主分类ID和子分类ID获取子分类名称
const getSubCategoryName = (mainCategoryId, subCategoryId) => {
  const subcategories = getSubcategoriesByMainCategory(mainCategoryId);
  const subcategory = subcategories.find(sub => sub.value === subCategoryId);
  return subcategory?.title || '';
};

// 工具函数：根据子分类ID获取子分类名称（用于CareerCard）
const getSubCategoryNameById = (subCategoryId) => {
  // 遍历所有分类映射来查找子分类
  for (const mapping of categoryMapping.value) {
    const subcategory = mapping.subcategories.find(sub => sub.value === subCategoryId);
    if (subcategory) {
      return subcategory.title;
    }
  }
  return '';
};

// 当前查询参数
const currentQueryParams = ref({
  type: 'all', // 'all', 'mainCategory', 'subCategory'
  mainCategory: null,
  subCategory: null
});

// 申请职业对话框
const showApplicationDialog = ref(false);
const applicationValid = ref(false);
const submitting = ref(false);
const newCareerApplication = ref({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null,
  skills: ''
});

// 随机图标数组
const availableIcons = [
  'mdi-briefcase',
  'mdi-laptop',
  'mdi-palette',
  'mdi-bullhorn',
  'mdi-cash-multiple',
  'mdi-account-tie',
  'mdi-school',
  'mdi-heart',
  'mdi-star',
  'mdi-lightbulb',
  'mdi-rocket',
  'mdi-diamond',
  'mdi-shield-star',
  'mdi-crown',
  'mdi-trophy',
  'mdi-medal',
  'mdi-compass',
  'mdi-target',
  'mdi-puzzle',
  'mdi-gear'
];

// 随机颜色数组
const availableColors = [
  'primary',
  'secondary',
  'success',
  'warning',
  'error',
  'info',
  'purple',
  'indigo',
  'blue',
  'cyan',
  'teal',
  'green',
  'orange',
  'deep-orange',
  'brown',
  'blue-grey',
  'pink',
  'red',
  'amber',
  'lime'
];

// 为职业数据添加随机图标和颜色
const addRandomIconsToCareers = (careerList) => {
  return careerList.map(career => ({
    ...career,
    icon: career.icon || availableIcons[Math.floor(Math.random() * availableIcons.length)],
    iconColor: career.iconColor || availableColors[Math.floor(Math.random() * availableColors.length)]
  }));
};

// 难度等级
const difficulties = ref([
  { value: 'all', title: '全部难度' },
  { value: 'beginner', title: '初级' },
  { value: 'intermediate', title: '中级' },
  { value: 'advanced', title: '高级' },
]);

// 模拟职业数据

// 加载职业数据
const loadCareerData = async (reset = true) => {
  try {
    if (reset) {
      loading.value = true;
      lastId.value = 0;
      currentQueryParams.value = { type: 'all', mainCategory: null, subCategory: null };
    }
    
    // 调用API获取所有职业数据
    const params = { lastId: lastId.value };
    const response = await learnService.getProfessionList(params);
    console.log("response", response.data);
    const newCareers = addRandomIconsToCareers(response.data || []);
    
    if (reset) {
      careers.value = newCareers;
      filteredCareers.value = newCareers;
      currentCareers.value = newCareers;
      displayedCareers.value = newCareers;
    } else {
      careers.value = [...careers.value, ...newCareers];
      filteredCareers.value = [...filteredCareers.value, ...newCareers];
      currentCareers.value = [...currentCareers.value, ...newCareers];
      displayedCareers.value = [...displayedCareers.value, ...newCareers];
    }
    
    // 更新最后ID和是否有更多数据
    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id;
      hasMoreCareers.value = newCareers.length === careerPageSize.value;
    } else {
      hasMoreCareers.value = false;
    }
  } catch (error) {
    console.error('加载职业数据失败:', error);
    showSnackbar('加载职业数据失败', 'error');
    // 如果API失败，设置为空数组
    if (reset) {
      careers.value = [];
      filteredCareers.value = [];
      currentCareers.value = [];
      displayedCareers.value = [];
    }
    hasMoreCareers.value = false;
  } finally {
    if (reset) {
      loading.value = false;
    }
  }
};

// 根据主分类获取职业数据
const loadCareersByMainCategory = async (mainCategoryId, reset = true) => {
  try {
    if (reset) {
      loading.value = true;
      lastId.value = 0;
      currentQueryParams.value = { type: 'mainCategory', mainCategory: mainCategoryId, subCategory: null };
    }
    
    const response = await learnService.getProfessionByMainCategory(mainCategoryId, lastId.value);
    const newCareers = addRandomIconsToCareers(response.data || []);
    
    if (reset) {
      currentCareers.value = newCareers;
      displayedCareers.value = newCareers;
    } else {
      currentCareers.value = [...currentCareers.value, ...newCareers];
      displayedCareers.value = [...displayedCareers.value, ...newCareers];
    }
    
    // 更新最后ID和是否有更多数据
    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id;
      hasMoreCareers.value = newCareers.length === careerPageSize.value;
    } else {
      hasMoreCareers.value = false;
    }
    
  } catch (error) {
    console.error('加载主分类职业失败:', error);
    showSnackbar('加载职业数据失败', 'error');
    // API失败时设置为空数组
    currentCareers.value = [];
    displayedCareers.value = [];
    hasMoreCareers.value = false;
  } finally {
    if (reset) {
      loading.value = false;
    }
  }
};

// 根据子分类获取职业数据
const loadCareersBySubCategory = async (mainCategoryId, subCategoryId, reset = true) => {
  try {
    if (reset) {
      loading.value = true;
      lastId.value = 0;
      currentQueryParams.value = { type: 'subCategory', mainCategory: mainCategoryId, subCategory: subCategoryId };
    }
    
    const response = await learnService.getProfessionBySubCategory(mainCategoryId, subCategoryId, lastId.value);
    const newCareers = addRandomIconsToCareers(response.data || []);
    
    if (reset) {
      currentCareers.value = newCareers;
      displayedCareers.value = newCareers;
    } else {
      currentCareers.value = [...currentCareers.value, ...newCareers];
      displayedCareers.value = [...displayedCareers.value, ...newCareers];
    }
    
    // 更新最后ID和是否有更多数据
    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id;
      hasMoreCareers.value = newCareers.length === careerPageSize.value;
    } else {
      hasMoreCareers.value = false;
    }
    
  } catch (error) {
    console.error('加载子分类职业失败:', error);
    showSnackbar('加载职业数据失败', 'error');
    // API失败时设置为空数组
    currentCareers.value = [];
    displayedCareers.value = [];
    hasMoreCareers.value = false;
  } finally {
    if (reset) {
      loading.value = false;
    }
  }
};

// 搜索和筛选
const filterCareers = () => {
  let filtered = careers.value;

  // 文本搜索
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase();
    filtered = filtered.filter(career => 
      career.title.toLowerCase().includes(searchLower) ||
      career.description.toLowerCase().includes(searchLower) ||
      career.skills.some(skill => skill.toLowerCase().includes(searchLower))
    );
  }

  // 类别筛选
  if (selectedCategory.value !== 'all') {
    filtered = filtered.filter(career => career.category === selectedCategory.value);
  }

  // 难度筛选
  if (selectedDifficulty.value !== 'all') {
    filtered = filtered.filter(career => career.difficulty === selectedDifficulty.value);
  }

  filteredCareers.value = filtered;
};

// 跳转到职业详情
const goToCareerDetail = (career) => {
  router.push(`/roadmap/${career.id}`);
};

// 监听筛选条件变化
const onFilterChange = () => {
  filterCareers();
};

// 执行搜索
const performSearch = () => {
  filterCareers();
};

// 三级导航函数
const selectFirstLevel = async (categoryValue) => {
  // 如果点击的是已选中的分类，则取消选中
  if (activeFirstLvl.value === categoryValue) {
    activeFirstLvl.value = -1;
    activeSecondLvl.value = -1;
    // 回到默认视图，显示所有职业
    await loadCareerData(true);
    return;
  }
  
  activeFirstLvl.value = categoryValue;
  activeSecondLvl.value = -1; // 重置二级选择
  
  // 直接使用categoryValue作为主分类ID
  if (categoryValue !== 'all' && categoryValue !== undefined && categoryValue !== null) {
    await loadCareersByMainCategory(categoryValue, true);
  }
};

const selectSecondLevel = async (subcategoryIndex) => {
  // 如果点击的是已选中的二级分类，则取消选中
  if (activeSecondLvl.value === subcategoryIndex) {
    activeSecondLvl.value = -1;
    // 回到一级分类显示该类别下的所有职业
    if (activeFirstLvl.value !== 'all' && activeFirstLvl.value !== undefined && activeFirstLvl.value !== null) {
      await loadCareersByMainCategory(activeFirstLvl.value, true);
    }
    return;
  }
  
  activeSecondLvl.value = subcategoryIndex;
  
  // 根据选中的一级和二级分类获取子分类ID
  const subCategoryId = getSubCategoryId(activeFirstLvl.value, subcategoryIndex);
  if (activeFirstLvl.value !== 'all' && subCategoryId !== null && subCategoryId !== undefined) {
    await loadCareersBySubCategory(activeFirstLvl.value, subCategoryId, true);
  }
};

// 从三级视图返回到二级视图（显示二级分类列表）
const goBackToSecondLevel = async () => {
  activeSecondLvl.value = -1; // 重置二级选择
  
  // 回到一级分类显示该类别下的所有职业
  if (activeFirstLvl.value !== 'all' && activeFirstLvl.value !== undefined && activeFirstLvl.value !== null) {
    await loadCareersByMainCategory(activeFirstLvl.value, true);
  }
};

const goBackToFirstLevel = () => {
  activeFirstLvl.value = -1;
  activeSecondLvl.value = -1;
  // 回到默认视图，重新加载所有职业数据
  loadCareerData(true);
};

// 加载更多职业 - 适配无限滚动
const loadMoreCareers = async ({ done }) => {
  if (loadingMore.value || !hasMoreCareers.value) {
    done('empty');
    return;
  }
  
  loadingMore.value = true;
  careerPage.value++;
  
  try {
    // 模拟加载延迟
    await new Promise(resolve => setTimeout(resolve, 800));
    
    // 根据当前查询类型调用相应的加载函数
    const { type, mainCategory, subCategory } = currentQueryParams.value;
    
    if (type === 'all') {
      await loadCareerData(false);
    } else if (type === 'mainCategory') {
      await loadCareersByMainCategory(mainCategory, false);
    } else if (type === 'subCategory') {
      await loadCareersBySubCategory(mainCategory, subCategory, false);
    }
    
    // 检查是否还有更多数据
    if (hasMoreCareers.value) {
      done('ok');
    } else {
      done('empty');
    }
  } catch (error) {
    console.error('加载职业失败:', error);
    done('error');
  } finally {
    loadingMore.value = false;
  }
};

// 申请职业相关函数
const openCareerApplicationDialog = () => {
  showApplicationDialog.value = true;
};

const closeApplicationDialog = () => {
  showApplicationDialog.value = false;
  newCareerApplication.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null,
    skills: ''
  };
};

const submitCareerApplication = async () => {
  try {
    submitting.value = true;
    
    const applicationData = {
      name: newCareerApplication.value.name,
      description: newCareerApplication.value.description,
      mainCategory: newCareerApplication.value.mainCategory,
      subCategory: newCareerApplication.value.subCategory,
      skills: newCareerApplication.value.skills || '', // 选填字段
    };

    // 调用API提交申请
    const response = await learnService.submitCareerApplication(applicationData);
    console.log("response: " + JSON.stringify(response));
    
    showSnackbar('职业申请已提交，我们将在3个工作日内审核', 'success');
    closeApplicationDialog();
    
  } catch (error) {
    console.error('提交职业申请失败:', error);
    showSnackbar('提交申请失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
};

// 清空搜索
const clearSearch = () => {
  searchText.value = '';
  selectedCategory.value = 'all';
  selectedDifficulty.value = 'all';
  filterCareers();
};

// 加载热门职业
const loadHotProfessions = async () => {
  try {
    console.log("加载热门职业数据");
    const response = await learnService.getHotProfessions(5);
    
    if (response.code === 200) {
      console.log('获取热门职业数据:', response.data);
      hotProfessions.value = response.data || [];
    } else {
      console.error('获取热门职业失败:', response);
      hotProfessions.value = [];
    }
  } catch (error) {
    console.error('Error loading hot professions:', error);
    hotProfessions.value = [];
  }
};

onMounted(() => {
  loadProfessionCategories();
  loadCareerData(true);
  loadHotProfessions();
});
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="12" md="9" lg="9" class="pr-lg-8">
          <!-- 页面头部 -->
          <div class="mb-8">
            <v-row justify="start" class="mb-4">
              <v-col cols="12">
                <div class="d-flex align-center justify-space-between mb-3">
                  <div class="d-flex align-center">
                    <v-avatar color="teal-lighten-4" size="40" class="mr-3">
                      <v-icon icon="mdi-briefcase-variant" color="teal-darken-2" size="20"></v-icon>
                    </v-avatar>
                    <div>
                      <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">职业中心</h1>
                      <p class="text-body-2 text-grey-darken-2 mb-0">探索职业道路，规划美好未来</p>
                    </div>
                  </div>
                  
                  <!-- 导航栏 -->
                  <div class="d-flex align-center">
                    <v-btn-toggle v-model="selectedNavTab" variant="text" color="primary" class="nav-toggle">
                      <v-btn value="learning" class="nav-btn" @click="router.push('/learning')">
                        正在学习
                      </v-btn>
                      <v-btn value="career" class="nav-btn" :class="{ 'nav-btn-active': selectedNavTab === 'career' }">
                        职业中心
                      </v-btn>
                      <v-btn value="courses" class="nav-btn" @click="router.push('/course/list')">
                        课程中心
                      </v-btn>
                    </v-btn-toggle>
                  </div>
                </div>
              </v-col>
            </v-row>

            <!-- 搜索和筛选区域 -->
            <v-row align="center" class="mb-6">
              <v-col cols="12">
                <div class="d-flex align-center search-container">
                  <v-text-field 
                    v-model="searchText"
                    hide-details="auto" 
                    density="compact" 
                    class="search-input flex-grow-1" 
                    placeholder="搜索你感兴趣的职业、技能或公司..." 
                    variant="outlined"
                    color="primary"
                    @keyup.enter="performSearch"
                    clearable>
                    <template v-slot:prepend-inner>
                      <v-icon icon="mdi-magnify" color="primary" size="20"></v-icon>
                    </template>
                  </v-text-field>
                  <v-btn 
                    color="primary" 
                    variant="flat" 
                    class="ml-3 search-btn" 
                    rounded="lg"
                    size="default"
                    @click="performSearch">
                    <v-icon icon="mdi-magnify" class="mr-2"></v-icon>
                    搜索
                  </v-btn>
                  <v-btn 
                  color="grey-darken-2" 
                  variant="tonal" 
                  rounded="lg"
                  size="default"
                  prepend-icon="mdi-plus-circle"
                  @click="openCareerApplicationDialog">
                  申请添加职业
                </v-btn>
                </div>
              </v-col>
            </v-row>

            <!-- 一级分类按钮导航 -->
            <v-row class="mb-4" v-if="!searchText.trim()">
              <v-col cols="12">
                <v-card flat class="bg-grey-lighten-5 px-6 pt-6 pb-2" rounded="xl" style="border: 1px #ccc solid;">
                  <!-- 标题区域 -->
                  <div class="d-flex align-center mb-5">
                    <div class="pa-3 rounded-xl bg-white mr-3">
                      <v-icon icon="mdi-briefcase-variant" color="blue-darken-2" size="24"></v-icon>
                    </div>
                    <div>
                      <h3 class="text-h6 font-weight-bold text-blue-grey-darken-3 mb-1">职业领域筛选</h3>
                      <p class="text-caption text-blue-grey-darken-1 mb-0">
                        <v-icon icon="mdi-filter-outline" size="12" class="mr-1"></v-icon>
                        选择感兴趣的职业领域，进一步筛选具体方向
                      </p>
                    </div>
                  </div>
                    
                  <!-- 一级分类按钮组 -->
                  <div class="d-flex flex-wrap mb-6" style="gap: 16px;">
                    <v-btn
                      v-for="category in categories.filter(cat => cat.value !== 'all')"
                      :key="category.value"
                      :color="activeFirstLvl === category.value ? 'blue-darken-1' : 'white'"
                      variant="flat"
                      rounded="xl"
                      class="font-weight-medium category-btn-flat"
                      @click="selectFirstLevel(category.value)">
                      <v-icon 
                        :icon="category.icon" 
                        size="18" 
                        class="mr-2"
                        :color="activeFirstLvl === category.value ? 'white' : 'blue-grey-darken-2'">
                      </v-icon>
                      <span :class="activeFirstLvl === category.value ? 'text-white' : 'text-blue-grey-darken-3'">
                        {{ category.title }}
                      </span>
                    </v-btn>
                  </div>
               
                  <!-- 二级分类按钮 -->
                  <div v-if="activeFirstLvl !== -1 && activeFirstLvl !== 0" class="mt-4">
                    <!-- 二级分类标题 -->
                    <div class="pa-4 mb-4 rounded-xl bg-white">
                      <div class="d-flex align-center mb-3">
                        <v-icon icon="mdi-chevron-right" color="blue-darken-1" size="16" class="mr-2"></v-icon>
                        <h4 class="text-subtitle-1 font-weight-bold text-blue-grey-darken-3 mb-0">
                          {{ categories.find(c => c.value === activeFirstLvl)?.title }} - 具体方向
                        </h4>
                      </div>
                      
                      <!-- 二级分类按钮组 -->
                      <div class="d-flex flex-wrap" style="gap: 12px;">
                        <v-btn
                          v-for="(subcategory, subcategoryIndex) in getSubcategoriesByMainCategory(activeFirstLvl)" 
                          :key="subcategoryIndex"
                          :color="activeSecondLvl === subcategoryIndex ? 'orange-darken-1' : 'grey-lighten-3'"
                          variant="flat"
                          rounded="xl"
                          class="font-weight-medium subcategory-btn-flat"
                          @click="selectSecondLevel(subcategoryIndex)">
                          <v-icon 
                            :icon="activeSecondLvl === subcategoryIndex ? 'mdi-folder-open' : 'mdi-folder-outline'" 
                            size="14" 
                            class="mr-1"
                            :color="activeSecondLvl === subcategoryIndex ? 'white' : 'blue-grey-darken-2'">
                          </v-icon>
                          <span :class="activeSecondLvl === subcategoryIndex ? 'text-white' : 'text-blue-grey-darken-3'">
                            {{ subcategory.title }}
                          </span>
                        </v-btn>
                      </div>
                    </div>
                  </div>
                </v-card>
              </v-col>
            </v-row>

            <!-- 职业列表显示区域 -->
            <v-row class="mb-4">
              <v-col cols="12">
                <v-card flat color="transparent" rounded="lg">
                  <!-- 职业列表 -->
                  <div v-if="activeFirstLvl !== -1 && activeSecondLvl !== -1">
                    <div class="d-flex align-center mb-5">
                      <v-btn 
                        icon="mdi-arrow-left" 
                        variant="text" 
                        size="small"
                        color="grey-darken-2"
                        @click="goBackToSecondLevel"></v-btn>
                      <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 ml-2">
                        {{ getSubcategoriesByMainCategory(activeFirstLvl)[activeSecondLvl]?.title }} 
                        - 共 {{ currentCareers.length }} 个职业
                      </h4>
                    </div>
                    
                    <!-- 空状态提示 -->
                    <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
                      <v-icon icon="mdi-briefcase-search-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                      <h3 class="text-h6 text-grey-darken-1 mb-2">暂无相关职业</h3>
                      <p class="text-body-2 text-grey mb-4">该分类下暂时没有职业信息</p>
                    </div>
                    
                    <!-- 无限滚动容器 -->
                    <v-infinite-scroll 
                      v-else
                      :items="displayedCareers" 
                      :onLoad="loadMoreCareers"
                      color="primary">
                      <v-row>
                        <v-col 
                          v-for="career in displayedCareers" 
                          :key="career.id"
                          cols="12" 
                          md="6" 
                          lg="4">
                          <CareerCard 
                            :career="career" 
                            :get-category-name="getCategoryName"
                            :get-sub-category-name-by-id="getSubCategoryNameById"
                            @click="goToCareerDetail(career)" />
                        </v-col>
                      </v-row>
                      
                      <!-- 加载中状态 -->
                      <template v-slot:loading>
                        <div class="text-center py-4">
                          <v-progress-circular 
                            indeterminate 
                            color="primary" 
                            size="24">
                          </v-progress-circular>
                          <p class="text-body-2 text-grey mt-2">正在加载更多职业...</p>
                        </div>
                      </template>
                      
                      <!-- 没有更多数据时的提示 -->
                      <template v-slot:empty>
                        <div class="text-center pt-8 pb-4">
                          <p class="text-body-2 text-grey"> - 已加载全部职业 - </p>
                        </div>
                      </template>
                    </v-infinite-scroll>
                  </div>

                  <!-- 默认职业列表（未选择任何分类） -->
                  <div v-else-if="activeFirstLvl === -1">
                    <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
                      热门职业推荐（共 {{ careers.length }} 个职业）
                    </h4>
                    
                    <!-- 空状态提示 -->
                    <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
                      <v-icon icon="mdi-briefcase-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                      <h3 class="text-h6 text-grey-darken-1 mb-2">暂无职业信息</h3>
                      <p class="text-body-2 text-grey mb-4">请稍后再试或联系管理员</p>
                    </div>
                    
                    <!-- 无限滚动容器 -->
                    <v-infinite-scroll 
                      v-else
                      :items="displayedCareers" 
                      :onLoad="loadMoreCareers"
                      color="primary">
                      <v-row>
                        <v-col 
                          v-for="career in displayedCareers" 
                          :key="career.id"
                          cols="12" 
                          md="6" 
                          lg="4">
                          <CareerCard 
                            :career="career" 
                            :get-category-name="getCategoryName"
                            :get-sub-category-name-by-id="getSubCategoryNameById"
                            @click="goToCareerDetail(career)" />
                        </v-col>
                      </v-row>
                      
                      <!-- 加载中状态 -->
                      <template v-slot:loading>
                        <div class="text-center py-4">
                          <v-progress-circular 
                            indeterminate 
                            color="primary" 
                            size="24">
                          </v-progress-circular>
                          <p class="text-body-2 text-grey mt-2">正在加载更多职业...</p>
                        </div>
                      </template>
                      
                      <!-- 没有更多数据时的提示 -->
                      <template v-slot:empty>
                        <div class="text-center pt-8 pb-4">
                          <p class="text-body-2 text-grey"> - 已加载全部职业 - </p>
                        </div>
                      </template>
                    </v-infinite-scroll>
                  </div>

                  <!-- 一级分类选中但二级未选中 -->
                  <div v-else-if="activeFirstLvl !== -1 && activeSecondLvl === -1">
                    <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
                      {{ categories.find(c => c.value === activeFirstLvl)?.title }} 职业（共 {{ currentCareers.length }} 个）
                    </h4>
                    
                    <!-- 空状态提示 -->
                    <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
                      <v-icon icon="mdi-briefcase-search-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                      <h3 class="text-h6 text-grey-darken-1 mb-2">该分类下暂无职业</h3>
                      <p class="text-body-2 text-grey mb-4">请选择具体的二级分类或等待更多职业上线</p>
                    </div>
                    
                    <!-- 无限滚动容器 -->
                    <v-infinite-scroll 
                      v-else
                      :items="displayedCareers" 
                      :onLoad="loadMoreCareers"
                      color="primary">
                      <v-row>
                        <v-col 
                          v-for="career in displayedCareers" 
                          :key="career.id"
                          cols="12" 
                          md="6" 
                          lg="4">
                          <CareerCard 
                            :career="career" 
                            :get-category-name="getCategoryName"
                            :get-sub-category-name-by-id="getSubCategoryNameById"
                            @click="goToCareerDetail(career)" />
                        </v-col>
                      </v-row>
                      
                      <!-- 加载中状态 -->
                      <template v-slot:loading>
                        <div class="text-center py-4">
                          <v-progress-circular 
                            indeterminate 
                            color="primary" 
                            size="24">
                          </v-progress-circular>
                          <p class="text-body-2 text-grey mt-2">正在加载更多职业...</p>
                        </div>
                      </template>
                      
                      <!-- 没有更多数据时的提示 -->
                      <template v-slot:empty>
                        <div class="text-center pt-8 pb-4">
                          <p class="text-body-2 text-grey"> - 已加载全部职业 - </p>
                        </div>
                      </template>
                    </v-infinite-scroll>
                  </div>
                </v-card>
              </v-col>
            </v-row>
          </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
          <p class="text-grey-darken-2 mt-4">正在加载职业信息...</p>
        </div>

        <!-- 搜索结果显示（保持原有的卡片模式） -->
        <div v-if="searchText.trim()">
          <v-row v-if="filteredCareers.length > 0">
            <v-col 
              v-for="career in filteredCareers" 
              :key="career.id"
              cols="12" 
              md="6" 
              lg="4">
              <CareerCard 
                :career="career" 
                :get-category-name="getCategoryName"
                :get-sub-category-name-by-id="getSubCategoryNameById"
                @click="goToCareerDetail(career)" />
            </v-col>
          </v-row>

          <!-- 无结果提示 -->
          <v-row v-else>
            <v-col cols="12">
              <v-card flat class="text-center py-12" color="grey-lighten-5" rounded="lg">
                <v-icon icon="mdi-briefcase-search-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
                <h3 class="text-h6 text-grey-darken-1 mb-2">未找到相关职业</h3>
                <p class="text-body-2 text-grey mb-4">试试其他关键词或浏览分类</p>
              </v-card>
            </v-col>
          </v-row>
        </div>
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="12" md="3" lg="3" class="d-none d-md-block">
        <div class="sticky-sidebar">
          <!-- 热门职业 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-fire" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">热门职业</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">实时更新排行</p>
                </div>
              </div>

              <v-list bg-color="transparent" class="pa-0">
                <v-list-item v-for="(profession, index) in hotProfessions" :key="profession.id"
                             class="px-3 py-2 ma-1 rounded-lg cursor-pointer hover-item" 
                             :class="index < 3 ? 'bg-white' : 'bg-transparent'"
                             density="compact"
                             @click="goToCareerDetail(profession)">
                  <template v-slot:prepend>
                    <v-avatar :color="index < 3 ? 'primary' : 'grey-lighten-2'" size="24" class="mr-2">
                      <span class="text-caption font-weight-bold"
                            :class="index < 3 ? 'text-white' : 'text-grey-darken-2'">
                        {{ index + 1 }}
                      </span>
                    </v-avatar>
                  </template>

                  <v-list-item-title class="text-body-2 font-weight-medium">
                    {{ profession.name }}
                  </v-list-item-title>
                  <v-list-item-subtitle class="text-caption">
                    {{ profession.learnerCount || 0 }} 人学习
                  </v-list-item-subtitle>

                  <template v-slot:append>
                    <v-chip variant="flat" color="grey-lighten-3" size="x-small">
                      <v-icon icon="mdi-trending-up" size="10" class="mr-1"></v-icon>
                      热门
                    </v-chip>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>

            <v-card-actions class="px-4 pb-4">
              <v-btn @click="router.push('/ranking?tab=professions')" variant="tonal" color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                查看全部热门职业
              </v-btn>
            </v-card-actions>
          </v-card>

          <!-- 学习建议 -->
          <v-card flat color="grey-lighten-5" rounded="lg">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-lightbulb-outline" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">学习建议</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">职业规划指导</p>
                </div>
              </div>

              <p class="text-body-2 text-grey-darken-2 mb-3">
                选择职业时建议考虑个人兴趣、能力特长以及市场需求。
              </p>
              
              <v-list density="compact" class="bg-transparent pa-0">
                <v-list-item density="compact" class="px-0 py-1">
                  <template v-slot:prepend>
                    <v-icon icon="mdi-check-circle" color="success" size="16" class="mr-2"></v-icon>
                  </template>
                  <v-list-item-title class="text-body-2">了解职业要求</v-list-item-title>
                </v-list-item>
                <v-list-item density="compact" class="px-0 py-1">
                  <template v-slot:prepend>
                    <v-icon icon="mdi-check-circle" color="success" size="16" class="mr-2"></v-icon>
                  </template>
                  <v-list-item-title class="text-body-2">制定学习计划</v-list-item-title>
                </v-list-item>
                <v-list-item density="compact" class="px-0 py-1">
                  <template v-slot:prepend>
                    <v-icon icon="mdi-check-circle" color="success" size="16" class="mr-2"></v-icon>
                  </template>
                  <v-list-item-title class="text-body-2">持续技能提升</v-list-item-title>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>
      </v-col>
    </v-row>

    <!-- 申请职业对话框 -->
    <v-dialog v-model="showApplicationDialog" max-width="600" >
      <v-card rounded="xl">
        <v-card-title class="pa-6 pb-0">
          <div class="d-flex align-center">
            <v-avatar color="success" size="40" class="mr-3">
              <v-icon icon="mdi-briefcase-plus" color="white"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h6 font-weight-bold">申请新职业</h3>
              <p class="text-body-2 text-grey-darken-2 mb-0">提交职业申请，经审核后将添加到职业库</p>
            </div>
          </div>
        </v-card-title>

        <v-card-text class="pa-6">
          <v-form ref="applicationForm" v-model="applicationValid">
            <v-text-field
              v-model="newCareerApplication.name"
              label="职业名称"
              variant="outlined"
              rounded="lg"
              class="mb-4"
              required
              placeholder="例如：全栈开发工程师">
              <template v-slot:prepend-inner>
                <v-icon icon="mdi-briefcase" color="grey-darken-1"></v-icon>
              </template>
            </v-text-field>

            <!-- 使用新的分类选择器 -->
            <div class="mb-4">
              <div class="text-body-2 font-weight-medium mb-2">职业分类</div>
              <CategorySelector
                v-model:model-main-category="newCareerApplication.mainCategory"
                v-model:model-sub-category="newCareerApplication.subCategory"
              />
            </div>

            <v-textarea
              v-model="newCareerApplication.description"
              label="职业描述"
              variant="outlined"
              rounded="lg"
              rows="3"
              required
              class="mb-4"
              placeholder="详细描述这个职业的工作内容和职责...">
            </v-textarea>

            <v-text-field
              v-model="newCareerApplication.skills"
              label="核心技能 (选填，用逗号分隔)"
              variant="outlined"
              rounded="lg"
              placeholder="例如：JavaScript, Vue.js, Node.js">
            </v-text-field>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn 
            variant="text" 
            @click="closeApplicationDialog">
            取消
          </v-btn>
          <v-btn 
            color="success" 
            variant="flat"
            :disabled="!applicationValid"
            :loading="submitting"
            @click="submitCareerApplication">
            提交申请
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
/* Flat 风格 - 无阴影设计 */

/* 搜索区域样式 */
.search-container {
  gap: 12px;
}

.search-input :deep(.v-field) {
  border-radius: 16px !important;
  border: 1px solid rgba(25, 118, 210, 0.2) !important;
}

.search-btn {
  font-weight: 600 !important;
  letter-spacing: 0.5px !important;
  border: 1px solid rgba(25, 118, 210, 0.3) !important;
  transition: all 0.2s ease !important;
}

.search-btn:hover {
  transform: translateY(-1px) !important;
  border-color: #1976d2 !important;
}

/* 职业分类筛选区域 - Flat 样式 */
.category-btn-flat {
  transition: all 0.3s ease !important;
  text-transform: none !important;
}

.category-btn-flat:hover {
  transform: translateY(-2px) !important;
  border-color: #1565c0 !important;
}

.subcategory-btn-flat {
  transition: all 0.3s ease !important;
  text-transform: none !important;
  height: 32px;
}

.subcategory-btn-flat:hover {
  transform: translateY(-1px) !important;
  border-color: #1565c0 !important;
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

.sticky-sidebar {
  position: sticky;
  top: 20px;
}

.hover-item {
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.hover-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.cursor-pointer {
  cursor: pointer;
}

.gap-2 {
  gap: 12px;
}

/* 三级导航样式 */
.transition-all {
  transition: all 0.3s ease;
}

.hover-shadow:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15) !important;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
