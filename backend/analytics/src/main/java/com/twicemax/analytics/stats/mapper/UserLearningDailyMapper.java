package com.twicemax.analytics.stats.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户每日学习统计 Mapper
 *
 * 表结构：
 * CREATE TABLE user_learning_daily (
 *     user_id BIGINT NOT NULL,
 *     stat_date DATE NOT NULL,
 *     completed_nodes INT DEFAULT 0,
 *     cancel_completed_nodes INT DEFAULT 0,
 *     reviewed_cards INT DEFAULT 0,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 *     PRIMARY KEY (user_id, stat_date)
 * );
 */
@Mapper
public interface UserLearningDailyMapper {

    // ===== 写入操作 =====

    /**
     * 增加完成节点数（当天有记录则累加，没有则新建）
     */
    @Insert("INSERT INTO user_learning_daily (user_id, stat_date, completed_nodes, cancel_completed_nodes, reviewed_cards) " +
            "VALUES (#{userId}, #{statDate}, #{count}, 0, 0) " +
            "ON DUPLICATE KEY UPDATE " +
            "completed_nodes = completed_nodes + #{count}, " +
            "updated_at = NOW()")
    int incrementCompletedNodes(@Param("userId") long userId,
                                @Param("statDate") LocalDate statDate,
                                @Param("count") int count);

    /**
     * 增加取消完成节点数（当天有记录则累加，没有则新建）
     */
    @Insert("INSERT INTO user_learning_daily (user_id, stat_date, completed_nodes, cancel_completed_nodes, reviewed_cards) " +
            "VALUES (#{userId}, #{statDate}, 0, #{count}, 0) " +
            "ON DUPLICATE KEY UPDATE " +
            "cancel_completed_nodes = cancel_completed_nodes + #{count}, " +
            "updated_at = NOW()")
    int incrementCancelCompletedNodes(@Param("userId") long userId,
                                      @Param("statDate") LocalDate statDate,
                                      @Param("count") int count);

    /**
     * 增加复习卡片数（当天有记录则累加，没有则新建）
     */
    @Insert("INSERT INTO user_learning_daily (user_id, stat_date, completed_nodes, cancel_completed_nodes, reviewed_cards) " +
            "VALUES (#{userId}, #{statDate}, 0, 0, #{count}) " +
            "ON DUPLICATE KEY UPDATE " +
            "reviewed_cards = reviewed_cards + #{count}, " +
            "updated_at = NOW()")
    int incrementReviewedCards(@Param("userId") long userId,
                               @Param("statDate") LocalDate statDate,
                               @Param("count") int count);

    // ===== 查询操作 =====

    /**
     * 获取用户指定日期的学习数据
     */
    @Select("SELECT * FROM user_learning_daily " +
            "WHERE user_id = #{userId} AND stat_date = #{statDate}")
    UserLearningDailyDO getByUserIdAndDate(@Param("userId") long userId,
                                           @Param("statDate") LocalDate statDate);

    /**
     * 获取用户日期范围内的学习数据（用于热力图今日数据）
     */
    @Select("SELECT * FROM user_learning_daily " +
            "WHERE user_id = #{userId} AND stat_date >= #{startDate} AND stat_date <= #{endDate} " +
            "ORDER BY stat_date")
    List<UserLearningDailyDO> getByUserIdAndDateRange(@Param("userId") long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    // ===== 同步操作 =====

    /**
     * 获取指定日期所有用户的学习数据（用于每日同步任务）
     */
    @Select("SELECT * FROM user_learning_daily WHERE stat_date = #{statDate}")
    List<UserLearningDailyDO> getAllByDate(@Param("statDate") LocalDate statDate);

    /**
     * 删除指定日期的数据（同步到 yearly 表后清理）
     */
    @Delete("DELETE FROM user_learning_daily WHERE stat_date = #{statDate}")
    int deleteByDate(@Param("statDate") LocalDate statDate);

    /**
     * 删除指定日期之前的数据（清理历史数据）
     */
    @Delete("DELETE FROM user_learning_daily WHERE stat_date < #{beforeDate}")
    int deleteBeforeDate(@Param("beforeDate") LocalDate beforeDate);
}
