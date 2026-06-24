package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoadmapRequest {

    @NotNull(message = "专业ID不能为空")
    private Long roleId;

    @NotBlank(message = "路线图内容不能为空")
    @ConfigurableSize(configKey = "roadmap-content")
    private String content;

    @NotBlank(message = "路线图描述不能为空")
    @ConfigurableSize(configKey = "roadmap-description")
    private String description;

    /**
     * 是否在创建后立即提交审核：
     * - true：createDraft 后立刻 submit（产生一条 SUBMITTED revision）
     * - false 或不传：仅保存为草稿（state=NEVER_PUBLISHED，无 revision）
     */
    private Boolean submit;
}
