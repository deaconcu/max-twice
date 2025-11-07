# 浏览模式公开API实施方案

本文档详细说明了为实现“浏览模式”（即允许未登录游客访问部分内容）而需要进行的后端改造方案。

## 1. 核心目标与原则

- **核心目标**: 创建一套以 `/api/public` 为前缀的公开接口，允许游客访问**职业学习路径 (Roadmap)** 和**帖子内容 (Post)**。
- **核心原则**:
    1.  **分离入口 (Controller)，共享逻辑 (Service)**: 为公开内容创建新的 `Public` Controller，但与现有的私有 Controller 共享同一个 `Service`。
    2.  **明确流量划分**: 公开流量 (`/api/public/**`) 与登录用户流量 (`/api/v1/**`) 在入口处完全分离，便于独立配置限流、监控等策略。
    3.  **最小化代码冗余**: 核心业务逻辑只存在于 `Service` 层，避免复制粘贴和后续的维护噩梦。
    4.  **兼容匿名用户**: 改造 `Service` 层，使其能够优雅地处理 `userId` 为 `null` 的情况，为游客返回无个性化信息（如点赞、收藏状态）的默认数据。

## 2. 总体实施计划

整个实施过程将分阶段进行：

- **【阶段一】**: 职业学习路径 (Roadmap) 功能公开。
- **【阶段二】**: 帖子内容 (Post) 功能公开。
- **【阶段三】**: 重构与清理，确保代码整洁和一致性。

## 3. 详细技术方案

### 阶段一：职业学习路径 (Roadmap) 公开

#### 步骤 1.1: 创建 `PublicRoadmapController`

- **位置**: `backend/learn-api/src/main/java/com/prosper/learn/api/controller/PublicRoadmapController.java`
- **目的**: 提供一个公开的 HTTP 端点，用于获取学习路径详情。该 Controller 不处理任何业务逻辑，仅作为流量入口，并将请求转发给 `RoadmapService`。

- **示例代码**:
  ```java
  @RestController
  @RequestMapping("/api/public/v1/roadmaps")
  @Tag(name = "Public Roadmap API", description = "公开的职业学习路径接口")
  public class PublicRoadmapController {

      @Autowired
      private RoadmapService roadmapService;

      @GetMapping("/{roadmapId}")
      @Operation(summary = "获取指定学习路径的公开详情")
      public RoadmapDetailDTO getRoadmapDetails(@PathVariable Long roadmapId) {
          // 关键：调用 service 时，显式传入 null 作为 userId，表示游客访问
          return roadmapService.getRoadmapDetails(roadmapId, null);
      }
  }
  ```

#### 步骤 1.2: 改造 `RoadmapService`

- **位置**: `.../domain/service/business/RoadmapService.java`
- **目的**: 修改核心业务方法，使其能够处理 `userId` 为 `null` 的情况。

- **改造要点**:
    - 将 `getRoadmapDetails(Long roadmapId, Long userId)` 方法的 `userId` 参数标记为 `@Nullable`。
    - 在所有依赖 `userId` 进行个性化查询（如查询节点完成状态 `UserNodeProgress`）的逻辑块前，增加 `if (userId != null)` 的判断。
    - 如果 `userId` 为 `null`，则直接跳过个性化查询，并在返回的 DTO 中使用默认值（例如，节点的 `completed` 状态为 `false`）。

- **示例代码 (改造后)**:
  ```java
  public RoadmapDetailDTO getRoadmapDetails(Long roadmapId, @Nullable Long userId) {
      // ... 获取路线图基本信息 ...
      List<Node> nodes = nodeRepository.findByRoadmapId(roadmapId);

      List<NodeDTO> nodeDTOs = nodes.stream().map(node -> {
          UserNodeProgress progress = null;
          // 只在 userId 存在时才查询进度
          if (userId != null) {
              progress = progressRepository.findByUserIdAndNodeId(userId, node.getId());
          }
          // toNodeDTO 方法需要被重构，以处理 progress 为 null 的情况
          return toNodeDTO(node, progress);
      }).collect(Collectors.toList());

      // ... 封装并返回 DTO
  }
  ```

#### 步骤 1.3: 配置 Spring Security

- **位置**: `SecurityConfig.java` (或项目中的Spring Security配置文件)
- **目的**: 允许对所有 `/api/public/**` 路径的 `GET` 请求进行匿名访问。

- **示例配置**:
  ```java
  @Override
  protected void configure(HttpSecurity http) throws Exception {
      http
          // ... CSRF, CORS 等其他配置 ...
          .authorizeRequests()
              // 新增规则：允许对 /api/public/ 开头的 GET 请求进行匿名访问
              .antMatchers(HttpMethod.GET, "/api/public/**").permitAll()
              // 保留原有规则
              .antMatchers("/api/v1/**").authenticated()
              // 其他路径规则...
              .anyRequest().authenticated();
          // ...
  }
  ```

---

### 阶段二：帖子内容 (Post) 公开

此阶段的改造与阶段一完全一致，仅作用于 `Post` 相关实体。

1.  **创建 `PublicPostController`**:
    - URL: `/api/public/v1/posts/{postId}`
    - 调用 `postService.getPostDetails(postId, null)`。

2.  **改造 `PostService`**:
    - 在获取帖子详情的业务逻辑中，对 `userId` 进行 `null` 检查。
    - 当 `userId` 为 `null` 时，返回的 `PostDTO` 中 `isLiked`, `isFavorited` 等个性化字段应设为 `false`。

3.  **安全配置**:
    - 无需额外修改。阶段一中添加的 `.antMatchers(HttpMethod.GET, "/api/public/**").permitAll()` 规则已覆盖此场景。

## 4. 风险与前端影响

- **风险**: `Service` 层的改造是核心，也是风险点。必须确保所有依赖 `userId` 的代码路径都被正确处理，避免 `NullPointerException`。
- **前端影响**: 前端团队可以使用新的 `/api/public/...` 接口来安全地展示公开内容。当用户需要执行写操作（如点赞、评论）时，前端需引导用户登录，然后调用原有的、需要认证的 `/api/v1/...` 接口。
