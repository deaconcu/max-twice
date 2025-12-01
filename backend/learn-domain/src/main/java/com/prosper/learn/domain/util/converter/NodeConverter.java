package com.prosper.learn.domain.util.converter;

import com.prosper.learn.common.Enums;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.node.*;
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

    // ========== 新版语义化方法 ==========

    /**
     * 转换为摘要DTO（基础信息）
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    NodeSummaryDTO toSummaryDTOInternal(NodeDO nodeDO);

    default NodeSummaryDTO toSummaryDTO(NodeDO nodeDO) {
        if (nodeDO == null) return null;

        NodeSummaryDTO dto = toSummaryDTOInternal(nodeDO);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    default List<NodeSummaryDTO> toSummaryDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null) return null;
        return nodeDOList.stream().map(this::toSummaryDTO).toList();
    }

    /**
     * 转换为详情DTO（包含管理信息）
     */
    @Named("toDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "courseId")
    @Mapping(target = "creatorId")
    @Mapping(target = "state")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    NodeDetailDTO toDetailDTOInternal(NodeDO nodeDO);

    default NodeDetailDTO toDetailDTO(NodeDO nodeDO) {
        if (nodeDO == null) return null;

        NodeDetailDTO dto = toDetailDTOInternal(nodeDO);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }

    @IterableMapping(qualifiedByName = "toDetailDTO")
    default List<NodeDetailDTO> toDetailDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null) return null;
        return nodeDOList.stream().map(this::toDetailDTO).toList();
    }

    /**
     * 转换为带课程的DTO（不含 course 对象，由 Service 层填充）
     */
    @Named("toWithCourseDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "courseId")
    @Mapping(target = "creatorId")
    @Mapping(target = "state")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    NodeWithCourseDTO toWithCourseDTOInternal(NodeDO nodeDO);

    default NodeWithCourseDTO toWithCourseDTO(NodeDO nodeDO) {
        if (nodeDO == null) return null;

        NodeWithCourseDTO dto = toWithCourseDTOInternal(nodeDO);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }

    @IterableMapping(qualifiedByName = "toWithCourseDTO")
    default List<NodeWithCourseDTO> toWithCourseDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null) return null;
        return nodeDOList.stream().map(this::toWithCourseDTO).toList();
    }

    /**
     * 转换为带进度的DTO
     */
    @Named("toWithProgressDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    NodeWithProgressDTO toWithProgressDTOInternal(NodeDO nodeDO);

    default NodeWithProgressDTO toWithProgressDTO(NodeDO nodeDO, boolean isCompleted) {
        if (nodeDO == null) return null;

        NodeWithProgressDTO dto = toWithProgressDTOInternal(nodeDO);
        dto.setIsCompleted(isCompleted);

        if (nodeDO.getState() != null && nodeDO.getState() == Enums.ContentState.BANNED.value()) {
            dto.setName("目录节点已被屏蔽");
            dto.setDescription("");
        }

        return dto;
    }
}