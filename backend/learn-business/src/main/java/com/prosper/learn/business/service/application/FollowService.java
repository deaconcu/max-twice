package com.prosper.learn.business.service.application;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.business.service.domain.MessageService;
import com.prosper.learn.dto.response.FolloweeDTO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.business.service.data.FollowDataService;
import com.prosper.learn.business.service.data.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 关注服务
 * 
 * 负责管理用户之间的关注关系，包括：
 * - 关注和取消关注用户
 * - 获取关注列表
 * - 发送关注通知消息
 * 
 * 核心功能：
 * - 关注操作的事务性保证
 * - 防止重复关注和取消关注
 * - 自动发送关注通知消息
 * - 分页获取关注列表
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    /** 默认分页大小常量 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** 关注数据访问接口 */
    private final FollowDataService followDataService;
    
    /** 用户数据访问接口 */
    private final UserDataService userDataService;
    
    /** 消息服务，用于发送关注通知 */
    private final MessageService messageService;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    /**
     * 验证用户ID有效性
     * 
     * @param userId 用户ID
     * @throws BusinessException 当用户ID无效时抛出异常
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }
    }
    
    /**
     * 验证用户存在性
     * 
     * @param userId 用户ID
     * @return 用户实体对象
     * @throws BusinessException 当用户不存在时抛出异常
     */
    private UserDO validateUserExists(Long userId) {
        validateUserId(userId);
        UserDO userDO = userDataService.getById(userId);
        if (userDO == null) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }
    
    /**
     * 验证关注参数有效性
     * 
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @throws BusinessException 当参数无效时抛出异常
     */
    private void validateFollowParams(Long followerId, Long followeeId) {
        validateUserId(followerId);
        validateUserId(followeeId);
        
        if (followerId.equals(followeeId)) {
            throw ErrorCode.INVALID_PARAMETER.exception("不能关注自己");
        }
    }
    
    /**
     * 批量获取用户信息并转换为Map
     * 
     * @param userIds 用户ID列表
     * @return 用户ID到用户对象的映射
     */
    private Map<Long, UserDO> getUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<UserDO> userDOList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userDOMap = new HashMap<>();
        for (UserDO userDO : userDOList) {
            userDOMap.put(userDO.getId(), userDO);
        }
        return userDOMap;
    }
    
    /**
     * 将关注数据转换为DTO列表
     * 
     * @param followDOList 关注数据列表
     * @param userDOMap 用户信息映射
     * @return 关注者DTO列表
     */
    private List<FolloweeDTO> convertToFolloweeDTOList(List<FollowDO> followDOList, Map<Long, UserDO> userDOMap) {
        List<FolloweeDTO> followeeDTOList = new ArrayList<>();
        
        for (FollowDO followDO : followDOList) {
            UserDO userDO = userDOMap.get(followDO.getFolloweeId());
            if (userDO != null) {
                FolloweeDTO followeeDTO = new FolloweeDTO();
                followeeDTO.setId(followDO.getFolloweeId());
                followeeDTO.setName(userDO.getName());
                followeeDTO.setBiography(userDO.getBiography());
                followeeDTO.setCreatedAt(Utils.getTimeString(followDO.getCreatedAt()));
                followeeDTOList.add(followeeDTO);
            }
        }
        
        return followeeDTOList;
    }

    /**
     * 关注用户
     *
     * 执行关注操作，包括验证用户存在性、检查重复关注、创建关注记录和发送通知。
     *
     * @param follower 关注者（当前用户）
     * @param followeeId 被关注者ID
     * @throws BusinessException 当参数无效或用户不存在时抛出异常
     */
    @Transactional
    public void follow(UserDO follower, Long followeeId) {
        validateFollowParams(follower.getId(), followeeId);

        // 验证被关注者存在性
        UserDO followee = validateUserExists(followeeId);

        // 检查是否已经关注
        FollowDO existingFollow = followDataService.get(follower.getId(), followeeId);
        if (existingFollow == null) {
            // 创建关注记录
            followDataService.insert(follower.getId(), followeeId);

            // 发送关注通知消息
            messageService.createFollowMessage(followeeId, follower.getId());
        }
    }

    /**
     * 取消关注用户
     * 
     * 执行取消关注操作，包括验证用户存在性、检查关注关系存在性、删除关注记录。
     * 
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @throws BusinessException 当参数无效或用户不存在时抛出异常
     */
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        validateFollowParams(followerId, followeeId);
        
        // 验证被关注者存在性
        UserDO followee = validateUserExists(followeeId);
        
        // 检查关注关系是否存在
        FollowDO existingFollow = followDataService.get(followerId, followeeId);
        if (existingFollow != null) {
            // 删除关注记录
            followDataService.delete(followerId, followeeId);
        }
    }

    /**
     * 获取用户的关注列表
     * 
     * 分页获取指定用户关注的用户列表，包含用户基本信息和关注时间。
     * 
     * @param userId 用户ID
     * @param lastCreateTime 最后一条记录的创建时间（用于分页）
     * @return 关注者DTO列表
     * @throws BusinessException 当用户不存在时抛出异常
     */
    public List<FolloweeDTO> getFollowees(Long userId, LocalDateTime lastCreateTime) {
        // 验证用户存在性
        UserDO follower = validateUserExists(userId);
        
        // 获取关注记录列表
        List<FollowDO> followDOList = followDataService.getList(userId, lastCreateTime, DEFAULT_PAGE_SIZE);
        
        if (followDOList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 提取被关注者ID列表
        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }
        
        // 批量获取用户信息
        Map<Long, UserDO> userDOMap = getUserMap(userIds);
        
        // 转换为DTO列表
        return convertToFolloweeDTOList(followDOList, userDOMap);
    }
}