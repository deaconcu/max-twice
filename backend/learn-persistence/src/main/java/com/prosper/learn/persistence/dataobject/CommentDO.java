package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDO {

    private Long id;

    private String content;

    private Integer objectType;

    private Long objectId;

    private Integer replyCount;

    private Long replyToCommentId;

    private Long creatorId;

    private Long toUserId;

    private Integer upvoteCount;

    private Byte state;

    private String reason;  // 拒绝/封禁原因

    private Double score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
