<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dagre from 'dagre'
import { professionServiceV1, roadmapServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'
import { ObjectType, VoteType } from '@/types/enums'
import { useRoute } from 'vue-router'
import RoadmapDetail from '@/components/roadmap/RoadmapDetail.vue'
import RoadmapCreate from '@/components/roadmap/RoadmapCreate.vue'
import RoadmapHeader from '@/components/roadmap/RoadmapHeader.vue'
import RoadmapList from '@/components/roadmap/RoadmapList.vue'
import RightSidebar from '@/components/common/RightSidebar.vue'
import ErrorPage from '@/components/common/ErrorPage.vue'
import type { Roadmap } from '@/types/roadmap'
import type { Profession } from '@/types/profession'
import type { FlowNode, FlowEdge } from '@/types/flow'
import { Position } from '@vue-flow/core'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'

// 使用全局流程图类型的别名
type Node = FlowNode
type Edge = FlowEdge

const { t } = useI18n()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

// 状态管理
const roadmaps: Ref<Roadmap[]> = ref([])
const profession: Ref<Profession | null> = ref(null)
const error: Ref<string | null> = ref(null)
const errorCode: Ref<number | null> = ref(null)
const showModal: Ref<boolean> = ref(false)
const selectedRoadmap: Ref<Roadmap | null> = ref(null)
const layoutDirection: Ref<string> = ref('BT')
const showCreateModal: Ref<boolean> = ref(false)
const copiedRoadmapData: Ref<Roadmap | null> = ref(null)

// 路由参数
const route = useRoute()
const professionId: Ref<number> = ref(Number(route.params.professionId) || 1)

// 自动布局函数
const applyAutoLayout = (nodeList: Node[], edgeList: Edge[], direction: string = 'BT'): Node[] => {
  console.log('Applying auto layout with direction:', direction)
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
    dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
  })
  edgeList.forEach((edge) => {
    dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
  })

  // 计算布局
  dagre.layout(dagreGraph)

  // 更新节点位置
  return nodeList.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id.toString())
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      },
    }
  })
}

// 解析 content 字段
const parseContent = (content: string | object): { nodes: Node[]; edges: Edge[] } => {
  try {
    const data = typeof content === 'string' ? JSON.parse(content) : content
    const nodes = (data.nodes || []).map((node: any): Node => {
      // 如果是根节点（id=0），设置为职业名称且只能入不能出
      if (node.id === 0 || node.id === '0') {
        return {
          id: node.id,
          type: 'default',
          data: {
            label: profession.value?.name || '当前职业', // 使用动态职业名称
            link: null, // 根节点不跳转
            ...node.data,
          },
          position: node.position || { x: 0, y: 0 },
          targetPosition: Position.Bottom, // 根节点只能入，不能出
        }
      }

      return {
        id: node.id,
        type: 'default', // 使用默认节点类型
        data: {
          label: node.name, // 使用 name 字段
          link: `/read?courseId=${node.id}`, // 默认链接
          ...node.data,
        },
        position: node.position || { x: 0, y: 0 },
        sourcePosition: Position.Top, // source 在上面
        targetPosition: Position.Bottom, // target 在下面
      }
    })

    const edges = (data.edges || []).map((edge: any): Edge => ({
      id: `${edge.source}-${edge.target}`,
      source: edge.source,
      target: edge.target,
      type: edge.type || 'bezier',
      animated: edge.animated || true,
      label: edge.label,
      // 移除内联样式，让CSS接管样式控制
    }))

    return { nodes, edges }
  } catch (err) {
    console.error('解析课程内容失败:', err)
    return { nodes: [], edges: [] }
  }
}

// 加载课程数据
// 使用 useFetch 加载职业和路线图数据
const {
  loading,
  execute: loadRoadmaps,
} = useFetch<{ profession: Profession; roadmaps: Roadmap[] }>({
  fetchFn: async () => {
    // 并行获取职业信息和路线图数据
    const [professionResponse, roadmapResponse] = await Promise.all([
      professionServiceV1.getProfession(professionId.value),
      roadmapServiceV1.getProfessionRoadmaps(professionId.value),
    ])

    // 检查职业信息响应
    if (professionResponse.code !== 200) {
      errorCode.value = professionResponse.code
      error.value = professionResponse.message || '获取职业信息失败'
      throw new Error(error.value)
    }

    return {
      profession: professionResponse.data,
      roadmaps: roadmapResponse.data || roadmapResponse,
    }
  },
  immediate: true,
  onSuccess: (data) => {
    // 设置职业信息
    profession.value = data.profession

    // 处理路线图数据
    const roadmapData = data.roadmaps
    if (!Array.isArray(roadmapData)) {
      throw new Error('返回的数据格式不正确')
    }

    roadmaps.value = roadmapData.map((roadmap: any): Roadmap => {
      const { nodes, edges } = parseContent(roadmap.content)
      const layoutedNodes = applyAutoLayout(nodes, edges, layoutDirection.value)

      return {
        ...roadmap,
        nodes: layoutedNodes,
        edges,
        votes: roadmap.votes || Math.floor(Math.random() * 100),
      }
    })
  },
  onError: (err: any) => {
    console.error('加载课程数据失败:', err)
    error.value = `加载课程数据失败: ${err.message || '未知错误'}`
  },
})

// 弹窗控制
const openModal = (roadmap: Roadmap): void => {
  selectedRoadmap.value = roadmap
  showModal.value = true
}

const closeModal = (): void => {
  showModal.value = false
  selectedRoadmap.value = null
}

// 创建新课程表相关
const openCreateModal = (): void => {
  showCreateModal.value = true
}

const closeCreateModal = (): void => {
  showCreateModal.value = false
  // 清空复制的课程表数据
  copiedRoadmapData.value = null
}

// 计算总学习人数
const getTotalLearners = (): number => {
  // TODO
  return 199
  /*
  return roadmaps.value.reduce((total, roadmap) => {
    return total + (roadmap.learners || Math.floor(Math.random() * 1000) + 100)
  }, 0)
  */
}

// 复制课程表到创建页面
const copyRoadmapToCreate = (roadmap: Roadmap): void => {
  // 设置要复制的课程表数据
  copiedRoadmapData.value = roadmap

  // 打开创建课程表弹窗
  showCreateModal.value = true

  showSnackbar(`正在复制课程表 "${roadmap.description || '未命名'}" 到编辑区域`)
}

// 保存课程表功能
const saveRoadmap = async (saveData: { description: string; content: any }): Promise<void> => {
  if (!saveData || !saveData.description) {
    showSnackbar(t('roadmap.fillDescription'))
    return
  }

  // 执行保存
  await performSave(saveData.description, saveData.content)
}

// 执行实际的保存操作
const performSave = async (description: string, content: any): Promise<void> => {
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

      // 关闭创建对话框
      showCreateModal.value = false

      // 重新加载课程表列表
      await loadRoadmaps()
    } else {
      // 如果 code 不是 200，视为保存失败
      const errorMessage = response.message || t('roadmap.saveFailed')
      console.error('保存课程表失败，服务器响应码:', response.code, '错误信息:', errorMessage)
      showSnackbar(`${t('roadmap.saveFailed')}：${errorMessage}`)
    }
  } catch (error: any) {
    console.error('保存课程表失败:', error)
    showSnackbar(`${t('roadmap.saveFailed')}：${error.response?.data?.message || error.message}`)
  }
}

// 监听路由参数变化
watch(
  () => route.params.professionId,
  (newId) => {
    if (newId) {
      professionId.value = Number(newId)
      loadRoadmaps()
    }
  }
)

const handleNodeClick = ({ node }: { node: Node }): void => {
  // 根节点不跳转
  if (node.id === '0') {
    return
  }

  if (node.data.link) {
    window.open(node.data.link, '_blank') // 打开链接
  }
}

// 处理子组件的更新事件
const handleUpdateRoadmap = (roadmapId: string | number, updates: Partial<Roadmap>): void => {
  const roadmap = roadmaps.value.find((r) => r.id === roadmapId)
  if (roadmap) {
    Object.assign(roadmap, updates)
  }
}

// 处理置顶状态变更
const handleRoadmapsUpdated = async (roadmapId?: string | number, pinned?: boolean): Promise<void> => {
  if (!roadmapId || pinned === undefined) {
    // 如果没有参数，只重新加载
    await loadRoadmaps()
    return
  }

  const roadmap = roadmaps.value.find((r) => r.id === roadmapId)
  if (roadmap) {
    if (pinned) {
      // 置顶：更新状态并移动到第一个位置
      roadmap.pinned = true
      const index = roadmaps.value.findIndex((r) => r.id === roadmapId)
      if (index > -1 && index !== 0) {
        const [pinnedRoadmap] = roadmaps.value.splice(index, 1)
        roadmaps.value.unshift(pinnedRoadmap)
      }
    } else {
      // 取消置顶：更新状态并移动到非置顶区第一个
      roadmap.pinned = false
      const currentIndex = roadmaps.value.findIndex((r) => r.id === roadmapId)

      if (currentIndex > -1) {
        const [unpinnedRoadmap] = roadmaps.value.splice(currentIndex, 1)

        // 找到第一个非置顶路线图的位置
        const firstUnpinnedIndex = roadmaps.value.findIndex((r) => !r.pinned)
        const insertIndex = firstUnpinnedIndex > -1 ? firstUnpinnedIndex : roadmaps.value.length
        roadmaps.value.splice(insertIndex, 0, unpinnedRoadmap)
      }
    }
  }
}

// 使用 useMutation 处理投票
const { execute: handleVote } = useMutation(
  (roadmapId: number) => upvoteServiceV1.upvote(roadmapId, ObjectType.ROADMAP, VoteType.NORMAL),
  {
    onSuccess: (response, roadmapId) => {
      // 查找并更新路线图数据
      const roadmap = roadmaps.value.find(r => r.id === roadmapId)
      if (roadmap) {
        roadmap.vote = response.upvotes
        roadmap.upvoted = response.upvoted
      }

      // 如果是当前选中的路线图，也更新它
      if (selectedRoadmap.value && selectedRoadmap.value.id === roadmapId) {
        selectedRoadmap.value.vote = response.upvotes
        selectedRoadmap.value.upvoted = response.upvoted
      }

      showSnackbar(response.upvoted ? t('roadmap.voteSuccess') : t('roadmap.voteCancel'))
    },
    onError: () => {
      showSnackbar(t('roadmap.voteFailed'))
    },
  },
)

// 投票功能
const voteRoadmap = async (roadmap: Roadmap, event: Event): Promise<void> => {
  event?.stopPropagation()
  await handleVote(roadmap.id)
}

// 初始化
// onMounted 已经被 useFetch 的 immediate: true 替代

</script>

<template>
  <v-container fluid>
    <!-- 错误页面 -->
    <ErrorPage
      v-if="errorCode"
      :error-code="errorCode"
      :error-message="error || '发生错误'"
      :show-retry="false"
      @back-home="() => $router.push('/')"
    />

    <!-- 正常内容 -->
    <v-row v-else class="mt-2">
      <v-col cols="12" lg="9" class="pr-lg-8">
        <!-- 页面头部 -->
        <RoadmapHeader
          :profession-name="profession?.name || 'JAVA初级程序员'"
          :roadmaps="roadmaps"
          :total-learners="getTotalLearners()"
          @create-roadmap="openCreateModal"
        />

        <!-- 课程表列表 -->
        <RoadmapList
          :roadmaps="roadmaps"
          :loading="loading"
          :error="error"
          :pinned-roadmaps="roadmaps.filter((r) => r.pinned).map((r) => r.id)"
          :profession-id="professionId"
          @open-detail="openModal as any"
          @copy-roadmap="copyRoadmapToCreate as any"
          @create-roadmap="openCreateModal"
          @roadmaps-updated="handleRoadmapsUpdated"
          @update-roadmap="handleUpdateRoadmap"
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
      :roadmap="selectedRoadmap as any"
      @close="closeModal"
      @vote="voteRoadmap as any"
      @node-click="handleNodeClick as any"
    />

    <!-- 创建新课程表弹窗 -->
    <RoadmapCreate
      v-model="showCreateModal"
      :profession-name="profession?.name"
      :copied-roadmap="copiedRoadmapData"
      @close="closeCreateModal"
      @save="saveRoadmap"
    />
  </v-container>
</template>

<style scoped>
/* 全局按钮内容居中 */
:deep(.v-btn__content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 4px !important;
}
</style>