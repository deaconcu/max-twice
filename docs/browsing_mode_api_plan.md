# 浏览模式公开API实施方案 (V6 - 生产强化版)

本文档根据前端代码分析及多轮详细评审意见，制定了实现"浏览模式"的最终可执行后端改造方案。

## 实施进度 Checklist

### 阶段一：职业详情页公开API
- [x] 1.1 改造 ProfessionService.getById() 支持 nullable userId（已有方法无需userId）
- [x] 1.2 在 PublicController 添加 GET /api/v1/public/professions/{id}
- [x] 1.3 改造 RoadmapService.getRoadmapsByProfession() 支持 nullable userId（新增getRoadmapsByProfessionPublic）
- [x] 1.4 在 PublicController 添加 GET /api/v1/public/professions/{id}/roadmaps

### 阶段二：阅读页公开API
- [x] 2.1 改造 PageService.readPageByPath() 支持 nullable userId（新增readPageByPathPublic）
- [x] 2.2 改造 PageService.readPageByNode() 支持 nullable userId（暂不需要）
- [x] 2.3 改造 PageService.readPageByPost() 支持 nullable userId（暂不需要）
- [x] 2.4 改造 PageService.readPageByComment() 支持 nullable userId（暂不需要）
- [x] 2.5 在 PublicController 添加 GET /api/v1/public/pages/read（已完成并连接Service）

### 阶段三：安全与性能配置
- [x] 3.1 配置 SaToken 允许 /api/v1/public/** 匿名访问（已配置在 AppConfiguration.java:97）
- [x] 3.2 实施限流策略（已在 PublicController 类级别添加 @RateLimit，每IP每分钟50次）
- [x] 3.3 配置 Redis 缓存策略（已在 CacheConfig.java 配置，dataService层自动缓存）
- [x] 3.4 配置 CORS 策略（已配置在 AppConfiguration.java:54-59）

### 阶段四：测试与验证
- [ ] 4.1 编写 Service 层单元测试（userId=null 场景）
- [ ] 4.2 编写公开 API 集成测试（无 Token 访问）
- [ ] 4.3 验证私有 API 功能不受影响
- [ ] 4.4 手工测试游客模式和登录模式

---

## 1. 核心目标与原则

- **核心目标**: 为前端的三个只读页面 (`/profession/{id}`, `/roadmap/{id}`, `/read?courseId=...`) 所依赖的所有后端API创建公开的、无需登录的版本。
- **核心原则**:
    1.  **分离入口，共享逻辑**: 为私有API创建对应的公共版API (`/api/public/...`)，但共享同一个服务层逻辑。
    2.  **精确对应**: 公开API严格根据前端 `apiServiceV1.js` 文件中的真实定义来创建。
    3.  **兼容匿名用户**: 改造 `Service` 层，使其能优雅地处理 `userId` 为 `null` 的情况，为游客返回无个性化信息的默认数据。
    4.  **向后兼容**: 所有改造不影响现有 `/api/v1/**` 私有接口的任何行为。

### 1.1 兼容性保证

-   **私有API完全不受影响**:
    -   `/api/v1/professions/{id}` 等私有接口的行为、性能、认证和授权逻辑保持不变。
    -   所有现有的单元测试和集成测试应全部通过。
-   **Service层改造向后兼容**:
    -   例如，`getProfession(Long id, Long userId)` 改为 `getProfession(Long id, @Nullable Long userId)`。
    -   当 `userId` 不为 `null` 时，其内部逻辑与行为将与之前完全一致。这意味着调用此Service的现有私有Controller无需任何修改。

## 2. 待公开的API列表 (最终分析结果)

1.  **职业信息**: `GET /api/v1/professions/{id}`
2.  **路线图列表**: `GET /api/v1/professions/{id}/roadmaps`
3.  **阅读页聚合内容**: `GET /api/v1/pages/read`

---

## 3. 详细实施计划

### 3.1 核心实现逻辑示例 (Service层改造)

所有Service层的改造都遵循以下模式：方法接受一个 `@Nullable Long userId` 参数，并在查询个性化数据前进行null检查。

```java
// 以 ProfessionService 为例
public ProfessionDTO getProfession(Long id, @Nullable Long userId) {
    Profession profession = professionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("职业不存在"));

    ProfessionDTO dto = toDTO(profession); // toDTO 只转换基础信息

    // 只有登录用户才加载个性化数据
    if (userId != null) {
        dto.setSubscribed(subscriptionRepository.existsByUserIdAndCourseId(userId, id));
    } else {
        // 匿名用户设置默认值
        dto.setSubscribed(false);
    }
    return dto;
}
```

### 3.2 阶段一：公开职业详情页 (`/profession/{id}`)

#### **任务1.1: 公开 `GET /api/v1/professions/{id}`**
-   **后端改造**:
    1.  **创建Controller**: `PublicProfessionController.java`。
    2.  **添加方法**: URL为 `GET /api/public/v1/professions/{id}`，内部调用 `professionService.getProfession(id, null)`。
    3.  **改造Service**: 按 `3.1` 示例改造 `ProfessionService` 的 `getProfession` 方法。

#### **任务1.2: 公开 `GET /api/v1/professions/{id}/roadmaps`**
-   **后端改造**:
    1.  **添加方法** (于 `PublicProfessionController`):
        -   **URL**: `GET /api/public/v1/professions/{id}/roadmaps`
        -   **参数**: `@RequestParam(required = false, defaultValue = "0") Long lastId`, `@RequestParam(required = false, defaultValue = "20") Integer pageSize`
        -   **调用**: `roadmapService.getRoadmapsByProfession(id, lastId, pageSize, null)`。
    2.  **改造Service**: 改造 `RoadmapService` 的 `getRoadmapsByProfession` 方法，使其 `currentUser` 参数可为 `null`。

### 3.3 阶段二：公开阅读页 (`/read`)

#### **任务2.1: 公开 `GET /api/v1/pages/read`**
-   **后端改造**:
    1.  **创建Controller**: `PublicPageController.java`。
    2.  **添加方法**: 添加一个统一的 `read` 方法。
        -   **URL**: `GET /api/public/v1/pages/read`
        -   **参数**: `@RequestParam(required = false) Long courseId`, `@RequestParam(required = false) String path`, `@RequestParam(required = false) Long nodeId`, `@RequestParam(required = false) Long postId`, `@RequestParam(required = false) Long commentId`
        -   **参数逻辑说明**: 至少需要提供 `courseId`+`path`, `nodeId`, `postId`, `commentId` 四组参数中的一组，否则应返回 `400 Bad Request`。
    3.  **调用Service**: 根据传入的参数，调用 `pageService` 中对应的 `readByXXX(..., null)` 方法。
    4.  **改造Service**: 改造 `PageService` 中所有 `readBy...` 方法，使其 `userId` 参数可为 `null`。

### 3.4 阶段三：全局配置

#### **任务3.1: 配置 Spring Security**
-   在 `SecurityConfig.java` 中，允许对 `/api/public/**` 的匿名GET请求。

---

## 4. 安全与性能考虑

### 4.1 限流策略 (Rate Limiting)
-   **策略**: 对 `/api/public/**` 路径下的所有接口实施基于IP的限流。
-   **建议阈值**: 每IP每分钟60次请求。
-   **实现**: 使用 `Bucket4j` 库结合拦截器实现。
-   **限流响应**: 返回 `429 Too Many Requests`，响应体: `{"code": 429, "message": "请求过于频繁,请稍后再试", "retryAfter": 60}`。

### 4.2 数据脱敏 (Data Desensitization)
-   在公开API的响应中，**绝不**包含用户邮箱、手机号、IP地址等敏感信息。

### 4.3 跨域资源共享 (CORS)
-   调整CORS配置，确保 `/api/public/**` 路径允许来自前端域名的 `GET` 请求。

### 4.4 缓存策略 (Caching)
-   **策略**: 对公开API的响应内容启用缓存，减少数据库压力。
-   **实现**: 在Controller方法上使用 `@Cacheable` 注解。
-   **缓存Key示例**: `public:profession:{id}`, `public:roadmaps:{professionId}:{lastId}:{pageSize}`
-   **缓存过期时间**: 建议 5-10 分钟。
-   **存储**: 使用 Redis。

### 4.5 错误处理规范
-   **资源不存在**: 当请求的资源不存在时，返回 `404 Not Found`。响应体: `{"code": 404, "message": "资源不存在"}`。
-   **资源已删除/隐藏**: 对公开API不可见，统一返回 `404 Not Found`，避免泄露资源存在历史。
-   **参数验证失败**: 返回 `400 Bad Request`。响应体: `{"code": 400, "message": "参数错误", "details": ["参数 'xx' 不能为空"]}`。

### 4.6 API 响应格式示例
#### 成功响应 (200 OK)
```json
{
  "code": 200,
  "data": {
    "id": 123,
    "name": "Java开发工程师",
    "description": "...",
    "subscribed": false
  }
}
```

### 4.7 数据库查询优化
-   对匿名用户，应在 `Service` 或 `Repository` 层避免执行 JOIN 用户相关表的查询。
-   确保用于分页和筛选的字段（如 `professionId`, `lastId`）已建立数据库索引。

---

## 5. 测试与验证计划

### 5.1 单元测试
-   为每个被改造的 `Service` 方法添加单元测试，验证当 `userId` 为 `null` 时方法的正确性。

### 5.2 集成测试
-   **公开API**: 编写测试，在不带认证Token的情况下请求公开API，断言HTTP状态码为200，且返回数据不含个性化信息。
-   **私有API**: 确保现有私有API的集成测试全部通过。

### 5.3 手工测试 Checklist
-   [ ] **游客访问**: 在浏览器隐身模式下，访问相关页面，确认内容正常展示。
-   [ ] **登录后访问**: 登录账户后，访问页面，确认能看到个性化信息。
-   [ ] **功能切换**: 游客模式下，点击需登录操作的按钮，应弹出登录提示。

### 5.4 代码审查 Checklist
-   [ ] 所有 Service 方法的 `userId` 参数都添加了 `@Nullable` 注解。
-   [ ] 所有 Service 方法都正确处理了 `userId == null` 的情况。
-   [ ] 个性化字段在匿名情况下都有正确的默认值。
-   [ ] 没有在匿名情况下查询用户相关表。
-   [ ] DTO 中没有包含敏感信息 (邮箱、手机、IP等)。
-   [ ] Controller 层正确处理了所有可能的参数组合和校验。
-   [ ] `SecurityConfig` 中的规则顺序正确 (`permitAll` 在 `authenticated` 之前)。
-   [ ] 单元测试覆盖了 `userId=null` 的场景。
-   [ ] 集成测试覆盖了不带Token的公开API调用。

---

## 6. 部署与回滚策略

### 6.1 部署顺序
1.  **部署后端** -> 2. **验证API** -> 3. **部署前端**

### 6.2 回滚方案
-   **紧急关闭**: 通过修改 `SecurityConfig` 可快速熔断所有公开访问。
-   **代码回滚**: 后端回滚不影响现有功能，因为改造是向后兼容的。

---

## 7. 监控与日志

### 7.1 关键指标监控
-   公开API的QPS、响应时间、错误率 (4xx/5xx)。
-   限流触发次数。

### 7.2 日志记录
-   记录所有公开API的访问日志 (IP, 路径, 参数, 响应码)。
-   记录限流触发事件。
