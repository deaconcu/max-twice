package com.twicemax.application.converter;

import com.twicemax.application.dto.response.userlearning.UserLearningDTO;
import com.twicemax.learning.enrollment.UserLearningDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户学习记录转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserLearningConverter {

    /**
     * 转换为基础 DTO（不带关联对象）
     * state 字段通过 DO 的 getState() 方法动态计算
     */
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserLearningDTO<Object> toDTO(UserLearningDO userLearningDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserLearningDTO<Object>> toDTO(List<UserLearningDO> userLearningDOList);
}
