package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.NodeService;
import com.prosper.learn.dto.response.NodeDTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 节点管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminNodesController {

    private final NodeService nodeService;

    /**
     * 管理后台：按条件获取节点列表
     * 映射: GET /api/v1/admin/nodes?state=0&nodeId=1&courseId=2&creatorId=3&lastId=123
     */
    @GetMapping("/nodes")
    public ApiResponse<List<NodeDTO>> getAdminNodes(
            @RequestParam(value = "state", required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "courseId", required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", required = false) Long lastId) {
        List<NodeDTO> nodes = nodeService.listByFilter(state, nodeId, courseId, creatorId, lastId);
        return ApiResponse.success(nodes);
    }

    /**
     * 修改节点状态
     */
    @PutMapping("/nodes/{nodeId}/state")
    @OperationLog(
        module = "内容管理",
        type = "#state == 2 ? '审核通过节点' : (#state == 3 ? '审核拒绝节点' : '修改节点状态')",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "#nodeId",
        reason = "#reason"
    )
    public ApiResponse<NodeDTO> updateNodeState(
            @PathVariable @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam @Min(value = 0, message = "状态值必须大于等于0") Integer state,
            @RequestParam(required = false, defaultValue = "") String reason) {
        Enums.ContentState contentState = Enums.ContentState.getByValue(state);
        if (contentState == null) {
            throw new IllegalArgumentException("Invalid state value: " + state);
        }
        NodeDTO node = nodeService.updateNodeState(nodeId, contentState, reason);
        return ApiResponse.success(node);
    }
}
