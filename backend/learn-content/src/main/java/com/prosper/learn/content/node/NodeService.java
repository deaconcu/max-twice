package com.prosper.learn.content.node;

import com.prosper.learn.common.Enums;
import com.prosper.learn.business.service.domain.MessageDomainService;
import com.prosper.learn.business.service.data.CourseDataService;
import com.prosper.learn.business.service.data.NodeDataService;
import com.prosper.learn.business.util.converter.NodeConverter;
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
    private final CourseDataService courseDataService;
    private final MessageDomainService messageDomainService;
    private final NodeConverter nodeConverter;
    private final CourseService courseService;

    public NodeWithCourseDTO getById(Long id, Enums.DTOVersion dtoVersion) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid node ID");
        }
        NodeDO nodeDO = nodeDataService.getById(id);
        switch (dtoVersion) {
            case DTOVersion.V3 -> {
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

    // ========== DTO转换方法 ==========

    /**
     * v3 = 包含课程对象的节点
     */
    public NodeWithCourseDTO toWithCourseDTO(NodeDO nodeDO) {
        if (nodeDO == null)  return null;

        NodeWithCourseDTO dto = nodeConverter.toWithCourseDTO(nodeDO);
        if (dto != null && nodeDO.getCourseId() != null) {
            CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
            dto.setCourse(courseService.toSummaryDTO(courseDO));
        }
        return dto;
    }

    public List<NodeWithCourseDTO> toWithCourseDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null || nodeDOList.isEmpty()) return List.of();

        List<NodeWithCourseDTO> dtoList = nodeConverter.toWithCourseDTO(nodeDOList);
        // 批量加载课程信息（基于节点）
        List<Long> courseIds = Util.getIds(nodeDOList, dto -> ((NodeDO) dto).getCourseId());
        Map<Long, CourseDO> courseMap = courseService.getCourseMap(courseIds);

        for (NodeWithCourseDTO dto : dtoList) {
            dto.setCourse(courseService.toSummaryDTO(courseMap.get(dto.getCourseId())));
        }
        return dtoList;
    }

    // ========== 查询方法 ==========

    /**
     * 管理后台：按条件筛选节点列表
     * 如果提供了 nodeId，其他参数将被忽略
     */
    public List<NodeDetailDTO> listByFilter(Byte state, Long nodeId, Long courseId, Long creatorId, Long lastId) {
        if (nodeId != null) {
            state = null;
            courseId = null;
            creatorId = null;
            lastId = null;
        }

        List<NodeDO> nodeDOList = nodeDataService.getListByFilter(nodeId, courseId, creatorId, state, lastId);
        // 管理后台使用 toDetailDTOInternal，返回原始数据，不做屏蔽处理
        return nodeDOList.stream()
                .map(nodeConverter::toDetailDTOInternal)
                .toList();
    }

    /**
     * 修改节点状态
     */
    @Transactional
    public NodeDetailDTO updateNodeState(Long nodeId, Enums.ContentState state, String reason) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        nodeDataService.validateExists(nodeId);

        // 根据状态调用相应的方法，以便发送通知
        switch (state) {
            case ContentState.REJECTED -> reject(nodeId, reason);
            case ContentState.BANNED -> ban(nodeId, reason);
            case ContentState.PUBLISHED -> approve(nodeId);
            default -> throw new IllegalArgumentException("Unsupported state: " + state);
        }

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        return nodeConverter.toDetailDTO(nodeDO);
    }

    /**
     * 审批通过节点
     */
    @Transactional
    public void approve(Long nodeId) {
        nodeDataService.validateExists(nodeId);

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Util.validateStateTransition(nodeDO.getState(), Enums.ContentState.PUBLISHED);

        nodeDataService.approve(nodeId);
    }

    /**
     * 拒绝节点（审核不通过）
     */
    @Transactional
    public void reject(Long nodeId, String reason) {
        nodeDataService.validateExists(nodeId);

        // 获取节点和课程信息
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Util.validateStateTransition(nodeDO.getState(), Enums.ContentState.REJECTED);

        CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());

        nodeDataService.reject(nodeId, reason);

        // 发送拒绝通知
        if (courseDO != null) {
            messageDomainService.sendNodeModeration(
                nodeDO.getCreatorId(),
                nodeDO.getId(),
                nodeDO.getName(),
                courseDO.getId(),
                courseDO.getName(),
                Enums.ModerationAction.REJECTED,
                reason
            );
        }
    }

    /**
     * 封禁节点（违规封禁）
     */
    @Transactional
    public void ban(Long nodeId, String reason) {
        nodeDataService.validateExists(nodeId);

        // 获取节点和课程信息
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        Util.validateStateTransition(nodeDO.getState(), Enums.ContentState.BANNED);

        CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());

        nodeDataService.ban(nodeId, reason);

        // 发送封禁通知
        if (courseDO != null) {
            messageDomainService.sendNodeModeration(
                nodeDO.getCreatorId(),
                nodeDO.getId(),
                nodeDO.getName(),
                courseDO.getId(),
                courseDO.getName(),
                Enums.ModerationAction.BANNED,
                reason
            );
        }
    }
}
