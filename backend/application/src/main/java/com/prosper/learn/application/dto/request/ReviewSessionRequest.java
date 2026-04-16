package com.prosper.learn.application.dto.request;

import com.prosper.learn.application.dto.response.ReviewCardResultDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;

/**
 * 复习会话请求DTO
 */
@Data
public class ReviewSessionRequest {

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @NotNull(message = "总卡片数不能为空")
    @PositiveOrZero(message = "总卡片数必须大于等于0")
    private Integer totalCards;

    @NotNull(message = "已复习卡片数不能为空")
    @PositiveOrZero(message = "已复习卡片数必须大于等于0")
    private Integer reviewedCards;

    @NotNull(message = "正确答案数不能为空")
    @PositiveOrZero(message = "正确答案数必须大于等于0")
    private Integer correctAnswers;

    @NotEmpty(message = "复习结果列表不能为空")
    @Valid
    private List<ReviewCardResultDTO> results;

}