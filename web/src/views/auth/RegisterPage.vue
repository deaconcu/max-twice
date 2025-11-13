<template>
  <div class="register-page">
    <AppHeader />

    <v-container fluid class="register-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" md="5" class="intro-section d-none d-md-flex">
          <IntroSection :items="RIGHTS_DECLARATION" />
        </v-col>

        <!-- Right side - Register Form -->
        <v-col cols="12" md="5" sm="8" class="d-flex justify-center">
          <div class="register-card-wrapper">
            <v-card class="register-card" border rounded="lg">
              <!-- Logo Section -->
              <v-card-text class="text-center pa-8">
                <div class="logo-circle">
                  <v-icon size="48" color="primary">mdi-reddit</v-icon>
                </div>
                <h2 class="text-h5 font-weight-bold mt-4 mb-2">
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
                    :disabled="isRegistering"
                    variant="outlined"
                    density="comfortable"
                    hide-details="auto"
                    class="mb-4"
                  />

                  <!-- 密码输入 -->
                  <v-text-field
                    v-model="formData.password"
                    :label="t('user.register.password')"
                    :type="showPassword ? 'text' : 'password'"
                    :rules="passwordRules"
                    :disabled="isRegistering"
                    :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                    variant="outlined"
                    density="comfortable"
                    hide-details="auto"
                    class="mb-4"
                    @click:append-inner="showPassword = !showPassword"
                  />

                  <!-- 确认密码输入 -->
                  <v-text-field
                    v-model="formData.confirmPassword"
                    :label="t('user.register.confirmPassword')"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    :rules="confirmPasswordRules"
                    :disabled="isRegistering"
                    :append-inner-icon="showConfirmPassword ? 'mdi-eye-off' : 'mdi-eye'"
                    variant="outlined"
                    density="comfortable"
                    hide-details="auto"
                    class="mb-2"
                    @click:append-inner="showConfirmPassword = !showConfirmPassword"
                  />

                  <!-- 错误提示 -->
                  <v-alert
                    v-if="errorMessage"
                    type="error"
                    variant="tonal"
                    density="compact"
                    closable
                    class="mb-4 mt-4"
                    @click:close="errorMessage = ''"
                  >
                    {{ errorMessage }}
                  </v-alert>

                  <!-- 注册按钮 -->
                  <v-btn
                    type="submit"
                    :loading="isRegistering"
                    :disabled="isRegistering"
                    block
                    size="large"
                    color="primary"
                    class="text-none font-weight-bold mb-3 mt-6"
                  >
                    {{
                      isRegistering
                        ? t('user.register.registering')
                        : t('user.register.registerButton')
                    }}
                  </v-btn>

                  <!-- 用户协议 -->
                  <p class="text-caption text-center text-medium-emphasis mb-4">
                    继续操作即表示你同意我们的
                    <a href="#" class="text-primary text-decoration-none">用户协议</a>
                    并确认已了解
                    <a href="#" class="text-primary text-decoration-none">隐私政策</a>。
                  </p>

                  <v-divider class="my-6" />

                  <!-- 登录链接 -->
                  <div class="text-center">
                    <span class="text-body-2 text-medium-emphasis"> 已有账号？ </span>
                    <a
                      href="#"
                      class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                      @click.prevent="goToLogin"
                    >
                      立即登录
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
import { emailRules, passwordRules, confirmPassword } from '@/utils/validation'
import { RIGHTS_DECLARATION } from '@/constants/site'
import { HEADER_HEIGHT } from '@/constants/layout'
import AppHeader from '@/components/layout/AppHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'

const router = useRouter()
const { t } = useI18n()
const { register, isRegistering } = useAuth()

// 表单引用
const registerFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

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

// 确认密码验证规则
const confirmPasswordRules = computed(() => [
  ...passwordRules,
  confirmPassword(formData.value.password, '两次输入的密码不一致'),
])

/**
 * 处理注册
 */
const handleRegister = async () => {
  // 验证表单
  if (!registerFormRef.value) return
  const { valid } = await registerFormRef.value.validate()
  if (!valid) return

  try {
    errorMessage.value = ''
    const success = await register(formData.value.email, formData.value.password)

    if (success) {
      // TODO: 注册成功后跳转到邮箱验证页面
      // await router.push({ path: '/verify-email', query: { email: formData.value.email } })
    } else {
      errorMessage.value = t('user.register.registerFailed')
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('user.register.registerFailed')
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

/* 装饰性渐变球 - 右上角 */
.register-page::before {
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
.register-page::after {
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
