package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.*;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.persistence.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户业务服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final CourseMapper courseMapper;
    private final FollowMapper followMapper;
    private final VerificationMapper verificationMapper;
    private final JavaMailSender mailSender;
    private final PostingService postingService;
    private final MessageService messageService;

    /**
     * 获取当前用户信息
     */
    public UserDTO getCurrentUser(Long userId) {
        UserDO userDO = userMapper.getById(userId);
        return Converter.INSTANCE.toUserDTO(userDO);
    }

    /**
     * 更新当前用户信息
     */
    public void updateCurrentUser(Long userId, String name, String biography) {
        UserDO userDO = userMapper.getById(userId);
        userDO.setName(name);
        userDO.setBiography(biography);
        userMapper.update(userDO);
    }

    /**
     * 获取用户信息（包含关注状态）
     */
    public UserDTOV3 getUser(Long userId, Long viewerId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserDTOV3 userDTOV3 = Converter.INSTANCE.toUserDTOV3(userDO);

        // 检查是否已关注
        FollowDO followDO = followMapper.get(viewerId, userId);
        if (followDO != null) {
            userDTOV3.setFollowed(1);
        }
        return userDTOV3;
    }

    /**
     * 搜索用户
     */
    public List<UserDTOV4> searchUsers(String name) {
        List<UserDO> userDOList = userMapper.searchByName(name);
        return Converter.INSTANCE.toUserDTOV4(userDOList);
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(String userName, String email, String password) {
        UserDO user = new UserDO();
        user.setName(userName);
        user.setEmail(email);
        user.setPassword(Utils.md5(password));
        user.setCreatedAt(Utils.getLocalDateTime());
        user.setUpdatedAt(Utils.getLocalDateTime());
        userMapper.insert(user);

        // 生成验证码
        String code = generateVerificationCode();
        VerificationDO verification = new VerificationDO(email, code);
        verificationMapper.insert(verification);

        // 发送验证邮件
        sendVerificationEmail(email, code);
    }

    /**
     * 用户登录验证
     * 只做业务验证，不操作认证状态
     */
    public UserDTOV2 validateLogin(String email, String password) {
        UserDO userDO = userMapper.getByEmail(email);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        
        // TODO: 密码验证
        // if (!userDO.getPassword().equals(Utils.md5(password))) throw ErrorCode.PASSWORD_WRONG.exception();
        
        UserDTOV2 userDTOV2 = Converter.INSTANCE.toUserDTOV2(userDO);

        // 获取用户订阅信息
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
        
        return userDTOV2;
    }

    /**
     * 邮箱验证
     * 只做验证逻辑，不操作认证状态
     */
    @Transactional
    public UserDTO validateEmail(String email, String code) {
        // 验证验证码
        VerificationDO verificationDO = verificationMapper.getByEmail(email, false);
        if (verificationDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (!verificationDO.getCode().equals(code)) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        // 标记验证码已使用
        verificationDO.setUsed(true);
        verificationMapper.update(verificationDO);

        // 激活用户邮箱
        UserDO user = userMapper.getByEmail(email);
        if (user == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        
        if (user.getEmailValidated()) {
            return Converter.INSTANCE.toUserDTO(user);
        }

        user.setEmailValidated(true);
        userMapper.update(user);
        return Converter.INSTANCE.toUserDTO(user);
    }

    /**
     * 获取用户文章或内容
     */
    public Object getUserPosts(Long userId, Long lastId, String type) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        
        if ("content".equals(type)) {
            return postingService.getUserContentsWithViews(userDO.getId(), lastId);
        } else {
            return postingService.getUserArticleWithViews(userDO.getId(), lastId);
        }
    }

    /**
     * 获取用户订阅
     */
    public Object getUserSubscriptions(Long userId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(String::trim)
                    .filter(trim -> !trim.isEmpty())
                    .mapToLong(Long::parseLong)
                    .boxed()
                    .toList();

            if (ids.isEmpty()) {
                return new ArrayList<>();
            }

            List<CourseDO> courseDOList = courseMapper.getByIds(ids);
            log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                    courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
            return Converter.INSTANCE.toCourseDTOV2(courseDOList);
        } catch (Exception e) {
            log.error("获取用户{}收藏课程时出错: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 添加订阅
     */
    @Transactional
    public Object subscribe(Long userId, Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userId);
        String idsStr;
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO();
            userProfileDO.setUserId(userId);
            idsStr = String.valueOf(courseId);
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.insert(userProfileDO);
        } else {
            List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                    .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
            if (ids.contains(courseDO.getId())) {
                return new int[0];
            }
            ids.add(courseDO.getId());
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }

        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return idsArr;
    }

    /**
     * 批量更新订阅
     */
    @Transactional
    public Object updateSubscriptions(Long userId, String subscription) {
        List<Long> ids = new ArrayList<>();
        String[] parts = subscription.split(",");
        for (String part : parts) {
            try {
                ids.add(Long.parseLong(part));
            } catch (Exception e) {
                log.error("解析订阅ID失败", e);
            }
        }
        
        List<CourseDO> courseDOList = courseMapper.getByIds(ids);
        long[] idsArr = new long[courseDOList.size()];
        for (int i = 0; i < courseDOList.size(); i++) {
            idsArr[i] = courseDOList.get(i).getId();
        }
        
        String idsStr = Arrays.stream(idsArr).mapToObj(String::valueOf).collect(Collectors.joining(","));
        UserProfileDO userProfileDO = userProfileMapper.getById(userId);
        if (userProfileDO == null) {
            userProfileDO = new UserProfileDO(userId, idsStr);
            userProfileMapper.insert(userProfileDO);
        } else {
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }
        return ids;
    }

    /**
     * 取消订阅
     */
    @Transactional
    public Object unsubscribe(Long userId, Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userId);
        List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
        if (!ids.contains(courseDO.getId())) {
            return new int[0];
        }
        ids.remove(courseDO.getId());

        String idsStr = "";
        if (!ids.isEmpty()) {
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        userProfileDO.setSubscription(idsStr);
        userProfileMapper.update(userProfileDO);
        
        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return idsArr;
    }

    /**
     * 关注用户
     */
    @Transactional
    public void follow(Long followerId, Long followeeId) {
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
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        UserDO userDO = userMapper.getById(followeeId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO != null) {
            followMapper.delete(followerId, followeeId);
        }
    }

    /**
     * 获取关注列表
     */
    public Object getFollowees(Long userId, String lastCreateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime time = LocalDateTime.parse(lastCreateTime, formatter);

        UserDO followerDO = userMapper.getById(userId);
        if (followerDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int pageSize = 10;
        List<FollowDO> followDOList = followMapper.getList(userId, time, pageSize);

        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        if (userIds.isEmpty()) {
            return new LinkedList<>();
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

        return followeeDTOList;
    }

    /**
     * 发送验证邮件
     */
    private void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("deaconcc@126.com");
        message.setTo(toEmail);
        message.setSubject("Your Verification Code");
        message.setText("Your verification code is: " + code);
        mailSender.send(message);
    }

    /**
     * 生成验证码
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}