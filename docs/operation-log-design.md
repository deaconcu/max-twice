# 操作日志系统设计文档

## 1. 概述

操作日志系统用于记录管理员对用户内容的关键操作，便于审计、追溯和纠纷处理。

### 1.1 设计原则

- **只记录跨越权限边界的操作**：管理员对其他用户内容的操作
- **只记录不可逆的关键操作**：删除、屏蔽、审核等
- **不记录查询操作**：查询操作使用访问日志
- **不记录用户自助操作**：用户修改自己的内容不记录（数据表本身就是记录）

### 1.2 记录范围

#### 必须记录的操作

1. **用户管理**
   - 修改用户角色（普通用户 ↔ 版主 ↔ 管理员）
   - 封禁/解封用户
   - 删除用户账号

2. **内容管理**
   - 审核通过/拒绝（帖子、课程、职业、路线图、记忆卡片组）
   - 屏蔽内容（帖子、评论、课程等）
   - 删除内容（帖子、评论、课程、节点等）
   - 恢复被屏蔽的内容

3. **系统配置**
   - 修改系统配置（如只读模式）
   - 修改分类配置

#### 不记录的操作

- 查询列表、搜索（使用访问日志）
- 用户自己创建/修改内容（数据表本身已记录）
- 用户日常互动（点赞、收藏、关注等有专门的数据表）

## 2. 数据库设计

### 2.1 操作日志表 (operation_log)

```sql
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) NOT NULL COMMENT '操作人名称（冗余字段，避免用户改名后无法追溯）',
  `operator_role` TINYINT NOT NULL COMMENT '操作人角色（0=普通用户, 1=审核员, 2=管理员, 3=超级管理员）',

  `module` VARCHAR(50) NOT NULL COMMENT '模块名称（用户管理、内容管理、系统配置等）',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型（封禁用户、删除帖子、审核通过等）',
  `operation_level` TINYINT NOT NULL DEFAULT 2 COMMENT '操作级别（1=低, 2=中, 3=高）',

  `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型（User, Post, Course, Comment, SystemConfig等）',
  `target_id` BIGINT NOT NULL COMMENT '目标ID（SystemConfig类型时为0）',
  `target_name` VARCHAR(255) COMMENT '目标名称（冗余字段，便于查看）',

  `reason` VARCHAR(500) COMMENT '操作原因（如拒绝理由、屏蔽原因、封禁原因）',
  `extra_data` JSON COMMENT '额外数据（如修改前后的值、详细参数等）',

  `ip_address` VARCHAR(45) COMMENT '操作IP地址',

  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

  PRIMARY KEY (`id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_module_type` (`module`, `operation_type`),
  KEY `idx_operation_level` (`operation_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

### 2.2 字段说明

- **operator_id/operator_name/operator_role**：记录操作人信息，name 和 role 冗余存储避免后续变更无法追溯
  - operator_role 值：0=普通用户, 1=审核员, 2=管理员, 3=超级管理员（对应 UserRole 枚举的 code）
- **module**：模块分类（用户管理、内容管理、系统配置）
- **operation_type**：具体操作类型（封禁用户、删除帖子等），使用 VARCHAR(50) 保证可读性和扩展性
- **operation_level**：操作风险级别
  - 低(1)：审核通过、恢复内容
  - 中(2)：审核拒绝、临时屏蔽（默认值）
  - 高(3)：删除、封禁、修改角色
- **target_type/target_id/target_name**：被操作的目标对象
  - target_type：使用 VARCHAR(50)，支持实体对象（User, Post, Course, Comment 等）和非实体对象（SystemConfig）
  - target_id：目标对象的ID，SystemConfig 类型时为 0
  - target_name：冗余字段，便于直接查看
- **reason**：操作原因（拒绝理由、屏蔽原因、封禁原因），某些操作必须填写（如拒绝、删除、封禁）
- **extra_data**：JSON 格式存储额外信息（如修改前后的值、详细参数等）
- **ip_address**：操作者的 IP 地址（注意反向代理的 X-Forwarded-For）

### 2.3 设计决策说明

#### 为什么删除 user_agent 字段？
- **价值有限**：操作日志主要用于审计和追溯，user_agent 信息使用频率极低
- **占用空间大**：VARCHAR(500) 字段占用较多空间
- **IP 地址已足够**：如需追溯操作来源，IP 地址已经够用
- **访问日志已记录**：如需检测自动化脚本等，应该在访问日志层面处理

#### 为什么 target_type 使用 VARCHAR 而不是 TINYINT？
- **ContentType 枚举的局限性**：ContentType 定义的是系统中的实体对象（User, Post, Course 等）
- **非实体目标的需求**：操作日志需要记录对系统配置（如只读模式）的操作，这些不是实体对象
- **扩展性考虑**：未来可能有更多非实体的操作目标
- **可读性优先**：操作日志是审计表，可读性比节省空间更重要
- **性能影响可接受**：操作日志不是高频查询表，VARCHAR(50) 的性能影响可接受

#### operator_role 与 UserRole 枚举的对应关系
```java
public enum UserRole {
    USER(0, "user", "普通用户", 0),
    MODERATOR(1, "moderator", "审核员", 30),
    ADMIN(2, "admin", "管理员", 60),
    SUPER_ADMIN(3, "super_admin", "超级管理员", 100);
}
```
操作日志中 operator_role 存储的是 UserRole.getCode()，即：
- 0 = 普通用户 (USER)
- 1 = 审核员 (MODERATOR)
- 2 = 管理员 (ADMIN)
- 3 = 超级管理员 (SUPER_ADMIN)

## 3. 实现方案

### 3.1 技术选型

- **AOP + 自定义注解**：使用 Spring AOP 拦截带注解的方法
- **异步记录**：使用 `@Async` 避免影响主业务性能
- **JSON 序列化**：使用 Jackson 序列化额外数据

### 3.2 核心组件

#### 3.2.1 操作日志注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    /** 模块名称 */
    String module();

    /** 操作类型 */
    String type();

    /** 操作级别 */
    OperationLevel level() default OperationLevel.MEDIUM;

    /** 目标类型（如 "User", "Post", "Course"） */
    String targetType();

    /** 目标ID参数名（SpEL表达式，如 "#userId", "#id"） */
    String targetId();

    /** 目标名称参数名（可选，SpEL表达式） */
    String targetName() default "";

    /** 原因参数名（可选，SpEL表达式，如 "#reason"） */
    String reason() default "";
}
```

#### 3.2.2 操作级别枚举

```java
public enum OperationLevel {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int code;
    private final String description;
}
```

#### 3.2.3 AOP 切面

```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 1. 获取当前操作人信息
        // 2. 解析 SpEL 表达式获取目标ID、名称、原因等
        // 3. 执行原方法
        // 4. 异步记录操作日志
        // 5. 返回结果
    }
}
```

#### 3.2.4 操作日志服务

```java
@Service
public class OperationLogService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    /**
     * 异步记录操作日志
     */
    @Async
    public void recordLog(OperationLogDTO logDTO) {
        // 保存到数据库
    }

    /**
     * 查询操作日志（管理后台）
     */
    public List<OperationLogVO> queryLogs(OperationLogQueryDTO query) {
        // 按条件查询
    }
}
```

### 3.3 使用示例

```java
@PostMapping("/users/{id}/ban")
@RequireRole(UserRole.ADMIN)
@OperationLog(
    module = "用户管理",
    type = "封禁用户",
    level = OperationLevel.HIGH,
    targetType = "User",
    targetId = "#id",
    reason = "#ban ? '封禁' : '解封'"
)
public ApiResponse<UserDTO> banUser(
        @PathVariable Long id,
        @RequestParam Boolean ban,
        @CurrentUser UserDO currentUser) {
    // 业务逻辑
}
```

## 4. 前端展示

### 4.1 操作日志列表页面

**路径**：`/admin/operation-logs`

**功能**：
- 按模块筛选（用户管理、内容管理、系统配置）
- 按操作类型筛选
- 按操作人筛选
- 按时间范围筛选
- 按操作级别筛选
- 支持导出

**列表字段**：
- 操作时间
- 操作人
- 操作模块
- 操作类型
- 目标对象
- 操作原因
- 操作级别（高亮显示高风险操作）

### 4.2 操作日志详情弹窗

**展示内容**：
- 完整的操作信息
- 额外数据（JSON 格式化展示）
- IP 地址

## 5. 性能优化

### 5.1 异步写入

- 使用 `@Async` 异步记录日志，不阻塞主业务
- 配置独立的线程池，避免与主业务竞争资源

### 5.2 批量写入

- 如果日志量大，可以使用消息队列（RabbitMQ/Kafka）批量写入
- 当前系统规模可以直接异步写入数据库

### 5.3 索引优化

- 为常用查询字段建立索引：
  - `operator_id`：按操作人查询
  - `target_type + target_id`：按目标对象查询
  - `created_at`：按时间范围查询
  - `module + operation_type`：按模块和类型查询

### 5.4 数据归档

- 操作日志保留 1 年
- 超过 1 年的日志归档到历史表或文件存储
- 高风险操作日志永久保留

## 6. 安全考虑

### 6.1 权限控制

- 只有管理员可以查看所有操作日志
- 版主只能查看自己的操作日志
- 操作日志不可修改、不可删除（除非系统管理员）

### 6.2 敏感信息脱敏

- IP 地址脱敏（如只显示前 3 段）
- 如果涉及密码、token 等敏感信息，不记录到 extra_data

## 7. 监控告警

### 7.1 异常监控

- 监控高风险操作频率（如短时间内大量删除操作）
- 监控异常操作模式（如凌晨大量操作）

### 7.2 告警机制

- 单个管理员在 1 小时内执行超过 50 次高风险操作 → 告警
- 删除操作超过某个阈值 → 告警

## 8. 实施计划

详见 `operation-log-checklist.md`
