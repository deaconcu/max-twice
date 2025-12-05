package com.prosper.learn.shared.dto.response.old;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoadmapDTOV1 {

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
