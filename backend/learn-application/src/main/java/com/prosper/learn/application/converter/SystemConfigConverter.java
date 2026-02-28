package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.SystemConfigDTO;
import com.prosper.learn.shared.infrastructure.config.SystemDO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 系统配置转换器
 */
@Mapper(componentModel = "spring")
public interface SystemConfigConverter {

    SystemConfigDTO toDTO(SystemDO systemDO);

    List<SystemConfigDTO> toDTOList(List<SystemDO> systemDOs);
}
