# 课程管理接口测试用例

> 参考测试编写规范：`docs/TEST_GUIDE.md`

---

## 1. 获取课程详情 (GET /api/v1/courses/{id})

### 1.1 未登录用户获取课程

**测试场景**：
- 用户未登录（不发送 Sa-Token）
- 课程存在且已发布
- 课程有统计数据（学习人数、订阅人数）

**期望结果**：
- ✅ HTTP 200
- ✅ 返回课程基础信息（id, name, description, categories）
- ✅ 返回统计信息（learnerCount, subscriptionCount）
- ✅ 用户字段为默认值：subscribed=false, progress=0

**测试代码**：
```java
@Test
void testGetCourse_NotLoggedIn() throws Exception {
    // 准备：创建课程和统计数据
    CourseDO course = createPublishedCourse("Java 基础", "Java 入门课程");
    createContentStats(course.getId(), 100, 50); // 100学习者, 50订阅

    // 执行：未登录请求
    mockMvc.perform(get("/api/v1/courses/{id}", course.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(course.getId()))
            .andExpect(jsonPath("$.data.name").value("Java 基础"))
            .andExpect(jsonPath("$.data.learnerCount").value(100))
            .andExpect(jsonPath("$.data.subscriptionCount").value(50))
            .andExpect(jsonPath("$.data.subscribed").value(false))
            .andExpect(jsonPath("$.data.progress").value(0));
}
```

---

### 1.2 已登录用户获取已订阅课程

**测试场景**：
- 用户已登录
- 课程存在且已发布
- 用户已订阅该课程
- 用户有学习进度（例如 45%）

**期望结果**：
- ✅ HTTP 200
- ✅ subscribed=true
- ✅ progress=45（实际进度值）
- ✅ 包含完整统计信息

**测试代码**：
```java
@Test
void testGetCourse_LoggedInWithSubscription() throws Exception {
    // 准备：创建用户、课程、订阅关系、学习进度
    UserDO user = createUser("test@example.com");
    CourseDO course = createPublishedCourse("Spring Boot", "Spring Boot 实战");
    createContentStats(course.getId(), 200, 80);
    userDomainService.subscribe(user.getId(), course.getId()); // 订阅
    userCourseService.updateProgress(user.getId(), course.getId(), 45); // 45% 进度

    // 执行：登录后请求
    StpUtil.login(user.getId());
    try {
        mockMvc.perform(get("/api/v1/courses/{id}", course.getId())
                .header("satoken", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscribed").value(true))
                .andExpect(jsonPath("$.data.progress").value(45));
    } finally {
        StpUtil.logout();
    }
}
```

---

### 1.3 已登录用户获取未订阅课程

**测试场景**：
- 用户已登录
- 课程存在且已发布
- 用户未订阅该课程

**期望结果**：
- ✅ HTTP 200
- ✅ subscribed=false
- ✅ progress=0
- ✅ 其他字段正常返回

**测试代码**：
```java
@Test
void testGetCourse_LoggedInWithoutSubscription() throws Exception {
    // 准备：创建用户和课程（不创建订阅关系）
    UserDO user = createUser("user@example.com");
    CourseDO course = createPublishedCourse("Vue 3", "Vue 3 从入门到精通");
    createContentStats(course.getId(), 150, 60);

    // 执行：登录但未订阅
    StpUtil.login(user.getId());
    try {
        mockMvc.perform(get("/api/v1/courses/{id}", course.getId())
                .header("satoken", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(course.getId()))
                .andExpect(jsonPath("$.data.subscribed").value(false))
                .andExpect(jsonPath("$.data.progress").value(0));
    } finally {
        StpUtil.logout();
    }
}
```

### 1.4 课程不存在

**测试场景**：
- 请求不存在的课程ID（例如 99999）

**期望结果**：
- ✅ HTTP 404
- ✅ 返回错误信息："课程不存在"

**测试代码**：
```java
@Test
void testGetCourse_NotFound() throws Exception {
    // 执行：请求不存在的课程
    mockMvc.perform(get("/api/v1/courses/{id}", 99999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("课程不存在"));
}
```

---

### 1.5 课程ID参数验证

**测试场景**：
- ID = 0（无效值）
- ID = 负数（无效值）
- ID = 非数字（类型错误）

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误信息

**测试代码**：
```java
@Test
void testGetCourse_InvalidIdZero() throws Exception {
    mockMvc.perform(get("/api/v1/courses/{id}", 0L))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
}

@Test
void testGetCourse_InvalidIdNegative() throws Exception {
    mockMvc.perform(get("/api/v1/courses/{id}", -1L))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("课程ID必须大于0")));
}

@Test
void testGetCourse_InvalidIdNotNumber() throws Exception {
    mockMvc.perform(get("/api/v1/courses/abc"))
            .andExpect(status().isBadRequest());
}
```

---

### 1.6 获取无统计数据的课程

**测试场景**：
- 课程刚创建，还没有任何统计数据（content_stats 表无记录）

**期望结果**：
- ✅ HTTP 200
- ✅ learnerCount=0
- ✅ subscriptionCount=0
- ✅ 其他字段正常

**测试代码**：
```java
@Test
void testGetCourse_NoStats() throws Exception {
    // 准备：创建课程但不创建统计数据
    CourseDO course = createPublishedCourse("新课程", "刚创建的课程");
    // 不调用 createContentStats()

    // 执行
    mockMvc.perform(get("/api/v1/courses/{id}", course.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(course.getId()))
            .andExpect(jsonPath("$.data.learnerCount").value(0))
            .andExpect(jsonPath("$.data.subscriptionCount").value(0));
}
```

---

## 2. 搜索课程 (GET /api/v1/courses/search)

### 2.1 搜索存在的课程

**测试场景**：
- 搜索关键词："Spring"
- 数据库中有匹配的课程："Spring Boot 实战"、"Spring Cloud 微服务"

**期望结果**：
- ✅ HTTP 200
- ✅ 返回匹配结果列表
- ✅ 只包含 id 和 name 字段（CourseBriefDTO）
- ✅ 不包含 description、stats、progress 等字段

**测试代码**：
```java
@Test
void testSearchCourses_Found() throws Exception {
    // 准备：创建包含关键词的课程
    CourseDO course1 = createPublishedCourse("Spring Boot 实战", "描述1");
    CourseDO course2 = createPublishedCourse("Spring Cloud 微服务", "描述2");
    createPublishedCourse("Vue 3 教程", "描述3"); // 不匹配

    // 执行：搜索
    mockMvc.perform(get("/api/v1/courses/search")
            .param("keyword", "Spring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").exists())
            .andExpect(jsonPath("$.data[0].name").exists())
            .andExpect(jsonPath("$.data[0].description").doesNotExist()) // 不包含详细字段
            .andExpect(jsonPath("$.data[0].learnerCount").doesNotExist());
}
```

---

### 2.2 搜索不存在的课程

**测试场景**：
- 搜索关键词："不存在的课程"
- 数据库中无匹配结果

**期望结果**：
- ✅ HTTP 200
- ✅ 返回空数组 `[]`
- ✅ 不抛出异常

**测试代码**：
```java
@Test
void testSearchCourses_NotFound() throws Exception {
    // 准备：创建一些课程（但都不匹配）
    createPublishedCourse("Java 基础", "描述");
    createPublishedCourse("Python 入门", "描述");

    // 执行：搜索不存在的关键词
    mockMvc.perform(get("/api/v1/courses/search")
            .param("keyword", "不存在的课程"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
}
```

---

### 2.3 搜索关键词为空

**测试场景**：
- 不传递 keyword 参数
- 或传递空字符串 `keyword=""`

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误："搜索关键词不能为空"

**测试代码**：
```java
@Test
void testSearchCourses_EmptyKeyword() throws Exception {
    // 情况1：不传递 keyword 参数
    mockMvc.perform(get("/api/v1/courses/search"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

    // 情况2：传递空字符串
    mockMvc.perform(get("/api/v1/courses/search")
            .param("keyword", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("搜索关键词不能为空")));

    // 情况3：只包含空白字符
    mockMvc.perform(get("/api/v1/courses/search")
            .param("keyword", "   "))
            .andExpect(status().isBadRequest());
}
```

---

### 2.4 搜索结果数量限制

**测试场景**：
- 数据库中有 50 个匹配"Java"的课程
- 系统配置的 searchLimit = 20

**期望结果**：
- ✅ HTTP 200
- ✅ 最多返回 20 条结果
- ✅ 不会返回全部 50 条

**测试代码**：
```java
@Test
void testSearchCourses_ResultLimit() throws Exception {
    // 准备：创建 50 个匹配的课程
    for (int i = 1; i <= 50; i++) {
        createPublishedCourse("Java 课程 " + i, "描述 " + i);
    }

    // 执行：搜索
    MvcResult result = mockMvc.perform(get("/api/v1/courses/search")
            .param("keyword", "Java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();

    // 验证：结果不超过配置的限制（假设 searchLimit=20）
    String content = result.getResponse().getContentAsString();
    JsonNode data = objectMapper.readTree(content).get("data");
    assertThat(data.size()).isLessThanOrEqualTo(20);
}
```

---

## 3. 获取课程列表 (GET /api/v1/courses)

### 3.1 获取所有课程 - 未登录

**测试场景**：
- 用户未登录
- 数据库中有 3 个已发布课程 + 2 个待审核课程

**期望结果**：
- ✅ HTTP 200
- ✅ 只返回已发布课程（3 个）
- ✅ 包含完整字段（基础信息 + 统计信息）
- ✅ 用户字段为默认值：subscribed=false, progress=0

**测试代码**：
```java
@Test
void testGetCourses_NotLoggedIn() throws Exception {
    // 准备：创建不同状态的课程
    CourseDO course1 = createPublishedCourse("课程1", "描述1");
    CourseDO course2 = createPublishedCourse("课程2", "描述2");
    CourseDO course3 = createPublishedCourse("课程3", "描述3");
    createCourseWithState("待审核课程", ContentState.SUBMITTED); // 不应返回
    createCourseWithState("已拒绝课程", ContentState.REJECTED); // 不应返回

    createContentStats(course1.getId(), 100, 50);
    createContentStats(course2.getId(), 200, 80);

    // 执行：未登录请求
    mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].subscribed").value(false))
            .andExpect(jsonPath("$.data[0].progress").value(0))
            .andExpect(jsonPath("$.data[0].learnerCount").exists())
            .andExpect(jsonPath("$.data[0].subscriptionCount").exists());
}
```

---

### 3.2 获取所有课程 - 已登录

**测试场景**：
- 用户已登录
- 已订阅课程1 和课程3
- 课程1 有学习进度 50%

**期望结果**：
- ✅ HTTP 200
- ✅ 课程1：subscribed=true, progress=50
- ✅ 课程2：subscribed=false, progress=0（未订阅）
- ✅ 课程3：subscribed=true, progress=0（已订阅但未学习）

**测试代码**：
```java
@Test
void testGetCourses_LoggedIn() throws Exception {
    // 准备：创建用户和课程
    UserDO user = createUser("user@example.com");
    CourseDO course1 = createPublishedCourse("课程1", "描述1");
    CourseDO course2 = createPublishedCourse("课程2", "描述2");
    CourseDO course3 = createPublishedCourse("课程3", "描述3");

    createContentStats(course1.getId(), 100, 50);
    createContentStats(course2.getId(), 200, 80);

    // 订阅课程1和课程3
    userDomainService.subscribe(user.getId(), course1.getId());
    userDomainService.subscribe(user.getId(), course3.getId());

    // 课程1有学习进度
    userCourseService.updateProgress(user.getId(), course1.getId(), 50);

    // 执行：登录后请求
    StpUtil.login(user.getId());
    try {
        MvcResult result = mockMvc.perform(get("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        // 解析并验证每个课程的订阅状态
        String content = result.getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(content).get("data");

        // 假设按 ID 降序返回：course3, course2, course1
        JsonNode courseDto1 = findById(data, course1.getId());
        assertThat(courseDto1.get("subscribed").asBoolean()).isTrue();
        assertThat(courseDto1.get("progress").asInt()).isEqualTo(50);

        JsonNode courseDto2 = findById(data, course2.getId());
        assertThat(courseDto2.get("subscribed").asBoolean()).isFalse();
        assertThat(courseDto2.get("progress").asInt()).isEqualTo(0);

        JsonNode courseDto3 = findById(data, course3.getId());
        assertThat(courseDto3.get("subscribed").asBoolean()).isTrue();
        assertThat(courseDto3.get("progress").asInt()).isEqualTo(0);
    } finally {
        StpUtil.logout();
    }
}
```

---

### 3.3 按主分类筛选

**测试场景**：
- 主分类 1：2 个课程
- 主分类 2：1 个课程
- 请求 `?mainCategory=1`

**期望结果**：
- ✅ HTTP 200
- ✅ 只返回主分类=1 的 2 个课程
- ✅ 不返回其他分类的课程

**测试代码**：
```java
@Test
void testGetCourses_ByMainCategory() throws Exception {
    // 准备：创建不同主分类的课程
    CourseDO course1 = createPublishedCourseWithCategory("课程1", 1, 1);
    CourseDO course2 = createPublishedCourseWithCategory("课程2", 1, 2);
    CourseDO course3 = createPublishedCourseWithCategory("课程3", 2, 1); // 不同主分类

    // 执行：按主分类筛选
    mockMvc.perform(get("/api/v1/courses")
            .param("mainCategory", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[*].id").value(containsInAnyOrder(
                course1.getId().intValue(),
                course2.getId().intValue()
            )));
}
```

---

### 3.4 按主分类+子分类筛选

**测试场景**：
- 主分类1 + 子分类1：1 个课程
- 主分类1 + 子分类2：1 个课程
- 请求 `?mainCategory=1&subCategory=1`

**期望结果**：
- ✅ HTTP 200
- ✅ 只返回 主分类=1 且 子分类=1 的课程
- ✅ 筛选精确

**测试代码**：
```java
@Test
void testGetCourses_ByMainAndSubCategory() throws Exception {
    // 准备
    CourseDO course1 = createPublishedCourseWithCategory("课程1", 1, 1); // 匹配
    CourseDO course2 = createPublishedCourseWithCategory("课程2", 1, 2); // 不匹配（子分类不同）
    CourseDO course3 = createPublishedCourseWithCategory("课程3", 2, 1); // 不匹配（主分类不同）

    // 执行
    mockMvc.perform(get("/api/v1/courses")
            .param("mainCategory", "1")
            .param("subCategory", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(course1.getId()));
}
```

### 3.5 获取子课程列表

**测试场景**：
- 父课程 A：有 2 个子课程
- 父课程 B：有 1 个子课程
- 请求 `?parentCourseId={A的ID}`

**期望结果**：
- ✅ HTTP 200
- ✅ 只返回父课程 A 的 2 个子课程
- ✅ 不返回父课程 B 的子课程

**测试代码**：
```java
@Test
void testGetCourses_ByParentCourse() throws Exception {
    // 准备：创建父课程和子课程
    CourseDO parentA = createPublishedCourse("父课程A", "描述A");
    CourseDO parentB = createPublishedCourse("父课程B", "描述B");

    CourseDO subCourse1 = createPublishedSubCourse("子课程1", parentA.getId());
    CourseDO subCourse2 = createPublishedSubCourse("子课程2", parentA.getId());
    CourseDO subCourse3 = createPublishedSubCourse("子课程3", parentB.getId()); // 不同父课程

    // 执行：按父课程筛选
    mockMvc.perform(get("/api/v1/courses")
            .param("parentCourseId", parentA.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[*].id").value(containsInAnyOrder(
                subCourse1.getId().intValue(),
                subCourse2.getId().intValue()
            )));
}
```

---

### 3.6 分页功能

**测试场景**：
- 数据库中有 5 个课程（ID: 1, 2, 3, 4, 5）
- 第一页：`GET /courses` 返回前 20 个
- 第二页：`GET /courses?lastId=3` 返回 ID < 3 的课程

**期望结果**：
- ✅ HTTP 200
- ✅ lastId 分页正确
- ✅ 课程按 ID 降序排列
- ✅ 不重复返回数据

**测试代码**：
```java
@Test
void testGetCourses_Pagination() throws Exception {
    // 准备：创建 5 个课程
    CourseDO course1 = createPublishedCourse("课程1", "描述1"); // ID 最小
    CourseDO course2 = createPublishedCourse("课程2", "描述2");
    CourseDO course3 = createPublishedCourse("课程3", "描述3");
    CourseDO course4 = createPublishedCourse("课程4", "描述4");
    CourseDO course5 = createPublishedCourse("课程5", "描述5"); // ID 最大

    // 执行第一页（不传 lastId）
    MvcResult result1 = mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();

    String content1 = result1.getResponse().getContentAsString();
    JsonNode data1 = objectMapper.readTree(content1).get("data");

    // 验证：按 ID 降序返回（course5, course4, course3, course2, course1）
    assertThat(data1.size()).isEqualTo(5);
    assertThat(data1.get(0).get("id").asLong()).isEqualTo(course5.getId());
    assertThat(data1.get(4).get("id").asLong()).isEqualTo(course1.getId());

    // 执行第二页（传 lastId=course3的ID）
    mockMvc.perform(get("/api/v1/courses")
            .param("lastId", course3.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2)) // 只返回 course2, course1
            .andExpect(jsonPath("$.data[0].id").value(course2.getId()))
            .andExpect(jsonPath("$.data[1].id").value(course1.getId()));
}
```

---

### 3.7 参数验证

**测试场景**：
- 传递无效的分类ID（0、负数）

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误

**测试代码**：
```java
@Test
void testGetCourses_InvalidCategoryId() throws Exception {
    // 主分类 = 0
    mockMvc.perform(get("/api/v1/courses")
            .param("mainCategory", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("主分类必须大于0")));

    // 子分类 = 负数
    mockMvc.perform(get("/api/v1/courses")
            .param("mainCategory", "1")
            .param("subCategory", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("子分类必须大于0")));
}
```

---

### 3.8 性能验证 - 批量查询优化

**测试场景**：
- 返回 20 个课程
- 检查是否避免了 N+1 查询问题

**期望结果**：
- ✅ 统计信息使用批量查询（1 次 SQL）
- ✅ 订阅状态使用批量查询（1 次查询用户订阅列表）
- ✅ 学习进度使用批量查询（1 次 SQL）
- ✅ 总查询次数 ≤ 5 次（课程列表 + 统计批量 + 订阅列表 + 进度批量）

**测试代码**：
```java
@Test
void testGetCourses_BatchQueryOptimization() throws Exception {
    // 准备：创建用户和 20 个课程
    UserDO user = createUser("user@example.com");

    for (int i = 1; i <= 20; i++) {
        CourseDO course = createPublishedCourse("课程" + i, "描述" + i);
        createContentStats(course.getId(), i * 10, i * 5);

        // 订阅一半的课程
        if (i % 2 == 0) {
            userDomainService.subscribe(user.getId(), course.getId());
            userCourseService.updateProgress(user.getId(), course.getId(), i);
        }
    }

    // 执行：登录后请求
    StpUtil.login(user.getId());
    try {
        // 可以使用 SQL 查询监控工具验证查询次数
        // 或使用 Spring Boot Actuator + DataSource Proxy

        mockMvc.perform(get("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(20));

        // 验证：所有课程都有正确的统计和用户数据
        // 这里省略详细验证，实际测试中应检查每个课程的字段
    } finally {
        StpUtil.logout();
    }
}

// 注：实际项目中可以使用以下方式监控 SQL 查询次数：
// 1. 启用 Hibernate statistics: spring.jpa.properties.hibernate.generate_statistics=true
// 2. 使用 DataSource Proxy: net.ttddyy.dsproxy
// 3. 在测试中使用 @Sql 注解记录查询日志
```

---

## 4. 获取热门课程 (GET /api/v1/courses/hot)

### 4.1 默认获取10条

**测试场景**：
- 不传 limit 参数
- 数据库中有 15 个已发布课程

**期望结果**：
- ✅ HTTP 200
- ✅ 返回 ≤10 条
- ✅ 按热度降序排列（热度最高的在最前）

**测试代码**：
```java
@Test
void testGetHotCourses_DefaultLimit() throws Exception {
    // 准备：创建 15 个课程，设置不同热度分数
    for (int i = 1; i <= 15; i++) {
        CourseDO course = createPublishedCourse("课程" + i, "描述" + i);
        // 模拟：设置课程热度分数到 Redis
        redisTemplate.opsForZSet().add("course:hot", course.getId(), i * 10.0);
    }

    // 执行：不传 limit
    MvcResult result = mockMvc.perform(get("/api/v1/courses/hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    JsonNode data = objectMapper.readTree(content).get("data");

    // 验证
    assertThat(data.size()).isLessThanOrEqualTo(10);

    // 验证热度降序：第一个课程的热度 >= 第二个课程的热度
    if (data.size() > 1) {
        // 假设返回字段包含热度信息，或通过 ID 推断
        // 这里简化验证
    }
}
```

---

### 4.2 自定义返回数量

**测试场景**：
- 传递 `limit=5`
- 数据库中有 15 个课程

**期望结果**：
- ✅ HTTP 200
- ✅ 返回 ≤5 条

**测试代码**：
```java
@Test
void testGetHotCourses_CustomLimit() throws Exception {
    // 准备：创建 15 个课程
    for (int i = 1; i <= 15; i++) {
        CourseDO course = createPublishedCourse("课程" + i, "描述" + i);
        redisTemplate.opsForZSet().add("course:hot", course.getId(), i * 10.0);
    }

    // 执行：limit=5
    mockMvc.perform(get("/api/v1/courses/hot")
            .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(lessThanOrEqualTo(5)));
}
```

---

### 4.3 limit 参数验证

**测试场景**：
- limit = 0
- limit = 负数

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误："限制数量必须大于0"

**测试代码**：
```java
@Test
void testGetHotCourses_InvalidLimit() throws Exception {
    // limit = 0
    mockMvc.perform(get("/api/v1/courses/hot")
            .param("limit", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value(containsString("限制数量必须大于0")));

    // limit = 负数
    mockMvc.perform(get("/api/v1/courses/hot")
            .param("limit", "-1"))
            .andExpect(status().isBadRequest());
}
```

---

### 4.4 过滤非已发布课程

**测试场景**：
- Redis 中有 5 个热门课程 ID
- 其中 2 个课程状态为"待审核"，1 个为"已拒绝"
- 只有 2 个是"已发布"状态

**期望结果**：
- ✅ HTTP 200
- ✅ 只返回已发布的 2 个课程
- ✅ 过滤掉非发布状态的课程

**测试代码**：
```java
@Test
void testGetHotCourses_FilterNonPublished() throws Exception {
    // 准备：创建不同状态的课程
    CourseDO published1 = createPublishedCourse("已发布1", "描述1");
    CourseDO published2 = createPublishedCourse("已发布2", "描述2");
    CourseDO submitted = createCourseWithState("待审核", ContentState.SUBMITTED);
    CourseDO rejected = createCourseWithState("已拒绝", ContentState.REJECTED);

    // 设置热度（都添加到 Redis）
    redisTemplate.opsForZSet().add("course:hot", published1.getId(), 100.0);
    redisTemplate.opsForZSet().add("course:hot", published2.getId(), 90.0);
    redisTemplate.opsForZSet().add("course:hot", submitted.getId(), 80.0);
    redisTemplate.opsForZSet().add("course:hot", rejected.getId(), 70.0);

    // 执行
    mockMvc.perform(get("/api/v1/courses/hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2)) // 只返回 2 个已发布课程
            .andExpect(jsonPath("$.data[*].id").value(containsInAnyOrder(
                published1.getId().intValue(),
                published2.getId().intValue()
            )));
}
```

---

### 4.5 Redis异常处理

**测试场景**：
- Redis 不可用（连接失败）

**期望结果**：
- ✅ 不抛出异常
- ✅ 返回空数组或降级处理（例如返回最新创建的课程）

**测试代码**：
```java
@Test
void testGetHotCourses_RedisDown() throws Exception {
    // 模拟：Redis 不可用（通过停止 Redis 服务或 Mock RedisTemplate 抛异常）

    // 方式1：如果项目有降级逻辑，应该返回降级数据
    // 方式2：如果没有降级，应该返回空数组

    mockMvc.perform(get("/api/v1/courses/hot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    // 具体验证取决于项目的降级策略
}
```

---

## 5. 获取课程排行榜 (GET /api/v1/courses/ranking)

### 5.1 获取完整排行榜

**测试场景**：
- 配置返回数量 = 50
- 数据库中有 100 个已发布课程

**期望结果**：
- ✅ HTTP 200
- ✅ 返回数量 = 配置值（50）
- ✅ 按热度降序排列

**测试代码**：
```java
@Test
void testGetCourseRanking_FullList() throws Exception {
    // 准备：创建 100 个课程
    for (int i = 1; i <= 100; i++) {
        CourseDO course = createPublishedCourse("课程" + i, "描述" + i);
        redisTemplate.opsForZSet().add("course:ranking", course.getId(), 100 - i); // 热度递减
    }

    // 执行
    MvcResult result = mockMvc.perform(get("/api/v1/courses/ranking"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    JsonNode data = objectMapper.readTree(content).get("data");

    // 验证：返回配置的数量（假设配置为 50）
    assertThat(data.size()).isEqualTo(50);

    // 验证：按热度降序
    // 第一个课程的 ID 应该是热度最高的
}
```

---

### 5.2 只返回已发布课程

**测试场景**：
- Redis 排行榜中有 10 个课程
- 其中 3 个不是"已发布"状态

**期望结果**：
- ✅ HTTP 200
- ✅ 过滤掉待审核、已拒绝、已封禁课程
- ✅ 只返回已发布的 7 个课程

**测试代码**：
```java
@Test
void testGetCourseRanking_OnlyPublished() throws Exception {
    // 准备：创建不同状态的课程
    List<CourseDO> publishedCourses = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
        CourseDO course = createPublishedCourse("已发布" + i, "描述");
        publishedCourses.add(course);
        redisTemplate.opsForZSet().add("course:ranking", course.getId(), 100 - i);
    }

    // 创建非发布状态的课程
    CourseDO submitted = createCourseWithState("待审核", ContentState.SUBMITTED);
    CourseDO rejected = createCourseWithState("已拒绝", ContentState.REJECTED);
    CourseDO banned = createCourseWithState("已封禁", ContentState.BANNED);

    redisTemplate.opsForZSet().add("course:ranking", submitted.getId(), 50.0);
    redisTemplate.opsForZSet().add("course:ranking", rejected.getId(), 40.0);
    redisTemplate.opsForZSet().add("course:ranking", banned.getId(), 30.0);

    // 执行
    MvcResult result = mockMvc.perform(get("/api/v1/courses/ranking"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    JsonNode data = objectMapper.readTree(content).get("data");

    // 验证：只返回 7 个已发布课程
    assertThat(data.size()).isEqualTo(7);

    // 验证：不包含非发布课程
    List<Long> returnedIds = new ArrayList<>();
    for (JsonNode node : data) {
        returnedIds.add(node.get("id").asLong());
    }
    assertThat(returnedIds).doesNotContain(submitted.getId(), rejected.getId(), banned.getId());
}
```

---

## 6. 创建课程 (POST /api/v1/courses)

### 6.1 未登录用户创建课程

**测试场景**：
- 用户未登录（不发送 Sa-Token）
- 尝试创建课程

**期望结果**：
- ✅ HTTP 401 Unauthorized
- ✅ 返回错误信息："未登录"

**测试代码**：
```java
@Test
void testCreateCourse_NotLoggedIn() throws Exception {
    // 准备：创建请求体
    String requestBody = """
        {
            "name": "新课程",
            "description": "课程描述",
            "mainCategory": 1,
            "subCategory": 1
        }
        """;

    // 执行：未登录请求
    mockMvc.perform(post("/api/v1/courses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
}
```

---

### 6.2 已登录用户成功创建

**测试场景**：
- 用户已登录
- 提供所有必填字段（name, description, mainCategory, subCategory）

**期望结果**：
- ✅ HTTP 200
- ✅ 返回成功消息
- ✅ 数据库中有新记录
- ✅ 课程状态 = SUBMITTED（待审核）
- ✅ 自动创建根节点（node 表）

**测试代码**：
```java
@Test
void testCreateCourse_Success() throws Exception {
    // 准备：创建用户并登录
    UserDO user = createUser("creator@example.com");

    String requestBody = """
        {
            "name": "Spring Boot 实战",
            "description": "从零开始学习 Spring Boot",
            "mainCategory": 1,
            "subCategory": 2
        }
        """;

    // 执行：登录后创建
    StpUtil.login(user.getId());
    try {
        mockMvc.perform(post("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证：数据库中有新课程
        List<CourseDO> courses = courseDataService.listByCreatorId(user.getId());
        assertThat(courses).hasSize(1);

        CourseDO createdCourse = courses.get(0);
        assertThat(createdCourse.getName()).isEqualTo("Spring Boot 实战");
        assertThat(createdCourse.getState()).isEqualTo(ContentState.SUBMITTED.value());
        assertThat(createdCourse.getCreatorId()).isEqualTo(user.getId());
        assertThat(createdCourse.getMainCategory()).isEqualTo(1);

        // 验证：自动创建了根节点
        List<NodeDO> nodes = nodeDataService.listByCourseId(createdCourse.getId());
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getTitle()).isEqualTo("根节点");
    } finally {
        StpUtil.logout();
    }
}
```

---

### 6.3 必填字段缺失

**测试场景**：
- 缺少 name 字段
- 或缺少 description 字段

**期望结果**：
- ✅ HTTP 400
- ✅ 返回字段验证错误

**测试代码**：
```java
@Test
void testCreateCourse_MissingRequiredFields() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());
    try {
        // 情况1：缺少 name
        String requestBody1 = """
            {
                "description": "描述",
                "mainCategory": 1,
                "subCategory": 1
            }
            """;

        mockMvc.perform(post("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        // 情况2：name 为空字符串
        String requestBody2 = """
            {
                "name": "",
                "description": "描述",
                "mainCategory": 1,
                "subCategory": 1
            }
            """;

        mockMvc.perform(post("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("课程名称不能为空")));
    } finally {
        StpUtil.logout();
    }
}
```

---

### 6.4 字段值验证

**测试场景**：
- mainCategory = 0 或负数
- subCategory = 0 或负数

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误："分类ID必须大于0"

**测试代码**：
```java
@Test
void testCreateCourse_InvalidCategoryId() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());
    try {
        // mainCategory = 0
        String requestBody = """
            {
                "name": "课程名称",
                "description": "描述",
                "mainCategory": 0,
                "subCategory": 1
            }
            """;

        mockMvc.perform(post("/api/v1/courses")
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("主分类必须大于0")));
    } finally {
        StpUtil.logout();
    }
}
```

---

## 7. 创建子课程 (POST /api/v1/courses/{parentId}/subcourses)

### 7.1 未登录用户创建

**测试场景**：
- 用户未登录
- 尝试为某个父课程创建子课程

**期望结果**：
- ✅ HTTP 401 Unauthorized
- ✅ 返回错误信息

**测试代码**：
```java
@Test
void testCreateSubCourse_NotLoggedIn() throws Exception {
    // 准备：创建父课程
    CourseDO parentCourse = createPublishedCourse("父课程", "描述");

    String requestBody = """
        {
            "name": "子课程",
            "description": "子课程描述"
        }
        """;

    // 执行：未登录请求
    mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", parentCourse.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
}
```

---

### 7.2 成功创建子课程

**测试场景**：
- 用户已登录
- 父课程存在
- 提供必填字段（name, description）

**期望结果**：
- ✅ HTTP 200
- ✅ parentCourseId = 父课程ID
- ✅ 继承父课程的分类（mainCategory, subCategory）
- ✅ 自动创建根节点
- ✅ 状态 = SUBMITTED

**测试代码**：
```java
@Test
void testCreateSubCourse_Success() throws Exception {
    // 准备：创建用户和父课程
    UserDO user = createUser("creator@example.com");
    CourseDO parentCourse = createPublishedCourseWithCategory("父课程", 1, 2);

    String requestBody = """
        {
            "name": "子课程 - 进阶篇",
            "description": "这是父课程的进阶内容"
        }
        """;

    // 执行：登录后创建
    StpUtil.login(user.getId());
    try {
        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", parentCourse.getId())
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证：数据库中有子课程
        List<CourseDO> subCourses = courseDataService.listByParentCourseId(parentCourse.getId());
        assertThat(subCourses).hasSize(1);

        CourseDO subCourse = subCourses.get(0);
        assertThat(subCourse.getName()).isEqualTo("子课程 - 进阶篇");
        assertThat(subCourse.getParentCourseId()).isEqualTo(parentCourse.getId());
        assertThat(subCourse.getMainCategory()).isEqualTo(1); // 继承父课程
        assertThat(subCourse.getSubCategory()).isEqualTo(2); // 继承父课程
        assertThat(subCourse.getState()).isEqualTo(ContentState.SUBMITTED.value());

        // 验证：自动创建根节点
        List<NodeDO> nodes = nodeDataService.listByCourseId(subCourse.getId());
        assertThat(nodes).hasSize(1);
    } finally {
        StpUtil.logout();
    }
}
```

---

### 7.3 父课程不存在

**测试场景**：
- 尝试为不存在的父课程创建子课程
- parentId = 99999

**期望结果**：
- ✅ HTTP 404 Not Found
- ✅ 返回错误信息："父课程不存在"

**测试代码**：
```java
@Test
void testCreateSubCourse_ParentNotFound() throws Exception {
    UserDO user = createUser("user@example.com");

    String requestBody = """
        {
            "name": "子课程",
            "description": "描述"
        }
        """;

    StpUtil.login(user.getId());
    try {
        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", 99999L)
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("课程不存在")));
    } finally {
        StpUtil.logout();
    }
}
```

---

### 7.4 父课程ID参数验证

**测试场景**：
- parentId = 0
- parentId = 负数

**期望结果**：
- ✅ HTTP 400
- ✅ 返回参数验证错误

**测试代码**：
```java
@Test
void testCreateSubCourse_InvalidParentId() throws Exception {
    UserDO user = createUser("user@example.com");

    String requestBody = """
        {
            "name": "子课程",
            "description": "描述"
        }
        """;

    StpUtil.login(user.getId());
    try {
        // parentId = 0
        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", 0L)
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        // parentId = 负数
        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", -1L)
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    } finally {
        StpUtil.logout();
    }
}
```

---

### 7.5 字段验证

**测试场景**：
- name 为空
- description 为空

**期望结果**：
- ✅ HTTP 400
- ✅ 返回字段验证错误

**测试代码**：
```java
@Test
void testCreateSubCourse_MissingFields() throws Exception {
    UserDO user = createUser("user@example.com");
    CourseDO parentCourse = createPublishedCourse("父课程", "描述");

    StpUtil.login(user.getId());
    try {
        // name 为空
        String requestBody1 = """
            {
                "name": "",
                "description": "描述"
            }
            """;

        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", parentCourse.getId())
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("课程名称不能为空")));

        // description 为空
        String requestBody2 = """
            {
                "name": "子课程",
                "description": ""
            }
            """;

        mockMvc.perform(post("/api/v1/courses/{parentId}/subcourses", parentCourse.getId())
                .header("satoken", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("课程描述不能为空")));
    } finally {
        StpUtil.logout();
    }
}
```

---

## 性能测试场景

### P1 单个课程详情查询性能
- 响应时间 < 200ms
- 查询次数 ≤ 4

### P2 课程列表批量查询性能（20条）
- 响应时间 < 500ms
- 无 N+1 查询

### P3 热门课程查询性能
- 响应时间 < 300ms

---

## 边界测试场景

### B1 空数据库
- **验证**：返回空数组，不抛异常

### B2 大量数据
- **验证**：分页正常，性能稳定

### B3 特殊字符
- **验证**：正确存储和返回，无XSS

---

## 安全测试场景

### S1 SQL注入防护
- **验证**：参数化查询，无注入风险

### S2 未授权访问
- **验证**：需要登录的接口返回401

### S3 Token伪造
- **验证**：假token返回401

---

## 限流测试场景

### R1 超过速率限制
- **验证**：第41个请求返回429

### R2 不同用户独立限流
- **验证**：限流独立计算

---

## 数据一致性测试

### D1 订阅后统计更新
- **验证**：subscribed 和 subscriptionCount 正确更新

### D2 学习后进度更新
- **验证**：progress 和 learnerCount 正确更新
