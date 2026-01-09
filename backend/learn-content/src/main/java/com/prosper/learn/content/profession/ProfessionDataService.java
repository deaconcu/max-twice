package com.prosper.learn.content.profession;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
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
 * 专业数据服务，提供缓存功能
 */
@Slf4j
@Service
public class ProfessionDataService extends AbstractDataService<ProfessionDO, ProfessionMapper, Long> {
    
    @Autowired
    private ProfessionMapper professionMapper;
    
    @Override
    protected ProfessionMapper mapper() {
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

    @Override
    protected int deleteByIdFromMapper(ProfessionMapper mapper, Long id) {
        return 0;
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
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 统计活跃职业数量
     */
    public Long countActiveProfessions() {
        return professionMapper.countActiveProfessions();
    }
    
    /**
     * 根据状态和最后ID查询
     */
    public List<ProfessionDO> listByStateAndLastId(byte state, Long lastId, int limit) {
        return professionMapper.listByStateAndLastId(state, lastId, limit);
    }

    /**
     * 根据主分类和最后ID查询
     */
    public List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return professionMapper.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    /**
     * 根据子分类和最后ID查询
     */
    public List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return professionMapper.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    /**
     * 根据主分类、子分类和最后ID查询
     */
    public List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return professionMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    /**
     * 插入新职业
     */
    public void insert(ProfessionDO professionDO) {
        professionMapper.insert(professionDO);
    }

    /**
     * 搜索职业（按关键词）
     */
    public List<ProfessionDO> searchByKeyword(String keyword) {
        return professionMapper.searchByKeyword(keyword);
    }

    /**
     * 审批通过职业
     */
    public int approve(long id) {
        return professionMapper.updateState(id, Enums.ContentState.PUBLISHED.value(), "");
    }

    /**
     * 拒绝职业申请
     */
    public int reject(long id, String reason) {
        return professionMapper.updateState(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁职业
     */
    public int ban(long id, String reason) {
        return professionMapper.updateState(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 删除职业
     */
    public void delete(long id) {
        professionMapper.delete(id);
    }

    /**
     * 重写父类方法，抛出 PROFESSION_NOT_FOUND 而不是通用的 NOT_FOUND
     */
    @Override
    public ProfessionDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
        ProfessionDO profession = getById(id);
        if (profession == null) {
            throw StatusCode.PROFESSION_NOT_FOUND.exception();
        }
        return profession;
    }
}