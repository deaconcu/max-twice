package com.prosper.learn.web.client;

import com.prosper.learn.dto.response.Response;
import com.prosper.learn.dto.response.PlatformStatsDTO;

/**
 * 平台统计数据接口
 * 
 * @author Claude
 * @since 2024-01-20
 */
public interface PlatformStatsClient {
    
    /**
     * 获取平台统计数据
     * 
     * @return 平台统计数据
     */
    //@GetMapping("/platform/stats")
    Response<PlatformStatsDTO> getPlatformStats();
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 刷新后的统计数据
     */
    //@PostMapping("/platform/stats/refresh")
    Response<PlatformStatsDTO> refreshPlatformStats();
}