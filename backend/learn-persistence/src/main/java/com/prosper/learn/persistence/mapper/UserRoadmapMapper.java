package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoadmapMapper {

    /**
     * 根据用户ID和路线图ID查询学习进度
     */
    @Select("SELECT * FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    UserRoadmapDO getByUserAndRoadmap(@Param("userId") Long userId, @Param("roadmapId") Long roadmapId);

    /**
     * 插入新的学习进度记录
     */
    @Insert("INSERT INTO user_roadmap (user_id, roadmap_id, progress_percent, status, started_at) " +
            "VALUES (#{userId}, #{roadmapId}, #{progressPercent}, #{status}, #{startedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserRoadmapDO progressDO);

    /**
     * 根据用户ID查询所有路线图学习进度
     */
    @Select("SELECT * FROM user_roadmap WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserRoadmapDO> getByUser(@Param("userId") Long userId);

    /**
     * 更新学习进度
     */
    @Update("UPDATE user_roadmap SET progress_percent = #{progressPercent}, status = #{status}, " +
            "completed_at = #{completedAt}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    void update(UserRoadmapDO progressDO);

    /**
     * 批量更新学习进度
     */
    @Update({"<script>",
             "<foreach collection='list' item='item' separator=';'>",
             "UPDATE user_roadmap SET progress_percent = #{item.progressPercent}, status = #{item.status}, " +
             "completed_at = #{item.completedAt}, updated_at = CURRENT_TIMESTAMP " +
             "WHERE user_id = #{item.userId} AND roadmap_id = #{item.roadmapId}",
             "</foreach>",
             "</script>"})
    void updateBatch(List<UserRoadmapDO> progressList);

    /**
     * 删除学习进度记录
     */
    @Delete("DELETE FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    void deleteByUserAndRoadmap(@Param("userId") Long userId, @Param("roadmapId") Long roadmapId);

    /**
     * 批量查询用户对多个路线图的学习状态
     */
    @Select({"<script>",
             "SELECT roadmap_id FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id IN ",
             "<foreach item='id' collection='roadmapIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</script>"})
    List<Integer> getBatchLearningStatus(@Param("userId") Long userId, @Param("roadmapIds") List<Integer> roadmapIds);
    
    /**
     * 统计用户正在学习的路线图数量
     */
    @Select("SELECT COUNT(*) FROM user_roadmap WHERE user_id = #{userId} AND status = 'IN_PROGRESS'")
    Integer countActiveRoadmapsByUserId(Long userId);
}
