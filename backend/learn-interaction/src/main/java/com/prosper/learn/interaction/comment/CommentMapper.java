package com.prosper.learn.interaction.comment;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comment where id = #{id} AND deleted_at IS NULL")
    CommentDO getById(long id);

    @Select({"<script>SELECT * FROM comment where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL" +
            "</script>"})
    List<CommentDO> getByIds(List<Long> ids);

    // 首页加载评论，按分数排序，只显示已通过且未删除的评论
    @Select("SELECT * FROM comment where object_id = #{objectId} and object_type = #{objectType} and reply_to_comment_id = 0 and state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectId(long objectId, int objectType, int count);

    // 分页加载评论，处理分数相同的情况，只显示已通过且未删除的评论
    @Select("SELECT * FROM comment where object_id = #{objectId} and object_type = #{objectType} and reply_to_comment_id = 0 and state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectIdPaginated(long objectId, int objectType, double lastScore, long lastId, int count);

    // TODO: 性能优化 - 当前 SQL 对每个父评论执行一次子查询，性能较差
    // 优化方案（需要 MySQL 8.0+）：使用窗口函数替代子查询
    // SELECT * FROM (
    //   SELECT *,
    //          ROW_NUMBER() OVER (PARTITION BY reply_to_comment_id
    //                            ORDER BY score DESC, id DESC) as rn
    //   FROM comment
    //   WHERE reply_to_comment_id IN <foreach ...>
    //     AND state = PUBLISHED_VALUE
    //     AND deleted_at IS NULL
    // ) ranked
    // WHERE rn = 1
    @Select("<script>SELECT * " +
            "FROM comment c1 " +
            "where c1.reply_to_comment_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "AND c1.state = " + ContentState.PUBLISHED_VALUE + " " +
            "AND c1.deleted_at IS NULL " +
            "AND c1.id = (" +
            "  SELECT c2.id " +
            "  FROM comment c2 " +
            "  WHERE c2.reply_to_comment_id = c1.reply_to_comment_id " +
            "  AND c2.state = " + ContentState.PUBLISHED_VALUE + " " +
            "  AND c2.deleted_at IS NULL " +
            "  ORDER BY c2.score DESC, c2.id DESC " +
            "  LIMIT 1" +
            ")</script>")
    List<CommentDO> getChildren(List<Long> ids);

    // 首页加载话题回复，按分数排序，只显示已通过且未删除的评论
    @Select("SELECT * FROM comment where reply_to_comment_id = #{commentId} and state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopic(long commentId, int count);

    // 分页加载话题回复，处理分数相同的情况，只显示已通过且未删除的评论
    @Select("SELECT * FROM comment where reply_to_comment_id = #{commentId} and state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopicPaginated(long commentId, double lastScore, long lastId, int count);

    @Select({"<script>",
            "SELECT * FROM comment WHERE state = #{state}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL",
            "ORDER BY id DESC LIMIT #{count}",
            "</script>"})
    List<CommentDO> getListByState(@Param("state") byte state, @Param("lastId") Long lastId, @Param("count") int count);

    @Select({"<script>",
            "SELECT * FROM comment",
            "<where>",
            "deleted_at IS NULL",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "<if test='objectType != null'> AND object_type = #{objectType}</if>",
            "<if test='objectId != null'> AND object_id = #{objectId}</if>",
            "<if test='creatorId != null'> AND creator_id = #{creatorId}</if>",
            "</where>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<CommentDO> getListByFilter(@Param("objectType") Integer objectType, @Param("objectId") Long objectId, @Param("creatorId") Long creatorId, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Insert("INSERT INTO comment(content, object_type, object_id, reply_to_comment_id, creator_id, to_user_id, state, score) " +
            "VALUES (#{content}, #{objectType}, #{objectId}, #{replyToCommentId}, #{creatorId}, #{toUserId}, #{state}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CommentDO commentDO);

    @Update("UPDATE comment SET state = #{state}, score = #{score} where id = #{id}")
    void update(CommentDO commentDO);

// --注释掉检查 START (2025/12/10 11:38):
//    @Update("UPDATE comment SET state = #{state} WHERE id = #{id}")
//    int updateState(@Param("id") long id, @Param("state") byte state);
// --注释掉检查 STOP (2025/12/10 11:38)

    @Update("UPDATE comment SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    int updateStateWithReason(@Param("id") long id, @Param("state") byte state, @Param("reason") String reason);

    /**
     * 软删除评论
     */
    @Update("UPDATE comment SET deleted_at = NOW() WHERE id = #{id}")
    int softDelete(long id);

    /**
     * 物理删除评论（仅用于测试或数据清理）
     */
    @Delete("DELETE FROM comment where id = #{id}")
    void delete(long id);

}
