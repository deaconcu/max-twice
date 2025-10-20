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
import org.springframework.transaction.annotation.Transactional;

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

    // ========== 查询方法 ==========

    /**
     * 根据节点、课程、创建者筛选节点列表（不限状态）
     * 如果提供了 nodeId，其他参数将被忽略
     */
    public List<NodeDTO> getNodesByFilter(Long nodeId, Long courseId, Long creatorId, Long lastId) {
        if (nodeId != null) {
            courseId = null;
            creatorId = null;
            lastId = null;
        }

        List<NodeDO> nodeDOList = nodeDataService.getListByFilter(nodeId, courseId, creatorId, lastId);
        return nodeConverter.toDTO(nodeDOList);
    }

    /**
     * 修改节点状态
     */
    public NodeDTO updateNodeState(Long nodeId, Enums.ContentState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        nodeDataService.validateExists(nodeId);
        nodeDataService.updateState(nodeId, state);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        return nodeConverter.toDTO(nodeDO);
    }

    /**
     * 审批通过节点
     */
    @Transactional
    public void approve(Long nodeId) {
        nodeDataService.validateExists(nodeId);
        nodeDataService.approve(nodeId);
    }

    /**
     * 拒绝节点（审核不通过）
     */
    @Transactional
    public void reject(Long nodeId) {
        nodeDataService.validateExists(nodeId);
        nodeDataService.reject(nodeId);
    }

    /**
     * 封禁节点（违规封禁）
     */
    @Transactional
    public void ban(Long nodeId) {
        nodeDataService.validateExists(nodeId);
        nodeDataService.ban(nodeId);
    }
}
