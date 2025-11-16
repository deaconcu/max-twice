<script setup lang="ts">
import { inject, onMounted, ref, computed } from 'vue'
import { courseApi, adminApi, systemApi } from '@/api'
import { ContentState } from '@/enums'
import type { Course } from '@/types/course.d'
import type { StateOption } from '@/types/common.d'
import CourseCard from './CourseCard.vue'
import RejectBanDialog from './RejectBanDialog.vue'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 编辑课程数据接口
interface EditCourseData {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
}

// 无限滚动回调接口
type InfiniteScrollCallback = (status: 'ok' | 'empty' | 'error') => void

// 状态选项
const stateOptions: StateOption[] = [
  {
    value: ContentState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline',
  },
  {
    value: ContentState.PUBLISHED,
    text: '已批准',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle-outline',
  },
  {
    value: ContentState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle-outline',
  },
  {
    value: ContentState.BANNED,
    text: '已屏蔽',
    color: 'grey-lighten-2',
    icon: 'mdi-cancel',
  },
]

// 响应式数据
const courseList = ref<Course[]>([])
const selectedStateIndex = ref<number>(0)
const loading = ref<boolean>(false)
const loadingMore = ref<boolean>(false)
const hasMore = ref<boolean>(true)
const lastId = ref<number>(0)

// ID查询相关
const searchCourseId = ref<string>('')
const searchedCourse = ref<Course | null>(null)
const searchLoading = ref<boolean>(false)
const searchAttempted = ref<boolean>(false)

// 子课程列表（用于显示查询课程的子课程）
const subcourseList = ref<Course[]>([])

// 分类数据
const mainCategories = ref<any[]>([])
const subCategories = ref<any[]>([])
const selectedMainCategory = ref<number | null>(null)
const selectedSubCategory = ref<number | null>(null)
const categoryMapping = ref<any[]>([])

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const selectedCourse = ref<Course | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 编辑对话框相关
const editDialog = ref<boolean>(false)
const editFormValid = ref<boolean>(false)
const editForm = ref<any>(null)
const editing = ref<boolean>(false)
const editCourseData = ref<EditCourseData>({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null,
})
const editSubCategories = ref<any[]>([])

// 显示拒绝对话框
const showRejectDialog = (course: Course): void => {
  selectedCourse.value = course
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 获取当前选中的状态
const getCurrentState = (): number => stateOptions[selectedStateIndex.value].value
const getCurrentStateText = (): string => stateOptions[selectedStateIndex.value].text

// 获取当前Tab的课程数量显示
const getTabCourseCount = (): string => {
  // 如果有查询结果，显示查询到的课程数量
  if (searchedCourse.value) {
    const totalCount = 1 + subcourseList.value.length
    return totalCount === 1 ? '1个课程' : `${totalCount}个课程`
  }
  // 否则显示当前状态下的课程数量
  return `${courseList.value.length}个课程`
}

// 加载课程列表
const loadCourses = async (isLoadMore = false): Promise<void> => {
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
    let response

    // 如果是已通过状态且选择了主分类但没有选择子分类，不加载
    if (
      currentState === ContentState.PUBLISHED &&
      selectedMainCategory.value &&
      !selectedSubCategory.value
    ) {
      courseList.value = []
      hasMore.value = false
      loading.value = false
      loadingMore.value = false
      return
    }

    // 如果是已通过状态且选择了主分类和子分类，使用分类查询（不支持分页）
    if (
      currentState === ContentState.PUBLISHED &&
      selectedMainCategory.value &&
      selectedSubCategory.value
    ) {
      response = await adminApi.getAdminCourses(
        currentState,
        null,
        selectedMainCategory.value,
        selectedSubCategory.value
      )
      // 分类查询不支持下拉刷新
      hasMore.value = false
    } else {
      // 使用lastId进行分页查询
      const currentLastId = isLoadMore ? lastId.value : null
      response = await adminApi.getAdminCourses(currentState, currentLastId)
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
      if (
        currentState !== ContentState.PUBLISHED ||
        !selectedMainCategory.value ||
        !selectedSubCategory.value
      ) {
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
      showSnackbar?.('加载课程失败: ' + (response.msg || '未知错误'))
    }
  } catch (error) {
    console.error('Error loading courses:', error)
    showSnackbar?.('加载课程失败: 服务器开小差了')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// v-infinite-scroll 加载更多回调
const onLoadMore = async ({ done }: { done: InfiniteScrollCallback }): Promise<void> => {
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
    showSnackbar?.('加载更多失败', 'error')
    done('error')
  }
}

// ID查询相关方法
const clearSearch = (): void => {
  searchCourseId.value = ''
  searchedCourse.value = null
  searchAttempted.value = false
  subcourseList.value = []
}

const searchCourseById = async (): Promise<void> => {
  if (!searchCourseId.value || Number(searchCourseId.value) <= 0) {
    showSnackbar?.('请输入有效的课程ID', 'error')
    return
  }

  searchLoading.value = true
  searchAttempted.value = true
  searchedCourse.value = null
  subcourseList.value = []

  try {
    const response = await adminApi.getAdminCourse(Number(searchCourseId.value))

    if (response.code === 200) {
      searchedCourse.value = response.data

      // 自动查询子课程（如果可能是父课程）
      try {
        const subcourseResponse = await adminApi.getAdminSubcourses(
          Number(searchCourseId.value),
          undefined
        )
        if (subcourseResponse.code === 200) {
          subcourseList.value = subcourseResponse.data || []
        }
      } catch (error) {
        console.warn('查询子课程失败:', error)
        subcourseList.value = []
      }

      if (subcourseList.value.length > 0) {
        showSnackbar?.(`查询成功，找到 ${subcourseList.value.length} 个子课程`, 'success')
      } else {
        showSnackbar?.('查询成功', 'success')
      }
    } else {
      searchedCourse.value = null
      showSnackbar?.('未找到该课程', 'warning')
    }
  } catch (error) {
    console.error('查询课程失败:', error)
    searchedCourse.value = null
    subcourseList.value = []
    showSnackbar?.('查询失败，请重试', 'error')
  } finally {
    searchLoading.value = false
  }
}

// 加载课程分类数据
const loadCourseCategories = async (): Promise<void> => {
  try {
    const response = await systemApi.getCourseCategories()

    if (response.code === 200 && response.data) {
      const { mainCategories: categories, categoryMapping: mapping } = response.data

      mainCategories.value = categories
      categoryMapping.value = mapping

      // 初始化子分类
      if (categories.length > 0) {
        const firstMapping = mapping.find((m: any) => m.mainCategoryId === categories[0].id)
        subCategories.value = firstMapping ? firstMapping.subCategories : []
      }
    }
  } catch (error) {
    console.error('Error loading course categories:', error)
  }
}

// 状态变化处理
const onStateChange = (): void => {
  // 清除分类筛选
  selectedMainCategory.value = null
  selectedSubCategory.value = null
  loadCourses()
}

// 分类变化处理
const onCategoryChange = (): void => {
  if (getCurrentState() === ContentState.PUBLISHED) {
    // 只有选择了主分类和子分类时才加载
    if (selectedMainCategory.value && selectedSubCategory.value) {
      loadCourses()
    }
  }
}

// 清除分类筛选
const clearCategoryFilter = (): void => {
  selectedMainCategory.value = null
  selectedSubCategory.value = null
  subCategories.value =
    categoryMapping.value.length > 0 ? categoryMapping.value[0].subCategories : []
  loadCourses()
}

// 主分类变化时更新子分类
const updateSubCategories = (mainCategoryId: number): void => {
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === mainCategoryId)
  subCategories.value = mapping ? mapping.subCategories : []

  // 默认选择第一个子分类
  if (subCategories.value.length > 0) {
    selectedSubCategory.value = subCategories.value[0].id
  } else {
    selectedSubCategory.value = null
  }
}

// 主分类变化事件处理
const onMainCategoryChange = (newValue: number | null): void => {
  selectedMainCategory.value = newValue
  if (newValue) {
    updateSubCategories(newValue)
    // 如果有子分类且已选择第一个，直接加载课程
    if (getCurrentState() === ContentState.PUBLISHED && selectedSubCategory.value) {
      loadCourses()
    } else if (getCurrentState() === ContentState.PUBLISHED) {
      // 没有子分类，清空列表
      courseList.value = []
    }
  } else {
    subCategories.value = []
    selectedSubCategory.value = null
    // 清除主分类选择，重新加载全部数据
    if (getCurrentState() === ContentState.PUBLISHED) {
      loadCourses()
    }
  }
}

// 使用 useMutation 通过课程
const { execute: executeApproveCourse } = useMutation(
  (courseId: number) => courseApi.approveCourse(courseId, 'APPROVE'),
  {
    successMessage: '课程已通过审核',
    onSuccess: (_, courseId) => {
      // 从当前列表中移除（如果当前不是已通过状态）
      if (getCurrentState() !== ContentState.PUBLISHED) {
        const index = courseList.value.findIndex((c) => c.id === courseId)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        // 更新状态
        const course = courseList.value.find((c) => c.id === courseId)
        if (course) course.state = ContentState.PUBLISHED
      }

      // 如果当前在ID查询且查询的是同一个课程，更新搜索结果
      if (searchedCourse.value?.id === courseId) {
        searchedCourse.value.state = ContentState.PUBLISHED
      }

      // 如果当前在子课程查询且列表中包含该课程，更新状态
      const subcourseIndex = subcourseList.value.findIndex((c) => c.id === courseId)
      if (subcourseIndex > -1) {
        subcourseList.value[subcourseIndex].state = ContentState.PUBLISHED
      }
    },
  }
)

// 通过课程
const approveCourse = async (course: Course): Promise<void> => {
  await executeApproveCourse(course.id)
}

// 显示屏蔽对话框
const banCourse = (course: Course): void => {
  selectedCourse.value = course
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { courseId: number; action: string; reason: string }) =>
    courseApi.approveCourse(data.courseId, data.action, data.reason),
  {
    onSuccess: (_, data) => {
      const message = data.action === 'BAN' ? '已屏蔽' : '已拒绝'
      const targetState = data.action === 'BAN' ? ContentState.BANNED : ContentState.REJECTED
      showSnackbar?.(message, 'success')

      // 从当前列表中移除（如果在对应状态tab中）
      const currentState = getCurrentState()
      const index = courseList.value.findIndex((c) => c.id === data.courseId)
      if (index > -1) {
        if (currentState !== targetState) {
          courseList.value.splice(index, 1)
        } else {
          courseList.value[index].state = targetState
        }
      }

      // 如果是搜索课程，更新状态
      if (searchedCourse.value && searchedCourse.value.id === data.courseId) {
        searchedCourse.value.state = targetState
      }

      // 更新子课程列表状态
      const subcourseIndex = subcourseList.value.findIndex((c) => c.id === data.courseId)
      if (subcourseIndex > -1) {
        subcourseList.value[subcourseIndex].state = targetState
      }

      showReasonDialog.value = false
      selectedCourse.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string): Promise<void> => {
  if (!selectedCourse.value) return

  const action = dialogType.value === 'ban' ? 'BAN' : 'REJECT'
  await executeRejectOrBan({
    courseId: selectedCourse.value.id,
    action,
    reason,
  })
}

// 取消屏蔽课程
const unbanCourse = async (course: Course): Promise<void> => {
  try {
    const response = await courseApi.approveCourse(course.id, 'APPROVE')

    if (response.code === 200) {
      showSnackbar?.('已取消屏蔽')
      // 从当前列表中移除（如果当前是屏蔽状态）
      if (getCurrentState() === ContentState.BANNED) {
        const index = courseList.value.findIndex((c) => c.id === course.id)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        // 更新状态
        course.state = ContentState.PUBLISHED
        delete (course as any).reason
      }
    } else {
      showSnackbar?.('操作失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('Error unbanning course:', error)
    showSnackbar?.('操作失败: 服务器开小差了')
  }
}

// 显示编辑对话框
const showEditModal = (course: Course): void => {
  selectedCourse.value = course
  editCourseData.value = {
    name: course.name,
    description: course.description || '',
    mainCategory: course.mainCategory || null,
    subCategory: course.subCategory || null,
  }

  // 更新编辑表单的子分类选项
  onEditMainCategoryChange()
  editDialog.value = true
}

// 编辑表单主分类变化
const onEditMainCategoryChange = (): void => {
  const mapping = categoryMapping.value.find(
    (m) => m.mainCategoryId === editCourseData.value.mainCategory
  )
  editSubCategories.value = mapping ? mapping.subCategories : []

  // 如果当前选择的子分类不在新的子分类列表中，清除选择
  if (
    editCourseData.value.subCategory &&
    !editSubCategories.value.find((sub) => sub.id === editCourseData.value.subCategory)
  ) {
    editCourseData.value.subCategory = null
  }
}

// 关闭编辑对话框
const closeEditModal = (): void => {
  editDialog.value = false
  selectedCourse.value = null
  editCourseData.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null,
  }
  editSubCategories.value = []
}

// 确认编辑
const confirmEdit = async (): Promise<void> => {
  if (!editFormValid.value) return

  try {
    editing.value = true
    const response = await courseApi.updateCourse(selectedCourse.value!.id, {
      name: editCourseData.value.name,
      description: editCourseData.value.description,
      mainCategory: editCourseData.value.mainCategory,
      subCategory: editCourseData.value.subCategory,
    })

    if (response.code === 200) {
      showSnackbar?.('课程信息已更新')

      // 更新列表中的课程信息
      const index = courseList.value.findIndex((c) => c.id === selectedCourse.value!.id)
      if (index > -1) {
        Object.assign(courseList.value[index], editCourseData.value)
      }

      // 如果当前在ID查询且查询的是同一个课程，更新搜索结果
      if (searchedCourse.value && searchedCourse.value.id === selectedCourse.value!.id) {
        Object.assign(searchedCourse.value, editCourseData.value)
      }

      // 如果当前在子课程查询且列表中包含该课程，更新子课程列表
      const subcourseIndex = subcourseList.value.findIndex((c) => c.id === selectedCourse.value!.id)
      if (subcourseIndex > -1) {
        Object.assign(subcourseList.value[subcourseIndex], editCourseData.value)
      }

      closeEditModal()
    } else {
      showSnackbar?.('更新失败', 'error')
    }
  } catch (error) {
    console.error('Error updating course:', error)
    showSnackbar?.('更新失败', 'error')
  } finally {
    editing.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadCourseCategories()
  loadCourses()
})
</script>

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

    <!-- 搜索区域 -->
    <div class="mb-6">
      <v-card flat class="pa-4 bg-grey-lighten-5" rounded="lg">
        <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
          <v-icon icon="mdi-card-search-outline" size="16" class="mr-2"></v-icon>
          通过ID查询课程
        </h4>
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
            :rules="[(v: string) => !v || Number(v) > 0 || '请输入有效的课程ID']"
            @keyup.enter="searchCourseById"
          >
            <template #prepend-inner>
              <v-icon icon="mdi-identifier" size="16" color="grey-darken-1"></v-icon>
            </template>
          </v-text-field>
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            :loading="searchLoading"
            :disabled="!searchCourseId || Number(searchCourseId) <= 0"
            @click="searchCourseById"
          >
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            查询
          </v-btn>
          <v-btn
            v-if="searchedCourse || searchAttempted"
            variant="flat"
            color="grey-lighten-2"
            rounded="lg"
            class="ml-2"
            @click="clearSearch"
          >
            <v-icon icon="mdi-close" size="16" class="mr-1"></v-icon>
            清除
          </v-btn>
        </div>
      </v-card>
    </div>

    <!-- 查询结果显示 -->
    <div v-if="searchedCourse || searchAttempted || searchLoading">
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
            <v-icon
              icon="mdi-folder-account-outline"
              size="16"
              color="blue-darken-1"
              class="mr-2"
            ></v-icon>
            <span class="text-subtitle-2 text-blue-darken-1 font-weight-medium"
              >查询课程（父课程）</span
            >
          </div>
          <div v-else-if="searchedCourse.parentCourse" class="d-flex align-center mb-3">
            <v-icon
              icon="mdi-file-document-outline"
              size="16"
              color="green-darken-1"
              class="mr-2"
            ></v-icon>
            <span class="text-subtitle-2 text-green-darken-1 font-weight-medium"
              >查询课程（子课程）</span
            >
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
            @reject="showRejectDialog"
            @ban="banCourse"
            @unban="unbanCourse"
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
            @reject="showRejectDialog"
            @ban="banCourse"
            @unban="unbanCourse"
          />
        </div>
      </div>

      <!-- 搜索空状态 -->
      <div v-else-if="searchAttempted" class="text-center py-12">
        <v-icon
          icon="mdi-file-search-outline"
          size="48"
          color="grey-lighten-1"
          class="mb-4"
        ></v-icon>
        <p class="text-body-1 text-grey-darken-1">未找到ID为 {{ searchCourseId }} 的课程</p>
        <p class="text-body-2 text-grey-darken-2">请检查课程ID是否正确</p>
      </div>
    </div>

    <!-- 状态管理内容（无查询时显示） -->
    <div v-else>
      <!-- 状态标签 -->
      <v-tabs
        v-model="selectedStateIndex"
        color="primary"
        class="mb-6"
        show-arrows
        @update:model-value="onStateChange"
      >
        <v-tab
          v-for="(state, index) in stateOptions"
          :key="state.value"
          :value="index"
          class="text-none"
        >
          <v-icon
            :icon="state.icon"
            :color="
              state.value === ContentState.SUBMITTED
                ? 'orange-darken-1'
                : state.value === ContentState.PUBLISHED
                  ? 'green-darken-1'
                  : state.value === ContentState.REJECTED
                    ? 'red-darken-1'
                    : 'grey-darken-1'
            "
            size="18"
            class="mr-2"
          ></v-icon>
          {{ state.text }}
        </v-tab>
      </v-tabs>

      <!-- 分类筛选 - 只在已通过状态显示 -->
      <div v-if="getCurrentState() === ContentState.PUBLISHED" class="mb-6">
        <v-card flat class="pa-4 bg-grey-lighten-5" rounded="lg">
          <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
            <v-icon icon="mdi-filter-variant" size="16" class="mr-2"></v-icon>
            分类筛选
          </h4>
          <div class="d-flex align-center w-50">
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
              hide-details
              clearable
              class="me-3"
              @update:model-value="onMainCategoryChange"
            >
              <template #prepend-inner>
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
              hide-details
              :disabled="!selectedMainCategory"
              class="me-3"
              @update:model-value="onCategoryChange"
            >
              <template #prepend-inner>
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
        <div
          v-if="
            getCurrentState() === ContentState.PUBLISHED &&
            selectedMainCategory &&
            !selectedSubCategory
          "
        >
          <v-icon
            icon="mdi-filter-outline"
            size="48"
            color="orange-lighten-1"
            class="mb-4"
          ></v-icon>
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
        <template
          v-if="
            getCurrentState() === ContentState.PUBLISHED &&
            selectedMainCategory &&
            selectedSubCategory
          "
        >
          <CourseCard
            v-for="course in courseList"
            :key="course.id"
            :course="course"
            :main-categories="mainCategories"
            :category-mapping="categoryMapping"
            @edit="showEditModal"
            @approve="approveCourse"
            @reject="showRejectDialog"
            @ban="banCourse"
            @unban="unbanCourse"
          />
        </template>

        <!-- 正常状态使用无限滚动 -->
        <v-infinite-scroll
          v-else
          :empty="!hasMore"
          class="course-list-container"
          @load="onLoadMore"
        >
          <CourseCard
            v-for="course in courseList"
            :key="course.id"
            :course="course"
            :main-categories="mainCategories"
            :category-mapping="categoryMapping"
            @edit="showEditModal"
            @approve="approveCourse"
            @reject="showRejectDialog"
            @ban="banCourse"
            @unban="unbanCourse"
          />

          <template #empty>
            <div class="text-center py-4">
              <p class="text-grey-darken-1 mt-2">- 已加载全部数据 -</p>
            </div>
          </template>

          <template #loading>
            <div class="text-center py-4">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
              <p class="text-grey-darken-1 mt-2">正在加载...</p>
            </div>
          </template>
        </v-infinite-scroll>
      </div>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="selectedCourse?.name || ''"
      :item-state="selectedCourse?.state"
      item-type="课程"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />

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
              :rules="[(v: string) => !!v || '请输入课程名称']"
              class="mb-4"
            ></v-text-field>

            <v-textarea
              v-model="editCourseData.description"
              label="课程描述"
              variant="outlined"
              rows="4"
              rounded="lg"
              bg-color="grey-lighten-5"
              :rules="[(v: string) => !!v || '请输入课程描述']"
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
                  :rules="[(v: number) => !!v || '请选择主分类']"
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
                  :rules="[(v: number) => !!v || '请选择子分类']"
                  :disabled="!editCourseData.mainCategory"
                ></v-select>
              </v-col>
            </v-row>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="flat" color="grey-lighten-2" rounded="lg" @click="closeEditModal">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="blue-lighten-4"
            rounded="lg"
            :loading="editing"
            :disabled="!editFormValid"
            @click="confirmEdit"
          >
            <v-icon icon="mdi-check" color="blue-darken-2" size="16" class="mr-1"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

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
