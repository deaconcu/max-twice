import type { Node } from './node'
import type { Course } from './course'
import type { Post } from './post'
import type { User } from './user'

/**
 * 节点进度响应
 */
export interface NodeProgressResponse {
  nodeId: number
  completed: boolean
  isNewlyCompleted?: boolean
  wasRemoved?: boolean
  courseProgress: number
  totalCompletedNodes: number
}

/**
 * 课程完成响应
 */
export interface CourseCompletionResponse {
  courseId: number
  completed: boolean
  message: string
}

/**
 * 审批响应
 */
export interface ApprovalResponse {
  success: boolean
  message: string
  objectId: number
  objectType: 'course' | 'profession'
  action: 'approve' | 'reject' | 'delete'
}

/**
 * 读取内容响应
 */
export interface ReadResponse {
  node?: Node
  parentCourse?: Course
  course?: Course
  subCourseList?: Course[]
  chosenPosting?: Post
  fixedPostings?: Post[]
  otherPostings?: Post[]
  lastId?: number
  toc?: any[]
  tocNodeInfos?: Record<number, Node>
  path?: string
  users?: User[]
  learning?: boolean
  post?: Post
  commentId?: number
  subCommentId?: number
}

/**
 * 点赞状态响应
 */
export interface UpvoteStatusResponse {
  objectId: number
  objectType: number
  upvotes: number
  upvoted: boolean
  twiceUpvotes?: number
  twiceUpvoted?: boolean
  helpfulUpvotes?: number
  helpfulUpvoted?: boolean
}