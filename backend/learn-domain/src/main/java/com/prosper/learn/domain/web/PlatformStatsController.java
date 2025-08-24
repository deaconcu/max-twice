package com.prosper.learn.domain.web;

import com.prosper.learn.domain.service.PlatformStatsService;
import com.prosper.learn.dto.PlatformStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 平台统计数据领域控制器
 * 
 * @author Claude
 * @since 2024-01-20
 */
@RestController
@RequestMapping("/platform")
public class PlatformStatsController {
    
    @Autowired
    private PlatformStatsService platformStatsService;
    
    /**
     * 获取平台统计数据
     * 
     * @return 平台统计数据
     */
    @GetMapping("/stats")
    public PlatformStatsDTO getPlatformStats() {
        return platformStatsService.getPlatformStats();
    }
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 刷新后的统计数据
     */
    @PostMapping("/stats/refresh")
    public PlatformStatsDTO refreshPlatformStats() {
        return platformStatsService.refreshPlatformStats();
    }
}