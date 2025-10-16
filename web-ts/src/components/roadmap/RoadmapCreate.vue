<script setup lang="ts">
  import { computed, inject, ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { VueFlow } from '@vue-flow/core'
  import { Background } from '@vue-flow/background'
  import { Controls } from '@vue-flow/controls'
  import { type Node, type Edge, type Connection, type NodeChange, type EdgeChange, Position } from '@vue-flow/core'
  import { courseServiceV1 } from '@/services/api/v1/apiServiceV1'
  import ConfirmDialog from '@/components/roadmap/ConfirmDialog.vue'
  import type { Course } from '@/types/course'
  import dagre from 'dagre'

  const { t } = useI18n()
  const showSnackbar = inject<(message: string) => void>('showSnackbar')

  // 职业名称常量 - 根节点显示用
  const PROFESSION_NAME = t('roadmapCreate.rootNodeTitle')

  interface NodePosition {
    x: number
    y: number
  }

  type RoadmapNode = Node & {
    id: string
    type: string
    data: {
      label: string
      link: string | null
    }
    position: NodePosition
    targetPosition?:Position 
    sourcePosition?:Position 
    selectable: boolean
    draggable?: boolean
    selected?: boolean
  }

  type RoadmapEdge = Edge & {
    id: string
    source: string
    target: string
    type: string
    animated: boolean
    selectable: boolean
    selected?: boolean
  }

  interface CopiedRoadmapContent {
    nodes?: RoadmapNode[]
    edges?: RoadmapEdge[]
  }

  interface CopiedRoadmap {
    content?: string | CopiedRoadmapContent
    description?: string
  }

  interface Props {
    modelValue?: boolean
    professionId?: string
    professionName?: string
    copiedRoadmap?: CopiedRoadmap | null
  }

  interface SaveData {
    description: string
    professionId: string
    content: string
  }

  interface ValidationResult {
    valid: boolean
    message?: string
  }

  interface NodesChange {
    type: string
    id: string
    position?: NodePosition
    selected?: boolean
  }

  interface EdgesChange {
    type: string
    id: string
    selected?: boolean
  }

  interface SelectionChangeParams {
    nodes: RoadmapNode[]
    edges: RoadmapEdge[]
  }

  interface Emits {
    (e: 'update:modelValue', value: boolean): void
    (e: 'close'): void
    (e: 'save', data: SaveData): void
    (e: 'roadmap-saved'): void
  }

  const props = withDefaults(defineProps<Props>(), {
    modelValue: false,
    professionId: '1',
    professionName: '',
    copiedRoadmap: null,
  })

  const emit = defineEmits<Emits>()

  const visible = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
  })

  // 创建根节点的通用函数
  const createRootNode = (position: NodePosition = { x: 400, y: 100 }): RoadmapNode => {
    return {
      id: '0',
      type: 'default',
      data: {
        label: props.professionName || t('roadmapCreate.rootNodeTitle'),
        link: null, // 根节点不跳转
      },
      position,
      targetPosition: Position.Bottom, // 只能入，不能出
      selectable: true,
      draggable: true,
    }
  }

  // 自动布局函数
  const applyAutoLayout = (nodeList: RoadmapNode[], edgeList: RoadmapEdge[], direction = 'BT'): RoadmapNode[] => {
    const dagreGraph = new dagre.graphlib.Graph()
    dagreGraph.setDefaultEdgeLabel(() => ({}))
    dagreGraph.setGraph({
      rankdir: direction,
      nodesep: 180,
      ranksep: 80,
      marginx: 20,
      marginy: 20,
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
          y: nodeWithPosition.y - nodeHeight / 2,
        },
      }
    })
  }

  // 内部状态管理 - 确认对话框相关
  const showSaveConfirmDialog = ref<boolean>(false)
  const showResetConfirmDialog = ref<boolean>(false)
  const roadmapDescription = ref<string>('')

  // 搜索相关状态
  const searchText = ref<string>('')
  const availableCourses = ref<Course[]>([])

  // 编辑相关状态 - 内部管理
  const internalNodes = ref<RoadmapNode[]>([])
  const internalEdges = ref<RoadmapEdge[]>([])
  const showEmptyAreaTip = ref<boolean>(true)

  // 保存相关方法
  const handleSave = (): void => {
    // 先进行基本验证
    if (internalNodes.value.length === 0) {
      showSnackbar?.(t('roadmapCreate.messages.addNodesFirst'))
      return
    }

    // 确保有根节点
    const hasRootNode = internalNodes.value.some((node) => node.id === '0')
    if (!hasRootNode) {
      showSnackbar?.(t('roadmapCreate.messages.rootNodeRequired'))
      return
    }

    // 验证树结构
    const validation = validateTreeStructure()
    if (!validation.valid) {
      showSnackbar?.(`课程表结构不合法：${validation.message}`)
      return
    }

    // 显示保存确认对话框
    showSaveConfirmDialog.value = true
  }

  // 确认保存
  const confirmSave = async (description: string): Promise<void> => {
    roadmapDescription.value = description

    // 调用父组件的保存方法
    emit('save', {
      description: roadmapDescription.value.trim(),
      professionId: props.professionId,
      content: serializeRoadmapContent(),
    })

    // 关闭确认对话框
    showSaveConfirmDialog.value = false

    // 清空描述
    roadmapDescription.value = ''
  }

  // 取消保存
  const cancelSaveConfirm = (): void => {
    showSaveConfirmDialog.value = false
    roadmapDescription.value = ''
  }

  // 重置相关方法
  const handleReset = (): void => {
    if (internalNodes.value.length === 0 && internalEdges.value.length === 0) {
      showSnackbar?.(t('roadmapCreate.messages.alreadyEmpty'))
      return
    }

    showResetConfirmDialog.value = true
  }

  // 确认重置
  const confirmReset = (): void => {
    showResetConfirmDialog.value = false
    roadmapDescription.value = ''

    // 直接重置内部状态
    internalNodes.value = []
    internalEdges.value = []
    showEmptyAreaTip.value = false

    // 重置后也要添加根节点
    internalNodes.value.push(createRootNode())

    showSnackbar?.(t('roadmapCreate.messages.resetSuccess'))
  }

  // 取消重置
  const cancelReset = (): void => {
    showResetConfirmDialog.value = false
  }

  // 搜索相关方法
  const loadAvailableCourses = async (searchName = ''): Promise<void> => {
    try {
      const response = await courseServiceV1.searchCourses(searchName)
      console.log('Search courses response:', response)
      if (response.code === 200) {
        availableCourses.value = response.data || []
      } else {
        availableCourses.value = []
        showSnackbar?.(response.message || t('roadmapCreate.messages.loadCoursesError', { error: 'Search failed' }))
      }
    } catch (err: any) {
      availableCourses.value = []
      showSnackbar?.(
        t('roadmapCreate.messages.loadCoursesError', {
          error: err.message || t('roadmapCreate.messages.unknownError'),
        })
      )
    }
  }

  const onSearchCourses = (): void => {
    if (!searchText.value.trim()) {
      availableCourses.value = []
      return
    }
    loadAvailableCourses(searchText.value.trim())
  }

  // 编辑相关方法
  const addCourseNode = (course: Course): void => {
    const node = internalNodes.value.find((node) => node.id === course.id.toString())
    if (node) {
      return
    }

    // 计算新节点位置，避免与现有节点重叠
    let newX = 100
    let newY = 100

    // 如果已有节点，在最右侧和最下方找空位
    if (internalNodes.value.length > 0) {
      const maxX = Math.max(...internalNodes.value.map((node) => node.position.x))
      const maxY = Math.max(...internalNodes.value.map((node) => node.position.y))

      // 新节点放在右侧，如果右侧空间不够则换行
      newX = maxX + 200 // 节点间距离
      newY = 100

      // 如果X坐标超出合理范围，换到下一行
      if (newX > 600) {
        newX = 100
        newY = maxY + 100 // 行间距离
      }
    }

    const newNode: RoadmapNode = {
      id: course.id.toString(), // 确保 id 是字符串
      type: 'default', // 使用默认节点类型
      data: {
        label: course.name,
        link: `/read?courseId=${course.id}`,
      },
      position: { x: newX, y: newY },
      sourcePosition: Position.Top, // 只能出，不能入
      targetPosition: Position.Bottom,
      selectable: true,
    }
    internalNodes.value.push(newNode)
    showEmptyAreaTip.value = false // 隐藏空区域提示
  }

  // 处理节点连接事件
  const onConnect = (connection: Connection): void => {
    const newEdge: RoadmapEdge = {
      id: `${connection.source}-${connection.target}`,
      source: connection.source,
      target: connection.target,
      type: 'bezier',
      animated: true,
      selectable: true,
    }
    internalEdges.value.push(newEdge)
  }

  const onNodesChange = (changes: NodeChange[]): void => {
    changes.forEach((change) => {
      if (change.type === 'position') {
        if (change.position) {
          // 更新节点位置
          const node = internalNodes.value.find((node) => node.id === change.id)
          if (node) {
            node.position = change.position
          }
        }
      } else if (change.type === 'select') {
        // 处理节点选择状态变化
        const node = internalNodes.value.find((node) => node.id === change.id)
        if (node) {
          node.selected = change.selected
        }
      } else if (change.type === 'remove') {
        // 删除节点
        deleteNode(change.id)
      }
    })
  }

  // 删除边的事件处理
  const onEdgesChange = (changes: EdgeChange[]): void => {
    changes.forEach((change) => {
      if (change.type === 'select') {
        // 处理边选择状态变化
        const edge = internalEdges.value.find((edge) => edge.id === change.id)
        if (edge) {
          edge.selected = change.selected
        }
      } else if (change.type === 'remove') {
        // 删除边
        deleteEdge(change.id)
      }
    })
  }

  // 删除节点的函数
  const deleteNode = (nodeId: string): void => {
    // 不允许删除根节点
    if (nodeId === '0' || nodeId === '0') {
      showSnackbar?.(t('roadmapCreate.messages.cannotDeleteRoot'))
      return
    }

    // 删除节点
    internalNodes.value = internalNodes.value.filter((node) => node.id !== nodeId)

    // 删除与该节点相关的边
    internalEdges.value = internalEdges.value.filter(
      (edge) => edge.source !== nodeId && edge.target !== nodeId
    )
  }

  // 删除边的函数
  const deleteEdge = (edgeId: string): void => {
    // 删除边
    internalEdges.value = internalEdges.value.filter((edge) => edge.id !== edgeId)
  }

  // 删除选中的节点和边
  const deleteSelected = (): void => {
    // 找到所有选中的节点
    const selectedNodes = internalNodes.value.filter((node) => node.selected)
    // 找到所有选中的边
    const selectedEdges = internalEdges.value.filter((edge) => edge.selected)

    if (selectedNodes.length === 0 && selectedEdges.length === 0) {
      return
    }

    // 删除选中的节点
    selectedNodes.forEach((node) => {
      deleteNode(node.id)
    })

    // 删除选中的边
    selectedEdges.forEach((edge) => {
      deleteEdge(edge.id)
    })
  }

  // 处理选择变化事件
  const onSelectionChange = ({ nodes, edges }: SelectionChangeParams): void => {
    // 更新节点的选中状态
    internalNodes.value.forEach((node) => {
      node.selected = nodes.some((selectedNode) => selectedNode.id === node.id)
    })

    // 更新边的选中状态
    internalEdges.value.forEach((edge) => {
      edge.selected = edges.some((selectedEdge) => selectedEdge.id === edge.id)
    })
  }

  const recalculateLayout = (): void => {
    if (internalNodes.value.length === 0) {
      return
    }

    // 调用 applyAutoLayout 函数重新计算布局
    const layoutedNodes = applyAutoLayout(
      internalNodes.value,
      internalEdges.value,
      'BT' // 默认方向为从上到下
    )

    // 更新节点位置
    internalNodes.value = layoutedNodes
  }

  // 验证课程表是否形成一棵有效的树
  const validateTreeStructure = (): ValidationResult => {
    const nodes = internalNodes.value
    const edges = internalEdges.value

    if (nodes.length === 0) {
      return { valid: false, message: t('roadmapCreate.validation.emptyRoadmap') }
    }

    // 单个节点的情况是有效的树
    if (nodes.length === 1) {
      return { valid: true }
    }

    // 对于有多个节点的情况，需要检查是否形成树结构
    if (edges.length !== nodes.length - 1) {
      return {
        valid: false,
        message: `树结构需要有 ${nodes.length - 1} 条边，但当前有 ${edges.length} 条边`,
      }
    }

    // 检查是否有环
    const nodeIds = nodes.map((node) => node.id)
    const adjacencyList = new Map<string, string[]>()

    // 初始化邻接表
    nodeIds.forEach((id) => {
      adjacencyList.set(id, [])
    })

    // 构建邻接表
    edges.forEach((edge) => {
      adjacencyList.get(edge.source)?.push(edge.target)
      adjacencyList.get(edge.target)?.push(edge.source)
    })

    // 使用DFS检查连通性和环
    const visited = new Set<string>()
    const parent = new Map<string, string | null>()

    const hasCycle = (nodeId: string, parentId: string | null): boolean => {
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
    const [startNode] = nodeIds
    if (hasCycle(startNode, null)) {
      return { valid: false, message: t('roadmapCreate.validation.circularDependency') }
    }

    // 检查是否所有节点都被访问到（连通性）
    if (visited.size !== nodes.length) {
      return { valid: false, message: t('roadmapCreate.validation.disconnectedGraph') }
    }

    return { valid: true }
  }

  // 序列化课程表内容为指定格式
  const serializeRoadmapContent = (): string => {
    // 获取所有边，格式为 [[source, target], [source, target], ...]
    const edgeArray = internalEdges.value
      .map((edge) => {
        const source = parseInt(edge.source)
        const target = parseInt(edge.target)

        // 验证节点ID是否有效
        if (isNaN(source) || isNaN(target)) {
          return null
        }

        return [source, target]
      })
      .filter((edge): edge is [number, number] => edge !== null) // 过滤掉无效边

    // 获取所有节点ID，格式为 [id1, id2, id3, ...]，包括根节点id=0
    const nodeArray = internalNodes.value
      .map((node) => {
        const id = parseInt(node.id)

        // 验证节点ID是否有效（包括id=0的根节点）
        if (isNaN(id)) {
          return null
        }

        return id
      })
      .filter((id): id is number => id !== null) // 过滤掉无效ID

    // 返回嵌套数组格式：[edges, nodes]
    const content = [edgeArray, nodeArray]

    return JSON.stringify(content)
  }

  // 初始化编辑区域
  const initializeEditor = (): void => {
    // 如果有复制的课程表数据，优先使用
    if (props.copiedRoadmap && props.copiedRoadmap.content) {
      try {
        const content =
          typeof props.copiedRoadmap.content === 'string'
            ? JSON.parse(props.copiedRoadmap.content)
            : props.copiedRoadmap.content

        // 检查是否是 {nodes: [], edges: []} 格式
        if (content.nodes && content.edges) {
          // 重建节点数据
          internalNodes.value = content.nodes.map((node: any): RoadmapNode => {
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
                draggable: true,
              }
            }
          })

          // 重建边数据
          internalEdges.value = content.edges.map((edge: any): RoadmapEdge => ({
            ...edge,
            id: edge.id || `${edge.source}-${edge.target}`,
            source: edge.source.toString(),
            target: edge.target.toString(),
            selectable: true,
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
          showSnackbar?.(t('roadmapCreate.messages.copySuccess'))
          return
        }
      } catch {
        showSnackbar?.(t('roadmapCreate.messages.copyFailed'))
      }
    }

    // 默认添加根节点（id=0）
    if (internalNodes.value.length === 0) {
      internalNodes.value.push(createRootNode())
      showEmptyAreaTip.value = false
    }
  }

  // 监听弹窗打开，初始化编辑器
  watch(
    () => props.modelValue,
    (newValue: boolean) => {
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
    }
  )
</script>

<template>
  <v-dialog v-model="visible" fullscreen :scrim="true" transition="dialog-bottom-transition">
    <v-card>
      <v-toolbar color="grey-lighten-5" flat class="flat-toolbar" density="compact">
        <v-toolbar-title class="grey-teal-darken-2">
          <v-icon class="mr-1" size="small" color="grey-darken-2">mdi-book</v-icon>
          {{ t('roadmapCreate.title') }}
        </v-toolbar-title>
        <v-spacer></v-spacer>
        <v-btn icon variant="flat" class="flat-icon-button" @click="$emit('close')">
          <v-icon color="teal-darken-1">mdi-close</v-icon>
        </v-btn>
      </v-toolbar>

      <v-container fluid class="pa-0 full-height-container">
        <v-row no-gutters class="full-height">
          <!-- 左侧课程搜索区域 -->
          <v-col cols="3" class="pt-2 search-sidebar">
            <v-card flat class="full-height">
              <v-card-text>
                <div class="d-flex align-center mb-2">
                  <v-text-field
                    v-model="searchText"
                    :label="t('roadmapCreate.searchCourses')"
                    append-inner-icon="mdi-magnify"
                    variant="outlined"
                    density="compact"
                    hide-details
                    class="flat-input"
                    color="teal-darken-1"
                    @click:append-inner="onSearchCourses"
                    @keyup.enter="onSearchCourses"
                  >
                  </v-text-field>
                </div>

                <v-list density="compact" class="course-list-container">
                  <v-list-item
                    v-for="course in availableCourses"
                    :key="course.id"
                    class="create-list-item d-flex align-center"
                    variant="flat"
                    @click="addCourseNode(course)"
                  >
                    <template #prepend>
                      <v-icon color="grey-darken-4">mdi-plus-circle</v-icon>
                    </template>
                    <v-list-item-title class="text-grey-darken-4 py-1">{{
                      course.name
                    }}</v-list-item-title>
                  </v-list-item>
                </v-list>

                <!-- 无搜索结果提示 -->
                <v-alert
                  v-if="availableCourses.length === 0 && searchText.trim()"
                  color="grey-darken-1"
                  type="info"
                  variant="tonal"
                  density="compact"
                >
                  {{ t('roadmapCreate.noCoursesFound') }}
                </v-alert>

                <!-- 初始状态提示 -->
                <v-alert
                  v-if="availableCourses.length === 0 && !searchText.trim()"
                  color="grey-darken-1"
                  type="info"
                  variant="tonal"
                  density="compact"
                >
                  {{ t('roadmapCreate.searchPrompt') }}
                </v-alert>

                <!-- 有搜索结果时的提示 -->
                <v-alert
                  v-if="availableCourses.length > 0"
                  color="grey-darken-1"
                  type="info"
                  variant="tonal"
                  density="compact"
                >
                  {{ t('roadmapCreate.addCourseHint') }}
                </v-alert>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- 右侧可编辑流程图区域 -->
          <v-col cols="9">
            <v-card flat class="full-height">
              <v-card-title class="pt-4 d-flex justify-end align-center text-teal-darken-3">
                <div class="d-flex align-center">
                  <div class="d-flex align-center border-e pr-4">
                    <v-btn
                      color="red-darken-1"
                      variant="tonal"
                      class="flat-button mr-2"
                      @click="deleteSelected"
                    >
                      <v-icon class="me-1" left>mdi-delete</v-icon>
                      {{ t('roadmapCreate.deleteSelected') }}
                    </v-btn>
                    <v-btn
                      color="blue-darken-4"
                      variant="tonal"
                      class="flat-button mr-2"
                      @click="handleReset"
                    >
                      <v-icon class="me-1" left>mdi-refresh</v-icon>
                      {{ t('roadmapCreate.reset') }}
                    </v-btn>
                    <v-btn
                      color="blue-darken-4"
                      variant="tonal"
                      class="flat-button"
                      @click="recalculateLayout"
                    >
                      <v-icon class="me-1" left>mdi-autorenew</v-icon>
                      {{ t('roadmapCreate.autoLayout') }}
                    </v-btn>
                  </div>
                  <v-btn
                    color="teal-darken-1"
                    variant="tonal"
                    class="flat-button ml-4"
                    @click="handleSave"
                  >
                    <v-icon class="me-1" left>mdi-content-save</v-icon>
                    {{ t('roadmapCreate.saveWithDescription') }}
                  </v-btn>
                </div>
              </v-card-title>

              <v-card-text class="pa-0 flow-container">
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
                  @selection-change="onSelectionChange"
                >
                  <Background pattern-color="#999" :gap="20" />
                  <Controls />
                </VueFlow>

                <!-- 空区域提示浮层 -->
                <div v-show="internalNodes.length === 1" class="empty-area-tip">
                  <div class="tip-content">
                    <v-icon size="48" color="teal-lighten-2" class="mb-3"
                      >mdi-vector-arrange-above</v-icon
                    >
                    <h3 class="text-teal-darken-1 mb-4">{{ t('roadmapCreate.editorTitle') }}</h3>
                    <p class="text-grey-darken-1 mb-2">
                      {{ t('roadmapCreate.editorDescription') }}
                    </p>
                    <p class="text-grey-darken-1 mb-0">
                      {{ t('roadmapCreate.editorInstructions') }}
                    </p>
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

  .full-height-container {
    height: calc(100vh - 64px);
  }

  .full-height {
    height: 100%;
  }

  .search-sidebar {
    border-right: 1px solid #eee;
  }

  .flow-container {
    height: calc(100% - 80px);
    position: relative;
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
  :deep(.vue-flow__node[data-id='0']) {
    background: #4f87a0 !important;
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
