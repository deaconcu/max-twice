package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.prosper.learn.web.v1.annotation.OperationLog;
import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 管理员 - 搜索管理
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final MeilisearchService meilisearchService;

    @PostMapping("/sync-all")
    @OperationLog(
        module = "系统维护",
        type = "全量同步搜索",
        level = OperationLevel.HIGH,
        targetType = "System",
        targetId = "0"
    )
    public ApiResponse<String> syncAll() {
        log.info("Admin triggered full sync");
        meilisearchService.syncAll();
        return ApiResponse.success("全量同步已开始，请查看日志");
    }

    @PostMapping("/sync-courses")
    @OperationLog(
        module = "系统维护",
        type = "同步课程搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "0"
    )
    public ApiResponse<Integer> syncCourses() {
        log.info("Admin triggered course sync");
        int count = meilisearchService.syncAllCourses();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-nodes")
    @OperationLog(
        module = "系统维护",
        type = "同步节点搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Node",
        targetId = "0"
    )
    public ApiResponse<Integer> syncNodes() {
        log.info("Admin triggered node sync");
        int count = meilisearchService.syncAllNodes();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-users")
    @OperationLog(
        module = "系统维护",
        type = "同步用户搜索",
        level = OperationLevel.MEDIUM,
        targetType = "User",
        targetId = "0"
    )
    public ApiResponse<Integer> syncUsers() {
        log.info("Admin triggered user sync");
        int count = meilisearchService.syncAllUsers();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-professions")
    @OperationLog(
        module = "系统维护",
        type = "同步职业搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Profession",
        targetId = "0"
    )
    public ApiResponse<Integer> syncProfessions() {
        log.info("Admin triggered profession sync");
        int count = meilisearchService.syncAllProfessions();
        return ApiResponse.success(count);
    }
}
