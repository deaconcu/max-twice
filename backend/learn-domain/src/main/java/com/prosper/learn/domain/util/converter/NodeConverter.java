package com.prosper.learn.domain.util.converter;

import com.prosper.learn.common.Enums;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.persistence.dataobject.NodeDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface NodeConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "courseId")
    @Mapping(target = "creatorId")
    @Mapping(target = "commentCount")
    @Mapping(target = "state")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    NodeDTO toDTOInternal(NodeDO nodeDO);

    default NodeDTO toDTO(NodeDO nodeDO) {
        if (nodeDO == null) return null;

        NodeDTO dto = toDTOInternal(nodeDO);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }

    @IterableMapping(qualifiedByName = "toDTO")
    default List<NodeDTO> toDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null) return null;
        return nodeDOList.stream().map(this::toDTO).toList();
    }
    
    @Named("toDTOV1")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    NodeDTO toDTOV1Internal(NodeDO nodeDO);

    default NodeDTO toDTOV1(NodeDO nodeDO) {
        if (nodeDO == null) return null;

        NodeDTO dto = toDTOV1Internal(nodeDO);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }

    @IterableMapping(qualifiedByName = "toDTOV1")
    default List<NodeDTO> toDTOV1(List<NodeDO> nodeDOList) {
        if (nodeDOList == null) return null;
        return nodeDOList.stream().map(this::toDTOV1).toList();
    }

    default NodeDTO toDTOV2(NodeDO nodeDO, boolean isCompleted) {
        if (nodeDO == null) return null;

        NodeDTO dto = toDTOV1(nodeDO);
        dto.setIsCompleted(isCompleted);

        return dto;
    }
}