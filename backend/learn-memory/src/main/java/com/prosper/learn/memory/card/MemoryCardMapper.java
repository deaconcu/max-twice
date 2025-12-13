package com.prosper.learn.memory.card;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
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

    @Select("SELECT * FROM memory_card WHERE deck_id = #{deckId} AND state = #{state} " +
            "ORDER BY created_at ASC")
    List<MemoryCardDO> getListByDeck(long deckId, int state);

    @Select({"<script>SELECT * FROM memory_card WHERE deck_id IN " +
            "<foreach item='deckId' collection='deckIds' open='(' separator=', ' close=')'>#{deckId}</foreach>" +
            " AND state = #{state} ORDER BY deck_id, created_at ASC" +
            "</script>"})
    List<MemoryCardDO> getByDeckIds(@Param("deckIds") List<Long> deckIds, @Param("state") int state);

// --注释掉检查 START (2025/12/10 12:01):
//    @Select("SELECT * FROM memory_card WHERE creator_id = #{creatorId} AND state = #{state} " +
//            "ORDER BY created_at DESC LIMIT #{limit}")
//    List<MemoryCardDO> getListByCreator(long creatorId, int state, int limit);
// --注释掉检查 STOP (2025/12/10 12:01)

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
            "WHERE id = #{id}")
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
            "</script>"})
    int batchUpdateCurrentVersionId(@Param("cards") List<MemoryCardDO> cards);

// --注释掉检查 START (2025/12/10 12:01):
//    @Update("UPDATE memory_card SET state = #{state} WHERE id = #{id}")
//    int updateState(long id, int state);
// --注释掉检查 STOP (2025/12/10 12:01)

    @Update({"<script>" +
            "<foreach collection='cards' item='card' separator=';'>" +
            "UPDATE memory_card SET " +
            "current_version_id = #{card.currentVersionId}, " +
            "state = #{card.state}, " +
            "updated_at = #{card.updatedAt} " +
            "WHERE id = #{card.id}" +
            "</foreach>" +
            "</script>"})
    int batchUpdate(@Param("cards") List<MemoryCardDO> cards);

    @Select("SELECT COUNT(*) FROM memory_card WHERE deck_id = #{deckId} AND state = #{state}")
    int countByDeck(long deckId, int state);

// --注释掉检查 START (2025/12/10 12:01):
//    @Select("SELECT id FROM memory_card WHERE deck_id = #{deckId} AND state = " + Enums.ContentState.PUBLISHED_VALUE + " ORDER BY id")
//    List<Long> getCardIdsByDeckId(long deckId);
// --注释掉检查 STOP (2025/12/10 12:01)

// --注释掉检查 START (2025/12/10 12:01):
//    @Select("SELECT COUNT(*) FROM memory_card WHERE creator_id = #{creatorId} AND state = #{state}")
//    int countByCreator(long creatorId, int state);
// --注释掉检查 STOP (2025/12/10 12:01)

}