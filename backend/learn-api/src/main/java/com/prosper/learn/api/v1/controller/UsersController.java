package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.api.util.MessageUtils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.domain.service.PostingService;
import com.prosper.learn.domain.service.LearningProgressService;
import com.prosper.learn.dto.*;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.common.Utils;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class UsersController {

    private final MessageUtils messageUtils;
    private final UserProfileMapper userProfileMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final FollowMapper followMapper;
    private final VerificationMapper verificationMapper;
    private final JavaMailSender mailSender;
    private final PostingService postingService;
    private final MessageService messageService;
    private final LearningProgressService learningProgressService;
    private final UserCourseMapper userCourseMapper;

    /**
     * 获取当前用户信息
     * 映射: GET /self → GET /api/v1/users/current
     */
    @GetMapping("/users/current")
    public ApiResponse<UserDTO> getCurrentUser() {
        int id = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);
        return ApiResponse.success(Converter.INSTANCE.toUserDTO(userDO));
    }

    /**
     * 修改当前用户信息
     * 映射: POST /self → PUT /api/v1/users/current
     */
    @PutMapping("/users/current")
    public ApiResponse<Void> updateCurrentUser(@RequestParam String name, @RequestParam String biography) {
        int id = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);
        userDO.setName(name);
        userDO.setBiography(biography);
        userMapper.update(userDO);
        return ApiResponse.success();
    }

    /**
     * 获取用户信息
     * 映射: GET /user/{id} → GET /api/v1/users/{id}
     */
    @GetMapping("/users/{id}")
    public ApiResponse<UserDTOV3> getUser(@PathVariable Long id) {
        int selfId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);

        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserDTOV3 userDTOV3 = Converter.INSTANCE.toUserDTOV3(userDO);

        FollowDO followDO = followMapper.get(selfId, id);
        if (followDO != null) {
            userDTOV3.setFollowed(1);
        }
        return ApiResponse.success(userDTOV3);
    }

    /**
     * 搜索用户
     * 映射: GET /user?name=xxx → GET /api/v1/users/search?name=xxx
     */
    @GetMapping("/users/search")
    public ApiResponse<List<UserDTOV4>> searchUsers(@RequestParam String name) {
        List<UserDO> userDOList = userMapper.searchByName(name);
        return ApiResponse.success(Converter.INSTANCE.toUserDTOV4(userDOList));
    }

    /**
     * 用户注册
     * 映射: POST /user → POST /api/v1/auth/register
     */
    @PostMapping("/auth/register")
    public ApiResponse<Void> register(@RequestParam String userName, @RequestParam String email, @RequestParam String password) {
        UserDO user = new UserDO();
        user.setName(userName);
        user.setEmail(email);
        user.setPassword(Utils.md5(password));
        user.setCreatedAt(Utils.getLocalDateTime());
        user.setUpdatedAt(Utils.getLocalDateTime());
        userMapper.insert(user);

        String code = generateVerificationCode();
        VerificationDO verification = new VerificationDO(email, code);
        verificationMapper.insert(verification);

        sendVerificationEmail(email, code);
        return ApiResponse.success();
    }

    /**
     * 用户登录
     * 映射: POST /login → POST /api/v1/auth/login
     */
    @PostMapping("/auth/login")
    public ApiResponse<UserDTOV2> login(@RequestParam String email, @RequestParam String password) {
        UserDO userDO = userMapper.getByEmail(email);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        //if (!userDO.getPassword().equals(Utils.md5(password))) return new Response(PASSWORD_IS_WRONG);
        StpUtil.login(userDO.getId());
        UserDTOV2 userDTOV2 = Converter.INSTANCE.toUserDTOV2(userDO);

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        
        if (userProfileDO != null && userProfileDO.getSubscription() != null && !userProfileDO.getSubscription().trim().isEmpty()) {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
            List<CourseDO> courseDOList = courseMapper.getByIds(ids);
            SubscriptionDTO[] subscriptionDTOS = new SubscriptionDTO[courseDOList.size()];
            int i = 0;
            for (CourseDO courseDO : courseDOList) {
                subscriptionDTOS[i++] = new SubscriptionDTO(courseDO.getId(), courseDO.getName());
            }
            userDTOV2.setSubscriptions(subscriptionDTOS);
        } else {
            userDTOV2.setSubscriptions(new SubscriptionDTO[0]);
        }
        
        return ApiResponse.success(userDTOV2);
    }

    /**
     * 邮箱验证
     * 映射: POST /user/validate → POST /api/v1/auth/validate-email
     */
    @PostMapping("/auth/validate-email")
    public ApiResponse<UserDTO> validateEmail(@RequestParam String email, @RequestParam String code) {
        // todo 5min
        VerificationDO verificationDO = verificationMapper.getByEmail(email, false);
        if (verificationDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (!verificationDO.getCode().equals(code)) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        verificationDO.setUsed(true);
        verificationMapper.update(verificationDO);

        UserDO user = userMapper.getByEmail(email);
        if (user == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (user.getEmailValidated()) return ApiResponse.success(Converter.INSTANCE.toUserDTO(user));

        user.setEmailValidated(true);
        StpUtil.login(user.getId());
        userMapper.update(user);
        return ApiResponse.success(Converter.INSTANCE.toUserDTO(user));
    }

    /**
     * 获取用户文章或内容
     * 映射: GET /user/article → GET /api/v1/users/{userId}/posts?type=article
     * 映射: GET /user/contents → GET /api/v1/users/{userId}/posts?type=content
     */
    @GetMapping("/users/{userId}/posts")
    public ApiResponse<Object> getUserPosts(
            @PathVariable Long userId, 
            @RequestParam Long lastId,
            @RequestParam(required = false, defaultValue = "article") String type) {
        
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        
        if ("content".equals(type)) {
            return ApiResponse.success(postingService.getUserContentsWithViews(userDO.getId(), lastId));
        } else {
            // 默认返回文章
            return ApiResponse.success(postingService.getUserArticleWithViews(userDO.getId(), lastId));
        }
    }

    /**
     * 获取用户订阅
     * 映射: GET /user/subscription → GET /api/v1/users/{userId}/subscriptions
     */
    @GetMapping("/users/{userId}/subscriptions")
    public ApiResponse<Object> getUserSubscriptions(@PathVariable Long userId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return ApiResponse.success(new ArrayList<>());
        }
        
        try {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(String::trim) // 过滤空字符串
                    .filter(trim -> !trim.isEmpty())
                    .mapToLong(Long::parseLong)
                    .boxed()
                    .toList();

            if (ids.isEmpty()) {
                return ApiResponse.success(new ArrayList<>());
            }

            List<CourseDO> courseDOList = courseMapper.getByIds(ids);
            log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                    courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
            return ApiResponse.success(Converter.INSTANCE.toCourseDTOV2(courseDOList));
        } catch (Exception e) {
            log.error("获取用户{}收藏课程时出错: {}", userId, e.getMessage());
            return ApiResponse.success(new ArrayList<>());
        }
    }

    /**
     * 添加订阅
     * 映射: POST /user/subscription → POST /api/v1/users/current/subscriptions
     */
    @PostMapping("/users/current/subscriptions")
    public ApiResponse<Object> subscribe(@RequestParam Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        long self = StpUtil.getLoginIdAsLong();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        String idsStr;
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(self);
            idsStr = String.valueOf(courseId);
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.insert(userProfileDO);
        } else {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
            if (ids.contains(courseDO.getId())) return ApiResponse.success();
            ids.add(courseDO.getId());
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }

        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return ApiResponse.success(idsArr);
    }

    /**
     * 批量更新订阅
     * 映射: PUT /user/subscription → PUT /api/v1/users/current/subscriptions
     */
    @PutMapping("/users/current/subscriptions")
    public ApiResponse<Object> updateSubscriptions(@RequestParam String subscription) {
        long self = StpUtil.getLoginIdAsLong();

        List<Long> ids = new ArrayList<>();
        String[] parts = subscription.split(",");
        for (String part : parts) {
            try {
                ids.add(Long.parseLong(part));
            } catch (Exception e) {
                log.error("error", e);
            }
        }
        List<CourseDO> courseDOList = courseMapper.getByIds(ids);

        long[] idsArr = new long[courseDOList.size()];
        for (int i = 0; i < courseDOList.size(); i++) {
            idsArr[i] = courseDOList.get(i).getId();
        }
        String idsStr = Arrays.stream(idsArr).mapToObj(String::valueOf).collect(Collectors.joining(","));
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO(self, idsStr);
            userProfileMapper.insert(userProfileDO);
        } else {
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }
        return ApiResponse.success(ids);
    }

    /**
     * 取消订阅
     * 映射: DELETE /user/subscription → DELETE /api/v1/users/current/subscriptions/{courseId}
     */
    @DeleteMapping("/users/current/subscriptions/{courseId}")
    public ApiResponse<Object> unsubscribe(@PathVariable Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int self = StpUtil.getLoginIdAsInt();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
        if (!ids.contains(courseDO.getId())) return ApiResponse.success();
        ids.remove(courseDO.getId());

        String idsStr = "";
        if (!ids.isEmpty()) {
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        userProfileDO.setSubscription(idsStr);
        userProfileMapper.update(userProfileDO);
        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return ApiResponse.success(idsArr);
    }

    /**
     * 关注用户
     * 映射: POST /user/follow → POST /api/v1/follows
     */
    @PostMapping("/follows")
    public ApiResponse<Void> follow(@RequestParam Long followeeId) {
        int followerId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(followeeId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO == null) {
            UserDO follower = userMapper.getById(followerId);
            followMapper.insert(followerId, followeeId);

            messageService.createFollowMessage(followeeId, follower.getId());
        }
        return ApiResponse.success();
    }

    /**
     * 取消关注
     * 映射: DELETE /user/follow → DELETE /api/v1/follows/{followeeId}
     */
    @DeleteMapping("/follows/{followeeId}")
    public ApiResponse<Void> unfollow(@PathVariable Long followeeId) {
        int followerId = StpUtil.getLoginIdAsInt();
        UserDO  userDO = userMapper.getById(followeeId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO != null) {
            followMapper.delete(followerId, followeeId);
        }
        return ApiResponse.success();
    }

    /**
     * 获取关注列表
     * 映射: GET /user/followee → GET /api/v1/users/{userId}/followees
     */
    @GetMapping("/users/{userId}/followees")
    public ApiResponse<Object> getFollowees(@PathVariable Long userId, @RequestParam String lastCreateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.parse(lastCreateTime, formatter);

        UserDO followerDO = userMapper.getById(userId);
        if (followerDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int pageSize = 10;
        List<FollowDO> followDOList = followMapper.getList(userId, time, pageSize);

        // get all user
        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        if (userIds.isEmpty()) {
            return ApiResponse.success(new LinkedList<>());
        }

        List<UserDO> userDOList = userMapper.getByIds(userIds);
        Map<Long, UserDO> userDOMap = new HashMap<>();
        for (UserDO userDO : userDOList) {
            userDOMap.put(userDO.getId(), userDO);
        }

        List<FolloweeDTO> followeeDTOList = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            FolloweeDTO followeeDTO = new FolloweeDTO();
            followeeDTO.setId(followDO.getFolloweeId());
            UserDO userDO = userDOMap.get(followDO.getFolloweeId());
            followeeDTO.setName(userDO.getName());
            followeeDTO.setBiography(userDO.getBiography());
            followeeDTO.setCreatedAt(Utils.getTimeString(followDO.getCreatedAt()));
            followeeDTOList.add(followeeDTO);
        }

        return ApiResponse.success(followeeDTOList);
    }

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<Object> markNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean isNewlyCompleted = learningProgressService.markNodeCompleted(userId, nodeId, courseId);
        
        // 获取更新后的课程进度
        UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", true);
        result.put("isNewlyCompleted", isNewlyCompleted);
        result.put("courseProgress", courseProgress);
        
        // 返回用户最新的完成统计
        long totalCompleted = learningProgressService.getUserCompletedCount(userId);
        result.put("totalCompletedNodes", totalCompleted);

        return ApiResponse.success(result);
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<Object> unmarkNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean wasRemoved = learningProgressService.unmarkNodeCompleted(userId, nodeId, courseId);
        
        // 获取更新后的课程进度
        UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", false);
        result.put("wasRemoved", wasRemoved);
        result.put("courseProgress", courseProgress);
        
        // 返回用户最新的完成统计
        long totalCompleted = learningProgressService.getUserCompletedCount(userId);
        result.put("totalCompletedNodes", totalCompleted);

        return ApiResponse.success(result);
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    public ApiResponse<Object> getNodeCompletionStatus(@PathVariable Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean isCompleted = learningProgressService.isNodeCompleted(userId, nodeId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", isCompleted);

        return ApiResponse.success(result);
    }

    /**
     * 标记课程完成
     * 映射: POST /user/complete/course/{courseId} → POST /api/v1/progress/courses/{courseId}/complete
     */
    @PostMapping("/progress/courses/{courseId}/complete")
    public ApiResponse<Object> markCourseCompleted(@PathVariable Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean result = learningProgressService.markCourseCompleted(userId, courseId);
        
        if (result) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("courseId", courseId);
            responseData.put("completed", true);
            responseData.put("message", "课程已标记为完成");
            
            return ApiResponse.success(responseData);
        } else {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
    }

    private void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("deaconcc@126.com");
        message.setTo(toEmail);
        message.setSubject("Your Verification Code");
        message.setText("Your verification code is: " + code);
        mailSender.send(message);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 生成6位数字
        return String.valueOf(code);
    }
}