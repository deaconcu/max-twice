package com.prosper.learn.content.roadmap;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface RoadmapMapper {

    @Select("SELECT * FROM roadmap WHERE id = #{id} AND deleted_at IS NULL")
    RoadmapDO getById(long id);

// --注释掉检查 START (2025/12/10 12:03):
//    @Select("SELECT * FROM roadmap WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
//    List<RoadmapDO> getList(int offset, int limit);
// --注释掉检查 STOP (2025/12/10 12:03)

// --注释掉检查 START (2025/12/10 12:03):
//    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} AND deleted_at IS NULL ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
//    List<RoadmapDO> getListByProfession(long professionId, int offset, int limit);
// --注释掉检查 STOP (2025/12/10 12:03)

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE id IN ",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             " AND deleted_at IS NULL ORDER BY created_at DESC",
             "</script>"})
    List<RoadmapDO> getByIds(List<Long> ids);

// --注释掉检查 START (2025/12/10 12:03):
//    @Select({"<script>",
//             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND deleted_at IS NULL",
//             "<if test='excludeIds != null and excludeIds.size() > 0'>",
//             " AND id NOT IN ",
//             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
//             "</if>",
//             " ORDER BY created_at DESC LIMIT #{offset}, #{limit}",
//             "</script>"})
//    List<RoadmapDO> getListByProfessionExcluding(long professionId, int offset, int limit, List<Long> excludeIds);
// --注释掉检查 STOP (2025/12/10 12:03)

// --注释掉检查 START (2025/12/10 12:03):
//    @Select({"<script>",
//             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND id &lt; #{lastId} AND deleted_at IS NULL",
//             "<if test='excludeIds != null and excludeIds.size() > 0'>",
//             " AND id NOT IN ",
//             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
//             "</if>",
//             " ORDER BY id DESC LIMIT #{limit}",
//             "</script>"})
//    List<RoadmapDO> getListByProfessionAfterIdExcluding(
//            long professionId, long lastId, int limit, List<Long> excludeIds);
// --注释掉检查 STOP (2025/12/10 12:03)

    @Select("SELECT * FROM roadmap WHERE creator_id = #{creatorId} AND deleted_at IS NULL ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getListByCreator(long creatorId, int offset, int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE creator_id = #{creatorId} AND deleted_at IS NULL",
             "<if test='state != null'> AND state = #{state}</if>",
             "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
             " ORDER BY id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, Byte state);

// --注释掉检查 START (2025/12/10 12:03):
//    @Select("SELECT * FROM roadmap WHERE content_hash = #{contentHash} AND deleted_at IS NULL")
//    List<RoadmapDO> getByContentHash(String contentHash);
// --注释掉检查 STOP (2025/12/10 12:03)

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO roadmap(content, content_hash, description, profession_id, creator_id, node_count, state) " +
            "VALUES (#{content}, #{contentHash}, #{description}, #{professionId}, #{creatorId}, #{nodeCount}, " + ContentState.SUBMITTED_VALUE + ")")
    int insert(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET content = #{content}, content_hash = #{contentHash}, description = #{description}, " +
            "node_count = #{nodeCount}, state = #{state} where id = #{id}")
    @MapKey("id")
    void update(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

// --注释掉检查 START (2025/12/10 12:03):
//    @Select("SELECT * FROM roadmap WHERE id IN " +
//            "(SELECT DISTINCT post_id FROM upvote WHERE post_type = 'roadmap') " +
//            "AND deleted_at IS NULL ORDER BY score DESC, id DESC LIMIT #{limit}")
//    List<RoadmapDO> getListByScore(int limit);
// --注释掉检查 STOP (2025/12/10 12:03)

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
    @Select("SELECT COUNT(*) FROM roadmap WHERE vote >= 0 AND deleted_at IS NULL")
    Long countPublicRoadmaps();

    // Admin管理接口
    @Select("<script>" +
            "SELECT * FROM roadmap WHERE deleted_at IS NULL " +
            "<if test='state != null'>AND state = #{state}</if> " +
            "<if test='professionId != null'>AND profession_id = #{professionId}</if> " +
            "<if test='creatorId != null'>AND creator_id = #{creatorId}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<RoadmapDO> listByFilter(Byte state, Long professionId, Long creatorId, Long lastId);

    @Update("UPDATE roadmap SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    int updateStateAndReason(@Param("id") long id, @Param("state") byte state, @Param("reason") String reason);

    @Update("UPDATE roadmap SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(long id);
}
