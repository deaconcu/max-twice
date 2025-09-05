package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class SendMessageRequest {
    
    @NotNull(message = "消息类型不能为空")
    private Integer type;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容长度不能超过500字符")
    private String content;
}