<script setup>
import { ref, onMounted, nextTick} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { learnService } from '@/services/learnService';

const { t } = useI18n();
const route = useRoute();
const props = defineProps(['commentId', 'comments', 'count', 'activeReplyId', 'offsetId']);
const offsetId = ref(0);
const displayLoadMore = ref(true);
const highlightedSubCommentId = ref(null);

// 设置子评论高亮并在5秒后移除
const setSubHighlight = (subCommentId) => {
  highlightedSubCommentId.value = subCommentId;
  setTimeout(() => {
    highlightedSubCommentId.value = null;
  }, 5000);
};

const emit = defineEmits(['update:activeReplyId'])

function updateActiveReplayId(newValue) {
  if (props.activeReplyId === newValue) {
    emit('update:activeReplyId', 0);
  } else {
    emit('update:activeReplyId', newValue);
  }
}

onMounted(() => {
  console.log("offsetId: " + props.offsetId);
  if (props.comments.length > 0) {
    offsetId.value = props.comments[props.comments.length - 1].id;
  }

  if (props.offsetId > offsetId.value) {
    props.comments = [];
    offsetId.value = props.offsetId - 1;
    loadMore({ done: (status) => console.log('Initial load:', status) });
  }
  
  // 如果有subCommentId，设置子评论高亮
  if ('subCommentId' in route.query) {
    nextTick(() => {
      setSubHighlight(parseInt(route.query.subCommentId));
    });
  }
});

const loadMore = async () => {
  try {
    const response = await learnService.getCommentsByTopic(props.commentId, offsetId.value);

    if (response.code === 401) {
      console.log('not login');
      //router.push('/login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      //comments.value = response.data;
      props.comments.push(...response.data)

      if (response.data.length > 0) {
        offsetId.value = response.data[response.data.length - 1].id;
      } else {
        displayLoadMore.value = false;
      }
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
    isLoggedIn.value = false; // 如果请求失败，认为用户未登录
  }
}

const upvote = async (comment) => {
  try {
    console.log("begin post");

    const response = await learnService.upvote(comment.id, 2, 2);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      comment.upvoteCount = response.data.upvoteCount;
      comment.upvoted = response.data.upvoted; 
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}
</script>

<template>
  <div  class="mt-4">
    <template v-for="(comment, key) in props.comments" :key="key">
      <v-row class="mx-0 pt-2 pb-1 w-100" :class="{highlighted: highlightedSubCommentId == comment.id}">
        <div>
          <v-avatar icon="mdi-account" size="30" color="red" class="mr-3">
            <span class="text-body-1">CJ</span>
          </v-avatar>
        </div>
        <div style="width:90%">
          <div class="text-body-2 mb-2 text-grey-darken-1">{{ t('subcomment.username') }} 
            <span class="ms-2 text-caption text-grey">{{ comment.ctime }}</span>
          </div>
          <div class="">{{ comment.content }} </div>
          <div class="ma-0 py-2 pb-1 d-flex align-center justify-start text-grey-darken-1">
            <v-btn class="ms-0" variant="flat" :color="comment.upvoted > 0 ? 'teal' : 'grey-lighten-4'" rounded="xl"
              density="compact" @click="upvote(comment)"
              prepend-icon="mdi-arrow-up">{{ comment.upvoteCount }}</v-btn>

            <v-btn class="mx-3" variant="text" density="compact" prepend-icon="mdi-chat-outline"
              @click="updateActiveReplayId(comment.id)">{{ t('subcomment.reply') }}</v-btn>
            
          </div>
          <div class="mt-2 mb-2">
            <v-text-field v-if="activeReplyId === comment.id" v-model="replyContent" variant="outlined"
              density="compact" append-inner-icon="mdi-email-fast-outline" @click:append-inner="sendComment"
              :placeholder="t('subcomment.addComment')" class="w-100" hide-details></v-text-field>
          </div>
        </div>
      </v-row>
    </template>
    <v-btn v-if="props.count>1 && displayLoadMore" variant="plain" @click="loadMore" class="pa-0 ma-0">{{ t('subcomment.viewMoreComments') }}</v-btn>
    </div>
</template>

<style scoped>
.highlighted{
  background: #fafafa !important;
  transition: background-color 1s ease;
}
</style>
