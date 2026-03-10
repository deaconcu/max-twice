<template>
  <DefaultLayout>
    <div class="role-list-page">
      <!-- 页面标题和搜索栏 -->
      <div class="page-header mb-6 mb-md-10">
        <div class="d-flex flex-column flex-sm-row align-start align-sm-end ga-4 header-wrapper">
          <!-- 左侧：标题 -->
          <div class="d-flex align-center title-container">
            <v-avatar
              :color="'rgb(var(--v-theme-surface-variant))'"
              :size="$vuetify.display.mobile ? 48 : 64"
              rounded="lg"
              class="mr-3 flex-shrink-0"
            >
              <v-icon
                icon="mdi-briefcase-search-outline"
                :size="$vuetify.display.mobile ? 24 : 32"
                color="grey-darken-1"
              />
            </v-avatar>
            <div style="min-width: 0; overflow: hidden">
              <h1 class="text-h5 text-md-h4 font-weight-bold text-grey-darken-4 text-truncate">
                {{ t('roleCenter.title') }}
              </h1>
              <p class="text-caption text-md-body-2 text-grey-darken-2 mt-1 text-truncate">
                {{ t('roleCenter.subtitle') }}
              </p>
            </div>
          </div>

          <!-- 右侧：操作按钮 -->
          <div class="d-flex align-center ga-3 actions-wrapper pb-1">
            <!-- 添加职业按钮（仅在右侧栏隐藏时显示） -->
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              class="d-lg-none flex-shrink-0"
              @click="openApplicationDialog"
            >
              <v-icon icon="mdi-plus" size="20" class="mr-1" />
              申请
            </v-btn>
          </div>
        </div>
      </div>

      <!-- 分类导航和职业网格 -->
      <div class="content-layout">
        <div class="main-content">
          <!-- 页面初始加载状态 -->
          <LoadingSpinner v-if="categoryStore.loading && categories.length === 0" />

          <template v-else>
            <!-- 分类导航 -->
            <RoleFilter
              v-model:main-category="selectedMainCategory"
              v-model:sub-category="selectedSubCategory"
              :categories="categories"
              :sub-categories="subCategories"
              @change="handleFilterChange"
            />

            <!-- 加载状态 -->
            <LoadingSpinner v-if="loading" />

            <!-- 空状态 -->
            <div v-else-if="filteredRoles.length === 0" class="text-center py-12">
            <v-card rounded="lg" class="pa-12 empty-state no-border">
              <v-icon icon="mdi-briefcase-outline" size="80" color="grey-lighten-1" class="mb-4" />
              <h3 class="text-h5 font-weight-bold text-grey-darken-2 mb-2">
                {{ t('roleCenter.empty.noJobs') }}
              </h3>
              <p class="text-body-1 text-grey-darken-1 mb-4">
                {{
                  searchText
                    ? t('roleCenter.empty.noSearchResultsDesc')
                    : t('roleCenter.empty.noRelatedJobsDesc')
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
          <div v-else class="mt-6 mt-md-8">

            <!-- 职业网格 -->
            <div class="role-grid">
              <RoleCard
                v-for="role in displayedRoles"
                :key="role.id"
                :role="role"
                @click="goToRoleDetail"
              />
            </div>

            <!-- 加载更多指示器 -->
            <div v-if="hasMore" ref="loadMoreTrigger" class="text-center mt-6 py-4 mb-16">
              <v-progress-circular v-if="loadingMore" indeterminate color="primary" size="32" />
            </div>
          </div>
          </template>
        </div>

        <!-- 右侧热门职业栏 -->
        <div class="right-sidebar d-none d-lg-block">
          <div class="sticky-wrapper">
            <!-- 申请职业按钮 -->
            <v-card
              rounded="xl"
              class="mb-4 create-role-card"
              elevation="0"
              @click="openApplicationDialog"
            >
              <v-card-text class="pa-6">
                <div class="d-flex align-center">
                  <v-avatar color="primary" size="40" class="mr-3">
                    <v-icon icon="mdi-plus" size="22" color="white" />
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="text-subtitle-1 font-weight-bold text-grey-darken-4">
                      {{ t('roleCenter.search.applyJob') }}
                    </div>
                    <div class="text-caption text-grey">申请新的职业方向</div>
                  </div>
                  <v-icon icon="mdi-chevron-right" size="20" color="grey-lighten-1" />
                </div>
              </v-card-text>
            </v-card>

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
                  v-for="(role, index) in popularRoles"
                  :key="role.id"
                  class="popular-item"
                  @click="goToRoleDetail(role)"
                >
                  <div class="rank-badge" :class="index < 3 ? 'rank-top' : ''">
                    {{ index + 1 }}
                  </div>
                  <div class="flex-grow-1">
                    <div class="popular-name">{{ role.name }}</div>
                    <div class="popular-count">
                      <v-icon icon="mdi-account-group" size="12" color="grey" />
                      {{ formatNumber(role.learnerCount) }}
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </div>
        </div>
      </div>

      <!-- 职业申请对话框 -->
      <v-dialog v-model="applicationDialog" max-width="600px" persistent>
        <v-card rounded="xl" border>
          <v-card-title class="pa-6">
            <div class="d-flex align-center">
              <v-icon icon="mdi-plus-circle" color="primary" size="32" class="mr-3" />
              <span class="text-h6 font-weight-bold">{{
                t('roleCenter.application.title')
              }}</span>
            </div>
          </v-card-title>

          <v-card-text class="px-6 pb-0">
            <p class="text-body-2 text-grey-darken-1 mb-4">
              {{ t('roleCenter.application.subtitle') }}
            </p>
            <v-form v-model="applicationValid">
              <v-text-field
                v-model="applicationForm.name"
                :label="t('roleCenter.application.jobName')"
                :placeholder="t('roleCenter.application.jobNamePlaceholder')"
                :rules="jobNameRules"
                :counter="professionNameMaxLength"
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
                :label="t('roleCenter.application.category')"
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
                :label="t('roleCenter.category.specificDirection')"
                :rules="categoryRules"
                variant="outlined"
                class="mb-4"
                :disabled="!applicationForm.mainCategory"
                clearable
                required
              />

              <v-textarea
                v-model="applicationForm.description"
                :label="t('roleCenter.application.description')"
                :placeholder="t('roleCenter.application.descriptionPlaceholder')"
                :rules="descriptionRules"
                :counter="professionDescriptionMaxLength"
                variant="outlined"
                clearable
                required
                rows="3"
                class="mb-4"
              />

              <v-text-field
                v-model="applicationForm.skills"
                :label="t('roleCenter.application.skills')"
                :placeholder="t('roleCenter.application.skillsPlaceholder')"
                :hint="t('roleCenter.application.skillsHint')"
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
              {{ t('roleCenter.application.cancel') }}
            </v-btn>
            <v-btn
              color="primary"
              variant="flat"
              rounded="lg"
              :disabled="!applicationValid || submitting"
              :loading="submitting"
              @click="submitApplication"
            >
              {{ t('roleCenter.application.submit') }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </div>
  </DefaultLayout>
</template>

<script lang="ts">
export default {
  name: 'RoleListPage',
}
</script>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useFetch, useMutation } from '@/composables'
import { handleApiCall } from '@/composables/utils'
import { professionApi } from '@/api'
import type { Profession } from '@/types/profession'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useCategoryStore } from '@/stores'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import RoleCard from '@/components/features/role/RoleCard.vue'
import RoleFilter from '@/components/features/role/RoleFilter.vue'

const router = useRouter()
const { t } = useI18n()
const categoryStore = useCategoryStore()

// 验证规则
const professionNameRules = useValidationRules('profession-name')
const professionDescriptionRules = useValidationRules('profession-description')
const professionNameMaxLength = useMaxLength('profession-name')
const professionDescriptionMaxLength = useMaxLength('profession-description')

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
const roles = ref<Profession[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const lastId = ref<number | undefined>(undefined)

// 页面加载时获取分类数据
onMounted(async () => {
  await categoryStore.checkAndLoad()
})

// 使用 useFetch 加载热门职业
const { data: hotRolesData, loading: _loadingHotRoles } = useFetch<Profession[]>({
  fetchFn: () => professionApi.getHotProfessions(15),
  immediate: true,
  defaultValue: [],
})

// 计算属性 - 从 Store 中获取分类数据
const categories = computed(() => {
  return categoryStore.getProfessionMainCategories()
})

const subCategories = computed(() => {
  const allSubCategories: { id: number; name: string; mainCategoryId: number }[] = []

  categories.value.forEach((mainCategory) => {
    const subs = categoryStore.getProfessionSubCategories(mainCategory.id)
    subs.forEach((sub) => {
      allSubCategories.push({
        id: sub.id,
        name: sub.name,
        mainCategoryId: mainCategory.id,
      })
    })
  })

  return allSubCategories
})

const hotRoles = computed(() => hotRolesData.value ?? [])

// 筛选后的职业（只在前端筛选搜索关键词）
const filteredRoles = computed(() => {
  let result = roles.value

  // 按搜索关键词筛选
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(
      (role) =>
        role.name.toLowerCase().includes(keyword) ||
        (role.description?.toLowerCase().includes(keyword) ?? false) ||
        (role.skills?.toLowerCase().includes(keyword) ?? false)
    )
  }

  return result
})

// 显示的职业列表
const displayedRoles = computed(() => {
  return filteredRoles.value
})

// 热门职业 - 前15个
const popularRoles = computed(() => {
  return hotRoles.value.slice(0, 15)
})

/**
 * 加载职业列表（初始加载或重新加载）
 */
const loadRoles = async (reset = false) => {
  if (reset) {
    roles.value = []
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
      : () => professionApi.getProfessionsByCategory(lastId.value)

    const result = await handleApiCall(fetchFn, {
      showToast: false,
      onError: (error) => {
        console.error('加载职业失败:', error.message)
      },
    })

    if (result && result.items && result.items.length > 0) {
      roles.value = [...roles.value, ...result.items]
      lastId.value = result.items[result.items.length - 1].id
      hasMore.value = result.hasMore
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
      : () => professionApi.getProfessionsByCategory(lastId.value)

    const result = await handleApiCall(fetchFn, {
      showToast: false,
      onError: (error) => {
        console.error('加载更多职业失败:', error.message)
      },
    })

    if (result && result.items && result.items.length > 0) {
      roles.value = [...roles.value, ...result.items]
      lastId.value = result.items[result.items.length - 1].id
      hasMore.value = result.hasMore
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
  void loadRoles(true)
}

/**
 * 根据分类加载职业
 */
const loadRolesByCategory = async (mainCategory: number, subCategory: number): Promise<void> => {
  currentCategory.value = { mainCategory, subCategory }
  await loadRoles(true)
}

/**
 * 处理搜索
 */
const handleSearch = async () => {
  if (!searchText.value) {
    currentCategory.value = null
    await loadRoles(true)
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
      roles.value = result
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
watch([selectedMainCategory, selectedSubCategory], async () => {
  // 先清理旧的 observer
  cleanupInfiniteScroll()

  if (selectedMainCategory.value && selectedSubCategory.value) {
    await loadRolesByCategory(selectedMainCategory.value, selectedSubCategory.value)
  } else {
    currentCategory.value = null
    await loadRoles(true)
  }

  // 数据加载完成后，重新设置无限滚动
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
onMounted(async () => {
  // 加载分类数据
  await categoryStore.checkAndLoad()
  // 加载职业数据
  await loadRoles(true)
  // 数据加载完成后设置无限滚动
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
const goToRoleDetail = (role: Profession) => {
  void router.push(`/role/${String(role.id)}`)
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
const jobNameRules = professionNameRules

const categoryRules = [(v: number) => !!v || '请选择分类']

const descriptionRules = professionDescriptionRules

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
    successMessage: t('roleCenter.application.submittedSuccess'),
    onSuccess: () => {
      closeApplicationDialog()
      // 刷新职业列表
      void loadRoles(true)
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
.role-list-page {
  padding-top: 24px;
}

@media (max-width: 960px) {
  .role-list-page {
    padding-top: 16px;
  }
}

.page-header {
  margin-bottom: 16px;
}

@media (min-width: 960px) {
  .page-header {
    margin-bottom: 24px;
  }
}

/* 头部包装器 */
.header-wrapper {
  width: 100%;
}

/* 标题容器 */
.title-container {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* 操作按钮包装器（搜索框+按钮） */
.actions-wrapper {
  width: 100%;
  flex-shrink: 0;
}

@media (min-width: 600px) {
  .actions-wrapper {
    width: auto;
    flex-shrink: 0;
  }
}

/* 搜索输入框 */
.search-input {
  border-radius: 12px;
  width: 100%;
}

@media (min-width: 600px) {
  .search-input {
    width: clamp(280px, 40vw, 600px);
  }
}

.empty-state {
  background-color: rgb(var(--v-theme-surface));
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 内容布局 */
.content-layout {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 48px;
  }
}

.main-content {
  flex: 1;
  min-width: 0;
}

/* 右侧热门职业栏 */
.right-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.sticky-wrapper {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  display: flex;
  flex-direction: column;
}

.create-role-card {
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #e9ecef;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.create-role-card:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
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
@media (max-width: 960px) {
  .role-grid {
    grid-template-columns: 1fr;
  }

  .popular-list {
    max-height: 400px;
  }
}
</style>
