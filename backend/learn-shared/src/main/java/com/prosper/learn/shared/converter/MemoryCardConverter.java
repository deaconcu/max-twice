package com.prosper.learn.shared.converter;

import com.prosper.learn.persistence.dataobject.MemoryCardDO;
import com.prosper.learn.persistence.dataobject.MemoryCardVersionDO;
import org.mapstruct.*;

/**
 * 记忆卡片转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface MemoryCardConverter {

    /**
     * 转换为卡片内容 DTO（基础信息：id + front + back）
     */
    @Named("toContentDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "cardDO.id", target = "id")
    @Mapping(source = "versionDO.front", target = "front")
    @Mapping(source = "versionDO.back", target = "back")
    CardContentDTO toContentDTO(MemoryCardDO cardDO, MemoryCardVersionDO versionDO);

    /**
     * 转换为含卡片组的 DTO（需要在 Service 层填充 deck 字段）
     */
    @Named("toWithDeckDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "cardDO.id", target = "id")
    @Mapping(source = "versionDO.front", target = "front")
    @Mapping(source = "versionDO.back", target = "back")
    CardWithDeckDTO toWithDeckDTO(MemoryCardDO cardDO, MemoryCardVersionDO versionDO);

    /**
     * 转换为含创建者的 DTO（需要在 Service 层填充 deck 和 creator 字段）
     */
    @Named("toWithCreatorDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "cardDO.id", target = "id")
    @Mapping(source = "versionDO.front", target = "front")
    @Mapping(source = "versionDO.back", target = "back")
    CardWithCreatorDTO toWithCreatorDTO(MemoryCardDO cardDO, MemoryCardVersionDO versionDO);

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
