<template>
  <div class="login-page">
    <SimpleHeader />

    <v-container fluid class="login-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" md="5" class="intro-section d-none d-md-flex">
          <IntroSection :items="RIGHTS_DECLARATION" />
        </v-col>

        <!-- Right side - Login Form -->
        <v-col cols="12" md="5" sm="8" class="d-flex justify-center">
          <div class="login-card-wrapper">
            <v-card class="login-card" border rounded="lg">
              <!-- Logo Section -->
              <v-card-text class="text-center pa-8">
                <div class="logo-circle">
                  <v-icon size="48" color="primary">mdi-reddit</v-icon>
                </div>
                <h2 class="text-h5 font-weight-bold mt-4 mb-2">
                  {{ t('user.login.title') }}
                </h2>
                <p class="text-body-2 text-medium-emphasis">{{ t('user.login.welcome') }}</p>
              </v-card-text>

              <!-- Login Form -->
              <v-card-text class="px-8 pb-8">
                <v-form ref="loginFormRef" @submit.prevent="handleLogin">
                  <!-- 邮箱输入 -->
                  <v-text-field
                    v-model="formData.email"
                    :label="t('user.login.email')"
                    :placeholder="t('user.login.emailPlaceholder')"
                    :rules="emailRules"
                    :counter="emailMaxLength"
                    :disabled="isLoggingIn"
                    variant="outlined"
                    density="comfortable"
                    class="mb-4"
                  />

                  <!-- 密码输入 -->
                  <v-text-field
                    v-model="formData.password"
                    :label="t('user.login.password')"
                    :placeholder="t('user.login.passwordPlaceholder')"
                    :type="showPassword ? 'text' : 'password'"
                    :rules="passwordRules"
                    :counter="passwordMaxLength"
                    :disabled="isLoggingIn"
                    :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                    variant="outlined"
                    density="comfortable"
                    class="mb-2"
                    @click:append-inner="showPassword = !showPassword"
                  />

                  <!-- 忘记密码 -->
                  <div class="text-right mb-6">
                    <a href="#" class="text-caption text-primary text-decoration-none">
                      {{ t('user.login.forgotPassword') }}
                    </a>
                  </div>

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

                  <!-- Turnstile 人机验证（登录失败多次后显示） -->
                  <div v-if="showCaptcha" class="turnstile-wrapper mb-4">
                    <TurnstileWidget
                      ref="turnstileRef"
                      @verify="onTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onTurnstileExpire"
                    />
                  </div>

                  <!-- 登录按钮 -->
                  <v-btn
                    type="submit"
                    :loading="isLoggingIn"
                    :disabled="isLoggingIn"
                    block
                    size="large"
                    color="primary"
                    class="text-none font-weight-bold mb-3"
                  >
                    {{ isLoggingIn ? t('user.login.loggingIn') : t('user.login.loginButton') }}
                  </v-btn>

                  <!-- 用户协议 -->
                  <p class="text-caption text-center text-medium-emphasis mb-4">
                    继续操作即表示你同意我们的
                    <a href="#" class="text-primary text-decoration-none">用户协议</a>
                    并确认已了解
                    <a href="#" class="text-primary text-decoration-none">隐私政策</a>。
                  </p>

                  <v-divider class="my-6" />

                  <!-- 注册链接 -->
                  <div class="text-center">
                    <span class="text-body-2 text-medium-emphasis">
                      {{ t('user.login.registerLink') }}
                    </span>
                    <a
                      href="#"
                      class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                      @click.prevent="goToRegister"
                    >
                      {{ t('user.login.registerNow') }}
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useAuth } from '@/composables/useAuth'
import { useEmailRules, useValidationRules, useMaxLength } from '@/composables/useValidation'
import { RIGHTS_DECLARATION } from '@/constants/site'
import { HEADER_HEIGHT } from '@/constants/layout'
import { BUSINESS_ERROR } from '@/constants/errorCode'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'
import TurnstileWidget from '@/components/common/TurnstileWidget.vue'

const router = useRouter()
const { t } = useI18n()
const { login, isLoggingIn } = useAuth()

// 验证规则
const emailRules = useEmailRules()
const passwordRules = useValidationRules('password')
const emailMaxLength = useMaxLength('email')
const passwordMaxLength = useMaxLength('password')

// 表单引用
const loginFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

// 表单数据
const formData = ref({
  email: '',
  password: '',
})

// UI 状态
const showPassword = ref(false)
const errorMessage = ref('')

// Turnstile 验证（登录失败多次后显示）
const showCaptcha = ref(false)
const turnstileToken = ref('')
const turnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

const onTurnstileVerify = (token: string) => {
  turnstileToken.value = token
}

const onTurnstileError = () => {
  errorMessage.value = '人机验证加载失败，请刷新页面重试'
}

const onTurnstileExpire = () => {
  turnstileToken.value = ''
}

/**
 * 处理登录
 */
const handleLogin = async () => {
  // 验证表单
  if (!loginFormRef.value) return
  const { valid } = await loginFormRef.value.validate()
  if (!valid) return

  // 如果显示验证码但未完成验证
  if (showCaptcha.value && !turnstileToken.value) {
    errorMessage.value = '请完成人机验证'
    return
  }

  try {
    errorMessage.value = ''
    const token = showCaptcha.value ? turnstileToken.value : undefined
    await login(formData.value.email, formData.value.password, token)
    // 登录成功后会自动跳转（在 useAuth 中处理）
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }

    // 检查是否需要验证码（错误码 2604）
    if (err?.code === BUSINESS_ERROR.CAPTCHA_REQUIRED) {
      showCaptcha.value = true
      errorMessage.value = '登录失败次数过多，请完成人机验证'
      return
    }

    // 检查是否为邮箱未验证错误（错误码 1104）
    if (err?.code === BUSINESS_ERROR.USER_EMAIL_NOT_VALIDATED) {
      // 跳转到邮箱验证页面
      await router.push({
        path: '/verify-email',
        query: { email: formData.value.email },
      })
      return
    }

    // 其他错误：显示错误信息
    if (err?.message) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = t('user.login.loginFailed')
    }

    // 重置 Turnstile
    if (showCaptcha.value) {
      turnstileToken.value = ''
      turnstileRef.value?.reset()
    }
  }
}

/**
 * 跳转到注册页
 */
const goToRegister = () => {
  void router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* 装饰性渐变球 - 右上角 */
.login-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(
    circle,
    rgba(255, 87, 34, 0.25) 0%,
    rgba(255, 87, 34, 0.1) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

/* 装饰性渐变球 - 左下角 */
.login-page::after {
  content: '';
  position: absolute;
  bottom: -25%;
  left: -15%;
  width: 1100px;
  height: 1100px;
  background: radial-gradient(
    circle,
    rgba(0, 188, 212, 0.2) 0%,
    rgba(0, 188, 212, 0.08) 40%,
    transparent 70%
  );
  border-radius: 50%;
  pointer-events: none;
}

.login-container {
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

/* Login Card */
.login-card-wrapper {
  width: 100%;
  max-width: 480px;
}

.login-card {
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

/* Turnstile 容器 */
.turnstile-wrapper {
  display: flex;
  justify-content: center;
}

/* Responsive */
@media (max-width: 960px) {
  .intro-section {
    text-align: center;
    padding: 20px;
  }
}
</style>
