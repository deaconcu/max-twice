package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserClient;
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

import static com.prosper.learn.dto.Response.*;

@RestController
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
    public Response getSelf() {
        int id = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);
        return new Response(userDO);
    }

    @Override
    public Response modifySelf(String name, String biography) {
        int id = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);
        userDO.setName(name);
        userDO.setBiography(biography);
        userMapper.update(userDO);
        return success;
    }

    @Override
    public Response getUser(int id) {
        int selfId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(id);

        UserDTOV3 userDTOV3 = Converter.INSTANCE.toUserDTOV3(userDO);
        if (userDO == null) return notFound;

        FollowDO followDO = followMapper.get(selfId, id);
        if (followDO != null) {
            userDTOV3.setFollowed(1);
        }
        return new Response(userDTOV3);
    }

    @Override
    public Response<List<UserDTOV4>> getUserByName(String name) {
        List<UserDO> userDOList = userMapper.searchByName(name);
        return new Response<>(Converter.INSTANCE.toUserDTOV4(userDOList));
    }

    @Override
    public Response<UserDTOV2> login(String email, String password) {
        UserDO userDO = userMapper.getByEmail(email);
        if (userDO == null) return new Response(USER_NOT_EXIST);
        //if (!userDO.getPassword().equals(Utils.md5(password))) return new Response(PASSWORD_IS_WRONG);
        StpUtil.login(userDO.getId());
        UserDTOV2 userDTOV2 = Converter.INSTANCE.toUserDTOV2(userDO);

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        
        if (userProfileDO != null && userProfileDO.getSubscription() != null && !userProfileDO.getSubscription().trim().isEmpty()) {
            List<Integer> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Integer::parseInt).collect(Collectors.toCollection(ArrayList::new));
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
        user.setCTime(Utils.getLocalDateTime());
        user.setUTime(Utils.getLocalDateTime());
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
        if (verificationDO == null) return new Response(BAD_REQUEST);
        if (!verificationDO.getCode().equals(code)) return new Response(BAD_REQUEST);

        verificationDO.setUsed(true);
        verificationMapper.update(verificationDO);

        UserDO user = userMapper.getByEmail(email);
        if (user == null) return new Response(BAD_REQUEST);
        if (user.isEmailValidated()) return new Response<>(SUCCESS);

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
    public Response getSelfArticle(int userId, int lastId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) return new Response(USER_NOT_EXIST);
        return new Response(postingService.getUserArticleWithViews(userDO.getId(), lastId));
    }

    @Override
    public Response getSelfContents(int userId, int lastId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) return new Response(USER_NOT_EXIST);
        return new Response(postingService.getUserContentsWithViews(userDO.getId(), lastId));
    }

    @Override
    public Response getSubscription(int userId) {
        //int self = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) return new Response(USER_NOT_EXIST);

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return new Response(new ArrayList<>());
        }
        
        try {
            List<Integer> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .filter(s -> !s.trim().isEmpty()) // 过滤空字符串
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .boxed()
                    .toList();

            if (ids.isEmpty()) {
                return new Response(new ArrayList<>());
            }

            List<CourseDO> courseDOList = courseMapper.getByIds(ids);
            log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                    courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
            return new Response(Converter.INSTANCE.toCourseDTOV2(courseDOList));
        } catch (Exception e) {
            log.error("获取用户{}收藏课程时出错: {}", userId, e.getMessage());
            return new Response(new ArrayList<>());
        }
    }

    @Override
    public Response subscript(int courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) return new Response(BAD_REQUEST);

        int self = StpUtil.getLoginIdAsInt();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        String idsStr = "";
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO();
            userProfileDO.setId(self);
            idsStr = String.valueOf(courseId);
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.insert(userProfileDO);
        } else {
            List<Integer> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Integer::parseInt).collect(Collectors.toCollection(ArrayList::new));
            if (ids.contains(courseDO.getId())) return success;
            ids.add(courseDO.getId());
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }

        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return new Response(idsArr);
    }

    @Override
    public Response subscript(String subscription) {
        int self = StpUtil.getLoginIdAsInt();

        List<Integer> ids = new ArrayList<>();
        String[] parts = subscription.split(",");
        for (String part : parts) {
            try {
                ids.add(Integer.parseInt(part));
            } catch (Exception e) {
            }
        }
        List<CourseDO> courseDOList = courseMapper.getByIds(ids);

        int[] idsArr = new int[courseDOList.size()];
        for (int i = 0; i < courseDOList.size(); i++) {
            idsArr[i] = courseDOList.get(i).getId();
            //idsStr = idsStr + courseDOList.get(i).getId();
            //if (i < courseDOList.size() - 1) idsStr = idsStr + ",";
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
        return new Response(ids);
    }

    @Override
    public Response unsubscript(int courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) return new Response(BAD_REQUEST);

        int self = StpUtil.getLoginIdAsInt();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        List<Integer> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(Integer::parseInt).collect(Collectors.toCollection(ArrayList::new));
        if (!ids.contains(courseDO.getId())) return success;
        ids.remove(Integer.valueOf(courseDO.getId()));

        String idsStr = "";
        if (ids.size() != 0) {
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        userProfileDO.setSubscription(idsStr);
        userProfileMapper.update(userProfileDO);
        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return new Response(idsArr);
    }

    @Override
    public Response follow(int followeeId) {
        int followerId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(followeeId);
        if (userDO == null) return new Response(BAD_REQUEST);

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO == null) {
            UserDO follower = userMapper.getById(followerId);
            followMapper.insert(followerId, followeeId);

            messageService.createFollowMessage(followeeId, follower.getId());
            //String content = "{\"id\":" + follower.getId() + ", \"name\": \"" + follower.getName() + "\"}";
            //messageService.createSystemMessage(Enums.MessageType.follow.value, followeeId, content);
        }
        return success;
    }

    @Override
    public Response unfollow(int followeeId) {
        int followerId = StpUtil.getLoginIdAsInt();
        UserDO  userDO = userMapper.getById(followeeId);
        if (userDO == null) return new Response(BAD_REQUEST);

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO != null) {
            followMapper.delete(followerId, followeeId);
        }
        return success;
    }

    @Override
    public Response followee(int followerId, String lastCreateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.parse(lastCreateTime, formatter);

        UserDO followerDO = userMapper.getById(followerId);
        if (followerDO == null) return new Response(userNotExist);

        //int followerId = StpUtil.getLoginIdAsInt();
        int pageSize = 10;
        List<FollowDO> followDOList = followMapper.getList(followerId, time, pageSize);

        // get all user
        List<Integer> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        if (userIds.size() == 0) {
            return new Response(new LinkedList<>());
        }

        List<UserDO> userDOList = userMapper.getByIds(userIds);
        Map<Integer, UserDO> userDOMap = new HashMap<>();
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
            followeeDTO.setCreateTime(Utils.getTimeString(followDO.getCreateTime()));
            followeeDTOList.add(followeeDTO);
        }

        return new Response(followeeDTOList);
    }

    @Override
    public Response markNodeCompleted(Integer nodeId, Integer courseId) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            
            boolean isNewlyCompleted = learningProgressService.markNodeCompleted(userId, nodeId, courseId);
            
            // 获取更新后的课程进度
            UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId((long)userId, (long)courseId);
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

        } catch (Exception e) {
            log.error("Error marking node {} as completed: {}", nodeId, e.getMessage(), e);
            return Response.failed;
        }
    }

    @Override
    public Response unmarkNodeCompleted(Integer nodeId, Integer courseId) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            
            boolean wasRemoved = learningProgressService.unmarkNodeCompleted(userId, nodeId, courseId);
            
            // 获取更新后的课程进度
            UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId((long)userId, (long)courseId);
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

        } catch (Exception e) {
            log.error("Error unmarking node {} as completed: {}", nodeId, e.getMessage(), e);
            return Response.failed;
        }
    }

    @Override
    public Response isNodeCompleted(Integer nodeId) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            
            boolean isCompleted = learningProgressService.isNodeCompleted(userId, nodeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("nodeId", nodeId);
            result.put("completed", isCompleted);

            return Response.success(result);

        } catch (Exception e) {
            log.error("Error checking if node {} is completed: {}", nodeId, e.getMessage(), e);
            return Response.failed;
        }
    }

    @Override
    public Response markCourseCompleted(Integer courseId) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            
            boolean result = learningProgressService.markCourseCompleted(userId, courseId);
            
            if (result) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("courseId", courseId);
                responseData.put("completed", true);
                responseData.put("message", "课程已标记为完成");
                
                return Response.success(responseData);
            } else {
                return Response.failed;
            }

        } catch (Exception e) {
            log.error("Error marking course {} as completed: {}", courseId, e.getMessage(), e);
            return Response.failed;
        }
    }
}
