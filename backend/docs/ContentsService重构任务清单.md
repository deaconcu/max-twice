# ContentsService 重构任务清单

## 概述

对 `ContentsService` 进行重构，提高代码质量、可维护性和健壮性。

## 重构类型分类

### 1. 配置外部化重构
**目标**：将硬编码常量提取到配置文件
- ✅ **已完成**：将 `ContentsProperties` 重构为 `SystemProperties` 统一配置类
- ✅ **已完成**：在 `application.yml` 中添加配置项

**配置设计原则**：
- **可变配置**：放入配置文件，如业务规则限制、环境相关参数
- **不变常量**：写成 static final 常量，如固定的键名、标识符

**配置重构详情**：

**步骤1：识别配置类型**
```java
// 可变配置（放入配置文件）
private int maxPinnedItems = 10;           // 业务规则，可能调整
private int maxHotCoursesLimit = 100;      // 性能限制，可能调整

// 不变常量（写成 static final）
private static final String HOT_COURSES_KEY = "course:hot:ranking";     // Redis键名固定
private static final String SUBSCRIPTION_PREFIX = "course:subscription:"; // 键前缀固定
```

**步骤2：创建统一配置类**
```java
@ConfigurationProperties(prefix = "app")
public class SystemProperties {
    private Contents contents = new Contents();
    private CourseRanking courseRanking = new CourseRanking();
    
    @Data
    public static class Contents {
        private int maxPinnedItems = 10;    // 可变：业务规则
        private String pinField = "^";      // 可变：可能改标识符
        private String chosenField = "+";   // 可变：可能改标识符
    }
    
    @Data 
    public static class CourseRanking {
        private int defaultHotCoursesLimit = 20;  // 可变：默认值可调整
        private int maxHotCoursesLimit = 100;     // 可变：限制值可调整
        private int clearBatchSize = 1000;        // 可变：性能参数可调整
    }
}
```

**步骤3：服务类中的配置使用**
```java
@Service
public class CourseRankingService {
    private final SystemProperties systemProperties;
    
    // 不变常量 - 写成 static final
    private static final String HOT_COURSES_KEY = "course:hot:ranking";
    private static final String COURSE_SUBSCRIPTION_PREFIX = "course:subscription:";
    private static final String COURSE_LEARNING_PREFIX = "course:learning:";
    
    // 可变配置 - 从配置文件读取
    private void validateLimit(int limit) {
        if (limit > systemProperties.getCourseRanking().getMaxHotCoursesLimit()) {
            throw ErrorCode.COURSE_RANKING_INVALID_LIMIT.exception();
        }
    }
}
```

**步骤4：配置文件**
```yaml
app:
  contents:
    # 最大置顶帖子数量（业务规则，可能调整）
    max-pinned-items: 10
    # 置顶字段标识（可能更改标识符）
    pin-field: "^"
    # 选中字段标识（可能更改标识符）
    chosen-field: "+"
  course-ranking:
    # 默认热门课程列表大小（默认值可调整）
    default-hot-courses-limit: 20
    # 最大热门课程列表大小（限制值可调整）
    max-hot-courses-limit: 100
    # 统计数据清理时的批量大小（性能参数可调整）
    clear-batch-size: 1000
```

### 2. 错误处理标准化重构
**目标**：统一使用 ErrorCode 枚举管理异常
- ✅ **已完成**：在 ErrorCode 中添加内容管理相关错误码
- 🔄 **进行中**：替换所有 RuntimeException 为 ErrorCode.exception()

**错误码设计**：
```java
// 通用业务相关 B00xx
JSON_PROCESSING_ERROR("B0003", "JSON处理异常"),

// 内容管理相关 B06xx
CONTENTS_COURSE_NOT_FOUND("B0601", "课程不存在"),
CONTENTS_POST_NOT_FOUND("B0602", "帖子不存在"), 
CONTENTS_USER_TOC_NOT_FOUND("B0603", "用户目录不存在"),
CONTENTS_TOC_INDEX_OUT_OF_BOUNDS("B0604", "目录索引超出范围"),
CONTENTS_PINNED_ITEMS_LIMIT_EXCEEDED("B0605", "置顶帖子数量超限"),
CONTENTS_INVALID_POST_TYPE("B0606", "无效的帖子类型"),
```

### 3. 代码重复消除重构
**目标**：提取公共方法，减少重复代码

**重复模式识别**：
1. **课程验证模式**（出现3次）
   ```java
   CourseDO courseDO = courseMapper.getById(courseId);
   if (courseDO == null) {
       throw new RuntimeException("course is not exist");
   }
   ```

2. **用户目录获取模式**（出现3次）
   ```java
   UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(userId, courseId);
   if (userCourseTocDO == null) throw new RuntimeException("user toc is not exist");
   ```

3. **目录索引验证模式**（出现3次）
   ```java
   if (tocIndex > tocHashArr.length) throw new RuntimeException("toc index out of index");
   ```

4. **目录更新流程模式**（出现3次）
   ```java
   // 获取并减少引用计数
   CourseTocDO nodeTocDO = courseTocMapper.get(tocHashArr[tocIndex - 1]);
   courseTocMapper.incrRef(nodeTocDO.getHash(), -1);
   
   // 更新目录内容
   String toc = updateContents(...);
   
   // 保存新目录并增加引用计数
   String hash = Utils.hashSHA(toc);
   if (courseTocMapper.get(hash) == null) courseTocMapper.insert(new CourseTocDO(hash, toc));
   courseTocMapper.incrRef(hash, 1);
   
   // 更新用户目录
   tocHashArr[tocIndex - 1] = hash;
   userCourseTocDO.setToc(String.join(",", tocHashArr));
   userCourseTocMapper.update(userCourseTocDO);
   ```

**提取的公共方法**：
```java
// 验证方法
private CourseDO validateCourseExists(long courseId)
private PostDO validatePostForContents(long postId)  // 注意：文章类型返回null而非抛异常
private UserCourseTocDO validateUserTocExists(long userId, long courseId)
private void validateTocIndex(int tocIndex, String[] tocHashArr)

// 目录操作方法
private String[] parseTocPath(String path)
private String getCurrentTocContent(String[] tocHashArr, int tocIndex)
private void saveUpdatedToc(String toc, String[] tocHashArr, int tocIndex, UserCourseTocDO userCourseTocDO)
```

### 4. 业务逻辑优化重构
**目标**：改进业务处理逻辑

**问题点**：
1. **getToc 方法重载冗余**
   - `getToc(userId, courseId, create)` 返回 ArrayNode
   - `getToc(userId, courseId, tocIndex)` 返回 String
   - 存在大量重复代码

2. **帖子类型检查逻辑不一致**
   - 原始代码：`postDO.getType() == Enums.PostType.article.value()` 时 `return`（静默跳过）
   - 重构需保持这种语义，不应抛异常

3. **JSON 处理异常处理不统一**
   - 多处 try-catch 但异常信息不明确

### 5. 输入验证增强重构
**目标**：添加参数验证，提高健壮性

**需要验证的参数**：
```java
// 路径格式验证
private void validatePathFormat(String path)

// 用户ID验证  
private void validateUserId(long userId)

// 帖子ID验证
private void validatePostId(long postId)

// 课程ID验证
private void validateCourseId(long courseId)
```

### 6. 性能优化重构
**目标**：优化数据库操作和内存使用

**优化点**：
1. **批量查询优化**：第74行 `courseTocMapper.getByHashes(tocHashArr)` 后应检查返回完整性
2. **空指针安全**：第254-257行存在潜在空指针风险
3. **JSON 处理优化**：减少重复的 JSON 解析操作

## 重构执行计划

### 阶段1：基础重构（已完成）
- ✅ 配置外部化
- ✅ ErrorCode 添加

### 阶段2：核心重构（当前）
- 🔄 提取公共验证方法
- 📋 替换异常处理
- 📋 消除代码重复

### 阶段3：业务逻辑优化
- 📋 重构 getToc 方法
- 📋 统一 JSON 异常处理
- 📋 优化帖子类型检查逻辑

### 阶段4：健壮性增强
- 📋 添加输入验证
- 📋 添加空值检查
- 📋 优化错误消息

### 阶段5：性能优化
- 📋 优化数据库查询
- 📋 减少 JSON 解析次数
- 📋 添加必要的缓存

## 注意事项

### 业务语义保持
1. **文章类型帖子处理**：必须保持静默跳过的语义，不能改为抛异常
2. **目录创建逻辑**：`getToc` 的 `create` 参数语义要保持
3. **置顶数量限制**：使用配置而非硬编码

### 向后兼容性
1. **公共方法签名**：不改变现有公共方法的签名
2. **事务边界**：保持现有事务注解的作用范围
3. **返回值类型**：保持现有返回值类型不变

### 代码质量标准
1. **异常处理**：统一使用 ErrorCode
2. **日志记录**：添加关键操作的日志
3. **方法职责**：每个方法职责单一
4. **命名规范**：方法名清晰表达意图

## 图例
- ✅ 已完成
- 🔄 进行中  
- 📋 待开始

## DailyStatsService重构示例

基于ContentsService重构模式，对DailyStatsService进行同样的重构。以下是重构示例：

### 重构前的问题识别

**重复代码模式**：
1. **Redis键名生成**（出现多次）
   ```java
   String userKey = "stats:" + dateStr + ":user";
   String postKey = "stats:" + dateStr + ":post";
   ```

2. **日期键生成**（出现多次）
   ```java
   String dayKey = date.getMonthValue() + "-" + date.getDayOfMonth();
   ```

3. **异常处理模式**（出现多次）
   ```java
   try {
       // 业务逻辑
   } catch (Exception e) {
       log.error("操作失败", e);
       return defaultValue; // 静默返回默认值
   }
   ```

### 重构步骤示例

**步骤1：添加配置和错误码**
```java
// SystemProperties.java 新增Stats配置
@Data
public static class Stats {
    private String systemStartDate = "2020-01-01";
    private int maxQueryDaysRange = 365;
    private int maxQueryYearRange = 5;
    private boolean enableStatsCache = true;
    private int cacheTtlMinutes = 60;
}

// ErrorCode.java 复用现有错误码
USER_NOT_FOUND("A0003", "user.not.found"),
CONTENTS_POST_NOT_FOUND("B0602", "帖子不存在"),
INVALID_DATE("B0004", "无效的日期"),
INVALID_DAYS_RANGE("B0005", "无效的天数范围"),
```

**步骤2：注入依赖并添加常量**
```java
@Service
@RequiredArgsConstructor  // 替换@Autowired
public class DailyStatsService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SystemProperties systemProperties; // 新增配置注入
    
    // 提取常量
    private static final String STATS_KEY_PREFIX = "stats:";
    private static final String USER_STATS_SUFFIX = ":user";
    private static final String POST_STATS_SUFFIX = ":post";
}
```

**步骤3：提取公共方法**
```java
// ========== 私有辅助方法 ==========

/**
 * 生成用户统计Redis键名
 */
private String generateUserStatsKey(String dateStr) {
    return STATS_KEY_PREFIX + dateStr + USER_STATS_SUFFIX;
}

/**
 * 生成文章统计Redis键名  
 */
private String generatePostStatsKey(String dateStr) {
    return STATS_KEY_PREFIX + dateStr + POST_STATS_SUFFIX;
}

/**
 * 生成日期键（月-日格式）
 */
private String generateDayKey(LocalDate date) {
    return date.getMonthValue() + "-" + date.getDayOfMonth();
}

/**
 * 验证用户ID有效性
 */
private void validateUserId(long userId) {
    if (userId <= 0) {
        throw ErrorCode.USER_NOT_FOUND.exception();
    }
}

/**
 * 验证日期有效性
 */
private void validateDate(LocalDate date) {
    if (date == null) {
        throw ErrorCode.INVALID_DATE.exception();
    }
    LocalDate systemStart = LocalDate.parse(systemProperties.getStats().getSystemStartDate());
    if (date.isBefore(systemStart) || date.isAfter(LocalDate.now())) {
        throw ErrorCode.INVALID_DATE.exception();
    }
}
```

**步骤4：应用重构方法**
```java
// 重构前
public String syncSpecificDate(LocalDate date) {
    // 检查Redis中是否有这个日期的数据
    String userKey = "stats:" + dateStr + ":user";    // 硬编码重复
    String postKey = "stats:" + dateStr + ":post";    // 硬编码重复
    
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("同步失败", e);  // 通用异常处理
        return "失败消息";         // 静默返回
    }
}

// 重构后  
public String syncSpecificDate(LocalDate date) {
    validateDate(date);  // 添加参数验证
    
    String dateStr = date.toString();
    String userKey = generateUserStatsKey(dateStr);  // 使用公共方法
    String postKey = generatePostStatsKey(dateStr);  // 使用公共方法
    
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("同步{}的数据失败", dateStr, e);
        throw ErrorCode.SYSTEM_ERROR.exception(e);  // 抛出具体异常
    }
}
```

### 重构效果对比

| 重构项 | 重构前 | 重构后 |
|--------|--------|--------|
| 硬编码常量 | `"stats:" + dateStr + ":user"` | `generateUserStatsKey(dateStr)` |
| 参数验证 | 无验证或分散验证 | 统一的`validateXxx()`方法 |
| 异常处理 | `catch Exception` 返回默认值 | 使用`ErrorCode.exception()`抛出具体异常 |
| 配置管理 | 硬编码`"2020-01-01"` | `systemProperties.getStats().getSystemStartDate()` |
| 依赖注入 | `@Autowired` | `@RequiredArgsConstructor` + `final` |

### 重构原则总结

1. **可变配置外部化**：业务规则、限制值放入配置文件
2. **不变常量内部化**：固定的键名、标识符定义为static final常量  
3. **重复代码提取**：3次以上重复的代码提取为公共方法
4. **异常处理标准化**：使用ErrorCode统一管理业务异常
5. **参数验证前置**：在方法开头进行参数有效性验证