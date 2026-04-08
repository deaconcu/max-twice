<template>
  <div class="toc-sidebar">
    <div class="toc-sticky-wrapper">
      <!-- 目录组选择卡片 -->
      <div v-if="toc && toc.length > 0" class="toc-groups-card">
        <div class="toc-chips">
          <v-chip
            size="default"
            rounded="lg"
            label
            variant="tonal"
            color=""
            class="me-0 px-3 text-body-2 text-md-body-1 text-medium-emphasis"
            style="font-weight: 600"
          >
            {{ t('toc.title') }}
          </v-chip>
          <div v-for="(item, index) in toc" :key="index" class="position-relative d-inline-block">
            <v-chip
              label
              rounded="lg"
              size="default"
              variant="flat"
              :color="currContentsIndex === index ? 'grey' : 'surface-variant'"
              class="text-body-2 text-md-body-1 font-weight-bold"
              @click="currContentsIndex = index"
            >
              {{ index + 1 }}
            </v-chip>
            <div v-if="index === 0" class="corner-badge">
              <v-icon
                icon="mdi-chart-line-variant"
                :size="$vuetify.display.mobile ? 6 : 8"
                color="white"
              />
            </div>
          </div>
          <v-btn
            v-if="showConfig"
            icon
            :size="$vuetify.display.mobile ? 'x-small' : 'small'"
            variant="text"
            class="config-btn ms-auto"
            @click="$emit('config')"
          >
            <v-icon :size="$vuetify.display.mobile ? 16 : 20">mdi-cog-outline</v-icon>
          </v-btn>
        </div>
      </div>

      <!-- 目录树 -->
      <div class="toc-card">
        <div
          class="toc-tree"
          :class="{ 'toc-tree-hover': isTocHovering }"
          @mouseenter="isTocHovering = true"
          @mouseleave="isTocHovering = false"
        >
          <TreeNode
            v-if="toc && toc[currContentsIndex]"
            :node-data="toc[currContentsIndex]"
            :node-infos="tocNodeInfos"
            :node-id="nodeId"
            :path="path"
            :curr-path="String(currContentsIndex + 1)"
            :depth="1"
            :is-learning="isLearning"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TreeNode from '@/components/common/TreeNode.vue'
import { useI18n } from '@/composables/useI18n'

withDefaults(defineProps<Props>(), {
  toc: () => [],
  tocNodeInfos: () => ({}),
  nodeId: undefined,
  path: null,
  isLearning: false,
  showConfig: false,
})

defineEmits<{
  config: []
}>()

const { t } = useI18n()

interface Props {
  toc?: any[]
  tocNodeInfos?: Record<number, any>
  nodeId?: number
  path?: string | null
  isLearning?: boolean
  showConfig?: boolean
}

const currContentsIndex = ref(0)
const isTocHovering = ref(false)
</script>

<style scoped>
/* 左侧 TOC 目录栏 */
.toc-sidebar {
  flex: 0 1 360px;
  max-width: 360px;
  padding: 6px 0px 24px 0;
  position: relative;
  margin-right: 20px;
  min-height: calc(100vh - 125px);
}

.toc-sidebar::after {
  content: '';
  position: absolute;
  top: 6px;
  right: 0;
  bottom: 24px;
  width: 1px;
  background-color: rgb(var(--v-theme-border));
}

.toc-sticky-wrapper {
  position: sticky;
  top: 65px;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.toc-card {
  background-color: white;
  padding: 10px 0;
  border-radius: 16px;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toc-tree {
  margin-top: 6px;
  padding-right: 42px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.toc-tree::-webkit-scrollbar {
  width: 2px;
}

.toc-tree::-webkit-scrollbar-track {
  background: transparent;
}

.toc-tree::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 2px;
}

.toc-tree-hover::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
}

.toc-tree-hover::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

/* 目录组选择卡片 */
.toc-groups-card {
  padding: 0;
}

.toc-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding-right: 47px;
}

.corner-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background-color: rgb(var(--v-theme-primary));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  z-index: 1;
}

.config-btn {
  flex-shrink: 0;
}

/* 小屏幕隐藏目录 */
@media (max-width: 1280px) {
  .toc-sidebar {
    display: none;
  }
}
</style>
