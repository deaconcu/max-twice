package com.prosper.learn.api.v1.controller;

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
 * 节点管理接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
public class NodesController {

    private final NodeService nodeService;

    /**
     * 根据节点、课程、创建者筛选节点列表
     */
    @GetMapping("/admin/nodes/filter")
    public ApiResponse<List<NodeDTO>> getNodesByFilter(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "courseId", required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        List<NodeDTO> nodes = nodeService.getNodesByFilter(nodeId, courseId, creatorId, lastId);
        return ApiResponse.success(nodes);
    }

    /**
     * 修改节点状态
     */
    @PutMapping("/admin/nodes/{nodeId}/state")
    public ApiResponse<NodeDTO> updateNodeState(
            @PathVariable @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam @Min(value = 0, message = "状态值必须大于等于0") Integer state) {
        Enums.ContentState contentState = Enums.ContentState.getByValue(state);
        if (state == null) {
            throw new IllegalArgumentException("Invalid state value: " + state);
        }
        NodeDTO node = nodeService.updateNodeState(nodeId, contentState);
        return ApiResponse.success(node);
    }
}
