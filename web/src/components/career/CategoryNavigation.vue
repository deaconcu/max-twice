<script setup>
  import { computed } from 'vue'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  // Props
  const props = defineProps({
    categories: {
      type: Array,
      default: () => [],
    },
    categoryMapping: {
      type: Array,
      default: () => [],
    },
    activeFirstLvl: {
      type: Number,
      default: -1,
    },
    activeSecondLvl: {
      type: Number,
      default: -1,
    },
    searchText: {
      type: String,
      default: '',
    },
  })

  // Emits
  const emit = defineEmits(['selectFirstLevel', 'selectSecondLevel'])

  // 工具函数：根据主分类ID获取子分类列表
  const getSubcategoriesByMainCategory = (mainCategoryId) => {
    const mapping = props.categoryMapping.find((item) => item.mainCategoryId === mainCategoryId)
    return mapping?.subcategories || []
  }

  // 处理一级分类选择
  const handleFirstLevelSelect = (categoryId) => {
    emit('selectFirstLevel', categoryId)
  }

  // 处理二级分类选择
  const handleSecondLevelSelect = (subcategoryIndex) => {
    emit('selectSecondLevel', subcategoryIndex)
  }

  // 当前一级分类信息
  const currentFirstCategory = computed(() => {
    return props.categories.find((c) => c.id === props.activeFirstLvl)
  })
</script>

<template>
  <!-- 一级分类按钮导航 -->
  <v-row v-if="!searchText.trim()" class="mb-4">
    <v-col cols="12">
      <v-card flat class="bg-grey-lighten-5 px-6 pt-6 pb-2 category-navigation-card" rounded="xl">
        <!-- 标题区域 -->
        <div class="d-flex align-center mb-5">
          <div class="pa-3 rounded-xl bg-white mr-3">
            <v-icon icon="mdi-briefcase-variant" color="blue-darken-2" size="24"></v-icon>
          </div>
          <div>
            <h3 class="text-h6 font-weight-bold text-blue-grey-darken-3 mb-1">
              {{ t('careerCenter.category.title') }}
            </h3>
            <p class="text-caption text-blue-grey-darken-1 mb-0">
              <v-icon icon="mdi-filter-outline" size="12" class="mr-1"></v-icon>
              {{ t('careerCenter.category.subtitle') }}
            </p>
          </div>
        </div>

        <!-- 一级分类按钮组 -->
        <div class="d-flex flex-wrap mb-6 category-buttons-gap">
          <v-btn
            v-for="category in categories"
            :key="category.id"
            :color="activeFirstLvl === category.id ? 'blue-darken-1' : 'white'"
            variant="flat"
            rounded="xl"
            class="font-weight-medium category-btn-flat"
            @click="handleFirstLevelSelect(category.id)"
          >
            <v-icon
              :icon="category.icon"
              size="18"
              class="mr-2"
              :color="activeFirstLvl === category.id ? 'white' : 'blue-grey-darken-2'"
            >
            </v-icon>
            <span
              :class="activeFirstLvl === category.id ? 'text-white' : 'text-blue-grey-darken-3'"
            >
              {{ category.title }}
            </span>
          </v-btn>
        </div>

        <!-- 二级分类按钮 -->
        <div v-if="activeFirstLvl !== -1 && activeFirstLvl !== 0" class="mt-4">
          <!-- 二级分类标题 -->
          <div class="pa-4 mb-4 rounded-xl bg-white">
            <div class="d-flex align-center mb-3">
              <v-icon
                icon="mdi-chevron-right"
                color="blue-darken-1"
                size="16"
                class="mr-2"
              ></v-icon>
              <h4 class="text-subtitle-1 font-weight-bold text-blue-grey-darken-3 mb-0">
                {{ currentFirstCategory?.title }} -
                {{ t('careerCenter.category.specificDirection') }}
              </h4>
            </div>

            <!-- 二级分类按钮组 -->
            <div class="d-flex flex-wrap subcategory-buttons-gap">
              <v-btn
                v-for="(subcategory, subcategoryIndex) in getSubcategoriesByMainCategory(
                  activeFirstLvl
                )"
                :key="subcategoryIndex"
                :color="activeSecondLvl === subcategoryIndex ? 'orange-darken-1' : 'grey-lighten-3'"
                variant="flat"
                rounded="xl"
                class="font-weight-medium subcategory-btn-flat"
                @click="handleSecondLevelSelect(subcategoryIndex)"
              >
                <v-icon
                  :icon="
                    activeSecondLvl === subcategoryIndex ? 'mdi-folder-open' : 'mdi-folder-outline'
                  "
                  size="14"
                  class="mr-1"
                  :color="activeSecondLvl === subcategoryIndex ? 'white' : 'blue-grey-darken-2'"
                >
                </v-icon>
                <span
                  :class="
                    activeSecondLvl === subcategoryIndex ? 'text-white' : 'text-blue-grey-darken-3'
                  "
                >
                  {{ subcategory.name }}
                </span>
              </v-btn>
            </div>
          </div>
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>

<style scoped>
  /* 职业领域筛选区域样式 */
  .category-navigation-card {
    border-left: 4px solid #d0d0d0 !important;
    border-right: 4px solid #d0d0d0 !important;
  }

  /* 分类按钮样式 */
  .category-btn-flat {
    transition: all 0.2s ease-in-out;
    text-transform: none;
    letter-spacing: normal;
    min-height: 48px;
    padding: 8px 16px;
  }

  .category-btn-flat:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .subcategory-btn-flat {
    border: 1px solid #e0e0e0;
    transition: all 0.15s ease-in-out;
    text-transform: none;
    letter-spacing: normal;
    min-height: 36px;
    padding: 6px 12px;
  }

  .subcategory-btn-flat:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .category-buttons-gap {
    gap: 16px;
  }

  .subcategory-buttons-gap {
    gap: 12px;
  }
</style>
