import { ref, onMounted, onUnmounted } from 'vue'
// import { learnService } from '@/services/learnService' // TODO: 等待V1 API支持学习时间同步

// 活跃状态检测的事件列表
const ACTIVITY_EVENTS = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click']
const INACTIVE_THRESHOLD = 3 * 60 * 1000 // 3分钟无活动则暂停计时
const SYNC_INTERVAL = 5 * 60 * 1000 // 5分钟同步一次
const LOCAL_STORAGE_KEY = 'learning_stats'

// 生成UUID
const generateId = () => {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 获取今天的日期字符串
const getTodayString = () => {
  return new Date().toISOString().split('T')[0]
}

// 从localStorage读取数据
const loadLocalData = () => {
  try {
    const data = localStorage.getItem(LOCAL_STORAGE_KEY)
    if (data) {
      return JSON.parse(data)
    }
  } catch (e) {
    console.warn('Failed to load learning stats from localStorage:', e)
  }
  
  return {
    sessions: [],
    dailyStats: {},
    lastSyncTime: null,
    continuousStreak: 0
  }
}

// 保存数据到localStorage
const saveLocalData = (data) => {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data))
  } catch (e) {
    console.warn('Failed to save learning stats to localStorage:', e)
  }
}

export function useStudyTimeTracker() {
  // 响应式状态
  const isTracking = ref(false)
  const isActive = ref(true)
  const currentDuration = ref(0) // 当前会话累计时长（秒）
  const todayTotal = ref(0) // 今日总学习时长（秒）
  
  // 内部状态
  let localData = loadLocalData()
  let lastActivityTime = Date.now()
  let currentSessionStart = null
  let currentSessionId = null
  let lastTickTime = null
  let syncTimer = null
  let activityCheckTimer = null
  
  // 课程和节点信息
  let currentCourseId = null
  let currentNodeId = null
  
  // 活跃状态检测
  const handleActivity = () => {
    lastActivityTime = Date.now()
    if (!isActive.value) {
      isActive.value = true
      startTicking()
    }
  }
  
  // 检查是否不活跃
  const checkInactivity = () => {
    const now = Date.now()
    if (now - lastActivityTime > INACTIVE_THRESHOLD) {
      if (isActive.value) {
        isActive.value = false
        pauseTicking()
      }
    }
  }
  
  // 开始计时
  const startTicking = () => {
    if (!currentSessionStart) {
      currentSessionStart = Date.now()
      currentSessionId = generateId()
    }
    lastTickTime = Date.now()
  }
  
  // 暂停计时
  const pauseTicking = () => {
    if (currentSessionStart && lastTickTime) {
      const sessionDuration = Math.floor((Date.now() - lastTickTime) / 1000)
      currentDuration.value += sessionDuration
      updateDailyStats(sessionDuration)
    }
  }
  
  // 更新每日统计
  const updateDailyStats = (additionalDuration) => {
    const today = getTodayString()
    if (!localData.dailyStats[today]) {
      localData.dailyStats[today] = {
        totalDuration: 0,
        sessionCount: 0,
        coursesStudied: []
      }
    }
    
    localData.dailyStats[today].totalDuration += additionalDuration
    todayTotal.value = localData.dailyStats[today].totalDuration
    
    // 记录学习的课程
    if (currentCourseId && !localData.dailyStats[today].coursesStudied.includes(currentCourseId)) {
      localData.dailyStats[today].coursesStudied.push(currentCourseId)
    }
  }
  
  // 结束当前学习会话
  const endCurrentSession = () => {
    if (!currentSessionStart) return
    
    pauseTicking() // 先暂停以确保时间计算正确
    
    const endTime = Date.now()
    const totalDuration = Math.floor((endTime - currentSessionStart) / 1000)
    
    if (totalDuration > 10) { // 只记录超过10秒的会话
      const session = {
        id: currentSessionId,
        courseId: currentCourseId,
        nodeId: currentNodeId,
        startTime: new Date(currentSessionStart).toISOString(),
        endTime: new Date(endTime).toISOString(),
        duration: totalDuration,
        date: getTodayString(),
        synced: false
      }
      
      localData.sessions.push(session)
      
      // 更新每日统计
      const today = getTodayString()
      if (!localData.dailyStats[today]) {
        localData.dailyStats[today] = {
          totalDuration: 0,
          sessionCount: 0,
          coursesStudied: []
        }
      }
      localData.dailyStats[today].sessionCount++
      
      saveLocalData(localData)
      console.log('Study session ended:', session)
    }
    
    // 重置当前会话
    currentSessionStart = null
    currentSessionId = null
    lastTickTime = null
    currentDuration.value = 0
  }
  
  // 同步数据到后端
  const syncToBackend = async () => {
    const unsyncedSessions = localData.sessions.filter(session => !session.synced)
    
    if (unsyncedSessions.length === 0) return
    
    try {
      // TODO: 等待V1 API支持学习时间同步功能
      // const response = await learnService.syncStudySessions(unsyncedSessions)
      console.log('Study time sync not implemented yet:', unsyncedSessions.length)
      
      // 暂时标记为已同步，避免重复尝试
      unsyncedSessions.forEach(session => {
        session.synced = true
      })
      localData.lastSyncTime = new Date().toISOString()
      saveLocalData(localData)
    } catch (error) {
      console.warn('Failed to sync study sessions:', error)
    }
  }
  
  // 计算连续学习天数
  const calculateContinuousStreak = () => {
    const today = getTodayString()
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    const yesterdayString = yesterday.toISOString().split('T')[0]
    
    // 检查今天是否有学习记录
    const hasStudiedToday = localData.dailyStats[today] && localData.dailyStats[today].totalDuration > 0
    const hasStudiedYesterday = localData.dailyStats[yesterdayString] && localData.dailyStats[yesterdayString].totalDuration > 0
    
    if (hasStudiedToday) {
      if (hasStudiedYesterday || localData.continuousStreak === 0) {
        // 连续学习或者是新开始
        if (localData.continuousStreak === 0) {
          localData.continuousStreak = 1
        }
      }
    }
    
    return localData.continuousStreak
  }
  
  // 开始追踪学习时间
  const startTracking = (courseId, nodeId = null) => {
    if (isTracking.value) {
      endCurrentSession() // 结束之前的会话
    }
    
    currentCourseId = courseId
    currentNodeId = nodeId
    isTracking.value = true
    isActive.value = true
    lastActivityTime = Date.now()
    
    // 开始计时
    startTicking()
    
    // 添加活动监听器
    ACTIVITY_EVENTS.forEach(event => {
      document.addEventListener(event, handleActivity, { passive: true })
    })
    
    // 启动不活跃检查定时器
    activityCheckTimer = setInterval(checkInactivity, 30000) // 每30秒检查一次
    
    // 启动同步定时器
    syncTimer = setInterval(syncToBackend, SYNC_INTERVAL)
    
    // 每秒更新当前时长显示
    const updateTimer = setInterval(() => {
      if (isActive.value && currentSessionStart) {
        const elapsed = Math.floor((Date.now() - currentSessionStart) / 1000)
        currentDuration.value = elapsed
      }
    }, 1000)
    
    // 保存定时器引用以便清理
    if (!window.studyTrackingTimers) {
      window.studyTrackingTimers = []
    }
    window.studyTrackingTimers.push(updateTimer)
    
    console.log('Study time tracking started for course:', courseId, 'node:', nodeId)
  }
  
  // 停止追踪
  const stopTracking = () => {
    if (!isTracking.value) return
    
    endCurrentSession()
    
    // 移除事件监听器
    ACTIVITY_EVENTS.forEach(event => {
      document.removeEventListener(event, handleActivity)
    })
    
    // 清理定时器
    if (activityCheckTimer) {
      clearInterval(activityCheckTimer)
      activityCheckTimer = null
    }
    
    if (syncTimer) {
      clearInterval(syncTimer)
      syncTimer = null
    }
    
    // 清理更新定时器
    if (window.studyTrackingTimers) {
      window.studyTrackingTimers.forEach(timer => clearInterval(timer))
      window.studyTrackingTimers = []
    }
    
    isTracking.value = false
    isActive.value = true
    currentDuration.value = 0
    
    // 最后同步一次
    syncToBackend()
    
    console.log('Study time tracking stopped')
  }
  
  // 获取今日学习统计
  const getTodayStats = () => {
    const today = getTodayString()
    return localData.dailyStats[today] || {
      totalDuration: 0,
      sessionCount: 0,
      coursesStudied: []
    }
  }
  
  // 获取总学习统计
  const getTotalStats = () => {
    let totalDuration = 0
    let totalSessions = 0
    const allCourses = new Set()
    
    Object.values(localData.dailyStats).forEach(dayStats => {
      totalDuration += dayStats.totalDuration
      totalSessions += dayStats.sessionCount
      dayStats.coursesStudied.forEach(courseId => allCourses.add(courseId))
    })
    
    return {
      totalDuration,
      totalSessions,
      coursesStudied: Array.from(allCourses),
      continuousStreak: calculateContinuousStreak()
    }
  }
  
  // 格式化时长显示
  const formatDuration = (seconds) => {
    if (seconds < 60) return `${seconds}秒`
    if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${hours}小时${minutes}分钟`
  }
  
  // 初始化今日统计
  onMounted(() => {
    const todayStats = getTodayStats()
    todayTotal.value = todayStats.totalDuration
  })
  
  // 页面卸载时清理
  onUnmounted(() => {
    stopTracking()
  })
  
  // 监听页面可见性变化
  const handleVisibilityChange = () => {
    if (document.hidden) {
      if (isActive.value) {
        isActive.value = false
        pauseTicking()
      }
    } else {
      isActive.value = true
      lastActivityTime = Date.now()
      if (isTracking.value) {
        startTicking()
      }
    }
  }
  
  onMounted(() => {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })
  
  onUnmounted(() => {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })
  
  return {
    // 响应式状态
    isTracking,
    isActive,
    currentDuration,
    todayTotal,
    
    // 方法
    startTracking,
    stopTracking,
    syncToBackend,
    getTodayStats,
    getTotalStats,
    formatDuration
  }
}