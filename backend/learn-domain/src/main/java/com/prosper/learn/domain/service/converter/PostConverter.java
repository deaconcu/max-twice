package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.PostDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.PostDTOV1;
import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.dto.response.old.UserDTOV1;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.domain.service.data.NodeDataService;
import com.prosper.learn.domain.service.data.UserDataService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PostConverter {

    @Autowired
    protected NodeDataService nodeDataService;
    @Autowired
    protected UserDataService userDataService;
    @Autowired
    protected NodeConverter nodeConverter;
    @Autowired
    protected UserConverter userConverter;
    
    @Named("toDTO")
    @Mapping(target = "views", ignore = true)
    public abstract PostDTO toDTO(PostDO postDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<PostDTO> toDTO(List<PostDO> postDOList);
    
    @Named("toDTOV2")
    public abstract PostDTO toDTOV2(PostDO postDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    public abstract List<PostDTO> toDTOV2(List<PostDO> postDOList);
    
    @Named("getNode")
    public NodeDTO getNode(Long nodeId) {
        if (nodeId == null) return null;
        
        return nodeConverter.toDTO(nodeDataService.getById(nodeId));
    }
    
    @Named("getCreator")
    public UserDTO getCreator(Long creatorId) {
        if (creatorId == null) return null;
        
        return userConverter.toDTOV4(userDataService.getById(creatorId));
    }
}