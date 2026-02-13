package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 复习卡片请求DTO
 */
@Data
public class ReviewCardRequest {

    @NotNull(message = "卡片ID不能为空")
    private Long cardId;

    @NotNull(message = "复习结果不能为空")
    @Min(value = 1, message = "复习结果值不正确")
    @Max(value = 4, message = "复习结果值不正确")
    private Integer result;

    private Integer timeSpent;

    /**
     * 当前学习队列（卡片ID列表）
     * LEARNING/RELEARNING 阶段，前端维护队列顺序，提交时一并传递
     */
    @Size(max = 100, message = "队列长度不能超过100")
    private List<Long> queue;

}