package com.twicemax.memory.review;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCardSrsDO {

    // ========== 卡片状态常量 ==========
    public static final byte TYPE_NEW = 0;          // 新卡片
    public static final byte TYPE_LEARNING = 1;     // 学习中
    public static final byte TYPE_REVIEW = 2;       // 复习
    public static final byte TYPE_RELEARNING = 3;   // 重新学习

    // ========== 基础字段 ==========

    private Long id;

    private Long userId;

    private Long cardId;

    private Long nodeId;  // 新增：记忆卡片所属的节点ID

    private Long deckId;  // 冗余字段：记忆卡片组ID，用于快速过滤被屏蔽的卡片组

    private Long courseId;  // 冗余字段：用户在哪个课程下学习这个节点，用于按课程分组复习

    private Integer deckVersion;

    private Long cardVersionId;

    // ========== Anki 算法字段 ==========

    /**
     * 卡片状态
     * 0=NEW(新卡片), 1=LEARNING(学习中), 2=REVIEW(复习), 3=RELEARNING(重新学习)
     */
    private Byte type;

    /**
     * 当前学习/重学步骤索引
     * 仅在 type=LEARNING(1) 或 type=RELEARNING(3) 时有意义
     */
    private Byte currentStep;

    /**
     * 复习间隔（天）
     * 仅在 type=REVIEW(2) 时有意义
     */
    private Integer interval;

    /**
     * 下次再现的复习卡片计数
     * 仅在 type=LEARNING(1) 或 type=RELEARNING(3) 时有意义
     * 当用户的 reviewCardCount >= reappearAt 时，卡片可展现
     */
    private Long reappearAt;

    /**
     * 遗忘前的间隔(天)
     * 仅在 type=RELEARNING(3) 时使用，用于计算重新毕业后的恢复间隔
     * 其他状态下为 NULL
     */
    private Short lapseOldInterval;

    // ========== 其他字段 ==========

    private LocalDateTime reviewDueAt;

    private LocalDateTime lastReviewedAt;

    private BigDecimal easeFactor;

    private Integer repetitions;

    private Integer lapseCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}