<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

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
      // 登录成功，跳转到Dashboard
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
    <v-container class="fill-height" fluid>
      <v-row align="center" justify="center">
        <v-col cols="12" sm="8" md="4">
          <v-card class="elevation-12">
            <v-card-title class="text-h5 text-center pa-6 bg-primary text-white">
              MaxTwice 管理后台
            </v-card-title>

            <v-card-text class="pa-6">
              <v-form @submit.prevent="login">
                <v-text-field
                  v-model="email"
                  label="邮箱"
                  type="email"
                  prepend-inner-icon="mdi-email"
                  variant="outlined"
                  :error-messages="errorMsg"
                ></v-text-field>

                <v-text-field
                  v-model="password"
                  label="密码"
                  type="password"
                  prepend-inner-icon="mdi-lock"
                  variant="outlined"
                  class="mt-4"
                ></v-text-field>

                <v-btn
                  type="submit"
                  color="primary"
                  block
                  size="large"
                  class="mt-6"
                  :loading="loading"
                >
                  登录
                </v-btn>
              </v-form>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<style scoped>
.fill-height {
  min-height: 100vh;
}
</style>
