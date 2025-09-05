package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreateMessageRequest {
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100字符")
    private String title;
    
    @NotBlank(message = "摘要不能为空")
    @Size(max = 200, message = "摘要长度不能超过200字符")
    private String summary;
    
    @NotBlank(message = "详细说明不能为空")
    @Size(max = 1000, message = "详细说明长度不能超过1000字符")
    private String explanation;
    
    @NotNull(message = "父消息ID不能为空")
    private Long parentId;
}