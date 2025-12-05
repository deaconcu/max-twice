package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CreateNotificationRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "节点ID不能为空")
    private Long nodeId;
}