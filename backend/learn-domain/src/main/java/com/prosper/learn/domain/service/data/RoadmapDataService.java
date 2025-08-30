package com.prosper.learn.domain.service.data;

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
            throw new RuntimeException("Failed to update roadmap: " + roadmap.getId(), e);
        }
    }
}