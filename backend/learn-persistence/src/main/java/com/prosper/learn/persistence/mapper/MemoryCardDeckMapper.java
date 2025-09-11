package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.MemoryCardDeckDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MemoryCardDeckMapper {

    @Select("SELECT * FROM memory_card_deck WHERE id = #{id}")
    MemoryCardDeckDO get(long id);

    @Select({"<script>SELECT * FROM memory_card_deck WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<MemoryCardDeckDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM memory_card_deck WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, MemoryCardDeckDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND state = #{state} " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPost(long postId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostKeyset(long postId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state} " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByCreator(long creatorId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByCreatorKeyset(long creatorId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE state = #{state} " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByState(int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByStateKeyset(double lastScore, long lastId, int state, int limit);

    @Insert("INSERT INTO memory_card_deck " +
            "(source_post_id, creator_id, title, description, version, state, upvote_count, card_count, score) " +
            "VALUES " +
            "(#{sourcePostId}, #{creatorId}, #{title}, #{description}, #{version}, #{state}, #{upvoteCount}, #{cardCount}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardDeckDO deck);

    @Update("UPDATE memory_card_deck SET " +
            "title = #{title}, description = #{description}, version = #{version}, " +
            "state = #{state}, updated_by = #{updatedBy}, card_count = #{cardCount} " +
            "WHERE id = #{id}")
    void update(MemoryCardDeckDO deck);

    @Update("UPDATE memory_card_deck SET " +
            "state = #{state}, auditor_id = #{auditorId}, audited_at = NOW() " +
            "WHERE id = #{id}")
    int updateAuditStatus(long id, int state, long auditorId);

    @Update("UPDATE memory_card_deck SET " +
            "upvote_count = #{upvoteCount}, score = #{score} " +
            "WHERE id = #{id}")
    int updateScore(long id, int upvoteCount, double score);

    @Update("UPDATE memory_card_deck SET card_count = #{cardCount} WHERE id = #{id}")
    int updateCardCount(long id, int cardCount);

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE source_post_id = #{postId} AND state = #{state}")
    int countByPost(long postId, int state);

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state}")
    int countByCreator(long creatorId, int state);

}