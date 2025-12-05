package com.prosper.learn.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreateSubcourseRequest {
    
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称长度不能超过100字符")
    private String name;
    
    @NotBlank(message = "课程描述不能为空")
    @Size(max = 500, message = "课程描述长度不能超过500字符")
    private String description;
}