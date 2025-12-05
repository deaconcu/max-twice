package com.prosper.learn.shared.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建帖子请求DTO
 *
 * @author Claude Code
 */
@Data
public class CreatePostRequest {

    /**
     * 帖子内容
     */
    @NotBlank(message = "帖子内容不能为空")
    @ConfigurableSize(configKey = "post-content")
    private String content;

    /**
     * 节点ID
     */
    @NotNull(message = "节点ID不能为空")
    private Long nodeId;

    /**
     * 帖子类型
     */
    @NotNull(message = "帖子类型不能为空")
    private Integer type;
}