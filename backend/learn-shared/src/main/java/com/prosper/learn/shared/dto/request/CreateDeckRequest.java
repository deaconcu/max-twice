package com.prosper.learn.shared.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建卡片组请求DTO
 */
@Data
public class CreateDeckRequest {

    @NotNull(message = "源帖子ID不能为空")
    private Long sourcePostId;

    @ConfigurableSize(configKey = "deck-description")
    private String description;

    @NotEmpty(message = "卡片列表不能为空")
    @Valid
    private List<CardInfo> cards;

    @Data
    public static class CardInfo {
        @NotBlank(message = "卡片正面不能为空")
        @ConfigurableSize(configKey = "card-front")
        private String front;

        @NotBlank(message = "卡片背面不能为空")
        @ConfigurableSize(configKey = "card-back")
        private String back;
    }

}