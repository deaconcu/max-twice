<script setup>
import { ref, inject, computed } from 'vue';
import { followServiceV1 } from '@/services/api/v1/apiServiceV1';
import { useUserStore } from "@/stores/user";

// Props
const props = defineProps({
  userId: {
    type: [String, Number],
    default: null
  },
  editable: {
    type: Boolean,
    default: false
  }
});

const userStore = useUserStore();
const showSnackbar = inject('showSnackbar');

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.userId);

// 是否为当前用户查看自己的信息
const isSelf = computed(() => !props.userId || props.userId === userStore.userId);

// 实际的可编辑状态：必须是自己的信息且明确允许编辑
const canEdit = computed(() => props.editable && isSelf.value);

const lastFolloweeId = ref("2100-01-01 00:00:01");
const followeeList = ref([]);

async function loadFollowee({ done }) {
  try {
    let response = '';
    response = await followServiceV1.getFollowees(targetUserId.value, lastFolloweeId.value);

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
}

const handleUnfollow = async (userId) => {
  if (!canEdit.value) return;
  
  try {
    // 这里应该调用取消关注的API
    // const response = await followServiceV1.unfollow(userId);
    // if (response.code === 200) {
    //   followeeList.value = followeeList.value.filter(item => item.id !== userId);
    //   showSnackbar('已取消关注');
    // }
    console.log('取消关注用户:', userId);
    showSnackbar('已取消关注');
  } catch (error) {
    console.error('取消关注失败:', error);
    showSnackbar('操作失败，请重试');
  }
};
</script>

<template>
  <div>
    <div class="mb-5 py-3 rounded text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">{{ canEdit ? '查看和管理您关注的用户' : '查看关注的用户' }}</span>
    </div>
    
    <v-infinite-scroll 
      :items="followeeList" 
      :onLoad="loadFollowee" 
      :no-more-text="'已经到底了'"
      style="position: relative;top:-12px"
    >
      <v-list>
        <v-list-item 
          v-for="(item, i) in followeeList" 
          :key="i" 
          :value="item" 
          class="mb-5 py-2"
        >
          <template v-slot:prepend>
            <v-avatar icon="mdi-account" size="34" color="red">
              <span class="text-body-1">{{ item.name ? item.name.charAt(0).toUpperCase() : 'U' }}</span>
            </v-avatar>
          </template>

          <v-list-item-title>{{ item.name }}</v-list-item-title>
          <v-list-item-subtitle>{{ item.biography }}</v-list-item-subtitle>

          <template v-slot:append>
            <v-btn 
              v-if="canEdit"
              variant="text" 
              @click="handleUnfollow(item.id)"
            >
              取消关注
            </v-btn>
          </template>
        </v-list-item>
      </v-list>
      
      <template v-slot:empty>
        <div class="text-body-2 text-grey py-5">已经到底了</div>
      </template>
    </v-infinite-scroll>
  </div>
</template>

<style scoped>
.v-infinite-scroll__side {
  display: none !important;
}
</style>