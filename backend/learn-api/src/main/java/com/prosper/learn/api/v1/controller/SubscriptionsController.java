package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订阅管理接口
 * 从UsersController拆分出来的订阅功能
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class SubscriptionsController {

    private final UserProfileMapper userProfileMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    /**
     * 获取用户订阅
     * 映射: GET /user/subscription → GET /api/v1/users/{userId}/subscriptions
     */
    @GetMapping("/users/{userId}/subscriptions")
    public ResponseEntity<ApiResponse<Object>> getUserSubscriptions(@PathVariable Long userId) {
        UserDO userDO = userMapper.getById(userId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserProfileDO userProfileDO = userProfileMapper.getById(userDO.getId());
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
        
        List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(String::trim)
                .filter(trim -> !trim.isEmpty())
                .mapToLong(Long::parseLong)
                .boxed()
                .toList();

        if (ids.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }

        List<CourseDO> courseDOList = courseMapper.getByIds(ids);
        log.info("查询到{}个收藏课程，课程信息: {}", courseDOList.size(), 
                courseDOList.stream().map(c -> "id=" + c.getId() + ",name=" + c.getName()).collect(Collectors.toList()));
        return ResponseEntity.ok(ApiResponse.success(Converter.INSTANCE.toCourseDTOV2(courseDOList)));
    }

    /**
     * 添加订阅
     * 映射: POST /user/subscription → POST /api/v1/users/current/subscriptions
     */
    @PostMapping("/users/current/subscriptions")
    public ResponseEntity<ApiResponse<Object>> subscribe(@RequestParam Long courseId) {
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
            if (ids.contains(courseDO.getId())) return ResponseEntity.ok(ApiResponse.success());
            ids.add(courseDO.getId());
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            userProfileDO.setSubscription(idsStr);
            userProfileMapper.update(userProfileDO);
        }

        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return ResponseEntity.ok(ApiResponse.success(idsArr));
    }

    /**
     * 批量更新订阅
     * 映射: PUT /user/subscription → PUT /api/v1/users/current/subscriptions
     */
    @PutMapping("/users/current/subscriptions")
    public ResponseEntity<ApiResponse<Object>> updateSubscriptions(@RequestParam String subscription) {
        long self = StpUtil.getLoginIdAsLong();

        List<Long> ids = new ArrayList<>();
        String[] parts = subscription.split(",");
        for (String part : parts) {
            ids.add(Long.parseLong(part));
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
        return ResponseEntity.ok(ApiResponse.success(ids));
    }

    /**
     * 取消订阅
     * 映射: DELETE /user/subscription → DELETE /api/v1/users/current/subscriptions/{courseId}
     */
    @DeleteMapping("/users/current/subscriptions/{courseId}")
    public ResponseEntity<ApiResponse<Object>> unsubscribe(@PathVariable Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int self = StpUtil.getLoginIdAsInt();
        UserProfileDO userProfileDO = userProfileMapper.getById(self);
        List<Long> ids = Arrays.stream(userProfileDO.getSubscription().split(","))
                .map(Long::parseLong).collect(Collectors.toCollection(ArrayList::new));
        if (!ids.contains(courseDO.getId())) return ResponseEntity.ok(ApiResponse.success());
        ids.remove(courseDO.getId());

        String idsStr = "";
        if (!ids.isEmpty()) {
            idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        userProfileDO.setSubscription(idsStr);
        userProfileMapper.update(userProfileDO);
        int[] idsArr = Arrays.stream(idsStr.split(",")).mapToInt(Integer::parseInt).toArray();
        return ResponseEntity.ok(ApiResponse.success(idsArr));
    }
}