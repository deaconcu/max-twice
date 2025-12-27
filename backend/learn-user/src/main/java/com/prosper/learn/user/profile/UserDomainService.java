package com.prosper.learn.user.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.auth.VerificationDO;
import com.prosper.learn.user.auth.VerificationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.UserRole;
import static com.prosper.learn.shared.domain.Enums.UserState;

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
    private static final int MAX_PINNED_ROADMAPS = 19;
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UserDataService userDataService;
    private final UserProfileDataService userProfileDataService;
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
            userDO.setState(UserState.BANNED.value());
        } else {
            userDO.setState(UserState.ACTIVE.value());
        }

        userDataService.update(userDO);
        log.info("User {} state changed to: {}", userId, ban ? "BANNED" : "ACTIVE");
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
        targetUser.setRole(roleCode);
        userDataService.update(targetUser);

        log.info("User {} role changed to {} by operator {}", userId, newRole.getDescription(), operatorId);
    }

    /**
     * 更新用户基本信息
     */
    @Transactional
    public void updateUserInfo(Long userId, String name, String biography) {
        UserDO userDO = userDataService.validateAndGet(userId);

        userDO.setName(name);
        userDO.setBiography(biography);
        userDataService.update(userDO);

        log.info("User {} info updated: name={}", userId, name);
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateUserAvatar(Long userId, String avatarUrl) {
        int updated = userDataService.updateAvatar(userId, avatarUrl);
        if (updated == 0) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        log.info("User {} avatar updated: {}", userId, avatarUrl);
    }

    // ========== 用户注册和验证 ==========

    /**
     * 创建用户（注册）
     *
     * @param email 邮箱
     * @param password 密码（明文，将被MD5加密）
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
        user.setPassword(Utils.md5(password));
        user.setEmail(email);
        user.setBiography("");
        user.setState(UserState.ACTIVE.value());
        user.setRole(UserRole.USER.getCode());
        userDataService.insert(user);

        log.info("User created: userId={}, email={}", user.getId(), email);
        return user;
    }

    /**
     * 创建邮箱验证码
     *
     * @param email 邮箱
     * @param code 验证码
     */
    @Transactional
    public void createVerificationCode(String email, String code) {
        VerificationDO verification = new VerificationDO(email, code);
        verificationDataService.insert(verification);
        log.info("Verification code created for email: {}", email);
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
        // 1. 查询未使用的验证码
        VerificationDO verificationDO = verificationDataService.getByEmail(email, false);
        if (verificationDO == null) {
            throw StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }

        // 2. 检查验证码是否过期
        int expiryMinutes = systemProperties.getUser().getVerificationCodeExpiryMinutes();
        LocalDateTime expiresAt = verificationDO.getCreatedAt().plusMinutes(expiryMinutes);
        if (LocalDateTime.now().isAfter(expiresAt)) {
            log.warn("Verification code expired for email: {}", email);
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
            throw StatusCode.USER_NOT_FOUND.exception();
        }

        if (!user.getEmailValidated()) {
            user.setEmailValidated(true);
            userDataService.update(user);
            log.info("User {} email validated", user.getId());
        }

        return user;
    }

    /**
     * 验证用户登录
     *
     * @param email 邮箱
     * @param password 密码（明文，将被MD5加密）
     * @return 验证通过的用户对象
     */
    public UserDO validateLogin(String email, String password) {
        UserDO userDO = userDataService.getByEmail(email);
        if (userDO == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }

        // TODO: 密码验证
        // if (!userDO.getPassword().equals(Utils.md5(password))) {
        //     throw ErrorCode.USER_PASSWORD_WRONG.exception();
        // }

        if (!userDO.getEmailValidated()) {
            throw StatusCode.USER_EMAIL_NOT_VALIDATED.exception();
        }

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
            throw StatusCode.USER_BANNED.exception();
        }

        return userDO;
    }

    // ========== 订阅管理 ==========

    /**
     * 添加订阅
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param checkDuplicate 是否检查重复订阅
     * @return 更新后的订阅ID列表
     */
    @Transactional
    public List<Long> addSubscription(Long userId, Long courseId, boolean checkDuplicate) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);

        if (userProfileDO == null) {
            // 创建新的用户配置
            userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(userId);
            userProfileDO.setSubscription(String.valueOf(courseId));
            userProfileDO.setRoadmapPin("{}");
            userProfileDataService.insert(userProfileDO);
            log.info("User {} subscribed to course {}", userId, courseId);
            return Collections.singletonList(courseId);
        } else {
            // 更新现有订阅
            List<Long> ids = parseSubscriptionIds(userProfileDO.getSubscription());
            if (checkDuplicate && ids.contains(courseId)) {
                throw StatusCode.USER_COURSE_ALREADY_SUBSCRIBED.exception();
            }
            ids.add(courseId);
            userProfileDO.setSubscription(formatSubscriptionIds(ids));
            userProfileDataService.update(userProfileDO);
            log.info("User {} subscribed to course {}", userId, courseId);
            return ids;
        }
    }

    /**
     * 取消订阅
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 更新后的订阅ID列表
     */
    @Transactional
    public List<Long> removeSubscription(Long userId, Long courseId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null || !StringUtils.hasText(userProfileDO.getSubscription())) {
            throw StatusCode.USER_COURSE_NOT_SUBSCRIBED.exception();
        }

        List<Long> ids = parseSubscriptionIds(userProfileDO.getSubscription());
        if (!ids.contains(courseId)) {
            throw StatusCode.USER_COURSE_NOT_SUBSCRIBED.exception();
        }

        ids.remove(courseId);
        userProfileDO.setSubscription(formatSubscriptionIds(ids));
        userProfileDataService.update(userProfileDO);

        log.info("User {} unsubscribed from course {}", userId, courseId);
        return ids;
    }

    /**
     * 批量更新订阅
     *
     * @param userId 用户ID
     * @param courseIds 课程ID列表
     */
    @Transactional
    public void updateSubscriptions(Long userId, List<Long> courseIds) {
        String idsStr = formatSubscriptionIds(courseIds);

        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(userId);
            userProfileDO.setSubscription(idsStr);
            userProfileDO.setRoadmapPin("{}");
            userProfileDataService.insert(userProfileDO);
        } else {
            userProfileDO.setSubscription(idsStr);
            userProfileDataService.update(userProfileDO);
        }

        log.info("User {} subscriptions updated: {} courses", userId, courseIds.size());
    }

    /**
     * 获取用户订阅的课程ID列表
     */
    public List<Long> getSubscriptionIds(Long userId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null || !StringUtils.hasText(userProfileDO.getSubscription())) {
            return new ArrayList<>();
        }
        return parseSubscriptionIds(userProfileDO.getSubscription());
    }

    /**
     * 检查用户是否订阅了指定课程
     */
    public boolean isSubscribed(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return false;
        }
        List<Long> subscriptionIds = getSubscriptionIds(userId);
        return subscriptionIds.contains(courseId);
    }

    // ========== 路线图置顶相关方法 ==========

    /**
     * 切换路线图置顶状态
     *
     * @param userId 用户ID
     * @param professionId 职业ID
     * @param roadmapId 路线图ID
     * @return true表示置顶，false表示取消置顶
     */
    @Transactional
    public boolean toggleRoadmapPin(Long userId, Long professionId, Long roadmapId) {
        // 获取用户配置
        UserProfileDO userProfile = userProfileDataService.getById(userId);

        // 解析置顶数据
        Map<String, List<Long>> pinMap = parsePinMap(userProfile);

        // 更新置顶列表
        String professionKey = String.valueOf(professionId);
        List<Long> professionPins = pinMap.getOrDefault(professionKey, new ArrayList<>());
        boolean isPinned = professionPins.contains(roadmapId);
        boolean result;

        if (isPinned) {
            // 取消置顶
            professionPins.remove(roadmapId);
            result = false;
        } else {
            // 添加置顶
            if (professionPins.size() >= MAX_PINNED_ROADMAPS) {
                throw StatusCode.ROADMAP_PIN_LIMIT_EXCEEDED.exception();
            }
            professionPins.add(roadmapId);
            result = true;
        }

        // 清理空列表
        if (professionPins.isEmpty()) {
            pinMap.remove(professionKey);
        } else {
            pinMap.put(professionKey, professionPins);
        }

        // 保存更新后的置顶数据
        savePinMap(userId, userProfile, pinMap);

        log.info("User {} {} roadmap {} in profession {}",
            userId, result ? "pinned" : "unpinned", roadmapId, professionId);

        return result;
    }

    /**
     * 获取用户在某个职业下置顶的路线图ID列表
     *
     * @param userId 用户ID
     * @param professionId 职业ID
     * @return 置顶的路线图ID列表
     */
    public List<Long> getPinnedRoadmapIds(Long userId, Long professionId) {
        UserProfileDO userProfile = userProfileDataService.getById(userId);
        if (userProfile == null || userProfile.getRoadmapPin() == null) {
            return new ArrayList<>();
        }

        try {
            Map<String, List<Long>> pinMap = objectMapper.readValue(
                userProfile.getRoadmapPin(), new TypeReference<>() {});
            List<Long> professionPins = pinMap.get(professionId.toString());
            return professionPins != null ? professionPins : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("解析置顶数据失败: userId={}, professionId={}", userId, professionId, e);
            return new ArrayList<>();
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 解析用户的路线图置顶数据
     */
    private Map<String, List<Long>> parsePinMap(UserProfileDO userProfile) {
        if (userProfile == null || userProfile.getRoadmapPin() == null) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("解析置顶数据失败: userId={}", userProfile.getUserId(), e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 保存用户的路线图置顶数据
     */
    private void savePinMap(Long userId, UserProfileDO userProfile, Map<String, List<Long>> pinMap) {
        try {
            String updatedPinJson = objectMapper.writeValueAsString(pinMap);

            if (userProfile == null) {
                // 创建新的用户配置
                userProfile = new UserProfileDO();
                userProfile.setUserId(userId);
                userProfile.setRoadmapPin(updatedPinJson);
                userProfile.setSubscription(DEFAULT_EMPTY_STRING);
                userProfileDataService.insert(userProfile);
            } else {
                // 更新现有配置
                userProfileDataService.updateRoadmapPin(userId, updatedPinJson);
            }
        } catch (JsonProcessingException e) {
            log.error("保存置顶数据失败: userId={}", userId, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 解析订阅ID列表
     */
    private List<Long> parseSubscriptionIds(String subscription) {
        if (!StringUtils.hasText(subscription)) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(subscription.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.error("解析订阅ID失败: {}", subscription, e);
            throw StatusCode.USER_SUBSCRIPTION_PARSE_ERROR.exception(e);
        }
    }

    /**
     * 格式化订阅ID列表为字符串
     */
    private String formatSubscriptionIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return DEFAULT_EMPTY_STRING;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

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
