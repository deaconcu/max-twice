package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.persistence.dataobject.NodeDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "courseId")
    @Mapping(target = "creatorId")
    @Mapping(target = "commentCount")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    NodeDTO toDTO(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<NodeDTO> toDTO(List<NodeDO> nodeDOList);
    
    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    NodeDTO toDTOV1(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    List<NodeDTO> toDTOV1(List<NodeDO> nodeDOList);

    default NodeDTO toDTOV2(NodeDO nodeDO, boolean isCompleted) {
        if (nodeDO == null) return null;
        
        NodeDTO dto = toDTOV1(nodeDO);
        dto.setIsCompleted(isCompleted);
        return dto;
    }
}