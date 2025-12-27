# 评论管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CommentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentDataService commentDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private CourseDataService courseDataService;

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
private PostDO createPublishedPost(String content, Long nodeId, Long creatorId) {
    PostDO post = new PostDO();
    post.setContent(content);
    post.setNodeId(nodeId);
    post.setCreatorId(creatorId);
    post.setType(PostType.ARTICLE.value());
    post.setState(ContentState.PUBLISHED.value());
    post.setScore(0.0);
    postDataService.insert(post);
    return post;
}

/**
 * 创建已发布顶级评论
 */
private CommentDO createPublishedComment(String content, Long objectId, ObjectType objectType, Long creatorId) {
    CommentDO comment = new CommentDO();
    comment.setContent(content);
    comment.setObjectId(objectId);
    comment.setObjectType(objectType.value());
    comment.setCreatorId(creatorId);
    comment.setReplyToCommentId(null);  // 顶级评论
    comment.setToUserId(null);
    comment.setState(ContentState.APPROVED.value());
    comment.setScore(0.0);
    comment.setUpvoteCount(0);
    comment.setReplyCount(0);
    commentDataService.insert(comment);
    return comment;
}

/**
 * 创建已发布回复评论
 */
private CommentDO createPublishedReply(String content, Long objectId, ObjectType objectType,
                                       Long creatorId, Long replyToCommentId, Long toUserId) {
    CommentDO comment = new CommentDO();
    comment.setContent(content);
    comment.setObjectId(objectId);
    comment.setObjectType(objectType.value());
    comment.setCreatorId(creatorId);
    comment.setReplyToCommentId(replyToCommentId);
    comment.setToUserId(toUserId);
    comment.setState(ContentState.APPROVED.value());
    comment.setScore(0.0);
    comment.setUpvoteCount(0);
    comment.setReplyCount(0);
    commentDataService.insert(comment);
    return comment;
}

/**
 * 创建指定状态的评论
 */
private CommentDO createCommentWithState(String content, Long objectId, ObjectType objectType,
                                         Long creatorId, ContentState state) {
    CommentDO comment = new CommentDO();
    comment.setContent(content);
    comment.setObjectId(objectId);
    comment.setObjectType(objectType.value());
    comment.setCreatorId(creatorId);
    comment.setState(state.value());
    comment.setScore(0.0);
    comment.setUpvoteCount(0);
    comment.setReplyCount(0);
    commentDataService.insert(comment);
    return comment;
}

/**
 * 创建评论点赞记录
 */
private void createCommentUpvote(Long userId, Long commentId) {
    UpvoteDO upvote = new UpvoteDO();
    upvote.setObjectId(commentId);
    upvote.setObjectType(ContentType.COMMENT.value());
    upvote.setUserId(userId);
    upvote.setType(1);  // 点赞
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
   - 包括评论、回复、点赞等所有操作
   - **无需手动清理数据**，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **依赖数据**:
   - 创建评论前必须先创建用户、评论对象（帖子/节点等）
   - 使用辅助方法确保依赖数据的正确创建

4. **两层评论结构**:
   - `replyToCommentId` 始终指向顶级评论
   - `toUserId` 指向被回复的用户

---

## Command 测试（写操作）

### 1. 创建评论 (POST /api/v1/comments)

#### 测试场景

##### 1.1 成功创建顶级评论 - 评论帖子
- **准备**：创建用户、帖子
- **请求**：POST /api/v1/comments
  ```json
  {
    "objectId": 123,
    "objectType": 0,
    "content": "这是一条评论..."
  }
  ```
- **验证**：
  - 返回 200 状态码和 CommentDetailDTO
  - 评论已创建，state=APPROVED（已通过）
  - replyToCommentId=null，toUserId=null（顶级评论）
  - creatorId 自动填充为当前用户
  - score 初始化为 0.0
  - upvoteCount=0, replyCount=0
  - creatorName 已填充
  - upvoted=false（刚创建，未点赞）

##### 1.2 成功创建顶级评论 - 评论节点
- **准备**：创建用户、课程、节点
- **请求**：POST /api/v1/comments，objectType=1（节点）
- **验证**：
  - 评论已创建
  - objectType=1
  - replyToCommentId=null

##### 1.3 成功创建回复评论 - 回复顶级评论
- **准备**：创建用户A、帖子、用户A创建的顶级评论
- **请求**：用户B创建回复
  ```json
  {
    "objectId": 123,
    "objectType": 0,
    "replyTo": 10,
    "toUser": A_userId,
    "content": "回复内容..."
  }
  ```
- **验证**：
  - 返回 200 和 CommentDetailDTO
  - replyToCommentId=10（指向顶级评论）
  - toUserId=A_userId（指向被回复用户）
  - toUserName 已填充
  - 父评论的 replyCount 增加1

##### 1.4 成功创建子回复 - 回复子评论
- **准备**：
  - 用户A创建顶级评论（commentId=10）
  - 用户B回复评论10（commentId=11）
- **请求**：用户C回复评论11
  ```json
  {
    "objectId": 123,
    "objectType": 0,
    "replyTo": 10,
    "toUser": B_userId,
    "content": "回复B的内容..."
  }
  ```
- **验证**：
  - replyToCommentId=10（仍指向顶级评论，不是11）
  - toUserId=B_userId（指向评论11的作者）
  - 顶级评论的 replyCount 增加1

##### 1.5 字段验证 - content 为空
- **请求**：content=""
- **验证**：返回 400，提示"评论内容不能为空"

##### 1.6 字段验证 - content 超长
- **请求**：content 长度超过配置的最大值
- **验证**：返回 400，提示"内容长度超出限制"

##### 1.7 字段验证 - objectId 缺失
- **请求**：不传 objectId
- **验证**：返回 400，提示"对象ID不能为空"

##### 1.8 字段验证 - objectId 无效（0、负数）
- **请求**：objectId=0 或 objectId=-1
- **验证**：返回 400，提示"对象ID必须大于0"

##### 1.9 字段验证 - objectType 缺失
- **请求**：不传 objectType
- **验证**：返回 400，提示"对象类型不能为空"

##### 1.10 字段验证 - objectType 超出范围
- **请求**：objectType=99（不在 0-4 范围）
- **验证**：返回 400，提示"对象类型必须在0-4之间"

##### 1.11 字段验证 - replyTo 无效（负数）
- **请求**：replyTo=-1
- **验证**：返回 400，提示"回复评论的ID必须大于0"

##### 1.12 字段验证 - toUser 无效（负数）
- **请求**：toUser=-1
- **验证**：返回 400，提示"用户ID必须大于0"

##### 1.13 业务验证 - 评论对象不存在
- **请求**：objectId=99999（不存在的帖子）
- **验证**：返回 404，提示"评论对象不存在"

##### 1.14 业务验证 - 回复的评论不存在
- **请求**：replyTo=99999（不存在的评论）
- **验证**：返回 404，提示"被回复的评论不存在"

##### 1.15 业务验证 - 被回复用户不存在
- **请求**：toUser=99999（不存在的用户）
- **验证**：返回 404，提示"被回复的用户不存在"

##### 1.16 业务验证 - replyTo 和 toUser 不匹配
- **准备**：评论A（作者=用户A）
- **请求**：replyTo=评论A，但 toUser=用户B（不是评论A的作者）
- **验证**：返回 400 或通过（取决于业务规则）

##### 1.17 权限验证 - 未登录
- **请求**：不传 token
- **验证**：返回 401，提示"未登录"

---

## Query 测试（读操作）

### 2. 获取对象评论列表 (GET /api/v1/comments?objectId=123&objectType=0)

#### 测试场景

##### 2.1 获取帖子的评论列表（带子评论）
- **准备**：
  - 创建帖子123
  - 创建3条顶级评论（commentId=1, 2, 3）
  - 评论1有2条回复（commentId=4, 5）
  - 评论2有1条回复（commentId=6）
- **请求**：GET /api/v1/comments?objectId=123&objectType=0
- **验证**：
  - 返回 200 和 KeysetPageResponse<CommentWithRepliesDTO>
  - items 包含3条顶级评论
  - 评论1的 children 包含2条回复（commentId=4, 5）
  - 评论2的 children 包含1条回复（commentId=6）
  - 评论3的 children 为空数组
  - 每条评论包含 creatorName, toUserName, upvoted 字段

##### 2.2 获取节点的评论列表
- **准备**：在节点下创建5条评论
- **请求**：GET /api/v1/comments?objectId=10&objectType=1
- **验证**：
  - 返回5条评论
  - objectType=1（节点）

##### 2.3 排序规则 - 按 score 和 id 降序
- **准备**：创建多条评论，设置不同的 score
  - 评论1：score=100, id=1
  - 评论2：score=100, id=2
  - 评论3：score=50, id=3
- **请求**：GET /api/v1/comments
- **验证**：
  - 返回顺序：评论2（score=100, id=2）→ 评论1（score=100, id=1）→ 评论3（score=50, id=3）
  - score 高的在前，score 相同时 id 大的在前

##### 2.4 分页查询 - 第一页
- **准备**：创建25条顶级评论
- **请求**：GET /api/v1/comments?objectId=123&objectType=0
- **验证**：
  - 返回20条评论（默认分页大小）
  - hasMore=true
  - nextCursor 包含 lastScore 和 lastId

##### 2.5 分页查询 - 使用游标
- **准备**：创建25条评论
- **请求**：
  - 第一次：GET /api/v1/comments?objectId=123&objectType=0
  - 第二次：GET /api/v1/comments?objectId=123&objectType=0&lastScore=X&lastId=Y
- **验证**：
  - 第一页返回20条，hasMore=true
  - 第二页返回5条，hasMore=false

##### 2.6 分页查询 - 游标分页逻辑
- **准备**：创建多条评论，设置不同的 score
- **验证**：
  - 第二页的所有评论 score ≤ lastScore
  - 当 score 相等时，id < lastId

##### 2.7 子评论数量限制
- **准备**：顶级评论有10条回复
- **请求**：GET /api/v1/comments
- **验证**：
  - children 可能只返回前3条回复（根据业务规则）
  - replyCount=10（显示总数）
  - 需要通过"获取评论回复"接口查看更多

##### 2.8 评论点赞状态 - 已点赞
- **准备**：
  - 用户A创建评论
  - 当前登录用户对评论点赞
- **请求**：GET /api/v1/comments（已登录）
- **验证**：
  - 评论的 upvoted=true
  - upvoteCount 已增加

##### 2.9 评论点赞状态 - 未点赞
- **准备**：用户A创建评论，当前用户未点赞
- **请求**：GET /api/v1/comments（已登录）
- **验证**：upvoted=false

##### 2.10 空结果 - 对象无评论
- **准备**：创建帖子，未创建任何评论
- **请求**：GET /api/v1/comments?objectId=123&objectType=0
- **验证**：返回空数组，hasMore=false

##### 2.11 参数验证 - objectId 缺失
- **请求**：GET /api/v1/comments?objectType=0
- **验证**：返回 400，提示"对象ID不能为空"

##### 2.12 参数验证 - objectType 缺失
- **请求**：GET /api/v1/comments?objectId=123
- **验证**：返回 400，提示"对象类型不能为空"

##### 2.13 参数验证 - objectId 无效（0、负数）
- **请求**：objectId=0 或 objectId=-1
- **验证**：返回 400

##### 2.14 参数验证 - objectType 无效
- **请求**：objectType=0 或 objectType=-1
- **验证**：返回 400

##### 2.15 参数验证 - lastId 为负数
- **请求**：lastId=-1
- **验证**：返回 400

##### 2.16 权限验证 - 需要登录
- **请求**：不传 token
- **验证**：返回 401

---

### 3. 获取评论回复列表 (GET /api/v1/comments/{id}/replies)

#### 测试场景

##### 3.1 获取评论的所有回复
- **准备**：
  - 创建顶级评论（commentId=10）
  - 创建5条回复（commentId=11-15）
- **请求**：GET /api/v1/comments/10/replies
- **验证**：
  - 返回 200 和 KeysetPageResponse<CommentDetailDTO>
  - items 包含5条回复
  - 所有回复的 replyToCommentId=10
  - 不包含子评论（children 字段不存在）

##### 3.2 排序规则 - 按 score 和 id 降序
- **准备**：创建多条回复，设置不同的 score
- **请求**：GET /api/v1/comments/10/replies
- **验证**：
  - 按 (score DESC, id DESC) 排序
  - score 高的在前

##### 3.3 分页查询 - 第一页
- **准备**：创建25条回复
- **请求**：GET /api/v1/comments/10/replies
- **验证**：
  - 返回20条（默认分页大小）
  - hasMore=true
  - nextCursor 包含 lastScore 和 lastId

##### 3.4 分页查询 - 使用游标
- **准备**：创建25条回复
- **请求**：
  - 第一次：GET /api/v1/comments/10/replies
  - 第二次：GET /api/v1/comments/10/replies?lastScore=X&lastId=Y
- **验证**：
  - 第一页20条，第二页5条
  - hasMore 标志正确

##### 3.5 回复的点赞状态
- **准备**：当前用户对某条回复点赞
- **请求**：GET /api/v1/comments/10/replies（已登录）
- **验证**：
  - 被点赞的回复 upvoted=true
  - 其他回复 upvoted=false

##### 3.6 空结果 - 评论无回复
- **准备**：创建顶级评论，未创建回复
- **请求**：GET /api/v1/comments/10/replies
- **验证**：返回空数组，hasMore=false

##### 3.7 业务验证 - 评论不存在
- **请求**：GET /api/v1/comments/99999/replies
- **验证**：返回 404，提示"评论不存在"

##### 3.8 参数验证 - 评论ID无效（0、负数）
- **请求**：id=0 或 id=-1
- **验证**：返回 400

##### 3.9 参数验证 - lastId 为负数
- **请求**：lastId=-1
- **验证**：返回 400

##### 3.10 权限验证 - 需要登录
- **请求**：不传 token
- **验证**：返回 401

---

## 参数验证测试

### 4. 通用参数验证

#### 测试场景

##### 4.1 ID参数验证（各接口）
- **测试接口**：
  - GET /api/v1/comments/{id}/replies
- **测试用例**：
  - id=0 → 返回 400
  - id=-1 → 返回 400
  - id 非数字 → 返回 400

##### 4.2 objectType 参数验证
- **测试用例**：
  - objectType=-1 → 返回 400
  - objectType=5 → 返回 400（超出 0-4 范围）
  - objectType 非数字 → 返回 400

##### 4.3 分页参数验证
- **测试用例**：
  - lastId=-1 → 返回 400
  - lastScore 非数字 → 返回 400

---

## 性能测试

### 5. 批量查询性能

#### 测试场景

##### 5.1 获取评论列表 - 避免 N+1 问题
- **准备**：创建20条顶级评论，每条有5条回复
- **请求**：GET /api/v1/comments?objectId=123&objectType=0
- **验证**：
  - 一次性返回20条顶级评论及其回复（100条子评论）
  - 用户名批量加载（creatorName, toUserName）
  - 点赞状态批量加载（upvoted）
  - 查询次数可控，避免 N+1

##### 5.2 获取回复列表 - 批量加载关联数据
- **准备**：创建20条回复
- **请求**：GET /api/v1/comments/10/replies
- **验证**：
  - 一次性返回20条回复及其关联数据
  - 避免对每条回复单独查询用户名和点赞状态

---

## 边界测试

### 6. 边界场景

#### 测试场景

##### 6.1 空数据库查询
- **请求**：查询不存在对象的评论
- **验证**：返回空数组，不报错

##### 6.2 大量数据分页
- **准备**：创建50条评论
- **验证**：
  - 分页功能正常（每页20条）
  - hasMore 标志正确
  - nextCursor 正确

##### 6.3 极端参数值
- **测试用例**：
  - content 长度为1（最小值）
  - content 长度接近最大值
  - score 为负数
  - upvoteCount 为负数

##### 6.4 并发创建评论
- **场景**：同一用户同时创建多条评论
- **验证**：所有评论都能成功创建

---

## 特殊业务场景测试

### 7. 评论结构测试

#### 测试场景

##### 7.1 两层评论结构验证
- **准备**：
  - 用户A创建顶级评论（commentId=10）
  - 用户B回复评论10（commentId=11）
  - 用户C回复评论11（commentId=12）
- **验证**：
  - 评论11：replyToCommentId=10, toUserId=A
  - 评论12：replyToCommentId=10（仍指向顶级评论），toUserId=B
  - 评论10的 replyCount=2（包含评论11和12）

##### 7.2 回复计数准确性
- **准备**：
  - 创建顶级评论
  - 创建5条回复
- **验证**：
  - 顶级评论的 replyCount=5
  - 每创建一条回复，replyCount 增加1

##### 7.3 删除回复后计数更新
- **准备**：
  - 顶级评论有5条回复（replyCount=5）
  - 删除1条回复
- **验证**：
  - replyCount=4
  - 删除的回复不再显示

##### 7.4 评论嵌套查询 - children 只显示部分
- **准备**：顶级评论有10条回复
- **请求**：GET /api/v1/comments
- **验证**：
  - children 可能只返回3条（前3条或热门3条）
  - replyCount=10（显示总数）
  - 需要"获取评论回复"接口查看全部

---

### 8. 点赞相关测试

#### 测试场景

##### 8.1 获取评论时包含点赞状态
- **准备**：用户对评论点赞
- **请求**：GET /api/v1/comments（已登录）
- **验证**：upvoted=true, upvoteCount 已增加

##### 8.2 未点赞时的默认值
- **准备**：用户未对评论点赞
- **请求**：GET /api/v1/comments（已登录）
- **验证**：upvoted=false

##### 8.3 点赞计数准确性
- **准备**：
  - 创建评论
  - 3个用户对其点赞
- **验证**：upvoteCount=3

##### 8.4 取消点赞后计数更新
- **准备**：用户对评论点赞（upvoteCount=1）
- **操作**：取消点赞
- **验证**：upvoteCount=0, upvoted=false

---

### 9. 状态流转测试

#### 测试场景

##### 9.1 创建 → 已通过
- **操作**：创建评论
- **验证**：state=APPROVED（自动通过，无需审核）

##### 9.2 审核 → 待审核（如果启用审核）
- **准备**：如果系统启用评论审核
- **操作**：创建评论
- **验证**：state=PENDING

##### 9.3 审核通过 → 已通过
- **准备**：待审核评论（state=PENDING）
- **操作**：管理员审核通过
- **验证**：state=APPROVED

##### 9.4 审核拒绝 → 已拒绝
- **准备**：待审核评论
- **操作**：管理员拒绝
- **验证**：state=REJECTED

##### 9.5 封禁 → 已封禁
- **准备**：已通过评论
- **操作**：管理员封禁
- **验证**：state=BANNED

---

## 对象类型测试

### 10. 不同对象类型的评论

#### 测试场景

##### 10.1 评论帖子（objectType=0）
- **准备**：创建帖子
- **请求**：objectType=0
- **验证**：评论创建成功

##### 10.2 评论节点（objectType=1）
- **准备**：创建节点
- **请求**：objectType=1
- **验证**：评论创建成功

##### 10.3 评论课程（objectType=2）
- **准备**：创建课程
- **请求**：objectType=2
- **验证**：评论创建成功

##### 10.4 评论用户（objectType=3）
- **准备**：创建用户
- **请求**：objectType=3
- **验证**：评论创建成功

##### 10.5 评论路线图（objectType=4）
- **准备**：创建路线图
- **请求**：objectType=4
- **验证**：评论创建成功

##### 10.6 不同对象类型的评论互不干扰
- **准备**：
  - 帖子123有5条评论
  - 节点123有3条评论
- **请求**：
  - GET /api/v1/comments?objectId=123&objectType=0 → 返回5条
  - GET /api/v1/comments?objectId=123&objectType=1 → 返回3条
- **验证**：不同对象类型的评论分开存储和查询

---

## 测试执行顺序建议

1. 先执行 **Command 测试**（1）- 确保写操作正常
2. 再执行 **Query 测试**（2-3）- 确保读操作正常
3. 然后执行 **参数验证测试**（4）- 确保输入验证完善
4. 再执行 **性能、边界测试**（5-6）- 确保系统健壮性
5. 最后执行 **业务场景测试**（7-10）- 确保业务逻辑正确

---

## 注意事项

1. **依赖数据创建顺序**：用户 → 评论对象（帖子/节点/课程等）→ 评论 → 回复
2. **状态管理**：注意评论的不同状态对查询结果的影响
3. **权限控制**：所有评论接口都需要登录
4. **分页测试**：使用 KeysetPageResponse，验证 hasMore 和 nextCursor
5. **两层评论结构**：
   - `replyToCommentId` 始终指向顶级评论
   - `toUserId` 指向被回复的用户
6. **点赞状态**：需要登录才能获取用户点赞状态
7. **子评论数量**：`CommentWithRepliesDTO.children` 可能只返回部分回复
8. **用户名填充**：`creatorName` 和 `toUserName` 需要动态查询填充
9. **排序规则**：评论按 (score DESC, id DESC) 排序，热门评论优先

---
