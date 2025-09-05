package com.prosper.learn.dto.request;

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
    @Min(value = -1, message = "投票类型最小值为-1")
    @Max(value = 1, message = "投票类型最大值为1")
    private Integer type;
}