import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types/api'
import type { User, LoginResponseData, UserStatsDTO } from '@/types/user'
import type { Course } from '@/types/course'
import type { Post } from '@/types/post'
import type { Comment } from '@/types/comment'
import type { Profession } from '@/types/profession'
import type { Roadmap } from '@/types/roadmap'
import type { UserCourse } from '@/types/userCourse'
import type { UserRoadmap } from '@/types/userRoadmap'
import type { Message } from '@/types/message'
import type { PlatformStats, DailyStats } from '@/types/stats'
import type { Node } from '@/types/node'
import type { 
  NodeProgressResponse, 
  CourseCompletionResponse, 
  ApprovalResponse, 
  ReadResponse, 
  UpvoteStatusResponse 
} from '@/types/response'
import { ObjectType, VoteType, MessageType } from '@/types/enums'

// 设置 axios 默认配置
axios.defaults.withCredentials = true

const apiClient = axios.create({
  baseURL: 'http://localhost:9202/',
  timeout: 60000,
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
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

// API v1 统一服务模块，使用 JSON 格式
const API_V1_PREFIX = '/api/v1'

// 用户认证服务
export const authServiceV1 = {
  login(email: string, password: string): Promise<ApiResponse<LoginResponseData>> {
    return apiClient.post(`${API_V1_PREFIX}/auth/login`, {
      email,
      password,
    })
  },

  register(email: string, password: string): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/auth/register`, {
      email,
      password,
    })
  },

  validateEmail(email: string, code: string): Promise<ApiResponse<User>> {
    return apiClient.post(`${API_V1_PREFIX}/auth/validate-email`, {
      email,
      code,
    })
  },
}

// 用户管理服务
export const userServiceV1 = {
  getCurrentUser(): Promise<ApiResponse<User>> {
    return apiClient.get(`${API_V1_PREFIX}/users/current`)
  },

  updateCurrentUser(name: string, biography: string): Promise<ApiResponse<User>> {
    return apiClient.put(`${API_V1_PREFIX}/users/current`, {
      name,
      biography,
    })
  },

  getUser(username: string): Promise<ApiResponse<User>> {
    return apiClient.get(`${API_V1_PREFIX}/users/${username}`)
  },

  searchUser(name: string): Promise<ApiResponse<User[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/search`, {
      params: { name },
    })
  },

  getUserPosts(userId: number, lastId?: number, type = 2): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/posts`, {
      params: { lastId, type },
    })
  },

  getCurrentUserAllPosts(lastId?: number, type = 2): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/me/posts`, {
      params: { lastId, type },
    })
  },

  getCurrentUserRoadmaps(lastId?: number): Promise<ApiResponse<Roadmap[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/me/roadmaps`, {
      params: { lastId },
    })
  },

  getUserRoadmaps(userId: number, lastId?: number): Promise<ApiResponse<Roadmap[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/roadmaps`, {
      params: { lastId },
    })
  },

  deleteRoadmap(roadmapId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/roadmaps/${roadmapId}`)
  },
}

// 关注服务
export const followServiceV1 = {
  follow(followeeId: number): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/follows`, {
      followeeId,
    })
  },

  unfollow(followeeId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/follows/${followeeId}`)
  },

  getFollowees(userId: number, lastCreateTime?: string): Promise<ApiResponse<User[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/followees`, {
      params: { lastCreateTime },
    })
  },
}

// 订阅服务
export const subscriptionServiceV1 = {
  getUserSubscriptions(userId: number): Promise<ApiResponse<UserCourse[]>> {
    return apiClient.get(`${API_V1_PREFIX}/users/${userId}/subscriptions`)
  },

  subscribe(courseId: number): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/users/current/subscriptions`, {
      courseId,
    })
  },

  updateSubscriptions(subscription: any): Promise<ApiResponse<any>> {
    return apiClient.put(`${API_V1_PREFIX}/users/current/subscriptions`, {
      subscription,
    })
  },

  unsubscribe(courseId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/users/current/subscriptions/${courseId}`)
  },
}

// 职业管理服务
export const professionServiceV1 = {
  getProfessionsByPage(page = 1): Promise<ApiResponse<Profession[]>> {
    return apiClient.get(`${API_V1_PREFIX}/professions`, {
      params: { page },
    })
  },

  // 按分类获取职业（前端使用）
  getProfessionsByCategory(lastId?: number, mainCategory?: number, subCategory?: number): Promise<ApiResponse<Profession[]>> {
    return apiClient.get(`${API_V1_PREFIX}/professions`, {
      params: { lastId, mainCategory, subCategory },
    })
  },

  getApprovedProfessions(lastId?: number | null): Promise<ApiResponse<Profession[]>> {
    const params = lastId !== undefined && lastId !== null ? { lastId } : {}
    return apiClient.get(`${API_V1_PREFIX}/professions/approved`, { params })
  },

  getProfession(id: number): Promise<ApiResponse<Profession>> {
    return apiClient.get(`${API_V1_PREFIX}/professions/${id}`)
  },

  createProfession(professionData: Partial<Profession>): Promise<ApiResponse<Profession>> {
    return apiClient.post(`${API_V1_PREFIX}/professions`, professionData)
  },

  updateProfession(id: number, professionData: Partial<Profession>): Promise<ApiResponse<Profession>> {
    return apiClient.put(`${API_V1_PREFIX}/admin/professions/${id}`, professionData)
  },

  approveProfession(id: number, action: string, reason?: string): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`${API_V1_PREFIX}/admin/professions/${id}/approve`, {
      action,
      reason,
    })
  },

  deleteProfession(id: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/professions/${id}`)
  },

  getHotProfessions(limit = 10): Promise<ApiResponse<Profession[]>> {
    return apiClient.get(`${API_V1_PREFIX}/professions/hot`, {
      params: { limit },
    })
  },
}

// 课程管理服务
export const courseServiceV1 = {
  getCourse(id: number): Promise<ApiResponse<Course>> {
    return apiClient.get(`${API_V1_PREFIX}/courses/${id}`)
  },

  searchCourses(name: string): Promise<ApiResponse<Course[]>> {
    return apiClient.get(`${API_V1_PREFIX}/courses/search`, {
      params: { name },
    })
  },

  getCoursesByState(state: number, lastId?: number): Promise<ApiResponse<Course[]>> {
    return apiClient.get(`${API_V1_PREFIX}/courses`, {
      params: { state, lastId },
    })
  },

  getCoursesByCategory(mainCategory?: number, subCategory?: number): Promise<ApiResponse<Course[]>> {
    return apiClient.get(`${API_V1_PREFIX}/courses`, {
      params: { mainCategory, subCategory },
    })
  },

  getHotCourses(): Promise<ApiResponse<Course[]>> {
    return apiClient.get(`${API_V1_PREFIX}/courses/hot`)
  },

  getCoursesRanking(): Promise<ApiResponse<Course[]>> {
    return apiClient.get(`${API_V1_PREFIX}/courses/ranking`)
  },

  createCourse(courseData: Partial<Course>): Promise<ApiResponse<Course>> {
    return apiClient.post(`${API_V1_PREFIX}/courses`, courseData)
  },

  updateCourse(id: number, courseData: Partial<Course>): Promise<ApiResponse<Course>> {
    return apiClient.put(`${API_V1_PREFIX}/courses/${id}`, courseData)
  },

  createSubcourse(parentId: number, name: string, description: string): Promise<ApiResponse<Course>> {
    return apiClient.post(`${API_V1_PREFIX}/courses/${parentId}/subcourses`, {
      name,
      description,
    })
  },

  approveCourse(id: number, action: string, reason?: string): Promise<ApiResponse<ApprovalResponse>> {
    return apiClient.post(`${API_V1_PREFIX}/courses/${id}/approve`, {
      action,
      reason,
    })
  },

  updateUserCourseToc(courseId: number, indexArray: string): Promise<ApiResponse<string>> {
    return apiClient.put(`${API_V1_PREFIX}/users/current/courses/${courseId}/toc`, {
      indexArray,
    })
  },
}

// 帖子管理服务
export const postServiceV1 = {
  getPosts(ids?: number[], nodeId?: number, lastScore?: number, lastPostingId?: number): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`${API_V1_PREFIX}/posts`, {
      params: { ids: ids?.join(','), nodeId, lastScore, lastId: lastPostingId },
    })
  },

  createPost(postData: Partial<Post>): Promise<ApiResponse<Post>> {
    return apiClient.post(`${API_V1_PREFIX}/posts`, postData)
  },

  updatePost(id: number, postData: Partial<Post>): Promise<ApiResponse<Post>> {
    return apiClient.put(`${API_V1_PREFIX}/posts/${id}`, postData)
  },

  deletePost(id: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/posts/${id}`)
  },

  getPost(id: number): Promise<ApiResponse<Post>> {
    return apiClient.get(`${API_V1_PREFIX}/posts/${id}`)
  },

  getNodePosts(nodeId: number): Promise<ApiResponse<Post[]>> {
    return apiClient.get(`${API_V1_PREFIX}/nodes/${nodeId}/posts`)
  },
}

// 评论管理服务
export const commentServiceV1 = {
  createComment(objectId: number, objectType: ObjectType, replyTo?: number | null, toUser?: number | null, content?: string): Promise<ApiResponse<Comment>> {
    const body: any = {
      objectId,
      objectType,
      content,
    }
    if (replyTo !== null && replyTo !== undefined) {
      body.replyTo = replyTo
    }
    if (toUser !== null && toUser !== undefined) {
      body.toUser = toUser
    }
    return apiClient.post(`${API_V1_PREFIX}/comments`, body)
  },

  getComments(objectId: number, objectType: ObjectType, offsetId = 0): Promise<ApiResponse<Comment[]>> {
    return apiClient.get(`${API_V1_PREFIX}/comments`, {
      params: { objectId, objectType, offsetId },
    })
  },

  getCommentReplies(id: number, offsetId = 0): Promise<ApiResponse<Comment[]>> {
    return apiClient.get(`${API_V1_PREFIX}/comments/${id}/replies`, {
      params: { offsetId },
    })
  },
}

// 路线图管理服务
export const roadmapServiceV1 = {
  getProfessionRoadmaps(professionId: number, lastId = 0): Promise<ApiResponse<Roadmap[]>> {
    return apiClient.get(`${API_V1_PREFIX}/professions/${professionId}/roadmaps`, {
      params: { lastId },
    })
  },

  updateRoadmap(id: number, content: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`${API_V1_PREFIX}/roadmaps/${id}`, {
      content,
    })
  },

  createRoadmap(professionId: number, content: string, description: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.post(`${API_V1_PREFIX}/roadmaps`, {
      professionId,
      content,
      description,
    })
  },

  getRoadmap(id: number): Promise<ApiResponse<Roadmap>> {
    return apiClient.get(`${API_V1_PREFIX}/roadmaps/${id}`)
  },

  pinRoadmap(professionId: number, roadmapId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`${API_V1_PREFIX}/roadmaps/pin`, {
      professionId,
      roadmapId,
    })
  },

  approveRoadmap(id: number, action: string, reason?: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.post(`${API_V1_PREFIX}/roadmaps/${id}/approve`, {
      action,
      reason,
    })
  },

  updateRoadmapDescription(id: number, description: string): Promise<ApiResponse<Roadmap>> {
    return apiClient.put(`${API_V1_PREFIX}/roadmaps/${id}/description`, {
      description,
    })
  },
}

// 点赞服务
export const upvoteServiceV1 = {
  upvote(objectId: number, objectType: ObjectType, type: VoteType): Promise<ApiResponse<UpvoteStatusResponse>> {
    return apiClient.post(`${API_V1_PREFIX}/upvotes`, {
      objectId,
      objectType,
      type,
    })
  },

  getUpvoteStatus(objectId: number, objectType: ObjectType): Promise<ApiResponse<UpvoteStatusResponse>> {
    return apiClient.get(`${API_V1_PREFIX}/upvotes/status`, {
      params: { objectId, objectType },
    })
  },
}

// 消息管理服务
export const messageServiceV1 = {
  applyCourse(title: string, summary: string, explanation: string, parentId?: number): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/messages/course-applications`, {
      title,
      summary,
      explanation,
      parentId,
    })
  },

  getSystemMessages(type?: MessageType, lastId?: number): Promise<ApiResponse<Message[]>> {
    return apiClient.get(`${API_V1_PREFIX}/messages/system`, {
      params: { type, lastId },
    })
  },

  /**
   * 按分类获取消息
   * @param category 消息分类 (1=互动消息, 2=系统消息, 3=私信)
   * @param lastId 最后一条消息的 ID，用于分页
   * @param type 可选的消息类型过滤
   */
  getMessagesByCategory(category: number, lastId?: number, type?: MessageType): Promise<ApiResponse<Message[]>> {
    const params: Record<string, any> = { category }
    if (lastId !== undefined && lastId !== null) {
      params.lastId = lastId
    }
    if (type !== undefined && type !== null) {
      params.type = type
    }
    return apiClient.get(`${API_V1_PREFIX}/messages/category`, { params })
  },

  getMessages(): Promise<ApiResponse<Message[]>> {
    return apiClient.get(`${API_V1_PREFIX}/messages`)
  },

  sendSystemMessage(type: MessageType, to: number, content: string): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/messages/system`, {
      type,
      to,
      content,
    })
  },

  updateCourseApplication(id: number, reply: string): Promise<ApiResponse<void>> {
    return apiClient.put(`${API_V1_PREFIX}/messages/course-applications/${id}`, {
      reply,
    })
  },

  // 邀请用户
  inviteUser(userId: number, nodeId: number): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/messages/invite`, {
      userId,
      nodeId,
    })
  },
}

// 学习进度服务
export const progressServiceV1 = {
  markNodeComplete(nodeId: number, courseId: number): Promise<ApiResponse<NodeProgressResponse>> {
    return apiClient.post(`${API_V1_PREFIX}/progress/nodes/${nodeId}/complete`, {
      courseId,
    })
  },

  unmarkNodeComplete(nodeId: number, courseId: number): Promise<ApiResponse<NodeProgressResponse>> {
    return apiClient.delete(`${API_V1_PREFIX}/progress/nodes/${nodeId}/complete`, {
      data: { courseId }
    })
  },

  getNodeStatus(nodeId: number): Promise<ApiResponse<Node>> {
    return apiClient.get(`${API_V1_PREFIX}/progress/nodes/${nodeId}/status`)
  },

  startCourse(courseId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`${API_V1_PREFIX}/progress/courses/${courseId}/start`, {
      courseId,
    })
  },

  getCourseProgress(courseId: number): Promise<ApiResponse<UserCourse>> {
    return apiClient.get(`${API_V1_PREFIX}/progress/courses/${courseId}`)
  },

  getAllCourseProgress(lastId?: number): Promise<ApiResponse<UserCourse[]>> {
    return apiClient.get(`${API_V1_PREFIX}/progress/courses`, {
      params: { lastId },
    })
  },

  updateCourseProgress(courseId: number, data: Partial<UserCourse>): Promise<ApiResponse<UserCourse>> {
    return apiClient.put(`${API_V1_PREFIX}/progress/courses/${courseId}`, data)
  },

  deleteCourseProgress(courseId: number): Promise<ApiResponse<string>> {
    return apiClient.delete(`${API_V1_PREFIX}/progress/courses/${courseId}`)
  },

  completeCourse(courseId: number): Promise<ApiResponse<CourseCompletionResponse>> {
    return apiClient.post(`${API_V1_PREFIX}/progress/courses/${courseId}/complete`)
  },

  // 路线图进度相关方法
  startRoadmap(roadmapId: number): Promise<ApiResponse<boolean>> {
    return apiClient.post(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}/start`)
  },

  getRoadmapProgress(roadmapId: number): Promise<ApiResponse<UserRoadmap>> {
    return apiClient.get(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`)
  },

  getUserRoadmaps(): Promise<ApiResponse<UserRoadmap[]>> {
    return apiClient.get(`${API_V1_PREFIX}/progress/roadmaps`)
  },

  updateRoadmapProgress(roadmapId: number, progressPercent: number): Promise<ApiResponse<UserRoadmap>> {
    return apiClient.put(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`, {
      progressPercent,
    })
  },

  deleteRoadmapProgress(roadmapId: number): Promise<ApiResponse<string>> {
    return apiClient.delete(`${API_V1_PREFIX}/progress/roadmaps/${roadmapId}`)
  },
}

// 统计服务
export const statsServiceV1 = {
  recordView(articleId: number, userId?: number, ipAddress?: string): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/stats/views`, {
      articleId,
      userId,
      ipAddress,
    })
  },

  getUserTodayStats(userId: number): Promise<ApiResponse<DailyStats>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/today`)
  },

  getUserYesterdayStats(userId: number): Promise<ApiResponse<DailyStats>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/yesterday`)
  },

  getUserHistoryStats(userId: number, days = 7): Promise<ApiResponse<DailyStats[]>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/history`, {
      params: { days },
    })
  },

  getUserPeriodStats(userId: number, days = 7): Promise<ApiResponse<UserStatsDTO>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/period`, {
      params: { days },
    })
  },

  getUserAllTimeStats(userId: number): Promise<ApiResponse<UserStatsDTO>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/users/${userId}/all-time`)
  },

  syncManual(): Promise<ApiResponse<string>> {
    return apiClient.post(`${API_V1_PREFIX}/stats/sync/manual`)
  },

  getHealth(): Promise<ApiResponse<string>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/health`)
  },

  syncDate(date: string): Promise<ApiResponse<string>> {
    return apiClient.post(`${API_V1_PREFIX}/stats/sync/date`, {
      date,
    })
  },

  getPlatformStats(): Promise<ApiResponse<PlatformStats>> {
    return apiClient.get(`${API_V1_PREFIX}/stats/platform`)
  },
}

// 页面聚合服务
export const pageServiceV1 = {
  readByCoursePath(courseId: number, path: string): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { courseId, path },
    })
  },

  readByNode(nodeId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { nodeId },
    })
  },

  readByPost(postId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { postId },
    })
  },

  readByComment(commentId: number): Promise<ApiResponse<ReadResponse>> {
    return apiClient.get(`${API_V1_PREFIX}/pages/read`, {
      params: { commentId },
    })
  },
}

// 内容管理服务
export const contentServiceV1 = {
  operateContent(data: any): Promise<ApiResponse<any>> {
    return apiClient.post(`${API_V1_PREFIX}/contents`, data)
  },
}

// AI服务
export const aiServiceV1 = {
  chat(prompt: string, model?: string): Promise<ApiResponse<string>> {
    return apiClient.post(`${API_V1_PREFIX}/ai/chat`, {
      prompt,
      model,
    })
  },
}

// 记忆卡片组服务
export const memoryCardDeckServiceV1 = {
  // AI生成记忆卡片组
  createAIDeck(postId: number): Promise<ApiResponse<void>> {
    return apiClient.post(`${API_V1_PREFIX}/memory/decks/${postId}/ai-generate`)
  },

  // 删除卡片组
  deleteDeck(deckId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`${API_V1_PREFIX}/memory/decks/${deckId}`)
  },
}

// 系统配置服务（公开接口）
export const systemServiceV1 = {
  // 获取课程分类数据
  getCourseCategories(): Promise<ApiResponse<any>> {
    return apiClient.get(`${API_V1_PREFIX}/public/course-categories`)
  },

  // 获取职业分类数据
  getProfessionCategories(): Promise<ApiResponse<any>> {
    return apiClient.get(`${API_V1_PREFIX}/public/profession-categories`)
  },

  // 获取只读模式状态（公开接口，无需登录）
  getReadonlyMode(): Promise<ApiResponse<{ enabled: boolean }>> {
    return apiClient.get(`${API_V1_PREFIX}/public/readonly-mode`)
  },
}

export default apiClient