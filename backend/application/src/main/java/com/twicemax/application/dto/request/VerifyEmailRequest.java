package com.twicemax.application.dto.request;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class VerifyEmailRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    private String email;
    
    @NotBlank(message = "验证码不能为空")
    @Size(max = 10, message = "验证码长度不能超过10字符")
    private String code;
}