# 用户管理接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UsersControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserDomainService userDomainService;

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
 * 创建已验证邮箱的用户
 */
private UserDO createValidatedUser(String email) {
    UserDO user = userDomainService.createUser(email, "password123");
    user.setEmailValidated(true);
    userDataService.update(user);
    return user;
}
```

### 说明

1. **@Transactional - 自动清理数据**:
   - 每个 `@Test` 方法执行完后，**所有数据库操作都会自动回滚**
   - 包括用户注册、登录、更新等所有操作
   - **无需手动清理数据**，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **认证状态管理**:
   - 需要登录的测试使用 `StpUtil.login(userId)` 模拟登录
   - 测试结束使用 `StpUtil.logout()` 清理登录状态

---

## Command 测试（写操作）

### 1. 用户注册 (POST /api/v1/auth/register)

#### 测试场景

##### 1.1 成功注册
- **请求**：POST /api/v1/auth/register，传入 email、password
- **验证**：
  - 返回 200 状态码
  - 用户已创建
  - 邮箱未验证（emailValidated=false）
  - 密码已加密存储

##### 1.2 字段验证 - email 为空
- **请求**：email=""
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"邮箱不能为空"

##### 1.3 字段验证 - email 格式错误
- **请求**：email="invalid-email"
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"邮箱格式不正确"

##### 1.4 字段验证 - password 为空
- **请求**：password=""
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"密码不能为空"

##### 1.5 字段验证 - password 过短
- **请求**：password="12345"（小于配置的最小长度）
- **验证**：返回 1002 (INVALID_PARAMETER)，提示密码长度不符合要求

##### 1.6 业务验证 - 邮箱已存在
- **准备**：创建用户 user@example.com
- **请求**：再次注册 user@example.com
- **验证**：返回 1102 (USER_EXISTS)，提示"用户已存在"

##### 1.7 限流验证 - 5次/分钟
- **准备**：连续注册6次（使用不同邮箱）
- **验证**：第6次返回 2301 (RATE_LIMIT_EXCEEDED)

#### 测试代码示例

```java
@Test
@DisplayName("成功注册")
void testRegister_Success() throws Exception {
    String requestBody = """
        {
            "email": "newuser@example.com",
            "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    // 验证用户已创建
    UserDO user = userDataService.getByEmail("newuser@example.com");
    assertThat(user).isNotNull();
    assertThat(user.getEmailValidated()).isFalse();
}

@Test
@DisplayName("邮箱已存在")
void testRegister_EmailExists() throws Exception {
    createUser("existing@example.com");

    String requestBody = """
        {
            "email": "existing@example.com",
            "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1102));
}
```

---

### 2. 用户登录 (POST /api/v1/auth/login)

#### 测试场景

##### 2.1 成功登录
- **准备**：创建用户
- **请求**：POST /api/v1/auth/login，传入正确的 email、password
- **验证**：
  - 返回 200 状态码
  - 返回用户简要信息（id, name, avatar）
  - 响应头包含 satoken

##### 2.2 字段验证 - email 为空
- **请求**：email=""
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 2.3 字段验证 - password 为空
- **请求**：password=""
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 2.4 业务验证 - 用户不存在
- **请求**：email="nonexistent@example.com"
- **验证**：返回 1007 (NOT_FOUND)，提示"用户不存在"

##### 2.5 业务验证 - 密码错误
- **准备**：创建用户，密码为 "password123"
- **请求**：password="wrongpassword"
- **验证**：返回 1103 (PASSWORD_ERROR)，提示"密码错误"

##### 2.6 限流验证 - 10次/分钟
- **准备**：连续登录11次
- **验证**：第11次返回 2301 (RATE_LIMIT_EXCEEDED)

#### 测试代码示例

```java
@Test
@DisplayName("成功登录")
void testLogin_Success() throws Exception {
    createUser("user@example.com");

    String requestBody = """
        {
            "email": "user@example.com",
            "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.name").exists())
            .andExpect(jsonPath("$.data.avatar").exists());
}

@Test
@DisplayName("密码错误")
void testLogin_WrongPassword() throws Exception {
    createUser("user@example.com");

    String requestBody = """
        {
            "email": "user@example.com",
            "password": "wrongpassword"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1103));
}
```

---

### 3. 邮箱验证 (POST /api/v1/auth/validate-email)

#### 测试场景

##### 3.1 成功验证邮箱
- **准备**：创建用户，生成验证码
- **请求**：POST /api/v1/auth/validate-email，传入 email、code
- **验证**：
  - 返回 200 状态码
  - 返回用户完整信息
  - emailValidated 变为 true
  - 自动登录（响应头包含 satoken）

##### 3.2 字段验证 - email 为空
- **请求**：email=""
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 3.3 字段验证 - code 为空
- **请求**：code=""
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 3.4 业务验证 - 验证码无效
- **准备**：创建用户
- **请求**：code="000000"（错误的验证码）
- **验证**：返回 1108 (INVALID_CODE)，提示"验证码无效"

##### 3.5 业务验证 - 验证码过期
- **准备**：创建用户，生成验证码，等待过期
- **请求**：传入过期的验证码
- **验证**：返回 1110 (CODE_EXPIRED)，提示"验证码已过期"

##### 3.6 限流验证 - 10次/分钟
- **准备**：连续验证11次
- **验证**：第11次返回 2301 (RATE_LIMIT_EXCEEDED)

#### 测试代码示例

```java
@Test
@DisplayName("成功验证邮箱")
void testValidateEmail_Success() throws Exception {
    UserDO user = createUser("user@example.com");
    // 假设验证码为 "123456"（实际测试中需要从系统获取）
    String code = "123456";

    String requestBody = String.format("""
        {
            "email": "user@example.com",
            "code": "%s"
        }
        """, code);

    mockMvc.perform(post("/api/v1/auth/validate-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.emailValidated").value(true));

    // 验证数据库状态
    UserDO updatedUser = userDataService.getById(user.getId());
    assertThat(updatedUser.getEmailValidated()).isTrue();
}
```

---

### 4. 修改当前用户信息 (PUT /api/v1/users/current)

#### 测试场景

##### 4.1 成功修改
- **准备**：创建用户并登录
- **请求**：PUT /api/v1/users/current，传入 name、biography
- **验证**：
  - 返回 200 状态码
  - 用户信息已更新

##### 4.2 字段验证 - name 为空
- **准备**：登录
- **请求**：name=""
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"用户名不能为空"

##### 4.3 字段验证 - name 超长
- **准备**：登录
- **请求**：name="超过最大长度的用户名..."
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 4.4 字段验证 - biography 超长
- **准备**：登录
- **请求**：biography="超过最大长度的简介..."
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 4.5 权限验证 - 未登录
- **请求**：不传 token
- **验证**：返回 1101 (USER_NOT_LOGIN)，提示"用户未登录"

#### 测试代码示例

```java
@Test
@DisplayName("成功修改用户信息")
void testUpdateCurrentUser_Success() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());

    try {
        String requestBody = """
            {
                "name": "新用户名",
                "biography": "这是我的新简介"
            }
            """;

        mockMvc.perform(put("/api/v1/users/current")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证数据库状态
        UserDO updatedUser = userDataService.getById(user.getId());
        assertThat(updatedUser.getName()).isEqualTo("新用户名");
        assertThat(updatedUser.getBiography()).isEqualTo("这是我的新简介");
    } finally {
        StpUtil.logout();
    }
}

@Test
@DisplayName("未登录修改信息")
void testUpdateCurrentUser_NotLoggedIn() throws Exception {
    String requestBody = """
        {
            "name": "新用户名",
            "biography": "这是我的新简介"
        }
        """;

    mockMvc.perform(put("/api/v1/users/current")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
}
```

---

### 5. 更新用户头像 (POST /api/v1/users/avatar)

#### 测试场景

##### 5.1 成功更新头像
- **准备**：创建用户并登录，准备图片文件
- **请求**：POST /api/v1/users/avatar，上传图片文件
- **验证**：
  - 返回 200 状态码
  - 返回新头像的 URL
  - 用户头像已更新

##### 5.2 字段验证 - 文件为空
- **准备**：登录
- **请求**：file=null
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"文件不能为空"

##### 5.3 业务验证 - 文件类型不支持
- **准备**：登录，准备 PDF 文件
- **请求**：上传 PDF 文件
- **验证**：返回 2404 (UNSUPPORTED_FILE_TYPE)，提示"不支持的文件类型"

##### 5.4 业务验证 - 文件过大
- **准备**：登录，准备超过限制的大图片
- **请求**：上传大文件
- **验证**：返回 2403 (FILE_TOO_LARGE)，提示"文件大小超出限制"

##### 5.5 权限验证 - 未登录
- **请求**：不传 token
- **验证**：返回 1101 (USER_NOT_LOGIN)

##### 5.6 限流验证 - 5次/分钟
- **准备**：连续上传6次
- **验证**：第6次返回 2301 (RATE_LIMIT_EXCEEDED)

#### 测试代码示例

```java
@Test
@DisplayName("成功更新头像")
void testUpdateAvatar_Success() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());

    try {
        // 创建模拟图片文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "avatar.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/users/avatar")
                .file(file)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isString());

        // 验证用户头像已更新
        UserDO updatedUser = userDataService.getById(user.getId());
        assertThat(updatedUser.getAvatar()).isNotNull();
    } finally {
        StpUtil.logout();
    }
}
```

---

## Query 测试（读操作）

### 6. 获取当前用户信息 (GET /api/v1/users/current)

#### 测试场景

##### 6.1 成功获取
- **准备**：创建用户并登录
- **请求**：GET /api/v1/users/current
- **验证**：
  - 返回 200 状态码
  - 返回完整用户信息（包含 email 等敏感信息）
  - 包含 id, name, email, avatar, biography, emailValidated, createdAt

##### 6.2 权限验证 - 未登录
- **请求**：不传 token
- **验证**：返回 1101 (USER_NOT_LOGIN)

#### 测试代码示例

```java
@Test
@DisplayName("成功获取当前用户信息")
void testGetCurrentUser_Success() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());

    try {
        mockMvc.perform(get("/api/v1/users/current")
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.avatar").exists())
                .andExpect(jsonPath("$.data.biography").exists())
                .andExpect(jsonPath("$.data.emailValidated").exists())
                .andExpect(jsonPath("$.data.createdAt").exists());
    } finally {
        StpUtil.logout();
    }
}

@Test
@DisplayName("未登录获取当前用户信息")
void testGetCurrentUser_NotLoggedIn() throws Exception {
    mockMvc.perform(get("/api/v1/users/current"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
}
```

---

### 7. 获取用户公开信息 (GET /api/v1/users/{username})

#### 测试场景

##### 7.1 成功获取
- **准备**：创建用户A和用户B，用户A登录
- **请求**：GET /api/v1/users/{userB.username}
- **验证**：
  - 返回 200 状态码
  - 返回用户B的公开信息（不含 email）
  - 包含 isFollowing 字段（当前用户是否关注该用户）

##### 7.2 查看已关注用户
- **准备**：用户A关注用户B
- **请求**：GET /api/v1/users/{userB.username}
- **验证**：isFollowing=true

##### 7.3 参数验证 - username 为空
- **请求**：username=""
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 7.4 业务验证 - 用户不存在
- **请求**：username="nonexistent"
- **验证**：返回 1007 (NOT_FOUND)，提示"用户不存在"

##### 7.5 权限验证 - 需要登录
- **请求**：不传 token
- **验证**：返回 1101 (USER_NOT_LOGIN)

#### 测试代码示例

```java
@Test
@DisplayName("成功获取用户公开信息")
void testGetUser_Success() throws Exception {
    UserDO currentUser = createUser("current@example.com");
    UserDO targetUser = createUser("target@example.com");

    StpUtil.login(currentUser.getId());

    try {
        mockMvc.perform(get("/api/v1/users/" + targetUser.getName())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(targetUser.getId()))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.avatar").exists())
                .andExpect(jsonPath("$.data.biography").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.isFollowing").exists())
                .andExpect(jsonPath("$.data.email").doesNotExist()); // 不包含邮箱
    } finally {
        StpUtil.logout();
    }
}

@Test
@DisplayName("用户不存在")
void testGetUser_NotFound() throws Exception {
    UserDO user = createUser("user@example.com");
    StpUtil.login(user.getId());

    try {
        mockMvc.perform(get("/api/v1/users/nonexistent")
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1007));
    } finally {
        StpUtil.logout();
    }
}
```

---

### 8. 搜索用户 (GET /api/v1/users/search)

#### 测试场景

##### 8.1 成功搜索
- **准备**：创建多个用户（张三、张三丰、李四）
- **请求**：GET /api/v1/users/search?name=张三
- **验证**：
  - 返回 200 状态码
  - 返回包含"张三"的用户列表（张三、张三丰）
  - 每个用户包含 id, name, avatar

##### 8.2 空结果
- **请求**：name="不存在的用户名"
- **验证**：返回空数组

##### 8.3 参数验证 - name 为空
- **请求**：name=""
- **验证**：返回 1002 (INVALID_PARAMETER)，提示"搜索名称不能为空"

##### 8.4 参数验证 - name 缺失
- **请求**：不传 name 参数
- **验证**：返回 1002 (INVALID_PARAMETER)

##### 8.5 限流验证 - 30次/分钟（按IP）
- **准备**：连续搜索31次
- **验证**：第31次返回 2301 (RATE_LIMIT_EXCEEDED)

##### 8.6 不需要登录
- **请求**：不传 token
- **验证**：可以正常搜索

#### 测试代码示例

```java
@Test
@DisplayName("成功搜索用户")
void testSearchUsers_Success() throws Exception {
    createUser("zhangsan@example.com");  // name: zhangsan
    createUser("zhangsanfeng@example.com");  // name: zhangsanfeng
    createUser("lisi@example.com");  // name: lisi

    mockMvc.perform(get("/api/v1/users/search")
            .param("name", "zhang"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").exists())
            .andExpect(jsonPath("$.data[0].name").exists())
            .andExpect(jsonPath("$.data[0].avatar").exists());
}

@Test
@DisplayName("搜索名称为空")
void testSearchUsers_EmptyName() throws Exception {
    mockMvc.perform(get("/api/v1/users/search")
            .param("name", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
}

@Test
@DisplayName("不需要登录也可搜索")
void testSearchUsers_NoLoginRequired() throws Exception {
    createUser("user@example.com");

    mockMvc.perform(get("/api/v1/users/search")
            .param("name", "user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

---

## 测试覆盖率要求

### 必测场景清单

每个接口至少包含：
- [x] 正常场景（happy path）
- [x] 参数验证（null、空、格式错误）
- [x] 认证授权（需要登录的接口测试未登录）
- [x] 数据不存在（404/NOT_FOUND）
- [x] 业务规则验证（邮箱重复、密码错误等）
- [x] 限流验证

### 接口覆盖情况

| 接口 | 正常场景 | 参数验证 | 认证授权 | 数据不存在 | 业务规则 | 限流 |
|------|----------|----------|----------|------------|----------|------|
| 用户注册 | ✅ | ✅ | N/A | N/A | ✅ | ✅ |
| 用户登录 | ✅ | ✅ | N/A | ✅ | ✅ | ✅ |
| 邮箱验证 | ✅ | ✅ | N/A | N/A | ✅ | ✅ |
| 获取当前用户信息 | ✅ | N/A | ✅ | N/A | N/A | N/A |
| 修改当前用户信息 | ✅ | ✅ | ✅ | N/A | N/A | N/A |
| 更新用户头像 | ✅ | ✅ | ✅ | N/A | ✅ | ✅ |
| 获取用户公开信息 | ✅ | ✅ | ✅ | ✅ | N/A | N/A |
| 搜索用户 | ✅ | ✅ | ✅ | N/A | N/A | ✅ |

---

## 运行测试

```bash
# 运行所有用户相关测试
mvn test -Dtest=UsersControllerTest

# 运行单个测试方法
mvn test -Dtest=UsersControllerTest#testRegister_Success

# 查看覆盖率报告
mvn test jacoco:report
```

---

## 注意事项

1. **密码加密**:
   - 测试中使用明文密码 "password123"
   - 验证时需要使用相同密码
   - 实际存储时密码已加密

2. **邮箱验证码**:
   - 测试中可能需要 mock 验证码生成和存储
   - 或者直接从数据库/Redis 读取生成的验证码

3. **文件上传**:
   - 使用 `MockMultipartFile` 模拟文件上传
   - 需要 mock 图片上传服务或使用真实服务

4. **限流测试**:
   - 限流基于 Redis 实现
   - 测试环境需要配置 Redis
   - 或者 mock 限流逻辑

5. **认证状态**:
   - 使用 `StpUtil.login()` 和 `StpUtil.logout()` 管理
   - 务必在 finally 块中清理登录状态

6. **@Transactional**:
   - 所有测试自动回滚
   - 但 Redis 中的数据（如限流计数、验证码）不会回滚
   - 可能需要在测试前后清理 Redis
