package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "password")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "emailValidated")
    @Mapping(target = "biography")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserDTO toDTO(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserDTO> toDTO(List<UserDO> userDOList);

    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "biography")
    UserDTO toDTOV1(UserDO userDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    List<UserDTO> toDTOV1(List<UserDO> userDOList);

    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    UserDTO toDTOV2(UserDO userDO);

    @IterableMapping(qualifiedByName = "toDTOV2")
    List<UserDTO> toDTOV2(List<UserDO> userDOList);

    default UserDTO toDTOV1(UserDO userDO, boolean followed) {
        if (userDO == null) return null;

        UserDTO dto = toDTOV1(userDO);
        dto.setFollowed(followed);
        return dto;
    }
}