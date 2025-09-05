package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新帖子请求DTO
 * 
 * @author Claude Code
 */
@Data
public class UpdatePostRequest {

    /**
     * 帖子内容
     */
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 10000, message = "帖子内容长度不能超过10000字符")
    private String content;
}