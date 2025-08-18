package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseTocDO {

    private int id;

    private int userId;

    private int courseId;

    private String toc;

    private LocalDateTime updatedAt;
}
