# 路线图管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

**测试类名**: `RoadmapsControllerTest`

**继承**: `extends BaseControllerTest`

**注解**:
- `@Transactional` - 自动回滚，测试之间互不影响

**必需依赖**:
- MockMvc - HTTP 请求测试
- ObjectMapper - JSON 解析
- UserDomainService - 创建测试用户
- ProfessionDataService - 创建测试专业
- RoadmapDataService - 查询路线图数据
- RoadmapDomainService - 路线图业务逻辑

### 测试辅助方法

需要创建以下辅助方法：

1. **createUser(String email)** - 创建测试用户
2. **createProfession(String name)** - 创建测试专业
3. **createPublishedRoadmap(Long professionId, Long creatorId, String description)** - 创建已发布路线图
4. **createDraftRoadmap(Long professionId, Long creatorId, String description)** - 创建草稿路线图
5. **getRoadmapContent()** - 生成有效的路线图内容 JSON 字符串

### 说明

1. **@Transactional - 自动清理数据**:
   - 每个 `@Test` 方法执行完后，所有数据库操作都会自动回滚
   - 包括用户、专业、路线图、置顶关系等所有操作
   - 无需手动清理数据，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **依赖数据**:
   - 创建路线图前必须先创建用户和专业
   - 使用辅助方法确保依赖数据的正确创建

4. **Sa-Token 登录**:
   - 需要登录的接口使用 `StpUtil.login(userId)` 模拟登录
   - 请求时添加 header: `"token", StpUtil.getTokenValue()`
   - 测试结束后使用 `StpUtil.logout()` 清理登录状态

5. **路线图内容格式**:
   - content 格式：`[[edges],[nodeIds]]`
   - edges: 边的数组，如 `[[1,2],[2,3]]`
   - nodeIds: 节点ID数组，如 `[1,2,3]`
   - 示例：`[[[1,2],[2,3]],[1,2,3]]`

---

## 接口1: 获取专业下的路线图列表 (GET /api/v1/professions/{professionId}/roadmaps)

### 测试场景

#### 1.1 成功获取路线图列表 - 有路线图
- **准备**：创建专业A，创建用户A，创建3个已发布路线图
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 3
  - 每个路线图包含字段：id, content, professionId, description, state, vote, comment, creatorId
  - 每个路线图包含用户字段：upvoted (boolean), pinned (boolean), learning (boolean)
  - 每个路线图包含跨域字段：creator (UserBriefDTO), profession (ProfessionBriefDTO)

#### 1.2 成功获取路线图列表 - 无路线图
- **准备**：创建专业A，用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是空数组

#### 1.3 字段验证 - professionId 无效（0）
- **准备**：用户A登录
- **请求**：GET /professions/0/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.4 字段验证 - professionId 无效（负数）
- **准备**：用户A登录
- **请求**：GET /professions/-1/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.5 权限验证 - 未登录
- **请求**：GET /professions/1/roadmaps (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 1.6 置顶功能 - 第一页显示置顶路线图
- **准备**：
  - 创建专业A，用户A，创建5个已发布路线图
  - 用户A置顶路线图1和路线图2
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps (lastId=null)
- **验证**：
  - 返回的数组前2个是路线图1和路线图2
  - 这两个路线图的 `pinned` = true
  - 其他路线图的 `pinned` = false

#### 1.7 置顶功能 - 第二页不显示置顶标记
- **准备**：同1.6
- **请求**：GET /professions/{professionA.id}/roadmaps?lastId={lastId}
- **验证**：
  - 返回的所有路线图 `pinned` = false（不在第一页，不重复显示置顶）

#### 1.8 用户状态字段 - upvoted
- **准备**：
  - 创建专业A，用户A，创建路线图B
  - 用户A点赞路线图B
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 路线图B的 `upvoted` = true

#### 1.9 用户状态字段 - learning
- **准备**：
  - 创建专业A，用户A，创建路线图B
  - 用户A开始学习路线图B（如果有学习功能）
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 路线图B的 `learning` = true

#### 1.10 跨域字段填充 - creator
- **准备**：创建专业A，用户A，用户A创建路线图B
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 路线图B的 `creator` 包含 { id, name }
  - `creator.id` = 用户A的ID
  - `creator.name` = 用户A的名称

#### 1.11 跨域字段填充 - profession
- **准备**：创建专业A，用户A，创建路线图B
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 路线图B的 `profession` 包含 { id, name, icon }
  - `profession.id` = 专业A的ID
  - `profession.name` = 专业A的名称

#### 1.12 内容格式转换 - content 包含课程名称和进度
- **准备**：创建专业A，用户A，创建包含课程1和课程2的路线图B
- **登录**：用户A登录
- **请求**：GET /professions/{professionA.id}/roadmaps
- **验证**：
  - 路线图B的 `content` 已转换为图形格式
  - content 包含 nodes 和 edges
  - nodes 中包含课程名称和用户进度信息

---

## 接口2: 更新路线图 (PUT /api/v1/roadmaps/{id})

### 测试场景

#### 2.1 成功更新路线图 - 只更新内容
- **准备**：用户A创建路线图B
- **登录**：用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "新内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 不存在或为 null（返回void）
  - 数据库验证：路线图B的 content 已更新

#### 2.2 成功更新路线图 - 同时更新内容和描述
- **准备**：用户A创建路线图B
- **登录**：用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "新内容", "description": "新描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库验证：路线图B的 content 和 description 都已更新

#### 2.3 字段验证 - content 缺失
- **准备**：用户A创建路线图B，用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.4 字段验证 - content 为空字符串
- **准备**：用户A创建路线图B，用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": ""}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.5 字段验证 - id 无效（0）
- **准备**：用户A登录
- **请求**：PUT /roadmaps/0，body: `{"content": "内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.6 业务验证 - 路线图不存在
- **准备**：用户A登录
- **请求**：PUT /roadmaps/99999，body: `{"content": "内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1301 (StatusCode.ROADMAP_NOT_FOUND)

#### 2.7 权限验证 - 修改他人的路线图
- **准备**：
  - 用户A创建路线图B
  - 用户C登录（非创建者，非管理员）
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "新内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1103 (StatusCode.PERMISSION_DENIED)

#### 2.8 权限验证 - 管理员可以修改任何路线图
- **准备**：
  - 用户A创建路线图B
  - 管理员用户登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "新内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库验证：路线图B的 content 已更新

#### 2.9 权限验证 - 未登录
- **请求**：PUT /roadmaps/1，body: `{"content": "内容"}` (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 2.10 内容验证 - 内容格式无效（如果启用验证）
- **准备**：
  - 配置启用内容格式验证
  - 用户A创建路线图B，用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "无效格式"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1302 (StatusCode.ROADMAP_CONTENT_INVALID)

#### 2.11 节点数量计算 - nodeCount 自动更新
- **准备**：用户A创建路线图B（包含3个节点），用户A登录
- **请求**：PUT /roadmaps/{B.id}，body: `{"content": "[[edges],[1,2,3,4,5]]"}` (5个节点)
- **验证**：
  - 返回 200 状态码
  - 数据库验证：路线图B的 nodeCount = 5

---

## 接口3: 创建路线图 (POST /api/v1/roadmaps)

### 测试场景

#### 3.1 成功创建路线图
- **准备**：创建专业A，用户B登录
- **请求**：POST /roadmaps，body: `{"professionId": A.id, "content": "内容", "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是创建的路线图ID（Long类型）
  - 数据库验证：路线图已创建，creatorId = 用户B的ID

#### 3.2 字段验证 - professionId 缺失
- **准备**：用户A登录
- **请求**：POST /roadmaps，body: `{"content": "内容", "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.3 字段验证 - content 缺失
- **准备**：用户A登录
- **请求**：POST /roadmaps，body: `{"professionId": 1, "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.4 字段验证 - description 缺失
- **准备**：用户A登录
- **请求**：POST /roadmaps，body: `{"professionId": 1, "content": "内容"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.5 业务验证 - 专业不存在
- **准备**：用户A登录
- **请求**：POST /roadmaps，body: `{"professionId": 99999, "content": "内容", "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1401 (StatusCode.PROFESSION_NOT_FOUND)

#### 3.6 权限验证 - 未登录
- **请求**：POST /roadmaps，body: `{"professionId": 1, "content": "内容", "description": "描述"}` (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 3.7 内容验证 - 内容格式无效（如果启用验证）
- **准备**：配置启用内容格式验证，用户A登录
- **请求**：POST /roadmaps，body: `{"professionId": 1, "content": "无效格式", "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1302 (StatusCode.ROADMAP_CONTENT_INVALID)

#### 3.8 节点数量计算 - nodeCount 自动设置
- **准备**：创建专业A，用户B登录
- **请求**：POST /roadmaps，body: `{"professionId": A.id, "content": "[[edges],[1,2,3]]", "description": "描述"}`
- **验证**：
  - 返回 200 状态码
  - 数据库验证：创建的路线图 nodeCount = 3

---

## 接口4: 获取路线图详情 (GET /api/v1/roadmaps/{id})

### 测试场景

#### 4.1 成功获取路线图详情
- **准备**：创建专业A，用户A，用户A创建路线图B
- **登录**：用户A登录
- **请求**：GET /roadmaps/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 包含完整路线图信息（RoadmapWithStatusDTO）
  - 包含 creator, profession, upvoted, pinned, learning 字段
  - content 已转换为图形格式

#### 4.2 字段验证 - id 无效（0）
- **准备**：用户A登录
- **请求**：GET /roadmaps/0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 4.3 业务验证 - 路线图不存在
- **准备**：用户A登录
- **请求**：GET /roadmaps/99999
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1301 (StatusCode.ROADMAP_NOT_FOUND)

#### 4.4 权限验证 - 未登录
- **请求**：GET /roadmaps/1 (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 4.5 内容格式转换 - content 包含课程信息
- **准备**：创建专业A，用户A，创建包含课程的路线图B
- **登录**：用户A登录
- **请求**：GET /roadmaps/{B.id}
- **验证**：
  - content 是 JSON 字符串
  - 解析后包含 nodes 和 edges
  - nodes 中包含课程名称（name）、完成状态（finished）、进度（progress）

---

## 接口5: 置顶/取消置顶路线图 (POST /api/v1/roadmaps/pin)

### 测试场景

#### 5.1 成功置顶路线图
- **准备**：创建专业A，用户B，创建路线图C（未置顶）
- **登录**：用户B登录
- **请求**：POST /roadmaps/pin，body: `{"professionId": A.id, "roadmapId": C.id}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` = true（表示已置顶）
  - 数据库验证：用户B在专业A下的置顶列表包含路线图C

#### 5.2 成功取消置顶路线图
- **准备**：
  - 创建专业A，用户B，创建路线图C
  - 用户B已置顶路线图C
- **登录**：用户B登录
- **请求**：POST /roadmaps/pin，body: `{"professionId": A.id, "roadmapId": C.id}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` = false（表示已取消置顶）
  - 数据库验证：用户B在专业A下的置顶列表不包含路线图C

#### 5.3 字段验证 - professionId 缺失
- **准备**：用户A登录
- **请求**：POST /roadmaps/pin，body: `{"roadmapId": 1}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 5.4 字段验证 - roadmapId 缺失
- **准备**：用户A登录
- **请求**：POST /roadmaps/pin，body: `{"professionId": 1}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 5.5 权限验证 - 未登录
- **请求**：POST /roadmaps/pin，body: `{"professionId": 1, "roadmapId": 1}` (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 5.6 业务验证 - 置顶数量达到上限
- **准备**：
  - 创建专业A，用户B
  - 用户B已置顶专业A下的最大数量路线图（如5个）
  - 创建新路线图C
- **登录**：用户B登录
- **请求**：POST /roadmaps/pin，body: `{"professionId": A.id, "roadmapId": C.id}`
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1303 (StatusCode.ROADMAP_PIN_LIMIT_EXCEEDED)

#### 5.7 跨用户隔离 - 用户A置顶不影响用户B
- **准备**：
  - 创建专业A，用户A，用户B，路线图C
  - 用户A置顶路线图C
- **登录**：用户B登录
- **请求**：GET /professions/{A.id}/roadmaps
- **验证**：
  - 路线图C的 `pinned` = false（用户B未置顶）

#### 5.8 跨专业隔离 - 专业A置顶不影响专业B
- **准备**：
  - 创建专业A和专业B，用户C，路线图D
  - 用户C在专业A下置顶路线图D
- **登录**：用户C登录
- **请求**：GET /professions/{B.id}/roadmaps
- **验证**：
  - 如果路线图D也属于专业B，则 `pinned` = false

---

## 接口6: 获取当前用户创建的路线图列表 (GET /api/v1/users/me/roadmaps)

### 测试场景

#### 6.1 成功获取路线图列表 - 包含所有状态
- **准备**：
  - 创建用户A
  - 用户A创建5个路线图：草稿、待审核、已发布、已拒绝、已封禁
- **登录**：用户A登录
- **请求**：GET /users/me/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 5
  - 包含所有状态的路线图
  - 每个路线图包含 profession 字段（id, name, icon）

#### 6.2 成功获取路线图列表 - 无路线图
- **准备**：创建用户A（未创建任何路线图）
- **登录**：用户A登录
- **请求**：GET /users/me/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是空数组

#### 6.3 权限验证 - 未登录
- **请求**：GET /users/me/roadmaps (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 6.4 分页功能 - 第一页
- **准备**：用户A创建25个路线图，用户A登录
- **请求**：GET /users/me/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.data` 长度为 20（默认分页大小）

#### 6.5 分页功能 - 第二页
- **准备**：同6.4
- **请求**：GET /users/me/roadmaps?lastId={lastId}
- **验证**：
  - 返回 200 状态码
  - `$.data` 长度为 5
  - 返回的ID都大于 lastId

#### 6.6 跨域字段填充 - profession
- **准备**：
  - 创建专业A（名称："后端开发"，图标："icon-backend"）
  - 用户B创建路线图C（专业A）
- **登录**：用户B登录
- **请求**：GET /users/me/roadmaps
- **验证**：
  - 路线图C的 `profession.id` = 专业A的ID
  - 路线图C的 `profession.name` = "后端开发"
  - 路线图C的 `profession.icon` = "icon-backend"

---

## 接口7: 获取指定用户创建的路线图列表 (GET /api/v1/users/{userId}/roadmaps)

### 测试场景

#### 7.1 成功获取路线图列表 - 只返回已发布
- **准备**：
  - 创建用户A
  - 用户A创建5个路线图：草稿、待审核、已发布1、已发布2、已拒绝
- **请求**：GET /users/{A.id}/roadmaps?lastId=0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 2
  - 只包含已发布状态的路线图
  - 每个路线图包含 profession 字段

#### 7.2 成功获取路线图列表 - 无已发布路线图
- **准备**：创建用户A，用户A只创建了草稿路线图
- **请求**：GET /users/{A.id}/roadmaps?lastId=0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是空数组

#### 7.3 字段验证 - userId 无效（0）
- **请求**：GET /users/0/roadmaps?lastId=0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 7.4 字段验证 - userId 无效（负数）
- **请求**：GET /users/-1/roadmaps?lastId=0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 7.5 字段验证 - lastId 缺失
- **请求**：GET /users/1/roadmaps
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 7.6 字段验证 - lastId 为负数
- **请求**：GET /users/1/roadmaps?lastId=-1
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 7.7 权限验证 - 不需要登录
- **准备**：创建用户A，用户A创建已发布路线图
- **请求**：GET /users/{A.id}/roadmaps?lastId=0 (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 可以正常获取已发布路线图列表

#### 7.8 分页功能
- **准备**：用户A创建25个已发布路线图
- **请求**：GET /users/{A.id}/roadmaps?lastId=0
- **验证**：
  - 返回 200 状态码
  - `$.data` 长度为 20

#### 7.9 跨域字段填充 - profession
- **准备**：同6.6
- **请求**：GET /users/{B.id}/roadmaps?lastId=0
- **验证**：
  - 路线图C的 `profession` 包含 { id, name, icon }

---

## 接口8: 删除路线图 (DELETE /api/v1/roadmaps/{id})

### 测试场景

#### 8.1 成功删除路线图
- **准备**：用户A创建路线图B
- **登录**：用户A登录
- **请求**：DELETE /roadmaps/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 不存在或为 null
  - 数据库验证：路线图B的 deleted_at 不为 null（软删除标记）

#### 8.2 字段验证 - id 无效（0）
- **准备**：用户A登录
- **请求**：DELETE /roadmaps/0
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 8.3 业务验证 - 路线图不存在
- **准备**：用户A登录
- **请求**：DELETE /roadmaps/99999
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1301 (StatusCode.ROADMAP_NOT_FOUND)

#### 8.4 权限验证 - 删除他人的路线图
- **准备**：
  - 用户A创建路线图B
  - 用户C登录（非创建者，非管理员）
- **请求**：DELETE /roadmaps/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1103 (StatusCode.PERMISSION_DENIED)

#### 8.5 权限验证 - 管理员可以删除任何路线图
- **准备**：
  - 用户A创建路线图B
  - 管理员用户登录
- **请求**：DELETE /roadmaps/{B.id}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库验证：路线图B的 deleted_at 不为 null

#### 8.6 权限验证 - 未登录
- **请求**：DELETE /roadmaps/1 (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 8.7 软删除验证 - 删除后不出现在列表中
- **准备**：
  - 创建专业A，用户B
  - 用户B创建路线图C（已发布）
  - 用户B删除路线图C
- **请求**：GET /professions/{A.id}/roadmaps（用户B登录）
- **验证**：
  - 返回的列表中不包含路线图C

#### 8.8 软删除验证 - 删除后出现在自己的列表中
- **准备**：同8.7
- **请求**：GET /users/me/roadmaps（用户B登录）
- **验证**：
  - 返回的列表中不包含路线图C（软删除的路线图在查询时被过滤）

---

## 参数验证测试

### 9. 通用参数验证

#### 9.1 ID参数验证（各接口）
- **测试接口**：
  - GET /professions/{professionId}/roadmaps
  - PUT /roadmaps/{id}
  - GET /roadmaps/{id}
  - DELETE /roadmaps/{id}
  - GET /users/{userId}/roadmaps
- **测试用例**：
  - id = 0 → 返回 200, code = 1002
  - id = -1 → 返回 200, code = 1002
  - id 非数字 → 返回 200, code = 1002 或 400

#### 9.2 请求体参数验证
- **测试接口**：
  - PUT /roadmaps/{id}
  - POST /roadmaps
  - POST /roadmaps/pin
- **测试用例**：
  - body 为空 → 返回 200, code = 1002
  - body 格式错误（非JSON）→ 返回 400
  - 必填字段缺失 → 返回 200, code = 1002

---

## 边界测试

### 10. 边界场景

#### 10.1 空数据库查询
- **请求**：查询不存在专业的路线图
- **验证**：返回空数组，不报错

#### 10.2 大量路线图
- **准备**：创建专业A，用户B创建50个路线图
- **请求**：GET /professions/{A.id}/roadmaps
- **验证**：
  - 返回前20个路线图（分页）
  - 响应时间合理（< 2秒）

#### 10.3 内容长度边界
- **准备**：用户A登录
- **请求**：POST /roadmaps，content 包含大量节点（如100个）
- **验证**：
  - 成功创建
  - nodeCount = 100

#### 10.4 描述长度边界
- **准备**：用户A登录
- **请求**：POST /roadmaps，description 达到最大长度
- **验证**：
  - 成功创建
  - 描述完整保存

---

## 业务场景测试

### 11. 路线图业务流程

#### 11.1 完整创建-修改-删除流程
- **操作**：
  1. 用户A创建路线图B → 成功
  2. 获取用户A的路线图列表 → 包含路线图B
  3. 修改路线图B的内容 → 成功
  4. 删除路线图B → 成功
  5. 获取用户A的路线图列表 → 包含路线图B（state=DELETED）
  6. 获取专业路线图列表 → 不包含路线图B
- **验证**：每步都成功，数据一致

#### 11.2 置顶流程
- **操作**：
  1. 用户A创建路线图B → 成功
  2. 用户A置顶路线图B → 成功，返回 true
  3. 获取专业路线图列表 → 路线图B在第一页，pinned=true
  4. 用户A取消置顶路线图B → 成功，返回 false
  5. 获取专业路线图列表 → 路线图B的 pinned=false
- **验证**：置顶状态正确

#### 11.3 跨用户隔离
- **操作**：
  1. 用户A创建路线图1
  2. 用户B创建路线图2
  3. 获取用户A的路线图 → 只包含路线图1
  4. 获取用户B的路线图 → 只包含路线图2
- **验证**：用户路线图互不影响

---

## 测试执行顺序建议

1. 先执行 **基础功能测试**（3.1, 4.1, 6.1, 7.1）- 确保核心功能正常
2. 再执行 **参数验证测试**（各接口的字段验证）- 确保输入验证完善
3. 然后执行 **权限验证测试**（各接口的权限测试）- 确保鉴权正确
4. 再执行 **业务逻辑测试**（置顶、跨域字段填充等）- 确保业务规则正确
5. 最后执行 **边界和场景测试**（10.x, 11.x）- 确保系统健壮性

---

## 注意事项

1. **登录要求**：
   - 6个接口需要登录（除了 GET /users/{userId}/roadmaps）
   - 获取当前用户路线图列表返回所有状态
   - 获取其他用户路线图列表只返回已发布状态

2. **置顶功能**：
   - 置顶是个性化功能，每个用户在每个专业下有独立的置顶列表
   - 置顶的路线图只在第一页优先显示
   - 第二页开始不显示置顶标记

3. **内容格式转换**：
   - GET /professions/{professionId}/roadmaps 和 GET /roadmaps/{id} 返回的 content 会被转换为图形格式
   - 图形格式包含课程名称和用户学习进度
   - POST /roadmaps 和 PUT /roadmaps/{id} 提交的 content 是原始格式

4. **软删除**：
   - 删除路线图是软删除，设置 deleted_at 字段
   - 软删除的路线图不会在任何列表中显示（查询时过滤 deleted_at IS NULL）
   - 创建者自己也无法在 GET /users/me/roadmaps 中看到已删除的路线图

5. **跨域字段**：
   - profession 字段只包含 { id, name, icon }，不包含完整信息
   - creator 字段只包含 { id, name }
   - 批量查询专业和用户信息，避免 N+1 查询

6. **分页机制**：
   - 使用游标分页（lastId），不使用 offset
   - 默认每页20条
   - lastId 为 null 或 0 表示第一页

7. **返回值差异**：
   - GET /professions/{professionId}/roadmaps: 返回 RoadmapWithStatusDTO（完整信息）
   - GET /users/me/roadmaps: 返回 RoadmapDetailDTO（包含profession）
   - GET /users/{userId}/roadmaps: 返回 RoadmapDetailDTO（包含profession）
   - GET /roadmaps/{id}: 返回 RoadmapWithStatusDTO（完整信息）
   - POST /roadmaps: 返回 Long（路线图ID）
   - POST /roadmaps/pin: 返回 Boolean（置顶状态）
   - PUT /roadmaps/{id}: 返回 void
   - DELETE /roadmaps/{id}: 返回 void

---

*此文档用于指导路线图管理接口的测试用例编写，确保测试覆盖全面且准确*
