package com.prosper.learn.memory.review;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface UserCardSrsMapper {

    @Select("SELECT * FROM user_card_srs WHERE id = #{id}")
    UserCardSrsDO get(long id);

    @Select("SELECT * FROM user_card_srs WHERE user_id = #{userId} AND card_id = #{cardId}")
    UserCardSrsDO getByUserAndCard(long userId, long cardId);

    @Select({"<script>SELECT * FROM user_card_srs WHERE user_id = #{userId} AND card_id IN " +
            "<foreach item='cardId' collection='cardIds' open='(' separator=', ' close=')'>#{cardId}</foreach>" +
            "</script>"})
    List<UserCardSrsDO> getByUserAndCards(long userId, Collection<Long> cardIds);

    @Select({"<script>SELECT * FROM user_card_srs WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserCardSrsDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM user_card_srs WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserCardSrsDO> getMapByIds(Collection<Long> ids);

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT srs.* FROM user_card_srs srs " +
//            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id " +
//            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= #{dueTime} " +
//            "AND deck.state = " + ContentState.PUBLISHED_VALUE + " " +
//            "AND EXISTS (" +
//            "    SELECT 1 FROM user_card_in_course ctx " +
//            "    JOIN user_course_srs_setting s ON ctx.course_id = s.course_id AND ctx.user_id = s.user_id " +
//            "    WHERE ctx.card_id = srs.card_id AND s.status = 1 AND s.user_id = srs.user_id" +
//            ") " +
//            "ORDER BY srs.review_due_at ASC LIMIT #{limit}")
//    List<UserCardSrsDO> getDueCardsForReview(long userId, LocalDateTime dueTime, int limit);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT srs.* FROM user_card_srs srs " +
//            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id " +
//            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= #{dueTime} " +
//            "AND deck.state = " + ContentState.PUBLISHED_VALUE + " " +
//            "AND EXISTS (" +
//            "    SELECT 1 FROM user_card_in_course ctx " +
//            "    WHERE ctx.card_id = srs.card_id AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId}" +
//            ") " +
//            "ORDER BY srs.review_due_at ASC LIMIT #{limit}")
//    List<UserCardSrsDO> getDueCardsByCourseForReview(long userId, long courseId, LocalDateTime dueTime, int limit);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT srs.* FROM user_card_srs srs " +
//            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id " +
//            "WHERE srs.user_id = #{userId} AND deck.state = " + ContentState.PUBLISHED_VALUE + " " +
//            "ORDER BY srs.review_due_at ASC LIMIT #{limit}")
//    List<UserCardSrsDO> getByUser(long userId, int limit);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT srs.* FROM user_card_srs srs " +
//            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id " +
//            "WHERE srs.user_id = #{userId} AND deck.state = " + ContentState.PUBLISHED_VALUE + " " +
//            "AND srs.card_id IN " +
//            "(SELECT card_id FROM user_card_in_course WHERE course_id = #{courseId} AND user_id = #{userId}) " +
//            "ORDER BY srs.review_due_at ASC")
//    List<UserCardSrsDO> getByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

    @Insert("INSERT INTO user_card_srs " +
            "(user_id, card_id, node_id, deck_id, course_id, deck_version, card_version_id, review_due_at, last_reviewed_at, " +
            "type, current_step, `interval`, reappear_at, lapse_old_interval, ease_factor, repetitions, lapse_count, created_at, updated_at) " +
            "VALUES " +
            "(#{userId}, #{cardId}, #{nodeId}, #{deckId}, #{courseId}, #{deckVersion}, #{cardVersionId}, #{reviewDueAt}, #{lastReviewedAt}, " +
            "#{type}, #{currentStep}, #{interval}, #{reappearAt}, #{lapseOldInterval}, #{easeFactor}, #{repetitions}, #{lapseCount}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCardSrsDO state);

    @Update("UPDATE user_card_srs SET " +
            "node_id = #{nodeId}, deck_id = #{deckId}, course_id = #{courseId}, deck_version = #{deckVersion}, card_version_id = #{cardVersionId}, " +
            "review_due_at = #{reviewDueAt}, last_reviewed_at = #{lastReviewedAt}, " +
            "type = #{type}, current_step = #{currentStep}, `interval` = #{interval}, reappear_at = #{reappearAt}, lapse_old_interval = #{lapseOldInterval}, " +
            "ease_factor = #{easeFactor}, repetitions = #{repetitions}, lapse_count = #{lapseCount}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    void update(UserCardSrsDO state);

// --注释掉检查 START (2025/12/10 12:04):
//    @Update("UPDATE user_card_srs SET review_due_at = #{reviewDueAt} WHERE id = #{id}")
//    int updateReviewDueAt(long id, LocalDateTime reviewDueAt);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Update("UPDATE user_card_srs SET " +
//            "review_due_at = #{reviewDueAt}, last_reviewed_at = NOW(), " +
//            "type = #{type}, current_step = #{currentStep}, `interval` = #{interval}, lapse_old_interval = #{lapseOldInterval}, " +
//            "ease_factor = #{easeFactor}, repetitions = #{repetitions}, lapse_count = #{lapseCount} " +
//            "WHERE user_id = #{userId} AND card_id = #{cardId}")
//    int updateAfterReview(long userId, long cardId, LocalDateTime reviewDueAt,
//                         byte type, byte currentStep, int interval, Short lapseOldInterval,
//                         java.math.BigDecimal easeFactor, int repetitions, int lapseCount);
// --注释掉检查 STOP (2025/12/10 12:04)

    @Delete("DELETE FROM user_card_srs WHERE user_id = #{userId} AND card_id = #{cardId}")
    int deleteByUserAndCard(long userId, long cardId);

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_srs WHERE user_id = #{userId}")
//    int countByUser(long userId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_srs " +
//            "WHERE user_id = #{userId} AND review_due_at <= #{dueTime}")
//    int countDueCards(long userId, LocalDateTime dueTime);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_srs srs " +
//            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= NOW() " +
//            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
//            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
//    long countDueCardsByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_in_course ctx " +
//            "WHERE ctx.user_id = #{userId} AND ctx.course_id = #{courseId} " +
//            "AND NOT EXISTS (SELECT 1 FROM user_card_srs srs WHERE srs.card_id = ctx.card_id " +
//            "AND srs.user_id = #{userId})")
//    long countNewCardsByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_srs srs " +
//            "WHERE srs.user_id = #{userId} AND srs.review_due_at > NOW() " +
//            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
//            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
//    long countReviewCardsByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_card_srs srs " +
//            "WHERE srs.user_id = #{userId} AND srs.repetitions >= 3 " +
//            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
//            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
//    long countLearnedCardsByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

    @Insert("""
          <script>
          INSERT IGNORE INTO user_card_srs
          (user_id, card_id, node_id, deck_id, course_id, deck_version, card_version_id, review_due_at,
           type, current_step, `interval`, reappear_at, lapse_old_interval, ease_factor, repetitions, lapse_count, created_at, updated_at)
          VALUES
          <foreach collection="states" item="state" separator=",">
              (#{state.userId}, #{state.cardId}, #{state.nodeId}, #{state.deckId}, #{state.courseId}, #{state.deckVersion}, #{state.cardVersionId},
               #{state.reviewDueAt}, #{state.type}, #{state.currentStep}, #{state.interval}, #{state.reappearAt}, #{state.lapseOldInterval},
               #{state.easeFactor}, #{state.repetitions}, #{state.lapseCount}, NOW(), NOW())
          </foreach>
          </script>
          """)
    int batchInsertIgnoreSrsStates(@Param("states") List<UserCardSrsDO> states);

    @Select("SELECT COUNT(*) FROM user_card_srs " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    long countReviewsInPeriod(long userId, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT AVG(repetitions * 1.0) FROM user_card_srs " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    Double calculateAverageScore(long userId, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT COUNT(*) * 30000 FROM user_card_srs " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    Long calculateTimeSpent(long userId, LocalDateTime startTime, LocalDateTime endTime);

    @Delete("""
          <script>
          DELETE FROM user_card_srs 
          WHERE user_id = #{userId} AND card_id IN
          <foreach collection="cardIds" item="cardId" open="(" separator="," close=")">
              #{cardId}
          </foreach>
          </script>
          """)
    int batchDeleteByUserAndCards(@Param("userId") long userId,
                                 @Param("cardIds") List<Long> cardIds);

    // ========== 支持分页的查询方法 ==========

    @Select({"<script>",
            "SELECT srs.* FROM user_card_srs srs",
            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "WHERE srs.user_id = #{userId} AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "<if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "ORDER BY srs.id ASC LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getByUserWithPaging(long userId, int limit, Long lastId);

    @Select({"<script>",
            "SELECT srs.* FROM user_card_srs srs",
            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "WHERE srs.user_id = #{userId} AND srs.course_id = #{courseId} AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "<if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "ORDER BY srs.id ASC LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getByUserAndCourseWithPaging(long userId, long courseId, int limit, Long lastId);

    @Select({"<script>",
            "(",
            "  SELECT srs.* FROM user_card_srs srs",
            "  INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "  WHERE srs.user_id = #{userId} AND srs.review_due_at &lt;= #{dueTime}",
            "  AND srs.type IN (1, 2, 3)",  // LEARNING, REVIEW, RELEARNING
            "  AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "  <if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "  ORDER BY",
            "    CASE srs.type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 ELSE 3 END,",
            "    srs.review_due_at ASC,",
            "    srs.id ASC",
            "  LIMIT #{limit}",
            ")",
            "UNION ALL",
            "(",
            "  SELECT srs.* FROM user_card_srs srs",
            "  INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "  WHERE srs.user_id = #{userId} AND srs.type = 0",  // NEW cards
            "  AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "  <if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "  ORDER BY srs.id ASC",
            "  LIMIT #{limit}",
            ")",
            "ORDER BY",
            "  CASE type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 WHEN 0 THEN 3 ELSE 4 END,",
            "  review_due_at ASC,",
            "  id ASC",
            "LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getDueCardsForReviewWithPaging(long userId, LocalDateTime dueTime, int limit, Long lastId);

    @Select({"<script>",
            "(",
            "  SELECT srs.* FROM user_card_srs srs",
            "  INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "  WHERE srs.user_id = #{userId} AND srs.course_id = #{courseId}",
            "  AND srs.review_due_at &lt;= #{dueTime}",
            "  AND srs.type IN (1, 2, 3)",  // LEARNING, REVIEW, RELEARNING
            "  AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "  <if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "  ORDER BY",
            "    CASE srs.type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 ELSE 3 END,",
            "    srs.review_due_at ASC,",
            "    srs.id ASC",
            "  LIMIT #{limit}",
            ")",
            "UNION ALL",
            "(",
            "  SELECT srs.* FROM user_card_srs srs",
            "  INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "  WHERE srs.user_id = #{userId} AND srs.course_id = #{courseId}",
            "  AND srs.type = 0",  // NEW cards
            "  AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "  <if test='lastId != null'>AND srs.id &gt; #{lastId}</if>",
            "  ORDER BY srs.id ASC",
            "  LIMIT #{limit}",
            ")",
            "ORDER BY",
            "  CASE type WHEN 1 THEN 0 WHEN 3 THEN 1 WHEN 2 THEN 2 WHEN 0 THEN 3 ELSE 4 END,",
            "  review_due_at ASC,",
            "  id ASC",
            "LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getDueCardsByCourseForReviewWithPaging(long userId, long courseId, LocalDateTime dueTime, int limit, Long lastId);

    @Select("SELECT * FROM user_card_srs WHERE user_id = #{userId} AND deck_id = #{deckId}")
    List<UserCardSrsDO> getByUserAndDeckId(long userId, long deckId);

    @Select("SELECT * FROM user_card_srs WHERE user_id = #{userId} AND node_id = #{nodeId}")
    List<UserCardSrsDO> getByUserAndNodeId(long userId, long nodeId);

    // ========== 新的复习逻辑：基于卡片计数的调度 ==========

    /**
     * 获取下一张待复习卡片（全部课程）
     * 优先级：
     * 1. LEARNING/RELEARNING 且 reappear_at <= reviewCardCount
     * 2. REVIEW 或 NEW（根据 newFirst 参数决定顺序）
     * 3. LEARNING/RELEARNING 未到计数（兜底）
     *
     * @param newFirst true=先新卡后复习，false=先复习后新卡
     */
    @Select({"<script>",
            "SELECT srs.* FROM user_card_srs srs",
            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "WHERE srs.user_id = #{userId}",
            "AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "ORDER BY",
            "  CASE",
            "    WHEN srs.type IN (1, 3) AND srs.reappear_at &lt;= #{reviewCardCount} THEN 0",
            "    <if test='newFirst'>",
            "    WHEN srs.type = 0 THEN 1",
            "    WHEN srs.type = 2 AND srs.review_due_at &lt;= NOW() THEN 2",
            "    </if>",
            "    <if test='!newFirst'>",
            "    WHEN srs.type = 2 AND srs.review_due_at &lt;= NOW() THEN 1",
            "    WHEN srs.type = 0 THEN 2",
            "    </if>",
            "    WHEN srs.type IN (1, 3) THEN 3",
            "    ELSE 4",
            "  END,",
            "  srs.reappear_at ASC,",
            "  srs.review_due_at ASC,",
            "  srs.id ASC",
            "LIMIT 1",
            "</script>"})
    UserCardSrsDO getNextCard(@Param("userId") long userId,
                              @Param("reviewCardCount") long reviewCardCount,
                              @Param("newFirst") boolean newFirst);

    /**
     * 获取下一张待复习卡片（指定课程）
     */
    @Select({"<script>",
            "SELECT srs.* FROM user_card_srs srs",
            "INNER JOIN memory_card_deck deck ON srs.deck_id = deck.id",
            "WHERE srs.user_id = #{userId}",
            "AND srs.course_id = #{courseId}",
            "AND deck.state = " + ContentState.PUBLISHED_VALUE,
            "ORDER BY",
            "  CASE",
            "    WHEN srs.type IN (1, 3) AND srs.reappear_at &lt;= #{reviewCardCount} THEN 0",
            "    <if test='newFirst'>",
            "    WHEN srs.type = 0 THEN 1",
            "    WHEN srs.type = 2 AND srs.review_due_at &lt;= NOW() THEN 2",
            "    </if>",
            "    <if test='!newFirst'>",
            "    WHEN srs.type = 2 AND srs.review_due_at &lt;= NOW() THEN 1",
            "    WHEN srs.type = 0 THEN 2",
            "    </if>",
            "    WHEN srs.type IN (1, 3) THEN 3",
            "    ELSE 4",
            "  END,",
            "  srs.reappear_at ASC,",
            "  srs.review_due_at ASC,",
            "  srs.id ASC",
            "LIMIT 1",
            "</script>"})
    UserCardSrsDO getNextCardByCourse(@Param("userId") long userId,
                                       @Param("courseId") long courseId,
                                       @Param("reviewCardCount") long reviewCardCount,
                                       @Param("newFirst") boolean newFirst);

    // ========== 复习统计 ==========

    /**
     * 统计指定日期范围内复习过的卡片数（用于"今日已复习"）
     */
    @Select("SELECT COUNT(*) FROM user_card_srs " +
            "WHERE user_id = #{userId} " +
            "AND last_reviewed_at >= #{startTime} AND last_reviewed_at < #{endTime}")
    long countReviewedInRange(@Param("userId") long userId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    // ========== 按课程分组统计（替代 user_card_in_course 的统计）==========

    /**
     * 批量获取多个课程的卡片统计
     */
    @Select({"<script>",
          "SELECT",
          "    srs.course_id AS courseId,",
          "    COUNT(srs.card_id) AS cardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} THEN 1 ELSE 0 END) AS dueCardCount,",
          "    SUM(CASE WHEN srs.repetitions = 0 THEN 1 ELSE 0 END) AS newCardCount,",
          "    SUM(CASE WHEN srs.repetitions &gt; 0 THEN 1 ELSE 0 END) AS learnedCardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} AND srs.repetitions > 0 THEN 1 ELSE 0 END) AS reviewCardCount",
          "FROM",
          "    user_card_srs srs",
          "INNER JOIN",
          "    memory_card_deck deck ON srs.deck_id = deck.id",
          "WHERE",
          "    srs.user_id = #{userId}",
          "    AND deck.state = " + Enums.ContentState.PUBLISHED_VALUE,
          "    AND srs.course_id IN",
          "    <foreach item=\"courseId\" collection=\"courseIds\" open=\"(\" separator=\",\" close=\")\">",
          "        #{courseId}",
          "    </foreach>",
          "GROUP BY",
          "    srs.course_id",
          "</script>"})
    List<CourseMemoryBankDO> getBatchCardStatsForCourses(@Param("userId") long userId,
                                                          @Param("courseIds") Collection<Long> courseIds,
                                                          @Param("now") LocalDateTime now);

    /**
     * 获取单个课程的卡片统计
     */
    @Select({"<script>",
          "SELECT",
          "    srs.course_id AS courseId,",
          "    COUNT(srs.card_id) AS cardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} THEN 1 ELSE 0 END) AS dueCardCount,",
          "    SUM(CASE WHEN srs.repetitions = 0 THEN 1 ELSE 0 END) AS newCardCount,",
          "    SUM(CASE WHEN srs.repetitions &gt; 0 THEN 1 ELSE 0 END) AS learnedCardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} AND srs.repetitions > 0 THEN 1 ELSE 0 END) AS reviewCardCount",
          "FROM",
          "    user_card_srs srs",
          "INNER JOIN",
          "    memory_card_deck deck ON srs.deck_id = deck.id",
          "WHERE",
          "    srs.user_id = #{userId}",
          "    AND srs.course_id = #{courseId}",
          "    AND deck.state = " + Enums.ContentState.PUBLISHED_VALUE,
          "</script>"})
    CourseMemoryBankDO getCardStatsForCourse(@Param("userId") long userId,
                                              @Param("courseId") long courseId,
                                              @Param("now") LocalDateTime now);

    // ========== 移动节点到课程 ==========

    /**
     * 批量更新节点下所有卡片的课程归属
     */
    @Update("UPDATE user_card_srs SET course_id = #{courseId}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND node_id = #{nodeId}")
    int updateCourseIdByUserAndNode(@Param("userId") long userId,
                                     @Param("nodeId") long nodeId,
                                     @Param("courseId") long courseId);

    /**
     * 删除用户在指定节点下来自其他卡片组的卡片
     */
    @Delete("DELETE FROM user_card_srs " +
            "WHERE user_id = #{userId} AND node_id = #{nodeId} AND deck_id != #{excludeDeckId}")
    int deleteByUserAndNodeExcludeDeck(@Param("userId") long userId,
                                        @Param("nodeId") long nodeId,
                                        @Param("excludeDeckId") long excludeDeckId);

}