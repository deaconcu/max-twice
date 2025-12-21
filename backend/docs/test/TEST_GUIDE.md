# 集成测试编写规范

## 测试框架配置

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 测试后自动回滚，无需手动清理
class YourControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 注入需要的 DataService 和 DomainService
}
```

---

## 测试数据准备

### 原则
- ✅ 使用动态ID（数据库自动生成）
- ✅ 在 @BeforeEach 或测试方法内创建数据
- ✅ 使用辅助方法提高复用性
- ✅ 依赖 @Transactional 自动回滚

### 辅助方法示例

```java
// 创建测试用户
private UserDO createUser(String email) {
    UserDO user = new UserDO();
    user.setEmail(email);
    user.setUsername(email.split("@")[0]);
    user.setPassword("test123");
    userDataService.insert(user);
    return user;  // 返回对象，使用 user.getId() 获取动态ID
}

// 创建测试实体
private EntityDO createEntity(...) {
    EntityDO entity = new EntityDO();
    // 设置属性
    dataService.insert(entity);
    return entity;  // 返回对象以获取生成的ID
}
```

---

## 测试命名规范

```
test[接口方法名]_[测试场景]

示例：
- testGetCourse_NotLoggedIn
- testGetCourse_CourseNotFound
- testCreateCourse_Success
- testCreateCourse_MissingFields
```

---

## 认证处理

### 需要登录的接口
```java
@Test
void testSomeProtectedEndpoint() throws Exception {
    UserDO user = createUser("test@test.com");
    StpUtil.login(user.getId());  // 模拟登录

    // 执行请求
    mockMvc.perform(...)

    StpUtil.logout();  // 清理登录状态
}
```

### 不需要登录的接口
```java
@Test
void testPublicEndpoint() throws Exception {
    // 直接执行，@CurrentUser 参数会是 null
    mockMvc.perform(get("/api/v1/courses/1"))
}
```

---

## 断言规范

### 必须验证
1. HTTP 状态码
2. 响应 code 字段
3. 关键业务字段

```java
mockMvc.perform(get("/api/v1/courses/1"))
    .andExpect(status().isOk())                    // HTTP 200
    .andExpect(jsonPath("$.code").value(200))      // 业务码
    .andExpect(jsonPath("$.data.id").exists())     // 字段存在
    .andExpect(jsonPath("$.data.name").value("测试课程"));  // 字段值
```

### 验证数组
```java
mockMvc.perform(get("/api/v1/courses"))
    .andExpect(jsonPath("$.data").isArray())
    .andExpect(jsonPath("$.data.length()").value(2))
    .andExpect(jsonPath("$.data[0].id").exists());
```

### 验证数据库变更
```java
// 创建操作后验证
List<CourseDO> courses = courseDataService.listByCreator(userId);
assertThat(courses).hasSize(1);
assertThat(courses.get(0).getName()).isEqualTo("新课程");
```

---

## 必测场景清单

每个接口至少包含：
- [ ] 正常场景（happy path）
- [ ] 参数验证（null、空、负数、格式错误）
- [ ] 认证授权（需要登录的接口测试未登录）
- [ ] 数据不存在（404）
- [ ] 业务规则验证

---

## 完整测试示例

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoursesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private UserDataService userDataService;

    // 辅助方法
    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        userDataService.insert(user);
        return user;
    }

    private CourseDO createCourse(String name, ContentState state, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setState(state.value());
        course.setCreatorId(creatorId);
        courseDataService.insert(course);
        return course;
    }

    // 测试用例
    @Test
    @DisplayName("未登录用户获取课程详情")
    void testGetCourse_NotLoggedIn() throws Exception {
        UserDO creator = createUser("creator@test.com");
        CourseDO course = createCourse("测试课程", ContentState.PUBLISHED, creator.getId());

        mockMvc.perform(get("/api/v1/courses/" + course.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(course.getId()))
            .andExpect(jsonPath("$.data.subscribed").value(false))
            .andExpect(jsonPath("$.data.progress").value(0));
    }

    @Test
    @DisplayName("课程不存在返回404")
    void testGetCourse_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/courses/99999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("未登录创建课程返回401")
    void testCreateCourse_NotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新课程\",\"description\":\"描述\",\"mainCategory\":1,\"subCategory\":2}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }
}
```

---

## 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=CoursesControllerTest

# 查看覆盖率
mvn test jacoco:report
```
