package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserProgressDO;
import org.apache.ibatis.annotations.*;

/**
 * 用户学习记录Mapper接口
 */
@Mapper
public interface UserProgressMapper {

    /**
     * 根据用户ID查询学习记录
     */
    @Select("SELECT * FROM user_progress WHERE user_id = #{userId}")
    UserProgressDO getByUserId(Integer userId);

    /**
     * 插入新的学习记录
     */
    @Insert("INSERT INTO user_progress(user_id, node_ids, count, created_at, updated_at) " +
            "VALUES (#{userId}, #{nodeIds}, #{count}, NOW(), NOW())")
    int insert(UserProgressDO record);

    /**
     * 更新学习记录
     */
    @Update("UPDATE user_progress SET " +
            "node_ids = #{nodeIds}, " +
            "count = #{count}, " +
            "updated_at = NOW() " +
            "WHERE user_id = #{userId}")
    int update(UserProgressDO record);

    /**
     * 插入或更新学习记录（upsert操作）
     */
    @Insert("INSERT INTO user_progress(user_id, node_ids, count, created_at, updated_at) " +
            "VALUES (#{userId}, #{nodeIds}, #{count}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "node_ids = VALUES(node_ids), " +
            "count = VALUES(count), " +
            "updated_at = NOW()")
    int upsert(UserProgressDO record);

    /**
     * 只获取用户完成的节点ID字符串
     */
    @Select("SELECT node_ids FROM user_progress WHERE user_id = #{userId}")
    String getCompletedNodeIds(Integer userId);

    /**
     * 获取用户完成节点总数
     */
    @Select("SELECT count FROM user_progress WHERE user_id = #{userId}")
    Integer getCompletedCount(Integer userId);
}