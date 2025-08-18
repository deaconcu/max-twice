package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.dto.CourseDTO;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.dto.Response;
import com.prosper.learn.api.client.CourseClient;
import com.prosper.learn.dto.CourseDTOV3;
import com.prosper.learn.domain.service.CourseService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class CourseController implements CourseClient {

    private final CourseMapper courseMapper;
    private final CourseService courseService;

    @Override
    public Response<CourseDTOV4> get(int id) {
        CourseDO course = courseMapper.getById(id);
        return new Response<>(Converter.INSTANCE.toCourseDTOV4(course));
    }

    @Override
    public Response<Object> getListByState(String state, int lastId) {
        try {
            List<CourseDTOV4> courseList = courseService.getListByStateAndLastId(state, lastId);
            return new Response<>(courseList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取课程列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> getListByCategory(int mainCategory, int subCategory) {
        try {
            List<CourseDTOV4> courseList = courseService.getListByCategory(mainCategory, subCategory);
            return new Response<>(courseList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取课程列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> getApprovedListByParent(int parentId) {
        try {
            List<CourseDTOV4> courseList = courseService.getListByParent(parentId, "APPROVED");
            return new Response<>(courseList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取子课程列表失败: " + e.getMessage());
        }
    }

    public Response<Object> getListByParent(int parentId) {
        try {
            List<CourseDTOV4> courseList = courseService.getListByParent(parentId, "ALL");
            return new Response<>(courseList);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取子课程列表失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> operate(int id, String action, String rejectedReason) {
        try {
            // 检查课程是否存在
            if (!courseService.exist(id)) {
                return new Response<>(Response.NOT_FOUND, "课程不存在");
            }

            return switch (action.toLowerCase()) {
                case "approve" -> {
                    courseService.approve(id);
                    yield new Response<>("批准成功");
                }
                case "reject" -> {
                    courseService.reject(id, rejectedReason);
                    yield new Response<>("拒绝成功");
                }
                case "delete" -> {
                    courseService.delete(id);
                    yield new Response<>("删除成功");
                }
                default -> new Response<>(Response.BAD_REQUEST, "不支持的操作类型: " + action);
            };
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "操作失败: " + e.getMessage());
        }
    }

    @Override
    public Response<Object> post(CourseDTO course) {
        int userId = StpUtil.getLoginIdAsInt();
        course.setCreator(userId);
        courseService.createCourse(course);
        return Response.success("课程创建成功");
    }

    @Override
    public Response post(String name, String description, int parentId) {
        int userId = StpUtil.getLoginIdAsInt();

        courseService.createSubcourse(name, description, parentId, userId);
        return Response.success("课程创建成功");
    }

    @Override
    public Response<Object> put(@PathVariable int id, CourseDTO course) {
        CourseDO courseDo = courseMapper.getById(id);
        if (courseDo == null) return new Response<>(404, "课程不存在");

        courseDo.setName(course.getName());
        courseDo.setDescription(course.getDescription());
        courseMapper.update(courseDo);

        return new Response<>(200, "更新成功");
    }

    @Override
    public Response<List<CourseDTOV3>> searchByName(@RequestParam String name) {
        List<CourseDO> courseList = courseMapper.searchByName(name, 20);
        return new Response<>(Converter.INSTANCE.toCourseDTOV3(courseList));
    }

    @Override
    public Response<Object> getHotCourses(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            List<CourseDTOV4> hotCourses = courseService.getHotCourses(limit);
            return new Response<>(hotCourses);
        } catch (Exception e) {
            return new Response<>(Response.FAILED, "获取热门课程失败: " + e.getMessage());
        }
    }
}
