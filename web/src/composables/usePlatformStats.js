import { onMounted, ref } from 'vue'
import { statsServiceV1 } from '@/services/api/v1/apiServiceV1'

// 缓存键和过期时间
const CACHE_KEY = 'platform_stats'
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟

// 全局缓存状态，在多个组件间共享
const globalStats = ref(null)
const isLoading = ref(false)
const error = ref(null)
const lastFetched = ref(null)

// 数字格式化函数
const formatNumber = (num) => {
  // 处理null、undefined或非数字类型
  if (num === null || isNaN(num)) {
    return '--'
  }

  // 确保是数字类型
  const number = Number(num)

  if (number >= 10000) {
    return `${(number / 1000).toFixed(1)}k`
  }
  return number.toLocaleString()
}

// 从localStorage获取缓存数据
const getCachedData = () => {
  try {
    const cached = localStorage.getItem(CACHE_KEY)
    if (cached) {
      const { data, timestamp } = JSON.parse(cached)
      const now = Date.now()

      // 检查缓存是否过期
      if (now - timestamp < CACHE_DURATION) {
        return data
      }
    }
  } catch (e) {
    console.warn('Failed to read platform stats cache:', e)
  }
  return null
}

// 缓存数据到localStorage
const setCachedData = (data) => {
  try {
    const cacheData = {
      data,
      timestamp: Date.now(),
    }
    localStorage.setItem(CACHE_KEY, JSON.stringify(cacheData))
  } catch (e) {
    console.warn('Failed to cache platform stats:', e)
  }
}

// 获取平台统计数据
const fetchPlatformStats = async (force = false) => {
  // 如果正在加载中，避免重复请求
  if (isLoading.value && !force) {
    return globalStats.value
  }

  // 检查缓存
  if (!force) {
    const cached = getCachedData()
    if (cached) {
      globalStats.value = cached
      lastFetched.value = Date.now()
      error.value = null // 清除之前的错误状态
      return cached
    }
  }

  isLoading.value = true
  error.value = null

  try {
    const response = await statsServiceV1.getPlatformStats()

    if (response.code === 200) {
      const stats = response.data

      // 格式化数据
      const formattedStats = {
        courseCount: formatNumber(stats.courseCount),
        careerPathCount: formatNumber(stats.careerPathCount),
        roadmapCount: formatNumber(stats.roadmapCount),
        knowledgeNodeCount: formatNumber(stats.knowledgeNodeCount),
        articleCount: formatNumber(stats.articleCount),
        rawData: stats,
      }

      globalStats.value = formattedStats
      lastFetched.value = Date.now()
      error.value = null // 清除错误状态

      // 缓存数据
      setCachedData(formattedStats)

      return formattedStats
    } else {
      // 后端返回错误状态码时，不抛出异常，直接设置默认数据
      console.warn('Platform stats API returned error:', response.msg || '获取平台数据失败')
      setDefaultStats()
      return globalStats.value
    }
  } catch (err) {
    // 网络错误或其他异常时，不抛出异常，设置默认数据
    console.warn('Error fetching platform stats:', err)

    // 如果有缓存数据，在错误时仍然使用缓存
    const cached = getCachedData()
    if (cached) {
      globalStats.value = cached
      error.value = null // 不显示错误状态
      return cached
    }

    // 没有缓存时设置默认数据
    setDefaultStats()
    return globalStats.value
  } finally {
    isLoading.value = false
  }
}

// 设置默认统计数据（显示暂无数据）
const setDefaultStats = () => {
  globalStats.value = {
    courseCount: '--',
    careerPathCount: '--',
    roadmapCount: '--',
    knowledgeNodeCount: '--',
    articleCount: '--',
    rawData: null,
  }
  error.value = null // 不显示错误状态，让UI显示默认数据
}

// 主要的 composable 函数
export const usePlatformStats = () => {
  // 组件挂载时获取数据
  onMounted(() => {
    fetchPlatformStats()
  })

  return {
    stats: globalStats,
    isLoading,
    error,
    fetchPlatformStats,
    formatNumber,
  }
}
