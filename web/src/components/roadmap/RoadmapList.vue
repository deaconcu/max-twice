<script setup>
import { computed, inject } from 'vue';
import { useI18n } from 'vue-i18n';
import { roadmapServiceV1, progressServiceV1 } from '@/services/api/v1/apiServiceV1';
import RoadmapCard from './RoadmapCard.vue';

const props = defineProps({
  roadmaps: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  },
  pinnedRoadmaps: {
    type: Array,
    default: () => []
  },
  professionId: {
    type: String,
    default: '1'
  }
});

const emit = defineEmits([
  'open-detail', 
  'copy-roadmap',
  'create-roadmap',
  'roadmaps-updated'
]);

const { t } = useI18n();
const showSnackbar = inject('showSnackbar');

// 检查是否置顶
const isPinned = (roadmapId) => {
  return props.pinnedRoadmaps.includes(roadmapId);
};

// 投票功能
const handleVote = async (roadmap, event) => {
  event.stopPropagation(); // 阻止卡片点击事件
  
  try {
    const response = await roadmapServiceV1.upvoteRoadmap(roadmap.id);
    console.log('投票响应:', response);
    if (response.code === 200) {
      // 更新本地投票数
      roadmap.vote = response.data.vote;
      roadmap.upvoted = response.data.upvoted; // 标记已投票
      if (roadmap.upvoted) {
        showSnackbar(t('roadmap.voteSuccess'));
      } else {
        showSnackbar(t('roadmap.voteCancel'));
      }
    } else {
      showSnackbar(t('roadmap.voteFailed'));
    }
  } catch (error) {
    console.error('Error voting roadmap:', error);
    showSnackbar('投票失败，请稍后重试');
  }
};

// 置顶功能
const handleTogglePin = async (roadmap, event) => {
  event.stopPropagation(); // 阻止卡片点击事件
  
  try {
    const response = await roadmapServiceV1.pinRoadmap(props.professionId, roadmap.id);
    console.log('置顶响应:', response);
    
    if (response.code === 200) {
      const status = response.data;
      
      if (status === "pinned") {
        // 置顶成功：更新课程状态，移动到第一个位置
        roadmap.pinned = true;
        
        // 从当前位置移除，添加到最前面
        const index = props.roadmaps.findIndex(c => c.id === roadmap.id);
        if (index > -1) {
          const [pinnedRoadmap] = props.roadmaps.splice(index, 1);
          props.roadmaps.unshift(pinnedRoadmap);
        }
        
        showSnackbar(t('roadmap.pinSuccess'));
      } else if (status === "unpinned") {
        // 取消置顶：更新课程状态，移动到非置顶区第一个
        roadmap.pinned = false;
        
        // 先找到当前课程的位置
        const currentIndex = props.roadmaps.findIndex(c => c.id === roadmap.id);
        
        if (currentIndex > -1) {
          // 先移除当前课程
          const [unpinnedRoadmap] = props.roadmaps.splice(currentIndex, 1);
          
          // 在剩余课程中找到第一个非置顶课程的位置
          const firstUnpinnedIndex = props.roadmaps.findIndex(c => !c.pinned);
          
          // 如果有非置顶课程，插入到第一个非置顶课程位置；否则添加到末尾
          const insertIndex = firstUnpinnedIndex > -1 ? firstUnpinnedIndex : props.roadmaps.length;
          props.roadmaps.splice(insertIndex, 0, unpinnedRoadmap);
        }
        
        showSnackbar(t('roadmap.unpinSuccess'));
      }
      
      // 通知父组件更新
      emit('roadmaps-updated');
    } else {
      showSnackbar(t('roadmap.pinFailed'));
    }
  } catch (error) {
    console.error('Error toggling pin roadmap:', error);
    showSnackbar('置顶操作失败，请稍后重试');
  }
};

// 开始学习功能
const handleStartLearning = async (roadmap, event) => {
  event.stopPropagation(); // 阻止卡片点击事件
  
  try {
    const response = await progressServiceV1.startRoadmap(roadmap.id);
    
    if (response.code === 200) {
      showSnackbar(t('roadmap.startLearningSuccess'));
      roadmap.learning = response.data;
    } else {
      showSnackbar(t('roadmap.startLearningFailed'));
      roadmap.learning = false;
    }
  } catch (error) {
    console.error('Error starting roadmap:', error);
    showSnackbar(t('roadmap.startLearningFailed'));
    roadmap.isLearning = false;
  }
};

// 事件处理
const handleOpenDetail = (roadmap) => {
  emit('open-detail', roadmap);
};

const handleCopy = (roadmap, event) => {
  event.stopPropagation(); // 阻止卡片点击事件
  emit('copy-roadmap', roadmap);
};

const handleCreateRoadmap = () => {
  emit('create-roadmap');
};
</script>

<template>
  <div class="roadmap-list">
    <!-- 加载和错误提示 -->
    <div v-if="loading || error" class="text-center py-8">
      <v-progress-circular v-if="loading" indeterminate color="primary" size="64"></v-progress-circular>
      <p v-if="loading" class="text-grey-darken-2 mt-4">正在加载课程表...</p>
      <v-alert v-if="error" type="error" variant="tonal" class="mt-4">{{ error }}</v-alert>
    </div>

    <!-- 课程表列表 -->
    <div v-else>
      <v-row v-if="roadmaps.length > 0">
        <RoadmapCard
          v-for="roadmap in roadmaps" 
          :key="roadmap.id"
          :roadmap="roadmap"
          :is-pinned="isPinned(roadmap.id)"
          @open-detail="handleOpenDetail"
          @vote="handleVote"
          @copy="handleCopy"
          @toggle-pin="handleTogglePin"
          @start-learning="handleStartLearning"
        />
      </v-row>

      <!-- 空状态 -->
      <v-row v-else>
        <v-col cols="12">
          <v-card flat class="text-center py-12" color="grey-lighten-5" rounded="lg">
            <v-icon icon="mdi-book-open-page-variant-outline" size="64" color="grey-lighten-1" class="mb-4"></v-icon>
            <h3 class="text-h6 text-grey-darken-1 mb-2">暂无课程表</h3>
            <p class="text-body-2 text-grey mb-4">{{ t('roadmap.firstToCreate') }}</p>
            <v-btn variant="flat" color="primary" @click="handleCreateRoadmap">
              <v-icon icon="mdi-plus" class="mr-2"></v-icon>
              {{ t('roadmap.createFirst') }}
            </v-btn>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </div>
</template>

<style scoped>
.roadmap-list {
  /* 路线图列表样式 */
}
</style>