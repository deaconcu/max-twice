<template>
  <DefaultLayout>
    <div class="career-list-page">
      <!-- 页面标题和搜索栏 -->
      <div class="page-header mb-10">
        <div class="d-flex align-end justify-space-between">
          <!-- 左侧：标题 -->
          <div class="d-flex align-center">
            <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
              <v-icon icon="mdi-briefcase-search-outline" size="32" color="grey-darken-1" />
            </v-avatar>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4">
                {{ t('careerCenter.title') }}
              </h1>
              <p class="text-body-2 text-grey-darken-2 mt-1">{{ t('careerCenter.subtitle') }}</p>
            </div>
          </div>

          <!-- 右侧：搜索栏 -->
          <div class="d-flex align-center search-container">
            <v-text-field
              v-model="searchText"
              :placeholder="t('careerCenter.search.placeholder')"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              class="search-input"
              color="primary"
              @keyup.enter="handleSearch"
            >
              <template #prepend-inner>
                <v-icon icon="mdi-magnify" color="grey-darken-1" size="20" />
              </template>
              <template #append-inner>
                <v-btn
                  icon="mdi-arrow-right"
                  color="grey-darken-1"
                  variant="text"
                  size="small"
                  density="comfortable"
                  @click="handleSearch"
                ></v-btn>
              </template>
            </v-text-field>
          </div>
        </div>
      </div>

      <!-- 分类导航和职业网格 -->
      <v-row>
        <v-col class="pr-12">
          <!-- 分类导航 -->
          <CareerFilter
            v-model:main-category="selectedMainCategory"
            v-model:sub-category="selectedSubCategory"
            :categories="categories"
            :sub-categories="subCategories"
            @change="handleFilterChange"
          />

          <!-- 加载状态 -->
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="64" />
            <p class="text-body-1 text-grey-darken-2 mt-4">{{ t('common.loading') }}</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="filteredCareers.length === 0" class="text-center py-12">
            <v-card rounded="lg" class="pa-12 empty-state no-border">
              <v-icon icon="mdi-briefcase-outline" size="80" color="grey-lighten-1" class="mb-4" />
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('careerCenter.empty.noJobs') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{
                  searchText
                    ? t('careerCenter.empty.noSearchResultsDesc')
                    : t('careerCenter.empty.noRelatedJobsDesc')
                }}
              </p>
              <v-btn
                v-if="selectedMainCategory || searchText"
                color="primary"
                variant="outlined"
                rounded="lg"
                @click="clearAll"
              >
                查看全部职业
              </v-btn>
            </v-card>
          </div>

          <!-- 职业列表 -->
          <div v-else>
            <!-- 分类标题 -->
            <div class="mb-4 mt-10">
              <div class="d-flex align-center justify-space-between">
                <div class="d-flex align-center">
                  <v-icon
                    icon="mdi-format-list-bulleted"
                    size="20"
                    class="mr-2 text-grey-darken-2"
                  ></v-icon>
                  <h2 class="text-h6 font-weight-regular text-grey-darken-4">
                    <span v-if="searchText">搜索结果</span>
                    <span v-else-if="selectedSubCategory">
                      {{ getCategoryName(selectedMainCategory) }} -
                      {{ getSubCategoryName(selectedSubCategory) }}
                    </span>
                    <span v-else-if="selectedMainCategory">{{
                      getCategoryName(selectedMainCategory)
                    }}</span>
                    <span v-else>全部职业</span>
                  </h2>
                </div>
                <p class="text-body-2 text-grey-darken-2">共 {{ filteredCareers.length }} 个职业</p>
              </div>
            </div>

            <!-- 职业网格 -->
            <div class="career-grid mb-16">
              <CareerCard
                v-for="career in displayedCareers"
                :key="career.id"
                :career="career"
                @click="goToCareerDetail"
              />
            </div>

            <!-- 加载更多指示器 -->
            <div v-if="hasMore" ref="loadMoreTrigger" class="text-center mt-6 py-4 mb-16">
              <v-progress-circular v-if="loadingMore" indeterminate color="primary" size="32" />
            </div>
          </div>
        </v-col>

        <!-- 右侧热门职业栏 -->
        <v-col class="right-sidebar">
          <div class="sticky-wrapper">
            <!-- 申请职业按钮 -->
            <v-btn
              color="grey-lighten-4"
              variant="flat"
              block
              rounded="lg"
              size="large"
              class="mb-4 create-course-btn"
              prepend-icon="mdi-plus-circle"
              @click="openApplicationDialog"
            >
              {{ t('careerCenter.search.applyJob') }}
            </v-btn>

            <v-card rounded="lg" class="popular-card no-border" flat>
              <v-card-title class="py-4 px-0 pb-3">
                <div class="d-flex align-center justify-space-between w-100">
                  <div class="d-flex align-center">
                    <v-icon icon="mdi-fire" color="error" class="mr-2" />
                    <span class="text-h6 font-weight-bold">热门职业</span>
                  </div>
                  <v-btn
                    variant="text"
                    size="small"
                    color="primary"
                    class="text-caption"
                    @click="clearAll"
                  >
                    全部
                    <v-icon icon="mdi-chevron-right" size="14" class="ml-1" />
                  </v-btn>
                </div>
              </v-card-title>
              <v-card-text class="px-0 popular-list pb-4">
                <div
                  v-for="(career, index) in popularCareers"
                  :key="career.id"
                  class="popular-item"
                  @click="goToCareerDetail(career)"
                >
                  <div class="rank-badge" :class="index < 3 ? 'rank-top' : ''">
                    {{ index + 1 }}
                  </div>
                  <div class="flex-grow-1">
                    <div class="popular-name">{{ career.name }}</div>
                    <div class="popular-count">
                      <v-icon icon="mdi-account-group" size="12" color="grey" />
                      {{ formatNumber(career.learnerCount) }}
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>

      <!-- 职业申请对话框 -->
      <v-dialog v-model="applicationDialog" max-width="600px" persistent>
        <v-card rounded="xl" border>
          <v-card-title class="pa-6">
            <div class="d-flex align-center">
              <v-icon icon="mdi-plus-circle" color="primary" size="32" class="mr-3" />
              <span class="text-h6 font-weight-bold">{{
                t('careerCenter.application.title')
              }}</span>
            </div>
          </v-card-title>

          <v-card-text class="px-6 pb-0">
            <p class="text-body-2 text-grey-darken-1 mb-4">
              {{ t('careerCenter.application.subtitle') }}
            </p>
            <v-form v-model="applicationValid">
              <v-text-field
                v-model="applicationForm.name"
                :label="t('careerCenter.application.jobName')"
                :placeholder="t('careerCenter.application.jobNamePlaceholder')"
                :rules="jobNameRules"
                variant="outlined"
                clearable
                required
                class="mb-4"
              />

              <v-select
                v-model="applicationForm.mainCategory"
                :items="categories"
                item-title="title"
                item-value="id"
                :label="t('careerCenter.application.category')"
                :rules="categoryRules"
                variant="outlined"
                class="mb-4"
                clearable
                required
              />

              <v-select
                v-model="applicationForm.subCategory"
                :items="getSubCategoriesForMain(applicationForm.mainCategory)"
                item-title="name"
                item-value="id"
                :label="t('careerCenter.category.specificDirection')"
                :rules="categoryRules"
                variant="outlined"
                class="mb-4"
                :disabled="!applicationForm.mainCategory"
                clearable
                required
              />

              <v-textarea
                v-model="applicationForm.description"
                :label="t('careerCenter.application.description')"
                :placeholder="t('careerCenter.application.descriptionPlaceholder')"
                :rules="descriptionRules"
                variant="outlined"
                clearable
                required
                rows="3"
                class="mb-4"
              />

              <v-text-field
                v-model="applicationForm.skills"
                :label="t('careerCenter.application.skills')"
                :placeholder="t('careerCenter.application.skillsPlaceholder')"
                :hint="t('careerCenter.application.skillsHint')"
                variant="outlined"
                persistent-hint
                class="mb-4"
              />
            </v-form>
          </v-card-text>

          <v-card-actions class="px-6 pb-6">
            <v-spacer />
            <v-btn
              variant="outlined"
              rounded="lg"
              :disabled="submitting"
              @click="closeApplicationDialog"
            >
              {{ t('careerCenter.application.cancel') }}
            </v-btn>
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              :disabled="!applicationValid || submitting"
              :loading="submitting"
              @click="submitApplication"
            >
              {{ t('careerCenter.application.submit') }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </div>
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { handleApiCall } from '@/composables/utils'
import { professionApi, systemApi } from '@/api'
import type { Profession } from '@/types/profession'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import CareerCard from '@/components/features/career/CareerCard.vue'
import CareerFilter from '@/components/features/career/CareerFilter.vue'

const router = useRouter()
const { t } = useI18n()

// 状态管理
const searchText = ref('')
const selectedMainCategory = ref<number | undefined>()
const selectedSubCategory = ref<number | undefined>()
const applicationDialog = ref(false)
const applicationValid = ref(false)
const loadMoreTrigger = ref<HTMLElement | null>(null)

// 职业申请表单
const applicationForm = ref({
  name: '',
  description: '',
  mainCategory: undefined as number | undefined,
  subCategory: undefined as number | undefined,
  skills: '',
})

// 当前分类查询参数
const currentCategory = ref<{ mainCategory: number; subCategory: number } | null>(null)

// 职业列表状态
const careers = ref<Profession[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const lastId = ref<number | undefined>(undefined)

// 使用 useFetch 加载职业分类
const { data: categoriesData, loading: _loadingCategories } = useFetch({
  fetchFn: () => systemApi.getProfessionCategories(),
  immediate: true,
  defaultValue: { mainCategories: [], categoryMapping: [] },
})

// 使用 useFetch 加载热门职业
const { data: hotCareersData, loading: _loadingHotCareers } = useFetch<Profession[]>({
  fetchFn: () => professionApi.getHotProfessions(15),
  immediate: true,
  defaultValue: [],
})

// 计算属性 - 从 useFetch 数据中提取
const categories = computed(() => {
  const data = categoriesData.value
  if (!data?.mainCategories) return []
  return data.mainCategories as { id: number; title: string; icon?: string }[]
})

const subCategories = computed(() => {
  const data = categoriesData.value
  if (!data?.categoryMapping) return []

  const allSubCategories: { id: number; name: string; mainCategoryId: number }[] = []
  const categoryMapping = data.categoryMapping as {
    mainCategoryId: number
    subcategories?: { id: number; name: string }[] // 注意：是 subcategories 不是 subCategories
  }[]

  categoryMapping.forEach((mapping) => {
    if (mapping.subcategories) {
      mapping.subcategories.forEach((sub) => {
        // 注意：是 subcategories 不是 subCategories
        allSubCategories.push({
          id: sub.id,
          name: sub.name,
          mainCategoryId: mapping.mainCategoryId,
        })
      })
    }
  })

  return allSubCategories
})

const hotCareers = computed(() => hotCareersData.value ?? [])

// 筛选后的职业（只在前端筛选搜索关键词）
const filteredCareers = computed(() => {
  let result = careers.value

  // 按搜索关键词筛选
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(
      (career) =>
        career.name.toLowerCase().includes(keyword) ||
        (career.description?.toLowerCase().includes(keyword) ?? false) ||
        (career.skills?.toLowerCase().includes(keyword) ?? false)
    )
  }

  return result
})

// 显示的职业列表
const displayedCareers = computed(() => {
  return filteredCareers.value
})

// 热门职业 - 前15个
const popularCareers = computed(() => {
  return hotCareers.value.slice(0, 15)
})

/**
 * 加载职业列表（初始加载或重新加载）
 */
const loadCareers = async (reset = false) => {
  if (reset) {
    careers.value = []
    lastId.value = undefined
    hasMore.value = true
  }

  try {
    loading.value = true

    const fetchFn = currentCategory.value
      ? () =>
          professionApi.getProfessionsByCategory(
            lastId.value,
            currentCategory.value!.mainCategory,
            currentCategory.value!.subCategory
          )
      : () => professionApi.getApprovedProfessions(lastId.value)

    const result = await handleApiCall(fetchFn, {
      showToast: false,
      onError: (error) => {
        console.error('加载职业失败:', error.message)
      },
    })

    if (result && result.length > 0) {
      careers.value = [...careers.value, ...result]
      lastId.value = result[result.length - 1].id
      hasMore.value = result.length >= 12 // 假设每页12条
    } else {
      hasMore.value = false
    }
  } finally {
    loading.value = false
  }
}

/**
 * 加载更多职业
 */
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return

  try {
    loadingMore.value = true

    const fetchFn = currentCategory.value
      ? () =>
          professionApi.getProfessionsByCategory(
            lastId.value,
            currentCategory.value!.mainCategory,
            currentCategory.value!.subCategory
          )
      : () => professionApi.getApprovedProfessions(lastId.value)

    const result = await handleApiCall(fetchFn, {
      showToast: false,
      onError: (error) => {
        console.error('加载更多职业失败:', error.message)
      },
    })

    if (result && result.length > 0) {
      careers.value = [...careers.value, ...result]
      lastId.value = result[result.length - 1].id
      hasMore.value = result.length >= 12
    } else {
      hasMore.value = false
    }
  } finally {
    loadingMore.value = false
  }
}

/**
 * 格式化数字（千位分隔）
 */
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

/**
 * 获取分类名称
 */
const getCategoryName = (categoryId?: number) => {
  if (!categoryId) return ''
  return categories.value.find((c) => c.id === categoryId)?.title ?? ''
}

/**
 * 获取子分类名称
 */
const getSubCategoryName = (subCategoryId?: number) => {
  if (!subCategoryId) return ''
  return subCategories.value.find((s) => s.id === subCategoryId)?.name ?? ''
}

/**
 * 获取指定主分类的子分类列表
 */
const getSubCategoriesForMain = (mainCategoryId?: number) => {
  if (!mainCategoryId) return []
  return subCategories.value.filter((s) => s.mainCategoryId === mainCategoryId)
}

/**
 * 清空所有筛选
 */
const clearAll = () => {
  selectedMainCategory.value = undefined
  selectedSubCategory.value = undefined
  searchText.value = ''
  currentCategory.value = null
  void loadCareers(true)
}

/**
 * 根据分类加载职业
 */
const loadCareersByCategory = async (mainCategory: number, subCategory: number): Promise<void> => {
  currentCategory.value = { mainCategory, subCategory }
  await loadCareers(true)
}

/**
 * 处理搜索
 */
const handleSearch = async () => {
  if (!searchText.value) {
    currentCategory.value = null
    await loadCareers(true)
    return
  }

  try {
    loading.value = true
    const result = await handleApiCall(() => professionApi.searchProfessions(searchText.value), {
      showToast: false,
      onError: (error) => {
        console.error('搜索职业失败:', error.message)
      },
    })

    if (result) {
      careers.value = result
      hasMore.value = false // 搜索结果不分页
    }
  } finally {
    loading.value = false
  }
}

/**
 * 处理筛选变化
 */
const handleFilterChange = () => {
  searchText.value = '' // 清空搜索
  // watch 会自动处理加载，不需要在这里重复调用
}

/**
 * 监听分类变化，自动加载职业
 */
watch([selectedMainCategory, selectedSubCategory], () => {
  if (selectedMainCategory.value && selectedSubCategory.value) {
    void loadCareersByCategory(selectedMainCategory.value, selectedSubCategory.value)
  } else {
    currentCategory.value = null
    void loadCareers(true)
  }
  // 重新设置无限滚动
  cleanupInfiniteScroll()
  setTimeout(setupInfiniteScroll, 100)
})

/**
 * Intersection Observer 实例
 */
let observer: IntersectionObserver | null = null

/**
 * 设置无限滚动
 */
const setupInfiniteScroll = () => {
  if (!loadMoreTrigger.value) return

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry?.isIntersecting && hasMore.value && !loadingMore.value) {
        void loadMore()
      }
    },
    {
      root: null,
      rootMargin: '100px',
      threshold: 0.1,
    }
  )

  observer.observe(loadMoreTrigger.value)
}

/**
 * 清理 Intersection Observer
 */
const cleanupInfiniteScroll = () => {
  if (observer && loadMoreTrigger.value) {
    observer.unobserve(loadMoreTrigger.value)
    observer.disconnect()
    observer = null
  }
}

/**
 * 组件挂载时设置无限滚动和加载初始数据
 */
onMounted(() => {
  void loadCareers(true)
  setTimeout(setupInfiniteScroll, 100)
})

/**
 * 组件卸载时清理
 */
onBeforeUnmount(() => {
  cleanupInfiniteScroll()
})

/**
 * 跳转到职业详情（路线图列表）
 */
const goToCareerDetail = (career: Profession) => {
  // TODO: 创建职业详情页面路由
  void router.push(`/career/${String(career.id)}`)
}

/**
 * 打开申请对话框
 */
const openApplicationDialog = () => {
  applicationDialog.value = true
}

/**
 * 关闭申请对话框
 */
const closeApplicationDialog = () => {
  applicationDialog.value = false
  applicationForm.value = {
    name: '',
    description: '',
    mainCategory: undefined,
    subCategory: undefined,
    skills: '',
  }
}

/**
 * 表单验证规则
 */
const jobNameRules = [
  (v: string) => !!v || t('careerCenter.application.nameRequired'),
  (v: string) => (v && v.length <= 50) || '职业名称不能超过50个字符',
]

const categoryRules = [(v: number) => !!v || '请选择分类']

const descriptionRules = [
  (v: string) => !!v || t('careerCenter.application.descriptionRequired'),
  (v: string) => (v && v.length >= 10) || '职业描述至少需要10个字符',
  (v: string) => (v && v.length <= 500) || '职业描述不能超过500个字符',
]

/**
 * 使用 useMutation 提交职业申请
 */
const { execute: executeSubmit, loading: submitting } = useMutation(
  (data: {
    name: string
    description: string
    mainCategory: number
    subCategory: number
    skills: string
  }) => professionApi.createProfession(data),
  {
    successMessage: t('careerCenter.application.submittedSuccess'),
    onSuccess: () => {
      closeApplicationDialog()
      // 刷新职业列表
      void loadCareers(true)
    },
  }
)

/**
 * 提交申请
 */
const submitApplication = async () => {
  if (!applicationForm.value.mainCategory || !applicationForm.value.subCategory) {
    return
  }

  await executeSubmit({
    name: applicationForm.value.name,
    description: applicationForm.value.description,
    mainCategory: applicationForm.value.mainCategory,
    subCategory: applicationForm.value.subCategory,
    skills: applicationForm.value.skills || '',
  })
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

/* 搜索容器样式 */
.search-container {
  gap: 0;
}

.search-input {
  border-radius: 12px;
  width: 600px;
}

.empty-state {
  background-color: rgb(var(--v-theme-surface));
}

.career-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 右侧热门职业栏 */
.right-sidebar {
  max-width: 280px;
}

.sticky-wrapper {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  display: flex;
  flex-direction: column;
}

.create-course-btn {
  font-weight: 600;
  text-transform: none;
  letter-spacing: 0.3px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.12);
  transition: all 0.25s ease;
}

.create-course-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  background-color: rgb(var(--v-theme-grey-lighten-3));
}

.popular-card {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: rgb(var(--v-theme-surface));
}

.popular-list {
  overflow-y: auto;
  flex: 1;
}

.popular-list::-webkit-scrollbar {
  width: 4px;
}

.popular-list::-webkit-scrollbar-track {
  background: transparent;
}

.popular-list::-webkit-scrollbar-thumb {
  background-color: rgba(var(--v-theme-on-surface), 0.1);
  border-radius: 2px;
}

.popular-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
}

.popular-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.popular-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface-variant));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-right: 12px;
  flex-shrink: 0;
}

.rank-badge.rank-top {
  background: linear-gradient(135deg, #ffd700 0%, #ffa500 100%);
  color: rgb(var(--v-theme-surface));
}

.popular-name {
  font-size: 14px;
  font-weight: 500;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-count {
  font-size: 12px;
  color: rgb(var(--v-theme-on-surface-variant));
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 移动端响应式 */
@media (max-width: 1264px) {
  .right-sidebar {
    max-width: 100%;
  }
}

@media (max-width: 960px) {
  .career-grid {
    grid-template-columns: 1fr;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }

  .popular-list {
    max-height: 400px;
  }
}
</style>
