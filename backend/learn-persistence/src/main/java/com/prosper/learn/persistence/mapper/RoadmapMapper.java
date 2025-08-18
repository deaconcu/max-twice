package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.RoadmapDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface RoadmapMapper {

    @Select("SELECT * FROM roadmap WHERE id = #{id}")
    RoadmapDO get(@Param("id") int id);

    @Select("SELECT * FROM roadmap ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getList(int offset, int limit);

    @Select("SELECT * FROM roadmap WHERE profession_id = #{professionId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getListByProfession(int professionId, int offset, int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE id IN ",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             " ORDER BY created_at DESC",
             "</script>"})
    List<RoadmapDO> getByIds(@Param("ids") List<Integer> ids);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId}",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY created_at DESC LIMIT #{offset}, #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionExcluding(@Param("professionId") int professionId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit,
                                                    @Param("excludeIds") List<Integer> excludeIds);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND id &lt; #{lastId}",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionAfterIdExcluding(@Param("professionId") int professionId,
                                                          @Param("lastId") int lastId,
                                                          @Param("limit") int limit,
                                                          @Param("excludeIds") List<Integer> excludeIds);

    @Select("SELECT * FROM roadmap WHERE creator_id = #{creatorId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<RoadmapDO> getListByCreator(int creatorId, int offset, int limit);

    @Select("SELECT * FROM roadmap WHERE content_hash = #{contentHash}")
    List<RoadmapDO> getByContentHash(@Param("contentHash") String contentHash);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO roadmap(content, content_hash, description, profession_id, creator_id, vote, comment) VALUES (#{content}, #{contentHash}, #{description}, #{professionId}, #{creatorId}, #{vote}, #{comment})")
    int insert(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET content = #{content}, content_hash = #{contentHash}, description = #{description}, vote = #{vote}, comment = #{comment}, updated_at = #{updatedAt} where id = #{id}")
    @MapKey("id")
    void update(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET vote = vote + #{delta} WHERE id = #{id} AND (vote + #{delta}) >= 0")
    int updateVoteCount(@Param("id") int id, @Param("delta") int delta);

    @Update("UPDATE roadmap SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(@Param("id") int id, @Param("score") Double score);

    @Select("SELECT * FROM roadmap WHERE id IN " +
            "(SELECT DISTINCT post_id FROM upvote WHERE post_type = 'roadmap') " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByScore(@Param("limit") int limit);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId}",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY score DESC, id DESC LIMIT #{offset}, #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionExcludingOrderByScore(@Param("professionId") int professionId,
                                                              @Param("offset") int offset,
                                                              @Param("limit") int limit,
                                                              @Param("excludeIds") List<Integer> excludeIds);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE profession_id = #{professionId} AND ",
             "(score &lt; #{lastScore} OR (score = #{lastScore} AND id &lt; #{lastId}))",
             "<if test='excludeIds != null and excludeIds.size() > 0'>",
             " AND id NOT IN ",
             "<foreach item='id' collection='excludeIds' open='(' separator=',' close=')'>#{id}</foreach>",
             "</if>",
             " ORDER BY score DESC, id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByProfessionAfterScoreExcluding(@Param("professionId") int professionId,
                                                            @Param("lastScore") Double lastScore,
                                                            @Param("lastId") int lastId,
                                                            @Param("limit") int limit,
                                                            @Param("excludeIds") List<Integer> excludeIds);
}
