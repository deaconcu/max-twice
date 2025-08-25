package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.PlatformStatsDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
    @GetMapping("/platform/stats")
    Response<PlatformStatsDTO> getPlatformStats();
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 刷新后的统计数据
     */
    @PostMapping("/platform/stats/refresh")
    Response<PlatformStatsDTO> refreshPlatformStats();
}