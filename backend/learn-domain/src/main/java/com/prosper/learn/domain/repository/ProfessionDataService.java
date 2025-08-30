package com.prosper.learn.domain.repository;

import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.mapper.ProfessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 专业数据服务，提供缓存功能
 */
@Slf4j
@Service
public class ProfessionDataService extends AbstractDataService<ProfessionDO, ProfessionMapper> {
    
    @Autowired
    private ProfessionMapper professionMapper;
    
    @Override
    protected ProfessionMapper getMapper() {
        return professionMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "professions";
    }
    
    @Override
    protected String getEntityName() {
        return "Profession";
    }
    
    @Override
    protected Long getEntityId(ProfessionDO entity) {
        return entity.getId();
    }
    
    @Override
    protected ProfessionDO getByIdFromMapper(ProfessionMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<ProfessionDO> getByIdsFromMapper(ProfessionMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }
    
    @Override
    protected Map<Long, ProfessionDO> getMapByIdsFromMapper(ProfessionMapper mapper, Collection<Long> ids) {
        return getByIdsFromMapper(mapper, ids).stream()
                .collect(Collectors.toMap(ProfessionDO::getId, Function.identity()));
    }
    
    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(60);  // 专业信息变化很少，长缓存时间
    }
    
    /**
     * 更新专业并清除缓存
     */
    @CacheEvict(value = "professions", key = "#profession.id")
    public void update(ProfessionDO profession) {
        if (profession == null || profession.getId() == null) {
            throw new IllegalArgumentException("Profession or profession ID cannot be null");
        }
        
        try {
            professionMapper.update(profession);
            log.debug("Updated profession {}", profession.getId());
        } catch (Exception e) {
            log.error("Error updating profession: {}", profession.getId(), e);
            throw new RuntimeException("Failed to update profession: " + profession.getId(), e);
        }
    }
}