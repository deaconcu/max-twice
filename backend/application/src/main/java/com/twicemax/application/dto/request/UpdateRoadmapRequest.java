package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新路线图请求体（仅 saveDraft 用）。
 * <p>
 * 该端点只更新草稿，不改变 roadmap.state，也不产生 revision。
 * 提交审核请走 POST /roadmaps/{id}/submit。
 */
@Data
public class UpdateRoadmapRequest {

    @NotBlank(message = "路线图内容不能为空")
    @ConfigurableSize(configKey = "roadmap-content")
    private String content;

    @ConfigurableSize(configKey = "roadmap-description")
    private String description;
}

