package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.analytics.ranking.scheduler.ProfessionRankingScheduler;
import com.prosper.learn.application.dto.request.CreateProfessionRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.profession.ProfessionDTO;
import com.prosper.learn.application.service.ProfessionService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 职业管理接口
 * 从ProfessionClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProfessionsController {

    private final ProfessionService professionService;
    private final ProfessionRankingScheduler professionRankingScheduler;

    /**
     * 获取职业列表
     * 支持按分类筛选和游标分页
     * 不传分类参数时返回所有已发布职业
     */
    @GetMapping("/professions")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<ProfessionDTO>> getProfessions(
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory) {

        int pageSize = 20;
        int limit = pageSize + 1;
        List<ProfessionDTO> professionList;

        if (mainCategory != null && subCategory != null) {
            // 按主分类+子分类获取
            professionList = professionService.getListByCategoryAndLastId(mainCategory, subCategory, lastId, limit);
        } else if (mainCategory != null) {
            // 按主分类获取
            professionList = professionService.getListByMainCategoryAndLastId(mainCategory, lastId, limit);
        } else {
            // 获取所有已发布职业
            professionList = professionService.getApprovedByLastId(lastId, limit);
        }

        // 判断是否有更多数据
        boolean hasMore = professionList.size() > pageSize;

        // 如果有更多数据，只返回 pageSize 条
        if (hasMore) {
            professionList = professionList.subList(0, pageSize);
        }

        // 提取下一页的游标
        Long nextLastId = null;
        if (hasMore && !professionList.isEmpty()) {
            nextLastId = professionList.get(professionList.size() - 1).getId();
        }

        KeysetPageResponse<ProfessionDTO> response = KeysetPageResponse.of(professionList, hasMore, null, nextLastId);
        return ApiResponse.query(response);
    }

    /**
     * 获取职业详情
     * 映射: GET /profession?id=123 → GET /api/v1/professions/{id}
     */
    @GetMapping("/professions/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ProfessionDTO> getProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        ProfessionDTO profession = professionService.getById(id, true, currentUser.getId());
        return ApiResponse.query(profession);
    }

    /**
     * 搜索职业
     */
    @GetMapping("/professions/search")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> searchProfessions(
            @RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword) {
        List<ProfessionDTO> professionList = professionService.searchByKeyword(keyword);
        return ApiResponse.query(professionList);
    }

    /**
     * 创建职业
     * 映射: POST /profession → POST /api/v1/professions
     */
    @PostMapping("/professions")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> createProfession(
            @Valid @RequestBody CreateProfessionRequest request,
            @CurrentUser UserDO currentUser) {
        professionService.create(request, currentUser);
        return ApiResponse.success();
    }
    /**
     * 热门职业
     * 映射: GET /profession/hot → GET /api/v1/professions/hot?limit=10
     */
    @GetMapping("/professions/hot")
    public ApiResponse<Object> getHotProfessions(
            @RequestParam(value = "limit", defaultValue = "10")
            @Positive(message = "限制数量必须大于0")
            Integer limit) {
        log.info("开始获取热门职业，limit: {}", limit);
        List<ProfessionDTO> hotProfessions = professionService.getHotProfessions(limit);
        log.info("成功获取热门职业数量: {}", hotProfessions.size());
        return ApiResponse.query(hotProfessions);
    }
}