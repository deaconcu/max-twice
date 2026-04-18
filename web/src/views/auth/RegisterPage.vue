<template>
  <div class="register-page">
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

    <v-container fluid class="register-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" lg="6" class="intro-section d-none d-lg-flex">
          <IntroSection />
        </v-col>

        <!-- Right side - Register Form -->
        <v-col cols="12" sm="12" md="8" lg="4" class="d-flex justify-center">
          <div class="register-card-wrapper">
            <v-card class="register-card" rounded="xl" elevation="0">
              <!-- Logo Section -->
              <v-card-text class="text-center pt-10 pb-2 px-8">
                <div
                  class="logo-with-slogan"
                  @mouseenter="showSlogan = true"
                  @mouseleave="showSlogan = false"
                >
                  <IcosahedronLogo :size="72" />
                  <span class="logo-slogan" :class="{ visible: showSlogan }">{{ t('intro.logoSlogan') }}</span>
                </div>
                <h2 class="text-h4 font-weight-bold mb-2">
                  {{ t('user.register.title') }}
                </h2>
                <p class="text-body-2 text-medium-emphasis">{{ t('user.register.subtitle') }}</p>
              </v-card-text>

              <!-- Register Form -->
              <v-card-text class="px-8 pb-8">
                <v-form ref="registerFormRef" @submit.prevent="handleRegister">
                  <!-- 邮箱输入 -->
                  <v-text-field
                    v-model="formData.email"
                    :label="t('user.register.email')"
                    type="email"
                    :rules="emailRules"
                    :counter="emailMaxLength"
                    :disabled="isRegistering"
                    variant="outlined"
                    density="comfortable"
                    class="mb-4"
                  />

                  <!-- 密码输入 -->
                  <v-text-field
                    v-model="formData.password"
                    :label="t('user.register.password')"
                    :type="showPassword ? 'text' : 'password'"
                    :rules="passwordRules"
                    :counter="passwordMaxLength"
                    :disabled="isRegistering"
                    :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                    variant="outlined"
                    density="comfortable"
                    class="mb-4"
                    @click:append-inner="showPassword = !showPassword"
                  />

                  <!-- 确认密码输入 -->
                  <v-text-field
                    v-model="formData.confirmPassword"
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

                  <!-- 错误提示 -->
                  <div v-if="errorMessage" class="error-message mb-4">
                    <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                    {{ errorMessage }}
                  </div>

                  <!-- Turnstile 人机验证 -->
                  <TurnstileWidget
                    ref="turnstileRef"
                    class="mb-4"
                    @verify="onTurnstileVerify"
                    @error="onTurnstileError"
                    @expire="onTurnstileExpire"
                  />

                  <!-- 注册按钮 -->
                  <v-btn
                    type="submit"
                    :loading="isRegistering"
                    :disabled="isRegistering || !formData.email || !formData.password || !formData.confirmPassword"
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

                  <!-- 用户协议 -->
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

                  <!-- 登录链接 -->
                  <div class="text-center">
                    <span class="text-body-2 text-medium-emphasis">
                      {{ t('user.register.loginLink') }}
                    </span>
                    <a
                      href="#"
                      class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                      @click.prevent="goToLogin"
                    >
                      {{ t('user.register.loginNow') }}
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useAuth } from '@/composables/useAuth'
import {
  useEmailRules,
  useValidationRules,
  useMaxLength,
  useConfirmPasswordRule,
} from '@/composables/useValidation'
import { HEADER_HEIGHT } from '@/constants/layout'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'
import TurnstileWidget from '@/components/common/TurnstileWidget.vue'
import IcosahedronLogo from '@/components/common/IcosahedronLogo.vue'

const router = useRouter()
const { t } = useI18n()
const { register, isRegistering } = useAuth()

// 验证规则
const emailRules = useEmailRules()
const passwordRules = useValidationRules('password')
const confirmPasswordRule = useConfirmPasswordRule()
const emailMaxLength = useMaxLength('email')
const passwordMaxLength = useMaxLength('password')

// 表单引用
const registerFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

// Logo slogan
const showSlogan = ref(false)

// 表单数据
const formData = ref({
  email: '',
  password: '',
  confirmPassword: '',
})

// UI 状态
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
let errorTimer: ReturnType<typeof setTimeout> | null = null

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

// Turnstile 验证
const turnstileToken = ref('')
const turnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

const onTurnstileVerify = (token: string) => {
  turnstileToken.value = token
}

const onTurnstileError = () => {
  showError(t('user.register.captchaLoadFailed'))
}

const onTurnstileExpire = () => {
  turnstileToken.value = ''
}

// 确认密码验证规则
const confirmPasswordRules = computed(() => [
  ...passwordRules.value,
  confirmPasswordRule(formData.value.password),
])

/**
 * 处理注册
 */
const handleRegister = async () => {
  // 验证表单
  if (!registerFormRef.value) return
  const { valid } = await registerFormRef.value.validate()
  if (!valid) return

  // 验证 Turnstile token
  if (!turnstileToken.value) {
    showError(t('user.register.captchaRequired'))
    return
  }

  try {
    errorMessage.value = ''
    const success = await register(
      formData.value.email,
      formData.value.password,
      turnstileToken.value
    )

    if (success) {
      // 注册成功后跳转到邮箱验证页面
      await router.push({
        path: '/verify-email',
        query: { email: formData.value.email },
      })
    }
  } catch (error: unknown) {
    // 显示后端返回的错误信息
    if (error instanceof Error && error.message) {
      showError(error.message)
    } else {
      showError(t('user.register.registerFailed'))
    }
    // 重置 Turnstile
    turnstileToken.value = ''
    turnstileRef.value?.reset()
  }
}

/**
 * 跳转到登录页
 */
const goToLogin = () => {
  void router.push('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* 模糊渐变球 - 右上角 */
.register-page::before {
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
.register-page::after {
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
  background-color: #AB47BC;
  border-radius: 50%;
  top: 12%;
  left: 15%;
}

/* 方形 - 黄色 */
.shape-12 {
  width: 40px;
  height: 40px;
  background-color: #FFCA28;
  border-radius: 8px;
  top: 70%;
  right: 8%;
  transform: rotate(20deg);
}

/* 圆形 - 粉色 */
.shape-13 {
  width: 35px;
  height: 35px;
  background-color: #EC407A;
  border-radius: 50%;
  bottom: 25%;
  left: 3%;
}

/* 方形 - 靛蓝 */
.shape-14 {
  width: 50px;
  height: 50px;
  background-color: #5C6BC0;
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
  background-color: #66BB6A;
  border-radius: 50%;
  top: 30%;
  left: 5%;
}

/* 方形 - 粉色 */
.shape-20 {
  width: 35px;
  height: 35px;
  background-color: #EC407A;
  border-radius: 6px;
  top: 8%;
  right: 25%;
  transform: rotate(30deg);
}

/* 圆形 - 黄色 */
.shape-21 {
  width: 45px;
  height: 45px;
  background-color: #FFCA28;
  border-radius: 50%;
  bottom: 40%;
  right: 5%;
}

/* 方形 - 绿色 */
.shape-23 {
  width: 30px;
  height: 30px;
  background-color: #66BB6A;
  border-radius: 5px;
  bottom: 12%;
  right: 30%;
  transform: rotate(-20deg);
}

/* 圆形 - 靛蓝 小 */
.shape-24 {
  width: 20px;
  height: 20px;
  background-color: #5C6BC0;
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
  background-color: #AB47BC;
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

.register-container {
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

/* Register Card */
.register-card-wrapper {
  width: 100%;
  max-width: 480px;
}

.register-card {
  background-color: rgba(var(--v-theme-surface), 0.5) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
}

.logo-circle {
  display: none;
}

.logo-icon {
  width: 56px;
  height: 56px;
  color: rgb(var(--v-theme-primary));
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

/* Responsive */
@media (max-width: 960px) {
  .intro-section {
    text-align: center;
    padding: 20px;
  }
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
  transition: opacity 0.3s ease, transform 0.3s ease;
  white-space: nowrap;
}

.logo-slogan.visible {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}
</style>
