package com.prosper.learn.application.dto.request;

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

    /**
     * 帖子状态（可选）
     * 0=DRAFT, 1=SUBMITTED
     * 如果不传，默认为 DRAFT（草稿）
     */
    private Integer state;
}