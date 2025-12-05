package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ChatRequest {
    
    @NotBlank(message = "提示内容不能为空")
    @Size(max = 1000, message = "提示内容长度不能超过1000字符")
    private String prompt;
    
    @NotBlank(message = "模型不能为空")
    @Size(max = 50, message = "模型名称长度不能超过50字符")
    private String model;
}