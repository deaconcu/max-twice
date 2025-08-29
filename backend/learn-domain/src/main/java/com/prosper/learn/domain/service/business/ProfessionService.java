package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.ProfessionState;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.ProfessionRankingService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.mapper.ProfessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionMapper professionMapper;
    private final ProfessionRankingService professionRankingService;
    private final SystemProperties systemProperties;
    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";

    // ========== 公共方法 ==========
    
    public ProfessionDTO getById(long id) {
        validateProfessionId(id);
        ProfessionDO professionDO = professionMapper.get(id);
        return professionDO != null ? Converter.INSTANCE.toProfessionDTO(professionDO) : null;
    }

    public List<ProfessionDTO> getListByStateAndLastId(ProfessionState state, long lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByStateAndLastId(state.value(), lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndLastId(int mainCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByMainCategoryAndLastId(mainCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListBySubCategoryAndLastId(int subCategory, int lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listBySubCategoryAndLastId(subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, long lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByPage(int page) {
        validatePageNumber(page);
        int pageSize = systemProperties.getProfession().getDefaultPageSize();
        List<ProfessionDO> professionDOList = professionMapper.listByPage((page - 1) * pageSize, pageSize);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public Long create(ProfessionDTO professionDTO) {
        validateProfessionForCreation(professionDTO);
        
        // 设置默认值
        setDefaultValues(professionDTO);
        
        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionDO.setState(ProfessionState.SUBMITTED.value());
        professionDO.setRejectedReason(DEFAULT_EMPTY_STRING);
        professionMapper.insert(professionDO);
        return professionDO.getId();
    }

    public void update(ProfessionDTO professionDTO) {
        validateProfessionForUpdate(professionDTO);
        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionMapper.update(professionDO);
    }

    public void approve(long id) {
        ProfessionDTO profession = validateProfessionExists(id);
        
        if (systemProperties.getProfession().isEnableStateValidation()) {
            validateNotAlreadyApproved(profession);
        }
        
        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionMapper.approve(id));
        } else {
            professionMapper.approve(id);
        }
    }

    public void reject(long id, String rejectedReason) {
        ProfessionDTO profession = validateProfessionExists(id);
        
        if (systemProperties.getProfession().isEnableStateValidation()) {
            validateNotAlreadyRejected(profession);
        }
        
        String reason = rejectedReason != null ? rejectedReason : DEFAULT_EMPTY_STRING;
        
        if (systemProperties.getProfession().isEnableConcurrencyCheck()) {
            validateConcurrentStateChange(professionMapper.reject(id, reason));
        } else {
            professionMapper.reject(id, reason);
        }
    }

    public void delete(long id) {
        validateProfessionId(id);
        professionMapper.delete(id);
    }

    public List<ProfessionDTO> getHotProfessions(int limit) {
        validateHotProfessionsLimit(limit);
        
        try {
            List<Integer> hotProfessionIds = professionRankingService.getHotProfessionIds(limit);
            
            if (hotProfessionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<ProfessionDO> professionDOList = professionMapper.getByIds(hotProfessionIds);
            
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
