package com.twicemax.content.roadmap;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 路线图数据服务
 * 负责路线图数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapDataService {

    private final RoadmapMapper roadmapMapper;

    // ==================== 查询方法 ====================

    @Cacheable(value = "roadmaps", key = "#id", unless = "#result == null")
    public RoadmapDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return roadmapMapper.getById(id);
    }

    public List<RoadmapDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return roadmapMapper.getByIds(validIds);
    }

    public Map<Long, RoadmapDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(RoadmapDO::getId, Function.identity()));
    }

    public Long countPublicRoadmaps() {
        return roadmapMapper.countPublicRoadmaps();
    }

    /**
     * 根据状态查询路线图列表（state 为 RoadmapState 字符串枚举值）。
     */
    public List<RoadmapDO> listByState(String state, Long lastId, int limit) {
        return roadmapMapper.listByState(state, lastId, limit);
    }

    public List<RoadmapDO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId, int limit) {
        return roadmapMapper.listByFilter(roadmapId, roleId, creatorId, lastId, limit);
    }

    public List<RoadmapDO> getListByRoleOrderBy(long roleId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByRoleOrderByLatest(roleId, limit);
        } else {
            return roadmapMapper.getListByRoleOrderByScore(roleId, limit);
        }
    }

    public List<RoadmapDO> getListByRoleAfterCursorOrderBy(long roleId, Double lastScore,
                                                           LocalDateTime lastCreatedAt, long lastId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByRoleAfterCreatedAt(roleId, lastCreatedAt, lastId, limit);
        } else {
            return roadmapMapper.getListByRoleAfterScore(roleId, lastScore, lastId, limit);
        }
    }

    /**
     * 根据创建者获取路线图列表（state 为 RoadmapState 字符串枚举值，可为 null）。
     */
    public List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, String state) {
        return roadmapMapper.getListByCreatorWithPaging(creatorId, lastId, limit, state);
    }

    // ==================== 验证方法 ====================

    public RoadmapDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("路线图ID无效");
        }
        RoadmapDO roadmap = getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        return roadmap;
    }

    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    public void insert(RoadmapDO roadmapDO) {
        roadmapMapper.insert(roadmapDO);
    }

    @CacheEvict(value = "roadmaps", key = "#roadmap.id")
    public void update(RoadmapDO roadmap) {
        if (roadmap == null || roadmap.getId() == null) {
            throw new IllegalArgumentException("Roadmap or roadmap ID cannot be null");
        }
        roadmapMapper.update(roadmap);
    }

    @CacheEvict(value = "roadmaps", key = "#id")
    public int updateScore(long id, double score) {
        return roadmapMapper.updateScore(id, score);
    }

    /**
     * 仅更新草稿（save-draft）。
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int updateDraft(long id, String draftContent, LocalDateTime draftUpdatedAt, String description) {
        return roadmapMapper.updateDraft(id, draftContent, draftUpdatedAt, description);
    }

    /**
     * 切换 pending_revision_id（提交 / 撤回 / 驳回 时用）。
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int updatePending(long id, Long pendingRevisionId, String draftContent, LocalDateTime draftUpdatedAt) {
        return roadmapMapper.updatePending(id, pendingRevisionId, draftContent, draftUpdatedAt);
    }

    /**
     * 审核通过：state=PUBLISHED，更新 content/hash/nodeCount/current_revision_id，pending 清空。
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int approve(long id, String content, String contentHash, Integer nodeCount, long currentRevisionId) {
        return roadmapMapper.approve(id, content, contentHash, nodeCount, currentRevisionId);
    }

    /**
     * 封禁：state=BANNED，pending 清空，回填 draft。
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int ban(long id, String draftContent, LocalDateTime draftUpdatedAt) {
        return roadmapMapper.ban(id, draftContent, draftUpdatedAt);
    }

    /**
     * 简单状态切换（解封：恢复到 PUBLISHED 或 NEVER_PUBLISHED）。
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int updateState(long id, String state) {
        return roadmapMapper.updateState(id, state);
    }

    @CacheEvict(value = "roadmaps", key = "#id")
    public int softDelete(long id) {
        return roadmapMapper.softDelete(id);
    }
}
