package com.prosper.learn.application.dto.response.old;

import com.prosper.learn.application.dto.response.ProfessionDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoadmapDTOV2 {

    private Long id;

    private String content;

    private ProfessionDTO profession;

    private String description;

    private Integer vote;

    private Integer comment;

    private Boolean upvoted;

    private UserDTOV4 creator;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
