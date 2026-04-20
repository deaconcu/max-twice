<template>
  <div class="auth-page">
    <!-- 几何图形装饰 -->
    <div class="geo-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <svg class="shape shape-3" viewBox="0 0 100 90" width="70" height="63">
        <path
          d="M50 8 Q50 0 56 8 L92 76 Q100 90 86 90 L14 90 Q0 90 8 76 L44 8 Q50 0 50 8 Z"
          fill="#66BB6A"
        />
      </svg>
      <div class="shape shape-4"></div>
      <div class="shape shape-5"></div>
      <div class="shape shape-6"></div>
      <div class="shape shape-7"></div>
      <div class="shape shape-8"></div>
      <div class="shape shape-9"></div>
      <div class="shape shape-10"></div>
      <div class="shape shape-11"></div>
      <div class="shape shape-12"></div>
      <div class="shape shape-13"></div>
      <div class="shape shape-14"></div>
      <svg class="shape shape-16" viewBox="0 0 100 90" width="50" height="45">
        <path
          d="M50 8 Q50 0 56 8 L92 76 Q100 90 86 90 L14 90 Q0 90 8 76 L44 8 Q50 0 50 8 Z"
          fill="#AB47BC"
        />
      </svg>
      <div class="shape shape-17"></div>
      <div class="shape shape-19"></div>
      <div class="shape shape-20"></div>
      <div class="shape shape-21"></div>
      <div class="shape shape-23"></div>
      <div class="shape shape-24"></div>
      <svg class="shape shape-25" viewBox="0 0 100 90" width="40" height="36">
        <path
          d="M50 8 Q50 0 56 8 L92 76 Q100 90 86 90 L14 90 Q0 90 8 76 L44 8 Q50 0 50 8 Z"
          fill="#FFCA28"
        />
      </svg>
      <div class="shape shape-26"></div>
      <div class="shape shape-27"></div>
      <div class="shape shape-28"></div>
      <div class="shape shape-29"></div>
      <div class="shape shape-30"></div>
      <div class="shape shape-32"></div>
      <div class="shape shape-34"></div>
      <div class="shape shape-37"></div>
      <div class="shape shape-38"></div>
    </div>

    <SimpleHeader />

    <v-container fluid class="auth-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" lg="6" class="intro-section d-none d-lg-flex">
          <IntroSection />
        </v-col>

        <!-- Right side - Auth Form -->
        <v-col cols="12" sm="12" md="8" lg="4" class="d-flex justify-center">
          <div class="auth-card-wrapper">
            <v-card class="auth-card" rounded="xl" elevation="0">
              <!-- Logo Section -->
              <v-card-text class="text-center pt-10 pb-2 px-8">
                <div
                  class="logo-with-slogan"
                  @mouseenter="showSlogan = true"
                  @mouseleave="showSlogan = false"
                >
                  <IcosahedronLogo :size="72" />
                  <span class="logo-slogan" :class="{ visible: showSlogan }">{{
                    t('intro.logoSlogan')
                  }}</span>
                </div>

                <!-- 动态标题 -->
                <Transition name="fade" mode="out-in">
                  <div :key="currentMode">
                    <h2 class="text-h4 font-weight-bold mb-2">
                      {{ modeConfig.title }}
                    </h2>
                    <p class="text-body-2 text-medium-emphasis">{{ modeConfig.subtitle }}</p>
                  </div>
                </Transition>
              </v-card-text>

              <!-- 动态表单区域 -->
              <v-card-text class="px-8 pb-8">
                <Transition name="slide-fade" mode="out-in">
                  <!-- 登录表单 -->
                  <v-form
                    v-if="currentMode === 'login'"
                    key="login"
                    ref="loginFormRef"
                    @submit.prevent="handleLogin"
                  >
                    <v-text-field
                      v-model="loginData.email"
                      :label="t('user.login.email')"
                      :placeholder="t('user.login.emailPlaceholder')"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :disabled="isLoggingIn"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <v-text-field
                      v-model="loginData.password"
                      :label="t('user.login.password')"
                      :placeholder="t('user.login.passwordPlaceholder')"
                      :type="showLoginPassword ? 'text' : 'password'"
                      :disabled="isLoggingIn"
                      :append-inner-icon="showLoginPassword ? 'mdi-eye-off' : 'mdi-eye'"
                      variant="outlined"
                      density="comfortable"
                      class="mb-2"
                      @click:append-inner="showLoginPassword = !showLoginPassword"
                    />

                    <div class="text-right mb-6">
                      <a href="#" class="text-caption text-primary text-decoration-none">
                        {{ t('user.login.forgotPassword') }}
                      </a>
                    </div>

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <TurnstileWidget
                      v-if="showLoginCaptcha"
                      ref="loginTurnstileRef"
                      class="mb-4"
                      @verify="onLoginTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onLoginTurnstileExpire"
                    />

                    <v-btn
                      type="submit"
                      :loading="isLoggingIn"
                      :disabled="isLoggingIn || !loginData.email || !loginData.password"
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{ isLoggingIn ? t('user.login.loggingIn') : t('user.login.loginButton') }}
                    </v-btn>

                    <p class="text-caption text-center text-medium-emphasis mb-4">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a
                      >。
                    </p>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.login.registerLink') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToRegister"
                      >
                        {{ t('user.login.registerNow') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 注册表单 -->
                  <v-form
                    v-else-if="currentMode === 'register'"
                    key="register"
                    ref="registerFormRef"
                    @submit.prevent="handleRegister"
                  >
                    <v-text-field
                      v-model="registerData.email"
                      :label="t('user.register.email')"
                      type="email"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :disabled="isRegistering"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <v-text-field
                      v-model="registerData.password"
                      :label="t('user.register.password')"
                      :type="showRegisterPassword ? 'text' : 'password'"
                      :rules="passwordRules"
                      :counter="passwordMaxLength"
                      :disabled="isRegistering"
                      :append-inner-icon="showRegisterPassword ? 'mdi-eye-off' : 'mdi-eye'"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                      @click:append-inner="showRegisterPassword = !showRegisterPassword"
                    />

                    <v-text-field
                      v-model="registerData.confirmPassword"
                      :label="t('user.register.confirmPassword')"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      :rules="confirmPasswordRules"
                      :counter="passwordMaxLength"
                      :disabled="isRegistering"
                      :append-inner-icon="showConfirmPassword ? 'mdi-eye-off' : 'mdi-eye'"
                      variant="outlined"
                      density="comfortable"
                      class="mb-2"
                      @click:append-inner="showConfirmPassword = !showConfirmPassword"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <TurnstileWidget
                      ref="registerTurnstileRef"
                      class="mb-4"
                      @verify="onRegisterTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onRegisterTurnstileExpire"
                    />

                    <v-btn
                      type="submit"
                      :loading="isRegistering"
                      :disabled="
                        isRegistering ||
                        !registerData.email ||
                        !registerData.password ||
                        !registerData.confirmPassword
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isRegistering
                          ? t('user.register.registering')
                          : t('user.register.registerButton')
                      }}
                    </v-btn>

                    <p class="text-caption text-center text-medium-emphasis mb-4">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a
                      >。
                    </p>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.register.loginLink') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.register.loginNow') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 邮箱验证表单 -->
                  <v-form
                    v-else-if="currentMode === 'verify'"
                    key="verify"
                    ref="verifyFormRef"
                    @submit.prevent="handleVerify"
                  >
                    <v-alert type="info" variant="tonal" density="compact" class="mb-4">
                      <div class="d-flex align-center">
                        <v-icon size="20" class="mr-2">mdi-email-outline</v-icon>
                        <span class="text-body-2">
                          {{ t('user.verifyEmail.codeSentTo') }}
                          <strong>{{ verifyEmail }}</strong>
                        </span>
                      </div>
                    </v-alert>

                    <v-text-field
                      v-model="verifyData.code"
                      :label="t('user.verifyEmail.codeLabel')"
                      :placeholder="t('user.verifyEmail.codePlaceholder')"
                      :rules="[verificationCodeRule]"
                      :disabled="isVerifying"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                      counter="6"
                      maxlength="6"
                      @input="verifyData.code = verifyData.code.replace(/\D/g, '')"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :loading="isVerifying"
                      :disabled="isVerifying || verifyData.code.length !== 6"
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isVerifying
                          ? t('user.verifyEmail.verifying')
                          : t('user.verifyEmail.verifyButton')
                      }}
                    </v-btn>

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

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.verifyEmail.alreadyVerified') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.verifyEmail.loginNow') }}
                      </a>
                    </div>
                  </v-form>
                </Transition>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useAuth } from '@/composables/useAuth'
import {
  useEmailRules,
  useValidationRules,
  useMaxLength,
  useConfirmPasswordRule,
  useVerificationCodeRule,
} from '@/composables/useValidation'
import { HEADER_HEIGHT } from '@/constants/layout'
import { BUSINESS_ERROR } from '@/constants/errorCode'
import { authApi } from '@/api'
import { useUserStore } from '@/stores'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'
import TurnstileWidget from '@/components/common/TurnstileWidget.vue'
import IcosahedronLogo from '@/components/common/IcosahedronLogo.vue'

type AuthMode = 'login' | 'register' | 'verify'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const { login, register, isLoggingIn, isRegistering } = useAuth()
const userStore = useUserStore()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 当前模式
const currentMode = ref<AuthMode>('login')

// 验证规则
const emailRules = useEmailRules()
const passwordRules = useValidationRules('password')
const confirmPasswordRule = useConfirmPasswordRule()
const verificationCodeRule = useVerificationCodeRule()
const emailMaxLength = useMaxLength('email')
const passwordMaxLength = useMaxLength('password')

// 表单引用
const loginFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const registerFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const verifyFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

// Logo slogan
const showSlogan = ref(false)

// 登录数据
const loginData = ref({
  email: '',
  password: '',
})

// 注册数据
const registerData = ref({
  email: '',
  password: '',
  confirmPassword: '',
})

// 验证数据
const verifyData = ref({
  code: '',
})
const verifyEmail = ref('')

// UI 状态
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
let errorTimer: ReturnType<typeof setTimeout> | null = null

// Turnstile 状态
const showLoginCaptcha = ref(false)
const loginTurnstileToken = ref('')
const registerTurnstileToken = ref('')
const loginTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)
const registerTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

// 验证页面状态
const isVerifying = ref(false)
const isResending = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

// 模式配置
const modeConfig = computed(() => {
  switch (currentMode.value) {
    case 'login':
      return {
        title: t('user.login.title'),
        subtitle: t('user.login.subtitle'),
      }
    case 'register':
      return {
        title: t('user.register.title'),
        subtitle: t('user.register.subtitle'),
      }
    case 'verify':
      return {
        title: t('user.verifyEmail.title'),
        subtitle: t('user.verifyEmail.subtitle'),
      }
    default:
      return {
        title: '',
        subtitle: '',
      }
  }
})

// 确认密码验证规则
const confirmPasswordRules = computed(() => [
  ...passwordRules.value,
  confirmPasswordRule(registerData.value.password),
])

// 显示错误消息，5秒后自动消失
const showError = (message: string) => {
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
  errorMessage.value = message
  errorTimer = setTimeout(() => {
    errorMessage.value = ''
  }, 5000)
}

// 清除错误
const clearError = () => {
  errorMessage.value = ''
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
}

// Turnstile 回调
const onLoginTurnstileVerify = (token: string) => {
  loginTurnstileToken.value = token
}

const onRegisterTurnstileVerify = (token: string) => {
  registerTurnstileToken.value = token
}

const onTurnstileError = () => {
  showError(t('user.login.captchaLoadFailed'))
}

const onLoginTurnstileExpire = () => {
  loginTurnstileToken.value = ''
}

const onRegisterTurnstileExpire = () => {
  registerTurnstileToken.value = ''
}

// 切换模式
const switchToLogin = () => {
  clearError()
  currentMode.value = 'login'
  void router.replace('/login')
}

const switchToRegister = () => {
  clearError()
  currentMode.value = 'register'
  void router.replace('/register')
}

const switchToVerify = (email: string) => {
  clearError()
  verifyEmail.value = email
  verifyData.value.code = ''
  currentMode.value = 'verify'
  void router.replace({ path: '/verify-email', query: { email } })
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  const { valid } = await loginFormRef.value.validate()
  if (!valid) return

  if (showLoginCaptcha.value && !loginTurnstileToken.value) {
    showError(t('user.login.captchaRequired'))
    return
  }

  try {
    clearError()
    const token = showLoginCaptcha.value ? loginTurnstileToken.value : undefined
    await login(loginData.value.email, loginData.value.password, token)
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }

    if (err.code === BUSINESS_ERROR.CAPTCHA_REQUIRED) {
      showLoginCaptcha.value = true
      showError(t('user.login.captchaTooManyFailed'))
      return
    }

    if (err.code === BUSINESS_ERROR.USER_EMAIL_NOT_VALIDATED) {
      switchToVerify(loginData.value.email)
      return
    }

    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.login.loginFailed'))
    }

    if (showLoginCaptcha.value) {
      loginTurnstileToken.value = ''
      loginTurnstileRef.value?.reset()
    }
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  const { valid } = await registerFormRef.value.validate()
  if (!valid) return

  if (!registerTurnstileToken.value) {
    showError(t('user.register.captchaRequired'))
    return
  }

  try {
    clearError()
    const success = await register(
      registerData.value.email,
      registerData.value.password,
      registerTurnstileToken.value
    )

    if (success) {
      switchToVerify(registerData.value.email)
    }
  } catch (error: unknown) {
    if (error instanceof Error && error.message) {
      showError(error.message)
    } else {
      showError(t('user.register.registerFailed'))
    }
    registerTurnstileToken.value = ''
    registerTurnstileRef.value?.reset()
  }
}

// 处理邮箱验证
const handleVerify = async () => {
  if (!verifyFormRef.value) return
  const { valid } = await verifyFormRef.value.validate()
  if (!valid) return

  if (!verifyEmail.value) {
    showError(t('user.verifyEmail.emailInvalid'))
    return
  }

  try {
    isVerifying.value = true
    clearError()

    const response = await authApi.validateEmail(verifyEmail.value, verifyData.value.code)

    if (response.code === 200 && response.data) {
      userStore.setUser(response.data)
      showSnackbar?.(t('user.verifyEmail.verifySuccess'), 'success')
      await router.push('/')
    } else {
      showError(response.message ?? t('user.verifyEmail.verifyFailed'))
    }
  } catch (error: unknown) {
    const err = error as { message?: string }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.verifyFailedRetry'))
    }
  } finally {
    isVerifying.value = false
  }
}

// 重新发送验证码
const handleResend = async () => {
  if (!verifyEmail.value) {
    showError(t('user.verifyEmail.emailInvalid'))
    return
  }

  try {
    isResending.value = true
    clearError()

    await authApi.resendVerificationCode(verifyEmail.value)
    showSnackbar?.(t('user.verifyEmail.codeSent'), 'success')
    startCountdown()
  } catch (error: unknown) {
    const err = error as { message?: string }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.sendFailed'))
    }
  } finally {
    isResending.value = false
  }
}

// 倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

// 根据路由初始化模式
const initModeFromRoute = () => {
  const routeName = route.name as string
  if (routeName === 'register') {
    currentMode.value = 'register'
  } else if (routeName === 'verify-email') {
    currentMode.value = 'verify'
    verifyEmail.value = (route.query.email as string) || ''
    if (!verifyEmail.value) {
      showSnackbar?.(t('user.verifyEmail.emailInvalidRedirect'), 'error')
      switchToRegister()
    }
  } else {
    currentMode.value = 'login'
  }
}

// 监听路由变化
watch(
  () => route.name,
  () => {
    initModeFromRoute()
  }
)

onMounted(() => {
  initModeFromRoute()
})

onUnmounted(() => {
  stopCountdown()
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* 模糊渐变球 - 右上角 */
.auth-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(
    circle,
    rgba(255, 87, 34, 0.15) 0%,
    rgba(255, 87, 34, 0.05) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

/* 模糊渐变球 - 左下角 */
.auth-page::after {
  content: '';
  position: absolute;
  bottom: -25%;
  left: -15%;
  width: 1100px;
  height: 1100px;
  background: radial-gradient(
    circle,
    rgba(0, 188, 212, 0.12) 0%,
    rgba(0, 188, 212, 0.04) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

/* 几何图形容器 */
.geo-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

/* 通用图形样式 */
.shape {
  position: absolute;
  opacity: 0.15;
}

/* 圆形 - 橙色 */
.shape-1 {
  width: 80px;
  height: 80px;
  background-color: #ff7043;
  border-radius: 50%;
  top: 8%;
  right: 12%;
}

/* 方形 - 青色 */
.shape-2 {
  width: 60px;
  height: 60px;
  background-color: #26c6da;
  border-radius: 8px;
  top: 20%;
  left: 8%;
  transform: rotate(15deg);
}

/* 三角形 - 橙色 SVG 圆角 */
.shape-3 {
  top: 60%;
  left: 5%;
  opacity: 0.15;
}

/* 圆形 - 青色 小 */
.shape-4 {
  width: 40px;
  height: 40px;
  background-color: #26c6da;
  border-radius: 50%;
  top: 35%;
  right: 6%;
}

/* 方形 - 橙色 */
.shape-5 {
  width: 50px;
  height: 50px;
  background-color: #ff7043;
  border-radius: 6px;
  bottom: 15%;
  right: 10%;
  transform: rotate(-10deg);
}

/* 圆形 - 青色 模糊 */
.shape-6 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(38, 198, 218, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  bottom: 30%;
  left: 10%;
  opacity: 1;
}

/* 小圆形 - 橙色 */
.shape-7 {
  width: 30px;
  height: 30px;
  background-color: #ff7043;
  border-radius: 50%;
  top: 45%;
  left: 20%;
}

/* 方形 - 青色 */
.shape-8 {
  width: 45px;
  height: 45px;
  background-color: #26c6da;
  border-radius: 10px;
  bottom: 35%;
  right: 18%;
  transform: rotate(25deg);
}

/* 模糊圆形 - 橙色 */
.shape-9 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 112, 67, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  top: 10%;
  left: 30%;
  opacity: 1;
}

/* 模糊圆形 - 青色 */
.shape-10 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(38, 198, 218, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  bottom: 10%;
  right: 25%;
  opacity: 1;
}

/* 圆形 - 紫色 */
.shape-11 {
  width: 55px;
  height: 55px;
  background-color: #ab47bc;
  border-radius: 50%;
  top: 12%;
  left: 15%;
}

/* 方形 - 黄色 */
.shape-12 {
  width: 40px;
  height: 40px;
  background-color: #ffca28;
  border-radius: 8px;
  top: 70%;
  right: 8%;
  transform: rotate(20deg);
}

/* 圆形 - 粉色 */
.shape-13 {
  width: 35px;
  height: 35px;
  background-color: #ec407a;
  border-radius: 50%;
  bottom: 25%;
  left: 3%;
}

/* 方形 - 靛蓝 */
.shape-14 {
  width: 50px;
  height: 50px;
  background-color: #5c6bc0;
  border-radius: 10px;
  top: 55%;
  right: 3%;
  transform: rotate(-15deg);
}

/* 三角形 - 紫色 */
.shape-16 {
  bottom: 8%;
  left: 25%;
  opacity: 0.15;
}

/* 模糊圆形 - 黄色 */
.shape-17 {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(255, 202, 40, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  top: 5%;
  left: 45%;
  opacity: 1;
}

/* 圆形 - 绿色 小 */
.shape-19 {
  width: 25px;
  height: 25px;
  background-color: #66bb6a;
  border-radius: 50%;
  top: 30%;
  left: 5%;
}

/* 方形 - 粉色 */
.shape-20 {
  width: 35px;
  height: 35px;
  background-color: #ec407a;
  border-radius: 6px;
  top: 8%;
  right: 25%;
  transform: rotate(30deg);
}

/* 圆形 - 黄色 */
.shape-21 {
  width: 45px;
  height: 45px;
  background-color: #ffca28;
  border-radius: 50%;
  bottom: 40%;
  right: 5%;
}

/* 方形 - 绿色 */
.shape-23 {
  width: 30px;
  height: 30px;
  background-color: #66bb6a;
  border-radius: 5px;
  bottom: 12%;
  right: 30%;
  transform: rotate(-20deg);
}

/* 圆形 - 靛蓝 小 */
.shape-24 {
  width: 20px;
  height: 20px;
  background-color: #5c6bc0;
  border-radius: 50%;
  top: 42%;
  right: 12%;
}

/* 三角形 - 黄色 */
.shape-25 {
  top: 75%;
  right: 15%;
  opacity: 0.15;
}

/* 模糊圆形 - 绿色 */
.shape-26 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(102, 187, 106, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  top: 25%;
  right: 40%;
  opacity: 1;
}

/* 圆形 - 紫色 小 */
.shape-27 {
  width: 28px;
  height: 28px;
  background-color: #ab47bc;
  border-radius: 50%;
  bottom: 50%;
  left: 12%;
}

/* 方形 - 橙色 小 */
.shape-28 {
  width: 22px;
  height: 22px;
  background-color: #ff7043;
  border-radius: 4px;
  top: 18%;
  right: 35%;
  transform: rotate(45deg);
}

/* 模糊圆形 - 橙色 中上 */
.shape-29 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(255, 112, 67, 0.12) 0%, transparent 70%);
  border-radius: 50%;
  top: 15%;
  right: 20%;
  opacity: 1;
}

/* 模糊圆形 - 青色 左中 */
.shape-30 {
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(38, 198, 218, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: 50%;
  left: 25%;
  opacity: 1;
}

/* 模糊圆形 - 黄色 左上 */
.shape-32 {
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(255, 202, 40, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: 8%;
  left: 20%;
  opacity: 1;
}

/* 模糊圆形 - 绿色 下中 */
.shape-34 {
  width: 170px;
  height: 170px;
  background: radial-gradient(circle, rgba(102, 187, 106, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  bottom: 15%;
  left: 50%;
  opacity: 1;
}

/* 模糊圆形 - 大 绿色 */
.shape-37 {
  width: 550px;
  height: 550px;
  background: radial-gradient(circle, rgba(102, 187, 106, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  bottom: 5%;
  left: 20%;
  opacity: 1;
}

/* 模糊圆形 - 大 黄色 */
.shape-38 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, rgba(255, 202, 40, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  top: 40%;
  left: 40%;
  opacity: 1;
}

.auth-container {
  height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`'));
  padding-top: 20px;
  padding-bottom: 20px;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  overflow-y: auto;
}

/* Introduction Section */
.intro-section {
  height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`') - 40px);
  max-height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`') - 40px);
  overflow: hidden;
  padding: 0;
}

/* Auth Card */
.auth-card-wrapper {
  width: 100%;
  max-width: 480px;
}

.auth-card {
  background-color: rgba(var(--v-theme-surface), 0.5) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
}

/* 表单圆角 */
.v-text-field :deep(.v-field) {
  border-radius: 12px;
}

.v-btn {
  border-radius: 12px;
}

a:hover {
  text-decoration: underline !important;
}

/* 错误提示 */
.error-message {
  display: flex;
  align-items: center;
  color: #e53935;
  font-size: 14px;
}

/* Logo with slogan */
.logo-with-slogan {
  display: inline-block;
  position: relative;
  margin-bottom: 16px;
}

.logo-slogan {
  position: absolute;
  left: 100%;
  top: 50%;
  transform: translateY(-50%) translateX(-8px);
  margin-left: 16px;
  font-size: 12px;
  color: #999;
  opacity: 0;
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
  white-space: nowrap;
}

.logo-slogan.visible {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Responsive */
@media (max-width: 960px) {
  .intro-section {
    text-align: center;
    padding: 20px;
  }
}
</style>
