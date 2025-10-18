package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.old.UserDTOV4;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoadmapDTO {

    private Long id;

    private String content;

    private Long professionId;

    private ProfessionDTO profession;

    private String description;

    private Byte state;  // 状态：0-待审核，1-已批准，2-已拒绝

    private Integer vote;

    private Integer comment;

    private Boolean upvoted;

    private Boolean pinned;

    private Boolean learning;

    private Long creatorId;

    private UserDTO creator;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
