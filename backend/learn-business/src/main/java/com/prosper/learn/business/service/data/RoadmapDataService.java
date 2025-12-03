package com.prosper.learn.business.service.data;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
            throw ErrorCode.DATABASE_ERROR.exception(e);
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
     * 根据职业和分数排序获取路线图列表（排除指定路线图）
     */
    public List<RoadmapDO> getListByProfessionExcludingOrderByScore(Long professionId, int limit, List<Long> excludeIds) {
        return roadmapMapper.getListByProfessionExcludingOrderByScore(professionId, limit, excludeIds);
    }

    /**
     * 根据职业和分数分页获取路线图列表（排除指定路线图）
     */
    public List<RoadmapDO> getListByProfessionAfterScoreExcluding(Long professionId, Double lastScore, Long lastId, int limit, List<Long> excludeIds) {
        return roadmapMapper.getListByProfessionAfterScoreExcluding(professionId, lastScore, lastId, limit, excludeIds);
    }

    /**
     * 根据创建者获取路线图列表（支持分页）
     */
    public List<RoadmapDO> getListByCreatorWithPaging(Long creatorId, Long lastId, int limit, Byte state) {
        if (lastId == null || lastId == 0) {
            return roadmapMapper.getListByCreator(creatorId, 0, limit);
        }
        return roadmapMapper.getListByCreatorWithPaging(creatorId, lastId, limit, state);
    }

    /**
     * Admin管理：按条件筛选路线图列表
     */
    public List<RoadmapDO> listByFilter(Byte state, Long professionId, Long creatorId, Long lastId) {
        return roadmapMapper.listByFilter(state, professionId, creatorId, lastId);
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