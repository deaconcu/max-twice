package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.toc.NodeTocDO;
import com.prosper.learn.content.toc.NodeTocDataService;
import com.prosper.learn.content.toc.UserNodeTocDO;
import com.prosper.learn.content.toc.UserNodeTocDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 课程目录接口测试
 * 测试文档: docs/test/toc.md
 */
@Transactional
public class TocControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private UserNodeTocDataService userNodeTocDataService;

    @Autowired
    private NodeTocDataService nodeTocDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        return userDomainService.createUser(email, "password123");
    }

    /**
     * 创建测试课程
     */
    private CourseDO createCourse(Long creatorId) {
        CourseDO course = new CourseDO();
        course.setCreatorId(creatorId);
        course.setName("测试课程");
        course.setDescription("测试课程描述");
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L);
        course.setState(ContentState.PUBLISHED.value());
        courseDataService.insert(course);

        // 创建根节点
        NodeDO rootNode = new NodeDO();
        rootNode.setNodeId(course.getId());
        rootNode.setName("根节点");
        rootNode.setDescription("根节点描述");
        rootNode.setCreatorId(creatorId);
        rootNode.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(rootNode);

        // 更新课程的 rootNodeId
        course.setRootNodeId(rootNode.getId());
        courseDataService.update(course);

        return course;
    }

    /**
     * 创建用户课程目录
     */
    private UserNodeTocDO createUserNodeToc(Long userId, Long courseId, String tocHashes) {
        UserNodeTocDO userNodeTocDO = new UserNodeTocDO();
        userNodeTocDO.setUserId(userId);
        userNodeTocDO.setNodeId(courseId);
        userNodeTocDO.setToc(tocHashes);
        userNodeTocDataService.insert(userNodeTocDO);
        return userNodeTocDO;
    }

    /**
     * 创建课程目录版本
     */
    private NodeTocDO createNodeToc(String hash, String tocContent) {
        NodeTocDO nodeTocDO = new NodeTocDO(hash, tocContent);
        nodeTocDataService.insert(nodeTocDO);
        return nodeTocDO;
    }

    /**
     * 生成目录内容JSON
     */
    private String createTocJson(Long nodeId) {
        ObjectNode node = objectMapper.createObjectNode();
        node.set(Long.toString(nodeId), objectMapper.createObjectNode());
        return node.toString();
    }

    // ==================== 测试用例 ====================

    /**
     * 测试1: 使用现有哈希重新排列目录
     */
    @Test
    @DisplayName("更新目录 - 使用现有哈希重新排列")
    void testUpdateToc_RearrangeExistingHashes() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-toc-1@test.com");
        CourseDO course = createCourse(user.getId());

        // 创建3个目录版本
        String tocA = createTocJson(1L);
        String tocB = createTocJson(2L);
        String tocC = createTocJson(3L);
        String hashA = Utils.hashSHA(tocA);
        String hashB = Utils.hashSHA(tocB);
        String hashC = Utils.hashSHA(tocC);

        createNodeToc(hashA, tocA);
        createNodeToc(hashB, tocB);
        createNodeToc(hashC, tocC);

        // 创建用户目录
        createUserNodeToc(user.getId(), course.getId(), hashA + "," + hashB + "," + hashC);

        StpUtil.login(user.getId());

        try {
            // 反转顺序：3,2,1
            String requestBody = String.format("{\"indexArray\": \"3,2,1\"}");

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("目录更新成功"));

            // 验证数据库更新
            UserNodeTocDO updated = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updated.getToc()).isEqualTo(hashC + "," + hashB + "," + hashA);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试2: 使用默认目录（索引为0）
     */
    @Test
    @DisplayName("更新目录 - 使用默认目录")
    void testUpdateToc_UseDefaultToc() throws Exception {
        UserDO user = createUser("test-toc-2@test.com");
        CourseDO course = createCourse(user.getId());

        // 创建2个目录版本
        String tocA = createTocJson(1L);
        String tocB = createTocJson(2L);
        String hashA = Utils.hashSHA(tocA);
        String hashB = Utils.hashSHA(tocB);

        createNodeToc(hashA, tocA);
        createNodeToc(hashB, tocB);

        createUserNodeToc(user.getId(), course.getId(), hashA + "," + hashB);

        StpUtil.login(user.getId());

        try {
            // 索引：1,0,2 (中间使用默认目录)
            String requestBody = "{\"indexArray\": \"1,0,2\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证数据库
            UserNodeTocDO updated = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            String[] hashes = updated.getToc().split(",");
            assertThat(hashes).hasSize(3);
            assertThat(hashes[0]).isEqualTo(hashA);
            assertThat(hashes[2]).isEqualTo(hashB);
            // 验证中间是默认目录
            String defaultToc = createTocJson(course.getRootNodeId());
            String defaultHash = Utils.hashSHA(defaultToc);
            assertThat(hashes[1]).isEqualTo(defaultHash);

            // 验证引用计数
            NodeTocDO defaultTocDO = nodeTocDataService.get(defaultHash);
            assertThat(defaultTocDO).isNotNull();
            assertThat(defaultTocDO.getRefCount()).isGreaterThan(0);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试3: 全部使用默认目录
     */
    @Test
    @DisplayName("更新目录 - 全部使用默认目录")
    void testUpdateToc_AllDefault() throws Exception {
        UserDO user = createUser("test-toc-3@test.com");
        CourseDO course = createCourse(user.getId());

        String tocA = createTocJson(1L);
        String hashA = Utils.hashSHA(tocA);
        createNodeToc(hashA, tocA);
        createUserNodeToc(user.getId(), course.getId(), hashA);

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"0,0,0\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证
            UserNodeTocDO updated = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            String[] hashes = updated.getToc().split(",");
            assertThat(hashes).hasSize(3);

            String defaultHash = Utils.hashSHA(createTocJson(course.getRootNodeId()));
            assertThat(hashes[0]).isEqualTo(defaultHash);
            assertThat(hashes[1]).isEqualTo(defaultHash);
            assertThat(hashes[2]).isEqualTo(defaultHash);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试4: 索引数组最大长度（9个元素）
     */
    @Test
    @DisplayName("更新目录 - 索引数组最大长度")
    void testUpdateToc_MaxLength() throws Exception {
        UserDO user = createUser("test-toc-4@test.com");
        CourseDO course = createCourse(user.getId());

        // 创建9个目录版本
        StringBuilder tocHashes = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            String toc = createTocJson((long) i);
            String hash = Utils.hashSHA(toc);
            createNodeToc(hash, toc);
            if (i > 1) tocHashes.append(",");
            tocHashes.append(hash);
        }

        createUserNodeToc(user.getId(), course.getId(), tocHashes.toString());

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1,2,3,4,5,6,7,8,9\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试5: 课程ID为null字符串
     * 路径中的 "null" 会被当作字符串解析，验证实际错误
     */
    @Test
    @DisplayName("参数验证 - 课程ID为null字符串")
    void testUpdateToc_NullCourseId() throws Exception {
        UserDO user = createUser("test-toc-5@test.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            // "null" 会被当作字符串，尝试转换为 Long 会失败
            mockMvc.perform(put("/api/v1/users/current/courses/null/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试6: 课程ID为0
     */
    @Test
    @DisplayName("参数验证 - 课程ID为0")
    void testUpdateToc_ZeroCourseId() throws Exception {
        UserDO user = createUser("test-toc-6@test.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/0/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试7: 课程ID为负数
     */
    @Test
    @DisplayName("参数验证 - 课程ID为负数")
    void testUpdateToc_NegativeCourseId() throws Exception {
        UserDO user = createUser("test-toc-7@test.com");
        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/-1/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试8: 索引数组为空字符串
     */
    @Test
    @DisplayName("参数验证 - 索引数组为空")
    void testUpdateToc_EmptyIndexArray() throws Exception {
        UserDO user = createUser("test-toc-8@test.com");
        CourseDO course = createCourse(user.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试9: 索引数组为null
     */
    @Test
    @DisplayName("参数验证 - 索引数组为null")
    void testUpdateToc_NullIndexArray() throws Exception {
        UserDO user = createUser("test-toc-9@test.com");
        CourseDO course = createCourse(user.getId());

        StpUtil.login(user.getId());

        try {
            String requestBody = "{}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试10: 索引数组长度超过9
     */
    @Test
    @DisplayName("参数验证 - 索引数组长度超过9")
    void testUpdateToc_TooLongIndexArray() throws Exception {
        UserDO user = createUser("test-toc-10@test.com");
        CourseDO course = createCourse(user.getId());

        String toc = createTocJson(1L);
        String hash = Utils.hashSHA(toc);
        createNodeToc(hash, toc);
        createUserNodeToc(user.getId(), course.getId(), hash);

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1,1,1,1,1,1,1,1,1,1\"}"; // 10个

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试11: 索引值包含非数字字符
     */
    @Test
    @DisplayName("参数验证 - 索引值包含非数字字符")
    void testUpdateToc_InvalidIndexFormat() throws Exception {
        UserDO user = createUser("test-toc-11@test.com");
        CourseDO course = createCourse(user.getId());

        String toc = createTocJson(1L);
        String hash = Utils.hashSHA(toc);
        createNodeToc(hash, toc);
        createUserNodeToc(user.getId(), course.getId(), hash);

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1,abc,3\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试12: 索引值超出范围
     */
    @Test
    @DisplayName("参数验证 - 索引值超出范围")
    void testUpdateToc_IndexOutOfBounds() throws Exception {
        UserDO user = createUser("test-toc-12@test.com");
        CourseDO course = createCourse(user.getId());

        // 创建3个目录版本
        StringBuilder tocHashes = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            String toc = createTocJson((long) i);
            String hash = Utils.hashSHA(toc);
            createNodeToc(hash, toc);
            if (i > 1) tocHashes.append(",");
            tocHashes.append(hash);
        }
        createUserNodeToc(user.getId(), course.getId(), tocHashes.toString());

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1,2,4\"}"; // 4超出范围

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.TOC_INDEX_OUT_OF_BOUNDS.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试13: 课程不存在
     */
    @Test
    @DisplayName("业务错误 - 课程不存在")
    void testUpdateToc_CourseNotFound() throws Exception {
        UserDO user = createUser("test-toc-13@test.com");

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/99999/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试14: 用户目录不存在
     */
    @Test
    @DisplayName("业务错误 - 用户目录不存在")
    void testUpdateToc_UserTocNotFound() throws Exception {
        UserDO user = createUser("test-toc-14@test.com");
        CourseDO course = createCourse(user.getId());
        // 不创建用户目录

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.TOC_USER_TOC_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试15: 未登录访问
     */
    @Test
    @DisplayName("认证 - 未登录访问")
    void testUpdateToc_NotLogin() throws Exception {
        String requestBody = "{\"indexArray\": \"1\"}";

        mockMvc.perform(put("/api/v1/users/current/courses/1/toc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试16: 获取存在的用户目录
     */
    @Test
    @DisplayName("获取目录 - 成功获取")
    void testGetUserNodeToc_Success() throws Exception {
        UserDO user = createUser("test-toc-16@test.com");
        CourseDO course = createCourse(user.getId());

        String toc = createTocJson(1L);
        String hash = Utils.hashSHA(toc);
        createNodeToc(hash, toc);
        createUserNodeToc(user.getId(), course.getId(), hash + "," + hash);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(hash + "," + hash));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试17: 获取不存在的用户目录返回null
     */
    @Test
    @DisplayName("获取目录 - 用户目录不存在返回null")
    void testGetUserNodeToc_NotFoundReturnsNull() throws Exception {
        UserDO user = createUser("test-toc-17@test.com");
        CourseDO course = createCourse(user.getId());
        // 不创建用户目录

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").doesNotExist()); // null不会出现在JSON中

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试18: 获取目录 - 未登录
     */
    @Test
    @DisplayName("获取目录 - 未登录访问")
    void testGetUserNodeToc_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/users/current/courses/1/toc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试19: 索引数组只有1个元素（边界条件）
     */
    @Test
    @DisplayName("边界条件 - 索引数组长度为1")
    void testUpdateToc_SingleElement() throws Exception {
        UserDO user = createUser("test-toc-19@test.com");
        CourseDO course = createCourse(user.getId());

        String toc = createTocJson(1L);
        String hash = Utils.hashSHA(toc);
        createNodeToc(hash, toc);
        createUserNodeToc(user.getId(), course.getId(), hash);

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试20: 重复索引值
     */
    @Test
    @DisplayName("边界条件 - 重复索引值")
    void testUpdateToc_DuplicateIndexes() throws Exception {
        UserDO user = createUser("test-toc-20@test.com");
        CourseDO course = createCourse(user.getId());

        String toc = createTocJson(1L);
        String hash = Utils.hashSHA(toc);
        createNodeToc(hash, toc);
        createUserNodeToc(user.getId(), course.getId(), hash);

        StpUtil.login(user.getId());

        try {
            String requestBody = "{\"indexArray\": \"1,1,1\"}";

            mockMvc.perform(put("/api/v1/users/current/courses/" + course.getId() + "/toc")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证目录都是相同的哈希
            UserNodeTocDO updated = userNodeTocDataService.getByUserAndNode(user.getId(), course.getId());
            assertThat(updated.getToc()).isEqualTo(hash + "," + hash + "," + hash);

        } finally {
            StpUtil.logout();
        }
    }
}
