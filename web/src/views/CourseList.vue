<script setup>
import { ref, onMounted, inject, watch } from 'vue';
import { useRouter } from 'vue-router';
import { learnService, userService } from '@/services/learnService';
import { useUserStore } from "@/stores/user";

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
  loadLearningCourses();
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
    const response = await learnService.getCourseCategories();

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
    
    const response = await learnService.getCoursesByCategory(mainCategory, subCategory);
    
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
      showSnackbar("请输入课程名称！", "error");
      return;
    }
    if (!applyCourseData.value.description.trim()) {
      showSnackbar("请输入课程描述！", "error");
      return;
    }
    if (!applyCourseData.value.mainCategoryId) {
      showSnackbar("请选择主分类！", "error");
      return;
    }
    if (!applyCourseData.value.subCategoryId) {
      showSnackbar("请选择子分类！", "error");
      return;
    }

    const courseData = {
      name: applyCourseData.value.name,
      description: applyCourseData.value.description,
      mainCategory: applyCourseData.value.mainCategoryId,
      subCategory: applyCourseData.value.subCategoryId
    };

    console.log("Creating course:", courseData);
    const response = await learnService.createCourse(courseData);

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
      showSnackbar("课程创建成功！");
    } else {
      showSnackbar(response.message || "创建失败，请重试！", "error");
    }
  } catch (error) {
    console.error('Error creating course:', error);
    showSnackbar("创建失败，请重试！", "error");
  }
}

const subscriptions = ref([]);
const learningCourses = ref([]);
const hotCourses = ref([]);

// 加载收藏课程
async function loadSubscription() {
  console.log("load subscription");
  try {
    const userId = user.userId;
    if (userId) {
      let response = await userService.getSubscription(userId);
      
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

// 加载正在学习的课程
async function loadLearningCourses() {
  console.log("load learning courses");
  try {
    let response = await learnService.getUserCourseList();

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get learning courses data:' + JSON.stringify(response.data));
      // 只取正在学习的课程（IN_PROGRESS状态）
      learningCourses.value = response.data.filter(userCourse => 
        userCourse.status === 'IN_PROGRESS'
      ).map(userCourse => ({
        id: userCourse.course.id,
        name: userCourse.course.name,
        description: userCourse.course.description,
        progress: userCourse.progressPercent || 0
      }));
    }
  } catch (error) {
    console.error('Error get learning courses:', error);
  }
};

// 加载热门课程
async function loadHotCourses() {
  console.log("load hot courses");
  try {
    let response = await learnService.getHotCourses();

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

        <v-col cols="8" class="pr-8">
          <div class="mb-8">
            <v-row justify="start" class="mb-4">
              <v-col cols="12">
                <div class="d-flex align-center justify-space-between mb-3">
                  <div class="d-flex align-center">
                    <v-avatar color="teal-lighten-4" size="40" class="mr-3">
                      <v-icon icon="mdi-book-multiple" color="teal-darken-2" size="20"></v-icon>
                    </v-avatar>
                    <div>
                      <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">课程中心</h1>
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
                        课程中心
                      </v-btn>
                    </v-btn-toggle>
                  </div>
                </div>
              </v-col>
            </v-row>
            <v-row justify="start" align="center">
              <v-col cols="6">
                <v-text-field hide-details="auto" density="compact" class="search-input" rounded="lg"
                  placeholder="搜索感兴趣的课程..." variant="outlined" @click:append-inner="sendMessage">
                  <template v-slot:prepend-inner>
                    <v-icon icon="mdi-magnify" color="grey-lighten-1" size="18"></v-icon>
                  </template>
                </v-text-field>
              </v-col>
              <v-col class="d-flex justify-end">
                <v-btn @click="applyCourseDialog = true" variant="flat" color="grey-darken-2" class="px-4 text-white" rounded="lg"
                  density="default">
                  <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
                  创建新课程
                </v-btn>
                <v-dialog v-model="applyCourseDialog" width="800" height="620" content-class="fix-dialog">
                  <v-card class="px-1 py-2" rounded="lg">
                    <v-card-title class="d-flex align-center">
                      <v-icon icon="mdi-file-cog-outline" size="small" class=""></v-icon>
                      <span class="ps-2">创建新课程</span>
                    </v-card-title>
                    <v-card-subtitle>
                      请填写课程信息并选择合适的分类
                    </v-card-subtitle>
                    <v-card-text class="px-4 py-8">
                      <v-text-field 
                        v-model="applyCourseData.name" 
                        label="课程名称" 
                        variant="outlined"
                        density="compact"
                        class="mb-4"
                        required>
                      </v-text-field>
                      
                      <v-textarea 
                        v-model="applyCourseData.description" 
                        label="课程描述" 
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
                        label="主分类"
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
                        label="子分类"
                        variant="outlined"
                        density="compact"
                        class="mb-4"
                        :disabled="!applyCourseData.mainCategoryId"
                        required>
                      </v-select>
                    </v-card-text>
                    <v-card-actions class="justify-center">
                      <v-btn text="取消" @click="applyCourseDialog = false" class="px-4" variant="outlined"></v-btn>
                      <v-btn text="创建课程" @click="postApplyCourse" class="px-4" color="primary"></v-btn>
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
                              <p class="text-body-2 text-grey-darken-2 mt-2">正在加载课程...</p>
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
                              <p class="text-body-1 text-grey-darken-2 mb-2">该分类下暂无课程</p>
                              <p class="text-body-2 text-grey-darken-1">请选择其他分类或申请添加新课程</p>
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

        <v-col cols="4">

          <!-- 正在学习的课程 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4" style="border-top: 4px solid #eee;">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-school" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">正在学习的课程</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">{{ learningCourses.length }} 门课程进行中</p>
                </div>
              </div>

              <div v-if="learningCourses.length === 0" class="text-center py-4">
                <v-icon icon="mdi-book-outline" size="48" color="grey-lighten-1" class="mb-2"></v-icon>
                <p class="text-body-2 text-grey-darken-1">暂无正在学习的课程</p>
                <p class="text-caption text-grey-darken-1">开始学习一门课程吧！</p>
              </div>

              <v-responsive v-else class="overflow-y-auto" max-height="180">
                <div v-for="course in learningCourses.slice(0, 4)" :key="course.id" 
                     class="mb-3 pa-3 rounded-lg bg-white course-item" 
                     @click="openInNewTab(course.id)">
                  <div class="d-flex align-center justify-space-between mb-2">
                    <h4 class="text-body-1 font-weight-medium text-grey-darken-4">{{ course.name }}</h4>
                    <v-chip size="x-small" :color="course.progress > 50 ? 'success' : 'primary'" variant="flat">
                      {{ course.progress }}%
                    </v-chip>
                  </div>
                  <v-progress-linear 
                    :model-value="course.progress" 
                    :color="course.progress > 50 ? 'success' : 'primary'" 
                    background-color="grey-lighten-3" 
                    height="6" 
                    rounded="lg">
                  </v-progress-linear>
                </div>
              </v-responsive>
            </v-card-text>

            <v-card-actions class="px-4 pb-4" v-if="learningCourses.length > 0">
              <v-btn @click="router.push({ path: '/self', query: { tab: 'learning', learningTab: 'courses' } });"
                variant="tonal" color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                查看全部学习课程
              </v-btn>
            </v-card-actions>
          </v-card>

          <!-- 我收藏的课程 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4" style="border-top: 4px solid #eee;">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-heart" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">我收藏的课程</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">{{ subscriptions.length }} 门课程</p>
                </div>
              </div>

              <div v-if="subscriptions.length === 0" class="text-center py-4">
                <v-icon icon="mdi-heart-outline" size="48" color="grey-lighten-1" class="mb-2"></v-icon>
                <p class="text-body-2 text-grey-darken-1">暂无收藏课程</p>
                <p class="text-caption text-grey-darken-1">发现喜欢的课程就收藏吧！</p>
              </div>

              <v-responsive v-else class="overflow-y-auto" max-height="160">
                <v-chip v-for="(subscription, index) in subscriptions.slice(0, 8)" :key="subscription.id || index"
                  variant="tonal" color="primary" @click="console.log('点击的subscription对象:', subscription); openInNewTab(subscription.id)" class="my-2 me-3 px-3 py-1"
                  density="comfortable">
                  <template v-slot:prepend>
                    <v-icon icon="mdi-bookmark" size="12" color="primary" class="mr-2"></v-icon>
                  </template>
                  {{ subscription.name || '课程信息异常' }}
                </v-chip>
              </v-responsive>
            </v-card-text>

            <v-card-actions class="px-4 pb-4" v-if="subscriptions.length > 0">
              <v-btn @click="router.push({ path: '/self', query: { tab: 'subscription' } });" variant="tonal"
                color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                查看全部收藏
              </v-btn>
            </v-card-actions>
          </v-card>

          <!-- 热门课程 -->
          <v-card flat color="grey-lighten-5" class="" rounded="lg" style="border-top: 4px solid #eee;">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-fire" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">热门课程</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">收藏和学习人数排行</p>
                </div>
              </div>

              <v-list bg-color="transparent" class="pa-0">
                <v-list-item v-for="(course, index) in hotCourses.slice(0, 5)" :key="course.id" :value="course.id"
                  class="px-3 py-2 ma-1 rounded-lg course-item" :class="index < 3 ? 'bg-white' : 'bg-transparent'"
                  density="compact" @click="openInNewTab(course.id)">
                  <template v-slot:prepend>
                    <v-avatar :color="index < 3 ? 'primary' : 'grey-lighten-2'" size="24" class="mr-2">
                      <span class="text-caption font-weight-bold"
                        :class="index < 3 ? 'text-white' : 'text-grey-darken-2'">
                        {{ index + 1 }}
                      </span>
                    </v-avatar>
                  </template>

                  <v-list-item-title class="text-body-1">
                    {{ course.name }}
                  </v-list-item-title>
                  <v-list-item-subtitle class="text-caption">
                    {{ (course.learnerCount + course.subscriptionCount).toLocaleString() }} 人学习/收藏
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
              <v-btn @click="router.push('/course/ranking')" variant="tonal" color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                查看完整排行榜
              </v-btn>
            </v-card-actions>
          </v-card>


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