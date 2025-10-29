package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.v1.annotation.JsonParam;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.CourseTocDataService;
import com.prosper.learn.domain.service.data.UserCourseTocDataService;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.CourseTocDO;
import com.prosper.learn.persistence.dataobject.UserCourseTocDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 课程目录接口
 * 处理用户课程目录的相关操作
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TocController {

    private final CourseMapper courseMapper;
    private final UserCourseTocDataService userCourseTocDataService;
    private final CourseTocDataService courseTocDataService;
    private final ObjectMapper objectMapper;

    /**
     * 更新用户课程目录
     * 映射: POST /toc → PUT /api/v1/users/current/courses/{courseId}/toc
     */
    @PutMapping("/users/current/courses/{courseId}/toc")
    @SaCheckLogin
    public ApiResponse<String> updateUserCourseToc(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @JsonParam("indexArray") @NotBlank(message = "索引数组不能为空") String indexArray,
            @CurrentUser UserDO currentUser) {

        // 验证课程存在性
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        // 获取当前用户课程目录
        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(currentUser.getId(), courseId);
        if (userCourseTocDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        String toc = userCourseTocDO.getToc();
        String[] tocHashes = toc.split(",");

        // 解析并验证索引数组
        String[] indexStrings = indexArray.split(",");
        if (indexStrings.length > 9) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int[] indexes = new int[indexStrings.length];
        for (int i = 0; i < indexStrings.length; i++) {
            try {
                indexes[i] = Integer.parseInt(indexStrings[i]);
                if (Math.abs(indexes[i]) > tocHashes.length) {
                    throw ErrorCode.SYSTEM_ERROR.exception();
                }
            } catch (NumberFormatException e) {
                throw ErrorCode.SYSTEM_ERROR.exception();
            }
        }

        // 创建默认目录结构
        ObjectNode defaultTocNode = objectMapper.createObjectNode();
        defaultTocNode.set(Long.toString(courseDO.getRootNodeId()), objectMapper.createObjectNode());
        String defaultTocStr = defaultTocNode.toString();
        String defaultTocHash = Utils.hashSHA(defaultTocStr);

        // 如果默认目录不存在则创建
        if (courseTocDataService.get(defaultTocHash) == null) {
            courseTocDataService.insert(new CourseTocDO(defaultTocHash, defaultTocStr));
        }

        // 构建新的目录数组
        String[] newTocArr = new String[indexes.length];
        int count = 0;
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] != 0) {
                newTocArr[i] = tocHashes[Math.abs(indexes[i]) - 1];
            } else {
                newTocArr[i] = defaultTocHash;
                count++;
            }
        }

        // 增加默认目录的引用计数
        courseTocDataService.incrRef(defaultTocHash, count);

        // 更新用户课程目录
        String newToc = String.join(",", newTocArr);
        userCourseTocDO.setToc(newToc);
        userCourseTocDataService.update(userCourseTocDO);

        return ApiResponse.success("目录更新成功");
    }

    /**
     * 获取用户课程目录
     * 新增接口: GET /api/v1/users/current/courses/{courseId}/toc
     */
    @GetMapping("/users/current/courses/{courseId}/toc")
    @SaCheckLogin
    public ApiResponse<String> getUserCourseToc(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(currentUser.getId(), courseId);

        if (userCourseTocDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        return ApiResponse.success(userCourseTocDO.getToc());
    }
}