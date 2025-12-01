# 用户统计系统重构设计文档

## 概述

基于现有 `user_stats` 表按年份分区的设计，重构用户统计系统以支持更丰富的统计维度，包括学习进度、社交关系、内容创作等累计统计数据。

## 核心设计原则

1. **数据分离**: 当日数据与历史数据物理分离
2. **实时同步**: 通过业务触发的懒同步机制
3. **职责明确**: 增量统计与累计统计分别处理
4. **性能优先**: 避免大表查询，优化当日数据访问

## 数据库设计

### 1. 表结构重构

#### 1.1 现有表重命名
```sql
-- 将现有表重命名为历史数据表
RENAME TABLE user_stats TO user_stats_yearly;
```

#### 1.2 新建当日统计表
```sql
CREATE TABLE user_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    daily_stat_date DATE NOT NULL,

    -- 日度增量统计
    daily_views INT DEFAULT 0 COMMENT '当日浏览量',
    daily_twice INT DEFAULT 0 COMMENT '当日两次能懂点赞数',
    daily_helpful INT DEFAULT 0 COMMENT '当日有帮助点赞数',
    daily_comments INT DEFAULT 0 COMMENT '当日评论数',

    -- 学习统计（累计快照）
    learning_courses INT DEFAULT 0 COMMENT '正在学习课程数',
    completed_courses INT DEFAULT 0 COMMENT '已完成课程数',
    in_progress_professions INT DEFAULT 0 COMMENT '正在进行职业数',
    completed_professions INT DEFAULT 0 COMMENT '已完成职业数',

    -- 社交统计（累计快照）
    following_users INT DEFAULT 0 COMMENT '关注的人数',
    following_courses INT DEFAULT 0 COMMENT '关注的课程数',
    following_professions INT DEFAULT 0 COMMENT '关注的职业数',

    -- 创作统计（累计快照）
    created_articles INT DEFAULT 0 COMMENT '创建的文章数',
    created_Indexs INT DEFAULT 0 COMMENT '创建的目录数',
    created_roadmaps INT DEFAULT 0 COMMENT '创建的路线图数',
    created_card_decks INT DEFAULT 0 COMMENT '创建的卡片组数',

    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_date (user_id, daily_stat_date),
    INDEX idx_stat_date (daily_stat_date),
    INDEX idx_updated_at (updated_at),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='用户当日统计表';
```

### 2. 数据分工

#### 2.1 user_stats 表职责
- 存储当日的增量活动数据
- 存储用户的累计统计快照
- 提供实时统计查询接口
- 数据范围：仅当日数据

#### 2.2 user_stats_yearly 表职责
- 存储历史增量活动数据（JSON格式）
- 支持历史趋势分析和报表
- 数据范围：昨日及之前的所有历史数据

**JSON格式说明：**
```json
{
  "9-5": [0, 1, 3, 5],    // 9月5日：[views, twice, helpful, comments]
  "9-9": [0, 5, 0, 0],    // 9月9日：[views, twice, helpful, comments]
  "9-10": [2, 0, 1, 3]    // 9月10日：[views, twice, helpful, comments]
}
```

数组索引对应：
- `[0]` - views（浏览量）
- `[1]` - twice（两次能懂点赞）
- `[2]` - helpful（有帮助点赞）
- `[3]` - comments（评论数）

## 数据同步机制

### 1. 懒同步策略

#### 1.1 触发时机
```java
public void incrementUserStat(Long userId, StatType type, int delta) {
    String today = LocalDate.now().toString();
    UserStatsDO todayStats = getOrCreateTodayRecord(userId, today);

    // 关键：创建今日记录前检查历史数据同步
    if (todayStats == null) {
        syncStaleStatsIfNeeded(userId);
        todayStats = createTodayRecord(userId, today);
    }

    updateStat(todayStats, type, delta);
}
```

#### 1.2 同步逻辑
```java
private void syncStaleStatsIfNeeded(Long userId) {
    // 查找昨日及之前的待同步数据
    List<UserStatsDO> staleStats = userStatsDAO.getStaleStats(userId,
        LocalDate.now().minusDays(1));

    if (!staleStats.isEmpty()) {
        for (UserStatsDO stats : staleStats) {
            syncToYearlyAndDelete(stats);
        }
    }
}

private void syncToYearlyAndDelete(UserStatsDO stats) {
    // 1. 提取日度增量数据，转换为数组格式
    int[] dailyStats = extractDailyStatsArray(stats);

    // 2. 更新yearly表的JSON（数组格式）
    updateYearlyStatsArray(stats.getUserId(), stats.getDailyStatDate(), dailyStats);

    // 3. 删除已同步的数据
    userStatsDataService.deleteById(stats.getId());
}

private int[] extractDailyStatsArray(UserStatsDO stats) {
    // 转换为固定顺序的数组：[views, twice, helpful, comments]
    return new int[] {
        stats.getDailyViews() != null ? stats.getDailyViews() : 0,
        stats.getDailyTwice() != null ? stats.getDailyTwice() : 0,
        stats.getDailyHelpful() != null ? stats.getDailyHelpful() : 0,
        stats.getDailyComments() != null ? stats.getDailyComments() : 0
    };
}
```

### 2. 非活跃用户处理

#### 2.1 查询时检查
```java
public UserStatsDTO getUserStats(Long userId) {
    // 查询前确保数据完整性
    syncStaleStatsIfNeeded(userId);

    UserStatsDO currentStats = getCurrentDayStats(userId);
    return convertToDTO(currentStats);
}
```

#### 2.2 保底清理任务
```java
@Scheduled(cron = "0 30 1 * * ?") // 每日1:30兜底清理
public void cleanupStaleStats() {
    // 处理超过1天的老数据，确保数据最终一致性
    List<UserStatsDO> staleStats = userStatsDAO.getStatsOlderThan(1);

    if (!staleStats.isEmpty()) {
        log.info("开始兜底同步，发现{}条待同步数据", staleStats.size());
        batchSyncToYearly(staleStats);
        log.info("兜底同步完成");
    }
}

@Transactional
private void batchSyncToYearly(List<UserStatsDO> staleStats) {
    for (UserStatsDO stats : staleStats) {
        try {
            syncToYearlyAndDelete(stats);
        } catch (Exception e) {
            log.error("同步失败，userId: {}, date: {}",
                stats.getUserId(), stats.getStatDate(), e);
            // 继续处理下一条，不因单条失败而中断
        }
    }
}
```

## 并发控制

### 1. 数据库级原子操作
```sql
-- 使用数据库原子操作避免应用层锁
UPDATE user_stats
SET daily_views = daily_views + ?,
    updated_at = NOW()
WHERE user_id = ? AND stat_date = ?;
```

### 2. 乐观锁机制
```java
@Version
private Long version; // 在UserStatsDO中添加版本字段

public void incrementUserStat(Long userId, StatType type, int delta) {
    try {
        atomicUpdate(userId, type, delta);
    } catch (OptimisticLockException e) {
        // 重试机制
        retryUpdate(userId, type, delta);
    }
}
```

## 服务层设计

### 1. UserStatsService 核心接口

```java
@Service
@RequiredArgsConstructor
public class UserStatsService {

    // 获取用户当前统计（主要接口）
    public UserStatsDTO getCurrentUserStats(Long userId);

    // 增量更新统计
    public void incrementDailyStat(Long userId, DailyStatType type, int delta);

    // 更新累计统计
    public void updateCumulativeStat(Long userId, CumulativeStatType type, int newValue);

    // 批量获取用户统计（排行榜用）
    public List<UserStatsDTO> batchGetUserStats(List<Long> userIds);

    // 获取历史趋势数据
    public HistoryStatsDTO getUserHistoryStats(Long userId, LocalDate startDate, LocalDate endDate);
}
```

### 2. 统计类型枚举

```java
public enum DailyStatType {
    VIEWS("daily_views"),
    TWICE("daily_twice"),
    HELPFUL("daily_helpful"),
    COMMENTS("daily_comments");
}

public enum CumulativeStatType {
    LEARNING_COURSES("learning_courses_count"),
    COMPLETED_COURSES("completed_courses_count"),
    FOLLOWING_USERS("following_users_count"),
    CREATED_ARTICLES("created_articles_count");
    // ...
}
```

### 3. 统计更新方法

```java
@Service
@RequiredArgsConstructor
public class UserStatsService {

    // 获取用户当前统计（主要接口）
    public UserStatsDTO getCurrentUserStats(Long userId);

    // 日度增量更新
    public void incrementDailyStat(Long userId, DailyStatType type, int delta);

    // 累计统计增量更新（推荐方式）
    public void incrementCumulativeStat(Long userId, CumulativeStatType type, int delta);

    // 累计统计设置绝对值（用于数据修复）
    public void setCumulativeStat(Long userId, CumulativeStatType type, int newValue);

    // 重新计算累计统计（用于数据校准）
    public void recalculateCumulativeStat(Long userId, CumulativeStatType type);
}
```

### 4. DTO设计

```java
@Data
@Builder
public class UserStatsDTO {
    // 用户基础信息
    private Long userId;
    private LocalDate statDate;

    // 当日活动统计
    private Integer dailyViews;
    private Integer dailyTwice;
    private Integer dailyHelpful;
    private Integer dailyComments;

    // 学习进度统计
    private Integer learningCourses;
    private Integer completedCourses;
    private Integer inProgressProfessions;
    private Integer completedProfessions;

    // 社交关系统计
    private Integer followingUsers;
    private Integer followingCourses;
    private Integer followingProfessions;

    // 创作内容统计
    private Integer createdArticles;
    private Integer createdIndexs;
    private Integer createdRoadmaps;
    private Integer createdCardDecks;

    // 汇总信息
    private Integer totalLearningItems;
    private Integer totalCreatedItems;
    private LocalDateTime lastUpdated;
}
```

## 数据访问层

### 1. 数据模型类

```java
// 当日统计数据模型
@Data
public class UserStatsDO {
    private Long id;
    private Long userId;
    private LocalDate dailyStatDate;

    // 日度增量统计
    private Integer dailyViews;
    private Integer dailyTwice;
    private Integer dailyHelpful;
    private Integer dailyComments;

    // 学习统计（累计快照）
    private Integer learningCourses;
    private Integer completedCourses;
    private Integer inProgressProfessions;
    private Integer completedProfessions;

    // 社交统计（累计快照）
    private Integer followingUsers;
    private Integer followingCourses;
    private Integer followingProfessions;

    // 创作统计（累计快照）
    private Integer createdArticles;
    private Integer createdIndexs;
    private Integer createdRoadmaps;
    private Integer createdCardDecks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 历史统计数据模型（原有的yearly表）
@Data
public class UserStatsYearlyDO {
    private Long userId;
    private String stats;  // JSON格式：{"9-5": [0,1,3,5], "9-9": [0,5,0,0]}
    private Integer statYear;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. UserStatsDataService 扩展

```java
@Service
public class UserStatsDataService extends AbstractDataService<UserStatsDO> {

    // 获取用户当日统计
    public UserStatsDO getCurrentDayStats(Long userId);

    // 日度统计原子增量更新
    public int atomicIncrementDaily(Long userId, LocalDate date, String field, int delta);

    // 累计统计原子增量更新（推荐方式）
    public int atomicIncrementCumulative(Long userId, String field, int delta);

    // 累计统计设置绝对值（数据修复用）
    public int setCumulativeStat(Long userId, String field, int newValue);

    // 批量获取用户统计
    public Map<Long, UserStatsDO> batchGetCurrentStats(List<Long> userIds);

    // 获取待同步的历史数据
    public List<UserStatsDO> getStaleStats(Long userId, LocalDate beforeDate);

    // 获取排行榜数据
    public List<UserStatsDO> getTopUsersByField(String field, int limit);
}

@Service
public class UserStatsYearlyDataService extends AbstractDataService<UserStatsYearlyDO> {

    // 更新yearly表的JSON统计数据
    public void updateYearlyStatsArray(Long userId, LocalDate date, int[] dailyStats);

    // 获取用户历史统计数据
    public List<UserStatsYearlyDO> getUserHistoryStats(Long userId, int startYear);
}
```

### 3. SQL映射示例

```xml
<!-- 日度统计原子增量更新 -->
<update id="atomicIncrementDaily">
    UPDATE user_stats
    SET ${field} = ${field} + #{delta}
    WHERE user_id = #{userId} AND daily_stat_date = #{date}
</update>

<!-- 累计统计原子增量更新 -->
<update id="atomicIncrementCumulative">
    UPDATE user_stats
    SET ${field} = GREATEST(0, ${field} + #{delta})
    WHERE user_id = #{userId} AND daily_stat_date = #{date}
</update>

<!-- 累计统计设置绝对值 -->
<update id="setCumulativeStat">
    UPDATE user_stats
    SET ${field} = #{newValue}
    WHERE user_id = #{userId} AND daily_stat_date = #{date}
</update>

<!-- 批量获取当前统计 -->
<select id="batchGetCurrentStats" resultType="UserStatsDO">
    SELECT * FROM user_stats
    WHERE user_id IN
    <foreach collection="userIds" item="userId" open="(" close=")" separator=",">
        #{userId}
    </foreach>
    AND daily_stat_date = #{date}
</select>

<!-- 获取待同步的历史数据 -->
<select id="getStaleStats" resultType="UserStatsDO">
    SELECT * FROM user_stats
    WHERE user_id = #{userId} AND daily_stat_date &lt; #{beforeDate}
    ORDER BY daily_stat_date ASC
</select>

<!-- yearly表：更新JSON统计数据 -->
<update id="updateYearlyStatsArray">
    UPDATE user_stats_yearly
    SET stats = JSON_SET(
        COALESCE(stats, JSON_OBJECT()),
        CONCAT('$.', #{dateKey}),
        JSON_ARRAY(#{views}, #{twice}, #{helpful}, #{comments})
    )
    WHERE user_id = #{userId} AND stat_year = #{statYear}
</update>
```
```

## API接口设计

### 1. UserStatsController

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserStatsController {

    @GetMapping("/{userId}/stats")
    public Response<UserStatsDTO> getUserStats(@PathVariable Long userId) {
        UserStatsDTO stats = userStatsService.getCurrentUserStats(userId);
        return Response.success(stats);
    }

    @GetMapping("/{userId}/stats/history")
    public Response<HistoryStatsDTO> getUserHistoryStats(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        HistoryStatsDTO history = userStatsService.getUserHistoryStats(userId, startDate, endDate);
        return Response.success(history);
    }

    @GetMapping("/stats/ranking")
    public Response<PageResult<UserRankingDTO>> getUserRanking(
            @RequestParam String orderBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<UserRankingDTO> ranking = userStatsService.getUserRanking(orderBy, page, size);
        return Response.success(ranking);
    }

    @PostMapping("/{userId}/stats/refresh")
    @RequireLogin
    public Response<UserStatsDTO> refreshUserStats(@PathVariable Long userId) {
        // 手动触发数据同步（管理功能）
        userStatsService.forceRefreshUserStats(userId);
        return getUserStats(userId);
    }
}
```

## 事件驱动更新

### 1. 业务事件监听（改进版本）

```java
@Component
@RequiredArgsConstructor
public class UserStatsEventListener {

    private final UserStatsService userStatsService;

    // 浏览事件 - 日度增量
    @EventListener
    public void onContentViewed(ContentViewedEvent event) {
        userStatsService.incrementDailyStat(event.getContentCreatorId(),
            DailyStatType.VIEWS, 1);
    }

    // 评论事件 - 日度增量
    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        userStatsService.incrementDailyStat(event.getContentCreatorId(),
            DailyStatType.COMMENTS, 1);
    }

    // 两次能懂点赞事件 - 日度增量
    @EventListener
    public void onTwiceUpvoted(TwiceUpvotedEvent event) {
        userStatsService.incrementDailyStat(event.getContentCreatorId(),
            DailyStatType.TWICE, 1);
    }

    // 有帮助点赞事件 - 日度增量
    @EventListener
    public void onHelpfulUpvoted(HelpfulUpvotedEvent event) {
        userStatsService.incrementDailyStat(event.getContentCreatorId(),
            DailyStatType.HELPFUL, 1);
    }

    // 关注事件 - 累计增量（推荐方式）
    @EventListener
    public void onUserFollowed(UserFollowedEvent event) {
        userStatsService.incrementCumulativeStat(event.getFollowerId(),
            CumulativeStatType.FOLLOWING_USERS, 1);
    }

    // 取关事件 - 累计增量
    @EventListener
    public void onUserUnfollowed(UserUnfollowedEvent event) {
        userStatsService.incrementCumulativeStat(event.getFollowerId(),
            CumulativeStatType.FOLLOWING_USERS, -1);
    }

    // 课程关注 - 累计增量
    @EventListener
    public void onCourseFollowed(CourseFollowedEvent event) {
        userStatsService.incrementCumulativeStat(event.getUserId(),
            CumulativeStatType.FOLLOWING_COURSES, 1);
    }

    // 课程学习开始 - 累计增量
    @EventListener
    public void onCourseEnrolled(CourseEnrolledEvent event) {
        userStatsService.incrementCumulativeStat(event.getUserId(),
            CumulativeStatType.LEARNING_COURSES, 1);
    }

    // 课程完成 - 状态转换
    @EventListener
    public void onCourseCompleted(CourseCompletedEvent event) {
        // 学习中的课程减1，已完成课程加1
        userStatsService.incrementCumulativeStat(event.getUserId(),
            CumulativeStatType.LEARNING_COURSES, -1);
        userStatsService.incrementCumulativeStat(event.getUserId(),
            CumulativeStatType.COMPLETED_COURSES, 1);
    }

    // 文章发布 - 累计增量
    @EventListener
    public void onArticlePublished(ArticlePublishedEvent event) {
        userStatsService.incrementCumulativeStat(event.getCreatorId(),
            CumulativeStatType.CREATED_ARTICLES, 1);
    }

    // 文章删除 - 累计增量
    @EventListener
    public void onArticleDeleted(ArticleDeletedEvent event) {
        userStatsService.incrementCumulativeStat(event.getCreatorId(),
            CumulativeStatType.CREATED_ARTICLES, -1);
    }
}
```

### 2. 数据校准机制

对于一些复杂场景或数据修复需求，提供重新计算的方法：

```java
@Component
@RequiredArgsConstructor
public class UserStatsCalibrationService {

    // 重新计算用户的关注数（数据修复用）
    public void recalculateFollowingCount(Long userId) {
        int actualCount = followDataService.countByFollowerId(userId);
        userStatsService.setCumulativeStat(userId,
            CumulativeStatType.FOLLOWING_USERS, actualCount);
    }

    // 重新计算用户的文章数
    public void recalculateArticleCount(Long userId) {
        int actualCount = postDataService.countPublishedArticlesByCreator(userId);
        userStatsService.setCumulativeStat(userId,
            CumulativeStatType.CREATED_ARTICLES, actualCount);
    }

    // 批量校准用户统计（管理员功能）
    @Async
    public void batchRecalibrate(List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                recalculateFollowingCount(userId);
                recalculateArticleCount(userId);
                // 可以继续添加其他统计项的校准
            } catch (Exception e) {
                log.error("用户统计校准失败，userId: {}", userId, e);
            }
        }
    }
}
```

### 3. 优势总结

**原子性保证：**
- 使用数据库的原子 UPDATE 操作
- 避免读取-修改-写入的竞态条件
- 保证并发安全

**事务一致性：**
- 增量操作不依赖外部查询
- 事务边界清晰
- 失败重试简单

**性能优化：**
- 避免额外的查询开销
- 减少数据库连接和锁定时间
- 支持高并发场景

**数据修复：**
- 提供重新计算机制
- 支持数据校准
- 异常情况可恢复

## 性能优化策略

### 1. 缓存策略

```java
@Cacheable(value = "userStats", key = "#userId", unless = "#result == null")
public UserStatsDTO getCurrentUserStats(Long userId) {
    return convertToDTO(userStatsDataService.getCurrentDayStats(userId));
}

@CacheEvict(value = "userStats", key = "#userId")
public void updateCumulativeStat(Long userId, CumulativeStatType type, int newValue) {
    // 更新后清除缓存
}
```

### 2. 批量操作优化

```java
@Transactional
public void batchUpdateStats(List<UserStatsUpdateRequest> requests) {
    // 按用户ID分组批量处理
    Map<Long, List<UserStatsUpdateRequest>> groupedRequests =
        requests.stream().collect(Collectors.groupingBy(UserStatsUpdateRequest::getUserId));

    groupedRequests.forEach(this::processBatchUpdatesForUser);
}
```

### 3. 索引优化

```sql
-- 确保高效的查询路径
CREATE INDEX idx_user_date ON user_stats (user_id, stat_date);
CREATE INDEX idx_date_field ON user_stats (stat_date, created_articles_count); -- 排行榜查询
CREATE INDEX idx_updated_at ON user_stats (updated_at); -- 同步任务查询
```

## 实施计划

### 第一期：核心功能实现
1. 数据库表结构创建和迁移
2. UserStatsService 核心逻辑实现
3. 基础 API 接口开发
4. 事件监听机制建立

### 第二期：功能完善
1. 历史数据查询接口
2. 排行榜功能实现
3. 批量操作优化
4. 缓存集成

### 第三期：性能优化
1. 数据库索引调优
2. 查询性能监控
3. 异常处理完善
4. 管理后台界面

## 数据迁移方案

### 1. 现有数据保留
- user_stats → user_stats_yearly (重命名)
- 历史数据和查询逻辑保持不变

### 2. 初始化当日数据
```java
@PostConstruct
public void initializeTodayStats() {
    // 系统启动时为活跃用户初始化当日统计记录
    List<Long> activeUserIds = getRecentActiveUsers(Duration.ofDays(7));

    for (Long userId : activeUserIds) {
        createTodayStatsIfNotExists(userId);
    }
}
```

## 总结

此设计方案通过以下核心理念实现了高效的用户统计系统：

1. **数据分离**: 当日热数据与历史冷数据物理分离，优化查询性能
2. **懒同步**: 通过业务触发的同步机制，避免定时任务的复杂性
3. **职责明确**: 增量统计用于趋势分析，累计统计用于实时展示
4. **事件驱动**: 基于业务事件的自动更新，保证数据准确性
5. **性能优先**: 针对主要查询场景优化，支持高并发访问

该方案在保持系统简洁性的同时，满足了个人资料页展示和排行榜功能的核心需求，具备良好的扩展性和维护性。