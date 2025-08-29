package com.prosper.learn.domain.service;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.FolloweeDTO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.FollowMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        UserDO followee = userMapper.getById(followeeId);
        if (followee == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }

        FollowDO existingFollow = followMapper.get(followerId, followeeId);
        if (existingFollow == null) {
            UserDO follower = userMapper.getById(followerId);
            followMapper.insert(followerId, followeeId);

            messageService.createFollowMessage(followeeId, follower.getId());
        }
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        UserDO followee = userMapper.getById(followeeId);
        if (followee == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }

        FollowDO existingFollow = followMapper.get(followerId, followeeId);
        if (existingFollow != null) {
            followMapper.delete(followerId, followeeId);
        }
    }

    public List<FolloweeDTO> getFollowees(Long userId, LocalDateTime lastCreateTime) {
        UserDO follower = userMapper.getById(userId);
        if (follower == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }

        int pageSize = 10;
        List<FollowDO> followDOList = followMapper.getList(userId, lastCreateTime, pageSize);

        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        if (userIds.isEmpty()) {
            return new ArrayList<>();
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
}