package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class NodeConverter {

    @Autowired
    protected CourseDataService courseDataService;
    
    @Named("toDTO")
    public abstract NodeDTO toDTO(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<NodeDTO> toDTO(List<NodeDO> nodeDOList);
    
    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    public abstract NodeDTO toDTOV1(NodeDO nodeDO);
    
    @IterableMapping(qualifiedByName = "toDTOV1")
    public abstract List<NodeDTO> toDTOV1(List<NodeDO> nodeDOList);

    public NodeDTO toDTOV2(NodeDO nodeDO, boolean isCompleted) {
        if (nodeDO == null) return null;
        
        NodeDTO dto = toDTOV1(nodeDO);
        dto.setIsCompleted(isCompleted);
        return dto;
    }
}