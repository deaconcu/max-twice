# 课程管理接口文档 (CoursesController)

## 基本信息

- **Controller**: `CoursesController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 40 requests/minute (per user)
- **前端 API**: `web/src/api/modules/course.ts`

## DTO 类型说明

### CourseBriefDTO (课程简要信息)
用途：极简课程信息，仅包含 ID 和名称

```json
{
  "id": 1,
  "name": "课程名称"
}
```

**字段说明**：
- `id` (Long): 课程ID
- `name` (String): 课程名称

**使用场景**：
- 课程搜索结果列表
- 作为父课程引用（嵌套在其他DTO中）

---

### CourseSummaryDTO (课程摘要信息)
用途：课程列表信息，包含基础描述和分类

```json
{
  "id": 1,
  "name": "课程名称",
  "description": "课程描述",
  "mainCategory": 1,
  "subCategory": 2
}
```

**字段说明**：
- `id` (Long): 课程ID
- `name` (String): 课程名称
- `description` (String): 课程描述（可能包含 Markdown 格式）
- `mainCategory` (Integer): 主分类ID（如：编程、设计、商业等）
- `subCategory` (Integer): 子分类ID（如：Java、Python、前端等）

**使用场景**：
- 子课程列表展示
- 课程分类浏览列表

---

### CourseDetailDTO (课程详情)
用途：完整课程详情信息，包含所有管理和状态字段

**继承关系**: CourseDetailDTO extends CourseSummaryDTO

```json
{
  "id": 608,
  "name": "高三政治",
  "description": "高三政治相关的专业课程和实践内容",
  "mainCategory": 10,
  "subCategory": 9,
  "creatorId": 83,
  "rootNodeId": 10,
  "parentCourseId": 0,
  "parentCourse": null,
  "state": 2,
  "reason": "",
  "createdAt": "2025-08-11 13:36:06",
  "updatedAt": "2025-10-24 03:07:40"
}
```

**完整字段说明**：

**从 CourseSummaryDTO 继承的字段**：
- `id` (Long): 课程ID
- `name` (String): 课程名称
- `description` (String): 课程描述
- `mainCategory` (Integer): 主分类ID
- `subCategory` (Integer): 子分类ID

**CourseDetailDTO 特有字段**：
- `creatorId` (Long): 创建者ID，用于权限校验和显示创建者信息
- `rootNodeId` (Long): 根节点ID，用于构建课程内容树
- `parentCourseId` (Long): 父课程ID
  - `0`: 表示是根课程
  - `> 0`: 表示是子课程，指向父课程的ID
- `parentCourse` (CourseBriefDTO): 父课程信息
  - 当 `parentCourseId > 0` 时填充，包含父课程的 id 和 name
  - 当 `parentCourseId = 0` 时为 `null`
- `state` (Byte): 课程状态
  - `0`: SUBMITTED (待审核)
  - `1`: PUBLISHED (已发布)
  - `2`: REJECTED (已拒绝)
  - `3`: BANNED (已封禁)
- `reason` (String): 审核原因
  - 仅在课程被拒绝或封禁时填充
  - 其他状态为空字符串或 null
- `createdAt` (String): 创建时间，格式：`yyyy-MM-dd HH:mm:ss`
- `updatedAt` (String): 更新时间，格式：`yyyy-MM-dd HH:mm:ss`

**使用场景**：
- 课程详情页面
- 课程编辑页面
- 管理后台课程审核
- 课程列表展示（包含完整信息）

---

### CourseWithStatsDTO (带统计信息的课程)
用途：热门课程排行榜，显示课程的受欢迎程度

**继承关系**: CourseWithStatsDTO extends CourseSummaryDTO

```json
{
  "id": 1,
  "name": "课程名称",
  "description": "课程描述",
  "mainCategory": 1,
  "subCategory": 2,
  "learnerCount": 100,
  "subscriptionCount": 50
}
```

**完整字段说明**：

**从 CourseSummaryDTO 继承的字段**：
- `id` (Long): 课程ID
- `name` (String): 课程名称
- `description` (String): 课程描述
- `mainCategory` (Integer): 主分类ID
- `subCategory` (Integer): 子分类ID

**CourseWithStatsDTO 特有字段**：
- `learnerCount` (Integer): 学习人数，当前正在学习该课程的用户数量
  - 数据源：从 Redis 排行榜服务动态查询
- `subscriptionCount` (Integer): 订阅人数（收藏人数）
  - 数据源：从 Redis 排行榜服务动态查询

**使用场景**：
- 热门课程排行榜
- 课程推荐列表

---

### CourseSummaryWithStatsAndProgressDTO (课程摘要+统计+进度)
用途：课程详情页面，包含课程基本信息、统计数据和用户学习状态

**继承关系**: CourseSummaryWithStatsAndProgressDTO extends CourseSummaryDTO

```json
{
  "id": 608,
  "name": "高三政治",
  "description": "高三政治相关的专业课程和实践内容",
  "mainCategory": 10,
  "subCategory": 9,
  "learnerCount": 150,
  "subscriptionCount": 89,
  "subscribed": true,
  "progress": 45
}
```

**完整字段说明**：

**从 CourseSummaryDTO 继承的字段**：
- `id` (Long): 课程ID
- `name` (String): 课程名称
- `description` (String): 课程描述
- `mainCategory` (Integer): 主分类ID
- `subCategory` (Integer): 子分类ID

**统计字段**（动态查询）：
- `learnerCount` (Integer): 学习人数，当前正在学习该课程的用户数量
  - 数据源：从 ContentStatsDataService 查询
  - 未登录/无数据时：0
- `subscriptionCount` (Integer): 订阅人数（收藏人数）
  - 数据源：从 ContentStatsDataService 查询
  - 未登录/无数据时：0

**用户个人字段**（需要登录）：
- `subscribed` (Boolean): 是否已订阅该课程
  - 登录时：根据用户订阅关系动态查询
  - 未登录时：false
- `progress` (Integer): 学习进度百分比（0-100）
  - 登录时：根据用户学习记录动态计算
  - 未登录时：0

**使用场景**：
- `CourseDetailPage.vue` - 课程详情页面（主要使用场景）
- 需要同时展示课程信息、热度统计和用户学习状态的场景

**注意**：
- 此 DTO 不包含管理字段（state, creatorId, reason 等）
- 支持未登录访问，未登录时用户字段返回默认值
- 统计字段实时从 Redis 查询，用户字段实时从数据库查询

---

## 接口列表

## 1. 获取课程详情

**接口路径**: `GET /api/v1/courses/{id}`

**是否需要登录**: 否（登录后会返回用户个人数据）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 课程ID，必须大于0 |

**返回类型**: `CourseSummaryWithStatsAndProgressDTO`

**字段说明**:
- **基础字段**（继承自 CourseSummaryDTO）:
  - `id`: 课程ID
  - `name`: 课程名称
  - `description`: 课程描述
  - `mainCategory`: 主分类ID
  - `subCategory`: 子分类ID
- **统计字段**:
  - `learnerCount`: 当前学习人数
  - `subscriptionCount`: 订阅（收藏）人数
- **用户字段**（需要登录）:
  - `subscribed`: 是否已订阅（未登录时为 false）
  - `progress`: 学习进度百分比 0-100（未登录时为 0）

**返回示例**:
```json
{
  "code": 200,
  "data": {
    "id": 608,
    "name": "高三政治",
    "description": "高三政治相关的专业课程和实践内容",
    "mainCategory": 10,
    "subCategory": 9,
    "learnerCount": 150,
    "subscriptionCount": 89,
    "subscribed": true,
    "progress": 45
  }
}
```

**前端调用**:
```typescript
// API 调用
courseApi.getCourse(id)

// 实际使用 (CourseDetailPage.vue:493)
const { data: course } = useFetch<Course>({
  fetchFn: () => courseApi.getCourse(courseId.value),
  immediate: true
})
```

**使用场景**:
- `CourseDetailPage.vue` - 课程详情页面

---

## 2. 搜索课程

**接口路径**: `GET /api/v1/courses/search`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 搜索关键词，不能为空 |

**返回类型**: `List<CourseBriefDTO>`

**返回示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Java基础课程"
    },
    {
      "id": 2,
      "name": "Java进阶课程"
    }
  ]
}
```

**前端调用**:
```typescript
// API 调用
courseApi.searchCourses(name)

// 实际使用 (CourseListPage.vue:444)
const handleSearch = async () => {
  const response = await courseApi.searchCourses(searchText.value)
  if (response.data) {
    coursesData.value = response.data
  }
}
```

**使用场景**:
- `CourseListPage.vue` - 课程搜索功能

---

## 3. 获取课程列表

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
   - 返回该分类下的所有已发布课程
   - 支持只传主分类，也可以传主分类+子分类
2. **获取子课程**: 传 `parentId`
   - 返回指定课程的所有子课程（仅已发布状态）
3. **获取所有课程**: 不传参数或只传 `lastId`
   - 返回所有已发布课程（支持分页）

**注意**:
- 普通用户只能看到已发布课程（state=1）
- 不支持按状态查询（由管理接口提供）
- 登录后会包含用户个人数据（订阅状态、学习进度）
- **返回格式为 KeysetPageResponse 分页响应**

**返回类型**: `KeysetPageResponse<CourseSummaryWithStatsAndProgressDTO>`

**返回示例**:
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 608,
        "name": "高三政治",
        "description": "高三政治相关的专业课程和实践内容",
        "mainCategory": 10,
        "subCategory": 9,
        "learnerCount": 150,
        "subscriptionCount": 89,
        "subscribed": true,
        "progress": 45
      }
    ],
    "hasMore": true,
    "nextCursor": {
      "lastId": 608
    }
  }
}
```

**返回字段说明**:
- `items`: 课程列表数组（每页最多20条）
- `hasMore`: 是否有更多数据（boolean）
- `nextCursor`: 下一页游标（当 hasMore=true 时存在）
  - `lastId`: 当前页最后一条数据的ID，用于下一页查询

**分页逻辑**:
1. 首次查询：不传 `lastId` 参数，返回前20条数据
2. 翻页查询：传入上一页返回的 `nextCursor.lastId`，获取下20条数据
3. 结束判断：当 `hasMore=false` 时，表示没有更多数据

**前端调用**:
```typescript
// API 调用方式1: 按分类筛选
courseApi.getCoursesByCategory(mainCategory?, subCategory?, lastId?)

// API 调用方式2: 获取子课程
courseApi.getSubCourses(parentId)

// API 调用方式3: 获取所有课程（分页）
courseApi.getCourses(lastId?)

// 实际使用 (CourseListPage.vue:289-330)
const loadCourses = async (reset = false) => {
  try {
    let response
    if (!currentCategory.value) {
      // 不选分类时，返回所有已发布课程
      response = await courseApi.getCoursesByCategory(undefined, undefined, lastId.value)
    } else {
      const { mainCategory, subCategory } = currentCategory.value
      response = await courseApi.getCoursesByCategory(mainCategory, subCategory, lastId.value)
    }

    if (response.data) {
      const pageResponse = response.data
      const newCourses = pageResponse.items

      if (reset) {
        coursesData.value = newCourses
      } else {
        coursesData.value = [...coursesData.value, ...newCourses]
      }

      // 更新分页状态
      hasMoreCourses.value = pageResponse.hasMore
      if (pageResponse.hasMore && pageResponse.nextCursor?.lastId) {
        lastId.value = pageResponse.nextCursor.lastId
      }
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}
```

**使用场景**:
- `CourseListPage.vue` - 课程列表页，支持分类筛选和分页
- 子课程列表展示

---

## 4. 获取热门课程

**接口路径**: `GET /api/v1/courses/hot`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量，必须大于0，最大200 |

**返回类型**: `List<CourseWithStatsDTO>`

**热度计算规则**:
```
综合热度 = 收藏数(bookmarks) + 学习中人数(in_progress_users) + 已完成人数(completed_users)
```

**排序规则**: 按综合热度降序排列

**数据源**: `content_stats` 表（实时统计）

**缓存策略**:
- 缓存时间：5分钟
- 缓存键：`hotCourses:{limit}`
- 不同的 limit 参数有独立缓存

**返回示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Java基础课程",
      "description": "从零开始学习Java",
      "mainCategory": 1,
      "subCategory": 2,
      "learnerCount": 1000,
      "subscriptionCount": 500
    }
  ]
}
```

**前端调用**:
```typescript
// 获取默认数量（10个）
courseApi.getHotCourses()

// 获取指定数量
courseApi.getHotCourses(20)

// 获取完整排行榜（100个）
courseApi.getHotCourses(100)

// 实际使用示例1 (HomePage.vue:70) - 首页推荐
const { data: hotCoursesData } = useFetch<Course[]>({
  fetchFn: () => courseApi.getHotCourses(),
  immediate: true,
  defaultValue: []
})

// 实际使用示例2 (CourseListPage.vue:272) - 课程列表页
const { data: hotCoursesData } = useFetch<Course[]>({
  fetchFn: () => courseApi.getHotCourses(),
  immediate: true,
  defaultValue: []
})
```

**使用场景**:
- `HomePage.vue` - 首页热门课程展示（默认10个，取前4个）
- `CourseListPage.vue` - 课程列表页热门推荐（默认10个）
- 排行榜专门页面 - 传入 `limit=100` 获取完整排行榜

**注意事项**:
- 只返回**已发布**状态的课程（state=1）
- 后端会获取 `limit × 2` 的数据，过滤后保证返回足够数量
- limit 参数最大值为 200，超过会自动限制

---

## 5. 创建课程

**接口路径**: `POST /api/v1/courses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`CreateCourseRequest`):
```json
{
  "name": "课程名称",
  "description": "课程描述",
  "mainCategory": 1,
  "subCategory": 2
}
```

**请求参数说明**:
- `name` (String, 必填): 课程名称
- `description` (String, 必填): 课程描述
- `mainCategory` (Integer, 必填): 主分类ID
- `subCategory` (Integer, 必填): 子分类ID

**返回类型**: `String`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "课程创建成功"
}
```

**前端调用**:
```typescript
// API 调用
courseApi.createCourse(courseData)

// 实际使用 (CourseListPage.vue:623)
const { execute: executeCreateCourse } = useMutation(
  (payload: CreateCourseRequest) => courseApi.createCourse(payload),
  {
    successMessage: '课程创建成功',
    onSuccess: () => {
      createDialog.value = false
      resetCreateForm.value = true
      // 刷新课程列表
      loadCourses()
    }
  }
)

// 调用方式
await executeCreateCourse({
  name: '课程名称',
  description: '课程描述',
  mainCategory: 1,
  subCategory: 2
})
```

**使用场景**:
- `CourseListPage.vue` - 创建新课程对话框

---

## 6. 创建子课程

**接口路径**: `POST /api/v1/courses/{parentId}/subcourses`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| parentId | Long | 是 | 父课程ID，必须大于0 |

**请求体** (`CreateSubcourseRequest`):
```json
{
  "name": "子课程名称",
  "description": "子课程描述"
}
```

**请求参数说明**:
- `name` (String, 必填): 子课程名称
- `description` (String, 必填): 子课程描述

**返回类型**: `String`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "课程创建成功"
}
```

**前端调用**:
```typescript
// API 调用
courseApi.createSubcourse(parentId, name, description)

// 实际使用 (CourseDetailPage.vue:524)
const { execute: executeCreateSubcourse } = useMutation(
  (payload: { parentId: number; name: string; description: string }) =>
    courseApi.createSubcourse(payload.parentId, payload.name, payload.description),
  {
    successMessage: '申请提交成功，等待审核',
    onSuccess: () => {
      applicationDialog.value = false
      // 刷新子课程列表
      loadSubCourses()
    }
  }
)

// 调用方式
await executeCreateSubcourse({
  parentId: courseId.value,
  name: '子课程名称',
  description: '子课程描述'
})
```

**使用场景**:
- `CourseDetailPage.vue` - 在主课程详情页创建子课程

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

## 测试用例建议

### 1. 获取课程详情
- ✅ 正常获取已发布课程
- ✅ 课程ID不存在返回404
- ✅ 课程ID为0或负数返回400

### 2. 搜索课程
- ✅ 搜索存在的课程名称
- ✅ 搜索不存在的课程返回空列表
- ✅ 搜索关键词为空返回400

### 3. 获取课程列表
- ✅ 不传参数获取所有已发布课程
- ✅ 按主分类筛选
- ✅ 按主分类+子分类筛选
- ✅ 获取子课程列表
- ✅ 分页功能（使用lastId）

### 4. 热门课程
- ✅ 默认获取10条
- ✅ 自定义limit参数
- ✅ 按热度排序

### 5. 创建课程
- ✅ 未登录返回401
- ✅ 已登录成功创建
- ✅ 必填字段缺失返回400

### 6. 创建子课程
- ✅ 未登录返回401
- ✅ 父课程不存在返回404
- ✅ 成功创建子课程
