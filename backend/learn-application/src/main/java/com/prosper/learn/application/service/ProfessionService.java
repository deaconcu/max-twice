package com.prosper.learn.application.service;

import com.prosper.learn.analytics.ranking.service.ProfessionRankingDomainService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.application.converter.ProfessionConverter;
import com.prosper.learn.application.dto.request.CreateProfessionRequest;
import com.prosper.learn.application.dto.request.UpdateProfessionRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.profession.ProfessionAdminDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDomainService;
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
 * 职业应用服务
 *
 * 负责协调跨领域逻辑、事件发布、DTO转换
 *
 * 核心功能：
 * - DTO 转换（ProfessionDTO ↔ ProfessionDO）
 * - 事件发布（ContentApprovedEvent、ContentRejectedEvent）
 * - 跨域数据聚合（getHotProfessions - 聚合 Ranking 数据）
 * - 配置管理（SystemProperties）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionService {

    // 领域服务
    private final ProfessionDomainService professionDomainService;

    // 跨域服务依赖
    private final ProfessionRankingDomainService professionRankingService;
    private final ContentStatsDataService contentStatsDataService;
    private final BookmarkService bookmarkService;
    private final UserService userService;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

    // 配置
    private final SystemProperties systemProperties;

    // DTO转换器
    private final ProfessionConverter professionConverter;
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== Query 方法 ==========

    /**
     * 返回正常状态的职业信息
     * 职业被拒绝或屏蔽时抛出异常
     */
    public ProfessionDTO getById(long id, boolean published, Long userId) {
        ProfessionDO professionDO = professionDomainService.validateAndGet(id);
        if (professionDO == null) return null;
        if (published &&
            (professionDO.getState() == ContentState.REJECTED.value() ||
             professionDO.getState() == ContentState.BANNED.value())) {
            throw StatusCode.PROFESSION_BLOCKED.exception();
        }
        return toDTO(professionDO, userId);
    }

    /**
     * 获取职业列表（管理后台专用，包含状态和原因）
     */
    public KeysetPageResponse<ProfessionAdminDTO> listByState(ContentState state, Long lastId, int limit) {
        Byte stateValue = state != null ? state.value() : null;
        List<ProfessionDO> professionDOList = professionDomainService.listByState(stateValue, lastId, limit + 1);

        boolean hasMore = professionDOList.size() > limit;
        if (hasMore) {
            professionDOList = professionDOList.subList(0, limit);
        }

        List<ProfessionAdminDTO> dtoList = professionConverter.toAdminDTO(professionDOList);

        // 填充 creator
        Set<Long> creatorIds = professionDOList.stream()
                .map(ProfessionDO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, UserBriefDTO> creatorMap = userService.getUserBriefMapByIds(creatorIds);
            for (int i = 0; i < dtoList.size(); i++) {
                Long creatorId = professionDOList.get(i).getCreatorId();
                if (creatorId != null) {
                    dtoList.get(i).setCreator(creatorMap.get(creatorId));
                }
            }
        }

        // 填充统计数据
        if (!dtoList.isEmpty()) {
            List<Long> professionIds = dtoList.stream()
                    .map(ProfessionAdminDTO::getId)
                    .collect(Collectors.toList());
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(
                    ContentType.profession, professionIds);
            Map<Long, ContentStatsDO> statsMap = statsList.stream()
                    .collect(Collectors.toMap(ContentStatsDO::getContentId, s -> s));
            for (ProfessionAdminDTO dto : dtoList) {
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
     * 获取职业详情（管理后台专用，任意状态）
     */
    public ProfessionAdminDTO getAdminById(Long id) {
        ProfessionDO professionDO = professionDomainService.getById(id);
        if (professionDO == null) {
            return null;
        }
        ProfessionAdminDTO dto = professionConverter.toAdminDTO(professionDO);
        dto.setCreator(userService.getUserBriefById(professionDO.getCreatorId()));

        // 填充统计数据
        contentStatsDataService.getByContent(ContentType.profession, id).ifPresent(stats -> {
            dto.setRoadmapCount(stats.getRoadmapCount() != null ? stats.getRoadmapCount() : 0);
            dto.setBookmarkCount(stats.getBookmarkCount() != null ? stats.getBookmarkCount() : 0);
        });

        return dto;
    }

    /**
     * 获取已发布的职业列表（公开接口，只返回已发布状态）
     */
    public List<ProfessionDTO> getApprovedByLastId(Long lastId, int limit) {
        List<ProfessionDO> professionDOList = professionDomainService.listByState(ContentState.PUBLISHED.value(), lastId, limit);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        List<ProfessionDO> professionDOList = professionDomainService.listByMainCategoryAndLastId(mainCategory, lastId, limit);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        List<ProfessionDO> professionDOList = professionDomainService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> searchByKeyword(String keyword) {
        List<ProfessionDO> professionDOList = professionDomainService.searchByKeyword(keyword);
        return toDTO(professionDOList);
    }

    /**
     * 管理后台按名称搜索职业（搜索所有状态，支持滚动分页）
     */
    public KeysetPageResponse<ProfessionAdminDTO> searchByName(String name, Long lastId) {
        int pageSize = 20;
        List<ProfessionDO> professionDOList = professionDomainService.searchByName(name, lastId, pageSize + 1);

        boolean hasMore = professionDOList.size() > pageSize;
        if (hasMore) {
            professionDOList = professionDOList.subList(0, pageSize);
        }

        List<ProfessionAdminDTO> dtoList = professionConverter.toAdminDTO(professionDOList);

        // 填充 creator
        Set<Long> creatorIds = professionDOList.stream()
                .map(ProfessionDO::getCreatorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, UserBriefDTO> creatorMap = userService.getUserBriefMapByIds(creatorIds);
            for (int i = 0; i < dtoList.size(); i++) {
                Long creatorId = professionDOList.get(i).getCreatorId();
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
    public Long create(CreateProfessionRequest request, UserDO creator) {
        // 调用 DomainService 创建职业
        return professionDomainService.create(
            creator.getId(),
            request.getName(),
            request.getDescription(),
            request.getSkills(),
            request.getMainCategory(),
            request.getSubCategory()
        );
    }

    @Transactional
    public void update(Long id, UpdateProfessionRequest request, UserDO operator) {
        // 参数验证
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }

        // 调用 DomainService 更新职业
        professionDomainService.update(
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
        professionDomainService.approve(
            id,
            systemProperties.getProfession().isEnableStateValidation(),
            systemProperties.getProfession().isEnableConcurrencyCheck()
        );

        // 获取职业信息
        ProfessionDO profession = professionDomainService.getById(id);

        // 发布审核通过事件，触发消息通知
        eventPublisher.publishEvent(ContentApprovedEvent.forProfession(
            profession.getCreatorId(),
            profession.getId(),
            profession.getName()
        ));
    }

    @Transactional
    public void reject(long id, String reason, UserDO operator) {
        // 调用 DomainService 执行拒绝
        professionDomainService.reject(
            id,
            reason,
            systemProperties.getProfession().isEnableStateValidation(),
            systemProperties.getProfession().isEnableConcurrencyCheck()
        );

        // 获取职业信息
        ProfessionDO profession = professionDomainService.getById(id);
        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forProfession(
            profession.getCreatorId(),
            profession.getId(),
            profession.getName(),
            reasonValue
        ));
    }

    @Transactional
    public void ban(long id, String reason, UserDO operator) {
        // 调用 DomainService 执行封禁
        professionDomainService.ban(
            id,
            reason,
            systemProperties.getProfession().isEnableStateValidation(),
            systemProperties.getProfession().isEnableConcurrencyCheck()
        );

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        // ban 不发送任何消息或事件
        log.info("职业 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reasonValue);
    }

    /**
     * 删除职业
     */
    @Transactional
    public void delete(long id, UserDO operator) {
        // 调用 DomainService 执行删除
        professionDomainService.delete(id);
    }

    /**
     * 获取热门职业列表
     * 跨域查询：聚合 profession 数据和 ranking 数据
     */
    public List<ProfessionDTO> getHotProfessions(int limit) {
        validateHotProfessionsLimit(limit);

        try {
            // 从 Ranking 域获取热门职业ID列表
            List<Long> hotProfessionIds = professionRankingService.getHotProfessionIds(limit);

            if (hotProfessionIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 从 Profession 域获取职业信息
            List<ProfessionDO> professionDOList = professionDomainService.getByIds(hotProfessionIds);

            List<ProfessionDTO> result = new ArrayList<>();
            for (ProfessionDO professionDO : professionDOList) {
                // 只返回已发布状态的职业
                if (professionDO.getState() != ContentState.PUBLISHED.value()) {
                    continue;
                }

                // 转换为 DTO
                ProfessionDTO professionDTO = toDTO(professionDO);

                // 从 Ranking 域获取学习人数
                long learningCount = professionRankingService.getProfessionLearningCount(professionDO.getId());
                professionDTO.setLearnerCount((int) learningCount);

                result.add(professionDTO);
            }

            return result;

        } catch (Exception e) {
            log.error("获取热门专业失败，limit: {}", limit, e);
            throw StatusCode.PROFESSION_HOT_LIST_FAILED.exception(e);
        }
    }

    // ========== DTO转换方法 ==========

    /**
     * 转换单个对象为DTO
     */
    public ProfessionDTO toDTO(ProfessionDO professionDO) {
        return professionConverter.toDTO(professionDO);
    }

    /**
     * 转换单个对象为DTO（含收藏状态）
     */
    public ProfessionDTO toDTO(ProfessionDO professionDO, Long userId) {
        ProfessionDTO dto = professionConverter.toDTO(professionDO);

        // 填充收藏状态
        if (userId != null) {
            dto.setBookmarked(bookmarkService.isBookmarked(userId, professionDO.getId(), ContentType.profession));
        } else {
            dto.setBookmarked(false);
        }

        return dto;
    }

    /**
     * 转换列表为DTO列表
     */
    public List<ProfessionDTO> toDTO(List<ProfessionDO> professionDOList) {
        return professionConverter.toDTO(professionDOList);
    }

    /**
     * 转换列表为DTO列表（含收藏状态）
     */
    public List<ProfessionDTO> toDTO(List<ProfessionDO> professionDOList, Long userId) {
        List<ProfessionDTO> dtos = professionConverter.toDTO(professionDOList);

        // 批量填充收藏状态
        if (userId != null && !dtos.isEmpty()) {
            List<Long> ids = dtos.stream().map(ProfessionDTO::getId).collect(Collectors.toList());
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, ids, ContentType.profession);
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            dtos.forEach(dto -> dto.setBookmarked(bookmarkedSet.contains(dto.getId())));
        } else {
            dtos.forEach(dto -> dto.setBookmarked(false));
        }

        return dtos;
    }

    // ========== Private 辅助方法 ==========

    private void validateHotProfessionsLimit(int limit) {
        ValidationUtils.require(limit > 0, "限制数量必须大于0");
        ValidationUtils.require(
            limit <= systemProperties.getProfession().getMaxHotProfessionsLimit(),
            "限制数量超过最大值"
        );
    }
}
