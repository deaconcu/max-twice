package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 创建卡片组请求DTO
 */
@Data
public class CreateDeckRequest {

    /**
     * 源帖子ID，可选字段
     * 如果为 null 或 0，表示该卡片组不关联来源文章
     */
    private Long sourcePostId;

    /**
     * 节点ID，当 sourcePostId 为 null 或 0 时必填
     */
    private Long nodeId;

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