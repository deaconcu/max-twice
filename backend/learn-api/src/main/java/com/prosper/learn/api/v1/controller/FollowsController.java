package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.dto.FolloweeDTO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.FollowMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 关注功能接口
 * 从UsersController拆分出来的关注功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FollowsController {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    /**
     * 关注用户
     * 映射: POST /user/follow → POST /api/v1/follows
     */
    @PostMapping("/follows")
    public ResponseEntity<ApiResponse<Void>> follow(@RequestParam Long followeeId) {
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
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 取消关注
     * 映射: DELETE /user/follow → DELETE /api/v1/follows/{followeeId}
     */
    @DeleteMapping("/follows/{followeeId}")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable Long followeeId) {
        int followerId = StpUtil.getLoginIdAsInt();
        UserDO userDO = userMapper.getById(followeeId);
        if (userDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        FollowDO followDO = followMapper.get(followerId, followeeId);
        if (followDO != null) {
            followMapper.delete(followerId, followeeId);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 获取关注列表
     * 映射: GET /user/followee → GET /api/v1/users/{userId}/followees
     */
    @GetMapping("/users/{userId}/followees")
    public ResponseEntity<ApiResponse<Object>> getFollowees(@PathVariable Long userId, @RequestParam String lastCreateTime) {
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
            return ResponseEntity.ok(ApiResponse.success(new LinkedList<>()));
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

        return ResponseEntity.ok(ApiResponse.success(followeeDTOList));
    }
}