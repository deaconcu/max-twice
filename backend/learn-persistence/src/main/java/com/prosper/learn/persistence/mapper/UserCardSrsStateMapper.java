package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserCardSrsStateDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserCardSrsStateMapper {

    @Select("SELECT * FROM user_card_srs_state WHERE id = #{id}")
    UserCardSrsStateDO get(long id);

    @Select("SELECT * FROM user_card_srs_state WHERE user_id = #{userId} AND card_id = #{cardId}")
    UserCardSrsStateDO getByUserAndCard(long userId, long cardId);

    @Select({"<script>SELECT * FROM user_card_srs_state WHERE user_id = #{userId} AND card_id IN " +
            "<foreach item='cardId' collection='cardIds' open='(' separator=', ' close=')'>#{cardId}</foreach>" +
            "</script>"})
    List<UserCardSrsStateDO> getByUserAndCards(long userId, Collection<Long> cardIds);

    @Select({"<script>SELECT * FROM user_card_srs_state WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserCardSrsStateDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM user_card_srs_state WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserCardSrsStateDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT srs.* FROM user_card_srs_state srs " +
            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= #{dueTime} " +
            "AND EXISTS (" +
            "    SELECT 1 FROM user_card_in_course ctx " +
            "    JOIN user_course_srs_setting s ON ctx.course_id = s.course_id AND ctx.user_id = s.user_id " +
            "    WHERE ctx.card_id = srs.card_id AND s.status = 1 AND s.user_id = srs.user_id" +
            ") " +
            "ORDER BY srs.review_due_at ASC LIMIT #{limit}")
    List<UserCardSrsStateDO> getDueCardsForReview(long userId, LocalDateTime dueTime, int limit);

    @Select("SELECT srs.* FROM user_card_srs_state srs " +
            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= #{dueTime} " +
            "AND EXISTS (" +
            "    SELECT 1 FROM user_card_in_course ctx " +
            "    WHERE ctx.card_id = srs.card_id AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId}" +
            ") " +
            "ORDER BY srs.review_due_at ASC LIMIT #{limit}")
    List<UserCardSrsStateDO> getDueCardsByCourseForReview(long userId, long courseId, LocalDateTime dueTime, int limit);

    @Select("SELECT * FROM user_card_srs_state WHERE user_id = #{userId} " +
            "ORDER BY review_due_at ASC LIMIT #{limit}")
    List<UserCardSrsStateDO> getByUser(long userId, int limit);

    @Select("SELECT * FROM user_card_srs_state " +
            "WHERE user_id = #{userId} AND card_id IN " +
            "(SELECT card_id FROM user_card_in_course WHERE course_id = #{courseId} AND user_id = #{userId}) " +
            "ORDER BY review_due_at ASC")
    List<UserCardSrsStateDO> getByUserAndCourse(long userId, long courseId);

    @Insert("INSERT INTO user_card_srs_state " +
            "(user_id, card_id, deck_version, card_version_id, review_due_at, last_reviewed_at, " +
            "interval_days, ease_factor, repetitions, lapse_count) " +
            "VALUES " +
            "(#{userId}, #{cardId}, #{deckVersion}, #{cardVersionId}, #{reviewDueAt}, #{lastReviewedAt}, " +
            "#{intervalDays}, #{easeFactor}, #{repetitions}, #{lapseCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCardSrsStateDO state);

    @Update("UPDATE user_card_srs_state SET " +
            "review_due_at = #{reviewDueAt}, last_reviewed_at = #{lastReviewedAt}, " +
            "interval_days = #{intervalDays}, ease_factor = #{easeFactor}, " +
            "repetitions = #{repetitions}, lapse_count = #{lapseCount} " +
            "WHERE id = #{id}")
    void update(UserCardSrsStateDO state);

    @Update("UPDATE user_card_srs_state SET review_due_at = #{reviewDueAt} WHERE id = #{id}")
    int updateReviewDueAt(long id, LocalDateTime reviewDueAt);

    @Update("UPDATE user_card_srs_state SET " +
            "review_due_at = #{reviewDueAt}, last_reviewed_at = NOW(), " +
            "interval_days = #{intervalDays}, ease_factor = #{easeFactor}, " +
            "repetitions = #{repetitions}, lapse_count = #{lapseCount} " +
            "WHERE user_id = #{userId} AND card_id = #{cardId}")
    int updateAfterReview(long userId, long cardId, LocalDateTime reviewDueAt, 
                         int intervalDays, java.math.BigDecimal easeFactor, int repetitions, int lapseCount);

    @Delete("DELETE FROM user_card_srs_state WHERE user_id = #{userId} AND card_id = #{cardId}")
    int deleteByUserAndCard(long userId, long cardId);

    @Select("SELECT COUNT(*) FROM user_card_srs_state WHERE user_id = #{userId}")
    int countByUser(long userId);

    @Select("SELECT COUNT(*) FROM user_card_srs_state " +
            "WHERE user_id = #{userId} AND review_due_at <= #{dueTime}")
    int countDueCards(long userId, LocalDateTime dueTime);

    @Select("SELECT COUNT(*) FROM user_card_srs_state srs " +
            "WHERE srs.user_id = #{userId} AND srs.review_due_at <= NOW() " +
            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
    long countDueCardsByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_in_course ctx " +
            "WHERE ctx.user_id = #{userId} AND ctx.course_id = #{courseId} " +
            "AND NOT EXISTS (SELECT 1 FROM user_card_srs_state srs WHERE srs.card_id = ctx.card_id " +
            "AND srs.user_id = #{userId})")
    long countNewCardsByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_srs_state srs " +
            "WHERE srs.user_id = #{userId} AND srs.review_due_at > NOW() " +
            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
    long countReviewCardsByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_srs_state srs " +
            "WHERE srs.user_id = #{userId} AND srs.repetitions >= 3 " +
            "AND EXISTS (SELECT 1 FROM user_card_in_course ctx WHERE ctx.card_id = srs.card_id " +
            "AND ctx.user_id = #{userId} AND ctx.course_id = #{courseId})")
    long countLearnedCardsByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_srs_state " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    long countReviewsInPeriod(long userId, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT DATEDIFF(CURDATE(), DATE(MAX(last_reviewed_at))) AS days_since_last " +
            "FROM user_card_srs_state WHERE user_id = #{userId} AND last_reviewed_at IS NOT NULL")
    int calculateStreakDays(long userId);

    @Select("SELECT AVG(repetitions * 1.0) FROM user_card_srs_state " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    Double calculateAverageScore(long userId, LocalDateTime startTime, LocalDateTime endTime);

    @Select("SELECT COUNT(*) * 30000 FROM user_card_srs_state " +
            "WHERE user_id = #{userId} AND last_reviewed_at BETWEEN #{startTime} AND #{endTime}")
    Long calculateTimeSpent(long userId, LocalDateTime startTime, LocalDateTime endTime);

}