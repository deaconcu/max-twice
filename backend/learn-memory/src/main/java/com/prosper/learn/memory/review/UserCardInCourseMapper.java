package com.prosper.learn.memory.review;

import com.prosper.learn.persistence.dataobject.CourseMemoryBankDO;
import com.prosper.learn.persistence.dataobject.UserCardInCourseDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.prosper.learn.common.Enums.ContentState.*;

public interface UserCardInCourseMapper {

    @Select("SELECT * FROM user_card_in_course WHERE id = #{id}")
    UserCardInCourseDO get(long id);

    @Select("SELECT * FROM user_card_in_course " +
            "WHERE user_id = #{userId} AND card_id = #{cardId} AND course_id = #{courseId}")
    UserCardInCourseDO getByUserCardAndCourse(long userId, long cardId, long courseId);

    @Select({"<script>SELECT * FROM user_card_in_course WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserCardInCourseDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM user_card_in_course WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserCardInCourseDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM user_card_in_course WHERE user_id = #{userId} AND course_id = #{courseId} " +
            "ORDER BY created_at DESC")
    List<UserCardInCourseDO> getByUserAndCourse(long userId, long courseId);

    @Select("SELECT * FROM user_card_in_course WHERE user_id = #{userId} AND card_id = #{cardId}")
    List<UserCardInCourseDO> getByUserAndCard(long userId, long cardId);

    @Select("SELECT * FROM user_card_in_course WHERE course_id = #{courseId}")
    List<UserCardInCourseDO> getByCourse(long courseId);

    @Select("SELECT DISTINCT course_id FROM user_card_in_course WHERE user_id = #{userId}")
    List<Long> getCourseIdsByUser(long userId);

    @Select("SELECT DISTINCT card_id FROM user_card_in_course " +
            "WHERE user_id = #{userId} AND course_id = #{courseId}")
    List<Long> getCardIdsByUserAndCourse(long userId, long courseId);

    @Insert("INSERT INTO user_card_in_course " +
            "(user_id, card_id, deck_id, course_id) " +
            "VALUES " +
            "(#{userId}, #{cardId}, #{deckId}, #{courseId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCardInCourseDO relation);

    @Insert({"<script>INSERT INTO user_card_in_course (user_id, card_id, deck_id, course_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.cardId}, #{item.deckId}, #{item.courseId})" +
            "</foreach>" +
            "</script>"})
    int batchInsert(List<UserCardInCourseDO> relations);

    @Delete("DELETE FROM user_card_in_course WHERE id = #{id}")
    int deleteById(long id);

    @Delete("DELETE FROM user_card_in_course " +
            "WHERE user_id = #{userId} AND card_id = #{cardId} AND course_id = #{courseId}")
    int deleteByUserCardAndCourse(long userId, long cardId, long courseId);

    @Delete("DELETE FROM user_card_in_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserAndCourse(long userId, long courseId);

    @Delete("DELETE FROM user_card_in_course WHERE user_id = #{userId} AND card_id = #{cardId}")
    int deleteByUserAndCard(long userId, long cardId);

    @Select("SELECT COUNT(*) FROM user_card_in_course WHERE user_id = #{userId}")
    int countByUser(long userId);

    @Select("SELECT COUNT(*) FROM user_card_in_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    int countByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_in_course WHERE course_id = #{courseId}")
    int countByCourse(long courseId);

    @Select({"<script>",
          "SELECT",
          "    ucc.course_id AS courseId,",
          "    COUNT(ucc.card_id) AS cardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} THEN 1 ELSE 0 END) AS dueCardCount,",
          "    SUM(CASE WHEN srs.repetitions = 0 THEN 1 ELSE 0 END) AS newCardCount,",
          "    SUM(CASE WHEN srs.repetitions &gt; 0 THEN 1 ELSE 0 END) AS learnedCardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} AND srs.repetitions > 0 THEN 1 ELSE 0 END) AS reviewCardCount",
          "FROM",
          "    user_card_in_course ucc",
          "INNER JOIN",
          "    memory_card_deck deck ON ucc.deck_id = deck.id",
          "LEFT JOIN",
          "    user_card_srs srs ON ucc.user_id = srs.user_id AND ucc.card_id = srs.card_id",
          "WHERE",
          "    ucc.user_id = #{userId}",
          "    AND deck.state = " + ContentState.PUBLISHED_VALUE,
          "    AND ucc.course_id IN",
          "    <foreach item=\"courseId\" collection=\"courseIds\" open=\"(\" separator=\",\" close=\")\">",
          "        #{courseId}",
          "    </foreach>",
          "GROUP BY",
          "    ucc.course_id",
          "</script>"})
    List<CourseMemoryBankDO> getBatchCardStatsForCourses(@Param("userId") Long userId, @Param("courseIds") Set<Long> courseIds, @Param("now") LocalDateTime now);

    @Select({"<script>",
          "SELECT",
          "    ucc.course_id AS courseId,",
          "    COUNT(ucc.card_id) AS cardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} THEN 1 ELSE 0 END) AS dueCardCount,",
          "    SUM(CASE WHEN srs.repetitions = 0 THEN 1 ELSE 0 END) AS newCardCount,",
          "    SUM(CASE WHEN srs.repetitions &gt; 0 THEN 1 ELSE 0 END) AS learnedCardCount,",
          "    SUM(CASE WHEN srs.review_due_at &lt;= #{now} AND srs.repetitions > 0 THEN 1 ELSE 0 END) AS reviewCardCount",
          "FROM",
          "    user_card_in_course ucc",
          "INNER JOIN",
          "    memory_card_deck deck ON ucc.deck_id = deck.id",
          "LEFT JOIN",
          "    user_card_srs srs ON ucc.user_id = srs.user_id AND ucc.card_id = srs.card_id",
          "WHERE",
          "    ucc.user_id = #{userId}",
          "    AND ucc.course_id = #{courseId}",
          "    AND deck.state = " + ContentState.PUBLISHED_VALUE,
          "</script>"})
    CourseMemoryBankDO getCardStatsForCourses(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("now") LocalDateTime now);

    @Insert("""
          <script>
          INSERT IGNORE INTO user_card_in_course (user_id, card_id, deck_id, course_id, created_at)
          VALUES
          <foreach collection="cardIds" item="cardId" separator=",">
              (#{userId}, #{cardId}, #{deckId}, #{courseId}, NOW())
          </foreach>
          </script>
          """)
    int batchInsertIgnore(@Param("userId") Long userId,
                         @Param("deckId") Long deckId,
                         @Param("courseId") Long courseId,
                         @Param("cardIds") List<Long> cardIds);

    @Delete("""
          <script>
          DELETE FROM user_card_in_course 
          WHERE user_id = #{userId} AND course_id = #{courseId} AND card_id IN
          <foreach collection="cardIds" item="cardId" open="(" separator="," close=")">
              #{cardId}
          </foreach>
          </script>
          """)
    int batchDeleteByUserCourseAndCards(@Param("userId") Long userId, 
                                       @Param("courseId") Long courseId, 
                                       @Param("cardIds") List<Long> cardIds);

    @Select("""
          <script>
          SELECT DISTINCT card_id FROM user_card_in_course 
          WHERE user_id = #{userId} AND card_id IN
          <foreach collection="cardIds" item="cardId" open="(" separator="," close=")">
              #{cardId}
          </foreach>
          </script>
          """)
    List<Long> getExistingCardIdsByUserAndCards(@Param("userId") Long userId, 
                                               @Param("cardIds") List<Long> cardIds);


}