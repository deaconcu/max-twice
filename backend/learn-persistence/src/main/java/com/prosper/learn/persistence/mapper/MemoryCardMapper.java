package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.MemoryCardDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Select("SELECT * FROM memory_card WHERE creator_id = #{creatorId} AND state = #{state} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<MemoryCardDO> getListByCreator(long creatorId, int state, int limit);

    @Insert("INSERT INTO memory_card " +
            "(deck_id, creator_id, current_version_id, state) " +
            "VALUES " +
            "(#{deckId}, #{creatorId}, #{currentVersionId}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardDO card);

    @Update("UPDATE memory_card SET " +
            "current_version_id = #{currentVersionId}, state = #{state} " +
            "WHERE id = #{id}")
    void update(MemoryCardDO card);

    @Update("UPDATE memory_card SET state = #{state} WHERE id = #{id}")
    int updateState(long id, int state);

    @Select("SELECT COUNT(*) FROM memory_card WHERE deck_id = #{deckId} AND state = #{state}")
    int countByDeck(long deckId, int state);

    @Select("SELECT COUNT(*) FROM memory_card WHERE creator_id = #{creatorId} AND state = #{state}")
    int countByCreator(long creatorId, int state);

}