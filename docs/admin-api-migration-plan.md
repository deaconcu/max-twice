# 管理后台接口迁移方案

## 背景
当前管理后台页面调用了部分非 `/admin` 路径的接口，需要对这些接口进行整理，根据使用场景决定迁移策略。

## 接口使用情况分析

### 1. 用户相关接口

#### 1.1 `GET /api/v1/users/{username}` - 获取用户信息
**前端调用情况:**
- ✅ **UserView.vue** - 用户个人主页（普通用户查看）
- ✅ **UserInfoTab.vue** - 用户信息标签（普通用户查看）
- ✅ **UserProfileCard.vue** - 用户资料卡片（普通用户查看）
- ✅ **UserCard.vue** - 用户卡片组件（普通用户查看）
- ✅ **UserPostsTab.vue** - 用户文章标签（普通用户查看）
- ✅ **UserContentsTab.vue** - 用户内容标签（普通用户查看）
- ✅ **UserRoadmapsTab.vue** - 用户路线图标签（普通用户查看）
- ❌ **UserManagement.vue** - 管理后台用户管理（管理员）

**迁移策略:** **拆分接口**
- 保留 `GET /api/v1/users/{username}` - 普通用户查看他人资料
- 新增 `GET /api/v1/admin/users/{userId}` - 管理员查看用户详情（支持按ID查询，返回更多管理信息）

#### 1.2 `GET /api/v1/users/search?name=xxx` - 搜索用户
**前端调用情况:**
- ✅ **InviteUser.vue** - 邀请用户对话框（普通用户功能）
- ❌ **UserManagement.vue** - 管理后台用户管理（管理员）

**迁移策略:** **拆分接口**
- 保留 `GET /api/v1/users/search?name=xxx` - 普通用户搜索用户
- 新增 `GET /api/v1/admin/users/search?name=xxx` - 管理员搜索用户（返回更多信息，包括被屏蔽用户）

---

### 2. 课程相关接口

#### 2.1 `GET /api/v1/courses/{id}` - 获取课程详情
**前端调用情况:**
- ✅ **CourseList.vue** - 课程列表页（普通用户查看）
- ✅ **HotRanking.vue** - 热门排行榜（普通用户查看）
- ✅ **HotCoursesRanking.vue** - 热门课程排行（普通用户查看）
- ❌ **CourseManagement.vue** - 管理后台课程管理（管理员）

**迁移策略:** **拆分接口**
- 保留 `GET /api/v1/courses/{id}` - 普通用户查看课程详情（只返回已发布课程）
- 新增 `GET /api/v1/admin/courses/{id}` - 管理员查看课程详情（返回所有状态课程）

#### 2.2 `GET /api/v1/courses?state=xxx&parentId=xxx` - 按状态获取子课程
**前端调用情况:**
- ❌ **CourseManagement.vue** - 管理后台课程管理（仅用于查询子课程）

**迁移策略:** **迁移到admin路径**
- 删除该用法的普通接口
- 新增 `GET /api/v1/admin/courses/{parentId}/subcourses?state=xxx` - 管理员查询子课程

#### 2.3 `GET /api/v1/courses?mainCategory=xxx&subCategory=xxx` - 按分类获取课程
**前端调用情况:**
- ✅ **CourseList.vue** - 课程列表页（普通用户按分类浏览）
- ❌ **CourseManagement.vue** - 管理后台课程管理（管理员）

**迁移策略:** **拆分接口**
- 保留 `GET /api/v1/courses?mainCategory=xxx&subCategory=xxx` - 普通用户按分类查询（只返回已发布课程）
- 新增 `GET /api/v1/admin/courses?mainCategory=xxx&subCategory=xxx` - 管理员按分类查询（返回指定状态的课程）

**原因:** 如果前后端分开部署（管理后台独立部署），管理后台无法跨域调用普通端接口，必须有独立的admin接口。

---

### 3. 记忆卡片相关接口

#### 3.1 `GET /api/v1/memory/decks/review` - 获取卡片组审核列表
**前端调用情况:**
- ❌ **MemoryCardReview.vue** - 管理后台记忆卡片审核（管理员）
- ❌ **DeckQuery.vue** - 管理后台卡片组查询（管理员）

**迁移策略:** **迁移到admin路径**
- 删除 `GET /api/v1/memory/decks/review`
- 新增 `GET /api/v1/admin/memory/decks` - 管理员查询卡片组（支持按状态、创建者等筛选）

#### 3.2 审核相关接口
**接口列表:**
- `POST /api/v1/memory/decks/{deckId}/approve` - 审核通过
- `POST /api/v1/memory/decks/{deckId}/reject` - 拒绝
- `POST /api/v1/memory/decks/{deckId}/ban` - 屏蔽
- `POST /api/v1/memory/decks/{deckId}/restore` - 恢复

**前端调用情况:**
- ❌ **MemoryCardReview.vue** - 管理后台记忆卡片审核（管理员）

**迁移策略:** **迁移到admin路径**
- 删除以上普通接口
- 新增 `POST /api/v1/admin/memory/decks/{deckId}/approve` - 统一审核接口（支持approve/reject/ban/restore操作）

---

## 迁移方案总结

### 需要拆分的接口（普通接口和admin接口并存）

| 普通接口 | 新增Admin接口 | 说明 |
|---------|-------------|------|
| `GET /api/v1/users/{username}` | `GET /api/v1/admin/users/{userId}` | 普通用户按username查看，管理员按userId查看并返回更多信息 |
| `GET /api/v1/users/search` | `GET /api/v1/admin/users/search` | 管理员搜索返回更多信息，包括被屏蔽用户 |
| `GET /api/v1/courses/{id}` | `GET /api/v1/admin/courses/{id}` | 普通用户只能查看已发布课程，管理员可查看所有状态 |
| `GET /api/v1/courses?mainCategory&subCategory` | `GET /api/v1/admin/courses?mainCategory&subCategory&state` | 普通用户只查询已发布课程，管理员可按状态查询 |

### 需要迁移的接口（只有admin接口）

| 原接口 | 新接口 | 说明 |
|-------|--------|------|
| `GET /api/v1/courses?state=xxx&parentId=xxx` | `GET /api/v1/admin/courses/{parentId}/subcourses` | 管理员查询子课程 |
| `GET /api/v1/memory/decks/review` | `GET /api/v1/admin/memory/decks` | 管理员查询卡片组 |
| `POST /api/v1/memory/decks/{id}/approve` | `POST /api/v1/admin/memory/decks/{id}/approve` | 审核操作 |
| `POST /api/v1/memory/decks/{id}/reject` | `POST /api/v1/admin/memory/decks/{id}/reject` | 审核操作 |
| `POST /api/v1/memory/decks/{id}/ban` | `POST /api/v1/admin/memory/decks/{id}/ban` | 审核操作 |
| `POST /api/v1/memory/decks/{id}/restore` | `POST /api/v1/admin/memory/decks/{id}/restore` | 审核操作 |

### 保持不变的接口

无。所有管理后台使用的接口都需要有对应的 admin 路径版本，以支持前后端分离部署架构。

---

## 实施步骤

### 阶段一: 后端实现新接口
1. **用户管理模块**
   - [ ] 创建 `AdminUsersController`
   - [ ] 实现 `GET /api/v1/admin/users/{userId}` - 按ID查询用户详情
   - [ ] 实现 `GET /api/v1/admin/users/search` - 管理员搜索用户
   - [ ] 添加 `@RequireRole(ADMIN)` 权限控制

2. **课程管理模块**
   - [ ] 在 `AdminCoursesController` 中新增接口
   - [ ] 实现 `GET /api/v1/admin/courses/{id}` - 查询课程详情（所有状态）
   - [ ] 实现 `GET /api/v1/admin/courses/{parentId}/subcourses` - 查询子课程
   - [ ] 实现 `GET /api/v1/admin/courses?mainCategory=xxx&subCategory=xxx&state=xxx` - 按分类查询课程
   - [ ] 添加 `@RequireRole(ADMIN)` 权限控制

3. **记忆卡片模块**
   - [ ] 创建 `AdminMemoryCardController`
   - [ ] 实现 `GET /api/v1/admin/memory/decks` - 查询卡片组（支持筛选）
   - [ ] 实现 `POST /api/v1/admin/memory/decks/{id}/approve` - 审核通过
   - [ ] 实现 `POST /api/v1/admin/memory/decks/{id}/reject` - 拒绝
   - [ ] 实现 `POST /api/v1/admin/memory/decks/{id}/ban` - 屏蔽
   - [ ] 实现 `POST /api/v1/admin/memory/decks/{id}/restore` - 恢复
   - [ ] 添加 `@RequireRole(ADMIN)` 权限控制

### 阶段二: 前端API服务层更新
1. **更新 `apiServiceV1.ts`**
   - [ ] `userServiceV1` 添加 `getUserById()` 和 `adminSearchUser()` 方法
   - [ ] `courseServiceV1` 添加 `getAdminCourse()` 和 `getAdminSubcourses()` 方法
   - [ ] 创建 `adminMemoryServiceV1` 服务模块

2. **更新 `memoryService.ts`**
   - [ ] 将审核相关方法迁移到 admin 路径

### 阶段三: 前端管理页面更新
1. **UserManagement.vue**
   - [ ] `getUser(userId)` → `userServiceV1.getUserById(userId)`
   - [ ] `searchUser(name)` → `userServiceV1.adminSearchUser(name)`

2. **CourseManagement.vue**
   - [ ] `getCourse(id)` → `courseServiceV1.getAdminCourse(id)`
   - [ ] `getCoursesByState()` 子课程查询 → `courseServiceV1.getAdminSubcourses(parentId)`
   - [ ] `getCoursesByCategory()` → `courseServiceV1.getAdminCoursesByCategory(mainCategory, subCategory, state)`

3. **MemoryCardReview.vue & DeckQuery.vue**
   - [ ] `getDecksForReview()` → `adminMemoryServiceV1.getDecks()`
   - [ ] `approveDeck()` → `adminMemoryServiceV1.approveDeck()`
   - [ ] `rejectDeck()` → `adminMemoryServiceV1.rejectDeck()`
   - [ ] `banDeck()` → `adminMemoryServiceV1.banDeck()`
   - [ ] `restoreDeck()` → `adminMemoryServiceV1.restoreDeck()`

### 阶段四: 测试与验证
1. [ ] 测试管理后台所有功能正常
2. [ ] 测试普通用户功能不受影响
3. [ ] 验证权限控制正确（非管理员无法访问admin接口）
4. [ ] 清理旧接口（如果完全迁移到admin路径的接口）

---

## 注意事项

1. **权限控制**: 所有 admin 接口必须添加 `@RequireRole(ADMIN)` 注解
2. **兼容性**: 拆分的接口需要保持普通接口向后兼容
3. **数据过滤**: 普通接口只返回已发布/正常状态的数据，admin接口返回所有状态
4. **错误处理**: admin接口需要返回更详细的错误信息
5. **日志记录**: 管理操作需要记录操作日志
6. **部署架构**: 所有管理后台功能必须使用 `/admin` 路径接口，以支持前后端分离部署，避免跨域问题

---

## 预计工作量

- 后端开发: 2-3天
- 前端调整: 1天
- 测试验证: 1天
- 总计: 4-5天

---

## 风险评估

1. **低风险**: 新增admin接口不影响现有功能
2. **中风险**: 迁移已有接口需要确保前端完全切换
3. **建议**: 先实现新接口，前端切换后再考虑删除旧接口

---

生成时间: 2025-10-29
