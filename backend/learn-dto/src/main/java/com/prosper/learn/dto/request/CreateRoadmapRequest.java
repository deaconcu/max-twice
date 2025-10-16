package com.prosper.learn.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoadmapRequest {

    @NotNull(message = "专业ID不能为空")
    private Long professionId;

    @NotBlank(message = "路线图内容不能为空")
    @ConfigurableSize(configKey = "roadmap-content")
    private String content;

    @NotBlank(message = "路线图描述不能为空")
    @ConfigurableSize(configKey = "roadmap-description")
    private String description;
}