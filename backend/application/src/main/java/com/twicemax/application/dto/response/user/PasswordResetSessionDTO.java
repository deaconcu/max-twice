package com.twicemax.application.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码重置会话响应。
 * <p>
 * 前端持有 {@code resetSessionToken} 进入下一步（输入验证码）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetSessionDTO {

    /** 重置会话 token */
    private String resetSessionToken;

    /** 所属邮箱（仅 UI 展示） */
    private String email;

    /** session 剩余有效秒数 */
    private long expiresIn;

    /** 距下次可重发验证码的剩余秒数，0 表示立即可重发 */
    private long resendAvailableIn;
}
