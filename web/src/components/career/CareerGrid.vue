<script setup>
  import { computed } from 'vue'
  import { useI18n } from 'vue-i18n'
  import CareerCard from '@/components/career/CareerCard.vue'

  const { t } = useI18n()

  // Props
  const props = defineProps({
    displayedCareers: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    activeFirstLvl: {
      type: Number,
      default: -1,
    },
    activeSecondLvl: {
      type: Number,
      default: -1,
    },
    currentCareers: {
      type: Array,
      default: () => [],
    },
    careers: {
      type: Array,
      default: () => [],
    },
    categoryMapping: {
      type: Array,
      default: () => [],
    },
    categories: {
      type: Array,
      default: () => [],
    },
    searchText: {
      type: String,
      default: '',
    },
  })

  // Emits
  const emit = defineEmits(['loadMoreCareers', 'goToCareerDetail', 'goBackToSecondLevel'])

  // 工具函数：根据主分类ID获取子分类列表
  const getSubcategoriesByMainCategory = (mainCategoryId) => {
    const mapping = props.categoryMapping.find((item) => item.mainCategoryId === mainCategoryId)
    return mapping?.subcategories || []
  }

  // 工具函数：根据ID获取分类名称
  const getCategoryName = (categoryId) => {
    const category = props.categories.find((cat) => cat.id === categoryId)
    return category?.title || ''
  }

  // 工具函数：根据子分类ID获取子分类名称
  const getSubCategoryNameById = (subCategoryId) => {
    for (const mapping of props.categoryMapping) {
      const subcategory = mapping.subcategories.find((sub) => sub.id === subCategoryId)
      if (subcategory) {
        return subcategory.name
      }
    }
    return ''
  }

  // 处理加载更多
  const handleLoadMoreCareers = (loadOptions) => {
    emit('loadMoreCareers', loadOptions)
  }

  // 处理职业详情跳转
  const handleGoToCareerDetail = (career) => {
    emit('goToCareerDetail', career)
  }

  // 处理返回二级分类
  const handleGoBackToSecondLevel = () => {
    emit('goBackToSecondLevel')
  }

  // 当前子分类信息
  const currentSubcategory = computed(() => {
    if (props.activeFirstLvl !== -1 && props.activeSecondLvl !== -1) {
      const subcategories = getSubcategoriesByMainCategory(props.activeFirstLvl)
      return subcategories[props.activeSecondLvl]
    }
    return null
  })

  // 判断是否显示职业列表
  const showCareerList = computed(() => {
    return true // 总是显示职业列表，由内部条件控制具体显示内容
  })
</script>

<template>
  <div v-if="showCareerList">
    <!-- 搜索结果或分类结果 -->
    <div v-if="searchText.trim() || (activeFirstLvl !== -1 && activeSecondLvl === -1)">
      <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
        <span v-if="searchText.trim()">
          {{
            t('careerCenter.listing.searchResults', {
              query: searchText,
              count: displayedCareers.length,
            })
          }}
        </span>
        <span v-else-if="activeFirstLvl !== -1">
          {{ getCategoryName(activeFirstLvl) }} -
          {{ t('careerCenter.listing.categoryJobs', { count: currentCareers.length }) }}
        </span>
      </h4>

      <!-- 空状态提示 -->
      <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
        <v-icon
          :icon="searchText.trim() ? 'mdi-magnify' : 'mdi-briefcase-search-outline'"
          size="64"
          color="grey-lighten-1"
          class="mb-4"
        >
        </v-icon>
        <h3 class="text-h6 text-grey-darken-1 mb-2">
          {{
            searchText.trim()
              ? t('careerCenter.empty.noSearchResults')
              : t('careerCenter.empty.noRelatedJobs')
          }}
        </h3>
        <p class="text-body-2 text-grey mb-4">
          {{
            searchText.trim()
              ? t('careerCenter.empty.noSearchResultsDesc')
              : t('careerCenter.empty.noRelatedJobsDesc')
          }}
        </p>
      </div>

      <!-- 职业列表 -->
      <v-infinite-scroll
        v-else
        :items="displayedCareers"
        :on-load="handleLoadMoreCareers"
        color="primary"
      >
        <v-row>
          <v-col v-for="career in displayedCareers" :key="career.id" cols="12" md="6" lg="4">
            <CareerCard
              :career="career"
              :get-category-name="getCategoryName"
              :get-sub-category-name-by-id="getSubCategoryNameById"
              @click="handleGoToCareerDetail(career)"
            />
          </v-col>
        </v-row>

        <!-- 加载中状态 -->
        <template #loading>
          <div class="text-center py-4">
            <v-progress-circular indeterminate color="primary" size="24"> </v-progress-circular>
            <p class="text-body-2 text-grey mt-2">{{ t('careerCenter.listing.loadingMore') }}</p>
          </div>
        </template>

        <!-- 没有更多数据时的提示 -->
        <template #empty>
          <div class="text-center pt-8 pb-4">
            <p class="text-body-2 text-grey">{{ t('careerCenter.listing.allLoaded') }}</p>
          </div>
        </template>
      </v-infinite-scroll>
    </div>

    <!-- 子分类职业列表 -->
    <div v-else-if="activeFirstLvl !== -1 && activeSecondLvl !== -1">
      <div class="d-flex align-center mb-3">
        <v-btn
          icon="mdi-arrow-left"
          variant="text"
          size="small"
          color="grey-darken-2"
          @click="handleGoBackToSecondLevel"
        >
        </v-btn>
        <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 ml-2">
          {{ currentSubcategory?.name }}
          - {{ t('careerCenter.listing.specificJobs', { count: currentCareers.length }) }}
        </h4>
      </div>

      <!-- 空状态提示 -->
      <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
        <v-icon
          icon="mdi-briefcase-search-outline"
          size="64"
          color="grey-lighten-1"
          class="mb-4"
        ></v-icon>
        <h3 class="text-h6 text-grey-darken-1 mb-2">{{ t('careerCenter.empty.noRelatedJobs') }}</h3>
        <p class="text-body-2 text-grey mb-4">{{ t('careerCenter.empty.noRelatedJobsDesc') }}</p>
      </div>

      <!-- 无限滚动容器 -->
      <v-infinite-scroll
        v-else
        :items="displayedCareers"
        :on-load="handleLoadMoreCareers"
        color="primary"
      >
        <v-row>
          <v-col v-for="career in displayedCareers" :key="career.id" cols="12" md="6" lg="4">
            <CareerCard
              :career="career"
              :get-category-name="getCategoryName"
              :get-sub-category-name-by-id="getSubCategoryNameById"
              @click="handleGoToCareerDetail(career)"
            />
          </v-col>
        </v-row>

        <!-- 加载中状态 -->
        <template #loading>
          <div class="text-center py-4">
            <v-progress-circular indeterminate color="primary" size="24"> </v-progress-circular>
            <p class="text-body-2 text-grey mt-2">{{ t('careerCenter.listing.loadingMore') }}</p>
          </div>
        </template>

        <!-- 没有更多数据时的提示 -->
        <template #empty>
          <div class="text-center pt-8 pb-4">
            <p class="text-body-2 text-grey">{{ t('careerCenter.listing.allLoaded') }}</p>
          </div>
        </template>
      </v-infinite-scroll>
    </div>

    <!-- 默认职业列表（未选择任何分类） -->
    <div v-else-if="activeFirstLvl === -1">
      <h4 class="text-subtitle-1 font-weight-bold text-grey-darken-3 mb-3">
        {{ t('careerCenter.listing.hotProfessions', { count: careers.length }) }}
      </h4>

      <!-- 空状态提示 -->
      <div v-if="!loading && displayedCareers.length === 0" class="text-center py-12">
        <v-icon icon="mdi-briefcase-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
        <h3 class="text-h6 text-grey-darken-1 mb-2">{{ t('careerCenter.empty.noJobs') }}</h3>
        <p class="text-body-2 text-grey mb-4">{{ t('careerCenter.empty.noJobsDesc') }}</p>
      </div>

      <!-- 无限滚动容器 -->
      <v-infinite-scroll
        v-else
        :items="displayedCareers"
        :on-load="handleLoadMoreCareers"
        color="primary"
      >
        <v-row>
          <v-col v-for="career in displayedCareers" :key="career.id" cols="12" md="6" lg="4">
            <CareerCard
              :career="career"
              :get-category-name="getCategoryName"
              :get-sub-category-name-by-id="getSubCategoryNameById"
              @click="handleGoToCareerDetail(career)"
            />
          </v-col>
        </v-row>

        <!-- 加载中状态 -->
        <template #loading>
          <div class="text-center py-4">
            <v-progress-circular indeterminate color="primary" size="24"> </v-progress-circular>
            <p class="text-body-2 text-grey mt-2">{{ t('careerCenter.listing.loadingMore') }}</p>
          </div>
        </template>

        <!-- 没有更多数据时的提示 -->
        <template #empty>
          <div class="text-center pt-8 pb-4">
            <p class="text-body-2 text-grey">{{ t('careerCenter.listing.allLoaded') }}</p>
          </div>
        </template>
      </v-infinite-scroll>
    </div>
  </div>
</template>

<style scoped>
  /* 职业网格样式 */
  .v-infinite-scroll {
    min-height: 200px;
  }

  /* 空状态样式 */
  .text-center {
    text-align: center;
  }

  /* 加载状态样式 */
  .text-center.py-4 {
    padding: 16px 0;
  }

  .text-center.pt-8.pb-4 {
    padding-top: 32px;
    padding-bottom: 16px;
  }
</style>
