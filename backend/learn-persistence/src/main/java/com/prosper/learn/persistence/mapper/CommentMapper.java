package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.CommentDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CommentMapper {

    @Select("SELECT * FROM comment where id = #{id}")
    CommentDO get(int id);

    // 首页加载评论，按分数排序
    @Select("SELECT * FROM comment where object_id = #{objectId} and type = #{type} and replyTo = 0 ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectId(int objectId, int type, int count);

    // 分页加载评论，处理分数相同的情况
    @Select("SELECT * FROM comment where object_id = #{objectId} and type = #{type} and replyTo = 0 AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByObjectIdPaginated(int objectId, int type, double lastScore, int lastId, int count);

    @Select("<script>SELECT * " +
            "FROM comment c1 " +
            "where c1.replyTo IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "AND c1.id = (" +
            "  SELECT c2.id " +
            "  FROM comment c2 " +
            "  WHERE c2.replyTo = c1.replyTo " +
            "  ORDER BY c2.score DESC, c2.id DESC " +
            "  LIMIT 1" +
            ")</script>")
    List<CommentDO> getChildren(List<Integer> ids);

    // 首页加载话题回复，按分数排序
    @Select("SELECT * FROM comment where replyTo = #{commentId} ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopic(int commentId, int count);

    // 分页加载话题回复，处理分数相同的情况
    @Select("SELECT * FROM comment where replyTo = #{commentId} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC limit #{count}")
    List<CommentDO> getByTopicPaginated(int commentId, double lastScore, int lastId, int count);

    @Select("SELECT * FROM comment where state = #{state} order by id limit #{count}")
    List<CommentDO> getListByState(int state, int count);

    @Insert("INSERT INTO comment(content, object_id, type, replyTo, fromUser, toUser, upvote_count, score) " +
            "VALUES (#{content}, #{objectId}, #{type}, #{replyTo}, #{fromUser}, #{toUser}, #{upvoteCount}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CommentDO commentDO);

    @Update("UPDATE comment SET replyCount = #{replyCount}, upvote_count = #{upvoteCount}, State = #{state}, score = #{score} where id = #{id}")
    void update(CommentDO commentDO);

    @Delete("DELETE FROM comment where id = #{id}")
    void delete(int id);

}
