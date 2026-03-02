package com.prosper.learn.content.roadmap;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 路线图数据服务，提供缓存功能
 */
@Slf4j
@Service
public class RoadmapDataService extends AbstractDataService<RoadmapDO, RoadmapMapper, Long> {
    
    @Autowired
    private RoadmapMapper roadmapMapper;
    
    @Override
    protected RoadmapMapper mapper() {
        return roadmapMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "roadmaps";
    }
    
    @Override
    protected String getEntityName() {
        return "Roadmap";
    }
    
    @Override
    protected Long getEntityId(RoadmapDO entity) {
        return entity.getId();
    }
    
    @Override
    protected RoadmapDO getByIdFromMapper(RoadmapMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<RoadmapDO> getByIdsFromMapper(RoadmapMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }
    
    @Override
    protected Map<Long, RoadmapDO> getMapByIdsFromMapper(RoadmapMapper mapper, Collection<Long> ids) {
        return getByIdsFromMapper(mapper, ids).stream()
                .collect(Collectors.toMap(RoadmapDO::getId, Function.identity()));
    }
    
    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(20);  // 路线图相对稳定，较长缓存时间
    }

    @Override
    protected int deleteByIdFromMapper(RoadmapMapper mapper, Long id) {
        return 0;
    }

    /**
     * 更新路线图并清除缓存
     */
    @CacheEvict(value = "roadmaps", key = "#roadmap.id")
    public void update(RoadmapDO roadmap) {
        if (roadmap == null || roadmap.getId() == null) {
            throw new IllegalArgumentException("Roadmap or roadmap ID cannot be null");
        }
        
        try {
            roadmapMapper.update(roadmap);
            log.debug("Updated roadmap {}", roadmap.getId());
        } catch (Exception e) {
            log.error("Error updating roadmap: {}", roadmap.getId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 统计公开路线图数量
     */
    public Long countPublicRoadmaps() {
        return roadmapMapper.countPublicRoadmaps();
    }

    /**
     * 插入新路线图
     */
    public void insert(RoadmapDO roadmapDO) {
        roadmapMapper.insert(roadmapDO);
    }

    /**
     * 更新投票数
     */
    /*
    @CacheEvict(value = "roadmaps", key = "#roadmapId")
    public void updateVoteCount(Long roadmapId, int voteDelta) {
        roadmapMapper.updateVoteCount(roadmapId, voteDelta);
    }
     */

    /**
     * 根据职业获取路线图列表（支持动态排序）
     */
    public List<RoadmapDO> getListByProfessionOrderBy(long professionId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByProfessionOrderByLatest(professionId, limit);
        } else {
            return roadmapMapper.getListByProfessionOrderByScore(professionId, limit);
        }
    }

    /**
     * 根据职业分页获取路线图列表（支持动态排序，使用游标）
     */
    public List<RoadmapDO> getListByProfessionAfterCursorOrderBy(long professionId, Double lastScore,
            LocalDateTime lastCreatedAt, long lastId, int limit, String sortBy) {
        if ("latest".equals(sortBy)) {
            return roadmapMapper.getListByProfessionAfterCreatedAt(professionId, lastCreatedAt, lastId, limit);
        } else {
            return roadmapMapper.getListByProfessionAfterScore(professionId, lastScore, lastId, limit);
        }
    }

    /**
     * 根据创建者获取路线图列表（支持分页）
     */
    public List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, Byte state) {
        return roadmapMapper.getListByCreatorWithPaging(creatorId, lastId, limit, state);
    }

    /**
     * Admin管理：按状态查询路线图列表
     */
    public List<RoadmapDO> listByState(Byte state, Long lastId, int limit) {
        return roadmapMapper.listByState(state, lastId, limit);
    }

    /**
     * Admin管理：高级筛选路线图列表
     */
    public List<RoadmapDO> listByFilter(Long roadmapId, Long professionId, Long creatorId, Long lastId, int limit) {
        return roadmapMapper.listByFilter(roadmapId, professionId, creatorId, lastId, limit);
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

    /**
     * 重写父类方法，抛出 ROADMAP_NOT_FOUND 而不是通用的 NOT_FOUND
     */
    @Override
    public RoadmapDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
        RoadmapDO roadmap = getById(id);
        if (roadmap == null) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        return roadmap;
    }
}