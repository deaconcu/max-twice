# 记忆卡片系统设计方案

## 1. 系统概述

基于艾宾浩斯遗忘曲线的智能记忆卡片系统，为每个学习内容(Post)自动生成记忆卡片，并按照科学的复习间隔安排用户复习，提高长期记忆效果。

## 2. 核心特性

### 2.1 自动生成记忆卡片
- 基于Post内容自动提取关键知识点
- 生成问答形式的记忆卡片
- 支持手动编辑和自定义卡片

### 2.2 智能复习调度
- 基于遗忘曲线算法计算复习时间
- 根据用户回答质量动态调整复习间隔
- 优先级队列管理待复习卡片

### 2.3 学习进度跟踪
- 记录每次复习的准确率和用时
- 生成个人学习报告和统计图表
- 预测知识点的遗忘风险

## 3. 数据库设计

### 3.1 记忆卡片表 (memory_cards)
```sql
CREATE TABLE memory_cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    card_type ENUM('QA', 'FILL_BLANK', 'CHOICE', 'IMAGE') DEFAULT 'QA',
    difficulty_level TINYINT DEFAULT 1, -- 1-5难度等级
    source_type ENUM('AUTO_GENERATED', 'MANUAL', 'AI_ENHANCED') DEFAULT 'AUTO_GENERATED',
    is_active BOOLEAN DEFAULT TRUE,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_post_user (post_id, user_id),
    INDEX idx_user_active (user_id, is_active),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 3.2 复习记录表 (review_records)
```sql
CREATE TABLE review_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    card_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    review_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    quality_rating TINYINT NOT NULL, -- 1-5评分 (1=完全忘记, 5=完美记住)
    response_time_seconds INT, -- 回答用时(秒)
    is_correct BOOLEAN,
    next_review_time DATETIME NOT NULL,
    interval_days DECIMAL(5,2) NOT NULL, -- 复习间隔(天)
    easiness_factor DECIMAL(3,2) DEFAULT 2.5, -- 简易因子
    repetitions INT DEFAULT 0, -- 重复次数
    
    INDEX idx_card_user (card_id, user_id),
    INDEX idx_next_review (user_id, next_review_time),
    FOREIGN KEY (card_id) REFERENCES memory_cards(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 3.3 学习统计表 (learning_stats)
```sql
CREATE TABLE learning_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    stat_date DATE NOT NULL,
    cards_reviewed INT DEFAULT 0,
    cards_correct INT DEFAULT 0,
    total_review_time_minutes INT DEFAULT 0,
    new_cards_created INT DEFAULT 0,
    average_quality_rating DECIMAL(3,2),
    
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_user_date (user_id, stat_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 4. 核心算法

### 4.1 遗忘曲线算法 (基于SM-2算法改进)

```javascript
class ForgettingCurveAlgorithm {
    /**
     * 计算下次复习时间
     * @param {number} quality - 回答质量评分 (1-5)
     * @param {number} repetitions - 当前重复次数
     * @param {number} easinessFactor - 简易因子
     * @param {number} currentInterval - 当前间隔天数
     * @returns {object} {nextInterval, nextReviewTime, newEasinessFactor, newRepetitions}
     */
    calculateNextReview(quality, repetitions, easinessFactor, currentInterval) {
        let newEasinessFactor = Math.max(1.3, 
            easinessFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        );
        
        let newRepetitions = repetitions;
        let nextInterval;
        
        if (quality < 3) {
            // 回答质量差，重新开始
            newRepetitions = 0;
            nextInterval = 1;
        } else {
            newRepetitions = repetitions + 1;
            
            if (newRepetitions === 1) {
                nextInterval = 1;
            } else if (newRepetitions === 2) {
                nextInterval = 6;
            } else {
                nextInterval = Math.round(currentInterval * newEasinessFactor);
            }
        }
        
        const nextReviewTime = new Date();
        nextReviewTime.setDate(nextReviewTime.getDate() + nextInterval);
        
        return {
            nextInterval,
            nextReviewTime,
            newEasinessFactor,
            newRepetitions
        };
    }
}
```

### 4.2 卡片生成算法

```javascript
class CardGenerator {
    /**
     * 从Post内容自动生成记忆卡片
     * @param {string} postContent - Post内容
     * @param {string} postTitle - Post标题
     * @returns {Array} 生成的卡片数组
     */
    generateCardsFromPost(postContent, postTitle) {
        const cards = [];
        
        // 1. 提取关键概念
        const keyConcepts = this.extractKeyConcepts(postContent);
        
        // 2. 生成概念定义类卡片
        keyConcepts.forEach(concept => {
            cards.push({
                type: 'QA',
                question: `什么是${concept.term}？`,
                answer: concept.definition,
                difficulty: this.calculateDifficulty(concept)
            });
        });
        
        // 3. 提取代码片段生成编程类卡片
        const codeBlocks = this.extractCodeBlocks(postContent);
        codeBlocks.forEach(code => {
            cards.push({
                type: 'FILL_BLANK',
                question: `完成以下代码：\n${code.incomplete}`,
                answer: code.complete,
                difficulty: 3
            });
        });
        
        // 4. 生成应用题
        const applications = this.extractApplications(postContent);
        applications.forEach(app => {
            cards.push({
                type: 'QA',
                question: `如何在实际项目中应用${app.concept}？`,
                answer: app.example,
                difficulty: 4
            });
        });
        
        return cards;
    }
    
    extractKeyConcepts(content) {
        // AI/NLP处理提取关键概念
        // 这里可以集成ChatGPT或其他AI服务
        return [];
    }
    
    extractCodeBlocks(content) {
        // 正则表达式提取代码块
        const codeRegex = /```[\s\S]*?```/g;
        return content.match(codeRegex) || [];
    }
}
```

## 5. 后端服务设计

### 5.1 MemoryCardService
```java
@Service
@RequiredArgsConstructor
public class MemoryCardService {
    
    private final MemoryCardMapper memoryCardMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ForgettingCurveAlgorithm forgettingCurve;
    
    /**
     * 为Post生成记忆卡片
     */
    public List<MemoryCardDTO> generateCardsForPost(int postId, int userId) {
        PostDO post = postMapper.getById(postId);
        
        // 调用AI服务生成卡片
        List<MemoryCardDTO> cards = aiCardGenerator.generateCards(post.getContent(), post.getTitle());
        
        // 保存到数据库
        cards.forEach(card -> {
            MemoryCardDO cardDO = new MemoryCardDO();
            cardDO.setPostId(postId);
            cardDO.setUserId(userId);
            cardDO.setQuestion(card.getQuestion());
            cardDO.setAnswer(card.getAnswer());
            cardDO.setCardType(card.getType());
            cardDO.setDifficultyLevel(card.getDifficulty());
            memoryCardMapper.insert(cardDO);
        });
        
        return cards;
    }
    
    /**
     * 获取待复习的卡片
     */
    public List<MemoryCardDTO> getCardsForReview(int userId, int limit) {
        return memoryCardMapper.getCardsForReview(userId, new Date(), limit);
    }
    
    /**
     * 记录复习结果
     */
    public void recordReview(int cardId, int userId, int quality, int responseTime) {
        // 获取上次复习记录
        ReviewRecordDO lastRecord = reviewRecordMapper.getLastRecord(cardId, userId);
        
        // 计算下次复习时间
        ForgettingCurveResult result = forgettingCurve.calculateNextReview(
            quality, 
            lastRecord.getRepetitions(), 
            lastRecord.getEasinessFactor(),
            lastRecord.getIntervalDays()
        );
        
        // 保存复习记录
        ReviewRecordDO record = new ReviewRecordDO();
        record.setCardId(cardId);
        record.setUserId(userId);
        record.setQualityRating(quality);
        record.setResponseTimeSeconds(responseTime);
        record.setIsCorrect(quality >= 3);
        record.setNextReviewTime(result.getNextReviewTime());
        record.setIntervalDays(result.getNextInterval());
        record.setEasinessFactor(result.getNewEasinessFactor());
        record.setRepetitions(result.getNewRepetitions());
        
        reviewRecordMapper.insert(record);
        
        // 更新学习统计
        updateLearningStats(userId, quality >= 3);
    }
}
```

### 5.2 REST API设计
```java
@RestController
@RequestMapping("/api/memory-cards")
@RequiredArgsConstructor
public class MemoryCardController {
    
    private final MemoryCardService memoryCardService;
    
    @PostMapping("/generate")
    public Response<List<MemoryCardDTO>> generateCards(@RequestBody GenerateCardsRequest request) {
        List<MemoryCardDTO> cards = memoryCardService.generateCardsForPost(
            request.getPostId(), 
            StpUtil.getLoginIdAsInt()
        );
        return Response.success(cards);
    }
    
    @GetMapping("/review")
    public Response<List<MemoryCardDTO>> getReviewCards(@RequestParam(defaultValue = "10") int limit) {
        List<MemoryCardDTO> cards = memoryCardService.getCardsForReview(
            StpUtil.getLoginIdAsInt(), 
            limit
        );
        return Response.success(cards);
    }
    
    @PostMapping("/review")
    public Response<Void> submitReview(@RequestBody ReviewSubmissionRequest request) {
        memoryCardService.recordReview(
            request.getCardId(),
            StpUtil.getLoginIdAsInt(),
            request.getQuality(),
            request.getResponseTime()
        );
        return Response.success();
    }
    
    @GetMapping("/stats")
    public Response<LearningStatsDTO> getLearningStats() {
        LearningStatsDTO stats = memoryCardService.getLearningStats(StpUtil.getLoginIdAsInt());
        return Response.success(stats);
    }
}
```

## 6. 前端界面设计

### 6.1 记忆卡片组件
```vue
<template>
  <div class="memory-card-container">
    <!-- 卡片正面（问题） -->
    <v-card v-if="!showAnswer" class="memory-card question-side" elevation="8">
      <v-card-title class="text-h6">
        <v-icon icon="mdi-help-circle" class="mr-2"></v-icon>
        问题 {{ currentIndex + 1 }} / {{ totalCards }}
      </v-card-title>
      
      <v-card-text class="question-content">
        <div v-html="currentCard.question"></div>
      </v-card-text>
      
      <v-card-actions class="justify-center">
        <v-btn 
          color="primary" 
          variant="flat" 
          @click="showAnswer = true"
          prepend-icon="mdi-eye"
        >
          查看答案
        </v-btn>
      </v-card-actions>
    </v-card>
    
    <!-- 卡片背面（答案和评分） -->
    <v-card v-else class="memory-card answer-side" elevation="8">
      <v-card-title class="text-h6">
        <v-icon icon="mdi-lightbulb" class="mr-2"></v-icon>
        答案
      </v-card-title>
      
      <v-card-text class="answer-content">
        <div v-html="currentCard.answer"></div>
      </v-card-text>
      
      <v-divider></v-divider>
      
      <v-card-text>
        <p class="text-subtitle-2 mb-3">回忆质量评分：</p>
        <div class="quality-buttons">
          <v-btn
            v-for="(option, index) in qualityOptions"
            :key="index"
            :color="option.color"
            variant="outlined"
            class="mx-1"
            @click="submitReview(option.value)"
          >
            {{ option.value }} - {{ option.label }}
          </v-btn>
        </div>
      </v-card-text>
    </v-card>
    
    <!-- 进度条 -->
    <v-progress-linear 
      :model-value="(currentIndex / totalCards) * 100"
      color="primary"
      class="mt-4"
    ></v-progress-linear>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  cards: { type: Array, required: true }
});

const currentIndex = ref(0);
const showAnswer = ref(false);
const startTime = ref(Date.now());

const currentCard = computed(() => props.cards[currentIndex.value]);
const totalCards = computed(() => props.cards.length);

const qualityOptions = [
  { value: 1, label: '完全忘记', color: 'error' },
  { value: 2, label: '困难回忆', color: 'warning' },
  { value: 3, label: '勉强记住', color: 'orange' },
  { value: 4, label: '轻松回忆', color: 'success' },
  { value: 5, label: '完美记住', color: 'green' }
];

const submitReview = (quality) => {
  const responseTime = Math.floor((Date.now() - startTime.value) / 1000);
  
  // 提交复习结果
  emit('review-submitted', {
    cardId: currentCard.value.id,
    quality,
    responseTime
  });
  
  // 下一张卡片
  nextCard();
};

const nextCard = () => {
  if (currentIndex.value < totalCards.value - 1) {
    currentIndex.value++;
    showAnswer.value = false;
    startTime.value = Date.now();
  } else {
    emit('review-completed');
  }
};
</script>
```

### 6.2 学习统计面板
```vue
<template>
  <div class="learning-stats-panel">
    <v-row>
      <!-- 今日统计 -->
      <v-col cols="12" md="4">
        <v-card class="stat-card">
          <v-card-title>今日复习</v-card-title>
          <v-card-text>
            <div class="stat-number">{{ todayStats.cardsReviewed }}</div>
            <div class="stat-label">张卡片</div>
            <v-progress-linear 
              :model-value="(todayStats.cardsCorrect / todayStats.cardsReviewed) * 100"
              color="success"
            ></v-progress-linear>
            <div class="stat-subtitle">正确率 {{ Math.round((todayStats.cardsCorrect / todayStats.cardsReviewed) * 100) }}%</div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <!-- 待复习 -->
      <v-col cols="12" md="4">
        <v-card class="stat-card">
          <v-card-title>待复习</v-card-title>
          <v-card-text>
            <div class="stat-number">{{ pendingCards.total }}</div>
            <div class="stat-label">张卡片</div>
            <div class="urgency-breakdown">
              <v-chip color="error" size="small">紧急: {{ pendingCards.urgent }}</v-chip>
              <v-chip color="warning" size="small">今日: {{ pendingCards.today }}</v-chip>
              <v-chip color="info" size="small">明日: {{ pendingCards.tomorrow }}</v-chip>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <!-- 学习时长 -->
      <v-col cols="12" md="4">
        <v-card class="stat-card">
          <v-card-title>学习时长</v-card-title>
          <v-card-text>
            <div class="stat-number">{{ formatTime(todayStats.totalTime) }}</div>
            <div class="stat-label">今日累计</div>
            <div class="weekly-average">
              本周平均: {{ formatTime(weeklyStats.averageTime) }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    
    <!-- 记忆曲线图表 -->
    <v-row class="mt-4">
      <v-col cols="12">
        <v-card>
          <v-card-title>记忆保持率趋势</v-card-title>
          <v-card-text>
            <canvas ref="chartCanvas"></canvas>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
```

## 7. 实施计划

### Phase 1: 基础功能 (4周)
- [ ] 数据库表设计和创建
- [ ] 基础的CRUD操作API
- [ ] 简单的卡片生成算法
- [ ] 基础的遗忘曲线算法实现

### Phase 2: 核心功能 (6周)
- [ ] AI辅助的卡片生成服务
- [ ] 完整的复习调度系统
- [ ] 前端记忆卡片界面
- [ ] 学习统计和报告功能

### Phase 3: 高级功能 (4周)
- [ ] 个性化推荐算法
- [ ] 社交分享功能
- [ ] 移动端优化
- [ ] 数据导入导出

### Phase 4: 优化和扩展 (2周)
- [ ] 性能优化
- [ ] 用户体验改进
- [ ] 多媒体卡片支持
- [ ] 游戏化元素

## 8. 技术栈

### 后端
- Spring Boot 3.x
- MyBatis Plus
- Redis (缓存复习队列)
- MySQL 8.0
- AI服务集成 (OpenAI GPT-4 / 本地NLP)

### 前端
- Vue 3 + Vuetify 3
- Chart.js (统计图表)
- PWA支持 (离线复习)

### 部署
- Docker容器化
- CI/CD自动部署
- 监控和日志系统

## 9. 预期效果

1. **学习效率提升**: 基于科学的遗忘曲线，预期可提高30-50%的长期记忆保持率
2. **个性化学习**: 根据用户表现动态调整，形成个人专属的学习节奏
3. **数据驱动**: 详细的学习数据分析，帮助用户了解自己的学习模式
4. **用户粘性**: 游戏化和社交化的设计增强用户参与度

## 10. 风险评估

### 技术风险
- AI生成卡片质量不稳定 → 建立人工审核机制
- 算法复杂度高 → 分阶段实现，先简后复
- 数据量增长快 → 数据库分表分库策略

### 产品风险
- 用户接受度不高 → A/B测试验证效果
- 学习负担过重 → 提供个性化设置选项
- 内容版权问题 → 明确使用条款和用户协议

这个记忆卡片系统将显著提升用户的学习效果和平台粘性，是一个很有价值的功能增强。