# 《复习算法 Anki 化改造 - 实施 Checklist》

**版本**: 1.0
**日期**: 2025-10-30
**关联文档**: `anki_refactor_plan.md`

---

## 📋 实施阶段总览

```
[阶段 1] 准备工作 → [阶段 2] 数据库变更 → [阶段 3] 后端重构 → [阶段 4] 测试验证 → [阶段 5] 部署上线
```

---

## 阶段 1: 准备工作

### 1.1 代码审查与分析
- [ ] 阅读并理解 `anki_refactor_plan.md` 完整内容
- [ ] 审查现有 `ReviewService.java` 代码逻辑
- [ ] 审查现有 `UserCardSrsDO` 数据模型
- [ ] 审查现有 `UserCardSrsMapper` 查询语句
- [ ] 识别所有调用 `submitReview` 的前端页面/接口
- [ ] 识别所有调用 `getReviewQueue` 的前端页面/接口

### 1.2 配置参数确认
- [ ] 与产品/用户确认默认配置值:
  - [ ] `learningSteps = [10, 60]` (分钟)
  - [ ] `relearningSteps = [20]` (分钟)
  - [ ] `graduatingInterval = 1` (天)
  - [ ] `easyInterval = 4` (天)
  - [ ] `easyBonus = 1.3`
  - [ ] `newIntervalMultiplier = 0.5` (50%)
- [ ] 决定配置存储位置: `application.yml` (全局) 或数据库 (用户级)

### 1.3 环境准备
- [ ] 创建开发分支: `feature/anki-algorithm`
- [ ] 准备本地测试数据库
- [ ] 备份现有 `user_card_srs` 表结构和数据 (如有必要)

---

## 阶段 2: 数据库变更

### 2.1 编写 Flyway 迁移脚本
- [ ] 创建迁移文件: `V{version}__add_anki_algorithm_fields.sql`
- [ ] 编写 SQL:
  ```sql
  -- 2.1.1 新增 type 字段
  ALTER TABLE user_card_srs
  ADD COLUMN type TINYINT NOT NULL DEFAULT 0
  COMMENT '0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING';

  -- 2.1.2 新增 current_step 字段
  ALTER TABLE user_card_srs
  ADD COLUMN current_step TINYINT NOT NULL DEFAULT 0
  COMMENT '学习/重学步骤索引';

  -- 2.1.3 重命名 interval_days 字段
  ALTER TABLE user_card_srs
  CHANGE COLUMN interval_days interval INT NOT NULL
  COMMENT '间隔: type=1/3时单位分钟, type=2时单位天';

  -- 2.1.4 新增 lapse_old_interval 字段
  ALTER TABLE user_card_srs
  ADD COLUMN lapse_old_interval SMALLINT NULL
  COMMENT '遗忘前的间隔(天), 仅RELEARNING状态使用';
  ```

### 2.2 创建索引
- [ ] 编写索引创建 SQL:
  ```sql
  -- 2.2.1 复习队列查询优化索引
  CREATE INDEX idx_user_review_queue
  ON user_card_srs(user_id, type, review_due_at);

  -- 2.2.2 新卡片查询优化索引
  CREATE INDEX idx_user_new_cards
  ON user_card_srs(user_id, type, id);
  ```

### 2.3 数据迁移脚本 (如需要)
- [ ] 如果有现有数据, 编写初始化脚本:
  ```sql
  -- 将所有现有卡片初始化为 REVIEW 状态
  UPDATE user_card_srs
  SET type = 2, current_step = 0
  WHERE type = 0;  -- 假设旧数据默认 type=0
  ```

### 2.4 执行与验证
- [ ] 在本地开发环境执行迁移脚本
- [ ] 验证表结构变更成功:
  - [ ] `DESCRIBE user_card_srs;` 检查字段
  - [ ] `SHOW INDEX FROM user_card_srs;` 检查索引
- [ ] 验证现有数据完整性 (如有)

---

## 阶段 3: 后端重构

### 3.1 更新数据模型层

#### 3.1.1 修改 `UserCardSrsDO`
- [ ] 新增字段:
  ```java
  private Byte type;  // 0=NEW, 1=LEARNING, 2=REVIEW, 3=RELEARNING
  private Byte currentStep;
  private Short lapseOldInterval;  // 使用 Short (对应 SMALLINT)
  ```
- [ ] 重命名字段: `intervalDays` → `interval`
- [ ] 添加常量定义:
  ```java
  public static final byte TYPE_NEW = 0;
  public static final byte TYPE_LEARNING = 1;
  public static final byte TYPE_REVIEW = 2;
  public static final byte TYPE_RELEARNING = 3;
  ```

#### 3.1.2 修改 MyBatis Mapper XML
- [ ] 更新所有 SQL 中的字段名: `interval_days` → `interval`
- [ ] 更新 ResultMap 映射: 添加 `type`, `currentStep`, `lapseOldInterval`
- [ ] 更新 INSERT/UPDATE 语句

### 3.2 创建配置类
- [ ] 创建 `SrsAlgorithmConfig.java`:
  ```java
  @Configuration
  @ConfigurationProperties(prefix = "srs.algorithm")
  public class SrsAlgorithmConfig {
      private int[] learningSteps = {10, 60};
      private int[] relearningSteps = {20};
      private int graduatingInterval = 1;
      private int easyInterval = 4;
      private double easyBonus = 1.3;
      private double newIntervalMultiplier = 0.5;
      // getters/setters
  }
  ```
- [ ] 在 `application.yml` 添加配置:
  ```yaml
  srs:
    algorithm:
      learning-steps: [10, 60]
      relearning-steps: [20]
      graduating-interval: 1
      easy-interval: 4
      easy-bonus: 1.3
      new-interval-multiplier: 0.5
  ```

### 3.3 重构 `ReviewService`

#### 3.3.1 重构 `submitReview` 主方法
- [ ] 保留现有方法签名 (保持 API 兼容)
- [ ] 实现状态分发逻辑:
  ```java
  public void submitReview(Long userId, ReviewCardRequest request) {
      UserCardSrsDO card = // 获取卡片

      switch (card.getType()) {
          case TYPE_NEW:
              handleNewCard(card, request.getResult());
              break;
          case TYPE_LEARNING:
              handleLearningCard(card, request.getResult(), config.getLearningSteps());
              break;
          case TYPE_REVIEW:
              handleReviewCard(card, request.getResult());
              break;
          case TYPE_RELEARNING:
              handleLearningCard(card, request.getResult(), config.getRelearningSteps());
              break;
      }

      // 更新数据库
      updateCardSrs(card);
  }
  ```

#### 3.3.2 实现 `handleNewCard()`
- [ ] 评级 1/2: 进入 LEARNING, step=0, interval=learningSteps[0]
- [ ] 评级 3: 进入 LEARNING, step=1, 检查是否毕业
- [ ] 评级 4: 直接进入 REVIEW, interval=easyInterval
- [ ] 单元测试编写 (见阶段 4)

#### 3.3.3 实现 `handleLearningCard(steps)`
- [ ] 评级 1: step=0, interval=steps[0]
- [ ] 评级 2:
  - [ ] step 不变
  - [ ] 计算平均间隔
  - [ ] 边界处理: 最后一步时使用 steps[currentStep]
- [ ] 评级 3:
  - [ ] step++
  - [ ] 检查毕业: `step >= steps.length`
  - [ ] 区分 LEARNING/RELEARNING 毕业逻辑:
    - [ ] LEARNING → REVIEW: interval = graduatingInterval
    - [ ] RELEARNING → REVIEW: interval = MAX(graduatingInterval, ⌊lapseOldInterval × newIntervalMultiplier⌋)
    - [ ] 清空 lapseOldInterval
- [ ] 评级 4:
  - [ ] 立即毕业
  - [ ] 区分 LEARNING/RELEARNING:
    - [ ] LEARNING → REVIEW: interval = easyInterval
    - [ ] RELEARNING → REVIEW: interval = MAX(easyInterval, ⌊lapseOldInterval × newIntervalMultiplier⌋)
  - [ ] 清空 lapseOldInterval
- [ ] 单元测试编写 (见阶段 4)

#### 3.3.4 实现 `handleReviewCard()`
- [ ] 评级 1 (遗忘):
  - [ ] **保存遗忘前间隔**: `lapseOldInterval = interval`
  - [ ] 进入 RELEARNING, step=0, interval=relearningSteps[0] (分钟)
  - [ ] repetitions = 0
  - [ ] lapseCount++
  - [ ] easeFactor -= 0.20 (最小 1.3)
- [ ] 评级 2 (困难):
  - [ ] repetitions++
  - [ ] interval = interval × 1.2 (天)
  - [ ] easeFactor -= 0.15 (最小 1.3)
- [ ] 评级 3 (良好):
  - [ ] repetitions++
  - [ ] interval = interval × easeFactor (天)
  - [ ] easeFactor 不变
- [ ] 评级 4 (简单):
  - [ ] repetitions++
  - [ ] interval = interval × easeFactor × easyBonus (天)
  - [ ] easeFactor += 0.15
- [ ] 所有评级: 更新 `reviewDueAt` = NOW() + interval
- [ ] 单元测试编写 (见阶段 4)

#### 3.3.5 实现 EF 更新工具方法
- [ ] 创建 `updateEaseFactor(card, change)`:
  ```java
  private void updateEaseFactor(UserCardSrsDO card, double change) {
      double newEF = card.getEaseFactor() + change;
      card.setEaseFactor(Math.max(1.3, newEF));  // 最小值 1.3
  }
  ```

#### 3.3.6 实现 `reviewDueAt` 计算工具方法
- [ ] 创建 `calculateDueTime(interval, unit)`:
  ```java
  private LocalDateTime calculateDueTime(int interval, TimeUnit unit) {
      if (unit == TimeUnit.MINUTES) {
          return LocalDateTime.now().plusMinutes(interval);
      } else {  // DAYS
          return LocalDateTime.now().plusDays(interval);
      }
  }
  ```

### 3.4 重构 `getReviewQueue`

#### 3.4.1 修改 Mapper 查询
- [ ] **方式一 (推荐)**: 实现 UNION ALL 单次查询
  ```sql
  <select id="getReviewQueue" resultMap="UserCardSrsResultMap">
      SELECT * FROM (
          -- 到期卡片
          SELECT * FROM user_card_srs
          WHERE user_id = #{userId}
            AND review_due_at &lt;= NOW()
            AND type IN (1, 2, 3)
          ORDER BY
              CASE type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 END,
              review_due_at ASC
          LIMIT #{limit}
      ) AS due_cards

      UNION ALL

      SELECT * FROM (
          -- 新卡片补充
          SELECT * FROM user_card_srs
          WHERE user_id = #{userId} AND type = 0
          ORDER BY id ASC
          LIMIT #{newCardLimit}
      ) AS new_cards

      LIMIT #{limit}
  </select>
  ```

- [ ] **方式二 (备选)**: 代码层两次查询
  ```java
  List<UserCardSrsDO> dueCards = mapper.getDueCards(userId, limit);
  if (dueCards.size() < limit) {
      int need = limit - dueCards.size();
      List<UserCardSrsDO> newCards = mapper.getNewCards(userId, need);
      dueCards.addAll(newCards);
  }
  return dueCards;
  ```

#### 3.4.2 测试队列查询
- [ ] 验证优先级: LEARNING > RELEARNING > REVIEW > NEW
- [ ] 验证到期时间排序
- [ ] 验证新卡片补充逻辑
- [ ] 验证 limit 限制

### 3.5 更新 DTO/Request 对象
- [ ] 检查 `ReviewCardRequest` 是否需要修改
- [ ] 检查返回的 DTO 是否需要添加新字段 (type, currentStep 等)
- [ ] 更新前端需要的响应字段

---

## 阶段 4: 测试验证

### 4.1 单元测试 - `handleNewCard()`

#### 4.1.1 评级 1 测试
- [ ] 输入: NEW 卡片, 评级=1
- [ ] 验证: type=LEARNING, step=0, interval=10分钟
- [ ] 验证: reviewDueAt = NOW() + 10分钟

#### 4.1.2 评级 2 测试
- [ ] 输入: NEW 卡片, 评级=2
- [ ] 验证: 结果同评级 1

#### 4.1.3 评级 3 测试 (不毕业)
- [ ] 输入: NEW 卡片, 评级=3, learningSteps=[10,60]
- [ ] 验证: type=LEARNING, step=1, interval=60分钟

#### 4.1.4 评级 3 测试 (直接毕业)
- [ ] 输入: NEW 卡片, 评级=3, learningSteps=[10] (单步)
- [ ] 验证: type=REVIEW, step=0, interval=1天

#### 4.1.5 评级 4 测试
- [ ] 输入: NEW 卡片, 评级=4
- [ ] 验证: type=REVIEW, step=0, interval=4天

### 4.2 单元测试 - `handleLearningCard()` (LEARNING)

#### 4.2.1 评级 1 测试
- [ ] 输入: LEARNING (step=1), 评级=1
- [ ] 验证: step=0, interval=10分钟

#### 4.2.2 评级 2 测试 (中间步骤)
- [ ] 输入: LEARNING (step=0, steps=[10,60]), 评级=2
- [ ] 验证: step=0, interval=(10+60)/2=35分钟

#### 4.2.3 评级 2 测试 (最后步骤)
- [ ] 输入: LEARNING (step=1, steps=[10,60]), 评级=2
- [ ] 验证: step=1, interval=60分钟 (不变)

#### 4.2.4 评级 3 测试 (推进步骤)
- [ ] 输入: LEARNING (step=0, steps=[10,60]), 评级=3
- [ ] 验证: step=1, interval=60分钟

#### 4.2.5 评级 3 测试 (毕业)
- [ ] 输入: LEARNING (step=1, steps=[10,60]), 评级=3
- [ ] 验证: type=REVIEW, step=0, interval=1天

#### 4.2.6 评级 4 测试
- [ ] 输入: LEARNING (任何step), 评级=4
- [ ] 验证: type=REVIEW, step=0, interval=4天

### 4.3 单元测试 - `handleReviewCard()`

#### 4.3.1 评级 1 测试 (遗忘)
- [ ] 输入: REVIEW (interval=30天, EF=2.5, repetitions=5), 评级=1
- [ ] 验证: type=RELEARNING, step=0, interval=20分钟
- [ ] 验证: lapseOldInterval=30 (保存)
- [ ] 验证: repetitions=0 (重置)
- [ ] 验证: lapseCount++ (增加1)
- [ ] 验证: easeFactor=2.3 (2.5-0.2)

#### 4.3.2 评级 2 测试 (困难)
- [ ] 输入: REVIEW (interval=10天, EF=2.5, repetitions=3), 评级=2
- [ ] 验证: type=REVIEW (不变), interval=12天 (10×1.2)
- [ ] 验证: repetitions=4 (增加)
- [ ] 验证: easeFactor=2.35 (2.5-0.15)

#### 4.3.3 评级 3 测试 (良好)
- [ ] 输入: REVIEW (interval=10天, EF=2.5, repetitions=3), 评级=3
- [ ] 验证: type=REVIEW, interval=25天 (10×2.5)
- [ ] 验证: repetitions=4
- [ ] 验证: easeFactor=2.5 (不变)

#### 4.3.4 评级 4 测试 (简单)
- [ ] 输入: REVIEW (interval=10天, EF=2.5, easyBonus=1.3), 评级=4
- [ ] 验证: type=REVIEW, interval=32天 (10×2.5×1.3, 向下取整)
- [ ] 验证: repetitions=4
- [ ] 验证: easeFactor=2.65 (2.5+0.15)

#### 4.3.5 EF 下限测试
- [ ] 输入: REVIEW (EF=1.3), 评级=1
- [ ] 验证: easeFactor=1.3 (不能低于最小值)

### 4.4 单元测试 - `handleLearningCard()` (RELEARNING)

#### 4.4.1 评级 1 测试 (重学期间再次重来)
- [ ] 输入: RELEARNING (step=0, lapseOldInterval=30), 评级=1
- [ ] 验证: step=0, interval=20分钟
- [ ] 验证: lapseOldInterval=30 (保持不变)

#### 4.4.2 评级 3 测试 (重新毕业)
- [ ] 输入: RELEARNING (step=0, steps=[20], lapseOldInterval=30, newIntervalMultiplier=0.5), 评级=3
- [ ] 验证: type=REVIEW, step=0
- [ ] 验证: interval=15天 (MAX(1, ⌊30×0.5⌋)=15)
- [ ] 验证: lapseOldInterval=NULL (清空)

#### 4.4.3 评级 3 测试 (低间隔保护)
- [ ] 输入: RELEARNING (lapseOldInterval=2, newIntervalMultiplier=0.5, graduatingInterval=1), 评级=3
- [ ] 验证: interval=1天 (MAX(1, ⌊2×0.5⌋)=MAX(1,1)=1)

#### 4.4.4 评级 4 测试 (重新毕业-简单)
- [ ] 输入: RELEARNING (lapseOldInterval=30, easyInterval=4, newIntervalMultiplier=0.5), 评级=4
- [ ] 验证: interval=15天 (MAX(4, ⌊30×0.5⌋)=15)
- [ ] 验证: lapseOldInterval=NULL

#### 4.4.5 评级 4 测试 (低间隔情况)
- [ ] 输入: RELEARNING (lapseOldInterval=6, easyInterval=4, newIntervalMultiplier=0.5), 评级=4
- [ ] 验证: interval=4天 (MAX(4, ⌊6×0.5⌋)=MAX(4,3)=4)

### 4.5 集成测试 - 完整学习流程

#### 4.5.1 首次学习流程
- [ ] NEW (评级3) → LEARNING (step=1)
- [ ] LEARNING (评级3) → REVIEW (interval=1天)
- [ ] REVIEW (评级3) → REVIEW (interval=2.5天)
- [ ] 验证每一步的状态正确

#### 4.5.2 遗忘-重学流程
- [ ] REVIEW (interval=30天, 评级1) → RELEARNING (lapseOldInterval=30)
- [ ] RELEARNING (评级3) → REVIEW (interval=15天)
- [ ] 验证 lapseOldInterval 正确保存和清空

#### 4.5.3 重学期间再次重来
- [ ] REVIEW (interval=30天, 评级1) → RELEARNING
- [ ] RELEARNING (评级1) → RELEARNING (step=0, lapseOldInterval保持30)
- [ ] RELEARNING (评级3) → REVIEW (interval=15天)

### 4.6 测试 - 队列查询

#### 4.6.1 优先级测试
- [ ] 创建测试数据: 各种状态的到期卡片
- [ ] 调用 getReviewQueue(userId, limit=10)
- [ ] 验证返回顺序: LEARNING > RELEARNING > REVIEW

#### 4.6.2 新卡片补充测试
- [ ] 创建测试数据: 3张到期卡片, 10张新卡片
- [ ] 调用 getReviewQueue(userId, limit=10)
- [ ] 验证返回: 3张到期 + 7张新卡片

#### 4.6.3 边界测试
- [ ] 0张到期卡片, 请求10张
- [ ] 验证返回: 10张新卡片
- [ ] 15张到期卡片, 请求10张
- [ ] 验证返回: 10张到期卡片 (不包含新卡片)

### 4.7 性能测试
- [ ] 插入 10,000 条测试数据
- [ ] 测试 getReviewQueue 查询性能 (应 < 100ms)
- [ ] 测试 submitReview 更新性能 (应 < 50ms)
- [ ] 验证索引是否生效: `EXPLAIN SELECT ...`

---

## 阶段 5: 前端适配 (如需要)

### 5.1 API 兼容性检查
- [ ] 检查 `submitReview` 接口签名是否改变
- [ ] 检查返回的 DTO 字段变化
- [ ] 前端是否需要展示新字段 (type, currentStep, lapseCount 等)

### 5.2 前端 UI 调整 (如需要)
- [ ] 评级按钮文案确认:
  - [ ] "重来" / "Again" (评级1)
  - [ ] "困难" / "Hard" (评级2)
  - [ ] "良好" / "Good" (评级3)
  - [ ] "简单" / "Easy" (评级4)
- [ ] 是否展示卡片状态 (NEW/LEARNING/REVIEW/RELEARNING)
- [ ] 是否展示学习进度 (step 1/2)
- [ ] 是否展示遗忘次数 (lapseCount)

### 5.3 前端测试
- [ ] 新卡片学习流程测试
- [ ] 复习卡片测试
- [ ] 遗忘-重学流程测试
- [ ] 队列刷新测试

---

## 阶段 6: 文档与部署

### 6.1 代码文档
- [ ] 为核心方法添加 Javadoc:
  - [ ] `handleNewCard()`
  - [ ] `handleLearningCard()`
  - [ ] `handleReviewCard()`
- [ ] 添加复杂逻辑的行内注释

### 6.2 用户文档 (如需要)
- [ ] 编写用户指南: 解释四个评级按钮的含义
- [ ] 编写 FAQ: 常见问题解答
- [ ] 编写算法说明: 向用户解释间隔重复原理

### 6.3 部署准备
- [ ] 代码 Review
- [ ] 合并到主分支
- [ ] 准备发布说明 (Release Notes)
- [ ] 准备回滚方案 (如果需要)

### 6.4 生产环境部署
- [ ] 在测试环境执行数据库迁移脚本
- [ ] 验证测试环境功能正常
- [ ] 在生产环境执行数据库迁移
- [ ] 部署后端代码
- [ ] 部署前端代码 (如有修改)
- [ ] 监控错误日志和性能指标

---

## 阶段 7: 监控与优化

### 7.1 数据监控
- [ ] 监控各状态卡片的数量分布:
  - [ ] NEW / LEARNING / REVIEW / RELEARNING 占比
- [ ] 监控平均遗忘率 (lapseCount / 总复习次数)
- [ ] 监控平均毕业时长 (NEW → REVIEW 的时间)
- [ ] 监控用户留存率变化

### 7.2 性能监控
- [ ] 监控 getReviewQueue 查询耗时
- [ ] 监控 submitReview 更新耗时
- [ ] 监控数据库连接池状态
- [ ] 检查慢查询日志

### 7.3 用户反馈收集
- [ ] 收集用户对新算法的反馈
- [ ] 调查用户对评级按钮的理解程度
- [ ] 统计用户的评级分布 (1/2/3/4 比例)

### 7.4 配置优化
- [ ] 根据数据分析调整配置值:
  - [ ] learningSteps 是否合适
  - [ ] newIntervalMultiplier 是否需要调整
  - [ ] easyInterval 是否过于激进

---

## 附录: 测试数据准备脚本

### 创建测试数据
```sql
-- 插入 NEW 状态测试卡片
INSERT INTO user_card_srs (user_id, card_id, type, current_step, interval, ease_factor, repetitions, lapse_count, review_due_at)
VALUES (1, 1001, 0, 0, 0, 2.5, 0, 0, NOW());

-- 插入 LEARNING 状态测试卡片 (到期)
INSERT INTO user_card_srs (user_id, card_id, type, current_step, interval, ease_factor, repetitions, lapse_count, review_due_at)
VALUES (1, 1002, 1, 0, 10, 2.5, 0, 0, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- 插入 REVIEW 状态测试卡片 (到期)
INSERT INTO user_card_srs (user_id, card_id, type, current_step, interval, ease_factor, repetitions, lapse_count, review_due_at)
VALUES (1, 1003, 2, 0, 30, 2.5, 5, 0, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 插入 RELEARNING 状态测试卡片
INSERT INTO user_card_srs (user_id, card_id, type, current_step, interval, ease_factor, repetitions, lapse_count, lapse_old_interval, review_due_at)
VALUES (1, 1004, 3, 0, 20, 2.3, 0, 1, 30, DATE_SUB(NOW(), INTERVAL 10 MINUTE));
```

---

## 进度追踪

**开始日期**: ___________
**预计完成日期**: ___________
**实际完成日期**: ___________

**当前阶段**: [ ] 阶段1 [ ] 阶段2 [ ] 阶段3 [ ] 阶段4 [ ] 阶段5 [ ] 阶段6 [ ] 阶段7

**阻塞问题**:
-

**备注**:
-
