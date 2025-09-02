<script setup>
import { ref, onMounted, onUnmounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';

import { 
  pageServiceV1, 
  courseServiceV1, 
  subscriptionServiceV1,
  progressServiceV1 
} from '@/services/api/v1/apiServiceV1';
import TreeNode from '../components/roadmap/TreeNode.vue';
import PostingList from '../components/read/PostingList.vue';
import ConfigContents from '../components/course/ConfigContents.vue';
import RightSidebar from '@/components/common/RightSidebar.vue';
import CourseCompletionDialog from '@/components/course/CourseCompletionDialog.vue';
import { PROGRESS_STATE } from '@/constants/statusConstants';

import { useUserStore } from "@/stores/user";
import { usePlatformStats } from '@/composables/usePlatformStats';
import { useStudyTimeTracker } from '@/composables/useStudyTimeTracker';

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');

const route = useRoute();
const router = useRouter();
const user = useUserStore();

// 使用平台统计数据
const { stats: platformStats, isLoading: statsLoading, error: statsError, refresh: refreshStats } = usePlatformStats();

// 使用学习时间追踪
const { 
  isTracking, 
  isActive, 
  currentDuration, 
  todayTotal, 
  startTracking, 
  stopTracking, 
  formatDuration 
} = useStudyTimeTracker();

console.log("sub: " + user.userId);
console.log("sub: " + user.subscription);

const sendMessage = () => {
  alert("search");
}

const data = ref(null);
const currContentsIndex = ref(0);
const nodes = ref(null);
const currNodeId = ref(0);
const lastPathNode = ref(null);
const configContents = ref(false);
const openContentsList = ref(true);
const path = ref("");
const pathText = ref("");
const displayCourseAll = ref(true);
const applyCourseDialog = ref(false);
const isCreatingSubcourse = ref(false);
const isLearning = ref(false); // 课程学习状态
const parentCourseInfo = ref(null); // 父课程信息
const subCourseList = ref([]); // 子课程列表
const isMainCourse = ref(true); // 是否为主课程
const treeNodeRefs = ref([]);
const showCongratulations = ref(false); // 恭喜完成课程弹窗

// 课程进度相关状态
const courseProgress = ref({
  completedNodes: 0,
  totalNodes: 0,
  progressPercent: 0,
  status: PROGRESS_STATE.NOT_STARTED // PROGRESS_STATE.NOT_STARTED, PROGRESS_STATE.IN_PROGRESS, PROGRESS_STATE.COMPLETED
});

const applyCourseData = ref({
  name: "",
  description: ""
})

onMounted(() => {
  loadData([])
});

onUnmounted(() => {
  // 页面卸载时停止学习时间追踪
  stopTracking();
});

window.addEventListener("scroll", () => {
  if (window.scrollY > 100) displayCourseAll.value = false; // 获取整个页面的滚动位置
});

router.afterEach((to, from) => {
  console.log(JSON.stringify(to.query));
  loadData([]);
});

async function loadData(parts) {
  try {
    let response;
    if ('commentId' in route.query) {
      response = await pageServiceV1.readByComment(route.query.commentId);
    } else if ('postId' in route.query) {
      response = await pageServiceV1.readByPost(route.query.postId);
    } else if ('nodeId' in route.query) {
      response = await pageServiceV1.readByNode(route.query.nodeId);
    } else if ('courseId' in route.query) {
      response = await pageServiceV1.readByCoursePath(route.query.courseId, route.query.path);
    }

    console.log("response: " + JSON.stringify(response));

    if (response.code === 401) {
      console.log('not login');
      //router.push('/login');
    } else if (response.code === 200) {

      if ('commentId' in response.data) {
        console.log("redirect to subcomment");
        const url = "/read?commentId=" + response.data.commentId + "&subCommentId=" + response.data.subCommentId;
        router.push(url);
      } else if ('path' in route.query && route.query.path != response.data.path) {
        const url = "read?courseId=" + route.query.courseId + "&path=" + response.data.path;
        router.push(url);
      } else {
        if (!Array.isArray(parts) || parts.length === 0) {
          data.value = response.data;
          data.value.otherPostings.forEach(posting => {
            if (posting.voteType === 0) {
              posting.voteType = null;
            }
          });

          // 处理新的数据格式
          if (response.data.parentCourse) {
            parentCourseInfo.value = response.data.parentCourse;
          }
          
          if (response.data.subCourseList) {
            subCourseList.value = response.data.subCourseList;
          }
          
          // 判断是否为主课程
          if (response.data.course && response.data.parentCourse) {
            isMainCourse.value = response.data.course.id === response.data.parentCourse.id;
          } else {
            isMainCourse.value = true; // 如果没有父课程，则认为是主课程
          }
          
          // 初始化学习状态
          isLearning.value = response.data.learning || false;

          // 启动学习时间追踪
          if (response.data.course && response.data.course.id) {
            const courseId = response.data.course.id;
            const nodeId = response.data.node ? response.data.node.id : null;
            
            // 停止之前的追踪（如果有）
            stopTracking();
            
            // 开始新的学习会话追踪
            startTracking(courseId, nodeId);
            
            console.log('Started study time tracking for course:', courseId, 'node:', nodeId);
          }

          path.value = data.value.path;
          
          console.log("课程信息:", {
            course: response.data.course,
            parentCourse: response.data.parentCourse,
            subCourseList: response.data.subCourseList,
            isMainCourse: isMainCourse.value
          });

          // 检查课程完成状态
          if (response.data.tocNodeInfos && response.data.course && response.data.toc) {
            const allNodesCompleted = checkAllNodesCompleted(response.data.tocNodeInfos, response.data.toc);
            const courseCompleted = response.data.course.isCompleted || false;
            
            // 如果第一组的所有节点都完成了，但课程还未完成，则设置课程为已完成并显示恭喜弹窗
            if (allNodesCompleted && !courseCompleted) {
              console.log('第一组所有节点已完成，自动设置课程为已完成');
              completeCourse(response.data.course.id);
            }
          }
        } else {
          if (parts.includes('chosenPosting')) {
            data.value.chosenPosting = response.data.chosenPosting;
          }
          if (parts.includes('fixedPostings')) {
            data.value.fixedPostings = response.data.fixedPostings;
          }
          if (parts.includes('contents')) {
            data.value.toc = response.data.toc;
            data.value.tocNodeInfos = response.data.tocNodeInfos;
          }
        }


        nodes.value = data.value.path.split("-");
        nodes.value[0]--;
        lastPathNode.value = nodes.value.reduce((acc, key) => acc && acc[key], data.value.toc);

        currContentsIndex.value = +nodes.value[0];
        nodes.value.shift();

        pathText.value = data.value.course.name + "/";
        nodes.value.forEach((item, index) => {
          if (index < 1) return;
          if (index < nodes.value.length - 1) {
            pathText.value += data.value.tocNodeInfos[item]?.name + "/"
          } else {
            pathText.value += data.value.tocNodeInfos[item]?.name;
          }
          console.log("path: " + pathText.value);
        });

        currNodeId.value = data.value.node.id;
        /*
        if (!('nodeId' in route.query) || nodeId == 0) {
          currNodeId.value = +nodes.value.pop();
        } else {
          currNodeId.value = nodeId;
        }
          */
      }
    }
  } catch (error) {
    console.error('Error:', error);
  }
}

const postApplyCourse = async () => {
  try {
    if (!user.userId) {
      showSnackbar(t('read.messages.pleaseLogin'), "error")
      return
    }
    
    isCreatingSubcourse.value = true
    console.log("courseId: " + route.params.courseId);
    const response = await courseServiceV1.createSubcourse(route.query.courseId, applyCourseData.value.name, applyCourseData.value.description)
    
    if (response.code === 200) {
      showSnackbar(t('read.subcourse.applySuccess'), "success")
      applyCourseDialog.value = false
      applyCourseData.value = { name: '', description: '' }
      // 重新加载子课程列表
    } else {
      showSnackbar(t('read.subcourse.createFailedWithMsg', { msg: response.msg || t('read.subcourse.unknownError') }), "error")
    }
  } catch (error) {
    console.error("创建子课程失败:", error)
    showSnackbar(t('read.subcourse.applyFailed'), "error")
  } finally {
    isCreatingSubcourse.value = false
  }
}

const closeCreateDialog = () => {
  applyCourseDialog.value = false
  applyCourseData.value = { name: '', description: '' }
}

// 跳转到子课程
const goToSubcourse = (subcourse) => {
  if (subcourse.id) {
    router.push(`/read?courseId=${subcourse.id}`)
  }
}

// 跳转到主课程
const goToParentCourse = () => {
  if (parentCourseInfo.value && parentCourseInfo.value.id) {
    router.push(`/read?courseId=${parentCourseInfo.value.id}`)
  }
}

const relatedLinks = ['高等数学', '概率论', 'C++编程实现', '软件测试']

// 检查第一组目录的所有节点是否已完成
const checkAllNodesCompleted = (tocNodeInfos, toc = null) => {
  if (!tocNodeInfos || Object.keys(tocNodeInfos).length === 0) {
    return false;
  }
  
  if (!toc || !toc[0]) {
    return false;
  }
  
  // 获取第一组目录中的所有节点ID
  const firstTocNodes = toc[0];
  const nodeIds = getAllNodeIdsFromToc(firstTocNodes);
  
  console.log('第一组目录节点IDs:', nodeIds);
  
  // 检查第一组的这些节点是否都已完成
  return nodeIds.every(nodeId => {
    const nodeInfo = tocNodeInfos[nodeId];
    const isCompleted = nodeInfo?.isCompleted === true;
    console.log(`节点${nodeId}完成状态:`, isCompleted, nodeInfo);
    return isCompleted;
  });
};

// 递归获取目录结构中的所有节点ID
const getAllNodeIdsFromToc = (tocNode) => {
  const nodeIds = [];
  
  const traverse = (node) => {
    if (typeof node === 'object' && node !== null) {
      Object.keys(node).forEach(key => {
        // 跳过特殊键
        if (key === '+' || key === '^') {
          return;
        }
        
        // 如果key是数字，说明是节点ID
        const nodeId = parseInt(key);
        if (!isNaN(nodeId)) {
          nodeIds.push(nodeId);
        }
        
        // 递归处理子节点
        if (typeof node[key] === 'object') {
          traverse(node[key]);
        }
      });
    }
  };
  
  traverse(tocNode);
  return nodeIds;
};

// 设置课程为已完成
const completeCourse = async (courseId) => {
  try {
    const response = await progressServiceV1.completeCourse(courseId);
    if (response.code === 200) {
      showCongratulations.value = true;
      // 更新当前数据中的课程完成状态
      if (data.value && data.value.course) {
        data.value.course.isCompleted = true;
      }
    } else {
      console.error('设置课程完成状态失败:', response.msg);
    }
  } catch (error) {
    console.error('设置课程完成状态失败:', error);
  }
};

// 处理节点完成事件
const onNodeCompleted = (nodeId) => {
  console.log('节点完成事件触发，nodeId:', nodeId);
  
  // 检查第一组目录的所有节点是否都已完成
  if (data.value && data.value.tocNodeInfos && data.value.course && data.value.toc) {
    const allNodesCompleted = checkAllNodesCompleted(data.value.tocNodeInfos, data.value.toc);
    const courseCompleted = data.value.course.isCompleted || false;
    
    // 如果第一组的所有节点都完成了，但课程还未完成，则设置课程为已完成并显示恭喜弹窗
    if (allNodesCompleted && !courseCompleted) {
      console.log('第一组所有节点已完成，自动设置课程为已完成');
      completeCourse(data.value.course.id);
    }
  }
};

// 获取下一个节点信息的方法
const getNextNodeInfo = () => {
  try {
    if (treeNodeRefs.value && treeNodeRefs.value[currContentsIndex.value]) {
      const treeNodeRef = treeNodeRefs.value[currContentsIndex.value];
      if (treeNodeRef && treeNodeRef.getNextNode) {
        return treeNodeRef.getNextNode(data.value.path);
      }
    }
    return null;
  } catch (error) {
    console.error('Error getting next node info:', error);
    return null;
  }
};

const subscript = async (courseId, action) => {
  let response = null;
  try {
    if (action) {
      response = await subscriptionServiceV1.subscribe(courseId);
    } else {
      response = await subscriptionServiceV1.unsubscribe(courseId);
    }

    if (response.code === 401) {
      console.log('not login');
      showSnackbar(t('read.messages.pleaseLogin'), 'error');
    } else if (response.code === 200) {
      console.log("data:" + response.data);
      console.log('done');
      // 更新parentCourseInfo的订阅状态
      if (parentCourseInfo.value) {
        parentCourseInfo.value.subscribed = action;
      }
      // 同时更新用户store中的数据
      user.setSubscription(response.data);
      showSnackbar(action ? t('read.messages.favoriteSuccess') : t('read.messages.favoriteRemoved'), 'success');
    } else {
      showSnackbar(t('read.messages.operationFailed'), 'error');
    }
  } catch (error) {
    console.error('Error updating subscription:', error);
    showSnackbar('操作失败，请稍后重试', 'error');
  }
}

// 开始学习课程
const startCourse = async () => {
  try {
    const response = await progressServiceV1.startCourse(route.query.courseId);
    
    if (response.code === 401) {
      console.log('not login');
      showSnackbar(t('read.messages.pleaseLogin'));
    } else if (response.code === 200) {
      isLearning.value = response.data;
      if (response.data) {
        showSnackbar(t('read.messages.startLearningSuccess'));
      } else {
        showSnackbar(t('read.messages.stopLearningSuccess'));
      }
    } else {
      showSnackbar(t('read.messages.operationFailed'));
    }
  } catch (error) {
    console.error('Error starting course:', error);
    showSnackbar(t('read.messages.operationFailed'));
  }
}

</script>

<template>
  <v-container v-if="data" fluid>
    <!-- 页面主内容 -->
    <v-row class="mt-0">
      <v-col cols="9" class="pr-6">
        <div class="course-header">
          <div class="d-flex align-center">
            <div class="course-info">
              <!-- 课程标题区域 -->
              <div class="d-flex align-center mb-4">
                <v-avatar
                  size="32"
                  border="md"
                  class="me-3">
                <v-icon icon="mdi-book-open-outline" size="20" color="grey"></v-icon>
                </v-avatar>
                <h1 class="course-title">{{ parentCourseInfo.name }}</h1> 
                <v-chip 
                  v-if="!isMainCourse"
                  color="red-darken-2" 
                  variant="flat" 
                  rounded="xl" 
                  class="ms-5 px-5"
                  density="comfortable"
                  prepend-icon="mdi-bookmark-outline"
                >
                  <span class="font-weight-black text-body-1">{{ t('read.course.subCourse', { name: data.course.name }) }}</span>
                </v-chip>
              </div>
              
              <!-- 标签和操作按钮一行显示 -->
              <div class="d-flex align-center">
                <!-- 子课程标签 -->
                
                
                <!-- 返回主课程按钮 -->
                <v-btn
                  v-if="!isMainCourse && parentCourseInfo"
                  @click="goToParentCourse"
                  variant="text"
                  rounded="lg"
                  prepend-icon="mdi-arrow-left"
                  size="small"
                  class="action-btn-inline "
                >
                  <span class="text-body-2">{{ t('read.course.backToMainCourse') }}</span>
                </v-btn>
                
                <!-- 订阅按钮 -->
                <v-btn
                  v-if="parentCourseInfo && parentCourseInfo.subscribed" 
                  @click="subscript(parentCourseInfo ? parentCourseInfo.id : data.course.id, false)"
                  prepend-icon="mdi-heart" 
                  variant="text" 
                  color="error" 
                  rounded="lg"
                  size="small"
                  class="action-btn-inline"
                >
                  <span class="text-body-2">{{ t('read.course.subscribed') }}</span>
                </v-btn>

                <v-btn 
                  v-else
                  @click="subscript(parentCourseInfo ? parentCourseInfo.id : data.course.id, true)"
                  prepend-icon="mdi-heart-outline" 
                  variant="text" 
                  rounded="lg"
                  size="small"
                  class="action-btn-inline"
                >
                  <span class="text-body-2">订阅课程</span>
                </v-btn>
                
                <!-- 展开/收起按钮，右对齐 -->
                <v-btn 
                  variant="text" 
                  rounded="lg" 
                  :prepend-icon="displayCourseAll ? 'mdi-chevron-up' : 'mdi-chevron-down'"
                  @click="displayCourseAll = !displayCourseAll"
                  size="small"
                  class="expand-btn-inline"
                >
                  <span class="text-body-2">{{ displayCourseAll ? t('read.course.collapse') : t('read.course.details') }}</span>
                </v-btn>
              </div>
            </div>
            <div class="course-actions">
              
              <div class="progress-info">
                <v-icon icon="mdi-chart-donut" size="20" color="grey-darken-3"></v-icon>
                <div class="progress-text">
                  <span class="progress-stats">23,434 节点</span>
                  <div class="progress-bar-container">
                    <v-progress-linear 
                      :model-value="60" 
                      color="primary" 
                      height="4"
                      rounded
                      class="progress-bar"
                    ></v-progress-linear>
                    <span class="progress-percent">60%</span>
                  </div>
                </div>
              </div>

              <!-- 学习状态区域 -->
              <div class="learning-status-container clickable" :class="{ 'learning-active': isLearning }" @click="startCourse">
                <!-- 第一行：状态信息 -->
                <div class="d-flex justify-space-between align-center" :class="{ 'mb-1': isLearning }">
                  <div class="d-flex align-center">
                    <v-icon 
                      :icon="!isLearning ? 'mdi-play-circle-outline' : ((data?.course?.progress || 0) >= 10000 ? 'mdi-check-circle' : 'mdi-play-circle')"
                      size="16" 
                      class="mr-1"
                      :color="!isLearning ? 'grey-darken-2' : ((data?.course?.progress || 0) >= 10000 ? 'success' : 'primary')"
                    ></v-icon>
                    <span class="text-body-2 font-weight-bold" :class="!isLearning ? 'text-grey-darken-3' : ((data?.course?.progress || 0) >= 10000 ? 'text-success' : 'text-primary')">
                      {{ !isLearning ? t('read.course.startLearning') : ((data?.course?.progress || 0) >= 10000 ? t('read.course.learningCompleted') : t('read.course.learningInProgress')) }}
                    </span>
                  </div>
                  <!-- 只有在学习状态下才显示进度百分比 -->
                  <span v-if="isLearning" class="text-caption font-weight-bold">{{ parseFloat(((data?.course?.progress || 0) / 100).toFixed(2)) }}%</span>
                </div>
                
                <!-- 第二行：进度条（只有在学习状态下才显示） -->
                <v-progress-linear 
                  v-if="isLearning"
                  :model-value="(data?.course?.progress || 0) / 100" 
                  :color="(data?.course?.progress || 0) >= 10000 ? 'success' : 'primary'" 
                  height="3"
                  rounded
                ></v-progress-linear>
              </div>
            </div>
          </div>

          <v-expand-transition>
            <div v-if="displayCourseAll" class="expanded-content">
              <div class="course-description">
                <!-- 如果有父课程信息，优先显示父课程描述；否则显示当前课程描述 -->
                {{ parentCourseInfo ? parentCourseInfo.description : data.course.description }}
              </div>

              <!-- 子课程列表标题 -->
              <div class="subcourses-header">
                <v-chip color="" variant="text" prepend-icon="mdi-format-list-group" rounded="lg" class="me-0">
                  <span class="text-grey-darken-4">
                    子课程列表
                  </span>
                </v-chip>
                <v-icon
                  icon="mdi-information-outline" 
                  variant="text"
                  size="x-small"
                  color="grey-darken-1"
                  class="mx-0"
                ></v-icon>
                <v-spacer></v-spacer>
                <!-- 只有主课程才能申请子课程 -->
                <v-btn 
                  @click="applyCourseDialog = true" 
                  variant="text" 
                  prepend-icon="mdi-plus-circle-outline" 
                  color="grey-darken-4"
                  size="default"
                  density="default"
                  rounded="xl"
                >
                  申请子课程
                </v-btn>
                
              </div>

              <div class="">
                <div v-for="subcourse in subCourseList" :key="subcourse.id || subcourse.name" class="subcourse-item">
                  <div class="subcourse-info">
                    <h3 class="subcourse-title">{{ subcourse.name }}</h3>
                    <p class="subcourse-desc">{{ subcourse.description }}</p>
                  </div>
                  <!-- 如果当前子课程就是正在学习的课程，显示"正在学习"标签 -->
                  <v-chip 
                    v-if="!isMainCourse && subcourse.id === data.course.id"
                    variant="flat" 
                    color="success" 
                    rounded="lg" 
                    prepend-icon="mdi-play-circle"
                  >
                    <span class="font-weight-bold">正在学习</span>
                  </v-chip>
                  <v-chip
                    v-else
                    variant="tonal" 
                    color="success" 
                    rounded="lg" 
                    @click="goToSubcourse(subcourse)"
                    :disabled="!subcourse.id"
                    prepend-icon="mdi-play"
                  >
                    <span class="font-weight-medium">开始学习</span>
                  </v-chip>
                </div>
                
                <!-- 空状态提示 -->
                <div v-if="subCourseList.length === 0" class="empty-subcourses">
                  <p class="text-grey d-flex align-center justify-center mb-1">
                    <v-icon icon="mdi-book-outline" size="18" color="grey-lighten-1" class="me-2"></v-icon>
                    <span class="font-weight-medium text-body-2">
                      {{ isMainCourse ? t('read.course.noSubCourses') : t('read.course.noSameLevelCourses') }}
                    </span>
                  </p>
                  <p v-if="isMainCourse" class="text-body-2 text-grey-lighten-1">点击上方 + 按钮创建第一个子课程</p>
                </div>
              </div>
            </div>
          </v-expand-transition>
        </div>

        <!-- 创建子课程对话框 -->
        <v-dialog v-model="applyCourseDialog" width="500" persistent>
          <v-card rounded="xl" elevation="8">
            <!-- 头部 -->
            <div class="d-flex align-center justify-space-between pa-6 pb-4">
              <div class="d-flex align-center">
                <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
                  <v-icon icon="mdi-book-plus-multiple" color="blue-darken-1" size="20"></v-icon>
                </div>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-3">创建子课程</h3>
                  <p class="text-body-2 text-grey-darken-1 mb-0">为当前课程添加新的子课程</p>
                </div>
              </div>
              <v-btn
                icon="mdi-close"
                variant="text"
                size="small"
                @click="closeCreateDialog"
              ></v-btn>
            </div>
            
            <v-divider></v-divider>
            
            <!-- 表单内容 -->
            <v-card-text class="pa-6">
              <v-form ref="subcourseForm" @submit.prevent="postApplyCourse">
                <div class="mb-4">
                  <label class="text-body-2 font-weight-medium text-grey-darken-2 mb-2 d-block">
                    课程名称 <span class="text-red">*</span>
                  </label>
                  <v-text-field 
                    v-model="applyCourseData.name" 
                    :placeholder="t('read.subcourse.namePlaceholder')"
                    variant="outlined"
                    rounded="lg"
                    density="comfortable"
                    :rules="[v => !!v || t('read.subcourse.nameRequired')]"
                    hide-details="auto"
                  ></v-text-field>
                </div>
                
                <div class="mb-4">
                  <label class="text-body-2 font-weight-medium text-grey-darken-2 mb-2 d-block">
                    课程描述 <span class="text-red">*</span>
                  </label>
                  <v-textarea 
                    v-model="applyCourseData.description" 
                    :placeholder="t('read.subcourse.descriptionPlaceholder')"
                    variant="outlined"
                    rounded="lg"
                    rows="4"
                    density="comfortable"
                    :rules="[v => !!v || t('read.subcourse.descriptionRequired')]"
                    hide-details="auto"
                  ></v-textarea>
                </div>
              </v-form>
            </v-card-text>
            
            <!-- 底部操作按钮 -->
            <v-card-actions class="pa-6 pt-0">
              <v-spacer></v-spacer>
              <v-btn
                variant="outlined"
                rounded="lg"
                @click="closeCreateDialog"
                class="mr-3"
                :disabled="isCreatingSubcourse"
              >
                取消
              </v-btn>
              <v-btn
                color="primary"
                variant="flat"
                rounded="lg"
                @click="postApplyCourse"
                :loading="isCreatingSubcourse"
                :disabled="!applyCourseData.name || !applyCourseData.description"
              >
                <v-icon icon="mdi-check" size="16" class="mr-2"></v-icon>
                创建课程
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        
        <v-row class="mt-1">

          <!-- left -->
          <v-col cols="3" class="pt-7">
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
                      :class="{ 'rotate-180': openContentsList }"
                      class="action-btn-refined"
                      @click="openContentsList = !openContentsList">
                    </v-btn>
                    <v-btn
                      icon="mdi-cog-outline"
                      variant="text"
                      size="small"
                      density="compact"
                      color="grey-darken-2"
                      class="action-btn-refined"
                      @click="configContents = true">
                    </v-btn>
                  </div>
                </div>

                <v-expand-transition>
                  <div v-if="openContentsList" class="toc-chips-refined">
                    <div 
                      v-for="(item, index) in data.toc" 
                      :key="index"
                      class="chip-refined"
                      :class="{ 
                        'chip-active': currContentsIndex === index,
                        'chip-primary': index === 0 
                      }"
                      @click="currContentsIndex = index">
                      
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

                <configContents v-model="configContents" :contents="data.toc" :courseId="data.course.id"
                  @loadData="loadData"></configContents>
              </div>

              <!-- three -->
              <v-tabs-window v-model="currContentsIndex" class="pt-4">
                <v-tabs-window-item v-for="(item, index) in data.toc" :value="index">
                  <TreeNode 
                    ref="treeNodeRefs"
                    :nodeData="data.toc[index]" 
                    :nodeInfos="data.tocNodeInfos"
                    :course-id="data.course.id" 
                    :path="data.path" 
                    :curr-path="String(index + 1)"
                    :depth="1"
                    :isLearning="isLearning" />
                </v-tabs-window-item>
              </v-tabs-window>
            </div>
          </v-col>

          <!-- list -->
          <v-col cols="9" class="pr-0 pt-4 d-flex justify-center">
            <v-col cols="10">
              <PostingList 
                :data="data" 
                :nodes="nodes" 
                :currNodeId="currNodeId" 
                :currNode="lastPathNode" 
                :pathText="pathText" 
                :getNextNodeInfo="getNextNodeInfo"
                :isLearning="isLearning"
                @loadData="loadData"
                @nodeCompleted="onNodeCompleted"
                @startLearning="startCourse" />
            </v-col>
          </v-col>
        </v-row>
      </v-col>

      <!-- right -->
      <v-col cols="3" class="">
        <RightSidebar/>
      </v-col>
        
    </v-row>

    <!-- 恭喜完成课程弹窗 -->
    <CourseCompletionDialog 
      v-model="showCongratulations" 
      :course-name="data?.course?.name || ''"
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

/* 课程头部 - 简洁版 */
.course-header {
  border-radius: 16px;
  margin-bottom: 2px;
  border:2px #4c83cc solid;
  border-top: 8px #4c83cc solid;
  padding: 18px 16px;
  transition: background-color 0.2s ease;
}

.course-info {
  flex: 1;
}

.course-icon {
  color: #424242;
}

.course-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.2;
}

.subcourse-name-display {
  margin-left: 48px; /* 对齐图标 */
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.course-chip {
  font-weight: 500;
}

.expand-btn {
  text-transform: none;
  font-weight: 500;
}

.course-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 240px;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.hover-button:hover {
  background: #f5f5f5;
}

.progress-text {
  flex: 1;
}

.progress-stats {
  font-size: 14px;
  font-weight: 600;
  color: #212121;
  display: block;
  line-height: 1.2;
  margin-bottom: 6px;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  flex: 1;
  min-width: 120px;
}

.progress-percent {
  font-size: 12px;
  font-weight: 600;
  color: #424242;
  min-width: 32px;
}

/* 展开内容 */
.expanded-content {
  padding-top: 20px;
  margin-top: 0px;
}

.course-description {
  font-size: 15px;
  line-height: 1.6;
  color: #212121;
  margin-bottom: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #b2d3dc;
}

.subcourses-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 2px;
}

.subcourse-info {
  flex: 1;
}

.subcourse-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px 0;
  line-height: 1.3;
}

.subcourse-desc {
  font-size: 14px;
  color: #424242;
  margin: 0;
  line-height: 1.4;
}

.learn-button:hover {
  background-color: #f0f0f0 !important;
  opacity: 1 !important; 
  transform: translateY(-1px);
}

/* 学习状态容器样式 */
.learning-status-container {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  min-width: 180px;
  height: 65px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: all 0.2s ease;
}

/* 未开始学习时居中显示 */
.learning-status-container:not(.learning-active) {
  justify-content: center;
  align-items: center;
}

.learning-status-container.clickable {
  cursor: pointer;
  user-select: none;
}

.learning-status-container.clickable:hover {
  background: #e3f2fd;
  border-color: #1976d2;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.2);
}

.subcourse-learn-btn {
  border: 1px solid #e0e0e0 !important;
}

.subcourse-learn-btn:hover {
  transform: translateY(-1px);
  background: #f0f0f0 !important;
}

.subcourse-learn-btn.current-learning {
  border: 1px solid #4caf50 !important;
}

.subcourse-learn-btn.current-learning:hover {
  background-color: #e8f5e8 !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .course-header {
    padding: 16px;
  }
  
  .course-main-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .course-actions {
    width: 100%;
    min-width: unset;
    align-items: stretch;
  }
  
  .progress-info {
    align-self: stretch;
  }
  
  .course-title {
    font-size: 20px;
  }
}

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
  background: linear-gradient(135deg, rgba(255,255,255,0.4), rgba(255,255,255,0.1));
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
  color: #white;
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
  filter: drop-shadow(0 1px 1px rgba(0,0,0,0.2));
}


/* 保持原有样式 */
.sticky-left {
  position: sticky;
  top: 15px;
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

/* 右侧栏样式优化 */
.sidebar-container {
  position: sticky;
  top: 0px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 推荐课程卡片 */
.featured-card {
  border: none;
  overflow: hidden;
}

.featured-image {
  position: relative;
}

.featured-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  font-weight: 600;
  font-size: 12px;
}

.featured-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.3;
  margin-bottom: 4px;
}

.featured-subtitle {
  font-size: 13px;
  color: #424242;
  line-height: 1.4;
}

.featured-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rating-text {
  font-size: 13px;
  font-weight: 600;
  color: #212121;
}

.featured-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #424242;
  font-weight: 500;
}

/* 相关课程列表 */
.related-courses {
  background: #f8f9fa;
  border: none;
  border-radius: 12px;
  padding: 20px;
  transition: background-color 0.2s ease;
}

.related-courses:hover {
  background: #f5f5f5;
}

.related-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 16px 0;
  line-height: 1.2;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.related-item {
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.related-item:hover {
  background-color: #f8fafc;
}

.related-link {
  text-decoration: none;
  display: block;
  padding: 10px 12px;
  border-radius: 8px;
}

.related-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.related-name {
  font-size: 14px;
  color: #212121;
  font-weight: 500;
  transition: color 0.2s ease;
}

.related-link:hover .related-name {
  color: #1976d2;
}

.related-arrow {
  color: #757575;
  transition: all 0.2s ease;
}

.related-link:hover .related-arrow {
  color: #1976d2;
  transform: translateX(2px);
}

/* 学习统计卡片 */
.stats-card {
  border: none;
}

.stats-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-box {
  text-align: center;
  padding: 12px 8px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.stat-box:hover {
  background: #f0f0f0;
  border-color: #bdbdbd;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #1976d2;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 11px;
  color: #424242;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar-container {
    position: relative;
    top: unset;
    margin-top: 20px;
  }
  
  .featured-card {
    margin-bottom: 16px;
  }
  
  .related-courses {
    padding: 16px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  
  .stat-box {
    padding: 8px 6px;
  }
  
  .stat-value {
    font-size: 16px;
  }
}

.scrollable-div {
  height: 520px;
  overflow-y: auto;
}

.hidden-scrollbar {
  scrollbar-width: none;
}

.hidden-scrollbar::-webkit-scrollbar {
  display: none;
}

.course-icon {
  opacity: 0.8;
}

.action-btn-inline:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.expand-btn-inline:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

/* 移除旧的样式 */
.course-header-section {
  margin-bottom: 1rem;
}

.subcourse-chip-container {
  margin-left: 48px;
}

.subcourse-chip {
  backdrop-filter: blur(8px);
}

.course-actions-section {
  margin-top: 1rem;
}

.action-btn {
  min-width: 120px;
  height: 40px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.expand-btn {
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

.expand-btn:hover {
  opacity: 1;
}

/* 子课程列表样式 */
.current-learning-chip {
  animation: gentle-pulse 3s infinite;
}

@keyframes gentle-pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
  100% {
    opacity: 1;
  }
}

.subcourse-learn-btn {
  transition: opacity 0.2s ease;
}

.subcourse-learn-btn:hover {
  opacity: 0.8;
}

.subcourse-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  margin-bottom: 8px;
  border-radius: 12px;
  background: #fafafa;
  transition: all 0.2s ease;
}

.subcourse-item:hover {
  background: #f5f5f5;
  border-color: #e0e0e0;
}

.subcourse-info {
  flex: 1;
  margin-right: 12px;
}

.subcourse-title {
  font-size: 1rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
  line-height: 1.3;
}

.subcourse-desc {
  color: #666;
  font-size: 0.85rem;
  line-height: 1.3;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.tab-icon {
  position: absolute;
  top: -5px;
  right: -5px;
  z-index: 10;
}

.empty-subcourses {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 24px;
  text-align: center;
  border: 0px dashed #e0e0e0;
  border-radius: 12px;
  margin: 0px 0;
  background-color: #f9f9f9;
}

.empty-subcourses p {
  margin: 4px 0;
}

@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

.skeleton-shimmer {
  background: linear-gradient(90deg, #fafafa 25%, #f0f0f0 50%, #fafafa 75%);
  background-size: 200px 100%;
  animation: shimmer 2.5s infinite;
}
</style>