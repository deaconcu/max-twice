package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "验证会话不能为空")
    @Size(max = 128, message = "验证会话格式错误")
    private String pendingSessionToken;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 10, message = "验证码长度不能超过10字符")
    private String code;
}
