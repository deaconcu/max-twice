<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useSystemConfigStore } from '@/stores/modules/systemConfig'

const router = useRouter()
const systemConfigStore = useSystemConfigStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

const login = async () => {
  if (!email.value || !password.value) {
    errorMsg.value = '请输入邮箱和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const response = await axios.post('/api/v1/auth/login', {
      email: email.value,
      password: password.value,
    })

    if (response.data.code === 200) {
      // 登录成功，加载系统配置
      await systemConfigStore.init()
      // 跳转到Dashboard
      router.push('/')
    } else {
      errorMsg.value = response.data.message || '登录失败'
    }
  } catch (error: any) {
    errorMsg.value = error.response?.data?.message || '登录失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-app>
    <div class="login-container">
      <div class="login-box">
        <!-- Logo 和标题 -->
        <div class="login-header">
          <div class="logo">
            <v-icon icon="mdi-cog" size="48"></v-icon>
          </div>
          <h1 class="text-h5 font-weight-bold mt-4 mb-2">MaxTwice</h1>
          <p class="text-body-2 text-medium-emphasis">管理后台</p>
        </div>

        <!-- 登录表单 -->
        <v-form @submit.prevent="login" class="login-form">
          <v-text-field
            v-model="email"
            label="邮箱"
            type="email"
            variant="outlined"
            density="comfortable"
            placeholder="请输入邮箱"
            :error-messages="errorMsg"
            hide-details="auto"
          ></v-text-field>

          <v-text-field
            v-model="password"
            label="密码"
            type="password"
            variant="outlined"
            density="comfortable"
            placeholder="请输入密码"
            class="mt-4"
            hide-details
          ></v-text-field>

          <v-btn
            type="submit"
            variant="flat"
            block
            size="large"
            class="mt-6"
            :loading="loading"
          >
            登录
          </v-btn>
        </v-form>
      </div>
    </div>
  </v-app>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.login-box {
  width: 100%;
  max-width: 400px;
  padding: 48px 32px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background-color: #f5f5f5;
  border-radius: 50%;
}

.login-form {
  margin-top: 24px;
}
</style>

