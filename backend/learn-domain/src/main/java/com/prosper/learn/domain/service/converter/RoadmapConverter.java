package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.domain.service.data.UserDataService;
import com.prosper.learn.domain.service.data.ProfessionDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {UserDataService.class, ProfessionDataService.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoadmapConverter {
    
    @Named("toDTO")
    RoadmapDTOV1 toDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<RoadmapDTOV1> toDTO(List<RoadmapDO> roadmapDOList);

    @Named("toDTOV2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "profession", qualifiedByName = "getProfession")
    @Mapping(target = "description")
    @Mapping(target = "vote")
    @Mapping(target = "comment")
    @Mapping(target = "upvoted")
    @Mapping(target = "creator", qualifiedByName = "getCreator")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapDTOV1 toDTOV2(RoadmapDO roadmapDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    List<RoadmapDTOV1> toDTOV2(List<RoadmapDO> roadmapDOList);
    
    @Named("getCreator")
    default UserDTOV4 getCreator(Long creatorId, @Context UserDataService userDataService) {
        if (creatorId == null) return null;
        
        // 这里需要根据实际的UserConverter调用
        // return userConverter.toDTOV4(userDataService.getById(creatorId));
        return null; // 暂时返回null，需要在实际使用时调用UserConverter
    }
    
    @Named("getProfession")
    default ProfessionDTO getProfession(Long professionId, @Context ProfessionDataService professionDataService) {
        if (professionId == null) return null;
        
        // 这里需要根据实际的ProfessionConverter调用
        // return professionConverter.toDTO(professionDataService.getById(professionId));
        return null; // 暂时返回null，需要在实际使用时调用ProfessionConverter
    }
}