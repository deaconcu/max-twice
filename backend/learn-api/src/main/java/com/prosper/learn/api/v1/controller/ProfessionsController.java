package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import static com.prosper.learn.common.Enums.ProfessionState;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.ProfessionService;
import com.prosper.learn.domain.service.ProfessionRankingScheduler;
import com.prosper.learn.dto.ProfessionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class ProfessionsController {

    private final ProfessionService professionService;
    private final ProfessionRankingScheduler professionRankingScheduler;

    /**
     * 分页获取职业
     * 映射: GET /profession/list?page=1 → GET /api/v1/professions?page=0&size=20
     */
    @GetMapping("/professions")
    public ResponseEntity<ApiResponse<Object>> getProfessionsByPage(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Byte state,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer mainCategory,
            @RequestParam(required = false) Integer subCategory) {
        
        if (page != null) {
            // 分页获取职业
            List<ProfessionDTO> professionList = professionService.getListByPage(page);
            return ResponseEntity.ok(ApiResponse.success(professionList));
        } else if (state != null && lastId != null) {
            // 按状态获取职业
            ProfessionState professionState = ProfessionState.getByValue(state.intValue());
            if (professionState == null) {
                throw ErrorCode.INVALID_PARAMETER.exception("Invalid profession state: " + state);
            }
            List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(professionState, lastId);
            return ResponseEntity.ok(ApiResponse.success(professionList));
        } else if (mainCategory != null && subCategory != null && lastId != null) {
            // 按分类获取
            List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
            return ResponseEntity.ok(ApiResponse.success(professionList));
        } else if (mainCategory != null && lastId != null) {
            // 按主分类获取
            List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndLastId(mainCategory, lastId);
            return ResponseEntity.ok(ApiResponse.success(professionList));
        } else {
            throw new IllegalArgumentException("缺少必要参数");
        }
    }

    /**
     * 获取已批准职业
     * 映射: GET /profession/list/approved → GET /api/v1/professions/approved?lastId=123
     */
    @GetMapping("/professions/approved")
    public ResponseEntity<ApiResponse<Object>> getApprovedProfessions(@RequestParam(required = false, defaultValue = "0") Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(ProfessionState.APPROVED, lastId);
        return ResponseEntity.ok(ApiResponse.success(professionList));
    }

    /**
     * 获取职业详情
     * 映射: GET /profession?id=123 → GET /api/v1/professions/{id}
     */
    @GetMapping("/professions/{id}")
    public ResponseEntity<ApiResponse<ProfessionDTO>> getProfession(@PathVariable Long id) {
        ProfessionDTO profession = professionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(profession));
    }

    /**
     * 创建职业
     * 映射: POST /profession → POST /api/v1/professions
     */
    @PostMapping("/professions")
    public ResponseEntity<ApiResponse<Object>> createProfession(@RequestBody ProfessionDTO professionDTO) {
        if (professionDTO.getName() == null || professionDTO.getName().trim().isEmpty()) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (professionDTO.getPrice() == null || professionDTO.getPrice().trim().isEmpty()) {
            professionDTO.setPrice("");
        }
        if (professionDTO.getSkills() == null || professionDTO.getSkills().trim().isEmpty()) {
            professionDTO.setSkills("");
        }
        if (professionDTO.getIcon() == null || professionDTO.getIcon().trim().isEmpty()) {
            professionDTO.setIcon("mdi-triangle-outline");
        }

        professionDTO.setCreator(StpUtil.getLoginIdAsLong());
        professionService.create(professionDTO);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 更新职业
     * 映射: PUT /profession → PUT /api/v1/professions/{id}
     */
    @PutMapping("/professions/{id}")
    public ResponseEntity<ApiResponse<Object>> updateProfession(@PathVariable Long id, @RequestBody ProfessionDTO professionDTO) {
        professionDTO.setId(id);
        if (professionDTO.getName() == null || professionDTO.getName().trim().isEmpty()) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (professionDTO.getPrice() == null || professionDTO.getPrice().trim().isEmpty()) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        if (professionDTO.getSkills() == null || professionDTO.getSkills().trim().isEmpty()) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        ProfessionDTO existing = professionService.getById(professionDTO.getId());
        if (existing == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        professionService.update(professionDTO);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 职业审核操作
     * 映射: POST /profession/operate → POST /api/v1/professions/{id}/approve
     */
    @PostMapping("/professions/{id}/approve")
    public ResponseEntity<ApiResponse<Object>> approveProfession(
            @PathVariable Long id, 
            @RequestParam String action, 
            @RequestParam(required = false) String rejectedReason) {
        
        return switch (action.toLowerCase()) {
            case "approve" -> {
                professionService.approve(id);
                yield ResponseEntity.ok(ApiResponse.success("批准成功"));
            }
            case "reject" -> {
                professionService.reject(id, rejectedReason);
                yield ResponseEntity.ok(ApiResponse.success("拒绝成功"));
            }
            case "delete" -> {
                professionService.delete(id);
                yield ResponseEntity.ok(ApiResponse.success("删除成功"));
            }
            default -> throw ErrorCode.SYSTEM_ERROR.exception();
        };
    }

    /**
     * 删除职业
     * 映射: DELETE /profession → DELETE /api/v1/professions/{id}
     */
    @DeleteMapping("/professions/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteProfession(@PathVariable Long id) {
        professionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    /**
     * 热门职业
     * 映射: GET /profession/hot → GET /api/v1/professions/hot?limit=10
     */
    @GetMapping("/professions/hot")
    public ResponseEntity<ApiResponse<Object>> getHotProfessions(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("开始获取热门职业，limit: {}", limit);
        List<ProfessionDTO> hotProfessions = professionService.getHotProfessions(limit);
        log.info("成功获取热门职业数量: {}", hotProfessions.size());
        return ResponseEntity.ok(ApiResponse.success(hotProfessions));
    }
}