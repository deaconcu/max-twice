package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.FolloweeDTO;
import com.prosper.learn.application.dto.response.SubscriptionDTO;
import com.prosper.learn.application.dto.response.user.*;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.interaction.follow.FollowDO;
import com.prosper.learn.interaction.follow.FollowDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final JavaMailSender mailSender;
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

    /**
     * 获取用户公开信息（包含关注状态）
     * 被屏蔽的用户会抛出异常
     */
    public UserPublicDTO getUser(Long userId, Long viewerId) {
        validateUserId(viewerId);
        UserDO userDO = validateUserExists(userId);

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
            throw ErrorCode.USER_BANNED.exception();
        }

        return toPublicDTO(userDO, viewerId);
    }

    /**
     * 根据用户名获取用户公开信息（包含关注状态）
     * 被屏蔽的用户会抛出异常
     */
    public UserPublicDTO getUserByUsername(String username, Long viewerId) {
        validateUserId(viewerId);
        UserDO userDO = userDataService.validateAndGetByName(username);

        if (userDO.getState() != null && userDO.getState() == UserState.BANNED.value()) {
            throw ErrorCode.USER_BANNED.exception();
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
        validateUserExists(userId);

        // 获取订阅ID列表
        List<Long> subscriptionIds = userDomainService.getSubscriptionIds(userId);
        if (subscriptionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域查询：获取课程信息
        List<CourseDO> courseDOList = courseDataService.getByIds(subscriptionIds);
        log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(),
            courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));

        // 转换为DTO
        return courseConverter.toSummaryDTO(courseDOList);
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
     * 用户注册
     */
    @Transactional
    public void register(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        // 委托给 DomainService 创建用户
        UserDO user = userDomainService.createUser(email, password);

        // 跨域操作：发送验证邮件
        if (systemProperties.getUser().isEnableEmailValidation()) {
            String code = generateVerificationCode();
            userDomainService.createVerificationCode(email, code);
            sendVerificationEmail(email, code);
        }
    }

    /**
     * 用户登录验证
     * 只做业务验证，不操作认证状态
     */
    public UserBriefDTO validateLogin(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        // 委托给 DomainService 验证
        UserDO userDO = userDomainService.validateLogin(email, password);

        return toBriefDTO(userDO);
    }

    /**
     * 邮箱验证
     * 只做验证逻辑，不操作认证状态
     */
    @Transactional
    public UserProfileDTO validateEmail(String email, String code) {
        validateEmail(email);
        validateVerificationCode(code);

        // 委托给 DomainService 验证
        UserDO user = userDomainService.validateEmail(email, code);

        return toProfileDTO(user);
    }

    /**
     * 添加订阅
     */
    @Transactional
    public Object subscribe(Long userId, Long courseId) {
        validateCourseExists(courseId);
        validateUserId(userId);

        // 委托给 DomainService 添加订阅
        boolean checkDuplicate = systemProperties.getUser().isEnableDuplicateSubscriptionCheck();
        List<Long> subscriptionIds = userDomainService.addSubscription(userId, courseId, checkDuplicate);

        // 发布收藏事件
        eventPublisher.publishEvent(new ContentBookmarkedEvent(
            userId,
            courseId,
            ContentType.course
        ));

        return subscriptionIds.stream().mapToInt(Long::intValue).toArray();
    }

    /**
     * 批量更新订阅
     */
    @Transactional
    public Object updateSubscriptions(Long userId, String subscription) {
        validateUserId(userId);
        validateSubscriptionString(subscription);

        // 解析并验证订阅ID
        List<Long> ids = parseAndValidateSubscriptionIds(subscription);

        // 跨域验证：验证课程是否存在
        List<CourseDO> courseDOList = courseDataService.getByIds(ids);
        List<Long> validIds = courseDOList.stream()
            .map(CourseDO::getId)
            .collect(Collectors.toList());

        // 委托给 DomainService 更新订阅
        userDomainService.updateSubscriptions(userId, validIds);

        return validIds;
    }

    /**
     * 取消订阅
     */
    @Transactional
    public Object unsubscribe(Long userId, Long courseId) {
        validateCourseExists(courseId);
        validateUserId(userId);

        // 委托给 DomainService 取消订阅
        List<Long> subscriptionIds = userDomainService.removeSubscription(userId, courseId);

        // 发布取消收藏事件
        eventPublisher.publishEvent(new ContentUnbookmarkedEvent(
            userId,
            courseId,
            ContentType.course
        ));

        return subscriptionIds.stream().mapToInt(Long::intValue).toArray();
    }

    /**
     * 关注用户
     */
    @Transactional
    public void follow(Long followerId, Long followeeId) {
        validateUserId(followerId);
        validateUserExists(followeeId);
        
        if (followerId.equals(followeeId)) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }

        FollowDO followDO = followDataService.get(followerId, followeeId);
        if (systemProperties.getUser().isEnableDuplicateFollowCheck() && followDO != null) {
            throw ErrorCode.USER_ALREADY_FOLLOWED.exception();
        }
        
        if (followDO == null) {
            UserDO follower = userDataService.getById(followerId);
            followDataService.insert(followerId, followeeId);
            messageService.createFollowMessage(followeeId, follower.getId());
        }
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        validateUserId(followerId);
        validateUserExists(followeeId);

        FollowDO followDO = followDataService.get(followerId, followeeId);
        if (followDO != null) {
            followDataService.delete(followerId, followeeId);
        }
    }

    /**
     * 发送验证邮件
     */
    private void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(systemProperties.getUser().getEmailSender());
        message.setTo(toEmail);
        message.setSubject(systemProperties.getUser().getEmailSubject());
        message.setText("Your verification code is: " + code);
        mailSender.send(message);
    }

    private String generateVerificationCode() {
        Random random = new Random();
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

    /**
     * 转换为带订阅信息的用户 DTO
     * 用途：用户个人中心
     * 替代：原 V3
     */
    public UserWithSubscriptionsDTO toWithSubscriptionsDTO(UserDO userDO) {
        if (userDO == null) return null;

        UserWithSubscriptionsDTO dto = userConverter.toWithSubscriptionsDTO(userDO);
        dto.setSubscriptions(getSubscriptions(userDO.getId()));
        return dto;
    }

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
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    public UserDO validateUserExists(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    private void validateCourseExists(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
    }
    
    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw ErrorCode.USER_INVALID_EMAIL_FORMAT.exception();
        }
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        if (username.length() > systemProperties.getUser().getMaxUsernameLength()) {
            throw ErrorCode.USER_INVALID_USERNAME_LENGTH.exception();
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < systemProperties.getUser().getMinPasswordLength()) {
            throw ErrorCode.USER_INVALID_PASSWORD_LENGTH.exception();
        }
    }

    private void validateSearchName(String name) {
        if (!StringUtils.hasText(name)) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }

    private void validateVerificationCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }

    private void validateSubscriptionString(String subscription) {
        if (subscription == null) {
            throw ErrorCode.INVALID_PARAMETER.exception();
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

    private FolloweeDTO createFolloweeDTO(FollowDO followDO, UserDO userDO) {
        FolloweeDTO followeeDTO = new FolloweeDTO();
        followeeDTO.setId(followDO.getFolloweeId());
        followeeDTO.setName(userDO.getName());
        followeeDTO.setBiography(userDO.getBiography());
        followeeDTO.setCreatedAt(Utils.getTimeString(followDO.getCreatedAt()));
        return followeeDTO;
    }
}