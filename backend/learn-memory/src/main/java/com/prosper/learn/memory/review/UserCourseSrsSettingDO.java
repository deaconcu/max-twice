package com.prosper.learn.memory.review;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseSrsSettingDO {

    private Long id;

    private Long userId;

    private Long courseId;

    private Byte frequencySetting;

    private Byte state;

    /**
     * 新卡与复习卡顺序
     * @see com.prosper.learn.shared.domain.Enums.CardOrder
     */
    private Byte cardOrder;

    /**
     * 每日新卡上限（默认20）
     */
    private Integer dailyNewLimit;

    /**
     * 每日复习上限（默认100）
     */
    private Integer dailyReviewLimit;

    /**
     * 冻结开始时间
     */
    private LocalDateTime frozenAt;

    /**
     * 累计冻结时长（秒）
     */
    private Long frozenDuration;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}