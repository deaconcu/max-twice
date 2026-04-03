package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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

    @PostMapping("/sync-professions")
    @Transactional
    @OperationLog(
        module = "系统维护",
        type = "同步职业搜索",
        level = OperationLevel.MEDIUM,
        targetType = "Profession",
        targetId = "0"
    )
    public ApiResponse<Integer> syncProfessions() {
        log.info("管理后台 触发职业同步");
        int count = meilisearchService.syncAllProfessions();
        return ApiResponse.success(count);
    }
}
