package com.prosper.learn.application.service;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.SubscriptionDTO;
import com.prosper.learn.application.dto.response.course.CourseSummaryWithStatsAndProgressDTO;
import com.prosper.learn.application.dto.response.user.*;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.interaction.follow.FollowDO;
import com.prosper.learn.interaction.follow.FollowDataService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserCourseDomainService;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 用户业务服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserDomainService userDomainService;
    private final UserDataService userDataService;
    private final CourseDataService courseDataService;
    private final FollowDataService followDataService;
    private final ContentStatsDataService contentStatsDataService;
    private final UserCourseDomainService userCourseDomainService;
    private final EmailService emailService;
    private final MessageService messageService;
    private final SystemProperties systemProperties;
    private final UserConverter userConverter;
    private final CourseConverter courseConverter;
    private final ApplicationEventPublisher eventPublisher;

    // ========== 常量定义 ==========

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // ========== 公共方法 Query ==========

    /**
     * 获取当前用户完整信息（含订阅信息）
     */
    public UserProfileDTO getUser(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        return toProfileDTO(userDO);
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 获取用户公开信息（包含关注状态）
//     * 被屏蔽的用户会抛出异常
//     */
//    public UserPublicDTO getUser(Long userId, Long viewerId) {
//        validateUserId(viewerId);
//        UserDO userDO = validateUserExists(userId);
//
//        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
//            throw ErrorCode.USER_BANNED.exception();
//        }
//
//        return toPublicDTO(userDO, viewerId);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    /**
     * 根据用户名获取用户公开信息（包含关注状态）
     * 被屏蔽的用户会抛出异常
     */
    public UserPublicDTO getUserByUsername(String username, Long viewerId) {
        validateUserId(viewerId);
        UserDO userDO = userDataService.validateAndGetByName(username);

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
            throw StatusCode.USER_BANNED.exception();
        }

        return toPublicDTO(userDO, viewerId);
    }

    /**
     * 批量加载用户简要信息
     */
    public Map<Long, UserBriefDTO> getUserMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        List<UserBriefDTO> userList = toBriefDTO(userDataService.getByIds(ids));
        return userList.stream().collect(Collectors.toMap(UserBriefDTO::getId, user -> user));
    }

    /**
     * 搜索用户（返回简要信息）
     */
    public List<UserBriefDTO> searchUsers(String name) {
        validateSearchName(name);
        List<UserDO> userDOList = userDataService.searchByName(name);
        return userConverter.toBriefDTO(userDOList);
    }

    /**
     * 获取用户列表（管理员使用，返回完整信息）
     */
    public List<UserProfileDTO> getUsers(Long offsetId, int pageSize) {
        List<UserDO> userDOList;
        if (offsetId == null) {
            userDOList = userDataService.getList(pageSize);
        } else {
            userDOList = userDataService.getListPaginated(offsetId, pageSize);
        }
        // Note: 这里不填充 subscriptions，如需要可在 Controller 层调用
        return userConverter.toProfileDTO(userDOList);
    }

    /**
     * 更新用户状态（管理员操作）
     */
    public UserProfileDTO updateUserState(Long userId, boolean ban, UserDO operator) {
        // 委托给 DomainService
        userDomainService.updateUserState(userId, ban);

        log.info("管理员 {} {} 用户 {}", operator.getId(), ban ? "封禁" : "解封", userId);

        // 查询并返回 DTO
        UserDO userDO = userDataService.getById(userId);
        return userConverter.toProfileDTO(userDO);
    }

    /**
     * 修改用户角色（管理员操作）
     * 只有管理员可以修改用户角色
     * 只有超级管理员可以设置超级管理员
     */
    public UserProfileDTO setUserRole(Long userId, Integer roleCode, UserDO operator) {
        validateUserId(userId);

        // 委托给 DomainService，传入操作者信息
        UserRole newRole = UserRole.fromCode(roleCode);
        UserRole operatorRole = UserRole.fromCode(operator.getRole());
        userDomainService.setUserRole(userId, operator.getId(), operatorRole, roleCode);

        log.info("管理员 {} 将用户 {} 的角色修改为 {}", operator.getId(), userId, newRole.getDescription());

        // 查询并返回 DTO
        UserDO userDO = userDataService.getById(userId);
        return userConverter.toProfileDTO(userDO);
    }

    /**
     * 获取用户订阅
     */
    public Object getUserSubscriptions(Long userId) {
        userDataService.validateAndGet(userId);

        // 获取订阅ID列表
        List<Long> subscriptionIds = userDomainService.getSubscriptionIds(userId);
        if (subscriptionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域查询：获取课程信息
        List<CourseDO> courseDOList = courseDataService.getByIds(subscriptionIds);
        log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(),
            courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));

        // 转换为DTO（包含统计字段）
        List<CourseSummaryWithStatsAndProgressDTO> dtoList = courseConverter.toSummaryWithStatsAndProgressDTO(courseDOList);

        // 批量填充统计信息和用户信息（learnerCount, subscriptionCount, subscribed, progress）
        fillStatsAndProgressForCourses(dtoList, userId);

        // 填充父课程名称（如果是子课程）
        fillParentCourseNames(dtoList);

        return dtoList;
    }

    /**
     * 填充父课程名称
     * 对于子课程，查询并填充其父课程名称
     */
    private void fillParentCourseNames(List<CourseSummaryWithStatsAndProgressDTO> dtoList) {
        // 收集所有需要查询的父课程ID
        List<Long> parentCourseIds = dtoList.stream()
            .filter(dto -> dto.getParentCourseId() != null)
            .map(CourseSummaryWithStatsAndProgressDTO::getParentCourseId)
            .distinct()
            .collect(Collectors.toList());

        if (parentCourseIds.isEmpty()) {
            return;
        }

        // 批量查询父课程信息
        List<CourseDO> parentCourses = courseDataService.getByIds(parentCourseIds);
        Map<Long, String> parentCourseNameMap = parentCourses.stream()
            .collect(Collectors.toMap(CourseDO::getId, CourseDO::getName));

        // 填充父课程名称
        for (CourseSummaryWithStatsAndProgressDTO dto : dtoList) {
            if (dto.getParentCourseId() != null) {
                dto.setParentCourseName(parentCourseNameMap.get(dto.getParentCourseId()));
            }
        }
    }


    // ========== 公共方法 Command ==========

    /**
     * 更新当前用户信息
     */
    public void updateCurrentUser(Long userId, String name, String biography) {
        validateUsername(name);

        // 委托给 DomainService
        userDomainService.updateUserInfo(userId, name, biography);
    }

    /**
     * 更新用户头像
     */
    @Transactional
    public void updateUserAvatar(Long userId, String avatarUrl) {
        log.info("更新用户 {} 头像: {}", userId, avatarUrl);
        userDomainService.updateUserAvatar(userId, avatarUrl);
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(String email, String password) {
        validateEmailFormat(email);
        validatePassword(password);

        // 委托给 DomainService 创建用户
        UserDO user = userDomainService.createUser(email, password);

        // 跨域操作：异步发送验证邮件（不阻塞注册流程）
        if (systemProperties.getUser().isEnableEmailValidation()) {
            String code = generateVerificationCode();
            userDomainService.createVerificationCode(email, code);

            // 异步发送邮件，失败不影响注册
            emailService.sendVerificationEmailAsync(email, code);
        }

        log.info("用户注册成功: {}", email);
    }

    /**
     * 用户登录验证
     * 只做业务验证，不操作认证状态
     */
    public UserBriefDTO validateLogin(String email, String password) {
        validateEmailFormat(email);
        validatePassword(password);

        // 委托给 DomainService 验证
        UserDO userDO = userDomainService.validateLogin(email, password);

        return toBriefDTO(userDO);
    }

    /**
     * 邮箱验证 - 验证邮箱验证码
     * 只做验证逻辑，不操作认证状态
     */
    @Transactional
    public UserProfileDTO verifyEmailWithCode(String email, String code) {
        validateEmailFormat(email);
        validateVerificationCode(code);

        // 委托给 DomainService 验证
        UserDO user = userDomainService.validateEmail(email, code);

        return toProfileDTO(user);
    }

    /**
     * 重新发送验证码
     *
     * @param email 邮箱
     */
    public void resendVerificationCode(String email) {
        validateEmailFormat(email);

        // 1. 验证用户是否存在
        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw StatusCode.USER_NOT_FOUND.exception("用户不存在");
        }

        // 2. 检查邮箱是否已验证
        if (user.getEmailValidated()) {
            throw StatusCode.INVALID_PARAMETER.exception("邮箱已验证，无需重新发送");
        }

        // 3. 生成新验证码并发送
        String code = generateVerificationCode();
        userDomainService.createVerificationCode(email, code);

        // 4. 异步发送邮件
        emailService.sendVerificationEmailAsync(email, code);

        log.info("重新发送验证码成功: {}", email);
    }

    /**
     * 添加订阅
     */
    @Transactional
    public void subscribe(Long userId, Long courseId) {
        courseDataService.validateAndGet(courseId);
        validateUserId(userId);

        // 委托给 DomainService 添加订阅
        userDomainService.addSubscription(userId, courseId);

        // 发布收藏事件
        eventPublisher.publishEvent(new ContentBookmarkedEvent(
            userId,
            courseId,
            ContentType.course
        ));
    }

    /**
     * 取消订阅
     */
    @Transactional
    public void unsubscribe(Long userId, Long courseId) {
        courseDataService.validateAndGet(courseId);
        validateUserId(userId);

        // 委托给 DomainService 取消订阅
        userDomainService.removeSubscription(userId, courseId);

        // 发布取消收藏事件
        eventPublisher.publishEvent(new ContentUnbookmarkedEvent(
            userId,
            courseId,
            ContentType.course
        ));
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 关注用户
//     */
//    @Transactional
//    public void follow(Long followerId, Long followeeId) {
//        validateUserId(followerId);
//        validateUserExists(followeeId);
//
//        if (followerId.equals(followeeId)) {
//            throw ErrorCode.INVALID_PARAMETER.exception();
//        }
//
//        FollowDO followDO = followDataService.get(followerId, followeeId);
//        if (systemProperties.getUser().isEnableDuplicateFollowCheck() && followDO != null) {
//            throw ErrorCode.USER_ALREADY_FOLLOWED.exception();
//        }
//
//        if (followDO == null) {
//            UserDO follower = userDataService.getById(followerId);
//            followDataService.insert(followerId, followeeId);
//            messageService.createFollowMessage(followeeId, follower.getId());
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 取消关注
//     */
//    @Transactional
//    public void unfollow(Long followerId, Long followeeId) {
//        validateUserId(followerId);
//        validateUserExists(followeeId);
//
//        FollowDO followDO = followDataService.get(followerId, followeeId);
//        if (followDO != null) {
//            followDataService.delete(followerId, followeeId);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    /**
     * 生成验证码
     * 使用 SecureRandom 确保密码学安全
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int min = systemProperties.getUser().getVerificationCodeMin();
        int max = systemProperties.getUser().getVerificationCodeMax();
        int code = min + random.nextInt(max - min + 1);
        return String.valueOf(code);
    }

    //=========== DTO转换方法 ==========

    /**
     * 转换为用户简要 DTO（id + name）
     * 用途：作者署名、用户引用
     */
    public UserBriefDTO toBriefDTO(UserDO userDO) {
        return userConverter.toBriefDTO(userDO);
    }

    public List<UserBriefDTO> toBriefDTO(List<UserDO> userDOList) {
        return userConverter.toBriefDTO(userDOList);
    }

    /**
     * 转换为用户摘要 DTO（公开信息）
     * 用途：用户列表、作者信息
     * 替代：原 V1
     */
    public UserSummaryDTO toSummaryDTO(UserDO userDO) {
        return userConverter.toSummaryDTO(userDO);
    }

    public List<UserSummaryDTO> toSummaryDTO(List<UserDO> userDOList) {
        return userConverter.toSummaryDTO(userDOList);
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 转换为带订阅信息的用户 DTO
//     * 用途：用户个人中心
//     * 替代：原 V3
//     */
//    public UserWithSubscriptionsDTO toWithSubscriptionsDTO(UserDO userDO) {
//        if (userDO == null) return null;
//
//        UserWithSubscriptionsDTO dto = userConverter.toWithSubscriptionsDTO(userDO);
//        dto.setSubscriptions(getSubscriptions(userDO.getId()));
//        return dto;
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    /**
     * 转换为公开用户信息 DTO（含关注状态）
     * 用途：查看其他用户主页
     * 替代：原 V4
     */
    public UserPublicDTO toPublicDTO(UserDO userDO, Long viewerId) {
        UserPublicDTO dto = userConverter.toPublicDTO(userDO);
        dto.setIsFollowing(false);

        FollowDO followDO = followDataService.get(viewerId, userDO.getId());
        if (followDO != null) {
            dto.setIsFollowing(true);
        }
        return dto;
    }

    /**
     * 转换为用户完整资料 DTO（含敏感信息）
     * 用途：用户查看自己的完整资料
     * 替代：原 toDTO
     */
    public UserProfileDTO toProfileDTO(UserDO userDO) {
        if (userDO == null) return null;

        UserProfileDTO dto = userConverter.toProfileDTO(userDO);
        dto.setSubscriptions(getSubscriptions(userDO.getId()));
        return dto;
    }


    // ========== 私有辅助方法 ==========

    /**
     * 获取用户订阅信息
     */
    private SubscriptionDTO[] getSubscriptions(Long userId) {
        // 使用 DomainService 获取订阅ID列表
        List<Long> ids = userDomainService.getSubscriptionIds(userId);
        if (ids.isEmpty()) {
            return new SubscriptionDTO[0];
        }

        // 跨域查询：获取课程信息
        List<CourseDO> courseDOList = courseDataService.getByIds(ids);
        SubscriptionDTO[] subscriptionDTOS = new SubscriptionDTO[courseDOList.size()];
        int i = 0;
        for (CourseDO courseDO : courseDOList) {
            subscriptionDTOS[i++] = new SubscriptionDTO(courseDO.getId(), courseDO.getName());
        }
        return subscriptionDTOS;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证邮箱格式
     */
    private void validateEmailFormat(String email) {
        if (!StringUtils.hasText(email)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw StatusCode.USER_INVALID_EMAIL_FORMAT.exception();
        }
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
        if (username.length() > systemProperties.getUser().getMaxUsernameLength()) {
            throw StatusCode.USER_INVALID_USERNAME_LENGTH.exception();
        }
    }

    private void validatePassword(String password) {
        // 长度检查
        if (password == null || password.length() < systemProperties.getUser().getMinPasswordLength()) {
            throw StatusCode.USER_INVALID_PASSWORD_LENGTH.exception();
        }

        // 强度检查：必须包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");

        if (!hasLetter || !hasDigit) {
            throw StatusCode.USER_PASSWORD_TOO_WEAK.exception();
        }
    }

    private void validateSearchName(String name) {
        if (!StringUtils.hasText(name)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    private void validateVerificationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    private void validateSubscriptionString(String subscription) {
        if (subscription == null) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    private List<Long> parseAndValidateSubscriptionIds(String subscription) {
        List<Long> ids = new ArrayList<>();
        String[] parts = subscription.split(",");
        for (String part : parts) {
            try {
                Long id = Long.parseLong(part.trim());
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException e) {
                log.error("解析订阅ID失败: {}", part, e);
            }
        }
        return ids;
    }

    /**
     * 批量填充课程统计信息和用户进度信息
     */
    private void fillStatsAndProgressForCourses(List<CourseSummaryWithStatsAndProgressDTO> dtoList, Long userId) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        // 收集所有课程ID
        List<Long> courseIds = dtoList.stream()
                .map(CourseSummaryWithStatsAndProgressDTO::getId)
                .toList();

        // 批量查询统计数据
        List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(ContentType.course, courseIds);
        Map<Long, ContentStatsDO> statsMap = statsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats, (a, b) -> a));

        // 批量查询学习进度
        Map<Long, Integer> progressMap = new HashMap<>();
        // 使用批量查询避免 N+1 问题
        Map<Long, UserCourseDO> userCoursesMap = userCourseDomainService.getUserCoursesBatch(userId, courseIds);
        progressMap = userCoursesMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getProgressPercent() != null ?
                        entry.getValue().getProgressPercent() : 0
            ));

        // 填充每个课程的统计字段和用户字段
        for (CourseSummaryWithStatsAndProgressDTO dto : dtoList) {
            // 填充统计信息
            ContentStatsDO stats = statsMap.get(dto.getId());
            if (stats != null) {
                dto.setLearnerCount(stats.getLearnerCount() != null ? stats.getLearnerCount() : 0);
                dto.setBookmarkCount(stats.getBookmarkCount() != null ? stats.getBookmarkCount() : 0);
            } else {
                dto.setLearnerCount(0);
                dto.setBookmarkCount(0);
            }

            // 填充用户信息（userId不为null时，说明用户已登录且正在查看自己的订阅，所以subscribed应该是true）
            dto.setBookmarked(true);  // 用户订阅列表中的课程，subscribed 始终为 true
            dto.setProgress(progressMap.getOrDefault(dto.getId(), 0));
        }
    }

// --注释掉检查 START (2025/12/10 11:32):
//    private FolloweeDTO createFolloweeDTO(FollowDO followDO, UserDO userDO) {
//        FolloweeDTO followeeDTO = new FolloweeDTO();
//        followeeDTO.setId(followDO.getFolloweeId());
//        followeeDTO.setName(userDO.getName());
//        followeeDTO.setBiography(userDO.getBiography());
//        followeeDTO.setCreatedAt(Utils.getTimeString(followDO.getCreatedAt()));
//        return followeeDTO;
//    }
// --注释掉检查 STOP (2025/12/10 11:32)
}