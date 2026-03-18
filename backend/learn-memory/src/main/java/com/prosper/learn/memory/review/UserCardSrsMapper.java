package com.prosper.learn.memory.review;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Delete("DELETE FROM user_card_srs WHERE user_id = #{userId} AND card_id = #{cardId}")
    int deleteByUserAndCard(long userId, long cardId);

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
            "SELECT * FROM user_card_srs",
            "WHERE user_id = #{userId}",
            "<if test='lastId != null'>AND id &gt; #{lastId}</if>",
            "ORDER BY id ASC LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getByUserWithPaging(long userId, int limit, Long lastId);

    @Select({"<script>",
            "SELECT * FROM user_card_srs",
            "WHERE user_id = #{userId} AND course_id = #{courseId}",
            "<if test='lastId != null'>AND id &gt; #{lastId}</if>",
            "ORDER BY id ASC LIMIT #{limit}",
            "</script>"})
    List<UserCardSrsDO> getByUserAndCourseWithPaging(long userId, long courseId, int limit, Long lastId);

    @Select("SELECT * FROM user_card_srs WHERE user_id = #{userId} AND deck_id = #{deckId}")
    List<UserCardSrsDO> getByUserAndDeckId(long userId, long deckId);

    @Select("SELECT * FROM user_card_srs WHERE user_id = #{userId} AND node_id = #{nodeId}")
    List<UserCardSrsDO> getByUserAndNodeId(long userId, long nodeId);

    // ========== 新的复习逻辑：基于卡片计数的调度（分步查询，更高效） ==========

    // ----- 全部课程 -----

    /**
     * 获取下一张学习中的卡片（LEARNING/RELEARNING 且 reappear_at 已到）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND type IN (1, 3) AND reappear_at <= #{reviewCardCount} " +
            "ORDER BY reappear_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextLearningCard(@Param("userId") long userId,
                                       @Param("reviewCardCount") long reviewCardCount);

    /**
     * 获取下一张复习卡片（REVIEW 且 review_due_at 已到期）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND type = 2 AND review_due_at <= NOW() " +
            "ORDER BY review_due_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextReviewCard(@Param("userId") long userId);

    /**
     * 获取下一张新卡片（NEW）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND type = 0 " +
            "ORDER BY id ASC LIMIT 1")
    UserCardSrsDO getNextNewCard(@Param("userId") long userId);

    /**
     * 获取下一张待学习的卡片（LEARNING/RELEARNING 且 reappear_at 未到，兜底用）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND type IN (1, 3) AND reappear_at > #{reviewCardCount} " +
            "ORDER BY reappear_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextPendingLearningCard(@Param("userId") long userId,
                                              @Param("reviewCardCount") long reviewCardCount);

    // ----- 指定课程 -----

    /**
     * 获取下一张学习中的卡片（指定课程）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND course_id = #{courseId} AND type IN (1, 3) AND reappear_at <= #{reviewCardCount} " +
            "ORDER BY reappear_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextLearningCardByCourse(@Param("userId") long userId,
                                               @Param("courseId") long courseId,
                                               @Param("reviewCardCount") long reviewCardCount);

    /**
     * 获取下一张复习卡片（指定课程）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND course_id = #{courseId} AND type = 2 AND review_due_at <= NOW() " +
            "ORDER BY review_due_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextReviewCardByCourse(@Param("userId") long userId,
                                             @Param("courseId") long courseId);

    /**
     * 获取下一张新卡片（指定课程）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND course_id = #{courseId} AND type = 0 " +
            "ORDER BY id ASC LIMIT 1")
    UserCardSrsDO getNextNewCardByCourse(@Param("userId") long userId,
                                          @Param("courseId") long courseId);

    /**
     * 获取下一张待学习的卡片（指定课程，兜底用）
     */
    @Select("SELECT * FROM user_card_srs " +
            "WHERE user_id = #{userId} AND course_id = #{courseId} AND type IN (1, 3) AND reappear_at > #{reviewCardCount} " +
            "ORDER BY reappear_at ASC, id ASC LIMIT 1")
    UserCardSrsDO getNextPendingLearningCardByCourse(@Param("userId") long userId,
                                                      @Param("courseId") long courseId,
                                                      @Param("reviewCardCount") long reviewCardCount);

    // ========== 复习统计 ==========

    /**
     * 统计用户的卡片总数
     */
    @Select("SELECT COUNT(*) FROM user_card_srs WHERE user_id = #{userId}")
    long countByUser(@Param("userId") long userId);

    /**
     * 统计指定日期范围内复习过的卡片数（用于"今日已复习"）
     */
    @Select("SELECT COUNT(*) FROM user_card_srs " +
            "WHERE user_id = #{userId} " +
            "AND last_reviewed_at >= #{startTime} AND last_reviewed_at < #{endTime}")
    long countReviewedInRange(@Param("userId") long userId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    // ========== 按课程分组统计 ==========

    /**
     * 批量获取多个课程的卡片统计
     * dueCardCount: LEARNING/RELEARNING（正在学习中）+ REVIEW且到期的卡片（不含新卡）
     * newCardCount: NEW 类型（type=0）的卡片
     */
    @Select({"<script>",
          "SELECT",
          "    course_id AS courseId,",
          "    COUNT(card_id) AS cardCount,",
          "    SUM(CASE WHEN type IN (1, 3) OR (type = 2 AND review_due_at &lt;= #{now}) THEN 1 ELSE 0 END) AS dueCardCount,",
          "    SUM(CASE WHEN type = 0 THEN 1 ELSE 0 END) AS newCardCount,",
          "    SUM(CASE WHEN type != 0 THEN 1 ELSE 0 END) AS learnedCardCount,",
          "    SUM(CASE WHEN type = 2 AND review_due_at &lt;= #{now} THEN 1 ELSE 0 END) AS reviewCardCount",
          "FROM user_card_srs",
          "WHERE user_id = #{userId}",
          "    AND course_id IN",
          "    <foreach item=\"courseId\" collection=\"courseIds\" open=\"(\" separator=\",\" close=\")\">",
          "        #{courseId}",
          "    </foreach>",
          "GROUP BY course_id",
          "</script>"})
    List<CourseMemoryBankDO> getBatchCardStatsForCourses(@Param("userId") long userId,
                                                          @Param("courseIds") Collection<Long> courseIds,
                                                          @Param("now") LocalDateTime now);

    /**
     * 获取单个课程的卡片统计
     * dueCardCount: LEARNING/RELEARNING（正在学习中）+ REVIEW且到期的卡片（不含新卡）
     * newCardCount: NEW 类型（type=0）的卡片
     */
    @Select("SELECT " +
          "    course_id AS courseId, " +
          "    COUNT(card_id) AS cardCount, " +
          "    SUM(CASE WHEN type IN (1, 3) OR (type = 2 AND review_due_at <= #{now}) THEN 1 ELSE 0 END) AS dueCardCount, " +
          "    SUM(CASE WHEN type = 0 THEN 1 ELSE 0 END) AS newCardCount, " +
          "    SUM(CASE WHEN type != 0 THEN 1 ELSE 0 END) AS learnedCardCount, " +
          "    SUM(CASE WHEN type = 2 AND review_due_at <= #{now} THEN 1 ELSE 0 END) AS reviewCardCount " +
          "FROM user_card_srs " +
          "WHERE user_id = #{userId} AND course_id = #{courseId}")
    CourseMemoryBankDO getCardStatsForCourse(@Param("userId") long userId,
                                              @Param("courseId") long courseId,
                                              @Param("now") LocalDateTime now);

    // ========== 优化统计查询（利用索引 user_id, type, course_id, review_due_at）==========

    /**
     * 批量统计新卡片数（type=0），每个课程有独立的 LIMIT
     */
    @Select({"<script>",
          "<foreach item=\"param\" collection=\"params\" separator=\" UNION ALL \">",
          "    SELECT #{param.courseId} AS courseId,",
          "           (SELECT COUNT(*) FROM (SELECT 1 FROM user_card_srs",
          "            WHERE user_id = #{userId} AND type = 0 AND course_id = #{param.courseId}",
          "            LIMIT #{param.newLimit}) t) AS newCardCount",
          "</foreach>",
          "</script>"})
    List<CourseMemoryBankDO> countNewCardsWithLimit(@Param("userId") long userId,
                                                     @Param("params") List<CourseQueryParam> params);

    /**
     * 批量统计 REVIEW 且到期的卡片数，每个课程有独立的 LIMIT
     */
    @Select({"<script>",
          "<foreach item=\"param\" collection=\"params\" separator=\" UNION ALL \">",
          "    SELECT #{param.courseId} AS courseId,",
          "           (SELECT COUNT(*) FROM (SELECT 1 FROM user_card_srs",
          "            WHERE user_id = #{userId} AND type = 2 AND course_id = #{param.courseId}",
          "            AND review_due_at &lt;= #{now}",
          "            LIMIT #{param.reviewLimit}) t) AS reviewCardCount",
          "</foreach>",
          "</script>"})
    List<CourseMemoryBankDO> countReviewDueCardsWithLimit(@Param("userId") long userId,
                                                          @Param("params") List<CourseQueryParam> params,
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