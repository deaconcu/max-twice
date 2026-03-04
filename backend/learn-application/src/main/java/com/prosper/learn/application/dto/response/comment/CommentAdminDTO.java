package com.prosper.learn.application.dto.response.comment;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 评论管理员 DTO
 */
@Data
public class CommentAdminDTO {

    private Long id;

    private String content;

    private Integer objectType;

    private Long objectId;

    private Integer replyCount;

    private Long replyToCommentId;

    private Long creatorId;

    private UserBriefDTO creator;

    private Long toUserId;

    private UserBriefDTO toUser;

    private Integer likeCount;

    private Integer state;

    private Double score;

    private String createdAt;

    private String reason;

    private Integer rejectCount;
}
