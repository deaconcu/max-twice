package com.twicemax.application.service;

import com.twicemax.application.converter.UserConverter;
import com.twicemax.application.dto.response.user.AuthLoginResponseDTO;
import com.twicemax.application.dto.response.user.PasswordResetSessionDTO;
import com.twicemax.application.dto.response.user.PendingSessionDTO;
import com.twicemax.application.dto.response.user.SetPasswordSessionDTO;
import com.twicemax.application.dto.response.user.UserProfileDTO;
import com.twicemax.infrastructure.captcha.TurnstileService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.auth.AuthRiskDomainService;
import com.twicemax.user.auth.OtpCodeService;
import com.twicemax.user.auth.OtpScene;
import com.twicemax.user.auth.OtpSessionService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

import static com.twicemax.shared.domain.Enums.UserRole;
import static com.twicemax.shared.domain.Enums.UserState;

/**
 * 认证业务服务
 * <p>
 * 负责认证域的所有编排：
 * <ul>
 *     <li>邮箱验证码登录（含不存在自动建号）</li>
 *     <li>密码登录</li>
 *     <li>注册（邮箱+密码）</li>
 *     <li>邮箱验证（pending session + code）</li>
 *     <li>忘记密码三步流程</li>
 * </ul>
 * Turnstile 人机校验、失败频控都在本层吞掉；Controller 只负责取 IP 和 StpUtil.login。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserDomainService userDomainService;
    private final UserDataService userDataService;
    private final OtpSessionService otpSessionService;
    private final OtpCodeService otpCodeService;
    private final EmailService emailService;
    private final AuthRiskDomainService authRiskDomainService;
    private final TurnstileService turnstileService;
    private final UserConverter userConverter;
    private final SystemProperties systemProperties;

    /**
     * 内测开关：开启后非管理员无法登录（发送验证码 / 密码登录都会在入口处被拦），
     * 给人"邀请制早期产品"的观感。默认 false，生产 yml 显式开启。
     */
    @Value("${app.invite-only-mode:false}")
    private boolean inviteOnlyMode;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * 内测期：拒绝非管理员（包含邮箱尚未注册的新人）。
     * 注意：对不存在的邮箱也要拦，否则泄露"该邮箱是否已注册"。
     */
    private void guardInviteOnly(String email) {
        if (!inviteOnlyMode) return;
        UserDO user = userDataService.getByEmail(email);
        if (user == null || !user.hasRole(UserRole.ADMIN)) {
            throw StatusCode.INVITE_ONLY.exception();
        }
    }

    // ========== 邮箱验证码登录 ==========

    /**
     * 发送登录验证码。
     */
    public PendingSessionDTO sendLoginCode(String email, String turnstileToken, String remoteIp) {
        validateEmailFormat(email);
        if (!turnstileService.verify(turnstileToken, remoteIp)) {
            throw StatusCode.CAPTCHA_INVALID.exception();
        }

        guardInviteOnly(email);

        OtpSessionService.Created session = otpSessionService.create(OtpScene.LOGIN, email);
        String code = otpCodeService.issue(OtpScene.LOGIN, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        return PendingSessionDTO.builder()
                .pendingSessionToken(session.token())
                .email(email)
                .expiresIn(session.expiresInSeconds())
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.LOGIN, email))
                .build();
    }

    /**
     * 重发登录验证码。
     */
    public PendingSessionDTO resendLoginCode(String pendingSessionToken) {
        String email = otpSessionService.requireEmail(OtpScene.LOGIN, pendingSessionToken);
        String code = otpCodeService.resend(OtpScene.LOGIN, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        return PendingSessionDTO.builder()
                .pendingSessionToken(pendingSessionToken)
                .email(email)
                .expiresIn(otpSessionService.ttlSeconds(OtpScene.LOGIN, pendingSessionToken))
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.LOGIN, email))
                .build();
    }

    /**
     * 校验登录验证码；用户不存在时自动建号（password=null，emailValidated=true）。
     */
    @Transactional
    public UserProfileDTO verifyLoginCode(String pendingSessionToken, String code) {
        validateVerificationCode(code);

        String email = otpSessionService.requireEmail(OtpScene.LOGIN, pendingSessionToken);

        otpCodeService.verify(OtpScene.LOGIN, email, code, false);

        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            user = userDomainService.createUser(email, null);
            log.info("邮箱验证码登录自动建号: userId={}, email={}", user.getId(), email);
        }

        if (!Boolean.TRUE.equals(user.getEmailValidated())) {
            userDataService.updateEmailValidated(user.getId(), true);
            user.setEmailValidated(true);
        }

        if (user.getState() != null && user.getState() == UserState.BANNED.value()) {
            throw StatusCode.USER_BANNED.exception();
        }

        otpSessionService.invalidate(OtpScene.LOGIN, pendingSessionToken);

        return userConverter.toProfileDTO(user);
    }

    // ========== 密码登录 ==========

    /**
     * 密码登录。
     * <p>
     * 自吞：Turnstile（失败次数超阈值时强校验）+ 失败计数 + 校验密码。
     * 邮箱已验证 → 返回 {@code user}；未验证 → 返回 {@code pending}（复用活跃验证码或签发）。
     */
    public AuthLoginResponseDTO loginWithPassword(
            String email, String password, String turnstileToken, String remoteIp) {

        // 失败次数超阈值时强制人机验证
        if (authRiskDomainService.isCaptchaRequired(remoteIp)) {
            if (!StringUtils.hasText(turnstileToken)) {
                throw StatusCode.CAPTCHA_REQUIRED.exception();
            }
            if (!turnstileService.verify(turnstileToken, remoteIp)) {
                throw StatusCode.CAPTCHA_INVALID.exception();
            }
            authRiskDomainService.clearFailures(remoteIp);
        }

        try {
            validateEmailFormat(email);
            validatePassword(password);

            guardInviteOnly(email);

            UserDO userDO = userDomainService.validateLoginIgnoringEmailValidated(email, password);

            if (Boolean.TRUE.equals(userDO.getEmailValidated())) {
                return AuthLoginResponseDTO.builder().user(userConverter.toProfileDTO(userDO)).build();
            }

            // 未验证邮箱：复用或签发 REGISTER 场景 OTP
            PendingSessionDTO pending = otpCodeService.exists(OtpScene.REGISTER, email)
                    ? createPendingSessionWithoutSendingEmail(email)
                    : issuePendingSessionAndSendEmail(email);
            return AuthLoginResponseDTO.builder().pending(pending).build();
        } catch (RuntimeException e) {
            authRiskDomainService.recordFailure(remoteIp);
            throw e;
        }
    }

    // ========== 注册 ==========

    /**
     * 用户注册（邮箱+密码）。
     */
    @Transactional
    public PendingSessionDTO register(String email, String password) {
        validateEmailFormat(email);
        validatePassword(password);

        guardInviteOnly(email);

        userDomainService.createUser(email, password);

        return issuePendingSessionAndSendEmail(email);
    }

    // ========== 邮箱验证（REGISTER 场景） ==========

    /**
     * 邮箱验证 - 凭 pending session token + 验证码。
     */
    @Transactional
    public UserProfileDTO verifyEmailWithCode(String pendingSessionToken, String code) {
        validateVerificationCode(code);

        String email = otpSessionService.requireEmail(OtpScene.REGISTER, pendingSessionToken);

        otpCodeService.verify(OtpScene.REGISTER, email, code, false);

        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception("用户不存在");
        }

        if (!Boolean.TRUE.equals(user.getEmailValidated())) {
            userDataService.updateEmailValidated(user.getId(), true);
            log.info("用户邮箱验证成功: userId={}", user.getId());
        }

        otpSessionService.invalidate(OtpScene.REGISTER, pendingSessionToken);

        return userConverter.toProfileDTO(user);
    }

    /**
     * 重新发送验证码 - 凭 pending session token（REGISTER 场景）。
     */
    public PendingSessionDTO resendVerificationCode(String pendingSessionToken) {
        String email = otpSessionService.requireEmail(OtpScene.REGISTER, pendingSessionToken);

        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getEmailValidated())) {
            throw StatusCode.INVALID_PARAMETER.exception("邮箱已验证，无需重新发送");
        }

        String code = otpCodeService.resend(OtpScene.REGISTER, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        long expiresIn = otpSessionService.ttlSeconds(OtpScene.REGISTER, pendingSessionToken);
        return PendingSessionDTO.builder()
                .pendingSessionToken(pendingSessionToken)
                .email(email)
                .expiresIn(expiresIn)
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.REGISTER, email))
                .build();
    }

    // ========== 忘记密码 / 设置密码（PASSWORD_RESET 场景） ==========

    /**
     * 请求发送重置验证码。邮箱不存在返回伪响应防枚举。
     */
    public PasswordResetSessionDTO requestPasswordReset(String email, String turnstileToken, String remoteIp) {
        validateEmailFormat(email);
        if (!turnstileService.verify(turnstileToken, remoteIp)) {
            throw StatusCode.CAPTCHA_INVALID.exception();
        }

        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            log.info("忘记密码：邮箱不存在，返回伪响应防枚举: email={}", email);
            return PasswordResetSessionDTO.builder()
                    .resetSessionToken(generateFakeResetToken())
                    .email(email)
                    .expiresIn(30L * 60)
                    .resendAvailableIn(systemProperties.getUser().getVerificationCodeSendIntervalSeconds())
                    .build();
        }

        OtpSessionService.Created session = otpSessionService.create(OtpScene.PASSWORD_RESET, email);
        String code = otpCodeService.issue(OtpScene.PASSWORD_RESET, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        return PasswordResetSessionDTO.builder()
                .resetSessionToken(session.token())
                .email(email)
                .expiresIn(session.expiresInSeconds())
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.PASSWORD_RESET, email))
                .build();
    }

    /**
     * 重发密码重置验证码。
     */
    public PasswordResetSessionDTO resendPasswordResetCode(String resetSessionToken) {
        String email = otpSessionService.requireEmail(OtpScene.PASSWORD_RESET, resetSessionToken);
        String code = otpCodeService.resend(OtpScene.PASSWORD_RESET, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        return PasswordResetSessionDTO.builder()
                .resetSessionToken(resetSessionToken)
                .email(email)
                .expiresIn(otpSessionService.ttlSeconds(OtpScene.PASSWORD_RESET, resetSessionToken))
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.PASSWORD_RESET, email))
                .build();
    }

    /**
     * 校验重置验证码；成功后在 session 标记 verified，供 confirm 阶段消费。
     */
    public void verifyPasswordResetCode(String resetSessionToken, String code) {
        validateVerificationCode(code);
        String email = otpSessionService.requireEmail(OtpScene.PASSWORD_RESET, resetSessionToken);
        otpCodeService.verify(OtpScene.PASSWORD_RESET, email, code, true);
        otpSessionService.markVerified(OtpScene.PASSWORD_RESET, resetSessionToken);
        otpCodeService.invalidate(OtpScene.PASSWORD_RESET, email);
    }

    /**
     * 确认新密码并落库；成功后失效 session，返回 userId 供 Controller 踢 token。
     */
    @Transactional
    public Long confirmPasswordReset(String resetSessionToken, String newPassword) {
        validatePassword(newPassword);
        String email = otpSessionService.requireVerifiedEmail(
                OtpScene.PASSWORD_RESET, resetSessionToken,
                StatusCode.PASSWORD_RESET_NOT_VERIFIED);
        Long userId = userDomainService.updatePasswordByEmail(email, newPassword);
        otpSessionService.invalidate(OtpScene.PASSWORD_RESET, resetSessionToken);
        log.info("密码重置完成: userId={}, email={}", userId, email);
        return userId;
    }

    // ========== 已登录用户设置密码（SET_PASSWORD 场景） ==========

    /**
     * 向当前登录用户的邮箱发送设置密码的 OTP。
     * <p>
     * 仅对当前无密码账号开放；已设置密码的用户应走修改密码流程（Phase 2）。
     *
     * @return 含 resendAvailableIn 的会话信息，供前端倒计时禁用重发按钮
     */
    public SetPasswordSessionDTO sendSetPasswordCode(UserDO currentUser) {
        if (currentUser.getPassword() != null) {
            throw StatusCode.USER_PASSWORD_ALREADY_SET.exception();
        }
        String email = currentUser.getEmail();

        // 幂等：当前 OTP 仍在重发冷却期内，不发新邮件，直接返回剩余冷却秒数。
        // 用于支持"刷新进入页面也能恢复倒计时"，避免用户清前端缓存即可绕过冷却。
        if (otpCodeService.exists(OtpScene.SET_PASSWORD, email)) {
            long resendIn = otpCodeService.secondsUntilResendAvailable(OtpScene.SET_PASSWORD, email);
            if (resendIn > 0) {
                return SetPasswordSessionDTO.builder().resendAvailableIn(resendIn).build();
            }
        }

        String code = otpCodeService.issue(OtpScene.SET_PASSWORD, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);
        log.info("发送设置密码 OTP: userId={}", currentUser.getId());
        return SetPasswordSessionDTO.builder()
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.SET_PASSWORD, email))
                .build();
    }

    /**
     * 校验 OTP 并为当前登录的空密码账号设置密码。
     */
    @Transactional
    public void confirmSetPassword(UserDO currentUser, String code, String newPassword) {
        if (currentUser.getPassword() != null) {
            throw StatusCode.USER_PASSWORD_ALREADY_SET.exception();
        }
        validateVerificationCode(code);
        validatePassword(newPassword);

        String email = currentUser.getEmail();
        otpCodeService.verify(OtpScene.SET_PASSWORD, email, code, false);

        userDomainService.setPasswordForEmptyPasswordUser(currentUser.getId(), newPassword);
        otpCodeService.invalidate(OtpScene.SET_PASSWORD, email);
    }

    // ========== 私有辅助 ==========

    private PendingSessionDTO issuePendingSessionAndSendEmail(String email) {
        String code = otpCodeService.issue(OtpScene.REGISTER, email);
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);
        return createPendingSessionWithoutSendingEmail(email);
    }

    private PendingSessionDTO createPendingSessionWithoutSendingEmail(String email) {
        OtpSessionService.Created created = otpSessionService.create(OtpScene.REGISTER, email);
        return PendingSessionDTO.builder()
                .pendingSessionToken(created.token())
                .email(email)
                .expiresIn(created.expiresInSeconds())
                .resendAvailableIn(otpCodeService.secondsUntilResendAvailable(OtpScene.REGISTER, email))
                .build();
    }

    private static String generateFakeResetToken() {
        byte[] bytes = new byte[48];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateEmailFormat(String email) {
        if (!StringUtils.hasText(email)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw StatusCode.USER_INVALID_EMAIL_FORMAT.exception();
        }
    }

    private void validatePassword(String password) {
        var validation = systemProperties.getValidation();
        if (password == null
                || password.length() < validation.getPasswordMinLength()
                || password.length() > validation.getPasswordMaxLength()) {
            throw StatusCode.USER_INVALID_PASSWORD_LENGTH.exception();
        }
        int score = new me.gosimple.nbvcxz.Nbvcxz().estimate(password).getBasicScore();
        if (score < 2) {
            throw StatusCode.USER_PASSWORD_TOO_WEAK.exception();
        }
    }

    private void validateVerificationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }
}
