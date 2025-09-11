package com.prosper.learn.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 课程记忆库信息响应DTO
 */
@Data
public class CourseMemoryBankDTO {

    private CourseDTO course;

    private UserCourseSrsSettingDTO setting;

    private Integer cardCount;

    private Integer dueCardCount;

    private Integer newCardCount;

    private Integer reviewCardCount;

    private Integer learnedCardCount;

}