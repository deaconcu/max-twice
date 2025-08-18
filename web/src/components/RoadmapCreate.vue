<script setup>
import { ref, computed, inject, watch } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { learnService } from '@/services/learnService'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import dagre from 'dagre'

const showSnackbar = inject('showSnackbar');

// 职业名称常量 - 根节点显示用
const PROFESSION_NAME = 'JAVA初级程序员'

// 创建根节点的通用函数
function createRootNode(position = { x: 400, y: 100 }) {
  return {
    id: '0',
    type: 'default',
    data: { 
      label: PROFESSION_NAME,
      link: null // 根节点不跳转
    },
    position: position,
    targetPosition: 'bottom', // 只能入，不能出
    selectable: true,
    draggable: true
  }
}

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

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  professionId: {
    type: String,
    default: '1'
  },
  copiedRoadmap: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'update:modelValue', 
  'close', 
  'save',
  'roadmap-saved'
])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 内部状态管理 - 确认对话框相关
const showSaveConfirmDialog = ref(false)
const showResetConfirmDialog = ref(false)
const roadmapDescription = ref('')

// 搜索相关状态
const searchText = ref('')
const availableCourses = ref([])

// 编辑相关状态 - 内部管理
const internalNodes = ref([])
const internalEdges = ref([])
const showEmptyAreaTip = ref(true)

// 保存相关方法
function handleSave() {
  // 先进行基本验证
  if (internalNodes.value.length === 0) {
    showSnackbar('请先添加课程节点')
    return
  }
  
  // 确保有根节点
  const hasRootNode = internalNodes.value.some(node => node.id === '0')
  if (!hasRootNode) {
    showSnackbar('课程表必须包含根节点')
    return
  }

  // 验证树结构
  const validation = validateTreeStructure()
  if (!validation.valid) {
    showSnackbar(`课程表结构不合法：${validation.message}`)
    return
  }

  // 显示保存确认对话框
  showSaveConfirmDialog.value = true
}

// 确认保存
async function confirmSave(description) {
  roadmapDescription.value = description
  
  // 调用父组件的保存方法
  emit('save', {
    description: roadmapDescription.value.trim(),
    professionId: props.professionId,
    content: serializeRoadmapContent()
  })
  
  // 关闭确认对话框
  showSaveConfirmDialog.value = false
  
  // 清空描述
  roadmapDescription.value = ''
}

// 取消保存
function cancelSaveConfirm() {
  showSaveConfirmDialog.value = false
  roadmapDescription.value = ''
}

// 重置相关方法
function handleReset() {
  if (internalNodes.value.length === 0 && internalEdges.value.length === 0) {
    showSnackbar('编辑区域已为空，无需重置')
    return
  }
  
  showResetConfirmDialog.value = true
}

// 确认重置
function confirmReset() {
  showResetConfirmDialog.value = false
  roadmapDescription.value = ''
  
  // 直接重置内部状态
  internalNodes.value = []
  internalEdges.value = []
  showEmptyAreaTip.value = false
  
  // 重置后也要添加根节点
  internalNodes.value.push(createRootNode())
  
  showSnackbar('课程表编辑区域已重置')
}

// 取消重置
function cancelReset() {
  showResetConfirmDialog.value = false
}

// 搜索相关方法
async function loadAvailableCourses(searchName = '') {
  try {
    const response = await learnService.searchByName(searchName);
    availableCourses.value = response.data || [];
    console.log('获取课程数据成功:', availableCourses.value);
  } catch (err) {
    console.error('获取课程数据失败:', err);
    availableCourses.value = [];
    showSnackbar('获取课程数据失败: ' + (err.message || '未知错误'));
  }
}

function onSearchCourses() {
  if (!searchText.value.trim()) {
    availableCourses.value = [];
    return;
  }
  loadAvailableCourses(searchText.value.trim());
}

// 编辑相关方法
function addCourseNode(course) {
  const node = internalNodes.value.find(node => node.id === course.id.toString());
  if (node) {
    console.log(`课程 "${course.name}" 已存在，不再重复添加`)
    return;
  }

  // 计算新节点位置，避免与现有节点重叠
  let newX = 100
  let newY = 100

  // 如果已有节点，在最右侧和最下方找空位
  if (internalNodes.value.length > 0) {
    const maxX = Math.max(...internalNodes.value.map(node => node.position.x))
    const maxY = Math.max(...internalNodes.value.map(node => node.position.y))

    // 新节点放在右侧，如果右侧空间不够则换行
    newX = maxX + 200 // 节点间距离
    newY = 100

    // 如果X坐标超出合理范围，换到下一行
    if (newX > 600) {
      newX = 100
      newY = maxY + 100 // 行间距离
    }
  }

  const newNode = {
    id: course.id.toString(), // 确保 id 是字符串
    type: 'default', // 使用默认节点类型
    data: { 
      label: course.name,
      link: '/read?courseId=' + course.id
    },
    position: { x: newX, y: newY },
    sourcePosition: 'top',
    targetPosition: 'bottom',
    selectable: true
  }
  internalNodes.value.push(newNode)
  showEmptyAreaTip.value = false // 隐藏空区域提示
  console.log(`成功添加课程节点: ${course.name}`)
}

// 处理节点连接事件
function onConnect(connection) {
  console.log('连接事件:', connection)
  const newEdge = {
    id: `${connection.source}-${connection.target}`,
    source: connection.source,
    target: connection.target,
    type: 'bezier',
    animated: true,
    selectable: true
  }
  internalEdges.value.push(newEdge)
}

function onNodesChange(changes) {
  changes.forEach(change => {
    if (change.type === 'position') {
      if (change.position) {
        // 更新节点位置
        const node = internalNodes.value.find(node => node.id === change.id);
        if (node) {
          node.position = change.position;
        }
      }
    } else if (change.type === 'select') {
      // 处理节点选择状态变化
      const node = internalNodes.value.find(node => node.id === change.id);
      if (node) {
        node.selected = change.selected;
      }
    } else if (change.type === 'remove') {
      // 删除节点
      deleteNode(change.id);
    }
  });
}

// 删除边的事件处理
function onEdgesChange(changes) {
  changes.forEach(change => {
    if (change.type === 'select') {
      // 处理边选择状态变化
      const edge = internalEdges.value.find(edge => edge.id === change.id);
      if (edge) {
        edge.selected = change.selected;
      }
    } else if (change.type === 'remove') {
      // 删除边
      deleteEdge(change.id);
    }
  });
}

// 删除节点的函数
function deleteNode(nodeId) {
  // 不允许删除根节点
  if (nodeId === '0' || nodeId === 0) {
    showSnackbar('根节点不能删除')
    return
  }
  
  console.log(`删除节点: ${nodeId}`);
  
  // 删除节点
  internalNodes.value = internalNodes.value.filter(node => node.id !== nodeId);

  // 删除与该节点相关的边
  internalEdges.value = internalEdges.value.filter(
    edge => edge.source !== nodeId && edge.target !== nodeId
  );

  console.log(`节点 ${nodeId} 及其相关边已删除`);
}

// 删除边的函数
function deleteEdge(edgeId) {
  console.log(`删除边: ${edgeId}`);
  
  // 删除边
  internalEdges.value = internalEdges.value.filter(edge => edge.id !== edgeId);

  console.log(`边 ${edgeId} 已删除`);
}

// 删除选中的节点和边
function deleteSelected() {
  // 找到所有选中的节点
  const selectedNodes = internalNodes.value.filter(node => node.selected);
  // 找到所有选中的边
  const selectedEdges = internalEdges.value.filter(edge => edge.selected);

  if (selectedNodes.length === 0 && selectedEdges.length === 0) {
    console.log('没有选中的节点或边');
    return;
  }

  // 删除选中的节点
  selectedNodes.forEach(node => {
    deleteNode(node.id);
  });

  // 删除选中的边
  selectedEdges.forEach(edge => {
    deleteEdge(edge.id);
  });

  console.log(`删除了 ${selectedNodes.length} 个节点和 ${selectedEdges.length} 条边`);
}

// 处理选择变化事件
function onSelectionChange({ nodes, edges }) {
  console.log('选择变化:', { nodes, edges });
  
  // 更新节点的选中状态
  internalNodes.value.forEach(node => {
    node.selected = nodes.some(selectedNode => selectedNode.id === node.id);
  });
  
  // 更新边的选中状态
  internalEdges.value.forEach(edge => {
    edge.selected = edges.some(selectedEdge => selectedEdge.id === edge.id);
  });
}

function recalculateLayout() {
  if (internalNodes.value.length === 0) {
    console.warn('没有节点可布局');
    return;
  }

  // 调用 applyAutoLayout 函数重新计算布局
  const layoutedNodes = applyAutoLayout(
    internalNodes.value,
    internalEdges.value,
    'BT' // 默认方向为从上到下
  );

  // 更新节点位置
  internalNodes.value = layoutedNodes;
  console.log('重新布局完成:', internalNodes.value);
}

// 验证课程表是否形成一棵有效的树
function validateTreeStructure() {
  const nodes = internalNodes.value
  const edges = internalEdges.value

  if (nodes.length === 0) {
    return { valid: false, message: '课程表不能为空' }
  }

  // 单个节点的情况是有效的树
  if (nodes.length === 1) {
    return { valid: true }
  }

  // 对于有多个节点的情况，需要检查是否形成树结构
  if (edges.length !== nodes.length - 1) {
    return { 
      valid: false, 
      message: `树结构需要有 ${nodes.length - 1} 条边，但当前有 ${edges.length} 条边` 
    }
  }

  // 检查是否有环
  const nodeIds = nodes.map(node => node.id)
  const adjacencyList = new Map()
  
  // 初始化邻接表
  nodeIds.forEach(id => {
    adjacencyList.set(id, [])
  })

  // 构建邻接表
  edges.forEach(edge => {
    adjacencyList.get(edge.source).push(edge.target)
    adjacencyList.get(edge.target).push(edge.source)
  })

  // 使用DFS检查连通性和环
  const visited = new Set()
  const parent = new Map()

  function hasCycle(nodeId, parentId) {
    visited.add(nodeId)
    parent.set(nodeId, parentId)

    const neighbors = adjacencyList.get(nodeId) || []
    for (const neighbor of neighbors) {
      if (!visited.has(neighbor)) {
        if (hasCycle(neighbor, nodeId)) {
          return true
        }
      } else if (neighbor !== parentId) {
        // 找到了不是父节点的已访问节点，说明有环
        return true
      }
    }
    return false
  }

  // 从第一个节点开始DFS
  const startNode = nodeIds[0]
  if (hasCycle(startNode, null)) {
    return { valid: false, message: '课程表中存在环路，不能形成树结构' }
  }

  // 检查是否所有节点都被访问到（连通性）
  if (visited.size !== nodes.length) {
    return { valid: false, message: '课程表不是连通的，存在孤立的节点或子图' }
  }

  return { valid: true }
}

// 序列化课程表内容为指定格式
function serializeRoadmapContent() {
  // 获取所有边，格式为 [[source, target], [source, target], ...]
  const edgeArray = internalEdges.value.map(edge => {
    const source = parseInt(edge.source)
    const target = parseInt(edge.target)
    
    // 验证节点ID是否有效
    if (isNaN(source) || isNaN(target)) {
      console.warn('发现无效的边ID:', edge)
      return null
    }
    
    return [source, target]
  }).filter(edge => edge !== null) // 过滤掉无效边
  
  // 获取所有节点ID，格式为 [id1, id2, id3, ...]，包括根节点id=0
  const nodeArray = internalNodes.value.map(node => {
    const id = parseInt(node.id)
    
    // 验证节点ID是否有效（包括id=0的根节点）
    if (isNaN(id)) {
      console.warn('发现无效的节点ID:', node)
      return null
    }
    
    return id
  }).filter(id => id !== null) // 过滤掉无效ID
  
  // 返回嵌套数组格式：[edges, nodes]
  const content = [edgeArray, nodeArray]
  
  console.log('序列化结果:', content)
  return JSON.stringify(content)
}

// 初始化编辑区域
function initializeEditor() {
  // 如果有复制的课程表数据，优先使用
  if (props.copiedRoadmap && props.copiedRoadmap.content) {
    try {
      const content = typeof props.copiedRoadmap.content === 'string' 
        ? JSON.parse(props.copiedRoadmap.content) 
        : props.copiedRoadmap.content
      
      // 检查是否是 {nodes: [], edges: []} 格式
      if (content.nodes && content.edges) {
        // 重建节点数据
        internalNodes.value = content.nodes.map(node => {
          if (node.id === 0 || node.id === '0') {
            // 根节点
            return createRootNode(node.position)
          } else {
            // 其他节点保持原有数据结构
            return {
              ...node,
              id: node.id.toString(),
              type: 'default',
              selectable: true,
              sourcePosition: 'top',
              targetPosition: 'bottom',
              draggable: true
            }
          }
        })
        
        // 重建边数据
        internalEdges.value = content.edges.map(edge => ({
          ...edge,
          id: edge.id || `${edge.source}-${edge.target}`,
          source: edge.source.toString(),
          target: edge.target.toString(),
          selectable: true
        }))
        
        // 应用自动布局
        if (internalNodes.value.length > 0) {
          const layoutedNodes = applyAutoLayout(internalNodes.value, internalEdges.value, 'BT')
          internalNodes.value = layoutedNodes
        }
        
        // 设置课程表描述
        if (props.copiedRoadmap.description) {
          roadmapDescription.value = `复制 - ${props.copiedRoadmap.description}`
        }
        
        showEmptyAreaTip.value = false
        console.log('成功复制课程表数据')
        showSnackbar('课程表复制成功，可以开始编辑')
        return
      }
    } catch (error) {
      console.error('解析复制的课程表数据失败:', error)
      showSnackbar('复制课程表数据失败，将创建空白课程表')
    }
  }
  
  // 默认添加根节点（id=0）
  if (internalNodes.value.length === 0) {
    internalNodes.value.push(createRootNode())
    showEmptyAreaTip.value = false
  }
}

// 监听弹窗打开，初始化编辑器
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    initializeEditor()
  } else {
    // 弹窗关闭时清空内部状态
    internalNodes.value = []
    internalEdges.value = []
    showEmptyAreaTip.value = true
    searchText.value = ''
    availableCourses.value = []
    roadmapDescription.value = ''
  }
})
</script>

<template>
  <v-dialog v-model="visible" fullscreen :scrim="true" transition="dialog-bottom-transition">
    <v-card>
      <v-toolbar color="grey-lighten-5" flat class="flat-toolbar" density="compact">
        <v-toolbar-title class="grey-teal-darken-2">
          <v-icon class="mr-1" size="small" color="grey-darken-2">mdi-book</v-icon>
          创建新课程表
        </v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon variant="flat" @click="$emit('close')" class="flat-icon-button">
          <v-icon color="teal-darken-1">mdi-close</v-icon>
        </v-btn>
      </v-toolbar>

      <v-container fluid class="pa-0" style="height: calc(100vh - 64px)">
        <v-row no-gutters style="height: 100%">
          <!-- 左侧课程搜索区域 -->
          <v-col cols="3" class="pt-2" style="border-right: 1px solid #eee;">
            <v-card flat style="height: 100%">
              <v-card-text>
                <div class="d-flex align-center mb-2">
                  <v-text-field 
                    v-model="searchText" 
                    label="搜索课程" 
                    append-inner-icon="mdi-magnify" 
                    variant="outlined"
                    density="compact" 
                    hide-details 
                    class="flat-input" 
                    color="teal-darken-1"
                    @click:append-inner="onSearchCourses"
                    @keyup.enter="onSearchCourses">
                  </v-text-field>
                </div>

                <v-list density="compact" class="course-list-container">
                  <v-list-item 
                    v-for="course in availableCourses" 
                    :key="course.id" 
                    @click="addCourseNode(course)"
                    class="create-list-item d-flex align-center" 
                    variant="flat">
                    <template v-slot:prepend>
                      <v-icon color="grey-darken-4">mdi-plus-circle</v-icon>
                    </template>
                    <v-list-item-title class="text-grey-darken-4 py-1">{{ course.name }}</v-list-item-title>
                  </v-list-item>
                </v-list>

                <!-- 无搜索结果提示 -->
                <v-alert v-if="availableCourses.length === 0 && searchText.trim()" color="grey-darken-1" type="info" variant="tonal" density="compact">
                  未找到相关课程，请尝试其他关键词
                </v-alert>
                
                <!-- 初始状态提示 -->
                <v-alert v-if="availableCourses.length === 0 && !searchText.trim()" color="grey-darken-1" type="info" variant="tonal" density="compact">
                  请输入课程名称进行搜索
                </v-alert>
                
                <!-- 有搜索结果时的提示 -->
                <v-alert v-if="availableCourses.length > 0" color="grey-darken-1" type="info" variant="tonal" density="compact">
                  点击课程添加到右侧编辑区域
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 右侧可编辑流程图区域 -->
          <v-col cols="9">
            <v-card flat style="height: 100%">
              <v-card-title class="pt-4 d-flex justify-end align-center text-teal-darken-3">
                <div class="d-flex align-center">
                  <div class="d-flex align-center border-e pr-4">
                    <v-btn color="red-darken-1" variant="tonal" class="flat-button mr-2" @click="deleteSelected">
                      <v-icon class="me-1" left>mdi-delete</v-icon>
                      删除选中
                    </v-btn>
                    <v-btn color="blue-darken-4" variant="tonal" class="flat-button mr-2" @click="handleReset">
                      <v-icon class="me-1" left>mdi-refresh</v-icon>
                      重置
                    </v-btn>
                    <v-btn color="blue-darken-4" variant="tonal" class="flat-button" @click="recalculateLayout">
                      <v-icon class="me-1" left>mdi-autorenew</v-icon>
                      自动布局
                    </v-btn>
                  </div>
                  <v-btn color="teal-darken-1" variant="tonal" class="flat-button ml-4" @click="handleSave">
                    <v-icon class="me-1" left>mdi-content-save</v-icon>
                    编写描述并保存课程表
                  </v-btn>
                </div>
              </v-card-title>

              <v-card-text style="height: calc(100% - 80px)" class="pa-0" :style="{ position: 'relative' }">
                <VueFlow 
                  :nodes="internalNodes" 
                  :edges="internalEdges"
                  :fit-view-on-init="true"
                  :min-zoom="0.5" 
                  :max-zoom="1.5"
                  :snap-to-grid="true"
                  :snap-grid="[20, 20]" 
                  :nodes-draggable="true"
                  :nodes-connectable="true" 
                  :elements-selectable="true" 
                  :edges-updatable="true"
                  :multi-selection-active="true"
                  @connect="onConnect" 
                  @nodes-change="onNodesChange" 
                  @edges-change="onEdgesChange"
                  @selection-change="onSelectionChange">
                  <Background pattern-color="#999" :gap="20" />
                  <Controls />
                </VueFlow>
                
                <!-- 空区域提示浮层 -->
                <div v-show="internalNodes.length === 1" class="empty-area-tip">
                  <div class="tip-content">
                    <v-icon size="48" color="teal-lighten-2" class="mb-3">mdi-vector-arrange-above</v-icon>
                    <h3 class="text-teal-darken-1 mb-4">课程表编辑区域</h3>
                    <p class="text-grey-darken-1 mb-2">从左侧搜索并点击课程来添加节点</p>
                    <p class="text-grey-darken-1 mb-0">拖拽节点连接线来建立学习路径</p>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-card>
    
    <!-- 确认对话框 -->
    <ConfirmDialog
      :show-save-dialog="showSaveConfirmDialog"
      :show-reset-dialog="showResetConfirmDialog"
      :initial-description="roadmapDescription"
      @confirm-save="confirmSave"
      @cancel-save="cancelSaveConfirm"
      @confirm-reset="confirmReset"
      @cancel-reset="cancelReset"
    />
  </v-dialog>
</template>



<style scoped>
.flat-toolbar {
  border-radius: 0 !important;
  box-shadow: none !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  background: #fcfcfc !important;
}

.flat-icon-button {
  border-radius: 8px !important;
  background-color: transparent !important;
  box-shadow: none !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.flat-icon-button:hover {
  background: rgba(255, 255, 255, 0.1) !important;
}

.flat-input :deep(.v-field) {
  border-radius: 8px !important;
  box-shadow: none !important;
}

.flat-button {
  border-radius: 8px !important;
  box-shadow: none !important;
  text-transform: none !important;
  font-weight: 500 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.flat-button:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
}

.course-list-container {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.create-list-item {
  border-radius: 8px !important;
  margin: 8px 0 !important;
  padding: 8px 8px !important;
  border: 1px solid transparent !important;
  transition: all 0.2s ease !important;
  cursor: pointer;
}

.create-list-item:hover {
  border-color: #dddddd !important;
  background: #d6f3f4 !important;
  transform: translateY(-1px) !important;
}

.create-list-item:nth-child(odd) {
  background-color: #f9f9f9; /* 奇数行背景色 */
}

.create-list-item:nth-child(even) {
  background-color: #fdfdfd; /* 偶数行背景色 */
}

/* 空区域提示浮层样式 */
.empty-area-tip {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(2px);
  z-index: 10;
}

.tip-content {
  text-align: center;
  padding: 2rem;
  margin-bottom: 10%;
  border-radius: 12px;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  border: 2px dashed #b2dfdb;
}

/* 选中状态的样式 */
:deep(.vue-flow__node.selected) {
  box-shadow: 0 0 0 2px #1976d2 !important;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: #1976d2 !important;
  stroke-width: 3px !important;
}

/* 确保节点和边可以被选中 */
:deep(.vue-flow__node) {
  padding: 3px;
  border-radius: 8px;
  cursor: pointer !important;
}

:deep(.vue-flow__edge) {
  cursor: pointer !important;
}

/* 编辑模式下的根节点样式 */
:deep(.vue-flow__node[data-id="0"]) {
  background: #4f87a0  !important;
  border: 4px double #cae0e9 !important;
  color: #ffffff !important;
  font-weight: 500 !important;
}

/* 连接点样式 - 入口红色，出口绿色 */
:deep(.vue-flow__handle.vue-flow__handle-top) {
  padding: 3px;
  border: 1px solid #000000 !important; 
  background: #f44336 !important; 
}

:deep(.vue-flow__handle.vue-flow__handle-bottom) {
  padding: 3px;
  border: 1px solid #000000 !important;
  background: #4caf50 !important; 
}
</style>
