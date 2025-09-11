package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserCardInCourseDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
            "(user_id, card_id, course_id) " +
            "VALUES " +
            "(#{userId}, #{cardId}, #{courseId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCardInCourseDO relation);

    @Insert({"<script>INSERT INTO user_card_in_course (user_id, card_id, course_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.cardId}, #{item.courseId})" +
            "</foreach>" +
            "</script>"})
    int batchInsert(List<UserCardInCourseDO> relations);

    @Delete("DELETE FROM user_card_in_course " +
            "WHERE user_id = #{userId} AND card_id = #{cardId} AND course_id = #{courseId}")
    int deleteByUserCardAndCourse(long userId, long cardId, long courseId);

    @Delete("DELETE FROM user_card_in_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserAndCourse(long userId, long courseId);

    @Delete("DELETE FROM user_card_in_course WHERE user_id = #{userId} AND card_id = #{cardId}")
    int deleteByUserAndCard(long userId, long cardId);

    @Select("SELECT COUNT(*) FROM user_card_in_course WHERE user_id = #{userId}")
    int countByUser(long userId);

    @Select("SELECT COUNT(*) FROM user_card_in_course " +
            "WHERE user_id = #{userId} AND course_id = #{courseId}")
    int countByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_card_in_course WHERE course_id = #{courseId}")
    int countByCourse(long courseId);

}