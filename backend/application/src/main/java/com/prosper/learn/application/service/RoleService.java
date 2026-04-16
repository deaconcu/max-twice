package com.prosper.learn.application.service;

import com.prosper.learn.analytics.ranking.service.RoleRankingDomainService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.application.converter.RoleConverter;
import com.prosper.learn.application.dto.request.CreateRoleRequest;
import com.prosper.learn.application.dto.request.UpdateRoleRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.role.RoleAdminDTO;
import com.prosper.learn.application.dto.response.role.RoleDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.role.RoleDO;
import com.prosper.learn.content.role.RoleDomainService;
import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import com.prosper.learn.shared.common.utils.ValidationUtils;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 角色应用服务
 *
 * 负责协调跨领域逻辑、事件发布、DTO转换
 *
 * 核心功能：
 * - DTO 转换（RoleDTO ↔ RoleDO）
 * - 事件发布（ContentApprovedEvent、ContentRejectedEvent）
 * - 跨域数据聚合（getHotRole- 聚合 Ranking 数据）
 * - 配置管理（SystemProperties）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    // 领域服务
    private final RoleDomainService roleDomainService;

    // 跨域服务依赖
    private final RoleRankingDomainService roleRankingService;
    private final ContentStatsDataService contentStatsDataService;
    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final ContentVisibilityService contentVisibilityService;
    private final MeilisearchService meilisearchService;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

    // 配置
    private final SystemProperties systemProperties;

    // DTO转换器
    private final RoleConverter roleConverter;
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== Query 方法 ==========

    /**
     * 返回正常状态的角色信息
     * 角色被拒绝或屏蔽时抛出异常
     */
    public RoleDTO getById(long id, boolean published, Long userId) {
        RoleDO roleDO = roleDomainService.validateAndGet(id);
        if (roleDO == null) return null;

        // 检查角色的可见性
        contentVisibilityService.validateVisibility(ContentType.role, id, userId);

        return toDTO(roleDO, userId);
    }

    /**
     * 获取角色列表（管理后台专用，包含状态和原因）
     */
    public KeysetPageResponse<RoleAdminDTO> listByState(ContentState state, Long lastId, int limit) {
        Byte stateValue = state != null ? state.value() : null;
        List<RoleDO> roleDOList = roleDomainService.listByState(stateValue, lastId, limit + 1);

        boolean hasMore = roleDOList.size() > limit;
        if (hasMore) {
            roleDOList = roleDOList.subList(0, limit);
        }

        List<RoleAdminDTO> dtoList = roleConverter.toAdminDTO(roleDOList);

        // 填充 creator
        Set<Long> creatorIds = roleDOList.stream()
                .map(RoleDO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, UserBriefDTO> creatorMap = userService.getUserBriefMapByIds(creatorIds);
            for (int i = 0; i < dtoList.size(); i++) {
                Long creatorId = roleDOList.get(i).getCreatorId();
                if (creatorId != null) {
                    dtoList.get(i).setCreator(creatorMap.get(creatorId));
                }
            }
        }

        // 填充统计数据
        if (!dtoList.isEmpty()) {
            List<Long> roleIds = dtoList.stream()
                    .map(RoleAdminDTO::getId)
                    .collect(Collectors.toList());
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(
                    ContentType.role, roleIds);
            Map<Long, ContentStatsDO> statsMap = statsList.stream()
                    .collect(Collectors.toMap(ContentStatsDO::getContentId, s -> s));
            for (RoleAdminDTO dto : dtoList) {
                ContentStatsDO stats = statsMap.get(dto.getId());
                if (stats != null) {
                    dto.setRoadmapCount(stats.getRoadmapCount() != null ? stats.getRoadmapCount() : 0);
                    dto.setBookmarkCount(stats.getBookmarkCount() != null ? stats.getBookmarkCount() : 0);
                } else {
                    dto.setRoadmapCount(0);
                    dto.setBookmarkCount(0);
                }
            }
        }

        Long nextLastId = dtoList.isEmpty() ? null : dtoList.get(dtoList.size() - 1).getId();
        return KeysetPageResponse.of(dtoList, hasMore, null, nextLastId);
    }

    /**
     * 获取角色详情（管理后台专用，任意状态）
     */
    public RoleAdminDTO getAdminById(Long id) {
        RoleDO roleDO = roleDomainService.getById(id);
        if (roleDO == null) {
            return null;
        }
        RoleAdminDTO dto = roleConverter.toAdminDTO(roleDO);
        dto.setCreator(userService.getUserBriefById(roleDO.getCreatorId()));

        // 填充统计数据
        contentStatsDataService.getByContent(ContentType.role, id).ifPresent(stats -> {
            dto.setRoadmapCount(stats.getRoadmapCount() != null ? stats.getRoadmapCount() : 0);
            dto.setBookmarkCount(stats.getBookmarkCount() != null ? stats.getBookmarkCount() : 0);
        });

        return dto;
    }

    /**
     * 获取已发布的角色列表（公开接口，只返回已发布状态）
     */
    public List<RoleDTO> getApprovedByLastId(Long lastId, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByState(ContentState.PUBLISHED.value(), lastId, limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> getListByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByMainCategoryAndLastId(mainCategory, lastId, limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> getListByCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> searchByKeyword(String keyword) {
        List<RoleDO> roleDOList = roleDomainService.searchByKeyword(keyword);
        return toDTO(roleDOList);
    }

    /**
     * 管理后台按名称搜索角色（搜索所有状态，支持滚动分页）
     */
    public KeysetPageResponse<RoleAdminDTO> searchByName(String name, Long lastId) {
        int pageSize = 20;
        List<RoleDO> roleDOList = roleDomainService.searchByName(name, lastId, pageSize + 1);

        boolean hasMore = roleDOList.size() > pageSize;
        if (hasMore) {
            roleDOList = roleDOList.subList(0, pageSize);
        }

        List<RoleAdminDTO> dtoList = roleConverter.toAdminDTO(roleDOList);

        // 填充 creator
        Set<Long> creatorIds = roleDOList.stream()
                .map(RoleDO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, UserBriefDTO> creatorMap = userService.getUserBriefMapByIds(creatorIds);
            for (int i = 0; i < dtoList.size(); i++) {
                Long creatorId = roleDOList.get(i).getCreatorId();
                if (creatorId != null) {
                    dtoList.get(i).setCreator(creatorMap.get(creatorId));
                }
            }
        }

        Long nextLastId = dtoList.isEmpty() ? null : dtoList.get(dtoList.size() - 1).getId();
        return KeysetPageResponse.of(dtoList, hasMore, null, nextLastId);
    }

    // ========== Command 方法 ==========

    @Transactional
    public Long create(CreateRoleRequest request, UserDO creator) {
        // 调用 DomainService 创建角色
        return roleDomainService.create(
            creator.getId(),
            request.getName(),
            request.getDescription(),
            request.getSkills(),
            request.getMainCategory(),
            request.getSubCategory()
        );
    }

    @Transactional
    public void update(Long id, UpdateRoleRequest request, UserDO operator) {
        // 参数验证
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }

        // 调用 DomainService 更新角色
        roleDomainService.update(
            id,
            request.getName(),
            request.getDescription(),
            request.getPrice() != null ? String.valueOf(request.getPrice()) : null,
            request.getSkills(),
            request.getMainCategory(),
            request.getSubCategory(),
            request.getIcon(),
            request.getReason()
        );
    }

    @Transactional
    public void approve(long id, UserDO operator) {
        // 调用 DomainService 执行审核通过
        roleDomainService.approve(id);

        // 获取角色信息
        RoleDO roleDO = roleDomainService.getById(id);

        // 发布审核通过事件，触发消息通知
        eventPublisher.publishEvent(ContentApprovedEvent.forRole(
            roleDO.getCreatorId(),
            roleDO.getId(),
            roleDO.getName()
        ));

        // 异步更新搜索索引
        meilisearchService.indexRole(roleDO, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void reject(long id, String reason, UserDO operator) {
        // 调用 DomainService 执行拒绝
        roleDomainService.reject(id, reason);

        // 获取角色信息
        RoleDO role = roleDomainService.getById(id);
        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forRole(
            role.getCreatorId(),
            role.getId(),
            role.getName(),
            reasonValue
        ));

        // 异步更新搜索索引（从索引中移除）
        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void ban(long id, String reason, UserDO operator) {
        // 调用 DomainService 执行封禁
        roleDomainService.ban(id, reason);

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        // ban 不发送任何消息或事件
        log.info("角色 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reasonValue);

        // 异步更新搜索索引（从索引中移除）
        RoleDO role = roleDomainService.getById(id);
        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    /**
     * 删除角色
     */
    @Transactional
    public void delete(long id, UserDO operator) {
        // 调用 DomainService 执行删除
        roleDomainService.delete(id);

        // 异步从搜索索引中移除
        meilisearchService.deleteRole(id, DataSourceContextHolder.getLanguage());
    }

    /**
     * 获取热门角色列表
     * 跨域查询：聚合 role 数据和 ranking 数据
     */
    public List<RoleDTO> getHotRoles(int limit) {
        validateHotRolesLimit(limit);

        try {
            // 从 Ranking 域获取热门角色ID列表
            List<Long> hotRoleIds = roleRankingService.getHotRoleIds(limit);

            if (hotRoleIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 从 Role 域获取角色信息
            List<RoleDO> roleDOList = roleDomainService.getByIds(hotRoleIds);

            List<RoleDTO> result = new ArrayList<>();
            for (RoleDO roleDO : roleDOList) {
                // 只返回已发布状态的角色
                if (roleDO.getState() != ContentState.PUBLISHED.value()) {
                    continue;
                }

                // 转换为 DTO
                RoleDTO roleDTO = toDTO(roleDO);

                // 从 Ranking 域获取学习人数
                long learningCount = roleRankingService.getRoleLearningCount(roleDO.getId());
                roleDTO.setLearnerCount((int) learningCount);

                result.add(roleDTO);
            }

            return result;

        } catch (Exception e) {
            log.error("获取热门专业失败，limit: {}", limit, e);
            throw StatusCode.ROLE_HOT_LIST_FAILED.exception(e);
        }
    }

    // ========== DTO转换方法 ==========

    /**
     * 转换单个对象为DTO
     */
    public RoleDTO toDTO(RoleDO roleDO) {
        return roleConverter.toDTO(roleDO);
    }

    /**
     * 转换单个对象为DTO（含收藏状态）
     */
    public RoleDTO toDTO(RoleDO roleDO, Long userId) {
        RoleDTO dto = roleConverter.toDTO(roleDO);

        // 填充收藏状态
        if (userId != null) {
            dto.setBookmarked(bookmarkService.isBookmarked(userId, roleDO.getId(), ContentType.role));
        } else {
            dto.setBookmarked(false);
        }

        return dto;
    }

    /**
     * 转换列表为DTO列表
     */
    public List<RoleDTO> toDTO(List<RoleDO> roleDOList) {
        return roleConverter.toDTO(roleDOList);
    }

    /**
     * 转换列表为DTO列表（含收藏状态）
     */
    public List<RoleDTO> toDTO(List<RoleDO> roleDOList, Long userId) {
        List<RoleDTO> dtos = roleConverter.toDTO(roleDOList);

        // 批量填充收藏状态
        if (userId != null && !dtos.isEmpty()) {
            List<Long> ids = dtos.stream().map(RoleDTO::getId).collect(Collectors.toList());
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, ids, ContentType.role);
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            dtos.forEach(dto -> dto.setBookmarked(bookmarkedSet.contains(dto.getId())));
        } else {
            dtos.forEach(dto -> dto.setBookmarked(false));
        }

        return dtos;
    }

    // ========== Private 辅助方法 ==========

    private void validateHotRolesLimit(int limit) {
        ValidationUtils.require(limit > 0, "限制数量必须大于0");
        ValidationUtils.require(limit <= 200, "限制数量超过最大值");
    }
}
