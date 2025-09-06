package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {CourseDataService.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeConverter {
    
    @Named("toDTO")
    NodeDTOV0 toDTO(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<NodeDTOV0> toDTO(List<NodeDO> nodeDOList);
    
    NodeDO toNodeDO(NodeDTOV0 nodeDTOV0);
    
    @Named("toDTOV1")
    @Mapping(target = "id")
    @Mapping(target = "name")
    NodeDTOV0 toDTOV1(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    List<NodeDTOV0> toDTOV1(List<NodeDO> nodeDOList);
    
    @Named("toDTOV2")
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "isCompleted", ignore = true)
    NodeDTOV0 toDTOV2(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTOV2")
    List<NodeDTOV0> toDTOV2(List<NodeDO> nodeDOList);
    
    default NodeDTOV0 toDTOV2WithCompleted(NodeDO nodeDO, boolean isCompleted) {
        if (nodeDO == null) return null;
        
        NodeDTOV0 dto = toDTOV2(nodeDO);
        dto.setIsCompleted(isCompleted);
        return dto;
    }
}