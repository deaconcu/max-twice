package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重新发送验证码请求
 */
@Data
public class ResendVerificationCodeRequest {

    @NotBlank(message = "验证会话不能为空")
    @Size(max = 128, message = "验证会话格式错误")
    private String pendingSessionToken;
}
