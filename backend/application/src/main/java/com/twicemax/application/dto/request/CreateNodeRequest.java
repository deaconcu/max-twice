package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 创建节点请求
 */
@Data
public class CreateNodeRequest {

    @NotBlank(message = "节点名称不能为空")
    private String name;

    @NotBlank(message = "节点描述不能为空")
    private String description;

    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须大于0")
    private Long courseId;
}
