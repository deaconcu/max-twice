<template>
  <div class="verify-page">
    <AppHeader />

    <v-container fluid class="verify-container">
      <v-row align="center" justify="center">
        <v-col cols="12" sm="10" md="8" lg="6" xl="5">
          <v-card class="verify-card" border rounded="xl">
            <v-card-text class="text-center pa-10">
              <div class="icon-circle mb-6">
                <v-icon size="64" color="primary">mdi-email-outline</v-icon>
              </div>

              <h1 class="text-h4 font-weight-bold mb-4">验证您的邮箱</h1>

              <p class="text-body-1 text-medium-emphasis mb-6">
                我们已向 <span class="font-weight-bold text-primary">{{ email }}</span> 发送了一封验证邮件
              </p>

              <p class="text-body-2 text-medium-emphasis mb-8">
                请点击邮件中的链接完成验证。如果没有收到邮件，请检查垃圾邮件箱。
              </p>

              <v-text-field
                v-model="verificationCode"
                label="验证码"
                variant="outlined"
                density="comfortable"
                :rules="[rules.required]"
                hide-details="auto"
                class="mb-6"
                placeholder="请输入6位验证码"
              />

              <v-btn
                @click="handleVerify"
                block
                size="large"
                color="primary"
                class="text-none font-weight-bold mb-4"
                :disabled="!verificationCode || verificationCode.length !== 6"
              >
                验证邮箱
              </v-btn>

              <v-btn
                @click="handleResend"
                variant="text"
                color="primary"
                class="text-none"
                :disabled="countdown > 0"
              >
                {{ countdown > 0 ? `${countdown}秒后可重新发送` : '重新发送验证邮件' }}
              </v-btn>

              <v-divider class="my-6" />

              <div class="text-center">
                <a href="#" @click.prevent="$router.push('/login')" class="text-body-2 text-primary text-decoration-none">
                  返回登录
                </a>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'

const route = useRoute()
const router = useRouter()

const email = ref(route.query.email as string || '')
const verificationCode = ref('')
const countdown = ref(0)
let countdownInterval: number | null = null

const rules = {
  required: (value: string) => !!value || '请输入验证码'
}

const handleVerify = () => {
  if (verificationCode.value && verificationCode.value.length === 6) {
    console.log('Verify email:', email.value, 'Code:', verificationCode.value)
    // 验证成功后跳转到登录页
    router.push('/login')
  }
}

const handleResend = () => {
  console.log('Resend verification email to:', email.value)
  // 重新发送邮件
  countdown.value = 60
  startCountdown()
}

const startCountdown = () => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
  countdownInterval = window.setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else if (countdownInterval) {
      clearInterval(countdownInterval)
      countdownInterval = null
    }
  }, 1000)
}

onMounted(() => {
  if (!email.value) {
    router.push('/register')
  }
  // 初始倒计时60秒
  countdown.value = 60
  startCountdown()
})

onUnmounted(() => {
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
})
</script>

<style scoped>
.verify-page {
  height: 100vh;
  background-color: #FAFBFC;
  position: relative;
  overflow: hidden;
}

.verify-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(circle, rgba(255, 87, 34, 0.25) 0%, rgba(255, 87, 34, 0.1) 40%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.verify-page::after {
  content: '';
  position: absolute;
  bottom: -25%;
  left: -15%;
  width: 1100px;
  height: 1100px;
  background: radial-gradient(circle, rgba(0, 188, 212, 0.2) 0%, rgba(0, 188, 212, 0.08) 40%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.verify-container {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
  padding: 40px 20px;
}

.verify-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-border));
}

.icon-circle {
  width: 120px;
  height: 120px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
}

.v-text-field :deep(.v-field) {
  border-radius: 20px;
}

.v-btn {
  border-radius: 20px;
}

a:hover {
  text-decoration: underline !important;
}
</style>
