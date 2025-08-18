

<script setup>
import { ref, nextTick } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import dagre from 'dagre'

// 使用 useVueFlow 组合式函数
const { 
  nodes, 
  edges, 
  addNodes, 
  addEdges, 
  onConnect: onConnectEvent, 
  onInit,
  fitView,
  updateNode
} = useVueFlow()

const layoutDirection = ref('TB')
let nodeIdCounter = 6

// 自动布局函数 - 完全由算法生成位置
const applyAutoLayout = (nodeList, edgeList, direction = 'TB') => {
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setDefaultEdgeLabel(() => ({}))
  dagreGraph.setGraph({ 
    rankdir: direction,
    nodesep: 180,
    ranksep: 60,
    marginx: 20,
    marginy: 20
  })

  const nodeWidth = 172
  const nodeHeight = 36

  // 添加节点到图中，不需要预设位置
  nodeList.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  })

  // 添加边到图中
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  // 执行布局算法
  dagre.layout(dagreGraph)

  // 获取算法计算的位置并应用到节点
  const layoutedNodes = nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id)
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      }
    }
  })

  return layoutedNodes
}

// 初始化
onInit(async () => {
  // 初始节点 - 不设置position，完全由布局算法决定
  const initialNodes = [
    { id: '1', type: 'input', data: { label: '开始节点' } },
    { id: '2', data: { label: '处理步骤处理步骤1处理步骤1处理步骤11' } },
    { id: '3', data: { label: '处理步骤2' } },
    { id: '4', data: { label: '判断条件' } },
    { id: '5', type: 'output', data: { label: '结束节点' } }
  ]

  const initialEdges = [
    { id: 'e1-2', source: '1', target: '2', type:'bezier', animated: true },
    { id: 'e2-3', source: '2', target: '3', type:'bezier', animated: true  },
    { id: 'e3-4', source: '3', target: '4', type:'bezier', animated: true  },
    { id: 'e4-5', source: '4', target: '5', type:'bezier', animated: true  },
    { id: 'e1-6', source: '1', target: '3', type:'bezier', animated: true },
    { id: 'e1-7', source: '1', target: '4', type:'bezier', animated: true },
    { id: 'e1-8', source: '1', target: '5', type:'bezier', animated: true },
  ]

  // 应用自动布局
  const layoutedNodes = applyAutoLayout(initialNodes, initialEdges, layoutDirection.value)
  
  // 添加到流程图
  addNodes(layoutedNodes)
  addEdges(initialEdges)
  
  await nextTick()
  fitView()
})

// 添加新节点 - 不设置位置，完全由布局算法决定
const addRandomNode = async () => {
  const newNodeId = String(nodeIdCounter++)
  const newNode = {
    id: newNodeId,
    data: { label: `新节点 ${newNodeId}` }
    // 注意：这里没有position属性，位置完全由布局算法决定
  }
  
  // 添加新节点
  addNodes([newNode])
  
  // 随机连接到已有节点
  if (nodes.value.length > 1) {
    const existingNodes = nodes.value.filter(node => node.id !== newNodeId)
    const randomExistingNode = existingNodes[Math.floor(Math.random() * existingNodes.length)]
    const newEdge = {
      id: `e${randomExistingNode.id}-${newNodeId}`,
      source: randomExistingNode.id,
      target: newNodeId
    }
    
    addEdges([newEdge])
  }
  
  // 重新应用布局
  await autoLayout()
}

// 重新布局
const autoLayout = async () => {
  const layoutedNodes = applyAutoLayout(nodes.value, edges.value, layoutDirection.value)
  
  // 更新每个节点的位置
  layoutedNodes.forEach((layoutedNode) => {
    updateNode(layoutedNode.id, {
      position: layoutedNode.position
    })
  })
  
  await nextTick()
  fitView()
}

// 连接节点回调
const handleConnect = (connection) => {
  const newEdge = {
    id: `e${connection.source}-${connection.target}`,
    source: connection.source,
    target: connection.target,
    animated: true 
  }
  
  addEdges([newEdge])
  
  // 连接后重新布局
  nextTick(() => {
    autoLayout()
  })
}

// 监听连接事件
onConnectEvent(handleConnect)

// 布局方向改变时重新布局
const onLayoutDirectionChange = () => {
  autoLayout()
}
</script>

<template>
  <div class="flow-container">
    <div class="controls">
      <button @click="addRandomNode" class="control-button">
        添加节点
      </button>
      <button @click="autoLayout" class="control-button">
        重新布局
      </button>
      <select 
        v-model="layoutDirection" 
        @change="onLayoutDirectionChange"
        class="control-select"
      >
        <option value="TB">上下布局</option>
        <option value="LR">左右布局</option>
        <option value="BT">下上布局</option>
        <option value="RL">右左布局</option>
      </select>
    </div>
    
    <div class="vue-flow-wrapper">
      <VueFlow 
        fit-view-on-init
        :min-zoom="1.5"
        :max-zoom="1.8"
        snap-to-grid
        :snap-grid="[20, 20]"
      >
        <Background 
          pattern-color="#aaa" 
          :gap="20"
        />
        <Controls />
      </VueFlow>
    </div>
  </div>
</template>

<style>
/* 导入必要的 Vue Flow 样式 */
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';

.flow-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.controls {
  padding: 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.control-button {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s ease;
}

.control-button:hover {
  background: #0056b3;
}

.control-button:active {
  transform: translateY(1px);
}

.control-select {
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 6px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease;
}

.control-select:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.vue-flow-wrapper {
  flex: 1;
  position: relative;
}

/* 自定义节点样式 */
.vue-flow__node {
  padding:4px 8px;
  width: 150px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.2s ease;
  display: inline-block;
}

.vue-flow__node:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.vue-flow__node.selected {
  box-shadow: 0 0 0 2px #007bff;
}

/* 自定义边样式 */
.vue-flow__edge.animated .vue-flow__edge-path {
  stroke-dasharray: 5;
  animation: dashdraw 0.5s linear infinite;
}

@keyframes dashdraw {
  to {
    stroke-dashoffset: -10;
  }
}

/* 控制面板样式优化 */
.vue-flow__controls {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.vue-flow__controls-button {
  border: none;
  background: white;
  transition: background-color 0.2s ease;
}

.vue-flow__controls-button:hover {
  background: #f8f9fa;
}
</style>
