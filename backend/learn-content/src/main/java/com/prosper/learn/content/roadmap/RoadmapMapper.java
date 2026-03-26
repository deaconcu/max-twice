package com.prosper.learn.content.roadmap;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface RoadmapMapper {

    @Select("SELECT * FROM roadmap WHERE id = #{id} AND deleted_at IS NULL")
    RoadmapDO getById(long id);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE id IN ",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             " AND deleted_at IS NULL ORDER BY created_at DESC",
             "</script>"})
    List<RoadmapDO> getByIds(List<Long> ids);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE creator_id = #{creatorId} AND deleted_at IS NULL",
             "<if test='state != null'> AND state = #{state}</if>",
             "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId}</if>",
             " ORDER BY id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, Byte state);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO roadmap(content, content_hash, description, profession_id, creator_id, node_count, state, score) " +
            "VALUES (#{content}, #{contentHash}, #{description}, #{professionId}, #{creatorId}, #{nodeCount}, #{state}, #{score})")
    int insert(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET content = #{content}, content_hash = #{contentHash}, description = #{description}, " +
            "node_count = #{nodeCount}, state = #{state} where id = #{id}")
    @MapKey("id")
    void update(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

    /**
     * 根据职业获取路线图列表（按创建时间排序）
     */
    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByProfessionOrderByLatest(
            @Param("professionId") long professionId,
            @Param("limit") int limit);

    /**
     * 根据职业获取路线图列表（按分数排序）
     */
    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByProfessionOrderByScore(
            @Param("professionId") long professionId,
            @Param("limit") int limit);

    /**
     * 分页获取职业路线图（按创建时间排序）
     */
    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "AND (created_at < #{lastCreatedAt} OR (created_at = #{lastCreatedAt} AND id < #{lastId})) " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByProfessionAfterCreatedAt(
            @Param("professionId") long professionId,
            @Param("lastCreatedAt") java.time.LocalDateTime lastCreatedAt,
            @Param("lastId") long lastId,
            @Param("limit") int limit);

    /**
     * 分页获取职业路线图（按分数排序）
     */
    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "AND (score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByProfessionAfterScore(
            @Param("professionId") long professionId,
            @Param("lastScore") Double lastScore,
            @Param("lastId") long lastId,
            @Param("limit") int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY score DESC, id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionExcludingOrderByScore(
            long professionId, int limit, List<Long> excludeIds);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL AND ",
             "(score &lt; #{lastScore} OR (score = #{lastScore} AND id &lt; #{lastId}))",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY score DESC, id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionAfterScoreExcluding(
            long professionId, double lastScore, long lastId, int limit, List<Long> excludeIds);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM roadmap WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL")
    Long countPublicRoadmaps();

    // Admin管理接口 - 按状态查询
    @Select("<script>" +
            "SELECT * FROM roadmap WHERE deleted_at IS NULL " +
            "<if test='state != null'>AND state = #{state}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoadmapDO> listByState(Byte state, Long lastId, @Param("limit") int limit);

    // Admin管理接口 - 高级筛选
    @Select("<script>" +
            "SELECT * FROM roadmap WHERE deleted_at IS NULL " +
            "<if test='roadmapId != null'>AND id = #{roadmapId}</if> " +
            "<if test='professionId != null'>AND profession_id = #{professionId}</if> " +
            "<if test='creatorId != null'>AND creator_id = #{creatorId}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoadmapDO> listByFilter(Long roadmapId, Long professionId, Long creatorId, Long lastId, @Param("limit") int limit);

    @Update("UPDATE roadmap SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    int updateStateAndReason(@Param("id") long id, @Param("state") byte state, @Param("reason") String reason);

    @Update("UPDATE roadmap SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(long id);
}
