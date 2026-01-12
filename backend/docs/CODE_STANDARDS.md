# 后端代码规范

## 1. 时区处理

### ❌ 禁止
```java
LocalDate.now()
LocalDateTime.now()
```

### ✅ 必须使用
```java
TimeZoneUtil.now()           // 获取当前日期
TimeZoneUtil.nowDateTime()   // 获取当前日期时间
```

**原因**: 确保所有时间使用统一时区（America/Los_Angeles），避免跨环境时间不一致。

---

## 2. 批量查询

### ❌ 禁止 N+1 查询
```java
// 错误：循环查询
for (Long id : ids) {
    Entity entity = dataService.getById(id);
    map.put(id, entity);
}
```

### ✅ 使用批量查询
```java
// 正确：批量查询
List<Entity> entities = dataService.getByIds(ids);
Map<Long, Entity> map = entities.stream()
    .collect(Collectors.toMap(Entity::getId, e -> e));
```

**原因**: N+1 查询会严重影响性能，批量查询可减少 99% 数据库调用。

---

## 3. 缓存失效

### ❌ 禁止部分清除
```java
@CacheEvict(value = "cache", key = "#id")
public void update(Long id) {
    // 只清除单个key，可能导致 'all' 缓存不一致
}
```

### ✅ 清除所有相关缓存
```java
@CacheEvict(value = "cache", allEntries = true)
public void update(Long id) {
    // 清除所有相关缓存
}
```

**原因**: 避免缓存不一致，确保数据正确性。

---

## 4. 安全性

### ❌ 禁止硬编码密钥
```java
private String apiKey = "sk-xxx...";  // 严重安全漏洞
```

### ✅ 使用环境变量
```java
@Value("${app.ai.api-key}")
private String apiKey;

// application.yml
app:
  ai:
    api-key: ${AI_API_KEY}  # 从环境变量读取
```

### ❌ 禁止使用 Random
```java
new Random().nextInt()  // 不安全
```

### ✅ 使用 SecureRandom
```java
new SecureRandom().nextInt()  // 密码学安全
```

**原因**: 保护敏感信息，防止安全漏洞。

---

## 5. 分布式锁

### ✅ 使用注解
```java
@DistributedLock(
    key = "'resource:' + #userId + ':' + #resourceId",
    waitTime = 5,
    leaseTime = 30
)
public void criticalOperation(Long userId, Long resourceId) {
    // 业务逻辑自动加锁
}
```

**特性**:
- 支持 SpEL 表达式动态生成 key
- 自动释放锁，防止死锁
- 业务异常也会正确释放锁

---

## 6. Redis 操作

### ❌ 禁止使用 KEYS 命令
```java
Set<String> keys = redisTemplate.keys("prefix:*");  // 会阻塞 Redis
```

### ✅ 使用 SCAN 命令
```java
redisTemplate.execute((RedisCallback<Object>) connection -> {
    ScanOptions options = ScanOptions.scanOptions()
        .match("prefix:*")
        .count(100)
        .build();
    Cursor<byte[]> cursor = connection.scan(options);
    // 处理结果
});
```

**原因**: KEYS 命令会阻塞 Redis，SCAN 命令渐进式扫描不影响性能。

---

## 7. 异常处理

### ✅ 使用明确的业务异常
```java
throw StatusCode.INVALID_PARAMETER.exception("ID不能为空");
throw StatusCode.PERMISSION_DENIED.exception();
throw StatusCode.USER_NOT_FOUND.exception();
```

### ❌ 禁止捕获所有异常后返回 null
```java
try {
    return service.getData(id);
} catch (Exception e) {
    return null;  // 掩盖了真正的错误
}
```

### ✅ 只捕获预期的异常
```java
try {
    return service.getData(id);
} catch (NotLoginException e) {
    return null;  // 未登录是预期场景
}
// 其他异常向上抛出
```

---

## 8. 事务管理

### ✅ 写操作必须加事务
```java
@Transactional
public void createEntity(CreateRequest request) {
    // 多个写操作
    entityDataService.insert(entity);
    statsDataService.increment(entity.getId());
}
```

### ⚠️ 只读操作不需要事务
```java
// 不需要 @Transactional
public EntityDTO getById(Long id) {
    return entityDataService.getById(id);
}
```

---

## 9. 参数验证

### ✅ 多层验证
```java
// Controller 层
public ApiResponse<?> create(@Valid @RequestBody CreateRequest request) { }

// Service 层
public void create(CreateRequest request) {
    ValidationUtils.requirePositiveId(request.getId());
    ValidationUtils.requireNonBlank(request.getName(), "名称");
}
```

---

## 10. 日志记录

### ✅ 正确使用日志级别
```java
log.debug("查询用户: userId={}", userId);           // 调试信息
log.info("创建课程成功: courseId={}", courseId);    // 重要操作
log.warn("缓存获取失败，降级到数据库", e);           // 警告
log.error("数据库操作失败", e);                     // 错误
```

### ❌ 禁止
```java
e.printStackTrace();           // 禁止使用
System.out.println("...");    // 禁止使用
```

---

## 11. 魔法数字

### ❌ 禁止魔法数字
```java
if (status == 1) { }           // 1 是什么意思？
switch (action) {
    case 1: doChoose(); break;  // 1 代表什么？
}
```

### ✅ 使用枚举
```java
if (status == Status.ACTIVE.value()) { }

ContentAction action = ContentAction.getByValue(request.getAction());
switch (action) {
    case CHOOSE: doChoose(); break;
    case UNCHOOSE: doUnchoose(); break;
}
```

---

## 12. 命名规范

### Java
```java
public class UserService { }           // 类名: PascalCase
public void getUserById() { }          // 方法: camelCase
private String userName;               // 变量: camelCase
public static final int MAX_SIZE = 100; // 常量: UPPER_SNAKE_CASE
```

### 数据库
```sql
CREATE TABLE user_course (             -- 表名: snake_case
    user_id BIGINT,                    -- 字段: snake_case
    created_at TIMESTAMP
);
```

---

## 13. 注释规范

### ✅ 有价值的注释
```java
/**
 * 使用批量查询避免 N+1 问题
 */
List<Entity> entities = service.batchGet(ids);

// 确保统计记录存在再更新
statsService.getOrCreate(id);
```

### ❌ 无价值的注释
```java
// 获取用户
User user = getUser();  // 代码已经自解释

// TODO 待实现        // 没有说明具体任务
```

---

## 14. 代码清理

### ❌ 禁止提交注释代码
```java
// public void oldMethod() {
//     // 大段注释代码
// }
```

**原因**: Git 可以恢复历史代码，注释代码影响可读性。

---

## 15. 性能优化

### ✅ 使用字段缓存
```java
// 反射操作缓存 Field 对象
private final Map<Class<?>, Map<String, Field>> fieldCache = new ConcurrentHashMap<>();
```

### ✅ 批量操作
```java
// 使用 Redis MGET 批量获取
List<Object> values = redisTemplate.opsForValue().multiGet(keys);
```

---

## 检查清单

提交代码前检查：

- [ ] 没有使用 `LocalDate.now()` 或 `LocalDateTime.now()`
- [ ] 没有 N+1 查询（循环调用单个查询）
- [ ] 没有硬编码密钥或密码
- [ ] 写操作有 `@Transactional` 注解
- [ ] 有适当的日志记录
- [ ] 没有魔法数字，使用了枚举
- [ ] 删除了所有注释代码
- [ ] 没有使用 `Random`，安全场景使用 `SecureRandom`
- [ ] Redis 操作没有使用 `KEYS` 命令
- [ ] 异常处理明确，不掩盖错误

---

**最后更新**: 2026-01-11
**适用范围**: learn-* 所有后端模块
