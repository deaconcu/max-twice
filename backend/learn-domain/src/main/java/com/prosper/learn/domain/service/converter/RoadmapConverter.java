package com.prosper.learn.domain.service.converter;

import com.prosper.learn.domain.service.business.UpvoteService;
import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.domain.service.data.UserDataService;
import com.prosper.learn.domain.service.data.ProfessionDataService;
import com.prosper.learn.domain.service.business.RoadmapService;
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
    @Autowired
    protected UpvoteService upvoteService;
    @Autowired
    protected RoadmapService roadmapService;
    
    @Named("toDTO")
    public abstract RoadmapDTO toDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<RoadmapDTO> toDTO(List<RoadmapDO> roadmapDOList);

    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content", source = "content", qualifiedByName = "getFormattedContent")
    @Mapping(target = "profession", source = "professionId", qualifiedByName = "getProfession")
    @Mapping(target = "description")
    @Mapping(target = "vote")
    @Mapping(target = "comment")
    @Mapping(target = "upvoted", source = "id", qualifiedByName = "getUpvotedStatus")
    @Mapping(target = "creator", source = "creatorId", qualifiedByName = "getCreator")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    public abstract RoadmapDTO toDTOV2(RoadmapDO roadmapDO, long userId);

    @IterableMapping(qualifiedByName = "toDTOV2")
    public abstract List<RoadmapDTO> toDTOV2(List<RoadmapDO> roadmapDOList, long userId);

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
    
    @Named("getUpvotedStatus")
    public Boolean getUpvotedStatus(Long roadmapId, long userId) {
        if (roadmapId == null) return false;
        return upvoteService.hasUpvotedRoadmap(roadmapId, userId);
    }
    
    @Named("getFormattedContent")
    public String getFormattedContent(String content, long userId) {
        if (content == null) return null;
        return roadmapService.parseContentToGraphFormat(content, userId);
    }
}