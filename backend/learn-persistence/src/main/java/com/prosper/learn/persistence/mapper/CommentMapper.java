package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CommentMapper {

    @Select("SELECT * FROM comment where id = #{id}")
    CommentDO getById(long id);

    @Select({"<script>SELECT * FROM comment where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CommentDO> getByIds(List<Long> ids);

    // 首页加载评论，按分数排序
    @Select("SELECT * FROM comment where object_id = #{objectId} and type = #{type} and reply_to_comment_id = 0 ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectId(long objectId, int type, int count);

    // 分页加载评论，处理分数相同的情况
    @Select("SELECT * FROM comment where object_id = #{objectId} and type = #{type} and reply_to_comment_id = 0 AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectIdPaginated(long objectId, int type, double lastScore, long lastId, int count);

    @Select("<script>SELECT * " +
            "FROM comment c1 " +
            "where c1.reply_to_comment_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "AND c1.id = (" +
            "  SELECT c2.id " +
            "  FROM comment c2 " +
            "  WHERE c2.reply_to_comment_id = c1.reply_to_comment_id " +
            "  ORDER BY c2.score DESC, c2.id DESC " +
            "  LIMIT 1" +
            ")</script>")
    List<CommentDO> getChildren(List<Long> ids);

    // 首页加载话题回复，按分数排序
    @Select("SELECT * FROM comment where reply_to_comment_id = #{commentId} ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopic(long commentId, int count);

    // 分页加载话题回复，处理分数相同的情况
    @Select("SELECT * FROM comment where reply_to_comment_id = #{commentId} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopicPaginated(long commentId, double lastScore, long lastId, int count);

    @Select("SELECT * FROM comment where state = #{state} order by id limit #{count}")
    List<CommentDO> getListByState(int state, int count);

    @Insert("INSERT INTO comment(content, object_type, object_id, reply_to_comment_id, from_user_id, to_user_id, upvote_count, score) " +
            "VALUES (#{content}, #{type}, #{objectId}, #{replyToCommentId}, #{fromUserId}, #{toUserId}, #{upvoteCount}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CommentDO commentDO);

    @Update("UPDATE comment SET reply_count = #{replyCount}, upvote_count = #{upvoteCount}, State = #{state}, score = #{score} where id = #{id}")
    void update(CommentDO commentDO);

    @Delete("DELETE FROM comment where id = #{id}")
    void delete(long id);

}
