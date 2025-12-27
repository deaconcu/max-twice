# 帖子管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostsControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostService postService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private UpvoteDataService upvoteDataService;

    // 测试辅助方法见下方
}
```

### 必需的测试辅助方法

```java
/**
 * 创建测试用户
 */
private UserDO createUser(String email) {
    return userDomainService.createUser(email, "password123");
}

/**
 * 创建已发布课程
 */
private CourseDO createPublishedCourse(String name, Long creatorId) {
    CourseDO course = new CourseDO();
    course.setName(name);
    course.setDescription("课程描述");
    course.setCreatorId(creatorId);
    course.setState(ContentState.PUBLISHED.value());
    course.setMainCategory(1);
    course.setSubCategory(1);
    course.setRootNodeId(0L);
    course.setParentCourseId(0L);
    courseDataService.insert(course);

    // 创建根节点
    NodeDO rootNode = NodeDO.createRoot(creatorId, course.getId());
    nodeDataService.insert(rootNode);

    // 更新课程的 rootNodeId
    course.setRootNodeId(rootNode.getId());
    courseDataService.update(course);

    return course;
}

/**
 * 创建已发布节点
 */
private NodeDO createPublishedNode(String name, Long courseId, Long creatorId) {
    NodeDO node = new NodeDO();
    node.setName(name);
    node.setCourseId(courseId);
    node.setCreatorId(creatorId);
    node.setState(ContentState.PUBLISHED.value());
    node.setType(NodeType.REGULAR.value());
    nodeDataService.insert(node);
    return node;
}

/**
 * 创建已发布帖子
 */
private PostDO createPublishedPost(String content, Long nodeId, Long creatorId, PostType type) {
    PostDO post = new PostDO();
    post.setContent(content);
    post.setNodeId(nodeId);
    post.setCreatorId(creatorId);
    post.setType(type.value());
    post.setState(ContentState.PUBLISHED.value());
    post.setScore(0.0);
    postDataService.insert(post);
    return post;
}

/**
 * 创建指定状态的帖子
 */
private PostDO createPostWithState(String content, Long nodeId, Long creatorId, ContentState state) {
    PostDO post = new PostDO();
    post.setContent(content);
    post.setNodeId(nodeId);
    post.setCreatorId(creatorId);
    post.setType(PostType.ARTICLE.value());
    post.setState(state.value());
    post.setScore(0.0);
    postDataService.insert(post);
    return post;
}

/**
 * 创建投票记录
 */
private void createVote(Long userId, Long postId, VoteType voteType) {
    UpvoteDO upvote = new UpvoteDO();
    upvote.setObjectId(postId);
    upvote.setObjectType(ContentType.post.value());
    upvote.setUserId(userId);
    upvote.setType(voteType.value());
    upvoteDataService.insert(upvote);
}

/**
 * JSON 工具方法：在数组中查找指定 ID 的节点
 */
private JsonNode findById(JsonNode array, Long id) {
    for (JsonNode node : array) {
        if (node.get("id").asLong() == id) {
            return node;
        }
    }
    return null;
}
```

### 说明

1. **@Transactional - 自动清理数据**:
   - 每个 `@Test` 方法执行完后，**所有数据库操作都会自动回滚**
   - 包括帖子、评论、投票等所有操作
   - **无需手动清理数据**，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **依赖数据**:
   - 创建帖子前必须先创建用户、课程、节点
   - 使用辅助方法确保依赖数据的正确创建

---

## Command 测试（写操作）

### 1. 创建帖子 (POST /api/v1/posts)

#### 测试场景

##### 1.1 成功创建普通帖子
- **准备**：创建用户、课程、节点
- **请求**：POST /api/v1/posts，传入 content、nodeId、type=0
- **验证**：
  - 返回 200 状态码
  - 帖子已创建，state=SUBMITTED（待审核）
  - creatorId 自动填充为当前用户
  - score 初始化为 0.0

##### 1.2 成功创建文章帖子
- **准备**：创建用户、课程、节点
- **请求**：POST /api/v1/posts，传入 content、nodeId、type=2
- **验证**：帖子类型为 ARTICLE

##### 1.3 成功创建内容帖子（目录）
- **准备**：创建用户、课程、节点
- **请求**：POST /api/v1/posts，传入 content、nodeId、type=1
- **验证**：帖子类型为 CONTENT

##### 1.4 字段验证 - content 为空
- **请求**：content=""
- **验证**：返回 400，提示"内容不能为空"

##### 1.5 字段验证 - content 超长
- **请求**：content 长度超过配置的最大值
- **验证**：返回 400，提示"内容长度超出限制"

##### 1.6 字段验证 - nodeId 缺失
- **请求**：不传 nodeId
- **验证**：返回 400，提示"节点ID不能为空"

##### 1.7 字段验证 - nodeId 无效（0、负数）
- **请求**：nodeId=0 或 nodeId=-1
- **验证**：返回 400，提示"节点ID必须大于0"

##### 1.8 业务验证 - 节点不存在
- **请求**：nodeId=99999（不存在的节点）
- **验证**：返回 404，提示"节点不存在"

##### 1.9 业务验证 - 节点已被屏蔽
- **准备**：创建状态为 BANNED 的节点
- **请求**：在该节点下创建帖子
- **验证**：返回 403，提示"节点已被屏蔽"

##### 1.10 权限验证 - 未登录
- **请求**：不传 token
- **验证**：返回 401，提示"未登录"

---

### 2. 修改帖子 (PUT /api/v1/posts/{id})

#### 测试场景

##### 2.1 成功修改帖子内容
- **准备**：创建已发布帖子
- **请求**：PUT /api/v1/posts/{id}，传入新的 content
- **验证**：
  - 返回 200 和更新后的 PostSummaryDTO
  - content 已更新
  - updatedAt 时间已更新

##### 2.2 修改帖子后状态变为待审核
- **准备**：创建已发布帖子（state=PUBLISHED）
- **请求**：修改内容
- **验证**：state 变为 SUBMITTED（待审核）

##### 2.3 字段验证 - content 为空
- **请求**：content=""
- **验证**：返回 400

##### 2.4 权限验证 - 只有创建者可以修改
- **准备**：用户A创建帖子
- **请求**：用户B尝试修改
- **验证**：返回 403，提示"无权修改"

##### 2.5 权限验证 - 管理员可以修改任何帖子
- **准备**：普通用户创建帖子
- **请求**：管理员修改帖子
- **验证**：修改成功

##### 2.6 业务验证 - 帖子不存在
- **请求**：PUT /api/v1/posts/99999
- **验证**：返回 404

##### 2.7 业务验证 - 帖子ID无效（0、负数）
- **请求**：id=0 或 id=-1
- **验证**：返回 400

---

### 3. 删除帖子 (DELETE /api/v1/posts/{id})

#### 测试场景

##### 3.1 成功删除帖子（软删除）
- **准备**：创建已发布帖子
- **请求**：DELETE /api/v1/posts/{id}
- **验证**：
  - 返回 200
  - 帖子状态变为 DELETED
  - 帖子仍在数据库中（软删除）

##### 3.2 权限验证 - 只有创建者可以删除
- **准备**：用户A创建帖子
- **请求**：用户B尝试删除
- **验证**：返回 403

##### 3.3 权限验证 - 管理员可以删除任何帖子
- **准备**：普通用户创建帖子
- **请求**：管理员删除帖子
- **验证**：删除成功

##### 3.4 业务验证 - 帖子不存在
- **请求**：DELETE /api/v1/posts/99999
- **验证**：返回 404

##### 3.5 业务验证 - 重复删除
- **准备**：删除帖子一次
- **请求**：再次删除同一帖子
- **验证**：返回 404 或 400（已删除）

---

## Query 测试（读操作）

### 4. 获取帖子详情 (GET /api/v1/posts/{id})

#### 测试场景

##### 4.1 获取已发布帖子
- **准备**：创建已发布帖子
- **请求**：GET /api/v1/posts/{id}
- **验证**：
  - 返回 200 和 PostSummaryDTO
  - 包含所有基础字段（id, content, nodeId, creatorId, type, state, score, timestamps）

##### 4.2 获取待审核帖子
- **准备**：创建待审核帖子（state=SUBMITTED）
- **请求**：GET /api/v1/posts/{id}
- **验证**：可以获取，state=0

##### 4.3 无权限查看 - 已删除的帖子
- **准备**：创建已删除帖子（state=DELETED）
- **请求**：GET /api/v1/posts/{id}
- **验证**：返回 404 或 403

##### 4.4 帖子不存在
- **请求**：GET /api/v1/posts/99999
- **验证**：返回 404

##### 4.5 帖子ID无效（0、负数）
- **请求**：id=0 或 id=-1
- **验证**：返回 400

##### 4.6 不需要登录
- **准备**：创建已发布帖子
- **请求**：不传 token 访问
- **验证**：可以正常获取

---

### 5. 批量获取帖子 (GET /api/v1/posts?ids=1,2,3)

#### 测试场景

##### 5.1 按 IDs 批量查询
- **准备**：创建3个已发布帖子
- **请求**：GET /api/v1/posts?ids=1,2,3
- **验证**：
  - 返回 200 和 List<PostWithVoteDTO>
  - 返回3个帖子
  - 每个帖子包含 userVote 字段（当前用户的投票状态）

##### 5.2 批量查询 - 包含用户投票信息
- **准备**：创建2个帖子，用户对第1个帖子点赞（like）
- **请求**：GET /api/v1/posts?ids=1,2（已登录）
- **验证**：
  - 帖子1的 userVote=2（点赞，VoteType.like）
  - 帖子2的 userVote 为 null 或不存在（未投票）

##### 5.3 批量查询 - 部分ID不存在
- **准备**：创建帖子1、2
- **请求**：GET /api/v1/posts?ids=1,2,99999
- **验证**：只返回存在的帖子（1、2）

##### 5.4 批量查询 - ids 为空
- **请求**：GET /api/v1/posts?ids=
- **验证**：返回 400，提示"必须提供 ids 或 nodeId 参数"

##### 5.5 权限验证 - 需要登录
- **请求**：不传 token
- **验证**：返回 401

---

### 6. 按节点分页查询帖子 (GET /api/v1/posts?nodeId=10)

#### 测试场景

##### 6.1 获取节点下的帖子（第一页）
- **准备**：在节点10下创建5个帖子
- **请求**：GET /api/v1/posts?nodeId=10
- **验证**：
  - 返回 200 和 KeysetPageResponse<PostWithVoteDTO>
  - items 包含5个帖子
  - hasMore=false（数量不足20条）
  - 按 score 倒序排列

##### 6.2 分页查询 - 使用游标
- **准备**：在节点下创建25个帖子
- **请求**：
  - 第一次：GET /api/v1/posts?nodeId=10
  - 第二次：GET /api/v1/posts?nodeId=10&lastScore=X&lastId=Y
- **验证**：
  - 第一页返回20条，hasMore=true，包含 nextCursor
  - 第二页返回5条，hasMore=false

##### 6.3 分页查询 - 游标分页逻辑
- **准备**：创建多个帖子，设置不同的 score
- **验证**：
  - 返回的帖子按 (score DESC, id DESC) 排序
  - 第二页的所有帖子 score ≤ lastScore
  - 当 score 相等时，id < lastId

##### 6.4 空结果 - 节点下无帖子
- **准备**：创建空节点
- **请求**：GET /api/v1/posts?nodeId=10
- **验证**：返回空数组，hasMore=false

##### 6.5 参数验证 - nodeId 无效（0、负数）
- **请求**：nodeId=0 或 nodeId=-1
- **验证**：返回 400

##### 6.6 参数验证 - lastId 为负数
- **请求**：GET /api/v1/posts?nodeId=10&lastId=-1
- **验证**：返回 400

##### 6.7 参数冲突 - 同时传 ids 和 nodeId
- **请求**：GET /api/v1/posts?ids=1,2&nodeId=10
- **验证**：优先使用 ids 参数（返回按 IDs 查询的结果）

##### 6.8 权限验证 - 需要登录
- **请求**：不传 token
- **验证**：返回 401

---

### 7. 获取用户帖子 (GET /api/v1/users/{userId}/posts)

#### 测试场景

##### 7.1 获取用户的文章列表（type=2）
- **准备**：用户创建5篇文章（type=2）和3个内容帖（type=1）
- **请求**：GET /api/v1/users/{userId}/posts?type=2
- **验证**：
  - 返回 200 和 KeysetPageResponse<PostFullDTO>
  - 只返回5篇文章（type=2）
  - 不包含内容帖（type=1）

##### 7.2 获取用户的内容帖列表（type=1）
- **准备**：用户创建3个内容帖（type=1）
- **请求**：GET /api/v1/users/{userId}/posts?type=1
- **验证**：只返回内容帖

##### 7.3 只返回已发布的帖子
- **准备**：用户创建5篇文章，其中2篇待审核、1篇已拒绝、2篇已发布
- **请求**：GET /api/v1/users/{userId}/posts?type=2
- **验证**：只返回2篇已发布的文章（state=PUBLISHED）

##### 7.4 分页查询
- **准备**：用户创建25篇文章
- **请求**：
  - 第一次：GET /api/v1/users/{userId}/posts?type=2
  - 第二次：GET /api/v1/users/{userId}/posts?type=2&lastId=X
- **验证**：
  - 第一页20条，hasMore=true
  - 第二页5条，hasMore=false

##### 7.5 空结果 - 用户无帖子
- **准备**：创建新用户，未创建任何帖子
- **请求**：GET /api/v1/users/{userId}/posts?type=2
- **验证**：返回空数组

##### 7.6 参数验证 - userId 无效（0、负数）
- **请求**：userId=0 或 userId=-1
- **验证**：返回 400

##### 7.7 参数验证 - type 无效
- **请求**：type=99（无效类型）
- **验证**：返回 400，提示"无效的帖子类型"

##### 7.8 默认类型 - 不传 type 参数
- **请求**：GET /api/v1/users/{userId}/posts（不传 type）
- **验证**：默认返回 type=2（文章）

##### 7.9 不需要登录
- **请求**：不传 token
- **验证**：可以正常获取

---

### 8. 获取当前用户所有状态的帖子 (GET /api/v1/users/me/posts)

#### 测试场景

##### 8.1 获取当前用户所有状态的文章
- **准备**：当前用户创建4篇文章：待审核、已发布、已拒绝、已屏蔽
- **请求**：GET /api/v1/users/me/posts?type=2
- **验证**：
  - 返回所有4篇文章
  - 包含所有状态（SUBMITTED, PUBLISHED, REJECTED, BANNED）

##### 8.2 获取当前用户所有状态的内容帖
- **准备**：当前用户创建3个内容帖，不同状态
- **请求**：GET /api/v1/users/me/posts?type=1
- **验证**：返回所有内容帖

##### 8.3 分页查询
- **准备**：当前用户创建25篇文章
- **请求**：使用 lastId 分页
- **验证**：正确分页

##### 8.4 与公开接口对比 - 返回所有状态
- **准备**：当前用户创建5篇文章，只有2篇已发布
- **对比**：
  - GET /api/v1/users/{userId}/posts → 只返回2篇已发布
  - GET /api/v1/users/me/posts → 返回全部5篇

##### 8.5 权限验证 - 必须登录
- **请求**：不传 token
- **验证**：返回 401

##### 8.6 只能查看自己的内容
- **准备**：用户A登录
- **请求**：GET /api/v1/users/me/posts
- **验证**：只返回用户A的帖子，不包含其他用户的

---

## 参数验证测试

### 9. 通用参数验证

#### 测试场景

##### 9.1 ID参数验证（各接口）
- **测试接口**：
  - GET /api/v1/posts/{id}
  - PUT /api/v1/posts/{id}
  - DELETE /api/v1/posts/{id}
- **测试用例**：
  - id=0 → 返回 400
  - id=-1 → 返回 400
  - id 非数字 → 返回 400

##### 9.2 分页参数验证
- **测试用例**：
  - lastId=-1 → 返回 400
  - lastScore 非数字 → 返回 400

---

## 性能测试

### 10. 批量查询性能

#### 测试场景

##### 10.1 按 IDs 批量查询 - 避免 N+1 问题
- **准备**：创建20个帖子
- **请求**：GET /api/v1/posts?ids=1,2,3,...,20
- **验证**：
  - 一次性返回所有帖子
  - 包含用户投票信息
  - 查询次数可控（避免 N+1）

##### 10.2 按节点分页查询 - 批量加载关联数据
- **准备**：在节点下创建20个帖子
- **请求**：GET /api/v1/posts?nodeId=10
- **验证**：
  - 一次性返回20个帖子及其投票信息
  - 避免对每个帖子单独查询投票

---

## 边界测试

### 11. 边界场景

#### 测试场景

##### 11.1 空数据库查询
- **请求**：查询不存在的节点下的帖子
- **验证**：返回空数组，不报错

##### 11.2 大量数据分页
- **准备**：创建50个帖子
- **验证**：
  - 分页功能正常（每页20条）
  - hasMore 标志正确
  - nextCursor 正确

##### 11.3 极端参数值
- **测试用例**：
  - content 长度为1（最小值）
  - content 长度接近最大值
  - score 为负数
  - 批量查询 ids 数量很多（100个）

##### 11.4 并发创建帖子
- **场景**：同一用户同时创建多个帖子
- **验证**：所有帖子都能成功创建

---

## 特殊业务场景测试

### 12. 投票相关测试

#### 测试场景

##### 12.1 获取帖子时包含用户投票
- **准备**：用户对帖子点赞（VoteType.like, value=2）
- **请求**：GET /api/v1/posts?ids={id}
- **验证**：返回的 PostWithVoteDTO 中 userVote=2

##### 12.2 未投票时的默认值
- **准备**：用户未对帖子投票
- **请求**：GET /api/v1/posts?ids={id}
- **验证**：userVote 为 null 或不存在

##### 12.3 未登录时无投票信息
- **请求**：不登录访问（公开接口）
- **验证**：userVote 为 null 或不存在

##### 12.4 投票类型说明
- **系统支持的投票类型**：
  - `VoteType.twice`（value=1）：twice 投票
  - `VoteType.like`（value=2）：点赞
- **未投票状态**：userVote 字段为 null 或不存在
- **存储结构**：使用 UpvoteDO，包含 objectId（帖子ID）、objectType（ContentType.post）、userId、type（投票类型）

---

## 状态流转测试

### 13. 帖子状态流转

#### 测试场景

##### 13.1 创建 → 待审核
- **操作**：创建帖子
- **验证**：state=SUBMITTED

##### 13.2 修改已发布帖子 → 待审核
- **准备**：已发布帖子（state=PUBLISHED）
- **操作**：修改内容
- **验证**：state 变为 SUBMITTED

##### 13.3 删除 → 已删除
- **准备**：任意状态的帖子
- **操作**：删除
- **验证**：state=DELETED

##### 13.4 审核通过 → 已发布
- **准备**：待审核帖子（state=SUBMITTED）
- **操作**：管理员审核通过
- **验证**：state=PUBLISHED

##### 13.5 审核拒绝 → 已拒绝
- **准备**：待审核帖子
- **操作**：管理员拒绝
- **验证**：state=REJECTED

---

## 测试执行顺序建议

1. 先执行 **Command 测试**（1-3）- 确保写操作正常
2. 再执行 **Query 测试**（4-8）- 确保读操作正常
3. 然后执行 **参数验证测试**（9）- 确保输入验证完善
4. 最后执行 **性能、边界、业务场景测试**（10-13）- 确保系统健壮性

---

## 注意事项

1. **依赖数据创建顺序**：用户 → 课程 → 节点 → 帖子
2. **状态管理**：注意帖子的不同状态对查询结果的影响
3. **权限控制**：区分已登录/未登录、创建者/非创建者、普通用户/管理员
4. **分页测试**：使用 KeysetPageResponse，验证 hasMore 和 nextCursor
5. **软删除**：删除操作是软删除，数据仍在数据库中
6. **投票信息**：需要登录才能获取用户投票状态
