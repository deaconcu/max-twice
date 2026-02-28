<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
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

// 加载课程列表
const loadCourses = async (isLoadMore = false): Promise<void> => {
  try {
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
      courseList.value = []
      lastId.value = 0
      hasMore.value = true
    }

    const currentState = getCurrentState()
    let response

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

    if (
      currentState === ContentState.PUBLISHED &&
      selectedMainCategory.value &&
      selectedSubCategory.value
    ) {
      response = await adminApi.getCoursesByFilter(currentState, null)
      hasMore.value = false
    } else {
      const currentLastId = isLoadMore ? lastId.value : null
      response = await adminApi.getContentsByState('course', currentState, currentLastId)
    }

    if (response.code === 200) {
      const newCourses = response.data || []

      if (isLoadMore) {
        courseList.value.push(...newCourses)
      } else {
        courseList.value = newCourses
      }

      if (
        currentState !== ContentState.PUBLISHED ||
        !selectedMainCategory.value ||
        !selectedSubCategory.value
      ) {
        if (newCourses.length > 0) {
          lastId.value = newCourses[newCourses.length - 1].id
          hasMore.value = newCourses.length >= 10
        } else {
          hasMore.value = false
        }
      }
    } else {
      showSnackbar?.('加载课程失败: ' + (response.msg || '未知错误'))
    }
  } catch (error) {
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

      try {
        const subcourseResponse = await adminApi.getAdminSubcourses(
          Number(searchCourseId.value),
          undefined
        )
        if (subcourseResponse.code === 200) {
          subcourseList.value = subcourseResponse.data || []
        }
      } catch (error) {
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

      if (categories.length > 0) {
        const firstMapping = mapping.find((m: any) => m.mainCategoryId === categories[0].id)
        subCategories.value = firstMapping ? firstMapping.subcategories : []
      }
    }
  } catch (error) {
    console.error('Error loading course categories:', error)
  }
}

// 状态变化处理
const onStateChange = (): void => {
  selectedMainCategory.value = null
  selectedSubCategory.value = null
  loadCourses()
}

// 分类变化处理
const onCategoryChange = (): void => {
  if (getCurrentState() === ContentState.PUBLISHED) {
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
    categoryMapping.value.length > 0 ? categoryMapping.value[0].subcategories : []
  loadCourses()
}

// 主分类变化时更新子分类
const updateSubCategories = (mainCategoryId: number): void => {
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === mainCategoryId)
  subCategories.value = mapping ? mapping.subcategories : []

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
    if (getCurrentState() === ContentState.PUBLISHED && selectedSubCategory.value) {
      loadCourses()
    } else if (getCurrentState() === ContentState.PUBLISHED) {
      courseList.value = []
    }
  } else {
    subCategories.value = []
    selectedSubCategory.value = null
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
      if (getCurrentState() !== ContentState.PUBLISHED) {
        const index = courseList.value.findIndex((c) => c.id === courseId)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        const course = courseList.value.find((c) => c.id === courseId)
        if (course) course.state = ContentState.PUBLISHED
      }

      if (searchedCourse.value?.id === courseId) {
        searchedCourse.value.state = ContentState.PUBLISHED
      }

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

      const currentState = getCurrentState()
      const index = courseList.value.findIndex((c) => c.id === data.courseId)
      if (index > -1) {
        if (currentState !== targetState) {
          courseList.value.splice(index, 1)
        } else {
          courseList.value[index].state = targetState
        }
      }

      if (searchedCourse.value && searchedCourse.value.id === data.courseId) {
        searchedCourse.value.state = targetState
      }

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
      if (getCurrentState() === ContentState.BANNED) {
        const index = courseList.value.findIndex((c) => c.id === course.id)
        if (index > -1) {
          courseList.value.splice(index, 1)
        }
      } else {
        course.state = ContentState.PUBLISHED
        delete (course as any).reason
      }
    } else {
      showSnackbar?.('操作失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
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

  onEditMainCategoryChange()
  editDialog.value = true
}

// 编辑表单主分类变化
const onEditMainCategoryChange = (): void => {
  const mapping = categoryMapping.value.find(
    (m) => m.mainCategoryId === editCourseData.value.mainCategory
  )
  editSubCategories.value = mapping ? mapping.subcategories : []

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

      const index = courseList.value.findIndex((c) => c.id === selectedCourse.value!.id)
      if (index > -1) {
        Object.assign(courseList.value[index], editCourseData.value)
      }

      if (searchedCourse.value && searchedCourse.value.id === selectedCourse.value!.id) {
        Object.assign(searchedCourse.value, editCourseData.value)
      }

      const subcourseIndex = subcourseList.value.findIndex((c) => c.id === selectedCourse.value!.id)
      if (subcourseIndex > -1) {
        Object.assign(subcourseList.value[subcourseIndex], editCourseData.value)
      }

      closeEditModal()
    } else {
      showSnackbar?.('更新失败', 'error')
    }
  } catch (error) {
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
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">课程管理</h2>

    <!-- 搜索区域 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-magnify" size="18" class="mr-2"></v-icon>
        通过ID查询课程
      </v-card-title>
      <v-card-text>
        <div class="d-flex align-center ga-3">
          <v-text-field
            v-model="searchCourseId"
            label="输入课程ID"
            variant="outlined"
            density="compact"
            hide-details
            type="number"
            style="max-width: 200px"
            @keyup.enter="searchCourseById"
          ></v-text-field>
          <v-btn
            variant="tonal"
            size="default"
            :loading="searchLoading"
            :disabled="!searchCourseId || Number(searchCourseId) <= 0"
            @click="searchCourseById"
          >
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            查询
          </v-btn>
          <v-btn
            v-if="searchedCourse || searchAttempted"
            variant="text"
            size="default"
            @click="clearSearch"
          >
            清除
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 查询结果显示 -->
    <div v-if="searchedCourse || searchAttempted || searchLoading">
      <!-- 搜索加载状态 -->
      <div v-if="searchLoading" class="text-center py-8">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
        <p class="mt-3 text-grey-darken-1">查询中...</p>
      </div>

      <!-- 搜索结果 -->
      <v-card v-else-if="searchedCourse" flat class="border mb-4">
        <v-card-title class="d-flex align-center">
          <v-icon icon="mdi-book" size="18" class="mr-2"></v-icon>
          查询结果
        </v-card-title>
        <v-card-text>
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

          <!-- 子课程列表 -->
          <div v-if="subcourseList.length > 0" class="mt-4">
            <div class="text-body-2 font-weight-medium text-grey-darken-2 mb-3">
              子课程列表 ({{ subcourseList.length }}个)
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
        </v-card-text>
      </v-card>

      <!-- 搜索空状态 -->
      <div v-else-if="searchAttempted" class="text-center py-12">
        <v-icon icon="mdi-file-search-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
        <p class="text-body-1 text-grey-darken-1">未找到ID为 {{ searchCourseId }} 的课程</p>
      </div>
    </div>

    <!-- 状态管理内容（无查询时显示） -->
    <div v-else>
      <!-- 筛选与状态 -->
      <v-card flat class="border mb-4">
        <v-card-title class="d-flex align-center">
          <v-icon icon="mdi-filter-variant" size="18" class="mr-2"></v-icon>
          筛选与状态
        </v-card-title>
        <v-card-text>
          <!-- 分类筛选 - 只在已通过状态显示 -->
          <div v-if="getCurrentState() === ContentState.PUBLISHED" class="d-flex align-center ga-3 mb-4">
            <v-select
              v-model="selectedMainCategory"
              :items="mainCategories"
              item-title="name"
              item-value="id"
              label="主分类"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              style="max-width: 180px"
              @update:model-value="onMainCategoryChange"
            ></v-select>
            <v-select
              v-model="selectedSubCategory"
              :items="subCategories"
              item-title="name"
              item-value="id"
              label="子分类"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              :disabled="!selectedMainCategory"
              style="max-width: 180px"
              @update:model-value="onCategoryChange"
            ></v-select>
            <v-btn
              v-if="selectedMainCategory || selectedSubCategory"
              variant="text"
              size="default"
              @click="clearCategoryFilter"
            >
              清除筛选
            </v-btn>
          </div>

          <!-- 状态标签 -->
          <v-tabs
            v-model="selectedStateIndex"
            color="primary"
            show-arrows
            @update:model-value="onStateChange"
          >
            <v-tab
              v-for="(state, index) in stateOptions"
              :key="state.value"
              :value="index"
              class="text-none"
            >
              <v-icon :icon="state.icon" size="16" class="mr-2"></v-icon>
              {{ state.text }}
            </v-tab>
          </v-tabs>
        </v-card-text>
      </v-card>

      <!-- 课程列表 -->
      <v-card flat class="border">
        <v-card-title class="d-flex align-center">
          <v-icon icon="mdi-book-multiple" size="18" class="mr-2"></v-icon>
          课程列表
        </v-card-title>
        <v-card-text>
          <!-- 空状态 -->
          <div v-if="!loading && courseList.length === 0" class="text-center py-12">
            <v-icon icon="mdi-book-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
            <p class="text-body-1 text-grey-darken-1">
              <template v-if="getCurrentState() === ContentState.PUBLISHED && selectedMainCategory && !selectedSubCategory">
                请选择子分类
              </template>
              <template v-else>
                暂无{{ getCurrentStateText() }}的课程
              </template>
            </p>
          </div>

          <!-- 列表 -->
          <div v-else>
            <!-- 使用分类筛选时不启用无限滚动 -->
            <template v-if="getCurrentState() === ContentState.PUBLISHED && selectedMainCategory && selectedSubCategory">
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
                <div class="text-center py-4 text-caption text-grey">
                  没有更多了
                </div>
              </template>

              <template #loading>
                <div class="text-center py-4">
                  <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
                  <span class="ml-2 text-grey-darken-1">加载中...</span>
                </div>
              </template>
            </v-infinite-scroll>
          </div>

          <!-- 加载指示器 -->
          <div v-if="loading && courseList.length === 0" class="text-center py-4">
            <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
            <span class="ml-2 text-grey-darken-1">加载中...</span>
          </div>
        </v-card-text>
      </v-card>
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
      <v-card rounded="lg" variant="flat">
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
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeEditModal">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="primary"
            rounded="lg"
            :loading="editing"
            :disabled="!editFormValid"
            @click="confirmEdit"
          >
            <v-icon icon="mdi-check" size="16" class="mr-1"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
