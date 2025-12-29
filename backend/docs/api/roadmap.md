# 路线图管理接口文档 (RoadmapsController)

## 基本信息

- **Controller**: `RoadmapsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 60 requests/minute (per user)
- **前端 API**: `web/src/api/modules/roadmap.ts` (roadmapApi)

## 接口列表

## 1. 获取专业下的路线图列表

**接口路径**: `GET /api/v1/professions/{professionId}/roadmaps`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| professionId | Long | 是 | 专业ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | null | 分页游标，上一页的最后一个路线图ID |

**返回类型**: `List<RoadmapWithStatusDTO>`

**RoadmapWithStatusDTO 字段说明**:
- `id` (Long): 路线图ID
- `content` (String): 路线图内容（JSON格式，已转换为图形格式）
- `professionId` (Long): 专业ID
- `description` (String): 路线图描述
- `state` (Integer): 状态（0=草稿, 1=待审核, 2=已发布, 3=已拒绝, 4=已删除, 5=已封禁）
- `vote` (Integer): 点赞数
- `comment` (Integer): 评论数
- `creatorId` (Long): 创建者ID
- `available` (Integer): 可用节点数
- `nodeCount` (Integer): 总节点数
- `createdAt` (String): 创建时间（格式化字符串）
- `updatedAt` (String): 更新时间（格式化字符串）
- `creator` (UserBriefDTO): 创建者信息 { id, name }
- `profession` (ProfessionDTO): 专业信息 { id, name }
- `upvoted` (Boolean): 当前用户是否已点赞
- `pinned` (Boolean): 当前用户是否已置顶此路线图
- `learning` (Boolean): 当前用户是否正在学习此路线图

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "content": "{\"nodes\":[...],\"edges\":[...]}",
      "professionId": 10,
      "description": "完整的后端开发学习路线",
      "state": 2,
      "vote": 150,
      "comment": 32,
      "creatorId": 5,
      "available": 8,
      "nodeCount": 12,
      "createdAt": "2025-01-01 10:00:00",
      "updatedAt": "2025-01-02 15:30:00",
      "creator": {
        "id": 5,
        "name": "张三"
      },
      "profession": {
        "id": 10,
        "name": "后端开发"
      },
      "upvoted": true,
      "pinned": false,
      "learning": true
    }
  ]
}
```

**业务逻辑**:
1. 验证专业ID是否有效（大于0）
2. 如果 lastId 为 null（第一页），查询用户在该专业下的置顶路线图ID列表
3. 调用 DomainService 查询路线图列表（包含置顶的路线图）
4. 批量填充跨域信息：
   - creator: 创建者信息（UserBriefDTO）
   - profession: 专业信息（ProfessionDTO）
   - upvoted: 当前用户是否已点赞（从点赞服务查询）
   - pinned: 是否在置顶列表中
   - learning: 当前用户是否正在学习此路线图（从学习记录查询）
   - content: 转换为图形格式（包含课程名称和用户进度）
5. 返回完整的 RoadmapWithStatusDTO 列表

**前端调用**:
```typescript
// API 调用
roadmapApi.getRoadmapsByProfession(professionId, lastId)

// 实际使用
const { data: roadmaps } = await roadmapApi.getRoadmapsByProfession(10, null)
// roadmaps 是 RoadmapWithStatusDTO[]
```

**使用场景**:
- 专业路线图列表页面
- 显示置顶的路线图（第一页时）
- 下拉加载更多路线图

---

## 2. 更新路线图

**接口路径**: `PUT /api/v1/roadmaps/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 路线图ID，必须大于0 |

**请求体**:
```json
{
  "content": "[[edges],[nodeIds]]"
}
```

**请求参数说明**:
- `content` (String, 必填): 路线图内容，JSON格式字符串，格式为 `[[edges],[nodeIds]]`
  - edges: 边的数组，每个边是 `[fromNodeId, toNodeId]`
  - nodeIds: 节点ID数组

**返回类型**: `void` (无返回数据)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**业务逻辑**:
1. 验证路线图ID和内容格式
2. 查询路线图是否存在
3. 权限验证：只有创建者或管理员可以修改
4. 如果启用内容验证（配置），验证内容格式是否有效
5. 计算节点数量（从 nodeIds 数组）
6. 委托给 DomainService 更新路线图

**前端调用**:
```typescript
// API 调用
roadmapApi.updateRoadmap(roadmapId, content)

// 实际使用
await roadmapApi.updateRoadmap(1, JSON.stringify([[edges],[nodeIds]]))
// 成功后刷新路线图详情
```

**使用场景**:
- 路线图编辑页面
- 保存路线图修改

**错误情况**:
- 路线图不存在：返回 1301 (ROADMAP_NOT_FOUND)
- 内容格式错误：返回 1002 (INVALID_PARAMETER)
- 内容验证失败：返回 1302 (ROADMAP_CONTENT_INVALID)
- 权限不足：返回 1103 (PERMISSION_DENIED)
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 3. 创建路线图

**接口路径**: `POST /api/v1/roadmaps`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "professionId": 10,
  "content": "[[edges],[nodeIds]]",
  "description": "路线图描述"
}
```

**请求参数说明**:
- `professionId` (Long, 必填): 专业ID，必须大于0
- `content` (String, 必填): 路线图内容，JSON格式字符串
- `description` (String, 可选): 路线图描述

**返回类型**: `Long` (路线图ID)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 123
}
```

**业务逻辑**:
1. 验证专业ID、内容格式和用户ID
2. 跨域验证：验证专业和用户是否存在
3. 如果启用内容验证（配置），验证内容格式是否有效
4. 计算节点数量
5. 委托给 DomainService 创建路线图
6. 返回新创建的路线图ID

**前端调用**:
```typescript
// API 调用
roadmapApi.createRoadmap(professionId, content, description)

// 实际使用
const { data: roadmapId } = await roadmapApi.createRoadmap(
  10,
  JSON.stringify([[edges],[nodeIds]]),
  "完整的后端开发学习路线"
)
// 创建成功后跳转到路线图详情页
```

**使用场景**:
- 创建新的路线图
- 路线图创建页面

**错误情况**:
- 专业不存在：返回 1401 (PROFESSION_NOT_FOUND)
- 用户不存在：返回 1116 (USER_NOT_FOUND)
- 内容格式错误：返回 1002 (INVALID_PARAMETER)
- 内容验证失败：返回 1302 (ROADMAP_CONTENT_INVALID)
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 4. 获取路线图详情

**接口路径**: `GET /api/v1/roadmaps/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 路线图ID，必须大于0 |

**返回类型**: `RoadmapWithStatusDTO`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "content": "{\"nodes\":[...],\"edges\":[...]}",
    "professionId": 10,
    "description": "完整的后端开发学习路线",
    "state": 2,
    "vote": 150,
    "comment": 32,
    "creatorId": 5,
    "available": 8,
    "nodeCount": 12,
    "createdAt": "2025-01-01 10:00:00",
    "updatedAt": "2025-01-02 15:30:00",
    "creator": {
      "id": 5,
      "name": "张三"
    },
    "profession": {
      "id": 10,
      "name": "后端开发"
    },
    "upvoted": true,
    "pinned": false,
    "learning": true
  }
}
```

**业务逻辑**:
1. 验证路线图ID和用户ID
2. 查询路线图详情
3. 填充跨域信息：
   - creator: 创建者信息
   - profession: 专业信息
   - upvoted: 当前用户是否已点赞
   - content: 转换为图形格式，包含课程名称和用户进度信息
4. 返回完整的 RoadmapWithStatusDTO

**前端调用**:
```typescript
// API 调用
roadmapApi.getRoadmap(roadmapId)

// 实际使用
const { data: roadmap } = await roadmapApi.getRoadmap(1)
// roadmap 是 RoadmapWithStatusDTO
```

**使用场景**:
- 路线图详情页面
- 显示路线图的完整信息和图形内容

**错误情况**:
- 路线图不存在：返回 1301 (ROADMAP_NOT_FOUND)
- 路线图ID无效：返回 1002 (INVALID_PARAMETER)
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 5. 置顶/取消置顶路线图

**接口路径**: `POST /api/v1/roadmaps/pin`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "professionId": 10,
  "roadmapId": 1
}
```

**请求参数说明**:
- `professionId` (Long, 必填): 专业ID，必须大于0
- `roadmapId` (Long, 必填): 路线图ID，必须大于0

**返回类型**: `Boolean` (置顶状态)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```
- `data`: true 表示已置顶，false 表示已取消置顶

**业务逻辑**:
1. 验证专业ID和路线图ID
2. 委托给 UserDomainService 处理置顶逻辑（toggle操作）
3. 如果当前已置顶，则取消置顶；如果未置顶，则置顶
4. 返回置顶后的状态（true=已置顶，false=已取消置顶）

**前端调用**:
```typescript
// API 调用
roadmapApi.pinRoadmap(professionId, roadmapId)

// 实际使用
const { data: pinned } = await roadmapApi.pinRoadmap(10, 1)
if (pinned) {
  console.log("路线图已置顶")
} else {
  console.log("路线图已取消置顶")
}
```

**使用场景**:
- 路线图列表页面的置顶按钮
- 用户个性化定制路线图排序

**错误情况**:
- 专业ID无效：返回 1002 (INVALID_PARAMETER)
- 路线图ID无效：返回 1002 (INVALID_PARAMETER)
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 6. 获取当前用户创建的路线图列表

**接口路径**: `GET /api/v1/users/me/roadmaps`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | null | 分页游标，上一页的最后一个路线图ID |

**返回类型**: `List<RoadmapSummaryDTO>`

**RoadmapSummaryDTO 字段说明**:
- `id` (Long): 路线图ID
- `content` (String): 路线图内容（JSON格式）
- `professionId` (Long): 专业ID
- `description` (String): 路线图描述
- `state` (Integer): 状态（0=草稿, 1=待审核, 2=已发布, 3=已拒绝, 4=已删除, 5=已封禁）
- `vote` (Integer): 点赞数
- `comment` (Integer): 评论数
- `creatorId` (Long): 创建者ID
- `available` (Integer): 可用节点数
- `nodeCount` (Integer): 总节点数
- `createdAt` (String): 创建时间
- `updatedAt` (String): 更新时间

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "content": "[[edges],[nodeIds]]",
      "professionId": 10,
      "description": "后端学习路线",
      "state": 2,
      "vote": 150,
      "comment": 32,
      "creatorId": 5,
      "available": 8,
      "nodeCount": 12,
      "createdAt": "2025-01-01 10:00:00",
      "updatedAt": "2025-01-02 15:30:00"
    }
  ]
}
```

**业务逻辑**:
1. 从认证信息中获取当前用户ID
2. 委托给 DomainService 查询用户创建的所有状态的路线图（包括草稿、待审核、已发布、已拒绝等）
3. 返回 RoadmapSummaryDTO 列表（不包含跨域信息）

**前端调用**:
```typescript
// API 调用
roadmapApi.getCurrentUserRoadmaps(lastId)

// 实际使用
const { data: myRoadmaps } = await roadmapApi.getCurrentUserRoadmaps(null)
// myRoadmaps 是 RoadmapSummaryDTO[]
```

**使用场景**:
- 用户个人中心 - 我的路线图
- 显示用户创建的所有路线图（包括草稿、待审核、已拒绝等）

**错误情况**:
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## 7. 获取指定用户创建的路线图列表

**接口路径**: `GET /api/v1/users/{userId}/roadmaps`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID，必须大于0 |

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| lastId | Long | 否 | null | 分页游标，上一页的最后一个路线图ID |

**返回类型**: `List<RoadmapSummaryDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "content": "[[edges],[nodeIds]]",
      "professionId": 10,
      "description": "后端学习路线",
      "state": 2,
      "vote": 150,
      "comment": 32,
      "creatorId": 5,
      "available": 8,
      "nodeCount": 12,
      "createdAt": "2025-01-01 10:00:00",
      "updatedAt": "2025-01-02 15:30:00"
    }
  ]
}
```

**业务逻辑**:
1. 验证用户ID是否有效
2. 委托给 DomainService 查询用户创建的已发布（PUBLISHED）路线图
3. 返回 RoadmapSummaryDTO 列表

**前端调用**:
```typescript
// API 调用
roadmapApi.getUserRoadmaps(userId, lastId)

// 实际使用
const { data: userRoadmaps } = await roadmapApi.getUserRoadmaps(5, null)
// userRoadmaps 是 RoadmapSummaryDTO[]
```

**使用场景**:
- 查看其他用户的主页 - 路线图列表
- 只显示已发布的路线图（不显示草稿、待审核等）

**错误情况**:
- 用户ID无效：返回 1002 (INVALID_PARAMETER)

---

## 8. 删除路线图

**接口路径**: `DELETE /api/v1/roadmaps/{id}`

**是否需要登录**: 是 (`@SaCheckLogin`)

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 路线图ID，必须大于0 |

**返回类型**: `void` (无返回数据)

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**业务逻辑**:
1. 验证路线图ID是否有效
2. 查询路线图是否存在
3. 权限验证：只能删除自己创建的路线图，除非是管理员
4. 委托给 DomainService 执行软删除（状态改为 DELETED）

**前端调用**:
```typescript
// API 调用
roadmapApi.deleteRoadmap(roadmapId)

// 实际使用
await roadmapApi.deleteRoadmap(1)
// 成功后刷新路线图列表
```

**使用场景**:
- 用户删除自己创建的路线图
- 管理员删除违规路线图

**错误情况**:
- 路线图不存在：返回 1301 (ROADMAP_NOT_FOUND)
- 路线图ID无效：返回 1002 (INVALID_PARAMETER)
- 权限不足：返回 1103 (PERMISSION_DENIED)
- 未登录：返回 1101 (USER_NOT_LOGIN)

---

## DTO 类型说明

### RoadmapSummaryDTO (路线图摘要)

**用途**: 路线图基本信息，用于列表展示

```typescript
interface RoadmapSummaryDTO {
  id: number              // 路线图ID
  content: string         // 路线图内容（JSON格式）
  professionId: number    // 专业ID
  description: string     // 路线图描述
  state: number           // 状态（0~5）
  vote: number            // 点赞数
  comment: number         // 评论数
  creatorId: number       // 创建者ID
  available: number       // 可用节点数
  nodeCount: number       // 总节点数
  createdAt: string       // 创建时间
  updatedAt: string       // 更新时间
}
```

**使用场景**:
- 用户个人中心的路线图列表
- 管理员后台的路线图管理

### RoadmapDetailDTO (路线图详情)

**用途**: 扩展 SummaryDTO，增加创建者和专业信息

```typescript
interface RoadmapDetailDTO extends RoadmapSummaryDTO {
  creator: UserBriefDTO    // 创建者信息 { id, name }
  profession: ProfessionDTO  // 专业信息 { id, name }
}
```

### RoadmapWithStatusDTO (路线图完整信息)

**用途**: 扩展 DetailDTO，增加用户相关状态

```typescript
interface RoadmapWithStatusDTO extends RoadmapDetailDTO {
  upvoted: boolean    // 当前用户是否已点赞
  pinned: boolean     // 当前用户是否已置顶
  learning: boolean   // 当前用户是否正在学习
}
```

**说明**:
- 这是最完整的路线图信息，包含所有字段
- content 字段会被转换为图形格式（包含课程名称和用户进度）
- 用于路线图详情页和专业路线图列表

---

## 路线图内容格式

### content 字段格式

路线图内容是一个 JSON 格式的字符串，格式为：
```json
[
  [
    [fromNodeId1, toNodeId1],
    [fromNodeId2, toNodeId2],
    ...
  ],
  [nodeId1, nodeId2, nodeId3, ...]
]
```

**说明**:
- 第一个数组：边的数组，每个边是 `[fromNodeId, toNodeId]`
- 第二个数组：节点ID数组，每个ID对应一个课程ID

**示例**:
```json
[
  [[1, 2], [2, 3], [2, 4]],
  [1, 2, 3, 4]
]
```
表示：
- 课程1 → 课程2
- 课程2 → 课程3
- 课程2 → 课程4

### content 图形格式转换

当返回路线图详情时，content 字段会被转换为图形格式（包含课程名称和用户进度）：

**转换后的格式**:
```json
{
  "nodes": [
    {
      "id": 1,
      "name": "Java 基础",
      "finished": true,
      "progress": 1.0
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "finished": false,
      "progress": 0.45
    }
  ],
  "edges": [
    [1, 2],
    [2, 3]
  ]
}
```

**说明**:
- nodes: 节点数组，包含课程ID、名称、完成状态和进度
- edges: 边数组，与原始格式相同
- finished: 是否完成（进度 >= 完成阈值，默认100）
- progress: 进度百分比（0.0 ~ 1.0）

---

## 数据存储说明

### roadmap 表字段

**主要字段**:
- `id` (BIGINT): 主键
- `content` (TEXT): 路线图内容（JSON格式）
- `profession_id` (BIGINT): 专业ID
- `description` (TEXT): 路线图描述
- `state` (TINYINT): 状态（0~5）
- `vote` (INT): 点赞数
- `comment` (INT): 评论数
- `creator_id` (BIGINT): 创建者ID
- `available` (INT): 可用节点数
- `node_count` (INT): 总节点数
- `created_at` (TIMESTAMP): 创建时间
- `updated_at` (TIMESTAMP): 更新时间

### user_profile 表的 pinned_roadmaps 字段

**字段类型**: `TEXT`

**存储格式**: JSON字符串，按专业分组
```json
{
  "10": [1, 5, 8],
  "20": [2, 3]
}
```

**说明**:
- key: 专业ID
- value: 该专业下置顶的路线图ID数组
- 每个专业下可以置顶多个路线图
- 置顶的路线图会在该专业的路线图列表第一页优先显示

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数验证失败 |
| 1101 | 未登录（需要登录的接口） |
| 1103 | 权限不足（非创建者或管理员） |
| 1116 | 用户不存在 |
| 1301 | 路线图不存在 |
| 1302 | 路线图内容格式无效 |
| 1401 | 专业不存在 |
| 429 | 请求频率超限（超过 60 次/分钟） |

---

## 配置项说明

### SystemProperties.Roadmap

**配置类**: `SystemProperties.java`

**配置项**:
```java
public static class Roadmap {
    // 默认分页大小
    private int defaultPageSize = 20;

    // 是否启用内容验证
    private boolean enableContentValidation = false;

    // 是否启用批量状态查询（learning字段）
    private boolean enableBatchStatusQuery = true;

    // 完成阈值（progress >= 此值则认为finished=true）
    private int completionThreshold = 100;

    // 进度精度除数（progress百分比除以此值）
    private double progressPrecisionDivisor = 100.0;
}
```

**说明**:
- `enableContentValidation`: 是否验证路线图内容格式的有效性
- `enableBatchStatusQuery`: 是否批量查询用户学习状态（影响 learning 字段）
- `completionThreshold`: 课程完成的进度阈值（默认100表示100%完成）
- `progressPrecisionDivisor`: 进度精度除数（100表示百分比，1表示小数）

---

## 权限说明

### 1. 查看权限
- **公开接口**（无需登录）:
  - GET /users/{userId}/roadmaps - 查看用户的已发布路线图
- **需要登录**:
  - GET /professions/{professionId}/roadmaps - 查看专业路线图列表
  - GET /roadmaps/{id} - 查看路线图详情
  - GET /users/me/roadmaps - 查看当前用户的所有路线图

### 2. 操作权限
- **创建**: 任何登录用户都可以创建路线图
- **修改**: 只有创建者或管理员可以修改路线图
- **删除**: 只有创建者或管理员可以删除路线图
- **置顶**: 只能置顶自己的路线图（个性化功能）

---

## 注意事项

1. **登录要求**:
   - 7个接口中，6个需要登录，只有 GET /users/{userId}/roadmaps 不需要登录
   - 获取当前用户路线图列表（/users/me/roadmaps）返回所有状态的路线图
   - 获取其他用户路线图列表（/users/{userId}/roadmaps）只返回已发布的路线图

2. **置顶功能**:
   - 置顶是个性化功能，每个用户可以为不同专业置顶不同的路线图
   - 置顶的路线图会在该专业的第一页优先显示
   - 只有第一页（lastId=null）才会加载置顶的路线图

3. **内容格式转换**:
   - GET /professions/{professionId}/roadmaps 和 GET /roadmaps/{id} 返回的 content 字段会被转换为图形格式
   - 图形格式包含课程名称和用户学习进度
   - POST /roadmaps 和 PUT /roadmaps/{id} 提交的 content 是原始格式

4. **分页机制**:
   - 使用游标分页（lastId），不使用 offset
   - 默认每页20条（可配置）
   - lastId 为 null 表示第一页

5. **返回值差异**:
   - GET /professions/{professionId}/roadmaps: 返回 `List<RoadmapWithStatusDTO>`（完整信息）
   - GET /users/me/roadmaps: 返回 `List<RoadmapSummaryDTO>`（基础信息）
   - GET /users/{userId}/roadmaps: 返回 `List<RoadmapSummaryDTO>`（基础信息）
   - GET /roadmaps/{id}: 返回 `RoadmapWithStatusDTO`（完整信息）
   - POST /roadmaps: 返回 `Long`（路线图ID）
   - POST /roadmaps/pin: 返回 `Boolean`（置顶状态）
   - PUT /roadmaps/{id}: 返回 `void`（无数据）
   - DELETE /roadmaps/{id}: 返回 `void`（无数据）

6. **跨域查询**:
   - 路线图详情会关联查询：creator（用户信息）、profession（专业信息）
   - 路线图列表会批量查询：upvoted（点赞状态）、pinned（置顶状态）、learning（学习状态）
   - content 转换会查询：课程名称、用户课程进度

7. **软删除**:
   - 删除路线图是软删除，state 改为 DELETED（4）
   - 软删除的路线图不会在列表中显示

8. **状态机**:
   - 路线图状态：DRAFT(0) → PENDING(1) → PUBLISHED(2) / REJECTED(3) / BANNED(5)
   - DELETED(4) 是软删除状态
   - 只有已发布（PUBLISHED）的路线图会在公开列表中显示

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
