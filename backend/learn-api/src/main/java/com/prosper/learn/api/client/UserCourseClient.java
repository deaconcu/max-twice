package com.prosper.learn.api.client;

import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserCourseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "userCourseProgress")
public interface UserCourseClient {

    /**
     * 用户开始学习课程
     */
    //@PostMapping("/user/course")
    Response<Object> startCourse(@RequestParam("courseId") Long courseId);

    /**
     * 获取用户课程学习进度
     */
    //@GetMapping("/user/course")
    Response<UserCourseDTO> getUserCourse(@RequestParam("courseId") Long courseId);

    /**
     * 获取用户所有课程学习进度
     */
    //@GetMapping("/user/course/list")
    Response<List<UserCourseDTO>> getUserCourseList(Long lastId);

    /**
     * 更新课程学习进度
     */
    //@PutMapping("/user/course")
    Response<UserCourseDTO> updateUserCourse(@RequestParam("courseId") Long courseId,
                                             @RequestParam("progressPercent") Integer progressPercent);

    /**
     * 删除课程学习记录
     */
    //@DeleteMapping("/user/course")
    Response<Object> deleteUserCourse(@RequestParam("courseId") Long courseId);
}
