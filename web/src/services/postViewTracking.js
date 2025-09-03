/**
 * Post瀑布流浏览量统计服务
 *
 * 功能说明：
 * - 使用Intersection Observer API检测post的可见性
 * - 基于停留时间判断是否为有效浏览
 * - 批量提交优化性能
 * - 防重复统计机制
 *
 * @author Claude
 * @since 2024-08-24
 */

import { learnService } from './learnService'
import { useUserStore } from '@/stores/user'

class PostViewTracker {
  constructor() {
    // 配置参数
    this.config = {
      visibilityThreshold: 0.5, // post需要露出50%才算进入可视区域
      minViewTime: 2500, // 有效浏览的最小停留时间（毫秒）
      batchSize: 8, // 批量提交的最大数量
      batchInterval: 4000, // 批量提交的时间间隔（毫秒）
      duplicateWindow: 45000, // 防重复统计的时间窗口（毫秒）
      rootMargin: '0px 0px -5% 0px', // 可视区域的根边距
      debug: false, // 是否启用调试日志
    }

    // 核心状态
    this.observer = null
    this.viewStartTimes = new Map() // postId -> 开始浏览时间
    this.recentViews = new Set() // 最近浏览过的postId集合（防重复）
    this.pendingViews = [] // 待提交的浏览记录队列
    this.observedElements = new Map() // postId -> DOM元素映射

    // 定时器
    this.batchTimer = null
    this.cleanupTimer = null

    // 初始化
    this.initializeObserver()
    this.startCleanupTimer()
    this.setupPageUnloadHandler()
  }

  /**
   * 初始化Intersection Observer
   */
  initializeObserver() {
    if (!('IntersectionObserver' in window)) {
      console.warn('[PostViewTracker] 浏览器不支持IntersectionObserver API')
      return
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const { postId } = entry.target.dataset

          if (!postId) {
            this.log('元素缺少postId属性', entry.target)
            return
          }

          if (entry.isIntersecting) {
            // post进入可视区域
            this.handlePostEnterView(postId)
          } else {
            // post离开可视区域
            this.handlePostLeaveView(postId)
          }
        })
      },
      {
        threshold: this.config.visibilityThreshold,
        rootMargin: this.config.rootMargin,
      }
    )

    this.log('Intersection Observer 已初始化')
  }

  /**
   * 处理post进入可视区域
   */
  handlePostEnterView(postId) {
    // 如果已经在跟踪中，跳过
    if (this.viewStartTimes.has(postId)) {
      return
    }

    // 如果最近已经统计过，跳过
    if (this.recentViews.has(postId)) {
      this.log('Post最近已统计过，跳过', { postId })
      return
    }

    // 记录开始浏览时间
    const startTime = Date.now()
    this.viewStartTimes.set(postId, startTime)

    this.log('Post进入可视区域，开始跟踪', {
      postId,
      startTime: new Date(startTime).toISOString(),
    })
  }

  /**
   * 处理post离开可视区域
   */
  handlePostLeaveView(postId) {
    const startTime = this.viewStartTimes.get(postId)

    if (!startTime) {
      return
    }

    // 计算在可视区域的停留时间
    const viewDuration = Date.now() - startTime

    // 清除开始时间记录
    this.viewStartTimes.delete(postId)

    this.log('Post离开可视区域', {
      postId,
      viewDuration,
      threshold: this.config.minViewTime,
    })

    // 检查是否达到有效浏览的时间阈值
    if (viewDuration >= this.config.minViewTime) {
      this.recordValidView(postId, viewDuration)
    } else {
      this.log('浏览时间不足，不统计', {
        postId,
        viewDuration,
        required: this.config.minViewTime,
      })
    }
  }

  /**
   * 记录一次有效的post浏览
   */
  recordValidView(postId, viewDuration) {
    const numericPostId = parseInt(postId)

    // 创建浏览记录
    const viewRecord = {
      postId: numericPostId,
      viewDuration,
      timestamp: Date.now(),
    }

    // 添加到待提交队列
    this.pendingViews.push(viewRecord)

    // 添加到防重复集合
    this.recentViews.add(postId)

    this.log('记录有效浏览', viewRecord)

    // 触发批量提交检查
    this.checkBatchSubmit()

    // 设置防重复过期时间
    setTimeout(() => {
      this.recentViews.delete(postId)
    }, this.config.duplicateWindow)
  }

  /**
   * 手动记录浏览（用于"查看全部内容"等明确的用户交互）
   * @param {string|number} postId - 文章ID
   * @param {string} trigger - 触发来源，如 'view_full_content', 'manual_click'
   */
  recordManualView(postId, trigger = 'manual_view') {
    const postIdStr = postId.toString()

    // 如果最近已经统计过，跳过
    if (this.recentViews.has(postIdStr)) {
      this.log('Post最近已统计过，跳过手动记录', { postId, trigger })
      return false
    }

    // 记录手动浏览，使用固定的有效浏览时长
    const manualViewDuration = this.config.minViewTime + 1000 // 比最小时长多1秒

    this.log('手动记录浏览', {
      postId,
      trigger,
      viewDuration: manualViewDuration,
    })

    this.recordValidView(postIdStr, manualViewDuration)
    return true
  }

  /**
   * 检查是否需要触发批量提交
   */
  checkBatchSubmit() {
    // 如果达到批量大小，立即提交
    if (this.pendingViews.length >= this.config.batchSize) {
      this.log('达到批量大小，立即提交', {
        pendingCount: this.pendingViews.length,
      })
      this.submitBatch()
      return
    }

    // 否则设置延时提交
    if (this.batchTimer) {
      clearTimeout(this.batchTimer)
    }

    this.batchTimer = setTimeout(() => {
      if (this.pendingViews.length > 0) {
        this.log('延时提交触发', {
          pendingCount: this.pendingViews.length,
        })
        this.submitBatch()
      }
    }, this.config.batchInterval)
  }

  /**
   * 批量提交浏览记录到后端
   */
  async submitBatch() {
    if (this.pendingViews.length === 0) {
      return
    }

    // 清除批量提交定时器
    if (this.batchTimer) {
      clearTimeout(this.batchTimer)
      this.batchTimer = null
    }

    // 取出所有待提交的记录
    const viewsToSubmit = this.pendingViews.splice(0)

    this.log('开始批量提交', {
      count: viewsToSubmit.length,
      views: viewsToSubmit.map((v) => ({ postId: v.postId, duration: v.viewDuration })),
    })

    // 获取当前用户ID
    const userStore = useUserStore()
    const userId = userStore.userId || null

    // 并发提交所有浏览记录
    const submitPromises = viewsToSubmit.map(async (viewRecord) => {
      try {
        // 调用后端API记录浏览（articleId参数传入postId）
        const response = await learnService.recordView(
          viewRecord.postId, // articleId参数传入postId
          userId, // 用户ID
          null // IP地址由后端获取
        )

        this.log('单个浏览记录提交成功', {
          postId: viewRecord.postId,
          response: response?.data,
        })

        return { success: true, postId: viewRecord.postId }
      } catch (error) {
        this.log('单个浏览记录提交失败', {
          postId: viewRecord.postId,
          error: error.message,
        })

        return { success: false, postId: viewRecord.postId, error }
      }
    })

    try {
      const results = await Promise.allSettled(submitPromises)

      const successCount = results.filter(
        (result) => result.status === 'fulfilled' && result.value.success
      ).length

      const failCount = viewsToSubmit.length - successCount

      this.log('批量提交完成', {
        total: viewsToSubmit.length,
        success: successCount,
        failed: failCount,
      })

      if (failCount > 0) {
        console.warn(`[PostViewTracker] ${failCount} 个浏览记录提交失败`)
      }
    } catch (error) {
      this.log('批量提交过程出错', { error: error.message })
    }
  }

  /**
   * 开始观察指定的post元素
   */
  observePost(element, postId) {
    if (!this.observer) {
      this.log('Observer未初始化，跳过观察', { postId })
      return
    }

    if (!element || !postId) {
      this.log('无效的元素或postId', { element, postId })
      return
    }

    // 设置元素的data属性
    element.dataset.postId = postId.toString()

    // 开始观察
    this.observer.observe(element)

    // 记录映射关系
    this.observedElements.set(postId.toString(), element)

    this.log('开始观察Post', { postId })
  }

  /**
   * 停止观察指定的post元素
   */
  unobservePost(postId) {
    if (!this.observer) {
      return
    }

    const element = this.observedElements.get(postId.toString())

    if (element) {
      this.observer.unobserve(element)
      this.observedElements.delete(postId.toString())

      // 如果正在跟踪，停止跟踪
      if (this.viewStartTimes.has(postId)) {
        this.handlePostLeaveView(postId)
      }

      this.log('停止观察Post', { postId })
    }
  }

  /**
   * 自动扫描并观察页面上的post元素
   */
  autoObservePosts(container = document) {
    // 查找所有带有data-post-id属性的元素
    const postElements = container.querySelectorAll('[data-post-id]')

    postElements.forEach((element) => {
      const { postId } = element.dataset
      if (postId && !this.observedElements.has(postId)) {
        this.observePost(element, postId)
      }
    })

    this.log('自动扫描并观察Posts', {
      found: postElements.length,
    })
  }

  /**
   * 立即提交所有待处理的浏览记录
   */
  flush() {
    // 停止所有正在跟踪的post
    for (const [postId] of this.viewStartTimes) {
      this.handlePostLeaveView(postId)
    }

    // 立即提交所有待处理记录
    if (this.pendingViews.length > 0) {
      this.log('强制提交剩余记录', { count: this.pendingViews.length })
      this.submitBatch()
    }
  }

  /**
   * 启动定期清理定时器
   */
  startCleanupTimer() {
    this.cleanupTimer = setInterval(
      () => {
        this.performCleanup()
      },
      5 * 60 * 1000
    ) // 每5分钟清理一次
  }

  /**
   * 执行清理操作
   */
  performCleanup() {
    const now = Date.now()
    let cleanedCount = 0

    // 清理超时的跟踪记录（超过10分钟的）
    for (const [postId, startTime] of this.viewStartTimes.entries()) {
      if (now - startTime > 10 * 60 * 1000) {
        this.viewStartTimes.delete(postId)
        cleanedCount++
      }
    }

    if (cleanedCount > 0) {
      this.log('定期清理完成', { cleanedTrackingRecords: cleanedCount })
    }
  }

  /**
   * 设置页面卸载处理
   */
  setupPageUnloadHandler() {
    const handleUnload = () => {
      this.log('页面卸载，提交剩余数据')
      this.flushSync()
    }

    window.addEventListener('beforeunload', handleUnload)
    window.addEventListener('pagehide', handleUnload)

    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.log('页面变为不可见，停止跟踪')
        this.flush()
      }
    })
  }

  /**
   * 同步方式提交剩余数据
   */
  flushSync() {
    // 停止所有正在跟踪的post
    for (const [postId] of this.viewStartTimes) {
      this.handlePostLeaveView(postId)
    }

    if (this.pendingViews.length === 0) {
      return
    }

    // 使用sendBeacon API进行可靠的数据提交
    if (navigator.sendBeacon) {
      const userStore = useUserStore()
      const userId = userStore.userId || null

      this.pendingViews.forEach((viewRecord) => {
        try {
          const params = new URLSearchParams({
            articleId: viewRecord.postId.toString(),
            userId: userId || '',
          })

          navigator.sendBeacon('/api/stats/view', params)
          this.log('Beacon发送成功', { postId: viewRecord.postId })
        } catch (error) {
          this.log('Beacon发送异常', {
            postId: viewRecord.postId,
            error: error.message,
          })
        }
      })

      this.log('同步提交完成', { count: this.pendingViews.length })
    }

    this.pendingViews = []
  }

  /**
   * 销毁跟踪器
   */
  destroy() {
    this.log('开始销毁PostViewTracker')

    this.flush()

    if (this.observer) {
      this.observer.disconnect()
      this.observer = null
    }

    if (this.batchTimer) {
      clearTimeout(this.batchTimer)
      this.batchTimer = null
    }

    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer)
      this.cleanupTimer = null
    }

    this.viewStartTimes.clear()
    this.recentViews.clear()
    this.pendingViews = []
    this.observedElements.clear()

    this.log('PostViewTracker销毁完成')
  }

  /**
   * 更新配置
   */
  updateConfig(newConfig) {
    this.config = { ...this.config, ...newConfig }
    this.log('配置已更新', this.config)
  }

  /**
   * 获取状态信息
   */
  getStatus() {
    return {
      config: { ...this.config },
      isObserverActive: Boolean(this.observer),
      currentlyTracking: this.viewStartTimes.size,
      pendingSubmissions: this.pendingViews.length,
      recentViewsCount: this.recentViews.size,
      observedPostsCount: this.observedElements.size,
    }
  }

  /**
   * 输出调试日志
   */
  log(message, data = null) {
    if (this.config.debug) {
      const timestamp = new Date().toISOString()
      if (data) {
        console.log(`[PostViewTracker ${timestamp}] ${message}`, data)
      } else {
        console.log(`[PostViewTracker ${timestamp}] ${message}`)
      }
    }
  }
}

// 全局PostViewTracker实例
let globalTracker = null

/**
 * 获取全局PostViewTracker实例
 */
export const getPostViewTracker = function () {
  if (!globalTracker) {
    globalTracker = new PostViewTracker()

    // 在开发环境启用调试日志
    if (import.meta.env.DEV) {
      globalTracker.updateConfig({ debug: true })
    }
  }

  return globalTracker
}

/**
 * 便捷的post浏览跟踪API
 */
export const postViewTracking = {
  /**
   * 开始观察post元素
   */
  observe(element, postId) {
    const tracker = getPostViewTracker()
    tracker.observePost(element, postId)
  },

  /**
   * 停止观察post元素
   */
  unobserve(postId) {
    const tracker = getPostViewTracker()
    tracker.unobservePost(postId)
  },

  /**
   * 自动扫描并观察页面上的post元素
   */
  autoObserve(container) {
    const tracker = getPostViewTracker()
    tracker.autoObservePosts(container)
  },

  /**
   * 手动记录浏览（用于"查看全部内容"按钮等明确的用户交互）
   * @param {string|number} postId - 文章ID
   * @param {string} trigger - 触发来源，如 'view_full_content'
   * @returns {boolean} 是否成功记录
   */
  recordManualView(postId, trigger) {
    const tracker = getPostViewTracker()
    return tracker.recordManualView(postId, trigger)
  },

  /**
   * 立即提交所有待处理的浏览记录
   */
  flush() {
    const tracker = getPostViewTracker()
    tracker.flush()
  },

  /**
   * 获取跟踪器状态
   */
  getStatus() {
    const tracker = getPostViewTracker()
    return tracker.getStatus()
  },

  /**
   * 更新跟踪器配置
   */
  configure(config) {
    const tracker = getPostViewTracker()
    tracker.updateConfig(config)
  },

  /**
   * 销毁跟踪器
   */
  destroy() {
    if (globalTracker) {
      globalTracker.destroy()
      globalTracker = null
    }
  },
}

export default postViewTracking
