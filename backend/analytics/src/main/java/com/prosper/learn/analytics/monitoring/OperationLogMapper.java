package com.prosper.learn.analytics.monitoring;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     */
    @Insert("INSERT INTO operation_log (operator_id, operator_name, operator_role, module, operation_type, " +
            "operation_level, target_type, target_id, target_name, reason, extra_data, ip_address, created_at) " +
            "VALUES (#{operatorId}, #{operatorName}, #{operatorRole}, #{module}, #{operationType}, " +
            "#{operationLevel}, #{targetType}, #{targetId}, #{targetName}, #{reason}, #{extraData}, #{ipAddress}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OperationLogDO log);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM operation_log WHERE id = #{id}")
    OperationLogDO getById(long id);

    /**
     * 基于游标的分页查询操作日志（keyset分页）
     */
    @Select("<script>" +
            "SELECT * FROM operation_log " +
            "WHERE 1=1 " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='targetType != null and targetType != \"\"'>AND target_type = #{targetType}</if> " +
            "<if test='targetId != null'>AND target_id = #{targetId}</if> " +
            "<if test='endTime != null'>AND created_at &lt;= #{endTime}</if> " +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<OperationLogDO> queryLogsByLastId(@Param("operatorId") Long operatorId,
                                           @Param("targetType") String targetType,
                                           @Param("targetId") Long targetId,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("lastId") Long lastId,
                                           @Param("limit") int limit);
}
