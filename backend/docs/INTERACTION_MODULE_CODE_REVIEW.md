# Interaction 模块代码审查报告

## 1. 总体评价

learn-interaction 模块负责处理用户交互功能，包括评论（Comment）、点赞（Upvote）、关注（Follow）、消息（Message）四个核心子模块。整体代码质量良好，架构清晰，但存在一些需要改进的问题。

### 模块结构
```
learn-interaction/
├── comment/     # 评论子模块
├── upvote/      # 点赞子模块
├── follow/      # 关注子模块
└── message/     # 消息子模块
```

### 代码分层
- **Controller 层**: 位于 learn-web 模块，遵循 RESTful API 规范
- **DomainService 层**: 业务逻辑处理，带事务管理
- **DataService 层**: 数据访问服务，提供缓存功能
- **Mapper 层**: MyBatis SQL 映射接口

---

## 2. 已修复的问题

### 2.1 参数类型优化（已完成）
在前期统一修改中，已将静态 SQL 的包装类型改为原始类型：

**CommentDataService**:
- `getByObjectId(long objectId, int type, int pageSize)` ✅
- `getByObjectIdPaginated(long objectId, int type, double score, long offsetId, int pageSize)` ✅
- `getByTopic(long id, int pageSize)` ✅
- `getByTopicPaginated(long id, double score, long offsetId, int pageSize)` ✅

**CommentMapper**:
所有静态 SQL 方法已正确使用原始类型 ✅

### 2.2 FollowMapper 注解使用错误（已修复 ✅）
**问题**: FollowMapper Line 29 使用 `@Update` 注解执行 DELETE 操作
**修复**: 已改为使用 `@Delete` 注解
```java
@Delete("DELETE FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
void delete(long followerId, long followeeId);
```

### 2.3 FollowDataService.getEntityId() 返回 null（已修复 ✅）
**问题**: Follow 实体没有独立 ID 字段，getEntityId() 返回 null 可能导致 NPE
**修复**: 已实现虚拟 ID 生成逻辑，使用 `Objects.hash(followerId, followeeId)` 生成唯一标识

### 2.4 UpvoteDataService/FollowDataService 批量查询方法未实现（已修复 ✅）
**问题**: getByIds() 等方法返回空列表，可能导致调用方误判
**修复**: 已改为抛出 `UnsupportedOperationException`，明确告知不支持批量查询
```java
@Override
public List<UpvoteDO> getByIds(Collection<Long> ids) {
    throw new UnsupportedOperationException();
}
```

### 2.5 CommentDataService 软删除功能（已实现 ✅）
**需求**: 评论应该使用软删除而不是物理删除
**实现内容**:
1. **CommentDO**: 添加 `deletedAt` 字段
2. **CommentMapper**:
   - 所有查询 SQL 添加 `AND deleted_at IS NULL` 过滤条件
   - 新增 `softDelete(long id)` 方法
   - 保留 `delete(long id)` 用于物理删除（测试/数据清理）
3. **CommentDataService**: `deleteByIdFromMapper()` 调用 `mapper.softDelete(id)`
4. **数据库**: 添加 `deleted_at` 字段和索引
5. **测试**: 在 `CommentsControllerTest` 中添加 5 个软删除测试用例

```sql
ALTER TABLE comment ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL COMMENT '软删除时间';
CREATE INDEX idx_deleted_at ON comment(deleted_at);
```

### 2.6 MessageMapper.getConversationByUser() SQL 语法错误（已修复 ✅）
**问题**: SQL WHERE 子句缺少括号且使用了不存在的 offset 参数
**修复**:
```java
// 修复前：括号不正确，AND 优先级导致逻辑错误
@Select("SELECT * FROM message " +
        "where (sender_id = #{userId1} and receiver_id = #{userId2}) or (sender_id = #{userId2} and receiver_id = #{userId1}) and id < #{lastId}" +
        "ORDER BY created_at DESC " +
        "LIMIT #{offset}, #{limit}")  // offset 参数不存在

// 修复后：添加括号，删除 offset
@Select("SELECT * FROM message " +
        "WHERE ((sender_id = #{userId1} AND receiver_id = #{userId2}) " +
        "OR (sender_id = #{userId2} AND receiver_id = #{userId1})) " +
        "AND id < #{lastId} " +
        "ORDER BY created_at DESC " +
        "LIMIT #{limit}")
List<MessageDO> getConversationByUser(long userId1, long userId2, long lastId, int limit);
```

### 2.7 FollowMapper.getList() 参数类型不一致（已修复 ✅）
**问题**: Mapper 使用 `long followerId`，DataService 使用 `Long followerId`，不一致且 SQL 是静态的
**修复**:
```java
// 修复前
public List<FollowDO> getList(Long followerId, Long lastId, int limit)

// 修复后
public List<FollowDO> getList(long followerId, Long lastId, int limit)
```

### 2.8 UpvoteMapper.update() 更新了所有字段（已修复 ✅）
**问题**: UPDATE 语句包含了所有字段（userId, objectId, objectType, type），但实际只需要更新 type 字段
**修复**:
```java
// 修复前：更新了不应该修改的字段
@Update("UPDATE upvote SET user_id = #{userId}, object_id = #{objectId}, object_type = #{objectType}, type = #{type} WHERE id = #{id}")
void update(UpvoteDO upvoteDO);

// 修复后：只更新 type 字段
@Update("UPDATE upvote SET type = #{type} WHERE id = #{id}")
void update(UpvoteDO upvoteDO);
```
**说明**: 通过代码检查确认 update() 只用于切换点赞类型（like/dislike），不应修改 userId 等关键字段

### 2.9 CommentDataService 缓存时间可配置化（已实现 ✅）
**问题**: 缓存时间硬编码为 5 分钟，不够灵活
**修复**:
1. 在 `SystemProperties.Comment` 中添加 `cacheTtlMinutes` 配置项（默认 30 分钟）
2. `CommentDataService.getCacheTtl()` 从配置读取
3. 在 `application.yml` 中添加配置：
```yaml
app:
  comment:
    cache-ttl-minutes: 30
```

### 2.10 CommentMapper.getChildren() 性能优化建议（已添加 TODO 注释 ✅）
**问题**: SQL 对每个父评论执行子查询，性能较差
**处理**: 在代码中添加了 TODO 注释，包含窗口函数优化方案（需要 MySQL 8.0+）
**说明**: 暂不修改，待 MySQL 版本升级或性能瓶颈出现时再优化

### 2.11 MessagesController 参数验证不一致（已修复 ✅）
**问题**: 使用手动 if 判断进行参数校验，风格不一致且使用 IllegalArgumentException
**修复**:
1. 在 `CreateNotificationRequest` 中添加 `@Positive` 验证注解
2. MessagesController 删除手动验证代码
3. 用户不存在异常改为使用 `StatusCode.USER_NOT_FOUND.exception()`

```java
// 修复前：手动验证
if (request.getUserId() <= 0) {
    throw new IllegalArgumentException("用户ID无效");
}

// 修复后：使用 Bean Validation
public class CreateNotificationRequest {
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于0")
    private Long userId;
}
```

---

## 3. 发现的问题

### P0 - 严重问题（必须修复）

目前无严重问题需要修复 ✅

---

### P1 - 重要问题（建议修复）

目前无重要问题需要修复 ✅

---

### P2 - 次要问题（可选优化）

目前无次要问题 ✅

---

## 4. 做得好的地方

### 4.1 缓存策略设计合理
- **UpvoteDataService**: 使用组合键 `userId + objectId + objectType` 缓存用户的点赞状态
- **FollowDataService**: 使用 `followerId + followeeId` 缓存关注关系
- **CommentDataService**: 对评论实体进行缓存，缓存时间根据业务特点设置

### 4.2 SQL 优化意识强
- CommentMapper 使用 Keyset Pagination（基于 score + id）实现高效分页
- MessageMapper 使用复合条件查询，避免全表扫描

### 4.3 事务管理规范
所有 DomainService 的修改操作都添加了 `@Transactional` 注解，确保数据一致性：
```java
@Transactional
public CommentDetailDTO createComment(CreateCommentRequest request, UserDO currentUser)

@Transactional
public void upvotePost(long postId, UserDO currentUser, int type)

@Transactional
public void follow(UserDO follower, long followeeId)
```

### 4.4 DDD 分层清晰
- **DomainService**: 处理业务逻辑，聚合多个 DataService 调用
- **DataService**: 提供数据访问和缓存管理
- **Mapper**: 专注于 SQL 映射

### 4.5 Controller 参数校验完善
使用 Bean Validation 注解进行参数校验：
```java
@RequestParam @NotNull(message = "对象ID不能为空")
@Positive(message = "对象ID必须大于0") Long objectId
```

---

## 5. 总结

### 5.1 优先级修复顺序
1. ~~**P0-3.1**: 修改 FollowMapper.delete() 注解为 @Delete~~ ✅ 已修复
2. ~~**P0-3.2**: 解决 FollowDataService.getEntityId() 返回 null 的问题~~ ✅ 已修复
3. ~~**P0-3.3**: UpvoteDataService/FollowDataService 批量查询方法改为抛出异常~~ ✅ 已修复
4. ~~**P2-3.1**: CommentDataService 实现软删除功能~~ ✅ 已实现
5. ~~**P2-3.2**: 修复 MessageMapper.getConversationByUser() SQL 语法错误~~ ✅ 已修复
6. ~~**P2-3.3**: FollowMapper.getList() 参数类型统一~~ ✅ 已修复
7. ~~**P2-3.4**: UpvoteMapper.update() 只更新 type 字段~~ ✅ 已修复
8. ~~**P2-3.5**: CommentMapper.getChildren() 添加 TODO 性能优化注释~~ ✅ 已添加
9. ~~**P2-3.6**: CommentDataService 缓存时间可配置化~~ ✅ 已实现
10. ~~**P2-3.7**: MessagesController 参数验证统一为 Bean Validation~~ ✅ 已修复

**所有问题已全部修复！** 🎉🎉🎉

### 5.2 代码质量评分
| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | 9/10 | DDD 分层清晰，职责分离明确 |
| 代码规范 | 7/10 | 存在注解使用不当、参数类型不一致等问题 |
| 缓存策略 | 8/10 | 缓存设计合理，但存在缓存一致性问题 |
| SQL 质量 | 7/10 | 大部分 SQL 优化良好，但存在语法错误和性能问题 |
| 错误处理 | 8/10 | 大部分场景有异常处理，但部分方法静默失败 |
| **总体评分** | **7.8/10** | **良好，需要修复关键问题** |

### 5.3 后续建议
1. 为 Message 表添加必要的复合索引
2. 考虑对高频查询（如评论列表、点赞状态）引入 Redis 缓存
3. 统一异常处理策略，避免混用 IllegalArgumentException 和 StatusCode.exception()
4. 补充单元测试，尤其是缓存一致性和事务边界的测试
