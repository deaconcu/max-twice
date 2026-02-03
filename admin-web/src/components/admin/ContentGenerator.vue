<script setup lang="ts">
import { ref, computed } from 'vue'
import axios from 'axios'

// 虚拟用户接口
interface VirtualUser {
  username: string
  email: string
  password: string
  userId?: number
  loggedIn: boolean
}

const userCount = ref(10)
const virtualUsers = ref<VirtualUser[]>([])
const loading = ref(false)
const createProgress = ref(0)
const loginProgress = ref(0)

// API测试
const selectedMode = ref<'specific' | 'random'>('specific')
const selectedUsername = ref('')
const httpMethod = ref('GET')
const apiEndpoint = ref('/api/v1/users/current')
const requestBody = ref('{}')
const apiResponse = ref('')
const apiLoading = ref(false)

// 生成真实感的用户名
const generateUsername = (): string => {
  const prefixes = ['dev', 'code', 'tech', 'user', 'programmer']
  const suffixes = ['smith', 'john', 'mary', 'wang', 'li', 'zhang']
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)]
  const suffix = suffixes[Math.floor(Math.random() * suffixes.length)]
  const num = Math.floor(Math.random() * 100)
  return `${prefix}_${suffix}${num}`
}

// 生成虚拟用户
const generateUsers = async () => {
  loading.value = true
  createProgress.value = 0
  virtualUsers.value = []

  const existingUsernames = new Set<string>()
  const password = 'GenPass2024!@#'

  for (let i = 0; i < userCount.value; i++) {
    let username = generateUsername()

    // 确保用户名唯一
    while (existingUsernames.has(username)) {
      username = generateUsername()
    }
    existingUsernames.add(username)

    const email = `${username}@example.com`

    try {
      const response = await axios.post('/api/v1/admin/users/create-virtual', {
        username,
        email,
        password,
      })

      if (response.data.code === 200) {
        virtualUsers.value.push({
          username,
          email,
          password,
          userId: response.data.data.id,
          loggedIn: false,
        })
      }
    } catch (error: any) {
      console.error(`创建用户失败: ${username}`, error.response?.data?.message)
    }

    createProgress.value = ((i + 1) / userCount.value) * 100
  }

  loading.value = false
}

// 登录所有用户
const loginAllUsers = async () => {
  loading.value = true
  loginProgress.value = 0

  for (let i = 0; i < virtualUsers.value.length; i++) {
    const user = virtualUsers.value[i]

    try {
      const response = await axios.post('/api/v1/auth/login', {
        email: user.email,
        password: user.password,
      })

      if (response.data.code === 200) {
        user.loggedIn = true
      }
    } catch (error) {
      console.error(`登录失败: ${user.username}`)
    }

    loginProgress.value = ((i + 1) / virtualUsers.value.length) * 100
  }

  loading.value = false
}

// 清空用户列表
const clearUsers = () => {
  virtualUsers.value = []
}

// 测试API
const testAPI = async () => {
  apiLoading.value = true
  apiResponse.value = ''

  try {
    // 选择用户
    let targetUser: VirtualUser | undefined
    if (selectedMode.value === 'specific') {
      targetUser = virtualUsers.value.find((u) => u.username === selectedUsername.value)
    } else {
      const loggedInUsers = virtualUsers.value.filter((u) => u.loggedIn)
      if (loggedInUsers.length > 0) {
        targetUser = loggedInUsers[Math.floor(Math.random() * loggedInUsers.length)]
      }
    }

    if (!targetUser || !targetUser.loggedIn) {
      apiResponse.value = JSON.stringify({ error: '请先选择已登录的用户' }, null, 2)
      apiLoading.value = false
      return
    }

    // 解析请求体
    let data = null
    if (httpMethod.value === 'POST' || httpMethod.value === 'PUT') {
      try {
        data = JSON.parse(requestBody.value)
      } catch {
        apiResponse.value = JSON.stringify({ error: 'JSON格式错误' }, null, 2)
        apiLoading.value = false
        return
      }
    }

    // 调用API
    let response
    if (httpMethod.value === 'GET') {
      response = await axios.get(apiEndpoint.value)
    } else if (httpMethod.value === 'POST') {
      response = await axios.post(apiEndpoint.value, data)
    } else if (httpMethod.value === 'PUT') {
      response = await axios.put(apiEndpoint.value, data)
    } else if (httpMethod.value === 'DELETE') {
      response = await axios.delete(apiEndpoint.value)
    }

    apiResponse.value = JSON.stringify(
      {
        user: targetUser.username,
        status: response?.status,
        data: response?.data,
      },
      null,
      2
    )
  } catch (error: any) {
    apiResponse.value = JSON.stringify(
      {
        error: error.response?.data || error.message,
      },
      null,
      2
    )
  } finally {
    apiLoading.value = false
  }
}

// 已登录用户列表
const loggedInUsers = computed(() => virtualUsers.value.filter((u) => u.loggedIn))
</script>

<template>
  <v-card flat>
    <v-card-title class="text-h5 font-weight-bold">内容生成工具</v-card-title>
    <v-card-subtitle>虚拟用户管理与内容生成</v-card-subtitle>

    <v-card-text>
      <!-- 1. 创建虚拟用户 -->
      <v-row>
        <v-col cols="12">
          <div class="text-h6 mb-4">1. 创建虚拟用户</div>

          <v-slider
            v-model="userCount"
            :min="1"
            :max="50"
            :step="1"
            label="用户数量"
            thumb-label
            class="mb-4"
          ></v-slider>

          <v-btn color="primary" @click="generateUsers" :loading="loading" size="large">
            生成并创建虚拟用户
          </v-btn>

          <v-progress-linear
            v-if="loading && createProgress > 0"
            :model-value="createProgress"
            class="mt-4"
            color="primary"
            height="6"
          ></v-progress-linear>
        </v-col>
      </v-row>

      <v-divider class="my-6"></v-divider>

      <!-- 2. 用户列表 -->
      <v-row>
        <v-col cols="12">
          <div class="d-flex justify-space-between align-center mb-4">
            <div class="text-h6">2. 用户列表 ({{ virtualUsers.length }})</div>

            <div>
              <v-btn
                color="success"
                @click="loginAllUsers"
                :disabled="virtualUsers.length === 0"
                class="mr-2"
              >
                登录所有用户
              </v-btn>

              <v-btn color="error" @click="clearUsers" :disabled="virtualUsers.length === 0">
                清空列表
              </v-btn>
            </div>
          </div>

          <v-progress-linear
            v-if="loading && loginProgress > 0"
            :model-value="loginProgress"
            class="mb-4"
            color="success"
            height="6"
          ></v-progress-linear>

          <v-table v-if="virtualUsers.length > 0">
            <thead>
              <tr>
                <th class="text-left">#</th>
                <th class="text-left">用户名</th>
                <th class="text-left">邮箱</th>
                <th class="text-left">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, index) in virtualUsers" :key="user.username">
                <td>{{ index + 1 }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.email }}</td>
                <td>
                  <v-chip :color="user.loggedIn ? 'success' : 'error'" size="small" label>
                    {{ user.loggedIn ? '已登录' : '未登录' }}
                  </v-chip>
                </td>
              </tr>
            </tbody>
          </v-table>

          <v-alert v-else type="info" variant="tonal" class="mt-4">
            暂无虚拟用户，请先创建
          </v-alert>
        </v-col>
      </v-row>

      <v-divider class="my-6"></v-divider>

      <!-- 3. API测试 -->
      <v-row>
        <v-col cols="12">
          <div class="text-h6 mb-4">3. API测试</div>

          <v-alert
            v-if="loggedInUsers.length === 0"
            type="warning"
            variant="tonal"
            class="mb-4"
          >
            请先创建并登录虚拟用户
          </v-alert>

          <template v-else>
            <v-row>
              <v-col cols="12" md="6">
                <v-radio-group v-model="selectedMode" inline label="用户模式">
                  <v-radio label="指定用户" value="specific"></v-radio>
                  <v-radio label="随机用户" value="random"></v-radio>
                </v-radio-group>

                <v-select
                  v-if="selectedMode === 'specific'"
                  v-model="selectedUsername"
                  :items="loggedInUsers.map((u) => u.username)"
                  label="选择用户"
                  variant="outlined"
                  density="comfortable"
                ></v-select>

                <v-select
                  v-model="httpMethod"
                  :items="['GET', 'POST', 'PUT', 'DELETE']"
                  label="HTTP方法"
                  variant="outlined"
                  density="comfortable"
                ></v-select>

                <v-text-field
                  v-model="apiEndpoint"
                  label="API端点"
                  variant="outlined"
                  density="comfortable"
                  placeholder="/api/v1/users/current"
                ></v-text-field>

                <v-textarea
                  v-if="httpMethod === 'POST' || httpMethod === 'PUT'"
                  v-model="requestBody"
                  label="请求体 (JSON)"
                  variant="outlined"
                  rows="4"
                  placeholder='{"key": "value"}'
                ></v-textarea>

                <v-btn color="primary" @click="testAPI" :loading="apiLoading" block size="large">
                  发送请求
                </v-btn>
              </v-col>

              <v-col cols="12" md="6">
                <div class="text-subtitle-1 mb-2">响应结果</div>

                <v-card v-if="apiResponse" variant="outlined" class="response-card">
                  <v-card-text>
                    <pre class="response-pre">{{ apiResponse }}</pre>
                  </v-card-text>
                </v-card>

                <v-alert v-else type="info" variant="tonal"> 尚未发送请求 </v-alert>
              </v-col>
            </v-row>
          </template>
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.response-card {
  max-height: 500px;
  overflow-y: auto;
}

.response-pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}
</style>
