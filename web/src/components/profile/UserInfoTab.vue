<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">个人信息</h4>
            <p class="text-body-2 text-grey mb-0">
              管理您的个人资料，包括头像、姓名和个人简介。
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-pencil" size="18" color="grey" class="mr-2 mt-1" />
              <span>点击链接修改信息</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-shield-lock" size="18" color="grey" class="mr-2 mt-1" />
              <span>信息仅自己可见</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-check-circle" size="18" color="grey" class="mr-2 mt-1" />
              <span>点击确定保存修改</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <!-- 信息卡片 -->
        <v-card rounded="xl" border elevation="0" class="info-card">
          <v-card-text class="pa-8">
            <!-- 头像 -->
            <div class="d-flex align-start mb-6">
              <div class="label-section">
                <span class="text-body-2 text-grey-darken-2 font-weight-medium">头像</span>
              </div>
              <div class="flex-grow-1">
                <v-avatar
                  :image="localUserInfo.avatar || undefined"
                  rounded="xl"
                  size="120"
                  color="grey-lighten-3"
                  class="avatar-border"
                >
                  <v-icon v-if="!localUserInfo.avatar" icon="mdi-account" size="60" color="grey" />
                </v-avatar>
              </div>
            </div>

            <v-divider class="my-6" />

            <!-- 姓名 -->
            <div class="d-flex align-start mb-6">
              <div class="label-section">
                <span class="text-body-2 text-grey-darken-2 font-weight-medium">姓名</span>
              </div>
              <div class="flex-grow-1">
                <div v-if="!displayModifyName" class="d-flex align-center">
                  <span class="text-h6 text-grey-darken-4">{{ localUserInfo.name }}</span>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-3"
                    @click="displayModifyName = true"
                  >
                    <v-icon icon="mdi-pencil" size="18" class="mr-1" />
                    修改
                  </v-btn>
                </div>
                <div v-else class="d-flex align-center">
                  <v-text-field
                    v-model="localUserInfo.name"
                    hide-details
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    style="max-width: 300px"
                  />
                  <v-btn
                    variant="flat"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-3"
                    @click="onModifyName"
                  >
                    <v-icon icon="mdi-check" size="18" class="mr-1" />
                    确定
                  </v-btn>
                  <v-btn
                    variant="text"
                    color="grey"
                    size="small"
                    rounded="lg"
                    class="ml-2"
                    @click="displayModifyName = false"
                  >
                    取消
                  </v-btn>
                </div>
              </div>
            </div>

            <v-divider class="my-6" />

            <!-- 简介 -->
            <div class="d-flex align-start mb-6">
              <div class="label-section">
                <span class="text-body-2 text-grey-darken-2 font-weight-medium">个人简介</span>
              </div>
              <div class="flex-grow-1">
                <div v-if="!displayModifyBio" class="d-flex align-center">
                  <span class="text-body-1 text-grey-darken-3">{{ localUserInfo.bio || '暂未设置' }}</span>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-3"
                    @click="displayModifyBio = true"
                  >
                    <v-icon icon="mdi-pencil" size="18" class="mr-1" />
                    修改
                  </v-btn>
                </div>
                <div v-else>
                  <v-textarea
                    v-model="localUserInfo.bio"
                    hide-details
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    rows="3"
                    placeholder="简单介绍一下自己..."
                    style="max-width: 500px"
                  />
                  <div class="d-flex align-center mt-3">
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      rounded="lg"
                      @click="onModifyBio"
                    >
                      <v-icon icon="mdi-check" size="18" class="mr-1" />
                      确定
                    </v-btn>
                    <v-btn
                      variant="text"
                      color="grey"
                      size="small"
                      rounded="lg"
                      class="ml-2"
                      @click="displayModifyBio = false"
                    >
                      取消
                    </v-btn>
                  </div>
                </div>
              </div>
            </div>

            <v-divider class="my-6" />

            <!-- 邮箱 -->
            <div class="d-flex align-start mb-6">
              <div class="label-section">
                <span class="text-body-2 text-grey-darken-2 font-weight-medium">邮箱</span>
              </div>
              <div class="flex-grow-1">
                <span class="text-body-1 text-grey">{{ localUserInfo.email }}</span>
              </div>
            </div>

            <v-divider class="my-6" />

            <!-- 加入日期 -->
            <div class="d-flex align-start">
              <div class="label-section">
                <span class="text-body-2 text-grey-darken-2 font-weight-medium">加入日期</span>
              </div>
              <div class="flex-grow-1">
                <span class="text-body-1 text-grey">{{ localUserInfo.joinDate }}</span>
              </div>
            </div>
          </v-card-text>
        </v-card>

        <!-- 提示信息 -->
        <v-alert
          v-if="showSuccessAlert"
          type="success"
          variant="tonal"
          rounded="lg"
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
  (data: { name: string; biography: string }) =>
    userApi.updateCurrentUser(data.name, data.biography),
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
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.info-card {
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

.label-section {
  min-width: 120px;
  padding-top: 4px;
}

/* 头像边框 */
.avatar-border {
  border: 4px solid #f5f5f5;
  transition: all 0.3s ease;
}

.avatar-border:hover {
  border-color: #e0e0e0;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }

  .label-section {
    min-width: 80px;
  }
}
</style>
