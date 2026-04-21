package com.twicemax.application.converter;

import com.twicemax.application.dto.response.bookmark.BookmarkDTO;
import com.twicemax.interaction.bookmark.BookmarkDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 收藏记录转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface BookmarkConverter {

    /**
     * 转换为基础 DTO（不带关联对象）
     */
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "parentId")
    @Mapping(target = "createdAt")
    BookmarkDTO<Object> toDTO(BookmarkDO bookmarkDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<BookmarkDTO<Object>> toDTO(List<BookmarkDO> bookmarkDOList);
}
