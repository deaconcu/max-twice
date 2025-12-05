package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.response.node.NodeDetailDTO;
import com.prosper.learn.application.service.NodeService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.web.v1.annotation.OperationLog;
import com.prosper.learn.web.v1.annotation.RequireRole;
import com.prosper.learn.web.v1.dto.ApiResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 节点管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminNodesController {

    private final NodeService nodeService;

    /**
     * 管理后台：按条件获取节点列表
     * 映射: GET /api/v1/admin/nodes?state=0&nodeId=1&courseId=2&creatorId=3&lastId=123
     */
    @GetMapping("/nodes")
    public ApiResponse<List<NodeDetailDTO>> getAdminNodes(
            @RequestParam(value = "state", required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "courseId", required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) Long lastId) {
        List<NodeDetailDTO> nodes = nodeService.listByFilter(state, nodeId, courseId, creatorId, lastId);
        return ApiResponse.success(nodes);
    }

    /**
     * 修改节点状态
     */
    @PutMapping("/nodes/{nodeId}/state")
    @OperationLog(
        module = "内容管理",
        type = "#state == 2 ? '审核通过节点' : (#state == 3 ? '审核拒绝节点' : '修改节点状态')",
        level = OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "#nodeId",
        reason = "#reason"
    )
    public ApiResponse<NodeDetailDTO> updateNodeState(
            @PathVariable @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam @Min(value = 0, message = "状态值必须大于等于0") Integer state,
            @RequestParam(required = false, defaultValue = "") String reason) {
        ContentState contentState = ContentState.getByValue(state);
        if (contentState == null) {
            throw new IllegalArgumentException("Invalid state value: " + state);
        }
        NodeDetailDTO node = nodeService.updateNodeState(nodeId, contentState, reason);
        return ApiResponse.success(node);
    }
}
