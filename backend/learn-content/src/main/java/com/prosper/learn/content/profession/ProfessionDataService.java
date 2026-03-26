package com.prosper.learn.content.profession;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 职业数据服务
 * 负责职业数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionDataService {

    private final ProfessionMapper professionMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询职业
     */
    @Cacheable(value = "professions", key = "#id", unless = "#result == null")
    public ProfessionDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return professionMapper.getById(id);
    }

    /**
     * 批量根据ID查询职业
     */
    public List<ProfessionDO> getByIds(Collection<Long> ids) {
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
        return professionMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询职业并转为Map
     */
    public Map<Long, ProfessionDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(ProfessionDO::getId, Function.identity()));
    }

    /**
     * 统计活跃职业数量
     */
    public Long countActiveProfessions() {
        return professionMapper.countActiveProfessions();
    }

    /**
     * 根据状态获取职业列表
     */
    public List<ProfessionDO> listByState(Byte state, Long lastId, int limit) {
        return professionMapper.listByState(state, lastId, limit);
    }

    /**
     * 根据主分类查询
     */
    public List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit) {
        return professionMapper.listByMainCategoryAndLastId(mainCategory, lastId, limit);
    }

    /**
     * 根据子分类查询
     */
    public List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit) {
        return professionMapper.listBySubCategoryAndLastId(subCategory, lastId, limit);
    }

    /**
     * 根据主分类和子分类查询
     */
    public List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit) {
        return professionMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId, limit);
    }

    /**
     * 搜索职业（按关键词，用户端）
     */
    public List<ProfessionDO> searchByKeyword(String keyword) {
        return professionMapper.searchByKeyword(keyword);
    }

    /**
     * 管理后台按名称搜索职业
     */
    public List<ProfessionDO> searchByName(String name, Long lastId, int limit) {
        return professionMapper.searchByName(name, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证职业ID并获取职业
     */
    public ProfessionDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("职业ID无效");
        }
        ProfessionDO profession = getById(id);
        if (profession == null) {
            throw StatusCode.PROFESSION_NOT_FOUND.exception();
        }
        return profession;
    }

    /**
     * 验证职业存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入职业
     */
    public void insert(ProfessionDO professionDO) {
        professionMapper.insert(professionDO);
    }

    /**
     * 更新职业
     */
    @CacheEvict(value = "professions", key = "#profession.id")
    public void update(ProfessionDO profession) {
        if (profession == null || profession.getId() == null) {
            throw new IllegalArgumentException("Profession or profession ID cannot be null");
        }
        professionMapper.update(profession);
    }

    /**
     * 审批通过职业
     */
    @CacheEvict(value = "professions", key = "#id")
    public int approve(long id) {
        return professionMapper.updateState(id, Enums.ContentState.PUBLISHED.value(), "");
    }

    /**
     * 拒绝职业申请
     */
    @CacheEvict(value = "professions", key = "#id")
    public int reject(long id, String reason) {
        return professionMapper.updateState(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁职业
     */
    @CacheEvict(value = "professions", key = "#id")
    public int ban(long id, String reason) {
        return professionMapper.updateState(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 删除职业
     */
    @CacheEvict(value = "professions", key = "#id")
    public void delete(long id) {
        professionMapper.delete(id);
    }
}
