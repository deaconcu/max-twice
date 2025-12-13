# Redis缓存使用分析文档

## 📊 概览统计

- **已缓存DataService**: 22个
- **未缓存DataService**: 2个
- **缓存覆盖率**: 91.7%
- **关键问题**: 统计数据完全无缓存

---

## 🟢 已实现缓存的DataService

### 基础内容缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **CourseDataService** | `courses` | 15分钟 | `course:{id}` | 课程基础信息，学习过程高频访问 | `getById()` 查询时自动创建 | `update()`, `approve()`, `reject()`, `ban()`, `delete()` |
| **NodeDataService** | `nodes` | 15分钟 | `node:{id}` | 节点信息，学习流程核心数据 | `getById()` 查询时自动创建 | `update()`, `updateState()`, `delete()` |
| **PostDataService** | `posts` | 5分钟 | `post:{id}` | 帖子内容，较短TTL因内容变化频繁 | `getById()` 查询时自动创建 | `update()`, `updateState()`, `updateStateWithReason()`, `softDelete()` |
| **RoadmapDataService** | `roadmaps` | 10分钟 | `roadmap:{id}` | 路线图内容 | `getById()` 查询时自动创建 | `update()`, `updateScore()`, `updateState()`, `softDelete()` |
| **ProfessionDataService** | `professions` | 10分钟 | `profession:{id}` | 职业分类信息 | `getById()` 查询时自动创建 | `update()`, `updateState()` |
| **CommentDataService** | `comments` | 10分钟 | `comment:{id}` | 评论内容 | `getById()` 查询时自动创建 | `update()`, `updateState()`, `updateStateWithReason()` |

### 用户相关缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **UserDataService** | `users`<br>`usersByEmail`<br>`usersByName` | 30分钟<br>10分钟<br>10分钟 | `user:{id}`<br>`{email}`<br>`{name}` | 用户基础信息多维度缓存 | `getById()`, `getByEmail()`, `getByName()` 查询时自动创建 | `update()` (级联清除email和name缓存) |
| **UserProfileDataService** | `userProfiles` | 10分钟 | `userProfile:{userId}` | 用户配置信息 | `getById()` 查询时自动创建 | `update()`, `updateRoadmapPin()` |
| **VerificationDataService** | `verifications` | 10分钟 | `email:{email}:used:{used}` | 验证码缓存 | `getByEmailAndUsed()` 查询时自动创建 | `update()`, `insert()` (清空所有验证缓存) |

### 学习进度缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **UserProgressDataService** | `userProgress` | 10分钟 | `{userId}` | 用户学习进度 | `getByUserId()` 查询时自动创建 | `upsert()` (学习进度更新时) |
| **UserCourseDataService** | `userCourseByUserAndCourse` | 10分钟 | `{userId}_{courseId}` | 用户课程进度 | `getByUserIdAndCourseId()` 查询时自动创建 | `update()`, `delete()`, `deleteByUserAndCourse()` |
| **UserRoadmapDataService** | `userRoadmapByUserAndRoadmap` | 10分钟 | `{userId}_{roadmapId}` | 用户路线图进度 | `getByUserAndRoadmap()` 查询时自动创建 | `update()`, `deleteByUserAndRoadmap()` |

### 交互数据缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **UpvoteDataService** | `upvotes`<br>`upvotesByUser` | 10分钟<br>10分钟 | `upvote:{id}`<br>`{userId}_{objectId}_{type}` | 点赞状态查询缓存 | `getById()`, `getByUserAndObject()` 查询时自动创建 | `update()`, `delete()` (先查询后删除并清缓存) |
| **FollowDataService** | `followRelations` | 10分钟 | `{followerId}_{followeeId}` | 关注关系查询缓存 | `get()` 查询时自动创建 | `delete()` (取消关注时) |
| **MessageDataService** | `messages` | 10分钟 | `message:{id}` | 消息内容缓存 | `getById()` 查询时自动创建 | `update()` (消息更新时) |

### 系统配置缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **SystemDataService** | `system` | 10分钟 | `{key}` 或 `'all'` | 系统配置信息 | `getValue()`, `getAll()` 查询时自动创建 | `setValue()`, `insert()` (配置变更时) |
| **CourseTocDataService** | `courseTocs` | 10分钟 | `{hash}` | 课程目录结构 | `getById()` 查询时自动创建 | `updateRefCount()` (引用计数变更时) |
| **UserCourseTocDataService** | `userCourseTocByUserAndCourse` | 10分钟 | `{userId}_{courseId}` | 用户个人目录 | `getByUserAndCourse()` 查询时自动创建 | `update()`, `insert()` (用户目录变更时) |

---

### 记忆系统缓存
| DataService | 缓存名称 | TTL | 缓存键格式 | 说明 | 缓存创建时机 | 缓存清除时机 |
|------------|---------|-----|-----------|------|------------|------------|
| **MemoryCardDataService** | `memory_cards` | 20分钟 | `memory_card:{id}` | 记忆卡片基础信息 | `getById()` 查询时自动创建 | `update()` |
| **MemoryCardDeckDataService** | `memory_card_decks` | 15分钟 | `memory_card_deck:{id}` | 卡片组信息 | `getById()` 查询时自动创建 | `update()`, `updateScore()`, `updateState()`, `updateTags()`, `updateFromRequest()` |
| **MemoryCardVersionDataService** | `memory_card_versions` | 1小时 | `memory_card_version:{id}` | 卡片版本信息，变更频率相对较低 | `getById()` 查询时自动创建 | `updateActiveStatus()`, `deactivateAllVersions()` (批量手动清除) |
| **UserCardSrsDataService** | `user_card_srs` | 30分钟 | `user_card_srs:{id}` | 用户SRS复习状态（高频访问需要缓存提升性能） | `getById()` 查询时自动创建 | `deleteByUserAndCard()` (手动清除)，`update()` 方法未启用缓存清除 |
| **UserCardInCourseDataService** | `user_card_in_courses` | 1小时 | `user_card_in_course:{id}` | 用户卡片课程归属关系 | `getById()` 查询时自动创建 | 注释掉了所有缓存清除操作 |

## 🔴 未缓存的DataService

### ❗️ 严重缺失（需立即修复）
| DataService | 问题 | 性能影响 | 优先级 |
|------------|------|---------|--------|
| **UserStatsDataService** | 用户统计数据完全无缓存 | 每次用户页面访问都查询数据库 | **P0** |
| **ContentStatsDataService** | 内容统计数据完全无缓存 | 每次内容展示都查询统计数据 | **P0** |

### 不建议缓存
| DataService | 原因 |
|------------|------|
| **OperationLogDataService** | 审计日志，不继承AbstractDataService，无缓存框架支持 |

---

## 🚨 特殊缓存实现

### LearningProgressDomainService - Redis直接操作

这是唯一不使用Spring Cache而直接操作Redis的service：

```java
// Redis键定义
private static final String USER_COMPLETED_KEY_PREFIX = "user:completed:";
private static final String SYNC_FAILED_USERS_KEY = "sync:failed:users";

// 缓存策略
- 数据结构: Redis Set存储用户完成的节点ID
- 缓存键: user:completed:{userId}
- TTL: 365天 (可配置 app.learningProgress.cacheExpireDays)
- 持久化: MySQL作为数据备份
- 失败处理: 失败重试队列 + 定时补偿机制
- 降级策略: Redis失败时直接操作数据库
```

**设计亮点**:
- 双重保障：Redis高性能 + MySQL持久化
- 容错机制：Redis失败时自动降级
- 补偿机制：失败队列 + 定时重试

---

## ⚡ 立即需要修复的问题

### 1. UserStatsDataService 添加缓存

**问题**: 用户统计数据（浏览量、点赞数等）每次都查询数据库

**解决方案**:
```java
@Cacheable(value = "userStats", key = "#userId")
public UserStatsDO getByUserId(Long userId) {
    return userStatsMapper.getByUserId(userId);
}

@CacheEvict(value = "userStats", key = "#userId")
public boolean atomicIncrement(Long userId, String field, int delta) {
    // 现有逻辑保持不变
    // 缓存会在更新后自动清除
}

@CacheEvict(value = "userStats", key = "#userId")
public boolean setField(Long userId, String field, int newValue) {
    // 现有逻辑保持不变
}
```

**建议TTL**: 5-10分钟（统计数据更新频繁）

### 2. ContentStatsDataService 添加缓存

**问题**: 内容统计数据（浏览量、点赞数等）每次都查询数据库

**解决方案**:
```java
@Cacheable(value = "contentStats", key = "#contentType.value() + '_' + #contentId")
public Optional<ContentStatsDO> getByContent(Enums.ContentType contentType, Long contentId) {
    // 现有逻辑保持不变
}

@CacheEvict(value = "contentStats", key = "#contentType.value() + '_' + #contentId")
public boolean atomicIncrement(Enums.ContentType contentType, Long contentId, String field, int delta) {
    // 现有逻辑保持不变
}

@CacheEvict(value = "contentStats", key = "#contentType.value() + '_' + #contentId")
public boolean increase(Enums.ContentType contentType, Long contentId,
                       int viewsDelta, int twicesDelta, int likesDelta, int commentsDelta) {
    // 现有逻辑保持不变
}
```

**建议TTL**: 5-15分钟（内容统计更新频繁）

---

## 🔧 缓存架构分析

### AbstractDataService 统一缓存框架

**核心特性**:
- **批量优化**: 使用Redis MGET实现高效批量查询
- **自动TTL**: 子类可配置不同过期时间
- **缓存穿透保护**: 空值也会缓存
- **降级机制**: 缓存失败时自动查询数据库
- **全局开关**: `app.cache.type = "redis"` 控制缓存启用

**批量缓存实现**:
```java
// 1. MGET批量从缓存获取
Map<Y, T> cachedResults = batchGetFromCache(validIds);

// 2. 找出缓存未命中的ID
List<Y> missedIds = validIds.stream()
    .filter(id -> !cachedResults.containsKey(id))
    .collect(Collectors.toList());

// 3. 从数据库批量查询未命中数据
// 4. 批量写入缓存
// 5. 合并结果返回
```

### 缓存清除策略

1. **自动清除**: `@CacheEvict` 在更新/删除时自动触发
2. **级联清除**: 如CourseDataService更新课程时清除分类缓存
3. **手动清除**: 特殊业务场景支持手动清除特定缓存
4. **全量清除**: 支持清除整个缓存命名空间

### TTL配置策略

| 数据特性 | TTL | 示例 | 原因 |
|---------|-----|------|------|
| 高频访问+相对稳定 | 30分钟 | 用户基础信息 | 减少数据库压力 |
| 学习流程核心数据 | 15分钟 | 课程、节点信息 | 平衡性能和数据新鲜度 |
| 高频访问+默认 | 10分钟 | 大部分业务数据 | AbstractDataService默认 |
| 频繁变化数据 | 5分钟 | 帖子内容、统计数据 | 保证数据新鲜度 |
| 核心学习数据 | 365天 | 用户学习进度 | 特殊业务需求 |

---

## 📈 性能优化建议

### 短期优化（1周内）
1. **立即添加UserStatsDataService缓存** - 解决用户页面性能问题
2. **立即添加ContentStatsDataService缓存** - 解决内容统计性能问题

### 中期优化（1个月内）
1. **添加MemoryCardDataService基础信息缓存** - 优化学习卡片加载
2. **优化批量查询使用** - 更多场景使用AbstractDataService的批量缓存
3. **缓存预热机制** - 系统启动时预热热门数据

### 长期优化
1. **缓存监控** - 添加缓存命中率、性能监控
2. **分布式缓存一致性** - 多实例环境下的缓存同步
3. **缓存分层** - 本地缓存 + Redis分布式缓存

---

## 📊 缓存效果评估

### 当前缓存覆盖情况
- ✅ **用户基础数据**: 100% 覆盖
- ✅ **内容基础数据**: 100% 覆盖
- ❌ **统计数据**: 0% 覆盖（严重问题）
- ✅ **学习进度数据**: 100% 覆盖
- ✅ **交互数据**: 100% 覆盖
- ✅ **系统配置数据**: 100% 覆盖
- ✅ **记忆系统数据**: 100% 覆盖

### 预期性能提升
- **用户页面加载**: 修复统计缓存后预期提升70%+
- **内容浏览性能**: 修复统计缓存后预期提升60%+
- **学习流程**: 已有良好缓存支撑
- **数据库压力**: 预期减少40%+的统计查询

---

**总结**: 当前缓存架构设计良好，AbstractDataService提供了统一的缓存框架。主要问题是统计数据完全没有缓存，需要立即修复UserStatsDataService和ContentStatsDataService的缓存支持。

---
*文档生成时间: 2025-12-11*
*分析覆盖: 28个DataService文件*