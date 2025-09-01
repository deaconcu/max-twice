package com.prosper.learn.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoadmapDTO {

    private Long id;

    private String content;

    private Long professionId;

    private String description;

    private Integer vote;

    private Integer comment;

    private Boolean upvoted;

    private Boolean pinned;

    private Boolean learning;

    private UserDTOV4 creator;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
