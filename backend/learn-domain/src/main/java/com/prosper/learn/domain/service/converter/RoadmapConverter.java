package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.domain.service.data.UserDataService;
import com.prosper.learn.domain.service.data.ProfessionDataService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RoadmapConverter {

    @Autowired
    protected UserDataService userDataService;
    @Autowired
    protected UserConverter userConverter;
    @Autowired
    protected ProfessionDataService professionDataService;
    @Autowired
    protected ProfessionConverter professionConverter;
    
    @Named("toDTO")
    public abstract RoadmapDTO toDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<RoadmapDTO> toDTO(List<RoadmapDO> roadmapDOList);

    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "profession", source = "professionId", qualifiedByName = "getProfession")
    @Mapping(target = "description")
    @Mapping(target = "vote")
    @Mapping(target = "comment")
    //@Mapping(target = "upvoted")
    @Mapping(target = "creator", source = "creatorId", qualifiedByName = "getCreator")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    public abstract RoadmapDTO toDTOV2(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDTOV2")
    public abstract List<RoadmapDTO> toDTOV2(List<RoadmapDO> roadmapDOList);

    @Named("getCreator")
    public UserDTO getCreator(Long creatorId) {
        if (creatorId == null) return null;
        return userConverter.toDTOV4(userDataService.getById(creatorId));
    }
    
    @Named("getProfession")
    public ProfessionDTO getProfession(Long professionId) {
        if (professionId == null) return null;
        return professionConverter.toDTO(professionDataService.getById(professionId));
    }
}