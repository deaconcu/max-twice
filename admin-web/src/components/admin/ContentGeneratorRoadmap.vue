<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { VueFlow, useVueFlow, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import type { Node, Edge } from '@vue-flow/core'
import dagre from 'dagre'
import { adminApi } from '@/api'
import { useMutation } from '@/composables'

// 引入样式
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

// Vue Flow 实例
const { fitView } = useVueFlow()

// 职业选择
const professionId = ref<string>('')

// 任务状态
const taskId = ref<string>('')
const taskStatus = ref<string>('')
const generatedContent = ref<string>('')
const taskError = ref<string>('')

// Vue Flow 节点和边
const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])

// 历史记录
const historyList = ref<any[]>([])
const loadingHistory = ref(false)

// 选中的节点
const selectedNode = ref<Node | null>(null)
const editDialog = ref(false)

// 历史记录弹窗
const historyDialog = ref(false)

// 监听弹窗打开，自动加载历史记录
watch(historyDialog, (newValue) => {
  if (newValue) {
    loadHistory()
  }
})

// Tab 切换
const resultTab = ref<'graph' | 'json'>('graph')

// 搜索相关
const searchKeyword = ref<string>('')
const searchResults = ref<any[]>([])
const searchLoading = ref(false)

// 节点编辑
const mappedId = ref<string>('')
const createNew = ref(true)

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
    const id = parseInt(professionId.value, 10)
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
    const data = JSON.parse(resultJson)

    // 转换节点
    nodes.value = data.nodes.map((node: any) => ({
      id: node.id,
      type: 'default',
      data: {
        label: node.name,
        nodeType: node.type,
        description: node.description,
        status: 'pending',  // pending, matched, confirmed
        mappedId: null,
        createNew: true,
      },
      position: { x: 0, y: 0 },
      sourcePosition: Position.Top,    // 从下往上，source在上方
      targetPosition: Position.Bottom, // target在下方
      style: node.type === 'course' ? COURSE_NODE_STYLE : NODE_NODE_STYLE,
    }))

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
    }, 100)
  } catch (error) {
    console.error('解析路径失败:', error)
  }
}

// 节点点击事件
const onNodeClick = (event: any) => {
  console.log('节点被点击:', event)
  const node = nodes.value.find(n => n.id === event.node.id)
  console.log('找到的节点:', node)
  if (node && node.id !== 'temp-0') {  // 根节点不可编辑
    selectedNode.value = node
    mappedId.value = node.data.mappedId || ''
    createNew.value = node.data.createNew !== false
    searchKeyword.value = ''
    searchResults.value = []
    editDialog.value = true
    console.log('打开编辑对话框')
  }
}

// 搜索课程/节点
const searchContent = async () => {
  if (!searchKeyword.value.trim()) return

  searchLoading.value = true
  try {
    // TODO: 调用搜索 API
    // const response = await adminApi.searchCoursesAndNodes(searchKeyword.value, selectedNode.value?.data.nodeType)
    // searchResults.value = response.data || []
    searchResults.value = []  // 暂时为空
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    searchLoading.value = false
  }
}

// 选择匹配项
const selectMatch = (item: any) => {
  if (selectedNode.value) {
    mappedId.value = item.id.toString()
    createNew.value = false
  }
}

// 保存节点映射
const saveNodeMapping = () => {
  if (!selectedNode.value) return

  selectedNode.value.data.mappedId = mappedId.value ? parseInt(mappedId.value) : null
  selectedNode.value.data.createNew = createNew.value

  // 更新节点状态和样式
  if (!createNew.value && mappedId.value) {
    selectedNode.value.data.status = 'matched'
    selectedNode.value.style = {
      ...selectedNode.value.style,
      border: '2px solid #4CAF50',
    }
  } else {
    selectedNode.value.data.status = 'confirmed'
    selectedNode.value.style = {
      ...selectedNode.value.style,
      border: '2px solid #FF9800',
    }
  }

  editDialog.value = false
}

// 自动布局
const applyAutoLayout = () => {
  console.log('applyAutoLayout 被调用, nodes数量:', nodes.value.length)

  if (nodes.value.length <= 1) {
    console.log('节点数量不足，跳过布局')
    return
  }

  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({
    rankdir: 'BT', // Bottom to Top (从下往上)
    nodesep: 100,   // 水平间距（减小）
    ranksep: 50,    // 垂直间距（减小）
    marginx: 20,
    marginy: 20,
  })

  const nodeWidth = 120
  const nodeHeight = 40

  // 添加节点（关键：使用 toString()）
  nodes.value.forEach((node) => {
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })

  // 添加边（关键：使用 toString()）
  edges.value.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)
  console.log('dagre 布局计算完成')

  // 更新节点位置
  nodes.value.forEach((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    node.position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    }
  })

  // 关键：重新赋值整个数组以触发响应式更新
  nodes.value = [...nodes.value]

  console.log('所有节点位置已更新')

  // 布局完成后，调用 fitView 聚焦到所有节点
  nextTick(() => {
    fitView({ padding: 0.2, duration: 300 })
  })
}

const startGenerate = () => {
  if (!professionId.value) {
    return
  }
  generateRoadmap()
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
    taskStatus.value = ''
    historyDialog.value = false // 关闭弹窗
  }
}

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
        <v-btn icon variant="text" size="small" @click="historyDialog = true">
          <v-icon icon="mdi-history"></v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">
          为指定职业生成完整的学习路径，AI 将自动设计从基础到高级的课程结构
        </p>

        <v-row class="align-center">
          <v-col cols="12" md="3">
            <v-text-field
              v-model="professionId"
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
              :disabled="!professionId"
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
    <v-card v-if="nodes.length > 0" flat class="border mb-4">
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

        <!-- 自动布局按钮（仅图表视图显示） -->
        <v-btn v-if="resultTab === 'graph'" variant="tonal" size="small" @click="applyAutoLayout">
          <v-icon icon="mdi-auto-fix" size="16" class="mr-1"></v-icon>
          自动布局
        </v-btn>
      </v-card-title>

      <v-card-text class="pa-0">
        <!-- 图表编辑器 -->
        <div v-show="resultTab === 'graph'" class="flow-container-full">
          <VueFlow
            :nodes="nodes"
            :edges="edges"
            :min-zoom="0.5"
            :max-zoom="2"
            fit-view-on-init
            :snap-to-grid="true"
            :snap-grid="[20, 20]"
            @node-click="onNodeClick"
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
            readonly
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
            <!-- 节点信息 -->
            <div class="mb-4 pa-3 bg-grey-lighten-4 rounded">
              <div class="text-body-2 font-weight-bold mb-1">{{ selectedNode.data.label }}</div>
              <div class="text-caption text-grey mb-2">
                类型: {{ selectedNode.data.nodeType === 'course' ? '课程' : '节点' }}
              </div>
              <div class="text-body-2">{{ selectedNode.data.description }}</div>
            </div>

            <!-- 操作选择 -->
            <v-radio-group v-model="createNew" class="mb-4">
              <v-radio :value="true" label="创建新的课程/节点"></v-radio>
              <v-radio :value="false" label="映射到已有课程/节点"></v-radio>
            </v-radio-group>

            <!-- 搜索区域 -->
            <div v-if="!createNew" class="mb-4">
              <v-text-field
                v-model="searchKeyword"
                label="搜索课程或节点"
                variant="outlined"
                density="compact"
                hide-details
                class="mb-2"
                append-inner-icon="mdi-magnify"
                @click:append-inner="searchContent"
                @keyup.enter="searchContent"
              ></v-text-field>

              <!-- 搜索结果 -->
              <div v-if="searchResults.length > 0" class="mt-2">
                <div
                  v-for="item in searchResults"
                  :key="item.id"
                  class="pa-2 border-b cursor-pointer hover-bg"
                  @click="selectMatch(item)"
                >
                  <div class="text-body-2 font-weight-medium">{{ item.name }}</div>
                  <div class="text-caption text-grey">ID: {{ item.id }}</div>
                </div>
              </div>

              <!-- 或手动输入ID -->
              <v-text-field
                v-model="mappedId"
                label="或直接输入课程/节点 ID"
                type="number"
                variant="outlined"
                density="compact"
                hide-details
                class="mt-3"
              ></v-text-field>
            </div>
          </div>
        </v-card-text>
        <v-card-actions class="bg-grey-lighten-5">
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="editDialog = false">取消</v-btn>
          <v-btn variant="tonal" color="primary" @click="saveNodeMapping">保存</v-btn>
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
                职业 ID: {{ item.professionId }} - {{ item.createdAt }}
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
  padding: 16px;
}

.json-container .fill-height {
  height: 100%;
}

.json-container :deep(.v-field) {
  height: 100%;
}

.json-container :deep(.v-field__field) {
  height: 100%;
}

.json-container :deep(.v-field__input) {
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
</style>
