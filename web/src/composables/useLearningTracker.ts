import { ref, onMounted, onUnmounted } from 'vue'
import { statsApi } from '@/api'

const STORAGE_KEY = 'daily_learning_reported'
const TIME_KEY = 'daily_learning_time'

export function useLearningTracker(options?: {
  timeThreshold?: number
  interactionThreshold?: number
}) {
  const timeThreshold = options?.timeThreshold ?? 5 * 60 * 1000 // 5分钟
  const interactionThreshold = options?.interactionThreshold ?? 3

  const startTime = ref(Date.now())
  const interactions = ref(0)
  const reported = ref(false)

  const getToday = () => new Date().toISOString().split('T')[0]

  const checkReported = () => {
    return localStorage.getItem(STORAGE_KEY) === getToday()
  }

  const markReported = () => {
    localStorage.setItem(STORAGE_KEY, getToday())
    localStorage.removeItem(TIME_KEY)
    reported.value = true
  }

  // 获取累计时间
  const getAccumulatedTime = (): number => {
    const stored = localStorage.getItem(TIME_KEY)
    if (!stored) return 0
    const { date, time } = JSON.parse(stored)
    return date === getToday() ? time : 0
  }

  // 保存累计时间
  const saveAccumulatedTime = () => {
    const sessionTime = Date.now() - startTime.value
    const total = getAccumulatedTime() + sessionTime
    localStorage.setItem(TIME_KEY, JSON.stringify({ date: getToday(), time: total }))
  }

  const tryReport = async () => {
    if (reported.value || checkReported()) return

    const totalTime = getAccumulatedTime() + (Date.now() - startTime.value)
    if (totalTime >= timeThreshold && interactions.value >= interactionThreshold) {
      try {
        await statsApi.reportDailyLearning()
        markReported()
      } catch (e) {
        console.error('Failed to report daily learning:', e)
      }
    }
  }

  const handleInteraction = () => {
    if (reported.value || checkReported()) return
    interactions.value++
    tryReport()
  }

  onMounted(() => {
    if (checkReported()) {
      reported.value = true
      return
    }
    startTime.value = Date.now()
    window.addEventListener('scroll', handleInteraction)
    window.addEventListener('click', handleInteraction)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleInteraction)
    window.removeEventListener('click', handleInteraction)
    saveAccumulatedTime()
    tryReport()
  })

  return { handleInteraction, reported }
}
