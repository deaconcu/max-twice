package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoadmapRequest {

    @NotBlank(message = "路线图内容不能为空")
    @ConfigurableSize(configKey = "roadmap-content")
    private String content;

    @ConfigurableSize(configKey = "roadmap-description")
    private String description;

    /**
     * 状态：0-草稿，1-提交审核
     * 必须传递，只能是这两个值
     */
    @NotNull(message = "状态不能为空")
    private Byte state;
}
