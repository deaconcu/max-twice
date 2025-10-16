package com.prosper.learn.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建卡片请求DTO
 */
@Data
public class CreateCardRequest {

    @NotNull(message = "卡片组ID不能为空")
    private Long deckId;

    @NotBlank(message = "卡片正面不能为空")
    @ConfigurableSize(configKey = "card-front")
    private String front;

    @NotBlank(message = "卡片背面不能为空")
    @ConfigurableSize(configKey = "card-back")
    private String back;

}