<script setup>
  import { computed } from 'vue'
  import CourseItemsList from './CourseItemsList.vue'

  // Props
  const props = defineProps({
    category: {
      type: Object,
      required: true,
    },
    categoryIndex: {
      type: Number,
      required: true,
    },
    activeFirstLvl: {
      type: Number,
      default: -1,
    },
    selectedSubCategory: {
      type: Number,
      default: -1,
    },
    courses: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
  })

  // Emits
  const emit = defineEmits(['toggleFirstLevel', 'selectSubCategory', 'openCourse'])

  // 计算属性
  const isExpanded = computed(() => props.activeFirstLvl === props.categoryIndex)
  const hasSelectedSubCategory = computed(() => props.selectedSubCategory !== -1)

  // 处理一级分类切换
  const handleToggleFirstLevel = () => {
    emit('toggleFirstLevel', props.categoryIndex)
  }

  // 处理二级分类选择
  const handleSelectSubCategory = (subIndex) => {
    emit('selectSubCategory', props.categoryIndex, subIndex)
  }

  // 处理课程打开
  const handleOpenCourse = (courseId) => {
    emit('openCourse', courseId)
  }
</script>

<template>
  <div class="mb-5">
    <v-card flat color="grey-lighten-5" rounded="xl" class="pa-0">
      <v-card-text class="pa-4">
        <!-- 分类标题行 -->
        <div class="d-flex start align-baseline mb-2">
          <v-btn
            variant="text"
            :ripple="false"
            class="ma-0 pa-1 pb-1 me-3 text-h5 font-weight-bold min-width-10"
            :color="isExpanded ? 'primary' : 'grey-darken-2'"
            @click="handleToggleFirstLevel"
          >
            <v-icon
              :icon="isExpanded ? 'mdi-chevron-down' : 'mdi-chevron-right'"
              size="18"
              class="mr-1"
            >
            </v-icon>
            <span class="font-weight-regular">{{ category.name }}</span>
          </v-btn>

          <!-- 统计信息 -->
          <div class="text-grey-darken-3 text-body-2 d-flex align-center ml-2 mb-3">
            <v-icon
              icon="mdi-book-multiple-outline"
              class="mr-1"
              size="16"
              color="primary"
            ></v-icon>
            <span class="mr-4">515</span>
            <v-icon icon="mdi-list-box-outline" class="mr-1" size="16" color="success"></v-icon>
            <span class="mr-4">23,423</span>
            <v-icon icon="mdi-chart-donut" class="mr-1" size="16" color="warning"></v-icon>
            <span>60%</span>
          </div>
        </div>

        <!-- 展开内容 -->
        <v-expand-transition>
          <div v-if="isExpanded" class="mt-3">
            <!-- 二级分类按钮组 -->
            <v-item-group class="mb-3">
              <v-item v-for="(subCategory, subIndex) in category.list" :key="subIndex">
                <v-btn
                  class="ma-1 mt-2 mb-1 border text-body-1"
                  variant="flat"
                  rounded="lg"
                  :ripple="false"
                  :color="subIndex === selectedSubCategory ? 'primary' : 'white'"
                  density="default"
                  @click="handleSelectSubCategory(subIndex)"
                >
                  {{ subCategory.name }}
                  <v-chip
                    v-if="subCategory.list"
                    variant="flat"
                    :color="subIndex === selectedSubCategory ? 'white' : 'grey-lighten-3'"
                    size="x-small"
                    class="ml-1"
                  >
                    {{ subCategory.list.length }}
                  </v-chip>
                </v-btn>
              </v-item>
            </v-item-group>

            <!-- 课程列表 -->
            <v-expand-transition>
              <v-tabs-window
                v-if="hasSelectedSubCategory"
                :model-value="selectedSubCategory"
                class="mt-3"
              >
                <v-tabs-window-item
                  v-for="(subCategory, subIndex) in category.list"
                  :key="subIndex"
                >
                  <CourseItemsList
                    :courses="courses"
                    :loading="loading"
                    @open-course="handleOpenCourse"
                  />
                </v-tabs-window-item>
              </v-tabs-window>
            </v-expand-transition>
          </div>
        </v-expand-transition>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
  /* 分类卡片样式 */
  .v-card {
    transition: all 0.2s ease;
  }

  .v-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  /* 按钮样式 */
  .v-btn {
    text-transform: none !important;
    font-weight: 500 !important;
  }

  .v-btn:hover {
    transform: translateY(-1px);
    transition: transform 0.2s ease;
  }

  /* 统计信息图标对齐 */
  .d-flex.align-center .v-icon {
    vertical-align: middle;
  }

  /* 二级分类按钮边框 */
  .border {
    border: 1px solid #e0e0e0 !important;
  }

  /* 分隔线样式 */
  .border-t-sm {
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  .min-width-10 {
    min-width: 10px;
  }
</style>
