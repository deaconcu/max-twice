package com.prosper.learn.application.service;

import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
import com.prosper.learn.application.converter.UserStatsConverter;
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
