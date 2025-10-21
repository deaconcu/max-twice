package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.ContentState;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.basic.ProfessionRankingService;
import com.prosper.learn.domain.util.converter.ProfessionConverter;
import com.prosper.learn.dto.request.CreateProfessionRequest;
import com.prosper.learn.dto.request.UpdateProfessionRequest;
import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.domain.service.data.ProfessionDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionDataService professionDataService;
    private final ProfessionRankingService professionRankingService;
    private final SystemProperties systemProperties;
    private final ProfessionConverter professionConverter;
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== 公共方法 ==========

    /**
     * 返回正常状态的职业信息
     * 职业被拒绝或屏蔽时抛出异常
     * @param id
     * @return
     */
    public ProfessionDTO getById(long id) {
        ProfessionDO professionDO = professionDataService.getById(id);
        if (professionDO == null) {
            return null;
        }

        if (professionDO.getState() == ContentState.REJECTED.value() ||
            professionDO.getState() == ContentState.BANNED.value()) {
            throw ErrorCode.PROFESSION_BLOCKED.exception();
        }
        return toDTO(professionDO);
    }

    public List<ProfessionDTO> getListByStateAndLastId(ContentState state, Long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByStateAndLastId(state.value(), lastId);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndLastId(int mainCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByMainCategoryAndLastId(mainCategory, lastId);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListBySubCategoryAndLastId(int subCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listBySubCategoryAndLastId(subCategory, lastId);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByCategoryAndLastId(int mainCategory, int subCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
        return toDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByPage(int page) {
        validatePageNumber(page);
        int pageSize = systemProperties.getProfession().getDefaultPageSize();
        List<ProfessionDO> professionDOList = professionDataService.listByPage((page - 1) * pageSize, pageSize);
        return toDTO(professionDOList);
    }

    public Long create(long userId, CreateProfessionRequest request) {
        ProfessionDO professionDO = new ProfessionDO();
        professionDO.setName(request.getName());
        professionDO.setDescription(request.getDescription());
        professionDO.setSkills(request.getSkills());
        professionDO.setMainCategory(request.getMainCategory());
        professionDO.setSubCategory(request.getSubCategory());
        professionDO.setCreatorId(userId);
        professionDO.setState(ContentState.SUBMITTED.value());
        professionDO.setReason("");
        professionDO.setIcon("");
        professionDataService.insert(professionDO);
        return professionDO.getId();
    }

    public void update(Long id, UpdateProfessionRequest request) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("更新请求不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception("职业名称不能为空");
        }

        // 验证职业是否存在并获取
        ProfessionDO professionDO = professionDataService.getById(id);
        if (professionDO == null) {
            throw ErrorCode.PROFESSION_NOT_FOUND.exception();
        }

        // 更新字段
        professionDO.setName(request.getName());
        professionDO.setDescription(request.getDescription());
        professionDO.setPrice(request.getPrice());
        professionDO.setSkills(request.getSkills());
        professionDO.setMainCategory(request.getMainCategory());
        professionDO.setSubCategory(request.getSubCategory());
        professionDO.setIcon(request.getIcon());
        professionDO.setReason(request.getReason());

        professionDataService.update(professionDO);
    }

    public void approve(long id) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证:只有已批准的职业不能重复批准,已拒绝和已屏蔽的可以重新批准
        if (systemProperties.getProfession().isEnableStateValidation()) {
            if (ContentState.PUBLISHED.value() == profession.getState()) {
                throw ErrorCode.ALREADY_APPROVED.exception();
            }
        }

        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionDataService.approve(id));
        } else {
            professionDataService.approve(id);
        }
    }

    public void reject(long id, String reason) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证:已拒绝和已屏蔽的不能重复拒绝
        if (systemProperties.getProfession().isEnableStateValidation()) {
            if (ContentState.REJECTED.value() == profession.getState()) {
                throw ErrorCode.ALREADY_REJECTED.exception();
            }
        }

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionDataService.reject(id, reasonValue));
        } else {
            professionDataService.reject(id, reasonValue);
        }
    }

    public void ban(long id, String reason) {
        ProfessionDO profession = professionDataService.validateAndGet(id);

        // 状态验证:已屏蔽的不能重复屏蔽
        if (systemProperties.getProfession().isEnableStateValidation()) {
            if (ContentState.BANNED.value() == profession.getState()) {
                throw ErrorCode.ALREADY_BANNED.exception();
            }
        }

        String reasonValue = reason != null ? reason : DEFAULT_EMPTY_STRING;

        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionDataService.ban(id, reasonValue));
        } else {
            professionDataService.ban(id, reasonValue);
        }
    }

    // TODO
    public void delete(long id) {
        //professionDataService.delete(id);
    }

    public List<ProfessionDTO> getHotProfessions(int limit) {
        validateHotProfessionsLimit(limit);
        
        try {
            List<Long> hotProfessionIds = professionRankingService.getHotProfessionIds(limit);
            
            if (hotProfessionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ProfessionDO> professionDOList = professionDataService.getByIds(hotProfessionIds);

            List<ProfessionDTO> result = new ArrayList<>();
            for (ProfessionDO professionDO : professionDOList) {
                if (professionDO.getState() != ContentState.PUBLISHED.value()) {
                    continue;
                }

                ProfessionDTO professionDTO = toDTO(professionDO);

                long learningCount = professionRankingService.getProfessionLearningCount(professionDO.getId());
                professionDTO.setLearnerCount((int) learningCount);

                result.add(professionDTO);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("获取热门专业失败，limit: {}", limit, e);
            throw ErrorCode.PROFESSION_HOT_LIST_FAILED.exception(e);
        }
    }

    // ========== DTO转换方法 ==========

    /**
     * 转换单个对象为DTO
     */
    public ProfessionDTO toDTO(ProfessionDO professionDO) {
        return professionConverter.toDTO(professionDO);
    }

    /**
     * 转换列表为DTO列表
     */
    public List<ProfessionDTO> toDTO(List<ProfessionDO> professionDOList) {
        return professionConverter.toDTO(professionDOList);
    }

    // ========== 私有辅助方法 ==========

    private void validatePageNumber(int page) {
        if (page <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }

    private void validateConcurrentStateChange(int rowsAffected) {
        if (rowsAffected == 0) {
            throw ErrorCode.PROFESSION_STATE_CONFLICT.exception();
        }
    }
    
    private void validateHotProfessionsLimit(int limit) {
        if (limit <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        if (limit > systemProperties.getProfession().getMaxHotProfessionsLimit()) {
            throw ErrorCode.PROFESSION_INVALID_LIMIT.exception();
        }
    }
}
