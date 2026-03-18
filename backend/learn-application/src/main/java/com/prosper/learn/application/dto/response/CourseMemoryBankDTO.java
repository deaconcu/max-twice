package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import lombok.Data;

/**
 * 课程记忆库信息响应DTO
 */
@Data
public class CourseMemoryBankDTO {

    private CourseBriefDTO course;

    private UserCourseSrsSettingDTO setting;

    // 今天需要复习多少张卡片
    private Integer dueCardCount;

    // 有多少张卡片从来没有学习过
    private Integer newCardCount;

    // 有多少张卡片处于复习中
    private Integer reviewCardCount;

    // 今日已学新卡数
    private Integer todayNewCount;

    // 今日已复习数
    private Integer todayReviewCount;

}