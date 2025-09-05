package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRoadmapRequest {
    
    @NotNull(message = "专业ID不能为空")
    private Long professionId;
    
    @NotBlank(message = "路线图内容不能为空")
    @Size(max = 5000, message = "路线图内容长度不能超过5000字符")
    private String content;
    
    @NotBlank(message = "路线图描述不能为空")
    @Size(max = 500, message = "路线图描述长度不能超过500字符")
    private String description;
}