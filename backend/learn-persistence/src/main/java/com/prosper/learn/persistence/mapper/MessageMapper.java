package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MessageMapper {

    @Select("SELECT * FROM message " +
            "WHERE id = #{id}")
    MessageDO getById(@Param("id") int id);

    @Select("SELECT * FROM message " +
            "WHERE type = #{type} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> listAll(@Param("type") int type, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM message " +
            "where (sender_id = #{userId1} and receiver_id = #{userId2}) or (sender_id = #{userId2} and receiver_id = #{userId1})" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> getConversationByUser(int userId1, int userId2, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM message " +
            "where type = #{type} and sender_id = #{sender} and receiver_id = #{receiver} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> getListByUser(int type, int sender, int receiver, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM message " +
            "where (sender_id = 0 and receiver_id = #{userId}) and type in (2, 3, 4, 5, 6, 7, 8) and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getSystemListByUser(int userId, int lastId, int limit);

    @Select("SELECT * FROM message " +
            "where (sender_id = 0 and receiver_id = #{userId}) and type = #{type} and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getSystemItemListByUser(int type, int userId, int lastId, int limit);

    @Select("SELECT * FROM message " +
            "where sender_id = #{userId} and type = 1 and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getApplyCourseListByUser(int userId, int lastId, int limit);

    @Select("SELECT * FROM message " +
            "where receiver_id = 0 and type = 1 " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> getApplyCourseList(int offset, int limit);

    @Select("SELECT count(*) FROM message " +
            "where receiver_id = 0 and type = 1 ")
    int getApplyCourseCount();

    @Insert("INSERT INTO message(sender_id, receiver_id, content, type, is_read) " +
            "VALUES (#{senderId}, #{receiverId}, #{content}, #{type}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MessageDO messageDO);

    @Update("UPDATE message " +
            "SET sender_id = #{senderId}, receiver_id = #{receiverId}, content= #{content}, " +
                "type = #{type}, is_read = #{isRead} " +
            "where id = #{id}")
    void update(MessageDO messageDO);
}
