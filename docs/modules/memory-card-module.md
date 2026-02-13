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
- `interval` — 复习间隔，LEARNING/RELEARNING 时单位为**分钟**，REVIEW 时单位为**天**
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
| **ReviewDomainService** | Anki 复习算法核心 | `getReviewQueue`, `submitReview`, `getReviewStats` |
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

### 4.1 核心概念

| 概念 | 说明 |
|------|------|
| **type（卡片状态）** | 0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING |
| **interval（间隔）** | LEARNING/RELEARNING 时单位为**分钟**，REVIEW 时单位为**天** |
| **currentStep（当前步骤）** | 学习/重学阶段的步骤索引（从 0 开始） |
| **easeFactor（难度因子）** | 默认 2.50，影响复习间隔的增长速度 |
| **repetitions（连续正确次数）** | 在 REVIEW 阶段连续答对的次数 |
| **lapseCount（遗忘次数）** | 从 REVIEW 掉回 RELEARNING 的总次数 |

### 4.2 配置参数

配置位置：`application.yml` 中的 `app.srs.algorithm`，对应 Java 类 `SystemProperties.Srs.Algorithm`

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `learningSteps` | `[10, 60]` | 新卡学习步骤（分钟）。第一步 10 分钟，第二步 60 分钟 |
| `relearningSteps` | `[20]` | 遗忘后重学步骤（分钟）。只有一步，20 分钟 |
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
│  │   步骤: learningSteps = [10, 60] 分钟                                    │    │
│  │                                                                         │    │
│  │   ┌────────┐    良好/简单     ┌────────┐    良好/简单     ┌─────────┐    │    │
│  │   │ Step 0 │ ───────────────→ │ Step 1 │ ───────────────→ │  毕业   │    │    │
│  │   │ 10分钟 │                  │ 60分钟 │                  │         │    │    │
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
│  │   步骤: relearningSteps = [20] 分钟                                      │     │
│  │   影响: lapseCount +1, easeFactor -0.20                                  │     │
│  │                                                                         │     │
│  │   ┌────────┐    良好/简单     ┌─────────┐                                │     │
│  │   │ Step 0 │ ───────────────→ │ 重新毕业 │ ──────→ 回到 REVIEW           │     │
│  │   │ 20分钟 │                  │         │        (间隔有所缩短)          │     │
│  │   └────────┘                  └─────────┘                                │     │
│  │        ▲                                                                │     │
│  │        │ 忘记                                                            │     │
│  │        └─────────────────────────────────────────────────────────────────│     │
│  │                     重置到 Step 0                                        │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
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

| 评级 | 状态变化 | 下次复习时间 |
|------|----------|--------------|
| 忘记 | → LEARNING Step 0 | **+10 分钟** |
| 困难 | → LEARNING Step 0 | **+10 分钟** |
| 良好 | → LEARNING Step 1（跳过第一步） | **+60 分钟** |
| 简单 | → REVIEW（直接毕业） | **+4 天** |

#### 4.5.2 LEARNING 卡片（学习中）

以 `learningSteps = [10, 60]` 为例：

| 评级 | 效果 | 下次复习时间 |
|------|------|--------------|
| 忘记 | 重置到 Step 0 | **+10 分钟** |
| 困难 | 停留当前步骤，间隔略延长 | 当前与下一步的平均值 |
| 良好 | 推进到下一步，或毕业 | Step 0→+60分钟，Step 1→+1天（毕业） |
| 简单 | 立即毕业到 REVIEW | **+4 天** |

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

#### 4.5.4 RELEARNING 卡片（重新学习）

以 `relearningSteps = [20]` 为例：

| 评级 | 效果 | 下次复习时间 |
|------|------|--------------|
| 忘记 | 重置到 Step 0 | **+20 分钟** |
| 困难 | 停留当前步骤 | +20 分钟 |
| 良好 | 毕业回到 REVIEW | 恢复间隔（原间隔 × 0.5，至少 1 天） |
| 简单 | 毕业回到 REVIEW | 恢复间隔（原间隔 × 0.5，至少 4 天） |

### 4.6 关键数据库字段

`user_card_srs` 表的核心字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | tinyint | 卡片状态：0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING |
| `current_step` | tinyint | 当前学习步骤索引（仅 type=1/3 时有意义） |
| `interval` | int | 复习间隔（type=1/3 时为分钟，type=2 时为天） |
| `lapse_old_interval` | smallint | 遗忘前的间隔天数（仅 type=3 时使用，用于计算恢复间隔） |
| `review_due_at` | datetime | **核心字段**：下次复习到期时间 |
| `ease_factor` | decimal(4,2) | 难度因子，默认 2.50，最小 1.30 |
| `repetitions` | int | REVIEW 阶段连续正确次数 |
| `lapse_count` | int | 遗忘总次数（从 REVIEW 掉回 RELEARNING 的次数） |

### 4.7 复习队列查询逻辑

系统通过以下条件获取待复习卡片：

```sql
SELECT * FROM user_card_srs
WHERE user_id = ?
  AND review_due_at <= NOW()           -- 已到期
  AND type IN (0, 1, 2, 3)             -- 所有状态
ORDER BY
  CASE type
    WHEN 1 THEN 0   -- LEARNING 优先
    WHEN 3 THEN 1   -- RELEARNING 次之
    WHEN 2 THEN 2   -- REVIEW 再次
    WHEN 0 THEN 3   -- NEW 最后
  END,
  review_due_at ASC
```

**重要：** 只有 `review_due_at <= NOW()` 的卡片才会出现在复习队列中。如果点击"忘记"后立即刷新页面，卡片需要等待指定的分钟数（如 10 分钟）后才会再次出现。

### 4.8 配置示例

```yaml
# application.yml
app:
  srs:
    max-cards-per-node: 200
    algorithm:
      learning-steps: [10, 60]        # 新卡学习步骤：10分钟, 60分钟
      relearning-steps: [20]          # 遗忘重学步骤：20分钟
      graduating-interval: 1          # 毕业间隔：1天
      easy-interval: 4                # 简单直接毕业：4天
      easy-bonus: 1.3                 # 简单奖励乘数
      new-interval-multiplier: 0.5    # 遗忘后恢复比例
      min-ease-factor: 1.3            # 最小难度因子
```

### 4.9 常见问题

**Q: 点击"忘记"后卡片为什么不立即再出现？**

A: 这是设计行为。点击"忘记"后，卡片的 `review_due_at` 会设置为当前时间 + 学习步骤间隔（如 10 分钟）。系统认为用户需要一定时间来消化信息后再次尝试。刷新页面后需要等待该时间过后卡片才会再次出现。

**Q: NEW 和 REVIEW 卡片点击"忘记"有什么区别？**

A:
- **NEW → LEARNING**：使用 `learningSteps`（如 10 分钟），不影响 easeFactor
- **REVIEW → RELEARNING**：使用 `relearningSteps`（如 20 分钟），easeFactor -0.20，lapseCount +1

**Q: easeFactor 有什么作用？**

A: easeFactor 决定了复习间隔的增长速度。默认 2.5 意味着每次正确回答后，间隔大约翻 2.5 倍。频繁忘记的卡片 easeFactor 会降低（最低 1.3），导致间隔增长更慢，复习更频繁。

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
| **复习** | getReviewQueue, reviewCard, getReviewStats |
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
进入复习页 → getReviewQueue（获取到期卡片）
  → 展示正面（问题）→ 用户点击"显示答案"
  → 展示背面 + 4个评级按钮 → reviewCard API
  → ReviewDomainService.submitReview()
    → 根据 type 分发到不同处理逻辑
    → 计算新间隔、ease_factor、下次复习时间
    → 更新 UserCardSrs
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
