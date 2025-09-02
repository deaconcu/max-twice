<script setup>
import { ref, inject, nextTick, computed } from 'vue';
import { userServiceV1, postServiceV1 } from '@/services/api/v1/apiServiceV1';
import UserPosting from '@/components/user/UserPosting.vue';
import Comment from '../read/Comment.vue';
import Tiptap from '../read/Tiptap.vue';
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();
const showSnackbar = inject('showSnackbar');

// Props
const props = defineProps({
  postType: {
    type: String,
    required: true,
    validator: value => ['article', 'content'].includes(value)
  },
  userId: {
    type: [String, Number],
    default: null
  }
});

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.userId);

// 内部状态管理
const mainArea = ref('list'); // 'list', 'detail', 'edit'
const currPosting = ref(null);
const scrollPosition = ref(0);
const lastPage = ref('');
const editorRef = ref(null);

// 文章列表数据
const postList = ref([]);
const lastPostId = ref(0x7FFFFFFF);

// 加载文章数据
async function loadPosts({ done }) {
  try {
    let response = '';
    response = await userServiceV1.getUserPosts(targetUserId.value, lastPostId.value, props.postType);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      postList.value.push(...response.data);

      if (response.data.length > 0) {
        lastPostId.value = response.data[response.data.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
}

// 内部处理区域切换
const switchMainArea = (value, posting) => {
  lastPage.value = mainArea.value;
  if (value == "list") {
    mainArea.value = 'list';
    nextTick(() => {
      window.scrollTo(0, scrollPosition.value);
    });
  } else if (value == "edit") {
    mainArea.value = 'edit';
    currPosting.value = posting;
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
  } else if (value == "detail") {
    mainArea.value = 'detail';
    currPosting.value = posting;
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
  }
};

const switchToLastPage = () => {
  console.log("lastPage: " + lastPage.value);
  switchMainArea(lastPage.value, currPosting.value);
};

// 修改文章
const modifyPosting = async () => {
  try {
    console.log("begin post");

    const response = await postServiceV1.updatePost(currPosting.value.id, { content: editorRef.value.editor.getHTML() });
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      currPosting.value.content = editorRef.value.editor.getHTML();
      switchToLastPage();
    }
  } catch (error) {
    console.error('Error submitting form:', error);
  }
}

// 处理删除文章
const handleDeletePosting = (id) => {
  postList.value = postList.value && postList.value.filter(item => item.id !== id);
};
</script>

<template>
  <div>
    <!-- 列表视图 -->
    <div v-if="mainArea == 'list'">
      <v-infinite-scroll 
        :items="postList" 
        :onLoad="loadPosts" 
        :key="postType" 
        :no-more-text="'已经到底了'"
        style="position: relative;top:-12px">
        
        <div v-for="(posting, index) in postList" :key="posting.id">
          <v-row class="ma-0 border-b px-0 pb-7" :class="{ 'pt-10': index != 0 }">
            
            <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
              <div class="d-flex align-center text-body-1">
                <a class="text-grey pe-2" 
                   :href="'/read?courseId=' + posting.node.course.id" 
                   target="_blank">
                  {{ posting.node.course.name }}
                </a>
                <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-1"></v-icon>
                <a class="text-grey ps-2"
                   :href="'/read?courseId=' + posting.node.course.id + '&nodeId=' + posting.node.id"
                   target="_blank">
                  {{ posting.node.name }}
                </a>
              </div>
            </div>

            <UserPosting 
              :posting="posting" 
              :type="'list'" 
              @switchMainArea="switchMainArea"
              @deletePosting="handleDeletePosting">
            </UserPosting>

          </v-row>
        </div>

        <template v-slot:empty>
          <div class="text-body-2 text-grey py-5">已经到底了</div>
        </template>
      </v-infinite-scroll>
    </div>

    <!-- 详情视图 -->
    <div v-if="mainArea == 'detail'">
      <UserPosting 
        :posting="currPosting" 
        :type="'detail'" 
        @switchMainArea="switchMainArea"
        @deletePosting="handleDeletePosting">
      </UserPosting>
      <v-row class="pa-0 ma-0 my-7">
        <Comment :posting="currPosting"></Comment>
      </v-row>
    </div>

    <!-- 编辑视图 -->
    <div v-if="mainArea == 'edit'">
      <v-row class="mx-0 sticky-top mb-1" align="center"
        style="background-color: white;transform: translateX(-38px); width: 110%;">
        <v-btn variant="flat" class="me-0" color="" density="comfortable" icon="mdi-chevron-left"
          @click="switchToLastPage"></v-btn>
        <span class="ps-1 font-weight-bold text-body-1">修改文章</span>
      </v-row>
      <tiptap ref="editorRef" pathText="" :content="currPosting.content" />
      <div class="pt-1 pb-2 px-0" style="position: sticky; bottom: 0px; background-color: #fff;">
        <v-btn variant="tonal" color="teal" size="large" class="rounded-lg" block
          @click="modifyPosting">写好了，提交</v-btn>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.sticky-top) {
  position: sticky;
  top: 49px;
  z-index: 10;
  height: 3.8vh;
  overflow-y: auto;
}

.v-infinite-scroll__side {
  display: none !important;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>