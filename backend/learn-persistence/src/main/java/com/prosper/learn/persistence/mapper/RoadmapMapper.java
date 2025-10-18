package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.RoadmapDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface RoadmapMapper {

    @Select("SELECT * FROM roadmap WHERE id = #{id}")
    RoadmapDO getById(long id);

    @Select("SELECT * FROM roadmap ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getList(int offset, int limit);

    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getListByProfession(long professionId, int offset, int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE id IN ",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             " ORDER BY created_at DESC",
             "</script>"})
    List<RoadmapDO> getByIds(List<Long> ids);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId}",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY created_at DESC LIMIT #{offset}, #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionExcluding(long professionId, int offset, int limit, List<Long> excludeIds);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND id &lt; #{lastId}",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionAfterIdExcluding(
            long professionId, long lastId, int limit, List<Long> excludeIds);

    @Select("SELECT * FROM roadmap WHERE creator_id = #{creatorId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getListByCreator(long creatorId, int offset, int limit);

    @Select("SELECT * FROM roadmap WHERE content_hash = #{contentHash}")
    List<RoadmapDO> getByContentHash(String contentHash);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO roadmap(content, content_hash, description, profession_id, creator_id, state) " +
            "VALUES (#{content}, #{contentHash}, #{description}, #{professionId}, #{creatorId}, 0)")
    int insert(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET content = #{content}, content_hash = #{contentHash}, description = #{description}, " +
            "vote = #{vote}, comment = #{comment}, state = #{state} where id = #{id}")
    @MapKey("id")
    void update(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET vote = vote + #{delta} WHERE id = #{id} AND (vote + #{delta}) >= 0")
    int updateVoteCount(long id, int delta);

    @Update("UPDATE roadmap SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

    @Select("SELECT * FROM roadmap WHERE id IN " +
            "(SELECT DISTINCT post_id FROM upvote WHERE post_type = 'roadmap') " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByScore(int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = 1",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY score DESC, id DESC LIMIT #{offset}, #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionExcludingOrderByScore(
            long professionId, int offset, int limit, List<Long> excludeIds);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND state = 1 AND ",
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
    @Select("SELECT COUNT(*) FROM roadmap WHERE vote >= 0")
    Long countPublicRoadmaps();

    // Admin管理接口
    @Select("<script>" +
            "SELECT * FROM roadmap WHERE 1=1 " +
            "<if test='state != null'>AND state = #{state}</if> " +
            "<if test='professionId != null'>AND profession_id = #{professionId}</if> " +
            "<if test='creatorId != null'>AND creator_id = #{creatorId}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<RoadmapDO> listByFilter(Byte state, Long professionId, Long creatorId, Long lastId);

    @Update("UPDATE roadmap SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") long id, @Param("state") byte state);
}
