package com.prosper.learn.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UpdateUserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50字符")
    private String name;
    
    @Size(max = 200, message = "个人简介长度不能超过200字符")
    private String biography;
}