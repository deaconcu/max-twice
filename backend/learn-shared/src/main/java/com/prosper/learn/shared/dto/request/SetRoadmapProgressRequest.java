package com.prosper.learn.shared.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class SetRoadmapProgressRequest {
    
    @NotNull(message = "专业ID不能为空")
    private Long professionId;
    
    @NotNull(message = "路线图ID不能为空")
    private Long roadmapId;
}