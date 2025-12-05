package com.prosper.learn.shared.converter;

import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通用转换器
 * 提供所有 Converter 共用的转换方法
 */
@Mapper(componentModel = "spring")
public interface CommonConverter {

    /**
     * LocalDateTime 转 String
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    default String map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
