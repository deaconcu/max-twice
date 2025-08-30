package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.CourseTocDO;
import com.prosper.learn.persistence.mapper.CourseTocMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 课程目录数据服务
 */
@Service
public class CourseTocDataService extends AbstractDataService<CourseTocDO, CourseTocMapper, String> {

    @Autowired
    private CourseTocMapper courseTocMapper;

    @Override
    protected CourseTocMapper mapper() {
        return courseTocMapper;
    }

    @Override
    protected String getCacheName() {
        return "courseTocs";
    }

    @Override
    protected String getEntityName() {
        return "CourseToc";
    }

    @Override
    protected String getEntityId(CourseTocDO entity) {
        return entity.getHash();
    }

    @Override
    protected CourseTocDO getByIdFromMapper(CourseTocMapper mapper, String id) {
        return courseTocMapper.get(id);
    }

    @Override
    protected List<CourseTocDO> getByIdsFromMapper(CourseTocMapper mapper, Collection<String> ids) {
        return List.of(); // CourseTocMapper没有按String集合批量查询的方法
    }

    @CacheEvict(value = "courseTocs", key = "#hash")
    void incrRef(String hash, int n) {
        courseTocMapper.incrRef(hash, n);
    }

    @Override
    protected Map<String, CourseTocDO> getMapByIdsFromMapper(CourseTocMapper mapper, Collection<String> ids) {
        // 可以使用现有的getByHashes方法
        String[] hashArray = ids.toArray(new String[0]);
        return courseTocMapper.getByHashes(hashArray);
    }
}