package com.prosper.learn.api.client;

import com.prosper.learn.dto.CourseDTO;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.dto.CourseDTOV3;
import com.prosper.learn.dto.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "learn-service", contextId = "course")
public interface CourseClient {

    // 获取单个对象
    // 课程详情页使用
    @GetMapping("/course/{id}")
    Response<CourseDTOV4> get(@PathVariable Long id);

    @GetMapping("/course/search")
    Response<List<CourseDTOV3>> searchByName(@RequestParam String name);

    // 根据状态和lastId获取列表
    @GetMapping(value = "/course/list", params = {"state", "lastId"})
    Response<Object> getListByState(@RequestParam String state, @RequestParam(value = "lastId", defaultValue = "0") Long lastId);

    // 根据主分类和子分类获取已批准的课程列表
    @GetMapping(value = "/course/list", params = {"mainCategory", "subCategory"})
    Response<Object> getListByCategory(@RequestParam int mainCategory, @RequestParam int subCategory);

    @GetMapping(value = "/course/list/approved", params = {"parentId"})
    Response<Object> getApprovedListByParent(@RequestParam Long parentId);

    @GetMapping(value = "/course/list", params = {"parentId"})
    Response<Object> getListByParent(@RequestParam Long parentId);

    // 获取热门课程（按收藏和正在学习人数排行）
    @GetMapping("/course/hot")
    Response<Object> getHotCourses(@RequestParam(value = "limit", defaultValue = "10") Integer limit);

    // 获取热门课程完整排行榜（前100名）
    @GetMapping("/course/ranking")
    Response<Object> getHotCoursesRanking();

    // 课程状态操作接口（批准、拒绝、屏蔽）
    @PostMapping("/course/operate")
    Response<Object> operate(@RequestParam Long id, @RequestParam String action, @RequestParam(required = false) String rejectedReason);

    // 用户提交新课程
    @PostMapping("/course")
    Response post(@RequestBody CourseDTO course);

    // 用户提交子课程
    @PostMapping("/subcourse")
    Response post(@RequestParam String name, @RequestParam String description, @RequestParam Long parentId);

    // 修改
    // 管理员批准，修改课程
    // 用户修改简介，名称
    @PutMapping("/course/{id}")
    Response put(@PathVariable Long id, @RequestBody CourseDTO course);
}
