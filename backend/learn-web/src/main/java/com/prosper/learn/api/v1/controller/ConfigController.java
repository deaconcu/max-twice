package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.business.service.application.ValidationConfigService;
import com.prosper.learn.dto.response.ValidationRuleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 配置接口控制器
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ValidationConfigService validationConfigService;

    /**
     * 获取所有验证规则配置
     * GET /api/v1/config/validation
     *
     * 返回格式:
     * {
     *   "code": 200,
     *   "data": {
     *     "card-front": {
     *       "minLength": 5,
     *       "maxLength": 500,
     *       "label": "问题"
     *     },
     *     "card-back": {
     *       "minLength": 1,
     *       "maxLength": 500,
     *       "label": "答案"
     *     },
     *     ...
     *   }
     * }
     *
     * 支持 ETag 机制：
     * - 首次请求：返回 200 + 完整数据 + ETag
     * - 后续请求：如果 ETag 匹配，返回 304 Not Modified
     */
    @GetMapping("/validation")
    public ResponseEntity<ApiResponse<Map<String, ValidationRuleDTO>>> getValidationRules(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        Map<String, ValidationRuleDTO> rules = validationConfigService.getAllRules();

        // 计算 ETag（使用配置内容的哈希值）
        String rulesJson = rules.toString();
        String etag = "\"" + DigestUtils.md5DigestAsHex(rulesJson.getBytes(StandardCharsets.UTF_8)) + "\"";

        // 检查客户端的 ETag 是否匹配
        if (etag.equals(ifNoneMatch)) {
            // ETag 匹配 → 配置没变化 → 返回 304
            return ResponseEntity
                    .status(304)  // 304 Not Modified
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }

        // ETag 不匹配 → 配置变化了 → 返回 200 + 完整数据
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl
                        .maxAge(3, TimeUnit.MINUTES)  // 浏览器缓存 3 分钟
                        .mustRevalidate())             // 过期后必须验证
                .body(ApiResponse.success(rules));
    }
}
