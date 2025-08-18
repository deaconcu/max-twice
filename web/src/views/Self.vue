<script setup>
import { ref, onMounted, inject, watch, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService } from '@/services/learnService'
import { userService } from '@/services/learnService'
import draggable from 'vuedraggable';
import { useUserStore } from "@/stores/user";
import UserPosting from '../components/UserPosting.vue';
import Comment from '../components/Comment.vue';
import Tiptap from '../components/Tiptap.vue'


//const isLoggedIn = ref(false);
const route = useRoute();
const router = useRouter();
const user = useUserStore();

const showSnackbar = inject('showSnackbar');

const items = ref([
  { text: '个人信息', icon: 'mdi-information-outline', value: "info" },
  { text: '我关注的课程', icon: 'mdi-book-multiple-outline', value: "subscription" },
  { text: '我关注的人', icon: 'mdi-account-heart', value: "follow" },
  { text: '我创建的目录', icon: 'mdi-format-list-group', value: "contents" },
  { text: '我创建的文章', icon: 'mdi-file-document-outline', value: "article" },
])

const messages = ref([
  { text: '个人信息', name: 'ADMIN', date: "2025-03-05 22:55:22" },
  { text: '我关注的课程', name: 'ADMIN', date: "2025-03-05 21:55:22" },
  { text: '我关注的人', name: 'ADMIN', date: "2025-03-05 20:55:22" },
  { text: '我创建的目录', name: 'ADMIN', date: "2025-03-04 22:55:22" },
  { text: '我创建的文章', name: 'ADMIN', date: "2025-03-03 22:55:22" },
  { text: '我的消息', name: 'ADMIN', date: "2025-03-02 22:55:22" },
])

const follows = ref([
  { id: 1, name: 'Jaiden', description: 'why so serious' },
  { id: 2, name: '一只小鲤鱼', description: '我想去银河系游泳' },
])

const relatedLinks = ['高等数学', '概率论', 'C++编程实现', '软件测试']

if (!route.query || !route.query.tab) router.replace({ query: { tab: 'info' } })

const selected = computed(() => route.query.tab || 'info');

const subscriptions = ref([]);
const subscriptionsCopy = ref([]);
const currPosting = ref(null);
const mainArea = ref('list');
const editorRef = ref(null);
const lastPage = ref('');

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
])

onMounted(() => {
  onTabChange(route.query.tab);
});

watch(() => route.query.tab, (newValue, oldValue) => {
  //selected.value = newValue;
  onTabChange(newValue);
})

const onTabChange = (value) => {
  mainArea.value = 'list';
  if (value == "info") {
    loadUser();
  } else if (value == "subscription") {
    loadSubscription();
  } else if (value == "contents") {
    //loadContents(100000000);
  } else if (value == "article") {
    //loadArticle(100000000);
  } else if (value == "posting") {
    scrollPosition.value = window.scrollY;
    window.scrollTo(0, 0);
    //currPosting.value = posting;
  }
}

const scrollPosition = ref(0);
const listName = ref('');

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
}

const switchToLastPage = () => {
  console.log("lastPage: " + lastPage.value);
  switchMainArea(lastPage.value, currPosting.value);
}

const info = ref({});

async function loadUser() {
  console.log("load user");
  try {
    let response = '';
    response = await userService.getSelf();

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      info.value = response.data;
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function PostUser() {
  console.log("post user");
  try {
    let response = '';
    response = await userService.postSelf(info.value.name, info.value.biography);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      showSnackbar("修改成功！")
      loadUser();
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function loadSubscription() {
  console.log("load subscription");
  try {
    let response = '';
    response = await userService.getSubscription(user.userId);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      subscriptions.value = response.data;
      subscriptionsCopy.value = JSON.parse(JSON.stringify(response.data));
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function saveSubscription() {
  console.log("save subscription");
  try {
    let response = '';
    const ids = subscriptions.value.map(item => item.id).join(',');
    response = await userService.putSubscription(ids);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      showSnackbar("修改成功！");
      user.setSubscription(response.data);
      loadSubscription();
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

const recoverSubscription = () => {
  subscriptions.value = JSON.parse(JSON.stringify(subscriptionsCopy.value));
}

const contentsList = ref([]);
const articleList = ref([]);

const lastContentsId = ref(0x7FFFFFFF);
const lastArticleId = ref(0x7FFFFFFF);

async function loadContents({ done }) {
  try {
    let response = '';
    response = await learnService.getUserContents(user.userId, lastContentsId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      contentsList.value.push(...response.data);

      if (response.data.length > 0) {
        lastContentsId.value = response.data[response.data.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
}

async function loadArticle({ done }) {
  try {
    let response = '';
    response = await learnService.getUserArticle(user.userId, lastArticleId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      articleList.value.push(...response.data);

      if (response.data.length > 0) {
        lastArticleId.value = response.data[response.data.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

const modifyPosting = async () => {
  try {
    console.log("begin post");

    const response = await learnService.putPosting(currPosting.value.id, editorRef.value.editor.getHTML());
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      currPosting.value.content = editorRef.value.editor.getHTML();
      switchToLastPage();
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const deletePosting = (id) => {
  contentsList.value = contentsList.value && contentsList.value.filter(item => item.id !== id);
  articleList.value = articleList.value && articleList.value.filter(item => item.id !== id);
}

const displayModifyName = ref(false);

const onModifyName = () => {
  displayModifyName.value = false;
  PostUser();
}

const displayModifyIntro = ref(false);

const onModifyIntro = () => {
  displayModifyIntro.value = false;
  PostUser();
}

const courseDescription = ref("");
const courseHoveringIndex = ref(-1);

const lastFolloweeId = ref("2100-01-01 00:00:01");
const followeeList = ref([]);
async function loadFollowee({ done }) {
  try {
    let response = '';
    response = await userService.getFolloweeList(user.userId, lastFolloweeId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      followeeList.value.push(...response.data);

      if (response.data.length > 0) {
        lastFolloweeId.value = response.data[response.data.length - 1].createTime;
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

</script>

<template>
  <v-container class="ma-0" fluid>
    <v-row no-gutters>
      <v-col cols="auto" class="pr-4 pt-6" style="width: 320px;">
        <!-- 更美观的左侧导航栏设计 -->
        <div class="sticky-left" style="position: sticky; top: 90px;">
          <!-- 用户信息卡片 -->
          <v-card class="user-profile-card mb-4" rounded="xl" elevation="0" color="grey-lighten-5">
            <v-card-text class="pa-5">
              <div class="text-center">
                <v-avatar size="64" class="mb-3 profile-avatar">
                  <v-img src="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"></v-img>
                </v-avatar>
                <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">{{ info?.name || '用户' }}</h3>
                <p class="text-body-2 text-grey-darken-2 mb-3">{{ info?.biography || '暂无简介' }}</p>
                
                <!-- 数据展示 -->
                <v-row class="ma-0" no-gutters>
                  <v-col cols="6" class="text-center">
                    <div class="stat-item">
                      <div class="text-h6 font-weight-bold text-primary">{{ user.subscription?.length || 0 }}</div>
                      <div class="text-caption text-grey-darken-1">关注课程</div>
                    </div>
                  </v-col>
                  <v-col cols="6" class="text-center">
                    <div class="stat-item">
                      <div class="text-h6 font-weight-bold text-success">{{ (contentsList?.length || 0) + (articleList?.length || 0) }}</div>
                      <div class="text-caption text-grey-darken-1">创建内容</div>
                    </div>
                  </v-col>
                </v-row>
              </div>
            </v-card-text>
          </v-card>

          <!-- 导航菜单卡片 -->
          <v-card class="navigation-card" rounded="xl" elevation="0" color="white">
            <v-card-text class="pa-3">
              <div class="nav-items">
                <div 
                  v-for="(item, i) in items" 
                  :key="i" 
                  class="nav-item-modern mb-2"
                  :class="{ 'nav-item-active-modern': selected === item.value }"
                  @click="router.push({ query: { tab: item.value } })">
                  
                  <div class="nav-item-content">
                    <div class="nav-icon-wrapper">
                      <v-icon :icon="item.icon" size="20" class="nav-icon-modern"></v-icon>
                    </div>
                    <span class="nav-title-modern">{{ item.text }}</span>
                    <v-icon 
                      v-if="selected === item.value" 
                      icon="mdi-chevron-right" 
                      size="16" 
                      color="primary" 
                      class="nav-arrow">
                    </v-icon>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </v-col>

      <v-col cols="auto" class="flex-grow-1 d-flex justify-center">
        <div style="width: 720px; max-width: 800px;" class="py-6">
        <div v-if="mainArea == 'list'">
          <v-slide-y-reverse-transition hide-on-leave>
            <!-- info -->
            <div v-if="selected == 'info'">
              <div class="mb-5 px-3 text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">点击图片修改头像，点击链接修改名称和介绍</span>
              </div>
              <v-row align="start" class="mt-12">
                <v-col cols="auto" class="text-end pe-6 border-e" style="padding-bottom: 130px;min-width: 135px;">
                  <div class="font-weight-bold">头像</div>
                </v-col>
                <v-col cols="9" class="ps-6">
                  <div class="">
                    <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg" rounded="lg"
                      size="120" class="mb-6" style="border: 3px solid #333; padding: 0px"></v-avatar>
                  </div>
                </v-col>
              </v-row>

              <v-row align="center">
                <v-col cols="auto"style="min-width: 135px;" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">名称</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyName" class="d-flex align-center">
                    {{ info.name }}
                    <v-btn @click="displayModifyName = true" prepend-icon="mdi-pencil" variant="plain" color="grey"
                      class="text-body-2 ps-8">修改</v-btn>
                  </div>
                  <div v-if="displayModifyName" class="d-flex align-baseline">
                    <v-text-field v-model="info.name" class="" hide-details density="compact" max-width="200"
                      variant="underlined"></v-text-field>
                    <v-btn @click="onModifyName" density="comfortable" prepend-icon="mdi-check" variant="plain"
                      color="grey" class="text-body-2 ps-8">确定</v-btn>
                  </div>
                </v-col>
              </v-row>

              <v-row align="center" class="">
                <v-col cols="auto" style="min-width: 135px;" class="text-end pe-6 py-4 border-e">
                  <div class="font-weight-bold">简单介绍自己</div>
                </v-col>
                <v-col cols="9" class="ps-6 py-0">
                  <div v-if="!displayModifyIntro" class="d-flex align-center">
                    {{ info.biography }}
                    <v-btn @click="displayModifyIntro = true" prepend-icon="mdi-pencil" variant="plain" color="grey"
                      class="text-body-2 ps-8">修改</v-btn>
                  </div>
                  <div v-if="displayModifyIntro" class="d-flex align-baseline">
                    <v-text-field v-model="info.biography" class="" hide-details density="compact" max-width="400"
                      variant="underlined"></v-text-field>
                    <v-btn @click="onModifyIntro" prepend-icon="mdi-check" variant="plain" color="grey"
                      class="text-body-2 ps-8">确定</v-btn>
                  </div>
                </v-col>
              </v-row>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- subscription -->
            <div v-if="selected == 'subscription'">
              <div class="mb-5 py-3 rounded text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">将鼠标放置在课程上可以查看简介，操作删除，拖动图标可以切换顺序</span>
              </div>
              <v-item-group class="mt-3 mb-5" column>
                <v-item>
                  <draggable v-model="subscriptions" item-key="id" class="pt-5">
                    <template #item="{ element, index }">
                      <span>
                        <v-hover>
                          <template v-slot:default="{ isHovering, props }">
                            <v-chip variant="flat" v-bind="props" class="mr-4 mb-4 px-4 py-4 text-body-1"
                              :class="courseHoveringIndex == index ? 'bg-red-lighten-1' : 'bg-grey-lighten-4'"
                              @mouseenter="courseDescription = element.description; courseHoveringIndex = index">
                              <span class="font-weight-medium">{{ element.name }}</span>
                              <v-slide-x-transition hide-on-leave>
                                <v-icon @click="subscriptions.splice(index, 1);" v-if="isHovering"
                                  icon="mdi-close-circle-outline" class="ms-2"></v-icon>
                              </v-slide-x-transition>
                            </v-chip>
                          </template>
                        </v-hover>
                      </span>
                    </template>

                  </draggable>
                </v-item>
              </v-item-group>
              <div v-if="JSON.stringify(subscriptions) != JSON.stringify(subscriptionsCopy)">
                <v-btn variant="flat" color="teal" density="comfortable" class="mr-4 mt-2" @click="saveSubscription">
                  保存
                </v-btn>
                <v-btn variant="text" color="" density="comfortable" class="mr-4 mt-2" @click="recoverSubscription">
                  恢复
                </v-btn>
              </div>
              <v-divider class="mb-9 mt-9"></v-divider>
              <div class="">
                {{ courseDescription }}
              </div>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- follow -->
            <div v-if="selected == 'follow'">
              <div class="mb-5 py-3 rounded text-grey d-flex align-center">
                <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
                <span class="text-body-2">查看和管理您关注的用户</span>
              </div>
              <v-infinite-scroll :items="follows" :onLoad="loadFollowee" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <v-list>
                  <v-list-item v-for="(item, i) in followeeList" :key="i" :value="item" class="mb-5 py-2">
                    <template v-slot:prepend>
                      <v-avatar icon="mdi-account" size="34" color="red">
                        <span class="text-body-1">CJ</span>
                      </v-avatar>
                    </template>

                    <v-list-item-title v-text="item.name"></v-list-item-title>
                    <v-list-item-subtitle v-text="item.biography"></v-list-item-subtitle>

                    <template v-slot:append>
                      <v-btn variant="text">取消关注</v-btn>
                    </template>
                  </v-list-item>
                </v-list>
                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- contents -->
            <div v-if="selected == 'contents'">
              <v-infinite-scroll :items="contentsList" :onLoad="loadContents" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <div v-for="(posting, index) in contentsList" :key="index">
                  <v-row class="ma-0 border-b px-0 pb-6" :class="{ 'pt-9': index != 0 }">
                    <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
                      <div class="d-flex align-center text-body-1">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
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

                    <UserPosting :posting="posting" :type="'list'" @switchMainArea="switchMainArea"
                      @deletePosting="deletePosting">
                    </UserPosting>

                  </v-row>
                </div>
                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>

            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>

            <!-- article-->
            <div v-if="selected == 'article'">
              <v-infinite-scroll :items="articleList" :onLoad="loadArticle" :key="selected" :no-more-text="'已经到底了'"
                style="position: relative;top:-12px">
                <div v-for="(posting, index) in articleList" :key="index">

                  <v-row class="ma-0 border-b px-0 pb-7" :class="{ 'pt-10': index != 0 }">

                    <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
                      <div class="d-flex align-center text-body-1">
                        <a class="text-grey pe-2" :href="'/read?courseId=' + posting.node.course.id" target="_blank">
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

                    <UserPosting :posting="posting" :type="'list'" @switchMainArea="switchMainArea"
                      @deletePosting="deletePosting">
                    </UserPosting>

                  </v-row>

                </div>

                <template v-slot:empty>
                  <div class="text-body-2 text-grey py-5">已经到底了</div>
                </template>
              </v-infinite-scroll>
            </div>
          </v-slide-y-reverse-transition>

          <v-slide-y-reverse-transition hide-on-leave>
            <div v-if="selected == 'message'" class="text-body-2">
              <div class="mb-10 text-grey-lighten-1 d-flex align-center">
                <v-icon icon="mdi-information-outline" start></v-icon>系统只保存30天内的消息，请及时查看
              </div>
              <v-row v-for="message in messages" key="" align="center">
                <v-col cols="2" class="text-end border-e pe-6 py-4">
                  <div class="pb-2">
                    <v-avatar image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg" rounded="true"
                      size="20" class="me-3"></v-avatar>
                    {{ message.name }}
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
            </div>
          </v-slide-y-reverse-transition>
        </div>

        <div v-if="mainArea == 'detail'">
          <UserPosting :posting="currPosting" :type="'detail'" @switchMainArea="switchMainArea"
            @deletePosting="deletePosting">
          </UserPosting>
          <v-row class="pa-0 ma-0 my-7">
            <Comment :posting="currPosting"></Comment>
          </v-row>
        </div>

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
      </v-col>

      <v-col cols="3" class="ps-12 pt-6">
        <!-- 参考消息页面设计的右侧边栏 -->
        <div class="sticky-right" style="position: sticky; top: 90px;">
          
          <!-- 用户统计卡片 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-chart-line" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">数据统计</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">您的账户数据概览</p>
                </div>
              </div>

              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">关注课程</span>
                <span class="text-h6 font-weight-bold text-primary">{{ user.subscription?.length || 0 }}</span>
              </div>
              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">创建内容</span>
                <span class="text-h6 font-weight-bold text-primary">{{ (contentsList?.length || 0) + (articleList?.length || 0) }}</span>
              </div>
              <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-body-2 text-grey-darken-3">关注用户</span>
                <span class="text-h6 font-weight-bold text-success">{{ followeeList?.length || 0 }}</span>
              </div>
              <div class="d-flex justify-space-between align-center">
                <span class="text-body-2 text-grey-darken-3">活跃天数</span>
                <span class="text-h6 font-weight-bold text-warning">30天</span>
              </div>
            </v-card-text>

            <v-card-actions class="px-4 pb-4">
              <v-btn variant="flat" color="grey-darken-2" rounded="lg" density="comfortable" class="w-100">
                <v-icon icon="mdi-account-settings" class="mr-2" size="16"></v-icon>
                账户设置
              </v-btn>
            </v-card-actions>
          </v-card>

          <!-- 快速操作卡片 -->
          <v-card flat color="grey-lighten-5" rounded="lg" class="mb-4">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-lightning-bolt" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">快速操作</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">常用功能入口</p>
                </div>
              </div>

              <div class="d-flex flex-column gap-2">
                <v-btn variant="tonal" color="primary" rounded="lg" density="default" class="justify-start mb-2">
                  <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
                  创建新内容
                </v-btn>
                <v-btn variant="tonal" color="info" rounded="lg" density="default" class="justify-start mb-2">
                  <v-icon icon="mdi-backup-restore" class="mr-2" size="16"></v-icon>
                  数据备份
                </v-btn>
                <v-btn variant="tonal" color="success" rounded="lg" density="default" class="justify-start">
                  <v-icon icon="mdi-export" class="mr-2" size="16"></v-icon>
                  导出数据
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <!-- 相关课程 -->
          <v-card flat color="grey-lighten-5" rounded="lg">
            <v-card-text class="pa-4">
              <div class="d-flex align-center mb-3">
                <v-avatar color="grey-darken-2" size="32" class="mr-3">
                  <v-icon icon="mdi-book-multiple" color="white" size="16"></v-icon>
                </v-avatar>
                <div>
                  <h3 class="text-h6 font-weight-bold text-grey-darken-4">推荐课程</h3>
                  <p class="text-body-2 text-grey-darken-2 mb-0">为您推荐学习内容</p>
                </div>
              </div>

              <v-list class="bg-transparent pa-0" density="compact">
                <v-list-item 
                  v-for="link in relatedLinks" 
                  :key="link"
                  class="px-2 py-1 ma-0 rounded course-item" 
                  density="compact"
                  @click="router.push('/')">
                  <v-list-item-title class="text-body-2 text-grey-darken-3">{{ link }}</v-list-item-title>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>
      </v-col>
    </v-row>
  </v-container>
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

/* 新版导航样式 */
.user-profile-card {
  background: #fafafa;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.profile-avatar {
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.stat-item {
  padding: 8px 0;
}

.navigation-card {
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.nav-item-modern {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s ease;
  padding: 0;
  border: 1px solid transparent;
}

.nav-item-modern:hover {
  background-color: rgba(25, 118, 210, 0.04);
  border-color: rgba(25, 118, 210, 0.1);
  transform: translateY(-1px);
}

.nav-item-active-modern {
  background-color: rgba(25, 118, 210, 0.08);
  border-color: rgba(25, 118, 210, 0.2);
}

.nav-item-content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  width: 100%;
}

.nav-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  transition: all 0.2s ease;
}

.nav-item-modern:hover .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.1);
}

.nav-item-active-modern .nav-icon-wrapper {
  background-color: rgba(25, 118, 210, 0.15);
}

.nav-icon-modern {
  color: rgba(0, 0, 0, 0.7);
  transition: color 0.2s ease;
}

.nav-item-active-modern .nav-icon-modern {
  color: #1976d2;
}

.nav-title-modern {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
  flex: 1;
  font-size: 14px;
}

.nav-item-active-modern .nav-title-modern {
  color: #1976d2;
  font-weight: 600;
}

.nav-arrow {
  margin-left: auto;
  opacity: 0.8;
}

/* 右侧卡片样式 */
.sticky-right {
  transition: all 0.3s ease;
}

/* 课程项悬停效果 */
.course-item {
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.course-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

/* 确保卡片无阴影 - 参考消息页面的flat设计 (不包括导航卡片) */
.v-card:not(.user-profile-card):not(.navigation-card) {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:not(.user-profile-card):not(.navigation-card):hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
}

/* 快速操作按钮间距 */
.gap-2 > * + * {
  margin-top: 8px;
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
</style>