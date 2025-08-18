package com.prosper.learn.api.web;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.ProfessionClient;
import com.prosper.learn.domain.service.ProfessionService;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.dto.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfessionController implements ProfessionClient {

    private final ProfessionService professionService;

    @Override
    public Response<Object> listByPage(int page) {
        try {
            List<ProfessionDTO> professionList = professionService.getListByPage(page);
            return new Response<>(professionList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取专业列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> listByStateAndLastId(String state, int lastId) {
        try {
            List<ProfessionDTO> professionList = professionService.getListByStateAndLastId(state, lastId);
            return new Response<>(professionList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取专业列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> listByMainCategoryAndLastId(int mainCategory, int lastId) {
        try {
            List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndLastId(mainCategory, lastId);
            return new Response<>(professionList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取专业列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, int lastId) {
        try {
            List<ProfessionDTO> professionList = professionService.getListByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
            return new Response<>(professionList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取专业列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> getById(int id) {
        try {
            ProfessionDTO profession = professionService.getById(id);
            if (profession == null) {
                return new Response<>(Response.NOT_FOUND, "专业不存在");
            }
            return new Response<>(profession);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取专业信息失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> create(ProfessionDTO professionDTO) {
        try {
            if (professionDTO.getName() == null || professionDTO.getName().trim().isEmpty()) {
                return new Response<>(Response.BAD_REQUEST, "专业名称不能为空");
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

            professionDTO.setCreator(StpUtil.getLoginIdAsLong()); // 设置创建者
            int id = professionService.create(professionDTO);
            return Response.success;
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "创建专业失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> update(ProfessionDTO professionDTO) {
        try {
            if (professionDTO.getId() == null) {
                return new Response<>(Response.BAD_REQUEST, "专业ID不能为空");
            }
            if (professionDTO.getName() == null || professionDTO.getName().trim().isEmpty()) {
                return new Response<>(Response.BAD_REQUEST, "专业名称不能为空");
            }
            if (professionDTO.getPrice() == null || professionDTO.getPrice().trim().isEmpty()) {
                return new Response<>(Response.BAD_REQUEST, "价格信息不能为空");
            }
            if (professionDTO.getSkills() == null || professionDTO.getSkills().trim().isEmpty()) {
                return new Response<>(Response.BAD_REQUEST, "技能要求不能为空");
            }

            // 检查专业是否存在
            ProfessionDTO existing = professionService.getById(professionDTO.getId());
            if (existing == null) {
                return new Response<>(Response.NOT_FOUND, "专业不存在");
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
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "更新专业失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> operate(int id, String action, String rejectedReason) {
        try {
            // 检查专业是否存在
            ProfessionDTO existing = professionService.getById(id);
            if (existing == null) {
                return new Response<>(Response.NOT_FOUND, "专业不存在");
            }

            switch (action.toLowerCase()) {
                case "approve":
                    professionService.approve(id);
                    return new Response<>("批准成功");
                case "reject":
                    if (rejectedReason == null || rejectedReason.trim().isEmpty()) {
                        return new Response<>(Response.BAD_REQUEST, "拒绝原因不能为空");
                    }
                    professionService.reject(id, rejectedReason.trim());
                    return new Response<>("拒绝成功");
                default:
                    return new Response<>(Response.BAD_REQUEST, "不支持的操作类型: " + action);
            }
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "操作失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> delete(int id) {
        try {
            // 检查专业是否存在
            ProfessionDTO existing = professionService.getById(id);
            if (existing == null) {
                return new Response<>(Response.NOT_FOUND, "专业不存在");
            }

            professionService.delete(id);
            return new Response<>("删除成功");
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "删除专业失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> listApproved(int lastId) {
        try {
            List<ProfessionDTO> professionList = professionService.getListByStateAndLastId("APPROVED", lastId);
            return new Response<>(professionList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取已批准专业列表失败: " + e.getMessage());
        }
    }
}
