package com.twicemax.memory.review;

import lombok.Data;

/**
 * 课程记忆库统计信息DO
 */
@Data
public class CourseMemoryBankDO {

    private Long courseId;

    // 今天需要复习多少张卡片（learningCount + reviewCardCount）
    private Integer dueCardCount;

    // 有多少张卡片从来没有学习过
    private Integer newCardCount;

    // LEARNING + RELEARNING 卡片数
    private Integer learningCount;

    // REVIEW 且到期的卡片数
    private Integer reviewCardCount;

}