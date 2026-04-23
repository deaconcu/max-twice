package com.twicemax.application.dto.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录接口响应。
 * <ul>
 *   <li>邮箱已验证：{@code user} 非空，{@code pending} 为 null，Controller 侧已完成 StpUtil.login</li>
 *   <li>邮箱未验证：{@code pending} 非空，{@code user} 为 null，前端跳转验证码页</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthLoginResponseDTO {

    /** 登录成功用户信息（已验证邮箱时返回） */
    private UserProfileDTO user;

    /** 待验证会话（邮箱未验证时返回） */
    private PendingSessionDTO pending;
}
