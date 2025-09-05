package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OperateRequest {
    
    @NotBlank(message = "操作类型不能为空")
    @Size(max = 20, message = "操作类型长度不能超过20字符")
    private String action;
    
    @Size(max = 500, message = "拒绝原因长度不能超过500字符")
    private String rejectedReason;
}