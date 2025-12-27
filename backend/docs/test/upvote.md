# 点赞接口测试文档 (UpvotesController)

## 测试文件

- **测试类**: `UpvotesControllerTest.java`
- **位置**: `learn-web/src/test/java/com/prosper/learn/web/v1/controller/`
- **API文档**: `docs/api/upvote.md`
- **Controller**: `UpvotesController.java`

## 测试框架配置

- 使用 `@SpringBootTest` 和 `@AutoConfigureMockMvc`
- 使用 `@Transactional` 实现测试后自动回滚
- 继承 `BaseControllerTest`

### 依赖注入

需要注入以下服务：
- `MockMvc` - 用于发送 HTTP 请求
- `PostDataService` - 帖子数据服务
- `CommentDataService` - 评论数据服务
- `RoadmapDataService` - 路线图数据服务
- `MemoryCardDeckDataService` - 记忆卡片组数据服务
- `UpvoteDataService` - 点赞数据服务
- `UserDataService` - 用户数据服务
- `CourseDataService` - 课程数据服务（帖子依赖）
- `NodeDataService` - 节点数据服务（帖子依赖）
- `ObjectMapper` - JSON 解析

---

## 必测场景清单

### 1. 帖子点赞 (POST /api/v1/upvotes, objectType=1)

#### 1.1 Twice 点赞完整流程
- [ ] 首次点 twice（type=1）→ 创建记录，返回 {twiceUpvoted: true, likeUpvoted: false}
- [ ] 验证数据库中存在 twice 点赞记录
- [ ] 重复点 twice → 取消点赞，返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 验证数据库中点赞记录已删除

#### 1.2 Like 点赞完整流程
- [ ] 首次点 like（type=2）→ 创建记录，返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 验证数据库中存在 like 点赞记录
- [ ] 重复点 like → 取消点赞，返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 验证数据库中点赞记录已删除

#### 1.3 点赞类型切换
- [ ] 先点 twice 再点 like → 切换为 like，返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 验证数据库中点赞记录类型已更新为 like
- [ ] 先点 like 再点 twice → 切换为 twice，返回 {twiceUpvoted: true, likeUpvoted: false}
- [ ] 验证数据库中点赞记录类型已更新为 twice

#### 1.4 异常场景
- [ ] 帖子不存在（objectId=99999）→ 返回 404
- [ ] 未登录点赞 → 返回 401

---

### 2. 评论点赞 (POST /api/v1/upvotes, objectType=3)

#### 2.1 Like 点赞流程（仅支持 like）
- [ ] 首次点 like → 创建记录，返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 验证数据库中存在 like 点赞记录
- [ ] 重复点 like → 取消点赞，返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 验证数据库中点赞记录已删除

#### 2.2 异常场景
- [ ] 评论不存在（objectId=99999）→ 返回 404
- [ ] 未登录点赞 → 返回 401

#### 2.3 边界场景（可选）
- [ ] 尝试点 twice（type=1）→ 应如何处理？（需要确认业务规则）
  - 选项1: 忽略 type 参数，统一按 like 处理
  - 选项2: 返回参数错误

---

### 3. 路线图点赞 (POST /api/v1/upvotes, objectType=4)

#### 3.1 Like 点赞流程（仅支持 like）
- [ ] 首次点 like → 创建记录，返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 验证数据库中存在点赞记录
- [ ] 重复点 like → 取消点赞，返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 验证数据库中点赞记录已删除

#### 3.2 异常场景
- [ ] 路线图不存在（objectId=99999）→ 返回 404
- [ ] 未登录点赞 → 返回 401

#### 3.3 特殊场景
- [ ] 路线图被删除后（deleted_at IS NOT NULL），已有的点赞状态如何处理？
- [ ] 路线图被封禁后（state=BANNED），是否还能点赞？

---

### 4. 记忆卡片组点赞 (POST /api/v1/upvotes, objectType=5)

#### 4.1 Like 点赞流程（仅支持 like）
- [ ] 首次点 like → 创建记录，返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 验证数据库中存在点赞记录
- [ ] 重复点 like → 取消点赞，返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 验证数据库中点赞记录已删除

#### 4.2 异常场景
- [ ] 卡片组不存在（objectId=99999）→ 返回 404
- [ ] 未登录点赞 → 返回 401

---

### 5. 获取点赞状态 (GET /api/v1/upvotes/status)

#### 5.1 正常场景
- [ ] 获取未点赞的状态 → 返回 {twiceUpvoted: false, likeUpvoted: false}
- [ ] 点 twice 后获取状态 → 返回 {twiceUpvoted: true, likeUpvoted: false}
- [ ] 点 like 后获取状态 → 返回 {twiceUpvoted: false, likeUpvoted: true}
- [ ] 切换点赞类型后获取状态 → 返回最新的点赞状态

#### 5.2 边界场景
- [ ] 获取不存在内容的点赞状态（objectId=99999）→ 返回 {twiceUpvoted: false, likeUpvoted: false}
  - 注意：不返回 404，而是返回默认未点赞状态

#### 5.3 异常场景
- [ ] 未登录获取状态 → 返回 401
- [ ] objectId = 0 → 返回 400
- [ ] objectType = 0 → 返回 400

---

### 6. 参数验证

#### 6.1 点赞接口参数验证 (POST /api/v1/upvotes)

**objectId 验证**:
- [ ] objectId = null → 返回 400，错误信息："对象ID不能为空"
- [ ] objectId = 0 → 返回 400，错误信息："对象ID必须大于0"
- [ ] objectId = -1 → 返回 400，错误信息："对象ID必须大于0"

**objectType 验证**:
- [ ] objectType = null → 返回 400，错误信息："对象类型不能为空"
- [ ] objectType = 0 → 返回 400，错误信息："对象类型必须大于0"
- [ ] objectType = -1 → 返回 400，错误信息："对象类型必须大于0"
- [ ] objectType = 99（不支持的类型）→ 返回 400，错误信息："不支持的内容类型"

**type 验证**:
- [ ] type = null → 返回 400，错误信息："投票类型不能为空"
- [ ] type = 0 → 返回 400，错误信息："投票类型不正确"
- [ ] type = 3 → 返回 400，错误信息："投票类型不正确"（有效范围：1-2）
- [ ] type = -1 → 返回 400，错误信息："投票类型不正确"

#### 6.2 获取状态接口参数验证 (GET /api/v1/upvotes/status)

**objectId 验证**:
- [ ] objectId = null → 返回 400
- [ ] objectId = 0 → 返回 400
- [ ] objectId = -1 → 返回 400

**objectType 验证**:
- [ ] objectType = null → 返回 400
- [ ] objectType = 0 → 返回 400
- [ ] objectType = -1 → 返回 400

---

### 7. 认证和授权

#### 7.1 未登录场景
- [ ] 未登录点赞（POST）→ 返回 401
- [ ] 未登录获取状态（GET）→ 返回 401

#### 7.2 已登录场景
- [ ] 已登录用户可以点赞任何已发布内容
- [ ] 已登录用户可以查看自己的点赞状态

---

### 8. 并发场景（可选，需要 JMeter）

#### 8.1 快速连续点击
- [ ] 用户快速连续点击同一个点赞按钮 → 验证数据一致性
- [ ] 验证最终数据库状态正确（不会出现重复记录）

#### 8.2 多设备同时点赞
- [ ] 同一用户在不同设备同时点赞同一内容 → 验证数据一致性
- [ ] 验证最终只有一条点赞记录

---

## 测试辅助方法

### 数据准备方法

建议创建以下辅助方法：

1. **createUser(String email)** - 创建测试用户
   - 返回创建的 UserDO 对象

2. **createPost(Long creatorId, Long nodeId)** - 创建测试帖子
   - 需要先创建 Course 和 Node
   - 返回创建的 PostDO 对象

3. **createComment(Long userId, Long objectId, ContentType objectType)** - 创建测试评论
   - 返回创建的 CommentDO 对象

4. **createRoadmap(Long creatorId)** - 创建测试路线图
   - 返回创建的 RoadmapDO 对象

5. **createDeck(Long creatorId)** - 创建测试记忆卡片组
   - 返回创建的 MemoryCardDeckDO 对象

6. **createCourse(Long creatorId)** - 创建测试课程
   - 返回创建的 CourseDO 对象

7. **createNode(Long courseId, Long parentId)** - 创建测试节点
   - 返回创建的 NodeDO 对象

### 验证方法

1. **assertUpvoteStatus(String response, boolean expectedTwice, boolean expectedLike)** - 验证点赞状态
   - 解析响应 JSON
   - 验证 twiceUpvoted 和 likeUpvoted 字段

2. **assertUpvoteExists(Long userId, Long objectId, Integer objectType, Integer expectedType)** - 验证数据库中的点赞记录
   - 查询数据库
   - 验证记录存在且类型正确

3. **assertUpvoteNotExists(Long userId, Long objectId, Integer objectType)** - 验证点赞记录已删除
   - 查询数据库
   - 验证记录不存在

---

## 测试执行顺序

建议按以下顺序编写和执行测试：

1. **基础功能测试**
   - 测试1: 帖子点赞完整流程
   - 测试5: 获取点赞状态

2. **其他内容类型测试**
   - 测试2: 评论点赞
   - 测试3: 路线图点赞
   - 测试4: 记忆卡片组点赞

3. **异常和边界测试**
   - 测试6: 参数验证
   - 测试7: 认证和授权

4. **性能测试（可选）**
   - 测试8: 并发场景

---

## 测试数据准备

### 帖子点赞测试数据
需要创建完整的依赖链：
1. 创建用户（2个：点赞用户 + 内容创建者）
2. 创建课程
3. 创建节点（使用课程的 rootNodeId）
4. 创建帖子（关联节点）
5. 登录点赞用户
6. 执行点赞操作

### 评论点赞测试数据
1. 创建用户
2. 创建帖子（参考上述步骤）
3. 创建评论（关联帖子）
4. 登录用户
5. 执行点赞操作

### 路线图点赞测试数据
1. 创建用户（2个）
2. 创建路线图
3. 登录用户
4. 执行点赞操作

### 记忆卡片组点赞测试数据
1. 创建用户（2个）
2. 创建记忆卡片组
3. 登录用户
4. 执行点赞操作

---

## 验证要点

### API 响应验证
- HTTP 状态码（200、400、401、404）
- 业务状态码（code 字段）
- 响应数据结构（twiceUpvoted、likeUpvoted 字段）
- 错误信息（message 字段）

### 数据库验证
- 点赞记录是否正确创建
- 点赞记录是否正确删除
- 点赞类型是否正确更新
- 不存在重复记录

### 业务逻辑验证
- Twice 和 Like 互斥（不能同时为 true）
- 重复点赞会取消
- 切换点赞类型会更新记录而非创建新记录
- 不同内容类型的点赞限制正确

---

## 断言示例说明

### API 响应断言
验证以下内容：
- `status().isOk()` - HTTP 200
- `jsonPath("$.code").value(StatusCode.OK.getCode())` - 业务码
- `jsonPath("$.data.twiceUpvoted").value(true/false)` - twice 状态
- `jsonPath("$.data.likeUpvoted").value(true/false)` - like 状态

### 数据库断言
验证以下内容：
- 记录存在性：`assertThat(upvote).isNotNull()` 或 `isNull()`
- 记录类型：`assertThat(upvote.getType()).isEqualTo(VoteType.twice.value())`
- 记录数量：`assertThat(upvotes).hasSize(1)` 或 `isEmpty()`

---

## 运行测试

```bash
# 运行所有点赞接口测试
mvn test -Dtest=UpvotesControllerTest

# 运行特定测试方法
mvn test -Dtest=UpvotesControllerTest#testPostUpvote_FullWorkflow

# 查看测试覆盖率
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## 测试覆盖率目标

- **行覆盖率**: ≥ 80%
- **分支覆盖率**: ≥ 75%
- **方法覆盖率**: 100%

---

## 注意事项

### 1. 测试隔离
- 每个测试方法使用 `@Transactional` 自动回滚
- 确保测试之间不互相影响
- 不依赖测试执行顺序

### 2. 数据准备
- 点赞测试需要创建完整的依赖链
- 使用辅助方法提高代码复用性
- 测试数据使用动态ID（数据库自动生成）

### 3. 状态验证
- 除了验证 API 响应，还要验证数据库中的实际记录
- 验证正向操作（创建、更新）和反向操作（删除）
- 验证边界条件和异常情况

### 4. 点赞类型限制
- Post 支持 twice 和 like 两种点赞
- Comment/Roadmap/MemoryCardDeck 仅支持 like 点赞
- 需要测试是否正确处理不支持的点赞类型（待确认业务规则）

### 5. 认证处理
- 使用 `StpUtil.login(userId)` 模拟登录
- 测试结束后使用 `StpUtil.logout()` 清理登录状态
- 使用 `try-finally` 确保登录状态正确清理

### 6. 并发测试
- 单元测试主要验证功能正确性
- 实际并发场景需要使用 JMeter 等压测工具
- 可以在单元测试中模拟简单的并发场景（使用 CompletableFuture）

### 7. 性能考虑
- 批量点赞查询避免 N+1 问题
- 点赞状态查询应使用批量接口
- 考虑使用 Redis 缓存热点数据的点赞状态

---

## 相关文档

- [测试编写规范](./TEST_GUIDE.md)
- [点赞接口文档](../api/upvote.md)
- [点赞业务逻辑](../../learn-interaction/README.md)
- [课程接口测试](./course.md)
