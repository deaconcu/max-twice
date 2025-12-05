package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新卡片请求DTO
 */
@Data
public class UpdateCardRequest {

    @NotNull(message = "卡片ID不能为空")
    private Long id;

    @NotBlank(message = "卡片正面不能为空")
    @Size(max = 2000, message = "卡片正面长度不能超过2000字符")
    private String front;

    @NotBlank(message = "卡片背面不能为空")
    @Size(max = 2000, message = "卡片背面长度不能超过2000字符")
    private String back;

}