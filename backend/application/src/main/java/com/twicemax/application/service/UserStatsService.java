package com.twicemax.application.service;

import com.twicemax.analytics.dto.UserStatsDTO;
import com.twicemax.analytics.stats.mapper.UserStatsDO;
import com.twicemax.analytics.stats.service.UserStatsDomainService;
import com.twicemax.application.converter.UserStatsConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户统计应用服务
 *
 * 负责调用领域服务并转换数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsDomainService userStatsDomainService;
    private final UserStatsConverter userStatsConverter;

    /**
     * 获取用户全部时间统计
     *
     * @param userId 用户ID
     * @return 用户统计DTO
     */
    public UserStatsDTO getUserAllTimeStats(long userId) {
        UserStatsDO statsDO = userStatsDomainService.getUserAllTimeStats(userId);
        return userStatsConverter.toDTO(statsDO);
    }
}
