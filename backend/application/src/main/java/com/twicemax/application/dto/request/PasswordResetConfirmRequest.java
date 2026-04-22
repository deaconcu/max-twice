package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码第三步：确认设置新密码
 */
@Data
public class PasswordResetConfirmRequest {

    @NotBlank(message = "重置会话不能为空")
    @Size(max = 128, message = "重置会话格式错误")
    private String resetSessionToken;

    @NotBlank(message = "密码不能为空")
    @ConfigurableSize(configKey = "password")
    private String newPassword;
}
