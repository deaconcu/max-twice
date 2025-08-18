package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserCourseClient;
import com.prosper.learn.domain.service.UserCourseService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserCourseController implements UserCourseClient {

    private final UserCourseService userCourseService;

    @Override
    public Response<Object> startCourse(Long courseId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (courseId == null || courseId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "课程ID不能为空", null);
            }

            boolean result = userCourseService.startCourse(userId, courseId);
            return Response.success(result);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "开始学习课程失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<UserCourseDTO> getUserCourse(Long courseId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (courseId == null || courseId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "课程ID不能为空", null);
            }

            UserCourseDTO progress = userCourseService.getUserCourse(userId, courseId);
            return new Response<>(progress);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "获取课程进度失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<List<UserCourseDTO>> getUserCourseList(Long lastId) {
        try {
            if (lastId == null || lastId < 0)  lastId = 0L;
            Long userId = StpUtil.getLoginIdAsLong();

            List<UserCourseDTO> progressList = userCourseService.getUserCourseList(userId, lastId);
            return new Response<>(progressList);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "获取课程进度列表失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<UserCourseDTO> updateUserCourse(Long courseId, Integer progressPercent) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (courseId == null || courseId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "课程ID不能为空", null);
            }

            if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
                return new Response<>(Response.BAD_REQUEST, "进度百分比必须在0-100之间", null);
            }

            UserCourseDTO progress = userCourseService.update(userId, courseId, progressPercent);
            return new Response<>(progress);
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "更新课程进度失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Response<Object> deleteUserCourse(Long courseId) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (courseId == null || courseId <= 0) {
                return new Response<>(Response.BAD_REQUEST, "课程ID不能为空", null);
            }

            userCourseService.delete(userId, courseId);
            return new Response<>("删除成功");
        } catch (Exception e) {
            if (e.getMessage().contains("NotLoginException")) {
                return new Response<>(Response.NOT_LOGIN, "请先登录", null);
            }
            return new Response<>(Response.FAILED, "删除课程进度失败: " + e.getMessage(), null);
        }
    }
}
