import type { Node } from './node'
import type { Course } from './course'
import type { Post } from './post'
import type { User } from './user'

/**
 * 页面聚合响应类型
 * 参考：web-ts/src/types/response.ts (ReadResponse)
 */

/**
 * 目录节点信息（简化版）
 * TODO: 根据实际后端返回结构调整
 */
export interface TocItem {
  id: number
  name: string
  children?: TocItem[]
}

/**
 * 读取内容响应
 * 用于 /api/v1/pages/read 接口
 * 根据不同的查询参数（courseId+path, nodeId, postId, commentId）返回不同的聚合数据
 */
export interface ReadResponse {
  // 节点相关
  node?: Node
  parentCourse?: Course

  // 课程相关
  course?: Course
  subCourseList?: Course[]

  // 帖子相关
  chosenPosting?: Post
  fixedPostings?: Post[]
  otherPostings?: Post[]
  lastId?: number

  // TOC（目录）相关
  toc?: TocItem[]
  tocNodeInfos?: Record<number, Node>
  path?: string

  // 其他
  users?: User[]
  learning?: boolean
  post?: Post
  commentId?: number
  subCommentId?: number
}
