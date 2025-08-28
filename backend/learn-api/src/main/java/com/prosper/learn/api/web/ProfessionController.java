package com.prosper.learn.api.web;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.ProfessionClient;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.ProfessionService;
import com.prosper.learn.domain.service.ProfessionRankingScheduler;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.dto.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProfessionController implements ProfessionClient {

    private final ProfessionService professionService;
    private final ProfessionRankingScheduler professionRankingScheduler;

    @Override
    public Response<Object> listByPage(Integer page) {
        List<ProfessionDTO> professionList = professionService.getListByPage(page);
        return new Response<>(professionList);
    }

    @Override
    public Response<Object> listByStateAndLastId(String state, Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(state, lastId);
        return new Response<>(professionList);
    }

    @Override
    public Response<Object> listByMainCategoryAndLastId(Integer mainCategory, Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndLastId(mainCategory, lastId);
        return new Response<>(professionList);
    }

    @Override
    public Response<Object> listByMainCategoryAndSubCategoryAndLastId(Integer mainCategory, Integer subCategory, Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
        return new Response<>(professionList);
    }

    @Override
    public Response<Object> getById(Long id) {
        ProfessionDTO profession = professionService.getById(id);
        if (profession == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
        return new Response<>(profession);
    }

    @Override
    public Response<Object> create(ProfessionDTO professionDTO) {
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
        return Response.success;
    }

    @Override
    public Response<Object> update(ProfessionDTO professionDTO) {
        if (professionDTO.getId() == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
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

        existing.setName(professionDTO.getName());
        existing.setDescription(professionDTO.getDescription() != null ? professionDTO.getDescription() : "");
        existing.setPrice(professionDTO.getPrice());
        existing.setSkills(professionDTO.getSkills());
        existing.setMainCategory(professionDTO.getMainCategory() != null ? professionDTO.getMainCategory() : 0);
        existing.setSubCategory(professionDTO.getSubCategory() != null ? professionDTO.getSubCategory() : 0);
        existing.setIcon(professionDTO.getIcon() != null ? professionDTO.getIcon() : "");
        existing.setRejectedReason(professionDTO.getRejectedReason() != null ? professionDTO.getRejectedReason() : "");

        professionService.update(existing);
        return new Response<>("更新成功");
    }

    @Override
    public Response<Object> operate(Long id, String action, String rejectedReason) {
        ProfessionDTO existing = professionService.getById(id);
        if (existing == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        switch (action.toLowerCase()) {
            case "approve":
                professionService.approve(id);
                return new Response<>("批准成功");
            case "reject":
                if (rejectedReason == null || rejectedReason.trim().isEmpty()) {
                    throw ErrorCode.SYSTEM_ERROR.exception();
                }
                professionService.reject(id, rejectedReason.trim());
                return new Response<>("拒绝成功");
            default:
                throw ErrorCode.SYSTEM_ERROR.exception();
        }
    }

    @Override
    public Response<Object> delete(Long id) {
        ProfessionDTO existing = professionService.getById(id);
        if (existing == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        professionService.delete(id);
        return new Response<>("删除成功");
    }

    @Override
    public Response<Object> listApproved(Long lastId) {
        List<ProfessionDTO> professionList = professionService.getListByStateAndLastId("APPROVED", lastId);
        return new Response<>(professionList);
    }

    @Override
    public Response<Object> getHotProfessions(Integer limit) {
        List<ProfessionDTO> hotProfessions = professionService.getHotProfessions(limit);
        return new Response<>(hotProfessions);
    }

    // 手动同步职业统计数据的管理接口
    @PostMapping("/profession/sync-stats")
    public Response<Object> syncProfessionStats() {
        log.info("手动触发职业统计数据同步...");
        professionRankingScheduler.manualSync();
        return new Response<>("职业统计数据同步成功");
    }
}