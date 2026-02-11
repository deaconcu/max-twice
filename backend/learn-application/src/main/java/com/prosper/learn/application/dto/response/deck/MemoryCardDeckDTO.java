package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 记忆卡片组响应DTO
 */
@Data
public class MemoryCardDeckDTO {

    private Long id;

    private Long postId;

    private Long nodeId;  // 添加nodeId字段，方便前端直接使用

    private UserBriefDTO creator;

    private String title;

    private String description;

    private Integer state;

    private String updatedAt;

    private String createdAt;

    private Integer likeCount;

    private Integer cardCount;

    private Boolean hasLiked;  // 当前用户是否已点赞

}