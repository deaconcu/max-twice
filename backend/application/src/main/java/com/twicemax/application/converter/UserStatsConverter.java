package com.twicemax.application.converter;

import com.twicemax.analytics.dto.UserStatsDTO;
import com.twicemax.analytics.stats.mapper.UserStatsDO;
import org.mapstruct.Mapper;

/**
 * 用户统计转换器
 */
@Mapper(componentModel = "spring", uses = CommonConverter.class)
public interface UserStatsConverter {

    UserStatsDTO toDTO(UserStatsDO statsDO);
}
