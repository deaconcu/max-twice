package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.ProfessionService;
import com.prosper.learn.dto.response.ProfessionDTO;
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
}
