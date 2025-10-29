package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.OperationLogDO;
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
    OperationLogDO getById(Long id);

    /**
     * 分页查询操作日志
     */
    @Select("<script>" +
            "SELECT * FROM operation_log " +
            "WHERE 1=1 " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='module != null and module != \"\"'>AND module = #{module}</if> " +
            "<if test='operationType != null and operationType != \"\"'>AND operation_type = #{operationType}</if> " +
            "<if test='operationLevel != null'>AND operation_level = #{operationLevel}</if> " +
            "<if test='targetType != null and targetType != \"\"'>AND target_type = #{targetType}</if> " +
            "<if test='targetId != null'>AND target_id = #{targetId}</if> " +
            "<if test='startTime != null'>AND created_at &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND created_at &lt;= #{endTime}</if> " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<OperationLogDO> queryLogs(@Param("operatorId") Long operatorId,
                                    @Param("module") String module,
                                    @Param("operationType") String operationType,
                                    @Param("operationLevel") Integer operationLevel,
                                    @Param("targetType") String targetType,
                                    @Param("targetId") Long targetId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计符合条件的日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_log " +
            "WHERE 1=1 " +
            "<if test='operatorId != null'>AND operator_id = #{operatorId}</if> " +
            "<if test='module != null and module != \"\"'>AND module = #{module}</if> " +
            "<if test='operationType != null and operationType != \"\"'>AND operation_type = #{operationType}</if> " +
            "<if test='operationLevel != null'>AND operation_level = #{operationLevel}</if> " +
            "<if test='targetType != null and targetType != \"\"'>AND target_type = #{targetType}</if> " +
            "<if test='targetId != null'>AND target_id = #{targetId}</if> " +
            "<if test='startTime != null'>AND created_at &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND created_at &lt;= #{endTime}</if>" +
            "</script>")
    int countLogs(@Param("operatorId") Long operatorId,
                  @Param("module") String module,
                  @Param("operationType") String operationType,
                  @Param("operationLevel") Integer operationLevel,
                  @Param("targetType") String targetType,
                  @Param("targetId") Long targetId,
                  @Param("startTime") LocalDateTime startTime,
                  @Param("endTime") LocalDateTime endTime);
}
