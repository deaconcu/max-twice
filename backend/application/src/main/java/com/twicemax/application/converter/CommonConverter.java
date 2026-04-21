package com.twicemax.application.converter;

import com.twicemax.shared.common.util.TimeZoneUtil;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;

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
        return TimeZoneUtil.formatDateTime(localDateTime);
    }
}
