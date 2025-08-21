package com.prosper.learn.api.web;

import com.prosper.learn.api.client.PlatformStatsApi;
import com.prosper.learn.common.ResponseResult;
import com.prosper.learn.domain.service.PlatformStatsService;
import com.prosper.learn.dto.PlatformStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 平台统计数据控制器
 * 
 * @author Claude
 * @since 2024-01-20
 */
@RestController
public class PlatformStatsController implements PlatformStatsApi {
    
    @Autowired
    private PlatformStatsService platformStatsService;
    
    /**
     * 获取平台统计数据
     * 
     * @return 平台统计数据
     */
    @Override
    public ResponseResult<PlatformStatsDTO> getPlatformStats() {
        try {
            PlatformStatsDTO stats = platformStatsService.getPlatformStats();
            return ResponseResult.ok(stats);
        } catch (Exception e) {
            ResponseResult<PlatformStatsDTO> result = ResponseResult.internal_server_error();
            return result.setMsg("获取平台统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 刷新后的统计数据
     */
    @Override
    public ResponseResult<PlatformStatsDTO> refreshPlatformStats() {
        try {
            PlatformStatsDTO stats = platformStatsService.refreshPlatformStats();
            return ResponseResult.ok(stats);
        } catch (Exception e) {
            ResponseResult<PlatformStatsDTO> result = ResponseResult.internal_server_error();
            return result.setMsg("刷新平台统计数据失败: " + e.getMessage());
        }
    }
}