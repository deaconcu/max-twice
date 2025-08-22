package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserStatsDO {

    private Long id;

    private Integer userId;

    private LocalDate statDate;

    private Long totalViews;

    private Long totalTwice;

    private Long totalHelpful;

    private Long totalComments;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}