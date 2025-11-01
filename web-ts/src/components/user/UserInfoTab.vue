<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import type { Ref } from 'vue'
import { userServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import type { User } from '@/types/user'

// Props
const props = defineProps<{
  userId?: number | null  // 保留用于兼容性
  username?: string | null  // 新增 username
  editable?: boolean
}>()

const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void
const userStore = useUserStore()

// 当前操作的用户名
const targetUsername = computed(() => props.username || userStore.currentUser?.name)

// 是否为当前用户查看自己的信息
const isSelf = computed(() => !props.username || props.username === userStore.currentUser?.name)

// 实际的可编辑状态：必须是自己的信息且明确允许编辑
const canEdit = computed(() => props.editable && isSelf.value)

// 组件内部数据
const displayModifyName: Ref<boolean> = ref(false)
const displayModifyIntro: Ref<boolean> = ref(false)

// 使用 useFetch 加载用户信息
const {
  data: info,
  loading,
  execute: loadUser
} = useFetch<User>({
  fetchFn: () => isSelf.value
    ? userServiceV1.getCurrentUser()
    : userServiceV1.getUser(targetUsername.value as string),
  immediate: true,
  onSuccess: (data) => {
    console.log('load user success')
    console.log(`get data:${JSON.stringify(data)}`)
  },
  onError: (error) => {
    console.error('Error get user:', error)
    showSnackbar('加载用户信息失败')
  }
})

// 使用 useMutation 更新用户信息
const { execute: updateUserInfo, loading: updating } = useMutation(
  (data: { name: string; biography: string }) =>
    userServiceV1.updateCurrentUser(data.name, data.biography),
  {
    successMessage: '修改成功！',
    onSuccess: loadUser
  }
)

// 更新用户信息（仅在可编辑时可用）
const updateUser = async (): Promise<void> => {
  if (!canEdit.value) return

  await updateUserInfo({
    name: info.value.name,
    biography: info.value.biography
  })
}

// 修改名称
const onModifyName = (): void => {
  displayModifyName.value = false
  updateUser()
}

// 修改介绍
const onModifyIntro = (): void => {
  displayModifyIntro.value = false
  updateUser()
}
</script>

<template>
  <div>
    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
      <p class="text-body-2 text-grey-darken-1 mt-4">加载用户信息中...</p>
    </div>

    <!-- 用户信息内容 -->
    <div v-else-if="info">
      <div v-if="canEdit" class="mb-5 px-3 text-grey d-flex align-center">
        <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
        <span class="text-body-2">点击图片修改头像，点击链接修改名称和介绍</span>
      </div>
      <div v-else class="mb-5 px-3 text-grey d-flex align-center">
        <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
        <span class="text-body-2">查看用户的个人信息</span>
      </div>

      <v-row align="start" class="mt-12">
        <v-col cols="auto" class="text-end pe-6 border-e avatar-col">
          <div class="font-weight-bold">头像</div>
        </v-col>
        <v-col cols="9" class="ps-6">
          <div class="">
            <v-avatar
              image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"
              rounded="lg"
              size="120"
              class="mb-6 avatar-border"
            />
          </div>
        </v-col>
      </v-row>

      <v-row align="center">
        <v-col cols="auto" class="text-end pe-6 py-4 border-e label-col">
          <div class="font-weight-bold">姓名</div>
        </v-col>
        <v-col cols="9" class="ps-6 py-0">
          <div v-if="!displayModifyName" class="d-flex align-center">
            {{ info.name }}
            <v-btn
              v-if="canEdit"
              prepend-icon="mdi-pencil"
              variant="plain"
              color="grey"
              class="text-body-2 ps-8"
              @click="displayModifyName = true"
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
              density="comfortable"
              prepend-icon="mdi-check"
              variant="plain"
              color="grey"
              class="text-body-2 ps-8"
              @click="onModifyName"
            >
              确定
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <v-row align="center" class="">
        <v-col cols="auto" class="text-end pe-6 py-4 border-e label-col">
          <div class="font-weight-bold">简单介绍自己</div>
        </v-col>
        <v-col cols="9" class="ps-6 py-0">
          <div v-if="!displayModifyIntro" class="d-flex align-center">
            {{ info.biography }}
            <v-btn
              v-if="canEdit"
              prepend-icon="mdi-pencil"
              variant="plain"
              color="grey"
              class="text-body-2 ps-8"
              @click="displayModifyIntro = true"
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
              prepend-icon="mdi-check"
              variant="plain"
              color="grey"
              class="text-body-2 ps-8"
              @click="onModifyIntro"
            >
              确定
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </div>

    <!-- 加载失败状态 -->
    <div v-else class="text-center py-8">
      <v-icon icon="mdi-alert-circle" color="error" size="32" class="mb-2"></v-icon>
      <p class="text-body-2 text-error">加载用户信息失败</p>
    </div>
  </div>
</template>

<style scoped>
/* 继承 Self.vue 的样式 */
.avatar-col {
  padding-bottom: 130px;
  min-width: 135px;
}

.label-col {
  min-width: 135px;
}
.avatar-border {
  border: 3px solid #333;
  padding: 0px;
}
</style>