package com.prosper.learn.learning.enrollment;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户路线图数据服务
 */
@Service
public class UserRoadmapDataService extends AbstractDataService<UserRoadmapDO, UserRoadmapMapper, Long> {

    @Autowired
    private UserRoadmapMapper userRoadmapMapper;

    @Autowired
    private SystemProperties systemProperties;

    @Override
    protected UserRoadmapMapper mapper() {
        return userRoadmapMapper;
    }

    @Override
    protected String getCacheName() {
        return "userRoadmaps";
    }

    @Override
    protected String getEntityName() {
        return "UserRoadmap";
    }

    @Override
    protected Long getEntityId(UserRoadmapDO entity) {
        return entity.getId();
    }

    @Override
    protected UserRoadmapDO getByIdFromMapper(UserRoadmapMapper mapper, Long id) {
        return null; // UserRoadmapMapper没有getById方法
    }

    @Override
    protected List<UserRoadmapDO> getByIdsFromMapper(UserRoadmapMapper mapper, Collection<Long> ids) {
        return List.of(); // UserRoadmapMapper没有批量按ID查询方法
    }

    @Override
    protected Map<Long, UserRoadmapDO> getMapByIdsFromMapper(UserRoadmapMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserRoadmapMapper没有批量按ID查询方法
    }

    @Override
    protected int deleteByIdFromMapper(UserRoadmapMapper mapper, Long id) {
        return 0;
    }

    /**
     * 根据用户ID和路线图ID查询学习进度
     */
    @Cacheable(value = "userRoadmapByUserAndRoadmap", key = "#userId + '_' + #roadmapId")
    public UserRoadmapDO getByUserAndRoadmap(long userId, long roadmapId) {
        return userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);
    }

    /**
     * 更新学习进度
     */
    @CacheEvict(value = "userRoadmapByUserAndRoadmap", key = "#progressDO.userId + '_' + #progressDO.roadmapId")
    public void update(UserRoadmapDO progressDO) {
        userRoadmapMapper.update(progressDO);
    }

    /**
     * 删除学习进度记录
     */
    @CacheEvict(value = "userRoadmapByUserAndRoadmap", key = "#userId + '_' + #roadmapId")
    public void deleteByUserAndRoadmap(long userId, long roadmapId) {
        userRoadmapMapper.deleteByUserAndRoadmap(userId, roadmapId);
    }

    /**
     * 插入新的学习记录
     */
    public void insert(UserRoadmapDO userRoadmapDO) {
        userRoadmapMapper.insert(userRoadmapDO);
    }

    /**
     * 批量更新学习记录
     */
    public void updateBatch(List<UserRoadmapDO> userRoadmapList) {
        if (userRoadmapList == null || userRoadmapList.isEmpty()) {
            return;
        }

        // 限制批量更新数量，防止性能问题
        int maxBatchSize = systemProperties.getDataService().getMaxBatchUpdateSize();
        if (userRoadmapList.size() > maxBatchSize) {
            throw StatusCode.BATCH_SIZE_EXCEEDED.exception();
        }

        // 循环调用带缓存清理的 update 方法
        for (UserRoadmapDO userRoadmapDO : userRoadmapList) {
            update(userRoadmapDO);
        }
    }

    /**
     * 根据用户ID查询学习记录
     */
    public List<UserRoadmapDO> getByUser(long userId) {
        return userRoadmapMapper.getByUser(userId);
    }

    /**
     * 批量查询学习状态
     */
    public List<Long> getBatchLearningStatus(long userId, List<Long> roadmapIds) {
        return userRoadmapMapper.getBatchLearningStatus(userId, roadmapIds);
    }

    /**
     * 获取用户正在学习的职业路线图
     * @param userId 用户ID
     * @param professionId 职业ID
     * @param limit 最大返回数量
     * @return 正在学习的路线图列表
     */
    public List<UserRoadmapDO> getLearningByProfession(long userId, long professionId, int limit) {
        return userRoadmapMapper.getLearningByProfession(userId, professionId, limit);
    }

    /**
     * 统计用户在指定职业下正在学习的路线图数量
     * @param userId 用户ID
     * @param professionId 职业ID
     * @return 正在学习的数量
     */
    public int countLearningByProfession(long userId, long professionId) {
        return userRoadmapMapper.countLearningByProfession(userId, professionId);
    }
}