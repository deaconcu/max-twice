
package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserRoadmapClient;
import com.prosper.learn.domain.service.business.UserRoadmapService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserRoadmapDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

//@RestController
@RequiredArgsConstructor
public class UserRoadmapController implements UserRoadmapClient {

    private final UserRoadmapService userRoadmapService;

    @Override
    public Response<Object> startRoadmap(Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (roadmapId == null || roadmapId <= 0) {
            throw new IllegalArgumentException("路线图ID不能为空");
        }

        boolean result = userRoadmapService.startRoadmap(userId, roadmapId);
        return Response.success(result);
    }

    @Override
    public Response<UserRoadmapDTO> getRoadmap(Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (roadmapId == null || roadmapId <= 0) {
            throw new IllegalArgumentException("路线图ID不能为空");
        }

        UserRoadmapDTO progress = userRoadmapService.getUserRoadmap(userId, roadmapId);
        return new Response<>(progress);
    }

    @Override
    public Response<List<UserRoadmapDTO>> getAllRoadmap() {
        Long userId = StpUtil.getLoginIdAsLong();

        List<UserRoadmapDTO> progressList = userRoadmapService.getUserAllRoadmap(userId);
        return new Response<>(progressList);
    }

    @Override
    public Response<UserRoadmapDTO> updateRoadmap(Long roadmapId, Integer progressPercent) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (roadmapId == null || roadmapId <= 0) {
            throw new IllegalArgumentException("路线图ID不能为空");
        }

        if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("进度百分比必须在0-100之间");
        }

        UserRoadmapDTO progress = userRoadmapService.updateProgress(userId, roadmapId, progressPercent);
        return new Response<>(progress);
    }

    @Override
    public Response<Object> deleteRoadmap(Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (roadmapId == null || roadmapId <= 0) {
            throw new IllegalArgumentException("路线图ID不能为空");
        }

        userRoadmapService.deleteRoadmap(userId, roadmapId);
        return new Response<>("删除成功");
    }
}