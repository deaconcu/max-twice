package com.prosper.learn.application.service;

import com.prosper.learn.application.dto.response.FolloweeDTO;
import com.prosper.learn.interaction.follow.FollowDO;
import com.prosper.learn.interaction.follow.FollowDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.user.relationship.UserFollowedEvent;
import com.prosper.learn.shared.domain.event.user.relationship.UserUnfollowedEvent;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 关注应用服务
 *
 * 负责协调跨领域逻辑、事件发布
 *
 * 核心功能：
 * - 验证用户存在性（跨域查询）
 * - 协调关注操作并发布事件
 * - 聚合关注列表和用户信息（跨域查询）
 *
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    /** 关注领域服务 */
    private final FollowDomainService followDomainService;

    /** 用户数据访问接口 */
    private final UserDataService userDataService;

    /** 事件发布器 */
    private final ApplicationEventPublisher eventPublisher;

    // ========== Command 方法（写操作）==========

    /**
     * 关注用户
     *
     * 执行关注操作，包括验证用户存在性、创建关注记录和发布事件。
     * 使用事件驱动架构，通过发布事件来处理消息通知和统计更新等副作用操作。
     *
     * @param follower 关注者（当前用户）
     * @param followeeId 被关注者ID
     * @throws BusinessException 当参数无效或用户不存在时抛出异常
     */
    @Transactional
    public void follow(UserDO follower, Long followeeId) {
        // 验证被关注者存在性（跨域查询）
        UserDO followee = validateUserExists(followeeId);

        // 调用 DomainService 执行关注逻辑
        boolean added = followDomainService.follow(follower.getId(), followeeId);

        // 如果成功关注（非幂等重复操作），发布关注事件
        if (added) {
            eventPublisher.publishEvent(new UserFollowedEvent(follower.getId(), followeeId));
        }
    }

    /**
     * 取消关注用户
     *
     * 执行取消关注操作，包括验证用户存在性、删除关注记录和发布事件。
     * 使用事件驱动架构，通过发布事件来处理统计更新等副作用操作。
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @throws BusinessException 当参数无效或用户不存在时抛出异常
     */
    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        // 验证被关注者存在性（跨域查询）
        UserDO followee = validateUserExists(followeeId);

        // 调用 DomainService 执行取消关注逻辑
        boolean removed = followDomainService.unfollow(followerId, followeeId);

        // 如果成功取消关注（非幂等重复操作），发布取消关注事件
        if (removed) {
            eventPublisher.publishEvent(new UserUnfollowedEvent(followerId, followeeId));
        }
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取用户的关注列表
     *
     * 分页获取指定用户关注的用户列表，包含用户基本信息和关注时间。
     * 这是一个跨域查询方法，需要聚合 interaction 域和 user 域的数据。
     *
     * @param userId 用户ID
     * @param lastCreateTime 最后一条记录的创建时间（用于分页）
     * @return 关注者DTO列表
     * @throws BusinessException 当用户不存在时抛出异常
     */
    public List<FolloweeDTO> getFollowees(Long userId, LocalDateTime lastCreateTime) {
        // 验证用户存在性（跨域查询）
        UserDO follower = validateUserExists(userId);

        // 获取关注记录列表（interaction 域）
        List<FollowDO> followDOList = followDomainService.getFollowees(userId, lastCreateTime);

        if (followDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取被关注者ID列表
        List<Long> userIds = new ArrayList<>();
        for (FollowDO followDO : followDOList) {
            userIds.add(followDO.getFolloweeId());
        }

        // 批量获取用户信息（user 域）
        Map<Long, UserDO> userDOMap = getUserMap(userIds);

        // 转换为DTO列表
        return convertToFolloweeDTOList(followDOList, userDOMap);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 验证用户存在性（跨域查询）
     *
     * @param userId 用户ID
     * @return 用户实体对象
     * @throws BusinessException 当用户不存在时抛出异常
     */
    private UserDO validateUserExists(Long userId) {
        if (userId == null || userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }

        UserDO userDO = userDataService.getById(userId);
        if (userDO == null) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
        return userDO;
    }

    /**
     * 批量获取用户信息并转换为Map（跨域查询）
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
}