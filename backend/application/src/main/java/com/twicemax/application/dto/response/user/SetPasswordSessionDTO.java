package com.twicemax.application.dto.response.user;

import lombok.Builder;
import lombok.Data;

/**
 * 已登录用户设置密码 OTP 发送后的会话信息。
 * <p>
 * 用户已登录，邮箱和身份在后端可取，因此不需要 token / email / expiresIn，
 * 只需要告诉前端多久后允许重发。
 */
@Data
@Builder
public class SetPasswordSessionDTO {
    /**
     * 距离下一次允许重发的秒数（前端用于倒计时禁用按钮）。
     */
    private Long resendAvailableIn;
}
