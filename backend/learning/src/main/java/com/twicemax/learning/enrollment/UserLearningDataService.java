package com.twicemax.learning.enrollment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户学习记录数据服务
 */
@Service
@RequiredArgsConstructor
public class UserLearningDataService {

    private final UserLearningMapper mapper;

    /**
     * 插入学习记录
     */
    public int insert(UserLearningDO record) {
        return mapper.insert(record);
    }

    /**
     * 更新学习记录
     */
    public int update(UserLearningDO record) {
        return mapper.update(record);
    }

    /**
     * 根据用户ID和对象查询学习记录
     */
    public UserLearningDO getByUserAndObject(long userId, byte objectType, long objectId) {
        return mapper.getByUserAndObject(userId, objectType, objectId);
    }

    /**
     * 根据用户ID和对象类型查询学习记录（支持滚动分页和状态过滤）
     *
     * @param userId 用户ID
     * @param objectType 对象类型（2=node, 4=roadmap）
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 上次查询的最后一条记录ID（null 返回第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDO> getByUserAndType(long userId, byte objectType, Byte state, Long lastId, int limit) {
        return mapper.getByUserAndType(userId, objectType, state, lastId, limit);
    }

    /**
     * 根据用户ID查询所有学习记录（支持滚动分页和状态过滤）
     *
     * @param userId 用户ID
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 上次查询的最后一条记录ID（null 返回第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDO> getByUserId(long userId, Byte state, Long lastId, int limit) {
        return mapper.getByUserId(userId, state, lastId, limit);
    }

    /**
     * 批量查询学习记录
     */
    public List<UserLearningDO> batchGetByUserAndObjects(long userId, byte objectType, Collection<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return List.of();
        }
        return mapper.batchGetByUserAndObjects(userId, objectType, objectIds);
    }

    /**
     * 根据用户、对象类型和父对象ID查询学习记录
     * 用于查询：某个 role 下正在学习的 roadmap
     *
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null 返回第一页）
     */
    public List<UserLearningDO> getByUserAndTypeAndParent(long userId, byte objectType, long parentId, Byte state, Long lastId, int limit) {
        return mapper.getByUserAndTypeAndParent(userId, objectType, parentId, state, lastId, limit);
    }

    /**
     * 删除学习记录
     */
    public int delete(long id) {
        return mapper.delete(id);
    }

    /**
     * 根据用户和对象删除学习记录
     */
    public int deleteByUserAndObject(long userId, byte objectType, long objectId) {
        return mapper.deleteByUserAndObject(userId, objectType, objectId);
    }

    /**
     * 检查学习记录是否存在
     */
    public boolean exists(long userId, byte objectType, long objectId) {
        return mapper.exists(userId, objectType, objectId) > 0;
    }

    /**
     * 更新课程的节点列表
     * 用于用户切换ToC时更新节点缓存
     */
    public int updateNodes(long userId, byte objectType, long objectId, String nodes) {
        return mapper.updateNodes(userId, objectType, objectId, nodes, LocalDateTime.now());
    }

    /**
     * 查询包含指定节点的课程学习记录
     * 用于节点完成时，反向查找需要更新进度的课程
     */
    public List<UserLearningDO> findByNodeContained(long userId, long nodeId) {
        return mapper.findByNodeContained(userId, nodeId);
    }

    /**
     * 查询用户学习的所有课程（通过 course 表 JOIN）
     * 返回 objectType=node 且是课程根节点的学习记录
     *
     * @param userId 用户ID
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null=第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDO> getCoursesByUser(long userId, Byte state, Long lastId, int limit) {
        return mapper.getCoursesByUser(userId, state, lastId, limit);
    }
}
