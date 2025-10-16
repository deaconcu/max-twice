package com.prosper.learn.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

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
    @Positive(message = "对象ID必须大于0")
    private Long objectId;

    /**
     * 对象类型
     */
    @NotNull(message = "对象类型不能为空")
    @Range(min = 0, max = 4, message = "对象ID必须在0-4之间")
    private Integer objectType;

    /**
     * 回复的评论ID
     */
    @Positive(message = "回复评论的ID必须大于0")
    private Long replyTo;

    /**
     * 回复子评论时，回复的用户ID，因为评论只有两级，回复子评论是@用户
     */
    @Positive(message = "用户ID必须大于0")
    private Long toUser;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @ConfigurableSize(configKey = "comment-content")
    private String content;
}