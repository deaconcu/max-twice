<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">个人信息</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            管理您的个人资料，包括头像、姓名和个人简介。
          </p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-pencil" size="14" class="mr-1" />
              点击链接修改信息
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-shield-lock" size="14" class="mr-1" />
              信息仅自己可见
            </div>
            <div>
              <v-icon icon="mdi-check-circle" size="14" class="mr-1" />
              点击确定保存修改
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <!-- 头像 -->
        <v-row align="start" class="mb-4">
          <v-col cols="auto" class="text-end pe-6 label-col">
            <div class="text-body-2 text-grey">头像</div>
          </v-col>
          <v-col cols="9" class="ps-6">
            <v-avatar
              :image="localUserInfo.avatar || undefined"
              rounded="md"
              size="120"
              color="grey-lighten-3"
              class="avatar-border"
            >
              <v-icon v-if="!localUserInfo.avatar" icon="mdi-account" size="60" color="grey" />
            </v-avatar>
          </v-col>
        </v-row>

        <!-- 姓名 -->
        <v-row align="center" class="mb-3">
          <v-col cols="auto" class="text-end pe-6 py-3 label-col">
            <div class="text-body-2 text-grey">姓名</div>
          </v-col>
          <v-col cols="9" class="ps-6 py-0">
            <div v-if="!displayModifyName" class="d-flex align-center">
              <span class="text-body-1">{{ localUserInfo.name }}</span>
              <v-btn
                prepend-icon="mdi-pencil"
                variant="plain"
                color="grey"
                density="compact"
                class="text-body-2 ps-4"
                @click="displayModifyName = true"
              >
                修改
              </v-btn>
            </div>
            <div v-else class="d-flex align-center">
              <v-text-field
                v-model="localUserInfo.name"
                hide-details
                density="compact"
                variant="underlined"
                style="max-width: 200px"
              />
              <v-btn
                prepend-icon="mdi-check"
                variant="plain"
                color="grey"
                density="compact"
                class="text-body-2 ps-4"
                @click="onModifyName"
              >
                确定
              </v-btn>
            </div>
          </v-col>
        </v-row>

        <!-- 简介 -->
        <v-row align="center" class="mb-3">
          <v-col cols="auto" class="text-end pe-6 py-3 label-col">
            <div class="text-body-2 text-grey">简单介绍自己</div>
          </v-col>
          <v-col cols="9" class="ps-6 py-0">
            <div v-if="!displayModifyBio" class="d-flex align-center">
              <span class="text-body-1">{{ localUserInfo.bio }}</span>
              <v-btn
                prepend-icon="mdi-pencil"
                variant="plain"
                color="grey"
                density="compact"
                class="text-body-2 ps-4"
                @click="displayModifyBio = true"
              >
                修改
              </v-btn>
            </div>
            <div v-else class="d-flex align-center">
              <v-text-field
                v-model="localUserInfo.bio"
                hide-details
                density="compact"
                variant="underlined"
                style="max-width: 400px"
              />
              <v-btn
                prepend-icon="mdi-check"
                variant="plain"
                color="grey"
                density="compact"
                class="text-body-2 ps-4"
                @click="onModifyBio"
              >
                确定
              </v-btn>
            </div>
          </v-col>
        </v-row>

        <!-- 邮箱 -->
        <v-row align="center" class="mb-3">
          <v-col cols="auto" class="text-end pe-6 py-3 label-col">
            <div class="text-body-2 text-grey">邮箱</div>
          </v-col>
          <v-col cols="9" class="ps-6 py-0">
            <span class="text-body-1 text-grey">{{ localUserInfo.email }}</span>
          </v-col>
        </v-row>

        <!-- 加入日期 -->
        <v-row align="center" class="mb-3">
          <v-col cols="auto" class="text-end pe-6 py-3 label-col">
            <div class="text-body-2 text-grey">加入日期</div>
          </v-col>
          <v-col cols="9" class="ps-6 py-0">
            <span class="text-body-1 text-grey">{{ localUserInfo.joinDate }}</span>
          </v-col>
        </v-row>

        <!-- 提示信息 -->
        <v-alert
          v-if="showSuccessAlert"
          type="success"
          variant="tonal"
          class="mt-6"
          closable
          @click:close="showSuccessAlert = false"
        >
          个人信息已成功保存！
        </v-alert>
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMutation } from '@/composables/useMutation'
import { userApi } from '@/api'

// Props
const props = defineProps<{
  userInfo: {
    name: string
    email: string
    bio: string
    joinDate: string
    avatar?: string
  }
}>()

// Emit
const emit = defineEmits<{
  update: [userInfo: typeof props.userInfo]
}>()

// 本地状态
const localUserInfo = ref({ ...props.userInfo })
const showSuccessAlert = ref(false)

// 显示修改状态
const displayModifyName = ref(false)
const displayModifyBio = ref(false)

// 使用 useMutation 更新用户信息
const { execute: updateUser, loading: updating } = useMutation(
  (data: { name: string; biography: string }) => userApi.updateCurrentUser(data.name, data.biography),
  {
    successMessage: '个人信息已成功保存！',
    showToast: false, // 我们使用自定义的 alert
    onSuccess: () => {
      showSuccessAlert.value = true
      setTimeout(() => {
        showSuccessAlert.value = false
      }, 3000)
      emit('update', localUserInfo.value)
    },
  }
)

// 监听 props 变化
watch(
  () => props.userInfo,
  (newVal) => {
    localUserInfo.value = { ...newVal }
  },
  { deep: true }
)

// 修改姓名
const onModifyName = async () => {
  displayModifyName.value = false
  await updateUser({
    name: localUserInfo.value.name,
    biography: localUserInfo.value.bio,
  })
}

// 修改简介
const onModifyBio = async () => {
  displayModifyBio.value = false
  await updateUser({
    name: localUserInfo.value.name,
    biography: localUserInfo.value.bio,
  })
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

/* 标签列宽度 */
.label-col {
  min-width: 135px;
}

/* 头像边框 */
.avatar-border {
  border: 3px solid #f5f5f5;
  padding: 0px;
}

/* 输入框边框颜色调整 */
:deep(.v-field--variant-outlined .v-field__outline) {
  color: rgba(0, 0, 0, 0.3);
}

/* 聚焦时的边框颜色保持原样 */
:deep(.v-field--variant-outlined.v-field--focused .v-field__outline) {
  color: rgb(var(--v-theme-primary));
}

/* 悬停时的边框颜色 */
:deep(.v-field--variant-outlined:hover .v-field__outline) {
  color: rgba(0, 0, 0, 0.42);
}

/* 禁用状态的边框颜色 */
:deep(.v-field--variant-outlined.v-field--disabled .v-field__outline) {
  color: rgba(0, 0, 0, 0.15);
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
