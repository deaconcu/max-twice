package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 已登录用户为空密码账号设置密码的确认请求。
 */
@Data
public class SetPasswordConfirmRequest {

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    @ConfigurableSize(configKey = "password", message = "{user.password.length.invalid}")
    private String newPassword;
}
