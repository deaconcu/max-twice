<template>
  <div class="login-page">
    <AppHeader />

    <v-container fluid class="login-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" md="5" class="intro-section d-none d-md-flex">
          <div class="intro-content">
            <h2 class="section-title">
              我们想和您一起，争取如下权利
            </h2>

            <div class="rights-carousel">
              <transition name="slide-fade" mode="out-in">
                <div class="right-item" :key="currentRightIndex">
                  <span class="right-number">{{ currentRightIndex + 1 }}.</span>
                  <span class="right-text">{{ rights[currentRightIndex] }}</span>
                </div>
              </transition>
              <div class="carousel-dots">
                <span
                  v-for="(_, index) in rights"
                  :key="index"
                  class="dot"
                  :class="{ active: index === currentRightIndex }"
                  @click="currentRightIndex = index"
                ></span>
              </div>
            </div>

            <h2 class="section-title">
              MaxTwice 的含义
            </h2>

            <p class="mission-text">
              我们想和您一起，努力做到：<span class="mission-quote">"这里的任何一个知识节点，都能只看两遍就懂"</span>
            </p>
          </div>
        </v-col>

        <!-- Right side - Login Form -->
        <v-col cols="12" md="5" sm="8" class="d-flex justify-center">
          <div class="login-card-wrapper">
            <v-card class="login-card" border rounded="xl">
              <!-- Logo Section -->
              <v-card-text class="text-center pa-8">
                <div class="logo-circle">
                  <v-icon size="48" color="primary">mdi-reddit</v-icon>
                </div>
                <h2 class="text-h5 font-weight-bold mt-4 mb-2">登录 MaxTwice</h2>
                <p class="text-body-2 text-medium-emphasis">欢迎回来</p>
              </v-card-text>

              <!-- Login Form -->
              <v-card-text class="px-8 pb-8">
                <v-form @submit.prevent="handleLogin">
                  <v-text-field
                    v-model="username"
                    label="用户名"
                    variant="outlined"
                    density="comfortable"
                    :rules="[rules.required]"
                    hide-details="auto"
                    class="mb-4"
                  />

                  <v-text-field
                    v-model="password"
                    label="密码"
                    type="password"
                    variant="outlined"
                    density="comfortable"
                    :rules="[rules.required]"
                    hide-details="auto"
                    class="mb-2"
                  />

                  <div class="text-right mb-6">
                    <a href="#" class="text-caption text-primary text-decoration-none">
                      忘记密码？
                    </a>
                  </div>

                  <v-btn
                    type="submit"
                    block
                    size="large"
                    color="primary"
                    class="text-none font-weight-bold mb-3"
                  >
                    登录
                  </v-btn>

                  <p class="text-caption text-center text-medium-emphasis mb-4">
                    继续操作即表示你同意我们的
                    <a href="#" class="text-primary text-decoration-none">用户协议</a>
                    并确认已了解
                    <a href="#" class="text-primary text-decoration-none">隐私政策</a>。
                  </p>

                  <v-divider class="my-6" />

                  <div class="text-center">
                    <span class="text-body-2 text-medium-emphasis">
                      还没有账号？
                    </span>
                    <a href="#" @click.prevent="$router.push('/register')" class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1">
                      立即注册
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
import { ref, onMounted, onUnmounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'

const username = ref('')
const password = ref('')
const currentRightIndex = ref(0)
let intervalId: number | null = null

const rights = [
  '没有考上好的大学仍有学习知识的权利',
  '不被学历和出身定义，用能力证明自己的权利',
  '自由探索兴趣，不受传统教育束缚的权利',
  '向任何人学习，不论其背景和资历的权利',
  '失败后重新开始，不被过去错误限制的权利',
  '质疑权威，独立思考和表达观点的权利',
  '按照自己的节奏学习，不被标准化考试评判的权利',
  '获得真实反馈，在实践中成长的权利',
  '选择非传统路径，追求热爱而非功利的权利',
  '相信终身学习，永远不晚的权利'
]

const rules = {
  required: (value: string) => !!value || '此字段为必填项'
}

const handleLogin = () => {
  if (username.value && password.value) {
    console.log('Login attempt:', { username: username.value, password: '***' })
  }
}

onMounted(() => {
  intervalId = window.setInterval(() => {
    currentRightIndex.value = (currentRightIndex.value + 1) % rights.length
  }, 3000)
})

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId)
  }
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  background-color: #FAFBFC;
  position: relative;
  overflow: hidden;
}

.login-page::before {
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

.login-page::after {
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

.login-container {
  height: calc(100vh - 56px);
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

.intro-content {
  max-width: 560px;
  margin: 0 auto;
}

/* Section Title */
.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 24px;
}

/* Rights Carousel */
.rights-carousel {
  margin-bottom: 56px;
  min-height: 120px;
}

.right-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  line-height: 1.7;
  padding: 20px 0;
}

.right-number {
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
  flex-shrink: 0;
  min-width: 28px;
  font-size: 1.1rem;
}

.right-text {
  color: rgb(var(--v-theme-on-surface));
  font-size: 1.05rem;
}

/* Carousel Dots */
.carousel-dots {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 24px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: rgba(var(--v-theme-on-surface), 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.4);
}

.dot.active {
  width: 24px;
  border-radius: 4px;
  background-color: rgb(var(--v-theme-primary));
}

/* Slide Fade Transition */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.5s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Mission Text */
.mission-text {
  margin-top: 0;
  color: rgb(var(--v-theme-on-surface));
  font-size: 1rem;
  line-height: 1.8;
  font-weight: 400;
}

/* Mission Quote */
.mission-quote {
  color: rgb(var(--v-theme-primary));
  font-size: 1.5rem;
  font-weight: 600;
  font-family: Georgia, serif;
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

  .feature-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}
</style>
