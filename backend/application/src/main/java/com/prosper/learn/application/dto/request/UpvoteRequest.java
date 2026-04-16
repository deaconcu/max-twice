package com.prosper.learn.application.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class UpvoteRequest {
    
    @NotNull(message = "对象ID不能为空")
    private Long objectId;
    
    @NotNull(message = "对象类型不能为空")
    private Integer objectType;
    
    @NotNull(message = "投票类型不能为空")
    @Min(value = 1, message = "投票类型不正确")
    @Max(value = 2, message = "投票类型不正确")
    private Integer type;
}