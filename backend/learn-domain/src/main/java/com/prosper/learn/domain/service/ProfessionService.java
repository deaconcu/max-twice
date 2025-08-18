package com.prosper.learn.domain.service;

import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.mapper.ProfessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionMapper professionMapper;

    public ProfessionDTO getById(int id) {
        ProfessionDO professionDO = professionMapper.get(id);
        return professionDO != null ? Converter.INSTANCE.toProfessionDTO(professionDO) : null;
    }

    public List<ProfessionDTO> getListByStateAndLastId(String state, int lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByStateAndLastId(state, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndLastId(int mainCategory, int lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByMainCategoryAndLastId(mainCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListBySubCategoryAndLastId(int subCategory, int lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listBySubCategoryAndLastId(subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, int lastId) {
        List<ProfessionDO> professionDOList = professionMapper.listByMainCategoryAndSubCategoryAndLastId(mainCategory, subCategory, lastId);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public List<ProfessionDTO> getListByPage(int page) {
        List<ProfessionDO> professionDOList = professionMapper.listByPage((page - 1) * 20, 20);
        return Converter.INSTANCE.toProfessionDTO(professionDOList);
    }

    public int create(ProfessionDTO professionDTO) {
        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionDO.setState("SUBMITED");
        professionDO.setRejectedReason("");
        professionMapper.insert(professionDO);
        return professionDO.getId();
    }

    public void update(ProfessionDTO professionDTO) {
        ProfessionDO professionDO = Converter.INSTANCE.toProfessionDO(professionDTO);
        professionMapper.update(professionDO);
    }

    public void approve(int id) {
        // 先查询当前状态
        ProfessionDTO profession = getById(id);
        if (profession == null) {
            throw new RuntimeException("操作失败：专业不存在");
        }
        if ("APPROVED".equals(profession.getState())) {
            throw new RuntimeException("操作失败：专业状态已是批准状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = professionMapper.approve(id);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：专业状态已被其他操作修改，请刷新后重试");
        }
    }

    public void reject(int id, String rejectedReason) {
        // 先查询当前状态
        ProfessionDTO profession = getById(id);
        if (profession == null) {
            throw new RuntimeException("操作失败：专业不存在");
        }
        if ("REJECTED".equals(profession.getState())) {
            throw new RuntimeException("操作失败：专业状态已是拒绝状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = professionMapper.reject(id, rejectedReason != null ? rejectedReason : "");
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：专业状态已被其他操作修改，请刷新后重试");
        }
    }

    public void delete(int id) {
        professionMapper.delete(id);
    }
}
