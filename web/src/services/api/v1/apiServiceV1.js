import apiClient from '../../apiClient'

// API v1 统一服务模块，使用 JSON 格式
const API_V1_PREFIX = '/api/v1'

// 用户认证服务
export const authServiceV1 = {
  login(email, password) {
    return apiClient.post(`${API_V1_PREFIX}/auth/login`, {
      email,
      password,
    })
  },

  register(userName, email, password) {
    return apiClient.post(`${API_V1_PREFIX}/auth/register`, {
      userName,
      email,
      password,
    })
  },

  validateEmail(email, code) {
    return apiClient.post(`${API_V1_PREFIX}/auth/validate-email`, {
      email,
      code,
    })
  },
}

// 用户管理服务
export const userServiceV1 = {
  getCurrentUser() {
    return apiClient.get(`${API_V1_PREFIX}/users/current`)
  },

  updateCurrentUser(name, biography) {
    return apiClient.put(`${API_V1_PREFIX}/users/current`, {
      name,
      biography,
    })
  },

  getUser(userId) {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}`)
  },

  searchUser(name) {
    return apiClient.get(`${API_V1_PREFIX}/users/search`, {
      params: { name },
    })
  },

  getUserPosts(userId, lastId, type = 'article') {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/posts`, {
      params: { lastId, type },
    })
  },
}

// 关注服务
export const followServiceV1 = {
  follow(followeeId) {
    return apiClient.post(`${API_V1_PREFIX}/follows`, {
      followeeId,
    })
  },

  unfollow(followeeId) {
    return apiClient.delete(`${API_V1_PREFIX}/follows/${followeeId}`)
  },

  getFollowees(userId, lastCreateTime) {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/followees`, {
      params: { lastCreateTime },
    })
  },
}

// 订阅服务
export const subscriptionServiceV1 = {
  getUserSubscriptions(userId) {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/subscriptions`)
  },

  subscribe(courseId) {
    return apiClient.post(`${API_V1_PREFIX}/users/current/subscriptions`, {
      courseId,
    })
  },

  updateSubscriptions(subscription) {
    return apiClient.put(`${API_V1_PREFIX}/users/current/subscriptions`, {
      subscription,
    })
  },

  unsubscribe(courseId) {
    return apiClient.delete(`${API_V1_PREFIX}/users/current/subscriptions/${courseId}`)
  },
}

// 职业管理服务
export const professionServiceV1 = {
  // 分页获取职业（管理端使用）
  getProfessionsByPage(page = 1) {
    return apiClient.get(`${API_V1_PREFIX}/professions`, {
      params: { page },
    })
  },

  // 按状态获取职业（管理端使用）
  getProfessions(state, lastId) {
    return apiClient.get(`${API_V1_PREFIX}/professions`, {
      params: { state, lastId },
    })
  },

  // 按分类获取职业（前端使用）
  getProfessionsByCategory(lastId, mainCategory, subCategory) {
    return apiClient.get(`${API_V1_PREFIX}/professions`, {
      params: { lastId, mainCategory, subCategory },
    })
  },

  getApprovedProfessions(lastId = 0) {
    return apiClient.get(`${API_V1_PREFIX}/professions/approved`, {
      params: { lastId },
    })
  },

  getProfession(id) {
    return apiClient.get(`${API_V1_PREFIX}/professions/${id}`)
  },

  createProfession(professionData) {
    return apiClient.post(`${API_V1_PREFIX}/professions`, professionData)
  },

  updateProfession(id, professionData) {
    return apiClient.put(`${API_V1_PREFIX}/admin/professions/${id}`, professionData)
  },

  approveProfession(id, action, rejectedReason) {
    return apiClient.post(`${API_V1_PREFIX}/admin/professions/${id}/approve`, {
      action,
      rejectedReason,
    })
  },

  deleteProfession(id) {
    return apiClient.delete(`${API_V1_PREFIX}/professions/${id}`)
  },

  getHotProfessions(limit = 10) {
    return apiClient.get(`${API_V1_PREFIX}/professions/hot`, {
      params: { limit },
    })
  },
}

// 课程管理服务
export const courseServiceV1 = {
  getCourse(id) {
    return apiClient.get(`${API_V1_PREFIX}/courses/${id}`)
  },

  searchCourses(name) {
    return apiClient.get(`${API_V1_PREFIX}/courses/search`, {
      params: { name },
    })
  },

  getCoursesByState(state, lastId) {
    return apiClient.get(`${API_V1_PREFIX}/courses`, {
      params: { state, lastId },
    })
  },

  getCoursesByCategory(mainCategory, subCategory) {
    return apiClient.get(`${API_V1_PREFIX}/courses`, {
      params: { mainCategory, subCategory },
    })
  },

  getHotCourses() {
    return apiClient.get(`${API_V1_PREFIX}/courses/hot`)
  },

  getCoursesRanking() {
    return apiClient.get(`${API_V1_PREFIX}/courses/ranking`)
  },

  createCourse(courseData) {
    return apiClient.post(`${API_V1_PREFIX}/courses`, courseData)
  },

  updateCourse(id, courseData) {
    return apiClient.put(`${API_V1_PREFIX}/admin/courses/${id}`, courseData)
  },

  createSubcourse(parentId, name, description) {
    return apiClient.post(`${API_V1_PREFIX}/courses/${parentId}/subcourses`, {
      name,
      description,
    })
  },

  approveCourse(id, action, rejectedReason) {
    return apiClient.post(`${API_V1_PREFIX}/admin/courses/${id}/approve`, {
      action,
      reason: rejectedReason,
    })
  },
}

// 帖子管理服务
export const postServiceV1 = {
  getPosts(ids, nodeId, lastScore, lastPostingId) {
    return apiClient.get(`${API_V1_PREFIX}/posts`, {
      params: { ids: ids?.join(','), nodeId, lastScore, lastId: lastPostingId },
    })
  },

  createPost(postData) {
    return apiClient.post(`${API_V1_PREFIX}/posts`, postData)
  },

  updatePost(id, postData) {
    return apiClient.put(`${API_V1_PREFIX}/posts/${id}`, postData)
  },

  deletePost(id) {
    return apiClient.delete(`${API_V1_PREFIX}/posts/${id}`)
  },

  getPost(id) {
    return apiClient.get(`${API_V1_PREFIX}/posts/${id}`)
  },

  getNodePosts(nodeId) {
    return apiClient.get(`${API_V1_PREFIX}/nodes/${nodeId}/posts`)
  },

  getPendingPosts() {
    return apiClient.get(`${API_V1_PREFIX}/admin/posts/pending`)
  },

  getPostsByState(state) {
    return apiClient.get(`${API_V1_PREFIX}/admin/posts`, {
      params: { state },
    })
  },

  approvePost(id, approve) {
    return apiClient.put(`${API_V1_PREFIX}/admin/posts/${id}/approve`, null, {
      params: { approve },
    })
  },
}

// 评论管理服务
export const commentServiceV1 = {
  createComment(objectId, type, replyTo, toUser, content) {
    return apiClient.post(`${API_V1_PREFIX}/comments`, {
      objectId,
      type,
      replyTo,
      toUser,
      content,
    })
  },

  getComments(objectId, type, offsetId = 0) {
    return apiClient.get(`${API_V1_PREFIX}/comments`, {
      params: { objectId, type, offsetId },
    })
  },

  getCommentReplies(id, offsetId = 0) {
    return apiClient.get(`${API_V1_PREFIX}/comments/${id}/replies`, {
      params: { offsetId },
    })
  },

  getPendingComments() {
    return apiClient.get(`${API_V1_PREFIX}/admin/comments/pending`)
  },

  approveComment(id, approve) {
    return apiClient.put(`${API_V1_PREFIX}/admin/comments/${id}/approve`, {
      approve,
    })
  },
}

// 路线图管理服务
export const roadmapServiceV1 = {
  getProfessionRoadmaps(professionId, lastId = 0) {
    return apiClient.get(`${API_V1_PREFIX}/professions/${professionId}/roadmaps`, {
      params: { lastId },
    })
  },

  updateRoadmap(id, content) {
    return apiClient.put(`${API_V1_PREFIX}/roadmaps/${id}`, {
      content,
    })
  },

  upvoteRoadmap(id) {
    return apiClient.put(`${API_V1_PREFIX}/roadmaps/${id}/upvote`)
  },

  createRoadmap(professionId, content, description) {
    return apiClient.post(`${API_V1_PREFIX}/roadmaps`, {
      professionId,
      content,
      description,
    })
  },

  getRoadmap(id) {
    return apiClient.get(`${API_V1_PREFIX}/roadmaps/${id}`)
  },

  pinRoadmap(professionId, roadmapId) {
    return apiClient.post(`${API_V1_PREFIX}/roadmaps/pin`, {
      professionId,
      roadmapId,
    })
  },
}

// 点赞服务
export const upvoteServiceV1 = {
  upvote(objectId, objectType, type) {
    return apiClient.post(`${API_V1_PREFIX}/upvotes`, {
      objectId,
      objectType,
      type,
    })
  },

  getUpvoteStatus(objectId, objectType) {
    return apiClient.get(`${API_V1_PREFIX}/upvotes/status`, {
      params: { objectId, objectType },
    })
  },
}

// 消息管理服务
export const messageServiceV1 = {
  applyCourse(title, summary, explanation, parentId) {
    return apiClient.post(`${API_V1_PREFIX}/messages/course-applications`, {
      title,
      summary,
      explanation,
      parentId,
    })
  },

  getSystemMessages(type, lastId) {
    return apiClient.get(`${API_V1_PREFIX}/messages/system`, {
      params: { type, lastId },
    })
  },

  getMessages() {
    return apiClient.get(`${API_V1_PREFIX}/messages`)
  },

  sendSystemMessage(type, to, content) {
    return apiClient.post(`${API_V1_PREFIX}/messages/system`, {
      type,
      to,
      content,
    })
  },

  updateCourseApplication(id, reply) {
    return apiClient.put(`${API_V1_PREFIX}/messages/course-applications/${id}`, {
      reply,
    })
  },

  // 邀请用户
  inviteUser(userId, nodeId) {
    return apiClient.post(`${API_V1_PREFIX}/messages/invite`, {
      userId,
      nodeId,
    })
  },
}

// 学习进度服务
export const progressServiceV1 = {
  markNodeComplete(nodeId, courseId) {
    return apiClient.post(`${API_V1_PREFIX}/progress/nodes/${nodeId}/complete`, {
      courseId,
    })
  },

  unmarkNodeComplete(nodeId) {
    return apiClient.delete(`${API_V1_PREFIX}/progress/nodes/${nodeId}/complete`)
  },

  getNodeStatus(nodeId) {
    return apiClient.get(`${API_V1_PREFIX}/progress/nodes/${nodeId}/status`)
  },

  startCourse(courseId) {
    return apiClient.post(`${API_V1_PREFIX}/progress/courses/${courseId}/start`, {
      courseId,
    })
  },

  getCourseProgress(courseId) {
    return apiClient.get(`${API_V1_PREFIX}/progress/courses/${courseId}`)
  },

  getAllCourseProgress(lastId) {
    return apiClient.get(`${API_V1_PREFIX}/progress/courses`, {
      params: { lastId },
    })
  },

  updateCourseProgress(courseId, data) {
    return apiClient.put(`${API_V1_PREFIX}/progress/courses/${courseId}`, data)
  },

  deleteCourseProgress(courseId) {
    return apiClient.delete(`${API_V1_PREFIX}/progress/courses/${courseId}`)
  },

  completeCourse(courseId) {
    return apiClient.post(`${API_V1_PREFIX}/progress/courses/${courseId}/complete`)
  },

  // 路线图进度相关方法
  startRoadmap(roadmapId) {
    return apiClient.post(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}/start`)
  },

  getRoadmapProgress(roadmapId) {
    return apiClient.get(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`)
  },

  getUserRoadmaps() {
    return apiClient.get(`${API_V1_PREFIX}/progress/roadmaps`)
  },

  updateRoadmapProgress(roadmapId, progressPercent) {
    return apiClient.put(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`, {
      progressPercent,
    })
  },

  deleteRoadmapProgress(roadmapId) {
    return apiClient.delete(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`)
  },
}

// 统计服务
export const statsServiceV1 = {
  recordView(articleId, userId, ipAddress) {
    return apiClient.post(`${API_V1_PREFIX}/stats/views`, {
      articleId,
      userId,
      ipAddress,
    })
  },

  getUserTodayStats(userId) {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/today`)
  },

  getUserYesterdayStats(userId) {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/yesterday`)
  },

  getUserHistoryStats(userId, days = 7) {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/history`, {
      params: { days },
    })
  },

  getUserPeriodStats(userId, days = 7) {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/period`, {
      params: { days },
    })
  },

  getUserAllTimeStats(userId) {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/all-time`)
  },

  syncManual() {
    return apiClient.post(`${API_V1_PREFIX}/stats/sync/manual`)
  },

  getHealth() {
    return apiClient.get(`${API_V1_PREFIX}/stats/health`)
  },

  syncDate(date) {
    return apiClient.post(`${API_V1_PREFIX}/stats/sync/date`, {
      date,
    })
  },

  getPlatformStats() {
    return apiClient.get(`${API_V1_PREFIX}/stats/platform`)
  },
}

// 页面聚合服务
export const pageServiceV1 = {
  readByCoursePath(courseId, path) {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { courseId, path },
    })
  },

  readByNode(nodeId) {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { nodeId },
    })
  },

  readByPost(postId) {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { postId },
    })
  },

  readByComment(commentId) {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { commentId },
    })
  },
}

// 内容管理服务
export const contentServiceV1 = {
  operateContent(data) {
    return apiClient.post(`${API_V1_PREFIX}/contents`, data)
  },
}

// AI服务
export const aiServiceV1 = {
  chat(prompt, model) {
    return apiClient.post(`${API_V1_PREFIX}/ai/chat`, {
      prompt,
      model,
    })
  },
}

// 记忆卡片组服务
export const memoryDeckServiceV1 = {
  // 获取当前用户的所有记忆卡片组
  getCurrentUserMemoryDecks(sortBy = 'createdAt', sortOrder = 'desc', lastScore, lastId, limit = 10) {
    return apiClient.get(`${API_V1_PREFIX}/memory/users/me/memory-decks`, {
      params: { sortBy, sortOrder, lastScore, lastId, limit },
    })
  },

  // 获取指定用户的记忆卡片组（仅已发布）
  getUserMemoryDecks(userId, sortBy = 'createdAt', sortOrder = 'desc', lastScore, lastId, limit = 10) {
    return apiClient.get(`${API_V1_PREFIX}/memory/users/${userId}/memory-decks`, {
      params: { sortBy, sortOrder, lastScore, lastId, limit },
    })
  },

  // 管理后台：审核卡片组
  approveDeck(deckId, action, reason) {
    return apiClient.post(`${API_V1_PREFIX}/admin/memory/decks/${deckId}/approve`, {
      action,
      reason,
    })
  },

  // 管理后台：查询卡片组列表
  getAdminDecks(state, postId, creatorId, lastId, limit) {
    return apiClient.get(`${API_V1_PREFIX}/admin/memory/decks`, {
      params: { state, postId, creatorId, lastId, limit },
    })
  },
}

// 系统配置服务
export const systemServiceV1 = {
  getSystemConfig(part) {
    if (part) {
      return apiClient.get(`${API_V1_PREFIX}/system`, {
        params: { part },
      })
    }
    return apiClient.get(`${API_V1_PREFIX}/system`)
  },

  // 根据key获取单个配置
  getConfigByKey(key) {
    return apiClient.get(`${API_V1_PREFIX}/system/${key}`)
  },

  // 更新配置（key-value模式）
  updateConfigByKey(key, value) {
    return apiClient.post(
      `${API_V1_PREFIX}/system`,
      {
        value,
      },
      {
        params: { key },
      }
    )
  },

  // 删除配置
  deleteConfigByKey(key) {
    return apiClient.delete(`${API_V1_PREFIX}/system`, {
      params: { key },
    })
  },

  // 获取课程分类数据
  getCourseCategories() {
    return apiClient.get(`${API_V1_PREFIX}/system`, {
      params: { part: 'courseCategories' },
    })
  },

  // 获取职业分类数据
  getProfessionCategories() {
    return apiClient.get(`${API_V1_PREFIX}/system`, {
      params: { part: 'professionCategories' },
    })
  },
}
