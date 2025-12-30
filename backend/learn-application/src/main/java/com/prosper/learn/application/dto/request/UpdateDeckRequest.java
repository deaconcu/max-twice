package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新卡片组请求DTO
 * 只允许更新描述，不允许更新标题
 */
@Data
public class UpdateDeckRequest {

    @Size(max = 1000, message = "描述长度不能超过1000字符")
    private String description;

}