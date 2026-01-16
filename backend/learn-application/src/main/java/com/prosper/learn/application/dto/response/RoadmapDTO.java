package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
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

    private Integer likeCount;

    private Integer commentCount;

    private Boolean liked;

    private Boolean pinned;

    private Boolean learning;

    private Long creatorId;

    private UserBriefDTO creator;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
