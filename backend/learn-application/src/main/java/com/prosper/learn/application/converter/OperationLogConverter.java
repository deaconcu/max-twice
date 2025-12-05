package com.prosper.learn.application.converter;

import com.prosper.learn.analytics.monitoring.OperationLogDO;
import com.prosper.learn.application.dto.response.OperationLogDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 操作日志转换器
 */
@Mapper(componentModel = "spring", uses = CommonConverter.class)
public interface OperationLogConverter {

    /**
     * DTO 转 DO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OperationLogDO toDataObject(OperationLogDTO dto);

    /**
     * DO 转 DTO
     */
    OperationLogDTO toDTO(OperationLogDO dataObject);
}
