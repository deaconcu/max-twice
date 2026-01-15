package com.prosper.learn.learning.enrollment;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoadmapMapper {

    /**
     * 根据用户ID和路线图ID查询学习进度
     */
    @Select("SELECT * FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    UserRoadmapDO getByUserAndRoadmap(long userId, long roadmapId);

    /**
     * 插入新的学习进度记录
     */
    @Insert("INSERT INTO user_roadmap (user_id, roadmap_id, profession_id, progress_percent, state, started_at) " +
            "VALUES (#{userId}, #{roadmapId}, #{professionId}, #{progressPercent}, #{state}, #{startedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserRoadmapDO progressDO);

    /**
     * 根据用户ID查询所有路线图学习进度
     */
    @Select("SELECT * FROM user_roadmap WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserRoadmapDO> getByUser(long userId);

    /**
     * 更新学习进度
     */
    @Update("UPDATE user_roadmap SET progress_percent = #{progressPercent}, state = #{state}, " +
            "completed_at = #{completedAt} " +
            "WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    void update(UserRoadmapDO progressDO);

// --注释掉检查 START (2025/12/10 12:05):
//    /**
//     * 批量更新学习进度
//     */
//    @Update({"<script>",
//             "<foreach collection='list' item='item' separator=';'>",
//             "UPDATE user_roadmap SET progress_percent = #{item.progressPercent}, state = #{item.state}, " +
//             "completed_at = #{item.completedAt} " +
//             "WHERE user_id = #{item.userId} AND roadmap_id = #{item.roadmapId}",
//             "</foreach>",
//             "</script>"})
//    void updateBatch(List<UserRoadmapDO> progressList);
// --注释掉检查 STOP (2025/12/10 12:05)

    /**
     * 删除学习进度记录
     */
    @Delete("DELETE FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id = #{roadmapId}")
    void deleteByUserAndRoadmap(long userId, long roadmapId);

    /**
     * 批量查询用户对多个路线图的学习状态
     */
    @Select({"<script>",
             "SELECT roadmap_id FROM user_roadmap WHERE user_id = #{userId} AND roadmap_id IN ",
             "<foreach item='id' collection='roadmapIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</script>"})
    List<Long> getBatchLearningStatus(long userId, List<Long> roadmapIds);

    /**
     * 获取用户正在学习的职业路线图（单表查询，使用冗余字段）
     */
    @Select("SELECT * FROM user_roadmap " +
            "WHERE user_id = #{userId} AND profession_id = #{professionId} " +
            "AND state = " + Enums.UserProgressState.IN_PROGRESS_VALUE + " " +
            "ORDER BY started_at DESC LIMIT #{limit}")
    List<UserRoadmapDO> getLearningByProfession(long userId, long professionId, int limit);

    /**
     * 统计用户在指定职业下正在学习的路线图数量
     */
    @Select("SELECT COUNT(*) FROM user_roadmap " +
            "WHERE user_id = #{userId} AND profession_id = #{professionId} " +
            "AND state = " + Enums.UserProgressState.IN_PROGRESS_VALUE)
    int countLearningByProfession(long userId, long professionId);
    
// --注释掉检查 START (2025/12/10 12:05):
//    /**
//     * 统计用户正在学习的路线图数量
//     */
//    @Select("SELECT COUNT(*) FROM user_roadmap WHERE user_id = #{userId} AND state = " + Enums.UserProgressState.IN_PROGRESS_VALUE)
//    Integer countActiveRoadmapsByUserId(long userId);
// --注释掉检查 STOP (2025/12/10 12:05)
}
