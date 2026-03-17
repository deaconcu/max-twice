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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}