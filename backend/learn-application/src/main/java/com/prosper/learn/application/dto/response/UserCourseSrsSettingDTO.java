package com.prosper.learn.application.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户课程SRS设置响应DTO
 */
@Data
public class UserCourseSrsSettingDTO {

    private Long id;

    private Integer frequencySetting;

    private Integer state;

    /**
     * 卡片顺序：0=先复习后新卡，1=先新卡后复习
     */
    private Integer cardOrder;

    /**
     * 每日新卡上限
     */
    private Integer dailyNewLimit;

    /**
     * 每日复习上限
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

}