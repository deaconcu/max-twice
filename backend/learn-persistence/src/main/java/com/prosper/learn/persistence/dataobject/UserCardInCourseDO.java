package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCardInCourseDO {

    private Long id;

    private Long userId;

    private Long cardId;

    private Long courseId;

    private LocalDateTime createdAt;

}