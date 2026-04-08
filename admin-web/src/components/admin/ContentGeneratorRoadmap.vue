<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch, onUnmounted } from 'vue'
import { VueFlow, useVueFlow, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import type { Node, Edge, Connection } from '@vue-flow/core'
import dagre from 'dagre'
import { adminApi } from '@/api'
import { useMutation, useFetch } from '@/composables'
import CategorySelector from '@/components/common/CategorySelector.vue'

// 引入样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

// Vue Flow 实例
const { fitView } = useVueFlow()

// 前台地址
const webBaseUrl = import.meta.env.VITE_WEB_BASE_URL || 'http://localhost:5178'

// 职业选择
const roleId = ref<string>('')

// 任务状态
const taskId = ref<string>('')
const taskStatus = ref<string>('')
const generatedContent = ref<string>('')
const taskError = ref<string>('')

// Vue Flow 节点和边
const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])

// 撤销/重做历史
const operationHistory = ref<Array<{ nodes: Node[]; edges: Edge[] }>>([])
const historyIndex = ref(-1)

// 历史记录
const historyList = ref<any[]>([])
const loadingHistory = ref(false)

// 选中的节点和边
const selectedNode = ref<Node | null>(null)
const selectedEdge = ref<Edge | null>(null)
const editDialog = ref(false)

// 节点信息编辑
const editingNodeInfo = ref(false)
const editNodeName = ref('')
const editNodeDescription = ref('')

// 添加节点/边对话框
const addNodeDialog = ref(false)
const newNodeId = ref('')
const newNodeIdType = ref<'course' | 'node'>('course')

const addEdgeDialog = ref(false)
const newEdgeSource = ref('')
const newEdgeTarget = ref('')

// 历史记录弹窗
const historyDialog = ref(false)

// 草稿列表弹窗
const draftDialog = ref(false)
const draftList = ref<any[]>([])
const loadingDrafts = ref(false)

// 监听弹窗打开，自动加载历史记录
watch(historyDialog, (newValue) => {
  if (newValue) {
    loadHistory()
  }
})

// 监听草稿弹窗打开，自动加载草稿列表
watch(draftDialog, (newValue) => {
  if (newValue) {
    loadDrafts()
  }
})

// Tab 切换
const resultTab = ref<'graph' | 'json'>('graph')

// 监听 Tab 切换，从图表切换到 JSON 时同步数据
watch(resultTab, (newValue, oldValue) => {
  if (oldValue === 'graph' && newValue === 'json' && nodes.value.length > 0) {
    // 从图表视图切换到 JSON 视图，将图表数据转换为 JSON
    const graphData = {
      nodes: nodes.value.map((node) => ({
        id: node.id,
        type: node.data.nodeType,
        name: node.data.label,
        description: node.data.description,
      })),
      edges: edges.value.map((edge) => ({
        source: edge.source,
        target: edge.target,
      })),
    }
    generatedContent.value = JSON.stringify(graphData, null, 2)
  }
})

// 搜索相关
const searchKeyword = ref<string>('')
const searchResults = ref<any[]>([])
const searchLoading = ref(false)

// 节点编辑
const mappedId = ref<string>('')
const mappedIdType = ref<'course' | 'node'>('course')
const createNew = ref(true)

// 创建新课程/节点的表单
const createCourseMainCategory = ref<number | null>(null)
const createCourseSubCategory = ref<number | null>(null)
const createNodeCourseId = ref<string>('')

// 节点样式
const ROOT_NODE_STYLE = {
  background: '#9C27B0',
  color: 'white',
  border: '2px solid #7B1FA2',
  borderRadius: '8px',
  padding: '12px 20px',
  fontSize: '14px',
  fontWeight: 'bold',
}

const COURSE_NODE_STYLE = {
  background: '#fff',
  color: '#1a1a1a',
  border: '2px solid #FF9800',
  borderRadius: '8px',
  padding: '10px 16px',
  fontSize: '13px',
}

const NODE_NODE_STYLE = {
  background: '#fff',
  color: '#1a1a1a',
  border: '2px solid #2196F3',
  borderRadius: '8px',
  padding: '10px 16px',
  fontSize: '13px',
}

const EDGE_STYLE = {
  stroke: '#b1b1b7',
  strokeWidth: 2,
}

// 生成路径
const { execute: generateRoadmap, loading: generating } = useMutation(
  async () => {
    const id = parseInt(roleId.value, 10)
    const response = await adminApi.generateRoadmap(id)
    return response
  },
  {
    successMessage: '路径生成任务已提交',
    showToast: false,
    onSuccess: (data) => {
      taskId.value = data.taskId
      taskStatus.value = data.status
      generatedContent.value = ''
      taskError.value = ''
      nodes.value = []
      edges.value = []
      // 开始轮询任务状态
      startPolling()
      // 刷新历史记录
      loadHistory()
    },
  }
)

// 轮询任务状态
let pollingTimer: ReturnType<typeof setInterval> | null = null

const startPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }

  pollingTimer = setInterval(async () => {
    try {
      const response = await adminApi.getRoadmapTask(taskId.value)
      if (response.code === 200 && response.data) {
        taskStatus.value = response.data.status

        if (response.data.status === 'COMPLETED') {
          generatedContent.value = response.data.result || ''
          parseAndDisplayRoadmap(response.data.result || '')
          stopPolling()
          // 10秒后清除成功提示
          setTimeout(() => {
            if (taskStatus.value === 'COMPLETED') {
              taskStatus.value = ''
            }
          }, 10000)
        } else if (response.data.status === 'FAILED') {
          taskError.value = response.data.error || '生成失败'
          stopPolling()
        }
      }
    } catch (error) {
      console.error('轮询任务状态失败:', error)
    }
  }, 2000) // 每2秒轮询一次
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

// 解析并显示路径
const parseAndDisplayRoadmap = (resultJson: string) => {
  try {
    // 清理可能的代码块标记
    let cleanJson = resultJson.trim()

    // 移除 ```json 或 ``` 开头
    if (cleanJson.startsWith('```json')) {
      cleanJson = cleanJson.substring(7)
    } else if (cleanJson.startsWith('```')) {
      cleanJson = cleanJson.substring(3)
    }

    // 移除 ``` 结尾
    if (cleanJson.endsWith('```')) {
      cleanJson = cleanJson.substring(0, cleanJson.length - 3)
    }

    cleanJson = cleanJson.trim()

    const data = JSON.parse(cleanJson)

    // 转换节点
    nodes.value = data.nodes.map((node: any) => {
      const isRootNode = node.id === '0' || node.id === 0
      return {
        id: node.id,
        type: 'default',
        data: {
          label: node.name,
          nodeType: node.type,
          description: node.description,
        },
        position: { x: 0, y: 0 },
        sourcePosition: isRootNode ? undefined : Position.Right,
        targetPosition: Position.Left,
        style: node.type === 'course' ? COURSE_NODE_STYLE : NODE_NODE_STYLE,
      }
    })

    // 转换边
    edges.value = data.edges.map((edge: any) => ({
      id: `${edge.source}-${edge.target}`,
      source: edge.source,
      target: edge.target,
      type: 'default',
      animated: true,
      style: EDGE_STYLE,
    }))

    // 应用自动布局
    setTimeout(() => {
      applyAutoLayout()
      // 更新节点样式（显示已映射状态）
      updateNodeStyles()
      // 保存初始状态到历史
      saveToHistory()
    }, 100)
  } catch (error) {
    console.error('解析路径失败:', error)
  }
}

// 节点点击事件（单击选中）
const onNodeClick = (event: any) => {
  const node = nodes.value.find((n) => n.id === event.node.id)
  if (node) {
    selectedNode.value = node
    selectedEdge.value = null // 取消边的选中

    // 高亮选中的节点
    updateNodeStyles()
    updateEdgeStyles()
  }
}

// 节点双击事件（打开编辑对话框）
const onNodeDoubleClick = (event: any) => {
  const node = nodes.value.find((n) => n.id === event.node.id)
  if (node && node.id !== 'temp-0') {
    // 根节点不可编辑
    selectedNode.value = node
    selectedEdge.value = null

    // 初始化编辑状态
    editingNodeInfo.value = false
    editNodeName.value = node.data.label
    editNodeDescription.value = node.data.description

    mappedId.value = ''
    mappedIdType.value = node.data.nodeType || 'course'
    createNew.value = true
    searchKeyword.value = ''
    searchResults.value = []

    // 重置创建表单
    createCourseMainCategory.value = null
    createCourseSubCategory.value = null
    createNodeCourseId.value = ''

    editDialog.value = true

    // 高亮选中的节点
    updateNodeStyles()
    updateEdgeStyles()
  }
}

// 开始编辑节点信息
const startEditNodeInfo = () => {
  editingNodeInfo.value = true
}

// 保存节点信息（名称和描述）
const saveNodeInfo = () => {
  if (!selectedNode.value) return

  selectedNode.value.data.label = editNodeName.value
  selectedNode.value.data.description = editNodeDescription.value

  nodes.value = [...nodes.value] // 触发响应式更新
  editingNodeInfo.value = false
  saveToHistory() // 保存到历史
}

// 取消编辑节点信息
const cancelEditNodeInfo = () => {
  editingNodeInfo.value = false
  editNodeName.value = selectedNode.value?.data.label || ''
  editNodeDescription.value = selectedNode.value?.data.description || ''
}

// 更新节点样式（高亮选中的节点）
const updateNodeStyles = () => {
  nodes.value.forEach((node) => {
    // 判断是否已映射：节点ID不是 temp- 开头就表示已映射到真实ID
    const hasRealId = !node.id.startsWith('temp-')

    // 基础样式
    const baseStyle = node.data.nodeType === 'course' ? COURSE_NODE_STYLE : NODE_NODE_STYLE

    if (node.id === selectedNode.value?.id) {
      // 选中的节点：添加灰色阴影，如果已映射则保留绿色背景
      node.style = {
        ...baseStyle,
        background: hasRealId ? '#E8F5E9' : baseStyle.background,
        boxShadow: '0 0 0 5px rgba(0, 0, 0, 0.15)',
      }
    } else if (hasRealId) {
      // 已映射到真实ID的节点：添加浅绿色背景
      node.style = {
        ...baseStyle,
        background: '#E8F5E9',  // 浅绿色背景
      }
    } else {
      // 未选中且未映射的节点：原始样式
      node.style = baseStyle
    }
  })
  nodes.value = [...nodes.value] // 触发响应式更新
}

// 更新边样式（高亮选中的边）
const updateEdgeStyles = () => {
  edges.value.forEach((edge) => {
    if (edge.id === selectedEdge.value?.id) {
      // 选中的边：加粗、蓝色
      edge.style = {
        stroke: '#2196F3',
        strokeWidth: 4,
      }
    } else {
      // 未选中的边：恢复原始样式
      edge.style = EDGE_STYLE
    }
  })
  edges.value = [...edges.value] // 触发响应式更新
}

// 搜索课程/节点（用当前节点名称搜索）
const searchContent = async () => {
  if (!selectedNode.value) return

  searchLoading.value = true
  try {
    // 使用当前节点的名称作为关键词搜索
    const keyword = selectedNode.value.data.label
    // TODO: 调用搜索 API
    searchResults.value = []  // 暂时为空
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    searchLoading.value = false
  }
}

// 选择匹配项
const selectMatch = (item: any) => {
  mappedId.value = item.id.toString()
  mappedIdType.value = item.type // 假设搜索结果返回type字段
}

// 映射节点时保存的旧节点ID
let replacingNodeOldId: string = ''

// 通过ID替换节点信息
const { loading: replacingNode, execute: replaceNodeById } = useMutation(
  async () => {
    if (!selectedNode.value) throw new Error('节点不存在')

    const id = parseInt(mappedId.value)
    if (!id || id <= 0) throw new Error('请输入有效的ID')

    // 保存当前节点ID到外部变量
    replacingNodeOldId = selectedNode.value.id

    let nodeId = id
    if (mappedIdType.value === 'course') {
      // 查询课程，获取根节点ID
      const courseResponse = await adminApi.getCourseDetail(id)
      if (!courseResponse?.data?.rootNodeId) {
        throw new Error('课程没有根节点')
      }
      nodeId = courseResponse.data.rootNodeId
    }

    // 统一查询节点信息
    return await adminApi.getNodeDetail(nodeId)
  },
  {
    showToast: false,
    onSuccess: (data) => {
      if (data && replacingNodeOldId) {
        const newNodeId = data.id.toString()
        const oldNodeId = replacingNodeOldId

        // 找到节点并更新其ID和信息
        const node = nodes.value.find((n) => n.id === oldNodeId)
        if (node) {
          // 根据 isCourseRoot 判断节点类型
          const nodeType = data.isCourseRoot === 1 ? 'course' : 'node'

          // 更新节点ID和信息
          node.id = newNodeId
          node.data.label = data.name || ''
          node.data.nodeType = nodeType
          node.data.description = data.description || ''
          node.style = nodeType === 'course' ? COURSE_NODE_STYLE : NODE_NODE_STYLE

          // 更新所有引用此节点的边
          edges.value.forEach((edge) => {
            if (edge.source === oldNodeId) {
              edge.source = newNodeId
              edge.id = `${newNodeId}-${edge.target}`
            }
            if (edge.target === oldNodeId) {
              edge.target = newNodeId
              edge.id = `${edge.source}-${newNodeId}`
            }
          })

          nodes.value = [...nodes.value]
          edges.value = [...edges.value]
          editDialog.value = false
          saveToHistory()
          updateNodeStyles()
        }

        // 清空保存的ID
        replacingNodeOldId = ''
      }
    },
  }
)

// 创建新课程时保存的旧节点ID
let creatingCourseOldId: string = ''

// 创建新课程
const { execute: createCourseAndMap, loading: creatingCourse } = useMutation(
  async () => {
    if (!selectedNode.value) throw new Error('节点不存在')
    if (!createCourseMainCategory.value) throw new Error('请选择主分类')
    if (!createCourseSubCategory.value) throw new Error('请选择子分类')

    // 保存当前节点ID到外部变量
    creatingCourseOldId = selectedNode.value.id

    const response = await adminApi.createCourse({
      name: selectedNode.value.data.label,
      description: selectedNode.value.data.description,
      mainCategory: createCourseMainCategory.value,
      subCategory: createCourseSubCategory.value,
    })

    const courseId = response.data
    // 查询课程详情获取 rootNodeId
    return await adminApi.getCourseDetail(courseId)
  },
  {
    successMessage: '课程创建成功',
    onSuccess: (courseData) => {
      if (courseData?.rootNodeId && creatingCourseOldId) {
        const newNodeId = courseData.rootNodeId.toString()
        const oldNodeId = creatingCourseOldId

        // 找到节点并更新其ID
        const node = nodes.value.find((n) => n.id === oldNodeId)
        if (node) {
          node.id = newNodeId

          // 更新所有引用此节点的边
          edges.value.forEach((edge) => {
            if (edge.source === oldNodeId) {
              edge.source = newNodeId
              edge.id = `${newNodeId}-${edge.target}`
            }
            if (edge.target === oldNodeId) {
              edge.target = newNodeId
              edge.id = `${edge.source}-${newNodeId}`
            }
          })

          nodes.value = [...nodes.value]
          edges.value = [...edges.value]
          editDialog.value = false
          saveToHistory()
          updateNodeStyles()
        }

        // 清空保存的ID
        creatingCourseOldId = ''
      }
    },
  }
)

// 创建新节点时保存的旧节点ID
let creatingNodeOldId: string = ''

// 创建新节点
const { execute: createNodeAndMap, loading: creatingNode } = useMutation(
  async () => {
    if (!selectedNode.value) throw new Error('节点不存在')
    if (!createNodeCourseId.value) throw new Error('请输入课程ID')

    // 保存当前节点ID到外部变量
    creatingNodeOldId = selectedNode.value.id

    return await adminApi.createNode({
      name: selectedNode.value.data.label,
      description: selectedNode.value.data.description,
      courseId: parseInt(createNodeCourseId.value),
    })
  },
  {
    successMessage: '节点创建成功',
    onSuccess: (nodeId) => {
      if (nodeId && creatingNodeOldId) {
        const newNodeId = nodeId.toString()
        const oldNodeId = creatingNodeOldId

        // 找到节点并更新其ID
        const node = nodes.value.find((n) => n.id === oldNodeId)
        console.log('找到节点:', node)
        if (node) {
          console.log('更新前:', { id: node.id })
          node.id = newNodeId
          console.log('更新后:', { id: node.id })

          // 更新所有引用此节点的边
          edges.value.forEach((edge) => {
            if (edge.source === oldNodeId) {
              edge.source = newNodeId
              edge.id = `${newNodeId}-${edge.target}`
            }
            if (edge.target === oldNodeId) {
              edge.target = newNodeId
              edge.id = `${edge.source}-${newNodeId}`
            }
          })

          nodes.value = [...nodes.value]
          edges.value = [...edges.value]
          console.log('nodes更新完成，最新nodes:', nodes.value)
          editDialog.value = false
          saveToHistory()
          updateNodeStyles()
        }

        // 清空保存的ID
        creatingNodeOldId = ''
      }
    },
  }
)

// 保存节点映射
const saveNodeMapping = () => {
  if (!selectedNode.value) return

  if (createNew.value) {
    // 创建新的课程或节点
    if (selectedNode.value.data.nodeType === 'course') {
      createCourseAndMap()
    } else {
      createNodeAndMap()
    }
  } else if (mappedId.value) {
    // 映射到已有课程/节点，通过ID替换
    replaceNodeById()
  } else {
    // 直接保存名称和描述的修改
    nodes.value = [...nodes.value] // 触发响应式更新
    editDialog.value = false
  }
}

// 自动布局
const applyAutoLayout = () => {
  if (nodes.value.length <= 1) {
    return
  }

  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: 'LR', // Left to Right (从左到右)
    nodesep: 20,    // 上下间距（减小）
    ranksep: 150,   // 左右间距（加大）
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 120

  // 动态计算节点高度（根据文字长度，区分中英文）
  const calculateNodeHeight = (text: string, width: number) => {
    // 计算文字实际宽度（中文约13px，英文约7px）
    let textWidth = 0
    for (let i = 0; i < text.length; i++) {
      const code = text.charCodeAt(i)
      // 判断是否为中文字符（简单判断）
      if (code > 255) {
        textWidth += 13 // 中文字符宽度
      } else {
        textWidth += 7 // 英文/数字字符宽度
      }
    }

    const padding = 32 // 左右padding总和
    const usableWidth = width - padding
    const lines = Math.ceil(textWidth / usableWidth)
    const minHeight = 40
    const lineHeight = 20
    return Math.max(minHeight, lines * lineHeight + 20)
  }

  // 添加节点（使用动态高度）
  nodes.value.forEach((node) => {
    const nodeHeight = calculateNodeHeight(node.data.label, nodeWidth)
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })

  // 添加边（关键：使用 toString()）
  edges.value.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  nodes.value.forEach((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    const nodeHeight = calculateNodeHeight(node.data.label, nodeWidth)
    node.position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    }
  })

  // 关键：重新赋值整个数组以触发响应式更新
  nodes.value = [...nodes.value]

  // 布局完成后，调用 fitView 聚焦到所有节点
  nextTick(() => {
    fitView({ padding: 0.2, duration: 300 })
  })
}

const startGenerate = () => {
  if (!roleId.value) {
    return
  }
  generateRoadmap()
}

// 从JSON加载路径
const loadFromJson = () => {
  try {
    parseAndDisplayRoadmap(generatedContent.value)
  } catch (error) {
    console.error('加载JSON失败:', error)
    alert('JSON格式错误，请检查')
  }
}

// ========== 编辑功能 ==========

// 保存当前状态到历史（用于撤销/重做）
const saveToHistory = () => {
  // 删除当前索引之后的历史
  operationHistory.value = operationHistory.value.slice(0, historyIndex.value + 1)
  // 添加当前状态
  operationHistory.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value)),
  })
  historyIndex.value++
  // 限制历史记录数量
  if (operationHistory.value.length > 50) {
    operationHistory.value.shift()
    historyIndex.value--
  }
}

// 撤销
const undo = () => {
  if (historyIndex.value > 0) {
    historyIndex.value--
    const state = operationHistory.value[historyIndex.value]
    nodes.value = JSON.parse(JSON.stringify(state.nodes))
    edges.value = JSON.parse(JSON.stringify(state.edges))
  }
}

// 重做
const redo = () => {
  if (historyIndex.value < operationHistory.value.length - 1) {
    historyIndex.value++
    const state = operationHistory.value[historyIndex.value]
    nodes.value = JSON.parse(JSON.stringify(state.nodes))
    edges.value = JSON.parse(JSON.stringify(state.edges))
  }
}

// 是否可以撤销/重做
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < operationHistory.value.length - 1)

// 删除选中的节点
const deleteSelectedNode = () => {
  if (!selectedNode.value) return

  const nodeId = selectedNode.value.id
  // 删除节点
  nodes.value = nodes.value.filter((n) => n.id !== nodeId)
  // 删除相关的边
  edges.value = edges.value.filter((e) => e.source !== nodeId && e.target !== nodeId)
  selectedNode.value = null
  editDialog.value = false

  // 操作完成后保存状态
  saveToHistory()
  // 更新样式
  updateNodeStyles()
  updateEdgeStyles()
}

// 删除选中的边
const deleteSelectedEdge = () => {
  if (!selectedEdge.value) return

  edges.value = edges.value.filter((e) => e.id !== selectedEdge.value!.id)
  selectedEdge.value = null

  // 操作完成后保存状态
  saveToHistory()
  // 更新样式
  updateEdgeStyles()
}

// 添加新节点（直接通过API获取并创建）
const { loading: addingNode, execute: addNode } = useMutation(
  async () => {
    const id = parseInt(newNodeId.value)
    if (!id || id <= 0) throw new Error('请输入有效的ID')

    let nodeId = id
    if (newNodeIdType.value === 'course') {
      // 查询课程，获取根节点ID
      const courseResponse = await adminApi.getCourseDetail(id)
      if (!courseResponse?.data?.rootNodeId) {
        throw new Error('课程没有根节点')
      }
      nodeId = courseResponse.data.rootNodeId
    }

    // 统一查询节点信息
    return await adminApi.getNodeDetail(nodeId)
  },
  {
    showToast: false,
    onSuccess: (data) => {
      if (data) {
        const newId = `temp-${Date.now()}`

        // 根据 isCourseRoot 判断节点类型
        const nodeType = data.isCourseRoot === 1 ? 'course' : 'node'

        const newNode: Node = {
          id: newId,
          type: 'default',
          data: {
            label: data.name || '',
            nodeType: nodeType,
            description: data.description || '',
          },
          position: { x: 100, y: 100 },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
          style: nodeType === 'course' ? COURSE_NODE_STYLE : NODE_NODE_STYLE,
        }

        nodes.value.push(newNode)

        // 重置表单
        newNodeId.value = ''
        addNodeDialog.value = false

        // 操作完成后保存状态
        saveToHistory()

        // 自动布局
        nextTick(() => {
          applyAutoLayout()
        })
      }
    },
  }
)

// 添加新边
const addEdge = () => {
  if (!newEdgeSource.value || !newEdgeTarget.value) return
  if (newEdgeSource.value === newEdgeTarget.value) return // 不能自己连自己

  // 检查是否已存在
  const exists = edges.value.some(
    (e) => e.source === newEdgeSource.value && e.target === newEdgeTarget.value
  )
  if (exists) return

  const newEdge: Edge = {
    id: `${newEdgeSource.value}-${newEdgeTarget.value}`,
    source: newEdgeSource.value,
    target: newEdgeTarget.value,
    type: 'default',
    animated: true,
    style: EDGE_STYLE,
  }

  edges.value.push(newEdge)

  // 重置表单
  newEdgeSource.value = ''
  newEdgeTarget.value = ''
  addEdgeDialog.value = false

  // 操作完成后保存状态
  saveToHistory()
}

// 节点选项（用于添加边时选择）
const nodeOptions = computed(() => {
  return nodes.value.map((n) => ({
    label: n.data.label,
    value: n.id,
  }))
})

// 边点击事件
const onEdgeClick = (event: any) => {
  const edge = edges.value.find((e) => e.id === event.edge.id)
  if (edge) {
    selectedEdge.value = edge
    selectedNode.value = null // 取消节点的选中

    // 高亮选中的边
    updateNodeStyles()
    updateEdgeStyles()
  }
}

// 拖拽连接事件
const onConnect = (connection: Connection) => {
  // 不允许从根节点出发的连接（根节点只有入口，没有出口）
  if (connection.source === 'temp-0') return

  // 不能自己连自己
  if (connection.source === connection.target) return

  // 检查是否已存在相同的连接
  const exists = edges.value.find(
    (e) => e.source === connection.source && e.target === connection.target
  )
  if (exists) return

  edges.value.push({
    id: `${connection.source}-${connection.target}`,
    source: connection.source ?? '',
    target: connection.target ?? '',
    type: 'default',
    animated: true,
    style: EDGE_STYLE,
  })

  // 保存状态
  saveToHistory()
}

// 加载历史记录
const loadHistory = async () => {
  loadingHistory.value = true
  try {
    const response = await adminApi.getRoadmapHistory()
    if (response.code === 200 && response.data) {
      historyList.value = response.data
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
  } finally {
    loadingHistory.value = false
  }
}

// 加载历史记录的路径
const loadHistoryRoadmap = (item: any) => {
  if (item.result) {
    generatedContent.value = item.result
    parseAndDisplayRoadmap(item.result)
    roleId.value = item.roleId?.toString() || ''
    taskStatus.value = ''
    historyDialog.value = false // 关闭弹窗
  }
}

// 加载草稿列表
const loadDrafts = async () => {
  loadingDrafts.value = true
  try {
    const response = await adminApi.getRoadmapDrafts()
    if (response.code === 200 && response.data) {
      draftList.value = response.data
    }
  } catch (error) {
    console.error('加载草稿列表失败:', error)
  } finally {
    loadingDrafts.value = false
  }
}

// 加载草稿的路径
const loadDraftRoadmap = async (item: any) => {
  try {
    const response = await adminApi.getRoadmapDraft(item.draftId)
    if (response.code === 200 && response.data) {
      generatedContent.value = response.data
      parseAndDisplayRoadmap(response.data)
      roleId.value = item.roleId?.toString() || ''
      taskStatus.value = ''
      draftDialog.value = false // 关闭弹窗
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
  }
}

// 保存草稿
const { execute: saveDraft, loading: savingDraft } = useMutation(
  async () => {
    if (!roleId.value) {
      throw new Error('请先输入职业 ID')
    }

    // 如果在图表视图，先将当前图表转换为JSON
    let contentToSave = generatedContent.value
    if (resultTab.value === 'graph' && nodes.value.length > 0) {
      const graphData = {
        nodes: nodes.value.map((node) => ({
          id: node.id,
          type: node.data.nodeType,
          name: node.data.label,
          description: node.data.description,
        })),
        edges: edges.value.map((edge) => ({
          source: edge.source,
          target: edge.target,
        })),
      }
      contentToSave = JSON.stringify(graphData, null, 2)
    }

    if (!contentToSave) {
      throw new Error('没有可保存的内容')
    }

    const id = parseInt(roleId.value, 10)
    return await adminApi.saveRoadmapDraft(id, contentToSave)
  },
  {
    successMessage: '草稿保存成功',
    onSuccess: () => {
      // 刷新草稿列表
      loadDrafts()
    },
  }
)

// 删除草稿
const { execute: deleteDraft } = useMutation(
  async (draftId: string) => {
    return await adminApi.deleteRoadmapDraft(draftId)
  },
  {
    successMessage: '草稿删除成功',
    onSuccess: () => {
      loadDrafts()
    },
  }
)

// 保存路径对话框
const saveRoadmapDialog = ref(false)
const roadmapDescription = ref('')
const roadmapState = ref<number>(0) // 0-草稿，1-提交审核

// 打开保存路径对话框
const openSaveRoadmapDialog = () => {
  roadmapDescription.value = ''
  roadmapState.value = 0
  saveRoadmapDialog.value = true
}

// 保存路径到数据库
const { execute: saveRoadmapToDB, loading: savingRoadmap } = useMutation(
  async () => {
    if (!roleId.value) {
      throw new Error('请先输入职业 ID')
    }

    // 如果在图表视图，先将当前图表转换为后端格式
    let contentToSave = generatedContent.value
    if (resultTab.value === 'graph' && nodes.value.length > 0) {
      // 转换为后端期望的格式: [[edges...], [nodeIds...]]
      // 注意：ID必须是数字类型，不能是字符串
      const edgesData = edges.value.map((edge) => [
        parseInt(edge.source),
        parseInt(edge.target)
      ])
      const nodeIds = nodes.value.map((node) => parseInt(node.id))

      contentToSave = JSON.stringify([edgesData, nodeIds])
    }

    if (!contentToSave) {
      throw new Error('没有可保存的内容')
    }

    if (!roadmapDescription.value.trim()) {
      throw new Error('请输入路径描述')
    }

    const id = parseInt(roleId.value, 10)
    return await adminApi.createRoadmap({
      roleId: id,
      content: contentToSave,
      description: roadmapDescription.value,
      state: roadmapState.value,
    })
  },
  {
    successMessage: '路径保存成功',
    onSuccess: () => {
      saveRoadmapDialog.value = false
      roadmapDescription.value = ''
    },
  }
)

// 组件加载时获取历史记录
onMounted(() => {
  loadHistory()
})

// 组件卸载时停止轮询
import { onUnmounted } from 'vue'
onUnmounted(() => {
  stopPolling()
})

</script>

<template>
  <div class="content-generator-roadmap">
    <h2 class="text-h5 font-weight-bold mb-6">学习路径生成</h2>

    <!-- 职业选择 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-map-marker-path" class="mr-2"></v-icon>
          AI 生成学习路径
        </div>
        <div class="d-flex align-center" style="gap: 8px">
          <v-btn icon variant="text" size="small" @click="draftDialog = true">
            <v-icon icon="mdi-content-save-outline"></v-icon>
          </v-btn>
          <v-btn icon variant="text" size="small" @click="historyDialog = true">
            <v-icon icon="mdi-history"></v-icon>
          </v-btn>
        </div>
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">
          为指定职业生成完整的学习路径，AI 将自动设计从基础到高级的课程结构
        </p>

        <v-row class="align-center">
          <v-col cols="12" md="3">
            <v-text-field
              v-model="roleId"
              label="职业 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              placeholder="输入职业 ID"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn
              variant="tonal"
              :loading="generating || taskStatus === 'PENDING'"
              :disabled="!roleId"
              @click="startGenerate"
            >
              <v-icon icon="mdi-auto-fix" class="mr-2"></v-icon>
              AI 生成路径
            </v-btn>
          </v-col>
          <v-col cols="auto">
            <!-- 状态提示（成功或失败） -->
            <span v-if="taskStatus === 'COMPLETED'" class="text-body-2 text-success">
              路径生成成功！
            </span>
            <span v-else-if="taskStatus === 'FAILED'" class="text-body-2 text-error">
              生成失败: {{ taskError }}
            </span>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 生成结果 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center justify-space-between">
        <v-tabs v-model="resultTab" density="compact">
          <v-tab value="graph">
            <v-icon icon="mdi-graph-outline" size="18" class="mr-2"></v-icon>
            图表视图
          </v-tab>
          <v-tab value="json">
            <v-icon icon="mdi-code-json" size="18" class="mr-2"></v-icon>
            原始数据
          </v-tab>
        </v-tabs>

        <!-- 工具栏按钮组 -->
        <div class="d-flex align-center" style="gap: 8px">
          <!-- JSON视图按钮 -->
          <template v-if="resultTab === 'json'">
            <v-btn variant="tonal" size="small" color="primary" @click="loadFromJson">
              <v-icon icon="mdi-import" size="16" class="mr-1"></v-icon>
              加载JSON数据
            </v-btn>
            <v-btn
              variant="tonal"
              size="small"
              :loading="savingDraft"
              :disabled="!roleId || !generatedContent"
              @click="saveDraft"
            >
              <v-icon icon="mdi-content-save" size="16" class="mr-1"></v-icon>
              保存草稿
            </v-btn>
            <v-btn
              variant="tonal"
              size="small"
              color="success"
              :disabled="!roleId || !generatedContent"
              @click="openSaveRoadmapDialog"
            >
              <v-icon icon="mdi-check-circle" size="16" class="mr-1"></v-icon>
              保存路径
            </v-btn>
          </template>

          <!-- 图表视图按钮 -->
          <template v-if="resultTab === 'graph'">
            <v-btn variant="tonal" size="small" @click="addNodeDialog = true">
              <v-icon icon="mdi-plus-circle" size="16" class="mr-1"></v-icon>
              添加节点
            </v-btn>
            <v-btn variant="tonal" size="small" @click="addEdgeDialog = true">
              <v-icon icon="mdi-connection" size="16" class="mr-1"></v-icon>
              添加连接
            </v-btn>
            <v-btn
              variant="tonal"
              size="small"
              color="error"
              :disabled="!selectedNode"
              @click="deleteSelectedNode"
            >
              <v-icon icon="mdi-delete" size="16" class="mr-1"></v-icon>
              删除节点
            </v-btn>
            <v-btn
              variant="tonal"
              size="small"
              :disabled="!selectedEdge"
              @click="deleteSelectedEdge"
            >
              <v-icon icon="mdi-link-off" size="16" class="mr-1"></v-icon>
              删除连接
            </v-btn>
            <v-divider vertical class="mx-1"></v-divider>
            <v-btn variant="tonal" size="small" :disabled="!canUndo" @click="undo">
              <v-icon icon="mdi-undo" size="16" class="mr-1"></v-icon>
              撤销
            </v-btn>
            <v-btn variant="tonal" size="small" :disabled="!canRedo" @click="redo">
              <v-icon icon="mdi-redo" size="16" class="mr-1"></v-icon>
              重做
            </v-btn>
            <v-divider vertical class="mx-1"></v-divider>
            <v-btn variant="tonal" size="small" @click="applyAutoLayout">
              <v-icon icon="mdi-auto-fix" size="16" class="mr-1"></v-icon>
              自动布局
            </v-btn>
            <v-divider vertical class="mx-1"></v-divider>
            <v-btn
              variant="tonal"
              size="small"
              :loading="savingDraft"
              :disabled="!roleId || nodes.length === 0"
              @click="saveDraft"
            >
              <v-icon icon="mdi-content-save" size="16" class="mr-1"></v-icon>
              保存草稿
            </v-btn>
            <v-btn
              variant="tonal"
              size="small"
              color="success"
              :disabled="!roleId || nodes.length === 0"
              @click="openSaveRoadmapDialog"
            >
              <v-icon icon="mdi-check-circle" size="16" class="mr-1"></v-icon>
              保存路径
            </v-btn>
          </template>
        </div>
      </v-card-title>

      <v-card-text class="pa-0">
        <!-- 图表编辑器 -->
        <div v-show="resultTab === 'graph'" class="flow-container-full">
          <div v-if="nodes.length === 0" class="d-flex align-center justify-center fill-height">
            <div class="text-center text-grey">
              <v-icon icon="mdi-graph-outline" size="64" class="mb-2"></v-icon>
              <p class="text-body-2">暂无路径数据</p>
              <p class="text-caption">请生成路径或在"原始数据"标签页中输入JSON</p>
            </div>
          </div>
          <VueFlow
            v-else
            :nodes="nodes"
            :edges="edges"
            :min-zoom="0.5"
            :max-zoom="2"
            fit-view-on-init
            :snap-to-grid="true"
            :snap-grid="[20, 20]"
            @node-click="onNodeClick"
            @node-double-click="onNodeDoubleClick"
            @edge-click="onEdgeClick"
            @connect="onConnect"
          >
            <Background />
            <Controls />
          </VueFlow>
        </div>

        <!-- 原始数据 -->
        <div v-show="resultTab === 'json'" class="json-container">
          <v-textarea
            v-model="generatedContent"
            variant="outlined"
            rows="20"
            hide-details
            class="code-textarea fill-height"
          ></v-textarea>
        </div>
      </v-card-text>
    </v-card>

    <!-- 节点编辑对话框 -->
    <v-dialog v-model="editDialog" max-width="600">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center bg-grey-lighten-5">
          <v-icon icon="mdi-pencil" class="mr-2"></v-icon>
          编辑节点
        </v-card-title>
        <v-card-text class="bg-white">
          <div v-if="selectedNode">
            <!-- 节点信息编辑区域 -->
            <div class="mb-4">
              <div class="d-flex align-center justify-space-between mb-2">
                <span class="text-body-2 font-weight-bold">节点信息</span>
                <v-btn
                  v-if="!editingNodeInfo"
                  icon
                  size="x-small"
                  variant="text"
                  @click="startEditNodeInfo"
                >
                  <v-icon icon="mdi-pencil" size="18"></v-icon>
                </v-btn>
              </div>

              <template v-if="!editingNodeInfo">
                <!-- 只读显示 -->
                <div class="mb-2">
                  <div class="text-body-2 font-weight-medium mb-2">{{ selectedNode.data.label }}</div>
                  <div class="text-caption text-grey mb-2">
                    类型: {{ selectedNode.data.nodeType === 'course' ? '课程' : '知识点' }}
                  </div>
                  <div class="text-body-2">{{ selectedNode.data.description }}</div>
                </div>
              </template>

              <template v-else>
                <!-- 编辑模式 -->
                <v-text-field
                  v-model="editNodeName"
                  label="节点名称"
                  variant="outlined"
                  density="compact"
                  class="mb-3"
                ></v-text-field>
                <v-textarea
                  v-model="editNodeDescription"
                  label="节点描述"
                  variant="outlined"
                  density="compact"
                  rows="3"
                  class="mb-3"
                ></v-textarea>
                <div class="d-flex gap-2">
                  <v-btn variant="text" size="small" @click="cancelEditNodeInfo">取消</v-btn>
                  <v-btn variant="tonal" size="small" color="primary" @click="saveNodeInfo">
                    保存
                  </v-btn>
                </div>
              </template>
            </div>

            <v-divider class="mb-4"></v-divider>

            <!-- 操作选择 -->
            <v-radio-group v-model="createNew" class="mb-4">
              <v-radio
                :value="true"
                :label="
                  selectedNode.data.nodeType === 'course' ? '创建新的课程' : '创建新的节点'
                "
              ></v-radio>
              <v-radio :value="false" label="映射到已有课程/节点"></v-radio>
            </v-radio-group>

            <!-- 创建新的表单 -->
            <div v-if="createNew" class="mb-4">
              <template v-if="selectedNode.data.nodeType === 'course'">
                <p class="text-body-2 text-grey mb-3">选择课程分类</p>
                <CategorySelector
                  v-model:model-main-category="createCourseMainCategory"
                  v-model:model-sub-category="createCourseSubCategory"
                />
              </template>
              <template v-else>
                <p class="text-body-2 text-grey mb-2">输入课程ID</p>
                <v-text-field
                  v-model="createNodeCourseId"
                  label="课程 ID"
                  type="number"
                  variant="outlined"
                  density="compact"
                  hide-details
                  placeholder="输入课程ID"
                ></v-text-field>
              </template>
            </div>

            <!-- 搜索区域 -->
            <div v-if="!createNew" class="mb-4">
              <p class="text-body-2 text-grey mb-2">
                使用当前节点名称"{{ selectedNode.data.label }}"搜索匹配项
              </p>
              <a
                :href="`${webBaseUrl}/search?q=${encodeURIComponent(selectedNode.data.label)}`"
                target="_blank"
              >
                <v-btn variant="outlined" size="small" color="grey" class="mb-3">
                  <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
                  搜索匹配项
                  <v-icon icon="mdi-open-in-new" size="14" class="ml-1"></v-icon>
                </v-btn>
              </a>

              <!-- 搜索结果（移除，不再需要） -->

              <v-divider class="my-3"></v-divider>

              <!-- 手动输入ID -->
              <p class="text-body-2 mb-2">或手动输入ID替换</p>
              <v-row>
                <v-col cols="8">
                  <v-text-field
                    v-model="mappedId"
                    label="课程/节点 ID"
                    type="number"
                    variant="outlined"
                    density="compact"
                    hide-details
                  ></v-text-field>
                </v-col>
                <v-col cols="4">
                  <v-select
                    v-model="mappedIdType"
                    :items="[
                      { label: '课程', value: 'course' },
                      { label: '节点', value: 'node' },
                    ]"
                    item-title="label"
                    item-value="value"
                    label="类型"
                    variant="outlined"
                    density="compact"
                    hide-details
                  ></v-select>
                </v-col>
              </v-row>
            </div>
          </div>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="editDialog = false">取消</v-btn>
          <v-btn
            variant="tonal"
            color="primary"
            :loading="replacingNode || creatingCourse || creatingNode"
            @click="saveNodeMapping"
          >
            保存
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 添加节点对话框 -->
    <v-dialog v-model="addNodeDialog" max-width="500">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center bg-grey-lighten-5">
          <v-icon icon="mdi-plus-circle" class="mr-2"></v-icon>
          添加节点
        </v-card-title>
        <v-card-text class="bg-white">
          <v-row class="mb-3">
            <v-col cols="8">
              <v-text-field
                v-model="newNodeId"
                label="课程/节点 ID"
                type="number"
                variant="outlined"
                density="compact"
                hide-details
                placeholder="输入ID"
              ></v-text-field>
            </v-col>
            <v-col cols="4">
              <v-select
                v-model="newNodeIdType"
                :items="[
                  { label: '课程', value: 'course' },
                  { label: '节点', value: 'node' },
                ]"
                item-title="label"
                item-value="value"
                label="类型"
                variant="outlined"
                density="compact"
                hide-details
              ></v-select>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="addNodeDialog = false">取消</v-btn>
          <v-btn
            variant="tonal"
            color="primary"
            :loading="addingNode"
            :disabled="!newNodeId"
            @click="addNode"
          >
            添加
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 添加边对话框 -->
    <v-dialog v-model="addEdgeDialog" max-width="600">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center bg-grey-lighten-5">
          <v-icon icon="mdi-connection" class="mr-2"></v-icon>
          添加连接
        </v-card-title>
        <v-card-text class="bg-white">
          <p class="text-body-2 text-grey mb-4">
            连接方向：前置课程 → 目标课程（必须先学完前置课程）
          </p>
          <v-select
            v-model="newEdgeSource"
            :items="nodeOptions"
            label="前置课程 (source)"
            variant="outlined"
            density="compact"
            class="mb-3"
          ></v-select>
          <v-select
            v-model="newEdgeTarget"
            :items="nodeOptions"
            label="目标课程 (target)"
            variant="outlined"
            density="compact"
          ></v-select>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="addEdgeDialog = false">取消</v-btn>
          <v-btn variant="tonal" color="primary" @click="addEdge">添加</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 历史记录弹窗 -->
    <v-dialog v-model="historyDialog" max-width="800">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center justify-space-between bg-grey-lighten-5">
          <div class="d-flex align-center">
            <v-icon icon="mdi-history" class="mr-2"></v-icon>
            历史记录
          </div>
          <v-btn icon variant="text" size="small" :loading="loadingHistory" @click="loadHistory">
            <v-icon icon="mdi-refresh"></v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="bg-white" style="max-height: 500px; overflow-y: auto">
          <v-list v-if="historyList.length > 0" density="compact" bg-color="white">
            <v-list-item
              v-for="item in historyList"
              :key="item.taskId"
              class="history-item"
              @click="loadHistoryRoadmap(item)"
            >
              <template #prepend>
                <v-icon
                  :icon="
                    item.status === 'COMPLETED'
                      ? 'mdi-check-circle'
                      : item.status === 'FAILED'
                        ? 'mdi-alert-circle'
                        : 'mdi-clock-outline'
                  "
                  :color="
                    item.status === 'COMPLETED' ? 'success' : item.status === 'FAILED' ? 'error' : 'grey'
                  "
                  size="20"
                ></v-icon>
              </template>
              <v-list-item-title class="text-body-2">
                职业 ID: {{ item.roleId }} - {{ item.createdAt }}
              </v-list-item-title>
              <v-list-item-subtitle class="text-caption">
                {{ item.status === 'COMPLETED' ? '生成成功' : item.status === 'FAILED' ? '生成失败' : '生成中' }}
              </v-list-item-subtitle>
            </v-list-item>
          </v-list>
          <div v-else class="text-center text-grey py-8">
            <v-icon icon="mdi-inbox" size="64" class="mb-2"></v-icon>
            <p class="text-body-2">暂无历史记录</p>
            <p class="text-caption">生成路径后会自动保存到历史</p>
          </div>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="historyDialog = false">关闭</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 草稿列表弹窗 -->
    <v-dialog v-model="draftDialog" max-width="800">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center justify-space-between bg-grey-lighten-5">
          <div class="d-flex align-center">
            <v-icon icon="mdi-content-save-outline" class="mr-2"></v-icon>
            草稿列表
          </div>
          <v-btn icon variant="text" size="small" :loading="loadingDrafts" @click="loadDrafts">
            <v-icon icon="mdi-refresh"></v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text class="bg-white" style="max-height: 500px; overflow-y: auto">
          <v-list v-if="draftList.length > 0" density="compact" bg-color="white">
            <v-list-item
              v-for="item in draftList"
              :key="item.draftId"
              class="history-item"
            >
              <template #prepend>
                <v-icon icon="mdi-file-document-outline" color="primary" size="20"></v-icon>
              </template>
              <v-list-item-title class="text-body-2">
                职业 ID: {{ item.roleId }} - {{ item.createdAt }}
              </v-list-item-title>
              <template #append>
                <div class="d-flex" style="gap: 8px">
                  <v-btn
                    icon
                    variant="text"
                    size="small"
                    @click.stop="loadDraftRoadmap(item)"
                  >
                    <v-icon icon="mdi-download" size="18"></v-icon>
                  </v-btn>
                  <v-btn
                    icon
                    variant="text"
                    size="small"
                    color="error"
                    @click.stop="deleteDraft(item.draftId)"
                  >
                    <v-icon icon="mdi-delete" size="18"></v-icon>
                  </v-btn>
                </div>
              </template>
            </v-list-item>
          </v-list>
          <div v-else class="text-center text-grey py-8">
            <v-icon icon="mdi-inbox" size="64" class="mb-2"></v-icon>
            <p class="text-body-2">暂无草稿</p>
            <p class="text-caption">在原始数据标签页中点击"保存草稿"按钮</p>
          </div>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="draftDialog = false">关闭</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 保存路径对话框 -->
    <v-dialog v-model="saveRoadmapDialog" max-width="600">
      <v-card color="white" elevation="4">
        <v-card-title class="d-flex align-center bg-grey-lighten-5">
          <v-icon icon="mdi-check-circle" class="mr-2"></v-icon>
          保存路径到数据库
        </v-card-title>
        <v-card-text class="bg-white">
          <p class="text-body-2 text-grey mb-4">
            将当前编辑的学习路径保存到数据库，可选择保存为草稿或提交审核
          </p>
          <v-textarea
            v-model="roadmapDescription"
            label="路径描述 *"
            variant="outlined"
            density="compact"
            rows="3"
            placeholder="请简要描述这个学习路径的特点..."
            class="mb-3"
          ></v-textarea>
          <v-radio-group v-model="roadmapState" class="mb-2">
            <v-radio :value="0" label="保存为草稿（可继续编辑）"></v-radio>
            <v-radio :value="1" label="提交审核（发布后用户可见）"></v-radio>
          </v-radio-group>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="saveRoadmapDialog = false">取消</v-btn>
          <v-btn
            variant="tonal"
            color="success"
            :loading="savingRoadmap"
            :disabled="!roadmapDescription.trim()"
            @click="saveRoadmapToDB"
          >
            保存
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.content-generator-roadmap {
  max-width: 100%;
  padding: 0;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.flow-container-full {
  height: calc(100vh - 320px);
  min-height: 600px;
  background: #fafafa;
}

.json-container {
  height: calc(100vh - 320px);
  min-height: 600px;
  padding: 0 16px 16px 16px;
}

.json-container .fill-height {
  height: 100%;
}

.json-container .code-textarea :deep(.v-field) {
  height: 100%;
}

.json-container .code-textarea :deep(.v-field__field) {
  height: 100%;
}

.json-container .code-textarea :deep(.v-field__input) {
  height: 100%;
  overflow-y: auto;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.code-textarea :deep(.v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
  line-height: 1.4 !important;
}

.cursor-pointer {
  cursor: pointer;
}

.hover-bg:hover {
  background-color: #f5f5f5;
}

.history-item {
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f5f5f5;
}

/* 隐藏根节点的 source handle */
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle.source),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-right),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-bottom) {
  display: none !important;
}
</style>
