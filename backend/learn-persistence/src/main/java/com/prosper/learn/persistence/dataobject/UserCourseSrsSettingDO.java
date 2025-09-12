package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseSrsSettingDO {

    private Long id;

    private Long userId;

    private Long courseId;

    private Byte frequencySetting;

    private Byte status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}