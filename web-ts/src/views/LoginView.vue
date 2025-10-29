<script setup lang="ts">
import { ref, inject } from 'vue'
import type { Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { authServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import { USER_EMAIL_NOT_VALIDATED, USER_BANNED } from '@/constants/errorCodes'

import Footer from '@/components/common/PageFooter.vue'
import LoginHeader from '@/components/auth/LoginHeader.vue'
import WelcomeSection from '@/components/auth/WelcomeSection.vue'
import RegisterDialog from '@/components/auth/RegisterDialog.vue'
import EmailVerificationDialog from '@/components/auth/EmailVerificationDialog.vue'
import LoginDialog from '@/components/auth/LoginDialog.vue'

// 类型定义
interface RegisterForm {
  email: string
  password: string
  passwordRepeat: string
  validateCode: string
}

interface LoginForm {
  email: string
  password: string
}

const router = useRouter()
const user = useUserStore()
const { t } = useI18n()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

// 密码可见性状态
const showPassword: Ref<boolean> = ref(false)
const showPasswordRepeat: Ref<boolean> = ref(false)

// 对话框状态
const registerFirstDialog: Ref<boolean> = ref(false)
const registerSecondDialog: Ref<boolean> = ref(false)
const loginDialog: Ref<boolean> = ref(false)

// 表单数据
const registerForm: Ref<RegisterForm> = ref({
  email: '',
  password: '',
  passwordRepeat: '',
  validateCode: '',
})

const loginForm: Ref<LoginForm> = ref({
  email: 'deaconcc@126.com',
  password: '',
})

// 密码可见性切换
const togglePasswordVisibility = (repeat: boolean): void => {
  if (!repeat) showPassword.value = !showPassword.value
  else showPasswordRepeat.value = !showPasswordRepeat.value
}

// 注册第一步
const submitRegisterFirstForm = async (): Promise<void> => {
  try {
    console.log('begin post')
    const response = await authServiceV1.register(
      registerForm.value.email,
      registerForm.value.password
    )
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      registerFirstDialog.value = false
      registerSecondDialog.value = true
    } else {
      showSnackbar(response.message || '注册失败，请重试', 'error')
    }
  } catch (error: any) {
    console.error('Error submitting form:', error)
    const message = error?.response?.data?.message || error?.message || '网络错误，请检查网络连接'
    showSnackbar(message, 'error')
  }
}

// 邮箱验证
const submitRegisterSecondForm = async (): Promise<void> => {
  try {
    console.log('begin post')
    const response = await authServiceV1.validateEmail(
      registerForm.value.email,
      registerForm.value.validateCode
    )
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      registerSecondDialog.value = false

      // 使用 setUser 保存完整的用户对象
      user.setUser({
        id: response.data.id,
        name: response.data.name,
        subscriptions: response.data.subscriptions,
        role: response.data.role
      })

      showSnackbar('注册成功,欢迎加入!', 'success')
      router.push({ name: 'courseList', params: {} })
    } else {
      showSnackbar(response.message || '验证失败，请重试', 'error')
    }
  } catch (error: any) {
    console.error('Error submitting form:', error)
    const message = error?.response?.data?.message || error?.message || '网络错误，请检查网络连接'
    showSnackbar(message, 'error')
  }
}

// 登录
const submitLogin = async (): Promise<void> => {
  try {
    console.log('begin post')
    const response = await authServiceV1.login(loginForm.value.email, loginForm.value.password)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      console.log('Form submitted successfully')
      loginDialog.value = false

      // 使用 setUser 保存完整的用户对象
      user.setUser({
        id: response.data.id,
        name: response.data.name,
        subscriptions: response.data.subscriptions,
        role: response.data.role
      })

      showSnackbar('登录成功', 'success')
      router.push({ name: 'courseList', params: {} })
    } else if (response.code === USER_EMAIL_NOT_VALIDATED) {
      loginDialog.value = false
      registerForm.value.email = loginForm.value.email
      registerSecondDialog.value = true
      showSnackbar('请先验证邮箱', 'warning')
    } else if (response.code === USER_BANNED) {
      showSnackbar('该账号已被屏蔽，无法登录', 'error')
    } else {
      showSnackbar(response.message || '登录失败，请检查邮箱和密码', 'error')
    }
  } catch (error: any) {
    console.error('Error submitting form:', error)
    const message = error?.response?.data?.message || error?.message || '网络错误，请检查网络连接'
    showSnackbar(message, 'error')
  }
}

// 重新发送验证码
const resendVerificationCode = (): void => {
  // TODO: 实现重新发送验证码逻辑
  console.log('重新发送验证码')
}
</script>

<template>
  <v-app>
    <!-- 顶部标题栏 -->
    <LoginHeader />

    <!-- 页面内容 -->
    <v-main class="main-background">
      <v-container class="fill-height ma-0" fluid>
        <v-row class="align-center justify-center" no-gutters>
          <!-- 左侧图片区域 -->
          <WelcomeSection />

          <!-- 右侧登录和注册部分 -->
          <v-col cols="6" class="pa-8">
            <v-card rounded="xl" elevation="0" color="white" class="pa-8 custom-login-card">
              <div class="text-center mb-6">
                <h2 class="text-h4 font-weight-bold text-grey-darken-4 mb-2">开始您的学习之旅</h2>
                <p class="text-body-1 text-grey-darken-2">我不懂的问题，能一次教会我吗？</p>
                <p class="text-body-1 text-grey-darken-2">
                  最多 <span class="text-primary font-weight-bold">两次</span>，不能再多了 ^_^
                </p>
              </div>

              <div class="mb-6">
                <p class="text-h6 font-weight-medium text-grey-darken-3 mb-4">
                  现在就加入，或者先逛逛
                </p>

                <v-dialog v-model="registerFirstDialog" max-width="600" persistent>
                  <template #activator="{ props: activatorProps }">
                    <v-btn
                      block
                      size="large"
                      color="primary"
                      rounded="lg"
                      v-bind="activatorProps"
                      class="font-weight-bold mb-4"
                    >
                      <v-icon icon="mdi-account-plus" class="mr-2"></v-icon>
                      {{ t('common.register') }}
                    </v-btn>
                  </template>
                </v-dialog>
              </div>

              <div class="mb-4">
                <p class="text-body-2 text-grey-darken-2 text-center mb-4">
                  注册即表示同意
                  <a href="#" class="text-primary text-decoration-none">服务条款</a>
                  及
                  <a href="#" class="text-primary text-decoration-none">隐私政策</a>
                </p>
              </div>

              <div class="text-center">
                <p class="text-body-1 font-weight-medium text-grey-darken-3 mb-3">已有账号？</p>

                <v-dialog v-model="loginDialog" max-width="600" persistent>
                  <template #activator="{ props: activatorProps }">
                    <v-btn
                      block
                      size="large"
                      variant="outlined"
                      color="primary"
                      rounded="lg"
                      v-bind="activatorProps"
                      class="font-weight-bold"
                    >
                      <v-icon icon="mdi-login" class="mr-2"></v-icon>
                      登 录
                    </v-btn>
                  </template>
                </v-dialog>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <!-- 对话框组件 -->
    <RegisterDialog
      v-model="registerFirstDialog"
      :register-form="registerForm"
      :show-password="showPassword"
      :show-password-repeat="showPasswordRepeat"
      @update:register-form="registerForm = $event"
      @submit="submitRegisterFirstForm"
      @toggle-password="togglePasswordVisibility"
    />

    <EmailVerificationDialog
      v-model="registerSecondDialog"
      :validate-code="registerForm.validateCode"
      @update:validate-code="registerForm.validateCode = $event"
      @submit="submitRegisterSecondForm"
      @resend="resendVerificationCode"
    />

    <LoginDialog
      v-model="loginDialog"
      :login-form="loginForm"
      :show-password="showPassword"
      @update:login-form="loginForm = $event"
      @submit="submitLogin"
      @toggle-password="() => togglePasswordVisibility(false)"
    />

    <!-- 页脚 -->
    <v-footer app>
      <v-container class="ma-0 pa-0" fluid>
        <Footer />
      </v-container>
    </v-footer>
  </v-app>
</template>

<style>
.main-background {
  min-width: 1440px;
}
</style>
<style>
.main-background {
  min-width: 1440px;
}
.custom-login-card {
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  max-width: 480px;
  margin: 0 auto;
}
</style>