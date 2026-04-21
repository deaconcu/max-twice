package com.twicemax.application.service;

import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.dataservice.UserStatsDataService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.analytics.stats.mapper.UserStatsDO;
import com.twicemax.application.converter.CourseConverter;
import com.twicemax.application.converter.UserConverter;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.response.user.*;
import com.twicemax.content.course.CourseDO;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.interaction.follow.FollowDO;
import com.twicemax.interaction.follow.FollowDataService;
import com.twicemax.learning.enrollment.UserLearningDO;
import com.twicemax.learning.enrollment.UserLearningDomainService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.user.profile.UserDomainService;
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

import static com.twicemax.shared.domain.Enums.*;

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
    private final UserStatsDataService userStatsDataService;
    private final UserLearningDomainService userLearningDomainService;
    private final EmailService emailService;
    private final MessageService messageService;
    private final SystemProperties systemProperties;
    private final UserConverter userConverter;
    private final CourseConverter courseConverter;
    private final ApplicationEventPublisher eventPublisher;
    private final MeilisearchService meilisearchService;

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
    public KeysetPageResponse<UserAdminDTO> getUsers(Long offsetId, int pageSize) {
        List<UserDO> userDOList;
        if (offsetId == null) {
            userDOList = userDataService.getList(pageSize + 1);
        } else {
            userDOList = userDataService.getListPaginated(offsetId, pageSize + 1);
        }

        boolean hasMore = userDOList.size() > pageSize;
        if (hasMore) {
            userDOList = userDOList.subList(0, pageSize);
        }

        List<UserAdminDTO> items = userConverter.toAdminDTO(userDOList);

        // 批量填充统计数据
        fillUserStats(items);

        Long nextLastId = hasMore && !items.isEmpty() ? items.get(items.size() - 1).getId() : null;

        return KeysetPageResponse.of(items, hasMore, null, nextLastId);
    }

    /**
     * 更新用户状态（管理员操作）
     */
    @Transactional
    public UserAdminDTO updateUserState(Long userId, boolean ban, UserDO operator) {
        // 委托给 DomainService
        userDomainService.updateUserState(userId, ban);

        log.info("管理员 {} {} 用户 {}", operator.getId(), ban ? "封禁" : "解封", userId);

        // 查询并返回 DTO
        UserDO userDO = userDataService.getById(userId);

        // 异步更新搜索索引
        if (ban) {
            meilisearchService.deleteUser(userId);
        } else {
            meilisearchService.indexUser(userDO);
        }

        UserAdminDTO dto = userConverter.toAdminDTO(userDO);
        fillUserStats(List.of(dto));
        return dto;
    }

    /**
     * 获取用户详情（管理员使用）
     */
    public UserAdminDTO getUserForAdmin(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        UserAdminDTO dto = userConverter.toAdminDTO(userDO);
        fillUserStats(List.of(dto));
        return dto;
    }

    /**
     * 搜索用户（管理员使用）
     */
    public List<UserAdminDTO> searchUsersForAdmin(String name) {
        validateSearchName(name);
        List<UserDO> userDOList = userDataService.searchByName(name);
        List<UserAdminDTO> items = userConverter.toAdminDTO(userDOList);
        fillUserStats(items);
        return items;
    }

    /**
     * 修改用户角色（管理员操作）
     * 只有管理员可以修改用户角色
     * 只有超级管理员可以设置超级管理员
     */
    @Transactional
    public UserAdminDTO setUserRole(Long userId, Integer roleCode, UserDO operator) {
        validateUserId(userId);

        // 委托给 DomainService，传入操作者信息
        UserRole newRole = UserRole.fromCode(roleCode);
        UserRole operatorRole = UserRole.fromCode(operator.getRole());
        userDomainService.setUserRole(userId, operator.getId(), operatorRole, roleCode);

        log.info("管理员 {} 将用户 {} 的角色修改为 {}", operator.getId(), userId, newRole.getDescription());

        // 查询并返回 DTO
        UserDO userDO = userDataService.getById(userId);
        UserAdminDTO dto = userConverter.toAdminDTO(userDO);
        fillUserStats(List.of(dto));
        return dto;
    }

    // ========== 公共方法 Command ==========

    /**
     * 更新当前用户信息
     */
    @Transactional
    public void updateCurrentUser(Long userId, String name, String biography, String timezone) {
        validateUsername(name);

        // 委托给 DomainService
        userDomainService.updateUserInfo(userId, name, biography, timezone);
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
        String code = generateVerificationCode();
        userDomainService.createVerificationCode(email, code);

        // 异步发送邮件，失败不影响注册
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

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
    @Transactional
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
        String language = DataSourceContextHolder.getLanguage();
        emailService.sendVerificationEmailAsync(email, code, language);

        log.info("重新发送验证码成功: {}", email);
    }

    /**
     * 生成验证码
     * 使用 SecureRandom 确保密码学安全
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        // 生成 6 位数字验证码 (100000 ~ 999999)
        int code = 100000 + random.nextInt(900000);
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
     * 批量获取用户简要信息 Map
     * 用途：批量填充 creator 字段
     */
    public Map<Long, UserBriefDTO> getUserBriefMapByIds(Set<Long> ids) {
        List<UserDO> userDOList = userDataService.getByIds(new ArrayList<>(ids));
        List<UserBriefDTO> dtoList = userConverter.toBriefDTO(userDOList);
        Map<Long, UserBriefDTO> result = new HashMap<>();
        for (int i = 0; i < userDOList.size(); i++) {
            result.put(userDOList.get(i).getId(), dtoList.get(i));
        }
        return result;
    }

    /**
     * 获取单个用户简要信息
     */
    public UserBriefDTO getUserBriefById(Long id) {
        if (id == null) return null;
        UserDO userDO = userDataService.getById(id);
        if (userDO == null) return null;
        return userConverter.toBriefDTO(userDO);
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
        return dto;
    }


    // ========== 私有辅助方法 ==========

    /**
     * 批量填充用户统计数据
     */
    private void fillUserStats(List<UserAdminDTO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> userIds = items.stream()
                .map(UserAdminDTO::getId)
                .collect(Collectors.toList());

        Map<Long, UserStatsDO> statsMap = userStatsDataService.batchGetByUserIds(userIds);

        for (UserAdminDTO dto : items) {
            UserStatsDO stats = statsMap.get(dto.getId());
            if (stats != null) {
                dto.setViewCount(stats.getViewCount());
                dto.setTwiceCount(stats.getTwiceCount());
                dto.setLikeCount(stats.getLikeCount());
                dto.setCommentCount(stats.getCommentCount());
                dto.setLearningCourseCount(stats.getLearningCourseCount());
                dto.setCompletedCourseCount(stats.getCompletedCourseCount());
                dto.setInProgressRoleCount(stats.getInProgressRoleCount());
                dto.setCompletedRoleCount(stats.getCompletedRoleCount());
                dto.setFollowingUserCount(stats.getFollowingUserCount());
                dto.setFollowingCourseCount(stats.getFollowingCourseCount());
                dto.setFollowingRoleCount(stats.getFollowingRoleCount());
                dto.setCreatedArticleCount(stats.getCreatedArticleCount());
                dto.setCreatedIndexCount(stats.getCreatedIndexCount());
                dto.setCreatedRoadmapCount(stats.getCreatedRoadmapCount());
                dto.setCreatedCardDeckCount(stats.getCreatedCardDeckCount());
            }
        }
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
        var validation = systemProperties.getValidation();
        if (username.length() < validation.getUsernameMinLength()
                || username.length() > validation.getUsernameMaxLength()) {
            throw StatusCode.USER_INVALID_USERNAME_LENGTH.exception();
        }
    }

    private void validatePassword(String password) {
        // 长度检查
        var validation = systemProperties.getValidation();
        if (password == null
                || password.length() < validation.getPasswordMinLength()
                || password.length() > validation.getPasswordMaxLength()) {
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
    private void fillStatsAndProgressForCourses(List<CourseFullDTO> dtoList, Long userId) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        // 收集所有课程ID
        List<Long> courseIds = dtoList.stream()
                .map(CourseFullDTO::getId)
                .toList();

        // 批量查询课程实体（获取 rootNodeId）
        List<CourseDO> courseDOList = courseDataService.getByIds(courseIds);

        // 批量查询统计数据
        List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(ContentType.course, courseIds);
        Map<Long, ContentStatsDO> statsMap = statsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats, (a, b) -> a));

        // 批量查询学习进度
        // 1. 提取所有课程的 rootNodeIds
        Map<Long, Long> courseToRootNodeMap = courseDOList.stream()
            .filter(c -> c.getRootNodeId() != null)
            .collect(Collectors.toMap(CourseDO::getId, CourseDO::getRootNodeId));

        List<Long> rootNodeIds = new ArrayList<>(courseToRootNodeMap.values());

        // 2. 批量查询 node 类型的学习记录
        Map<Long, UserLearningDO> nodeProgressMap = userLearningDomainService.getBatch(
            userId,
            ContentType.node,
            rootNodeIds
        );

        // 3. 转换为 courseId → progress 映射
        Map<Long, Integer> progressMap = new HashMap<>();
        for (Map.Entry<Long, Long> entry : courseToRootNodeMap.entrySet()) {
            Long courseId = entry.getKey();
            Long rootNodeId = entry.getValue();
            UserLearningDO learning = nodeProgressMap.get(rootNodeId);
            progressMap.put(courseId, learning != null && learning.getProgressPercent() != null
                ? learning.getProgressPercent() : 0);
        }

        // 填充每个课程的统计字段和用户字段
        for (CourseFullDTO dto : dtoList) {
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
}