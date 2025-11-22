package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.course.CourseBriefDTO;
import lombok.Data;

import java.util.List;

/**
 * 课程记忆库信息响应DTO
 */
@Data
public class CourseMemoryBankDTO {

    private CourseBriefDTO course;

    private UserCourseSrsSettingDTO setting;

    // 当前在学多少张卡片
    private Integer cardCount;

    // 今天需要复习多少张卡片
    private Integer dueCardCount;

    // 有多少张卡片从来没有学习过
    private Integer newCardCount;

    // 有多少张卡片处于复习中
    private Integer reviewCardCount;

    // 有多少张卡片已经学会了
    private Integer learnedCardCount;

}