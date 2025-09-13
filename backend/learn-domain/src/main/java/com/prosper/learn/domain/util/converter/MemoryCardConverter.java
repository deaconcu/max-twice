package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.MemoryCardViewDTO;
import com.prosper.learn.persistence.dataobject.MemoryCardDO;
import com.prosper.learn.persistence.dataobject.MemoryCardVersionDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 记忆卡片转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemoryCardConverter {


    @Named("toViewDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "cardDO.id", target = "id")
    @Mapping(source = "versionDO.front", target = "front")
    @Mapping(source = "versionDO.back", target = "back")
    MemoryCardViewDTO toViewDTO(MemoryCardDO cardDO, MemoryCardVersionDO versionDO);

}