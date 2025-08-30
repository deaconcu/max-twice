package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.SystemDO;
import com.prosper.learn.persistence.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 系统配置数据服务
 */
@Service
public class SystemDataService extends AbstractDataService<SystemDO, SystemMapper, Long> {

    @Autowired
    private SystemMapper systemMapper;

    @Override
    protected SystemMapper mapper() {
        return systemMapper;
    }

    @Override
    protected String getCacheName() {
        return "system";
    }

    @Override
    protected String getEntityName() {
        return "System";
    }

    @Override
    protected Long getEntityId(SystemDO entity) {
        return entity.getId();
    }

    @Override
    protected SystemDO getByIdFromMapper(SystemMapper mapper, Long id) {
        return systemMapper.get(id);
    }

    @Override
    protected List<SystemDO> getByIdsFromMapper(SystemMapper mapper, Collection<Long> ids) {
        return List.of(); // SystemMapper没有批量查询方法
    }

    @Override
    protected Map<Long, SystemDO> getMapByIdsFromMapper(SystemMapper mapper, Collection<Long> ids) {
        return Map.of(); // SystemMapper没有批量查询方法
    }

    /**
     * 更新系统配置
     */
    @CacheEvict(value = "system", key = "#systemDO.id")
    public void update(SystemDO systemDO) {
        systemMapper.update(systemDO);
    }
}