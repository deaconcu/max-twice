import type { Node } from './node'
import type { Course } from './course'
import type { Post } from './post'
import type { User } from './user'

export interface ReadResponse {
  node?: Node
  parentCourse?: Course
  course?: Course
  subCourseList?: Course[]
  chosenPosting?: Post
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
  rootNodeId?: number
}
