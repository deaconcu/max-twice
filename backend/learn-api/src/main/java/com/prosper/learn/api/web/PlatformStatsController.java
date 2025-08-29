package com.prosper.learn.api.web;

import com.prosper.learn.api.client.PlatformStatsClient;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.service.basic.PlatformStatsService;
import com.prosper.learn.dto.PlatformStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 平台统计数据控制器
 * 
 * @author Claude
 * @since 2024-01-20
 */
//@RestController
public class PlatformStatsController implements PlatformStatsClient {
    
    @Autowired
    private PlatformStatsService platformStatsService;
    
    /**
     * 获取平台统计数据
     * 
     * @return 平台统计数据
     */
    @Override
    public Response<PlatformStatsDTO> getPlatformStats() {
        PlatformStatsDTO stats = platformStatsService.getPlatformStats();
        return new Response<>(stats);
    }
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 刷新后的统计数据
     */
    @Override
    public Response<PlatformStatsDTO> refreshPlatformStats() {
        PlatformStatsDTO stats = platformStatsService.refreshPlatformStats();
        return new Response<>(stats);
    }
}