package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.CourseMemoryBankDTO;
import com.prosper.learn.persistence.dataobject.CourseMemoryBankDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 课程记忆库转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMemoryBankConverter {

    /**
     * 将CourseMemoryBankDO转换为CourseMemoryBankDTO的卡片统计部分
     * 注意：这个方法只转换卡片统计字段，不包含course和setting字段
     */
    @Named("toCardStatsDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "cardCount")
    @Mapping(target = "dueCardCount")
    @Mapping(target = "newCardCount")
    @Mapping(target = "reviewCardCount")
    @Mapping(target = "learnedCardCount")
    CourseMemoryBankDTO toCardStatsDTO(CourseMemoryBankDO courseMemoryBankDO);

    /**
     * 批量转换卡片统计信息
     */
    @IterableMapping(qualifiedByName = "toCardStatsDTO")
    List<CourseMemoryBankDTO> toCardStatsDTO(List<CourseMemoryBankDO> courseMemoryBankDOList);

    /**
     * 将CourseMemoryBankDO的统计信息应用到已有的CourseMemoryBankDTO
     * 这个方法用于在已经有course和setting信息的DTO上补充统计信息
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "cardCount")
    @Mapping(target = "dueCardCount")
    @Mapping(target = "newCardCount")
    @Mapping(target = "reviewCardCount")
    @Mapping(target = "learnedCardCount")
    void updateCardStats(@MappingTarget CourseMemoryBankDTO dto, CourseMemoryBankDO courseMemoryBankDO);
}