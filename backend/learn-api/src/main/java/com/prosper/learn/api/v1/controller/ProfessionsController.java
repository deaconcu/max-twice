package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;

import static com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.ProfessionService;
import com.prosper.learn.domain.service.scheduler.ProfessionRankingScheduler;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
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
@RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class ProfessionsController {

    private final ProfessionService professionService;
    private final ProfessionRankingScheduler professionRankingScheduler;

    /**
     * 分页获取职业
     * 映射: GET /profession/list?page=1 → GET /api/v1/professions?page=0&size=20
     */
    @GetMapping("/professions")
    public ApiResponse<Object> getProfessionsByPage(
            @RequestParam(required = false) @Min(value = 0, message = "页码不能小于0") Integer page,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory) {

        if (page != null) {
            // 分页获取职业
            List<ProfessionDTO> professionList = professionService.getListByPage(page);
            return ApiResponse.success(professionList);
        } else if (mainCategory != null && subCategory != null) {
            // 按分类获取（允许 lastId 为 null）
            List<ProfessionDTO> professionList = professionService.getListByCategoryAndLastId(mainCategory, subCategory, lastId);
            return ApiResponse.success(professionList);
        } else if (mainCategory != null) {
            // 按主分类获取（允许 lastId 为 null）
            List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndLastId(mainCategory, lastId);
            return ApiResponse.success(professionList);
        } else {
            throw new IllegalArgumentException("缺少必要参数");
        }
    }

    /**
     * 获取已批准职业
     * 映射: GET /profession/list/approved → GET /api/v1/professions/approved?lastId=123
     */
    @GetMapping("/professions/approved")
    public ApiResponse<Object> getApprovedProfessions(
            @RequestParam(required = false)
            @Min(value = 0, message = "最后ID不能小于0")
            Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(ContentState.PUBLISHED, lastId);
        return ApiResponse.success(professionList);
    }

    /**
     * 获取职业详情
     * 映射: GET /profession?id=123 → GET /api/v1/professions/{id}
     */
    @GetMapping("/professions/{id}")
    public ApiResponse<ProfessionDTO> getProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id) {
        ProfessionDTO profession = professionService.getById(id, true);
        return ApiResponse.success(profession);
    }

    /**
     * 创建职业
     * 映射: POST /profession → POST /api/v1/professions
     */
    @PostMapping("/professions")
    @SaCheckLogin
    public ApiResponse<Object> createProfession(
            @Valid @RequestBody CreateProfessionRequest request,
            @CurrentUser UserDO currentUser) {
        professionService.create(request, currentUser);
        return ApiResponse.success();
    }


    /**
     * 删除职业
     * 映射: DELETE /profession → DELETE /api/v1/professions/{id}
     */
    @DeleteMapping("/professions/{id}")
    @SaCheckLogin
    public ApiResponse<Object> deleteProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        professionService.delete(id, currentUser);
        return ApiResponse.success("删除成功");
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
        return ApiResponse.success(hotProfessions);
    }
}