package com.twicemax.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.request.LoginRequest;
import com.twicemax.application.dto.request.LoginSendCodeRequest;
import com.twicemax.application.dto.request.ResendVerificationCodeRequest;
import com.twicemax.application.dto.request.VerifyEmailRequest;
import com.twicemax.application.dto.response.user.AuthLoginResponseDTO;
import com.twicemax.application.dto.response.user.PendingSessionDTO;
import com.twicemax.application.dto.response.user.UserProfileDTO;
import com.twicemax.application.service.AuthService;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.util.AcceptLanguageUtils;
import com.twicemax.web.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 认证接口
 * <p>
 * Controller 只负责：取 IP、调用 AuthService、StpUtil.login。
 * 所有业务编排（Turnstile、失败计数、OTP、DB）都在 AuthService 内完成。
 * <p>
 * 当前对外端点：
 * <ul>
 *     <li>邮箱验证码登录（主入口，不存在用户自动建号）</li>
 *     <li>邮箱+密码登录（方案 A 辅助入口）</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/auth")
@Slf4j
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    // ========== 邮箱验证码登录 ==========

    @PostMapping("/login/send-code")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<PendingSessionDTO> sendLoginCode(
            @RequestBody @Valid LoginSendCodeRequest request,
            HttpServletRequest httpRequest) {
        String remoteIp = IpUtils.getIpAddress(httpRequest);
        return ApiResponse.success(authService.sendLoginCode(
                request.getEmail(), request.getTurnstileToken(), remoteIp));
    }

    @PostMapping("/login/verify-code")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<UserProfileDTO> verifyLoginCode(
            @RequestBody @Valid VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        // 新用户自动建号时用 Accept-Language 决定初始 locale；老用户走已有值，参数被忽略
        String locale = AcceptLanguageUtils.detectLocale(httpRequest);
        UserProfileDTO userDTO = authService.verifyLoginCode(
                request.getPendingSessionToken(), request.getCode(), locale);
        StpUtil.login(userDTO.getId());
        return ApiResponse.success(userDTO);
    }

    @PostMapping("/login/resend-code")
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<PendingSessionDTO> resendLoginCode(
            @RequestBody @Valid ResendVerificationCodeRequest request) {
        return ApiResponse.success(authService.resendLoginCode(request.getPendingSessionToken()));
    }

    // ========== 密码登录（方案 A 辅助入口） ==========

    @PostMapping("/login/password")
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ApiResponse<AuthLoginResponseDTO> loginWithPassword(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        String remoteIp = IpUtils.getIpAddress(httpRequest);
        AuthLoginResponseDTO response = authService.loginWithPassword(
                request.getEmail(), request.getPassword(),
                request.getTurnstileToken(), remoteIp);
        if (response.getUser() != null) {
            StpUtil.login(response.getUser().getId());
        }
        return ApiResponse.success(response);
    }
}
