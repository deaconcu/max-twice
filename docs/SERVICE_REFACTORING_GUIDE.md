# Service 拆分原则

## 核心原则

### 1. DomainService vs ApplicationService

**DomainService（领域服务）**:
- 只依赖本领域模块（如 interaction 只依赖 interaction）
- 处理领域内的核心业务逻辑
- 返回领域对象（DO）
- 不涉及跨域依赖

**ApplicationService（应用服务）**:
- 可以依赖多个领域模块
- 协调跨领域逻辑
- DTO 转换和填充
- 事件发布
- 返回 DTO 对象

---

## 拆分步骤

### 步骤 1: 识别跨域依赖

检查每个方法的依赖：
- ✅ 只依赖本领域 → 可以放到 DomainService
- ❌ 依赖其他领域 → 必须留在 ApplicationService

### 步骤 2: 方法拆分

对于包含跨域逻辑的方法，按职责拆分：

**示例：创建评论**
```java
// ApplicationService
public CommentDetailDTO createComment(request, user) {
    // 1. 跨域验证（验证 user、post/node/roadmap 是否存在）
    userDataService.validateAndGet(request.getToUser());
    postDataService.validateAndGet(request.getObjectId());

    // 2. 调用 DomainService（领域内验证 + 业务逻辑）
    CommentDO saved = domainService.createComment(...);

    // 3. DTO 转换（填充关联信息）
    return toDTO(saved);
}

// DomainService
public CommentDO createComment(objectId, objectType, ...) {
    // 1. 领域内验证（验证 replyTo 是否存在）
    if (replyToCommentId != null) {
        commentDataService.validateAndGet(replyToCommentId);
    }

    // 2. 构建领域对象
    CommentDO comment = new CommentDO();
    // ... 设置属性

    // 3. 持久化
    commentDataService.insert(comment);
    return commentDataService.getById(comment.getId());
}
```

### 步骤 3: 查询方法拆分

**DomainService 负责**:
- 数据查询逻辑
- 分页逻辑
- 返回 `List<DO>`

**ApplicationService 负责**:
- 调用 DomainService 获取数据
- DTO 转换
- 填充关联信息（点赞状态、用户名等）
- 构建响应对象

---

## 方法分类

### Command（写操作）
- 创建、更新、删除等操作
- ApplicationService: 跨域验证 → DomainService → 事件发布
- DomainService: 领域验证 → 业务逻辑 → 持久化

### Query（读操作）
- 查询操作
- ApplicationService: DomainService → DTO转换 → 填充关联
- DomainService: 数据查询 → 返回 DO

### DTO转换方法
- 全部放在 ApplicationService
- 使用 Converter + 填充关联信息

### Private辅助方法
- 跨域辅助 → ApplicationService
- 领域内辅助 → DomainService

---

## 代码组织

### ApplicationService 文件结构
```java
@Service
public class XxxService {

    // 依赖注入
    private final XxxDomainService domainService;
    private final OtherDataServices...

    // ========== Command 方法（写操作）==========
    // 创建、更新、删除等

    // ========== Query 方法（读操作）==========
    // 各种查询方法

    // ========== DTO 转换方法 ==========
    // DTO 转换和填充

    // ========== Private 辅助方法 ==========
    // 跨域辅助方法
}
```

### DomainService 文件结构
```java
@Service
public class XxxDomainService {

    // 依赖注入（仅本领域）
    private final XxxDataService dataService;

    // ========== Command 方法 ==========
    // 创建、更新、删除

    // ========== Query 方法 ==========
    // 查询方法

    // ========== Private 辅助方法 ==========
    // 领域内辅助方法
}
```

---

## 常见问题

### Q1: 如果方法只有一小部分跨域依赖怎么办？

**答**: 拆分该方法，跨域部分留在 ApplicationService，领域逻辑移到 DomainService。

### Q2: 事件发布应该在哪里？

**答**:
- 如果事件不需要跨域信息 → DomainService
- 如果事件需要跨域信息 → ApplicationService
- 优先保持 DomainService 纯粹

### Q3: DTO 转换方法应该在哪里？

**答**: 全部在 ApplicationService，因为填充关联信息通常需要跨域依赖。

### Q4: 状态转换逻辑应该在哪里？

**答**:
- 状态转换是领域知识 → DomainService
- 如果需要跨域查询来决定状态 → ApplicationService

### Q5: 验证方法应该在哪里？

**答**:
- 领域内验证（如验证 replyTo 评论）→ DomainService
- 跨域验证（如验证 user/post 是否存在）→ ApplicationService
- 使用各 DataService 的 `validateAndGet()` 方法，不要重复实现

---

## 重构清单

拆分现有 Service 时的检查清单：

- [ ] 识别所有跨域依赖
- [ ] 创建 DomainService 文件
- [ ] 移动纯领域方法到 DomainService
- [ ] 拆分混合方法（跨域验证 + 领域逻辑）
- [ ] 清理冗余的验证方法
- [ ] 更新 ApplicationService 调用 DomainService
- [ ] 按分区整理方法顺序（Command/Query/DTO/Private）
- [ ] 验证编译通过
- [ ] 运行测试

---

## 收益

✅ **职责清晰**: 领域逻辑与应用逻辑分离
✅ **易于测试**: DomainService 无跨域依赖，容易测试
✅ **可复用**: DomainService 可被多个 ApplicationService 调用
✅ **符合 DDD**: 领域层不依赖外层
✅ **易于维护**: 代码结构清晰，易于理解和修改
