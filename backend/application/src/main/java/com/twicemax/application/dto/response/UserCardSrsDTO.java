package com.twicemax.application.dto.response;

import com.twicemax.application.dto.response.course.CourseBriefDTO;
import lombok.Data;

/**
 * 用户卡片SRS状态响应DTO
 */
@Data
public class UserCardSrsDTO {

    private Long id;

    /**
     * 所属课程
     */
    private CourseBriefDTO course;

    /**
     * 卡片状态
     * 0=NEW(新卡片), 1=LEARNING(学习中), 2=REVIEW(复习), 3=RELEARNING(重新学习)
     */
    private Byte type;

    /**
     * 当前学习/重学步骤索引
     * 仅在 type=1(LEARNING) 或 type=3(RELEARNING) 时有意义
     */
    private Byte currentStep;

    /**
     * 复习间隔
     * 单位由 type 决定:
     * - type=1(LEARNING) 或 3(RELEARNING): 单位为分钟
     * - type=2(REVIEW): 单位为天
     */
    private Integer interval;

    private String reviewDueAt;

    private String lastReviewedAt;

    private Integer repetitions;

    private Integer lapseCount;

}