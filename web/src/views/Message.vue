<script setup>
import { ref, onMounted, inject, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService } from '@/services/learnService'
import draggable from 'vuedraggable';
import { useUserStore } from "@/stores/user";
import apiClient from '@/services/apiClient';
import RightSidebar from '@/components/RightSidebar.vue';

const user = useUserStore();
console.log("user id: " + JSON.stringify(user));

//const isLoggedIn = ref(false);
const route = useRoute();
const router = useRouter();

const showSnackbar = inject('showSnackbar');

const items = ref([
  { text: '系统通知', icon: 'mdi-chat-outline', value: "system" },
  { text: '课程申请', icon: 'mdi-chat-outline', value: "courseApply" },
  //{ text: '用户私信', icon: 'mdi-chat-outline', value: "private" },
])

const messages = ref([
  { text: '个人信息', name: 'ADMIN', date: "2025-03-05 22:55:22" },
  { text: '我关注的课程', name: 'ADMIN', date: "2025-03-05 21:55:22" },
  { text: '我关注的人', name: 'ADMIN', date: "2025-03-05 20:55:22" },
  { text: '我创建的目录', name: 'ADMIN', date: "2025-03-04 22:55:22" },
  { text: '我创建的文章', name: 'ADMIN', date: "2025-03-03 22:55:22" },
  { text: '我的消息', name: 'ADMIN', date: "2025-03-02 22:55:22" },
])

const relatedLinks = ['高等数学', '概率论', 'C++编程实现', '软件测试']

// 添加统计数据
const unreadCount = ref(5);
const totalMessages = ref(142);
const todayMessages = ref(3);
const weekMessages = ref(15);
const unreadMessages = ref(2);

const selected = ref("system");

const users = ref([
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/1.jpg',
    title: 'John Leider'
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/2.jpg',
    title: 'Summer BBQ',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/3.jpg',
    title: 'Oui oui',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/4.jpg',
    title: 'Birthday gift',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/5.jpg',
    title: 'Recipe to try',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/1.jpg',
    title: 'John Leider'
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/2.jpg',
    title: '数据结构与算法',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/3.jpg',
    title: 'Oui oui',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/4.jpg',
    title: 'Birthday gift',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/5.jpg',
    title: 'Recipe to try',
  },
]);

const topics = ref(
  [
    "计算机基础",
    "数据结构与算法",
    "高等数学",
    "大学物理",
    "有机化学",
    "线性代数",
    "心理学导论",
    "经济学原理",
    "市场营销",
    "英语写作",
    "日语入门",
    "艺术概论",
    "摄影基础",
    "建筑设计",
    "电子电路",
    "操作系统",
    "计算机网络",
    "人工智能导论",
    "数据分析",
    "机器学习",
    "国际贸易",
    "财务管理",
    "法律基础",
    "刑法学",
    "环境科学"
  ]);

const years = ref([
  {
    color: 'cyan',
    year: '1960',
  },
  {
    color: 'green',
    year: '1970',
  },
  {
    color: 'pink',
    year: '1980',
  },
  {
    color: 'amber',
    year: '1990',
  },
  {
    color: 'orange',
    year: '2000',
  },
]);

const sendMessage = () => {

}

onMounted(() => {
  //loadData();
});

const messageList = ref([]);
const systemMessageType = ref(99);
const lastId = ref(0x7fffffff);


watch(selected, (newValue, oldValue) => {
  console.log("selected changed");
  messageList.value = [];
  lastId.value = 0x7fffffff;
});

watch(systemMessageType, (newValue, oldValue) => {
  console.log("system message type changed");
  messageList.value = [];
  lastId.value = 0x7fffffff;
  systemMessageListKey.value++;
})

const lastReadTime = ref(null);
async function loadData({ done }) {
  //messageList.value = [];
  try {
    let response = '';
    if (selected.value == 'courseApply') {
      response = await learnService.getApplyCourseMessageByUser(user.userId, lastId.value);
    } else if (selected.value == 'system') {
      response = await learnService.getSystemMessageByUser(systemMessageType.value, user.userId, lastId.value);
    }

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      const appendList = response.data.messages;
      /*
      const appendList = response.data.messages.map(item => ({
        ...item,
        content: JSON.parse(item.content)
      }))
        */

      if (lastReadTime.value == null) {
        lastReadTime.value = new Date(response.data.lastReadTime);
      }
      messageList.value.push(...appendList);

      if (appendList.length > 0) {
        lastId.value = appendList[appendList.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

const getField = (content, name) => {
  try {
    const jsonContent = JSON.parse(content); // 解析字符串为对象
    return jsonContent[name]; // 使用传入的参数名
  } catch (e) {
    console.error("无效的 JSON:", e);
    return ""; // 如果解析失败，返回空字符串
  }
}

const messageTypeSelected = ref(0);
const systemMessageListKey = ref(0);

const showDot = (time) => {
  const date = new Date(time);
  console.log("result: " + (date > lastReadTime.value));
  return date > lastReadTime.value;
}

const getDatePart = (time, index) => {
  if (!time) return "";
  return time.split(' ')[index];
}
</script>

<template>
  <v-container class="ma-0" fluid>
    <v-row>
      <v-col class="pr-8" style="max-width: 320px;">
        <!-- 参考学习页面设计的左侧导航栏 -->
        <v-card flat color="grey-lighten-5" rounded="lg" class="sticky-left px-2" style="position: sticky; top: 90px;">
          <!-- 头部标题区域 -->
          <v-card-text class="pa-4">
            <div class="d-flex align-center mb-3">
              <v-avatar color="grey-darken-2" size="32" class="mr-3">
                <v-icon icon="mdi-message-reply-text" color="white" size="16"></v-icon>
              </v-avatar>
              <div>
                <h3 class="text-h6 font-weight-bold text-grey-darken-4">消息中心</h3>
                <p class="text-body-2 text-grey-darken-2 mb-0">查看您的通知和消息</p>
              </div>
            </div>
          </v-card-text>

          <!-- 导航菜单列表 -->
          <v-list class="pa-2 bg-transparent" density="compact">
            <v-list-item 
              v-for="(item, i) in items" 
              :key="i" 
              :value="item.value" 
              :active="selected === item.value"
              @click="selected = item.value" 
              density="comfortable" 
              rounded="lg" 
              class="mb-1 nav-item"
              :class="{ 'nav-item-active': selected === item.value }">
              
              <template v-slot:prepend>
                <v-icon :icon="item.icon" size="18" class="nav-icon"></v-icon>
              </template>
              <v-list-item-title class="text-body-1 nav-title">
                {{ item.text }}
              </v-list-item-title>
            </v-list-item>
          </v-list>

          <!-- 底部统计信息 -->
          <v-card-text class="pa-4 border-t">
            <div class="text-body-2 text-grey-darken-3 mb-2">
              <div class="d-flex justify-space-between align-center mb-1">
                <span>未读消息</span>
                <span class="text-primary font-weight-bold">{{ unreadCount || 0 }}</span>
              </div>
              <div class="d-flex justify-space-between align-center">
                <span>总消息数</span>
                <span class="text-grey-darken-2 font-weight-medium">{{ totalMessages || 0 }}</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col class="py-9">

        <v-slide-y-reverse-transition hide-on-leave>
          <div v-if="selected == 'system'" class="text-body-1 d-flex justify-start">
            <v-row>
              <v-col>
                <v-alert
                  text="系统只保存30天内的消息，请及时查看"
                  type="warning"
                  variant="tonal"
                  density="compact"
                  class="mb-5"
                ></v-alert>
                <div class="mb-4">
                  <div class="d-flex align-center mb-3">
                    <v-icon icon="mdi-filter-variant" color="grey-darken-2" size="16" class="mr-2"></v-icon>
                    <span class="text-body-2 font-weight-medium text-grey-darken-3">消息筛选</span>
                  </div>
                  <div class="d-flex justify-center">
                    <v-chip-group v-model="messageTypeSelected" selected-class="bg-orange text-white" mandatory class="my-2">
                      <v-chip @click="systemMessageType = 99;" variant="tonal" rounded="lg">全部</v-chip>
                      <v-chip @click="systemMessageType = 2;" variant="tonal" rounded="lg">新增关注</v-chip>
                      <v-chip @click="systemMessageType = 3;" variant="tonal" rounded="lg">新点赞</v-chip>
                      <v-chip @click="systemMessageType = 4;" variant="tonal" rounded="lg">邀请回答</v-chip>
                      <v-chip @click="systemMessageType = 5;" variant="tonal" rounded="lg">新评论</v-chip>
                    </v-chip-group>
                  </div>
                </div>

                <v-infinite-scroll :items="messageList" :onLoad="loadData" :key="systemMessageListKey">
                  <div v-for="message, index in messageList" class="mb-0">
                    <div v-if="getDatePart(message.createdAt, 0) !== getDatePart(messageList[index - 1]?.createdAt, 0)"
                      class="pe-6 py-5 my-0 border-e text-end" style="width: 138px;">
                      <v-chip color="grey" variant="text" size="default" rounded="lg" class="pe-0">
                        <font class="font-weight-bold">
                        {{ getDatePart(message.createdAt, 0) }}
                        </font>
                      </v-chip>
                    </div>
                    <div class="d-flex justify-start align-center">
                      <div class="border-e text-end pe-6 py-3" style="min-width: 138px;">
                        <div class="text-caption text-grey-darken-2">{{ getDatePart(message.createdAt, 1) }}</div>
                        <div class="mt-2">
                          <v-chip v-if="message.type == 2" color="info" variant="tonal" size="small" rounded="lg">新增关注</v-chip>
                          <v-chip v-if="message.type == 3" color="success" variant="tonal" size="small" rounded="lg">点赞</v-chip>
                          <v-chip v-if="message.type == 4" color="warning" variant="tonal" size="small" rounded="lg">邀请回答</v-chip>
                          <v-chip v-if="message.type == 5" color="primary" variant="tonal" size="small" rounded="lg">评论</v-chip>
                          <v-chip v-if="message.type == 6" color="primary" variant="tonal" size="small" rounded="lg">评论</v-chip>
                          <v-chip v-if="message.type == 7" color="primary" variant="tonal" size="small" rounded="lg">评论</v-chip>
                          <v-chip v-if="message.type == 8" color="primary" variant="tonal" size="small" rounded="lg">评论</v-chip>
                        </div>
                      </div>
                      <div class="ps-6 w-100">
                        <v-badge class="w-100" color="error" dot offset-x="3" offset-y="2" :model-value="showDot(message.createdAt)">
                          <div class="d-flex align-center px-3 py-3 rounded-lg d-inline-flex bg-grey-lighten-5 w-100" 
                               :class="{ 'border-primary bg-blue-lighten-5 shadow-sm': showDot(message.createdAt) }">
                            <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"
                              rounded="lg" size="28" class="me-3"></v-avatar>
                            <div v-if="message.type == 2" class="message-content">
                              <a :href="`/user?id=${message.follower.id}`" target="_blank">{{ message.follower.name
                                }}</a>
                              关注了您
                            </div>
                            <div v-if="message.type == 3" class="message-content">
                              <div v-if="message.objectType == 0">
                                <div v-if="message.voteType == 1">
                                  <a :href="`/user?id=${message.receiver.id}`" target="_blank">{{ message.receiver.name
                                    }}</a>
                                  认为您在目录 <a :href="`/read?postId=${message.objectId}`" target="_blank">{{ message.node.name }}</a>
                                  的文章能被一次读懂
                                </div>
                                <div v-if="message.voteType == 2">
                                  <a :href="`/user?id=${message.receiver.id}`" target="_blank">{{ message.receiver.name }}</a>
                                  认为您在目录 <a :href="`/read?postId=${message.objectId}`"
                                    target="_blank">{{ message.node.name }}</a>
                                  的文章能被两次读懂
                                </div>
                                <div v-if="message.voteType == 3">
                                  <a :href="`/user?id=${message.receiver.id}`" target="_blank">{{ message.receiver.name }}</a>
                                  认为您在目录 <a :href="`/read?postId=${message.objectId}`"
                                    target="_blank">{{ message.node.name }}</a>
                                  的文章有帮助
                                </div>
                              </div>
                              <div v-if="message.objectType == 2">
                                  <a :href="`/user?id=${message.receiver.id}`" target="_blank">{{ message.receiver.name }}</a>
                                  点赞了您在目录 <a :href="`/read?commentId=${message.objectId}`" target="_blank">{{ message.node.name }}</a>
                                  下的评论
                              </div>
                            </div>
                            <div v-if="message.type == 4" class="message-content">
                              <a :href="`/user?id=${message.inviter.id}`" target="_blank">{{ message.inviter.name }}</a>
                              邀请您给目录 <a :href="`/read?nodeId=${message.node.id}`" target="_blank">{{ message.node.name }}
                              </a>
                              添加文章
                            </div>
                            <div v-if="message.type == 5" class="message-content">
                              <a :href="`/user?id=${message.commenter.id}`" target="_blank">{{ message.commenter.name
                                }}</a>
                              评论了您创建的目录 <a :href="`/read?commentId=${message.commentId}`"
                                target="_blank">{{ message.node.name }}</a>
                            </div>
                            <div v-if="message.type == 6" class="message-content">
                              <a :href="`/user?id=${message.commenter.id}`" target="_blank">{{ message.commenter.name
                                }}</a>
                              评论了您在目录 <a :href="`/read?commentId=${message.commentId}`"
                                target="_blank">{{ message.node.name }}</a> 下的文章
                            </div>
                            <div v-if="message.type == 7" class="message-content">
                              <a :href="`/user?id=${message.commenter.id}`" target="_blank">{{ message.commenter.name
                                }}</a>
                              回复了您在目录 <a :href="`/read?commentId=${message.commentId}`"
                                target="_blank">{{ message.node.name }}
                              </a>下的评论
                            </div>
                            <div v-if="message.type == 8" class="message-content">
                              <a :href="`/user?id=${message.commenter.id}`" target="_blank">{{ message.commenter.name
                                }}</a>
                              回复了您在目录 <a :href="`/read?commentId=${message.commentId}`"
                                target="_blank">{{ message.node.name }}
                              </a> 的文章下的评论
                            </div>
                          </div>
                        </v-badge>
                      </div>
                    </div>
                  </div>
                  <template v-slot:empty>
                    <div class="text-body-2 text-grey py-9">已经到底了</div>
                  </template>
                </v-infinite-scroll>

                <!--
                <v-row v-for="message in messages" key="" align="center">
                  <v-col cols="2" class="text-end border-e pe-6 py-3">
                    <div class="pb-2">
                      <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"
                        rounded="true" size="20" class="me-3"></v-avatar>
                      评论回复
                    </div>
                    <div class="text-caption text-grey-lighten-1">{{ message.date }}</div>
                  </v-col>
                  <v-col cols="9" class="ps-6">
                    <v-badge content="NEW" size="small" color="red" dot offset-x="-2" offset-y="-2">
                      <div class="d-flex align-center border-thin border-dashed pa-3 rounded-lg d-inline-flex">
                        您的课程申请已被批准，课程在 [基础教育|数学] 的目录下，课程地址在<a href="">这里</a>，目录和文章的提交现已开放 {{ message.text }}
                      </div>
                    </v-badge>
                  </v-col>
                </v-row>
-->
              </v-col>
            </v-row>
          </div>
        </v-slide-y-reverse-transition>

        <v-slide-y-reverse-transition hide-on-leave>
          <div v-if="selected == 'courseApply'" class="text-body-1 d-flex justify-start">
            <v-row class="" style="max-width: 1150px;">
              <v-col cols="" class="mt-0">
                <div class="px-0 mx-0 pt-0">
                  <div class="px-8">
                    <v-infinite-scroll :items="messageList" :onLoad="loadData" :no-more-text="'已经到底了'"
                      style="position: relative;top:-12px">
                      <div v-for="message in messageList" class="mb-4">
                        <div class="pb-5 border-b d-flex justify-start">
                          <div class="px-3 py-2 rounded-lg d-inline-block flex-1-1 text-start text-body-1">
                            <p class="text-subtitle-1 font-weight-bold pb-3 text-grey-darken-2">课程申请 <span
                                v-if="message.content.parentId != '0'">(子课程)</span></p>
                            <p class="pb-2">
                              <span class="text-grey">名称：</span>{{ message.content.title }}
                            </p>
                            <p class="pb-2">
                              <span class="text-grey">简介</span>：{{ message.content.summary }}
                            </p>
                            <p class="pb-2">
                              <span class="text-grey">理由</span>：{{ message.content.explanation }}
                            </p>
                            <p class="pb-2" v-if="message.content.parentId != '0'">
                              <span class="text-grey">父课程</span>：{{ message.content.parentName }}
                            </p>
                            <p>
                              <span class="text-grey">状态</span>：{{ message.content.reply }}
                            </p>
                          </div>
                          <span class="ms-5 mt-2 text-right text-caption text-grey-lighten-1">5小时前</span>
                        </div>
                      </div>
                      <template v-slot:empty>
                        <div class="text-body-2 text-grey py-5">已经到底了</div>
                      </template>
                    </v-infinite-scroll>
                  </div>
                </div>
              </v-col>
            </v-row>
          </div>
        </v-slide-y-reverse-transition>

        <v-slide-y-reverse-transition hide-on-leave>
          <div v-if="selected == 'private'" class="text-body-1 d-flex justify-start">
            <v-row class="" style="max-width: 1050px;">
              <v-col cols="auto" class="pe-7 mt-0 pt-0 text-end">
                <v-list>
                  <v-list-item v-for="user, index in users" class="py-0 ps-1 pe-4 my-3 bg-grey-lighten-5 rounded-lg"
                    active-color="teal" :active="index == 0 ? true : false" :ripple="false" density="comfortable"
                    @click="handleClick(user)">
                    <template v-slot:prepend>
                      <v-list-item-media>
                        <v-img :src="user.prependAvatar" width="35" height="35" class="rounded-lg me-3"></v-img>
                      </v-list-item-media>
                    </template>
                    <template v-slot:default>
                      <span class="font-weight-bold text-body-2">{{ user.title }}</span>
                    </template>
                  </v-list-item>
                </v-list>
              </v-col>
              <v-col cols="" class="mt-0">
                <div class="border-s px-0 mx-0 pt-5">
                  <div class="px-8">
                    <div v-for="i in 12">
                      <div class="pb-5">
                        <v-avatar class="me-5">
                          <v-img alt="John" src="https://cdn.vuetifyjs.com/images/john.jpg"></v-img>
                        </v-avatar>
                        <div class="border px-4 py-3 rounded-xl d-inline-block">
                          你今天怎么样
                        </div>
                        <span class="ms-5 text-center text-caption text-grey-lighten-1">5小时前</span>
                      </div>
                      <div class="pb-5 text-end ">
                        <span class="me-5 text-center text-caption text-grey-lighten-1">5小时前</span>
                        <div class="border px-4 py-3 rounded-xl d-inline-block">
                          今天过得很好
                        </div>
                        <v-avatar class="ms-5">
                          <v-img alt="John" src="https://cdn.vuetifyjs.com/images/lists/2.jpg"></v-img>
                        </v-avatar>
                      </div>
                    </div>
                  </div>
                  <v-text-field variant="underlined" append-inner-icon="mdi-email-fast-outline"
                    @click:append-inner="sendMessage" single-line label="说点什么"
                    class="rounded-0 position-sticky bottom-0 bg-white mb-3 mx-8 pt-5 mb-10 text-h5"></v-text-field>
                </div>
              </v-col>
            </v-row>
          </div>
        </v-slide-y-reverse-transition>
      </v-col>

      <v-col cols="3" class="ps-8 pt-9">
        <!-- 参考学习页面设计的右侧边栏 -->
        <div class="sticky-right" style="position: sticky; top: 90px;">
          <RightSidebar />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.w-65 {
  max-width: 65%;
}

.v-infinite-scroll__side {
  display: none !important;
}

.v-infinite-scroll__loading {
  border-top: none !important;
  box-shadow: none !important;
}

/* 参考学习页面的导航项样式 */
.nav-item {
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.nav-item:hover {
  background: rgba(178, 223, 219, 0.15) !important;
}

.nav-item-active {
  background: #e3f2fd !important;
  color: #1976d2 !important;
}

.nav-item-active .nav-icon {
  color: #1976d2 !important;
}

.nav-item-active .nav-title {
  color: #1976d2 !important;
  font-weight: 600 !important;
}

/* 课程项悬停效果 */
.course-item {
  transition: all 0.2s ease;
  cursor: pointer;
}

.course-item:hover {
  background: rgba(0, 0, 0, 0.04) !important;
  transform: translateX(4px);
}

/* 确保卡片无阴影 - 参考学习页面的flat设计 */
.v-card {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
}

/* 按钮样式优化 */
.v-btn {
  text-transform: none !important;
  font-weight: 500 !important;
}

/* 改善字体渲染和清晰度 - 参考学习页面 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1, h2, h3, h4, h5, h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 增加hover效果但保持flat风格 */
.v-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sticky-left,
  .sticky-right {
    position: relative !important;
    top: unset !important;
    margin-bottom: 20px;
  }
  
  .pr-8,
  .ps-8 {
    padding-left: 16px !important;
    padding-right: 16px !important;
  }
}

/* 消息相关样式增强 */
.message-content {
  color: #333 !important;
}

.message-content a {
  color: #225a9b !important;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.message-content a:hover {
  color: #1565c0 !important;
  text-decoration: underline;
}

/* 消息卡片悬浮效果 */
.d-inline-flex:hover {
  transform: translateY(-1px);
  transition: all 0.2s ease;
}

/* 芯片选中状态增强 */
.v-chip.v-chip--selected {
  box-shadow: 0 2px 4px rgba(25, 118, 210, 0.3);
}

/* 日期芯片样式 */
.v-chip[color="primary"][variant="tonal"] {
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* 消息类型标签悬浮效果 */
.v-chip[size="x-small"]:hover {
  transform: scale(1.05);
  transition: transform 0.2s ease;
}

/* 消息列表项间距优化 */
.v-infinite-scroll > div {
  margin-bottom: 8px;
}

/* 边框线条优化 */
.border-e {
  border-right: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 筛选器标题样式 */
.d-flex.align-center .v-icon {
  opacity: 0.7;
}
</style>
