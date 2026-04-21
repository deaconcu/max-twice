package com.twicemax.application.dto.request;

import com.twicemax.shared.domain.Enums;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PostContentsRequest {

    @NotBlank(message = "路径不能为空")
    private String path;

    @NotNull(message = "节点ID不能为空")
    @Positive(message = "节点ID必须为正数")
    private Long nodeId;

    @NotNull(message = "帖子ID不能为空")
    @Positive(message = "帖子ID必须为正数")
    private Long postingId;

    @NotNull(message = "操作类型不能为空")
    @Min(value = 1, message = "操作类型不正确")
    @Max(value = 4, message = "操作类型不正确")
    private Integer action;

    /**
     * 获取操作类型枚举
     */
    public Enums.ContentAction getActionEnum() {
        return Enums.ContentAction.getByValue(action);
    }
}