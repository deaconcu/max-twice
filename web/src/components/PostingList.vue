<script setup>
import { ref, onMounted, nextTick, watch, toRef, onUnmounted, inject, computed } from 'vue';
import { learnService } from '@/services/learnService';
import { useRoute, useRouter } from 'vue-router';

import AddContents from '../components/AddContents.vue';
import AddArticle from '../components/AddArticle.vue';
import Invite from '../components/Invite.vue';
import Comment from '../components/Comment.vue';
import Posting from './Posting.vue';
import Tiptap from '../components/Tiptap.vue'

// 导入Post浏览量跟踪服务
import postViewTracking from '@/services/postViewTracking'

// 在开发环境导入调试工具
if (import.meta.env.DEV) {
  import('@/services/postViewTrackingDebug')
}

const route = useRoute();
const router = useRouter();

const showSnackbar = inject('showSnackbar');

const emit = defineEmits(['loadData', 'nodeCompleted', 'startLearning']);

const props = defineProps(['data', 'nodes', 'currNodeId', 'currNode', 'pathText', 'getNextNodeInfo', 'isLearning']);

const lastPostingId = ref(0);
const lastScore = ref(0);
const createContentsDialog = ref(false);
const createArticleDialog = ref(false);

const inviteDialog = ref(false);
const tab = ref('list');
const scrollPosition = ref(0);
const scrollKey = ref(0)
const editorRef = ref(null);
const vote = ref(null)

const currPosting = ref(null);
const nextNodeDialog = ref(false);
const nextNodeInfo = ref(null);

// 计算当前节点是否有子节点
const hasSubNodes = computed(() => {
  if (!props.currNode || typeof props.currNode !== 'object') {
    return false;
  }
  
  // 过滤掉特殊键 '^'，检查是否有其他子节点
  const childKeys = Object.keys(props.currNode).filter(key => key !== '^');
  return childKeys.length > 0;
});

onMounted(() => {
  window.addEventListener('popstate', restoreScrollPosition);

  console.log("post: " + props.data.post);

  if (("tab" in route.query) && route.query.tab == 'comment') {
    console.log("xxss");
    tab.value = 'comment';
  }

  if (props.data.post != null) {
    switchTab('detail', dataRef.value.post);
  }

  if (!("post" in props.data) && "commentId" in route.query) {
    switchTab('comment', '');
  }

  // 🔴 初始化Post浏览量跟踪
  initializePostViewTracking();
});

/**
 * 初始化Post浏览量跟踪功能
 * 为当前页面上的所有posts设置浏览量统计
 */
function initializePostViewTracking() {
  // 等待DOM渲染完成后开始跟踪
  nextTick(() => {
    // 自动扫描并开始跟踪页面上的posts
    postViewTracking.autoObserve();
    
    console.log('[PostingList] Post浏览量跟踪已初始化');
    
    // 在开发环境下输出跟踪状态（用于调试）
    if (import.meta.env.DEV) {
      const status = postViewTracking.getStatus();
      console.log('[PostingList] 跟踪状态:', status);
    }
  });
}

/**
 * 组件卸载时的清理工作
 */
onUnmounted(() => {
  // 提交剩余的浏览记录
  postViewTracking.flush();
  
  console.log('[PostingList] 组件卸载，已提交剩余浏览记录');
});

const restoreScrollPosition = () => {
  console.log("xx");
};

const dataRef = toRef(props, 'data')

watch(dataRef, (newItems, oldItems) => {
  console.log('数据发生变化:', newItems)
  scrollKey.value++;
  updateLastPostingId();
  console.log("path text: " + props.pathText);
  
  // 🔴 数据更新后重新扫描新的posts进行跟踪
  handleDataUpdate();
})

/**
 * 处理数据更新
 * 当posts数据发生变化时（如加载更多），重新扫描新的posts
 */
function handleDataUpdate() {
  // 等待DOM更新完成后重新扫描
  nextTick(() => {
    // 重新扫描并跟踪新加载的posts
    postViewTracking.autoObserve();
    
    console.log('[PostingList] 数据更新，重新扫描Posts');
    
    // 在开发环境下输出更新后的状态
    if (import.meta.env.DEV) {
      const status = postViewTracking.getStatus();
      console.log('[PostingList] 更新后跟踪状态:', {
        observedPosts: status.observedPostsCount,
        currentlyTracking: status.currentlyTracking,
        pendingSubmissions: status.pendingSubmissions
      });
    }
  });
}

watch(() => route.fullPath, () => {
  tab.value = 'list';
})

updateLastPostingId();

function loadData(parts) {
  emit("loadData", parts);
}

function updateLastPostingId() {
  const otherPostings = props.data.otherPostings;
  if (otherPostings.length > 0) {
    lastPostingId.value = otherPostings[otherPostings.length - 1].id;
    lastScore.value = otherPostings[otherPostings.length - 1].score;
  }
}

const switchTab = (tabName, posting) => {
  console.log('switch tab');
  tab.value = tabName; // 切换到指定 Tab

  if (tabName == "list") {
    console.log("scroll: " + scrollPosition.value);
    nextTick(() => {
      window.scrollTo(0, scrollPosition.value);
    });
  } else {
    console.log("save: " + window.scrollY);
    scrollPosition.value = window.scrollY;
    //window.scrollTo(0, 75);
    window.scrollTo(0, 0);
    currPosting.value = posting;
  }
};

async function loadMore({ done }) {
  try {
    const params = {
      lastId: lastPostingId.value,
      lastScore: lastScore.value,
      nodeId: props.currNodeId,
    }
    console.log("begin load: param: " + JSON.stringify(params));

    const response = await learnService.getPosting(params);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      response.data.forEach(posting => {
        if (posting.voteType === 0) {
          posting.voteType = null;
        }
      });
      props.data.otherPostings.push(...response.data);

      if (response.data.length > 0) {
        lastPostingId.value = response.data[response.data.length - 1].id;
        lastScore.value = response.data[response.data.length - 1].score;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const submitAddArticle = async () => {
  try {
    console.log("begin post");
    console.log("编辑器内容:", editorRef.value.editor.getHTML());

    const data = {
      "content": editorRef.value.editor.getHTML(),
      "nodeId": props.currNodeId,
      "type": 2
    }

    console.log('request: ' + JSON.stringify(data));
    const response = await learnService.addPosting(data);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      emit('loadData', []);
      //emit('update:dialog', false)
      createArticleDialog.value = false
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const tab1 = ref(null);

// 切换节点完成状态
const toggleNodeCompletion = async () => {
  // 检查是否在学习模式下
  if (!props.isLearning) {
    // 询问用户是否开始学习
    const confirmed = confirm('您还未开始学习此课程，是否要开始学习并完成当前节点？');
    if (!confirmed) {
      return; // 用户取消
    }
    
    // 用户确认，先开始学习
    try {
      // 这里需要调用开始学习的方法，通过事件通知父组件
      emit('startLearning');
    } catch (error) {
      console.error('启动学习模式失败:', error);
      showSnackbar && showSnackbar('启动学习模式失败，请重试', 'error');
      return;
    }
  }

  try {
    if (props.data.node.isCompleted) {
      // 取消完成
      const response = await learnService.unmarkNodeCompleted(props.currNodeId, props.data.course.id);
      console.log('Unmark node completed response:', response);
      
      if (response.code === 200) {
        props.data.node.isCompleted = false;
        
        // 同时更新 tocNodeInfos 中的节点状态
        if (props.data.tocNodeInfos && props.data.tocNodeInfos[props.currNodeId]) {
          props.data.tocNodeInfos[props.currNodeId].isCompleted = false;
        }
        
        // 更新课程进度（后端返回的是 progress*100 的值，直接使用）
        if (response.data && response.data.courseProgress !== undefined) {
          props.data.course.progress = response.data.courseProgress;
        }
        
        showSnackbar && showSnackbar('已取消完成状态', 'info');
      } else {
        console.error('Failed to unmark node as completed:', response.msg);
        showSnackbar && showSnackbar('取消完成失败，请重试', 'error');
      }
    } else {
      // 标记完成
      const response = await learnService.markNodeCompleted(props.currNodeId, props.data.course.id);
      console.log('Mark node completed response:', response);
      
      if (response.code === 200) {
        props.data.node.isCompleted = true;
        
        console.log('节点标记完成成功，当前节点ID:', props.currNodeId);
        console.log('更新前的tocNodeInfos:', props.data.tocNodeInfos);
        
        // 同时更新 tocNodeInfos 中的节点状态
        if (props.data.tocNodeInfos && props.data.tocNodeInfos[props.currNodeId]) {
          props.data.tocNodeInfos[props.currNodeId].isCompleted = true;
          console.log('tocNodeInfos中的节点状态已更新:', props.data.tocNodeInfos[props.currNodeId]);
        } else {
          console.warn('tocNodeInfos中找不到节点ID:', props.currNodeId, '可用的节点IDs:', Object.keys(props.data.tocNodeInfos || {}));
        }
        
        console.log('更新后的tocNodeInfos:', props.data.tocNodeInfos);
        
        // 更新课程进度（后端返回的是 progress*100 的值，直接使用）
        if (response.data && response.data.courseProgress !== undefined) {
          props.data.course.progress = response.data.courseProgress;
        }
        
        showSnackbar && showSnackbar('节点学习已完成！', 'success');
        
        // 通知父组件节点已完成，触发课程完成检查
        console.log('发送节点完成事件，节点ID:', props.currNodeId);
        emit('nodeCompleted', props.currNodeId);
        
        // 检查是否有下一个节点
        checkNextNode();
      } else {
        console.error('Failed to mark node as completed:', response.msg);
        showSnackbar && showSnackbar('标记完成失败，请重试', 'error');
      }
    }
  } catch (error) {
    console.error('Error toggling node completion:', error);
    showSnackbar && showSnackbar('操作失败，请重试', 'error');
  }
};

// 检查下一个节点并弹出确认对话框
const checkNextNode = () => {
  try {
    if (props.getNextNodeInfo) {
      const nextNode = props.getNextNodeInfo();
      if (nextNode) {
        nextNodeInfo.value = nextNode;
        nextNodeDialog.value = true;
      }
    }
  } catch (error) {
    console.error('Error checking next node:', error);
  }
};

// 跳转到下一个节点
const goToNextNode = () => {
  if (nextNodeInfo.value && nextNodeInfo.value.path) {
    const courseId = route.query.courseId;
    const nextPath = nextNodeInfo.value.path;
    const url = `/read?courseId=${courseId}&path=${nextPath}`;
    router.push(url);
  }
  nextNodeDialog.value = false;
};

// 处理从Posting组件发出的markNodeCompleted事件
const handleMarkNodeCompleted = async () => {
  // 检查当前节点是否有子节点，有子节点的不能标记完成
  if (hasSubNodes.value) {
    console.log('当前节点有子节点，不能标记为完成');
    return;
  }
  
  if (props.data.node.isCompleted) {
    // 如果已经完成了，不需要重复执行
    return;
  }
  
  try {
    // 标记完成
    const response = await learnService.markNodeCompleted(props.currNodeId);
    console.log('Mark node completed from posting response:', response);
    
    if (response.code === 200) {
      props.data.node.isCompleted = true;
      showSnackbar && showSnackbar('看两遍就懂！节点学习已完成！', 'success');
      
      // 检查是否有下一个节点
      checkNextNode();
    } else {
      console.error('Failed to mark node as completed from posting:', response.msg);
      showSnackbar && showSnackbar('标记完成失败，请重试', 'error');
    }
  } catch (error) {
    console.error('Error marking node completed from posting:', error);
    showSnackbar && showSnackbar('操作失败，请重试', 'error');
  }
};
</script>

<template>

  <template v-if="(tab == 'list' || tab == 'addArticle' || tab == 'comment') && data && data.tocNodeInfos" class="mb-4">
    <v-row class="ma-0 text-grey text-body-2 pb-2">
      <div v-if="!('nodeId' in route.query)" class="d-flex align-center">
        <template v-for="item in nodes" :key="item">
          <div class="d-flex align-center">
            {{ data.tocNodeInfos?.[item]?.name || item }}
            <v-icon icon="mdi-chevron-right" class="px-5"></v-icon>
          </div>
        </template>
      </div>
      <div v-else>
        <div class="d-flex align-center">
          课程节点
          <v-icon icon="mdi-chevron-right" class="px-4"></v-icon>
        </div>
      </div>
    </v-row>

    <div class="px-0 pb-1 pt-4 ma-0 mb-0" style="position: sticky; top: 0px; background-color: #fff;z-index: 1;">
      <div class="d-flex align-center justify-space-between mb-2">
          <div class="d-flex align-center">
            <v-icon icon="mdi-list-box-outline" color="primary-darken-1" size="24"></v-icon>
            <h2 class="text-h5 font-weight-bold text-grey-darken-4 ms-3">{{ data.node.name }}</h2>
          </div>
          <v-btn
            v-if="isLearning && !hasSubNodes"
            @click="toggleNodeCompletion"
            :color="data.node.isCompleted ? 'grey-lighten-2' : 'success'"
            :variant="data.node.isCompleted ? 'outlined' : 'flat'"
            rounded="lg"
            size="small"
            class="px-4"
            :prepend-icon="data.node.isCompleted ? 'mdi-check-circle' : 'mdi-circle-outline'"
          >
            <span class="font-weight-medium" :class="data.node.isCompleted ? 'text-grey-darken-2' : 'text-white'">
              {{ data.node.isCompleted ? '已完成' : '完成学习' }}
            </span>
          </v-btn>
      </div>
    </div>
    <p class="text-body-1 text-grey-darken-2 mb-0">{{ data.node.description }}</p>
    <v-row class="mt-8 mb-0 mx-0 justify-space-between">
      <div>
        <v-tabs density="compact" class="">
          <v-tab class="px-3" @click="switchTab('list', '')">
            <v-icon icon="mdi-list-box-outline" size="16" class="mr-2"></v-icon>
            <span class="font-weight-medium text-grey-darken-3">文章列表</span>
          </v-tab>
          <v-tab class="px-3" @click="switchTab('comment', '')">
            <v-icon icon="mdi-comment-outline" size="16" class="mr-2"></v-icon>
            <span class="font-weight-medium text-grey-darken-3">{{ data.node.commentCount }} 条评论</span>
          </v-tab>
        </v-tabs>
      </div>
      <div class="d-flex align-center">
        <v-btn @click="createContentsDialog = true" variant="flat" color="grey-lighten-4" rounded="lg" class="px-3 me-2"
          density="comfortable">
          <v-icon icon="mdi-format-list-group-plus" size="14" class="mr-2" color="grey-darken-3"></v-icon>
          <span class="font-weight-medium text-grey-darken-3">添加目录</span>
        </v-btn>
        <AddContents :nodeId="props.currNodeId" :pathText="props.pathText" v-model="createContentsDialog"
          @loadData="loadData"></AddContents>

        <v-btn @click="createArticleDialog = true" variant="flat" color="grey-lighten-4" rounded="lg" 
          density="comfortable" class="px-3 me-2">
          <v-icon icon="mdi-note-plus-outline" size="14" class="mr-2" color="grey-darken-3"></v-icon>
          <span class="font-weight-medium text-grey-darken-3">添加文章</span>
        </v-btn>
        <AddArticle :nodeId="props.currNodeId" v-model="createArticleDialog" @loadData="loadData"></AddArticle>

        <v-btn @click="inviteDialog = true" variant="flat" color="grey-lighten-4" rounded="lg" class="px-3"
          density="comfortable">
          <v-icon icon="mdi-account-plus-outline" size="14" class="mr-2" color="grey-darken-3"></v-icon>
          <span class="font-weight-medium text-grey-darken-3">邀请回答</span>
        </v-btn>
        <Invite :nodeId="props.currNodeId" v-model="inviteDialog"></Invite>
      </div>
    </v-row>
  </template>

  <!-- list -->
  <template v-if="tab === 'list'">
    <div>
      <div v-if="data.chosenPosting" class="pt-8">
        <Posting :posting="data.chosenPosting" :currNode="currNode" @loadData="loadData" @switchTab="switchTab" :data="data" :isLearning="isLearning" @markNodeCompleted="handleMarkNodeCompleted">
        </Posting>
        <v-divider class="mt-11" color="grey-darken-2"></v-divider>
      </div>

      <div v-for="(posting, key) in data.fixedPostings" :key="key" class="pt-8">
        <Posting :posting="posting" :currNode="currNode" @loadData="loadData" @switchTab="switchTab" :data="data" :isLearning="isLearning" @markNodeCompleted="handleMarkNodeCompleted"></Posting>
        <v-divider class="mt-11" color="grey-darken-2"></v-divider>
      </div>

      <v-infinite-scroll :items="data.otherPostings" :onLoad="loadMore" :key="scrollKey" :no-more-text="'已经到底了'"
        class="">
        <div v-for="(posting, index) in data.otherPostings" :class="index == 0 ? 'pt-4' : 'pt-8'"
          v-show="!((currNode['+'] && currNode['+'] == posting.id) || (currNode['^'] && currNode['^'].includes(posting.id)))">
          <Posting :posting="posting" :currNode="currNode" @loadData="loadData" @switchTab="switchTab" :data="data" :isLearning="isLearning" @markNodeCompleted="handleMarkNodeCompleted"></Posting>
          <v-divider class="mt-11" color="grey-darken-2"></v-divider>
        </div>
        <template v-slot:empty>
          <div class="text-body-2 text-grey py-8"> - 已经到底了 - </div>
        </template>
      </v-infinite-scroll>
    </div>
  </template>

  <template v-else-if="tab === 'addArticle'">
    <div>
      <tiptap ref="editorRef" :pathText="props.pathText" />
      <div class="pt-1 pb-2 px-0" style="position: sticky; bottom: 0px; background-color: #fff;">
        <v-btn variant="flat" color="grey-darken-2" size="large" class="rounded-lg" block
          @click="submitAddArticle">
          <span class="text-white">写好了，提交</span>
        </v-btn>
      </div>
    </div>
  </template>

  <template v-else-if="tab === 'comment'">
    <v-row class="pa-0 ma-0 my-8">
      <Comment :object="data.node" :type="1"></Comment>
    </v-row>
  </template>

  <!-- detail -->
  <template v-else>
    <Posting :data="props.data" :posting="currPosting" :currNode="currNode" :detail="true" :isLearning="isLearning" @loadData="loadData" @switchTab="switchTab" @markNodeCompleted="handleMarkNodeCompleted">
    </Posting>

    <v-row class="pa-0 ma-0 my-5">
      <Comment :object="currPosting" :type="0"></Comment>
    </v-row>
  </template>

  <!-- 跳转下一节点确认对话框 -->
  <v-dialog v-model="nextNodeDialog" width="400" persistent>
    <v-card rounded="xl" elevation="8">
      <!-- 头部 -->
      <div class="d-flex align-center justify-space-between pa-6 pb-4">
        <div class="d-flex align-center">
          <div class="pa-3 rounded-lg bg-success-lighten-5 mr-3">
            <v-icon icon="mdi-arrow-right-circle" color="success-darken-1" size="20"></v-icon>
          </div>
          <div>
            <h3 class="text-h6 font-weight-bold text-grey-darken-3">学习完成</h3>
            <p class="text-body-2 text-grey-darken-1 mb-0">恭喜完成当前节点学习！</p>
          </div>
        </div>
      </div>
      
      <v-divider></v-divider>
      
      <!-- 内容 -->
      <v-card-text class="pa-6">
        <p class="text-body-1 mb-4">
          您已经完成了当前节点的学习，是否继续学习下一个节点？
        </p>
        <div v-if="nextNodeInfo" class="d-flex align-center pa-4 bg-grey-lighten-5 rounded-lg">
          <v-icon icon="mdi-book-open-page-variant" color="primary" class="mr-3"></v-icon>
          <div>
            <div class="font-weight-medium text-grey-darken-3">下一个节点</div>
            <div class="text-body-2 text-primary">{{ nextNodeInfo.name }}</div>
          </div>
        </div>
      </v-card-text>
      
      <!-- 底部操作按钮 -->
      <v-card-actions class="pa-6 pt-0">
        <v-spacer></v-spacer>
        <v-btn
          variant="outlined"
          rounded="lg"
          @click="nextNodeDialog = false"
          class="mr-3"
        >
          稍后再学
        </v-btn>
        <v-btn
          color="success"
          variant="flat"
          rounded="lg"
          @click="goToNextNode"
        >
          <v-icon icon="mdi-arrow-right" size="16" class="mr-2"></v-icon>
          继续学习
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>


</template>

<style scoped>
:deep(.sticky-top) {
  position: sticky;
  top: 0px;
  z-index: 10;
  height: 3.8vh;
  overflow-y: auto;
}

.custom-btn-toggle .v-btn:not(.v-btn--variant-elevated) {
  color: #000;
  /* 修改未选中按钮的字体颜色 */
}
</style>