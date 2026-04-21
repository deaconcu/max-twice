package com.twicemax.analytics.monitoring;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误日志Mapper
 */
@Mapper
public interface ErrorLogMapper {

    /**
     * 根据指纹查询
     */
    @Select("SELECT * FROM error_log WHERE fingerprint = #{fingerprint}")
    ErrorLogDO getByFingerprint(String fingerprint);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM error_log WHERE id = #{id}")
    ErrorLogDO getById(Long id);

    /**
     * 插入错误日志
     */
    @Insert("INSERT INTO error_log (fingerprint, source, error_type, message, stack_trace, " +
            "url, user_id, ip, user_agent, extra_data, count, first_seen_at, last_seen_at, status) " +
            "VALUES (#{fingerprint}, #{source}, #{errorType}, #{message}, #{stackTrace}, " +
            "#{url}, #{userId}, #{ip}, #{userAgent}, #{extraData}, #{count}, #{firstSeenAt}, #{lastSeenAt}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ErrorLogDO log);

    /**
     * 更新错误日志（count+1，更新lastSeenAt）
     */
    @Update("UPDATE error_log SET count = count + 1, last_seen_at = #{lastSeenAt} WHERE id = #{id}")
    void incrementCount(@Param("id") Long id, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    /**
     * 更新状态
     */
    @Update("UPDATE error_log SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 查询错误日志列表（按最近发生时间倒序）
     */
    @Select("<script>" +
            "SELECT * FROM error_log " +
            "WHERE 1=1 " +
            "<if test='source != null'>AND source = #{source}</if> " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY last_seen_at DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<ErrorLogDO> queryLogs(@Param("source") String source,
                               @Param("status") String status,
                               @Param("lastId") Long lastId,
                               @Param("limit") int limit);

    /**
     * 删除过期的错误日志
     */
    @Delete("DELETE FROM error_log WHERE last_seen_at < #{expireTime}")
    int deleteExpired(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 统计未处理的错误数量
     */
    @Select("SELECT COUNT(*) FROM error_log WHERE status = 'new'")
    int countNew();
}
