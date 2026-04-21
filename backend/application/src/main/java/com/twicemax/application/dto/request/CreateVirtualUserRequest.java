package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建虚拟用户请求（管理员专用）
 */
@Data
public class CreateVirtualUserRequest {

    @NotBlank(message = "用户名不能为空")
    @ConfigurableSize(configKey = "username")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ConfigurableSize(configKey = "email")
    private String email;

    @NotBlank(message = "密码不能为空")
    @ConfigurableSize(configKey = "password")
    private String password;
}
