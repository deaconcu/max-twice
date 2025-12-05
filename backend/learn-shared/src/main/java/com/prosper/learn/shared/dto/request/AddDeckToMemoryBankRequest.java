package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 添加卡片组到用户记忆库请求DTO
 */
@Data
public class AddDeckToMemoryBankRequest {

    @NotNull(message = "卡片组ID不能为空")
    @Positive(message = "卡片组ID必须为正数")
    private Long deckId;

    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;

}