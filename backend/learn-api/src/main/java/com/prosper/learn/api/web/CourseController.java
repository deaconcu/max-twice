package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.response.CourseDTO;
import com.prosper.learn.dto.response.CourseDTOV4;
import com.prosper.learn.dto.response.Response;
import com.prosper.learn.api.client.CourseClient;
import com.prosper.learn.dto.response.CourseDTOV3;
import com.prosper.learn.domain.service.business.CourseService;
import com.prosper.learn.domain.service.scheduler.CourseRankingScheduler;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.prosper.learn.common.Enums.*;

//@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class CourseController implements CourseClient {

    private final CourseMapper courseMapper;
    private final CourseService courseService;
    private final CourseRankingScheduler courseRankingScheduler;

    @Override
    public Response<CourseDTOV4> get(Long id) {
        CourseDO course = courseMapper.getById(id);
        return new Response<>(Converter.INSTANCE.toCourseDTOV4(course));
    }

    @Override
    public Response<Object> getListByState(Integer state, Long lastId) {
        List<CourseDTOV4> courseList = courseService.getListByStateAndLastId(CourseState.getByValue(state), lastId);
        return new Response<>(courseList);
    }

    @Override
    public Response<Object> getListByCategory(int mainCategory, int subCategory) {
        List<CourseDTOV4> courseList = courseService.getListByCategory(mainCategory, subCategory);
        return new Response<>(courseList);
    }

    @Override
    public Response<Object> getApprovedListByParent(Long parentId) {
        List<CourseDTOV4> courseList = courseService.getListByParent(parentId, CourseState.APPROVED);
        return new Response<>(courseList);
    }

    public Response<Object> getListByParent(Long parentId) {
        List<CourseDTOV4> courseList = courseService.getListByParent(parentId, null);
        return new Response<>(courseList);
    }

    @Override
    public Response<Object> operate(Long id, String action, String rejectedReason) {
        if (!courseService.exist(id)) {
            throw ErrorCode.SYSTEM_ERROR.exception();
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
            default -> throw ErrorCode.SYSTEM_ERROR.exception();
        };
    }

    @Override
    public Response<Object> post(CourseDTO course) {
        long userId = StpUtil.getLoginIdAsLong();
        course.setCreator(userId);
        //courseService.createCourse(course);
        return Response.success("课程创建成功");
    }

    @Override
    public Response post(String name, String description, Long parentId) {
        long userId = StpUtil.getLoginIdAsLong();

        courseService.createSubcourse(name, description, parentId, userId);
        return Response.success("课程创建成功");
    }

    @Override
    public Response<Object> put(@PathVariable Long id, CourseDTO course) {
        CourseDO courseDo = courseMapper.getById(id);
        if (courseDo == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

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
    public Response<Object> getHotCourses(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("开始获取热门课程，limit: {}", limit);
        List<CourseDTOV4> hotCourses = courseService.getHotCourses(limit);
        log.info("成功获取热门课程数量: {}", hotCourses.size());
        return new Response<>(hotCourses);
    }

    @Override
    public Response<Object> getHotCoursesRanking() {
        log.info("开始获取热门课程完整排行榜");
        List<CourseDTOV4> hotCoursesRanking = courseService.getHotCoursesRanking();
        log.info("成功获取热门课程排行榜数量: {}", hotCoursesRanking.size());
        return new Response<>(hotCoursesRanking);
    }

    // 手动同步课程统计数据的管理接口
    @PostMapping("/course/sync-stats")
    public Response<Object> syncCourseStats() {
        log.info("手动触发课程统计数据同步...");
        courseRankingScheduler.manualSync();
        return new Response<>("课程统计数据同步成功");
    }
}
