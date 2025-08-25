/**
 * Post浏览量跟踪测试工具
 * 
 * 用于在开发环境下测试和调试Post浏览量统计功能
 * 可以通过浏览器控制台使用：window.PostViewTrackingDebug
 * 
 * @author Claude
 * @since 2024-08-24
 */

import postViewTracking from './postViewTracking'

class PostViewTrackingDebug {
  constructor() {
    this.isEnabled = import.meta.env.DEV
    
    if (this.isEnabled) {
      // 在开发环境下将调试工具挂载到window对象
      window.PostViewTrackingDebug = this
      console.log('[Debug] Post浏览量跟踪调试工具已加载')
      console.log('[Debug] 使用方法: window.PostViewTrackingDebug.help()')
    }
  }

  /**
   * 显示帮助信息
   */
  help() {
    console.log(`
🔍 Post浏览量跟踪调试工具使用指南

📊 状态查询:
  - status()           获取当前跟踪状态
  - listObservedPosts() 列出所有正在观察的posts
  - listTrackingPosts() 列出正在跟踪时间的posts
  - pendingViews()      查看待提交的浏览记录

⚙️ 配置管理:
  - getConfig()         查看当前配置
  - enableDebug()       启用调试日志
  - disableDebug()      禁用调试日志
  - setViewTime(ms)     设置有效浏览时间阈值

🧪 测试功能:
  - simulateView(postId) 模拟浏览指定post
  - flush()             立即提交所有待处理记录
  - rescan()            重新扫描页面上的posts

📱 监控功能:
  - startMonitoring()   开始实时监控
  - stopMonitoring()    停止实时监控
    `)
  }

  /**
   * 获取当前跟踪状态
   */
  status() {
    const status = postViewTracking.getStatus()
    
    console.log('📊 Post浏览量跟踪状态:')
    console.table({
      '观察器状态': status.isObserverActive ? '✅ 活跃' : '❌ 未激活',
      '观察的Posts数量': status.observedPostsCount,
      '正在跟踪的Posts': status.currentlyTracking,
      '待提交记录数': status.pendingSubmissions,
      '防重复记录数': status.recentViewsCount
    })
    
    console.log('🔧 当前配置:')
    console.table({
      '可见性阈值': status.config.visibilityThreshold,
      '最小浏览时间': `${status.config.minViewTime}ms`,
      '批量大小': status.config.batchSize,
      '批量间隔': `${status.config.batchInterval}ms`,
      '防重复窗口': `${status.config.duplicateWindow}ms`,
      '调试模式': status.config.debug ? '✅ 开启' : '❌ 关闭'
    })
    
    return status
  }

  /**
   * 列出所有正在观察的posts
   */
  listObservedPosts() {
    const status = postViewTracking.getStatus()
    
    console.log(`📋 正在观察的Posts (${status.observedPostsCount}个):`)
    
    if (status.observedPosts.length === 0) {
      console.log('  暂无观察的Posts')
      return
    }
    
    status.observedPosts.forEach((postId, index) => {
      const element = document.querySelector(`[data-post-id="${postId}"]`)
      const isVisible = element ? this.isElementVisible(element) : false
      
      console.log(`  ${index + 1}. Post ID: ${postId} ${isVisible ? '👁️ 可见' : '🚫 不可见'}`)
    })
    
    return status.observedPosts
  }

  /**
   * 列出正在跟踪时间的posts
   */
  listTrackingPosts() {
    const status = postViewTracking.getStatus()
    
    console.log(`⏱️ 正在跟踪时间的Posts (${status.currentlyTracking}个):`)
    
    if (status.trackingPosts.length === 0) {
      console.log('  暂无跟踪中的Posts')
      return
    }
    
    const now = Date.now()
    status.trackingPosts.forEach((postId, index) => {
      // 注意：这里无法直接获取开始时间，只能显示postId
      console.log(`  ${index + 1}. Post ID: ${postId} ⏰ 跟踪中...`)
    })
    
    return status.trackingPosts
  }

  /**
   * 查看待提交的浏览记录
   */
  pendingViews() {
    const status = postViewTracking.getStatus()
    
    console.log(`📤 待提交的浏览记录 (${status.pendingSubmissions}个):`)
    
    // 注意：由于封装原因，这里无法直接获取待提交记录的详细内容
    // 只能显示数量
    if (status.pendingSubmissions === 0) {
      console.log('  暂无待提交记录')
    } else {
      console.log(`  有 ${status.pendingSubmissions} 个记录等待提交`)
    }
    
    return status.pendingSubmissions
  }

  /**
   * 获取当前配置
   */
  getConfig() {
    const status = postViewTracking.getStatus()
    console.log('⚙️ 当前配置:', status.config)
    return status.config
  }

  /**
   * 启用调试日志
   */
  enableDebug() {
    postViewTracking.configure({ debug: true })
    console.log('🐛 调试日志已启用')
  }

  /**
   * 禁用调试日志
   */
  disableDebug() {
    postViewTracking.configure({ debug: false })
    console.log('🔇 调试日志已禁用')
  }

  /**
   * 设置有效浏览时间阈值
   */
  setViewTime(milliseconds) {
    if (typeof milliseconds !== 'number' || milliseconds < 0) {
      console.error('❌ 无效的时间值，请输入正数（毫秒）')
      return
    }
    
    postViewTracking.configure({ minViewTime: milliseconds })
    console.log(`⏱️ 有效浏览时间阈值已设置为 ${milliseconds}ms`)
  }

  /**
   * 模拟浏览指定post
   */
  simulateView(postId) {
    if (!postId) {
      console.error('❌ 请提供postId参数')
      return
    }
    
    const element = document.querySelector(`[data-post-id="${postId}"]`)
    
    if (!element) {
      console.error(`❌ 未找到postId为 ${postId} 的元素`)
      return
    }
    
    console.log(`🎯 开始模拟浏览 Post ${postId}`)
    
    // 滚动到元素位置
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    
    console.log(`📍 已滚动到 Post ${postId} 位置`)
    console.log(`⏳ 请等待 ${this.getConfig().minViewTime}ms 以上再滚动离开，以触发有效浏览`)
  }

  /**
   * 立即提交所有待处理记录
   */
  flush() {
    console.log('🚀 立即提交所有待处理的浏览记录...')
    postViewTracking.flush()
    console.log('✅ 提交完成')
  }

  /**
   * 重新扫描页面上的posts
   */
  rescan() {
    console.log('🔄 重新扫描页面上的Posts...')
    postViewTracking.autoObserve()
    
    setTimeout(() => {
      const status = this.status()
      console.log(`✅ 扫描完成，当前观察 ${status.observedPostsCount} 个Posts`)
    }, 100)
  }

  /**
   * 开始实时监控
   */
  startMonitoring() {
    if (this.monitorInterval) {
      console.log('📺 监控已在运行中')
      return
    }
    
    console.log('📺 开始实时监控Post浏览状态...')
    console.log('💡 提示：使用 stopMonitoring() 停止监控')
    
    this.monitorInterval = setInterval(() => {
      const status = postViewTracking.getStatus()
      
      console.clear()
      console.log('📺 Post浏览量跟踪实时监控')
      console.log('=' * 50)
      console.log(`⏰ 时间: ${new Date().toLocaleString()}`)
      console.log(`👁️ 观察中: ${status.observedPostsCount} 个Posts`)
      console.log(`⏱️ 跟踪中: ${status.currentlyTracking} 个Posts`)
      console.log(`📤 待提交: ${status.pendingSubmissions} 个记录`)
      console.log(`🛡️ 防重复: ${status.recentViewsCount} 个记录`)
      console.log('=' * 50)
      
      // 显示当前可见的posts
      this.showVisiblePosts()
      
    }, 1000)
  }

  /**
   * 停止实时监控
   */
  stopMonitoring() {
    if (this.monitorInterval) {
      clearInterval(this.monitorInterval)
      this.monitorInterval = null
      console.log('📺 实时监控已停止')
    } else {
      console.log('📺 监控未在运行')
    }
  }

  /**
   * 显示当前可见的posts
   */
  showVisiblePosts() {
    const visiblePosts = []
    const allPostElements = document.querySelectorAll('[data-post-id]')
    
    allPostElements.forEach(element => {
      if (this.isElementVisible(element)) {
        visiblePosts.push(element.dataset.postId)
      }
    })
    
    if (visiblePosts.length > 0) {
      console.log(`👁️ 当前可见的Posts: ${visiblePosts.join(', ')}`)
    } else {
      console.log('👁️ 当前无可见Posts')
    }
  }

  /**
   * 检查元素是否在可视区域内
   */
  isElementVisible(element) {
    const rect = element.getBoundingClientRect()
    const windowHeight = window.innerHeight || document.documentElement.clientHeight
    const windowWidth = window.innerWidth || document.documentElement.clientWidth
    
    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <= windowHeight &&
      rect.right <= windowWidth
    )
  }

  /**
   * 生成测试报告
   */
  generateTestReport() {
    const status = postViewTracking.getStatus()
    
    const report = {
      timestamp: new Date().toISOString(),
      observerStatus: status.isObserverActive,
      totalPosts: document.querySelectorAll('[data-post-id]').length,
      observedPosts: status.observedPostsCount,
      trackingPosts: status.currentlyTracking,
      pendingSubmissions: status.pendingSubmissions,
      recentViews: status.recentViewsCount,
      config: status.config,
      visiblePosts: this.getVisiblePostIds(),
      browserInfo: {
        userAgent: navigator.userAgent,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight
        },
        supportsIntersectionObserver: 'IntersectionObserver' in window,
        supportsSendBeacon: 'sendBeacon' in navigator
      }
    }
    
    console.log('📋 测试报告:')
    console.log(JSON.stringify(report, null, 2))
    
    return report
  }

  /**
   * 获取当前可见的post IDs
   */
  getVisiblePostIds() {
    const visiblePosts = []
    const allPostElements = document.querySelectorAll('[data-post-id]')
    
    allPostElements.forEach(element => {
      if (this.isElementVisible(element)) {
        visiblePosts.push(element.dataset.postId)
      }
    })
    
    return visiblePosts
  }
}

// 创建调试工具实例
const debugTool = new PostViewTrackingDebug()

export default debugTool