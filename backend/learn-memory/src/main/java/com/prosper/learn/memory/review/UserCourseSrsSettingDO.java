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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}