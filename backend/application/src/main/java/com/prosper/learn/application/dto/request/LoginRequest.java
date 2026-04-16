package com.prosper.learn.application.dto.request;

import com.prosper.learn.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ConfigurableSize(configKey = "email")
    private String email;

    @NotBlank(message = "密码不能为空")
    @ConfigurableSize(configKey = "password")
    private String password;

    /**
     * Turnstile 验证 token（可选，登录失败多次后需要）
     */
    private String turnstileToken;
}