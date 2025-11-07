<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import CareerFilter from '@/components/career/CareerFilter.vue'
import CategoryNavigation from '@/components/career/CategoryNavigation.vue'
import CareerGrid from '@/components/career/CareerGrid.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import LeftSidebar from '@/components/layout/LeftSidebar.vue'
import type {
  CareerWithDisplay,
  ProfessionCategory,
  CategoryMapping,
  CareerApplication
} from '@/types/profession'

const router = useRouter()

// 状态管理
const loading = ref(false)
const searchText = ref('')
const activeFirstLvl = ref(-1)
const activeSecondLvl = ref(-1)
const showApplicationDialog = ref(false)
const applicationValid = ref(false)
const submitting = ref(false)

// Mock 数据 - 分类
const categories = ref<ProfessionCategory[]>([
  { id: 1, title: '技术开发', icon: 'mdi-laptop', order: 1 },
  { id: 2, title: '产品设计', icon: 'mdi-palette', order: 2 },
  { id: 3, title: '商业运营', icon: 'mdi-briefcase', order: 3 },
  { id: 4, title: '市场营销', icon: 'mdi-bullhorn', order: 4 },
  { id: 5, title: '数据分析', icon: 'mdi-chart-bar', order: 5 }
])

// Mock 数据 - 分类映射
const categoryMapping = ref<CategoryMapping[]>([
  {
    mainCategoryId: 1,
    subcategories: [
      { id: 101, name: '前端开发', order: 1 },
      { id: 102, name: '后端开发', order: 2 },
      { id: 103, name: '全栈开发', order: 3 },
      { id: 104, name: '移动开发', order: 4 },
      { id: 105, name: 'DevOps', order: 5 }
    ]
  },
  {
    mainCategoryId: 2,
    subcategories: [
      { id: 201, name: 'UI设计', order: 1 },
      { id: 202, name: 'UX设计', order: 2 },
      { id: 203, name: '产品经理', order: 3 },
      { id: 204, name: '交互设计', order: 4 }
    ]
  },
  {
    mainCategoryId: 3,
    subcategories: [
      { id: 301, name: '运营专员', order: 1 },
      { id: 302, name: '商务拓展', order: 2 },
      { id: 303, name: '项目管理', order: 3 },
      { id: 304, name: '供应链', order: 4 }
    ]
  },
  {
    mainCategoryId: 4,
    subcategories: [
      { id: 401, name: '内容营销', order: 1 },
      { id: 402, name: '品牌营销', order: 2 },
      { id: 403, name: '增长运营', order: 3 },
      { id: 404, name: 'SEO/SEM', order: 4 }
    ]
  },
  {
    mainCategoryId: 5,
    subcategories: [
      { id: 501, name: '数据分析师', order: 1 },
      { id: 502, name: '数据工程师', order: 2 },
      { id: 503, name: '数据科学家', order: 3 },
      { id: 504, name: 'BI分析师', order: 4 }
    ]
  }
])

// 随机图标和颜色
const availableIcons = [
  'mdi-laptop', 'mdi-palette', 'mdi-briefcase', 'mdi-bullhorn', 'mdi-chart-bar',
  'mdi-rocket', 'mdi-lightbulb', 'mdi-star', 'mdi-diamond', 'mdi-crown'
]

const availableColors = [
  'primary', 'success', 'warning', 'error', 'info', 'purple', 'indigo',
  'blue', 'cyan', 'teal', 'green', 'orange'
]

// Mock 数据 - 职业列表
const allCareers = ref<CareerWithDisplay[]>([
  {
    id: 1,
    name: '前端工程师',
    description: '负责开发网站和应用的用户界面，使用现代前端框架构建响应式、高性能的Web应用。需要精通HTML、CSS、JavaScript等技术，熟悉Vue/React等主流框架。',
    mainCategory: 1,
    subCategory: 101,
    skills: 'Vue.js,React,TypeScript,HTML,CSS,JavaScript,Webpack',
    icon: availableIcons[0],
    iconColor: availableColors[0],
    learnerCount: 15234
  },
  {
    id: 2,
    name: '后端工程师',
    description: '设计和开发服务器端应用程序，负责数据库设计、API开发和系统架构。精通Java、Python或Node.js等后端语言，熟悉微服务架构和云原生技术。',
    mainCategory: 1,
    subCategory: 102,
    skills: 'Java,Spring Boot,MySQL,Redis,微服务,Docker',
    icon: availableIcons[1],
    iconColor: availableColors[1],
    learnerCount: 12890
  },
  {
    id: 3,
    name: '全栈工程师',
    description: '同时掌握前端和后端开发技能，能够独立完成完整的Web应用开发。需要具备全面的技术栈知识和良好的系统设计能力。',
    mainCategory: 1,
    subCategory: 103,
    skills: 'Vue,Node.js,MongoDB,TypeScript,RESTful API',
    icon: availableIcons[2],
    iconColor: availableColors[2],
    learnerCount: 8765
  },
  {
    id: 4,
    name: 'iOS开发工程师',
    description: '专注于iOS平台的移动应用开发，使用Swift或Objective-C构建高质量的iPhone和iPad应用。',
    mainCategory: 1,
    subCategory: 104,
    skills: 'Swift,iOS SDK,UIKit,SwiftUI,Xcode',
    icon: availableIcons[3],
    iconColor: availableColors[3],
    learnerCount: 6543
  },
  {
    id: 5,
    name: 'Android开发工程师',
    description: '负责Android平台的移动应用开发，使用Kotlin或Java创建流畅、高性能的Android应用。',
    mainCategory: 1,
    subCategory: 104,
    skills: 'Kotlin,Android SDK,Jetpack,Material Design',
    icon: availableIcons[4],
    iconColor: availableColors[4],
    learnerCount: 7234
  },
  {
    id: 6,
    name: 'DevOps工程师',
    description: '负责开发和运维的自动化流程，构建CI/CD管道，管理云基础设施，确保系统的高可用性和稳定性。',
    mainCategory: 1,
    subCategory: 105,
    skills: 'Docker,Kubernetes,Jenkins,AWS,Linux,自动化',
    icon: availableIcons[5],
    iconColor: availableColors[5],
    learnerCount: 9876
  },
  {
    id: 7,
    name: 'UI设计师',
    description: '设计用户界面的视觉元素，创建美观、一致的设计系统。精通Figma、Sketch等设计工具，了解最新的设计趋势。',
    mainCategory: 2,
    subCategory: 201,
    skills: 'Figma,Sketch,Adobe XD,设计系统,视觉设计',
    icon: availableIcons[6],
    iconColor: availableColors[6],
    learnerCount: 11234
  },
  {
    id: 8,
    name: 'UX设计师',
    description: '研究用户行为和需求，设计用户体验流程，通过用户测试和数据分析优化产品的可用性和用户满意度。',
    mainCategory: 2,
    subCategory: 202,
    skills: '用户研究,原型设计,可用性测试,交互设计,信息架构',
    icon: availableIcons[7],
    iconColor: availableColors[7],
    learnerCount: 8234
  },
  {
    id: 9,
    name: '产品经理',
    description: '负责产品规划和管理，定义产品需求，协调开发团队，确保产品按时交付并满足用户需求和商业目标。',
    mainCategory: 2,
    subCategory: 203,
    skills: '需求分析,产品规划,Axure,用户故事,敏捷开发',
    icon: availableIcons[8],
    iconColor: availableColors[8],
    learnerCount: 13456
  },
  {
    id: 10,
    name: '运营专员',
    description: '负责产品运营和用户增长，策划运营活动，分析运营数据，提升用户活跃度和留存率。',
    mainCategory: 3,
    subCategory: 301,
    skills: '内容运营,用户运营,活动策划,数据分析,社群运营',
    icon: availableIcons[9],
    iconColor: availableColors[9],
    learnerCount: 10567
  },
  {
    id: 11,
    name: '商务拓展',
    description: '开拓新的商业机会，建立和维护合作伙伴关系，谈判商务合作，推动业务增长。',
    mainCategory: 3,
    subCategory: 302,
    skills: '商务谈判,市场分析,客户关系,合作伙伴,销售',
    icon: availableIcons[0],
    iconColor: availableColors[10],
    learnerCount: 7890
  },
  {
    id: 12,
    name: '项目经理',
    description: '管理项目的全生命周期，协调资源，控制进度和成本，确保项目按计划交付并达到质量标准。',
    mainCategory: 3,
    subCategory: 303,
    skills: 'PMP,项目管理,敏捷,Scrum,风险管理,沟通协调',
    icon: availableIcons[1],
    iconColor: availableColors[11],
    learnerCount: 9234
  },
  {
    id: 13,
    name: '内容营销',
    description: '创作和分发有价值的内容，吸引和留住目标受众，通过内容驱动用户行为和品牌认知。',
    mainCategory: 4,
    subCategory: 401,
    skills: '内容创作,文案策划,SEO,社交媒体,新媒体运营',
    icon: availableIcons[2],
    iconColor: availableColors[0],
    learnerCount: 11890
  },
  {
    id: 14,
    name: '品牌营销',
    description: '建立和管理品牌形象，策划品牌活动，提升品牌知名度和美誉度，塑造品牌价值。',
    mainCategory: 4,
    subCategory: 402,
    skills: '品牌策划,市场调研,广告投放,公关传播,品牌管理',
    icon: availableIcons[3],
    iconColor: availableColors[1],
    learnerCount: 8567
  },
  {
    id: 15,
    name: '增长运营',
    description: '通过数据驱动的方法实现用户和收入增长，优化转化漏斗，执行增长实验和A/B测试。',
    mainCategory: 4,
    subCategory: 403,
    skills: '增长黑客,数据分析,A/B测试,用户获取,转化优化',
    icon: availableIcons[4],
    iconColor: availableColors[2],
    learnerCount: 12345
  },
  {
    id: 16,
    name: 'SEO专员',
    description: '优化网站搜索引擎排名，制定SEO策略，分析关键词和竞争对手，提升网站流量和可见度。',
    mainCategory: 4,
    subCategory: 404,
    skills: 'SEO,SEM,关键词分析,Google Analytics,搜索引擎优化',
    icon: availableIcons[5],
    iconColor: availableColors[3],
    learnerCount: 6789
  },
  {
    id: 17,
    name: '数据分析师',
    description: '收集、处理和分析数据，提供业务洞察和决策支持，使用数据可视化工具呈现分析结果。',
    mainCategory: 5,
    subCategory: 501,
    skills: 'SQL,Python,Excel,Tableau,Power BI,统计分析',
    icon: availableIcons[6],
    iconColor: availableColors[4],
    learnerCount: 14567
  },
  {
    id: 18,
    name: '数据工程师',
    description: '构建和维护数据管道，设计数据仓库，确保数据质量和可用性，支持大数据处理和分析。',
    mainCategory: 5,
    subCategory: 502,
    skills: 'Spark,Hadoop,ETL,数据仓库,大数据,数据管道',
    icon: availableIcons[7],
    iconColor: availableColors[5],
    learnerCount: 10234
  },
  {
    id: 19,
    name: '数据科学家',
    description: '运用统计学和机器学习方法解决复杂的业务问题，构建预测模型，发现数据中的模式和洞察。',
    mainCategory: 5,
    subCategory: 503,
    skills: 'Python,机器学习,深度学习,统计学,TensorFlow,数据挖掘',
    icon: availableIcons[8],
    iconColor: availableColors[6],
    learnerCount: 13678
  },
  {
    id: 20,
    name: 'BI分析师',
    description: '开发商业智能报表和仪表板，为管理层提供数据驱动的业务洞察，支持战略决策。',
    mainCategory: 5,
    subCategory: 504,
    skills: 'Power BI,Tableau,SQL,数据可视化,商业分析,报表开发',
    icon: availableIcons[9],
    iconColor: availableColors[7],
    learnerCount: 9456
  }
])

const careers = ref<CareerWithDisplay[]>([...allCareers.value])
const filteredCareers = ref<CareerWithDisplay[]>([...allCareers.value])
const displayedCareers = ref<CareerWithDisplay[]>([...allCareers.value])

// 职业申请表单
const newCareerApplication = ref<CareerApplication>({
  name: '',
  description: '',
  mainCategory: null,
  subCategory: null,
  skills: ''
})

// 热门职业 - 按 id 排序取前15个 (模拟热度)
const popularCareers = computed(() => {
  return [...allCareers.value]
    .sort((a, b) => a.id - b.id)
    .slice(0, 15)
})

// 搜索和筛选
const filterCareers = (): void => {
  let filtered = [...allCareers.value]

  // 文本搜索
  if (searchText.value.trim()) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter(
      (career) =>
        career.name?.toLowerCase().includes(searchLower) ||
        career.description?.toLowerCase().includes(searchLower) ||
        career.skills?.toLowerCase().includes(searchLower)
    )
  }

  filteredCareers.value = filtered
  displayedCareers.value = filtered
}

// 执行搜索
const performSearch = (): void => {
  filterCareers()
}

// 选择一级分类
const selectFirstLevel = (categoryId: number): void => {
  if (activeFirstLvl.value === categoryId) {
    activeFirstLvl.value = -1
    activeSecondLvl.value = -1
    displayedCareers.value = [...allCareers.value]
    return
  }

  activeFirstLvl.value = categoryId
  activeSecondLvl.value = -1

  if (categoryId !== -1 && categoryId !== 0) {
    displayedCareers.value = allCareers.value.filter(c => c.mainCategory === categoryId)
  }
}

// 选择二级分类
const selectSecondLevel = (subcategoryIndex: number): void => {
  if (activeSecondLvl.value === subcategoryIndex) {
    activeSecondLvl.value = -1
    if (activeFirstLvl.value !== -1) {
      displayedCareers.value = allCareers.value.filter(c => c.mainCategory === activeFirstLvl.value)
    }
    return
  }

  activeSecondLvl.value = subcategoryIndex

  const mapping = categoryMapping.value.find(m => m.mainCategoryId === activeFirstLvl.value)
  const subCategory = mapping?.subcategories[subcategoryIndex]

  if (subCategory) {
    displayedCareers.value = allCareers.value.filter(c => c.subCategory === subCategory.id)
  }
}

// 跳转到职业详情（学习路径列表）
const goToCareerDetail = (career: CareerWithDisplay): void => {
  router.push(`/roadmap/${career.id}`)
}

// 打开申请对话框
const openCareerApplicationDialog = (): void => {
  showApplicationDialog.value = true
}

// 关闭申请对话框
const closeApplicationDialog = (): void => {
  showApplicationDialog.value = false
  newCareerApplication.value = {
    name: '',
    description: '',
    mainCategory: null,
    subCategory: null,
    skills: ''
  }
}

// 提交职业申请
const submitCareerApplication = (): void => {
  console.log('提交职业申请:', newCareerApplication.value)
  closeApplicationDialog()
}

// 获取子分类
const getSubcategoriesByMainCategory = (mainCategoryId: number | null) => {
  if (!mainCategoryId) return []
  const mapping = categoryMapping.value.find(m => m.mainCategoryId === mainCategoryId)
  return mapping?.subcategories || []
}
</script>

<template>
  <div class="career-center-page">
    <AppHeader />
    <LeftSidebar />

    <div class="main-content">
      <!-- 页面标题 -->
      <div class="mb-6">
        <div class="d-flex align-center">
          <v-avatar color="grey-lighten-3" size="64" rounded="lg" class="mr-3">
            <v-icon size="32" color="#666666">mdi-briefcase-variant</v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4">职业中心</h1>
            <p class="text-body-2 text-grey-darken-2 mt-1">探索职业路径，规划职业发展</p>
          </div>
        </div>
      </div>

      <!-- 搜索和筛选 -->
      <CareerFilter
        v-model:search-text="searchText"
        @perform-search="performSearch"
        @open-career-application="openCareerApplicationDialog"
      />

      <!-- 分类导航和职业网格 -->
      <div class="content-wrapper">
        <div class="main-content-area">
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
            :categories="categories"
            :category-mapping="categoryMapping"
            :search-text="searchText"
            @go-to-career-detail="goToCareerDetail"
          />
        </div>

        <!-- 右侧热门职业栏 -->
        <div class="sidebar-area">
          <v-card rounded="lg" class="popular-card sticky-card narrow-sidebar no-border" flat>
            <v-card-title class="pa-4 pb-3">
              <div class="d-flex align-center justify-space-between w-100">
                <div class="d-flex align-center">
                  <v-icon icon="mdi-fire" color="error" class="mr-2"></v-icon>
                  <span class="text-h6 font-weight-bold">热门职业</span>
                </div>
                <v-btn
                  variant="text"
                  size="small"
                  color="primary"
                  class="text-caption"
                  @click="() => {
                    activeFirstLvl = -1
                    activeSecondLvl = -1
                    searchText = ''
                    displayedCareers = [...allCareers.value]
                  }"
                >
                  全部
                  <v-icon icon="mdi-chevron-right" size="14" class="ml-1"></v-icon>
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0 popular-list pb-4">
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
                    <v-icon icon="mdi-account-group" size="12" color="grey"></v-icon>
                    {{ career.learnerCount?.toLocaleString() }}
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
    </div>

    <!-- 职业申请对话框 -->
    <v-dialog v-model="showApplicationDialog" max-width="600px" persistent>
      <v-card rounded="lg" border>
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-plus-circle" color="primary" size="32" class="mr-3"></v-icon>
            <span class="text-h6 font-weight-bold">申请新职业</span>
          </div>
        </v-card-title>

        <v-card-text class="px-6 pb-0">
          <v-form v-model="applicationValid">
            <v-text-field
              v-model="newCareerApplication.name"
              label="职业名称"
              variant="outlined"
              clearable
              required
              class="mb-4"
            ></v-text-field>

            <v-textarea
              v-model="newCareerApplication.description"
              label="职业描述"
              variant="outlined"
              clearable
              required
              rows="3"
              class="mb-4"
            ></v-textarea>

            <v-select
              v-model="newCareerApplication.mainCategory"
              :items="categories"
              item-title="title"
              item-value="id"
              label="主分类"
              variant="outlined"
              class="mb-4"
              clearable
              required
            ></v-select>

            <v-select
              v-model="newCareerApplication.subCategory"
              :items="getSubcategoriesByMainCategory(newCareerApplication.mainCategory)"
              item-title="name"
              item-value="id"
              label="子分类"
              variant="outlined"
              class="mb-4"
              :disabled="!newCareerApplication.mainCategory"
              clearable
              required
            ></v-select>

            <v-text-field
              v-model="newCareerApplication.skills"
              label="所需技能（用逗号分隔）"
              variant="outlined"
              hint="例如：Vue.js,TypeScript,Node.js"
              persistent-hint
              class="mb-4"
            ></v-text-field>
          </v-form>
        </v-card-text>

        <v-card-actions class="px-6 pb-6">
          <v-spacer></v-spacer>
          <v-btn
            variant="outlined"
            rounded="lg"
            :disabled="submitting"
            @click="closeApplicationDialog"
          >
            取消
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!applicationValid || submitting"
            :loading="submitting"
            @click="submitCareerApplication"
          >
            提交申请
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

  </div>
</template>

<style scoped>
.career-center-page {
  min-height: 100vh;
  background-color: #FFFFFF;
}

.main-content {
  margin-left: max(160px, calc((100vw - 1550px) / 2));
  padding: 80px 40px 40px 40px;
  max-width: 1550px;
  width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
}

@media (min-width: 2229px) {
  .main-content {
    margin-left: max(160px, calc((100vw - 1550px) / 2));
    padding: 80px 40px 40px 40px;
    width: calc(100% - max(160px, calc((100vw - 1550px) / 2)));
    max-width: 1550px;
  }
}

/* 移动端 */
@media (max-width: 960px) {
  .main-content {
    margin-left: 0;
    width: 100%;
    max-width: 100%;
    padding: 80px 20px 80px 20px;
  }

  .content-wrapper {
    display: block;
  }

  .main-content-area {
    margin-right: 0;
  }

  .sidebar-area {
    margin-top: 20px;
  }

  .sticky-card {
    position: static;
    max-height: none;
  }

  .popular-list {
    max-height: 400px;
  }
}

/* 两栏布局 */
.content-wrapper {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

.main-content-area {
  flex: 1;
  min-width: 0;
}

.sidebar-area {
  flex-shrink: 0;
}

.sticky-card {
  position: sticky;
  top: 75px;
  max-height: calc(100vh - 95px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.narrow-sidebar {
  width: 280px;
  max-width: 280px;
}

@media (max-width: 1264px) {
  .narrow-sidebar {
    width: 100%;
    max-width: 100%;
  }
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
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.popular-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.popular-item {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.popular-item:hover {
  background-color: #F5F5F5;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background-color: #E5E5E5;
  color: #666666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-right: 12px;
  flex-shrink: 0;
}

.rank-badge.rank-top {
  background: linear-gradient(135deg, #FFD700 0%, #FFA500 100%);
  color: #FFFFFF;
}

.popular-name {
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-count {
  font-size: 12px;
  color: #666666;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 确保页面不会超出视口 */
.career-page {
  overflow-x: hidden;
}
</style>
