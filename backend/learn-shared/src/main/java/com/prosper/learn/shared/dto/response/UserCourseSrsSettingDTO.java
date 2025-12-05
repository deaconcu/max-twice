package com.prosper.learn.shared.dto.response;

import lombok.Data;

/**
 * 用户课程SRS设置响应DTO
 */
@Data
public class UserCourseSrsSettingDTO {

    private Long id;

    private Integer frequencySetting;

    private Integer state;

}