package com.twicemax.web.v2.controller.admin;

import com.twicemax.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.twicemax.web.v2.annotation.OperationLog;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 管理员 - 搜索管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/search")
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
    public ResponseEntity<Void> syncAll() {
        log.info("管理后台 触发全量同步");
        meilisearchService.syncAll();
        return ResponseEntity.noContent().build();
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
    public int syncCourses() {
        log.info("管理后台 触发课程同步");
        return meilisearchService.syncAllCourses();
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
    public int syncNodes() {
        log.info("管理后台 触发节点同步");
        return meilisearchService.syncAllNodes();
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
    public int syncUsers() {
        log.info("管理后台 触发用户同步");
        return meilisearchService.syncAllUsers();
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
    public int syncRoles() {
        log.info("管理后台 触发角色同步");
        return meilisearchService.syncAllRoles();
    }
}
