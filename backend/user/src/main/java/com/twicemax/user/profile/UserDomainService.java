package com.twicemax.user.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.auth.VerificationDO;
import com.twicemax.user.auth.VerificationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    // 登录失败次数相关常量
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:ip:";
    private static final int LOGIN_FAIL_MAX_ATTEMPTS = 3;
    private static final Duration LOGIN_FAIL_EXPIRE_TIME = Duration.ofMinutes(15);

    private final UserDataService userDataService;
    private final VerificationDataService verificationDataService;
    private final SystemProperties systemProperties;
    private final StringRedisTemplate stringRedisTemplate;

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
    public void setUserRole(Long userId, Long operatorId, UserRole operatorRole, Integer roleCode) {
        UserDO targetUser = userDataService.validateAndGet(userId);
        UserRole newRole = UserRole.fromCode(roleCode);

        // 1. 防止用户修改自己的角色
        if (userId.equals(operatorId)) {
            throw StatusCode.PERMISSION_DENIED.exception("不能修改自己的角色");
        }

        // 2. 只有超级管理员可以设置超级管理员
        if (newRole == UserRole.SUPER_ADMIN && operatorRole != UserRole.SUPER_ADMIN) {
            throw StatusCode.PERMISSION_DENIED.exception("只有超级管理员可以设置超级管理员");
        }

        // 3. 执行角色修改
        userDataService.updateRole(userId, roleCode);

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
     * @param password 密码（明文，将被BCrypt加密）
     * @return 创建的用户对象
     */
    @Transactional
    public UserDO createUser(String email, String password) {
        // 检查用户是否已存在
        UserDO existingUser = userDataService.getByEmail(email);
        if (existingUser != null) {
            throw StatusCode.USER_ALREADY_EXISTS.exception();
        }

        // 创建用户
        UserDO user = new UserDO();
        user.setName("MT_" + generateRandomBase62(8));
        user.setPassword(passwordEncoder.encode(password));  // BCrypt加密
        user.setEmail(email);
        user.setBiography("");
        user.setAvatar("");
        user.setState(UserState.ACTIVE.value());
        user.setRole(UserRole.USER.getCode());
        user.setEmailValidated(false); // 设置邮箱验证状态默认值
        userDataService.insert(user);

        log.info("用户创建成功: userId={}，email={}", user.getId(), email);
        return user;
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
     * 创建邮箱验证码
     *
     * @param email 邮箱
     * @param code 验证码
     */
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
     */
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

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
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
     */
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

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
            throw StatusCode.USER_BANNED.exception();
        }

        return userDO;
    }

    // ========== 登录失败次数管理 ==========

    /**
     * 检查是否需要验证码
     */
    public boolean isCaptchaRequired(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= LOGIN_FAIL_MAX_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 记录登录失败
     */
    public void recordLoginFailure(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, LOGIN_FAIL_EXPIRE_TIME);
        }
        log.debug("IP {} 登录失败次数: {}", ip, count);
    }

    /**
     * 登录成功，清除失败记录
     */
    public void clearLoginFailures(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        stringRedisTemplate.delete(key);
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
