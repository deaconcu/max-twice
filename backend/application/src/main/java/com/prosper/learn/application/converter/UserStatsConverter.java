package com.prosper.learn.application.converter;

import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import org.mapstruct.Mapper;

/**
 * 用户统计转换器
 */
@Mapper(componentModel = "spring", uses = CommonConverter.class)
public interface UserStatsConverter {

    UserStatsDTO toDTO(UserStatsDO statsDO);
}
