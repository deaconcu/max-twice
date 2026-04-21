package com.twicemax.application.converter;

import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import com.twicemax.memory.card.MemoryCardDO;
import com.twicemax.memory.card.MemoryCardVersionDO;
import org.mapstruct.*;

/**
 * 记忆卡片转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface MemoryCardConverter {

    /**
     * 转换为含SRS状态的 DTO（需要在 Service 层填充所有关联字段）
     */
    @Named("toWithSrsDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "cardDO.id", target = "id")
    @Mapping(source = "versionDO.front", target = "front")
    @Mapping(source = "versionDO.back", target = "back")
    CardWithSrsDTO toWithSrsDTO(MemoryCardDO cardDO, MemoryCardVersionDO versionDO);

}
