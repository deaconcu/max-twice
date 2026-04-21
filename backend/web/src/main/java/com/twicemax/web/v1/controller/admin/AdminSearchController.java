package com.twicemax.web.v1.controller.admin;

import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.twicemax.web.v1.annotation.OperationLog;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 管理员 - 搜索管理
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final MeilisearchService meilisearchService;

    @PostMapping("/sync-all")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "全量同步搜索",
        level = OperationLevel.HIGH,
        targetType = "System",
        targetId = "0"
    )
    public ApiResponse<String> syncAll() {
        log.info("管理后台 触发全量同步");
        meilisearchService.syncAll();
        return ApiResponse.success("全量同步已开始，请查看日志");
    }

    @PostMapping("/sync-courses")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "同步课程搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "0"
    )
    public ApiResponse<Integer> syncCourses() {
        log.info("管理后台 触发课程同步");
        int count = meilisearchService.syncAllCourses();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-nodes")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "同步节点搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "0"
    )
    public ApiResponse<Integer> syncNodes() {
        log.info("管理后台 触发节点同步");
        int count = meilisearchService.syncAllNodes();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-users")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "同步用户搜索",
        level = OperationLevel.MEDIUM,
        targetType = "User",
        targetId = "0"
    )
    public ApiResponse<Integer> syncUsers() {
        log.info("管理后台 触发用户同步");
        int count = meilisearchService.syncAllUsers();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-roles")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "同步角色搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Role",
        targetId = "0"
    )
    public ApiResponse<Integer> syncRoles() {
        log.info("管理后台 触发角色同步");
        int count = meilisearchService.syncAllRoles();
        return ApiResponse.success(count);
    }
}
