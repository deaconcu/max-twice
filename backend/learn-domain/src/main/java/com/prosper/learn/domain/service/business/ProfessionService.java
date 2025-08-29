package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.ProfessionState;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.ProfessionRankingService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.mapper.ProfessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionMapper professionMapper;
    private final ProfessionRankingService professionRankingService;

    public ProfessionDTO getById(long id) {
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
        List<ProfessionDO> professionDOList = professionMapper.listByPage((page - 1) * 20, 20);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public Long create(ProfessionDTO professionDTO) {
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

        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionDO.setState(ProfessionState.SUBMITTED.value());
        professionDO.setRejectedReason("");
        professionMapper.insert(professionDO);
        return professionDO.getId();
    }

    public void update(ProfessionDTO professionDTO) {
        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionMapper.update(professionDO);
    }

    public void approve(long id) {
        // 先查询当前状态
        ProfessionDTO profession = getById(id);
        if (profession == null) {
            throw new RuntimeException("操作失败：专业不存在");
        }
        if (ProfessionState.APPROVED.value() == profession.getState()) {
            throw new RuntimeException("操作失败：专业状态已是批准状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = professionMapper.approve(id);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：专业状态已被其他操作修改，请刷新后重试");
        }
    }

    public void reject(long id, String rejectedReason) {
        // 先查询当前状态
        ProfessionDTO profession = getById(id);
        if (profession == null) {
            throw new RuntimeException("操作失败：专业不存在");
        }
        if (ProfessionState.REJECTED.value() == profession.getState()) {
            throw new RuntimeException("操作失败：专业状态已是拒绝状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = professionMapper.reject(id, rejectedReason != null ? rejectedReason : "");
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：专业状态已被其他操作修改，请刷新后重试");
        }
    }

    public void delete(long id) {
        professionMapper.delete(id);
    }

    // 获取热门职业（使用Redis排行榜，按学习人数排序）
    public List<ProfessionDTO> getHotProfessions(int limit) {
        try {
            // 从Redis获取热门职业ID列表
            List<Integer> hotProfessionIds = professionRankingService.getHotProfessionIds(limit);
            
            if (hotProfessionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 根据ID列表获取职业详情
            List<ProfessionDO> professionDOList = professionMapper.getByIds(hotProfessionIds);
            
            // 转换为DTO并附加学习人数统计
            List<ProfessionDTO> result = new ArrayList<>();
            for (ProfessionDO professionDO : professionDOList) {
                ProfessionDTO professionDTO = Converter.INSTANCE.toProfessionDTO(professionDO);
                
                // 从Redis获取学习人数统计
                long learningCount = professionRankingService.getProfessionLearningCount(professionDO.getId());
                professionDTO.setLearnerCount((int) learningCount);
                
                result.add(professionDTO);
            }
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取热门职业失败: " + e.getMessage(), e);
        }
    }
}
