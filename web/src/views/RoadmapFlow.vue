<script setup>
import { ref, onMounted, watch, computed, h, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import dagre from 'dagre'
import { roadmapServiceV1, progressServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useRoute, useRouter } from 'vue-router'
import RoadmapCard from '@/components/roadmap/RoadmapCard.vue'
import RoadmapDetail from '@/components/roadmap/RoadmapDetail.vue'
import RoadmapCreate from '@/components/roadmap/RoadmapCreate.vue'
import RoadmapHeader from '@/components/roadmap/RoadmapHeader.vue'
import RoadmapList from '@/components/roadmap/RoadmapList.vue'
import RightSidebar from '@/components/common/RightSidebar.vue';

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');

// 状态管理
const roadmaps = ref([])
const loading = ref(true)
const error = ref(null)
const showModal = ref(false)
const selectedRoadmap = ref(null)
const layoutDirection = ref('BT')
const showCreateModal = ref(false)
const copiedRoadmapData = ref(null)

// 路由参数
const route = useRoute()
const router = useRouter()
const professionId = ref(route.params.professionId || '1')

// 职业名称常量 - 根节点显示用
const PROFESSION_NAME = 'JAVA初级程序员'

// 自动布局函数
const applyAutoLayout = (nodeList, edgeList, direction = 'BT') => {
  console.log("Applying auto layout with direction:", direction);
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: direction,
    nodesep: 180,
    ranksep: 80,
    marginx: 20,
    marginy: 20
  })

  const nodeWidth = 100
  const nodeHeight = 36

  // 添加节点和边到 dagre 图
  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  return nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id)
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2
      }
    }
  })
}

// 解析 content 字段
function parseContent(content) {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    const nodes = (data.nodes || []).map(node => {
      // 如果是根节点（id=0），设置为职业名称且只能入不能出
      if (node.id === 0 || node.id === '0') {
        return {
          id: node.id,
          type: 'default',
          data: {
            label: PROFESSION_NAME, // 根节点显示职业名称
            link: null, // 根节点不跳转
            ...node.data
          },
          position: node.position || { x: 0, y: 0 },
          targetPosition: 'bottom'  // 根节点只能入，不能出
        }
      }
      
      return {
        id: node.id,
        type: 'default', // 使用默认节点类型
        data: {
          label: node.name, // 使用 name 字段
          link: '/read?courseId=' + node.id, // 默认链接
          ...node.data
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: 'top',    // source 在上面
        targetPosition: 'bottom'  // target 在下面
      }
    })

    const edges = (data.edges || []).map(edge => ({
      id: `${edge.source}-${edge.target}`,
      source: edge.source,
      target: edge.target,
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label
      // 移除内联样式，让CSS接管样式控制
    }))

    return { nodes, edges }
  } catch (err) {
    console.error('解析课程内容失败:', err)
    return { nodes: [], edges: [] }
  }
}

// 加载课程数据
async function loadRoadmaps() {
  try {
    loading.value = true
    error.value = null
    const response = await roadmapServiceV1.getProfessionRoadmaps(professionId.value)

    console.log("课程数据：response:", response);

    // 确保我们在处理 response.data
    const data = response.data || response
    if (!Array.isArray(data)) {
      throw new Error('返回的数据格式不正确')
    }

    roadmaps.value = data.map(roadmap => {
      const { nodes, edges } = parseContent(roadmap.content)
      // 首先进行布局计算
      const layoutedNodes = applyAutoLayout(nodes, edges, layoutDirection.value)

      console.log("nodes:" + JSON.stringify(layoutedNodes));
      return {
        ...roadmap,
        nodes: layoutedNodes,
        edges: edges,
        votes: roadmap.votes || Math.floor(Math.random() * 100) // 模拟投票数据
      }
    })
  } catch (err) {
    console.error('加载课程数据失败:', err)
    error.value = '加载课程数据失败: ' + (err.message || '未知错误')
  } finally {
    loading.value = false
  }
}

// 弹窗控制
function openModal(roadmap) {
  selectedRoadmap.value = roadmap
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  selectedRoadmap.value = null
}

// 创建新课程表相关
function openCreateModal() {
  showCreateModal.value = true
}

function closeCreateModal() {
  showCreateModal.value = false
  // 清空复制的课程表数据
  copiedRoadmapData.value = null
}

// 计算总学习人数
function getTotalLearners() {
  return roadmaps.value.reduce((total, roadmap) => {
    return total + (roadmap.learners || Math.floor(Math.random() * 1000) + 100);
  }, 0);
}

// 复制课程表到创建页面
function copyRoadmapToCreate(roadmap) {
  // 设置要复制的课程表数据
  copiedRoadmapData.value = roadmap
  
  // 打开创建课程表弹窗
  showCreateModal.value = true
  
  showSnackbar(`正在复制课程表 "${roadmap.description || '未命名'}" 到编辑区域`)
}

// 保存课程表功能
async function saveRoadmap(saveData) {
  if (!saveData || !saveData.description) {
    showSnackbar(t('roadmap.fillDescription'))
    return
  }
  
  // 执行保存
  await performSave(saveData.description, saveData.content)
}

// 执行实际的保存操作
async function performSave(description, content) {
  try {
    console.log('保存课程表，序列化内容:', content)
    console.log('课程表描述:', description)
    
    // 调用接口保存，传入描述信息
    const response = await roadmapServiceV1.createRoadmap(
      professionId.value, 
      content, 
      description.trim()
    )
    
    console.log('保存课程表响应:', response)
    
    // 只有当 response.code === 200 时才算保存成功
    if (response.code === 200) {
      console.log('课程表保存成功:', response.data)
      showSnackbar(t('roadmap.saveSuccess'))
      
      // 重新加载课程表列表
      await loadRoadmaps()
    } else {
      // 如果 code 不是 200，视为保存失败
      const errorMessage = response.msg || t('roadmap.saveFailed')
      console.error('保存课程表失败，服务器响应码:', response.code, '错误信息:', errorMessage)
      showSnackbar(t('roadmap.saveFailed') + '：' + errorMessage)
    }
    
  } catch (error) {
    console.error('保存课程表失败:', error)
    showSnackbar(t('roadmap.saveFailed') + '：' + (error.response?.data?.message || error.message))
  }
}

// 监听路由参数变化
watch(() => route.params.professionId, (newId) => {
  if (newId) {
    professionId.value = newId
    loadRoadmaps()
  }
})

function handleNodeClick({event, node}) {
  // 根节点不跳转
  if (node.id === '0') {
    return
  }
  
  if (node.data.link) {
    window.open(node.data.link, '_blank'); // 打开链接
  }
}

// 初始化
onMounted(() => {
  loadRoadmaps()
})
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="12" lg="9" class="pr-lg-8">
        <!-- 页面头部 -->
        <RoadmapHeader
          profession-name="JAVA初级程序员"
          :roadmaps="roadmaps"
          :total-learners="getTotalLearners()"
          @create-roadmap="openCreateModal"
        />

        <!-- 课程表列表 -->
        <RoadmapList
          :roadmaps="roadmaps"
          :loading="loading"
          :error="error"
          :pinned-roadmaps="roadmaps.filter(r => r.pinned).map(r => r.id)"
          :profession-id="professionId"
          @open-detail="openModal"
          @copy-roadmap="copyRoadmapToCreate"
          @create-roadmap="openCreateModal"
          @roadmaps-updated="() => {}"
        />
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="12" lg="3" class="d-none d-lg-block">
        <RightSidebar /> 
      </v-col>
    </v-row>

    <!-- 弹出层 -->
    <RoadmapDetail
      v-model="showModal"
      :roadmap="selectedRoadmap"
      @close="closeModal"
      @vote="voteRoadmap" 
      @node-click="handleNodeClick"
    />

    <!-- 创建新课程表弹窗 -->
    <RoadmapCreate
      v-model="showCreateModal"
      :copied-roadmap="copiedRoadmapData"
      @close="closeCreateModal"
      @save="saveRoadmap"
    />
  </v-container>
</template>

<style>
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';

/* 连接点样式 - 入口红色，出口绿色 */
.vue-flow__handle.vue-flow__handle-top {
  padding: 3px;
  border: 1px solid #000000 !important; 
  background: #f44336 !important; 
}

.vue-flow__handle.vue-flow__handle-bottom {
  padding: 3px;
  border: 1px solid #000000 !important;
  background: #4caf50 !important; 
}

/* 只读模式下隐藏连接点 */
.vue-flow-readonly .vue-flow__handle {
  width: 0 !important;
  height: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 只读模式下美化默认节点 */
.vue-flow-readonly .vue-flow__node {
  border-radius: 16px !important;
  background: #f5f5f5  !important;
  border: 3px solid #9e9e9e !important;
  color: #424242 !important;
  font-weight: 500 !important;
  font-size: 1.00rem !important;
  transition: all 0.2s ease;
  cursor: pointer !important;
  padding: 8px 8px !important;
  align-items: center;
  justify-content: center;
}

/* 根节点特殊样式 */
.vue-flow-readonly .vue-flow__node[data-id="0"] {
  background: #1976d2  !important;
  border: 4px double #1976d2 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

.vue-flow-readonly .vue-flow__node[data-id="0"]:hover {
  background: #1976d2  !important;
  border: 4px double #1976d2 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

/* 编辑模式下的根节点样式 */
.vue-flow__node[data-id="0"] {
  background: #1976d2  !important;
  border: 4px double #1976d2 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

.vue-flow-readonly .vue-flow__node:hover {
  background: #e3f2fd  !important;
  border-color: #1976d2 !important;
  transform: translateY(-10px);
  color: #0d47a1 !important;
}

.vue-flow__edge-path {
  stroke-width: 2px !important;
}

.vue-flow__edge-path {
  stroke-width: 2px !important;
}

/* 扁平化设计样式 */
.flat-button {
  border-radius: 8px !important;
  text-transform: none !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.flat-button:hover {
  transform: translateY(-1px) !important;
}

.flat-chip {
  border-radius: 6px !important;
  border: 1px solid rgba(25, 118, 210, 0.3) !important;
  transition: all 0.2s ease !important;
}

/* 选中状态的样式 */
.vue-flow__node.selected {
  box-shadow: 0 0 0 2px #1976d2 !important;
}

.vue-flow__edge.selected .vue-flow__edge-path {
  stroke: #1976d2 !important;
  stroke-width: 3px !important;
}

/* 确保节点和边可以被选中 */
.vue-flow__node {
  padding: 3px;
  border-radius: 8px;
  cursor: pointer !important;
}

.vue-flow__edge {
  cursor: pointer !important;
}

/* 全局按钮内容居中 */
:deep(.v-btn__content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 4px !important;
}

/* 保留核心样式 */

</style>
