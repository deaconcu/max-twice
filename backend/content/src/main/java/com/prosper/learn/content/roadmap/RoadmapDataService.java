package com.prosper.learn.content.roadmap;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
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

    /**
     * 根据ID查询路线图
     */
    @Cacheable(value = "roadmaps", key = "#id", unless = "#result == null")
    public RoadmapDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return roadmapMapper.getById(id);
    }

    /**
     * 批量根据ID查询路线图
     */
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

    /**
     * 批量根据ID查询路线图并转为Map
     */
    public Map<Long, RoadmapDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(RoadmapDO::getId, Function.identity()));
    }

    /**
     * 统计公开路线图数量
     */
    public Long countPublicRoadmaps() {
        return roadmapMapper.countPublicRoadmaps();
    }

    /**
     * 根据状态查询路线图列表
     */
    public List<RoadmapDO> listByState(Byte state, Long lastId, int limit) {
        return roadmapMapper.listByState(state, lastId, limit);
    }

    /**
     * 高级筛选路线图列表
     */
    public List<RoadmapDO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId, int limit) {
        return roadmapMapper.listByFilter(roadmapId, roleId, creatorId, lastId, limit);
    }

    /**
     * 根据角色获取路线图列表（支持动态排序）
     */
    public List<RoadmapDO> getListByRoleOrderBy(long roleId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByRoleOrderByLatest(roleId, limit);
        } else {
            return roadmapMapper.getListByRoleOrderByScore(roleId, limit);
        }
    }

    /**
     * 根据角色分页获取路线图列表（支持动态排序，使用游标）
     */
    public List<RoadmapDO> getListByRoleAfterCursorOrderBy(long roleId, Double lastScore,
                                                           LocalDateTime lastCreatedAt, long lastId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByRoleAfterCreatedAt(roleId, lastCreatedAt, lastId, limit);
        } else {
            return roadmapMapper.getListByRoleAfterScore(roleId, lastScore, lastId, limit);
        }
    }

    /**
     * 根据创建者获取路线图列表（支持分页）
     */
    public List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, Byte state) {
        return roadmapMapper.getListByCreatorWithPaging(creatorId, lastId, limit, state);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证路线图ID并获取路线图
     */
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

    /**
     * 验证路线图存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入路线图
     */
    public void insert(RoadmapDO roadmapDO) {
        roadmapMapper.insert(roadmapDO);
    }

    /**
     * 更新路线图
     */
    @CacheEvict(value = "roadmaps", key = "#roadmap.id")
    public void update(RoadmapDO roadmap) {
        if (roadmap == null || roadmap.getId() == null) {
            throw new IllegalArgumentException("Roadmap or roadmap ID cannot be null");
        }
        roadmapMapper.update(roadmap);
    }

    /**
     * 批准路线图
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int approve(long id) {
        return roadmapMapper.updateStateAndReason(id, Enums.ContentState.PUBLISHED.value(), "");
    }

    /**
     * 拒绝路线图
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int reject(long id, String reason) {
        return roadmapMapper.updateStateAndReason(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁路线图
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int ban(long id, String reason) {
        return roadmapMapper.updateStateAndReason(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 软删除路线图
     */
    @CacheEvict(value = "roadmaps", key = "#id")
    public int softDelete(long id) {
        return roadmapMapper.softDelete(id);
    }
}
