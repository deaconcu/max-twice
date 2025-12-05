package com.prosper.learn.content.post;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

public interface PostMapper {

    @Select("SELECT * FROM post WHERE id = #{id} AND deleted_at IS NULL")
    PostDO get(long id);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL</script>"})
    List<PostDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL</script>"})
    @MapKey("id")
    Map<Long, PostDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM post " +
            "WHERE node_id = #{nodeId} and state = #{state} AND deleted_at IS NULL " +
            "order by created_at desc limit #{limit}")
    List<PostDO> getListByNode(long nodeId, int limit, byte state);

    @Select("SELECT * FROM post " +
            "WHERE node_id = #{nodeId} and id < #{lastId} and state = #{state} AND deleted_at IS NULL " +
            "order by id desc limit #{limit}")
    List<PostDO> getListByLastId(long nodeId, long lastId, int limit, byte state);

    @Select({"<script>",
            "SELECT * FROM post WHERE creator_id = #{userId} and type = #{type}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "AND deleted_at IS NULL",
            "<if test='state != null'> AND state = #{state}</if>",
            "ORDER BY id DESC LIMIT #{count}",
            "</script>"})
    List<PostDO> getPostsByUser(@Param("userId") long userId, @Param("type") int type, @Param("lastId") Long lastId, @Param("state") Byte state, @Param("count") int count);

    @Insert("INSERT INTO post (node_id, creator_id, type, content, state) " +
            "VALUES (#{nodeId}, #{creatorId}, #{type}, #{content}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PostDO posting);

    @Update("UPDATE post " +
            "SET " +
            "node_id = #{nodeId}, content = #{content}, state=#{state} where id = #{id}")
    void update(PostDO posting);

    // 新增分数相关方法
    @Update("UPDATE post SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScore(long nodeId, int limit, byte state);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} AND state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<PostDO> getListByNodeAndScoreAndPaginated(long nodeId, double lastScore, long lastId, int limit, byte state);

    @Select("SELECT id, created_at FROM post WHERE state = #{state} AND deleted_at IS NULL")
    List<PostDO> getAllPostsForScoreCalculation(byte state);

    @Select("SELECT * FROM post where state = #{state} AND deleted_at IS NULL order by id DESC limit #{count}")
    List<PostDO> getListByState(byte state, int count);

    @Select("SELECT * FROM post where state = #{state} and id < #{lastId} AND deleted_at IS NULL order by id DESC limit #{limit}")
    List<PostDO> getListByStateWithPagination(byte state, long lastId, int limit);

    /**
     * 统计活跃文章数量（state=APPROVED表示已发布状态）
     *
     * @return 文章总数
     */
    @Select("SELECT COUNT(*) FROM post WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL")
    Long countActiveArticles();

    @Select("SELECT COUNT(*) FROM post WHERE node_id = #{nodeId} AND creator_id = #{creatorId} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL")
    Long countPostsByNodeAndCreator(@Param("nodeId") long nodeId, @Param("creatorId") long creatorId);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} AND creator_id = #{creatorId} AND state != #{excludeState} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<PostDO> getListByNodeAndCreator(@Param("nodeId") long nodeId, @Param("creatorId") long creatorId, @Param("excludeState") byte excludeState);

    @Select({"<script>",
            "SELECT * FROM post WHERE id &lt; #{lastId} AND deleted_at IS NULL",
            "<if test='nodeId != null'> AND node_id = #{nodeId}</if>",
            "<if test='creatorId != null'> AND creator_id = #{creatorId}</if>",
            "<if test='state != null'> AND state = #{state}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<PostDO> getListByNodeAndCreatorWithPagination(@Param("nodeId") Long nodeId, @Param("creatorId") Long creatorId, @Param("lastId") Long lastId, @Param("state") Byte state, @Param("limit") int limit);

    @Update("UPDATE post SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") long id, @Param("state") byte state);

    @Update("UPDATE post SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    int updateStateWithReason(@Param("id") long id, @Param("state") byte state, @Param("reason") String reason);

    @Update("UPDATE post SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(long id);
}
