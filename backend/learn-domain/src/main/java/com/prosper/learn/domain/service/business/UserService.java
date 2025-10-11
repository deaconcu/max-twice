package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.util.converter.UserConverter;
import com.prosper.learn.domain.service.data.*;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.dto.response.old.UserDTOV2;
import com.prosper.learn.persistence.dataobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.*;

/**
 * 用户业务服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserDataService userDataService;
    private final UserProfileDataService userProfileDataService;
    private final CourseDataService courseDataService;
    private final FollowDataService followDataService;
    private final VerificationDataService verificationDataService;
    private final JavaMailSender mailSender;
    private final MessageService messageService;
    private final SystemProperties systemProperties;
    private final UserConverter userConverter;
    private final CourseService courseService;
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // ========== 公共方法 Query ==========

    /**
     * 获取当前用户信息
     */
    public UserDTO getUser(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        return toDTO(userDO);
    }

    public UserDTO getUser(Long userId, DTOVersion dtoVersion) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        switch (dtoVersion) {
            case V1 -> { return toDTOV1(userDO); }
            case V2 -> { return toDTOV2(userDO); }
            case V3 -> { return toDTOV3(userDO); }
        }
        throw ErrorCode.USER_NOT_FOUND.exception();
    }

    /**
     * 获取用户信息（包含关注状态）
     */
    public UserDTO getUser(Long userId, Long viewerId) {
        validateUserId(viewerId);
        UserDO userDO = validateUserExists(userId);

        return toDTOV4(userDO, viewerId);
    }

    /**
     * 批量加载用户信息
     */
    public Map<Long, UserDTO> getUserMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }

        List<UserDTO> userList = toDTOV2(userDataService.getByIds(ids));
        return userList.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
    }

    /**
     * 搜索用户
     */
    public List<UserDTO> searchUsers(String name) {
        validateSearchName(name);
        List<UserDO> userDOList = userDataService.searchByName(name);
        return userConverter.toDTOV2(userDOList);
    }

    /**
     * 获取用户订阅
     */
    public Object getUserSubscriptions(Long userId) {
        validateUserExists(userId);

        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null || isEmptySubscription(userProfileDO.getSubscription())) {
            return new ArrayList<>();
        }

        return parseUserSubscriptions(userProfileDO.getSubscription(), userId);
    }


    // ========== 公共方法 Command ==========

    /**
     * 更新当前用户信息
     */
    public void updateCurrentUser(Long userId, String name, String biography) {
        UserDO userDO = validateUserExists(userId);
        validateUsername(name);
        
        userDO.setName(name);
        userDO.setBiography(biography);
        userDataService.update(userDO);
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(String userName, String email, String password) {
        validateRegistrationParams(userName, email, password);

        UserDO existingUser = userDataService.getByEmail(email);
        if (existingUser != null) {
            throw ErrorCode.USER_ALREADY_EXISTS.exception();
        }

        UserDO user = new UserDO();
        user.setName(userName);
        user.setPassword(Utils.md5(password));
        user.setEmail(email);
        user.setBiography("");
        userDataService.insert(user);

        if (systemProperties.getUser().isEnableEmailValidation()) {
            String code = generateVerificationCode();
            VerificationDO verification = new VerificationDO(email, code);
            verificationDataService.insert(verification);
            sendVerificationEmail(email, code);
        }
    }

    /**
     * 用户登录验证
     * 只做业务验证，不操作认证状态
     */
    public UserDTO validateLogin(String email, String password) {
        validateEmail(email);
        validatePassword(password);
        
        UserDO userDO = userDataService.getByEmail(email);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        
        // TODO: 密码验证
        // if (!userDO.getPassword().equals(Utils.md5(password))) throw ErrorCode.USER_PASSWORD_WRONG.exception();
        
        UserDTO userDTO = toDTOV2(userDO);
        return userDTO;
    }

    /**
     * 邮箱验证
     * 只做验证逻辑，不操作认证状态
     */
    @Transactional
    public UserDTO validateEmail(String email, String code) {
        validateEmail(email);
        validateVerificationCode(code);
        
        VerificationDO verificationDO = verificationDataService.getByEmail(email, false);
        if (verificationDO == null) {
            throw ErrorCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }
        if (!verificationDO.getCode().equals(code)) {
            throw ErrorCode.USER_VERIFICATION_CODE_INVALID.exception();
        }

        verificationDO.setUsed(true);
        verificationDataService.update(verificationDO);

        UserDO user = userDataService.getByEmail(email);
        if (user == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        
        if (user.getEmailValidated()) {
            return userConverter.toDTO(user);
        }

        user.setEmailValidated(true);
        userDataService.update(user);
        return userConverter.toDTO(user);
    }

    /**
     * 添加订阅
     */
    @Transactional
    public Object subscribe(Long userId, Long courseId) {
        validateCourseExists(courseId);
        validateUserId(userId);

        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        String idsStr;
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(userId);
            idsStr = String.valueOf(courseId);
            userProfileDO.setSubscription(idsStr);
            userProfileDO.setRoadmapPin("");
            userProfileDataService.insert(userProfileDO);
        } else {
            List<Long> ids = parseSubscriptionIds(userProfileDO.getSubscription());
            if (systemProperties.getUser().isEnableDuplicateSubscriptionCheck() && ids.contains(courseId)) {
                throw ErrorCode.USER_COURSE_ALREADY_SUBSCRIBED.exception();
            }
            ids.add(courseId);
            idsStr = formatSubscriptionIds(ids);
            userProfileDO.setSubscription(idsStr);
            userProfileDataService.update(userProfileDO);
        }

        return parseSubscriptionIdsToArray(idsStr);
    }

    /**
     * 批量更新订阅
     */
    @Transactional
    public Object updateSubscriptions(Long userId, String subscription) {
        validateUserId(userId);
        validateSubscriptionString(subscription);
        
        List<Long> ids = parseAndValidateSubscriptionIds(subscription);
        List<CourseDO> courseDOList = courseDataService.getByIds(ids);
        
        List<Long> validIds = courseDOList.stream()
            .map(CourseDO::getId)
            .collect(Collectors.toList());
        
        String idsStr = formatSubscriptionIds(validIds);
        updateUserProfile(userId, idsStr);
        
        return validIds;
    }

    /**
     * 取消订阅
     */
    @Transactional
    public Object unsubscribe(Long userId, Long courseId) {
        validateCourseExists(courseId);
        validateUserId(userId);

        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null || isEmptySubscription(userProfileDO.getSubscription())) {
            throw ErrorCode.USER_COURSE_NOT_SUBSCRIBED.exception();
        }
        
        List<Long> ids = parseSubscriptionIds(userProfileDO.getSubscription());
        if (!ids.contains(courseId)) {
            throw ErrorCode.USER_COURSE_NOT_SUBSCRIBED.exception();
        }
        
        ids.remove(courseId);
        String idsStr = formatSubscriptionIds(ids);
        userProfileDO.setSubscription(idsStr);
        userProfileDataService.update(userProfileDO);
        
        return parseSubscriptionIdsToArray(idsStr);
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

    public UserDTO toDTO(UserDO userDO) {
        return userConverter.toDTO(userDO);
    }

    public List<UserDTO> toDTO(List<UserDO> userDO) {
        return userConverter.toDTO(userDO);
    }

    /**
     * v1 = id + name + biography
     */
    public UserDTO toDTOV1(UserDO userDO) {
        return userConverter.toDTOV1(userDO);
    }

    public List<UserDTO> toDTOV1(List<UserDO> userDO) {
        return userConverter.toDTOV1(userDO);
    }

    /**
     * v2 = id + name
     */
    public UserDTO toDTOV2(UserDO userDO) {
        return userConverter.toDTOV2(userDO);
    }

    public List<UserDTO> toDTOV2(List<UserDO> userDO) {
        return userConverter.toDTOV2(userDO);
    }

    /**
     * v3 = id + name + subscriptions
     */
    public UserDTO toDTOV3(UserDO userDO) {
        if (userDO == null) return null;
        
        UserDTO userDTO = userConverter.toDTOV2(userDO);
        userDTO.setSubscriptions(getSubscriptions(userDO.getId()));
        return userDTO;
    }

    /**
     * v4 = v1 + followed (针对特定viewer)
     */
    public UserDTO toDTOV4(UserDO userDO, Long viewerId) {
        UserDTO userDTO = userConverter.toDTOV1(userDO);
        userDTO.setFollowing(false);

        FollowDO followDO = followDataService.get(viewerId, userDO.getId());
        if (followDO != null) {
            userDTO.setFollowing(true);
        }
        return userDTO;
    }


    // ========== 私有辅助方法 ==========
    
    /**
     * 获取用户订阅信息
     */
    private SubscriptionDTO[] getSubscriptions(Long userId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);

        if (userProfileDO != null && userProfileDO.getSubscription() != null
                && !userProfileDO.getSubscription().trim().isEmpty()) {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
            List<CourseDO> courseDOList = courseDataService.getByIds(ids);
            SubscriptionDTO[] subscriptionDTOS = new SubscriptionDTO[courseDOList.size()];
            int i = 0;
            for (CourseDO courseDO : courseDOList) {
                subscriptionDTOS[i++] = new SubscriptionDTO(courseDO.getId(), courseDO.getName());
            }
            return subscriptionDTOS;
        } else {
            return new SubscriptionDTO[0];
        }
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
        if (email == null || email.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw ErrorCode.USER_INVALID_EMAIL_FORMAT.exception();
        }
    }
    
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
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
        if (name == null || name.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateVerificationCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateDateTimeString(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        try {
            LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw ErrorCode.INVALID_DATE.exception();
        }
    }
    
    private void validateSubscriptionString(String subscription) {
        if (subscription == null) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateRegistrationParams(String userName, String email, String password) {
        validateUsername(userName);
        validateEmail(email);
        validatePassword(password);
    }
    
    private boolean isEmptySubscription(String subscription) {
        return subscription == null || subscription.trim().isEmpty();
    }
    
    private List<Long> parseSubscriptionIds(String subscription) {
        try {
            return Arrays.stream(subscription.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toCollection(ArrayList::new));
        } catch (NumberFormatException e) {
            throw ErrorCode.USER_SUBSCRIPTION_PARSE_ERROR.exception(e);
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
    
    private String formatSubscriptionIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return DEFAULT_EMPTY_STRING;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
    
    private int[] parseSubscriptionIdsToArray(String idsStr) {
        if (idsStr == null || idsStr.trim().isEmpty()) {
            return new int[0];
        }
        try {
            return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
        } catch (NumberFormatException e) {
            log.error("解析订阅ID数组失败: {}", idsStr, e);
            return new int[0];
        }
    }
    
    private void updateUserProfile(Long userId, String subscription) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO(userId, subscription);
            userProfileDataService.insert(userProfileDO);
        } else {
            userProfileDO.setSubscription(subscription);
            userProfileDataService.update(userProfileDO);
        }
    }
    
    private void setUserSubscriptions(UserDTOV2 userDTOV2, Long userId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO != null && !isEmptySubscription(userProfileDO.getSubscription())) {
            try {
                List<Long> ids = parseSubscriptionIds(userProfileDO.getSubscription());
                List<CourseDO> courseDOList = courseDataService.getByIds(ids);
                SubscriptionDTO[] subscriptionDTOS = courseDOList.stream()
                    .map(course -> new SubscriptionDTO(course.getId(), course.getName()))
                    .toArray(SubscriptionDTO[]::new);
                userDTOV2.setSubscriptions(subscriptionDTOS);
            } catch (Exception e) {
                log.error("获取用户{}订阅信息失败", userId, e);
                userDTOV2.setSubscriptions(new SubscriptionDTO[0]);
            }
        } else {
            userDTOV2.setSubscriptions(new SubscriptionDTO[0]);
        }
    }
    
    private Object parseUserSubscriptions(String subscription, Long userId) {
        try {
            List<Long> ids = Arrays.stream(subscription.split(","))
                .map(String::trim)
                .filter(trim -> !trim.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

            if (ids.isEmpty()) {
                return new ArrayList<>();
            }

            List<CourseDO> courseDOList = courseDataService.getByIds(ids);
            log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
            return courseService.toDTOV2(courseDOList);
        } catch (Exception e) {
            log.error("获取用户{}收藏课程时出错: {}", userId, e.getMessage());
            throw ErrorCode.USER_SUBSCRIPTION_PARSE_ERROR.exception(e);
        }
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