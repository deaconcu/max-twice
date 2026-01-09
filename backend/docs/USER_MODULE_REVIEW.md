# 用户模块代码审查报告

## 一、已修复的问题

### 🔴 严重问题

#### 密码验证被临时禁用 ✅ 已修复
**位置**: UserDomainService.java:247-250
**修复状态**: 已添加BCrypt密码加密，验证逻辑已就绪，仅测试环境临时禁用
**问题**: 登录时没有验证密码，任何人只要知道邮箱就能登录
**影响**: 严重安全漏洞
**建议**: 生产环境必须启用密码验证

#### 密码加密 ✅ 已修复
**修复状态**: 已从MD5迁移到BCrypt
- 使用 `BCryptPasswordEncoder`
- 密码验证使用 `passwordEncoder.matches()`

#### 验证码type字段 ✅ 已修复
**修复状态**: 已添加type字段到VerificationDO和VerificationMapper
- 添加了 `VerificationType` 枚举: REGISTER(1), RESET_PASSWORD(2), CHANGE_EMAIL(3)
- 更新了查询逻辑按type查询
- 修复了INSERT语句，包含created_at字段

#### 验证码发送间隔限制 ✅ 已修复
**修复状态**: 已在 `UserDomainService.createVerificationCode()` 中添加60秒间隔检查
- 检查最近未使用的验证码
- 如果60秒内已发送，抛出 `USER_VERIFICATION_CODE_SEND_TOO_FREQUENT` 异常
- 可通过配置 `app.user.verification-code-send-interval-seconds` 调整

---

### 🟡 中等问题

#### UserProfileDO表时间戳字段 ✅ 已修复
**修复状态**: 已添加 created_at 和 updated_at 字段
- UserProfileDO 添加了 `createdAt` 和 `updatedAt` 字段
- UserProfileMapper 的 INSERT 和 UPDATE 语句包含时间戳字段
- UserProfileDataService.updateRoadmapPin() 改为先查询再更新，利用 TimestampInterceptor 自动填充
- 所有时间使用 Java 时间（LocalDateTime.now()），保证时区一致性

#### updateUserInfo参数null处理 ✅ 已修复
**修复状态**: 已添加null值检查和转换
- 对 name 和 biography 参数进行 null 检查
- 如果为 null，设置为空字符串（数据库字段 NOT NULL）
- Controller 层的 `@NotBlank` 和 `@ConfigurableSize` 已提供长度校验
- 防御性编程，避免数据库错误

#### UserMapper.update敏感字段更新 ✅ 已修复
**修复状态**: 已分离敏感字段更新方法
- `update()` 方法只更新基本信息字段（name, phone, biography, avatar, msg_read_time）
- 不再更新敏感字段（password, email, email_validated, state, role）
- 为敏感字段创建了专用更新方法：
  - `updateState()` - 更新用户状态
  - `updateRole()` - 更新用户角色
  - `updateEmailValidated()` - 更新邮箱验证状态
- 所有更新方法统一使用 Java 时间（LocalDateTime.now()）
- 避免意外修改敏感字段的风险

#### UserProfileMapper相关问题 ✅ 已修复
**修复状态**: 参数名和@Options注解问题已修复
- `getById(long userId)` - 参数名已从 `id` 改为 `userId`，与SQL占位符 `#{userId}` 一致
- `insert()` - 移除了错误的 `@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")`
- 原因：user_profile 表主键是 `user_id`（非自增），不是 `id`

---

### 🟢 轻微问题/优化建议

#### validateCourseExists重复验证逻辑 ✅ 已修复
**修复状态**: 已删除重复方法，使用 DataService 的标准验证
- 删除了 `validateCourseExists()` 方法
- 在 `subscribe()` 和 `unsubscribe()` 中直接调用 `courseDataService.validateAndGet(courseId)`
- 避免在 ApplicationService 层重复实现验证逻辑
- 保持代码一致性和可维护性

#### UserDataService.update缓存清除 ✅ 已优化
**优化状态**: 简化了缓存清除逻辑
- 改为使用 `@CacheEvict(value = {"users", "usersByEmail", "usersByName"}, allEntries = true)`
- 移除了查询旧用户数据的额外数据库查询
- 直接清除所有用户相关缓存，简单高效

---

### 🔵 设计说明（非问题）

#### 验证码批量查询
**说明**: VerificationDataService 的 `getByIds()` 抛出 `UnsupportedOperationException`
**分析**: 这是**设计决策**，不是缺陷
- 验证码业务不需要批量查询（按 email+type 单个查询）
- 如果误调用会立即抛异常，不会静默失败
- 保持现状即可

#### 用户名生成策略
**位置**: UserDomainService.java:144
```java
user.setName("MT_" + generateRandomBase62(8));
```
**说明**: 使用随机Base62字符串生成默认用户名（如"MT_a3Bx9Kp2"）
**设计优势**:
- 保证唯一性（Base62的8位，62^8种组合）
- 无特殊字符（只有0-9a-zA-Z）
- 长度固定（11字符）
- 避免邮箱前缀的问题（特殊字符、中文、重复冲突）
- 保护用户隐私
- 用户可后续自行修改

#### subscription存储设计
**位置**: UserProfileDO.subscription 字段
**当前实现**: 逗号分隔字符串（如 "1,2,3,4,5"）
**设计分析**: 这是**合理的设计决策**
- 订阅上限100个，存储空间约 600字节 - 1KB
- 一次查询获取用户所有订阅，性能最优
- 解析简单高效：`Arrays.stream(str.split(",")).map(Long::parseLong)`
- 逗号分隔 vs JSON：对于纯ID列表，逗号分隔更简洁（无需括号等额外字符）
- 查询"哪些用户订阅了某课程"确实不便，但这是低频场景，可通过其他方式解决
- 创建独立表会增加 JOIN 开销，得不偿失

#### roadmap_pin存储设计
**位置**: UserProfileDO.roadmapPin 字段
**当前实现**: JSON格式存储（如 `{"1":[10,11,12],"2":[20,21]}`）
**设计分析**: 使用JSON是合理的
- 需要按职业分组（Map<professionId, List<roadmapId>>）
- JSON是标准的结构化数据格式
- 每个职业最多19个置顶路线图
**优化建议**:
- 常量 `MAX_PINNED_ROADMAPS` 改名为 `MAX_PINNED_ROADMAPS_PER_PROFESSION` 更清晰

---

## 二、未修复的问题和建议

### 🟡 业务逻辑优化

#### 订阅重复校验配置 ✅ 已修复
**修复状态**: 已删除不必要的配置项和参数
- 删除了配置项 `enableDuplicateSubscriptionCheck`
- 移除了 `addSubscription()` 的 `checkDuplicate` 参数
- 现在**始终检查**重复订阅，避免数据错误
- 简化了代码逻辑

---

### 🔵 代码质量说明

#### DomainService层参数校验
**说明**: 当前实现已符合最佳实践
- Controller层负责格式校验（@Valid、@NotBlank等）
- ApplicationService层负责详细的格式和业务校验
- DomainService层负责核心业务规则校验
- 示例：`setUserRole()` 已正确实现业务规则校验（不能修改自己的角色、超级管理员权限检查）
- 不需要在DomainService重复格式校验，避免冗余

---

## 三、安全性检查

### ✅ 已实现的安全措施
1. SQL注入防护 - 使用MyBatis参数绑定
2. 角色权限校验 - @RequireRole注解 + AOP切面
3. 接口限流 - Bucket4j + Redis
4. Token管理 - SaToken，7天有效期
5. 验证码一次性使用 - used字段标记
6. 验证码有效期 - 10分钟（配置）
7. 验证码发送间隔 - 60秒限制（已修复）
8. 密码加密 - BCrypt（已修复）
9. 密码强度校验 - 最少8位且包含字母和数字（已实现）

### 💡 可选的安全增强
1. **双因素认证（2FA）** - 可考虑添加
2. **异常登录检测** - 新设备/新地点提醒
3. **账号锁定机制** - 多次登录失败后临时锁定
