<script setup lang="ts">
import { inject, onMounted, onUnmounted, ref } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { pageServiceV1, progressServiceV1 } from '@/services/api/v1/apiServiceV1'
import { MemoryService } from '@/services/memoryService'
import PostingList from '@/components/read/PostingList.vue'
import RightSidebar from '@/components/common/RightSidebar.vue'
import AIAssistant from '@/components/common/AIAssistant.vue'
import MemoryCardSidebar from '@/components/memory/MemoryCardSidebar.vue'
import CreateDeckDialog from '@/components/memory/CreateDeckDialog.vue'
import DeckDetailDialog from '@/components/memory/DeckDetailDialog.vue'
import CourseCompletionDialog from '@/components/course/CourseCompletionDialog.vue'
import CourseHeader from '@/components/read/CourseHeader.vue'
import CourseTableOfContents from '@/components/read/CourseTableOfContents.vue'

import { useStudyTimeTracker } from '@/composables/useStudyTimeTracker'

const { t } = useI18n()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

const route = useRoute()
const router = useRouter()

// 使用学习时间追踪
const { startTracking, stopTracking } = useStudyTimeTracker()

const data: Ref<any> = ref(null)
const currContentsIndex: Ref<number> = ref(0)
const nodes: Ref<any> = ref(null)
const currNodeId: Ref<number> = ref(0)
const lastPathNode: Ref<any> = ref(null)
const configContents: Ref<boolean> = ref(false)
const openContentsList: Ref<boolean> = ref(true)
const path: Ref<string> = ref('')
const pathText: Ref<string> = ref('')
const displayCourseAll: Ref<boolean> = ref(true)
const isLearning: Ref<boolean> = ref(false) // 课程学习状态
const parentCourseInfo: Ref<any> = ref(null) // 父课程信息
const subCourseList: Ref<any[]> = ref([]) // 子课程列表
const isMainCourse: Ref<boolean> = ref(true) // 是否为主课程
const showCongratulations: Ref<boolean> = ref(false) // 恭喜完成课程弹窗
const courseTableOfContentsRef: Ref<any> = ref(null)
const showCreateDeckDialog: Ref<boolean> = ref(false) // 创建卡片组对话框
const showDeckDetailDialog: Ref<boolean> = ref(false) // 卡片组详情对话框
const selectedDeck: Ref<any> = ref(null) // 选中的卡片组
const memoryCardSidebarRef: Ref<any> = ref(null) // 记忆卡片组侧边栏引用
const currentTab: Ref<string> = ref('list') // 当前PostingList的tab状态
const currentPosting: Ref<any> = ref(null) // 当前查看的文章

onMounted(() => {
  loadData([])
})

onUnmounted(() => {
  // 页面卸载时停止学习时间追踪
  stopTracking()
})

window.addEventListener('scroll', () => {
  if (window.scrollY > 100) displayCourseAll.value = false // 获取整个页面的滚动位置
})

router.afterEach((to) => {
  console.log(JSON.stringify(to.query))
  loadData([])
})

const loadData = async (parts: string[]): Promise<void> => {
  try {
    let response: any
    if ('commentId' in route.query) {
      response = await pageServiceV1.readByComment(Number(route.query.commentId as string))
    } else if ('postId' in route.query) {
      response = await pageServiceV1.readByPost(Number(route.query.postId as string))
    } else if ('nodeId' in route.query) {
      response = await pageServiceV1.readByNode(Number(route.query.nodeId as string))
    } else if ('courseId' in route.query) {
      response = await pageServiceV1.readByCoursePath(Number(route.query.courseId as string), route.query.path as string)
    }

    console.log(`Read response: ${JSON.stringify(response)}`)

    if (response.code === 401) {
      console.log('not login')
      //router.push('/login');
    } else if (response.code === 200) {
      if ('commentId' in response.data) {
        console.log('redirect to subcomment')
        const url = `/read?commentId=${response.data.commentId}&subCommentId=${
          response.data.subCommentId
        }`
        router.push(url)
      } else if ('path' in route.query && route.query.path !== response.data.path) {
        const url = `read?courseId=${route.query.courseId}&path=${response.data.path}`
        router.push(url)
      } else {
        if (!Array.isArray(parts) || parts.length === 0) {
          data.value = response.data
          data.value.otherPostings.forEach((posting: any) => {
            if (posting.voteType === 0) {
              posting.voteType = null
            }
          })

          // 处理新的数据格式
          if (response.data.parentCourse) {
            parentCourseInfo.value = response.data.parentCourse
          }

          if (response.data.subCourseList) {
            subCourseList.value = response.data.subCourseList
          }

          // 判断是否为主课程
          if (response.data.course && response.data.parentCourse) {
            isMainCourse.value = response.data.course.id === response.data.parentCourse.id
          } else {
            isMainCourse.value = true // 如果没有父课程，则认为是主课程
          }

          // 初始化学习状态
          isLearning.value = response.data.learning || false

          // 启动学习时间追踪
          if (response.data.course && response.data.course.id) {
            const courseId = response.data.course.id
            const nodeId = response.data.node ? response.data.node.id : null

            // 停止之前的追踪（如果有）
            stopTracking()

            // 开始新的学习会话追踪
            startTracking(courseId, nodeId)

            console.log('Started study time tracking for course:', courseId, 'node:', nodeId)
          }

          path.value = data.value.path

          console.log('课程信息:', {
            course: response.data.course,
            parentCourse: response.data.parentCourse,
            subCourseList: response.data.subCourseList,
            isMainCourse: isMainCourse.value,
          })

          // 检查课程完成状态
          if (response.data.tocNodeInfos && response.data.course && response.data.toc) {
            const allNodesCompleted = checkAllNodesCompleted(
              response.data.tocNodeInfos,
              response.data.toc
            )
            const courseCompleted = response.data.course.isCompleted || false

            // 如果第一组的所有节点都完成了，但课程还未完成，则设置课程为已完成并显示恭喜弹窗
            if (allNodesCompleted && !courseCompleted) {
              console.log('第一组所有节点已完成，自动设置课程为已完成')
              completeCourse(response.data.course.id)
            }
          }
        } else {
          if (parts.includes('chosenPosting')) {
            data.value.chosenPosting = response.data.chosenPosting
          }
          if (parts.includes('fixedPostings')) {
            data.value.fixedPostings = response.data.fixedPostings
          }
          if (parts.includes('contents')) {
            data.value.toc = response.data.toc
            data.value.tocNodeInfos = response.data.tocNodeInfos
          }
        }

        nodes.value = data.value.path.split('-')
        nodes.value[0]--
        lastPathNode.value = nodes.value.reduce((acc: any, key: any) => acc && acc[key], data.value.toc)

        currContentsIndex.value = Number(nodes.value[0])
        nodes.value.shift()

        pathText.value = `${data.value.course.name}/`
        nodes.value.forEach((item: any, index: number) => {
          if (index < 1) return
          if (index < nodes.value.length - 1) {
            pathText.value += `${data.value.tocNodeInfos[item]?.name}/`
          } else {
            pathText.value += data.value.tocNodeInfos[item]?.name
          }
          console.log(`path: ${pathText.value}`)
        })

        currNodeId.value = data.value.node.id
      }
    }
  } catch (error) {
    console.error('Error:', error)
  }
}

// 检查第一组目录的所有节点是否已完成
const checkAllNodesCompleted = (tocNodeInfos: any, toc: any = null): boolean => {
  if (!tocNodeInfos || Object.keys(tocNodeInfos).length === 0) {
    return false
  }

  if (!toc || !toc[0]) {
    return false
  }

  // 获取第一组目录中的所有节点ID
  const [firstTocNodes] = toc
  const nodeIds = getAllNodeIdsFromToc(firstTocNodes)

  console.log('第一组目录节点IDs:', nodeIds)

  // 检查第一组的这些节点是否都已完成
  return nodeIds.every((nodeId: number) => {
    const nodeInfo = tocNodeInfos[nodeId]
    const isCompleted = nodeInfo?.isCompleted === true
    console.log(`节点${nodeId}完成状态:`, isCompleted, nodeInfo)
    return isCompleted
  })
}

// 递归获取目录结构中的所有节点ID
const getAllNodeIdsFromToc = (tocNode: any): number[] => {
  const nodeIds: number[] = []

  const traverse = (node: any) => {
    if (typeof node === 'object' && node !== null) {
      Object.keys(node).forEach((key) => {
        // 跳过特殊键
        if (key === '+' || key === '^') {
          return
        }

        // 如果key是数字，说明是节点ID
        const nodeId = parseInt(key)
        if (!isNaN(nodeId)) {
          nodeIds.push(nodeId)
        }

        // 递归处理子节点
        if (typeof node[key] === 'object') {
          traverse(node[key])
        }
      })
    }
  }

  traverse(tocNode)
  return nodeIds
}

// 设置课程为已完成
const completeCourse = async (courseId: number): Promise<void> => {
  try {
    const response = await progressServiceV1.completeCourse(courseId)
    if (response.code === 200) {
      showCongratulations.value = true
      // 更新当前数据中的课程完成状态
      if (data.value && data.value.course) {
        data.value.course.isCompleted = true
      }
    } else {
      console.error('设置课程完成状态失败:', response.message)
    }
  } catch (error) {
    console.error('设置课程完成状态失败:', error)
  }
}

// 处理节点完成事件
const onNodeCompleted = (nodeId: number): void => {
  console.log('节点完成事件触发，nodeId:', nodeId)

  // 检查第一组目录的所有节点是否都已完成
  if (data.value && data.value.tocNodeInfos && data.value.course && data.value.toc) {
    const allNodesCompleted = checkAllNodesCompleted(data.value.tocNodeInfos, data.value.toc)
    const courseCompleted = data.value.course.isCompleted || false

    // 如果第一组的所有节点都完成了，但课程还未完成，则设置课程为已完成并显示恭喜弹窗
    if (allNodesCompleted && !courseCompleted) {
      console.log('第一组所有节点已完成，自动设置课程为已完成')
      completeCourse(data.value.course.id)
    }
  }
}

// 获取下一个节点信息的方法
const getNextNodeInfo = (): any => {
  try {
    if (courseTableOfContentsRef.value && courseTableOfContentsRef.value.getNextNodeInfo) {
      return courseTableOfContentsRef.value.getNextNodeInfo()
    }
    return null
  } catch (error) {
    console.error('Error getting next node info:', error)
    return null
  }
}

// 处理订阅状态变化
const handleSubscriptionChange = ({ action }: { action: boolean }): void => {
  // 更新parentCourseInfo的订阅状态
  if (parentCourseInfo.value) {
    parentCourseInfo.value.subscribed = action
  }
}

// 开始学习课程
const startCourse = async (): Promise<void> => {
  try {
    const response = await progressServiceV1.startCourse(Number(route.query.courseId as string))

    if (response.code === 401) {
      console.log('not login')
      showSnackbar(t('read.messages.pleaseLogin'))
    } else if (response.code === 200) {
      isLearning.value = response.data
      if (response.data) {
        showSnackbar(t('read.messages.startLearningSuccess'))
      } else {
        showSnackbar(t('read.messages.stopLearningSuccess'))
      }
    } else {
      showSnackbar(t('read.messages.operationFailed'))
    }
  } catch (error) {
    console.error('Error starting course:', error)
    showSnackbar(t('read.messages.operationFailed'))
  }
}

// 处理创建卡片组
const handleCreateDeck = (): void => {
  showCreateDeckDialog.value = true
}

// 处理卡片组创建完成
const handleDeckCreated = (deck: any): void => {
  showSnackbar('卡片组创建成功！', 'success')
  // 刷新侧边栏数据并切换到"只看我的"标签
  if (memoryCardSidebarRef.value && memoryCardSidebarRef.value.loadDecks) {
    // 切换到"只看我的"标签
    if (memoryCardSidebarRef.value.switchToMyDecks) {
      memoryCardSidebarRef.value.switchToMyDecks()
    }
    // 刷新数据
    memoryCardSidebarRef.value.loadDecks(true)
  }
}

// 处理添加卡片组到学习计划
const handleAddDeck = async (deck: any): Promise<void> => {
  console.log('ReadView received addDeck event:', deck)
  
  try {
    // 获取当前课程ID
    const courseId = data.value?.course?.id
    if (!courseId) {
      showSnackbar('无法确定课程信息', 'error')
      return
    }

    // 调用API添加卡片组到记忆库
    const response = await MemoryService.addDeckToMemoryBank({
      deckId: deck.id,
      courseId: courseId
    })

    if (response.code === 200) {
      showSnackbar(`已将"${deck.title}"添加到${data.value.course.name}课程的学习计划`, 'success')
    } else {
      showSnackbar('添加失败，请重试', 'error')
    }
    
    // TODO: 可选择性刷新侧边栏数据以显示更新后的状态
  } catch (error) {
    console.error('Failed to add deck to memory bank:', error)
    showSnackbar('添加失败，请重试', 'error')
  }
}

// 处理查看卡片组详情
const handleViewDeck = (deck: any): void => {
  console.log('ReadView received viewDeck event:', deck)
  selectedDeck.value = deck
  showDeckDetailDialog.value = true
}

// 检查是否显示记忆卡片组侧边栏
const shouldShowMemoryCardSidebar = (): boolean => {
  // 当在文章详情页时显示记忆卡片组侧边栏
  return !!data.value?.post || !!(route.query.postId)
}

// 处理PostingList的tab切换
const handleTabSwitch = (tab: string, posting?: any): void => {
  currentTab.value = tab
  if (posting && typeof posting === 'object') {
    currentPosting.value = posting
  } else if (tab === 'list') {
    // 切换回list标签时，清空currentPosting
    currentPosting.value = null
  }
}
</script>

<template>
  <v-container v-if="data" fluid>
    <!-- 页面主内容 -->
    <v-row class="mt-0">
      <v-col cols="9" class="pr-6">
        <CourseHeader
          :parent-course-info="parentCourseInfo"
          :current-course="data.course"
          :sub-course-list="subCourseList"
          :is-main-course="isMainCourse"
          :is-learning="isLearning"
          :display-course-all="displayCourseAll"
          @start-learning="isLearning = $event"
          @subscribe-course="handleSubscriptionChange"
          @toggle-display="displayCourseAll = !displayCourseAll"
          @subcourse-created="loadData([])"
        />

        <v-row class="mt-1">
          <!-- left -->
          <v-col cols="3" class="pt-7">
            <CourseTableOfContents
              ref="courseTableOfContentsRef"
              :data="data"
              :curr-contents-index="currContentsIndex"
              :open-contents-list="openContentsList"
              :config-contents="configContents"
              :is-learning="isLearning"
              @update:curr-contents-index="currContentsIndex = $event"
              @update:open-contents-list="openContentsList = $event"
              @update:config-contents="configContents = $event"
              @load-data="loadData"
            />
          </v-col>

          <!-- list -->
          <v-col cols="9" class="pr-0 pt-4 d-flex justify-center">
            <v-col cols="10">
              <PostingList
                :data="data"
                :nodes="nodes"
                :curr-node-id="currNodeId"
                :curr-node="lastPathNode"
                :path-text="pathText"
                :get-next-node-info="getNextNodeInfo"
                :is-learning="isLearning"
                @load-data="loadData"
                @node-completed="onNodeCompleted"
                @start-learning="startCourse"
                @switch-tab="handleTabSwitch"
              />
            </v-col>
          </v-col>
        </v-row>
      </v-col>

      <!-- right -->
      <v-col cols="3" class="">
        <div v-if="currentTab !== 'list' && currentPosting" class="sticky-sidebar d-flex flex-column" style="gap: 16px;">
          <!-- AI答疑助手 (文章详情页) -->
          <AIAssistant />
          
          <!-- 记忆卡片组侧边栏 (文章详情页) -->
          <MemoryCardSidebar
            ref="memoryCardSidebarRef"
            :post="currentPosting"
            @create-deck="handleCreateDeck"
            @add-deck="handleAddDeck"
            @view-deck="handleViewDeck"
          />
        </div>
        
        <!-- 默认侧边栏 (其他页面) -->
        <RightSidebar v-else />
      </v-col>
    </v-row>

    <!-- 恭喜完成课程弹窗 -->
    <CourseCompletionDialog v-model="showCongratulations" :course-name="data?.course?.name || ''" />
    
    <!-- 创建卡片组对话框 -->
    <CreateDeckDialog
      v-if="currentPosting"
      v-model="showCreateDeckDialog"
      :post="currentPosting"
      @created="handleDeckCreated"
    />

    <!-- 卡片组详情对话框 -->
    <DeckDetailDialog
      v-model="showDeckDetailDialog"
      :deck="selectedDeck"
      @add-to-study="handleAddDeck"
    />
  </v-container>
</template>

<style scoped>
.text-h7 {
  font-size: 1.15rem;
}

/* 新增样式 */
.data-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.data-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.vision-card {
  border: 2px solid #ffebee;
}

.vision-content {
  background: linear-gradient(135deg, #ffebee 0%, #fce4ec 100%);
  border: 1px solid #f8bbd9;
}

.stat-card {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-card:hover {
  transform: translateX(4px);
  border-color: rgba(76, 175, 80, 0.3);
}

.progress-item {
  transition: all 0.2s ease;
}

.progress-item:hover {
  transform: scale(1.05);
}

.ranking-item {
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.ranking-item:hover {
  transform: translateX(4px);
  border-color: rgba(25, 118, 210, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.rank-chip {
  min-width: 50px !important;
}

/* 紧凑间距样式 */
.pa-0-5 {
  padding: 2px !important;
}

/* 粘性侧边栏样式 */
.sticky-sidebar {
  position: sticky;
  top: 65px;
  align-self: flex-start;
}
</style>