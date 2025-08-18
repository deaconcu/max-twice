
package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserRoadmapClient;
import com.prosper.learn.domain.service.UserRoadmapService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserRoadmapDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserRoadmapController implements UserRoadmapClient {

    private final UserRoadmapService userRoadmapService;

    @Override
    public Response<Object> startRoadmap(Long roadmapId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (roadmapId == null || roadmapId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "路线图ID不能为空", null);
            }

            boolean result = userRoadmapService.startRoadmap(userId, roadmapId);
            return Response.success(result);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "开始学习路线图失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<UserRoadmapDTO> getRoadmap(Long roadmapId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (roadmapId == null || roadmapId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "路线图ID不能为空", null);
            }

            UserRoadmapDTO progress = userRoadmapService.getUserRoadmap(userId, roadmapId);
            return new Response<>(progress);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "获取路线图进度失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<List<UserRoadmapDTO>> getAllRoadmap() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            List<UserRoadmapDTO> progressList = userRoadmapService.getUserAllRoadmap(userId);
            return new Response<>(progressList);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "获取路线图进度列表失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<UserRoadmapDTO> updateRoadmap(Long roadmapId, Integer progressPercent) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (roadmapId == null || roadmapId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "路线图ID不能为空", null);
            }

            if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
                return new Response<>(Response.BAD_REQUEST, "进度百分比必须在0-100之间", null);
            }

            UserRoadmapDTO progress = userRoadmapService.updateProgress(userId, roadmapId, progressPercent);
            return new Response<>(progress);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "更新路线图进度失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<Object> deleteRoadmap(Long roadmapId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (roadmapId == null || roadmapId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "路线图ID不能为空", null);
            }

            userRoadmapService.deleteRoadmap(userId, roadmapId);
            return new Response<>("删除成功");
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "删除路线图进度失败: " + e.getMessage(), null);
        }
    }
}