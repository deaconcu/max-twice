package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新卡片组请求DTO
 */
@Data
public class UpdateDeckRequest {

    @NotNull(message = "卡片组ID不能为空")
    private Long id;

    @Size(max = 255, message = "标题长度不能超过255字符")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000字符")
    private String description;

}