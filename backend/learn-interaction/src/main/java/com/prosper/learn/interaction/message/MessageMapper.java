package com.prosper.learn.interaction.message;

import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.MessageDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MessageMapper {

    @Select("SELECT * FROM message " +
            "WHERE id = #{id}")
    MessageDO getById(long id);

    @Select("SELECT * FROM message " +
            "WHERE type = #{type} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> listAll(int type, int limit, int offset);

    @Select("SELECT * FROM message " +
            "WHERE type = #{type} and id < #{lastId}" +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> listByPull(int type, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "where (sender_id = #{userId1} and receiver_id = #{userId2}) or (sender_id = #{userId2} and receiver_id = #{userId1}) and id < #{lastId}" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> getConversationByUser(long userId1, long userId2, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "where type = #{type} and sender_id = #{sender} and receiver_id = #{receiver} and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getListByUser(int type, long sender, long receiver, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "where (sender_id = 0 and receiver_id = #{userId}) and type in (2, 3, 4, 5, 6, 7, 8) and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getSystemListByUser(long userId, long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM message " +
            "WHERE sender_id = 0 AND receiver_id = #{userId} AND type = #{type} " +
            "<if test='lastId != null and lastId > 0'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<MessageDO> getSystemItemListByUser(@Param("type") int type,
                                            @Param("userId") long userId,
                                            @Param("lastId") Long lastId,
                                            @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM message " +
            "WHERE sender_id = 0 AND receiver_id = #{userId} AND type = #{type} " +
            "<if test='lastId != null and lastId > 0'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<MessageDO> listByType(@Param("type") int type,
                               @Param("userId") long userId,
                               @Param("lastId") Long lastId,
                               @Param("limit") int limit);

    @Select("SELECT * FROM message " +
            "where sender_id = #{userId} and type = 1 and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getApplyCourseListByUser(long userId, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "where receiver_id = 0 and type = 1 " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}")
    List<MessageDO> getApplyCourseList(int offset, int limit);

    @Select("SELECT count(*) FROM message " +
            "where receiver_id = 0 and type = 1 ")
    int getApplyCourseCount();

    @Insert("INSERT INTO message(sender_id, receiver_id, content, type, category) " +
            "VALUES (#{senderId}, #{receiverId}, #{content}, #{type}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MessageDO messageDO);

    @Update("UPDATE message " +
            "SET sender_id = #{senderId}, receiver_id = #{receiverId}, content= #{content}, type = #{type}, category = #{category} " +
            "where id = #{id}")
    void update(MessageDO messageDO);

    // 新增：按 category 查询消息（支持分页）
    @Select("<script>" +
            "SELECT * FROM message " +
            "WHERE receiver_id = #{receiverId} AND category = #{category} " +
            "<if test='lastId != null and lastId > 0'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<MessageDO> listByCategory(@Param("receiverId") long receiverId,
                                   @Param("category") int category,
                                   @Param("lastId") Long lastId,
                                   @Param("limit") int limit);
}
