<script setup>
import { ref, onMounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService } from '@/services/learnService';
import ProfessionManagement from '@/components/admin/ProfessionManagement.vue';
import SystemConfiguration from '@/components/admin/SystemConfiguration.vue';
import SystemOperations from '@/components/admin/SystemOperations.vue';
import CourseManagement from '@/components/admin/CourseManagement.vue';

//const isLoggedIn = ref(false);
const route = useRoute();
const router = useRouter();

const showSnackbar = inject('showSnackbar');

const tab = ref('option-1');

const courseName = ref("");
const courseDesc = ref("");
const parentCourseId = ref(0);

const cleanForm = () => {
  courseName.value = "";
  courseDesc.value = "";
  parentCourseId.value = 0;
  createCourseMessageId.value = 0;
}

onMounted(async () => {
  // 组件挂载时的初始化逻辑
  console.log('Admin view mounted');
});

const postCourse = async () => {
  try {
    const response = await learnService.postCourse(courseName.value, courseDesc.value, parentCourseId.value, createCourseMessageId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('done');
      showSnackbar("添加成功！")
      cleanForm();
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
    // isLoggedIn.value = false; // 如果请求失败，认为用户未登录
  }
}

const createCourseMessageId = ref(0);

onMounted(() => {
});

watch(tab, (newValue, oldValue) => {
  console.log("Tab changed to:", newValue);
  if (newValue == "comment") {
    getCommentSensorList();
  } else if (newValue == "post") {
    getPostSensorList();
  }
});



const postMessage = async (type, toUserId, reason) => {
  try {
    const response = await learnService.postSystemMessage(type, toUserId, reason)

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('done');
      showSnackbar("操作成功！")
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
    isLoggedIn.value = false; // 如果请求失败，认为用户未登录
  }
}



const onClickCreateCourse = (name, desc, parentId, messageId) => {
  if (parentId == 0) {
    tab.value = "option-2";
    parentCourseId.value = 0;
  } else {
    tab.value = "option-3";
    parentCourseId.value = parentId;
  }
  createCourseMessageId.value = messageId;
  console.log("ss:" + courseName);
  courseName.value = name;
  courseDesc.value = desc;
}

const postList = ref([]);

const getPostSensorList = async () => {
  try {
    const response = await learnService.postCensorList()

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      postList.value = response.data;
      console.log('done');
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}

const approvePost = async (post, action) => {
  try {
    const response = await learnService.approvePost(post.id, action)

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('done');
      console.log("post: " + JSON.stringify(response.data))
      post.state = response.data.state;
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}

const commentList = ref([]);

const getCommentSensorList = async () => {
  try {
    const response = await learnService.commentCensorList()

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      commentList.value = response.data;
      console.log('done');
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}

const approveComment = async (comment, action) => {
  try {
    const response = await learnService.approveComment(comment.id, action)

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('done');
      console.log("comment: " + JSON.stringify(response.data))
      //Object.assign(comment, response.data);
      comment.state = response.data.state;
    }
  } catch (error) {
    console.error('Error verifying login status:', error);
  }
}
</script>

<template>
  <v-container class="ma-0 pa-0 bg-white" fluid>
    <!-- 页面头部 -->
    <div class="admin-header px-6 pb-4 pt-6">
      <div class="d-flex align-center">
        <v-avatar color="teal-lighten-1" size="30" class="mr-3">
          <v-icon icon="mdi-cog" color="white" size="22"></v-icon>
        </v-avatar>
        <div>
          <span class="text-h6 font-weight-bold text-grey-darken-3">管理中心</span> <br>
          <span class="text-body-2 text-grey-darken-1">系统配置与内容审核管理</span>
        </div>
      </div>
    </div>

    <div class="d-flex flex-row pt-4">
      <!-- 侧边栏 -->
      <div class="sidebar-container" style="min-width:260px;">
        <div class="pa-3 pb-2">
          <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-2 mb-3">功能模块</h3>
        </div>
        <v-tabs v-model="tab" color="teal" direction="vertical" class="px-2" flat>
          <v-tab prepend-icon="mdi-cog-outline" text="系统配置" value="option-1"
            class="text-body-1 text-grey-darken-2 my-2 rounded-lg justify-start"></v-tab>
          <v-tab prepend-icon="mdi-cog-sync" text="系统操作" value="system-operations"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>

          <div class="px-3 py-3">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">课程管理</div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <v-tab prepend-icon="mdi-briefcase-check-outline" text="职业管理" value="option-7"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>
          <v-tab prepend-icon="mdi-email-outline" text="课程管理" value="option-6"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>


          <!--
          <v-tab prepend-icon="mdi-book-plus-outline" text="创建课程" value="option-2"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>
          <v-tab prepend-icon="mdi-book-plus-multiple-outline" text="创建子课程" value="option-3"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>
          -->

          <div class="px-3 py-3">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">内容审核</div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <v-tab prepend-icon="mdi-note-check-outline" text="审核文章" value="post"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>
          <v-tab prepend-icon="mdi-comment-check-outline" text="审核评论" value="comment"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"></v-tab>
        </v-tabs>
      </div>

      <!-- 主内容区域 -->
      <div class="flex-grow-1 px-4 pl-6">
        <!-- 系统配置 -->
        <v-card v-if="tab == 'option-1'" flat class="pa-6" rounded="lg">
          <SystemConfiguration />
        </v-card>
        
        <!-- 系统操作 -->
        <v-card v-if="tab == 'system-operations'" flat class="pa-6" rounded="lg">
          <SystemOperations />
        </v-card>

        <!-- 职业申请管理 -->
        <v-card v-if="tab == 'option-7'" flat class="pa-6" rounded="lg">
          <ProfessionManagement />
        </v-card>

        <!-- 课程申请管理 -->
        <v-card v-if="tab == 'option-6'" flat class="pa-6" rounded="lg">
          <CourseManagement />
        </v-card>


        <v-card v-if="tab == 'option-2'" flat class="pa-6" rounded="lg">
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-book-plus-outline" color="teal" class="mr-3"></v-icon>
            <h3 class="text-h6 font-weight-bold">创建课程</h3>
          </div>
          <v-text-field v-model="courseName" class="mb-4" label="课程名称" variant="outlined" rounded="lg"
            bg-color="white"></v-text-field>
          <v-textarea v-model="courseDesc" label="课程简介" variant="outlined" rows="5" class="mb-4" rounded="lg"
            bg-color="white"></v-textarea>
          <v-text-field v-if="createCourseMessageId != 0" class="mb-4" label="课程请求ID" variant="outlined" disabled
            v-model="createCourseMessageId" rounded="lg" bg-color="white"></v-text-field>
          <div>
            <v-btn variant="flat" color="teal" class="mr-4" rounded="lg" @click="postCourse">
              <v-icon icon="mdi-plus" class="mr-2"></v-icon>
              创建
            </v-btn>
            <v-btn variant="flat" color="grey-lighten-3" rounded="lg" @click="clearForm">
              <v-icon icon="mdi-refresh" class="mr-2"></v-icon>
              清除
            </v-btn>
          </div>
        </v-card>

        <v-card v-if="tab == 'option-3'" flat class="pa-6" rounded="lg">
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-book-plus-multiple-outline" color="teal" class="mr-3"></v-icon>
            <h3 class="text-h6 font-weight-bold">创建子课程</h3>
          </div>
          <v-text-field v-model="courseName" class="mb-4" label="子课程名称" variant="outlined" rounded="lg"
            bg-color="white"></v-text-field>
          <v-text-field v-model="parentCourseId" class="mb-4" label="父课程ID" variant="outlined" rounded="lg"
            bg-color="white"></v-text-field>
          <v-textarea v-model="courseDesc" label="子课程简介" variant="outlined" rows="5" class="mb-4" rounded="lg"
            bg-color="white"></v-textarea>
          <v-text-field v-if="createCourseMessageId != 0" class="mb-4" label="课程请求ID" variant="outlined" disabled
            v-model="createCourseMessageId" rounded="lg" bg-color="white"></v-text-field>
          <div>
            <v-btn variant="flat" color="teal" class="mr-4" rounded="lg" @click="postCourse">
              <v-icon icon="mdi-plus" class="mr-2"></v-icon>
              创建子课程
            </v-btn>
            <v-btn variant="flat" color="grey-lighten-3" rounded="lg" @click="clearForm">
              <v-icon icon="mdi-refresh" class="mr-2"></v-icon>
              清除
            </v-btn>
          </div>
        </v-card>

        <v-card v-if="tab == 'post'" flat class="pa-6" rounded="lg">
          <div class="d-flex align-center justify-space-between mb-6">
            <div class="d-flex align-center">
              <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
                <v-icon icon="mdi-note-check-outline" color="teal-darken-1" size="20"></v-icon>
              </div>
              <div>
                <h3 class="text-h6 font-weight-bold text-grey-darken-3">文章审核</h3>
                <p class="text-body-2 text-grey-darken-1 mb-0">审核用户提交的文章内容</p>
              </div>
            </div>
            <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
              <v-icon icon="mdi-file-document-multiple" color="blue-darken-2" size="16" class="mr-1"></v-icon>
              <span class="text-blue-darken-2 text-caption">{{ postList.length }} 篇待审核</span>
            </v-chip>
          </div>

          <div v-if="postList.length === 0" class="text-center py-12">
            <v-icon icon="mdi-file-document-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
            <p class="text-body-1 text-grey-darken-1">暂无待审核的文章</p>
          </div>

          <div v-for="post in postList" :key="post.id" class="mb-4">
            <v-card flat class="border rounded-lg pa-5" hover>
              <div class="d-flex align-start">
                <!-- 状态和操作区域 -->
                <div class="mr-4" style="min-width: 200px;">
                  <div class="mb-3">
                    <v-chip v-if="post.state == 0" variant="flat" color="orange-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                      待审核
                    </v-chip>
                    <v-chip v-if="post.state == 1" variant="flat" color="green-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      已通过
                    </v-chip>
                    <v-chip v-if="post.state == 2" variant="flat" color="red-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                      已拒绝
                    </v-chip>
                  </div>
                  <div class="d-flex flex-column ga-2">
                    <v-btn variant="flat" color="green-lighten-4" rounded="lg" size="small" @click="approvePost(post, 1)">
                      <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                      通过
                    </v-btn>
                    <v-btn variant="flat" color="red-lighten-4" rounded="lg" size="small" @click="approvePost(post, 0)">
                      <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                      拒绝
                    </v-btn>
                  </div>
                </div>

                <!-- 内容区域 -->
                <div class="flex-grow-1">
                  <div class="d-flex align-center mb-3">
                    <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                      <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
                    </v-avatar>
                    <div>
                      <div class="text-body-2 font-weight-medium text-grey-darken-2">文章ID: {{ post.id }}</div>
                      <div class="text-caption text-grey-darken-1">{{ post.ctime }}</div>
                    </div>
                  </div>

                  <div class="bg-grey-lighten-5 rounded-lg pa-4">
                    <div v-if="post.type == 2" class="tiptap post-content" v-html="post.content"></div>
                    <div v-if="post.type == 1">
                      <div class="text-caption text-grey-darken-1 mb-2">目录：</div>
                      <div class="gap-2">
                        <v-chip v-for="(item, index) in post.content.split(',')" :key="index" style="display: block;"
                          variant="flat" color="grey-lighten-4" rounded="lg" class="my-2 py-1">
                          {{ item.trim() }}
                        </v-chip>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </v-card>
          </div>
        </v-card>

        <v-card v-if="tab == 'comment'" flat class="pa-6" rounded="lg">
          <div class="d-flex align-center justify-space-between mb-6">
            <div class="d-flex align-center">
              <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
                <v-icon icon="mdi-comment-check-outline" color="teal-darken-1" size="20"></v-icon>
              </div>
              <div>
                <h3 class="text-h6 font-weight-bold text-grey-darken-3">评论审核</h3>
                <p class="text-body-2 text-grey-darken-1 mb-0">审核用户提交的评论内容</p>
              </div>
            </div>
            <v-chip variant="flat" color="purple-lighten-4" rounded="lg">
              <v-icon icon="mdi-comment-multiple" color="purple-darken-2" size="16" class="mr-1"></v-icon>
              <span class="text-purple-darken-2 text-caption">{{ commentList.length }} 条待审核</span>
            </v-chip>
          </div>

          <div v-if="commentList.length === 0" class="text-center py-12">
            <v-icon icon="mdi-comment-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
            <p class="text-body-1 text-grey-darken-1">暂无待审核的评论</p>
          </div>

          <div v-for="comment in commentList" :key="comment.id" class="mb-4">
            <v-card flat class="border rounded-lg pa-5" hover>
              <div class="d-flex align-start">
                <!-- 状态和操作区域 -->
                <div class="mr-4" style="min-width: 200px;">
                  <div class="mb-3">
                    <v-chip v-if="comment.state == 0" variant="flat" color="orange-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                      待审核
                    </v-chip>
                    <v-chip v-if="comment.state == 1" variant="flat" color="green-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                      已通过
                    </v-chip>
                    <v-chip v-if="comment.state == 2" variant="flat" color="red-lighten-4" rounded="lg" size="small">
                      <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                      已拒绝
                    </v-chip>
                  </div>
                  <div class="d-flex flex-column ga-2">
                    <v-btn variant="flat" color="green-lighten-4" rounded="lg" size="small" @click="approveComment(comment, 1)">
                      <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                      通过
                    </v-btn>
                    <v-btn variant="flat" color="red-lighten-4" rounded="lg" size="small" @click="approveComment(comment, 0)">
                      <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                      拒绝
                    </v-btn>
                  </div>
                </div>

                <!-- 内容区域 -->
                <div class="flex-grow-1">
                  <div class="d-flex align-center justify-space-between mb-3">
                    <div class="d-flex align-center">
                      <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                        <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
                      </v-avatar>
                      <div>
                        <div class="text-body-2 font-weight-medium text-grey-darken-2">评论ID: {{ comment.id }}</div>
                        <div class="text-caption text-grey-darken-1">{{ comment.ctime }}</div>
                      </div>
                    </div>
                    <v-btn variant="outlined" color="teal" size="small" rounded="lg" 
                           :href="`/read?commentId=${comment.id}`" target="_blank">
                      <v-icon icon="mdi-open-in-new" size="14" class="mr-1"></v-icon>
                      查看原文
                    </v-btn>
                  </div>

                  <div class="bg-grey-lighten-5 rounded-lg pa-4">
                    <div class="text-caption text-grey-darken-1 mb-2">评论内容：</div>
                    <div class="text-body-1 text-grey-darken-2 line-height-relaxed">
                      {{ comment.content }}
                    </div>
                  </div>
                </div>
              </div>
            </v-card>
          </div>
        </v-card>
      </div>
    </div>
  </v-container>
</template>

<style scoped>
/* 页面头部样式 */
.admin-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

/* 侧边栏样式 */
.sidebar-container {
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  padding-right: 1rem;
}

/* Tab样式优化 */
:deep(.v-tab--selected) {
  background-color: rgb(var(--v-theme-teal-lighten-5)) !important;
  color: rgb(var(--v-theme-teal-darken-2)) !important;
}

:deep(.v-tab) {
  color: rgb(var(--v-theme-grey-darken-1));
  margin-bottom: 4px;
  text-transform: none !important;
  font-weight: 500 !important;
  justify-content: flex-start !important;
  padding-left: 16px !important;
}

:deep(.v-tab:hover) {
  background-color: rgba(0, 150, 136, 0.04) !important;
}

/* 卡片边框样式 */
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 卡片悬停效果 */
:deep(.v-card[hover]:hover) {
  transform: translateY(-2px);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

/* 文章内容样式 */
.post-content {
  max-height: 700px;
  overflow-y: auto;
  line-height: 1.6;
  padding: 2px 12px
}

.post-content :deep(p) {
  margin-bottom: 8px;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  margin-bottom: 8px;
  margin-top: 16px;
}

/* 行高优化 */
.line-height-relaxed {
  line-height: 1.7;
}

/* 文本区域样式 */
.config-textarea :deep(.v-field__input) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
  font-size: 13px !important;
  line-height: 1.5 !important;
}

:deep(.v-field__input) {
  font-size: 14px !important;
}

:deep(.tiptap) {
  line-height: 1.6;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.border-dashed {
  border-style: dashed;
}

.w-85 {
  max-width: 85%;
}
</style>
