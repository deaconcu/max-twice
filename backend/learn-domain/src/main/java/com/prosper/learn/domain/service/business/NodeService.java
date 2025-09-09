package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.data.NodeDataService;
import com.prosper.learn.domain.util.Util;
import com.prosper.learn.domain.util.converter.NodeConverter;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeDataService nodeDataService;
    private final NodeConverter nodeConverter;
    private final CourseService courseService;

    public NodeDTO getById(Long id, Enums.DTOVersion dtoVersion) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid node ID");
        }
        NodeDO nodeDO = nodeDataService.getById(id);
        switch (dtoVersion) {
            case V3 -> {
                return toDTOV3(nodeDO);
            }
            default -> throw new IllegalArgumentException("Unsupported DTO version: " + dtoVersion);
        }
    }

    /**
     * 批量加载节点信息
     */
    public Map<Long, NodeDTO> getNodeMap(List<Long> ids) {
        if (ids.isEmpty()) return new HashMap<>();

        List<NodeDO> nodeList = nodeDataService.getByIds(ids);
        return toDTOV3(nodeList).stream().collect(
                Collectors.toMap(NodeDTO::getId, node -> node));
    }

    // ========== DTO转换方法 ==========

    /**
     * v3 = v0 + course
     */
    public NodeDTO toDTOV3(NodeDO nodeDO) {
        if (nodeDO == null)  return null;

        NodeDTO nodeDTO = nodeConverter.toDTO(nodeDO);
        if (nodeDTO != null && nodeDO.getCourseId() != null) {
            nodeDTO.setCourse(courseService.getCourseById(nodeDO.getCourseId()));
        }
        return nodeDTO;
    }

    public List<NodeDTO> toDTOV3(List<NodeDO> nodeDOList) {
        if (nodeDOList == null || nodeDOList.isEmpty()) return List.of();

        List<NodeDTO> dtoList = nodeConverter.toDTO(nodeDOList);
        // 批量加载课程信息（基于节点）
        List<Long> courseIds = Util.getIds(nodeDOList, dto -> ((NodeDO) dto).getCourseId());
        Map<Long, CourseDO> courseMap = courseService.getCourseMap(courseIds);

        for (NodeDTO nodeDTO : dtoList) {
            nodeDTO.setCourse(courseService.toDTOV3(courseMap.get(nodeDTO.getCourseId())));
        }
        return nodeDOList.stream().map(this::toDTOV3).collect(Collectors.toList());
    }
}
