<template>
  <div class="pa-0 pa-sm-1">
    <!-- 信息卡片 -->
        <v-card rounded="lg" elevation="0" class="info-card">
          <v-card-text class="pa-0 pa-sm-1">
            <!-- 头像 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >头像</span
                >
              </div>
              <div class="flex-grow-1 d-flex flex-column align-start">
                <div class="position-relative">
                  <UserAvatar
                    :name="localUserInfo.name"
                    :avatar-url="localUserInfo.avatar"
                    :size="$vuetify.display.mobile ? 64 : 80"
                    rounded="lg"
                    avatar-class="avatar-border"
                  />
                  <v-btn
                    icon
                    size="small"
                    color="primary"
                    class="avatar-edit-btn"
                    :loading="uploadingAvatar"
                    @click="triggerFileInput"
                  >
                    <v-icon icon="mdi-camera" />
                  </v-btn>
                  <input
                    ref="fileInput"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    style="display: none"
                    @change="handleAvatarUpload"
                  />
                </div>
                <p class="text-caption text-grey mt-2 text-center text-sm-start">
                  支持 JPG、PNG、WebP 格式，最大 5MB
                </p>
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
                <div v-else>
                  <v-text-field
                    v-model="localUserInfo.name"
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    :rules="usernameRules"
                    :counter="usernameMaxLength"
                    class="mb-2"
                    style="max-width: 300px"
                  />
                  <div class="d-flex ga-2">
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      rounded="lg"
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

            <!-- 时区 -->
            <div class="d-flex flex-column flex-sm-row align-start mb-4 mb-md-6">
              <div class="label-section mb-2 mb-sm-0">
                <span class="text-caption text-md-body-2 text-grey-darken-2 font-weight-medium"
                  >时区</span
                >
              </div>
              <div class="flex-grow-1">
                <div
                  v-if="!displayModifyTimezone"
                  class="d-flex flex-column flex-sm-row align-start align-sm-center"
                >
                  <span class="text-body-2 text-md-body-1 text-grey-darken-3 mb-2 mb-sm-0">{{
                    getTimezoneLabel(localUserInfo.timezone)
                  }}</span>
                  <v-btn
                    variant="text"
                    color="primary"
                    size="small"
                    rounded="lg"
                    class="ml-sm-3"
                    @click="displayModifyTimezone = true"
                  >
                    <v-icon icon="mdi-pencil" size="16" class="mr-1" />
                    修改
                  </v-btn>
                </div>
                <div v-else>
                  <v-select
                    v-model="localUserInfo.timezone"
                    :items="timezoneOptions"
                    item-title="label"
                    item-value="value"
                    density="comfortable"
                    variant="outlined"
                    rounded="lg"
                    class="mb-2"
                    style="max-width: 350px"
                  />
                  <div class="d-flex ga-2">
                    <v-btn
                      variant="flat"
                      color="primary"
                      size="small"
                      rounded="lg"
                      @click="onModifyTimezone"
                    >
                      <v-icon icon="mdi-check" size="16" class="mr-1" />
                      确定
                    </v-btn>
                    <v-btn
                      variant="text"
                      color="grey"
                      size="small"
                      rounded="lg"
                      @click="displayModifyTimezone = false"
                    >
                      取消
                    </v-btn>
                  </div>
                </div>
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
</template>

<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { useMutation } from '@/composables/useMutation'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { userApi } from '@/api'
import UserAvatar from '@/components/common/UserAvatar.vue'

// 注入全局 showSnackbar
const showSnackbar = inject<(message: string, type: string) => void>('showSnackbar')

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
    timezone?: string
  }
}>()

// Emit
const emit = defineEmits<{
  updateAvatar: [avatarUrl: string]
}>()

// 本地状态
const localUserInfo = ref({
  ...props.userInfo,
  timezone: props.userInfo.timezone || DEFAULT_TIMEZONE,
})
const showSuccessAlert = ref(false)

// 显示修改状态
const displayModifyName = ref(false)
const displayModifyBio = ref(false)
const displayModifyTimezone = ref(false)

// 默认时区
const DEFAULT_TIMEZONE = 'America/Los_Angeles'

// 常用时区选项
const timezoneOptions = [
  { label: '(UTC-12:00) 国际日期变更线西', value: 'Etc/GMT+12' },
  { label: '(UTC-11:00) 中途岛', value: 'Pacific/Midway' },
  { label: '(UTC-10:00) 夏威夷', value: 'Pacific/Honolulu' },
  { label: '(UTC-09:00) 阿拉斯加', value: 'America/Anchorage' },
  { label: '(UTC-08:00) 太平洋时间 (美国/加拿大)', value: 'America/Los_Angeles' },
  { label: '(UTC-07:00) 山地时间 (美国/加拿大)', value: 'America/Denver' },
  { label: '(UTC-06:00) 中部时间 (美国/加拿大)', value: 'America/Chicago' },
  { label: '(UTC-05:00) 东部时间 (美国/加拿大)', value: 'America/New_York' },
  { label: '(UTC-04:00) 大西洋时间 (加拿大)', value: 'America/Halifax' },
  { label: '(UTC-03:00) 巴西利亚', value: 'America/Sao_Paulo' },
  { label: '(UTC+00:00) 伦敦, 都柏林', value: 'Europe/London' },
  { label: '(UTC+01:00) 巴黎, 柏林, 罗马', value: 'Europe/Paris' },
  { label: '(UTC+02:00) 开罗, 雅典', value: 'Europe/Athens' },
  { label: '(UTC+03:00) 莫斯科, 伊斯坦布尔', value: 'Europe/Moscow' },
  { label: '(UTC+04:00) 迪拜', value: 'Asia/Dubai' },
  { label: '(UTC+05:00) 卡拉奇', value: 'Asia/Karachi' },
  { label: '(UTC+05:30) 孟买, 新德里', value: 'Asia/Kolkata' },
  { label: '(UTC+06:00) 达卡', value: 'Asia/Dhaka' },
  { label: '(UTC+07:00) 曼谷, 河内', value: 'Asia/Bangkok' },
  { label: '(UTC+08:00) 北京, 上海, 香港, 台北', value: 'Asia/Shanghai' },
  { label: '(UTC+08:00) 新加坡', value: 'Asia/Singapore' },
  { label: '(UTC+09:00) 东京, 首尔', value: 'Asia/Tokyo' },
  { label: '(UTC+10:00) 悉尼, 墨尔本', value: 'Australia/Sydney' },
  { label: '(UTC+12:00) 奥克兰', value: 'Pacific/Auckland' },
]

// 获取时区显示标签
const getTimezoneLabel = (timezone?: string) => {
  const tz = timezone || DEFAULT_TIMEZONE
  const option = timezoneOptions.find((o) => o.value === tz)
  return option ? option.label : tz
}

// 头像上传相关
const fileInput = ref<HTMLInputElement>()

// 使用 useMutation 上传头像（调用新的一体化接口）
const { execute: uploadAvatar, loading: uploadingAvatar } = useMutation(
  (file: File) => userApi.updateAvatar(file),
  {
    showToast: false,
  }
)

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value?.click()
}

// 处理头像上传
const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    showSnackbar?.('只支持 JPG、PNG、WebP 格式的图片', 'error')
    return
  }

  // 验证文件大小 (5MB)
  if (file.size > 5 * 1024 * 1024) {
    showSnackbar?.('图片大小不能超过 5MB', 'error')
    return
  }

  try {
    // 调用接口：上传图片 + 更新数据库，返回头像地址
    const avatarUrl = await uploadAvatar(file)

    // 通知父组件更新 profileUser
    if (avatarUrl) {
      emit('updateAvatar', avatarUrl)
    }

    showSnackbar?.('头像更新成功', 'success')
  } catch (error: any) {
    console.error('头像上传失败:', error)
    showSnackbar?.(error.message || '头像上传失败', 'error')
  } finally {
    // 清空 input，允许重复选择同一文件
    if (target) target.value = ''
  }
}

// 使用 useMutation 更新用户信息
const { execute: updateUser, loading: updating } = useMutation(
  (data: { name: string; biography: string; avatar?: string; timezone?: string }) =>
    userApi.updateCurrentUser(data.name, data.biography, data.avatar, data.timezone),
  {
    successMessage: '个人信息已成功保存！',
    showToast: false, // 我们使用自定义的 alert
    onSuccess: () => {
      showSuccessAlert.value = true
      setTimeout(() => {
        showSuccessAlert.value = false
      }, 3000)
    },
  }
)

// 监听 props 变化
watch(
  () => props.userInfo,
  (newVal) => {
    localUserInfo.value = {
      ...newVal,
      timezone: newVal.timezone || DEFAULT_TIMEZONE,
    }
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

// 修改时区
const onModifyTimezone = async () => {
  displayModifyTimezone.value = false
  await updateUser({
    name: localUserInfo.value.name,
    biography: localUserInfo.value.bio,
    timezone: localUserInfo.value.timezone,
  })
}
</script>

<style scoped>
.info-card {
  background-color: rgb(var(--v-theme-surface));
  border: none !important;
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

/* 头像编辑按钮 */
.position-relative {
  position: relative;
}

.avatar-edit-btn {
  position: absolute;
  bottom: -4px;
  right: -4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
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
