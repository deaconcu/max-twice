<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import UserInfoTab from '@/components/user/UserInfoTab.vue';
import LearningTab from '@/components/user/LearningTab.vue';
import UserPostsTab from '@/components/user/UserPostsTab.vue';
import UserContentsTab from '@/components/user/UserContentsTab.vue';
import UserFollowingTab from '@/components/user/UserFollowingTab.vue';
import SubscriptionTab from '@/components/user/SubscriptionTab.vue';
import RightSidebar from '@/components/common/RightSidebar.vue';
import UserProfileCard from '@/components/user/UserProfileCard.vue';
import TabNavigation from '@/components/common/TabNavigation.vue';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

// 标签页配置（User页面不包含stats）
const items = ref([
  { text: t('user.profile.personalInfo'), icon: 'mdi-information-outline', value: "info" },
  { text: t('user.profile.learning'), icon: 'mdi-school-outline', value: "learning" },
  { text: t('user.profile.subscriptions'), icon: 'mdi-book-multiple-outline', value: "subscription" },
  { text: t('user.profile.following'), icon: 'mdi-account-heart', value: "follow" },
  { text: t('user.profile.myContents'), icon: 'mdi-format-list-group', value: "contents" },
  { text: t('user.profile.myArticles'), icon: 'mdi-file-document-outline', value: "article" },
]);

// 组件映射
const tabComponents = {
  info: UserInfoTab,
  learning: LearningTab,
  subscription: SubscriptionTab,
  follow: UserFollowingTab,
  contents: UserContentsTab,
  article: UserPostsTab
};

// 组件属性映射 - User页面所有组件都传入userId，都不可编辑
const getComponentProps = (tabValue) => {
  const baseProps = {
    userId: route.query.id
  };
  
  if (tabValue === 'article') {
    return { ...baseProps, postType: 'article' };
  }
  
  return baseProps;
};

// 当前选中的标签页
if (!route.query || !route.query.tab) router.replace({ query: { tab: 'info', id: route.query.id } });
const selected = computed(() => route.query.tab || 'info');

// 当前渲染的组件
const currentComponent = computed(() => tabComponents[selected.value]);
const currentProps = computed(() => getComponentProps(selected.value));

// 处理标签页切换
const handleTabChange = (newTab) => {
  router.push({ query: { tab: newTab, id: route.query.id } });
};

// 生命周期
onMounted(() => {
  // UserProfileCard 组件会自动加载用户信息
});
</script>

<template>
  <v-container class="ma-0" fluid>
    <v-row no-gutters>
      <v-col cols="auto" class="pr-4 pt-6" style="width: 320px;">
        <!-- 更美观的左侧导航栏设计 -->
        <div class="sticky-left" style="position: sticky; top: 30px;">
          <!-- 用户信息卡片 -->
          <UserProfileCard 
            :userId="route.query.id"
            :editable="false"
            class="mb-4"
          />

          <!-- 导航菜单 -->
          <TabNavigation
            v-model="selected"
            :items="items"
            :width="320"
            @tab-change="handleTabChange"
          />
        </div>
      </v-col>

      <v-col cols="auto" class="flex-grow-1 d-flex justify-center">
        <div style="width: 720px; max-width: 800px;" class="py-6">
          <v-slide-y-reverse-transition hide-on-leave>
            <component 
              :is="currentComponent" 
              v-bind="currentProps"
              :key="`${selected}-${route.query.id}`"
            />
          </v-slide-y-reverse-transition>
        </div>
      </v-col>

      <v-col cols="3" class="ps-12 pt-6">
        <RightSidebar />
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
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

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
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

/* 确保卡片无阴影 - 参考消息页面的flat设计 (不包括导航卡片和学习卡片) */
.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card) {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:not(.user-profile-card):not(.navigation-card):not(.flat-card):hover {
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