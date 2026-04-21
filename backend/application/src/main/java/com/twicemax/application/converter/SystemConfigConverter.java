package com.twicemax.application.converter;

import com.twicemax.application.dto.response.SystemConfigDTO;
import com.twicemax.shared.infrastructure.config.SystemDO;
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
