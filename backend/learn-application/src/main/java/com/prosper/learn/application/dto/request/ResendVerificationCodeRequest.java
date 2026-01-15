package com.prosper.learn.application.dto.request;

import com.prosper.learn.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重新发送验证码请求
 *
 * @author Claude
 * @since 2026-01-15
 */
@Data
public class ResendVerificationCodeRequest {

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @ConfigurableSize(configKey = "email")
    private String email;
}
