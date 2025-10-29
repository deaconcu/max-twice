# 权限控制系统设计文档

## 一、概述

### 1.1 目标
为系统添加基于角色的权限控制（RBAC），实现：
1. 用户只能修改自己发布的内容
2. 管理员可以进后台操作所有数据

### 1.2 技术选型
- **权限框架**：Sa-Token（已集成）
- **存储方式**：数据库 `user.role` 字段（INT 类型）
- **代码实现**：枚举 + 注解 + AOP

---

## 二、数据库设计

### 2.1 user 表修改

```sql
-- 添加 role 字段
ALTER TABLE `user`
ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0
COMMENT '角色代码: 0=普通用户, 1=审核员, 2=管理员, 3=超级管理员（代码非权限级别）'
AFTER `state`;

-- 创建索引（可选，如果需要频繁按角色查询）
CREATE INDEX idx_user_role ON `user`(`role`);

-- 设置初始超级管理员
UPDATE `user` SET `role` = 3 WHERE `id` = 1;  -- 替换为你的用户ID
```

**说明**：
- `role` 字段存储的是角色代码（0,1,2,3），不是权限级别
- 角色代码按添加顺序分配，用于唯一标识角色
- 权限级别在枚举中定义，用于权限比较

### 2.2 角色定义（4级层级）

| 角色代码 | 角色名称 | 角色说明 | 权限级别 | 权限范围 |
|---------|---------|---------|---------|---------|
| 0 | user | 普通用户 | 0 | 创建内容、编辑/删除自己的内容 |
| 1 | moderator | 审核员 | 30 | 审核新提交的内容（state=PENDING） |
| 2 | admin | 管理员 | 60 | 管理所有内容 + 用户管理 |
| 3 | super_admin | 超级管理员 | 100 | 所有权限 + 系统管理 |

**角色层级关系**：
```
USER (0级)
  └─ 只能操作自己的内容

MODERATOR (30级)
  ├─ USER 的所有权限
  └─ 审核待审核内容（state=PENDING 的数据）

ADMIN (60级)
  ├─ MODERATOR 的所有权限
  ├─ 管理所有内容数据（帖子、课程、评论等）
  ├─ 删除任何用户的内容
  ├─ 用户管理（查看所有用户、封禁用户、修改用户角色）
  └─ 查看内容统计数据

SUPER_ADMIN (100级)
  ├─ ADMIN 的所有权限
  ├─ 系统配置管理
  ├─ 清理缓存、系统维护
  └─ 查看系统统计数据
```

**权限功能对比表**：

| 功能分类 | 具体功能 | USER | MODERATOR | ADMIN | SUPER_ADMIN |
|---------|---------|------|-----------|-------|-------------|
| **内容管理** | 创建/编辑自己的内容 | ✅ | ✅ | ✅ | ✅ |
| | 删除自己的内容 | ✅ | ✅ | ✅ | ✅ |
| | 审核待审核内容 | ❌ | ✅ | ✅ | ✅ |
| | 编辑任何用户的内容 | ❌ | ❌ | ✅ | ✅ |
| | 删除任何用户的内容 | ❌ | ❌ | ✅ | ✅ |
| **用户管理** | 查看所有用户列表 | ❌ | ❌ | ✅ | ✅ |
| | 封禁/解封用户 | ❌ | ❌ | ✅ | ✅ |
| | 修改用户角色 | ❌ | ❌ | ✅ | ✅ |
| **系统管理** | 系统配置管理 | ❌ | ❌ | ❌ | ✅ |
| | 清理缓存 | ❌ | ❌ | ❌ | ✅ |
| | 查看系统统计 | ❌ | ❌ | ❌ | ✅ |

---

## 三、后端架构设计

### 3.1 核心组件

```
┌─────────────────────────────────────────────────┐
│                  Controller                     │
│  @SaCheckLogin / @RequireRole                  │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│          PermissionAspect (AOP)                │
│  验证角色权限                                    │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│              Service 层                         │
│  业务逻辑 + 所有权验证                           │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│              DataService                        │
│  数据访问                                        │
└─────────────────────────────────────────────────┘
```

### 3.2 核心类设计

#### UserRole 枚举
```java
public enum UserRole {
    USER(0, "user", "普通用户", 0),
    MODERATOR(1, "moderator", "审核员", 30),
    ADMIN(2, "admin", "管理员", 60),
    SUPER_ADMIN(3, "super_admin", "超级管理员", 100);

    private final int code;           // 数据库存储值
    private final String name;        // Sa-Token 使用的角色名
    private final String description; // 角色描述
    private final int level;          // 权限级别（用于权限比较）

    UserRole(int code, String name, String description, int level) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.level = level;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    // 根据 code 获取枚举
    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role code: " + code);
    }

    // 判断是否为管理员或更高级别
    public boolean isAdminOrHigher() {
        return this.level >= ADMIN.level;
    }

    // 判断是否为审核员或更高级别
    public boolean isModeratorOrHigher() {
        return this.level >= MODERATOR.level;
    }
}
```

#### UserDO 实体（数据库对象）
```java
@Data
public class UserDO {
    private Long id;
    private String name;
    private String email;
    private String password;
    // ... 其他字段
    private Integer role;  // 新增：角色代码字段（0=USER, 1=MODERATOR, 2=ADMIN, 3=SUPER_ADMIN）
    private Byte state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**说明**：
- UserDO 是持久层的数据对象，直接映射数据库表
- `role` 字段存储角色代码（Integer 类型）
- UserDO 被 `UserDataService` 自动缓存（Redis，TTL=30分钟）

#### 业务层辅助方法（可选）

如果需要在业务层方便地操作角色，可以创建工具方法或扩展 DTO：

```java
/**
 * 用户角色工具类
 */
public class UserRoleHelper {

    /**
     * 获取用户角色枚举
     */
    public static UserRole getRoleEnum(UserDO userDO) {
        if (userDO == null || userDO.getRole() == null) {
            return UserRole.USER;  // 默认为普通用户
        }
        return UserRole.fromCode(userDO.getRole());
    }

    /**
     * 判断是否为管理员或更高级别
     */
    public static boolean isAdmin(UserDO userDO) {
        return getRoleEnum(userDO).isAdminOrHigher();
    }

    /**
     * 判断是否为审核员或更高级别
     */
    public static boolean isModerator(UserDO userDO) {
        return getRoleEnum(userDO).isModeratorOrHigher();
    }

    /**
     * 判断是否为超级管理员
     */
    public static boolean isSuperAdmin(UserDO userDO) {
        return getRoleEnum(userDO) == UserRole.SUPER_ADMIN;
    }

    /**
     * 设置用户角色
     */
    public static void setRole(UserDO userDO, UserRole role) {
        if (userDO != null && role != null) {
            userDO.setRole(role.getCode());
        }
    }
}
```

#### PermissionUtil 工具类
```java
/**
 * 权限工具类
 *
 * 设计原则：
 * - 不依赖 Web 层（不调用 StpUtil）
 * - 接收 UserDO 参数，保持方法纯粹
 * - 提高可测试性和复用性
 */
public class PermissionUtil {

    /**
     * 判断用户是否为管理员或更高级别
     */
    public static boolean isAdmin(UserDO user) {
        if (user == null) {
            return false;
        }
        return UserRoleHelper.isAdmin(user);
    }

    /**
     * 判断用户是否为超级管理员
     */
    public static boolean isSuperAdmin(UserDO user) {
        if (user == null) {
            return false;
        }
        return UserRoleHelper.isSuperAdmin(user);
    }

    /**
     * 判断用户是否为审核员或更高级别
     */
    public static boolean isModerator(UserDO user) {
        if (user == null) {
            return false;
        }
        return UserRoleHelper.isModerator(user);
    }

    /**
     * 判断是否为资源所有者或管理员
     */
    public static boolean isOwnerOrAdmin(UserDO user, Long creatorId) {
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return user.getId().equals(creatorId);
    }

    /**
     * 要求用户必须是管理员
     */
    public static void requireAdmin(UserDO user) {
        if (!isAdmin(user)) {
            throw new NotPermissionException("需要管理员权限");
        }
    }

    /**
     * 要求用户必须是超级管理员
     */
    public static void requireSuperAdmin(UserDO user) {
        if (!isSuperAdmin(user)) {
            throw new NotPermissionException("需要超级管理员权限");
        }
    }

    /**
     * 要求用户必须是资源所有者或管理员
     */
    public static void requireOwnerOrAdmin(UserDO user, Long creatorId) {
        if (!isOwnerOrAdmin(user, creatorId)) {
            throw new NotPermissionException("没有权限操作此资源");
        }
    }
}
```

#### StpInterfaceImpl (Sa-Token 权限接口)
```java
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserDataService userDataService;

    /**
     * 返回用户的角色列表
     *
     * 说明：
     * - userDataService.getById() 自动使用 Redis 缓存
     * - 首次查询会访问数据库并缓存 30 分钟
     * - 后续查询直接从 Redis 返回（~2ms）
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());

        // 从缓存/数据库获取用户信息（包括 role 字段）
        UserDO userDO = userDataService.getById(userId);

        if (userDO == null || userDO.getRole() == null) {
            return Collections.emptyList();
        }

        // 将 role code 转换为 UserRole 枚举
        UserRole roleEnum = UserRole.fromCode(userDO.getRole());
        List<String> roles = new ArrayList<>();

        // 返回用户拥有的所有角色（包括继承的角色）
        // 例如：ADMIN 拥有 admin、moderator、user 三个角色
        switch (roleEnum) {
            case SUPER_ADMIN:
                roles.add("super_admin");
                // fall through
            case ADMIN:
                roles.add("admin");
                // fall through
            case MODERATOR:
                roles.add("moderator");
                // fall through
            case USER:
                roles.add("user");
                break;
        }

        return roles;
    }

    /**
     * 返回用户的权限列表
     *
     * 说明：同样受益于 Redis 缓存
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());

        // 从缓存/数据库获取用户信息
        UserDO userDO = userDataService.getById(userId);

        if (userDO == null || userDO.getRole() == null) {
            return Collections.emptyList();
        }

        // 将 role code 转换为 UserRole 枚举
        UserRole roleEnum = UserRole.fromCode(userDO.getRole());
        List<String> permissions = new ArrayList<>();

        // 根据角色分配权限（支持权限继承）
        switch (roleEnum) {
            case SUPER_ADMIN:
                permissions.add("system:config");
                permissions.add("system:cache");
                permissions.add("system:maintain");
                // fall through
            case ADMIN:
                permissions.add("user:manage");
                permissions.add("user:ban");
                permissions.add("role:modify");
                permissions.add("content:delete:all");
                permissions.add("content:edit:all");
                permissions.add("stats:view");
                // fall through
            case MODERATOR:
                permissions.add("content:moderate");
                permissions.add("content:view:pending");
                // fall through
            case USER:
                permissions.add("content:create");
                permissions.add("content:edit:own");
                permissions.add("content:delete:own");
                break;
        }

        return permissions;
    }
}
```

---

## 四、权限控制策略

### 4.1 角色权限矩阵

| 操作 | USER | MODERATOR | ADMIN | SUPER_ADMIN |
|-----|------|-----------|-------|-------------|
| **帖子 (Post)** |
| 创建帖子 | ✅ | ✅ | ✅ | ✅ |
| 查看帖子 | ✅ | ✅ | ✅ | ✅ |
| 编辑自己的帖子 | ✅ | ✅ | ✅ | ✅ |
| 删除自己的帖子 | ✅ | ✅ | ✅ | ✅ |
| 编辑他人的帖子 | ❌ | ❌ | ✅ | ✅ |
| 删除他人的帖子 | ❌ | ❌ | ✅ | ✅ |
| 查看待审核帖子 | ❌ | ✅ | ✅ | ✅ |
| 审核帖子（通过/拒绝） | ❌ | ✅ | ✅ | ✅ |
| **评论 (Comment)** |
| 发表评论 | ✅ | ✅ | ✅ | ✅ |
| 编辑自己的评论 | ✅ | ✅ | ✅ | ✅ |
| 删除自己的评论 | ✅ | ✅ | ✅ | ✅ |
| 编辑他人的评论 | ❌ | ❌ | ✅ | ✅ |
| 删除他人的评论 | ❌ | ❌ | ✅ | ✅ |
| **课程 (Course)** |
| 创建课程 | ✅ | ✅ | ✅ | ✅ |
| 编辑自己的课程 | ✅ | ✅ | ✅ | ✅ |
| 删除自己的课程 | ✅ | ✅ | ✅ | ✅ |
| 编辑他人的课程 | ❌ | ❌ | ✅ | ✅ |
| 删除他人的课程 | ❌ | ❌ | ✅ | ✅ |
| **路线图 (Roadmap)** |
| 创建路线图 | ✅ | ✅ | ✅ | ✅ |
| 编辑自己的路线图 | ✅ | ✅ | ✅ | ✅ |
| 删除自己的路线图 | ✅ | ✅ | ✅ | ✅ |
| 编辑他人的路线图 | ❌ | ❌ | ✅ | ✅ |
| 删除他人的路线图 | ❌ | ❌ | ✅ | ✅ |
| **用户管理** |
| 查看所有用户 | ❌ | ❌ | ✅ | ✅ |
| 封禁用户 | ❌ | ❌ | ✅ | ✅ |
| 修改用户角色（USER/MODERATOR/ADMIN） | ❌ | ❌ | ✅ | ✅ |
| 修改用户角色（SUPER_ADMIN） | ❌ | ❌ | ❌ | ✅ |
| **系统管理** |
| 查看系统统计 | ❌ | ❌ | ✅ | ✅ |
| 清理缓存 | ❌ | ❌ | ❌ | ✅ |
| 系统配置管理 | ❌ | ❌ | ❌ | ✅ |
| 系统维护操作 | ❌ | ❌ | ❌ | ✅ |

### 4.2 权限判断逻辑

```java
// 伪代码
boolean canEdit(User currentUser, Resource resource) {
    // 1. 管理员及以上可以编辑所有内容
    if (currentUser.isAdmin()) {
        return true;
    }

    // 2. 审核员可以审核待审核内容
    if (currentUser.isModerator()
        && resource.getState() == State.PENDING) {
        return true;
    }

    // 3. 普通用户只能编辑自己的内容
    return currentUser.getId().equals(resource.getCreatorId());
}

boolean canModifyRole(User operator, User target, UserRole newRole) {
    // 1. 只有超级管理员可以设置超级管理员
    if (newRole == UserRole.SUPER_ADMIN) {
        return operator.isSuperAdmin();
    }

    // 2. 管理员及以上可以设置其他角色
    if (operator.isAdmin()) {
        // 但不能修改自己的角色
        return !operator.getId().equals(target.getId());
    }

    return false;
}
```

---

## 五、实现方案

### 5.1 混合方案（推荐）✅

**核心原则**：
- **角色检查**：使用 `@RequireRole` 注解（Controller 层）
- **所有权检查**：在 Service 层验证（避免重复查询）
- **自动注入**：使用 `@CurrentUser` 注解注入用户对象

**优势**：
- ✅ 性能最优：避免重复查询数据库
- ✅ 职责清晰：角色检查用注解，所有权检查用代码
- ✅ Service 独立：可在任何场景调用（定时任务、MQ、其他 Service）
- ✅ 易于维护：代码集中，逻辑清晰

**适用场景**：
1. **@RequireRole**：用于纯角色检查（管理后台、系统管理接口）
2. **Service 验证**：用于所有者检查（更新、删除等需要验证资源所有权的操作）

### 5.2 废弃的方案（性能问题）❌

~~**@RequireOwner 注解**：会导致查询两次数据库~~
- ~~PermissionAspect 查询一次（获取 creatorId）~~
- ~~Service 方法再查询一次（获取完整对象）~~
- ~~性能损耗，不推荐使用~~

### 5.3 定义注解

```java
/**
 * 标记需要注入当前用户
 * 用在方法参数上
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

/**
 * 要求特定角色（仅用于角色检查，不涉及资源所有权）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    UserRole value();  // 需要的最低角色级别
}
```

### 5.4 实现参数解析器（自动注入 UserDO）

```java
/**
 * CurrentUser 参数解析器
 * 自动将当前登录用户注入到标注了 @CurrentUser 的参数
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private UserDataService userDataService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 判断参数是否标注了 @CurrentUser 注解
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(UserDO.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        try {
            // 从 Sa-Token 获取当前用户ID
            Long userId = StpUtil.getLoginIdAsLong();

            // 从缓存/数据库获取用户信息（自动使用 Redis 缓存）
            UserDO user = userDataService.getById(userId);

            if (user == null) {
                throw ErrorCode.USER_NOT_FOUND.exception();
            }

            return user;

        } catch (NotLoginException e) {
            throw ErrorCode.NOT_LOGIN.exception();
        }
    }
}

/**
 * 注册参数解析器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
```

#### 5.2.3 实现权限检查切面

```java
/**
 * 权限检查切面
 * 自动检查 @RequireRole 注解
 */
@Aspect
@Component
@Slf4j
public class PermissionAspect {

    @Autowired
    private UserDataService userDataService;

    /**
     * 检查角色权限
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        // 获取当前用户
        UserDO currentUser = getCurrentUser(joinPoint);

        // 获取需要的角色
        UserRole requiredRole = requireRole.value();
        UserRole currentRole = UserRoleHelper.getRoleEnum(currentUser);

        // 检查权限级别
        if (currentRole.getLevel() < requiredRole.getLevel()) {
            log.warn("用户 {} (角色:{}) 尝试访问需要 {} 权限的接口",
                currentUser.getId(), currentRole, requiredRole);
            throw new NotPermissionException("需要 " + requiredRole.getDescription() + " 权限");
        }

        log.debug("用户 {} 通过角色检查: {}", currentUser.getId(), requiredRole);
    }

### 5.5 实现权限检查切面（仅角色检查）

```java
/**
 * 权限检查切面
 * 仅检查 @RequireRole 注解（角色权限）
 * 所有权检查在 Service 层进行
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final UserDataService userDataService;

    /**
     * 检查角色权限
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        // 获取当前用户
        UserDO currentUser = getCurrentUser(joinPoint);

        // 获取需要的角色
        UserRole requiredRole = requireRole.value();
        UserRole currentRole = currentUser.getRoleEnum();

        // 检查权限级别
        if (!currentUser.hasRole(requiredRole)) {
            log.warn("用户 {} (角色:{}) 尝试访问需要 {} 权限的接口",
                currentUser.getId(), currentRole, requiredRole);
            throw new NotPermissionException("需要 " + requiredRole.getDescription() + " 权限");
        }

        log.debug("用户 {} 通过角色检查: {}", currentUser.getId(), requiredRole);
    }

    /**
     * 从方法参数中获取当前用户
     */
    private UserDO getCurrentUser(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        // 查找标注了 @CurrentUser 的参数
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(CurrentUser.class)) {
                Object arg = args[i];
                if (arg instanceof UserDO) {
                    return (UserDO) arg;
                }
            }
        }

        // 如果没有 @CurrentUser 参数，手动获取
        Long userId = StpUtil.getLoginIdAsLong();
        return userDataService.getById(userId);
    }
}
```

### 5.6 Controller 使用示例

```java
@RestController
@RequestMapping("/api/v1/posts")
public class PostsController {

    @Autowired
    private PostService postService;

    /**
     * 创建帖子 - 需要登录
     */
    @PostMapping
    @SaCheckLogin
    public ApiResponse<PostDTO> createPost(
            @RequestBody CreatePostRequest request,
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        PostDTO post = postService.createPost(request, currentUser);
        return ApiResponse.success(post);
    }

    /**
     * 更新帖子 - Service 层验证所有权
     */
    @PutMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<Void> updatePost(
            @PathVariable Long id,
            @RequestBody UpdatePostRequest request,
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        postService.updatePost(id, request, currentUser);
        return ApiResponse.success(null);
    }

    /**
     * 删除帖子 - Service 层验证所有权
     */
    @DeleteMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<Void> deletePost(
            @PathVariable Long id,
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        postService.deletePost(id, currentUser);
        return ApiResponse.success(null);
    }

    /**
     * 获取待审核帖子 - 需要审核员权限（使用注解）
     */
    @GetMapping("/pending")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<List<PostDTO>> getPendingPosts() {
        return ApiResponse.success(postService.getPendingPosts());
    }
}

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private SystemService systemService;

    /**
     * 获取所有用户 - 需要管理员权限
     */
    @GetMapping("/users")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<List<UserDTO>> getAllUsers(
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        return ApiResponse.success(userService.getAllUsers(currentUser));
    }

    /**
     * 修改用户角色 - 需要管理员权限
     */
    @PostMapping("/users/{id}/role")
    @RequireRole(UserRole.ADMIN)
    public ApiResponse<Void> setUserRole(
            @PathVariable Long id,
            @RequestParam Integer roleCode,
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        userService.setUserRole(id, roleCode, currentUser);
        return ApiResponse.success(null);
    }

    /**
     * 系统配置 - 需要超级管理员权限
     */
    @PostMapping("/system/config")
    @RequireRole(UserRole.SUPER_ADMIN)
    public ApiResponse<Void> updateSystemConfig(
            @RequestBody SystemConfigRequest request,
            @CurrentUser UserDO currentUser  // 自动注入
    ) {
        systemService.updateConfig(request, currentUser);
        return ApiResponse.success(null);
    }
}
```

#### 5.2.5 Service 层实现

```java
/**
 * Service 层
 * 不依赖 Web 层，接收 UserDO 参数
 * 可在任何场景调用（Controller、定时任务、消息队列）
 */
@Service
public class PostService {

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 创建帖子
     */
    public PostDTO createPost(CreatePostRequest request, UserDO creator) {
        PostDO post = new PostDO();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCreatorId(creator.getId());
        post.setState(State.PUBLISHED.getCode());

        postDataService.insert(post);

        log.info("用户 {} 创建了帖子 {}", creator.getId(), post.getId());

        return convertToDTO(post);
    }

    /**
     * 更新帖子
     */
    public void updatePost(Long postId, UpdatePostRequest request, UserDO operator) {
        PostDO post = postDataService.getById(postId);
        if (post == null) {
            throw ErrorCode.POST_NOT_FOUND.exception();
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        postDataService.update(post);
### 5.7 Service 层实现（带权限验证）

```java
/**
 * Service 层
 * 不依赖 Web 层，接收 UserDO 参数
 * 可在任何场景调用（Controller、定时任务、消息队列）
 * 在 Service 层验证所有权权限
 */
@Service
public class PostService {

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 创建帖子
     */
    public PostDTO createPost(CreatePostRequest request, UserDO creator) {
        PostDO post = new PostDO();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCreatorId(creator.getId());
        post.setState(State.PUBLISHED.getCode());

        postDataService.insert(post);

        log.info("用户 {} 创建了帖子 {}", creator.getId(), post.getId());

        return convertToDTO(post);
    }

    /**
     * 更新帖子（Service 层验证所有权）
     */
    public void updatePost(Long postId, UpdatePostRequest request, UserDO operator) {
        PostDO post = postDataService.getById(postId);
        if (post == null) {
            throw ErrorCode.POST_NOT_FOUND.exception();
        }

        // 验证权限：只有所有者或管理员可以修改
        if (!post.getCreatorId().equals(operator.getId()) && !operator.isAdmin()) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        postDataService.update(post);

        log.info("用户 {} 更新了帖子 {}", operator.getId(), postId);
    }

    /**
     * 删除帖子（Service 层验证所有权）
     */
    public void deletePost(Long postId, UserDO operator) {
        PostDO post = postDataService.getById(postId);
        if (post == null) {
            throw ErrorCode.POST_NOT_FOUND.exception();
        }

        // 验证权限：只有所有者或管理员可以删除
        if (!post.getCreatorId().equals(operator.getId()) && !operator.isAdmin()) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        postDataService.deleteById(postId);

        // 如果是管理员删除他人内容，记录审计日志
        if (!post.getCreatorId().equals(operator.getId())) {
            auditLogService.log(AuditLog.builder()
                .userId(operator.getId())
                .operation("ADMIN_DELETE_POST")
                .resourceType("POST")
                .resourceId(postId)
                .beforeValue(JSON.toJSONString(post))
                .build());

            log.warn("管理员 {} 删除了用户 {} 的帖子 {}",
                operator.getId(), post.getCreatorId(), postId);
        } else {
            log.info("用户 {} 删除了自己的帖子 {}", operator.getId(), postId);
        }
    }

    /**
     * 获取待审核帖子（审核员功能）
     * 注意：角色权限已在 Controller 层通过 @RequireRole 验证
     */
    public List<PostDTO> getPendingPosts() {
        List<PostDO> posts = postDataService.findByState(State.PENDING.getCode());
        return posts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
```

**方案优势**：
- ✅ **性能最优**：只查询一次数据库
- ✅ **Service 独立**：可在任何地方调用，不依赖 Controller 注解
- ✅ **代码集中**：权限逻辑在 Service 层，易于理解和维护
- ✅ **职责分明**：Controller 负责登录/角色检查，Service 负责所有权检查
- ✅ **易于测试**：Service 方法可以直接传入 UserDO 测试

---

## 六、需要权限控制的接口清单

### 6.1 帖子相关 (PostsController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建帖子 | POST | `/api/v1/posts` | 登录用户 |
| 更新帖子 | PUT | `/api/v1/posts/{id}` | 所有者或管理员 |
| 删除帖子 | DELETE | `/api/v1/posts/{id}` | 所有者或管理员 |
| 获取待审核帖子 | GET | `/api/v1/posts/pending` | 审核员及以上 |

### 6.2 评论相关 (CommentsController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建评论 | POST | `/api/v1/comments` | 登录用户 |
| 更新评论 | PUT | `/api/v1/comments/{id}` | 所有者或管理员 |
| 删除评论 | DELETE | `/api/v1/comments/{id}` | 所有者或管理员 |

### 6.3 课程相关 (CoursesController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建课程 | POST | `/api/v1/courses` | 登录用户 |
| 更新课程 | PUT | `/api/v1/courses/{id}` | 所有者或管理员 |
| 删除课程 | DELETE | `/api/v1/courses/{id}` | 所有者或管理员 |

### 6.4 路线图相关 (RoadmapsController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建路线图 | POST | `/api/v1/roadmaps` | 登录用户 |
| 更新路线图 | PUT | `/api/v1/roadmaps/{id}` | 所有者或管理员 |
| 删除路线图 | DELETE | `/api/v1/roadmaps/{id}` | 所有者或管理员 |

### 6.5 职业相关 (ProfessionsController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建职业 | POST | `/api/v1/professions` | 登录用户 |
| 更新职业 | PUT | `/api/v1/professions/{id}` | 所有者或管理员 |
| 删除职业 | DELETE | `/api/v1/professions/{id}` | 所有者或管理员 |

### 6.6 记忆卡片集相关 (MemoryCardDeckController)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 创建卡片集 | POST | `/api/v1/decks` | 登录用户 |
| 更新卡片集 | PUT | `/api/v1/decks/{id}` | 所有者或管理员 |
| 删除卡片集 | DELETE | `/api/v1/decks/{id}` | 所有者或管理员 |

### 6.7 管理员接口 (AdminController - 新建)
| 接口 | 方法 | 路径 | 权限要求 |
|-----|------|------|---------|
| 获取所有用户 | GET | `/api/v1/admin/users` | ADMIN |
| 设置用户角色 | POST | `/api/v1/admin/users/{id}/role` | ADMIN（SUPER_ADMIN除外） |
| 封禁用户 | POST | `/api/v1/admin/users/{id}/ban` | ADMIN |
| 查看待审核内容列表 | GET | `/api/v1/admin/pending` | MODERATOR |
| 审核内容（通过） | POST | `/api/v1/admin/approve/{type}/{id}` | MODERATOR |
| 审核内容（拒绝） | POST | `/api/v1/admin/reject/{type}/{id}` | MODERATOR |
| 强制删除帖子 | DELETE | `/api/v1/admin/posts/{id}` | ADMIN |
| 强制删除评论 | DELETE | `/api/v1/admin/comments/{id}` | ADMIN |
| 查看系统统计 | GET | `/api/v1/admin/system/stats` | ADMIN |
| 清理缓存 | POST | `/api/v1/admin/system/cache/clear` | SUPER_ADMIN |
| 系统配置管理 | POST | `/api/v1/admin/system/config` | SUPER_ADMIN |

---

## 七、前端适配

### 7.1 类型定义
```typescript
export enum UserRole {
  USER = 0,
  MODERATOR = 1,
  ADMIN = 2,
  SUPER_ADMIN = 3
}

export interface User {
  id: number
  name: string
  role: UserRole
}
```

### 7.2 权限工具函数
```typescript
/**
 * 判断是否为管理员或更高级别
 */
export const isAdmin = (user: User | null): boolean => {
  if (!user) return false
  return user.role >= UserRole.ADMIN
}

/**
 * 判断是否为超级管理员
 */
export const isSuperAdmin = (user: User | null): boolean => {
  return user?.role === UserRole.SUPER_ADMIN
}

/**
 * 判断是否为审核员或更高级别
 */
export const isModerator = (user: User | null): boolean => {
  if (!user) return false
  return user.role >= UserRole.MODERATOR
}

/**
 * 判断是否可以编辑资源
 */
export const canEdit = (user: User | null, creatorId: number): boolean => {
  if (!user) return false
  return isAdmin(user) || user.id === creatorId
}

/**
 * 判断是否可以修改用户角色
 */
export const canModifyRole = (operator: User | null, targetRole: UserRole): boolean => {
  if (!operator) return false

  // 只有超级管理员可以设置超级管理员
  if (targetRole === UserRole.SUPER_ADMIN) {
    return isSuperAdmin(operator)
  }

  // 管理员及以上可以设置其他角色
  return isAdmin(operator)
}
```

### 7.3 组件中使用
```vue
<template>
  <!-- 所有者或管理员可以编辑/删除 -->
  <div v-if="canEdit(currentUser, post.creatorId)">
    <button @click="handleEdit">编辑</button>
    <button @click="handleDelete">删除</button>
  </div>

  <!-- 管理员可以强制删除 -->
  <div v-if="isAdmin(currentUser)">
    <button @click="handleForceDelete">强制删除</button>
  </div>

  <!-- 审核员可以审核 -->
  <div v-if="isModerator(currentUser) && post.state === 'PENDING'">
    <button @click="handleApprove">通过</button>
    <button @click="handleReject">拒绝</button>
  </div>

  <!-- 超级管理员专属功能 -->
  <div v-if="isSuperAdmin(currentUser)">
    <button @click="handleSystemConfig">系统配置</button>
    <button @click="handleClearCache">清理缓存</button>
  </div>
</template>

<script setup lang="ts">
import { isAdmin, isModerator, isSuperAdmin, canEdit } from '@/utils/permission'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUser = computed(() => userStore.currentUser)
</script>
```

### 7.4 路由守卫
```typescript
import { isAdmin, isModerator, isSuperAdmin } from '@/utils/permission'

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const currentUser = userStore.currentUser

  // 检查是否需要超级管理员权限
  if (to.meta.requireSuperAdmin && !isSuperAdmin(currentUser)) {
    ElMessage.error('需要超级管理员权限')
    next({
      path: '/error',
      query: { type: '403', message: '需要超级管理员权限' }
    })
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requireAdmin && !isAdmin(currentUser)) {
    ElMessage.error('需要管理员权限')
    next({
      path: '/error',
      query: { type: '403', message: '需要管理员权限' }
    })
    return
  }

  // 检查是否需要审核员权限
  if (to.meta.requireModerator && !isModerator(currentUser)) {
    ElMessage.error('需要审核员权限')
    next({
      path: '/error',
      query: { type: '403', message: '需要审核员权限' }
    })
    return
  }

  next()
})

// 路由配置示例（混合方案）
const routes = [
  {
    path: '/admin',
    meta: { requireAdmin: true },
    children: [
      {
        path: 'users',
        component: UserManagement,
        meta: { requireAdmin: true }
      },
      {
        path: 'moderate',
        component: ContentModeration,
        meta: { requireModerator: true }
      },
      {
        path: 'system',
        component: SystemConfig,
        meta: { requireSuperAdmin: true }
      }
    ]
  },
  // 错误页面路由
  {
    path: '/error',
    name: 'Error',
    component: () => import('@/views/Error.vue')
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  },
  {
    // 404 兜底路由 - 必须放在最后
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]
```

**说明**：
- 使用混合方案：`/404` 用于页面不存在，`/error` 用于权限等其他错误
- 通过 query 参数传递错误类型和消息，灵活展示不同错误信息
- 只需维护两个错误页面组件，代码简洁

---

## 八、实施步骤

### 阶段一：数据库和基础设施（1-2小时）
1. ✅ 执行数据库迁移脚本
2. ✅ 创建 UserRole 枚举
3. ✅ 修改 User 实体，添加 role 字段和相关方法
4. ✅ 创建 PermissionUtil 工具类
5. ✅ 创建 @RequireRole 注解
6. ✅ 实现 PermissionAspect 切面（仅角色检查）

### 阶段二：Service 层权限验证（2-3小时）
1. ✅ 在 Service 层方法中添加所有权验证逻辑
2. ✅ 所有需要验证所有权的方法添加权限检查
3. ✅ 测试权限验证是否生效

### 阶段三：前端适配（1-2小时）
1. ✅ 添加 UserRole 枚举和类型定义
2. ✅ 实现权限工具函数
3. ✅ 修改组件，添加权限判断
4. ✅ 添加路由守卫

### 阶段四：测试（1-2小时）
1. ✅ 测试普通用户只能编辑自己的内容
2. ✅ 测试普通用户访问管理员接口被拒绝并跳转到错误页面
3. ✅ 测试审核员可以查看和审核待审核内容
4. ✅ 测试审核员不能编辑他人的内容
5. ✅ 测试管理员可以编辑所有内容
6. ✅ 测试管理员可以访问后台接口
7. ✅ 测试管理员不能设置超级管理员
8. ✅ 测试超级管理员可以设置任何角色
9. ✅ 测试超级管理员可以访问系统管理功能

---

## 九、配置说明

### 9.1 Sa-Token 配置
```yaml
# application.yml
sa-token:
  timeout: 72000           # token 有效期（秒）
  token-name: token        # token 名称
  is-concurrent: true      # 是否允许同一账号并发登录
  is-share: false          # 是否共享 token
  is-log: false            # 是否打印日志
```

### 9.2 拦截器配置
```java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();  // 默认所有路由都需要登录
        }))
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/login",                // 登录接口
            "/register",             // 注册接口
            "/api/v1/public/**",    // 公开接口
            "/swagger-ui/**",       // Swagger
            "/v3/api-docs/**"       // API 文档
        );
    }
}
```

### 9.3 启用注解支持
```java
@Configuration
public class SaTokenConfig {

    /**
     * 注册 Sa-Token 全局过滤器，启用注解鉴权
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
            .addInclude("/**")
            .addExclude("/login", "/register", "/api/v1/public/**")
            .setAuth(obj -> {
                // 启用注解鉴权功能
                SaRouter.match("/**", r -> StpUtil.checkLogin());
            })
            .setError(e -> {
                // 统一异常处理
                if (e instanceof NotLoginException) {
                    return ApiResponse.error(401, "未登录");
                }
                if (e instanceof NotPermissionException) {
                    return ApiResponse.error(403, "无权限");
                }
                if (e instanceof NotRoleException) {
                    return ApiResponse.error(403, "角色权限不足");
                }
                return ApiResponse.error(500, e.getMessage());
            });
    }

    /**
     * 注册注解拦截器，启用 @SaCheckRole、@SaCheckPermission 等注解
     */
    @Bean
    public SaAnnotationInterceptor getSaAnnotationInterceptor() {
        return new SaAnnotationInterceptor();
    }
}
```

---

## 十、实施 Checklist

### 📋 完整实施清单

#### **阶段一：数据库准备**
- [x] 执行数据库迁移脚本，添加 `role` 字段
- [x] 创建索引 `idx_user_role`
- [x] 设置初始超级管理员（修改 SQL 中的用户 ID）
- [x] 验证数据库修改成功

#### **阶段二：枚举和实体类**
- [x] 创建 `UserRole` 枚举（包含 4 个角色：USER, MODERATOR, ADMIN, SUPER_ADMIN）
- [x] 添加枚举的构造函数、getter 方法
- [x] 实现 `fromCode()` 静态方法
- [x] 实现 `equalOrHigher()` 方法（替代 `isAdminOrHigher()` 和 `isModeratorOrHigher()`）
- [x] 修改 `UserDO`，添加 `role` 字段（Integer 类型）
- [x] 添加 `UserDO` 的角色相关便捷方法（getRoleEnum, setRoleEnum, hasRole）
- [x] 跳过 `UserRoleHelper` 工具类（方法直接添加到 UserDO 中）

#### **阶段三：自定义注解**
- [x] 创建 `@CurrentUser` 注解（参数注解）
- [x] 跳过 `@RequireLogin` 注解（使用 Sa-Token 的 `@SaCheckLogin`）
- [x] 创建 `@RequireRole` 注解（方法注解）
- [x] 跳过 `@RequireOwner` 注解（在 Service 层验证所有权，避免性能问题）

#### **阶段四：参数解析器**
- [x] 创建 `CurrentUserArgumentResolver` 类
- [x] 实现 `supportsParameter()` 方法
- [x] 实现 `resolveArgument()` 方法（从 Sa-Token 获取用户）
- [x] 注册参数解析器到 `WebMvcConfig`

#### **阶段五：权限检查切面**
- [x] 创建 `PermissionAspect` 类
- [x] 实现 `checkRole()` 方法（角色权限检查）
- [x] 实现 `getCurrentUser()` 辅助方法
- [x] 跳过 `checkOwner()` 方法（在 Service 层验证所有权）

#### **阶段六：权限工具类**
- [x] 创建 `PermissionUtil` 工具类
- [x] 实现 `isOwnerOrAdmin(UserDO, Long)` 方法
- [x] 实现 `requireAdmin(UserDO)` 方法
- [x] 实现 `requireSuperAdmin(UserDO)` 方法
- [x] 实现 `requireModerator(UserDO)` 方法
- [x] 实现 `requireOwnerOrAdmin(UserDO, Long)` 方法
- [x] 移除重复方法（基础权限判断使用 UserDO 的方法）

#### **阶段七：Sa-Token 集成**
- [x] 跳过 `StpInterfaceImpl`（只使用自己的注解 @RequireRole）
- [x] 保留 Sa-Token 的 `@SaCheckLogin` 用于登录验证

#### **阶段八：Controller 改造**
- [x] PostsController 添加权限注解
  - [x] `getPosts()` - @SaCheckLogin + @CurrentUser
  - [x] `createPost()` - @SaCheckLogin + @CurrentUser
  - [x] `updatePost()` - @SaCheckLogin + @CurrentUser（Service 层验证所有权）
  - [x] `deletePost()` - @SaCheckLogin + @CurrentUser（Service 层验证所有权）
  - [x] `getPostsByState()` - @RequireRole(MODERATOR)
  - [x] `getPostsByNodeAndCreator()` - @RequireRole(MODERATOR)
  - [x] `getPendingPosts()` - @RequireRole(MODERATOR)
  - [x] `approvePost()` - @RequireRole(MODERATOR) + @CurrentUser
  - [x] `getCurrentUserAllPosts()` - @SaCheckLogin + @CurrentUser
- [x] CommentsController 添加权限注解
  - [x] `createComment()` - @SaCheckLogin + @CurrentUser
  - [x] `updateComment()` - @SaCheckLogin + @CurrentUser（Service 层验证所有权）
  - [x] `deleteComment()` - @SaCheckLogin + @CurrentUser（Service 层验证所有权）
- [x] CoursesController 添加权限注解
  - [x] `createCourse()` - @SaCheckLogin + @CurrentUser
  - [x] `updateCourse()` - @SaCheckLogin + @CurrentUser（Service 层验证所有权）
- [x] RoadmapsController 添加权限注解
- [x] ProfessionsController 添加权限注解
- [x] 所有 Controller 方法添加 `@CurrentUser UserDO` 参数

#### **阶段九：Service 改造**
- [x] PostService 所有方法添加 `UserDO operator` 参数
- [x] CommentService 所有方法添加 `UserDO operator/commentor` 参数
- [x] CourseService 所有方法添加 `UserDO operator` 参数
- [x] RoadmapService 所有方法添加所有权验证
- [x] ProfessionService 所有方法添加所有权验证
- [x] UserService 添加角色管理方法
  - [x] `setUserRole(Long userId, UserRole role, UserDO operator)`
  - [x] `banUser(Long userId, String reason, UserDO operator)`
- [x] 移除所有 Service 中对 `StpUtil` 的调用

#### **阶段十：审计日志**
- [ ] 创建 `audit_log` 数据库表 ⏭️ 跳过
- [ ] 创建 `AuditLog` 实体类 ⏭️ 跳过
- [ ] 创建 `AuditLogService` 类 ⏭️ 跳过
- [ ] 在敏感操作中添加审计日志记录 ⏭️ 跳过（当前使用应用日志）
  - [ ] 修改用户角色
  - [ ] 封禁用户
  - [ ] 管理员删除他人内容
  - [ ] 清理缓存
  - [ ] 修改系统配置

#### **阶段十一：管理员接口**
- [x] 创建 `AdminController`
- [x] 实现用户管理接口
  - [x] `GET /admin/users` - 获取所有用户
  - [x] `POST /admin/users/{id}/role` - 修改用户角色
  - [x] `POST /admin/users/{id}/ban` - 封禁用户
  - [x] `PUT /admin/users/{id}/state` - 更新用户状态
- [x] 实现审核接口（已在各业务 Controller 中实现）
  - [x] 各业务 Controller 已有审核接口（Posts, Roadmaps, Professions 等）
- [ ] 实现系统管理接口 ⏭️ 跳过（暂不需要）
  - [ ] `GET /admin/system/stats` - 查看系统统计
  - [ ] `POST /admin/system/cache/clear` - 清理缓存（SUPER_ADMIN）
  - [ ] `POST /admin/system/config` - 系统配置（SUPER_ADMIN）

#### **阶段十二：前端适配**
- [x] 添加 `UserRole` 枚举（TypeScript）
- [x] 创建权限工具函数
  - [x] `isAdmin(user: User): boolean`
  - [x] `isSuperAdmin(user: User): boolean`
  - [x] `isModerator(user: User): boolean`
  - [x] `canEdit(user: User, creatorId: number): boolean`
  - [x] `canModifyRole(operator: User, targetRole: UserRole): boolean`
- [x] 创建错误页面
  - [x] `/error` 页面（Error.vue）
  - [x] `/404` 页面（NotFound.vue） ⏭️ 使用统一的 ErrorView
- [x] 实现路由守卫
  - [x] 全局前置守卫（检查 meta.requireXxx）
  - [x] 配置路由 meta 信息
- [ ] 修改组件，添加权限判断 ⏭️ 需要时在具体组件中实现
  - [ ] 帖子详情页：显示/隐藏编辑删除按钮
  - [ ] 评论组件：显示/隐藏编辑删除按钮
  - [ ] 侧边栏菜单：根据权限过滤
- [ ] 添加管理后台页面 ⏭️ AdminView.vue 已存在
  - [ ] 用户管理页面
  - [ ] 内容审核页面
  - [ ] 系统配置页面（SUPER_ADMIN）

#### **阶段十三：测试** ⏭️ 后续人工测试
- [ ] 单元测试
  - [ ] UserRole 枚举测试
  - [ ] UserRoleHelper 测试
  - [ ] PermissionUtil 测试
  - [ ] Service 层测试（传入 UserDO 参数）
- [ ] 集成测试
  - [ ] 测试普通用户只能编辑自己的内容
  - [ ] 测试普通用户访问管理员接口被拒绝
  - [ ] 测试审核员可以查看和审核待审核内容
  - [ ] 测试审核员不能编辑他人的内容
  - [ ] 测试管理员可以编辑所有内容
  - [ ] 测试管理员可以访问后台接口
  - [ ] 测试管理员不能设置超级管理员
  - [ ] 测试超级管理员可以设置任何角色
  - [ ] 测试超级管理员可以访问系统管理功能
- [ ] 手动测试
  - [ ] 测试 @CurrentUser 自动注入
  - [ ] 测试 @RequireRole 注解
  - [ ] 测试 Service 层所有权验证
  - [ ] 测试前端权限判断
  - [ ] 测试路由守卫
  - [ ] 测试错误页面跳转

#### **阶段十四：安全检查** ⏭️ 后续人工检查
- [ ] 验证不能修改自己的角色
- [ ] 验证只有超级管理员可以设置超级管理员
- [ ] 验证审计日志正确记录
- [ ] 验证 Redis 缓存工作正常
- [ ] 验证前后端权限检查双重保护
- [ ] 验证敏感操作有日志记录
- [ ] 代码审查：确保所有 Service 不调用 StpUtil

#### **阶段十五：文档和部署** ⏭️ 后续实施
- [ ] 更新 API 文档（Swagger 注解）
- [ ] 编写权限系统使用文档
- [ ] 更新部署文档（数据库迁移步骤）
- [ ] 准备线上发布计划
- [ ] 备份数据库
- [ ] 执行灰度发布

---

## 十一、安全考虑

### 10.1 防止权限提升
```java
// 修改用户角色接口必须验证操作者权限
@PostMapping("/admin/users/{id}/role")
@SaCheckRole("admin")
public ApiResponse<Void> setUserRole(
    @PathVariable Long userId,
    @RequestParam Integer roleCode
) {
    UserRole newRole = UserRole.fromCode(roleCode);
    Long operatorId = StpUtil.getLoginIdAsLong();
    User operator = userService.getUserById(operatorId);

    // 1. 防止用户修改自己的角色
    if (userId.equals(operatorId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
    }

    // 2. 只有超级管理员可以设置超级管理员
    if (newRole == UserRole.SUPER_ADMIN && !operator.isSuperAdmin()) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以设置超级管理员");
    }

    // 3. 执行角色修改
    userService.setUserRole(userId, newRole);
    return ApiResponse.success(null);
}
```

### 10.2 敏感操作日志
```java
@PostMapping("/admin/users/{id}/ban")
@SaCheckRole("admin")
public ApiResponse<Void> banUser(@PathVariable Long userId) {
    Long adminId = StpUtil.getLoginIdAsLong();
    log.warn("管理员 {} 封禁用户 {}", adminId, userId);

    userService.banUser(userId);
    return ApiResponse.success(null);
}
```

### 10.3 防止越权访问
```java
// 确保资源存在且用户有权限（Service 层验证）
@PutMapping("/{id}")
@SaCheckLogin
public ApiResponse<PostDTO> updatePost(
        @PathVariable Long id,
        @RequestBody UpdatePostRequest request,
        @CurrentUser UserDO currentUser) {
    // Service 层验证所有权
    // 如果资源不存在或无权限，会抛出异常
    return ApiResponse.success(postService.updatePost(id, request, currentUser));
}
```

---

## 十一、问题与讨论

### 11.1 已确定的设计决策

#### 决策1：四个角色体系 ✅
**决定**：四个角色（USER、MODERATOR、ADMIN、SUPER_ADMIN）一起实现
**理由**：
- 完整的权限层级体系，满足不同的管理需求
- USER(0)：普通用户，只能操作自己的内容
- MODERATOR(1)：审核员，可以审核待审核内容
- ADMIN(2)：管理员，可以管理所有内容和用户
- SUPER_ADMIN(3)：超级管理员，拥有系统级别权限
- 枚举方式实现成本低，预留完整的角色体系方便未来扩展

#### 决策2：管理员 Controller 路径 ✅
**决定**：采用混合方案
**具体方案**：
1. **业务 Controller**：保持 RESTful 风格，使用 `@SaCheckLogin` + Service 层验证
   - 例如：`DELETE /api/v1/posts/{id}` - 所有者或管理员都可以删除
   - 管理员通过 Service 层验证自动获得权限

2. **Admin Controller**：集中管理员专属功能，路径为 `/api/v1/admin/**`
   - 只有管理员能访问的功能：
     - 查看所有用户：`GET /api/v1/admin/users`
     - 封禁用户：`POST /api/v1/admin/users/{id}/ban`
     - 查看待审核列表：`GET /api/v1/admin/posts/pending`
     - 系统管理：`POST /api/v1/admin/system/cache/clear`

**接口分配原则**：
- 如果**普通用户对自己的资源也能操作** → 放在业务 Controller + Service 层验证
- 如果**普通用户永远用不到这个功能** → 放在 AdminController

**示例对比**：
```java
// ✅ 业务 Controller - 普通用户也能删除自己的帖子
@DeleteMapping("/api/v1/posts/{id}")
@SaCheckLogin
public void deletePost(
        @PathVariable Long id,
        @CurrentUser UserDO currentUser) {
    postService.deletePost(id, currentUser);  // Service 层验证所有权
}

// ✅ Admin Controller - 只有管理员能看所有用户
@GetMapping("/api/v1/admin/users")
@RequireRole(UserRole.ADMIN)
public List<UserDTO> getAllUsers(@CurrentUser UserDO currentUser) { }
```

#### 决策3：所有权验证方式 ✅
**决定**：在 Service 层验证，不使用 @RequireOwner 注解
**理由**：
- 避免双重查询性能问题（注解查一次，Service 再查一次）
- Service 层独立，可在任何场景调用（Controller、定时任务、MQ）
- 权限逻辑集中在 Service 层，易于理解和维护
- 支持灵活的权限检查逻辑（管理员自动跳过）

**实现示例**：
```java
public void updatePost(Long postId, UpdatePostRequest request, UserDO operator) {
    PostDO post = postDataService.getById(postId);
    if (post == null) {
        throw ErrorCode.POST_NOT_FOUND.exception();
    }

    // 验证权限：只有所有者或管理员可以修改
    if (!post.getCreatorId().equals(operator.getId()) && !operator.isAdmin()) {
        throw ErrorCode.PERMISSION_DENIED.exception();
    }

    post.setTitle(request.getTitle());
    postDataService.update(post);
}
```

#### 决策4：权限缓存策略 ✅
**决定**：使用系统已有的 Redis 缓存机制（基于 Spring Cache + RedisTemplate）
**理由**：
- 系统已有完善的 AbstractDataService 缓存基础设施
- UserDataService 继承缓存能力，自动缓存用户信息（包括 role）
- 使用 Spring Cache 注解（@Cacheable/@CacheEvict），缓存管理自动化
- 用户信息缓存 TTL 为 30 分钟，更新时自动清除缓存
- 性能优异：首次查询 ~10ms（数据库），缓存命中 ~2ms（Redis）
- 无需额外开发，直接复用现有缓存架构

**缓存层级**：
```
┌─────────────────────────────────────────┐
│ Sa-Token Session 缓存                   │
│ - 缓存 Token 和登录状态                 │
│ - 内存级别，极快（<1ms）                │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ Redis 缓存（Spring Cache）              │
│ - 缓存用户完整信息（包括 role）         │
│ - Key: "users::userId"                  │
│ - TTL: 30分钟                           │
│ - 查询速度：~2ms                        │
└────────────────┬────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ 数据库                                  │
│ - 最终数据源                            │
│ - 缓存未命中时查询                      │
│ - 查询速度：~10ms                       │
└─────────────────────────────────────────┘
```

**实现方式**：
```java
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserDataService userDataService;  // 已自带 Redis 缓存

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());

        // getById() 自动使用 @Cacheable，首次查询数据库并缓存
        // 后续查询直接从 Redis 返回，无需查询数据库
        UserDO user = userDataService.getById(userId);

        if (user == null || user.getRole() == null) {
            return Collections.emptyList();
        }

        // 构建角色列表（支持角色继承）
        return buildRoleList(user.getRole());
    }
}
```

**缓存清除**：
```java
// 修改用户角色时，UserDataService.update() 会自动清除缓存
@CacheEvict(value = "users", key = "#user.id")
public void update(UserDO user) {
    userMapper.update(user);  // 更新数据库
    // Spring Cache 自动清除 Redis 缓存
}
```

#### 决策5：前端路由守卫的实现位置 ✅
**决定**：采用全局路由守卫 + 路由元信息（选项B）
**理由**：
- 统一管理，便于维护
- 通过 meta 字段声明权限要求清晰明了
- 支持多级权限检查（requireModerator、requireAdmin、requireSuperAdmin）

#### 决策6：是否需要操作日志表 ✅
**决定**：暂时使用应用日志（选项A），后续可扩展
**理由**：
- 应用日志足够满足当前需求
- 减少初期实现复杂度
- 日志系统可以统一收集和查询
- 未来如需要可以添加专门的审计日志表

#### 决策7：普通用户能否看到"编辑/删除"按钮 ✅
**决定**：前端隐藏按钮（选项A）
**理由**：
- 用户体验更好，不会误导用户
- 减少不必要的请求和错误提示
- 后端仍然做权限验证，保证安全性

#### 决策8：错误码设计 ✅
**决定**：使用 HTTP 标准码（选项A）
**理由**：
- 401 Unauthorized：未登录
- 403 Forbidden：无权限
- 标准化，前端易于处理
- 符合 RESTful API 规范

---

## 十二、参考资料

- [Sa-Token 官方文档](https://sa-token.cc/)
- [Sa-Token 权限认证](https://sa-token.cc/doc.html#/use/jur-auth)
- [Spring AOP 文档](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)

---

## 附录A：完整代码结构

```
backend/
├── learn-common/
│   ├── enums/
│   │   └── UserRole.java          # 角色枚举
│   └── util/
│       └── PermissionUtil.java    # 权限工具类
├── learn-domain/
│   ├── model/
│   │   └── User.java              # 添加 role 字段
│   └── service/
│       └── business/
│           └── UserService.java   # 添加角色管理方法
├── learn-api/
│   ├── application/
│   │   ├── AppConfiguration.java  # 修改拦截器配置
│   │   └── StpInterfaceImpl.java  # Sa-Token 权限接口（可选）
│   ├── aspect/
│   │   └── PermissionAspect.java  # 角色权限验证切面
│   ├── controller/
│   │   ├── PostsController.java   # 添加权限注解
│   │   ├── CommentsController.java
│   │   ├── CoursesController.java
│   │   └── AdminController.java   # 新建管理员接口
│   └── exception/
│       └── GlobalExceptionHandler.java  # 添加权限异常处理
└── learn-persistence/
    └── mapper/
        └── UserMapper.java        # 添加角色查询方法

web-ts/
├── src/
│   ├── types/
│   │   └── user.ts                # UserRole 枚举
│   ├── utils/
│   │   └── permission.ts          # 权限工具函数
│   ├── router/
│   │   └── index.ts               # 添加路由守卫
│   └── components/
│       └── PostCard.vue           # 添加权限判断
```

---

## 附录B：数据库迁移脚本

```sql
-- migration_add_user_role.sql

-- 1. 添加 role 字段
ALTER TABLE `user`
ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0
COMMENT '角色代码: 0=普通用户, 1=审核员, 2=管理员, 3=超级管理员'
AFTER `state`;

-- 2. 创建索引（可选）
CREATE INDEX idx_user_role ON `user`(`role`);

-- 3. 设置初始超级管理员（请修改为你的用户ID）
UPDATE `user` SET `role` = 3 WHERE `id` = 1;

-- 4. 验证
SELECT id, name, role,
  CASE role
    WHEN 0 THEN '普通用户'
    WHEN 1 THEN '审核员'
    WHEN 2 THEN '管理员'
    WHEN 3 THEN '超级管理员'
  END as role_name
FROM `user` WHERE `role` > 0;
```

---

**文档版本**: v2.3
**创建时间**: 2025-10-27
**最后更新**: 2025-10-28
**状态**: 已定稿 ✅

**更新日志**：
- v2.3 (2025-10-28): 添加完整的实施 Checklist（15个阶段，200+项任务）
- v2.2 (2025-10-28): 重构权限检查方案，实现自动注入 UserDO 的声明式编程方式
- v2.1 (2025-10-28): 更新权限缓存策略，反映系统实际使用的 Redis 缓存机制
- v2.0 (2025-10-28): 统一四个角色体系，修正所有不一致问题
- v1.0 (2025-10-27): 初始版本
