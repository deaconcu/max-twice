<script setup>
import { ref, inject, onMounted, computed } from 'vue';
import { userServiceV1 } from '@/services/api/v1/apiServiceV1';
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

const showSnackbar = inject('showSnackbar');
const userStore = useUserStore();

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.userId);

// 是否为当前用户查看自己的信息
const isSelf = computed(() => !props.userId || props.userId === userStore.userId);

// 实际的可编辑状态：必须是自己的信息且明确允许编辑
const canEdit = computed(() => props.editable && isSelf.value);

// 组件内部数据
const info = ref({});
const displayModifyName = ref(false);
const displayModifyIntro = ref(false);
const loading = ref(false);

// 加载用户信息
async function loadUser() {
  console.log("load user");
  try {
    loading.value = true;
    const response = isSelf.value 
      ? await userServiceV1.getCurrentUser()
      : await userServiceV1.getUser(targetUserId.value);

    if (response.code === 401) {
      console.log('not login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      info.value = response.data;
    }
  } catch (error) {
    console.error('Error get user:', error);
    showSnackbar("加载用户信息失败");
  } finally {
    loading.value = false;
  }
}

// 更新用户信息（仅在可编辑时可用）
async function updateUser() {
  if (!canEdit.value) return;
  
  console.log("update user");
  try {
    const response = await userServiceV1.updateCurrentUser(info.value.name, info.value.biography);

    if (response.code === 401) {
      console.log('not login');
      showSnackbar("请先登录");
    } else if (response.code === 200) {
      showSnackbar("修改成功！");
      await loadUser();
    } else {
      showSnackbar("修改失败");
    }
  } catch (error) {
    console.error('Error update user:', error);
    showSnackbar("修改失败");
  }
}

// 修改名称
const onModifyName = () => {
  displayModifyName.value = false;
  updateUser();
}

// 修改介绍
const onModifyIntro = () => {
  displayModifyIntro.value = false;
  updateUser();
}

// 组件挂载时加载数据
onMounted(() => {
  loadUser();
});
</script>

<template>
  <div>
    <div v-if="canEdit" class="mb-5 px-3 text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">点击图片修改头像，点击链接修改名称和介绍</span>
    </div>
    <div v-else class="mb-5 px-3 text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">查看用户的个人信息</span>
    </div>
    
    <v-row align="start" class="mt-12">
      <v-col cols="auto" class="text-end pe-6 border-e" style="padding-bottom: 130px;min-width: 135px;">
        <div class="font-weight-bold">头像</div>
      </v-col>
      <v-col cols="9" class="ps-6">
        <div class="">
          <v-avatar 
            image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg" 
            rounded="lg"
            size="120" 
            class="mb-6" 
            style="border: 3px solid #333; padding: 0px"
          ></v-avatar>
        </div>
      </v-col>
    </v-row>

    <v-row align="center">
      <v-col cols="auto" style="min-width: 135px;" class="text-end pe-6 py-4 border-e">
        <div class="font-weight-bold">名称</div>
      </v-col>
      <v-col cols="9" class="ps-6 py-0">
        <div v-if="!displayModifyName" class="d-flex align-center">
          {{ info.name }}
          <v-btn 
            v-if="canEdit"
            @click="displayModifyName = true" 
            prepend-icon="mdi-pencil" 
            variant="plain" 
            color="grey"
            class="text-body-2 ps-8"
          >
            修改
          </v-btn>
        </div>
        <div v-if="displayModifyName && canEdit" class="d-flex align-baseline">
          <v-text-field 
            v-model="info.name" 
            class="" 
            hide-details 
            density="compact" 
            max-width="200"
            variant="underlined"
          ></v-text-field>
          <v-btn 
            @click="onModifyName" 
            density="comfortable" 
            prepend-icon="mdi-check" 
            variant="plain"
            color="grey" 
            class="text-body-2 ps-8"
          >
            确定
          </v-btn>
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
          <v-btn 
            v-if="canEdit"
            @click="displayModifyIntro = true" 
            prepend-icon="mdi-pencil" 
            variant="plain" 
            color="grey"
            class="text-body-2 ps-8"
          >
            修改
          </v-btn>
        </div>
        <div v-if="displayModifyIntro && canEdit" class="d-flex align-baseline">
          <v-text-field 
            v-model="info.biography" 
            class="" 
            hide-details 
            density="compact" 
            max-width="400"
            variant="underlined"
          ></v-text-field>
          <v-btn 
            @click="onModifyIntro" 
            prepend-icon="mdi-check" 
            variant="plain" 
            color="grey"
            class="text-body-2 ps-8"
          >
            确定
          </v-btn>
        </div>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
/* 继承 Self.vue 的样式 */
</style>