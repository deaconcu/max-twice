package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.SetPasswordConfirmRequest;
import com.twicemax.application.dto.request.UpdateLocaleRequest;
import com.twicemax.application.dto.response.user.SetPasswordSessionDTO;
import com.twicemax.application.service.AuthService;
import com.twicemax.application.service.UserService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 已登录用户账户设置接口。
 * <p>
 * Controller 按资源组织（当前登录账户的设置面板），Service 仍然调用 AuthService，
 * 因为密码、邮箱等是认证域的凭据。
 * <p>
 * 当前提供：
 * <ul>
 *     <li>空密码账号首次设置密码（OTP 校验）</li>
 * </ul>
 * 未来可放：修改密码、修改邮箱、删除账号、登录设备管理等。
 */
@RestController
@RequestMapping("/account")
@Slf4j
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 发送设置密码 OTP 到当前用户邮箱。仅空密码账号可调用。
     */
    @PostMapping("/password/send-code")
    @SaCheckLogin
    @RateLimit(capacity = 3, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public SetPasswordSessionDTO sendSetPasswordCode(@CurrentUser UserDO currentUser) {
        return authService.sendSetPasswordCode(currentUser);
    }

    /**
     * 校验 OTP 并为空密码账号设置新密码。
     */
    @PostMapping("/password/confirm")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> confirmSetPassword(
            @RequestBody @Valid SetPasswordConfirmRequest request,
            @CurrentUser UserDO currentUser) {
        authService.confirmSetPassword(currentUser, request.getCode(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * 更新当前用户的偏好语言，LanguageSwitcher 切换语言时调用。
     * 成功后前端负责刷新页面（数据分库，需要重新加载内容）。
     */
    @PutMapping("/locale")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateLocale(
            @RequestBody @Valid UpdateLocaleRequest request,
            @CurrentUser UserDO currentUser) {
        userService.updateCurrentUserLocale(currentUser.getId(), request.getLocale());
        return ResponseEntity.noContent().build();
    }
}
