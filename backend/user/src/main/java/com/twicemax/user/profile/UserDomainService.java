package com.twicemax.user.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.auth.VerificationDO;
import com.twicemax.user.auth.VerificationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.twicemax.shared.domain.Enums.UserRole;
import static com.twicemax.shared.domain.Enums.UserState;
import static com.twicemax.shared.domain.Enums.VerificationType;

/**
 * 用户领域服务
 *
 * 只依赖 user 域，处理用户相关的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_EMPTY_STRING = "";
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserDataService userDataService;
    private final VerificationDataService verificationDataService;
    private final SystemProperties systemProperties;

    // ========== 用户状态和角色管理 ==========

    /**
     * 更新用户状态（封禁/解封）
     *
     * @param userId 用户ID
     * @param ban true表示封禁，false表示解封
     */
    @Transactional
    public void updateUserState(Long userId, boolean ban) {
        UserDO userDO = userDataService.validateAndGet(userId);

        if (ban) {
            userDataService.updateState(userId, UserState.BANNED.value());
        } else {
            userDataService.updateState(userId, UserState.ACTIVE.value());
        }

        log.info("用户状态变更: userId={}，状态={}", userId, ban ? "已封禁" : "正常");
    }

    /**
     * 修改用户角色
     *
     * @param userId 用户ID
     * @param operatorId 操作者ID
     * @param operatorRole 操作者角色
     * @param roleCode 新角色代码
     */
    @Transactional
    public void setUserRole(Long userId, Long operatorId, UserRole operatorRole, String roleName) {
        UserDO targetUser = userDataService.validateAndGet(userId);
        UserRole newRole = UserRole.fromName(roleName);

        // 1. 防止用户修改自己的角色
        if (userId.equals(operatorId)) {
            throw StatusCode.PERMISSION_DENIED.exception("不能修改自己的角色");
        }

        // 2. 只有超级管理员可以设置超级管理员
        if (newRole == UserRole.SUPER && operatorRole != UserRole.SUPER) {
            throw StatusCode.PERMISSION_DENIED.exception("只有超级管理员可以设置超级管理员");
        }

        // 3. 执行角色修改
        userDataService.updateRole(userId, newRole.value());

        log.info("用户角色变更: userId={}，新角色={}，操作者={}", userId, newRole.getDescription(), operatorId);
    }

    /**
     * 更新用户基本信息
     */
    @Transactional
    public void updateUserInfo(Long userId, String name, String biography, String timezone) {
        UserDO userDO = userDataService.validateAndGet(userId);

        // 处理null值，数据库字段不允许null
        userDO.setName(name == null ? "" : name);
        userDO.setBiography(biography == null ? "" : biography);
        userDO.setTimezone(timezone);
        userDataService.update(userDO);

        log.info("用户信息更新: userId={}，name={}，timezone={}", userId, name, timezone);
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateUserAvatar(Long userId, String avatarUrl) {
        int updated = userDataService.updateAvatar(userId, avatarUrl);
        if (updated == 0) {
            throw StatusCode.USER_NOT_FOUND.exception("用户不存在");
        }
        log.info("用户头像更新: userId={}，avatar={}", userId, avatarUrl);
    }

    // ========== 用户注册和验证 ==========

    /**
     * 创建用户（注册）
     *
     * @param email 邮箱
     * @param password 密码（明文，将被BCrypt加密）；可传 null 表示无密码账号（邮箱验证码登录建号）
     * @param locale 用户偏好语言（"zh" / "en"）；调用方负责保证非空合法
     * @return 创建的用户对象
     */
    @Transactional
    public UserDO createUser(String email, String password, String locale) {
        // 检查用户是否已存在
        UserDO existingUser = userDataService.getByEmail(email);
        if (existingUser != null) {
            throw StatusCode.USER_ALREADY_EXISTS.exception();
        }

        // 创建用户
        UserDO user = new UserDO();
        user.setName("MT_" + generateRandomBase62(8));
        user.setPassword(password == null ? null : passwordEncoder.encode(password));  // null 表示无密码账号
        user.setEmail(email);
        user.setBiography("");
        user.setAvatar("");
        user.setState(UserState.ACTIVE.value());
        user.setRole(UserRole.USER.value());
        user.setEmailValidated(false); // 设置邮箱验证状态默认值
        user.setLocale(locale);
        userDataService.insert(user);

        log.info("用户创建成功: userId={}", user.getId());
        return user;
    }

    /**
     * 更新用户偏好语言。白名单校验在上层（UserService）。
     */
    @Transactional
    public void updateUserLocale(Long userId, String locale) {
        int updated = userDataService.updateLocale(userId, locale);
        if (updated == 0) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        log.info("用户语言更新: userId={}, locale={}", userId, locale);
    }

    /**
     * 按邮箱更新密码（忘记密码流程使用）
     *
     * @param email 邮箱
     * @param newPlainPassword 新密码（明文，将被 BCrypt 加密）
     * @return 被更新的用户 id（供调用方踢掉该用户所有 token 使用）
     */
    @Transactional
    public Long updatePasswordByEmail(String email, String newPlainPassword) {
        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        String encoded = passwordEncoder.encode(newPlainPassword);
        userDataService.updatePassword(user.getId(), encoded);
        log.info("用户密码更新成功: userId={}", user.getId());
        return user.getId();
    }

    /**
     * 为空密码账号设置密码（已登录用户首次设置密码）。
     * <p>
     * 仅允许当前密码为 null 的用户调用；已设置过密码的用户会抛 USER_PASSWORD_ALREADY_SET，
     * 需走修改密码流程。
     *
     * @param userId 当前用户 ID
     * @param newPlainPassword 新密码（明文，将被 BCrypt 加密）
     */
    @Transactional
    public void setPasswordForEmptyPasswordUser(Long userId, String newPlainPassword) {
        UserDO user = userDataService.validateAndGet(userId);
        if (user.getPassword() != null) {
            throw StatusCode.USER_PASSWORD_ALREADY_SET.exception();
        }
        String encoded = passwordEncoder.encode(newPlainPassword);
        userDataService.updatePassword(userId, encoded);
        log.info("空密码账号首次设置密码成功: userId={}", userId);
    }

    /**
     * 创建邮箱验证码
     *
     * @param email 邮箱
     * @param code 验证码
     * @deprecated OTP 流程已迁移到 Redis 的 {@code OtpCodeService}，此方法（基于数据库表
     * {@code VerificationDO}）已无调用方，保留用于参考。未来应删除本方法及关联 DAO。
     */
    @Deprecated
    @Transactional
    public void createVerificationCode(String email, String code) {
        // 1. 检查是否60秒内已发送过验证码
        VerificationDO lastVerification = verificationDataService.getByEmailAndType(
            email, VerificationType.REGISTER.value(), false);

        if (lastVerification != null) {
            int sendIntervalSeconds = systemProperties.getUser().getVerificationCodeSendIntervalSeconds();
            LocalDateTime canSendAt = lastVerification.getCreatedAt().plusSeconds(sendIntervalSeconds);

            if (LocalDateTime.now().isBefore(canSendAt)) {
                log.warn("用户验证码发送过于频繁: email={}", email);
                throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
            }
        }

        // 2. 创建新验证码
        VerificationDO verification = new VerificationDO(email, code);
        verificationDataService.insert(verification);
        log.info("用户验证码创建成功: email={}", email);
    }

    /**
     * 验证邮箱
     *
     * @param email 邮箱
     * @param code 验证码
     * @return 验证后的用户对象
     * @deprecated 同 {@link #createVerificationCode}，OTP 流程已改走 Redis，本方法无调用方。
     */
    @Deprecated
    @Transactional
    public UserDO validateEmail(String email, String code) {
        // 1. 查询未使用的注册类型验证码
        VerificationDO verificationDO = verificationDataService.getByEmailAndType(email, VerificationType.REGISTER.value(), false);
        if (verificationDO == null) {
            throw StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }

        // 2. 检查验证码是否过期
        int expiryMinutes = systemProperties.getUser().getVerificationCodeExpiryMinutes();
        LocalDateTime expiresAt = verificationDO.getCreatedAt().plusMinutes(expiryMinutes);
        if (LocalDateTime.now().isAfter(expiresAt)) {
            log.warn("用户验证码已过期: email={}", email);
            throw StatusCode.USER_VERIFICATION_CODE_EXPIRED.exception();
        }

        // 3. 验证验证码
        if (!verificationDO.getCode().equals(code)) {
            throw StatusCode.USER_VERIFICATION_CODE_INVALID.exception();
        }

        // 4. 标记验证码已使用
        verificationDO.setUsed(true);
        verificationDataService.update(verificationDO);

        // 5. 更新用户邮箱验证状态
        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception("用户不存在");
        }

        if (!user.getEmailValidated()) {
            userDataService.updateEmailValidated(user.getId(), true);
            log.info("用户邮箱验证成功: userId={}", user.getId());
        }

        return user;
    }

    /**
     * 验证用户登录（忽略邮箱是否已验证）
     * <p>
     * 用于登录流程：密码正确但邮箱未验证时，需要返回用户对象让上层引导到邮箱验证流程，
     * 而不是直接抛 USER_EMAIL_NOT_VALIDATED。
     */
    public UserDO validateLoginIgnoringEmailValidated(String email, String password) {
        UserDO userDO = userDataService.getByEmail(email);
        if (userDO == null) {
            throw StatusCode.USER_LOGIN_FAILED.exception();
        }

        if (!passwordEncoder.matches(password, userDO.getPassword())) {
            throw StatusCode.USER_LOGIN_FAILED.exception();
        }

        if (UserState.BANNED.value().equals(userDO.getState())) {
            throw StatusCode.USER_BANNED.exception();
        }

        return userDO;
    }

    /**
     * 验证用户登录（要求邮箱已验证）
     *
     * @param email 邮箱
     * @param password 密码（明文，将被BCrypt验证）
     * @return 验证通过的用户对象
     * @deprecated 当前登录流程使用 {@link #validateLoginIgnoringEmailValidated}（未验证邮箱返回用户对象让上层引导验证），
     * 本方法已无调用方。保留用于参考，未来应删除。
     */
    @Deprecated
    public UserDO validateLogin(String email, String password) {
        UserDO userDO = userDataService.getByEmail(email);
        if (userDO == null) {
            throw StatusCode.USER_LOGIN_FAILED.exception();
        }

        if (!passwordEncoder.matches(password, userDO.getPassword())) {
            throw StatusCode.USER_LOGIN_FAILED.exception();
        }

        if (!userDO.getEmailValidated()) {
            throw StatusCode.USER_EMAIL_NOT_VALIDATED.exception();
        }

        if (UserState.BANNED.value().equals(userDO.getState())) {
            throw StatusCode.USER_BANNED.exception();
        }

        return userDO;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成随机Base62字符串
     * Base62: 0-9a-zA-Z (62个字符)
     *
     * @param length 字符串长度
     * @return 随机Base62字符串
     */
    private String generateRandomBase62(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(62)));
        }
        return sb.toString();
    }
}
