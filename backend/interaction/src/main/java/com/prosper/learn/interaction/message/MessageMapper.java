package com.prosper.learn.interaction.message;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("SELECT * FROM message " +
            "WHERE id = #{id}")
    MessageDO getById(long id);

    @Select("SELECT * FROM message " +
            "WHERE type = #{type} and id < #{lastId}" +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> listByPull(int type, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "WHERE ((sender_id = #{userId1} AND receiver_id = #{userId2}) " +
            "OR (sender_id = #{userId2} AND receiver_id = #{userId1})) " +
            "AND id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getConversationByUser(long userId1, long userId2, long lastId, int limit);

    @Select("SELECT * FROM message " +
            "where type = #{type} and sender_id = #{sender} and receiver_id = #{receiver} and id < #{lastId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MessageDO> getListByUser(int type, long sender, long receiver, long lastId, int limit);

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

    // 新增：查询全部消息（互动+系统，category IN (1, 2)）
    @Select("<script>" +
            "SELECT * FROM message " +
            "WHERE receiver_id = #{receiverId} AND category IN (1, 2) " +
            "<if test='lastId != null and lastId > 0'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<MessageDO> listAllMessages(@Param("receiverId") long receiverId,
                                    @Param("lastId") Long lastId,
                                    @Param("limit") int limit);

    // 统计未读消息数量（id > lastViewedMessageId）
    @Select("SELECT COUNT(*) FROM message " +
            "WHERE receiver_id = #{receiverId} " +
            "AND category IN (1, 2) " +
            "AND id > #{lastViewedMessageId}")
    int countUnreadMessages(@Param("receiverId") long receiverId,
                           @Param("lastViewedMessageId") long lastViewedMessageId);
}
