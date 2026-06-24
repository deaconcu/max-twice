package com.twicemax.web.v2.controller;

import com.meilisearch.sdk.model.Searchable;
import com.twicemax.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 搜索 API
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final MeilisearchService meilisearchService;

    @GetMapping("/courses")
    public List<Map<String, Object>> searchCourses(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return extractHits(meilisearchService.searchCourses(q, limit, offset));
    }

    @GetMapping("/nodes")
    public List<Map<String, Object>> searchNodes(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return extractHits(meilisearchService.searchNodes(q, limit, offset));
    }

    @GetMapping("/users")
    public List<Map<String, Object>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return extractHits(meilisearchService.searchUsers(q, limit, offset));
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> searchRoles(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return extractHits(meilisearchService.searchRoles(q, limit, offset));
    }

    @GetMapping("/all")
    public Map<String, List<Map<String, Object>>> searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, List<Map<String, Object>>> results = new HashMap<>();
        results.put("courses", extractHits(meilisearchService.searchCourses(q, limit, 0)));
        results.put("nodes", extractHits(meilisearchService.searchNodes(q, limit, 0)));
        results.put("users", extractHits(meilisearchService.searchUsers(q, limit, 0)));
        results.put("roles", extractHits(meilisearchService.searchRoles(q, limit, 0)));
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractHits(Searchable result) {
        if (result == null) {
            return Collections.emptyList();
        }
        try {
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
