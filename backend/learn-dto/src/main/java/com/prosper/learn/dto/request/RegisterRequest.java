package com.prosper.learn.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ConfigurableSize(configKey = "email")
    private String email;

    @NotBlank(message = "密码不能为空")
    @ConfigurableSize(configKey = "password")
    private String password;
}