package com.prosper.learn.web.v1.controller;

import com.meilisearch.sdk.model.Searchable;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 搜索 API
 */
@Slf4j
@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final MeilisearchService meilisearchService;

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> searchCourses(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Searchable result = meilisearchService.searchCourses(q, limit, offset);
        return ApiResponse.success(extractHits(result));
    }

    @GetMapping("/nodes")
    public ApiResponse<List<Map<String, Object>>> searchNodes(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Searchable result = meilisearchService.searchNodes(q, limit, offset);
        return ApiResponse.success(extractHits(result));
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Searchable result = meilisearchService.searchUsers(q, limit, offset);
        return ApiResponse.success(extractHits(result));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> searchRoles(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Searchable result = meilisearchService.searchRoles(q, limit, offset);
        return ApiResponse.success(extractHits(result));
    }

    @GetMapping("/all")
    public ApiResponse<Map<String, List<Map<String, Object>>>> searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, List<Map<String, Object>>> results = new HashMap<>();

        Searchable coursesResult = meilisearchService.searchCourses(q, limit, 0);
        results.put("courses", extractHits(coursesResult));

        Searchable nodesResult = meilisearchService.searchNodes(q, limit, 0);
        results.put("nodes", extractHits(nodesResult));

        Searchable usersResult = meilisearchService.searchUsers(q, limit, 0);
        results.put("users", extractHits(usersResult));

        Searchable rolesResult = meilisearchService.searchRoles(q, limit, 0);
        results.put("roles", extractHits(rolesResult));

        return ApiResponse.success(results);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractHits(Searchable result) {
        if (result == null) {
            return Collections.emptyList();
        }

        try {
            // Searchable 包装了原始结果，需要从 hits 中提取
            ArrayList<HashMap<String, Object>> hits = result.getHits();
            if (hits == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (HashMap<String, Object> hit : hits) {
                results.add(hit);
            }
            return results;
        } catch (Exception e) {
            log.error("搜索服务 提取搜索结果失败", e);
            return Collections.emptyList();
        }
    }
}
