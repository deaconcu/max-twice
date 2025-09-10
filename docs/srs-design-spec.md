# 记忆曲线与卡片复习功能设计文档 (SRS Design Spec)

## 1. 功能概述

为提升用户学习效率与长期记忆效果，项目将引入基于**艾宾浩斯记忆曲线**的**间隔重复系统 (Spaced Repetition System, SRS)** 功能。用户可以将文章中的核心知识点制作为“记忆卡片”，并使用科学的复习算法来巩固记忆。

## 2. 核心设计决策：社区驱动的卡片组 (Community-Driven Decks)

为了实现最大的灵活性、鼓励社区贡献并内置审核流程，我们采用**“社区驱动的卡片组”**模型。

- **“卡片组(Deck)”是独立的核心实体**: 卡片组拥有自己的标题、创建者，并由统一的`state`字段管理其生命周期（审核、正常、删除等）。它是用户交互和排序的基本单位。
- **Post与Deck是一对多关系**: 任何一篇文章都可以关联**多个**由不同用户（包括AI、作者、社区成员）创建的卡片组。
- **Deck与Card是一对多关系**: 每张记忆卡片都必须属于一个明确的卡片组。
- **内容审核与社区驱动**: 所有用户生成的内容（卡片组和卡片）初始为“审核中”状态。通过审核后，内容对社区可见，并由社区通过投票来使其脱颖而出。

## 3. 数据模型

### 3.1. `memory_card_deck` 表 - 卡片组信息

```sql
CREATE TABLE `memory_card_deck` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_post_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `version` INT NOT NULL DEFAULT 1,          -- 卡片组版本号
  `state` TINYINT NOT NULL DEFAULT 0,      -- 状态 (0:审核中, 1:正常, 2:锁定, 3:私有, 4:已删除)
  
  -- 追踪与审计字段
  `auditor_id` bigint DEFAULT NULL,        -- 审核员的ID
  `audited_at` datetime DEFAULT NULL,      -- 审核操作发生的时间
  `updated_by` bigint DEFAULT NULL,        -- 最后一次修改者的ID
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 记录更新时间
  
  -- 社区互动字段
  `upvote_count` int DEFAULT 0,
  `card_count` int DEFAULT 0,              -- 冗余卡片数量
  `score` double DEFAULT 0,
  
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
```

### 3.2. `memory_card` 与 `memory_card_version` - 卡片主表与版本表

为实现健壮的版本控制和清晰的数据结构，我们将卡片信息拆分为两张表：`memory_card` (主表) 和 `memory_card_version` (版本表)。审核流程仅在 `memory_card_deck` 层面进行。

#### 3.2.1. `memory_card` 表 - 卡片主表 (永久标识)

```sql
CREATE TABLE `memory_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,          -- 卡片的永久、唯一ID
  `deck_id` bigint NOT NULL,                    -- 所属卡片组ID
  `creator_id` bigint NOT NULL,                 -- 卡片的原始创建者ID (所有者)
  `current_version_id` bigint NOT NULL,         -- 指向 memory_card_version 表中当前生效的版本ID
  
  `state` TINYINT NOT NULL DEFAULT 0,           -- 卡片状态 (0: 正常, 1: 已被创建者删除)

  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (`id`)
);
```

#### 3.2.2. `memory_card_version` 表 - 卡片版本内容表

```sql
CREATE TABLE `memory_card_version` (
  `id` bigint NOT NULL AUTO_INCREMENT,          -- 版本自身的唯一ID
  `card_id` bigint NOT NULL,                    -- 关联到 memory_card.id
  `version` INT NOT NULL,                       -- 版本号 (1, 2, 3...)
  `creator_id` bigint NOT NULL,                 -- 此版本的创建者/编辑者ID
  
  -- 版本内容
  `front` TEXT NOT NULL,                        -- 卡片正面（问题）
  `back` TEXT NOT NULL,                         -- 卡片背面（答案）
  `content_hash` CHAR(40) DEFAULT NULL,    -- 内容的SHA1哈希，用于检测重复
  `is_active` TINYINT NOT NULL DEFAULT 0,  -- 是否为 memory_card 指向的当前版本 (冗余)
  
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`)
);
```
### 3.3. 用户学习记录数据模型 (V8.0 最终方案)

为了支持“入口分散，管理聚合”以及“全局唯一学习状态，上下文可调策略”的复杂需求，用户的学习记录将由以下三张核心表构成，采用**“课程级策略表 + 异步批量更新”**的架构。

#### 3.3.1. `user_card_srs_state` - 用户卡片全局记忆状态表

此表是用户记忆的“真理之源”。每个用户对每张卡片，永远只有**一条**记录，负责存储其最核心、最客观的记忆强度和预先计算好的复习计划。

```sql
CREATE TABLE `user_card_srs_state` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `card_id` bigint NOT NULL,           -- 记忆单元的核心ID
  `deck_version` INT NOT NULL,         -- 【重要】学习时卡片组的版本快照
  `card_version_id` bigint NOT NULL,   -- 【重要】用户学习时锁定的版本ID，实现内容快照
  
  -- 核心SRS字段 (基于SM-2算法)
  `review_due_at` datetime NOT NULL,          -- 【重要】预计算好的、全局唯一的下次复习时间
  `last_reviewed_at` datetime DEFAULT NULL,   -- 上次复习时间
  `interval_days` int DEFAULT 0,              -- 当前复习间隔（天）
  `ease_factor` decimal(4,2) DEFAULT 2.50,    -- 缓急因子
  `repetitions` int DEFAULT 0,                -- 连续正确次数
  
  -- 分析字段
  `lapse_count` INT DEFAULT 0,                -- 遗忘总次数
  
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_card` (`user_id`, `card_id`) -- 核心约束：确保状态唯一
);
```

#### 3.3.2. `user_course_srs_setting` - 用户课程复习策略表

此表存储用户在**课程维度**上的宏观管理策略。它非常轻量，每个用户和每个他已开始学习的课程之间只有一条记录。

```sql
CREATE TABLE `user_course_srs_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  
  -- 课程级策略与状态
  `frequency_setting` TINYINT NOT NULL DEFAULT 1, -- 复习频率 (例如: 0=高频, 1=普通, 2=低频)
  `status` TINYINT NOT NULL DEFAULT 1,            -- 学习状态 (1=学习中, 2=已暂停, 3=已归档)
  
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`, `course_id`)
);
```

#### 3.3.3. `user_card_in_course` - 用户卡片课程归属表

此表是一个纯粹的、轻量的**关联表**。它只负责记录“哪张卡片被用户纳入了哪个课程的学习计划”，从而建立起 `srs_state` 和 `course_setting` 之间的多对多关系。

```sql
CREATE TABLE `user_card_in_course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `card_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_card_course` (`user_id`, `card_id`, `course_id`)
);
```

## 4. 核心架构与工作流程 (V8.0)

本系统采用**“课程级策略表 + 异步批量更新”**的架构，以实现管理操作的高性能、复习查询的高性能和用户设置的“准即时”生效。

### 4.1. 添加卡片组到记忆库

1.  **触发**: 用户在文章详情页的侧边栏，点击某个卡片组（Deck）的 `[+] 添加到我的记忆库` 按钮。
2.  **后台操作**:
    *   **a. 关联课程**: 系统检查该Deck所属的`post_id`，并找到其归属的`course_id`。
    *   **b. 创建课程策略 (如果不存在)**: 检查`user_course_srs_setting`表中是否存在 `(user_id, course_id)` 的记录。如果不存在，则创建一条，使用默认设置。
    *   **c. 遍历卡片**: 遍历该Deck中的每一张卡片 (`card_id`)。
        *   **i. 写入`user_card_in_course`**: 插入一条 `(user_id, card_id, course_id)` 的关联记录。如果已存在则忽略。
        *   **ii. 检查并创建`srs_state`**: 检查`user_card_srs_state`表中是否存在 `(user_id, card_id)` 的记录。如果不存在，则创建一条新的全局状态记录，并根据**当前最优策略**计算其初始的`review_due_at`。

### 4.2. 更改课程复习频率 (异步更新流程)

1.  **触发**: 用户在“我的记忆库”管理页面，将“Vue.js课程”的复习频率从“普通”改为“高频”。
2.  **同步操作 (极快)**:
    *   API接收请求，立即执行一条`UPDATE`语句，更新`user_course_srs_setting`表中对应课程的`frequency_setting`字段。此操作仅更新单行，瞬间完成。
    *   API向消息队列（如Redis Stream）提交一个后台任务，例如 `job: { type: 'RECALCULATE_DUE_DATES', userId: 123, courseId: 456 }`。
    *   立刻向前台返回成功响应。用户感觉操作已完成。
3.  **异步操作 (后台执行)**:
    *   后台的工作进程(Worker)消费该任务。
    *   Worker执行一个较重的批量更新逻辑：
        1.  对于该`course_id`下的每一张受影响的卡片。
        2.  重新动态聚合计算出该卡片的**最优策略**（取其所属所有活跃课程中的最高频率）。
        3.  使用最新的最优策略，重新计算该卡片的`review_due_at`。
        4.  将新的`review_due_at`更新回`user_card_srs_state`表。
    *   这个过程确保了用户的设置最终会准确地应用到所有相关的卡片上。

### 4.3. 查询今日复习队列 (高性能读取)

由于所有卡片的`review_due_at`都是预先计算好的，因此获取每日复习队列的查询非常简单和高效。

```sql
SELECT srs.*
FROM user_card_srs_state srs
WHERE
    srs.user_id = ?
    AND srs.review_due_at <= NOW()
    -- 并且该卡片至少在一个课程中是活跃状态
    AND EXISTS (
        SELECT 1 
        FROM user_card_in_course ctx
        JOIN user_course_srs_setting s ON ctx.course_id = s.course_id AND ctx.user_id = s.user_id
        WHERE ctx.card_id = srs.card_id AND s.status = 1 AND s.user_id = srs.user_id
    );
```

## 5. 核心业务逻辑与服务 (重构)

- **`UserMemoryBankService`**: 核心服务。负责处理用户与记忆库的交互，包括添加卡片组、修改课程级设置(`user_course_srs_setting`)，并向消息队列**发布异步任务**。
- **`ReviewService`**: 负责执行复习流程。包括**查询今日复习队列**，以及在用户提交复习结果后，根据SM-2算法**计算并更新单张卡片的`srs_state`**。
- **`AsyncRecalculationWorker`**: 独立的后台服务。负责**消费异步任务**，执行耗时较长的批量`review_due_at`重新计算和更新操作。
- **`MemoryCardDeckService`**: 保持不变，负责卡片组内容的管理。
- **`AdminService`**: 保持不变，负责审核。

## 6. 核心用户交互 (UI/UX) (更新)

### A. 文章列表页 (Post List Page) - 功能预告

1.  **功能预告**: 在文章列表页，每篇文章的条目旁边，应低调地展示一个图标和数字（如 `🃏(8)`），直观地告诉用户该文章包含的记忆卡片组数量。
2.  **交互提示**: 当用户鼠标悬浮在此图标上时，显示提示文字：“查看详情页以浏览和添加记忆卡片组”，以此建立用户预期。
3.  **侧边栏不变**: 在此页面，右侧边栏保持其全局通用内容（如热门作者、公告等），不作改变。

### B. 文章详情页 (Post Detail Page) - 沉浸式卡片组侧边栏

1.  **侧边栏重定义**: 当用户进入文章详情页时，整个右侧边栏的职责将**完全切换**，专用于显示与当前文章关联的“**记忆卡片组列表**”。

2.  **侧边栏布局**:
    *   **固定头部 (Sticky Header)**: 边栏最顶部是一个不随内容滚动的固定区域，包含：
        *   当前文章的简洁标题，作为上下文提示。
        *   文章作者的头像和名称。
        *   一个醒目的 **`[+] 创建新卡片组`** 按钮。
    *   **可滚动列表 (Scrollable List)**: 头部下方是卡片组的完整列表，支持**无限滚动（下拉加载）**。

3.  **列表功能**:
    *   **排序与筛选**: 在列表顶部提供简单的控件，如 `[ 按热度排序 ] / [ 按时间排序 ]` 和 `[ ] 只看作者`，方便用户在大量内容中筛选。
    *   **列表项**: 每个卡片组项清晰展示其**标题、创建者、点赞数、卡片数量**，并附带一个 **`[+] 添加到我的记忆库`** 的核心操作按钮。

4.  **空状态设计**:
    *   如果当前文章还没有任何卡片组，侧边栏将显示友好的引导信息，如：“**成为第一个贡献者！** [立即为本文创建卡片组]”，将消极的空状态转化为积极的参与入口。

### C. 复习中心与记忆库管理

1.  **复习中心**:
    *   **统一队列**: 保持不变。查询逻辑见`4.3`。
    *   **专注界面**: 保持不变。
    *   **自我评估**: 保持不变。用户提交反馈后，`ReviewService`将更新`user_card_srs_state`表。
    *   **上下文追溯**: 复习时，应标注卡片来源。由于一张卡可能来自多个课程，可以优先显示**最优策略**（最高频率）的那个课程来源，并提供一个查看所有来源的选项。

2.  **我的记忆库管理页**:
    *   **聚合视图**: 页面应以**课程**为单位进行聚合展示。后台通过查询用户的`user_course_srs_setting`表来构建此视图。
    *   **课程级操作**: 对每个课程条目，提供`[暂停/恢复]`, `[调整频率]`, `[移除]`等操作。这些操作将直接、快速地更新`user_course_srs_setting`表中的单行记录，并触发后台异步任务。
    *   **钻取详情**: 提供`[查看详情]`入口，点击后可展示该课程下包含的所有卡片组列表，并允许对单个卡片组进行更细粒度的操作（例如，从特定课程中移除某个Deck）。

### D. 内容生命周期管理
- **版本化策略**: 当卡片内容更新时，正在复习旧版卡片的用户会收到通知。用户选择更新后，系统会更新卡片的版本号和deck的版本号，如果选择不更新，系统只会更新deck的版本号