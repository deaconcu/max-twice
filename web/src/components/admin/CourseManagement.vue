<template>
  <div class="course-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-book-check-outline" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">课程管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理和查看课程信息</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-book" color="blue-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-blue-darken-2 text-caption">
          {{ getTabCourseCount() }}
        </span>
      </v-chip>
    </div>

    <!-- Tab导航 -->
    <v-tabs v-model="activeTab" bg-color="transparent" color="primary" class="mb-6">
      <v-tab :value="0" class="text-none">
        <v-icon icon="mdi-format-list-bulleted" size="16" class="mr-2"></v-icon>
        状态管理
      </v-tab>
      <v-tab :value="1" class="text-none">
        <v-icon icon="mdi-card-search-outline" size="16" class="mr-2"></v-icon>
        课程查询
      </v-tab>
    </v-tabs>

    <!-- Tab内容 -->
    <v-window v-model="activeTab">
      <!-- 状态管理Tab -->
      <v-window-item :value="0">
        <!-- 状态筛选 -->
        <div class="mb-6">
          <v-chip-group v-model="selectedStateIndex" color="primary" variant="flat" @update:model-value="onStateChange" mandatory>
            <v-chip
              v-for="(state, index) in stateOptions"
              :key="state.value"
              :value="index"
              :color="state.color"
              rounded="lg"
              class="me-2"
            >
              <v-icon :icon="state.icon" size="16" class="mr-1"></v-icon>
              {{ state.text }}
            </v-chip>
          </v-chip-group>
        </div>

        <!-- 分类筛选 - 只在已通过状态显示 -->
        <div v-if="getCurrentState() === COURSE_STATE.APPROVED" class="mb-6">
          <v-card flat class="pa-4 bg-grey-lighten-5" rounded="lg">
            <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
              <v-icon icon="mdi-filter-variant" size="16" class="mr-2"></v-icon>
              分类筛选
            </h4>
            <div align="center" class="ma-0 d-flex align-center w-50">
                <v-select
                  v-model="selectedMainCategory"
                  :items="mainCategories"
                  item-title="name"
                  item-value="id"
                  label="主分类"
              variant="outlined"
              density="compact"
              rounded="lg"
              bg-color="white"
              hide-details="true"
              clearable
              @update:model-value="onMainCategoryChange"
              class="me-3"
            >
              <template v-slot:prepend-inner>
                <v-icon icon="mdi-folder-outline" size="16" color="grey-darken-1"></v-icon>
              </template>
            </v-select>
            <v-select
              v-model="selectedSubCategory"
              :items="subCategories"
              item-title="name"
              item-value="id"
              label="子分类"
              variant="outlined"
              density="compact"
              rounded="lg"
              bg-color="white"
              clearable
              hide-details="true"
              :disabled="!selectedMainCategory"
              class="me-3"
              @update:model-value="onCategoryChange"
            >
              <template v-slot:prepend-inner>
                <v-icon icon="mdi-folder-multiple-outline" size="16" color="grey-darken-1"></v-icon>
              </template>
            </v-select>
            <v-btn
              v-if="selectedMainCategory || selectedSubCategory"
              variant="flat"
              color="grey-lighten-2"
              rounded="lg"
              @click="clearCategoryFilter"
            >
              <v-icon icon="mdi-filter-off" size="16" class="mr-1"></v-icon>
              清除筛选
            </v-btn>
          </div>
      </v-card>
    </div>

        <!-- 加载状态 -->
        <div v-if="loading && courseList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary"></v-progress-circular>
          <p class="mt-3 text-grey-darken-1">加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="courseList.length === 0" class="text-center py-12">
          <!-- 分类筛选提示 -->
          <div v-if="getCurrentState() === COURSE_STATE.APPROVED && selectedMainCategory && !selectedSubCategory">
            <v-icon icon="mdi-filter-outline" size="48" color="orange-lighten-1" class="mb-4"></v-icon>
            <p class="text-body-1 text-grey-darken-1 mb-2">请选择子分类</p>
            <p class="text-body-2 text-grey-darken-2">已选择主分类，请继续选择子分类以查看课程列表</p>
          </div>
          <!-- 普通空状态 -->
          <div v-else>
            <v-icon icon="mdi-book-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
            <p class="text-body-1 text-grey-darken-1">暂无{{ getCurrentStateText() }}的课程申请</p>
          </div>
        </div>

        <!-- 课程申请列表 -->
        <div v-else>
          <!-- 使用分类筛选时不启用无限滚动 -->
          <template v-if="getCurrentState() === COURSE_STATE.APPROVED && selectedMainCategory && selectedSubCategory">
            <CourseCard
              v-for="course in courseList"
              :key="course.id"
              :course="course"
              :main-categories="mainCategories"
              :category-mapping="categoryMapping"
              @edit="showEditModal"
              @approve="approveCourse"
              @reject="showRejectModal"
              @delete="deleteCourse"
              @restore="restoreCourse"
            />
          </template>
          
          <!-- 正常状态使用无限滚动 -->
          <v-infinite-scroll
            v-else
            @load="onLoadMore"
            :empty="!hasMore"
            class="course-list-container"
          >
            <CourseCard
              v-for="course in courseList"
              :key="course.id"
              :course="course"
              :main-categories="mainCategories"
              :category-mapping="categoryMapping"
              @edit="showEditModal"
              @approve="approveCourse"
              @reject="showRejectModal"
              @delete="deleteCourse"
              @restore="restoreCourse"
            />

            <template v-slot:empty>
              <div class="text-center py-4">
                <p class="text-grey-darken-1 mt-2"> - 已加载全部数据 - </p>
              </div>
            </template>

            <template v-slot:loading>
              <div class="text-center py-4">
                <v-progress-circular indeterminate color="primary"></v-progress-circular>
                <p class="text-grey-darken-1 mt-2">正在加载...</p>
              </div>
            </template>
          </v-infinite-scroll>
        </div>
      </v-window-item>

      <!-- ID查询Tab -->
      <v-window-item :value="1">
        <!-- 搜索区域 -->
        <div class="mb-6">
          <v-card flat class="pa-4 bg-grey-lighten-5" rounded="lg">
            <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
              <v-icon icon="mdi-card-search-outline" size="16" class="mr-2"></v-icon>
              通过ID查询课程
            </h4>
            <p class="text-body-2 text-grey-darken-1 mb-3">
              输入课程ID进行查询，如果是父课程会同时显示所有子课程
            </p>
            <div class="d-flex align-center">
              <v-text-field
                v-model="searchCourseId"
                label="输入课程ID"
                variant="outlined"
                density="compact"
                rounded="lg"
                bg-color="white"
                hide-details="auto"
                type="number"
                class="me-3"
                @keyup.enter="searchCourseById"
                :rules="[v => !v || (v > 0) || '请输入有效的课程ID']"
              >
                <template v-slot:prepend-inner>
                  <v-icon icon="mdi-identifier" size="16" color="grey-darken-1"></v-icon>
                </template>
              </v-text-field>
              <v-btn
                variant="flat"
                color="primary"
                rounded="lg"
                @click="searchCourseById"
                :loading="searchLoading"
                :disabled="!searchCourseId || searchCourseId <= 0"
              >
                <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
                查询
              </v-btn>
            </div>
          </v-card>
        </div>

        <!-- 搜索加载状态 -->
        <div v-if="searchLoading" class="text-center py-8">
          <v-progress-circular indeterminate color="primary"></v-progress-circular>
          <p class="mt-3 text-grey-darken-1">查询中...</p>
        </div>

        <!-- 搜索结果 -->
        <div v-else-if="searchedCourse">
          <!-- 查询到的课程卡片 -->
          <div class="mb-6">
            <div v-if="subcourseList.length > 0" class="d-flex align-center mb-3">
              <v-icon icon="mdi-folder-account-outline" size="16" color="blue-darken-1" class="mr-2"></v-icon>
              <span class="text-subtitle-2 text-blue-darken-1 font-weight-medium">查询课程（父课程）</span>
            </div>
            <div v-else-if="searchedCourse.parent" class="d-flex align-center mb-3">
              <v-icon icon="mdi-file-document-outline" size="16" color="green-darken-1" class="mr-2"></v-icon>
              <span class="text-subtitle-2 text-green-darken-1 font-weight-medium">查询课程（子课程）</span>
            </div>
            <div v-else class="d-flex align-center mb-3">
              <v-icon icon="mdi-book-outline" size="16" color="grey-darken-1" class="mr-2"></v-icon>
              <span class="text-subtitle-2 text-grey-darken-1 font-weight-medium">查询课程</span>
            </div>
            
            <CourseCard
              :course="searchedCourse"
              :main-categories="mainCategories"
              :category-mapping="categoryMapping"
              @edit="showEditModal"
              @approve="approveCourse"
              @reject="showRejectModal"
              @delete="deleteCourse"
              @restore="restoreCourse"
            />
          </div>
          
          <!-- 子课程列表（如果查询的是父课程） -->
          <div v-if="subcourseList.length > 0">
            <div class="mb-4 d-flex align-center">
              <v-icon icon="mdi-file-tree" size="16" color="green-darken-1" class="mr-2"></v-icon>
              <span class="text-subtitle-2 text-green-darken-1 font-weight-medium">
                子课程列表 ({{ subcourseList.length }}个)
              </span>
            </div>
            
            <CourseCard
              v-for="course in subcourseList"
              :key="course.id"
              :course="course"
              :main-categories="mainCategories"
              :category-mapping="categoryMapping"
              @edit="showEditModal"
              @approve="approveCourse"
              @reject="showRejectModal"
              @delete="deleteCourse"
              @restore="restoreCourse"
            />
          </div>
        </div>

        <!-- 搜索空状态 -->
        <div v-else-if="searchAttempted" class="text-center py-12">
          <v-icon icon="mdi-file-search-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">未找到ID为 {{ searchCourseId }} 的课程</p>
          <p class="text-body-2 text-grey-darken-2">请检查课程ID是否正确</p>
        </div>

        <!-- 初始状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-card-search" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">输入课程ID进行查询</p>
          <p class="text-body-2 text-grey-darken-2">可以查询任意状态的课程并进行管理操作</p>
        </div>
      </v-window-item>
    </v-window>

    <!-- 拒绝对话框 -->
    <v-dialog v-model="rejectDialog" max-width="500px" persistent>
      <v-card rounded="lg">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center">
            <v-icon icon="mdi-close-circle-outline" color="red-darken-1" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">拒绝课程申请</span>
          </div>
        </v-card-title>
        
        <v-card-text class="pa-6 pt-2">
          <p class="text-body-2 text-grey-darken-1 mb-4">
            确定要拒绝课程 "{{ selectedCourse?.name }}" 吗？
          </p>
          
          <v-textarea
            v-model="rejectReason"
            label="拒绝原因"
            variant="outlined"
            rows="4"
            rounded="lg"
            bg-color="grey-lighten-5"
            :rules="[v => !!v || '请输入拒绝原因']"
            class="mb-0"
          ></v-textarea>
        </v-card-text>
        
        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn
            variant="flat"
            color="grey-lighten-2"
            rounded="lg"
            @click="closeRejectModal"
          >
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="red-lighten-4"
            rounded="lg"
            @click="confirmReject"
            :loading="rejecting"
            :disabled="!rejectReason.trim()"
          >
            <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
            确认拒绝
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 编辑对话框 -->
    <v-dialog v-model="editDialog" max-width="600px" persistent>
      <v-card rounded="lg">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center">
            <v-icon icon="mdi-pencil-circle-outline" color="blue-darken-1" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">编辑课程信息</span>
          </div>
        </v-card-title>
        
        <v-card-text class="pa-6 pt-2">
          <v-form ref="editForm" v-model="editFormValid">
            <v-text-field
              v-model="editCourseData.name"
              label="课程名称"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              :rules="[v => !!v || '请输入课程名称']"
              class="mb-4"
            ></v-text-field>
            
            <v-textarea
              v-model="editCourseData.description"
              label="课程描述"
              variant="outlined"
              rows="4"
              rounded="lg"
              bg-color="grey-lighten-5"
              :rules="[v => !!v || '请输入课程描述']"
              class="mb-4"
            ></v-textarea>

            <v-row>
              <v-col cols="6">
                <v-select
                  v-model="editCourseData.mainCategory"
                  :items="mainCategories"
                  item-title="name"
                  item-value="id"
                  label="主分类"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  :rules="[v => !!v || '请选择主分类']"
                  @update:model-value="onEditMainCategoryChange"
                ></v-select>
              </v-col>
              <v-col cols="6">
                <v-select
                  v-model="editCourseData.subCategory"
                  :items="editSubCategories"
                  item-title="name"
                  item-value="id"
                  label="子分类"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  :rules="[v => !!v || '请选择子分类']"
                  :disabled="!editCourseData.mainCategory"
                ></v-select>
              </v-col>
            </v-row>
          </v-form>
        </v-card-text>
        
        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn
            variant="flat"
            color="grey-lighten-2"
            rounded="lg"
            @click="closeEditModal"
          >
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="blue-lighten-4"
            rounded="lg"
            @click="confirmEdit"
            :loading="editing"
            :disabled="!editFormValid"
          >
            <v-icon icon="mdi-check" color="blue-darken-2" size="16" class="mr-1"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import { courseServiceV1, systemServiceV1 } from '@/services/api/v1/apiServiceV1'
import { COURSE_STATE } from '@/constants/statusConstants'
import CourseCard from './CourseCard.vue'
import { useI18n } from 'vue-i18n'

const showSnackbar = inject('showSnackbar')
const { t } = useI18n()

// 状态选项
const stateOptions = [
  { value: COURSE_STATE.SUBMITTED, text: '待审核', color: 'orange-lighten-4', icon: 'mdi-clock-outline' },
  { value: COURSE_STATE.APPROVED, text: '已批准', color: 'green-lighten-4', icon: 'mdi-check-circle-outline' },
  { value: COURSE_STATE.REJECTED, text: '已拒绝', color: 'red-lighten-4', icon: 'mdi-close-circle-outline' },
]

// 响应式数据
const activeTab = ref(0)
const courseList = ref([])
const selectedStateIndex = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const lastId = ref(0)

// ID查询相关
const searchCourseId = ref('')
const searchedCourse = ref(null)
const searchLoading = ref(false)
const searchAttempted = ref(false)

// 子课程列表（用于显示查询课程的子课程）
const subcourseList = ref([])

// 分类数据
const mainCategories = ref([])
const subCategories = ref([])
const selectedMainCategory = ref(null)
const selectedSubCategory = ref(null)
const categoryMapping = ref([])

// 拒绝对话框相关
const rejectDialog = ref(false)
const selectedCourse = ref(null)
const rejectReason = ref('')
const rejecting = ref(false)

// 编辑对话框相关
const editDialog = ref(false)
const editFormValid = ref(false)
const editForm = ref(null)
const editing = ref(false)
const editCourseData = ref({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null
})
const editSubCategories = ref([])

// 获取当前选中的状态
const getCurrentState = () => stateOptions[selectedStateIndex.value].value
const getCurrentStateText = () => stateOptions[selectedStateIndex.value].text

// 获取状态配置
const getStateConfig = (state) => {
  return stateOptions.find(option => option.value === state) || stateOptions[0]
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 获取当前Tab的课程数量显示
const getTabCourseCount = () => {
  switch (activeTab.value) {
    case 0:
      return courseList.value.length + '个课程'
    case 1:
      if (searchedCourse.value) {
        const totalCount = 1 + subcourseList.value.length
        return totalCount === 1 ? '1个课程' : `${totalCount}个课程`
      }
      return '0个课程'
    default:
      return '0个课程'
  }
}

// 加载课程列表
const loadCourses = async (isLoadMore = false) => {
  try {
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
      // 只有在非加载更多时才重置数据
      courseList.value = []
      lastId.value = 0
      hasMore.value = true
    }

    const currentState = getCurrentState()
    let response;

    // 如果是已通过状态且选择了主分类但没有选择子分类，不加载
    if (currentState === COURSE_STATE.APPROVED && selectedMainCategory.value && !selectedSubCategory.value) {
      courseList.value = []
      hasMore.value = false
      loading.value = false
      loadingMore.value = false
      return
    }

    // 如果是已通过状态且选择了主分类和子分类，使用分类查询（不支持分页）
    if (currentState === COURSE_STATE.APPROVED && selectedMainCategory.value && selectedSubCategory.value) {
      response = await courseServiceV1.getCoursesByCategory(selectedMainCategory.value, selectedSubCategory.value)
      // 分类查询不支持下拉刷新
      hasMore.value = false
    } else {
      // 使用lastId进行分页查询
      response = await courseServiceV1.getCoursesByState(currentState, isLoadMore ? lastId.value : 0)
    }

    if (response.code === 200) {
      const newCourses = response.data || []
      console.log('Loaded courses:', newCourses)
      
      if (isLoadMore) {
        courseList.value.push(...newCourses)
      } else {
        courseList.value = newCourses
      }

      // 更新lastId和hasMore状态（仅在状态查询时）
      if (currentState !== COURSE_STATE.APPROVED || !selectedMainCategory.value || !selectedSubCategory.value) {
        if (newCourses.length > 0) {
          lastId.value = newCourses[newCourses.length - 1].id
          // 如果返回的数据少于期望的数量，说明没有更多数据了
          hasMore.value = newCourses.length >= 10
        } else {
          hasMore.value = false
        }
      }
    } else {
      console.error('Failed to load courses:', response)
      showSnackbar('加载课程失败: ', response.msg || '未知错误')
    }
  } catch (error) {
    console.error('Error loading courses:', error)
    showSnackbar('加载课程失败: 服务器开小差了')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// v-infinite-scroll 加载更多回调
const onLoadMore = async ({ done }) => {
  if (!hasMore.value || loadingMore.value) {
    done('empty')
    return
  }

  try {
    await loadCourses(true)
    if (hasMore.value) {
      done('ok')
    } else {
      done('empty')
    }
  } catch (error) {
    console.error('Error loading more courses:', error)
    showSnackbar('加载更多失败', 'error')
    done('error')
  }
}

// ID查询相关方法
const searchCourseById = async () => {
  if (!searchCourseId.value || searchCourseId.value <= 0) {
    showSnackbar('请输入有效的课程ID', 'error')
    return
  }
  
  searchLoading.value = true
  searchAttempted.value = true
  searchedCourse.value = null
  subcourseList.value = []
  
  try {
    const response = await courseServiceV1.getCourse(searchCourseId.value)
    
    if (response.code === 200) {
      searchedCourse.value = response.data
      
      // 自动查询子课程（如果可能是父课程）
      try {
        const subcourseResponse = await courseServiceV1.getCoursesByState(null, null, null, null, searchCourseId.value)
        if (subcourseResponse.code === 200) {
          subcourseList.value = subcourseResponse.data || []
        }
      } catch (error) {
        console.warn('查询子课程失败:', error)
        subcourseList.value = []
      }
      
      if (subcourseList.value.length > 0) {
        showSnackbar(`查询成功，找到 ${subcourseList.value.length} 个子课程`, 'success')
      } else {
        showSnackbar('查询成功', 'success')
      }
    } else {
      searchedCourse.value = null
      showSnackbar('未找到该课程', 'warning')
    }
  } catch (error) {
    console.error('查询课程失败:', error)
    searchedCourse.value = null
    subcourseList.value = []
    showSnackbar('查询失败，请重试', 'error')
  } finally {
    searchLoading.value = false
  }
}

// 加载课程分类数据
const loadCourseCategories = async () => {
  try {
    const response = await systemServiceV1.getCourseCategories()
    
    if (response.code === 200 && response.data) {
      // 新的key-value格式：数据是JSON字符串
      const { mainCategories: categories, categoryMapping: mapping } = response.data;
      
      mainCategories.value = categories;
      categoryMapping.value = mapping;
      
      // 初始化子分类
      if (categories.length > 0) {
        const firstMapping = mapping.find(m => m.mainCategoryId === categories[0].id);
        subCategories.value = firstMapping ? firstMapping.subCategories : [];
      }
    }
  } catch (error) {
    console.error('Error loading course categories:', error)
  }
}

// 状态变化处理
const onStateChange = () => {
  // 清除分类筛选
  selectedMainCategory.value = null
  selectedSubCategory.value = null
  loadCourses()
}

// 分类变化处理
const onCategoryChange = () => {
  if (getCurrentState() === COURSE_STATE.APPROVED) {
    // 只有选择了主分类和子分类时才加载
    if (selectedMainCategory.value && selectedSubCategory.value) {
      loadCourses()
    }
  }
}

// 清除分类筛选
const clearCategoryFilter = () => {
  selectedMainCategory.value = null
  selectedSubCategory.value = null
  subCategories.value = categoryMapping.value.length > 0 ? categoryMapping.value[0].subCategories : []
  loadCourses()
}

// 主分类变化时更新子分类
const updateSubCategories = (mainCategoryId) => {
  const mapping = categoryMapping.value.find(m => m.mainCategoryId === mainCategoryId)
  subCategories.value = mapping ? mapping.subCategories : []
  
  // 默认选择第一个子分类
  if (subCategories.value.length > 0) {
    selectedSubCategory.value = subCategories.value[0].id
  } else {
    selectedSubCategory.value = null
  }
}

// 主分类变化事件处理
const onMainCategoryChange = (newValue) => {
  selectedMainCategory.value = newValue
  if (newValue) {
    updateSubCategories(newValue)
    // 如果有子分类且已选择第一个，直接加载课程
    if (getCurrentState() === COURSE_STATE.APPROVED && selectedSubCategory.value) {
      loadCourses()
    } else if (getCurrentState() === COURSE_STATE.APPROVED) {
      // 没有子分类，清空列表
      courseList.value = []
    }
  } else {
    subCategories.value = []
    selectedSubCategory.value = null
    // 清除主分类选择，重新加载全部数据
    if (getCurrentState() === COURSE_STATE.APPROVED) {
      loadCourses()
    }
  }
}

// 通过课程
const approveCourse = async (course) => {
  try {
    course.approving = true
    const response = await courseServiceV1.approveCourse(course.id, 'approve')
    
    if (response.code === 200) {
      showSnackbar('课程已通过审核')
      // 从当前列表中移除（如果当前不是已通过状态）
      if (getCurrentState() !== COURSE_STATE.APPROVED) {
        const index = courseList.value.findIndex(c => c.id === course.id)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        // 更新状态
        course.state = COURSE_STATE.APPROVED
      }
      
      // 如果当前在ID查询tab且查询的是同一个课程，更新搜索结果
      if (activeTab.value === 1 && searchedCourse.value && searchedCourse.value.id === course.id) {
        searchedCourse.value.state = COURSE_STATE.APPROVED
      }
      
      // 如果当前在子课程查询tab且列表中包含该课程，更新状态
      if (activeTab.value === 2) {
        const subcourseIndex = subcourseList.value.findIndex(c => c.id === course.id)
        if (subcourseIndex > -1) {
          subcourseList.value[subcourseIndex].state = COURSE_STATE.APPROVED
        }
      }
    } else {
      showSnackbar('操作失败: ', response.msg || '未知错误')
    }
  } catch (error) {
    console.error('Error approving course:', error)
    showSnackbar('操作失败: 服务器开小差了')
  } finally {
    course.approving = false
  }
}

// 删除课程
const deleteCourse = async (course) => {
  if (!confirm(`确定要删除课程 "${course.name}" 吗？此操作不可撤销！`)) {
    return
  }

  try {
    course.deleting = true
    const response = await courseServiceV1.approveCourse(course.id, 'delete')
    
    if (response.code === 200) {
      showSnackbar('课程已删除')
      // 从列表中移除课程
      const index = courseList.value.findIndex(c => c.id === course.id)
      if (index > -1) {
        courseList.value.splice(index, 1)
      }
      
      // 如果当前在ID查询tab且查询的是同一个课程，清空搜索结果
      if (activeTab.value === 1 && searchedCourse.value && searchedCourse.value.id === course.id) {
        searchedCourse.value = null
        searchAttempted.value = false
      }
      
      // 如果当前在子课程查询tab且列表中包含该课程，从列表中移除
      if (activeTab.value === 2) {
        const subcourseIndex = subcourseList.value.findIndex(c => c.id === course.id)
        if (subcourseIndex > -1) {
          subcourseList.value.splice(subcourseIndex, 1)
        }
      }
    } else {
      showSnackbar('删除失败: ', response.msg || '未知错误')
    }
  } catch (error) {
    console.error('Error deleting course:', error)
    showSnackbar('删除失败: 服务器开小差了')
  } finally {
    course.deleting = false
  }
}

// 恢复课程（从拒绝状态恢复）
const restoreCourse = async (course) => {
  try {
    course.restoring = true
    const response = await courseServiceV1.approveCourse(course.id, 'restore')
    
    if (response.code === 200) {
      showSnackbar('课程已恢复')
      // 从当前列表中移除（如果当前是拒绝状态）
      if (getCurrentState() === COURSE_STATE.REJECTED) {
        const index = courseList.value.findIndex(c => c.id === course.id)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        // 更新状态（恢复后通常变为待审核状态）
        course.state = COURSE_STATE.SUBMITTED
      }
      
      // 如果当前在ID查询tab且查询的是同一个课程，更新搜索结果
      if (activeTab.value === 1 && searchedCourse.value && searchedCourse.value.id === course.id) {
        searchedCourse.value.state = COURSE_STATE.SUBMITTED
        delete searchedCourse.value.rejectedReason
      }
      
      // 如果当前在子课程查询tab且列表中包含该课程，更新状态
      if (activeTab.value === 2) {
        const subcourseIndex = subcourseList.value.findIndex(c => c.id === course.id)
        if (subcourseIndex > -1) {
          subcourseList.value[subcourseIndex].state = COURSE_STATE.SUBMITTED
          delete subcourseList.value[subcourseIndex].rejectedReason
        }
      }
    } else {
      showSnackbar('恢复失败: ', response.msg || '未知错误')
    }
  } catch (error) {
    console.error('Error restoring course:', error)
    showSnackbar('恢复失败: 服务器开小差了')
  } finally {
    course.restoring = false
  }
}

// 显示编辑对话框
const showEditModal = (course) => {
  selectedCourse.value = course
  editCourseData.value = {
    name: course.name,
    description: course.description,
    mainCategory: course.mainCategory,
    subCategory: course.subCategory
  }
  
  // 更新编辑表单的子分类选项
  onEditMainCategoryChange()
  editDialog.value = true
}

// 编辑表单主分类变化
const onEditMainCategoryChange = () => {
  const mapping = categoryMapping.value.find(m => m.mainCategoryId === editCourseData.value.mainCategory)
  editSubCategories.value = mapping ? mapping.subCategories : []
  
  // 如果当前选择的子分类不在新的子分类列表中，清除选择
  if (editCourseData.value.subCategory && 
      !editSubCategories.value.find(sub => sub.id === editCourseData.value.subCategory)) {
    editCourseData.value.subCategory = null
  }
}

// 关闭编辑对话框
const closeEditModal = () => {
  editDialog.value = false
  selectedCourse.value = null
  editCourseData.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null
  }
  editSubCategories.value = []
}

// 确认编辑
const confirmEdit = async () => {
  if (!editFormValid.value) return

  try {
    editing.value = true
    const response = await courseServiceV1.updateCourse(selectedCourse.value.id, {
      name: editCourseData.value.name,
      description: editCourseData.value.description,
      mainCategory: editCourseData.value.mainCategory,
      subCategory: editCourseData.value.subCategory
    })
    
    if (response.code === 200) {
      showSnackbar('课程信息已更新')
      
      // 更新列表中的课程信息
      const index = courseList.value.findIndex(c => c.id === selectedCourse.value.id)
      if (index > -1) {
        Object.assign(courseList.value[index], editCourseData.value)
      }
      
      // 如果当前在ID查询tab且查询的是同一个课程，更新搜索结果
      if (activeTab.value === 1 && searchedCourse.value && searchedCourse.value.id === selectedCourse.value.id) {
        Object.assign(searchedCourse.value, editCourseData.value)
      }
      
      // 如果当前在子课程查询tab且列表中包含该课程，更新子课程列表
      if (activeTab.value === 2) {
        const subcourseIndex = subcourseList.value.findIndex(c => c.id === selectedCourse.value.id)
        if (subcourseIndex > -1) {
          Object.assign(subcourseList.value[subcourseIndex], editCourseData.value)
        }
      }
      
      closeEditModal()
    } else {
      showSnackbar('更新失败', 'error')
    }
  } catch (error) {
    console.error('Error updating course:', error)
    showSnackbar('更新失败', 'error')
  } finally {
    editing.value = false
  }
}
const showRejectModal = (course) => {
  selectedCourse.value = course
  rejectReason.value = ''
  rejectDialog.value = true
}

// 关闭拒绝对话框
const closeRejectModal = () => {
  rejectDialog.value = false
  selectedCourse.value = null
  rejectReason.value = ''
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectReason.value.trim()) return

  try {
    rejecting.value = true
    const action = 'reject'
    const response = await courseServiceV1.approveCourse(
      selectedCourse.value.id, 
      action, 
      rejectReason.value.trim()
    )
    
    if (response.code === 200) {
      showSnackbar(selectedCourse.value.state === COURSE_STATE.APPROVED ? '已撤销通过' : '已拒绝申请')
      
      // 从当前列表中移除（如果当前不是拒绝状态）
      if (getCurrentState() !== COURSE_STATE.REJECTED) {
        const index = courseList.value.findIndex(c => c.id === selectedCourse.value.id)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        // 更新状态
        selectedCourse.value.state = COURSE_STATE.REJECTED
        selectedCourse.value.rejectedReason = rejectReason.value.trim()
      }
      
      // 如果当前在ID查询tab且查询的是同一个课程，更新搜索结果
      if (activeTab.value === 1 && searchedCourse.value && searchedCourse.value.id === selectedCourse.value.id) {
        searchedCourse.value.state = COURSE_STATE.REJECTED
        searchedCourse.value.rejectedReason = rejectReason.value.trim()
      }
      
      // 如果当前在子课程查询tab且列表中包含该课程，更新状态
      if (activeTab.value === 2) {
        const subcourseIndex = subcourseList.value.findIndex(c => c.id === selectedCourse.value.id)
        if (subcourseIndex > -1) {
          subcourseList.value[subcourseIndex].state = COURSE_STATE.REJECTED
          subcourseList.value[subcourseIndex].rejectedReason = rejectReason.value.trim()
        }
      }
      
      closeRejectModal()
    } else {
      showSnackbar('操作失败: ' + (response.msg || '未知错误'))
    }
  } catch (error) {
    console.error('Error rejecting course:', error)
    showSnackbar('操作失败：服务器开小差了')
  } finally {
    rejecting.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadCourseCategories()
  loadCourses()
})
</script>

<style scoped>
.course-management {
  max-width: 100%;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.course-list-container {
  padding: 0 4px;
}
</style>
