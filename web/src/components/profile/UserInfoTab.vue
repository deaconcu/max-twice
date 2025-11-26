<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 - 宽度不够时隐藏 -->
    <v-col cols="12" md="2" class="d-none d-lg-block">
      <div class="sticky-sidebar">
        <div class="pa-3 pa-md-4">
          <div class="mb-4">
            <h4 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-2">
              个人信息
            </h4>
            <p class="text-caption text-md-body-2 text-grey mb-0">
              管理您的个人资料，包括头像、姓名和个人简介。
            </p>
          </div>
          <v-divider class="my-3 my-md-4" />
          <div class="text-caption text-md-body-2 text-grey">
            <div class="d-flex align-start mb-2 mb-md-3">
              <v-icon icon="mdi-pencil" size="16" color="grey" class="mr-2 mt-1" />
              <span>点击链接修改信息</span>
            </div>
            <div class="d-flex align-start mb-2 mb-md-3">
              <v-icon icon="mdi-shield-lock" size="16" color="grey" class="mr-2 mt-1" />
              <span>信息仅自己可见</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-check-circle" size="16" color="grey" class="mr-2 mt-1" />
              <span>点击确定保存修改</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" lg="10">
      <div class="pa-0 pa-sm-2">
        <!-- 信息卡片 -->
        <v-card rounded="xl" border elevation="0" class="info-card">
          <v-card-text class="pa-4 pa-sm-6 pa-md-8">
            <!-- 头像 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >头像</span
                >
              </div>
              <div class="flex-grow-1 d-flex justify-center justify-sm-start">
                <v-avatar
                  :image="localUserInfo.avatar || undefined"
                  rounded="xl"
                  :size="$vuetify.display.mobile ? 96 : 120"
                  color="grey-lighten-3"
                  class="avatar-border"
                >
                  <v-icon
                    v-if="!localUserInfo.avatar"
                    icon="mdi-account"
                    :size="$vuetify.display.mobile ? 48 : 60"
                    color="grey"
                  />
                </v-avatar>
              </div>
            </div>

            <v-divider class="my-4 my-md-6" />

            <!-- 姓名 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >姓名</span
                >
              </div>
              <div class="flex-grow-1">
                <div
                  v-if="!displayModifyName"
                  class="d-flex flex-column flex-sm-row align-start align-sm-center"
                >
                  <span class="text-body-1 text-md-h6 text-grey-darken-4 mb-2 mb-sm-0">{{
                    localUserInfo.name
                  }}</span>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-sm-3"
                    @click="displayModifyName = true"
                  >
                    <v-icon icon="mdi-pencil" size="16" class="mr-1" />
                    修改
                  </v-btn>
                </div>
                <div v-else class="d-flex flex-column flex-sm-row align-start">
                  <v-text-field
                    v-model="localUserInfo.name"
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    :rules="usernameRules"
                    :counter="usernameMaxLength"
                    class="mb-2 mb-sm-0"
                    style="max-width: 300px"
                  />
                  <div class="d-flex ga-2">
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      rounded="lg"
                      class="ml-sm-3"
                      @click="onModifyName"
                    >
                      <v-icon icon="mdi-check" size="16" class="mr-1" />
                      确定
                    </v-btn>
                    <v-btn
                      variant="text"
                      color="grey"
                      size="small"
                      rounded="lg"
                      @click="displayModifyName = false"
                    >
                      取消
                    </v-btn>
                  </div>
                </div>
              </div>
            </div>

            <v-divider class="my-4 my-md-6" />

            <!-- 简介 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >个人简介</span
                >
              </div>
              <div class="flex-grow-1">
                <div
                  v-if="!displayModifyBio"
                  class="d-flex flex-column flex-sm-row align-start align-sm-center"
                >
                  <span class="text-body-2 text-md-body-1 text-grey-darken-3 mb-2 mb-sm-0">{{
                    localUserInfo.bio || '暂未设置'
                  }}</span>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-sm-3"
                    @click="displayModifyBio = true"
                  >
                    <v-icon icon="mdi-pencil" size="16" class="mr-1" />
                    修改
                  </v-btn>
                </div>
                <div v-else>
                  <v-textarea
                    v-model="localUserInfo.bio"
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    rows="3"
                    placeholder="简单介绍一下自己..."
                    :rules="biographyRules"
                    :counter="biographyMaxLength"
                    style="max-width: 500px"
                  />
                  <div class="d-flex align-center mt-3 ga-2">
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      rounded="lg"
                      @click="onModifyBio"
                    >
                      <v-icon icon="mdi-check" size="16" class="mr-1" />
                      确定
                    </v-btn>
                    <v-btn
                      variant="text"
                      color="grey"
                      size="small"
                      rounded="lg"
                      @click="displayModifyBio = false"
                    >
                      取消
                    </v-btn>
                  </div>
                </div>
              </div>
            </div>

            <v-divider class="my-4 my-md-6" />

            <!-- 邮箱 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >邮箱</span
                >
              </div>
              <div class="flex-grow-1">
                <span class="text-body-2 text-md-body-1 text-grey">{{ localUserInfo.email }}</span>
              </div>
            </div>

            <v-divider class="my-4 my-md-6" />

            <!-- 加入日期 -->
            <div class="d-flex flex-column flex-sm-row align-start">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >加入日期</span
                >
              </div>
              <div class="flex-grow-1">
                <span class="text-body-2 text-md-body-1 text-grey">{{
                  localUserInfo.joinDate
                }}</span>
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
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { userApi } from '@/api'

// 验证规则
const usernameRules = useValidationRules('username')
const biographyRules = useValidationRules('biography')
const usernameMaxLength = useMaxLength('username')
const biographyMaxLength = useMaxLength('biography')

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
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

.label-section {
  min-width: 100px;
  padding-top: 4px;
}

@media (min-width: 600px) {
  .label-section {
    min-width: 120px;
  }
}

/* 头像边框 */
.avatar-border {
  border: 4px solid rgb(var(--v-theme-surface-variant));
  transition: all 0.3s ease;
}

.avatar-border:hover {
  border-color: rgb(var(--v-theme-outline));
}
</style>
