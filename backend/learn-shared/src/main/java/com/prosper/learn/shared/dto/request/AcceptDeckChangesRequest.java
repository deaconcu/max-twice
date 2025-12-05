package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 接受卡片组更新请求DTO
 */
@Data
public class AcceptDeckChangesRequest {

    @NotNull(message = "卡片组ID不能为空")
    private Long deckId;

    @NotEmpty(message = "卡片ID列表不能为空")
    private List<Long> cardIds;

}