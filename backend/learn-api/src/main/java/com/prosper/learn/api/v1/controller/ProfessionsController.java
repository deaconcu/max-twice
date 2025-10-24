package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;

import static com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.ProfessionService;
import com.prosper.learn.domain.service.scheduler.ProfessionRankingScheduler;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.dto.response.ProfessionDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 管理后台：按状态获取职业列表
     * 映射: GET /api/v1/admin/professions?state=0&lastId=123
     */
    @GetMapping("/admin/professions")
    public ApiResponse<Object> getAdminProfessions(
            @RequestParam @Positive(message = "状态必须大于0") Byte state,
            @RequestParam(required = false) Long lastId) {

        ContentState professionState = ContentState.getByValue(state.intValue());
        if (professionState == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("Invalid profession state: " + state);
        }

        List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(professionState, lastId);
        return ApiResponse.success(professionList);
    }

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
        } else if (mainCategory != null && subCategory != null && lastId != null) {
            // 按分类获取
            List<ProfessionDTO> professionList = professionService.getListByCategoryAndLastId(mainCategory, subCategory, lastId);
            return ApiResponse.success(professionList);
        } else if (mainCategory != null && lastId != null) {
            // 按主分类获取
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
    public ApiResponse<Object> createProfession(@Valid @RequestBody CreateProfessionRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        professionService.create(userId, request);
        return ApiResponse.success();
    }

    /**
     * 更新职业
     * 映射: PUT /profession → PUT /api/v1/professions/{id}
     */
    @PutMapping("/professions/{id}")
    public ApiResponse<Object> updateProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @Valid @RequestBody UpdateProfessionRequest request) {
        professionService.update(id, request);
        return ApiResponse.success();
    }

    /**
     * 职业审核操作
     * 映射: POST /profession/operate → POST /api/v1/professions/{id}/approve
     */
    @PostMapping("/professions/{id}/approve")
    public ApiResponse<ApprovalResponseDTO> approveProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @RequestBody @Valid OperateRequest request) {
        
        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                professionService.approve(id);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                professionService.reject(id, request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                professionService.ban(id, request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("ban")
                        .build();
            }
            case "delete" -> {
                professionService.delete(id);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("删除成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("delete")
                        .build();
            }
            default -> throw ErrorCode.SYSTEM_ERROR.exception();
        };
        
        return ApiResponse.success(response);
    }

    /**
     * 删除职业
     * 映射: DELETE /profession → DELETE /api/v1/professions/{id}
     */
    @DeleteMapping("/professions/{id}")
    public ApiResponse<Object> deleteProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id) {
        professionService.delete(id);
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