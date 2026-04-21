package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecordViewRequest {
    
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @Size(max = 45, message = "IP地址长度不能超过45字符")
    private String ipAddress;
}