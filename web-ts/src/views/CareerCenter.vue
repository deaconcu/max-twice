<script setup lang="ts">
import { inject, onMounted, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { professionServiceV1, systemServiceV1 } from '@/services/api/v1/apiServiceV1'
import RightSidebar from '@/components/common/RightSidebar.vue'
import CategoryNavigation from '@/components/career/CategoryNavigation.vue'
import CareerFilter from '@/components/career/CareerFilter.vue'
import CareerGrid from '@/components/career/CareerGrid.vue'
import { useToastMessage } from '@/composables/useToastMessage'
import type { 
  Profession, 
  ProfessionCategory, 
  CategoryMapping 
} from '@/types/profession'

// 扩展职业类型以包含显示属性
interface CareerWithDisplay extends Profession {
  icon?: string
  iconColor?: string
}

interface CurrentQueryParams {
  type: 'all' | 'mainCategory' | 'subCategory'
  mainCategory: number | null
  subCategory: number | null
}

interface CareerApplication {
  name: string
  description: string
  mainCategory: number | null
  subCategory: number | null
  skills: string
}

const { t } = useI18n()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void
const router = useRouter()
const { getMessage } = useToastMessage()

// 状态管理
const careers: Ref<CareerWithDisplay[]> = ref([])
const filteredCareers: Ref<CareerWithDisplay[]> = ref([])
const loading: Ref<boolean> = ref(true)
const searchText: Ref<string> = ref('')
const selectedCategory: Ref<string> = ref('all')
const selectedDifficulty: Ref<string> = ref('all')
const selectedNavTab: Ref<string> = ref('career')

// 动态加载的分类数据
const categories: Ref<ProfessionCategory[]> = ref([])
const categoryMapping: Ref<CategoryMapping[]> = ref([])

// 三级目录相关状态
const activeFirstLvl: Ref<number> = ref(-1)
const activeSecondLvl: Ref<number> = ref(-1)
const currentCareers: Ref<CareerWithDisplay[]> = ref([])

// 分页相关状态
const displayedCareers: Ref<CareerWithDisplay[]> = ref([])
const careerPage: Ref<number> = ref(1)
const careerPageSize: Ref<number> = ref(20)
const hasMoreCareers: Ref<boolean> = ref(false)
const loadingMore: Ref<boolean> = ref(false)
const lastId: Ref<number> = ref(0)

// 动态加载职业类别数据
const loadProfessionCategories = async (): Promise<void> => {
  try {
    const response = await systemServiceV1.getProfessionCategories()
    console.log('Loaded profession categories:', response.data)

    if (response.data) {
      categories.value = response.data.mainCategories || []
      categoryMapping.value = response.data.categoryMapping || []
    }
  } catch (error) {
    console.error('Failed to load profession categories:', error)
  }
}

// 工具函数：根据主分类ID获取子分类列表
const getSubcategoriesByMainCategory = (mainCategoryId: number) => {
  const mapping = categoryMapping.value.find((item) => item.mainCategoryId === mainCategoryId)
  return mapping?.subcategories || []
}

// 工具函数：获取子分类ID
const getSubCategoryId = (mainCategoryId: number, subcategoryIndex: number): number | null => {
  const subcategories = getSubcategoriesByMainCategory(mainCategoryId)
  return subcategories[subcategoryIndex]?.id || null
}

// 当前查询参数
const currentQueryParams: Ref<CurrentQueryParams> = ref({
  type: 'all',
  mainCategory: null,
  subCategory: null,
})

// 申请职业对话框
const showApplicationDialog: Ref<boolean> = ref(false)
const applicationValid: Ref<boolean> = ref(false)
const submitting: Ref<boolean> = ref(false)
const newCareerApplication: Ref<CareerApplication> = ref({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null,
  skills: '',
})

// 监听主分类变化，清空子分类选择
watch(
  () => newCareerApplication.value.mainCategory,
  () => {
    newCareerApplication.value.subCategory = null
  }
)

// 随机图标数组
const availableIcons: string[] = [
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
  'mdi-gear',
]

// 随机颜色数组
const availableColors: string[] = [
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
  'lime',
]

// 为职业数据添加随机图标和颜色
const addRandomIconsToCareers = (careerList: Profession[]): CareerWithDisplay[] => {
  return careerList.map((career) => ({
    ...career,
    icon: (career as any).icon || availableIcons[Math.floor(Math.random() * availableIcons.length)],
    iconColor:
      (career as any).iconColor || availableColors[Math.floor(Math.random() * availableColors.length)],
  }))
}

// 加载职业数据
const loadCareerData = async (reset: boolean = true): Promise<void> => {
  try {
    if (reset) {
      loading.value = true
      lastId.value = 0
      currentQueryParams.value = { type: 'all', mainCategory: null, subCategory: null }
    }

    const response = await professionServiceV1.getApprovedProfessions(lastId.value)
    console.log('response', response.data)
    const newCareers = addRandomIconsToCareers(response.data || [])

    if (reset) {
      careers.value = newCareers
      filteredCareers.value = newCareers
      currentCareers.value = newCareers
      displayedCareers.value = newCareers
    } else {
      careers.value = [...careers.value, ...newCareers]
      filteredCareers.value = [...filteredCareers.value, ...newCareers]
      currentCareers.value = [...currentCareers.value, ...newCareers]
      displayedCareers.value = [...displayedCareers.value, ...newCareers]
    }

    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id
      hasMoreCareers.value = newCareers.length === careerPageSize.value
    } else {
      hasMoreCareers.value = false
    }
  } catch (error) {
    console.error('加载职业数据失败:', error)
    showSnackbar(getMessage('careerCenter.errors.loadFailed'), 'error')
    if (reset) {
      careers.value = []
      filteredCareers.value = []
      currentCareers.value = []
      displayedCareers.value = []
    }
    hasMoreCareers.value = false
  } finally {
    if (reset) {
      loading.value = false
    }
  }
}

// 根据主分类获取职业数据
const loadCareersByMainCategory = async (mainCategoryId: number, reset: boolean = true): Promise<void> => {
  try {
    if (reset) {
      loading.value = true
      lastId.value = 0
      currentQueryParams.value = {
        type: 'mainCategory',
        mainCategory: mainCategoryId,
        subCategory: null,
      }
    }

    const response = await professionServiceV1.getProfessionsByCategory(
      lastId.value,
      mainCategoryId,
      null
    )
    const newCareers = addRandomIconsToCareers(response.data || [])

    if (reset) {
      careers.value = newCareers
      filteredCareers.value = newCareers
      currentCareers.value = newCareers
      displayedCareers.value = newCareers
    } else {
      careers.value = [...careers.value, ...newCareers]
      filteredCareers.value = [...filteredCareers.value, ...newCareers]
      currentCareers.value = [...currentCareers.value, ...newCareers]
      displayedCareers.value = [...displayedCareers.value, ...newCareers]
    }

    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id
      hasMoreCareers.value = newCareers.length === careerPageSize.value
    } else {
      hasMoreCareers.value = false
    }
  } catch (error) {
    console.error('加载主分类职业失败:', error)
    showSnackbar(getMessage('careerCenter.errors.loadCategoryFailed'), 'error')
    currentCareers.value = []
    displayedCareers.value = []
    hasMoreCareers.value = false
  } finally {
    if (reset) {
      loading.value = false
    }
  }
}

// 根据子分类获取职业数据
const loadCareersBySubCategory = async (mainCategoryId: number, subCategoryId: number, reset: boolean = true): Promise<void> => {
  try {
    if (reset) {
      loading.value = true
      lastId.value = 0
      currentQueryParams.value = {
        type: 'subCategory',
        mainCategory: mainCategoryId,
        subCategory: subCategoryId,
      }
    }

    const response = await professionServiceV1.getProfessionsByCategory(
      lastId.value,
      mainCategoryId,
      subCategoryId
    )
    const newCareers = addRandomIconsToCareers(response.data || [])

    if (reset) {
      careers.value = newCareers
      filteredCareers.value = newCareers
      currentCareers.value = newCareers
      displayedCareers.value = newCareers
    } else {
      careers.value = [...careers.value, ...newCareers]
      filteredCareers.value = [...filteredCareers.value, ...newCareers]
      currentCareers.value = [...currentCareers.value, ...newCareers]
      displayedCareers.value = [...displayedCareers.value, ...newCareers]
    }

    if (newCareers.length > 0) {
      lastId.value = newCareers[newCareers.length - 1].id
      hasMoreCareers.value = newCareers.length === careerPageSize.value
    } else {
      hasMoreCareers.value = false
    }
  } catch (error) {
    console.error('加载子分类职业失败:', error)
    showSnackbar(getMessage('careerCenter.errors.loadSubcategoryFailed'), 'error')
    currentCareers.value = []
    displayedCareers.value = []
    hasMoreCareers.value = false
  } finally {
    if (reset) {
      loading.value = false
    }
  }
}

// 搜索和筛选
const filterCareers = (): void => {
  let filtered = careers.value

  // 文本搜索
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter(
      (career) =>
        career.name?.toLowerCase().includes(searchLower) ||
        career.description?.toLowerCase().includes(searchLower) ||
        career.skills?.split(',').some((skill: string) => skill.toLowerCase().includes(searchLower))
    )
  }

  // 类别筛选
  if (selectedCategory.value !== 'all') {
    filtered = filtered.filter((career) => career.mainCategory === Number(selectedCategory.value))
  }

  // 难度筛选 - 暂时禁用，Profession 接口中没有 difficulty 字段
  // if (selectedDifficulty.value !== 'all') {
  //   filtered = filtered.filter((career) => career.difficulty === selectedDifficulty.value)
  // }

  filteredCareers.value = filtered
}

// 跳转到职业详情
const goToCareerDetail = (career: CareerWithDisplay): void => {
  router.push(`/roadmap/${career.id}`)
}

// 执行搜索
const performSearch = (): void => {
  filterCareers()
}

// 三级导航函数
const selectFirstLevel = async (categoryValue: number): Promise<void> => {
  if (activeFirstLvl.value === categoryValue) {
    activeFirstLvl.value = -1
    activeSecondLvl.value = -1
    await loadCareerData(true)
    return
  }

  activeFirstLvl.value = categoryValue
  activeSecondLvl.value = -1

  if (categoryValue !== undefined && categoryValue !== null && categoryValue !== 0) {
    await loadCareersByMainCategory(categoryValue, true)
  }
}

const selectSecondLevel = async (subcategoryIndex: number): Promise<void> => {
  if (activeSecondLvl.value === subcategoryIndex) {
    activeSecondLvl.value = -1
    if (
      activeFirstLvl.value !== undefined &&
      activeFirstLvl.value !== null &&
      activeFirstLvl.value !== 0
    ) {
      await loadCareersByMainCategory(activeFirstLvl.value, true)
    }
    return
  }

  activeSecondLvl.value = subcategoryIndex

  const subCategoryId = getSubCategoryId(activeFirstLvl.value, subcategoryIndex)
  if (
    activeFirstLvl.value !== undefined &&
    activeFirstLvl.value !== null &&
    activeFirstLvl.value !== 0 &&
    subCategoryId !== null &&
    subCategoryId !== undefined
  ) {
    await loadCareersBySubCategory(activeFirstLvl.value, subCategoryId, true)
  }
}

const goBackToSecondLevel = async (): Promise<void> => {
  activeSecondLvl.value = -1

  if (
    activeFirstLvl.value !== undefined &&
    activeFirstLvl.value !== null &&
    activeFirstLvl.value !== 0
  ) {
    await loadCareersByMainCategory(activeFirstLvl.value, true)
  }
}

// 加载更多职业
const loadMoreCareers = async ({ done }: { done: (status: string) => void }): Promise<void> => {
  if (loadingMore.value || !hasMoreCareers.value) {
    done('empty')
    return
  }

  loadingMore.value = true
  careerPage.value++

  try {
    await new Promise((resolve) => setTimeout(resolve, 800))

    const { type, mainCategory, subCategory } = currentQueryParams.value

    if (type === 'all') {
      await loadCareerData(false)
    } else if (type === 'mainCategory' && mainCategory !== null) {
      await loadCareersByMainCategory(mainCategory, false)
    } else if (type === 'subCategory' && mainCategory !== null && subCategory !== null) {
      await loadCareersBySubCategory(mainCategory, subCategory, false)
    }

    if (hasMoreCareers.value) {
      done('ok')
    } else {
      done('empty')
    }
  } catch (error) {
    console.error('加载职业失败:', error)
    done('error')
  } finally {
    loadingMore.value = false
  }
}

// 申请职业相关函数
const openCareerApplicationDialog = (): void => {
  showApplicationDialog.value = true
}

const closeApplicationDialog = (): void => {
  showApplicationDialog.value = false
  newCareerApplication.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null,
    skills: '',
  }
}

const submitCareerApplication = async (): Promise<void> => {
  try {
    submitting.value = true

    const applicationData = {
      name: newCareerApplication.value.name,
      description: newCareerApplication.value.description,
      mainCategory: newCareerApplication.value.mainCategory,
      subCategory: newCareerApplication.value.subCategory,
      skills: newCareerApplication.value.skills || '',
    }

    const response = await professionServiceV1.createProfession(applicationData)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      showSnackbar(getMessage('careerCenter.application.submittedSuccess'), 'success')
      closeApplicationDialog()
    } else {
      showSnackbar(response.message || getMessage('careerCenter.application.submitFailed'), 'error')
    }
  } catch (error) {
    console.error('提交职业申请失败:', error)
    showSnackbar(getMessage('careerCenter.application.submitFailed'), 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProfessionCategories()
  loadCareerData(true)
})
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="12" md="9" lg="9" class="pr-lg-8">
        <!-- 页面头部和搜索 -->
        <CareerFilter
          v-model:search-text="searchText"
          v-model:selected-nav-tab="selectedNavTab"
          @perform-search="performSearch"
          @open-career-application="openCareerApplicationDialog"
        />

        <!-- 分类导航 -->
        <CategoryNavigation
          :categories="categories"
          :category-mapping="categoryMapping"
          :active-first-lvl="activeFirstLvl"
          :active-second-lvl="activeSecondLvl"
          :search-text="searchText"
          @select-first-level="selectFirstLevel"
          @select-second-level="selectSecondLevel"
        />

        <!-- 职业网格 -->
        <CareerGrid
          :displayed-careers="displayedCareers"
          :loading="loading"
          :active-first-lvl="activeFirstLvl"
          :active-second-lvl="activeSecondLvl"
          :current-careers="currentCareers"
          :careers="careers"
          :category-mapping="categoryMapping"
          :categories="categories"
          :search-text="searchText"
          @load-more-careers="loadMoreCareers"
          @go-to-career-detail="goToCareerDetail"
          @go-back-to-second-level="goBackToSecondLevel"
        />
      </v-col>

      <v-col cols="3">
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
            :rules="[(v: string) => !!v || t('careerCenter.application.nameRequired')]"
            required
            class="mb-4"
          >
          </v-text-field>

          <v-textarea
            v-model="newCareerApplication.description"
            :label="t('careerCenter.application.description')"
            variant="outlined"
            :rules="[(v: string) => !!v || t('careerCenter.application.descriptionRequired')]"
            required
            rows="3"
            class="mb-4"
          >
          </v-textarea>

          <v-select
            v-model="newCareerApplication.mainCategory"
            :items="categories"
            item-title="title"
            item-value="id"
            :label="t('course.mainCategory')"
            variant="outlined"
            density="compact"
            class="mb-4"
            clearable
            :rules="[(v: number | null) => v !== null || t('validation.required.mainCategory')]"
            required
          >
          </v-select>

          <v-select
            v-model="newCareerApplication.subCategory"
            :items="getSubcategoriesByMainCategory(newCareerApplication.mainCategory || 0)"
            item-title="name"
            item-value="id"
            :label="t('course.subCategory')"
            variant="outlined"
            density="compact"
            class="mb-4"
            :disabled="!newCareerApplication.mainCategory"
            clearable
            :rules="[(v: number | null) => v !== null || t('validation.required.subCategory')]"
            required
          >
          </v-select>

          <v-text-field
            v-model="newCareerApplication.skills"
            :label="t('careerCenter.application.skills')"
            variant="outlined"
            :hint="t('careerCenter.application.skillsHint')"
            persistent-hint
            class="mb-4"
          >
          </v-text-field>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 pb-6">
        <v-spacer></v-spacer>
        <v-btn variant="text" :disabled="submitting" @click="closeApplicationDialog">
          {{ t('careerCenter.application.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          variant="flat"
          :disabled="!applicationValid || submitting"
          :loading="submitting"
          @click="submitCareerApplication"
        >
          {{ t('careerCenter.application.submit') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* Flat 风格 - 无阴影设计 */
</style>