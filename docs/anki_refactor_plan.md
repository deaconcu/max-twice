# 《复习算法 Anki 化改造方案》

**版本**: 1.1
**日期**: 2025-10-30

### 1. 目标

本次改造旨在将系统当前基于 SM-2 的复习算法，升级为一套参考 Anki 核心机制的、更现代化、更高效的间隔重复系统。新算法将引入“卡片状态”和“学习/重新学习”步骤，以提供更精细、更人性化的用户复习体验。

### 2. 核心概念引入

新算法将围绕 Anki 的三个核心概念构建：

1.  **卡片状态 (Card State)**: 每张用户卡片都将被赋予一个明确的状态，决定了它遵循的算法逻辑。
    -   **新卡片 (NEW)**: 从未学习过的卡片。
    -   **学习中 (LEARNING)**: 正在进行短期（分钟/小时级）重复学习的新卡片。
    -   **复习 (REVIEW)**: 已“毕业”并进入长期（天/月级）记忆轨道的卡片。
    -   **重新学习 (RELEARNING)**: 在复习中遗忘（答错）的卡片，暂时退回到短期学习状态。

2.  **学习步骤 (Learning Steps)**: 为“学习中”和“重新学习”状态的卡片设定的一系列**分钟级**的复习间隔。目标是在短期内快速建立初步记忆。

3.  **遗忘处理 (Lapse Handling)**: 当一张处于"复习"状态的卡片被遗忘时，系统将执行一套专门的"重新学习"流程，而不是简单地重置间隔。

#### 2.1. 卡片状态转换图

```
                                    ┌─────────────────────────────────────┐
                                    │         NEW (新卡片)                 │
                                    │         type = 0                    │
                                    └──────────────┬──────────────────────┘
                                                   │
                        ┌──────────────────────────┼──────────────────────────┐
                        │ 评级 1/2                 │ 评级 3               │ 评级 4
                        │ (重来/困难)              │ (良好)               │ (简单)
                        ▼                          ▼                          ▼
           ┌─────────────────────────┐  ┌─────────────────────────┐  ┌──────────────────┐
           │   LEARNING (学习中)      │  │   LEARNING (学习中)      │  │  REVIEW (复习)   │
           │   type = 1               │  │   type = 1               │  │  type = 2        │
           │   step = 0               │  │   step = 1 或毕业        │  │  interval = 4天  │
           │   interval = 10分钟      │  │   interval = 60分钟/1天  │  └──────────────────┘
           └────────┬─────────────────┘  └────────┬─────────────────┘
                    │                              │
                    │ 评级 1: 回到 step=0          │ 评级 2: 当前步骤重复
                    │ 评级 2: 当前步骤延长         │ 评级 3: step++
                    │ 评级 3: step++               │ 评级 4: 立即毕业
                    │ 评级 4: 立即毕业             │
                    │                              │
                    └──────────────┬───────────────┘
                                   │ 完成所有步骤
                                   ▼
                    ┌──────────────────────────────────────┐
                    │        REVIEW (复习)                  │
                    │        type = 2                       │
                    │        interval = 天级 (1, 2, 6...)   │
                    └────────┬─────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │ 评级 2/3/4   │              │ 评级 1 (遗忘)
              │ (继续复习)   │              │
              ▼              ▼              ▼
         间隔增长        间隔增长       ┌────────────────────────┐
         EF 降低        EF 不变        │ RELEARNING (重新学习)   │
         或 EF 增加                     │ type = 3                │
                                        │ step = 0                │
                                        │ interval = 20分钟       │
                                        │ lapseCount++            │
                                        └────────┬────────────────┘
                                                 │
                                                 │ 评级处理逻辑同 LEARNING
                                                 │ 完成重新学习步骤后
                                                 ▼
                                        ┌────────────────────────┐
                                        │   REVIEW (复习)         │
                                        │   type = 2              │
                                        │   回到复习轨道          │
                                        └─────────────────────────┘
```

**状态转换规则总结:**

| 当前状态 | 评级 | 目标状态 | 关键变化 |
| :--- | :---: | :--- | :--- |
| **NEW** | 1/2 | LEARNING (step=0) | 开始学习流程 |
| **NEW** | 3 | LEARNING (step=1) 或 REVIEW | 快速推进或直接毕业 |
| **NEW** | 4 | REVIEW | 立即毕业，间隔=4天 |
| **LEARNING** | 1 | LEARNING (step=0) | 重置到第一步 |
| **LEARNING** | 2 | LEARNING (当前step) | 延长当前步骤 |
| **LEARNING** | 3 | LEARNING (step++) 或 REVIEW | 推进步骤或毕业 |
| **LEARNING** | 4 | REVIEW | 立即毕业，间隔=4天 |
| **REVIEW** | 1 | RELEARNING (step=0) | 遗忘，lapseCount++ |
| **REVIEW** | 2/3/4 | REVIEW | 继续复习，调整间隔和EF |
| **RELEARNING** | 1 | RELEARNING (step=0) | 重置到第一步 |
| **RELEARNING** | 2/3 | RELEARNING (step++) 或 REVIEW | 推进步骤或重新毕业 |
| **RELEARNING** | 4 | REVIEW | 立即重新毕业 |

### 3. 方案详解：修改内容

#### 3.1. 数据库 Schema 变更 (`UserCardSrsDO`)

为了支持新的算法逻辑，我们需要对 `user_card_srs` 表进行以下修改：

1.  **新增 `type` 字段**: `ALTER TABLE user_card_srs ADD COLUMN type TINYINT NOT NULL DEFAULT 0;`
    -   **用途**: 存储卡片的当前生命周期状态。
    -   **值定义**:
        -   `0`: **新卡片 (NEW)**
        -   `1`: **学习中 (LEARNING)**
        -   `2`: **复习 (REVIEW)**
        -   `3`: **重新学习中 (RELEARNING)**

2.  **新增 `current_step` 字段**: `ALTER TABLE user_card_srs ADD COLUMN current_step TINYINT NOT NULL DEFAULT 0;`
    -   **用途**: 记录卡片在“学习中”或“重新学习中”状态时，所处的步骤索引。
    -   **值定义**:
        -   `0`, `1`, `2`...: 对应于配置中 `learningSteps` 或 `relearningSteps` 数组的索引。
        -   当卡片不处于 `LEARNING` 或 `RELEARNING` 状态时，此字段无意义，应为 `0`。

3.  **重命名 `interval_days` 字段**: `ALTER TABLE user_card_srs RENAME COLUMN interval_days TO interval;`
    -   **用途**: 存储复习间隔，其单位由 `type` 字段决定。
    -   **单位**:
        -   `type=1` (LEARNING): 单位是**分钟**。
        -   `type=2` (REVIEW): 单位是**天**。
        -   `type=3` (RELEARNING): 单位是**分钟**。

4.  **新增 `lapse_old_interval` 字段**: `ALTER TABLE user_card_srs ADD COLUMN lapse_old_interval INT NULL;`
    - **单位固定为天**: 无论 `interval` 字段的单位如何变化，此字段始终保存天数。
    -   **用途**: 保存卡片遗忘时的原始间隔（天），用于计算重新毕业后的恢复间隔。
    -   **使用时机**:
        -   当 `REVIEW` 状态卡片被评为"重来"(1)时，进入 `RELEARNING` 前保存当前 `interval` 值到此字段。
        -   `RELEARNING` 完成毕业时，使用此值计算新间隔: `MAX(graduatingInterval, ⌊lapse_old_interval × newIntervalMultiplier⌋)`。
        -   毕业后将此字段清空 (设为 `NULL`)。
    -   **其他状态**: `NEW`、`LEARNING`、`REVIEW` 状态时此字段为 `NULL`。

#### 3.2. 后端逻辑重构 (`ReviewService.java`)

`submitReview` 方法的逻辑将完全重构，引入状态机处理机制。

**`submitReview(userId, request)` 的新逻辑:**
1.  获取卡片当前的 `UserCardSrsDO` 状态。
2.  根据卡片的 `type` 字段，将处理逻辑分发到对应的方法：
    -   `type=0 (NEW)` -> `handleNewCard()`
    -   `type=1 (LEARNING)` -> `handleLearningCard(steps = learningSteps)`
    -   `type=2 (REVIEW)` -> `handleReviewCard()`
    -   `type=3 (RELEARNING)` -> `handleLearningCard(steps = relearningSteps)` (复用学习逻辑，但传入不同的步骤配置)

**`handleNewCard()` 实现细节:**
-   **评级“重来” (1) / “困难” (2)**:
    -   `type` 变为 `LEARNING (1)`。
    -   `current_step` 设为 `0`。
    -   间隔设为 `learningSteps[0]` (例如 10 分钟)。
-   **评级“良好” (3)**:
    -   `type` 变为 `LEARNING (1)`。
    -   `current_step` 设为 `1` (跳过第一步)。
    -   **检查是否毕业**: 如果 `current_step >= learningSteps.length`，则卡片毕业，`type` 变为 `REVIEW (2)`，间隔设为 `graduatingInterval`。
    -   否则，进入下一个学习步骤，间隔设为 `learningSteps[current_step]`。
-   **评级“简单” (4) -> 立即毕业**:
    -   `type` 变为 `REVIEW (2)`。
    -   `current_step` 重置为 `0`。
    -   间隔设为 `easyInterval` (例如 4 天)。

**`handleReviewCard()` 实现细节 (示例):**
-   **评级"重来" (1) -> 触发"遗忘 (Lapse)"**:
    -   **保存遗忘前间隔**: `lapseOldInterval` = 当前 `interval` (天)。
    -   `type` 变为 `RELEARNING (3)`。
    -   `current_step` 重置为 `0`。
    -   `repetitions` **重置为 `0`** (连续正确次数中断)。
    -   `lapseCount` **增加 `1`**。
    -   难度系数(EF) **大幅降低** (`-0.20`)。
    -   `interval` 设为 `relearningSteps[0]` (例如 20 分钟)。
-   **评级“困难” (2)**:
    -   `repetitions` **增加 `1`**。
    -   间隔 = `上次间隔 * 1.2` (间隔增长放缓)。
    -   难度系数(EF) **降低** (`-0.15`)。
    -   **设计说明**: “困难”的核心作用是降低未来复习的EF，而不是立即缩短间隔。如果用户觉得当前间隔太长，正确的操作是选择“重来”。
-   **评级“良好” (3)**:
    -   `repetitions` **增加 `1`**。
    -   间隔 = `上次间隔 * 难度系数(EF)` (标准行为)。
    -   难度系数(EF) **不变**。
-   **评级“简单” (4)**:
    -   `repetitions` **增加 `1`**。
    -   间隔 = `上次间隔 * 难度系数(EF) * easyBonus` (例如 `* 1.3`)。
    -   难度系数(EF) **增加** (`+0.15`)。

**`handleLearningCard(steps)` 实现细节:**
-   **评级“重来” (1)**:
    -   `current_step` 重置为 `0` (回到起点)。
    -   间隔设为 `steps[0]`。
-   **评级“困难” (2)**:
    -   `current_step` **保持不变**。
    -   间隔基于当前和下一步骤的平均值，提供一次巩固机会。
    -   **计算**: `新间隔 = (steps[current_step] + steps[current_step + 1]) / 2`。
    -   **边界处理**: 如果已是最后一步，则间隔可设为 `steps[current_step]`。
-   **评级"良好" (3)**:
    -   `current_step` 增加 `1` (推进到下一步)。
    -   **检查是否毕业**:
        -   如果 `current_step` >= `steps.length`，则卡片毕业：
            -   `type` 变为 `REVIEW (2)`。
            -   `current_step` 重置为 `0`。
            -   **间隔设置 (区分首次毕业和重新毕业)**:
                -   **LEARNING → REVIEW** (首次毕业): 间隔 = `graduatingInterval` (例如 1 天)
                -   **RELEARNING → REVIEW** (重新毕业): 间隔 = `MAX(graduatingInterval, ⌊lapseOldInterval × newIntervalMultiplier⌋)`
                    -   **设计理由**: 遗忘后的记忆痕迹仍然存在，重学效应使恢复速度快于首次学习，完全重置到 1 天会忽略之前的记忆积累
                    -   **实现要求**: 使用保存的 `lapseOldInterval` 字段计算，毕业后将该字段清空 (设为 `NULL`)
        -   否则，进入下一个学习步骤：
            -   间隔设为 `steps[current_step]`。
-   **评级"简单" (4) -> 立即毕业**:
    -   `type` 变为 `REVIEW (2)`。
    -   `current_step` 重置为 `0`。
    -   **间隔设置 (区分首次毕业和重新毕业)**:
        -   **LEARNING → REVIEW** (首次毕业): 间隔 = `easyInterval` (例如 4 天)
        -   **RELEARNING → REVIEW** (重新毕业): 间隔 = `MAX(easyInterval, ⌊lapseOldInterval × newIntervalMultiplier⌋)`
            -   "简单"评级表示记忆恢复良好，取两者较大值作为奖励
            -   毕业后将 `lapseOldInterval` 清空 (设为 `NULL`)

#### 3.2.1. 难度系数(EF)更新逻辑

为了简化实现并与主流 SRS 软件 Anki 的行为保持一致，我们**不再使用 SM-2 的复杂公式**，而是采用更直观的**固定值加减法**。
-   **来源**: 此逻辑直接参考 Anki 开源算法的实现。

 Again (重来)：我忘了 → 重置间隔，大幅降低 EF
 Hard (困难)：我勉强记得 → 略微延长间隔，降低 EF
 Good (良好)：我记得  → 正常延长间隔，保持 EF
 Easy (简单)：我轻松记得 → 大幅延长间隔，增加 EF

| 用户评级 (`result`) | 行为 | EF 变化 |
| :--- | :--- | :--- |
| **1 (重来)** | 遗忘 | `-0.20` |
| **2 (困难)** | 正确回忆，但很吃力 | `-0.15` |
| **3 (良好)** | 正常回忆 | **不变** |
| **4 (简单)** | 轻松回忆 | `+0.15` |
-   **约束**: EF 的**最小值被限制为 1.3** (130%)。

#### 3.2.2. 遗忘次数 (lapseCount) 处理规则

`lapseCount` 字段严格定义为"**从复习状态遗忘的次数**"，用于统计和识别难记卡片。

**更新规则:**

| 卡片状态 | 用户评级 | 是否增加 lapseCount | 说明 |
| :--- | :--- | :---: | :--- |
| **REVIEW (复习)** | 1 (重来) | ✅ **增加 1** | 这是真正的"遗忘"(Lapse) |
| **REVIEW (复习)** | 2 (困难) | ❌ 不增加 | 虽然困难但仍然记得，不算遗忘 |
| **LEARNING (学习中)** | 任何评级 | ❌ 不增加 | 还在学习阶段，谈不上"遗忘" |
| **RELEARNING (重新学习)** | 1 (重来) | ❌ 不增加 | 已在重新学习流程中，不重复计数 |
| **NEW (新卡片)** | 任何评级 | ❌ 不增加 | 首次学习不存在遗忘 |

**字段价值:**
1.  **用户统计**: 展示"这张卡片我忘记过几次"
2.  **难卡识别**: `lapseCount` 高的卡片是"顽固难卡"，可以特殊处理或优先复习
3.  **学习分析**: 统计用户的整体遗忘率和学习效果
4.  **未来扩展**: 支持"暂停难卡""降低难卡出现频率"等高级功能

#### 3.3. API 及队列查询

1.  **评级范围确认**: 前端提交的 `result` 严格为 **1-4** 整数 (`1`:重来, `2`:困难, `3`:良好, `4`:简单)。

2.  **复习队列查询 (`getReviewQueue`)**:
    -   **统一到期判断**: 对所有类型卡片，统一使用 `reviewDueAt <= NOW()` 作为到期标准。
    -   **实现优先级排序**: 在数据库查询时，使用 `ORDER BY` 确保优先级：
        1.  **第一排序**: `CASE type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 ELSE 3 END`，确保优先级：`学习中 > 重新学习中 > 复习中 > 新卡片`。
        2.  **第二排序**: 按 `reviewDueAt` 升序，确保最先到期的卡片最先出现。
    -   **新卡片补充策略**:
        -   **实现方式**: **推荐使用 SQL UNION ALL** 方式，一次查询完成，性能更优。
        -   **查询逻辑**:
            ```sql
            -- 第一部分: 到期卡片 (LEARNING/RELEARNING/REVIEW)
            SELECT * FROM user_card_srs
            WHERE user_id = ? AND review_due_at <= NOW() AND type IN (1, 2, 3)
            ORDER BY
                CASE type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 END,
                review_due_at ASC
            LIMIT ?

            UNION ALL

            -- 第二部分: 新卡片补充 (仅当第一部分不足时)
            SELECT * FROM user_card_srs
            WHERE user_id = ? AND type = 0
            ORDER BY id ASC  -- 按加入顺序 (创建时间隐式排序)
            LIMIT ?  -- 动态计算: MAX(0, 请求limit - 第一部分返回数)
            ```
        -   **新卡片排序规则**: 按 `id` 升序 (即卡片加入学习队列的先后顺序)
            -   **理由**: 用户通常希望按课程/章节的自然顺序学习新卡片
            -   **替代方案**: 如需随机化，可在应用层使用 `Collections.shuffle()` 打乱新卡片部分
        -   **实现细节**:
            -   **方式一 (推荐)**: 在 Mapper 中实现带 `UNION ALL` 的单次查询
            -   **方式二**: 代码层两次查询:
                1. 先查询到期卡片 (type IN (1,2,3))
                2. 如果数量 < limit，再查询 (limit - count) 张新卡片 (type = 0)
            -   **权衡**: 方式一 SQL 复杂但性能好；方式二逻辑清晰但多一次数据库往返

### 4. 待讨论和确认的配置项

| 参数名 | 建议默认值 | 描述 |
| :--- | :--- | :--- |
| `learningSteps` | `[10, 60]` (分钟) | 新卡片的学习步骤。 |
| `relearningSteps` | `[20]` (分钟) | 遗忘卡的重新学习步骤。 |
| `graduatingInterval`| `1` (天) | "学习中"卡片首次毕业后的复习间隔。 |
| `easyInterval` | `4` (天) | 新卡被评为"简单"后，直接毕业的间隔。 |
| `easyBonus` | `1.3` | 对"复习"卡片评为"简单"时的额外间隔奖励乘数。 |
| `newIntervalMultiplier` | `0.5` (50%) | 遗忘后重新毕业时的间隔恢复比例。范围 0-1，Anki 建议 0.5-0.7。 |

### 5. 风险与应对

1.  **数据迁移**: 需要编写一个 Flyway 迁移脚本来执行 `ALTER TABLE` 操作，并需要一个一次性脚本将现有卡片的 `type` 初始化为 `2` (复习)。
2.  **逻辑复杂性**: 新算法逻辑分支多，需要全面的单元测试来覆盖所有状态和评级的组合。
