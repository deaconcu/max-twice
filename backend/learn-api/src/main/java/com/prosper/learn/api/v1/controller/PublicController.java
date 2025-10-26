package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.data.SystemDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开接口 - 无需登录
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final SystemDataService systemDataService;

    /**
     * 查询只读模式状态（公开接口，无需登录）
     * GET /api/v1/public/readonly-mode
     */
    @GetMapping("/readonly-mode")
    public ApiResponse<Map<String, Object>> getReadOnlyMode() {
        try {
            boolean enabled = systemDataService.isReadOnlyMode();

            Map<String, Object> result = new HashMap<>();
            result.put("enabled", enabled);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to get readonly mode status", e);
            return ApiResponse.error("查询失败");
        }
    }
}
