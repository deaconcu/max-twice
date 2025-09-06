package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.ProfessionState;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.ProfessionRankingService;
import com.prosper.learn.domain.util.Converter;
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
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== 公共方法 ==========
    
    public ProfessionDTO getById(long id) {
        validateProfessionId(id);
        ProfessionDO professionDO = professionDataService.getById(id);
        return professionDO != null ? Converter.INSTANCE.toProfessionDTO(professionDO) : null;
    }

    public List<ProfessionDTO> getListByStateAndLastId(ProfessionState state, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByStateAndLastId(state.value(), lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndLastId(int mainCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByMainCategoryAndLastId(mainCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListBySubCategoryAndLastId(int subCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listBySubCategoryAndLastId(subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByCategoryAndLastId(int mainCategory, int subCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionDataService.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByPage(int page) {
        validatePageNumber(page);
        int pageSize = systemProperties.getProfession().getDefaultPageSize();
        List<ProfessionDO> professionDOList = professionDataService.listByPage((page - 1) * pageSize, pageSize);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public Long create(long userId, CreateProfessionRequest request) {
        ProfessionDO professionDO = new ProfessionDO();
        professionDO.setName(request.getName());
        professionDO.setDescription(request.getDescription());
        professionDO.setMainCategory(request.getMainCategory());
        professionDO.setSubCategory(request.getSubCategory());
        professionDO.setSkills(request.getSkills());
        professionDO.setCreatorId(userId);
        professionDO.setState(ProfessionState.SUBMITTED.value());
        professionDO.setRejectedReason(DEFAULT_EMPTY_STRING);
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
        professionDO.setRejectedReason(request.getRejectedReason());
        
        professionDataService.update(professionDO);
    }

    public void approve(long id) {
        ProfessionDTO profession = validateProfessionExists(id);
        
        if (systemProperties.getProfession().isEnableStateValidation()) {
            validateNotAlreadyApproved(profession);
        }
        
        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionDataService.approve(id));
        } else {
            professionDataService.approve(id);
        }
    }

    public void reject(long id, String rejectedReason) {
        ProfessionDTO profession = validateProfessionExists(id);
        
        if (systemProperties.getProfession().isEnableStateValidation()) {
            validateNotAlreadyRejected(profession);
        }
        
        String reason = rejectedReason != null ? rejectedReason : DEFAULT_EMPTY_STRING;
        
        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionDataService.reject(id, reason));
        } else {
            professionDataService.reject(id, reason);
        }
    }

    public void delete(long id) {
        validateProfessionId(id);
        professionDataService.delete(id);
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
                ProfessionDTO professionDTO = Converter.INSTANCE.toProfessionDTO(professionDO);
                
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
    
    // ========== 私有辅助方法 ==========
    
    private void validateProfessionId(long id) {
        if (id <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validatePageNumber(int page) {
        if (page <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private ProfessionDTO validateProfessionExists(long id) {
        validateProfessionId(id);
        ProfessionDTO profession = getById(id);
        if (profession == null) {
            throw ErrorCode.PROFESSION_NOT_FOUND.exception();
        }
        return profession;
    }
    
    private void validateProfessionForCreation(ProfessionDTO professionDTO) {
        if (professionDTO == null) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        if (professionDTO.getName() == null || professionDTO.getName().trim().isEmpty()) {
            throw ErrorCode.PROFESSION_NAME_REQUIRED.exception();
        }
    }
    
    private void validateProfessionForUpdate(ProfessionDTO professionDTO) {
        if (professionDTO == null || professionDTO.getId() == null) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        validateProfessionId(professionDTO.getId());
    }
    
    private void validateNotAlreadyApproved(ProfessionDTO profession) {
        if (ProfessionState.APPROVED.value() == profession.getState()) {
            throw ErrorCode.PROFESSION_ALREADY_APPROVED.exception();
        }
    }
    
    private void validateNotAlreadyRejected(ProfessionDTO profession) {
        if (ProfessionState.REJECTED.value() == profession.getState()) {
            throw ErrorCode.PROFESSION_ALREADY_REJECTED.exception();
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
    
    private void setDefaultValues(ProfessionDTO professionDTO) {
        if (professionDTO.getPrice() == null || professionDTO.getPrice().trim().isEmpty()) {
            professionDTO.setPrice(DEFAULT_EMPTY_STRING);
        }
        if (professionDTO.getSkills() == null || professionDTO.getSkills().trim().isEmpty()) {
            professionDTO.setSkills(DEFAULT_EMPTY_STRING);
        }
        if (professionDTO.getIcon() == null || professionDTO.getIcon().trim().isEmpty()) {
            professionDTO.setIcon(systemProperties.getProfession().getDefaultIcon());
        }
    }
}
