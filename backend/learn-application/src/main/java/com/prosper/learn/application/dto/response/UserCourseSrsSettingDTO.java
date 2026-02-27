package com.prosper.learn.application.dto.response;

import lombok.Data;

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

}