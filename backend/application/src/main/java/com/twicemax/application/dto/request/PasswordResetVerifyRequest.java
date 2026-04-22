package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码第二步：校验验证码
 */
@Data
public class PasswordResetVerifyRequest {

    @NotBlank(message = "重置会话不能为空")
    @Size(max = 128, message = "重置会话格式错误")
    private String resetSessionToken;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码格式错误")
    private String code;
}
