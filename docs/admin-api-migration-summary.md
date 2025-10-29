# 管理后台接口迁移实施总结

## 执行日期
2025-10-29

## 迁移概述
将管理后台使用的非 `/admin` 路径接口迁移到 `/admin` 路径下,以支持前后端分离部署架构。

---

## 已完成的工作

### 1. 后端 Controller 层修改

#### 1.1 AdminUserController 新增接口
文件: `backend/learn-api/src/main/java/com/prosper/learn/api/v1/controller/admin/AdminUserController.java`

**新增接口:**
- `GET /api/v1/admin/users/{userId}` - 按ID获取用户详情
  - 复用: `userService.getUser(userId)`

- `GET /api/v1/admin/users/search?name=xxx` - 搜索用户（包括被屏蔽用户）
  - 复用: `userService.searchUsers(name)`
  - **TODO**: 需要验证是否真的返回被屏蔽用户,如果不返回需要新增 Service 方法

#### 1.2 AdminCoursesController 新增接口
文件: `backend/learn-api/src/main/java/com/prosper/learn/api/v1/controller/admin/AdminCoursesController.java`

**修改接口:**
- `GET /api/v1/admin/courses` - 支持多种查询方式
  - 按状态查询: `?state=0&lastId=123`
  - 按分类查询: `?mainCategory=1&subCategory=2`
  - 复用: `courseService.getListByStateAndLastId()` 和 `courseService.getListByCategory()`
  - **TODO**: 按分类查询目前只返回已发布课程,如需其他状态需新增 Service 方法

**新增接口:**
- `GET /api/v1/admin/courses/{id}` - 获取课程详情
  - 复用: `courseService.getCourseById(id)`
  - **TODO**: 需要验证是否能获取非已发布状态的课程

- `GET /api/v1/admin/courses/{parentId}/subcourses?state=0` - 查询子课程
  - 复用: `courseService.getListByParent(parentId, state)`
  - **TODO**: 需要验证状态过滤逻辑

#### 1.3 AdminMemoryCardDeckController 创建
文件: `backend/learn-api/src/main/java/com/prosper/learn/api/v1/controller/admin/AdminMemoryCardDeckController.java`

**新增接口 (所有接口都标记了 TODO,需要实现 Service):**
- `GET /api/v1/admin/memory/decks` - 查询卡片组列表
- `POST /api/v1/admin/memory/decks/{deckId}/approve` - 审核通过
- `POST /api/v1/admin/memory/decks/{deckId}/reject` - 拒绝
- `POST /api/v1/admin/memory/decks/{deckId}/ban` - 屏蔽
- `POST /api/v1/admin/memory/decks/{deckId}/restore` - 恢复

**所有接口当前抛出 `UnsupportedOperationException`,需要:**
1. 注入 MemoryCardDeckService
2. 实现对应的 Service 方法
3. 连接实际业务逻辑

---

### 2. 前端 API 服务层修改

#### 2.1 apiServiceV1.ts
文件: `web-ts/src/services/api/v1/apiServiceV1.ts`

**userServiceV1 新增方法:**
- `getUserById(userId)` → `GET /api/v1/admin/users/{userId}`
- `adminSearchUser(name)` → `GET /api/v1/admin/users/search`

**courseServiceV1 修改/新增方法:**
- `getAdminCourses()` - 支持更多参数: `state, lastId, mainCategory, subCategory`
- `getAdminCourse(id)` → `GET /api/v1/admin/courses/{id}`
- `getAdminSubcourses(parentId, state)` → `GET /api/v1/admin/courses/{parentId}/subcourses`

#### 2.2 memoryService.ts
文件: `web-ts/src/services/memoryService.ts`

**修改方法 (路径更新为 admin):**
- `getDecksForReview()` → `GET /api/v1/admin/memory/decks`
- `getAdminDecks()` → `GET /api/v1/admin/memory/decks` (新增)
- `approveDeck()` → `POST /api/v1/admin/memory/decks/{id}/approve`
- `rejectDeck()` → `POST /api/v1/admin/memory/decks/{id}/reject`
- `banDeck()` → `POST /api/v1/admin/memory/decks/{id}/ban`
- `restoreDeck()` → `POST /api/v1/admin/memory/decks/{id}/restore`

---

### 3. 前端管理页面修改

#### 3.1 UserManagement.vue
文件: `web-ts/src/components/admin/UserManagement.vue`

**修改:**
- `searchById()`: `userServiceV1.getUser()` → `userServiceV1.getUserById()`
- `searchByName()`: `userServiceV1.searchUser()` → `userServiceV1.adminSearchUser()`

#### 3.2 CourseManagement.vue
文件: `web-ts/src/components/admin/CourseManagement.vue`

**修改:**
- `searchCourseById()`: `courseServiceV1.getCourse()` → `courseServiceV1.getAdminCourse()`
- 子课程查询: `courseServiceV1.getCoursesByState()` → `courseServiceV1.getAdminSubcourses()`
- 分类查询: `courseServiceV1.getCoursesByCategory()` → `courseServiceV1.getAdminCourses()` (合并到统一接口)

#### 3.3 MemoryCardReview.vue & DeckQuery.vue
文件: `web-ts/src/components/admin/MemoryCardReview.vue`, `web-ts/src/components/admin/DeckQuery.vue`

**修改:**
- 所有审核相关方法已自动使用新的 admin 路径 (通过 memoryService 的修改)

---

## 需要后续处理的 TODO 事项

### 高优先级

1. **MemoryCardDeck 模块完全未实现**
   - 文件: `AdminMemoryCardDeckController.java`
   - 需要: 创建/找到 MemoryCardDeckService
   - 需要: 实现所有审核相关的 Service 方法
   - 影响: 记忆卡片审核功能目前无法使用

2. **验证 UserService.searchUsers() 是否过滤被屏蔽用户**
   - 文件: `AdminUserController.java` 第 64 行
   - 如果过滤了: 需要新增 `searchUsersIncludingBanned()` 方法
   - 如果没过滤: 移除 TODO 注释即可

### 中优先级

3. **验证 CourseService.getCourseById() 是否支持所有状态**
   - 文件: `AdminCoursesController.java` 第 66 行
   - 如果只返回已发布: 需要新增支持所有状态的方法

4. **验证按分类查询是否支持指定状态**
   - 文件: `AdminCoursesController.java` 第 55 行
   - 当前只返回已发布课程
   - 如需其他状态: 需要新增 Service 方法

5. **验证子课程查询的状态过滤逻辑**
   - 文件: `AdminCoursesController.java` 第 80 行
   - 确认 `courseService.getListByParent()` 的行为

---

## 测试检查清单

### 后端测试
- [ ] AdminUserController
  - [ ] GET /api/v1/admin/users/{userId} - 能否获取被屏蔽用户
  - [ ] GET /api/v1/admin/users/search - 是否包含被屏蔽用户

- [ ] AdminCoursesController
  - [ ] GET /api/v1/admin/courses/{id} - 能否获取非已发布课程
  - [ ] GET /api/v1/admin/courses?mainCategory&subCategory - 是否只返回已发布
  - [ ] GET /api/v1/admin/courses/{parentId}/subcourses - 状态过滤是否正确

- [ ] AdminMemoryCardDeckController
  - [ ] 实现所有接口后进行完整测试

### 前端测试
- [ ] 用户管理
  - [ ] 按ID搜索用户
  - [ ] 按名称搜索用户
  - [ ] 搜索结果包含被屏蔽用户

- [ ] 课程管理
  - [ ] 按ID查询课程
  - [ ] 查询子课程
  - [ ] 按分类筛选课程
  - [ ] 切换不同状态 tab

- [ ] 记忆卡片审核
  - [ ] 查询卡片组列表
  - [ ] 审核通过/拒绝/屏蔽/恢复操作

### 权限测试
- [ ] 非管理员无法访问 `/api/v1/admin/*` 接口
- [ ] 管理员可以正常访问所有 admin 接口

---

## 架构改进

### 优点
✅ 支持前后端分离部署
✅ 管理后台和用户端完全解耦
✅ 权限控制更清晰 (@RequireRole 注解)
✅ 便于独立优化和扩展

### 注意事项
⚠️ 没有修改 Service 和 Mapper 层,只在 Controller 层复用现有方法
⚠️ 部分接口标记了 TODO,需要验证业务逻辑是否符合预期
⚠️ MemoryCardDeck 模块需要完整实现后端逻辑

---

## 文件修改清单

### 后端 (3个文件)
1. `backend/learn-api/.../admin/AdminUserController.java` - 新增2个接口
2. `backend/learn-api/.../admin/AdminCoursesController.java` - 修改1个+新增2个接口
3. `backend/learn-api/.../admin/AdminMemoryCardDeckController.java` - 新建文件,5个接口待实现

### 前端 (4个文件)
1. `web-ts/src/services/api/v1/apiServiceV1.ts` - 新增/修改方法
2. `web-ts/src/services/memoryService.ts` - 修改路径为 admin
3. `web-ts/src/components/admin/UserManagement.vue` - 修改2处调用
4. `web-ts/src/components/admin/CourseManagement.vue` - 修改3处调用

---

## 后续建议

1. **立即处理**: 实现 AdminMemoryCardDeckController 的所有接口
2. **尽快验证**: 标记 TODO 的所有业务逻辑
3. **测试验证**: 按照测试清单逐项验证
4. **文档更新**: 更新 API 文档,标注 admin 接口的使用场景
5. **监控部署**: 部署后监控 admin 接口的调用情况和错误率

---

生成时间: 2025-10-29
