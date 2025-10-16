package com.prosper.learn.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 复习卡片请求DTO
 */
@Data
public class ReviewCardRequest {

    @NotNull(message = "卡片ID不能为空")
    private Long cardId;

    @NotNull(message = "复习结果不能为空")
    @Min(value = 0, message = "复习结果值不正确")
    @Max(value = 3, message = "复习结果值不正确")
    private Integer result;

    private Integer timeSpent;

}