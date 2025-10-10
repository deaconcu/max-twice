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

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE source_post_id = #{postId} AND state = #{state}")
    int countByPost(long postId, int state);

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state}")
    int countByCreator(long creatorId, int state);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreator(long postId, long creatorId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorKeyset(long postId, long creatorId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND creator_id = #{creatorId} " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorAllStates(long postId, long creatorId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE source_post_id = #{postId} AND creator_id = #{creatorId} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorKeysetAllStates(long postId, long creatorId, double lastScore, long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE node_id = #{nodeId} AND state = #{state} " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByNode(long nodeId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE node_id = #{nodeId} AND state = #{state} AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, upvote_count DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByNodeKeyset(long nodeId, double lastScore, long lastId, int state, int limit);

    @Insert("INSERT INTO memory_card_deck " +
            "(source_post_id, node_id, creator_id, title, description, version, state, upvote_count, card_count, score) " +
            "VALUES " +
            "(#{sourcePostId}, #{nodeId}, #{creatorId}, #{title}, #{description}, #{version}, #{state}, #{upvoteCount}, #{cardCount}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardDeckDO deck);

    @Update("UPDATE memory_card_deck SET " +
            "title = #{title}, description = #{description}, version = #{version}, " +
            "state = #{state}, updated_by = #{updatedBy}, card_count = #{cardCount}, node_id = #{nodeId}, " +
            "upvote_count = #{upvoteCount}, score = #{score} " +
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

    @Update("UPDATE memory_card_deck SET state = #{state}, updated_at = NOW() WHERE id = #{id}")
    int updateState(long id, byte state);

    @Update("UPDATE memory_card_deck SET card_count = card_count + 1, state = #{state}, updated_at = NOW() WHERE id = #{id}")
    int incrementCardCountAndSetState(long id, byte state);

    @Update("UPDATE memory_card_deck SET card_count = card_count + 1, state = #{state}, version = version + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementCardCountAndSetStateAndVersion(long id, byte state);

    @Update("UPDATE memory_card_deck SET card_count = GREATEST(0, card_count - 1), updated_at = NOW() WHERE id = #{id}")
    int decrementCardCount(long id);

    @Update("UPDATE memory_card_deck SET card_count = GREATEST(0, card_count - 1), version = version + 1, updated_at = NOW() WHERE id = #{id}")
    int decrementCardCountAndIncrementVersion(long id);

    @Update("UPDATE memory_card_deck SET state = #{state}, version = version + 1, updated_at = NOW() WHERE id = #{id}")
    int updateStateAndIncrementVersion(long id, byte state);



}