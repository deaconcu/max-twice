# Memory 模块代码审查报告

## 1. 总体评价

learn-memory 模块负责处理记忆卡片和间隔重复学习（SRS）功能，是一个复杂度较高的核心业务模块。整体架构设计优秀，代码质量较高，采用了领域驱动设计（DDD）思想，层次清晰，职责分明。模块实现了完整的 Anki 算法，支持卡片版本管理、记忆库管理等高级功能。

### 模块结构
```
learn-memory/
├── card/              # 记忆卡片子模块
│   ├── MemoryCardDomainService.java
│   ├── MemoryCardDataService.java
│   ├── MemoryCardMapper.java
│   ├── MemoryCardDO.java
│   ├── MemoryCardVersionDomainService.java (无独立服务)
│   ├── MemoryCardVersionDataService.java
│   ├── MemoryCardVersionMapper.java
│   └── MemoryCardVersionDO.java
├── deck/              # 卡片组子模块
│   ├── MemoryCardDeckDomainService.java
│   ├── MemoryCardDeckDataService.java
│   ├── MemoryCardDeckMapper.java
│   └── MemoryCardDeckDO.java
├── bank/              # 记忆库子模块
│   └── MemoryBankDomainService.java
└── review/            # 复习子模块（SRS算法）
    ├── ReviewDomainService.java
    ├── UserCardSrsDataService.java
    ├── UserCardSrsMapper.java
    ├── UserCardSrsDO.java
    ├── UserCardInCourseDataService.java
    ├── UserCardInCourseMapper.java
    ├── UserCardInCourseDO.java
    ├── UserCourseSrsSettingDataService.java
    ├── UserCourseSrsSettingMapper.java
    ├── UserCourseSrsSettingDO.java
    └── CourseMemoryBankDO.java (统计DO)
```

### 代码分层
- **Controller 层**: 4个控制器（MemoryBankController, MemoryCardController, MemoryCardDeckController, ReviewController）
- **DomainService 层**: 4个领域服务（业务逻辑处理，带事务管理）
- **DataService 层**: 7个数据服务（数据访问服务，提供缓存功能）
- **Mapper 层**: 7个MyBatis映射器（SQL操作）

---

## 2. 已发现的问题

### P0 - 严重问题（无）

经过全面审查，未发现 P0 级别的严重问题。所有 SQL 语法正确，参数类型使用一致（原始类型），缓存策略合理。

---

### P1 - 重要问题（建议修复）

#### 2.1 MemoryCardDeckDataService.decrementUpvoteCount() 方法返回 false

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/deck/MemoryCardDeckDataService.java:450-453`

```java
@CacheEvict(value = "memory_card_decks", key = "#id")
public boolean decrementUpvoteCount(long id) {
    return false;
}
```

**问题**:
1. 方法体直接返回 false，没有实际实现
2. 方法上有 `@CacheEvict` 注解，表明设计上需要清除缓存，但逻辑未实现
3. 方法命名表明应该减少点赞数，但没有调用 Mapper 方法

**影响**: 如果业务代码调用此方法，点赞数不会实际减少，可能导致数据不一致。

**修复建议**:
```java
@CacheEvict(value = "memory_card_decks", key = "#id")
public boolean decrementUpvoteCount(long id) {
    try {
        int result = memoryCardDeckMapper.decrementUpvoteCount(id);
        return result > 0;
    } catch (Exception e) {
        log.error("Error decrementing upvote count: {}", id, e);
        throw StatusCode.DATABASE_ERROR.exception(e);
    }
}
```

同时需要在 MemoryCardDeckMapper 中添加对应的 SQL 方法：
```java
@Update("UPDATE memory_card_deck SET upvote_count = GREATEST(0, upvote_count - 1), updated_at = NOW() WHERE id = #{id}")
int decrementUpvoteCount(long id);
```

---

#### 2.2 ReviewController.batchSubmitReview() 被注释但未删除

**位置**: `/backend/learn-web/src/main/java/com/prosper/learn/web/v1/controller/ReviewController.java:82-93`

```java
/**
 * 批量提交复习结果
 *
 * @deprecated 此接口未被前端使用，且后端实现存在严重bug。
 *             前端已采用单次提交模式 (POST /submit)，体验更好。
 *             如需重新启用，必须先修复 ReviewService.batchSubmitReview() 的实现。
 */
//    @PostMapping("/batch-submit")
//    @SaCheckLogin
//    public ApiResponse<Void> batchSubmitReview(
//            @Valid @RequestBody ReviewSessionRequest session,
//            @CurrentUser UserDO currentUser) {
//        reviewService.batchSubmitReview(currentUser.getId(), session);
//        return ApiResponse.success();
//    }
```

**问题**:
1. 代码被注释但保留在文件中，说明实现有严重 bug
2. 注释提到 "后端实现存在严重bug"，但没有说明具体 bug 是什么
3. 虽然标记为 `@deprecated`，但注释的代码不会真正被弃用，可能引起误解

**建议**:
1. **立即删除**: 如果确定不会使用，直接删除注释的代码
2. **修复后启用**: 如果未来可能使用，应该：
   - 记录具体的 bug 描述（什么 bug？在哪个类？）
   - 创建 TODO 或 Issue 跟踪修复任务
   - 移除注释代码，保持代码库整洁

---

#### 2.3 MemoryCardDeckMapper 缺少 decrementUpvoteCount() SQL 方法

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/deck/MemoryCardDeckMapper.java`

**问题**: MemoryCardDeckDataService.decrementUpvoteCount() 方法调用的 Mapper 方法不存在。

**影响**: 如果尝试实现点赞减少功能，会在运行时抛出方法不存在异常。

**修复建议**: 在 MemoryCardDeckMapper 中添加：
```java
@Update("UPDATE memory_card_deck SET upvote_count = GREATEST(0, upvote_count - 1), updated_at = NOW() WHERE id = #{id}")
int decrementUpvoteCount(long id);
```

---

### P2 - 次要问题（可选优化）

#### 2.4 UserCardSrsMapper 复杂 UNION ALL 查询可能影响性能

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/review/UserCardSrsMapper.java:207-256` (getDueCardsForReviewWithPaging)

```java
@Select({"<script>",
        "(",
        "  SELECT srs.* FROM user_card_srs srs",
        "  INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
        "  WHERE srs.user_id = #{userId} AND srs.review_due_at &lt;= #{dueTime}",
        "  AND srs.type IN (1, 2, 3)",  // LEARNING, REVIEW, RELEARNING
        "  ...",
        ")",
        "UNION ALL",
        "(",
        "  SELECT srs.* FROM user_card_srs srs",
        "  ...",
        "  WHERE srs.user_id = #{userId} AND srs.type = 0",  // NEW cards
        ")",
        "ORDER BY ...",
        "LIMIT #{limit}",
        "</script>"})
List<UserCardSrsDO> getDueCardsForReviewWithPaging(...);
```

**观察**:
1. 使用 `UNION ALL` 查询到期卡片和新卡片，然后在外层排序
2. 复杂的排序逻辑：`CASE srs.type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 ELSE 3 END`
3. 两个子查询都有 INNER JOIN，可能增加查询成本

**影响**: 在用户卡片数量较大时（>10000张），查询性能可能较慢。

**优化建议**:
1. **添加数据库索引**:
   ```sql
   -- user_card_srs 表
   INDEX idx_user_type_due (user_id, type, review_due_at, id)
   INDEX idx_user_deck (user_id, deck_id)
   ```

2. **考虑缓存热点数据**: 对于活跃用户的到期卡片列表，可以使用 Redis 缓存（TTL: 5分钟）

3. **限制查询范围**: 如果新卡片数量很大，可以限制 NEW 卡片的数量（如最多返回10张新卡片）

---

#### 2.5 MemoryCardDeckDomainService 方法数量过多（54个方法）

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/deck/MemoryCardDeckDomainService.java`

**观察**:
- 类中包含 54 个公共方法
- 大量查询方法的变体：`getListByPost`, `getListByPostKeyset`, `getListByPostDynamic`, `getListByPostWithIdPaging`, `getListByPostForReview` 等
- 不同的分页方式（Keyset分页、ID分页、动态排序）和状态过滤组合产生了方法爆炸

**影响**:
1. 类过于庞大，难以维护和测试
2. 方法命名相似，容易混淆
3. 违反单一职责原则

**优化建议**:
1. **引入查询对象模式（Query Object Pattern）**:
   ```java
   public class DeckQueryBuilder {
       private Long postId;
       private Long creatorId;
       private Long nodeId;
       private ContentState state;
       private String sortBy;
       private Double lastScore;
       private Long lastId;
       private Integer limit;
       private boolean includeAllStates;

       // Builder 方法...

       public List<MemoryCardDeckDO> execute() {
           // 根据设置的参数选择合适的查询方法
       }
   }
   ```

2. **使用策略模式处理不同的分页和排序逻辑**

3. **考虑使用 MyBatis Dynamic SQL** 减少重复的 Mapper 方法

---

#### 2.6 缺少分布式锁防止并发问题

**观察**: 以下场景可能存在并发问题：
1. MemoryBankDomainService.addDeckToMemoryBank() - 批量插入卡片
2. MemoryCardDeckDomainService.acceptDeckChanges() - 接受卡片组更新
3. ReviewDomainService.submitReview() - 提交复习结果

**问题**:
- 在高并发场景下，同一用户同时添加相同卡片到不同课程，可能导致：
  - 节点卡片数量限制检查失效（检查时未达到限制，插入时超过限制）
  - SRS 状态重复创建

**影响**: 极端情况下可能出现数据不一致。

**优化建议**:
```java
@Transactional
public void addDeckToMemoryBank(Long userId, Long courseId, Long deckId, ...) {
    String lockKey = "memory:add_deck:" + userId + ":" + nodeId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
        if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            // 原有业务逻辑...
        } else {
            throw StatusCode.SYSTEM_BUSY.exception("系统繁忙，请稍后重试");
        }
    } finally {
        lock.unlock();
    }
}
```

---

#### 2.7 ReviewDomainService.calculateStreakDays() 实现可能不正确

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/review/UserCardSrsMapper.java:181-183`

```java
@Select("SELECT COALESCE(DATEDIFF(CURDATE(), DATE(MAX(last_reviewed_at))), 0) AS days_since_last " +
        "FROM user_card_srs WHERE user_id = #{userId} AND last_reviewed_at IS NOT NULL")
int calculateStreakDays(long userId);
```

**问题**:
1. 方法名是 `calculateStreakDays`（连续复习天数），但实际计算的是 **距离上次复习的天数**
2. 连续复习天数应该计算：用户连续多少天都有复习记录，而不是距离上次复习过了多久
3. 如果用户昨天复习了，今天没复习，这个方法会返回 1（天），但连续天数应该是 1 天（昨天）或 0 天（今天中断了）

**影响**: 统计数据不准确，可能误导用户。

**修复建议**:
```java
// 正确的连续复习天数计算
@Select("""
    SELECT COUNT(*) FROM (
        SELECT DISTINCT DATE(last_reviewed_at) AS review_date
        FROM user_card_srs
        WHERE user_id = #{userId}
          AND last_reviewed_at >= CURDATE() - INTERVAL (
              SELECT DATEDIFF(CURDATE(), MIN(DATE(last_reviewed_at)))
              FROM user_card_srs
              WHERE user_id = #{userId}
          ) DAY
        ORDER BY review_date DESC
    ) AS dates
    WHERE review_date >= (
        SELECT MAX(review_date) - INTERVAL (
            SELECT COUNT(*) - 1 FROM (
                SELECT review_date,
                       LAG(review_date, 1, review_date) OVER (ORDER BY review_date DESC) as prev_date
                FROM (SELECT DISTINCT DATE(last_reviewed_at) AS review_date FROM user_card_srs WHERE user_id = #{userId}) t
            ) gaps WHERE DATEDIFF(review_date, prev_date) = 1
        ) DAY
        FROM (SELECT DISTINCT DATE(last_reviewed_at) AS review_date FROM user_card_srs WHERE user_id = #{userId}) t
    )
""")
int calculateStreakDays(long userId);
```

或者更简单的实现（在 Java 代码中计算）：
```java
public int calculateStreakDays(long userId) {
    List<LocalDate> reviewDates = userCardSrsMapper.getDistinctReviewDates(userId);
    if (reviewDates.isEmpty()) return 0;

    reviewDates.sort(Comparator.reverseOrder());
    int streak = 0;
    LocalDate expectedDate = LocalDate.now();

    for (LocalDate reviewDate : reviewDates) {
        if (reviewDate.equals(expectedDate) || reviewDate.equals(expectedDate.minusDays(1))) {
            streak++;
            expectedDate = reviewDate.minusDays(1);
        } else {
            break;
        }
    }
    return streak;
}
```

---

#### 2.8 MemoryCardDomainService.batchUpdate() 批量操作效率低

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/card/MemoryCardMapper.java:81-90`

```java
@Update({"<script>" +
        "<foreach collection='cards' item='card' separator=';'>" +
        "UPDATE memory_card SET " +
        "current_version_id = #{card.currentVersionId}, " +
        "state = #{card.state}, " +
        "updated_at = #{card.updatedAt} " +
        "WHERE id = #{card.id} AND deleted_at IS NULL" +
        "</foreach>" +
        "</script>"})
int batchUpdate(@Param("cards") List<MemoryCardDO> cards);
```

**问题**:
1. 使用 `separator=';'` 执行多个 UPDATE 语句，不是真正的批量操作
2. MyBatis 会将其转换为多次数据库往返，性能不如单条 SQL
3. 每次更新都需要加锁，可能导致死锁

**优化建议**:
```java
// 使用 CASE WHEN 批量更新
@Update({"<script>" +
        "UPDATE memory_card SET " +
        "current_version_id = CASE id " +
        "<foreach collection='cards' item='card'>" +
        "WHEN #{card.id} THEN #{card.currentVersionId} " +
        "</foreach>" +
        "END, " +
        "state = CASE id " +
        "<foreach collection='cards' item='card'>" +
        "WHEN #{card.id} THEN #{card.state} " +
        "</foreach>" +
        "END, " +
        "updated_at = CASE id " +
        "<foreach collection='cards' item='card'>" +
        "WHEN #{card.id} THEN #{card.updatedAt} " +
        "</foreach>" +
        "END " +
        "WHERE id IN " +
        "<foreach collection='cards' item='card' open='(' separator=',' close=')'>" +
        "#{card.id}" +
        "</foreach>" +
        " AND deleted_at IS NULL" +
        "</script>"})
int batchUpdate(@Param("cards") List<MemoryCardDO> cards);
```

---

#### 2.9 缺少复杂查询的单元测试

**观察**:
- UserCardSrsMapper.getDueCardsForReviewWithPaging() 和 getDueCardsByCourseForReviewWithPaging() 使用了复杂的 UNION ALL 查询
- UserCardInCourseMapper.getBatchCardStatsForCourses() 使用了复杂的聚合查询
- 没有找到对应的单元测试文件

**建议**: 为复杂的 SQL 查询添加单元测试，覆盖以下场景：
1. 空数据情况
2. 边界情况（lastId 为 null、limit 为 0）
3. 不同卡片类型的排序优先级
4. 分页连续性（确保不会遗漏或重复数据）

---

#### 2.10 DataService 层部分方法缺少 @CacheEvict 注解

**位置**: `/backend/learn-memory/src/main/java/com/prosper/learn/memory/review/UserCardSrsDataService.java:116-130`

```java
//@CacheEvict(value = "user_card_srs", key = "#state.id")
public void update(UserCardSrsDO state) {
    if (state == null || state.getId() == null) {
        throw new IllegalArgumentException("State or state ID cannot be null");
    }

    try {
        userCardSrsMapper.update(state);
        log.debug("Updated SRS state {}", state.getId());
    } catch (Exception e) {
        log.error("Error updating SRS state: {}", state.getId(), e);
        throw StatusCode.DATABASE_ERROR.exception(e);
    }
}
```

**问题**: `@CacheEvict` 注解被注释掉，但没有说明原因。

**影响**:
- SRS 状态更新后，缓存不会自动清除
- 可能导致读取到过期的数据
- 但 SRS 状态更新频繁（每次复习都会更新），频繁清除缓存可能影响性能

**建议**:
1. **如果 SRS 状态不需要缓存**（因为更新频繁），应该：
   - 在 getCacheTtl() 中返回 Duration.ZERO
   - 移除所有缓存相关注解
2. **如果需要缓存**，应该：
   - 启用 @CacheEvict 注解
   - 设置较短的 TTL（如 1-5 分钟）

---

## 3. 做得好的地方

### 3.1 完整的 Anki 算法实现

ReviewDomainService 实现了完整的 Anki 间隔重复学习算法：
- **新卡片（NEW）**: 根据评级进入学习流程或直接毕业
- **学习中（LEARNING）**: 步进式学习，支持重来/困难/良好/简单
- **复习中（REVIEW）**: 基于 EF（难度系数）的间隔增长
- **重新学习（RELEARNING）**: 遗忘后的重新学习流程

```java
private void handleReviewCard(UserCardSrsDO card, int rating) {
    SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();
    int currentInterval = card.getInterval();
    BigDecimal ef = card.getEaseFactor();

    if (rating == 1) {
        // 遗忘，进入重新学习...
    } else if (rating == 3) {
        // 良好，标准间隔增长
        int newInterval = (int) (currentInterval * ef.doubleValue());
        card.setInterval(newInterval);
        card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
    }
    // ...
}
```

算法细节准确，考虑了各种边界情况。

---

### 3.2 卡片版本管理设计优秀

支持卡片内容版本控制：
- **版本快照**: UserCardSrsDO.cardVersionId 记录用户学习时的卡片版本
- **版本比对**: getDeckDiff() 可以对比新旧版本差异
- **增量更新**: acceptDeckChanges() 支持选择性接受更新

```java
public Map<String, Object> getDeckDiff(Long deckId, Integer userCurrentVersion, Long userId) {
    // 获取用户学习的版本和卡片组当前版本
    // 对比差异：新增、修改、删除的卡片
    // 返回详细的 diff 结果
}
```

这种设计允许卡片组创建者更新内容，同时用户可以选择是否同步更新。

---

### 3.3 领域驱动设计（DDD）分层清晰

模块严格遵循 DDD 分层架构：
- **领域层（DomainService）**: 纯业务逻辑，不依赖外部模块
- **数据访问层（DataService）**: 缓存管理、数据持久化
- **基础设施层（Mapper）**: SQL 操作

```java
// MemoryBankDomainService 只依赖 memory 域的其他服务
public class MemoryBankDomainService {
    private final UserCourseSrsSettingDataService courseSrsSettingDataService;
    private final UserCardInCourseDataService userCardInCourseDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    // 不依赖 course、user 等外部模块
}
```

这种设计降低了模块间耦合，便于测试和维护。

---

### 3.4 批量操作避免 N+1 查询

多处使用批量查询优化性能：

```java
// MemoryBankDomainService.removeDeckFromCourse()
// 批量查询仍有课程关系的卡片ID（优化1+N查询）
List<Long> existingCardIds = userCardInCourseDataService
    .getExistingCardIdsByUserAndCards(userId, cardIds);
Set<Long> existingCardSet = new HashSet<>(existingCardIds);

// 找出孤立的卡片ID（没有其他课程关系的卡片）
List<Long> orphanedCardIds = cardIds.stream()
    .filter(cardId -> !existingCardSet.contains(cardId))
    .collect(Collectors.toList());

// 批量删除孤立卡片的SRS状态
if (!orphanedCardIds.isEmpty()) {
    userCardSrsDataService.batchDeleteByUserAndCards(userId, orphanedCardIds);
}
```

避免了循环查询，提升了性能。

---

### 3.5 使用 INSERT IGNORE 处理幂等性

批量插入操作使用 INSERT IGNORE 自动跳过重复记录：

```java
@Insert("""
      <script>
      INSERT IGNORE INTO user_card_srs
      (user_id, card_id, node_id, deck_id, ...)
      VALUES
      <foreach collection="states" item="state" separator=",">
          (#{state.userId}, #{state.cardId}, ...)
      </foreach>
      </script>
      """)
int batchInsertIgnoreSrsStates(@Param("states") List<UserCardSrsDO> states);
```

这种设计简化了代码，避免了手动检查重复的复杂逻辑。

---

### 3.6 缓存策略设计合理

DataService 层统一管理缓存：
- **自动缓存**: 继承 AbstractDataService，getById() 自动使用缓存
- **手动清除**: 更新/删除操作使用 @CacheEvict 清除缓存
- **合理的 TTL**: 不同实体设置不同的过期时间
  - MemoryCard: 20分钟
  - MemoryCardVersion: 1小时（版本不常变）
  - UserCardSrs: 30分钟
  - UserCourseSrsSetting: 2小时

```java
@Override
protected Duration getCacheTtl() {
    return Duration.ofMinutes(20);
}
```

---

### 3.7 完善的参数校验

所有 Controller 方法都使用 Bean Validation：

```java
@GetMapping("/cards/node/{nodeId}")
@SaCheckLogin
public ApiResponse<List<CardWithSrsDTO>> getUserCardsByNode(
        @PathVariable @NotNull(message = "节点ID不能为空")
        @Positive(message = "节点ID必须大于0")
        Long nodeId,
        @CurrentUser UserDO currentUser) {
    // ...
}
```

DataService 层也有参数校验：
```java
public MemoryCardDO validateAndGet(Long id) {
    if (id == null) {
        throw StatusCode.INVALID_PARAMETER.exception("卡片ID不能为空");
    }
    if (id <= 0) {
        throw StatusCode.INVALID_PARAMETER.exception("卡片ID必须大于0");
    }
    // ...
}
```

多层校验确保了数据安全。

---

### 3.8 原子操作避免并发问题

使用数据库原子操作更新计数器：

```java
@Update("UPDATE memory_card_deck SET card_count = GREATEST(0, card_count - 1), updated_at = NOW() WHERE id = #{id}")
int decrementCardCount(long id);

@Update("UPDATE memory_card_deck SET card_count = card_count + 1, state = #{state}, version = version + 1, updated_at = NOW() WHERE id = #{id}")
int incrementCardCountAndSetStateAndVersion(long id, byte state);
```

GREATEST(0, card_count - 1) 确保计数不会变为负数。

---

### 3.9 Keyset 分页性能优化

多处使用 Keyset 分页（也称游标分页）代替传统的 OFFSET/LIMIT 分页：

```java
@Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL AND " +
        "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
        "ORDER BY score DESC, id DESC LIMIT #{limit}")
List<MemoryCardDeckDO> getListByPostKeyset(long postId, double lastScore, long lastId, int state, int limit);
```

Keyset 分页优势：
- **性能稳定**: 不会因为翻页深度增加而变慢（OFFSET 方式会）
- **无跳页问题**: 在数据变化时不会跳过或重复数据
- **适合无限滚动**: 前端常用的加载更多场景

---

### 3.10 明确的业务异常

使用明确的状态码抛出业务异常：

```java
throw StatusCode.MEMORY_CARD_NOT_FOUND.exception();
throw StatusCode.PERMISSION_DENIED.exception("无权限删除此卡片");
throw StatusCode.NODE_CARD_LIMIT_EXCEEDED.exception(
    String.format("该节点已有%d张卡片，添加%d张新卡片将超过%d张的限制",
        currentCardCount, newCardCount, maxCardsPerNode)
);
```

异常信息清晰，便于调试和向用户反馈。

---

## 4. 代码规范检查

### 4.1 参数类型使用正确 ✅

所有静态 SQL 的参数类型正确使用原始类型（long, int, byte），避免了装箱拆箱和 NPE 风险。

```java
// 正确使用原始类型
@Select("SELECT * FROM memory_card WHERE id = #{id}")
MemoryCardDO get(long id);

@Update("UPDATE memory_card_deck SET state = #{state} WHERE id = #{id}")
int updateState(long id, byte state);
```

---

### 4.2 事务管理完整 ✅

所有写操作的 DomainService 方法都有 `@Transactional` 注解：

```java
@Transactional
public void addDeckToMemoryBank(Long userId, Long courseId, Long deckId, ...) {
    // 多个数据库操作
}

@Transactional
public MemoryCardDO createCard(Long userId, Long deckId, String front, String back) {
    // 插入卡片 + 插入版本 + 更新卡片
}
```

---

### 4.3 日志记录完善 ✅

关键操作都有日志记录：

```java
log.info("Created card: {} in deck: {} by user: {}", card.getId(), deckId, userId);
log.info("Batch created {} cards for deck {} by user {}", cardContents.size(), deckId, userId);
log.error("Error inserting card: deckId={}", card.getDeckId(), e);
```

---

### 4.4 代码注释清晰 ✅

重要的业务逻辑和复杂算法都有注释：

```java
/**
 * 处理新卡片 (NEW -> LEARNING 或 REVIEW)
 */
private void handleNewCard(UserCardSrsDO card, int rating) {
    // ...
}

// 1. 检查节点下的卡片数量限制
int maxCardsPerNode = systemProperties.getSrs().getMaxCardsPerNode();
```

---

### 4.5 软删除实现正确 ✅

使用 `deleted_at` 字段实现软删除，所有查询都过滤了已删除数据：

```java
@Select("SELECT * FROM memory_card WHERE id = #{id} AND deleted_at IS NULL")
MemoryCardDO get(long id);

@Update("UPDATE memory_card SET deleted_at = #{deletedAt}, updated_at = #{updatedAt} " +
        "WHERE id = #{id} AND deleted_at IS NULL")
int softDelete(MemoryCardDO card);
```

---

## 5. 性能优化建议

### 5.1 数据库索引建议

根据查询模式，建议添加以下索引：

```sql
-- memory_card 表
CREATE INDEX idx_deck_state ON memory_card(deck_id, state, deleted_at);

-- memory_card_deck 表
CREATE INDEX idx_post_state_score ON memory_card_deck(post_id, state, deleted_at, score DESC, id DESC);
CREATE INDEX idx_node_state_score ON memory_card_deck(node_id, state, deleted_at, score DESC, id DESC);
CREATE INDEX idx_creator_state ON memory_card_deck(creator_id, state, deleted_at);

-- user_card_srs 表
CREATE INDEX idx_user_type_due ON user_card_srs(user_id, type, review_due_at, id);
CREATE INDEX idx_user_deck ON user_card_srs(user_id, deck_id);
CREATE INDEX idx_user_node ON user_card_srs(user_id, node_id);
CREATE INDEX idx_user_card ON user_card_srs(user_id, card_id);

-- user_card_in_course 表
CREATE INDEX idx_user_course_card ON user_card_in_course(user_id, course_id, card_id);
CREATE INDEX idx_user_card ON user_card_in_course(user_id, card_id);

-- user_course_srs_setting 表
CREATE UNIQUE INDEX idx_user_course ON user_course_srs_setting(user_id, course_id);
```

---

### 5.2 查询优化建议

1. **getDueCardsForReviewWithPaging**: 考虑将 NEW 卡片和到期卡片分开查询，避免复杂的 UNION ALL
2. **getBatchCardStatsForCourses**: 对于活跃用户，考虑将统计结果缓存到 Redis（TTL: 5分钟）
3. **getDeckDiff**: 对于大型卡片组（>100张卡片），考虑分页返回差异

---

## 6. 安全性检查

### 6.1 权限验证完整 ✅

所有敏感操作都有权限检查：

```java
// 只有创建者可以修改卡片
if (!existingCard.getCreatorId().equals(userId)) {
    throw StatusCode.PERMISSION_DENIED.exception("无权限修改此卡片");
}

// 只有创建者可以删除卡片组
if (!deck.getCreatorId().equals(userId)) {
    throw StatusCode.PERMISSION_DENIED.exception();
}
```

---

### 6.2 SQL 注入防护 ✅

所有 SQL 都使用参数化查询，没有字符串拼接：

```java
@Select("SELECT * FROM memory_card WHERE deck_id = #{deckId} AND state = #{state}")
List<MemoryCardDO> getListByDeck(long deckId, int state);
```

---

### 6.3 限流保护 ✅

所有 Controller 都配置了限流：

```java
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class MemoryCardController {
    // ...
}
```

---

## 7. 总结

### 7.1 优先级修复顺序

1. **P1-2.1**: 实现 MemoryCardDeckDataService.decrementUpvoteCount() 方法（如果需要点赞功能）
2. **P1-2.2**: 删除或修复 ReviewController.batchSubmitReview() 注释代码
3. **P2-2.7**: 修复 calculateStreakDays() 的实现逻辑
4. **P2-2.8**: 优化 batchUpdate() 的 SQL 实现
5. **P2-2.4**: 为复杂查询添加索引，优化性能

---

### 7.2 代码质量评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | 9.5/10 | DDD 分层清晰，职责分明，卡片版本管理设计优秀 |
| 代码规范 | 9/10 | 参数类型、事务管理、日志记录都很规范 |
| 缓存策略 | 8.5/10 | 缓存设计合理，但部分方法注释了 @CacheEvict |
| 异常处理 | 9/10 | 业务异常明确，错误信息清晰 |
| 性能优化 | 8/10 | 批量操作和 Keyset 分页设计优秀，但部分 SQL 可优化 |
| 安全性 | 9/10 | 权限验证、SQL 注入防护、限流保护完整 |
| **总体评分** | **8.8/10** | **优秀，少量可优化点** |

---

### 7.3 架构亮点

1. **完整的 Anki 算法实现**: 四种卡片状态（NEW/LEARNING/REVIEW/RELEARNING）的状态转换完整且准确
2. **卡片版本管理**: 支持内容更新、版本比对、增量同步，设计优秀
3. **领域驱动设计**: 严格的分层架构，低耦合高内聚
4. **性能优化**: Keyset 分页、批量操作、缓存策略设计合理
5. **幂等性设计**: 使用 INSERT IGNORE 处理并发插入

---

### 7.4 后续建议

1. **补充单元测试**: 重点测试复杂的 SQL 查询和 Anki 算法逻辑
2. **添加数据库索引**: 根据 5.1 节的建议添加索引，提升查询性能
3. **引入分布式锁**: 为高并发场景添加分布式锁保护
4. **重构查询方法**: 引入 Query Object Pattern，减少 MemoryCardDeckDomainService 的方法数量
5. **修复 calculateStreakDays()**: 正确实现连续复习天数的计算逻辑
6. **监控性能**: 对复杂查询添加慢查询日志，监控性能瓶颈

---

## 8. 附录：注释代码清理建议

模块中有大量注释掉的代码（标记为 `--注释掉检查 START/STOP`），建议进行清理：

### 8.1 应该删除的注释代码

这些方法未被使用，且没有计划重新启用：
- MemoryCardMapper: getListByCreator(), countByCreator(), getCardIdsByDeckId()
- MemoryCardVersionMapper: 无需删除（注释较少）
- UserCardSrsMapper: 大量注释的查询方法（如 getDueCardsForReview, getByUser, getByUserAndCourse 等）
- UserCardInCourseMapper: 大量注释的 CRUD 方法
- UserCourseSrsSettingMapper: updateFrequencySetting(), updateState(), deleteByUserAndCourse() 等

### 8.2 清理方式

1. **确认未被使用**: 使用 IDE 的 "Find Usages" 功能确认方法未被调用
2. **删除注释代码**: 直接删除注释的方法
3. **更新文档**: 如果有 API 文档，同步更新

**原因**:
- 注释代码会增加维护负担
- 可能误导其他开发者
- 如果未来需要，可以从 Git 历史恢复

---

**审查日期**: 2026-01-10
**审查人**: Claude Code
**模块版本**: learn-memory (current)
