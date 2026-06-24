package com.twicemax.application.service;

import com.twicemax.analytics.ranking.service.RoleRankingDomainService;
import com.twicemax.analytics.stats.mapper.ContentStatsDO;
import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.application.converter.RoleConverter;
import com.twicemax.application.dto.request.CreateRoleRequest;
import com.twicemax.application.dto.request.UpdateRoleRequest;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.application.dto.response.KeysetPageResponse;
import com.twicemax.application.dto.response.role.RoleAdminDTO;
import com.twicemax.application.dto.response.role.RoleDTO;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDomainService;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import com.twicemax.shared.common.utils.ValidationUtils;
import com.twicemax.shared.domain.Enums.ContentType;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.user.profile.UserDO;
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

/**
 * 角色应用服务（revision 模型）。
 * <p>
 * 主体状态 {@link NewContentState}：NEVER_PUBLISHED / PUBLISHED / BANNED；
 * 一次"申请→审核"的生命周期（SUBMITTED / PUBLISHED / REJECTED / WITHDRAWN）落在 content_revision。
 * <p>
 * 职责：
 * - 协调 RoleDomainService 完成业务流转
 * - 跨域聚合（Ranking、Stats、Bookmark）
 * - 发布生命周期事件（Approved / Rejected / Banned / Restored）
 * - DTO 转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleDomainService roleDomainService;

    private final RoleRankingDomainService roleRankingService;
    private final ContentStatsDataService contentStatsDataService;
    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final ContentVisibilityService contentVisibilityService;
    private final MeilisearchService meilisearchService;

    private final ApplicationEventPublisher eventPublisher;

    private final SystemProperties systemProperties;

    private final RoleConverter roleConverter;

    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== Query 方法 ==========

    /**
     * 返回正常状态的角色信息。
     * 角色被屏蔽时由 contentVisibilityService 抛出异常。
     */
    public RoleDTO getById(long id, boolean published, Long userId) {
        RoleDO roleDO = roleDomainService.validateAndGet(id);
        if (roleDO == null) return null;

        contentVisibilityService.validateVisibility(ContentType.role, id, userId);

        return toDTO(roleDO, userId);
    }

    /**
     * 管理后台按主体状态分页（NEVER_PUBLISHED / PUBLISHED / BANNED）。
     */
    public KeysetPageResponse<RoleAdminDTO> listByState(NewContentState state, Long lastId, int limit) {
        String stateValue = state != null ? state.value() : null;
        List<RoleDO> roleDOList = roleDomainService.listByState(stateValue, lastId, limit + 1);

        boolean hasMore = roleDOList.size() > limit;
        if (hasMore) {
            roleDOList = roleDOList.subList(0, limit);
        }

        List<RoleAdminDTO> dtoList = roleConverter.toAdminDTO(roleDOList);

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

    public RoleAdminDTO getAdminById(Long id) {
        RoleDO roleDO = roleDomainService.getById(id);
        if (roleDO == null) {
            return null;
        }
        RoleAdminDTO dto = roleConverter.toAdminDTO(roleDO);
        dto.setCreator(userService.getUserBriefById(roleDO.getCreatorId()));

        contentStatsDataService.getByContent(ContentType.role, id).ifPresent(stats -> {
            dto.setRoadmapCount(stats.getRoadmapCount() != null ? stats.getRoadmapCount() : 0);
            dto.setBookmarkCount(stats.getBookmarkCount() != null ? stats.getBookmarkCount() : 0);
        });

        return dto;
    }

    /**
     * 已发布角色列表（公开接口）。
     */
    public List<RoleDTO> getApprovedByLastId(String cursor, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByState(
                NewContentState.PUBLISHED_VALUE, Cursor.decode(cursor).id(), limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> getListByMainCategoryAndLastId(int mainCategory, String cursor, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByMainCategoryAndLastId(mainCategory, Cursor.decode(cursor).id(), limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> getListByCategoryAndLastId(int mainCategory, int subCategory, String cursor, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, Cursor.decode(cursor).id(), limit);
        return toDTO(roleDOList);
    }

    public List<RoleDTO> searchByKeyword(String keyword) {
        List<RoleDO> roleDOList = roleDomainService.searchByKeyword(keyword);
        return toDTO(roleDOList);
    }

    public KeysetPageResponse<RoleAdminDTO> searchByName(String name, Long lastId) {
        int pageSize = 20;
        List<RoleDO> roleDOList = roleDomainService.searchByName(name, lastId, pageSize + 1);

        boolean hasMore = roleDOList.size() > pageSize;
        if (hasMore) {
            roleDOList = roleDOList.subList(0, pageSize);
        }

        List<RoleAdminDTO> dtoList = roleConverter.toAdminDTO(roleDOList);

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
        return roleDomainService.create(
                creator.getId(),
                request.getName(),
                request.getDescription(),
                request.getSkills(),
                request.getMainCategory(),
                request.getSubCategory()
        );
    }

    /**
     * 用户被驳回 / 撤回后重新提交。
     */
    @Transactional
    public Long resubmit(Long roleId, UpdateRoleRequest request, UserDO author) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }
        return roleDomainService.resubmit(
                roleId,
                author.getId(),
                request.getName(),
                request.getDescription(),
                request.getSkills(),
                request.getIcon(),
                request.getMainCategory(),
                request.getSubCategory()
        );
    }

    /**
     * 作者撤回审核中的版本。
     */
    @Transactional
    public void withdraw(long roleId, UserDO author) {
        roleDomainService.withdraw(roleId, author.getId());
    }

    /**
     * 管理员直接编辑（走 revision 留审计）。
     */
    @Transactional
    public void edit(Long roleId, UpdateRoleRequest request, UserDO operator) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }
        roleDomainService.edit(
                roleId,
                operator.getId(),
                request.getName(),
                request.getDescription(),
                request.getSkills(),
                request.getIcon(),
                request.getMainCategory(),
                request.getSubCategory()
        );

        RoleDO role = roleDomainService.getById(roleId);
        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void approve(long id, UserDO operator) {
        roleDomainService.approve(id, operator.getId());

        RoleDO roleDO = roleDomainService.getById(id);

        eventPublisher.publishEvent(ContentApprovedEvent.forRole(
                roleDO.getCreatorId(),
                roleDO.getId(),
                roleDO.getName()
        ));

        meilisearchService.indexRole(roleDO, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void reject(long id, String reason, UserDO operator) {
        roleDomainService.reject(id, reason, operator.getId());

        RoleDO role = roleDomainService.getById(id);
        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        eventPublisher.publishEvent(ContentRejectedEvent.forRole(
                role.getCreatorId(),
                role.getId(),
                role.getName(),
                reasonValue
        ));

        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void ban(long id, String reason, UserDO operator) {
        roleDomainService.ban(id, reason, operator.getId());

        RoleDO role = roleDomainService.getById(id);
        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        eventPublisher.publishEvent(ContentBannedEvent.forRole(
                role.getCreatorId(),
                role.getId(),
                role.getName(),
                reasonValue
        ));

        log.info("角色 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reasonValue);

        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    /**
     * 解封：恢复到 PUBLISHED（已有发布版本）或 NEVER_PUBLISHED。
     */
    @Transactional
    public void restore(long id, UserDO operator) {
        roleDomainService.restore(id);

        RoleDO role = roleDomainService.getById(id);

        eventPublisher.publishEvent(ContentRestoredEvent.forRole(
                operator.getId(),
                role.getCreatorId(),
                role.getId(),
                role.getName(),
                DEFAULT_EMPTY_STRING
        ));

        meilisearchService.indexRole(role, DataSourceContextHolder.getLanguage());
    }

    @Transactional
    public void delete(long id, UserDO operator) {
        roleDomainService.delete(id);

        meilisearchService.deleteRole(id, DataSourceContextHolder.getLanguage());
    }

    /**
     * 用户视角：分页查看自己创建的角色（包含 NEVER_PUBLISHED / PUBLISHED；默认排除 BANNED）。
     */
    public List<RoleDTO> getUserRoles(Long userId, String cursor, NewContentState state, int limit) {
        List<RoleDO> roleDOList = roleDomainService.listByCreator(
                userId, Cursor.decode(cursor).id(), limit, state);
        return toDTO(roleDOList, userId);
    }

    public List<RoleDTO> getHotRoles(int limit) {
        validateHotRolesLimit(limit);

        try {
            List<Long> hotRoleIds = roleRankingService.getHotRoleIds(limit);

            if (hotRoleIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<RoleDO> roleDOList = roleDomainService.getByIds(hotRoleIds);

            List<RoleDTO> result = new ArrayList<>();
            for (RoleDO roleDO : roleDOList) {
                if (!NewContentState.PUBLISHED_VALUE.equals(roleDO.getState())) {
                    continue;
                }

                RoleDTO roleDTO = toDTO(roleDO);

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

    public RoleDTO toDTO(RoleDO roleDO) {
        return roleConverter.toDTO(roleDO);
    }

    public RoleDTO toDTO(RoleDO roleDO, Long userId) {
        RoleDTO dto = roleConverter.toDTO(roleDO);

        if (userId != null) {
            dto.setBookmarked(bookmarkService.isBookmarked(userId, roleDO.getId(), ContentType.role));
        } else {
            dto.setBookmarked(false);
        }

        return dto;
    }

    public List<RoleDTO> toDTO(List<RoleDO> roleDOList) {
        return roleConverter.toDTO(roleDOList);
    }

    public List<RoleDTO> toDTO(List<RoleDO> roleDOList, Long userId) {
        List<RoleDTO> dtos = roleConverter.toDTO(roleDOList);

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
