<script setup lang="ts">
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()
</script>

<template>
  <v-col cols="3" class="sidebar-container">
    <!-- 广告区域 -->
    <v-card variant="flat" class="mb-4 flat-card">
      <v-img src="/images/big.png" height="200" cover class="ad-image">
        <div class="ad-overlay">
          <v-card-title class="text-white text-center">
            {{ t('sidebar.adTitle') }}
          </v-card-title>
          <v-card-subtitle class="text-white text-center">
            {{ t('sidebar.adSubtitle') }}
          </v-card-subtitle>
        </div>
      </v-img>
      <v-card-actions class="justify-center">
        <v-btn color="teal-darken-1" variant="flat" class="flat-button">
          {{ t('sidebar.learnMore') }}
        </v-btn>
      </v-card-actions>
    </v-card>

    <!-- 操作说明区域 -->
    <v-card variant="flat" class="flat-card">
      <v-card-title class="text-teal-darken-3 d-flex align-center">
        <v-icon class="mr-2" color="teal-darken-1">mdi-help-circle</v-icon>
        {{ t('sidebar.operationGuide') }}
      </v-card-title>
      <v-card-text>
        <v-list density="compact" class="operation-guide">
          <v-list-item class="px-0">
            <template #prepend>
              <v-icon size="20" color="teal-darken-1">mdi-mouse</v-icon>
            </template>
            <v-list-item-title class="text-body-2">{{
              t('sidebar.clickCourse')
            }}</v-list-item-title>
          </v-list-item>

          <v-list-item class="px-0">
            <template #prepend>
              <v-icon size="20" color="teal-darken-1">mdi-thumb-up</v-icon>
            </template>
            <v-list-item-title class="text-body-2">{{ t('sidebar.voteCourse') }}</v-list-item-title>
          </v-list-item>

          <v-list-item class="px-0">
            <template #prepend>
              <v-icon size="20" color="orange-darken-1">mdi-arrow-up-thick</v-icon>
            </template>
            <v-list-item-title class="text-body-2">{{ t('sidebar.pinCourse') }}</v-list-item-title>
          </v-list-item>

          <v-list-item class="px-0">
            <template #prepend>
              <v-icon size="20" color="teal-darken-1">mdi-plus-circle</v-icon>
            </template>
            <v-list-item-title class="text-body-2">创建自定义课程表</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-text>
        <v-chip color="blue-lighten-4" variant="flat" size="small" class="mb-2 mr-2">
          <span class="text-blue-darken-2">小提示</span>
        </v-chip>
        <p class="text-caption text-grey-darken-1 mb-0">
          拖拽节点可以重新排列课程顺序，连接节点可以建立学习路径。
        </p>
      </v-card-text>
    </v-card>
  </v-col>
</template>

<style scoped>
  .flat-card {
    border-radius: 12px !important;
    border: 1px solid #b2dfdb !important;
    background: white !important;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
    transition: all 0.3s ease !important;
  }

  .flat-card:hover {
    border-color: #4db6ac !important;
    transform: translateY(-2px) !important;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12) !important;
  }

  .flat-button {
    border-radius: 8px !important;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08) !important;
    text-transform: none !important;
    font-weight: 500 !important;
    transition: all 0.2s ease !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
  }

  .flat-button:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
    transform: translateY(-1px) !important;
  }

  /* 广告区域样式 */
  .ad-image {
    position: relative;
    border-radius: 8px 8px 0 0 !important;
  }

  .ad-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0, 150, 136, 0.8), rgba(0, 77, 64, 0.9));
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border-radius: 8px 8px 0 0;
  }

  /* 操作指南样式 */
  .operation-guide .v-list-item {
    min-height: 40px !important;
    padding: 8px 0 !important;
  }

  .operation-guide .v-list-item-title {
    color: #455a64 !important;
    font-weight: 400 !important;
  }

  /* 整体配色调和 - teal主题 */
  :deep(.v-card-title) {
    color: #004d40 !important;
    font-weight: 500 !important;
  }

  :deep(.v-card-subtitle) {
    color: #00695c !important;
  }

  /* 右侧帮助区域整体容器样式 */
  .sidebar-container {
    padding-left: 16px;
  }

  /* 广告图片容器优化 */
  :deep(.v-img) {
    border-radius: 8px 8px 0 0 !important;
  }
</style>