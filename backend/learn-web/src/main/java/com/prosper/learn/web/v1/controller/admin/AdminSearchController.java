package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<String> syncAll() {
        log.info("Admin triggered full sync");
        meilisearchService.syncAll();
        return ApiResponse.success("全量同步已开始，请查看日志");
    }

    @PostMapping("/sync-courses")
    public ApiResponse<Integer> syncCourses() {
        log.info("Admin triggered course sync");
        int count = meilisearchService.syncAllCourses();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-nodes")
    public ApiResponse<Integer> syncNodes() {
        log.info("Admin triggered node sync");
        int count = meilisearchService.syncAllNodes();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-users")
    public ApiResponse<Integer> syncUsers() {
        log.info("Admin triggered user sync");
        int count = meilisearchService.syncAllUsers();
        return ApiResponse.success(count);
    }

    @PostMapping("/sync-professions")
    public ApiResponse<Integer> syncProfessions() {
        log.info("Admin triggered profession sync");
        int count = meilisearchService.syncAllProfessions();
        return ApiResponse.success(count);
    }
}
