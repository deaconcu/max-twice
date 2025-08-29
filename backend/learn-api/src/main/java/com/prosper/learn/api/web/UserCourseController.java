package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserCourseClient;
import com.prosper.learn.domain.service.UserCourseService;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@RestController
@RequiredArgsConstructor
public class UserCourseController implements UserCourseClient {

    private final UserCourseService userCourseService;

    @Override
    public Response<Object> startCourse(Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        boolean result = userCourseService.startCourse(userId, courseId);
        return Response.success(result);
    }

    @Override
    public Response<UserCourseDTO> getUserCourse(Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        UserCourseDTO progress = userCourseService.getUserCourse(userId, courseId);
        return new Response<>(progress);
    }

    @Override
    public Response<List<UserCourseDTO>> getUserCourseList(Long lastId) {
        if (lastId == null || lastId < 0) lastId = 0L;
        Long userId = StpUtil.getLoginIdAsLong();

        List<UserCourseDTO> progressList = userCourseService.getUserCourseList(userId, lastId);
        return new Response<>(progressList);
    }

    @Override
    public Response<UserCourseDTO> updateUserCourse(Long courseId, Integer progressPercent) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("进度百分比必须在0-100之间");
        }

        UserCourseDTO progress = userCourseService.update(userId, courseId, progressPercent);
        return new Response<>(progress);
    }

    @Override
    public Response<Object> deleteUserCourse(Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        userCourseService.delete(userId, courseId);
        return new Response<>("删除成功");
    }
}
