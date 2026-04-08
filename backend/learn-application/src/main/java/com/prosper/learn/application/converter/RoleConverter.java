package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.role.RoleAdminDTO;
import com.prosper.learn.application.dto.response.role.RoleBriefDTO;
import com.prosper.learn.application.dto.response.role.RoleDTO;
import com.prosper.learn.content.role.RoleDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface RoleConverter {

    @Named("toDTO")
    RoleDTO toDTO(RoleDO roleDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<RoleDTO> toDTO(List<RoleDO> roleDOList);

    @Named("toBriefDTO")
    RoleBriefDTO toBriefDTO(RoleDO roleDO);

    @IterableMapping(qualifiedByName = "toBriefDTO")
    List<RoleBriefDTO> toBriefDTO(List<RoleDO> roleDOList);

    @Named("toAdminDTO")
    RoleAdminDTO toAdminDTO(RoleDO roleDO);

    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<RoleAdminDTO> toAdminDTO(List<RoleDO> roleDOList);
}