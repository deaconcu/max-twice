package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.old.PostDTOV1;
import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.dto.response.old.UserDTOV1;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.domain.service.data.NodeDataService;
import com.prosper.learn.domain.service.data.UserDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {NodeDataService.class, UserDataService.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostConverter {
    
    @Named("toDTO")
    @Mapping(target = "node", ignore = true)
    @Mapping(target = "creator", ignore = true)
    PostDTOV1 toDTO(PostDO postDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<PostDTOV1> toDTO(List<PostDO> postDOList);
    
    PostDO toPostDO(PostDTOV1 postDTO);
    
    @Named("toDTOV2")
    @Mapping(target = "node", ignore = true)
    @Mapping(target = "creator", ignore = true)
    PostDTOV1 toDTOV2(PostDO postDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    List<PostDTOV1> toDTOV2(List<PostDO> postDOList);
    
    @Named("getNode")
    default NodeDTOV0 getNode(Long nodeId, @Context NodeDataService nodeDataService) {
        if (nodeId == null) return null;
        
        // 这里需要根据实际的NodeConverter调用
        // return nodeConverter.toDTO(nodeDataService.getById(nodeId));
        return null; // 暂时返回null，需要在实际使用时调用NodeConverter
    }
    
    @Named("getCreator")
    default UserDTOV1 getCreator(Long creatorId, @Context UserDataService userDataService) {
        if (creatorId == null) return null;
        
        // 这里需要根据实际的UserConverter调用
        // return userConverter.toDTOV1(userDataService.getById(creatorId));
        return null; // 暂时返回null，需要在实际使用时调用UserConverter
    }
}