<template>
  <v-dialog v-model="visible" fullscreen :scrim="true" transition="dialog-bottom-transition">
    <v-card>
      <v-toolbar color="grey-lighten-5" flat class="flat-toolbar" density="compact">
        <v-toolbar-title class="text-grey-darken-4">
          <v-icon class="mr-1" size="small" color="grey-darken-2">mdi-book</v-icon>
          {{ t('roadmapDetail.learningPath') }}
        </v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon variant="text" size="small" @click="$emit('close')" class="flat-icon-button">
          <v-icon color="teal-darken-1">mdi-close</v-icon>
        </v-btn>
      </v-toolbar>

      <v-container v-if="roadmap" fluid class="pa-0" style="height: calc(100vh - 64px); overflow: hidden;">
        <v-row no-gutters style="height: 100%;">
          <!-- 左侧课程表区域 -->
          <v-col cols="8" class="roadmap-detail-content" style="height: 100%;">
            <v-card flat style="height: 100%; display: flex; flex-direction: column;">
              <v-card-text class="vue-flow-container pa-2 roadmap-detail-content flex-grow-1" style="overflow: hidden; position: relative;">
                <VueFlow 
                  :nodes="roadmap.nodes" 
                  :edges="roadmap.edges" 
                  fit-view-on-init
                  :min-zoom="0.9" 
                  :max-zoom="1.0" 
                  :snap-to-grid="true" 
                  :snap-grid="[20, 20]" 
                  :nodes-draggable="false"
                  :nodes-connectable="false" 
                  :elements-selectable="false" 
                  @node-click="$emit('node-click', $event)"
                  class="vue-flow-readonly">
                  <Background pattern-color="#aaa" :gap="20" />
                  <Controls />
                </VueFlow>
                
                <!-- 浮层点赞按钮 -->
                <div class="floating-upvote-container">
                  <v-btn 
                    variant="outlined" 
                    :color="roadmap.upvoted ? 'red-darken-1' : 'grey-darken-2'"
                    @click="handleVote"
                  >
                    <v-icon 
                      size="18" 
                      :class="{ 'vote-animation': roadmap.upvoted }" 
                      :color="roadmap.upvoted ? 'red-darken-1' : 'grey-darken-2'"
                      >
                      {{ roadmap.upvoted ? 'mdi-thumb-up' : 'mdi-thumb-up-outline' }}
                    </v-icon>
                    <span class="ml-1 text-body-2">{{ roadmap.vote || 0 }}</span>
                    <v-tooltip activator="parent" location="bottom">
                      {{ roadmap.upvoted ? t('roadmapDetail.upvoted') : t('roadmapDetail.upvote') }}
                    </v-tooltip>
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 右侧评论区域 -->
          <v-col cols="4" style="height: 100%;">
            <v-card flat class="right-section">
              <!-- 课程说明和创建者信息 -->
              <div class="flex-shrink-0 px-4 pt-4">
                
                <!-- 创建者信息卡片 -->
                <v-card v-if="roadmap.creator" flat class="mb-3">
                  <v-card-text class="py-3 px-0">
                    <div class="d-flex align-center">
                      <v-avatar size="40" class="mr-3 border">
                        <v-img v-if="roadmap.creator.avatar" :src="roadmap.creator.avatar"></v-img>
                        <v-icon v-else color="teal-darken-1">mdi-account</v-icon>
                      </v-avatar>
                      <div class="flex-grow-1">
                        <div class="text-subtitle-2 text-teal-darken-2 font-weight-medium">
                          {{ roadmap.creator.name }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          创建于 {{ formatDate(roadmap.createdAt) }}
                        </div>
                      </div>
                    </div>
                  </v-card-text>
                </v-card>
                <!-- 课程描述 -->
                <div v-if="roadmap.description" class="pa-0 pb-3">
                  <div 
                    class="text-body-2 description-text"
                    :class="{ 'description-collapsed': !isDescriptionExpanded && shouldShowToggle }"
                  >
                    {{ roadmap.description }}
                  </div>
                  <v-btn 
                    v-if="shouldShowToggle"
                    variant="text" 
                    size="small" 
                    color="teal-darken-1"
                    class="pa-0 mt-1"
                    @click="toggleDescription"
                  >
                    {{ isDescriptionExpanded ? t('roadmapDetail.collapse') : t('roadmapDetail.viewMore') }}
                    <v-icon size="small" class="ml-1">
                      {{ isDescriptionExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down' }}
                    </v-icon>
                  </v-btn>
                </div>
              </div>

              <!-- 评论区域预留空间 -->
              <v-card-text class="px-4 pt-6" style="overflow: hidden;">
                <Comment :object="roadmap" :type="3"></Comment>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import Comment from '../components/Comment.vue';

const { t } = useI18n()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  roadmap: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'close', 'node-click', 'vote'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 描述展开/收起状态
const isDescriptionExpanded = ref(false)
const shouldShowToggle = ref(false)

// 点赞功能 - 直接调用父组件的 voteRoadmap 方法
function handleVote(event) {
  console.log('Handling vote for roadmap:', props.roadmap);
  emit('vote', props.roadmap, event)
}

// 检查描述是否超过100px
const checkDescriptionHeight = async () => {
  if (!props.roadmap?.description) return
  
  await nextTick()
  const element = document.querySelector('.description-text')
  if (element) {
    const maxHeight = 70 // 100px的高度限制
    shouldShowToggle.value = element.scrollHeight > maxHeight
  }
}

// 切换描述展开状态
const toggleDescription = () => {
  isDescriptionExpanded.value = !isDescriptionExpanded.value
}

// 监听roadmap变化，重新检查高度
watch(() => props.roadmap?.description, () => {
  isDescriptionExpanded.value = false // 重置展开状态
  checkDescriptionHeight()
}, { immediate: true })

// 监听dialog显示状态，确保在显示时检查高度
watch(visible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      checkDescriptionHeight()
    })
  }
})

// 格式化日期
function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.flat-toolbar {
  border-radius: 0 !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08) !important;
  border-bottom: 1px solid rgba(178, 223, 219, 0.3);
  background: linear-gradient(135deg, #ffffff 0%, #f8fdfd 100%) !important;
}


.flat-chip {
  border-radius: 6px !important;
  box-shadow: none !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  background-color: rgba(77, 182, 172, 0.1) !important;
}

.vue-flow-container {
  border-radius: 12px;
  overflow: hidden;
  border: none;
  box-shadow: none;
}

.vue-flow-readonly {
  background: white !important;
}

/* 整体配色调和 - teal主题 */
:deep(.v-card-title) {
  color: #004d40 !important;
  font-weight: 500 !important;
}

/* 详情页右侧评论区样式 */
.right-section {
  overflow-y: auto; 
  height: 100%;
  padding-left: 8px;
}

/* 详情页左侧课程表区域样式 */
.roadmap-detail-content {
  background: white !important;
  border-right: 1px solid rgba(178, 223, 219, 0.2) !important;
}

/* 只读模式下隐藏连接点 */
.vue-flow-readonly :deep(.vue-flow__handle) {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 只读模式下美化默认节点 */
.vue-flow-readonly :deep(.vue-flow__node) {
  border-radius: 16px !important;
  box-shadow: 0 4px 16px rgba(0, 150, 136, 0.10), 0 1.5px 6px rgba(0,0,0,0.06) !important;
  background: #fafafa  !important;
  border: 4px double #b2dfdb !important;
  color: #68976b !important;
  font-weight: 500 !important;
  font-size: 1.00rem !important;
  transition: all 0.2s ease;
  cursor: pointer !important;
  padding: 8px 8px !important;
  align-items: center;
  justify-content: center;
}

/* 根节点特殊样式 */
.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]) {
  background: #4f87a0  !important;
  border: 4px double #cae0e9 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

.vue-flow-readonly :deep(.vue-flow__node[data-id="0"]:hover) {
  background: #4f87a0  !important;
  border: 4px double #cae0e9 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

.vue-flow-readonly :deep(.vue-flow__node:hover) {
  box-shadow: 0 8px 24px rgba(0, 150, 136, 0.18), 0 2px 8px rgba(0,0,0,0.10) !important;
  background: #e0f2f1  !important;
  border-color: #b2dfdb !important;
  transform: translateY(-10px);
  color: #004d40 !important;
}

/* Vue Flow 边样式 */
.vue-flow-readonly :deep(.vue-flow__edge-path) {
  stroke-width: 2px !important;
}

/* 浮层点赞按钮样式 - Flat 风格 */
.floating-upvote-container {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1000;
}

.floating-upvote-btn {
  border-radius: 8px !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08) !important;
  text-transform: none !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  min-width: 60px !important;
  height: 32px !important;
  backdrop-filter: blur(4px);
  background: rgba(255, 255, 255, 0.95) !important;
}

.floating-upvote-btn:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  transform: translateY(-1px) !important;
  background: rgba(255, 255, 255, 1) !important;
  color: #004d40 !important; /* 悬停时的文字颜色 */
}

.floating-upvote-btn :deep(.v-btn__content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 4px !important;
  color: inherit !important; /* 继承父元素颜色 */
}

/* 确保图标颜色可见 */
.floating-upvote-btn :deep(.v-icon) {
  color: #00695c !important;
}

.floating-upvote-btn:hover :deep(.v-icon) {
  color: #004d40 !important;
}

/* 确保文字颜色可见 */
.floating-upvote-btn span {
  color: #00695c !important;
}

.floating-upvote-btn:hover span {
  color: #004d40 !important;
}

/* 点赞动画效果 */
.vote-animation {
  animation: vote-bounce 0.6s ease-in-out;
}

@keyframes vote-bounce {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

/* 描述文本样式 */
.description-text {
  line-height: 1.5;
  transition: max-height 0.3s ease;
}

.description-collapsed {
  max-height: 70px; /* 固定100px高度 */
  overflow: hidden;
  position: relative;
}

.description-collapsed::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20px; /* 渐变遮罩高度 */
  background: linear-gradient(transparent, white);
  pointer-events: none;
}

/* 已完成课程样式 */
.vue-flow-readonly :deep(.vue-flow__node.completed-course) {
  background: #e8f5e9 !important;
  border-color: #4caf50 !important;
  color: #2e7d32 !important;
}

/* 已完成课程标识 */
.vue-flow-readonly :deep(.vue-flow__node.completed-course::after) {
  content: '✓';
  position: absolute;
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  background: #4caf50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: bold;
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

/* 有进度课程的背景进度填充 */
.vue-flow-readonly :deep(.vue-flow__node.progress-course) {
  background: linear-gradient(to right, #beffb4 var(--progress, 0%), #fafafa var(--progress, 0%)) !important;
  color: #333 !important;
}
</style>
