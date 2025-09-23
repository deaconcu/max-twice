# AutoAuthor 运行设计（Redis ZSET 轮询 + 成功删除 + 失败 10 分钟重试同一节点）

最后更新：2025-09-22

## 目标与范围
- 当“新的节点 Node 被创建”后，后台自动由 AI 生成内容：
  - ARTICLE：可“一文说清楚”，生成 `ARTICLE` 类型 post。
  - DIRECTORY：生成 `CONTENTS`（目录）类型 post，并据此自动创建子节点。
- 归属与状态：由“系统预置 AI 用户（配置项 `ai.autoAuthor.aiUserId`）”创建，post 状态为 `APPROVED`。
- 目录帖内容存储：创建子节点后，将“子节点 ID 数组”作为目录帖 content（JSON 字符串，如 `[1,2,3,4]`）。

## 已确认规则
- 单实例执行器：当前仅运行 1 个执行器实例（运维保障），后续如需再加分布式锁。
- 触发来源：
  - 事件入队：`NodeCreatedEvent`（事务提交后）只负责入队，不直接执行。
  - 兜底扫描：仅“目录节点”范围内做补偿入队（见下文），避免漏网。
- 执行模型（轮询）：
  - 使用 Redis `ZSET` 作为就绪队列；执行器通过 `ZRANGE ready 0 0` 轮询“最早入队项”。
  - 幂等：执行前检查“是否已存在由 AI 用户创建且未删除的 post”；若是则视为完成并从队列删除。
- 失败处理（重要）：
  - 失败时不删除队列项；执行器线程睡眠 10 分钟后，继续对“同一 `nodeId`”重试；直至成功。
  - 不引入“失败集合/延迟集合”；保持设计最简。
- 目录帖不可更新；如需变更，另建新目录帖覆盖（产品策略另定）。

## 总体架构
1) 节点创建完成（事务提交后）发布 `NodeCreatedEvent` → `ZADD NX score=now` 放入 `ready`。
2) 兜底扫描（仅目录节点）找出“缺 AI 目录帖”的节点 → 同样 `ZADD NX` 入 `ready`。
3) 单执行器轮询 `ready` 获取队首 `nodeId` 处理。
4) 成功：生成相应 post（必要时批量创建子节点）并 `ZREM`。
5) 失败：记录日志；执行器线程睡眠 10 分钟后继续对“同一 `nodeId`”重试；不 `ZREM`。

## 仅扫描“目录节点”的约束
- 扫描逻辑仅面向“目录节点”的缺失场景：
  - 即：筛出“该节点不存在由 `aiUserId` 创建、状态非 DELETED 的帖子”的节点。
- 示例 SQL（按现有表结构）：
```sql
SELECT n.id AS node_id
FROM node n
LEFT JOIN post p
  ON p.node_id = n.id
 AND p.creator_id = :AI_USER_ID
 -- 不限定类型
 AND p.state != 2        -- 非 DELETED（Enums.PostState.deleted=2）
WHERE p.id IS NULL;
```

## Redis 设计
- 键前缀：`ai.autoAuthor.redis.keyPrefix = autoAuthor:`（建议）
- 就一个集合：`autoAuthor:ready`（ZSET）
  - 成员：`nodeId`（字符串）
  - 分值（score）：入队时间戳（毫秒/秒，统一即可）
  - 入队：`ZADD autoAuthor:ready NX <score=now> <member=nodeId>`（`NX` 去重）
  - 轮询：`ZRANGE autoAuthor:ready 0 0` 获取队首；成功后 `ZREM`。

## 执行器（单线程/并发=1）
- 轮询：
  - 若 `ZRANGE ready 0 0` 为空 → `sleep(pollIntervalSec)` 后继续。
  - 否则取 `nodeId`：
    - 若存在 AI 目录帖（幂等命中）→ `ZREM nodeId` 并处理下一个。
    - 否则尝试生成：
      - 成功 → `ZREM nodeId`
      - 失败 → `log.error` 并 `sleep(retryDelaySec=600)`，随后继续对同一 `nodeId` 尝试（因未删除，仍处于队首）。
- 说明：单实例下无需竞态保护；未来支持多实例时，可加“处理锁”或“取出标记+超时回滚”。

## 生成流程（目录/文章）
1) 调用本地 opencode：先 POST /session 创建会话，再 POST /session/:id/message 发送 ChatInput 获取决策与内容（providerID/modelID 由配置指定）。
2) 分支落库：
   - ARTICLE：`Post(type=ARTICLE, content=Markdown, creator=AI_USER_ID, state=APPROVED)`。
   - DIRECTORY：为 `children` 依序创建子节点（数量上限 `maxChildrenPerNode`），收集子节点 ID；
     再创建 `Post(type=CONTENTS, content='[id1,id2,...]', creator=AI_USER_ID, state=APPROVED)`。

## 配置项（建议默认值，可调）
- 开关：
  - `ai.autoAuthor.enabled = true`
  - `ai.autoAuthor.aiUserId = <必填>`
- 执行器：
  - `ai.autoAuthor.executor.pollIntervalSec = 2`
  - `ai.autoAuthor.executor.retryDelaySec = 600`  （失败后休眠重试，秒）
  - `ai.autoAuthor.maxChildrenPerNode = 30`
- 扫描计划：
  - `ai.autoAuthor.scan.cron = 0 0 3 * * *`  （每日 03:00）
- Redis：
  - `ai.autoAuthor.redis.keyPrefix = autoAuthor:`

## 管理接口（需登录，基于 Sa-Token）
- `POST /api/admin/auto-author/scan`：触发一次“目录节点”全量补偿扫描（返回入队数量）。
- `POST /api/admin/auto-author/enqueue/{nodeId}`：手动将某节点加入就绪队列。
- 鉴权：使用 Sa-Token（`StpUtil`）要求已登录；更细的“管理员权限”判定依据现有策略在实现时接入。

## 观测与审计
- 队列规模：`ZCARD ready`
- 生成指标：ARTICLE vs DIRECTORY 比例、平均耗时
- 失败指标：连续失败次数、单节点累计耗时
- 审计日志：`nodeId`, `decision`, `childrenCount`（若有）, `error`（失败进入休眠时）

## 伪代码（轮询 + 成功删除 + 失败原地重试）
- 入队（事件/扫描）：
```text
enqueue(nodeId):
  ZADD ready NX now nodeId
```
- 执行器：
```text
while enabled:
  ids = ZRANGE(ready, 0, 0)
  if ids is empty:
    sleep(pollIntervalSec)
    continue
  nodeId = ids[0]
  if existPost(nodeId, AI_USER_ID):
    ZREM(ready, nodeId)
    continue
  try:
    resp = opencode.generate(ctx(nodeId))
    if resp.decision == 'ARTICLE':
      createPost(nodeId, ARTICLE, resp.articleMd, AI_USER_ID, APPROVED)
    else if resp.decision == 'DIRECTORY':
      ids = []
      for child in resp.children.limit(maxChildrenPerNode):
        id = createNode(parent=nodeId, name=child.title)
        ids.append(id)
      createPost(nodeId, CONTENTS, json(ids), AI_USER_ID, APPROVED)
    else:
      throw new Error('unknown decision')
    ZREM(ready, nodeId)  # 成功后删除
  catch err:
    log.error(nodeId, err)
    sleep(retryDelaySec)  # 10 分钟后继续同一 nodeId
```

## 边界与注意点
- 失败阻塞效应：失败会阻塞整个流水线（10 分钟步进重试），符合“失败就暂停，十分钟后再继续”的要求。
- 幂等与去重：
  - 入队使用 `ZADD NX`；执行前再次幂等校验。
  - 扫描/事件可能重复入队，不影响正确性。
- 节点删除：执行前校验节点存在性；不存在则视为成功跳过并 `ZREM`。
- 规模控制：仅限制 `maxChildrenPerNode`，不引入 `maxDepth/maxTotalNodes`。
- 单实例约束：本版默认只有一个执行器实例在运行。

## 后续工作
- Prompt 调优与内容质量评估。
- 观测页面（队列规模、最近生成、失败重试中节点）。
- 如需“失败不中断整体消费”，可演进为“ready/delayed 分离 + 搬运器”策略（非本版范围）。
