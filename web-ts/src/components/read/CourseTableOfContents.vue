<script setup lang="ts">
import { computed, ref } from 'vue'
import TreeNode from './TreeNode.vue'
import ConfigContents from '../course/ConfigContents.vue'
import type { Course } from '@/types/course'

interface TocItem {
  [key: string]: any
}

interface NodeInfo {
  [key: string]: any
}

interface CourseData {
  toc: TocItem[]
  tocNodeInfos: Record<string, NodeInfo>
  course: Course
  path: string
}

interface Props {
  data: CourseData
  currContentsIndex?: number
  openContentsList?: boolean
  configContents?: boolean
  isLearning?: boolean
}

interface Emits {
  (e: 'update:currContentsIndex', value: number): void
  (e: 'update:openContentsList', value: boolean): void
  (e: 'update:configContents', value: boolean): void
  (e: 'loadData', event: any): void
}

interface TreeNodeRef {
  getNextNode: (path: string) => any
}

const props = withDefaults(defineProps<Props>(), {
  currContentsIndex: 0,
  openContentsList: true,
  configContents: false,
  isLearning: false,
})

const emit = defineEmits<Emits>()

// TreeNode refs
const treeNodeRefs = ref<TreeNodeRef[]>([])

// 本地状态管理
const localCurrContentsIndex = computed({
  get: (): number => props.currContentsIndex,
  set: (value: number): void => emit('update:currContentsIndex', value),
})

const localOpenContentsList = computed({
  get: (): boolean => props.openContentsList,
  set: (value: boolean): void => emit('update:openContentsList', value),
})

const localConfigContents = computed({
  get: (): boolean => props.configContents,
  set: (value: boolean): void => emit('update:configContents', value),
})

// 获取下一个节点信息的方法
const getNextNodeInfo = (): any | null => {
  try {
    if (treeNodeRefs.value && treeNodeRefs.value[localCurrContentsIndex.value]) {
      const treeNodeRef = treeNodeRefs.value[localCurrContentsIndex.value]
      if (treeNodeRef && treeNodeRef.getNextNode) {
        return treeNodeRef.getNextNode(props.data.path)
      }
    }
    return null
  } catch (error) {
    console.error('Error getting next node info:', error)
    return null
  }
}

// 暴露方法给父组件
defineExpose({
  getNextNodeInfo,
})
</script>

<template>
  <div class="sticky-left hidden-scrollbar">
    <div class="toc-refined">
      <div class="toc-header-refined border-b py-2">
        <div class="d-flex align-center">
          <v-icon icon="mdi-view-list" size="14" color="grey" class="mr-2"></v-icon>
          <span class="toc-title-text mr-2">课程目录</span>
        </div>

        <div class="toc-actions-refined">
          <v-btn
            icon="mdi-chevron-down"
            variant="text"
            size="small"
            density="compact"
            color="grey-darken-1"
            :class="{ 'rotate-180': localOpenContentsList }"
            class="action-btn-refined"
            @click="localOpenContentsList = !localOpenContentsList"
          >
          </v-btn>
          <v-btn
            icon="mdi-cog-outline"
            variant="text"
            size="small"
            density="compact"
            color="grey-darken-2"
            class="action-btn-refined"
            @click="localConfigContents = true"
          >
          </v-btn>
        </div>
      </div>

      <v-expand-transition>
        <div v-if="localOpenContentsList" class="toc-chips-refined">
          <div
            v-for="(item, index) in data.toc"
            :key="index"
            class="chip-refined"
            :class="{
              'chip-active': localCurrContentsIndex === index,
              'chip-primary': index === 0,
            }"
            @click="localCurrContentsIndex = index"
          >
            <div class="chip-inner">
              <span class="chip-number">{{ index + 1 }}</span>
            </div>

            <!-- 特色图标设计 -->
            <div v-if="index === 0" class="corner-badge">
              <v-icon icon="mdi-chart-line-variant" size="8" color="white"></v-icon>
            </div>

            <v-tooltip v-if="index === 0" activator="parent" location="top">
              <span>主目录 - 用于计算课程完成进度</span>
            </v-tooltip>
          </div>
        </div>
      </v-expand-transition>

      <ConfigContents
        v-model="localConfigContents"
        :contents="data.toc"
        :course-id="data.course.id"
        @load-data="emit('loadData', $event)"
      />
    </div>

    <!-- TreeNode 列表 -->
    <v-tabs-window v-model="localCurrContentsIndex" class="pt-4">
      <v-tabs-window-item v-for="(item, index) in data.toc" :key="index" :value="index">
        <TreeNode
          ref="treeNodeRefs"
          :node-data="data.toc[index]"
          :node-infos="data.tocNodeInfos"
          :course-id="data.course.id"
          :path="data.path"
          :curr-path="String(index + 1)"
          :depth="1"
          :is-learning="isLearning"
        />
      </v-tabs-window-item>
    </v-tabs-window>
  </div>
</template>

<style scoped>
/* 精致目录设计 */
.toc-refined {
  background: linear-gradient(145deg, #ffffff, #f8f9fa);
  border: 1px solid #e3e6eb;
  border-radius: 14px;
  overflow: hidden;
  position: relative;
}

.toc-refined::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(79, 172, 254, 0.3), transparent);
}

/* 精致标题栏 */
.toc-header-refined {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.toc-title-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toc-title-text {
  font-size: 13px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.3px;
}

.current-index-badge {
  background: linear-gradient(135deg, #1976d2, #42a5f5);
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  line-height: 1.2;
  box-shadow: 0 1px 3px rgba(25, 118, 210, 0.3);
}

.toc-actions-refined {
  display: flex;
  align-items: center;
  gap: 2px;
}

.action-btn-refined {
  border-radius: 8px !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

.action-btn-refined:hover {
  background: rgba(25, 118, 210, 0.08) !important;
  transform: scale(1.05);
}

.rotate-180 {
  transform: rotate(180deg);
}

/* 精致chips区域 */
.toc-chips-refined {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 14px 16px 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  overflow: visible; /* 确保角标不被裁剪 */
}

.chip-refined {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  background: #efefef;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  overflow: visible; /* 让角标显示出来 */
}

.chip-refined::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.1));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.chip-refined:hover::before {
  opacity: 1;
}

.chip-refined:hover {
  transform: translateY(-2px) scale(1.05);
  border-color: #1976d2;
}

.chip-active {
  background: #4c83cc;
  border-color: #1976d2 !important;
  transform: translateY(-1px) scale(1.02);
}

.chip-primary {
  border-color: #4caf50;
}

.chip-primary:hover {
  border-color: #4caf50;
}

.chip-primary.chip-active {
  background: #4c83cc;
  color: white;
}

.chip-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.chip-number {
  font-size: 13px;
  font-weight: 700;
  color: #37474f;
  line-height: 1;
}

.chip-active .chip-number {
  color: white;
}

.chip-primary .chip-number {
  color: white;
}

/* 特色角标设计 */
.corner-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: red;
  border: 2px solid white;
  box-shadow: 0 2px 6px rgba(76, 175, 80, 0.4);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.corner-badge .v-icon {
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.2));
}

/* 保持原有样式 */
.sticky-left {
  position: sticky;
  top: 65px;
  z-index: 10;
  height: 100vh;
  overflow-y: auto;
}

/* 左侧目录样式 */
.sticky-left .v-chip {
  transition: all 0.2s ease;
}

.sticky-left .v-chip:hover {
  background-color: rgba(25, 118, 210, 0.1) !important;
  color: #1976d2 !important;
}

.sticky-left .v-chip.text-primary {
  background-color: rgba(25, 118, 210, 0.15) !important;
  border-color: #1976d2 !important;
}

.hidden-scrollbar {
  scrollbar-width: none;
}

.hidden-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>