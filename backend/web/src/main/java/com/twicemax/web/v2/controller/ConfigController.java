package com.twicemax.web.v2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.twicemax.application.dto.response.ValidationRuleDTO;
import com.twicemax.application.service.ValidationConfigService;
import com.twicemax.shared.infrastructure.config.SystemDomainService;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置接口控制器
 */
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class ConfigController {

    private final ValidationConfigService validationConfigService;
    private final SystemDomainService systemDomainService;

    /**
     * 获取所有验证规则配置。支持 ETag 缓存：
     * - 首次请求：200 + 完整数据 + ETag
     * - ETag 匹配：304 Not Modified
     */
    @GetMapping("/validation")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public ResponseEntity<Map<String, ValidationRuleDTO>> getValidationRules(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        Map<String, ValidationRuleDTO> rules = validationConfigService.getAllRules();

        String rulesJson = rules.toString();
        String etag = "\"" + DigestUtils.md5DigestAsHex(rulesJson.getBytes(StandardCharsets.UTF_8)) + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity
                    .status(304)
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(rules);
    }

    /**
     * 获取课程分类数据
     */
    @GetMapping("/course-categories")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public JsonNode getCourseCategories() {
        return systemDomainService.getCourseCategories();
    }

    /**
     * 获取角色分类数据
     */
    @GetMapping("/role-categories")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public JsonNode getRoleCategories() {
        return systemDomainService.getRoleCategories();
    }
}

