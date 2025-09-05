package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论请求DTO
 * 
 * @author Claude Code
 */
@Data
public class CreateCommentRequest {

    /**
     * 对象ID
     */
    @NotNull(message = "对象ID不能为空")
    private Long objectId;

    /**
     * 对象类型
     */
    @NotNull(message = "对象类型不能为空")
    private Integer type;

    /**
     * 回复的评论ID
     */
    private Long replyTo;

    /**
     * 回复给的用户ID
     */
    private Long toUser;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过1000字符")
    private String content;
}