# 课程管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseDomainService courseDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserCourseService userCourseService;

    @Autowired
    private ContentStatsDataService contentStatsDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 测试辅助方法见下方
}
```

### 必需的测试辅助方法

```java
/**
 * 创建测试用户
 */
private UserDO createUser(String email) {
    UserDO user = new UserDO();
    user.setEmail(email);
    user.setUsername(email.split("@")[0]);
    user.setPassword("hashed_password");
    user.setState(UserState.ACTIVE.value());
    userDataService.insert(user);
    return user;
}

/**
 * 创建已发布课程（基础信息）
 */
private CourseDO createPublishedCourse(String name, String description) {
    Long courseId = courseDomainService.create(1L, name, description, 1, 1);
    courseDomainService.approve(courseId, false, false); // 审核通过
    return courseDataService.getById(courseId);
}

/**
 * 创建已发布课程（指定分类）
 */
private CourseDO createPublishedCourseWithCategory(String name, int mainCategory, int subCategory) {
    Long courseId = courseDomainService.create(1L, name, "描述", mainCategory, subCategory);
    courseDomainService.approve(courseId, false, false);
    return courseDataService.getById(courseId);
}

/**
 * 创建指定状态的课程
 */
private CourseDO createCourseWithState(String name, ContentState state) {
    Long courseId = courseDomainService.create(1L, name, "描述", 1, 1);
    CourseDO course = courseDataService.getById(courseId);

    // 根据目标状态执行相应操作
    if (state == ContentState.PUBLISHED) {
        courseDomainService.approve(courseId, false, false);
    } else if (state == ContentState.REJECTED) {
        courseDomainService.reject(courseId, "测试拒绝", false, false);
    } else if (state == ContentState.BANNED) {
        courseDomainService.ban(courseId, "测试封禁", false, false);
    }
    // SUBMITTED 状态不需要额外操作

    return courseDataService.getById(courseId);
}

/**
 * 创建子课程
 */
private CourseDO createPublishedSubCourse(String name, Long parentCourseId) {
    Long subCourseId = courseDomainService.createSubCourse(1L, name, "子课程描述", parentCourseId);
    courseDomainService.approve(subCourseId, false, false);
    return courseDataService.getById(subCourseId);
}

/**
 * 创建课程统计数据
 */
private void createContentStats(Long courseId, int learnerCount, int subscriptionCount) {
    ContentStatsDO stats = new ContentStatsDO();
    stats.setContentType(ContentType.COURSE.value());
    stats.setContentId(courseId);
    stats.setInProgressUsers(learnerCount);
    stats.setBookmarks(subscriptionCount);
    stats.setViews(0);
    stats.setUpvotes(0);
    stats.setCompletedUsers(0);
    contentStatsDataService.insert(stats);
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
   - 包括 INSERT、UPDATE、DELETE 等所有操作
   - 相当于测试完成后数据库恢复到测试前的状态
   - **无需手动清理数据**，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID，不依赖固定值
   - 避免了测试之间的ID冲突

3. **辅助方法**:
   - 封装常用的数据准备逻辑，使测试代码更清晰
   - 每个测试方法在 `@Test` 内部调用这些方法创建所需数据

4. **Redis 数据不会自动清理**:
   - `@Transactional` 只回滚数据库操作，**不影响 Redis**
   - 如果测试涉及 Redis，需要手动清理
   - 建议添加清理逻辑：
   ```java
   @BeforeEach
   void setUp() {
       // 清理 Redis 测试数据
       redisTemplate.delete("course:hot");
       redisTemplate.delete("course:ranking");
   }
   ```

**示例流程**：
```
测试1开始 → 创建课程A（ID=1） → 执行测试 → 测试结束 → 自动回滚（删除课程A）
测试2开始 → 创建课程B（ID=1，因为数据库已回滚） → 执行测试 → 测试结束 → 自动回滚
```

每个测试都在**干净的数据库环境**中运行，互不干扰。

---

## Command 测试（写操作）

### 1. 创建课程 (POST /api/v1/courses)

#### 1.1 成功创建课程

#### 1.2 字段验证失败

#### 1.3 分类ID验证

---

### 2. 创建子课程 (POST /api/v1/courses/{parentId}/subcourses)

#### 2.1 成功创建子课程

#### 2.2 父课程不存在

#### 2.3 父课程ID验证

#### 2.4 字段验证失败

---

## Query 测试（读操作）

### 3. 获取课程详情 (GET /api/v1/courses/{id})

#### 3.1 获取已订阅课程（有进度）

#### 3.2 获取未订阅课程

#### 3.3 课程不存在

#### 3.4 无统计数据

---

### 4. 获取课程列表 (GET /api/v1/courses)

#### 4.1 获取所有已发布课程

#### 4.2 按主分类筛选

#### 4.3 按主分类+子分类筛选

#### 4.4 获取子课程列表

#### 4.5 分页功能

#### 4.6 批量查询优化验证

---

### 5. 搜索课程 (GET /api/v1/courses/search)

#### 5.1 搜索成功

#### 5.2 搜索无结果

#### 5.3 关键词验证

#### 5.4 结果数量限制

---

### 6. 获取热门课程 (GET /api/v1/courses/hot)

#### 6.1 默认数量

#### 6.2 自定义数量

#### 6.3 limit参数验证

#### 6.4 过滤非发布状态

---

### 7. 获取课程排行榜 (GET /api/v1/courses/ranking)

#### 7.1 完整排行榜

#### 7.2 只返回已发布课程

---

## 参数验证测试

### 8. 通用参数验证

#### 8.1 ID参数验证（0、负数、非数字）

#### 8.2 分类参数验证

---

## 性能测试

### 9. 批量查询性能

#### 9.1 课程列表批量查询（避免N+1）

---

## 边界测试

### 10. 边界场景

#### 10.1 空数据库查询

#### 10.2 大量数据分页

---

