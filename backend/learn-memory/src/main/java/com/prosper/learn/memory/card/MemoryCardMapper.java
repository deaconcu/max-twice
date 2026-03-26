package com.prosper.learn.memory.card;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemoryCardMapper {

    @Select("SELECT * FROM memory_card WHERE id = #{id}")
    MemoryCardDO get(long id);

    @Select({"<script>SELECT * FROM memory_card WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<MemoryCardDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM memory_card WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, MemoryCardDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM memory_card WHERE deck_id = #{deckId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY created_at ASC")
    List<MemoryCardDO> getListByDeck(long deckId, int state);

    @Select({"<script>SELECT * FROM memory_card WHERE deck_id IN " +
            "<foreach item='deckId' collection='deckIds' open='(' separator=', ' close=')'>#{deckId}</foreach>" +
            " AND state = #{state} AND deleted_at IS NULL ORDER BY deck_id, created_at ASC" +
            "</script>"})
    List<MemoryCardDO> getByDeckIds(@Param("deckIds") List<Long> deckIds, @Param("state") int state);

    /**
     * 批量获取每个 deck 的第一张卡片
     */
    @Select({"<script>SELECT mc.deck_id, mc.current_version_id FROM memory_card mc " +
            "WHERE mc.id IN (SELECT MIN(id) FROM memory_card " +
            "WHERE deck_id IN <foreach item='deckId' collection='deckIds' open='(' separator=',' close=')'>#{deckId}</foreach> " +
            "AND state = #{state} AND deleted_at IS NULL GROUP BY deck_id)" +
            "</script>"})
    List<MemoryCardDO> getFirstCardByDeckIds(@Param("deckIds") List<Long> deckIds, @Param("state") int state);

    @Insert("INSERT INTO memory_card " +
            "(deck_id, creator_id, current_version_id, state) " +
            "VALUES " +
            "(#{deckId}, #{creatorId}, #{currentVersionId}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardDO card);

    @Insert({"<script>INSERT INTO memory_card " +
            "(deck_id, creator_id, current_version_id, state) VALUES " +
            "<foreach collection='cards' item='card' separator=','>",
            "(#{card.deckId}, #{card.creatorId}, #{card.currentVersionId}, #{card.state})",
            "</foreach></script>"})
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int batchInsert(@Param("cards") List<MemoryCardDO> cards);

    @Update("UPDATE memory_card SET " +
            "current_version_id = #{currentVersionId}, state = #{state} " +
            "WHERE id = #{id} AND deleted_at IS NULL")
    void update(MemoryCardDO card);

    @Update({"<script>" +
            "UPDATE memory_card SET current_version_id = CASE id " +
            "<foreach collection='cards' item='card'>" +
            "WHEN #{card.id} THEN #{card.currentVersionId} " +
            "</foreach>" +
            "END " +
            "WHERE id IN " +
            "<foreach collection='cards' item='card' open='(' separator=',' close=')'>" +
            "#{card.id}" +
            "</foreach>" +
            " AND deleted_at IS NULL" +
            "</script>"})
    int batchUpdateCurrentVersionId(@Param("cards") List<MemoryCardDO> cards);

    /**
     * 批量更新卡片
     *
     * 使用 CASE WHEN 语法实现真正的批量更新（单条 SQL）
     * 优势：
     * - 只需一次数据库往返，性能更好
     * - 单条 SQL，减少锁竞争
     * - 避免多条 UPDATE 可能导致的死锁
     */
    @Update({"<script>" +
            "UPDATE memory_card SET " +
            "current_version_id = CASE id " +
            "<foreach collection='cards' item='card'>" +
            "WHEN #{card.id} THEN #{card.currentVersionId} " +
            "</foreach>" +
            "END, " +
            "state = CASE id " +
            "<foreach collection='cards' item='card'>" +
            "WHEN #{card.id} THEN #{card.state} " +
            "</foreach>" +
            "END, " +
            "updated_at = CASE id " +
            "<foreach collection='cards' item='card'>" +
            "WHEN #{card.id} THEN #{card.updatedAt} " +
            "</foreach>" +
            "END " +
            "WHERE id IN " +
            "<foreach collection='cards' item='card' open='(' separator=',' close=')'>" +
            "#{card.id}" +
            "</foreach>" +
            " AND deleted_at IS NULL" +
            "</script>"})
    int batchUpdate(@Param("cards") List<MemoryCardDO> cards);

    @Select("SELECT COUNT(*) FROM memory_card WHERE deck_id = #{deckId} AND state = #{state} AND deleted_at IS NULL")
    int countByDeck(long deckId, int state);

    @Update("UPDATE memory_card SET deleted_at = #{deletedAt}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(MemoryCardDO card);

    /**
     * 批量软删除卡片
     */
    @Update({"<script>" +
            "UPDATE memory_card SET deleted_at = #{now}, updated_at = #{now} " +
            "WHERE id IN " +
            "<foreach collection='cardIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND deleted_at IS NULL" +
            "</script>"})
    int batchSoftDelete(@Param("cardIds") List<Long> cardIds, @Param("now") LocalDateTime now);

}
