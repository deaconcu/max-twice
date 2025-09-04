<template>
  <div class="category-selector">
    <!-- 主分类选择 -->
    <v-select
      v-model="selectedMainCategory"
      :items="formMainCategories"
      item-title="title"
      item-value="value"
      :label="t('categorySelector.mainCategory')"
      variant="outlined"
      rounded="lg"
      bg-color="grey-lighten-5"
      class="mb-4"
      @update:model-value="onMainCategoryChange"
    >
      <template #prepend-item>
        <v-list-item>
          <template #prepend>
            <v-icon icon="mdi-folder-outline" class="mr-3"></v-icon>
          </template>
          <v-list-item-title class="font-weight-bold">{{
            t('categorySelector.selectMainCategory')
          }}</v-list-item-title>
        </v-list-item>
        <v-divider></v-divider>
      </template>
    </v-select>

    <!-- 子分类选择 -->
    <v-select
      v-model="selectedSubCategory"
      :items="availableSubCategories"
      item-title="name"
      item-value="id"
      :label="t('categorySelector.subCategory')"
      variant="outlined"
      rounded="lg"
      bg-color="grey-lighten-5"
      :disabled="!selectedMainCategory || selectedMainCategory === 'all'"
      @update:model-value="onSubCategoryChange"
    >
      <template #prepend-item>
        <v-list-item>
          <template #prepend>
            <v-icon icon="mdi-folder-multiple-outline" class="mr-3"></v-icon>
          </template>
          <v-list-item-title class="font-weight-bold">{{
            t('categorySelector.selectSubCategory')
          }}</v-list-item-title>
        </v-list-item>
        <v-divider></v-divider>
      </template>
    </v-select>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue'
  import { systemServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  interface MainCategory {
    title: string
    value: string | number
  }

  interface SubCategory {
    id: string | number
    name: string
  }

  interface CategoryMapping {
    mainCategoryId: string | number
    subcategories: SubCategory[]
  }

  interface Props {
    modelMainCategory?: number | string | null
    modelSubCategory?: number | string | null
  }

  // Props
  const props = withDefaults(defineProps<Props>(), {
    modelMainCategory: null,
    modelSubCategory: null,
  })

  interface Emits {
    (e: 'update:modelMainCategory', value: number | string | null): void
    (e: 'update:modelSubCategory', value: number | string | null): void
  }

  // Emits
  const emit = defineEmits<Emits>()

  // 本地状态 - 使用 computed 避免解构 props
  const selectedMainCategory = computed({
    get: () => props.modelMainCategory,
    set: (value: number | string | null) => {
      emit('update:modelMainCategory', value)
    },
  })

  const selectedSubCategory = computed({
    get: () => props.modelSubCategory,
    set: (value: number | string | null) => {
      emit('update:modelSubCategory', value)
    },
  })

  // 动态数据
  const mainCategories = ref<MainCategory[]>([])
  const categoryMapping = ref<CategoryMapping[]>([])
  const loading = ref<boolean>(true)

  // 使用动态的主分类选项（过滤掉'all'选项，因为CategorySelector主要用于表单）
  const formMainCategories = computed(() => {
    return mainCategories.value.filter((cat) => cat.value !== 'all')
  })

  // 计算当前可用的子分类
  const availableSubCategories = computed(() => {
    if (!selectedMainCategory.value) return []

    const mapping = categoryMapping.value.find(
      (item) => item.mainCategoryId === selectedMainCategory.value
    )

    return mapping?.subcategories || []
  })

  // 加载职业类别数据
  const loadProfessionCategories = async (): Promise<void> => {
    try {
      loading.value = true
      const response = await systemServiceV1.getProfessionCategories()
      console.log('Loaded profession categories:', response.data)

      if (response.data) {
        mainCategories.value = response.data.mainCategories || []
        categoryMapping.value = response.data.categoryMapping || []
      }
    } catch (error) {
      console.error('Failed to load profession categories:', error)
      // 如果API调用失败，可以提供默认值或错误处理
    } finally {
      loading.value = false
    }
  }

  // 监听主分类变化
  const onMainCategoryChange = (value: number | string | null): void => {
    selectedMainCategory.value = value
    // 重置子分类
    selectedSubCategory.value = null

    emit('update:modelMainCategory', value)
    emit('update:modelSubCategory', null)
  }

  // 监听子分类变化
  const onSubCategoryChange = (value: number | string | null): void => {
    selectedSubCategory.value = value
    emit('update:modelSubCategory', value)
  }

  // 监听外部props变化
  watch(
    () => props.modelMainCategory,
    (newValue: number | string | null) => {
      selectedMainCategory.value = newValue
    }
  )

  watch(
    () => props.modelSubCategory,
    (newValue: number | string | null) => {
      selectedSubCategory.value = newValue
    }
  )

  // 组件挂载时加载数据
  onMounted(() => {
    loadProfessionCategories()
  })
</script>

<style scoped>
  .category-selector {
    width: 100%;
  }
</style>