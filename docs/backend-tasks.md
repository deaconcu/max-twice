# 后台任务文档

本文档列出系统中所有的定时任务、队列任务、异步任务和事件监听器。

---

## 一、定时任务 (@Scheduled)

### 1. 统计数据同步
| 项目 | 说明 |
|------|------|
| **任务名称** | 每日统计数据同步 |
| **执行时间** | 每天凌晨 2:30 |
| **入口文件** | `learn-web/.../Scheduler.java` |
| **实际执行** | `StatsSyncScheduler.syncYesterdayStats()` |
| **功能** | 将 Redis 中昨天的统计数据（浏览、点赞、评论）同步到数据库 |
| **语言分站** | ✅ 已支持 `forEachLanguage()` |
| **日志关键词** | `同步昨天统计数据`、`用户统计`、`内容统计` |

### 2. 角色排名同步
| 项目 | 说明 |
|------|------|
| **任务名称** | 角色统计数据同步到 Redis |
| **执行时间** | 每小时第 15 分钟 |
| **入口文件** | `learn-analytics/.../RoleRankingScheduler.java` |
| **功能** | 从数据库读取角色学习人数，同步到 Redis 用于排行榜 |
| **语言分站** | ✅ 已支持 `forEachLanguage()` |
| **日志关键词** | `开始同步角色统计数据`、`角色统计数据同步完成` |

### 3. 角色排名初始化
| 项目 | 说明 |
|------|------|
| **任务名称** | 应用启动时初始化角色统计 |
| **执行时间** | 应用启动后 10 秒 |
| **入口文件** | `learn-analytics/.../RoleRankingScheduler.java` |
| **功能** | 启动时执行一次角色统计同步 |
| **语言分站** | ✅ 已支持（调用 syncRoleStats）|

### 4. Redis 健康检查
| 项目 | 说明 |
|------|------|
| **任务名称** | Redis 连接健康检查 |
| **执行时间** | 每 1 分钟 |
| **入口文件** | `learn-analytics/.../StatsMonitorService.java` |
| **功能** | 检查 Redis 连接是否正常 |
| **语言分站** | 不需要（Redis 共享）|
| **日志关键词** | `Redis连接恢复正常`、`Redis连接异常` |

### 5. Redis 内存监控
| 项目 | 说明 |
|------|------|
| **任务名称** | Redis 内存使用监控 |
| **执行时间** | 每 1 小时 |
| **入口文件** | `learn-analytics/.../StatsMonitorService.java` |
| **功能** | 监控 Redis 内存使用，检查待同步数据量 |
| **语言分站** | 不需要 |
| **日志关键词** | `Redis内存使用情况`、`待同步的统计数据过多` |

### 6. 同步状态检查
| 项目 | 说明 |
|------|------|
| **任务名称** | 每日同步状态检查 |
| **执行时间** | 每天早上 8:00 |
| **入口文件** | `learn-analytics/.../StatsMonitorService.java` |
| **功能** | 检查昨天的数据是否同步完成，未完成则告警 |
| **语言分站** | ✅ 已支持 `forEachLanguage()` |
| **日志关键词** | `发现昨天的数据未同步完成`、`昨天的数据同步正常` |

### 7. AI 内容生成轮询
| 项目 | 说明 |
|------|------|
| **任务名称** | Robot 队列轮询执行器 |
| **执行时间** | 固定延迟（配置 `robot.pollIntervalSec`）|
| **入口文件** | `learn-application/.../RobotExecutor.java` |
| **功能** | 从 Redis 队列取任务，调用 AI 生成节点内容/记忆卡片 |
| **语言分站** | ✅ 已支持 `forEachLanguage()` |
| **日志关键词** | `Robot 轮询开始`、`Robot 轮询结束`、`节点内容生成成功` |

### 8. Robot 扫描器（已禁用）
| 项目 | 说明 |
|------|------|
| **任务名称** | 缺失内容扫描入队 |
| **执行时间** | 已注释禁用 |
| **入口文件** | `learn-application/.../RobotScanner.java` |
| **功能** | 扫描缺少 AI 内容的节点，加入队列 |
| **状态** | ⚠️ `@Scheduled` 已注释 |

---

## 二、应用生命周期事件

### 1. Meilisearch 初始化
| 项目 | 说明 |
|------|------|
| **触发时机** | 应用启动完成后 (ApplicationReadyEvent) |
| **入口文件** | `learn-application/.../MeilisearchInitListener.java` |
| **功能** | 初始化 Meilisearch 搜索索引 |
| **语言分站** | ✅ 内部使用 `forEachLanguage()` 创建各语言索引 |

### 2. Milvus 初始化
| 项目 | 说明 |
|------|------|
| **触发时机** | Bean 初始化 (@PostConstruct) |
| **入口文件** | `learn-infrastructure/.../MilvusService.java` |
| **功能** | 初始化 Milvus 向量数据库 Collection |
| **语言分站** | ✅ 为每种语言创建 Collection |

### 3. 应用关闭同步
| 项目 | 说明 |
|------|------|
| **触发时机** | 应用关闭时 (ContextClosedEvent) |
| **入口文件** | `learn-analytics/.../StatsMonitorService.java` |
| **功能** | 强制同步 Redis 中剩余的统计数据到数据库 |
| **语言分站** | ✅ 已支持 `forEachLanguage()` |

---

## 三、异步任务 (@Async)

### 1. 节点向量生成
| 项目 | 说明 |
|------|------|
| **方法** | `NodeEmbeddingService.upsertAsync()` |
| **触发** | 节点创建/更新/审核通过时 |
| **功能** | 生成节点文本的 Embedding 向量，存入 Milvus |
| **语言分站** | ✅ 传递 `language` 参数 |

### 2. 节点向量删除
| 项目 | 说明 |
|------|------|
| **方法** | `NodeEmbeddingService.deleteAsync()` |
| **触发** | 节点被拒绝/封禁时 |
| **功能** | 从 Milvus 删除节点向量 |
| **语言分站** | ✅ 传递 `language` 参数 |

### 3. Meilisearch 索引更新
| 项目 | 说明 |
|------|------|
| **方法** | `MeilisearchService.indexCourse/indexNode/indexRole/indexUser()` |
| **触发** | 内容审核通过/更新/删除时 |
| **功能** | 更新 Meilisearch 搜索索引 |
| **语言分站** | ✅ 业务索引传递 `language` 参数，用户索引共享 |

### 4. 路线图 AI 生成
| 项目 | 说明 |
|------|------|
| **方法** | `RoadmapGenerationService.generateRoadmapAsync()` |
| **触发** | 管理员手动触发 |
| **功能** | 调用 AI 生成学习路线图 |
| **语言分站** | ✅ 传递 `language` 参数 |

### 5. 通用异步任务
| 项目 | 说明 |
|------|------|
| **方法** | `AsyncTaskService.runAsyncWithProgress()` |
| **触发** | 各种需要进度反馈的长时任务 |
| **功能** | 包装异步任务，支持进度查询 |
| **语言分站** | ✅ 传递 `language` 参数 |

### 6. 邮件发送
| 项目 | 说明 |
|------|------|
| **方法** | `EmailService.sendVerificationEmailAsync()` |
| **触发** | 用户注册时 |
| **功能** | 异步发送验证码邮件 |
| **语言分站** | 不需要（用户共享）|

### 7. 错误日志记录
| 项目 | 说明 |
|------|------|
| **方法** | `ErrorLogService.recordBackendError/recordFrontendError()` |
| **触发** | 异常发生时 |
| **功能** | 异步记录错误到数据库 |
| **语言分站** | ⚠️ 跟随当前请求的语言上下文 |

---

## 四、事件监听器 (@EventListener)

### 1. 节点 Embedding 监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-application/.../NodeEmbeddingListener.java` |
| **监听事件** | `NodeCreatedEvent`, `NodeUpdatedEvent` |
| **功能** | 节点创建/更新时生成向量 |
| **异步** | ✅ @Async |
| **语言分站** | ✅ 从事件获取 `language` |

### 2. 分数计算监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-application/.../ScoreEventListener.java` |
| **监听事件** | 点赞、评论等事件 |
| **功能** | 重新计算帖子/评论/路线图/卡片组分数 |
| **异步** | ❌ 同步执行（@Async 已注释）|
| **语言分站** | ✅ 继承请求线程上下文 |

### 3. 消息通知监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-application/.../MessageEventListener.java` |
| **监听事件** | 点赞、评论、关注、审核等事件 |
| **功能** | 创建站内消息通知 |
| **异步** | ❌ 同步执行（@Async 已注释）|
| **语言分站** | ✅ 继承请求线程上下文 |

### 4. 用户统计监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-analytics/.../UserStatsEventListener.java` |
| **监听事件** | 关注、收藏、学习、复习等事件 |
| **功能** | 更新用户统计数据（直接写数据库）|
| **异步** | ❌ 同步执行 |
| **语言分站** | ✅ 继承请求线程上下文 |

### 5. 内容统计监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-analytics/.../ContentStatsEventListener.java` |
| **监听事件** | 收藏、学习开始/完成等事件 |
| **功能** | 更新内容统计数据（收藏数、学习人数等）|
| **异步** | ❌ 同步执行 |
| **语言分站** | ✅ 继承请求线程上下文 |

### 6. Redis 统计监听器
| 项目 | 说明 |
|------|------|
| **文件** | `learn-analytics/.../RedisStatsEventListener.java` |
| **监听事件** | 浏览、点赞、评论等事件 |
| **功能** | 更新 Redis 实时统计（写 Redis，定时同步到数据库）|
| **异步** | ❌ 同步执行 |
| **语言分站** | ✅ Redis key 使用 `RedisKeyPrefix.prefix()` |

---

## 五、队列任务

### 1. Robot 任务队列
| 项目 | 说明 |
|------|------|
| **队列存储** | Redis ZSET |
| **入口** | `RobotQueueService` |
| **执行器** | `RobotExecutor.poll()` |
| **任务类型** | `N:{nodeId}:...`（节点内容）、`C:{postId}`（记忆卡片）|
| **语言分站** | ✅ Redis key 使用 `RedisKeyPrefix.prefix()` |

---

## 六、待修复/待优化的问题

### ⚠️ 需要关注的问题

1. **ScoreEventListener 和 MessageEventListener 的 @Async 被注释**
   - 位置：`learn-application/.../ScoreEventListener.java`、`MessageEventListener.java`
   - 现状：所有方法的 `@Async` 都被注释掉了，同步执行
   - 影响：可能影响请求响应时间
   - 建议：确认是否需要恢复异步执行

2. **RobotScanner 定时任务被禁用**
   - 位置：`learn-application/.../RobotScanner.java:80`
   - 现状：`@Scheduled` 被注释
   - 建议：确认是否需要启用自动扫描

3. **错误日志分库问题**
   - 现状：错误日志跟随请求的语言上下文存入对应数据库
   - 问题：管理后台需要切换语言才能查看不同站的错误
   - 已解决：✅ 管理后台已添加语言站切换功能

---

## 七、配置项

相关配置在 `application.yml` 中：

```yaml
app:
  scheduler:
    enable-role-ranking-sync: true
    enable-role-ranking-startup-init: true
  stats-monitor:
    enable-health-monitor: true
    enable-memory-monitor: true
    enable-sync-status-check: true
    enable-shutdown-sync: true
    pending-data-threshold: 10000
  robot:
    enabled: true
    poll-interval-sec: 60
    ai-user-id: 1
```

---

## 八、日志查看

所有任务的日志都通过 Slf4j 输出，可通过以下关键词搜索：

| 任务 | 日志关键词 |
|------|-----------|
| 统计同步 | `同步昨天统计数据`、`用户统计`、`内容统计` |
| 角色排名 | `角色统计数据同步`、`更新了 X 个角色` |
| Redis 监控 | `Redis连接`、`Redis内存` |
| Robot | `Robot 轮询`、`节点内容生成`、`记忆卡片生成` |
| Meilisearch | `Meilisearch 索引`、`Meilisearch 同步` |
| Milvus | `Milvus`、`节点向量` |
