package com.prosper.learn.content.toc;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
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
    public void incrRef(String hash, int n) {
        courseTocMapper.incrRef(hash, n);
    }

    /**
     * 验证并获取课程目录
     *
     * @param hash 目录hash
     * @return 课程目录实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当目录不存在时抛出 COURSE_TOC_NOT_FOUND (1803)
     */
    @Override
    public CourseTocDO validateAndGet(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("目录hash不能为空");
        }

        CourseTocDO toc = getById(hash);
        if (toc == null) {
            throw StatusCode.COURSE_TOC_NOT_FOUND.exception();
        }

        return toc;
    }

    /**
     * 根据hash获取目录
     */
    public CourseTocDO get(String hash) {
        return courseTocMapper.get(hash);
    }

    /**
     * 插入目录
     */
    public void insert(CourseTocDO courseTocDO) {
        courseTocMapper.insert(courseTocDO);
    }

    /**
     * 根据hash数组获取目录映射
     */
    public Map<String, CourseTocDO> getByHashes(String[] hashes) {
        return courseTocMapper.getByHashes(hashes);
    }

    @Override
    protected Map<String, CourseTocDO> getMapByIdsFromMapper(CourseTocMapper mapper, Collection<String> ids) {
        // 可以使用现有的getByHashes方法
        String[] hashArray = ids.toArray(new String[0]);
        return courseTocMapper.getByHashes(hashArray);
    }

    @Override
    protected int deleteByIdFromMapper(CourseTocMapper mapper, String id) {
        return 0;
    }
}