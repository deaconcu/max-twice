package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建卡片请求DTO
 */
@Data
public class CreateCardRequest {

    @NotNull(message = "卡片组ID不能为空")
    private Long deckId;

    @NotBlank(message = "卡片正面不能为空")
    @Size(max = 2000, message = "卡片正面长度不能超过2000字符")
    private String front;

    @NotBlank(message = "卡片背面不能为空")
    @Size(max = 2000, message = "卡片背面长度不能超过2000字符")
    private String back;

}