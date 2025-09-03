<script setup>
  import { computed } from 'vue'

  const props = defineProps({
    items: {
      type: Array,
      required: true,
    },
    modelValue: {
      type: String,
      required: true,
    },
    direction: {
      type: String,
      default: 'vertical', // vertical, horizontal
      validator: (value) => ['vertical', 'horizontal'].includes(value),
    },
    sticky: {
      type: Boolean,
      default: true,
    },
  })

  const emit = defineEmits(['update:modelValue', 'tab-change'])

  const selectedTab = computed({
    get: () => props.modelValue,
    set: (value) => {
      emit('update:modelValue', value)
      emit('tab-change', value)
    },
  })

  const containerClasses = computed(() => {
    const classes = ['tab-navigation-container']
    if (props.sticky) classes.push('sticky-nav')
    return classes
  })

  // 将 items 分组（支持分组显示）
  const groupedItems = computed(() => {
    const groups = []
    let currentGroup = { items: [] }

    props.items.forEach((item) => {
      if (item.group) {
        // 如果当前组有内容，先推入 groups
        if (currentGroup.items.length > 0) {
          groups.push(currentGroup)
        }
        // 开始新组
        currentGroup = {
          title: item.group,
          items: [item],
        }
      } else {
        currentGroup.items.push(item)
      }
    })

    // 推入最后一组
    if (currentGroup.items.length > 0) {
      groups.push(currentGroup)
    }

    return groups
  })
</script>

<template>
  <div :class="containerClasses">
    <!-- 导航卡片 -->
    <v-card rounded="xl" elevation="0" color="grey-lighten-5" class="navigation-card">
      <v-card-text class="pa-5">
        <template v-for="(group, groupIndex) in groupedItems" :key="groupIndex">
          <!-- 分组标题 -->
          <div v-if="group.title && direction === 'vertical'" class="group-header px-3 py-2">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">
              {{ group.title }}
            </div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <!-- 导航项目 -->
          <div v-for="item in group.items" :key="item.value" class="nav-item-wrapper pa-1">
            <div
              class="nav-item"
              :class="[{ 'nav-item-active': selectedTab === item.value }]"
              @click="selectedTab = item.value"
            >
              <div class="nav-content">
                <!-- 图标 -->
                <v-icon :icon="item.icon" size="18" class="nav-icon mr-3"></v-icon>

                <!-- 文字 -->
                <span class="nav-text">{{ item.text }}</span>

                <!-- 徽章 -->
                <v-chip
                  v-if="item.badge"
                  :color="item.badgeColor || 'error'"
                  size="x-small"
                  class="ml-auto"
                  variant="flat"
                >
                  {{ item.badge }}
                </v-chip>
              </div>
            </div>
          </div>
        </template>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
  .tab-navigation-container {
    background: transparent;
    width: 100%;
  }

  .sticky-nav {
    position: sticky;
    top: 30px;
  }

  .nav-header {
    padding-left: 8px;
  }

  /* 导航卡片样式 */
  .navigation-card {
    border: 1px solid rgba(0, 0, 0, 0.08);
    background: white !important;
    width: 100% !important;
  }

  /* 导航项样式 */
  .nav-item {
    cursor: pointer;
    border-radius: 10px;
    transition: all 0.2s ease;
    padding: 12px 16px;
    margin: 2px 0;
    background: transparent;
    border: 1px solid transparent;
  }

  .nav-item:hover {
    background: rgba(25, 118, 210, 0.05);
    border-color: rgba(25, 118, 210, 0.1);
  }

  .nav-item-active {
    background: linear-gradient(135deg, rgba(25, 118, 210, 0.1) 0%, rgba(25, 118, 210, 0.05) 100%);
    border-color: rgba(25, 118, 210, 0.2);
  }

  .nav-content {
    display: flex;
    align-items: center;
    width: 100%;
  }

  .nav-icon {
    color: rgba(0, 0, 0, 0.6);
    transition: color 0.2s ease;
  }

  .nav-item-active .nav-icon {
    color: #1976d2;
  }

  .nav-text {
    font-size: 14px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    flex: 1;
    transition: color 0.2s ease;
  }

  .nav-item-active .nav-text {
    color: #1976d2;
    font-weight: 600;
  }

  .group-header {
    margin-top: 12px;
  }

  .group-header:first-child {
    margin-top: 4px;
  }

  /* 徽章优化 */
  .nav-item :deep(.v-chip) {
    font-size: 10px;
    height: 18px;
    font-weight: 600;
  }
</style>
