<template>
  <div class="auth-page">
    <!-- 背景装饰：8 人同时在学习的打卡墙 —— 上下两端各自向中间推进，中间留白让给登录区 -->
    <div class="bg-decor" aria-hidden="true">
      <div class="heatmap-wrap">
        <svg class="heatmap" viewBox="0 0 1600 900" preserveAspectRatio="xMidYMid slice">
          <defs>
            <!-- 未打卡的空格子：极淡的背景点阵，让画面没打卡的地方也是"方格"结构 -->
            <pattern id="cell-grid" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
              <rect
                x="1"
                y="1"
                width="22"
                height="22"
                rx="4"
                fill="rgb(var(--v-theme-on-surface))"
                fill-opacity="0.006"
              />
            </pattern>
          </defs>

          <!-- 底层：整张画布铺满"未打卡"的空格子 -->
          <rect x="0" y="0" width="1600" height="900" fill="url(#cell-grid)" />

          <!-- 8 列打卡矩阵：每列 1 种颜色，顶部向下、底部向上各堆一段 -->
          <g class="heat-cells">
            <rect
              v-for="cell in heatmapCells"
              :key="cell.key"
              :x="cell.x"
              :y="cell.y"
              width="22"
              height="22"
              rx="4"
              :fill="cell.color"
              :fill-opacity="cell.opacity"
            />
          </g>
        </svg>
      </div>
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
                  <!-- 登录表单（仅邮箱 + Turnstile） -->
                  <v-form
                    v-if="currentMode === 'login'"
                    key="login"
                    ref="loginFormRef"
                    v-model="loginFormValid"
                    @submit.prevent="handleLogin"
                  >
                    <v-text-field
                      v-model="loginData.email"
                      :label="t('user.login.email')"
                      :placeholder="t('user.login.emailPlaceholder')"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :maxlength="emailMaxLength"
                      :disabled="isLoggingIn"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :disabled="isLoggingIn || !loginFormValid || !loginData.email || waitingForTurnstile === 'login' || isInviteBlocked"
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-6"
                    >
                      <template v-if="waitingForTurnstile === 'login'">
                        <v-progress-circular indeterminate size="18" width="2" class="mr-2" />
                        {{ t('user.login.captchaVerifying') }}
                      </template>
                      <template v-else-if="isLoggingIn">
                        <v-progress-circular indeterminate size="18" width="2" class="mr-2" />
                        {{ t('user.login.loggingIn') }}
                      </template>
                      <template v-else>
                        {{ t('user.login.loginButton') }}
                      </template>
                    </v-btn>

                    <TurnstileWidget
                      ref="loginTurnstileRef"
                      appearance="interaction-only"
                      @verify="onLoginTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onLoginTurnstileExpire"
                    />

                    <p class="text-body-2 text-center mb-4">
                      <a
                        href="#"
                        class="text-primary text-decoration-none"
                        @click.prevent="switchToPasswordLogin"
                      >
                        {{ t('user.login.usePasswordLogin') }}
                      </a>
                    </p>

                    <p class="text-caption text-center text-medium-emphasis mb-2">
                      {{ t('user.login.autoRegisterHint') }}
                    </p>

                    <p class="text-caption text-medium-emphasis mb-0 agreement-text">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a>
                    </p>
                  </v-form>


                  <!-- 密码登录表单 -->
                  <v-form
                    v-else-if="currentMode === 'password-login'"
                    key="password-login"
                    ref="passwordFormRef"
                    v-model="passwordFormValid"
                    @submit.prevent="handlePasswordLogin"
                  >
                    <v-text-field
                      v-model="loginData.email"
                      :label="t('user.login.email')"
                      :placeholder="t('user.login.emailPlaceholder')"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :maxlength="emailMaxLength"
                      :disabled="isLoggingIn"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <v-text-field
                      v-model="passwordData.password"
                      :label="t('user.login.password')"
                      :placeholder="t('user.login.passwordPlaceholder')"
                      :type="passwordData.showPassword ? 'text' : 'password'"
                      :disabled="isLoggingIn"
                      autocomplete="current-password"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    >
                      <template #append-inner>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="passwordData.showPassword = !passwordData.showPassword"
                        >
                          {{ passwordData.showPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :disabled="
                        isLoggingIn ||
                        !passwordFormValid ||
                        !loginData.email ||
                        !passwordData.password ||
                        waitingForTurnstile === 'password-login' ||
                        isInviteBlocked
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-6"
                    >
                      <template v-if="waitingForTurnstile === 'password-login'">
                        <v-progress-circular indeterminate size="18" width="2" class="mr-2" />
                        {{ t('user.login.captchaVerifying') }}
                      </template>
                      <template v-else-if="isLoggingIn">
                        <v-progress-circular indeterminate size="18" width="2" class="mr-2" />
                        {{ t('user.login.loggingIn') }}
                      </template>
                      <template v-else>
                        {{ t('user.login.loginWithPasswordButton') }}
                      </template>
                    </v-btn>

                    <TurnstileWidget
                      ref="loginTurnstileRef"
                      appearance="interaction-only"
                      @verify="onLoginTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onLoginTurnstileExpire"
                    />

                    <p class="text-body-2 text-center mb-4">
                      <a
                        href="#"
                        class="text-primary text-decoration-none"
                        @click.prevent="switchToCodeLogin"
                      >
                        {{ t('user.login.useCodeLogin') }}
                      </a>
                    </p>

                    <p class="text-caption text-medium-emphasis mb-0 agreement-text">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a>
                    </p>
                  </v-form>


                  <!-- 邮箱验证表单 -->
                  <v-form
                    v-else-if="currentMode === 'verify'"
                    key="verify"
                    ref="verifyFormRef"
                    @submit.prevent="handleVerify"
                  >
                    <v-alert
                      color="primary"
                      variant="tonal"
                      density="compact"
                      rounded="lg"
                      class="mb-4"
                    >
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
                      validate-on="blur"
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
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.verifyEmail.changeEmail') }}
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
import {
  useEmailRules,
  useMaxLength,
  useVerificationCodeRule,
} from '@/composables/useValidation'
import { HEADER_HEIGHT } from '@/constants/layout'
import { BUSINESS_ERROR } from '@/constants/errorCode'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'
import TurnstileWidget from '@/components/common/TurnstileWidget.vue'
import IcosahedronLogo from '@/components/common/IcosahedronLogo.vue'

type AuthMode = 'login' | 'verify' | 'password-login'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const authStore = useAuthStore()
const userStore = useUserStore()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const currentMode = ref<AuthMode>('login')

// 背景打卡墙：8 个用户同时在学习的视觉隐喻
// 8 列横向占满画布，每列 1 种颜色，上下两端各自向中间堆一段；
// 格子透明度有深浅变化，模拟真实热力图（学得深的格子更深）
const heatmapCells = computed(() => {
  const CELL = 22
  const GAP = 2
  const STEP = CELL + GAP // 24
  const COL_COUNT = 8
  const VB_W = 1600
  const VB_H = 900

  // 横向：每列 8 槽（7 格彩色 + 1 格空白），8 列 × 8 × 24 = 1536，接近铺满 1600
  const cellsPerCol = 8 // 每列 8 个槽位
  const fillCellsPerCol = 7 // 前 7 槽填彩色，第 8 槽留白做列间分隔
  const colPx = cellsPerCol * STEP // 196
  const totalMatrixW = COL_COUNT * colPx // 1568
  const leftPad = Math.floor((VB_W - totalMatrixW) / 2 / STEP) * STEP // 0

  // 纵向：pattern 网格从 0 起步长 24，底部堆行也必须落在 24 的倍数上，
  // 所以用"画布能容纳的行数"的最后一行作为底行
  const rowsAvail = Math.floor(VB_H / STEP) // 37

  // 顶部 8 个用户的颜色，按色轮顺序（蓝→青→绿→黄→琥珀→红→粉→紫）
  const topColors = [
    '#3b82f6',
    '#06b6d4',
    '#10b981',
    '#eab308',
    '#f59e0b',
    '#ef4444',
    '#ec4899',
    '#8b5cf6',
  ]

  // 底部是另外 8 个用户，颜色顺序打乱，避免上下对称的"条带感"
  const bottomColors = [
    '#ec4899',
    '#10b981',
    '#8b5cf6',
    '#06b6d4',
    '#ef4444',
    '#eab308',
    '#3b82f6',
    '#f59e0b',
  ]

  // 顶部每列堆几行 / 底部每列堆几行
  // 顶部只有薄薄一层（"头轻"），底部更饱满、参差不齐
  const topRowCounts = [3, 2, 3, 2, 3, 1, 3, 2]
  const bottomRowCounts = [5, 9, 6, 8, 4, 7, 10, 5]

  // 固定 seed 线性同余：每次渲染结果一致
  let seed = 20260423
  const rand = () => {
    seed = (seed * 1103515245 + 12345) & 0x7fffffff
    return seed / 0x7fffffff
  }

  // 4 档热度：浅 → 深
  // 4 档热度：第 0 档 = 空白底色同款（彩色"消失"在空白里），后 3 档递深
  // 概率从浅到深梯度降低：空白最多、最深最少，模拟真实打卡分布
  const EMPTY_COLOR = 'rgb(var(--v-theme-on-surface))'
  const heatLevels: { color: (c: string) => string; opacity: number; weight: number }[] = [
    { color: () => EMPTY_COLOR, opacity: 0.006, weight: 4 },
    { color: (c) => c, opacity: 0.05, weight: 3 },
    { color: (c) => c, opacity: 0.07, weight: 2 },
    { color: (c) => c, opacity: 0.09, weight: 1 },
  ]
  const totalWeight = heatLevels.reduce((s, lv) => s + lv.weight, 0)

  const pickCell = (color: string) => {
    let r = rand() * totalWeight
    for (const lv of heatLevels) {
      r -= lv.weight
      if (r <= 0) return { color: lv.color(color), opacity: lv.opacity }
    }
    const last = heatLevels[heatLevels.length - 1]
    return { color: last.color(color), opacity: last.opacity }
  }

  const cells: { key: string; x: number; y: number; color: string; opacity: number }[] = []

  for (let col = 0; col < COL_COUNT; col++) {
    const colStartX = leftPad + col * colPx
    const topRowN = topRowCounts[col]
    const bottomRowN = bottomRowCounts[col]
    const topColor = topColors[col]
    const bottomColor = bottomColors[col]

    // 顶部段：从 y=0 向下堆 topRowN 行
    for (let r = 0; r < topRowN; r++) {
      for (let c = 0; c < fillCellsPerCol; c++) {
        if (rand() < 0.18) continue
        cells.push({
          key: `t-${col}-${r}-${c}`,
          x: colStartX + c * STEP,
          y: r * STEP,
          ...pickCell(topColor),
        })
      }
    }

    // 底部段：从画布最底一行往上堆 bottomRowN 行
    for (let r = 0; r < bottomRowN; r++) {
      for (let c = 0; c < fillCellsPerCol; c++) {
        if (rand() < 0.18) continue
        cells.push({
          key: `b-${col}-${r}-${c}`,
          x: colStartX + c * STEP,
          y: VB_H - CELL - r * STEP,
          ...pickCell(bottomColor),
        })
      }
    }
  }

  return cells
})

const emailRules = useEmailRules()
const verificationCodeRule = useVerificationCodeRule()
const emailMaxLength = useMaxLength('email')

const loginFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const verifyFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const passwordFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const loginFormValid = ref(false)
const passwordFormValid = ref(false)

const showSlogan = ref(false)

// 邮箱字段在「验证码登录」与「密码登录」之间共享
const loginData = ref({ email: '' })
const passwordData = ref({ password: '', showPassword: false })
const verifyData = ref({ code: '' })
const verifyEmail = computed(() => authStore.pendingSession?.email ?? '')

const errorMessage = ref('')
let errorTimer: ReturnType<typeof setTimeout> | null = null

const loginTurnstileToken = ref('')
const loginTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

// 用户已点击提交，但 Turnstile token 还没拿到 → 进入"等待验证"状态
// token 一旦到达，自动触发真正的提交（用户无需再点）
const waitingForTurnstile = ref<null | 'login' | 'password-login'>(null)

// 命中内测拦截：禁用表单并保留错误提示，刷新页面才能再试
// 设计意图：避免再次点击触发 Turnstile reset → CF 弹挑战框，观感突兀
const isInviteBlocked = ref(false)

const isLoggingIn = computed(() => authStore.isLoggingIn)
const isVerifying = ref(false)
const isResending = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

const modeConfig = computed(() => {
  switch (currentMode.value) {
    case 'login':
      return {
        title: t('user.login.title'),
        subtitle: t('user.login.subtitle'),
      }
    case 'verify':
      return {
        title: t('user.verifyEmail.title'),
        subtitle: t('user.verifyEmail.subtitle'),
      }
    case 'password-login':
      return {
        title: t('user.login.passwordLoginTitle'),
        subtitle: t('user.login.passwordLoginSubtitle'),
      }
    default:
      return { title: '', subtitle: '' }
  }
})

const showError = (message: string, persistent = false) => {
  if (errorTimer) clearTimeout(errorTimer)
  errorMessage.value = message
  if (persistent) return
  errorTimer = setTimeout(() => {
    errorMessage.value = ''
  }, 10000)
}

const clearError = () => {
  errorMessage.value = ''
  if (errorTimer) clearTimeout(errorTimer)
}

const onLoginTurnstileVerify = (token: string) => {
  loginTurnstileToken.value = token
}
const onLoginTurnstileExpire = () => {
  loginTurnstileToken.value = ''
}
const onTurnstileError = () => {
  showError(t('user.login.captchaLoadFailed'))
}

const startCountdown = (seconds = 60) => {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) stopCountdown()
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

const switchToLogin = () => {
  clearError()
  authStore.clearPendingSession()
  waitingForTurnstile.value = null
  currentMode.value = 'login'
  void router.replace('/login')
}

const switchToPasswordLogin = () => {
  clearError()
  authStore.clearPendingSession()
  waitingForTurnstile.value = null
  passwordData.value.password = ''
  passwordData.value.showPassword = false
  loginTurnstileToken.value = ''
  loginTurnstileRef.value?.reset()
  currentMode.value = 'password-login'
}

const switchToCodeLogin = () => {
  clearError()
  waitingForTurnstile.value = null
  loginTurnstileToken.value = ''
  loginTurnstileRef.value?.reset()
  currentMode.value = 'login'
}

const switchToVerify = () => {
  clearError()
  verifyData.value.code = ''
  currentMode.value = 'verify'
  const pending = authStore.pendingSession
  if (pending && pending.resendAvailableIn > 0) {
    startCountdown(pending.resendAvailableIn)
  } else {
    stopCountdown()
  }
  void router.replace('/verify-email')
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  const { valid } = await loginFormRef.value.validate()
  if (!valid) return

  clearError()

  // 如果 token 还没拿到（首次验证慢/上次过期续签中），进入等待态，
  // watch 会在 token 到达时自动调 doLoginSendCode
  if (!loginTurnstileToken.value) {
    waitingForTurnstile.value = 'login'
    return
  }
  await doLoginSendCode()
}

const doLoginSendCode = async () => {
  try {
    await authStore.loginSendCode(loginData.value.email, loginTurnstileToken.value)
    switchToVerify()
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    const isInvite = err.code === BUSINESS_ERROR.INVITE_ONLY
    if (err.message) {
      showError(err.message, isInvite)
    } else {
      showError(t('user.login.sendCodeFailed'))
    }
    if (isInvite) {
      // 内测拒绝：锁住表单，避免重置 Turnstile 让 CF 再次弹挑战框
      isInviteBlocked.value = true
      return
    }
    loginTurnstileToken.value = ''
    loginTurnstileRef.value?.reset()
  }
}

const handlePasswordLogin = async () => {
  if (!passwordFormRef.value) return
  const { valid } = await passwordFormRef.value.validate()
  if (!valid) return

  clearError()

  if (!loginTurnstileToken.value) {
    waitingForTurnstile.value = 'password-login'
    return
  }
  await doPasswordLogin()
}

const doPasswordLogin = async () => {
  try {
    const result = await authStore.login(
      loginData.value.email,
      passwordData.value.password,
      loginTurnstileToken.value
    )
    if (result === 'success') {
      showSnackbar?.(t('user.login.loginSuccess'), 'success')
      const redirect = route.query.redirect as string
      await router.push(redirect || '/')
    } else if (result === 'pending') {
      // 邮箱未验证：复用现有验证码流程
      switchToVerify()
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    const isInvite = err.code === BUSINESS_ERROR.INVITE_ONLY
    if (err.message) {
      showError(err.message, isInvite)
    } else {
      showError(t('user.login.loginFailed'))
    }
    if (isInvite) {
      isInviteBlocked.value = true
      return
    }
    loginTurnstileToken.value = ''
    loginTurnstileRef.value?.reset()
  }
}

const handleVerify = async () => {
  if (!verifyFormRef.value) return
  const { valid } = await verifyFormRef.value.validate()
  if (!valid) return

  if (!authStore.pendingSession) {
    showError(t('user.verifyEmail.emailInvalid'))
    switchToLogin()
    return
  }

  try {
    isVerifying.value = true
    clearError()
    const user = await authStore.verifyLoginCode(verifyData.value.code)
    if (user) {
      userStore.setUser(user)
      showSnackbar?.(t('user.verifyEmail.verifySuccess'), 'success')
      const redirect = route.query.redirect as string
      await router.push(redirect || '/')
    } else {
      showError(t('user.verifyEmail.verifyFailed'))
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.code === BUSINESS_ERROR.PENDING_SESSION_INVALID) {
      authStore.clearPendingSession()
      showError(err.message ?? t('user.verifyEmail.emailInvalid'))
      switchToLogin()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.verifyFailedRetry'))
    }
  } finally {
    isVerifying.value = false
  }
}

const handleResend = async () => {
  if (!authStore.pendingSession) {
    showError(t('user.verifyEmail.emailInvalid'))
    switchToLogin()
    return
  }

  try {
    isResending.value = true
    clearError()
    const next = await authStore.resendLoginCode()
    showSnackbar?.(t('user.verifyEmail.codeSent'), 'success')
    if (next) {
      startCountdown(next.resendAvailableIn > 0 ? next.resendAvailableIn : 60)
    } else {
      startCountdown(60)
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.code === BUSINESS_ERROR.PENDING_SESSION_INVALID) {
      authStore.clearPendingSession()
      showError(err.message ?? t('user.verifyEmail.emailInvalid'))
      switchToLogin()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.sendFailed'))
    }
  } finally {
    isResending.value = false
  }
}

const initModeFromRoute = () => {
  const routeName = route.name as string
  if (routeName === 'verify-email') {
    if (!authStore.pendingSession) {
      showSnackbar?.(t('user.verifyEmail.emailInvalidRedirect'), 'error')
      currentMode.value = 'login'
      void router.replace('/login')
      return
    }
    currentMode.value = 'verify'
    const pending = authStore.pendingSession
    if (pending.resendAvailableIn > 0) startCountdown(pending.resendAvailableIn)
  } else {
    currentMode.value = 'login'
  }
}

watch(
  () => route.name,
  () => {
    initModeFromRoute()
  }
)

// Turnstile token 到达时，若用户已点过提交按钮（在等待中），自动继续提交
watch(loginTurnstileToken, (token) => {
  if (!token || !waitingForTurnstile.value) return
  const action = waitingForTurnstile.value
  waitingForTurnstile.value = null
  if (action === 'login') void doLoginSendCode()
  else if (action === 'password-login') void doPasswordLogin()
})

onMounted(() => {
  initModeFromRoute()
})

onUnmounted(() => {
  stopCountdown()
  if (errorTimer) clearTimeout(errorTimer)
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* ========== 背景装饰：学习热力图 ========== */
.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.heatmap-wrap {
  position: absolute;
  inset: 0;
  opacity: 0.9;
  will-change: transform;
}

.heatmap {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 角落极淡光晕，给背景一丝深度但不抢戏 */
.bg-decor::before {
  content: '';
  position: absolute;
  top: -15%;
  right: -10%;
  width: 700px;
  height: 700px;
  background: radial-gradient(
    circle,
    rgba(var(--v-theme-primary), 0.08) 0%,
    transparent 70%
  );
  pointer-events: none;
  border-radius: 50%;
}

.bg-decor::after {
  content: '';
  position: absolute;
  bottom: -20%;
  left: -12%;
  width: 800px;
  height: 800px;
  background: radial-gradient(
    circle,
    rgba(124, 58, 237, 0.07) 0%,
    transparent 70%
  );
  pointer-events: none;
  border-radius: 50%;
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
  align-items: flex-start;
  color: #e53935;
  font-size: 14px;
  line-height: 1.5;
}

.error-message .v-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* 协议文案：区域按内容宽度居中，内部文本左对齐 */
.agreement-text {
  width: fit-content;
  max-width: 402px;
  margin-left: auto;
  margin-right: auto;
  text-align: left;
  line-height: 1.6;
}

/* 密码强度条 */
.strength-segments {
  display: flex;
  align-items: center;
  gap: 3px;
  line-height: 1;
  flex: none;
  white-space: nowrap;
}
.strength-label {
  flex: none;
  min-width: 56px;
  text-align: right;
  margin-right: 6px;
}
.strength-segments .seg {
  flex: none;
  width: 6px;
  height: 0.7em;
  border-radius: 2px;
  background-color: rgba(var(--v-theme-on-surface), 0.12);
  transition: background-color 0.2s ease;
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
