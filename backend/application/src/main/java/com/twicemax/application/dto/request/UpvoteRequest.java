package com.twicemax.application.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class UpvoteRequest {

    @NotNull(message = "对象ID不能为空")
    private Long objectId;

    @NotNull(message = "对象类型不能为空")
    private Integer objectType;

    @NotNull(message = "投票类型不能为空")
    private String type;
}