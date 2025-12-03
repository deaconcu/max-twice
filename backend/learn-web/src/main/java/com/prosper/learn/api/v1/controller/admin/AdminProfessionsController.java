package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.business.service.application.ProfessionService;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.request.UpdateProfessionRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 职业管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminProfessionsController {

    private final ProfessionService professionService;

    /**
     * 管理后台：按状态获取职业列表
     * 映射: GET /api/v1/admin/professions?state=0&lastId=123
     */
    @GetMapping("/professions")
    @RequireRole(UserRole.MODERATOR)
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
     * 管理后台：更新职业信息
     * 映射: PUT /api/v1/professions/{id} → PUT /api/v1/admin/professions/{id}
     */
    @PutMapping("/professions/{id}")
    @RequireRole(UserRole.ADMIN)
    @OperationLog(
        module = "内容管理",
        type = "更新职业信息",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Profession",
        targetId = "#id"
    )
    public ApiResponse<Object> updateProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @Valid @RequestBody UpdateProfessionRequest request,
            @CurrentUser UserDO currentUser) {
        professionService.update(id, request, currentUser);
        return ApiResponse.success();
    }

    /**
     * 管理后台：职业审核操作
     * 映射: POST /api/v1/professions/{id}/approve → POST /api/v1/admin/professions/{id}/approve
     */
    @PostMapping("/professions/{id}/approve")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "#request.action == 'APPROVE' ? '审核通过职业' : (#request.action == 'REJECT' ? '审核拒绝职业' : '屏蔽职业')",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Profession",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ApiResponse<ApprovalResponseDTO> approveProfession(
            @PathVariable @NotNull(message = "职业ID不能为空")
            @Positive(message = "职业ID必须大于0")
            Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                professionService.approve(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                professionService.reject(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                professionService.ban(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(id)
                        .objectType("profession")
                        .action("ban")
                        .build();
            }
            case "delete" -> {
                professionService.delete(id, currentUser);
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
}
