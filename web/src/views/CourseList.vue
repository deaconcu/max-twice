<script setup>
import { ref, onMounted, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { courseServiceV1, subscriptionServiceV1 } from '@/services/api/v1/apiServiceV1';
import { learnService } from '@/services/learnService'; // 临时保留，用于尚未迁移的接口
import { useUserStore } from "@/stores/user";
import RightSidebar from '@/components/RightSidebar.vue';

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');

const user = useUserStore();
console.log("user id: " + JSON.stringify(user));

const router = useRouter();

const sendMessage = () => {
  alert("search");
}

const config = ref([]);
const selected = ref([]);
const selectedNavTab = ref('courses'); // 导航栏当前选中项
const courses = ref([]); // 存储当前分类的课程数据
const loading = ref(false); // 加载状态

const activeFirstLvl = ref(-1)
const applyCourseDialog = ref(false);
const applyCourseData = ref({
  name: "",
  description: "",
  mainCategoryId: "",
  subCategoryId: ""
})

onMounted(() => {
  loadSystem();
  loadSubscription();
  loadHotCourses();
});

// 监听分类选择变化，加载对应课程
watch([() => activeFirstLvl.value, () => selected.value], async ([newFirstLvl, newSelected]) => {
  if (newFirstLvl >= 0 && newSelected[newFirstLvl] >= 0 && config.value.courses) {
    const mainCategory = config.value.courses[newFirstLvl];
    const subCategory = mainCategory.list[newSelected[newFirstLvl]];
    
    if (mainCategory && subCategory) {
      await loadCoursesByCategory(mainCategory.id, subCategory.id);
    }
  }
}, { deep: true });

const loadSystem = async () => {
  try {
    const response = await learnService.getCourseCategories(); // TODO: 需要迁移到V1 API

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      // 转换新的API数据结构为模板期望的格式
      const { mainCategories, categoryMapping } = response.data.courseCategories;
      
      // 构建课程分类树结构
      const coursesConfig = mainCategories.map(mainCategory => {
        const mapping = categoryMapping.find(m => m.mainCategoryId === mainCategory.id);
        return {
          id: mainCategory.id,
          name: mainCategory.name,
          icon: mainCategory.icon,
          color: mainCategory.color,
          list: mapping ? mapping.subCategories : []
        };
      });
      
      config.value = { courses: coursesConfig };
      selected.value = new Array(coursesConfig.length).fill(-1);
    }
  } catch (error) {
    console.error('Error loading course categories:', error);
  }
}

// 根据分类加载课程数据
const loadCoursesByCategory = async (mainCategory, subCategory) => {
  try {
    loading.value = true;
    console.log(`Loading courses for mainCategory: ${mainCategory}, subCategory: ${subCategory}`);
    
    const response = await learnService.getCoursesByCategory(mainCategory, subCategory); // TODO: 需要迁移到V1 API
    
    if (response.code === 200) {
      courses.value = response.data || [];
      console.log('Loaded courses:', courses.value);
    } else {
      console.error('Failed to load courses:', response);
      courses.value = [];
    }
  } catch (error) {
    console.error('Error loading courses:', error);
    courses.value = [];
  } finally {
    loading.value = false;
  }
}

// 获取当前选中主分类的子分类列表
const getSubCategories = () => {
  if (!applyCourseData.value.mainCategoryId || !config.value.courses) {
    return [];
  }
  
  const mainCategory = config.value.courses.find(cat => cat.id === applyCourseData.value.mainCategoryId);
  return mainCategory ? mainCategory.list : [];
}


const postApplyCourse = async () => {
  try {
    // 验证必填字段
    if (!applyCourseData.value.name.trim()) {
      showSnackbar(t('validation.required.courseName'), "error");
      return;
    }
    if (!applyCourseData.value.description.trim()) {
      showSnackbar(t('validation.required.courseDescription'), "error");
      return;
    }
    if (!applyCourseData.value.mainCategoryId) {
      showSnackbar(t('validation.required.mainCategory'), "error");
      return;
    }
    if (!applyCourseData.value.subCategoryId) {
      showSnackbar(t('validation.required.subCategory'), "error");
      return;
    }

    const courseData = {
      name: applyCourseData.value.name,
      description: applyCourseData.value.description,
      mainCategory: applyCourseData.value.mainCategoryId,
      subCategory: applyCourseData.value.subCategoryId
    };

    console.log("Creating course:", courseData);
    const response = await courseServiceV1.createCourse(courseData);

    if (response.code === 401) {
      showSnackbar("请先登录！", "error");
    } else if (response.code === 200) {
      console.log('Course created successfully');
      applyCourseDialog.value = false;
      // 清空表单
      applyCourseData.value = {
        name: "",
        description: "",
        mainCategoryId: "",
        subCategoryId: ""
      };
      showSnackbar(t('message.courseCreateSuccess'));
    } else {
      showSnackbar(response.message || "创建失败，请重试！", "error");
    }
  } catch (error) {
    console.error('Error creating course:', error);
    showSnackbar("创建失败，请重试！", "error");
  }
}

const subscriptions = ref([]);
const hotCourses = ref([]);

// 加载收藏课程
async function loadSubscription() {
  console.log("load subscription");
  try {
    const userId = user.userId;
    if (userId) {
      let response = await subscriptionServiceV1.getUserSubscriptions(userId);
      
      if (response.code === 401) {
        console.log('not login');
        subscriptions.value = [];
      } else if (response.code === 200) {
        console.log('收藏课程API响应完整数据:', JSON.stringify(response, null, 2));
        console.log('response.data类型:', typeof response.data);
        console.log('response.data内容:', response.data);
        if (Array.isArray(response.data)) {
          console.log('数组长度:', response.data.length);
          response.data.forEach((item, index) => {
            console.log(`收藏课程[${index}]:`, item);
            console.log(`- id: ${item.id} (${typeof item.id})`);
            console.log(`- name: ${item.name} (${typeof item.name})`);
            console.log(`- 完整对象:`, JSON.stringify(item, null, 2));
          });
        }
        subscriptions.value = response.data || [];
      } else {
        console.log('获取收藏课程失败:', response);
        subscriptions.value = [];
      }
    } else {
      console.log('没有用户ID，无法加载收藏课程');
      subscriptions.value = [];
    }
  } catch (error) {
    console.error('Error get subscription:', error);
    subscriptions.value = [];
  }
};

// 加载热门课程
async function loadHotCourses() {
  console.log("load hot courses");
  try {
    let response = await courseServiceV1.getHotCourses();

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get hot courses data:' + JSON.stringify(response.data));
      hotCourses.value = response.data;
    }
  } catch (error) {
    console.error('Error get hot courses:', error);
    // 如果没有后端接口，使用模拟数据
    hotCourses.value = [
      { id: 1, name: '数据结构与算法', learnerCount: 15534, subscriptionCount: 8900 },
      { id: 2, name: '英语写作', learnerCount: 20001, subscriptionCount: 7200 },
      { id: 3, name: '计算机网络', learnerCount: 6888, subscriptionCount: 9100 },
      { id: 4, name: '人工智能导论', learnerCount: 12230, subscriptionCount: 5800 },
      { id: 5, name: '法律基础', learnerCount: 18910, subscriptionCount: 4500 }
    ];
  }
};

const switchSecondLevel = (firstIndex, secondIndex) => {
  selected.value[firstIndex] = secondIndex;
  //selectedCopy.value = Array.from(selected.value);
}

const handleHoverEnter = (firstIndex, secondIndex) => {
  selectedCopy.value = Array.from(selected.value);
  selected.value[firstIndex] = secondIndex;
}

const handleHoverLeave = (firstIndex, secondIndex) => {
  selected.value = Array.from(selectedCopy.value);
}

const switchFirstLevel = (firstIndex) => {
  activeFirstLvl.value = (activeFirstLvl.value == firstIndex ? -1 : firstIndex);
  selected.value.fill(-1);
}

// 移除旧的items数组，使用新的hotCourses数组

const review = ref('80%');

function openInNewTab(courseId) {
  const url = router.resolve({ path: '/read', query: { courseId: courseId } }).href
  window.open(url, '_blank')
}
</script>

<template>
  <Suspense>
    <v-container fluid>
      <v-row class="mt-2">

        <v-col cols="9" class="pr-8">
          <div class="mb-8">
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
                    <v-btn-toggle v-model="selectedNavTab" variant="text" color="primary" class="nav-toggle">
                      <v-btn value="learning" class="nav-btn" @click="router.push('/learning')">
                        正在学习
                      </v-btn>
                      <v-btn value="career" class="nav-btn" @click="router.push('/career')">
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
            <v-row justify="start" align="center">
              <v-col cols="6">
                <v-text-field hide-details="auto" density="compact" class="search-input" rounded="lg"
                  :placeholder="t('course.search')" variant="outlined" @click:append-inner="sendMessage">
                  <template v-slot:prepend-inner>
                    <v-icon icon="mdi-magnify" color="grey-lighten-1" size="18"></v-icon>
                  </template>
                </v-text-field>
              </v-col>
              <v-col class="d-flex justify-end">
                <v-btn @click="applyCourseDialog = true" variant="flat" color="grey-darken-2" class="px-4 text-white" rounded="lg"
                  density="default">
                  <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
                  {{ t('course.createNew') }}
                </v-btn>
                <v-dialog v-model="applyCourseDialog" width="800" height="620" content-class="fix-dialog">
                  <v-card class="px-1 py-2" rounded="lg">
                    <v-card-title class="d-flex align-center">
                      <v-icon icon="mdi-file-cog-outline" size="small" class=""></v-icon>
                      <span class="ps-2">{{ t('course.createNew') }}</span>
                    </v-card-title>
                    <v-card-subtitle>
                      {{ t('course.fillInfo') }}
                    </v-card-subtitle>
                    <v-card-text class="px-4 py-8">
                      <v-text-field 
                        v-model="applyCourseData.name" 
                        :label="t('course.name')" 
                        variant="outlined"
                        density="compact"
                        class="mb-4"
                        required>
                      </v-text-field>
                      
                      <v-textarea 
                        v-model="applyCourseData.description" 
                        :label="t('course.description')" 
                        variant="outlined" 
                        density="compact"
                        rows="4"
                        class="mb-4"
                        required>
                      </v-textarea>
                      
                      <v-select
                        v-model="applyCourseData.mainCategoryId"
                        :items="config.courses || []"
                        item-title="name"
                        item-value="id"
                        :label="t('course.mainCategory')"
                        variant="outlined"
                        density="compact"
                        class="mb-4"
                        required
                        @update:modelValue="applyCourseData.subCategoryId = ''">
                      </v-select>
                      
                      <v-select
                        v-model="applyCourseData.subCategoryId"
                        :items="getSubCategories()"
                        item-title="name"
                        item-value="id"
                        :label="t('course.subCategory')"
                        variant="outlined"
                        density="compact"
                        class="mb-4"
                        :disabled="!applyCourseData.mainCategoryId"
                        required>
                      </v-select>
                    </v-card-text>
                    <v-card-actions class="justify-center">
                      <v-btn text="取消" @click="applyCourseDialog = false" class="px-4" variant="outlined"></v-btn>
                      <v-btn :text="t('course.create')" @click="postApplyCourse" class="px-4" color="primary"></v-btn>
                    </v-card-actions>
                  </v-card>
                </v-dialog>
              </v-col>
            </v-row>
          </div>
          <div v-for="(firstLevel, firstIndex) in config.courses" class="mb-5">
            <v-card flat color="grey-lighten-5" rounded="xl" class="pa-0">
              <v-card-text class="pa-4">
                <div class="d-flex start align-baseline mb-2">
                  <v-btn variant="text" :ripple="false" class="ma-0 pa-1 pb-1 me-3 text-h5 font-weight-bold"
                    style="min-width: 10px;" :color="activeFirstLvl === firstIndex ? 'primary' : 'grey-darken-2'"
                    @click="switchFirstLevel(firstIndex)">
                    <v-icon :icon="activeFirstLvl === firstIndex ? 'mdi-chevron-down' : 'mdi-chevron-right'" size="18"
                      class="mr-1"></v-icon>
                    <span class="font-weight-regular">{{ firstLevel.name }}</span>
                  </v-btn>
                  <div class="text-grey-darken-3 text-body-2 d-flex align-center ml-2 mb-3">
                    <v-icon icon="mdi-book-multiple-outline" class="mr-1" size="16" color="primary"></v-icon>
                    <span class="mr-4">515</span>
                    <v-icon icon="mdi-list-box-outline" class="mr-1" size="16" color="success"></v-icon>
                    <span class="mr-4">23,423</span>
                    <v-icon icon="mdi-chart-donut" class="mr-1" size="16" color="warning"></v-icon>
                    <span>60%</span>
                  </div>
                </div>



                <v-expand-transition>
                  <div v-if="activeFirstLvl === firstIndex" class="mt-3">
                    <v-item-group class="mb-3">
                      <v-item v-for="(secondLevel, secondIndex) in firstLevel.list" :key="secondIndex">
                        <v-btn class="ma-1 mt-2 mb-1 border text-body-1" variant="flat" rounded="lg" :ripple="false"
                          :color="secondIndex === selected[firstIndex] ? 'primary' : 'white'" density="default"
                          @click="switchSecondLevel(firstIndex, secondIndex)">
                          {{ secondLevel.name }}
                          <v-chip v-if="secondLevel.list" variant="flat"
                            :color="secondIndex === selected[firstIndex] ? 'white' : 'grey-lighten-3'" size="x-small"
                            class="ml-1">
                            {{ secondLevel.list.length }}
                          </v-chip>
                        </v-btn>
                      </v-item>
                    </v-item-group>

                    <v-expand-transition>
                      <v-tabs-window v-model="selected[firstIndex]" v-if="selected[firstIndex] != -1" class="mt-3">
                        <v-tabs-window-item v-for="(secondLevel, secondIndex) in firstLevel.list" :key="secondIndex">
                          <div class="border-t-sm pt-3">
                            <!-- 加载状态 -->
                            <div v-if="loading" class="text-center py-4">
                              <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
                              <p class="text-body-2 text-grey-darken-2 mt-2">{{ t('course.loading') }}</p>
                            </div>
                            
                            <!-- 课程列表 -->
                            <div v-else-if="courses.length > 0" class="mt-0">
                              <v-row class="px-4 pb-4">
                                <div v-for="course in courses" :key="course.id">
                                  <v-btn
                                    @click="openInNewTab(course.id)" 
                                    class="hover-card mt-4 me-2 text-body-1"
                                    variant="flat"
                                    size="default"
                                    rounded="lg"
                                    :ripple="false"
                                    prepend-icon="mdi-play-circle-outline"
                                  >
                                    {{ course.name }}
                                    <v-chip size="x-small" class="ms-2" color="purple-lighten-2">
                                      {{ Math.floor(Math.random() * 9000) + 1000 }}人学习
                                    </v-chip>
                                  </v-btn>
                                </div>
                              </v-row>
                            </div>
                            
                            <!-- 无课程提示 -->
                            <div v-else class="text-center py-6">
                              <v-icon icon="mdi-book-outline" size="48" color="grey-lighten-1" class="mb-3"></v-icon>
                              <p class="text-body-1 text-grey-darken-2 mb-2">{{ t('course.noCourses') }}</p>
                              <p class="text-body-2 text-grey-darken-1">{{ t('course.selectOther') }}</p>
                            </div>
                          </div>
                        </v-tabs-window-item>
                      </v-tabs-window>
                    </v-expand-transition>

                  </div>
                </v-expand-transition>
              </v-card-text>
            </v-card>
          </div>
        </v-col>

        <v-col cols="3">
          <RightSidebar />
        </v-col>
      </v-row>

    </v-container>
  </Suspense>
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

.v-number-input * {
  --v-input-control-height: 20px;
  --v-input-padding-top: 0px;
  --v-field-input-padding-bottom: 0px;
  --v-field-padding-bottom: 0px;
}

:deep(.fix-dialog) {
  top: 150px !important;
  position: absolute !important;
}

/* 改善字体渲染和清晰度 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 课程卡片样式 */
.course-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.course-card:hover {
  transform: translateY(-2px);
}

.hover-card {
  border: 1px solid #e0e0e0;
}

/* 增加hover效果但保持flat风格 */
.v-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}

.v-chip:hover {
  transform: scale(1.02);
  transition: transform 0.2s ease;
}

/* 自定义滚动条样式 */
:deep(.v-responsive) {
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.1) transparent;
}

:deep(.v-responsive)::-webkit-scrollbar {
  width: 4px;
}

:deep(.v-responsive)::-webkit-scrollbar-track {
  background: transparent;
}

:deep(.v-responsive)::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
</style>