<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { commentServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1';
import Subcomment from './Subcomment.vue';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const props = defineProps(['object', 'type']);

const comments = ref([]);
const inputComment = ref("");
const offsetId = ref(0);
const scrollKey = ref(Date.now());
const activeReplyId = ref(null);
const replyContent = ref('');
const targetItem = ref(null);
const highlightedCommentId = ref(null);
const commentArea = ref(null);

// 用于获取目标元素的ref函数
const setTargetRef = (el) => {
  if (el) {
    targetItem.value = el;
  }
};

// 设置高亮并在5秒后移除
const setHighlight = (commentId) => {
  highlightedCommentId.value = commentId;
  setTimeout(() => {
    highlightedCommentId.value = null;
  }, 5000);
};

async function load({ done }) {
  try {
    if ('commentId' in route.query && offsetId.value == 0) {
      offsetId.value = route.query.commentId - 4 > 0 ? route.query.commentId - 4 : 0;
    }
    const response = await commentServiceV1.getComments(props.object.id, props.type, offsetId.value);

    if (response.code === 401) {
      console.log('not login');
      //router.push('/login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      comments.value.push(...response.data)

      if (response.data.length > 0) {
        offsetId.value = response.data[response.data.length - 1].id;
        done('ok')

        await nextTick();
        //const el = (Array.isArray($refs.targetItem) ? $refs.targetItem[0] : $refs.targetItem)
        console.log('targetItem.value: ' + targetItem.value);
        targetItem.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })

        // 如果有commentId且没有subCommentId，设置主评论高亮
        if ('commentId' in route.query && !('subCommentId' in route.query)) {
          setHighlight(parseInt(route.query.commentId));
        }
      } else {
        done('empty')
        console.log('set empty');
      }
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  } finally {
  }
};

const sendComment = async (content) => {
  try {
    console.log("begin post");

    const response = await commentServiceV1.createComment(props.object.id, props.type, 0, 0, inputComment.value);
    inputComment.value = "";
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      comments.value.unshift(response.data);
      props.object.commentCount++;
      //offsetId.value = 0;
      //comments.value = [];
      //scrollKey.value = Date.now();
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
};

const sendSubcomment = async (comment) => {
  try {
    console.log("begin post");

    const response = await commentServiceV1.createComment(props.object.id, props.type, activeReplyId.value, 0, replyContent.value);
    replyContent.value = "";
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      props.object.commentCount++;
      //offsetId.value = 0;
      if (comment.children == null) {
        comment.children = [response.data];
      } else {
        comment.children.unshift(response.data);
      }
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
};

const returnToAllComment = () => {
  let url = "";
  if (props.type == 0) {
    url = "/read?postId=" + props.object.id;
  } else {
    url = "/read?nodeId=" + props.object.id + "&tab=comment";
  }
  window.location.href = url.toString();
}

onMounted(async () => {
  await nextTick();
  commentArea.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
});

const upvote = async (comment) => {
  try {
    console.log("begin post");

    const response = await upvoteServiceV1.upvote(comment.id, 2, 2);
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
  <v-text-field v-model="inputComment" variant="outlined" density="compact" append-inner-icon="mdi-email-fast-outline"
    @click:append-inner="sendComment" :placeholder="t('comment.addComment')" class="w-100"></v-text-field>
  <a variant="text" v-if="'commentId' in route.query" @click="returnToAllComment" class="cursor-pointer"
    ref="commentArea">... {{ t('comment.viewAllComments') }}</a>
  <v-infinite-scroll :items="comments" :onLoad="load" :key="scrollKey" class="w-100">
    <div v-for="(comment, key) in comments" :key="comment.id"
      :ref="comment.id == route.query.commentId ? setTargetRef : null">
      <v-row class="mx-0 my-0 py-2 w-100" :class="{ highlighted: highlightedCommentId == comment.id }">
        <div>
          <v-avatar icon="mdi-account" size="30" color="grey" class="mr-3">
            <span class="text-body-1">CJ</span>
          </v-avatar>
        </div>
        <div style="width:90%">
          <div class="text-body-2 mb-2 text-grey-darken-1">{{ t('comment.username') }} 
            <span class="ms-2 text-caption text-grey">{{ comment.createdAt}}</span>
          </div>
          <div>{{ comment.content }} </div>
          <div class="ma-0 mt-3 pb-1 d-flex align-center justify-start text-grey-darken-1 text-body-2">

            <v-btn class="ms-0" variant="flat" :color="comment.upvoted > 0 ? 'teal' : 'grey-lighten-4'" rounded="xl"
              density="compact" @click="upvote(comment)" prepend-icon="mdi-arrow-up">{{ comment.upvoteCount }}</v-btn>

            <v-btn class="mx-3" variant="text" density="compact" prepend-icon="mdi-chat-outline"
              :color="activeReplyId === comment.id ? 'grey-lighten-4' : ''"
              @click="activeReplyId = (activeReplyId == comment.id) ? 0 : comment.id">{{ t('comment.reply') }}</v-btn>
          </div>
          <div class="mt-2 mb-2">
            <v-text-field v-if="activeReplyId === comment.id" v-model="replyContent" variant="outlined"
              density="compact" append-inner-icon="mdi-email-fast-outline" @click:append-inner="sendSubcomment(comment)"
              :placeholder="t('comment.addComment')" class="w-100" hide-details></v-text-field>
          </div>
          <Subcomment v-if="comment.children != null" v-model:activeReplyId="activeReplyId" :commentId="comment.id"
            :comments="comment.children" :count="comment.replyCount"
            :offsetId="comment.id == route.query.commentId && 'subCommentId' in route.query ? route.query.subCommentId : 0"
            ref="subcommentRef"></Subcomment>
        </div>
      </v-row>
    </div>
    <template v-slot:empty>
      <div v-if="comments.length > 0" class="text-grey py-4">- {{ t('comment.endOfComments') }} -</div>
      <div v-else class="py-2 text-center">
        <v-icon size="50" color="grey-lighten-2" class="mb-2">mdi-comment-text-outline</v-icon>
        <div class="text-grey-lighten-1 text-body-2 mb-2">{{ t('comment.noComments') }}</div>
      </div>
    </template>
  </v-infinite-scroll>
</template>

<style scoped>
.highlighted {
  background: #fafafa !important;
  transition: background-color 1s ease;
}
</style>
