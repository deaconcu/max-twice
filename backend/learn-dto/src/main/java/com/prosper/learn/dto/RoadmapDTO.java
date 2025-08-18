package com.prosper.learn.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoadmapDTO {

    private Integer id;

    private String content;

    private Integer professionId;

    private String description;

    private int vote;

    private int comment;

    private boolean upvoted;

    private boolean pinned;

    private boolean learning;

    private UserDTOV4 creator;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
