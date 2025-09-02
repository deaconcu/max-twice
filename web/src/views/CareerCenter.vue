<script setup>
import { ref, onMounted, inject, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { systemServiceV1, professionServiceV1 } from '@/services/api/v1/apiServiceV1';
import CareerCard from '@/components/career/CareerCard.vue';
import CategorySelector from '@/components/common/CategorySelector.vue';
import RightSidebar from '@/components/common/RightSidebar.vue';
import CategoryNavigation from '@/components/career/CategoryNavigation.vue';
import CareerFilter from '@/components/career/CareerFilter.vue';
import CareerGrid from '@/components/career/CareerGrid.vue';
import { useToastMessage } from '@/composables/useToastMessage';

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');
const router = useRouter();
const { getMessage } = useToastMessage();

// 状态管理
const careers = ref([]);
const filteredCareers = ref([]);
const loading = ref(true);
const searchText = ref('');
const selectedCategory = ref('all');
const selectedDifficulty = ref('all');
const selectedNavTab = ref('career'); // 导航栏当前选中项

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
    const response = await systemServiceV1.getProfessionCategories();
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
  return subcategories[subcategoryIndex]?.id || null;
};

// 工具函数：根据ID获取分类名称
const getCategoryName = (categoryId) => {
  const category = categories.value.find(cat => cat.id === categoryId);
  return category?.title || '';
};

// 工具函数：根据主分类ID和子分类ID获取子分类名称
const getSubCategoryName = (mainCategoryId, subCategoryId) => {
  const subcategories = getSubcategoriesByMainCategory(mainCategoryId);
  const subcategory = subcategories.find(sub => sub.id === subCategoryId);
  return subcategory?.name || '';
};

// 工具函数：根据子分类ID获取子分类名称（用于CareerCard）
const getSubCategoryNameById = (subCategoryId) => {
  // 遍历所有分类映射来查找子分类
  for (const mapping of categoryMapping.value) {
    const subcategory = mapping.subcategories.find(sub => sub.id === subCategoryId);
    if (subcategory) {
      return subcategory.name;
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

// 难度等级 - 使用computed确保语言切换时更新
const difficulties = computed(() => [
  { value: 'all', title: t('careerCenter.difficulty.all') },
  { value: 'beginner', title: t('careerCenter.difficulty.beginner') },
  { value: 'intermediate', title: t('careerCenter.difficulty.intermediate') },
  { value: 'advanced', title: t('careerCenter.difficulty.advanced') },
]);

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
    const response = await professionServiceV1.getApprovedProfessions(lastId.value);
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
    showSnackbar(getMessage('careerCenter.errors.loadFailed'), 'error');
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
    
    const response = await professionServiceV1.getProfessionsByCategory(lastId.value, mainCategoryId, null);
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
    console.error('加载主分类职业失败:', error);
    showSnackbar(getMessage('careerCenter.errors.loadCategoryFailed'), 'error');
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
    
    const response = await professionServiceV1.getProfessionsByCategory(lastId.value, mainCategoryId, subCategoryId);
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
    console.error('加载子分类职业失败:', error);
    showSnackbar(getMessage('careerCenter.errors.loadSubcategoryFailed'), 'error');
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
  
  // 使用categoryValue作为主分类ID
  if (categoryValue !== undefined && categoryValue !== null && categoryValue !== 0) {
    await loadCareersByMainCategory(categoryValue, true);
  }
};

const selectSecondLevel = async (subcategoryIndex) => {
  // 如果点击的是已选中的二级分类，则取消选中
  if (activeSecondLvl.value === subcategoryIndex) {
    activeSecondLvl.value = -1;
    // 回到一级分类显示该类别下的所有职业
    if (activeFirstLvl.value !== undefined && activeFirstLvl.value !== null && activeFirstLvl.value !== 0) {
      await loadCareersByMainCategory(activeFirstLvl.value, true);
    }
    return;
  }
  
  activeSecondLvl.value = subcategoryIndex;
  
  // 根据选中的一级和二级分类获取子分类ID
  const subCategoryId = getSubCategoryId(activeFirstLvl.value, subcategoryIndex);
  if (activeFirstLvl.value !== undefined && activeFirstLvl.value !== null && activeFirstLvl.value !== 0 && subCategoryId !== null && subCategoryId !== undefined) {
    await loadCareersBySubCategory(activeFirstLvl.value, subCategoryId, true);
  }
};

// 从三级视图返回到二级视图（显示二级分类列表）
const goBackToSecondLevel = async () => {
  activeSecondLvl.value = -1; // 重置二级选择
  
  // 回到一级分类显示该类别下的所有职业
  if (activeFirstLvl.value !== undefined && activeFirstLvl.value !== null && activeFirstLvl.value !== 0) {
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
    const response = await professionServiceV1.createProfession(applicationData);
    console.log("response: " + JSON.stringify(response));
    
    showSnackbar(getMessage('careerCenter.application.submittedSuccess'), 'success');
    closeApplicationDialog();
    
  } catch (error) {
    console.error('提交职业申请失败:', error);
    showSnackbar(getMessage('careerCenter.application.submitFailed'), 'error');
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

onMounted(() => {
  loadProfessionCategories();
  loadCareerData(true);
});
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="12" md="9" lg="9" class="pr-lg-8">
        <!-- 页面头部和搜索 -->
        <CareerFilter 
          v-model:searchText="searchText"
          v-model:selectedNavTab="selectedNavTab"
          @performSearch="performSearch"
          @openCareerApplication="openCareerApplicationDialog"
        />

        <!-- 分类导航 -->
        <CategoryNavigation 
          :categories="categories"
          :categoryMapping="categoryMapping"
          :activeFirstLvl="activeFirstLvl"
          :activeSecondLvl="activeSecondLvl"
          :searchText="searchText"
          @selectFirstLevel="selectFirstLevel"
          @selectSecondLevel="selectSecondLevel"
        />

        <!-- 职业网格 -->
        <CareerGrid 
          :displayedCareers="displayedCareers"
          :loading="loading"
          :activeFirstLvl="activeFirstLvl"
          :activeSecondLvl="activeSecondLvl"
          :currentCareers="currentCareers"
          :careers="careers"
          :categoryMapping="categoryMapping"
          :categories="categories"
          :searchText="searchText"
          @loadMoreCareers="loadMoreCareers"
          @goToCareerDetail="goToCareerDetail"
          @goBackToSecondLevel="goBackToSecondLevel"
        />
      </v-col>

      <v-col cols="3" class="ps-12 pt-6">
        <RightSidebar />
      </v-col>
    </v-row>
  </v-container>

  <!-- 申请职业对话框 -->
  <v-dialog v-model="showApplicationDialog" max-width="600px" persistent>
    <v-card rounded="lg">
      <v-card-title class="pa-6">
        <div class="d-flex align-center">
          <v-avatar color="primary" size="32" class="mr-3">
            <v-icon icon="mdi-plus-circle" color="white" size="16"></v-icon>
          </v-avatar>
          <span class="text-h6 font-weight-bold">{{ t('careerCenter.application.title') }}</span>
        </div>
      </v-card-title>

      <v-card-text class="px-6 pb-0">
        <v-form v-model="applicationValid">
          <v-text-field
            v-model="newCareerApplication.name"
            :label="t('careerCenter.application.name')"
            variant="outlined"
            :rules="[(v) => !!v || t('careerCenter.application.nameRequired')]"
            required
            class="mb-4">
          </v-text-field>

          <v-textarea
            v-model="newCareerApplication.description"
            :label="t('careerCenter.application.description')"
            variant="outlined"
            :rules="[(v) => !!v || t('careerCenter.application.descriptionRequired')]"
            required
            rows="3"
            class="mb-4">
          </v-textarea>

          <v-text-field
            v-model="newCareerApplication.skills"
            :label="t('careerCenter.application.skills')"
            variant="outlined"
            :hint="t('careerCenter.application.skillsHint')"
            persistent-hint
            class="mb-4">
          </v-text-field>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn
          variant="text"
          @click="closeApplicationDialog"
          :disabled="submitting">
          {{ t('careerCenter.application.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          @click="submitCareerApplication"
          :disabled="!applicationValid || submitting"
          :loading="submitting">
          {{ t('careerCenter.application.submit') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* Flat 风格 - 无阴影设计 */
</style>