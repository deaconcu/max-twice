package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserClient;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.service.business.PostingService;
import com.prosper.learn.domain.service.business.LearningProgressService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.common.Utils;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.dto.response.Response.*;

//@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController implements UserClient {

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

    @Override
    public Response<UserDTO> getSelf() {
        long id = StpUtil.getLoginIdAsLong();
        UserDO userDO = userMapper.getById(id);
        return Response.success(Converter.INSTANCE.toUserDTO(userDO));
    }

    @Override
    public Response<Object> modifySelf(String name, String biography) {
        long id = StpUtil.getLoginIdAsLong();
        UserDO userDO = userMapper.getById(id);
        userDO.setName(name);
        userDO.setBiography(biography);
        userMapper.update(userDO);
        return success;
    }

    @Override
    public Response<Object> getUser(Long id) {
        long selfId = StpUtil.getLoginIdAsLong();
        UserDO userDO = userMapper.getById(id);

        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserDTOV3 userDTOV3 = Converter.INSTANCE.toUserDTOV3(userDO);

        FollowDO followDO = followMapper.get(selfId, id);
        if (followDO != null) {
            userDTOV3.setFollowed(1);
        }
        return Response.success(userDTOV3);
    }

    @Override
    public Response<List<UserDTOV4>> getUserByName(String name) {
        List<UserDO> userDOList = userMapper.searchByName(name);
        return new Response<>(Converter.INSTANCE.toUserDTOV4(userDOList));
    }

    @Override
    public Response<UserDTOV2> login(String email, String password) {
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
        
        return new Response<>(userDTOV2);
    }

    @Override
    public Response<Object> register(String userName, String email, String password) {
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
        return success;
    }

    @Override
    public Response<UserDTO> validateMail(String email, String code) {
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
        if (user.getEmailValidated()) return new Response<>(SUCCESS);

        user.setEmailValidated(true);
        StpUtil.login(user.getId());
        userMapper.update(user);
        return new Response<>(Converter.INSTANCE.toUserDTO(user));
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

    @Override
    public Response<Object> getSelfArticle(Long userId, Long lastId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        return Response.success(postingService.getUserArticleWithViews(userDO.getId(), lastId));
    }

    @Override
    public Response<Object> getSelfContents(Long userId, Long lastId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        return Response.success(postingService.getUserContentsWithViews(userDO.getId(), lastId));
    }

    @Override
    public Response<Object> getSubscription(Long userId) {
        //int self = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return Response.success(new ArrayList<>());
        }
        
        try {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(String::trim) // 过滤空字符串
                    .filter(trim -> !trim.isEmpty())
                    .mapToLong(Long::parseLong)
                    .boxed()
                    .toList();

            if (ids.isEmpty()) {
                return Response.success(new ArrayList<>());
            }

            List<CourseDO> courseDOList = courseMapper.getByIds(ids);
            log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                    courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
            return Response.success(Converter.INSTANCE.toCourseDTOV2(courseDOList));
        } catch (Exception e) {
            log.error("获取用户{}收藏课程时出错: {}", userId, e.getMessage());
            return Response.success(new ArrayList<>());
        }
    }

    @Override
    public Response<Object> subscript(Long courseId) {
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
            if (ids.contains(courseDO.getId())) return success;
            ids.add(courseDO.getId());
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }

        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return Response.success(idsArr);
    }

    @Override
    public Response<Object> subscript(String subscription) {
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
        return Response.success(ids);
    }

    @Override
    public Response<Object> unsubscript(Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int self = StpUtil.getLoginIdAsInt();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
        if (!ids.contains(courseDO.getId())) return success;
        ids.remove(courseDO.getId());

        String idsStr = "";
        if (!ids.isEmpty()) {
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        userProfileDO.setSubscription(idsStr);
        userProfileMapper.update(userProfileDO);
        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return Response.success(idsArr);
    }

    @Override
    public Response<Object> follow(Long followeeId) {
        long followerId = StpUtil.getLoginIdAsLong();
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
        return success;
    }

    @Override
    public Response<Object> unfollow(Long followeeId) {
        long followerId = StpUtil.getLoginIdAsLong();
        UserDO  userDO = userMapper.getById(followeeId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO != null) {
            followMapper.delete(followerId, followeeId);
        }
        return success;
    }

    @Override
    public Response<Object> followee(Long followerId, String lastCreateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.parse(lastCreateTime, formatter);

        UserDO followerDO = userMapper.getById(followerId);
        if (followerDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        //int followerId = StpUtil.getLoginIdAsInt();
        int pageSize = 10;
        List<FollowDO> followDOList = followMapper.getList(followerId, time, pageSize);

        // get all user
        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        if (userIds.isEmpty()) {
            return Response.success(new LinkedList<>());
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

        return Response.success(followeeDTOList);
    }

    @Override
    public Response<Object> markNodeCompleted(Long nodeId, Long courseId) {
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

        return Response.success(result);
    }

    @Override
    public Response<Object> unmarkNodeCompleted(Long nodeId, Long courseId) {
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

        return Response.success(result);
    }

    @Override
    public Response<Object> isNodeCompleted(Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean isCompleted = learningProgressService.isNodeCompleted(userId, nodeId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", isCompleted);

        return Response.success(result);
    }

    @Override
    public Response<Object> markCourseCompleted(Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean result = learningProgressService.markCourseCompleted(userId, courseId);
        
        if (result) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("courseId", courseId);
            responseData.put("completed", true);
            responseData.put("message", "课程已标记为完成");
            
            return Response.success(responseData);
        } else {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
    }
}
