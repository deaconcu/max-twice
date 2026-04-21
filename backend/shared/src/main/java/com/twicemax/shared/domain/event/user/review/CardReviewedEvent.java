package com.twicemax.shared.domain.event.user.review;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * 记忆卡片复习完成事件
 * 当用户完成一张卡片的复习时触发
 */
@Data
@AllArgsConstructor
public class CardReviewedEvent {

    /** 用户ID */
    private Long userId;

    /** 卡片ID */
    private Long cardId;

    /** 复习结果 (1=AGAIN, 2=HARD, 3=GOOD, 4=EASY) */
    private Integer result;

    /** 用户时区的今天日期（用于更新连续复习天数） */
    private LocalDate userToday;
}
