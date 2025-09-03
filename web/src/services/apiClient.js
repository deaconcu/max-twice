import axios from 'axios'

axios.defaults.withCredentials = true

const apiClient = axios.create({
  baseURL: 'http://localhost:9202/',
  timeout: 60000, // 超时时间
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 在发送请求前可以添加 Authorization Token 等
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default apiClient
