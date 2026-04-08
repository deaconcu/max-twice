<template>
  <div class="verify-email-page">
    <SimpleHeader />

    <v-container fluid class="verify-container">
      <v-row align="center" justify="center">
        <!-- 左侧 - 网站介绍 -->
        <v-col cols="12" md="5" class="intro-section d-none d-md-flex">
          <IntroSection :items="RIGHTS_DECLARATION" />
        </v-col>

        <!-- 右侧 - 验证表单 -->
        <v-col cols="12" md="5" sm="8" class="d-flex justify-center">
          <div class="verify-card-wrapper">
            <v-card class="verify-card" border rounded="lg">
              <!-- Logo Section -->
              <v-card-text class="text-center pa-8">
                <div class="logo-circle">
                  <v-icon size="48" color="success">mdi-email-check</v-icon>
                </div>
                <h2 class="text-h5 font-weight-bold mt-4 mb-2">
                  {{ t('user.verifyEmail.title') }}
                </h2>
                <p class="text-body-2 text-medium-emphasis">{{ t('user.verifyEmail.subtitle') }}</p>
              </v-card-text>

              <!-- 验证表单 -->
              <v-card-text class="px-8 pb-8">
                <v-form ref="verifyFormRef" @submit.prevent="handleVerify">
                  <!-- 邮箱显示 -->
                  <v-alert type="info" variant="tonal" density="compact" class="mb-4">
                    <div class="d-flex align-center">
                      <v-icon size="20" class="mr-2">mdi-email-outline</v-icon>
                      <span class="text-body-2">
                        {{ t('user.verifyEmail.codeSentTo') }} <strong>{{ email }}</strong>
                      </span>
                    </div>
                  </v-alert>

                  <!-- 验证码输入 -->
                  <v-text-field
                    v-model="formData.code"
                    :label="t('user.verifyEmail.codeLabel')"
                    :placeholder="t('user.verifyEmail.codePlaceholder')"
                    :rules="[verificationCodeRule]"
                    :disabled="isVerifying"
                    variant="outlined"
                    density="comfortable"
                    class="mb-4"
                    counter="6"
                    maxlength="6"
                    @input="formData.code = formData.code.replace(/\D/g, '')"
                  />

                  <!-- 错误提示 -->
                  <v-alert
                    v-if="errorMessage"
                    type="error"
                    variant="tonal"
                    density="compact"
                    closable
                    class="mb-4"
                    @click:close="errorMessage = ''"
                  >
                    {{ errorMessage }}
                  </v-alert>

                  <!-- 验证按钮 -->
                  <v-btn
                    type="submit"
                    :loading="isVerifying"
                    :disabled="isVerifying"
                    block
                    size="large"
                    color="success"
                    class="text-none font-weight-bold mb-3"
                  >
                    {{
                      isVerifying
                        ? t('user.verifyEmail.verifying')
                        : t('user.verifyEmail.verifyButton')
                    }}
                  </v-btn>

                  <!-- 重新发送验证码 -->
                  <div class="text-center mb-4">
                    <v-btn
                      variant="text"
                      color="primary"
                      size="small"
                      :disabled="isResending || countdown > 0"
                      :loading="isResending"
                      @click="handleResend"
                    >
                      {{
                        countdown > 0
                          ? t('user.verifyEmail.resendCountdown', { seconds: countdown })
                          : t('user.verifyEmail.resendButton')
                      }}
                    </v-btn>
                  </div>

                  <v-divider class="my-6" />

                  <!-- 返回登录 -->
                  <div class="text-center">
                    <span class="text-body-2 text-medium-emphasis">
                      {{ t('user.verifyEmail.alreadyVerified') }}
                    </span>
                    <a
                      href="#"
                      class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                      @click.prevent="goToLogin"
                    >
                      {{ t('user.verifyEmail.loginNow') }}
                    </a>
                  </div>
                </v-form>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { authApi } from '@/api'
import { useUserStore } from '@/stores'
import { useVerificationCodeRule } from '@/composables/useValidation'
import { RIGHTS_DECLARATION } from '@/constants/site'
import { HEADER_HEIGHT } from '@/constants/layout'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'

const router = useRouter()
const userStore = useUserStore()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
const { t } = useI18n()

// 验证规则
const verificationCodeRule = useVerificationCodeRule()

// 从路由获取邮箱地址
const email = ref((router.currentRoute.value.query.email as string) || '')

// 表单数据
const formData = ref({
  code: '',
})

// 表单引用
const verifyFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

// 状态
const isVerifying = ref(false)
const isResending = ref(false)
const errorMessage = ref('')
const countdown = ref(0)
let countdownTimer: number | null = null

// 验证邮箱
const handleVerify = async () => {
  if (!verifyFormRef.value) return
  const { valid } = await verifyFormRef.value.validate()
  if (!valid) return

  if (!email.value) {
    errorMessage.value = t('user.verifyEmail.emailInvalid')
    return
  }

  try {
    isVerifying.value = true
    errorMessage.value = ''

    const response = await authApi.validateEmail(email.value, formData.value.code)

    if (response.code === 200 && response.data) {
      // 保存用户信息
      userStore.setUser(response.data)

      // 显示成功提示
      showSnackbar?.(t('user.verifyEmail.verifySuccess'), 'success')

      // 跳转到首页
      await router.push('/')
    } else {
      errorMessage.value = response.message || t('user.verifyEmail.verifyFailed')
    }
  } catch (error: any) {
    // 显示错误信息
    if (error?.message) {
      errorMessage.value = error.message
    } else {
      errorMessage.value = t('user.verifyEmail.verifyFailedRetry')
    }
  } finally {
    isVerifying.value = false
  }
}

// 重新发送验证码
const handleResend = async () => {
  if (!email.value) {
    errorMessage.value = t('user.verifyEmail.emailInvalid')
    return
  }

  try {
    isResending.value = true
    errorMessage.value = ''

    await authApi.resendVerificationCode(email.value)

    // 显示成功提示
    showSnackbar?.(t('user.verifyEmail.codeSent'), 'success')

    // 启动60秒倒计时
    startCountdown()
  } catch (error: any) {
    // 显示错误信息
    if (error?.message) {
      errorMessage.value = error.message
    } else {
      errorMessage.value = t('user.verifyEmail.sendFailed')
    }
  } finally {
    isResending.value = false
  }
}

// 启动倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

// 停止倒计时
const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

// 跳转到登录页
const goToLogin = () => {
  void router.push('/login')
}

// 组件挂载时检查邮箱
onMounted(() => {
  if (!email.value) {
    showSnackbar?.(t('user.verifyEmail.emailInvalidRedirect'), 'error')
    void router.push('/register')
  }
})

// 组件卸载时清理定时器
onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
.verify-email-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* 装饰性渐变球 - 右上角 */
.verify-email-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(
    circle,
    rgba(76, 175, 80, 0.25) 0%,
    rgba(76, 175, 80, 0.1) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

/* 装饰性渐变球 - 左下角 */
.verify-email-page::after {
  content: '';
  position: absolute;
  bottom: -25%;
  left: -15%;
  width: 1100px;
  height: 1100px;
  background: radial-gradient(
    circle,
    rgba(33, 150, 243, 0.2) 0%,
    rgba(33, 150, 243, 0.08) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

.verify-container {
  height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`'));
  padding-top: 20px;
  padding-bottom: 100px;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  overflow-y: auto;
}

/* Introduction Section */
.intro-section {
  padding: 40px;
}

/* Verify Card */
.verify-card-wrapper {
  width: 100%;
  max-width: 480px;
}

.verify-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-border));
}

.logo-circle {
  width: 80px;
  height: 80px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
}

/* 表单圆角 */
.v-text-field :deep(.v-field) {
  border-radius: 20px;
}

.v-btn {
  border-radius: 20px;
}

a:hover {
  text-decoration: underline !important;
}

/* Responsive */
@media (max-width: 960px) {
  .intro-section {
    text-align: center;
    padding: 20px;
  }
}
</style>
