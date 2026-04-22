package com.twicemax.application.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱验证待处理会话响应
 * <p>
 * 剩余秒数为响应时刻快照，前端自行倒计时，不轮询后端。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingSessionDTO {

    /** 会话 token */
    private String pendingSessionToken;

    /** 所属邮箱（仅 UI 展示） */
    private String email;

    /** session 剩余有效秒数 */
    private long expiresIn;

    /** 距下次可重发验证码的剩余秒数，0 表示立即可重发 */
    private long resendAvailableIn;
}
