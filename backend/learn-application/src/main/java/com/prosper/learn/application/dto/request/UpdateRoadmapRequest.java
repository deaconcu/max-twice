package com.prosper.learn.application.dto.request;

import com.prosper.learn.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoadmapRequest {

    @NotBlank(message = "路线图内容不能为空")
    @ConfigurableSize(configKey = "roadmap-content")
    private String content;

    @ConfigurableSize(configKey = "roadmap-description")
    private String description;
}
