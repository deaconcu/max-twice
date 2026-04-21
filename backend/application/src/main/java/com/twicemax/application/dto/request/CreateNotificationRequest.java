package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;


@Data
public class CreateNotificationRequest {

    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于0")
    private Long userId;

    @NotNull(message = "节点ID不能为空")
    @Positive(message = "节点ID必须大于0")
    private Long nodeId;
}