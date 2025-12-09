package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.dto.response.node.NodeDetailDTO;
import com.prosper.learn.application.dto.response.node.NodeWithCourseDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.node.NodeDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 节点应用服务
 *
 * 负责协调跨子域逻辑、DTO转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeDomainService domainService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final NodeConverter nodeConverter;
    private final CourseConverter courseConverter;

    // ========== Query 方法（读操作）==========

    public NodeWithCourseDTO getById(Long id, DTOVersion dtoVersion) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid node ID");
        }
        NodeDO nodeDO = nodeDataService.getById(id);
        switch (dtoVersion) {
            case V3 -> {
                return toWithCourseDTO(nodeDO);
            }
            default -> throw new IllegalArgumentException("Unsupported DTO version: " + dtoVersion);
        }
    }

    /**
     * 批量加载节点信息
     */
    public Map<Long, NodeWithCourseDTO> getNodeMap(List<Long> ids) {
        if (ids.isEmpty()) return new HashMap<>();

        List<NodeDO> nodeList = nodeDataService.getByIds(ids);
        return toWithCourseDTO(nodeList).stream().collect(
                Collectors.toMap(NodeWithCourseDTO::getId, node -> node));
    }

    /**
     * 管理后台：按条件筛选节点列表
     */
    public List<NodeDetailDTO> listByFilter(Byte state, Long nodeId, Long courseId, Long creatorId, Long lastId) {
        // 调用 DomainService 查询
        List<NodeDO> nodeDOList = domainService.listByFilter(nodeId, courseId, creatorId, state, lastId);

        // 管理后台使用 toDetailDTOInternal，返回原始数据，不做屏蔽处理
        return nodeDOList.stream()
                .map(nodeConverter::toDetailDTOInternal)
                .toList();
    }

    // ========== DTO 转换方法 ==========

    /**
     * v3 = 包含课程对象的节点
     */
    public NodeWithCourseDTO toWithCourseDTO(NodeDO nodeDO) {
        if (nodeDO == null)  return null;

        NodeWithCourseDTO dto = nodeConverter.toWithCourseDTO(nodeDO);
        if (dto != null && nodeDO.getCourseId() != null) {
            CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
            dto.setCourse(courseConverter.toSummaryDTO(courseDO));
        }
        return dto;
    }

    public List<NodeWithCourseDTO> toWithCourseDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null || nodeDOList.isEmpty()) return List.of();

        List<NodeWithCourseDTO> dtoList = nodeConverter.toWithCourseDTO(nodeDOList);

        // 批量加载课程信息（基于节点）
        List<Long> courseIds = Utils.getIds(nodeDOList, dto -> ((NodeDO) dto).getCourseId());
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(courseIds);

        for (NodeWithCourseDTO dto : dtoList) {
            dto.setCourse(courseConverter.toSummaryDTO(courseMap.get(dto.getCourseId())));
        }
        return dtoList;
    }

    // ========== Command 方法（写操作）==========

    /**
     * 修改节点状态
     */
    @Transactional
    public NodeDetailDTO updateNodeState(Long nodeId, ContentState state, String reason) {
        // 调用 DomainService 执行状态变更
        domainService.updateNodeState(nodeId, state, reason);

        // 查询并返回 DTO
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        return nodeConverter.toDetailDTO(nodeDO);
    }

    /**
     * 审批通过节点
     */
    @Transactional
    public void approve(Long nodeId) {
        domainService.approve(nodeId);
    }

    /**
     * 拒绝节点（审核不通过）
     */
    @Transactional
    public void reject(Long nodeId, String reason) {
        domainService.reject(nodeId, reason);
    }

    /**
     * 封禁节点（违规封禁）
     */
    @Transactional
    public void ban(Long nodeId, String reason) {
        domainService.ban(nodeId, reason);
    }
}
