package com.prosper.learn.memory.deck;

import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemoryCardDeckMapper {

    @Select("SELECT * FROM memory_card_deck WHERE id = #{id} AND deleted_at IS NULL")
    MemoryCardDeckDO get(long id);

    @Select({"<script>SELECT * FROM memory_card_deck WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL</script>"})
    List<MemoryCardDeckDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM memory_card_deck WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL</script>"})
    @MapKey("id")
    Map<Long, MemoryCardDeckDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPost(long postId, int state, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostWithIdPaging(long postId, int state, Long lastId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL",
            "<if test='sortBy == \"createdAt\"'>",
            "  <if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "  ORDER BY id DESC",
            "</if>",
            "<if test='sortBy != \"createdAt\"'>",
            "  <if test='lastScore != null and lastId != null'>",
            "    AND (score &lt; #{lastScore} OR (score = #{lastScore} AND id &lt; #{lastId}))",
            "  </if>",
            "  ORDER BY score DESC, id DESC",
            "</if>",
            " LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostDynamic(long postId, int state, String sortBy, Double lastScore, Long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostKeyset(long postId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByCreator(long creatorId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByCreatorWithIdPaging(long creatorId, Long lastId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByCreatorWithIdPagingAndState(long creatorId, int state, Long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByCreatorKeyset(long creatorId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE state = #{state} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByState(int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByStateKeyset(double lastScore, long lastId, int state, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE state = #{state}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> listByState(int state, Long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostForReview(long postId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByCreatorForReview(long creatorId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorForReview(long postId, long creatorId, int state, int limit);

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE post_id = #{postId} AND state = #{state} AND deleted_at IS NULL")
    int countByPost(long postId, int state);

    @Select("SELECT COUNT(*) FROM memory_card_deck WHERE creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL")
    int countByCreator(long creatorId, int state);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreator(long postId, long creatorId, int state, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state}",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " AND deleted_at IS NULL ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPaging(long postId, long creatorId, byte state, Long lastId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL",
            "<if test='sortBy == \"createdAt\"'>",
            "  <if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "  ORDER BY id DESC",
            "</if>",
            "<if test='sortBy != \"createdAt\"'>",
            "  <if test='lastScore != null and lastId != null'>",
            "    AND (score &lt; #{lastScore} OR (score = #{lastScore} AND id &lt; #{lastId}))",
            "  </if>",
            "  ORDER BY score DESC, id DESC",
            "</if>",
            " LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostAndCreatorDynamic(long postId, long creatorId, int state, String sortBy, Double lastScore, Long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorKeyset(long postId, long creatorId, double lastScore, long lastId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorAllStates(long postId, long creatorId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND deleted_at IS NULL",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            " ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPagingAllStates(long postId, long creatorId, Long lastId, int limit);

    @Select({"<script>",
            "SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND deleted_at IS NULL",
            "<if test='sortBy == \"createdAt\"'>",
            "  <if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "  ORDER BY id DESC",
            "</if>",
            "<if test='sortBy != \"createdAt\"'>",
            "  <if test='lastScore != null and lastId != null'>",
            "    AND (score &lt; #{lastScore} OR (score = #{lastScore} AND id &lt; #{lastId}))",
            "  </if>",
            "  ORDER BY score DESC, id DESC",
            "</if>",
            " LIMIT #{limit}",
            "</script>"})
    List<MemoryCardDeckDO> getListByPostAndCreatorDynamicAllStates(long postId, long creatorId, String sortBy, Double lastScore, Long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE post_id = #{postId} AND creator_id = #{creatorId} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByPostAndCreatorKeysetAllStates(long postId, long creatorId, double lastScore, long lastId, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE node_id = #{nodeId} AND state = #{state} AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByNode(long nodeId, int state, int limit);

    @Select("SELECT * FROM memory_card_deck WHERE node_id = #{nodeId} AND state = #{state} AND deleted_at IS NULL AND " +
            "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<MemoryCardDeckDO> getListByNodeKeyset(long nodeId, double lastScore, long lastId, int state, int limit);

    @Insert("INSERT INTO memory_card_deck " +
            "(post_id, node_id, creator_id, title, description, version, state, card_count) " +
            "VALUES " +
            "(#{postId}, #{nodeId}, #{creatorId}, #{title}, #{description}, #{version}, #{state}, #{cardCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MemoryCardDeckDO deck);

    @Update("UPDATE memory_card_deck SET " +
            "title = #{title}, description = #{description}, version = #{version}, " +
            "state = #{state}, reason = #{reason}, card_count = #{cardCount}, node_id = #{nodeId}, " +
            "score = #{score} " +
            "WHERE id = #{id}")
    void update(MemoryCardDeckDO deck);

    @Update("UPDATE memory_card_deck SET " +
            "state = #{state}, auditor_id = #{auditorId}, audited_at = NOW() " +
            "WHERE id = #{id}")
    int updateAuditStatus(long id, int state, long auditorId);

    @Update("UPDATE memory_card_deck SET score = #{score} WHERE id = #{id}")
    int updateScore(long id, double score);

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

    @Update("UPDATE memory_card_deck SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(long id);

}