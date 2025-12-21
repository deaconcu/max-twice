# 后端接口文档

## 基本信息
- **Base URL**: `/api/v1`
- **认证方式**: Sa-Token (通过 `@SaCheckLogin` 注解标识需要登录的接口)
- **Rate Limit**: 各 Controller 有不同的限流配置

---

## 1. 课程管理接口 (CoursesController)

### 基本信息
- **Rate Limit**: 40 requests/minute (per user)
- **Controller**: `CoursesController.java`
- **前端 API**: `web/src/api/modules/course.ts`

### 1.1 获取课程详情

**接口路径**: `GET /api/v1/courses/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "课程名称",
    "description": "课程描述",
    "mainCategory": 1,
    "subCategory": 2,
    "state": 3,
    "parentCourseId": 0,
    "rootNodeId": 10,
    "creatorId": 100,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `courseApi.getCourse(id)`
- 使用场景：课程详情页

---

### 1.2 搜索课程

**接口路径**: `GET /api/v1/courses/search`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 搜索关键词，不能为空 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "课程名称",
      "description": "课程描述"
    }
  ]
}
```

**前端调用位置**:
- `courseApi.searchCourses(name)`
- 使用场景：课程搜索功能

---

### 1.3 获取课程列表

**接口路径**: `GET /api/v1/courses`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 分页游标，上一页最后一条数据的ID |
| mainCategory | Integer | 否 | 主分类ID，必须大于0 |
| subCategory | Integer | 否 | 子分类ID，必须大于0（需要与mainCategory一起使用） |
| parentId | Long | 否 | 父课程ID，用于获取子课程 |

**参数组合规则**:
1. **按分类筛选**: 传 `mainCategory`（可选 `subCategory`）
2. **获取子课程**: 传 `parentId`
3. **获取所有课程**: 不传参数或只传 `lastId`（返回所有已发布课程，支持分页）

**注意**: 普通用户只能看到已发布课程（state=3）

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "课程名称",
      "description": "课程描述",
      "mainCategory": 1,
      "subCategory": 2,
      "state": 3
    }
  ]
}
```

**前端调用位置**:
- `courseApi.getCoursesByCategory(mainCategory?, subCategory?, lastId?)`
- `courseApi.getSubCourses(parentId)`
- 使用场景：
  - 课程列表页 (`CourseListPage.vue`)
  - 分类筛选
  - 子课程列表

---

### 1.4 获取热门课程

**接口路径**: `GET /api/v1/courses/hot`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "课程名称",
      "description": "课程描述",
      "viewCount": 1000,
      "subscriberCount": 500,
      "ranking": 1
    }
  ]
}
```

**前端调用位置**:
- `courseApi.getHotCourses()`
- 使用场景：首页热门课程展示

---

### 1.5 获取课程排行榜

**接口路径**: `GET /api/v1/courses/ranking`

**是否需要登录**: 否

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "课程名称",
      "description": "课程描述",
      "viewCount": 1000,
      "subscriberCount": 500,
      "ranking": 1
    }
  ]
}
```

**前端调用位置**:
- `courseApi.getCoursesRanking()`
- 使用场景：课程排行榜页面

---

### 1.6 创建课程

**接口路径**: `POST /api/v1/courses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "name": "课程名称",
  "description": "课程描述",
  "mainCategory": 1,
  "subCategory": 2
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "课程创建成功"
}
```

**前端调用位置**:
- `courseApi.createCourse(courseData)`
- 使用场景：创建新课程

---

### 1.7 创建子课程

**接口路径**: `POST /api/v1/courses/{parentId}/subcourses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | Long | 是 | 父课程ID，必须大于0 |

**请求体**:
```json
{
  "name": "子课程名称",
  "description": "子课程描述"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "课程创建成功"
}
```

**前端调用位置**:
- `courseApi.createSubcourse(parentId, name, description)`
- 使用场景：在主课程下创建子课程

---

## 测试用例建议

### 课程管理接口测试用例

#### 1.1 获取课程详情
- ✅ 正常获取已发布课程
- ✅ 课程ID不存在返回404
- ✅ 课程ID为0或负数返回400

#### 1.2 搜索课程
- ✅ 搜索存在的课程名称
- ✅ 搜索不存在的课程返回空列表
- ✅ 搜索关键词为空返回400

#### 1.3 获取课程列表
- ✅ 不传参数获取所有已发布课程
- ✅ 按主分类筛选
- ✅ 按主分类+子分类筛选
- ✅ 获取子课程列表
- ✅ 分页功能（使用lastId）

#### 1.4 热门课程
- ✅ 默认获取10条
- ✅ 自定义limit参数
- ✅ 按热度排序

#### 1.5 创建课程
- ✅ 未登录返回401
- ✅ 已登录成功创建
- ✅ 必填字段缺失返回400

#### 1.6 创建子课程
- ✅ 未登录返回401
- ✅ 父课程不存在返回404
- ✅ 成功创建子课程

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败 |
| 401 | 未登录（需要登录的接口） |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

---

## DTO 说明

### CourseDetailDTO
包含课程的完整信息，用于详情展示和列表展示。

### CourseBriefDTO
包含课程的简要信息，用于搜索结果。

### CourseWithStatsDTO
包含课程信息和统计数据（浏览量、订阅数、排名），用于热门课程和排行榜。

---

---

## 2. 帖子管理接口 (PostsController)

### 基本信息
- **Rate Limit**: 30 requests/minute (per user)
- **Controller**: `PostsController.java`
- **前端 API**: `web/src/api/modules/post.ts`

### 2.1 批量获取帖子

**接口路径**: `GET /api/v1/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| ids | List<Long> | 否 | - | 帖子ID列表，逗号分隔 |
| nodeId | Long | 否 | - | 节点ID，必须大于0 |
| lastScore | Double | 否 | 0 | 分页游标 - 上一页最后一条的分数 |
| lastId | Long | 否 | 0 | 分页游标 - 上一页最后一条的ID |

**参数组合规则**:
1. **按ID查询**: 传 `ids`，返回指定ID的帖子列表
2. **按节点查询**: 传 `nodeId`（可选 `lastScore` 和 `lastId`），返回节点下的帖子列表

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "nodeId": 10,
      "creatorId": 100,
      "type": 2,
      "content": "帖子内容",
      "state": 3,
      "score": 95.5,
      "helpful": 10,
      "unhelpful": 2,
      "voteType": 1,
      "creator": {
        "id": 100,
        "username": "用户名",
        "avatar": "头像URL"
      }
    }
  ]
}
```

**前端调用位置**:
- `postApi.getPosts(ids?, nodeId?, lastScore?, lastPostingId?)`
- 使用场景：
  - 滚动加载帖子列表 (`NodePostsPage.vue`, `ContentReadPage.vue`)
  - 获取指定帖子的详细信息

---

### 2.2 创建帖子

**接口路径**: `POST /api/v1/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "nodeId": 10,
  "type": 2,
  "content": "帖子内容（Markdown或HTML）"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `postApi.createPost(postData)`
- 使用场景：发布新帖子

---

### 2.3 更新帖子

**接口路径**: `PUT /api/v1/posts/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**请求体**:
```json
{
  "content": "更新后的帖子内容"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "nodeId": 10,
    "content": "更新后的帖子内容",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `postApi.updatePost(id, postData)`
- 使用场景：编辑已发布的帖子

---

### 2.4 删除帖子

**接口路径**: `DELETE /api/v1/posts/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `postApi.deletePost(id)`
- 使用场景：删除帖子（软删除）

---

### 2.5 获取帖子详情

**接口路径**: `GET /api/v1/posts/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 帖子ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "nodeId": 10,
    "creatorId": 100,
    "type": 2,
    "content": "帖子内容",
    "state": 3,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `postApi.getPost(id)`
- 使用场景：查看帖子详情

---

### 2.6 获取节点帖子列表

**接口路径**: `GET /api/v1/nodes/{nodeId}/posts`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "nodeId": 10,
      "content": "帖子内容",
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

**前端调用位置**:
- `postApi.getNodePosts(nodeId)`
- 使用场景：获取某个节点下的所有帖子

---

### 2.7 获取用户帖子列表

**接口路径**: `GET /api/v1/users/{userId}/posts`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | - | 分页游标 |
| type | Integer | 否 | 2 | 帖子类型：1=目录，2=文章 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": 2,
      "content": "帖子内容",
      "state": 3
    }
  ]
}
```

**前端调用位置**:
- 用于个人主页展示用户发布的内容
- 使用场景：查看某个用户的所有已发布帖子

---

### 2.8 获取当前用户所有状态的帖子

**接口路径**: `GET /api/v1/users/me/posts`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | - | 分页游标 |
| type | Integer | 否 | 2 | 帖子类型：1=目录，2=文章 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "type": 2,
        "content": "帖子内容",
        "state": 2
      }
    ],
    "lastId": 1,
    "hasMore": true
  }
}
```

**前端调用位置**:
- 用于个人中心内容管理
- 使用场景：查看自己的所有帖子（包括待审核、已发布、已拒绝、已屏蔽）

---

## 3. 页面聚合接口 (PagesController)

### 基本信息
- **Rate Limit**: 80 requests/minute (per user)
- **Controller**: `PagesController.java`
- **前端 API**: `web/src/api/modules/page.ts`

### 3.1 读取页面数据

**接口路径**: `GET /api/v1/pages/read`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 否 | 课程ID，必须大于0 |
| path | String | 否 | 课程路径（需要配合courseId使用） |
| nodeId | Long | 否 | 节点ID，必须大于0 |
| postId | Long | 否 | 帖子ID，必须大于0 |
| commentId | Long | 否 | 评论ID，必须大于0 |

**参数优先级**: `commentId` > `postId` > `nodeId` > `courseId + path`

**参数组合规则**:
1. **按评论查询**: 传 `commentId`
2. **按帖子查询**: 传 `postId`
3. **按节点查询**: 传 `nodeId`
4. **按课程路径查询**: 传 `courseId` 和 `path`（path 可为空）

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "node": {
      "id": 10,
      "name": "节点名称",
      "description": "节点描述"
    },
    "parentCourse": {
      "id": 1,
      "name": "父课程名称"
    },
    "course": {
      "id": 2,
      "name": "当前课程名称"
    },
    "subCourseList": [],
    "toc": [],
    "tocNodeInfos": {},
    "path": "1-10",
    "chosenPosting": null,
    "fixedPostings": [],
    "otherPostings": [
      {
        "id": 100,
        "content": "帖子内容",
        "creator": {
          "id": 1000,
          "username": "作者"
        }
      }
    ],
    "users": [],
    "learning": false
  }
}
```

**前端调用位置**:
- `pageApi.readByComment(commentId)`
- `pageApi.readByPost(postId)`
- `pageApi.readByNode(nodeId)`
- `pageApi.readByCoursePath(courseId, path)`
- 使用场景：
  - 内容阅读页 (`ContentReadPage.vue`)
  - 节点帖子页 (`NodePostsPage.vue`)
  - 帖子详情页

**说明**: 这是一个聚合接口，返回渲染一个完整页面所需的所有数据，包括：
- 课程信息（父课程、当前课程、子课程列表）
- 目录结构（toc、tocNodeInfos）
- 当前节点信息
- 帖子列表（置顶帖子、固定帖子、其他帖子）
- 用户信息
- 学习状态

---

## 测试用例补充

### 帖子管理接口测试用例

#### 2.1 批量获取帖子
- ✅ 按ID列表查询
- ✅ 按节点ID查询
- ✅ 分页查询（使用lastScore和lastId）
- ✅ 未登录返回401

#### 2.2 创建帖子
- ✅ 成功创建文章类型帖子
- ✅ 成功创建目录类型帖子
- ✅ 未登录返回401
- ✅ 必填字段缺失返回400

#### 2.3 更新帖子
- ✅ 成功更新自己的帖子
- ✅ 无权限更新他人帖子返回403
- ✅ 未登录返回401

#### 2.4 删除帖子
- ✅ 成功删除自己的帖子
- ✅ 无权限删除他人帖子返回403
- ✅ 未登录返回401

#### 2.5 获取帖子详情
- ✅ 正常获取已发布帖子
- ✅ 帖子不存在返回404

#### 2.6 获取节点帖子列表
- ✅ 正常获取节点下的帖子
- ✅ 节点不存在返回空列表或404

#### 2.7 获取用户帖子列表
- ✅ 获取用户的文章列表
- ✅ 获取用户的目录列表
- ✅ 分页功能

### 页面聚合接口测试用例

#### 3.1 读取页面数据
- ✅ 按课程路径查询
- ✅ 按节点ID查询
- ✅ 按帖子ID查询
- ✅ 按评论ID查询
- ✅ 参数优先级正确
- ✅ 未登录返回401
- ✅ 资源不存在返回404
- ✅ 无参数返回400

---

## 4. 评论管理接口 (CommentsController)

### 基本信息
- **Rate Limit**: 40 requests/minute (per user)
- **Controller**: `CommentsController.java`
- **前端 API**: `web/src/api/modules/comment.ts`

### 4.1 创建评论

**接口路径**: `POST /api/v1/comments`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "objectId": 100,
  "objectType": 1,
  "content": "评论内容",
  "replyToCommentId": 0
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "objectId": 100,
    "objectType": 1,
    "content": "评论内容",
    "creatorId": 1000,
    "replyToCommentId": 0,
    "score": 0,
    "createTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `commentApi.createComment(commentData)`
- 使用场景：发表评论、回复评论

---

### 4.2 获取对象评论列表

**接口路径**: `GET /api/v1/comments`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| objectId | Long | 是 | 对象ID，必须大于0 |
| objectType | Integer | 是 | 对象类型（1=帖子，2=节点等），必须大于0 |
| lastScore | Double | 否 | 分页游标 - 上一页最后一条的分数 |
| lastId | Long | 否 | 分页游标 - 上一页最后一条的ID |

**排序规则**: 按 score 降序、id 降序（score 高的在前，score 相同时 id 大的在前）

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "objectId": 100,
        "content": "评论内容",
        "creator": {
          "id": 1000,
          "username": "用户名"
        },
        "score": 10.5,
        "helpful": 5,
        "unhelpful": 1,
        "replyCount": 3,
        "replies": []
      }
    ],
    "lastScore": 10.5,
    "lastId": 1,
    "hasMore": true
  }
}
```

**前端调用位置**:
- `commentApi.getComments(objectId, objectType, lastScore?, lastId?)`
- 使用场景：
  - 评论区 (`CommentSection.vue`)
  - 加载评论列表

---

### 4.3 获取评论回复列表

**接口路径**: `GET /api/v1/comments/{id}/replies`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 评论ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastScore | Double | 否 | 分页游标 - 上一页最后一条的分数 |
| lastId | Long | 否 | 分页游标 - 上一页最后一条的ID |

**排序规则**: 按 score 降序、id 降序

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 2,
        "objectId": 100,
        "content": "回复内容",
        "creator": {
          "id": 2000,
          "username": "回复者"
        },
        "replyToCommentId": 1,
        "score": 5.0
      }
    ],
    "lastScore": 5.0,
    "lastId": 2,
    "hasMore": false
  }
}
```

**前端调用位置**:
- `commentApi.getCommentReplies(commentId, lastScore?, lastId?)`
- 使用场景：展开查看评论的回复

---

## 5. 点赞管理接口 (UpvotesController)

### 基本信息
- **Rate Limit**: 100 requests/minute (per user)
- **Controller**: `UpvotesController.java`
- **前端 API**: `web/src/api/modules/upvote.ts`

### 5.1 点赞操作

**接口路径**: `POST /api/v1/upvotes`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "objectId": 100,
  "objectType": 1,
  "type": 1
}
```

**参数说明**:
- `objectType`: 1=帖子, 2=评论, 3=路线图, 4=记忆卡片组
- `type`: 1=helpful(赞同), 2=unhelpful(不赞同)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "objectId": 100,
    "objectType": 1,
    "helpful": 15,
    "unhelpful": 2,
    "userVoteType": 1
  }
}
```

**前端调用位置**:
- `upvoteApi.upvote(objectId, objectType, type)`
- 使用场景：
  - 帖子点赞 (`SinglePost.vue`)
  - 评论点赞
  - 路线图点赞
  - 记忆卡片组点赞

---

### 5.2 获取点赞状态

**接口路径**: `GET /api/v1/upvotes/status`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| objectId | Long | 是 | 对象ID，必须大于0 |
| objectType | Integer | 是 | 对象类型，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "objectId": 100,
    "objectType": 1,
    "helpful": 15,
    "unhelpful": 2,
    "userVoteType": 1
  }
}
```

**前端调用位置**:
- `upvoteApi.getUpvoteStatus(objectId, objectType)`
- 使用场景：查询用户对某个对象的点赞状态

---

## 6. 用户管理接口 (UsersController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user，部分接口有独立限流)
- **Controller**: `UsersController.java`
- **前端 API**: `web/src/api/modules/user.ts`, `web/src/api/modules/auth.ts`

### 6.1 获取当前用户信息

**接口路径**: `GET /api/v1/users/current`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "email": "user@example.com",
    "name": "显示名称",
    "avatar": "头像URL",
    "biography": "个人简介",
    "createTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `userApi.getCurrentUser()`
- 使用场景：获取登录用户的完整信息

---

### 6.2 更新当前用户信息

**接口路径**: `PUT /api/v1/users/current`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "name": "新的显示名称",
  "biography": "新的个人简介"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `userApi.updateCurrentUser(userData)`
- 使用场景：个人中心修改资料

---

### 6.3 更新用户头像

**接口路径**: `POST /api/v1/users/avatar`

**是否需要登录**: 是 (`@SaCheckLogin`)

**Rate Limit**: 5 requests/minute (per user)

**请求体**: `multipart/form-data`
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 头像图片文件 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "https://cdn.example.com/avatars/123.jpg"
}
```

**前端调用位置**:
- `userApi.updateAvatar(file)`
- 使用场景：上传和更新头像

---

### 6.4 获取用户信息（通过用户名）

**接口路径**: `GET /api/v1/users/{username}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名，不能为空 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "name": "显示名称",
    "avatar": "头像URL",
    "biography": "个人简介",
    "isFollowing": false
  }
}
```

**前端调用位置**:
- `userApi.getUserByUsername(username)`
- 使用场景：查看其他用户的公开信息

---

### 6.5 搜索用户

**接口路径**: `GET /api/v1/users/search`

**是否需要登录**: 否

**Rate Limit**: 30 requests/minute (per IP)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 搜索关键词，不能为空 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "username": "用户名",
      "name": "显示名称",
      "avatar": "头像URL"
    }
  ]
}
```

**前端调用位置**:
- `userApi.searchUsers(name)`
- 使用场景：搜索用户

---

### 6.6 用户注册

**接口路径**: `POST /api/v1/auth/register`

**是否需要登录**: 否

**Rate Limit**: 5 requests/minute (per IP)

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `authApi.register(email, password)`
- 使用场景：用户注册

---

### 6.7 用户登录

**接口路径**: `POST /api/v1/auth/login`

**是否需要登录**: 否

**Rate Limit**: 10 requests/minute (per IP)

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "name": "显示名称",
    "avatar": "头像URL"
  }
}
```

**前端调用位置**:
- `authApi.login(email, password)`
- 使用场景：用户登录

**说明**: 登录成功后会自动设置 Sa-Token 的登录状态

---

### 6.8 邮箱验证

**接口路径**: `POST /api/v1/auth/validate-email`

**是否需要登录**: 否

**Rate Limit**: 10 requests/minute (per IP)

**请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "用户名",
    "email": "user@example.com",
    "name": "显示名称",
    "avatar": "头像URL"
  }
}
```

**前端调用位置**:
- `authApi.validateEmail(email, code)`
- 使用场景：邮箱验证（验证成功后自动登录）

---

## 7. 学习进度接口 (ProgressController)

### 基本信息
- **Rate Limit**: 60 requests/minute (per user)
- **Controller**: `ProgressController.java`
- **前端 API**: `web/src/api/modules/progress.ts`

### 7.1 标记节点完成

**接口路径**: `POST /api/v1/progress/nodes/{nodeId}/complete`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**请求体**:
```json
{
  "courseId": 1
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nodeId": 10,
    "completed": true,
    "courseProgress": 25
  }
}
```

**前端调用位置**:
- `progressApi.markNodeCompleted(nodeId, courseId)`
- 使用场景：学习模式下标记节点完成

---

### 7.2 取消节点完成

**接口路径**: `DELETE /api/v1/progress/nodes/{nodeId}/complete`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**请求体**:
```json
{
  "courseId": 1
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "nodeId": 10,
    "completed": false,
    "courseProgress": 20
  }
}
```

**前端调用位置**:
- `progressApi.unmarkNodeCompleted(nodeId, courseId)`
- 使用场景：取消节点完成状态

---

### 7.3 检查节点完成状态

**接口路径**: `GET /api/v1/progress/nodes/{nodeId}/status`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 10,
    "name": "节点名称",
    "completed": true
  }
}
```

**前端调用位置**:
- `progressApi.getNodeStatus(nodeId)`
- 使用场景：查询节点完成状态

---

### 7.4 开始学习课程

**接口路径**: `POST /api/v1/progress/courses/{courseId}/start`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**前端调用位置**:
- `progressApi.startCourse(courseId)`
- 使用场景：开始学习某个课程

---

### 7.5 获取课程进度

**接口路径**: `GET /api/v1/progress/courses/{courseId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 100,
    "courseId": 1,
    "progressPercent": 25,
    "course": {
      "id": 1,
      "name": "课程名称"
    }
  }
}
```

**前端调用位置**:
- `progressApi.getCourseProgress(courseId)`
- 使用场景：查看某个课程的学习进度

---

### 7.6 获取所有课程进度

**接口路径**: `GET /api/v1/progress/courses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "courseId": 1,
      "progressPercent": 25,
      "course": {
        "id": 1,
        "name": "课程名称"
      }
    }
  ]
}
```

**前端调用位置**:
- `progressApi.getAllCoursesProgress(lastId?)`
- 使用场景：个人中心查看所有正在学习的课程

---

### 7.7 更新课程进度

**接口路径**: `PUT /api/v1/progress/courses/{courseId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**请求体**:
```json
{
  "progressPercent": 50
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "courseId": 1,
    "progressPercent": 50
  }
}
```

**前端调用位置**:
- `progressApi.updateCourseProgress(courseId, progressPercent)`
- 使用场景：手动更新课程进度

---

### 7.8 删除课程进度

**接口路径**: `DELETE /api/v1/progress/courses/{courseId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除成功"
}
```

**前端调用位置**:
- `progressApi.deleteCourseProgress(courseId)`
- 使用场景：停止学习某个课程

---

### 7.9 标记课程完成

**接口路径**: `POST /api/v1/progress/courses/{courseId}/complete`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": 1,
    "completed": true,
    "completedAt": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `progressApi.markCourseCompleted(courseId)`
- 使用场景：标记整个课程学习完成

---

### 7.10 开始学习路线图

**接口路径**: `POST /api/v1/progress/roadmaps/{roadmapId}/start`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**前端调用位置**:
- `progressApi.startRoadmap(roadmapId)`
- 使用场景：开始学习路线图

---

### 7.11 获取路线图进度

**接口路径**: `GET /api/v1/progress/roadmaps/{roadmapId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "roadmapId": 1,
    "progressPercent": 30,
    "roadmap": {
      "id": 1,
      "title": "路线图标题"
    }
  }
}
```

**前端调用位置**:
- `progressApi.getRoadmapProgress(roadmapId)`
- 使用场景：查看路线图学习进度

---

### 7.12 获取所有路线图进度

**接口路径**: `GET /api/v1/progress/roadmaps`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roadmapId": 1,
      "progressPercent": 30,
      "roadmap": {
        "id": 1,
        "title": "路线图标题"
      }
    }
  ]
}
```

**前端调用位置**:
- `progressApi.getAllRoadmapsProgress()`
- 使用场景：查看所有正在学习的路线图

---

### 7.13 更新路线图进度

**接口路径**: `PUT /api/v1/progress/roadmaps/{roadmapId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

**请求体**:
```json
{
  "progressPercent": 50
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "roadmapId": 1,
    "progressPercent": 50
  }
}
```

**前端调用位置**:
- `progressApi.updateRoadmapProgress(roadmapId, progressPercent)`
- 使用场景：更新路线图学习进度

---

### 7.14 删除路线图进度

**接口路径**: `DELETE /api/v1/progress/roadmaps/{roadmapId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roadmapId | Long | 是 | 路线图ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "删除成功"
}
```

**前端调用位置**:
- `progressApi.deleteRoadmapProgress(roadmapId)`
- 使用场景：停止学习路线图

---

## 8. 订阅管理接口 (SubscriptionsController)

### 基本信息
- **Rate Limit**: 60 requests/minute (per user)
- **Controller**: `SubscriptionsController.java`
- **前端 API**: `web/src/api/modules/course.ts` (subscriptionApi)

### 8.1 获取用户订阅列表

**接口路径**: `GET /api/v1/users/{userId}/subscriptions`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "courseId": 1,
      "courseName": "课程名称"
    }
  ]
}
```

**前端调用位置**:
- `subscriptionApi.getUserSubscriptions(userId)`
- 使用场景：查看用户订阅的课程列表

---

### 8.2 订阅课程

**接口路径**: `POST /api/v1/users/current/subscriptions`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "courseId": 1
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `subscriptionApi.subscribe(courseId)`
- 使用场景：订阅课程

---

### 8.3 批量更新订阅

**接口路径**: `PUT /api/v1/users/current/subscriptions`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "subscription": "1,2,3"
}
```

**参数说明**:
- `subscription`: 订阅的课程ID列表，逗号分隔

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "courseId": 1,
      "courseName": "课程1"
    },
    {
      "courseId": 2,
      "courseName": "课程2"
    }
  ]
}
```

**前端调用位置**:
- `subscriptionApi.updateSubscriptions(subscriptionIds)`
- 使用场景：批量管理订阅列表

---

### 8.4 取消订阅课程

**接口路径**: `DELETE /api/v1/users/current/subscriptions/{courseId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `subscriptionApi.unsubscribe(courseId)`
- 使用场景：取消订阅课程

---

## 9. 记忆卡片接口 (MemoryCardController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `MemoryCardController.java`
- **前端 API**: `web/src/api/modules/memory.ts`

### 9.1 创建卡片
**路径**: `POST /api/v1/memory/cards`
**登录**: 是
**请求体**: `{ "deckId": 1, "front": "问题", "back": "答案" }`
**前端**: `memoryApi.createCard(cardData)`

### 9.2 更新卡片
**路径**: `PUT /api/v1/memory/cards/{cardId}`
**登录**: 是
**前端**: `memoryApi.updateCard(cardId, cardData)`

### 9.3 获取节点下的卡片列表
**路径**: `GET /api/v1/memory/cards/node/{nodeId}`
**登录**: 是
**前端**: `memoryApi.getUserCardsByNode(nodeId)`

### 9.4 获取卡片内容差异
**路径**: `GET /api/v1/memory/cards/{cardId}/diff`
**登录**: 是
**前端**: `memoryApi.getCardDiff(cardId)`

### 9.5 删除卡片
**路径**: `DELETE /api/v1/memory/cards/{cardId}`
**登录**: 是
**前端**: `memoryApi.deleteCard(cardId)`

---

## 10. 记忆卡片组接口 (MemoryCardDeckController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `MemoryCardDeckController.java`
- **前端 API**: `web/src/api/modules/memory.ts`

### 10.1 获取帖子下的公共卡片组列表
**路径**: `GET /api/v1/memory/posts/{postId}/decks`
**登录**: 否
**查询参数**: `sortBy`, `sortOrder`, `lastScore`, `lastId`, `limit`
**前端**: `memoryApi.getPostPublicDecks(postId, ...)`
**使用场景**: 记忆卡片侧边栏 (`MemoryCardSidebar.vue`)

### 10.2 获取帖子创建者的卡片组
**路径**: `GET /api/v1/memory/posts/{postId}/creator-deck`
**登录**: 是
**前端**: `memoryApi.getPostCreatorDeck(postId)`

### 10.3 获取用户自己在帖子下提交的卡片组
**路径**: `GET /api/v1/memory/posts/{postId}/my-deck`
**登录**: 是
**前端**: `memoryApi.getMyPostDeck(postId)`

### 10.4 获取当前用户的所有卡片组
**路径**: `GET /api/v1/memory/users/me/memory-decks`
**登录**: 是
**查询参数**: `lastId`, `limit`
**前端**: `memoryApi.getCurrentUserAllDecks(lastId?, limit?)`

### 10.5 获取指定用户的卡片组
**路径**: `GET /api/v1/memory/users/{userId}/memory-decks`
**登录**: 是
**前端**: `memoryApi.getUserDecks(userId, lastId?, limit?)`

### 10.6 获取卡片组审核列表
**路径**: `GET /api/v1/memory/decks/review`
**登录**: 是
**查询参数**: `postId`, `creatorId`, `state`, `lastId`
**前端**: `memoryApi.getDecksForReview(...)`

### 10.7 获取卡片组详情
**路径**: `GET /api/v1/memory/decks/{deckId}`
**登录**: 是
**前端**: `memoryApi.getDeckDetail(deckId)`
**使用场景**: 卡片组详情对话框 (`DeckDetailDialog.vue`)

### 10.8 获取节点下的卡片组列表
**路径**: `GET /api/v1/memory/decks/node/{nodeId}`
**登录**: 是
**查询参数**: `lastScore`, `lastId`, `limit`
**前端**: `memoryApi.getDecksByNode(nodeId, ...)`

### 10.9 创建卡片组
**路径**: `POST /api/v1/memory/decks`
**登录**: 是
**请求体**: `{ "postId": 1, "title": "标题", "description": "描述", "cards": [...] }`
**前端**: `memoryApi.createDeck(deckData)`
**使用场景**: 创建卡片组对话框 (`CreateDeckDialog.vue`)

### 10.10 更新卡片组
**路径**: `PUT /api/v1/memory/decks/{deckId}`
**登录**: 是
**前端**: `memoryApi.updateDeck(deckId, deckData)`

### 10.11 获取卡片组更新差异
**路径**: `GET /api/v1/memory/decks/{deckId}/diff`
**登录**: 是
**查询参数**: `userCurrentVersion`
**前端**: `memoryApi.getDeckDiff(deckId, version?)`

### 10.12 接受卡片组更新
**路径**: `POST /api/v1/memory/decks/{deckId}/accept-changes`
**登录**: 是
**请求体**: `[cardId1, cardId2, ...]`
**前端**: `memoryApi.acceptDeckChanges(deckId, cardIds)`

### 10.13 整体替换卡片组中的所有卡片
**路径**: `PUT /api/v1/memory/decks/{deckId}/cards`
**登录**: 是
**前端**: `memoryApi.replaceAllCards(deckId, deckData)`

### 10.14 AI生成记忆卡片组
**路径**: `POST /api/v1/memory/decks/{postId}/ai-generate`
**登录**: 是
**前端**: `memoryApi.createAIDeck(postId)`

### 10.15 删除卡片组
**路径**: `DELETE /api/v1/memory/decks/{id}`
**登录**: 是
**前端**: `memoryApi.deleteDeck(id)`

---

## 11. 记忆库接口 (MemoryBankController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `MemoryBankController.java`
- **前端 API**: `web/src/api/modules/memory.ts`

### 11.1 添加卡片组到记忆库
**路径**: `POST /api/v1/memory/memory-bank/decks`
**登录**: 是
**请求体**: `{ "deckId": 1, "courseId": 1 }`
**前端**: `memoryApi.addDeckToMemoryBank({ deckId, courseId })`
**使用场景**: 添加卡片组到学习计划 (`DeckDetailDialog.vue`)

### 11.2 获取记忆库课程列表
**路径**: `GET /api/v1/memory/memory-bank/courses`
**登录**: 是
**查询参数**: `status` (可选)
**前端**: `memoryApi.getMemoryBankCourses(status?)`

### 11.3 更新课程复习策略
**路径**: `PUT /api/v1/memory/memory-bank/courses/{courseId}/settings`
**登录**: 是
**请求体**: `{ "dailyNewCards": 20, "dailyReviewCards": 100 }`
**前端**: `memoryApi.updateCourseSetting(courseId, settings)`

### 11.4 从记忆库移除卡片组
**路径**: `DELETE /api/v1/memory/memory-bank/courses/{courseId}/decks/{deckId}`
**登录**: 是
**前端**: `memoryApi.removeDeckFromCourse(courseId, deckId)`

---

## 12. 目录管理接口 (TocController)

### 基本信息
- **Rate Limit**: 60 requests/minute (per user)
- **Controller**: `TocController.java`
- **前端 API**: `web/src/api/modules/course.ts`

### 12.1 更新用户课程目录

**接口路径**: `PUT /api/v1/users/current/courses/{courseId}/toc`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**请求体**:
```json
{
  "indexArray": "1,2,3,0,4"
}
```

**参数说明**:
- `indexArray`: 目录索引数组，逗号分隔，0表示使用默认目录

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "目录更新成功"
}
```

**前端调用位置**:
- `courseApi.updateUserCourseToc(courseId, indexArray)`
- 使用场景：配置目录对话框 (`ConfigContentsDialog.vue`)

---

### 12.2 获取用户课程目录

**接口路径**: `GET /api/v1/users/current/courses/{courseId}/toc`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "hash1,hash2,hash3"
}
```

**前端调用位置**:
- `tocApi.getUserCourseToc(courseId)`
- 使用场景：获取用户自定义的课程目录

---

## 13. 内容管理接口 (ContentsController)

### 基本信息
- **Rate Limit**: 80 requests/minute (per user)
- **Controller**: `ContentsController.java`
- **前端 API**: `web/src/api/modules/post.ts` (postApi.operateContent)

### 13.1 内容操作（选择目录、置顶等）

**接口路径**: `POST /api/v1/contents`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "path": "1-10",
  "courseId": 1,
  "postingId": 100,
  "action": 1
}
```

**参数说明**:
- `action`: 1=设置为目录, 2=取消设置为目录, 3=置顶, 4=取消置顶

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `postApi.operateContent({ path, courseId, postingId, action })`
- 使用场景：管理内容的展示方式（选择目录、置顶）

---

## 14. 统计数据接口 (StatsController)

### 基本信息
- **Rate Limit**: 100 requests/minute (per user)
- **Controller**: `StatsController.java`
- **前端 API**: `web/src/api/modules/stats.ts`

### 14.1 记录访问
**路径**: `POST /api/v1/stats/views`
**登录**: 否
**请求体**: `{ "articleId": 1, "userId": 100 }`
**前端**: `statsApi.recordView(articleId, userId)`

### 14.2 获取用户今日统计
**路径**: `GET /api/v1/stats/users/{userId}/today`
**登录**: 否
**前端**: `statsApi.getUserTodayStats(userId)`

### 14.3 获取用户昨日统计
**路径**: `GET /api/v1/stats/users/{userId}/yesterday`
**登录**: 否
**前端**: `statsApi.getUserYesterdayStats(userId)`

### 14.4 获取用户历史统计
**路径**: `GET /api/v1/stats/users/{userId}/history`
**登录**: 否
**查询参数**: `days` (默认7)
**前端**: `statsApi.getUserHistoryStats(userId, days?)`

### 14.5 获取用户时间段统计
**路径**: `GET /api/v1/stats/users/{userId}/period`
**登录**: 否
**查询参数**: `days` (默认7)
**前端**: `statsApi.getUserPeriodStats(userId, days?)`

### 14.6 获取用户全部时间统计
**路径**: `GET /api/v1/stats/users/{userId}/all-time`
**登录**: 否
**前端**: `statsApi.getUserAllTimeStats(userId)`

### 14.7 手动同步统计数据
**路径**: `POST /api/v1/stats/sync/manual`
**登录**: 否
**前端**: `statsApi.manualSync()`

### 14.8 获取系统健康状态
**路径**: `GET /api/v1/stats/health`
**登录**: 否
**前端**: `statsApi.getHealthStatus()`

### 14.9 同步指定日期的统计数据
**路径**: `POST /api/v1/stats/sync/date`
**登录**: 否
**请求体**: `{ "date": "2024-01-01" }`
**前端**: `statsApi.syncByDate(date)`

### 14.10 获取平台统计数据
**路径**: `GET /api/v1/stats/platform`
**登录**: 否
**前端**: `statsApi.getPlatformStats()`

---

## 15. 路线图接口 (RoadmapsController)

### 基本信息
- **Rate Limit**: 40 requests/minute (per user)
- **Controller**: `RoadmapsController.java`
- **前端 API**: `web/src/api/modules/roadmap.ts`

### 15.1 获取职业路线图列表
**路径**: `GET /api/v1/professions/{professionId}/roadmaps`
**登录**: 是
**查询参数**: `lastId` (可选)
**前端**: `roadmapApi.getRoadmapsByProfession(professionId, lastId?)`

### 15.2 创建路线图
**路径**: `POST /api/v1/roadmaps`
**登录**: 是
**请求体**: `{ "professionId": 1, "content": "路线图内容", "description": "描述" }`
**前端**: `roadmapApi.createRoadmap(roadmapData)`

### 15.3 更新路线图
**路径**: `PUT /api/v1/roadmaps/{id}`
**登录**: 是
**请求体**: `{ "content": "更新后的内容" }`
**前端**: `roadmapApi.updateRoadmap(id, content)`

### 15.4 获取路线图详情
**路径**: `GET /api/v1/roadmaps/{id}`
**登录**: 是
**前端**: `roadmapApi.getRoadmap(id)`

### 15.5 置顶路线图
**路径**: `POST /api/v1/roadmaps/pin`
**登录**: 是
**请求体**: `{ "professionId": 1, "roadmapId": 10 }`
**前端**: `roadmapApi.pinRoadmap(professionId, roadmapId)`

### 15.6 获取当前用户的路线图
**路径**: `GET /api/v1/users/me/roadmaps`
**登录**: 是
**查询参数**: `lastId` (可选)
**前端**: `roadmapApi.getCurrentUserRoadmaps(lastId?)`

### 15.7 获取指定用户的路线图
**路径**: `GET /api/v1/users/{userId}/roadmaps`
**登录**: 否
**查询参数**: `lastId` (必填)
**前端**: `roadmapApi.getUserRoadmaps(userId, lastId)`

### 15.8 删除路线图
**路径**: `DELETE /api/v1/roadmaps/{id}`
**登录**: 是
**前端**: `roadmapApi.deleteRoadmap(id)`

---

## 16. 图片上传接口 (ImageUploadController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `ImageUploadController.java`
- **前端 API**: `web/src/api/modules/image.ts`

### 16.1 上传图片

**接口路径**: `POST /api/v1/images/upload`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**: `multipart/form-data`
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件 |
| refType | String | 是 | 引用类型：post/comment/avatar/course/roadmap |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileUrl": "https://cdn.example.com/images/123.jpg",
    "fileKey": "images/123.jpg",
    "fileSize": 102400
  }
}
```

**前端调用位置**:
- `imageApi.upload(file, refType)`
- 使用场景：编辑器上传图片 (`ArticleEditor.vue`)

---

### 16.2 标记图片为使用中

**接口路径**: `POST /api/v1/images/mark-used`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "fileUrls": [
    "https://cdn.example.com/images/123.jpg",
    "https://cdn.example.com/images/456.jpg"
  ]
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `imageApi.markAsUsed(fileUrls)`
- 使用场景：发布文章时标记图片为使用中

---

### 16.3 删除图片

**接口路径**: `DELETE /api/v1/images`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileUrl | String | 是 | 图片URL，不能为空 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `imageApi.delete(fileUrl)`
- 使用场景：删除未使用的图片

---

### 16.4 获取配额使用情况

**接口路径**: `GET /api/v1/images/quota`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "used": 10485760,
    "total": 104857600,
    "remaining": 94371840,
    "percentage": 10.0
  }
}
```

**前端调用位置**:
- `imageApi.getQuota()`
- 使用场景：显示存储空间使用情况

---

### 16.5 获取上传历史

**接口路径**: `GET /api/v1/images/history`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 20 | 返回数量 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "fileUrl": "https://cdn.example.com/images/123.jpg",
      "fileSize": 102400,
      "refType": "post",
      "status": "used",
      "uploadTime": "2024-01-01T00:00:00"
    }
  ]
}
```

**前端调用位置**:
- `imageApi.getHistory(limit?)`
- 使用场景：图片管理页面

---

## 接口文档总结

### 已完成的 Controller（共24个）

1. **CoursesController** - 7个接口（课程管理）
17. **ProfessionsController** - 6个接口（职业管理）
2. **PostsController** - 8个接口（帖子管理）
3. **PagesController** - 1个接口（页面聚合）
4. **CommentsController** - 3个接口（评论管理）
5. **UpvotesController** - 2个接口（点赞管理）
6. **UsersController** - 8个接口（用户管理）
7. **ProgressController** - 14个接口（学习进度）
8. **SubscriptionsController** - 4个接口（订阅管理）
9. **MemoryCardController** - 5个接口（记忆卡片）
10. **MemoryCardDeckController** - 15个接口（记忆卡片组）
11. **MemoryBankController** - 4个接口（记忆库）
12. **TocController** - 2个接口（目录管理）
13. **ContentsController** - 1个接口（内容操作）
14. **StatsController** - 10个接口（统计数据）
15. **RoadmapsController** - 8个接口（路线图）
16. **ImageUploadController** - 5个接口（图片上传）
18. **FollowsController** - 3个接口（关注管理）
19. **MessagesController** - 7个接口（消息管理）
20. **ConfigController** - 1个接口（系统配置）
21. **PublicController** - 6个接口（公开接口）
22. **UserStatsController** - 4个接口（用户统计）
23. **ReviewController** - 4个接口（复习管理）
24. **AdminAutoAuthorController** - 4个接口（AutoAuthor管理）

**总计：133个接口已完成文档**

### 弃用的 Controller

- **AiController** - AI聊天功能（接口已注释，前端不使用）

---

## 下一步

现在已经完成了 **133个接口** 的文档，覆盖了系统的 **全部24个Controller**，包括：

### 核心功能模块
- ✅ 课程管理（课程、订阅、目录、内容操作）
- ✅ 帖子管理（帖子、评论、点赞）
- ✅ 用户系统（用户、认证、关注、消息）
- ✅ 学习进度（课程进度、路线图进度）
- ✅ 记忆卡片（卡片、卡片组、记忆库、复习）
- ✅ 统计系统（用户统计、平台统计）
- ✅ 路线图（职业、路线图）
- ✅ 公开接口（匿名访问）
- ✅ 管理后台（AutoAuthor管理）

### 建议的后续工作

1. **编写自动化测试**
   - 基于此文档编写集成测试
   - 使用 JUnit + MockMvc 测试所有接口
   - 验证参数校验、认证授权、错误处理

2. **性能测试**
   - 对高频接口进行压力测试
   - 验证限流配置是否合理
   - 检查分页接口的性能

3. **安全审计**
   - 检查所有需要登录的接口是否正确标注 @SaCheckLogin
   - 验证敏感操作的权限控制
   - 检查输入参数的安全性验证

4. **API 版本管理**
   - 当前所有接口都是 v1 版本
   - 未来如需重大变更，可添加 v2 版本

5. **生成 OpenAPI/Swagger 文档**
   - 使用 SpringDoc 或 Swagger 生成交互式 API 文档
   - 方便前端开发和第三方集成

---

## 17. 职业管理接口 (ProfessionsController)

### 基本信息
- **Rate Limit**: 40 requests/minute (per user)
- **Controller**: `ProfessionsController.java`
- **前端 API**: `web/src/api/modules/profession.ts`

### 17.1 获取职业列表

**接口路径**: `GET /api/v1/professions`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "软件工程师",
      "description": "职业描述",
      "state": 3
    }
  ]
}
```

**前端调用位置**:
- `professionApi.getProfessions(lastId?)`
- 使用场景：职业列表页

---

### 17.2 获取已审核职业列表

**接口路径**: `GET /api/v1/professions/approved`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "软件工程师",
      "description": "职业描述",
      "state": 3
    }
  ]
}
```

**前端调用位置**:
- `professionApi.getApprovedProfessions(lastId?)`
- 使用场景：公开显示的职业列表

---

### 17.3 获取职业详情

**接口路径**: `GET /api/v1/professions/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "软件工程师",
    "description": "职业描述",
    "state": 3,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  }
}
```

**前端调用位置**:
- `professionApi.getProfession(id)`
- 使用场景：职业详情页

---

### 17.4 创建职业

**接口路径**: `POST /api/v1/professions`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "name": "职业名称",
  "description": "职业描述"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "职业创建成功"
}
```

**前端调用位置**:
- `professionApi.createProfession(professionData)`
- 使用场景：创建新职业

---

### 17.5 删除职业

**接口路径**: `DELETE /api/v1/professions/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "职业删除成功"
}
```

**前端调用位置**:
- `professionApi.deleteProfession(id)`
- 使用场景：删除职业

---

### 17.6 获取热门职业

**接口路径**: `GET /api/v1/professions/hot`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "软件工程师",
      "description": "职业描述",
      "roadmapCount": 5
    }
  ]
}
```

**前端调用位置**:
- `professionApi.getHotProfessions(limit?)`
- 使用场景：首页热门职业展示

---

## 18. 关注管理接口 (FollowsController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `FollowsController.java`
- **前端 API**: `web/src/api/modules/follow.ts`

### 18.1 关注用户

**接口路径**: `POST /api/v1/follows`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "followeeId": 100
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `followApi.follow(followeeId)`
- 使用场景：关注用户

---

### 18.2 取消关注

**接口路径**: `DELETE /api/v1/follows/{followeeId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| followeeId | Long | 是 | 被关注用户ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `followApi.unfollow(followeeId)`
- 使用场景：取消关注用户

---

### 18.3 获取用户关注列表

**接口路径**: `GET /api/v1/users/{userId}/followees`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 100,
      "username": "用户名",
      "name": "显示名称",
      "avatar": "头像URL",
      "isFollowing": true
    }
  ]
}
```

**前端调用位置**:
- `followApi.getUserFollowees(userId, lastId?)`
- 使用场景：查看用户关注的人列表

---

## 19. 消息管理接口 (MessagesController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `MessagesController.java`
- **前端 API**: `web/src/api/modules/message.ts`

### 19.1 申请课程

**接口路径**: `POST /api/v1/messages/course-applications`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "courseId": 1,
  "message": "申请理由"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `messageApi.applyCourse(courseId, message)`
- 使用场景：申请成为课程贡献者

---

### 19.2 获取系统消息

**接口路径**: `GET /api/v1/messages/system`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "system",
      "content": "消息内容",
      "isRead": false,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

**前端调用位置**:
- `messageApi.getSystemMessages(lastId?)`
- 使用场景：系统消息列表

---

### 19.3 按分类获取消息

**接口路径**: `GET /api/v1/messages/category`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| category | String | 是 | - | 消息分类 |
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "category": "course_application",
      "content": "消息内容",
      "isRead": false,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

**前端调用位置**:
- `messageApi.getMessagesByCategory(category, lastId?)`
- 使用场景：按分类查看消息

---

### 19.4 获取消息列表

**接口路径**: `GET /api/v1/messages`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | 0 | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "system",
      "content": "消息内容",
      "isRead": false,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

**前端调用位置**:
- `messageApi.getMessages(lastId?)`
- 使用场景：消息中心

---

### 19.5 发送系统消息

**接口路径**: `POST /api/v1/messages/system`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "userId": 100,
  "content": "消息内容"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `messageApi.sendSystemMessage(userId, content)`
- 使用场景：管理员发送系统消息

---

### 19.6 修改课程申请

**接口路径**: `PUT /api/v1/messages/course-applications/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 申请ID，必须大于0 |

**请求体**:
```json
{
  "status": "approved"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `messageApi.updateCourseApplication(id, status)`
- 使用场景：审批课程申请

---

### 19.7 邀请用户

**接口路径**: `POST /api/v1/messages/invite`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "userId": 100,
  "courseId": 1,
  "message": "邀请信息"
}
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `messageApi.inviteUser(userId, courseId, message)`
- 使用场景：邀请用户参与课程

---

## 20. 系统配置接口 (ConfigController)

### 基本信息
- **Rate Limit**: 100 requests/minute (per IP)
- **Controller**: `ConfigController.java`
- **前端 API**: `web/src/api/modules/config.ts`

### 20.1 获取验证规则配置

**接口路径**: `GET /api/v1/config/validation`

**是否需要登录**: 否

**响应头**:
- 支持 ETag 缓存机制
- 当配置未改变时返回 304 Not Modified

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "password": {
      "minLength": 8,
      "maxLength": 32,
      "requireUppercase": true,
      "requireLowercase": true,
      "requireNumber": true,
      "requireSpecialChar": true
    },
    "username": {
      "minLength": 3,
      "maxLength": 20,
      "allowedChars": "[a-zA-Z0-9_-]"
    },
    "email": {
      "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    }
  }
}
```

**前端调用位置**:
- `configApi.getValidationRules()`
- 使用场景：表单验证规则配置

**说明**: 此接口支持 ETag 缓存，客户端应保存 ETag 值并在后续请求中通过 If-None-Match 头发送，以减少不必要的数据传输。

---

## 21. 公开接口 (PublicController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per IP)
- **Controller**: `PublicController.java`
- **前端 API**: `web/src/api/modules/public.ts`
- **特点**: 所有接口无需登录，支持匿名访问

### 21.1 获取课程分类数据

**接口路径**: `GET /api/v1/public/course-categories`

**是否需要登录**: 否

**响应头**:
- 支持 ETag 缓存机制
- 当配置未改变时返回 304 Not Modified

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      {
        "id": 1,
        "name": "编程",
        "subCategories": [
          { "id": 101, "name": "前端开发" },
          { "id": 102, "name": "后端开发" }
        ]
      }
    ]
  }
}
```

**前端调用位置**:
- `publicApi.getCourseCategories()`
- 使用场景：课程分类筛选器

---

### 21.2 获取职业分类数据

**接口路径**: `GET /api/v1/public/profession-categories`

**是否需要登录**: 否

**响应头**:
- 支持 ETag 缓存机制
- 当配置未改变时返回 304 Not Modified

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      {
        "id": 1,
        "name": "技术类",
        "professions": [
          { "id": 101, "name": "软件工程师" }
        ]
      }
    ]
  }
}
```

**前端调用位置**:
- `publicApi.getProfessionCategories()`
- 使用场景：职业分类筛选器

---

### 21.3 查询只读模式状态

**接口路径**: `GET /api/v1/public/readonly-mode`

**是否需要登录**: 否

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enabled": false
  }
}
```

**前端调用位置**:
- `publicApi.getReadOnlyMode()`
- 使用场景：系统维护时禁用写操作

---

### 21.4 获取职业详情（公开）

**接口路径**: `GET /api/v1/public/professions/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "软件工程师",
    "description": "职业描述",
    "state": 3
  }
}
```

**前端调用位置**:
- `publicApi.getProfession(id)`
- 使用场景：未登录用户查看职业详情

---

### 21.5 获取职业的路线图列表（公开）

**接口路径**: `GET /api/v1/public/professions/{professionId}/roadmaps`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| professionId | Long | 是 | 职业ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | - | 分页游标 |
| pageSize | Integer | 否 | 20 | 每页数量 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "路线图标题",
      "description": "路线图描述",
      "creatorId": 100
    }
  ]
}
```

**前端调用位置**:
- `publicApi.getProfessionRoadmaps(professionId, lastId?, pageSize?)`
- 使用场景：未登录用户浏览路线图

---

### 21.6 读取页面数据（公开）

**接口路径**: `GET /api/v1/public/pages/read`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 是 | 课程ID，必须大于0 |
| path | String | 否 | 课程路径 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "course": { "id": 1, "name": "课程名称" },
    "node": { "id": 10, "name": "节点名称" },
    "toc": [],
    "otherPostings": [],
    "learning": false
  }
}
```

**前端调用位置**:
- `publicApi.readPage(courseId, path?)`
- 使用场景：未登录用户浏览课程内容

**说明**: 返回的数据不包含个性化信息，学习进度、订阅状态等均为默认值

---

## 22. 用户统计接口 (UserStatsController)

### 基本信息
- **Rate Limit**: 无特定限流（继承全局限流）
- **Controller**: `UserStatsController.java`
- **前端 API**: `web/src/api/modules/userStats.ts`

### 22.1 获取用户统计数据

**接口路径**: `GET /api/v1/users/{userId}/stats`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "views": 1000,
    "twices": 50,
    "likes": 200,
    "comments": 30,
    "learningCourses": 5,
    "completedCourses": 10,
    "createdArticles": 20,
    "createdIndexs": 5,
    "createdRoadmaps": 2,
    "createdCardDecks": 15
  }
}
```

**前端调用位置**:
- `userStatsApi.getUserStats(userId)`
- 使用场景：个人主页展示统计数据

---

### 22.2 批量获取用户统计

**接口路径**: `POST /api/v1/users/stats/batch`

**是否需要登录**: 否

**请求体**:
```json
[100, 101, 102, 103]
```

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "100": { "userId": 100, "views": 500 },
    "101": { "userId": 101, "views": 300 }
  }
}
```

**前端调用位置**:
- `userStatsApi.batchGetUserStats(userIds)`
- 使用场景：排行榜批量获取用户统计

**说明**: 限制最多查询100个用户

---

### 22.3 获取排行榜 Top N 用户

**接口路径**: `GET /api/v1/users/stats/top`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| field | String | 否 | views | 排序字段（views/twices/likes/comments） |
| limit | Integer | 否 | 10 | 返回数量，最大100 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 1,
      "views": 5000,
      "twices": 200
    }
  ]
}
```

**前端调用位置**:
- `userStatsApi.getTopUsers(field?, limit?)`
- 使用场景：用户排行榜

---

### 22.4 获取统计字段列表

**接口路径**: `GET /api/v1/users/stats/fields`

**是否需要登录**: 否

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dailyStats": {
      "views": "总浏览量",
      "twices": "总两次能懂点赞数",
      "likes": "总有用点赞数",
      "comments": "总评论数"
    },
    "learningStats": {
      "learning_courses": "正在学习课程数",
      "completed_courses": "已完成课程数"
    }
  }
}
```

**前端调用位置**:
- `userStatsApi.getStatFields()`
- 使用场景：前端字段映射和说明

---

## 23. 复习管理接口 (ReviewController)

### 基本信息
- **Rate Limit**: 50 requests/minute (per user)
- **Controller**: `ReviewController.java`
- **前端 API**: `web/src/api/modules/review.ts`

### 23.1 获取复习队列

**接口路径**: `GET /api/v1/memory/review/queue`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| courseId | Long | 否 | 课程ID，筛选特定课程的卡片 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "card": {
        "id": 1,
        "front": "问题",
        "back": "答案",
        "deckId": 10
      },
      "srs": {
        "easeFactor": 2.5,
        "interval": 1,
        "nextReviewAt": "2024-01-01T00:00:00"
      }
    }
  ]
}
```

**前端调用位置**:
- `reviewApi.getReviewQueue(courseId?)`
- 使用场景：记忆卡片复习页面

**说明**: 只返回到期需要复习的卡片，最多100个

---

### 23.2 获取卡片列表

**接口路径**: `GET /api/v1/memory/review/cards`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| courseId | Long | 否 | - | 课程ID |
| limit | Integer | 否 | 20 | 返回数量，最大100 |
| lastId | Long | 否 | - | 分页游标 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "card": { "id": 1, "front": "问题", "back": "答案" },
      "srs": { "interval": 3, "nextReviewAt": "2024-01-05T00:00:00" }
    }
  ]
}
```

**前端调用位置**:
- `reviewApi.getCardList(courseId?, limit?, lastId?)`
- 使用场景：查看所有卡片（不限到期状态）

---

### 23.3 提交复习结果

**接口路径**: `POST /api/v1/memory/review/submit`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "cardId": 1,
  "quality": 4,
  "reviewTime": "2024-01-01T12:00:00"
}
```

**参数说明**:
- `quality`: 复习质量评分（0-5），5最好，0最差

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `reviewApi.submitReview(reviewData)`
- 使用场景：用户完成卡片复习后提交结果

---

### 23.4 获取复习统计

**接口路径**: `GET /api/v1/memory/review/stats`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| period | String | 否 | WEEK | 统计周期（DAY/WEEK/MONTH/YEAR） |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCards": 100,
    "reviewedCards": 50,
    "dueCards": 20,
    "averageQuality": 4.2,
    "dailyReviewCount": [5, 10, 8, 12, 15, 20, 18]
  }
}
```

**前端调用位置**:
- `reviewApi.getReviewStats(period?)`
- 使用场景：复习统计图表

---

## 24. AutoAuthor 管理接口 (AdminAutoAuthorController)

### 基本信息
- **Rate Limit**: 30 requests/minute (per user)
- **Controller**: `AdminAutoAuthorController.java`
- **前端 API**: `web/src/api/modules/admin.ts`
- **特点**: 管理员专用接口

### 24.1 扫描节点

**接口路径**: `POST /api/v1/admin/auto-author/scan`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `adminApi.scanAutoAuthor()`
- 使用场景：管理后台扫描需要AI生成内容的节点

**说明**: 扫描最多10页，将需要生成内容的节点加入队列

---

### 24.2 将节点加入队列

**接口路径**: `POST /api/v1/admin/auto-author/enqueue/{nodeId}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| nodeId | Long | 是 | 节点ID，必须大于0 |

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `adminApi.enqueueNode(nodeId)`
- 使用场景：手动指定节点生成内容

---

### 24.3 重置生成会话

**接口路径**: `POST /api/v1/admin/auto-author/session/reset`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**前端调用位置**:
- `adminApi.resetAutoAuthorSession()`
- 使用场景：重置AI生成会话状态

---

### 24.4 清空生成队列

**接口路径**: `DELETE /api/v1/admin/auto-author/queue`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": "已清空队列，共删除 10 个待处理节点"
}
```

**前端调用位置**:
- `adminApi.clearAutoAuthorQueue()`
- 使用场景：清空待生成内容的队列

---

## 待补充的其他 Controller 接口

- AiController (AI功能) - 已弃用，接口被注释

