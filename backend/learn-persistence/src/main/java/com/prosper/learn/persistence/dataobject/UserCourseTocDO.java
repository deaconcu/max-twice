package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseTocDO {

    private Long id;

    private Long userId;

    private Long courseId;

    private String toc;

    private LocalDateTime updatedAt;
}
