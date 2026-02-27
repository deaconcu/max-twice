# 记忆卡片模块设计文档

## 1. 模块概述

基于 **Anki SRS（间隔重复系统）算法** 的智能复习系统。用户可以为学习内容创建记忆卡片组，通过科学的复习间隔强化记忆。

### 核心概念

| 概念 | 说明 |
|------|------|
| **卡片组 (Deck)** | 一组记忆卡片的集合，关联到节点（Node），可选关联来源帖子（Post）。需审核后才对其他用户可见 |
| **卡片 (Card)** | 一张记忆卡片，包含正面（问题）和背面（答案），支持多版本 |
| **卡片版本 (CardVersion)** | 卡片内容的版本快照，修改时不覆盖旧内容，而是创建新版本 |
| **SRS 状态 (UserCardSrs)** | 用户对每张卡片的学习状态，包含间隔、难度系数、下次复习时间等 |
| **记忆库 (MemoryBank)** | 用户将卡片组加入学习计划后形成的个人记忆库，按课程组织 |

## 2. 数据结构

### 2.1 实体关系

```
Course (课程)
  └── Node (节点)
        ├── Post (帖子/文章) ──可选关联──┐
        └── MemoryCardDeck (卡片组) ◄────┘
              └── MemoryCard (卡片)
                    └── MemoryCardVersion (卡片版本)

User (用户)
  ├── UserCourseSrsSetting (课程复习设置)
  ├── UserCardInCourse (用户-卡片-课程关系)
  └── UserCardSrs (用户-卡片 SRS 状态)
```

### 2.2 关键字段说明

**memory_card_deck（卡片组）：**
- `post_id` — 来源帖子ID，**0 表示无来源文章**（直接在节点下创建）
- `state` — 内容审核状态：1=审核中, 2=已发布, 3=已拒绝, 4=已封禁
- `version` — 版本号，每次卡片增删改时递增，用于让学习者检测卡片组更新
- `score` — 热度分数，用于排序

**memory_card_version（卡片版本）：**
- `content_hash` — SHA 内容哈希，用于去重和检测变更
- `is_active` — 是否为当前激活版本，修改卡片时旧版本置为 false

**user_card_srs（SRS 状态）：**
- `type` — 卡片状态：0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING
- `reappear_at` — LEARNING/RELEARNING 卡片的下次展现计数
- `interval` — REVIEW 阶段的复习间隔（天）
- `ease_factor` — 难度系数，默认 2.50，根据复习表现动态调整
- `card_version_id` — 用户学习的卡片版本ID，与 card.current_version_id 对比可检测内容更新
- `deck_version` — 加入时的卡片组版本，用于检测卡片组结构更新
- `deck_id` / `node_id` — 冗余字段，避免联表查询，提升复习队列性能

**user_card_in_course（用户-卡片-课程关系）：**
- 同一张卡片可以被添加到多个课程。从课程移除卡片组时只删此关系，卡片在所有课程中都被移除后才删 SRS 状态

**user_course_srs_setting（课程复习设置）：**
- `frequency_setting` — 复习频率：1=高频, 2=普通, 3=低频
- `state` — 学习状态：1=学习中, 2=已暂停, 3=已归档

## 3. 后端架构

### 3.1 分层结构

```
Controller (learn-web)        — 接收请求、参数校验、权限检查
    ↓
Application Service (learn-application) — 跨域编排、DTO转换、事件发布
    ↓
Domain Service (learn-memory)  — 核心业务逻辑、领域规则
    ↓
Data Service / Mapper (learn-memory) — 数据访问
    ↓
Database
```

### 3.2 领域服务

| 服务 | 职责 | 关键方法 |
|------|------|---------|
| **MemoryCardDomainService** | 卡片 CRUD、版本管理 | `createCard`, `batchCreateCards`, `updateCard`(创建新版本), `deleteCard`, `getCardContentDiff` |
| **MemoryCardDeckDomainService** | 卡片组 CRUD、审核状态流转 | `createDeck`, `updateDeck`, `deleteDeck`, `approve`, `reject`, `ban`, `restore` |
| **ReviewDomainService** | Anki 复习算法核心 | `getNextCard`, `submitReview`, `getReviewStats` |
| **MemoryBankDomainService** | 记忆库管理（加入/移除卡片组） | `addDeckToMemoryBank`(分布式锁), `removeDeckFromCourse`, `updateCourseSetting` |

### 3.3 应用服务

| 服务 | 职责 |
|------|------|
| **MemoryCardDeckService** | 聚合 User/Post/Node/Course/Upvote 数据，DTO 转换，审核事件发布 |
| **MemoryCardService** | 卡片创建、聚合卡片内容（从 CardVersion 获取 front/back） |

### 3.4 审核状态机

```
SUBMITTED (1) ──批准──→ PUBLISHED (2) ──封禁──→ BANNED (4)
     │                        │                      │
     └──拒绝──→ REJECTED (3)  └──────────────────────└──恢复──→ PUBLISHED (2)
```

## 4. SRS 算法（间隔重复系统）

本系统采用改良的 **Anki 算法**（基于 SM-2），通过科学的间隔重复帮助用户高效记忆。

**核心改进**：LEARNING/RELEARNING 阶段采用**卡片计数**而非时间间隔，避免用户等待，提升复习体验。

### 4.1 核心概念

| 概念 | 说明 |
|------|------|
| **type（卡片状态）** | 0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING |
| **reviewCount（复习计数）** | 用户全局复习计数器，每复习一张卡片 +1，永不重置 |
| **reappearAt（再现计数）** | LEARNING/RELEARNING 卡片的下次展现计数（当 `reviewCount >= reappearAt` 时可展现） |
| **currentStep（当前步骤）** | 学习/重学阶段的步骤索引（从 0 开始） |
| **easeFactor（难度因子）** | 默认 2.50，影响 REVIEW 阶段复习间隔的增长速度 |
| **repetitions（连续正确次数）** | 在 REVIEW 阶段连续答对的次数 |
| **lapseCount（遗忘次数）** | 从 REVIEW 掉回 RELEARNING 的总次数 |

### 4.2 配置参数

配置位置：`application.yml` 中的 `app.srs.algorithm`，对应 Java 类 `SystemProperties.Srs.Algorithm`

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `cardGaps` | `[3, 8]` | LEARNING/RELEARNING 阶段的卡片间隔。Step 0 间隔 3 张，Step 1 间隔 8 张 |
| `graduatingInterval` | `1` | 学习完成后首次进入复习的间隔（天） |
| `easyInterval` | `4` | 点击"简单"直接毕业时的间隔（天） |
| `easyBonus` | `1.3` | 复习时点击"简单"的额外奖励乘数 |
| `newIntervalMultiplier` | `0.5` | 遗忘后重新毕业时的间隔恢复比例 |
| `minEaseFactor` | `1.3` | 难度因子的最小值，防止间隔增长过慢 |

### 4.3 卡片状态流转图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                                                                 │
│  ┌─────────┐                                                                    │
│  │   NEW   │  新卡片，从未复习过                                                  │
│  │  (0)    │                                                                    │
│  └────┬────┘                                                                    │
│       │                                                                         │
│       │ 首次复习                                                                 │
│       ▼                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                         LEARNING (1) 学习中                              │    │
│  │                                                                         │    │
│  │   步骤: cardGaps = [3, 8] 张卡片                                         │    │
│  │                                                                         │    │
│  │   ┌────────┐      良好        ┌────────┐      良好        ┌─────────┐    │    │
│  │   │ Step 0 │ ───────────────→ │ Step 1 │ ───────────────→ │  毕业   │    │    │
│  │   │ +3张   │                  │ +8张   │                  │         │    │    │
│  │   └────────┘                  └────────┘                  └────┬────┘    │    │
│  │        ▲                           ▲                           │         │    │
│  │        │ 忘记                       │ 忘记                       │         │    │
│  │        └───────────────────────────┴───────────────────────────│         │    │
│  │                     重置到 Step 0                               │         │    │
│  └─────────────────────────────────────────────────────────────────│─────────┘    │
│                                                                    │              │
│                                                                    ▼              │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                         REVIEW (2) 复习阶段                              │     │
│  │                                                                         │     │
│  │   间隔单位: 天                                                           │     │
│  │   间隔计算: interval × easeFactor                                        │     │
│  │                                                                         │     │
│  │   示例: 1天 → 2.5天 → 6天 → 15天 → 38天 → ...                            │     │
│  │                                                                         │     │
│  └────────────────────────────────────┬────────────────────────────────────┘     │
│                                       │                                          │
│                                       │ 忘记 (rating=1)                          │
│                                       ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                       RELEARNING (3) 重新学习                            │     │
│  │                                                                         │     │
│  │   步骤: cardGaps = [3, 8] 张卡片（与 LEARNING 共用配置）                   │     │
│  │   影响: lapseCount +1, easeFactor -0.20                                  │     │
│  │                                                                         │     │
│  │   ┌────────┐      良好        ┌────────┐      良好        ┌─────────┐    │     │
│  │   │ Step 0 │ ───────────────→ │ Step 1 │ ───────────────→ │ 重新毕业 │    │     │
│  │   │ +3张   │                  │ +8张   │                  │         │    │     │
│  │   └────────┘                  └────────┘                  └────┬────┘    │     │
│  │        ▲                           ▲                           │         │     │
│  │        │ 忘记                       │ 忘记                       │         │     │
│  │        └───────────────────────────┴───────────────────────────│         │     │
│  │                     重置到 Step 0                               │         │     │
│  └─────────────────────────────────────────────────────────────────│─────────┘     │
│                                                                    │               │
│                                                                    ▼               │
│                                                          回到 REVIEW              │
│                                                         (间隔有所缩短)            │
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

### 4.4 用户评级按钮

复习时用户有 4 个选择：

| 按钮 | rating 值 | 含义 |
|------|-----------|------|
| 🔴 **忘记了** | 1 (AGAIN) | 完全不记得，需要重新学习 |
| 🟠 **困难** | 2 (HARD) | 勉强想起来，但很费力 |
| 🟢 **良好** | 3 (GOOD) | 正常回忆起来 |
| 🔵 **简单** | 4 (EASY) | 太简单了，可以延长间隔 |

### 4.5 各状态下的评级效果

#### 4.5.1 NEW 卡片（首次复习）

| 评级 | 状态变化 | 下次展现 |
|------|----------|----------|
| 忘记 | → LEARNING Step 0 | **+3 张卡片后** |
| 困难 | → LEARNING Step 0 | **+3 张卡片后** |
| 良好 | → LEARNING Step 1（跳过第一步） | **+8 张卡片后** |
| 简单 | → REVIEW（直接毕业） | **+4 天** |

#### 4.5.2 LEARNING/RELEARNING 卡片（学习中）

以 `cardGaps = [3, 8]` 为例：

| 评级 | Step 0 效果 | Step 1 效果 |
|------|-------------|-------------|
| 忘记 | 保持 Step 0，**+3 张** | 重置到 Step 0，**+3 张** |
| 困难 | 保持 Step 0，**+5 张**（平均值） | 保持 Step 1，**+8 张** |
| 良好 | 进入 Step 1，**+8 张** | 毕业 |
| 简单 | 立即毕业，**+4 天** | 立即毕业，**+4 天** |

**说明**：困难时取当前步骤和下一步骤间隔的平均值；最后一步时保持当前间隔。

#### 4.5.3 REVIEW 卡片（复习阶段）

| 评级 | interval 变化 | easeFactor 变化 | 其他 |
|------|---------------|-----------------|------|
| 忘记 | → RELEARNING | **-0.20** | lapseCount +1，repetitions 归零 |
| 困难 | **×1.2** | **-0.15** | repetitions +1 |
| 良好 | **×easeFactor** | 不变 | repetitions +1 |
| 简单 | **×easeFactor×1.3** | **+0.15** | repetitions +1 |

**示例：** 假设当前 interval=7天，easeFactor=2.5
- 困难：7 × 1.2 = **8.4 天**
- 良好：7 × 2.5 = **17.5 天**
- 简单：7 × 2.5 × 1.3 = **22.75 天**

### 4.6 关键数据库字段

**user 表新增字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `review_count` | bigint | 用户全局复习计数器，每复习一张卡片 +1，永不重置 |

**user_card_srs 表核心字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | tinyint | 卡片状态：0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING |
| `current_step` | tinyint | 当前学习步骤索引（仅 type=1/3 时有意义） |
| `reappear_at` | bigint | LEARNING/RELEARNING 卡片的下次展现计数（`reviewCount >= reappearAt` 时可展现）|
| `interval` | int | REVIEW 阶段的复习间隔（天） |
| `lapse_old_interval` | smallint | 遗忘前的间隔天数（仅 type=3 时使用，用于计算恢复间隔） |
| `review_due_at` | datetime | REVIEW/NEW 卡片的下次复习到期时间 |
| `ease_factor` | decimal(4,2) | 难度因子，默认 2.50，最小 1.30 |
| `repetitions` | int | REVIEW 阶段连续正确次数 |
| `lapse_count` | int | 遗忘总次数（从 REVIEW 掉回 RELEARNING 的次数） |

### 4.7 获取下一张卡片逻辑

```sql
SELECT * FROM user_card_srs
WHERE user_id = ?
  AND (course_id = ? OR ?)  -- 可选课程筛选
ORDER BY
  CASE
    -- 1. LEARNING/RELEARNING 且已到展现计数
    WHEN type IN (1, 3) AND reappear_at <= ? THEN 0
    -- 2. REVIEW 且已到期
    WHEN type = 2 AND review_due_at <= NOW() THEN 1
    -- 3. NEW 卡片
    WHEN type = 0 THEN 2
    -- 4. LEARNING/RELEARNING 未到计数（无其他卡片时也返回）
    WHEN type IN (1, 3) THEN 3
    -- 5. 其他（未到期的 REVIEW）
    ELSE 4
  END,
  reappear_at ASC,      -- LEARNING/RELEARNING 按计数排序（小的优先）
  review_due_at ASC     -- REVIEW 按时间排序
LIMIT 1
```

**说明**：
- 优先返回已到展现计数的 LEARNING/RELEARNING 卡片
- 其次返回已到期的 REVIEW 卡片
- 然后是 NEW 卡片
- 如果只剩 LEARNING/RELEARNING 卡片且未到计数，也直接返回（避免用户无卡可复习）

### 4.8 配置示例

```yaml
# application.yml
app:
  srs:
    max-cards-per-node: 200
    algorithm:
      card-gaps: [3, 8]             # LEARNING/RELEARNING 阶段的卡片间隔
      graduating-interval: 1        # 毕业间隔：1天
      easy-interval: 4              # 简单直接毕业：4天
      easy-bonus: 1.3               # 简单奖励乘数
      new-interval-multiplier: 0.5  # 遗忘后恢复比例
      min-ease-factor: 1.3          # 最小难度因子
```

### 4.9 常见问题

**Q: LEARNING/RELEARNING 卡片为什么用卡片计数而不是时间间隔？**

A: 传统 Anki 使用时间间隔（如 10 分钟后再现），但这要求用户等待，体验不佳。改用卡片计数后，用户可以连续复习，LEARNING 卡片会在复习若干张其他卡片后自动再现，既保证了间隔效果，又不打断复习流程。

**Q: 如果只剩 LEARNING 卡片且未到展现计数怎么办？**

A: 系统会直接返回该卡片，避免用户无卡可复习。这种情况通常发生在用户只有少量卡片时。

**Q: 按课程复习时，卡片计数是全局的还是按课程的？**

A: **全局计数**。从记忆角度，间隔复习的本质是让大脑经历「遗忘-重新提取」过程，复习其他课程的卡片同样能产生干扰效应。因此无论复习哪个课程，计数器都会递增。

**Q: NEW 和 REVIEW 卡片点击"忘记"有什么区别？**

A:
- **NEW → LEARNING**：使用 `cardGaps[0]`（如 3 张卡片后再现），不影响 easeFactor
- **REVIEW → RELEARNING**：使用 `cardGaps[0]`，easeFactor -0.20，lapseCount +1

**Q: easeFactor 有什么作用？**

A: easeFactor 决定了 REVIEW 阶段复习间隔的增长速度。默认 2.5 意味着每次正确回答后，间隔大约翻 2.5 倍。频繁忘记的卡片 easeFactor 会降低（最低 1.3），导致间隔增长更慢，复习更频繁。

## 5. 前端架构

### 5.1 组件结构

```
ContentReadPage.vue (Read 页面)
├── PostingList.vue (主内容区，Tab 切换)
│   ├── [文章列表 Tab]
│   ├── [记忆卡片 Tab] → MemoryCardList.vue（节点下所有卡片组）
│   │                     └── CreateDeckDialog.vue（新增卡片组）
│   └── [评论 Tab]
├── MemoryCardSidebar.vue (右侧边栏，文章详情时显示当前帖子的卡片组)
│   └── CreateDeckDialog.vue（关联帖子创建）
└── DeckDetailDialog.vue (卡片组详情 + 加入学习)
```

### 5.2 核心 API

| 功能分类 | API |
|---------|-----|
| **卡片组** | createDeck, getDeckDetail, deleteDeck, getDecksByNode, getPostPublicDecks, upvoteDeck |
| **卡片** | createCard, updateCard, deleteCard |
| **复习** | getNextCard, submitReview, getReviewStats |
| **记忆库** | addDeckToMemoryBank, getMemoryBankCourses, removeCourseMemoryBank |
| **更新检测** | getDeckDiff, getCardDiff, acceptDeckChanges |
| **管理** | approveDeck, rejectDeck, banDeck, restoreDeck |

## 6. 核心业务流程

### 6.1 创建卡片组

```
用户点击"新增卡片组" → 填写卡片和描述 → createDeck API
  → MemoryCardDeckService.createDeck()
    → 验证用户、验证帖子/节点（postId=0 时从请求获取 nodeId）
    → 创建卡片组（状态=SUBMITTED）
    → 批量创建卡片 + 版本
  → 等待审核 → 通过后其他用户可见
```

### 6.2 添加到记忆库

```
用户点击"加入学习" → addDeckToMemoryBank API
  → MemoryBankDomainService.addDeckToMemoryBank()（分布式锁）
    → 检查节点卡片数量上限
    → 创建课程学习设置（UserCourseSrsSetting）
    → 批量创建 UserCardInCourse 关系
    → 批量创建 UserCardSrs（初始 type=NEW）
```

### 6.3 复习流程

```
进入复习页 → getNextCard（获取下一张卡片）
  → 展示正面（问题）→ 用户点击"显示答案"
  → 展示背面 + 4个评级按钮 → submitReview API
  → ReviewDomainService.submitReview()
    → user.reviewCount++
    → 根据 type 分发到不同处理逻辑
    → LEARNING/RELEARNING: 计算 reappearAt = reviewCount + gap
    → REVIEW: 计算 interval、reviewDueAt
    → 更新 UserCardSrs
    → 返回下一张卡片
```

### 6.4 卡片更新检测

```
创建者修改卡片 → 创建新 MemoryCardVersion → 卡片组 version+1
学习者复习时 → 检测 srs.card_version_id ≠ card.current_version_id
  → 提示"内容已更新" → 用户可查看差异 → 选择"接受更新"
```

## 7. 关键设计决策

1. **post_id = 0**：卡片组可以不关联来源文章，直接在节点下创建
2. **版本化卡片内容**：修改不覆盖，创建新版本，学习者可检测更新并选择是否接受
3. **冗余字段优化**：user_card_srs 冗余 deck_id/node_id，避免联表查询
4. **分布式锁**：addDeckToMemoryBank 使用分布式锁（key=userId+nodeId），防止并发超限
5. **孤立卡片清理**：从课程移除卡片组时只删关系，所有课程都移除后才删 SRS 状态
6. **内容审核**：卡片组创建后默认 SUBMITTED 状态，需审核通过才对外可见
7. **卡片计数替代时间间隔**：LEARNING/RELEARNING 阶段使用全局复习计数（reviewCount）和再现计数（reappearAt）调度卡片，避免用户等待，提升复习体验
8. **全局计数器**：reviewCount 为用户全局计数器，跨课程共享，因为记忆干扰效应不分课程
